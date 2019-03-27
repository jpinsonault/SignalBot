package signalbot

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URL

class UrlUtils{
    companion object {
        fun loadAttachment(content: String, outputDirectory: File): File?{
            val url = getAnyUrlIn(content)
            if(url != null){
                val filename = FilenameUtils.getName(url.path)
                val extension = FilenameUtils.getExtension(url.path)

                if(extension != null && extension != ""){
                    val localFilepath = downloadFromUrl(url, outputDirectory, filename)

                    return if (localFilepath == null){
                        null
                    } else{
                        File(localFilepath)
                    }
                }
            }
            return null
        }

        fun getAnyUrlIn(content: String): URL? {
            val regex = Regex("""(https?://(?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?://(?:www\.|(?!www))[a-zA-Z0-9]+\.[^\s]{2,}|www\.[a-zA-Z0-9]+\.[^\s]{2,})""")

            val match = regex.find(content)
            if(match != null) {
                val url = URL(match.groupValues[0])
                return url
            }

            return null
        }

        @Throws(IOException::class)
        fun downloadFromUrl(url: URL, directory: File, filename: String): String? {
            val outputPath = "$directory/$filename"

            try {
                FileUtils.copyURLToFile(url, File(outputPath))
                return outputPath
            } catch (e: Exception){
                return null
            }
        }
    }
}