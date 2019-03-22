package signalbot

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
                val localFilepath = downloadFromUrl(url, outputDirectory, filename)

                return if (localFilepath == null){
                    null
                } else{
                    File(localFilepath)
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
            var `is`: InputStream? = null
            var fos: FileOutputStream? = null

            val outputPath = "$directory/$filename"

            try {
                //connect
                val urlConn = url.openConnection()

                //get inputstream from connection
                `is` = urlConn.getInputStream()
                fos = FileOutputStream(outputPath)

                // 4KB buffer
                val buffer = ByteArray(4096)
                var length = `is`.read(buffer)

                // read from source and write into local file
                while (length > 0) {
                    fos.write(buffer, 0, length)
                    length = `is`.read(buffer)
                }
                return outputPath
            } catch (e: Exception){
                return null
            }
            finally {
                try {
                    if (`is` != null) {
                        `is`.close()
                    }
                } finally {
                    fos?.close()
                }
            }
        }
    }
}