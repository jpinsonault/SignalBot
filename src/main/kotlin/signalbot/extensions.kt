package signalbot

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil.close
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.net.URL


fun String.runCommand(workingDir: File, log: Boolean=false): String {
    println("Running command: ${this}")
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        throw(e)
    }
}