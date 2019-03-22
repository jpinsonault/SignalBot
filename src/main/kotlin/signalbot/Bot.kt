package signalbot

import org.apache.commons.io.FilenameUtils
import org.dizitart.no2.Document.createDocument
import java.io.File
import org.dizitart.no2.Nitrite
import org.dizitart.no2.UpdateOptions
import org.dizitart.no2.filters.Filters.eq
import signalbot.UrlUtils.Companion.loadAttachment
import java.net.URL


class Message(val content: String, val author: String, val groupInfo: GroupInfo?)

class GroupInfo(val id: String, val name: String)

class Envelope(val from: String,
               val timestamp: Long,
               val messageTimestamp: Long?,
               val body: String?,
               val groupInfo: GroupInfo?)

class Client(val userPhone: String){
    private var onReadyCallback: (Client) -> Unit = {}
    private var onMessageCallback: (Client, Message) -> Unit = {_, _ ->}
    lateinit var db: Nitrite
    lateinit var tempDir: File

    fun start(){
        db = Nitrite.builder()
            .compressed()
            .filePath("./bot.db")
            .openOrCreate()

        tempDir = createTempDir()

        onReadyCallback(this)

        while (true){
            runReceiveCommand()
        }
    }

    fun onReady(callback: (Client) -> Unit){
        onReadyCallback = callback
    }

    fun onMessage(callback: (Client, Message) -> Unit){
        onMessageCallback = callback
    }

    fun gotEnvelope(envelope: Envelope){
        if(envelope.body != null){
            val message = Message(envelope.body, envelope.from, envelope.groupInfo)
            onMessageCallback(this, message)
        }
    }

    // Parse envelopes and call appropriate callbacks
    fun parseReceivedMessage(rawText: String): Envelope? {
        val fromRegex = """Envelope from: (\+\d+)""".toRegex(RegexOption.MULTILINE)
        val timestampRegex = """Timestamp: (\d+)""".toRegex(RegexOption.MULTILINE)
        val messageTimestampRegex = """Message timestamp: (\d+)""".toRegex(RegexOption.MULTILINE)
        val bodyRegex = """Body: (.*)""".toRegex(RegexOption.MULTILINE)
        val groupRegex = """Group info:""".toRegex(RegexOption.MULTILINE)
        val groupIdRegex = """  Id: ([^\s]+)""".toRegex(RegexOption.MULTILINE)
        val groupNameRegex = """  Name: ([^\s]+)""".toRegex(RegexOption.MULTILINE)

        val fromMatch = fromRegex.find(rawText)?.groupValues?.get(1)
        val timestampMatch = timestampRegex.find(rawText)?.groupValues?.get(1)
        val messageTimestampMatch = messageTimestampRegex.find(rawText)?.groupValues?.get(1)
        val bodyMatch = bodyRegex.find(rawText)?.groupValues?.get(1)
        val groupMatch = groupRegex.find(rawText)?.groupValues != null

        var groupInfo: GroupInfo? = null
        if (groupMatch){
            val groupId = groupIdRegex.find(rawText)?.groupValues?.get(1)
            val groupName = groupNameRegex.find(rawText)?.groupValues?.get(1)

            groupInfo = GroupInfo(groupId!!, groupName!!)
        }

        if (fromMatch == null) return null

        return Envelope(
            from = fromMatch,
            timestamp = timestampMatch!!.toLong(),
            messageTimestamp = messageTimestampMatch?.toLong(),
            body = bodyMatch,
            groupInfo = groupInfo
        )
    }

    fun runSendCommand(content: String, toPhone: String, attachment: File? = null){
        var command = "$signalCli -u $userPhone send -m \"$content\" $toPhone"

        if (attachment != null){
            command = "$command -a ${attachment.absolutePath}"
        }
        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    fun runSendToGroupCommand(content: String, groupID: String, attachment: File? = null){
        var command = "$signalCli -u $userPhone send -m \"$content\" -g $groupID"

        if (attachment != null){
            command = "$command -a ${attachment.absolutePath}"
        }
        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    fun runReceiveCommand(){
        val output = "$signalCli -u $userPhone receive".runCommand(File(System.getProperty("user.dir")))
        val messages = output.split("\r\n\r\n").map { it.trim() }

        val envelopes = messages.map { parseReceivedMessage(it) }.filterNotNull()

        envelopes.forEach { gotEnvelope(it) }
    }

    fun saveKeyValue(prefix: String, key: String, value: Any){
        val collection = db.getCollection(prefix)

        val doc = createDocument("key", key)
            .put("value", value)

        collection.update(eq("key", key), doc, UpdateOptions.updateOptions(true))
    }

    fun removeKeyValue(prefix: String, key: String){
        val collection = db.getCollection(prefix)

        collection.remove(eq("key", key))
    }

    fun <T>loadKeyValue(prefix: String, key: String, default: T): T{
        val collection = db.getCollection(prefix)

        val data = collection.find(eq("key", key))

        if (data.size() == 0){
            return default
        }

        return data.first().getValue("value") as T
    }

    fun replyTo(message: Message, content: String, attachment: File? = null){
        val messageAttachment: File? = attachment ?: loadAttachment(content, tempDir)

        if (message.groupInfo != null){
            sendToGroup(content, message.groupInfo.id, messageAttachment)
        }
        else{
            send(content, message.author, messageAttachment)
        }
    }

    fun send(content: String, toPhone: String, attachment: File? = null){
        runSendCommand(content, toPhone, attachment)
    }

    fun sendToGroup(content: String, groupId: String, attachment: File? = null){
        runSendToGroupCommand(content, groupId, attachment)
    }

    companion object {
        val signalCli = "signal-cli.bat"
    }
}