package signalbot.apis

import signalbot.*
import signalbot.UrlUtils.Companion.loadAttachment
import java.io.File

typealias sendMessageFn = (client: Client, content: String, to: String, group: Boolean, attachment: File?) -> Unit
typealias receiveEnvelopeFn = (client: Client) -> List<Envelope>

interface SignalApi{
    fun sendMessage(content: String, to: String, group: Boolean, attachment: File? = null)
    fun replyTo(message: Message, content: String, attachment: File? = null)
    fun receiveEnvelopes(): List<Envelope>
    fun updateGroupPhoto(photoUrl: String, groupId: String)
    fun updateGroupName(name: String, groupId: String)
}

class DefaultSignalApi(val client: Client): SignalApi {
    override fun sendMessage(content: String, to: String, group: Boolean, attachment: File?){
        val messageAttachment = attachment ?: client.getAttachmentFromCallbacks(content)

        if (group){
            runSendToGroupCommand(content, to, client.botPhone, messageAttachment)
        }
        else{
            runSendCommand(content, to, client.botPhone, messageAttachment)
        }

        client.clearTempDir()
    }

    override fun replyTo(message: Message, content: String, attachment: File?){
        val isGroup = message.groupInfo != null
        val to = if (isGroup) message.groupInfo!!.id else message.author

        sendMessage(content, to, isGroup, attachment)
    }

    override fun receiveEnvelopes(): List<Envelope> {
        val envelopeStrings = runReceiveEnvelopesCommand()
        val envelopes = envelopeStrings.mapNotNull { parseReceivedEnvelope(it) }

        return envelopes
    }

    override fun updateGroupPhoto(photoUrl: String, groupId: String){
        val photoLocation = loadAttachment(photoUrl, client.tempDir)

        if(photoLocation != null){
            runUpdateGroupPhotoCommand(photoLocation, groupId)
        }
    }

    override fun updateGroupName(name: String, groupId: String){
        runUpdateGroupNameCommand(name, groupId)
    }

    private fun runSendCommand(content: String, botPhone: String, toPhone: String, attachment: File? = null){
        var command = "${Client.signalCli} -u $botPhone send -m \"$content\" $toPhone"

        if (attachment != null){
            command = "$command -a ${attachment.absolutePath}"
        }
        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    private fun runSendToGroupCommand(content: String, botPhone: String, groupID: String, attachment: File? = null){
        var command = "${Client.signalCli} -u $botPhone send -m \"$content\" -g $groupID"

        if (attachment != null){
            command = "$command -a ${attachment.absolutePath}"
        }
        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    private fun runReceiveEnvelopesCommand(): List<String>{
        val output = "${Client.signalCli} -u ${client.botPhone} receive".runCommand(File(System.getProperty("user.dir")))
        val envelopeStrings = output.split("\r\n\r\n").map { it.trim() }

        return envelopeStrings
    }

    private fun runUpdateGroupPhotoCommand(photo: File, groupId: String){
        val command = "${Client.signalCli} -u ${client.botPhone} updateGroup -g \"$groupId\" -a \"${photo.absolutePath}\""

        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    private fun runUpdateGroupNameCommand(name: String, groupId: String){
        val command = "${Client.signalCli} -u ${client.botPhone} updateGroup -g \"$groupId\" -n \"${name}\""

        val output = command.runCommand(File(System.getProperty("user.dir")))
    }

    // Parse envelopes and call appropriate callbacks
    private fun parseReceivedEnvelope(rawText: String): Envelope? {
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

}