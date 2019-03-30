package signalbot

import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.nio.file.Paths

class Config(val botPhone: String, val signalCliCommand: String){
    companion object {
        fun fromFile(configLocation: String): Config{
            try {
                val text = File(configLocation).readText()

                return Config(
                    botPhone = JSONObject(text).getString("bot_phone_number"),
                    signalCliCommand = JSONObject(text).getString("signal_cli_command"))
            }
            catch (e: Exception){
                throw Exception("Problem opening signalbot config file: $configLocation")
            }
        }
    }
}