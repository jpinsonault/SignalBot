package signalbot

import org.dizitart.no2.Document.createDocument
import java.io.File
import org.dizitart.no2.filters.Filters.eq
import signalbot.UrlUtils.Companion.loadAttachment
import signalbot.botmodules.PauserBot
import signalbot.botmodules.RateLimiter
import org.dizitart.no2.*
import org.dizitart.no2.filters.Filters
import java.nio.file.Paths


class Message(val content: String, val author: String, val groupInfo: GroupInfo?)

class GroupInfo(val id: String, val name: String)

class Envelope(val from: String,
               val timestamp: Long,
               val messageTimestamp: Long?,
               val body: String?,
               val groupInfo: GroupInfo?)

class Client{
    private var onReadyCallback: (Client) -> Unit = {}
    private var onMessageCallback: (Client, Message) -> Unit = {_, _ ->}
    private val onAttachmentInsertionCallbacks = mutableListOf<(Client, String) -> File?>()
    lateinit var db: Nitrite
    lateinit var tempDir: File
    var paused: Boolean = false
    private val onTimerCallbacks = mutableMapOf<RateLimiter, (Client) -> Unit>()
    lateinit var botPhone: String

    val botStartTime: Long = System.currentTimeMillis()

    val upTimeMinutes: Double
        get() = (System.currentTimeMillis() - botStartTime)/1000.0/60.0

    fun start(){
        val config = Config.loadFile(Paths.get(System.getProperty("user.home"), ".signal").toString())
        botPhone = config.botPhone

        db = Nitrite.builder()
            .compressed()
            .filePath("./bot.db")
            .openOrCreate()

        tempDir = createTempDir()

        onReadyCallback(this)

        while (true){
            onTimerCallbacks.forEach {limiter, callback ->
                limiter.tryOrSkip { callback(this) }
            }

            runReceiveCommand()
        }
    }

    fun pause(){
        println("Pausing bot")
        paused = true
    }

    fun unpause(){
        println("Unpausing bot")
        paused = false
    }

    fun onReady(callback: (Client) -> Unit){
        onReadyCallback = callback
    }

    fun onMessage(callback: (Client, Message) -> Unit){
        onMessageCallback = { client, message ->
            // Insert any built in bots here
            PauserBot.checkMessage(client, message)

            if(!client.paused) {
                callback(client, message)
            }
        }
    }

    fun onPeriodicTimer(periodMilliseconds: Long, callback: (Client) -> Unit){
        val rateLimiter = RateLimiter(periodMilliseconds)
        onTimerCallbacks[rateLimiter] = callback
    }

    // Intercept outgoing bot messages to override attachments
    fun onAttachmentInsertion(callback: (Client, String) -> File?){
        onAttachmentInsertionCallbacks.add(callback)
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
        var command = "$signalCli -u $botPhone send -m \"$content\" $toPhone"

        if (attachment != null){
            command = "$command -a ${attachment.absolutePath}"
        }
        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    fun runSendToGroupCommand(content: String, groupID: String, attachment: File? = null){
        var command = "$signalCli -u $botPhone send -m \"$content\" -g $groupID"

        if (attachment != null){
            command = "$command -a ${attachment.absolutePath}"
        }
        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    fun runUpdateGroupPhotoCommand(photo: File, groupId: String){
        val command = "$signalCli -u $botPhone updateGroup -g \"$groupId\" -a \"${photo.absolutePath}\""

        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    fun runUpdateGroupNameCommand(name: String, groupId: String){
        val command = "$signalCli -u $botPhone updateGroup -g \"$groupId\" -n \"${name}\""

        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    fun runReceiveCommand(){
        val output = "$signalCli -u $botPhone receive".runCommand(File(System.getProperty("user.dir")))
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

    fun saveDocument(prefix: String, document: Document){
        val collection = db.getCollection(prefix)

        collection.insert(document)
    }

    fun loadDocuments(prefix: String, filter: Filter): List<Document>{
        val collection = db.getCollection(prefix)

        val found = collection.find(filter)

        if(found.size() > 0){
            return found.toList()
        }

        return listOf()
    }

    fun removeDocument(prefix: String, document: Document) {
        val collection = db.getCollection(prefix)

        collection.remove(document)
    }

    fun removeDocuments(prefix: String, filter: Filter) {
        val collection = db.getCollection(prefix)

        collection.remove(filter)
    }

    fun clearCollection(prefix: String){
        val collection = db.getCollection(prefix)

        collection.remove(Filters.ALL)
    }

    // Returns the first attachment found or null
    fun getAttachmentFromCallbacks(content: String): File? {
        for (callback in onAttachmentInsertionCallbacks) {
            val attachment = callback(this, content)
            if (attachment != null) return attachment
        }

        return null
    }

    fun replyTo(message: Message, content: String, attachment: File? = null){
        val isGroup = message.groupInfo != null
        val to = if (isGroup) message.groupInfo!!.id else message.author

        send(content, to, isGroup, attachment)
    }

    fun send(content: String, to: String, group: Boolean, attachment: File? = null){
        val messageAttachment = attachment ?: getAttachmentFromCallbacks(content)

        if (group){
            runSendToGroupCommand(content, to, messageAttachment)
        }
        else{
            runSendCommand(content, to, messageAttachment)
        }

        clearTempDir()
    }

    fun clearTempDir(){
        val files = tempDir.listFiles()

        files.forEach { it.deleteRecursively() }
    }

    fun updateGroupPhoto(photoUrl: String, groupId: String){
        val photoLocation = loadAttachment(photoUrl, tempDir)

        if(photoLocation != null){
            runUpdateGroupPhotoCommand(photoLocation, groupId)
        }
    }

    fun updateGroupName(name: String, groupId: String){
        runUpdateGroupNameCommand(name, groupId)
    }

    companion object {
        val signalCli = "signal-cli.bat"
    }
}
