package signalbot

class PlusPlus{
    companion object {
        val prefix = "PlusPlusBot"

        fun checkMessage(client: Client, message: Message){
            val regex = Regex("""([^\s]+)(\+\+|--)""")
            val matches = regex.find(message.content)?.groupValues

            if (matches != null){
                val key = matches[1]
                val incDec = matches[2]
                if (incDec == "--") decrement(client, key)
                else increment(client, key)

                client.replyTo(message, "$key$incDec (${currentKarma(client, key)})")
            }
        }

        fun currentKarma(client: Client, key: String): Int{
            return client.loadKeyValue(prefix, key, 0)
        }

        fun increment(client: Client, key: String){
            var value = currentKarma(client, key)

            value += 1
            client.saveKeyValue(prefix, key, value)
        }

        fun decrement(client: Client, key: String){
            var value = currentKarma(client, key)

            value -= 1
            client.saveKeyValue(prefix, key, value)
        }
    }
}