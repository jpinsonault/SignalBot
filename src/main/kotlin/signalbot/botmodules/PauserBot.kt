package signalbot.botmodules

import signalbot.Client
import signalbot.Message

class PauserBot{
    companion object {
        val wakeMeUrl = "https://i.imgur.com/CumCrSI.png"
        val mustCrushUrl = "https://i.imgur.com/GZnzuhv.png"

        fun checkMessage(client: Client, message: Message){
            println("%%%%%%%%%%%%%%%")
            val pauseRegex = Regex("""^!pause$""")
            val unPauseRegex = Regex("""^!unpause$""")

            if (pauseRegex.containsMatchIn(message.content)){
                client.pause()
                client.api.replyTo(message, "Pausing bot - $wakeMeUrl")
            }
            if (unPauseRegex.containsMatchIn(message.content)){
                client.unpause()
                client.api.replyTo(message, "Unpausing bot - $mustCrushUrl")
            }
        }
    }
}