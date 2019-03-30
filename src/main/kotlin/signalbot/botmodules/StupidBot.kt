package signalbot.botmodules

import signalbot.Client
import signalbot.Message
import signalbot.UrlUtils.Companion.loadAttachment
import signalbot.ifFound

class StupidBot{
    companion object {
        val stupidBotUrls = listOf(
            "https://i.imgur.com/5Y0NNEE.jpg",
            "https://i.imgur.com/JI5R7bG.jpg"
        )
        val goodBotUrls = listOf(
            "https://i.imgur.com/vXU6KzD.png",
            "https://i.imgur.com/PAQPgiR.jpg"
        )

        fun checkMessage(client: Client, message: Message){
            val badRegex = Regex("""^(bad|dumb|stupid|fucking|shutup) (robot|bot)$""")
            val goodRegex = Regex("""^(good) (robot|bot)$""")

            badRegex.ifFound(message.content){
                val attachment = loadAttachment(stupidBotUrls.random(), client.tempDir)
                client.api.replyTo(message, "=[ I'm sorry", attachment)
            }

            goodRegex.ifFound(message.content){
                val attachment = loadAttachment(goodBotUrls.random(), client.tempDir)
                client.api.replyTo(message, "=] Aww thanks", attachment)
            }
        }
    }
}