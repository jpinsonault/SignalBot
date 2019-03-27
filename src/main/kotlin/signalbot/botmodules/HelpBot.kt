package signalbot.botmodules

import signalbot.Client
import signalbot.Message
import signalbot.UrlUtils
import signalbot.ifFound

class HelpBot{
    companion object {
        fun checkMessage(client: Client, message: Message){
            val helpRegex = Regex("""^!help$""")

            helpRegex.ifFound(message.content){
                
            }
        }
    }
}