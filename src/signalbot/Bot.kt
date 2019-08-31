package signalbot

import java.io.File

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

    fun start(){
        onReadyCallback(this)

        while (true){
            runReceiveCommand()
            Thread.sleep(1000)
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
            println("@#$@#$@#$@#$")
            println(envelope.timestamp)
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

    fun runReceiveCommand(){
        val output = "$signalCli -u $userPhone receive".runCommand(File(System.getProperty("user.dir")))
        val output2 = """Envelope from: +15037530269 (device: 2)
Timestamp: 1553211152943 (2019-03-21T23:32:32.943Z)
Message timestamp: 1553211152943 (2019-03-21T23:32:32.943Z)
Body: asdf
Group info:
  Id: nBv/VaTfuPOD9PlkmoD5sg==
  Name: test
  Type: DELIVER
Profile key update, key length:32"""
        val messages = output.split("\r\n\r\n").map { it.trim() }

        val envelopes = messages.map { parseReceivedMessage(it) }.filterNotNull()

        envelopes.forEach { gotEnvelope(it) }
    }

    companion object {
        val signalCli = "signal-cli.bat"
    }
}