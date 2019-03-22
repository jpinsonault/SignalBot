package signalbot

import java.io.File

class MediaPrieviewBot{
    companion object {
        val streamableHost = "streamable.com"

        fun checkMessage(client: Client, message: Message){
            val possibleUrl = UrlUtils.getAnyUrlIn(message.content)

            if (possibleUrl != null && possibleUrl.host == streamableHost){
                val command = "youtube-dl --no-continue -o \"${client.tempDir}/%(id)s.%(ext)s\" $possibleUrl"
                val output = command.runCommand(File(System.getProperty("user.dir")))

                val desinationRegex = Regex("""\[download] Destination: ([^\s]+)""")
                val destination = desinationRegex.find(output)?.groupValues?.get(1)

                println(output)
                println("destination: $destination")
                if (destination != null){
                    client.replyTo(message, possibleUrl.toString(), File(destination))
                }
            }
        }
    }
}