package signalbot.botmodules

import org.apache.commons.io.FilenameUtils
import org.apache.http.client.utils.URLEncodedUtils
import signalbot.*
import signalbot.UrlUtils.Companion.getAnyUrlIn
import signalbot.UrlUtils.Companion.loadAttachment
import java.io.File
import java.net.URL
import java.nio.charset.Charset

class MediaPrieviewBot{
    companion object {
        val streamableHost = "streamable.com"
        val youtubeHost = "youtube.com"
        val youtubeShortHost = "youtu.be"

        val youtubeDlHosts = listOf(youtubeHost, streamableHost, youtubeShortHost)

        fun checkMessage(client: Client, message: Message){
            val youtubeDlRegex = Regex("""^!youtube-dl\s+(.+)""")

            youtubeDlRegex.ifFound(message.content) {
                val possibleUrl = UrlUtils.getAnyUrlIn(message.content)
                println("content: ${message.content} - $possibleUrl")

                if (possibleUrl != null){
                    val attachment = getYoutubeDlAttachment(client, possibleUrl.toString())

                    if (attachment != null){
                        client.replyTo(message, possibleUrl.toString(), attachment)
                    }
                }
            }

            Regex("""!(p|preview)\s+(.+)""").ifFound(message.content){
                getImageOrPreview(client, message.content).ifNotNull {attachment ->
                    client.replyTo(message, "", attachment)
                }
            }
        }

        fun getImageOrPreview(client: Client, content: String): File? {
            return getAnyUrlIn(content).ifNotNull {url ->
                 loadAttachment(url.toString(), client.tempDir)
            }
        }

        fun getYoutubeDlAttachment(client: Client, content: String): File?{
            val url = getAnyUrlIn(content)
            if (url != null && youtubeDlHosts.contains(url.host)){
                val command = "youtube-dl --no-continue -o \"${client.tempDir}/%(id)s.%(ext)s\" $url"
                val output = command.runCommand(File(System.getProperty("user.dir")))

                val desinationRegex = Regex("""\[download] Destination: ([^\s]+)""")
                val mergingIntoRegex = Regex("""\[ffmpeg] Merging formats into "([^"]+)""")
                val mergingIntoMatches = mergingIntoRegex.find(output)?.groupValues

                if(mergingIntoMatches != null){
                    return File(mergingIntoMatches[1])
                }
                else{
                    return desinationRegex.ifFound(output){
                        File(it.groupValues[1])
                    }
                }
            }

            return null
        }

        fun getYoutubeThumbnailAttachment(client: Client, content: String): File?{
            val possibleUrl = UrlUtils.getAnyUrlIn(content)

            if (possibleUrl != null) {
                val videoId = if(possibleUrl.host.contains(youtubeHost)){
                    val params = URLEncodedUtils.parse(possibleUrl.toURI(), Charset.forName("UTF-8"))
                    params.firstOrNull { it.name == "v" }?.value
                }else if (possibleUrl.host.contains(youtubeShortHost)){
                    FilenameUtils.getName(possibleUrl.path)
                }
                else { null }

                if (videoId != null){
                    println(youtubeThumbnailUrl(videoId))
                    return loadAttachment(youtubeThumbnailUrl(videoId), client.tempDir)
                }
            }
            return null
        }

        fun init(client: Client){
            client.onAttachmentInsertion { client, content ->
                getYoutubeDlAttachment(client, content)
            }

            client.onAttachmentInsertion { client, content ->
                getYoutubeThumbnailAttachment(client, content)
            }
        }

        fun youtubeThumbnailUrl(youtubeId: String): String{
            return "https://img.youtube.com/vi/${youtubeId}/hqdefault.jpg"
        }
    }
}