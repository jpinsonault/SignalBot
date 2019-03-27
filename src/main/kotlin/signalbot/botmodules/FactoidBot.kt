package signalbot.botmodules

import signalbot.Client
import signalbot.Message

class FactoidBot{
    companion object {
        val prefix = "FactoidBot"

        fun checkMessage(client: Client, message: Message){
            val setRegex = Regex("""^~(.*) +is +(.*)\s*""")
            val setMatches = setRegex.find(message.content)?.groupValues

            val unsetRegex = Regex("""~(.*) +unset""")
            val unsetMatches = unsetRegex.find(message.content)?.groupValues

            val getRegex = Regex("""\s*~(.*)\s*""")
            val getMatches = getRegex.find(message.content)?.groupValues

            if (setMatches != null){
                val key = setMatches[1]
                val value = setMatches[2]

                client.saveKeyValue(prefix, key, value)
                client.replyTo(message, "ok, $key is $value")
                return
            }

            if (unsetMatches != null){
                val key = unsetMatches[1]

                client.removeKeyValue(prefix, key)
                client.replyTo(message, "ok, $key has been unset")
                return
            }

            if (getMatches != null){
                val key = getMatches[1]

                val value = client.loadKeyValue(prefix, key, "")

                if (value != ""){
                    client.replyTo(message, "$key is $value")
                }
                return
            }
        }
    }
}