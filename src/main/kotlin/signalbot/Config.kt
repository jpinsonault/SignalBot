package signalbot

import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.nio.file.Paths

class Config(val botPhone: String){
    companion object {
        fun loadFile(configLocation: String): Config{
            try {
                val text = File(configLocation).readText()

                return Config(JSONObject(text).getString("bot_phone_number"))
            }
            catch (e: Exception){
                throw Exception("Problem opening signalbot config file: $configLocation")
            }
        }
    }
}