package signalbot.botmodules

import signalbot.Client
import signalbot.Message
import signalbot.ifFound
import java.util.*

class DiceBot{
    companion object {
        fun checkMessage(client: Client, message: Message) {
            val regex = Regex("""^!roll (\d+)?[dD](\d+)""")

            regex.ifFound(message.content) { matches ->
                if (matches.groupValues[1] == "") {
                    val size = matches.groupValues[2].toIntOrNull()

                    if(size != null){
                        val result = Random().nextInt(size)
                        client.replyTo(message, "Rolled a d$size - $result")
                    }
                }
            }
        }
    }
}