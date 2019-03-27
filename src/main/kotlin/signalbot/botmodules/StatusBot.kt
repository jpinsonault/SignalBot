package signalbot.botmodules

import signalbot.Client
import signalbot.Message
import signalbot.UrlUtils
import signalbot.ifFound

class StatusBot{
    companion object {
        fun checkMessage(client: Client, message: Message){
            val statusRegex = Regex("""^!status\s*$""")

            statusRegex.ifFound(message.content){

                client.replyTo(message, "Uptime (minutes): %.2f".format(client.upTimeMinutes))
            }
        }
    }
}