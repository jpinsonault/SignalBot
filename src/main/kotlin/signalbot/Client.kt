package signalbot

import java.io.File
import signalbot.botmodules.PauserBot
import signalbot.botmodules.RateLimiter
import org.dizitart.no2.*
import signalbot.apis.DefaultSignalApi
import signalbot.apis.NitriteStorage
import signalbot.apis.SignalApi
import signalbot.apis.StorageApi
import java.nio.file.Paths


class Message(val content: String, val author: String, val groupInfo: GroupInfo?)

class GroupInfo(val id: String, val name: String)

class Envelope(val from: String,
               val timestamp: Long,
               val messageTimestamp: Long?,
               val body: String?,
               val groupInfo: GroupInfo?)

class Client {
    lateinit var tempDir: File
    lateinit var config: Config
    lateinit var storage: StorageApi
    val api: SignalApi = DefaultSignalApi(this)

    val botStartTime: Long = System.currentTimeMillis()
    var paused: Boolean = false

    val upTimeMinutes: Double
        get() = (System.currentTimeMillis() - botStartTime)/1000.0/60.0

    private var onReadyCallback: (Client) -> Unit = {}
    private var onMessageCallback: (Client, Message) -> Unit = {_, _ ->}
    private val onAttachmentInsertionCallbacks = mutableListOf<(Client, String) -> File?>()
    private val onTimerCallbacks = mutableMapOf<RateLimiter, (Client) -> Unit>()

    fun start(){
        config = Config.fromFile(Paths.get(System.getProperty("user.home"), ".signal").toString())

        val db = Nitrite.builder()
            .compressed()
            .filePath("./bot.db")
            .openOrCreate()

        storage = NitriteStorage(db)

        tempDir = createTempDir()

        onReadyCallback(this)

        while (true){
            onTimerCallbacks.forEach {limiter, callback ->
                limiter.tryOrSkip { callback(this) }
            }

            val envelopes = api.receiveEnvelopes()
            envelopes
                .filter {true}
                .forEach { gotEnvelope(it) }
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

    // Returns the first attachment found or null
    fun getAttachmentFromCallbacks(content: String): File? {
        for (callback in onAttachmentInsertionCallbacks) {
            val attachment = callback(this, content)
            if (attachment != null) return attachment
        }

        return null
    }

    fun clearTempDir(){
        val files = tempDir.listFiles()

        files.forEach { it.deleteRecursively() }
    }
}
