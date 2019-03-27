package signalbot.botmodules

import org.dizitart.no2.Document
import org.dizitart.no2.filters.Filters.lte
import signalbot.Client
import signalbot.Message
import signalbot.ifFound

class TimerData(val endTime: Long, val reminder: String, val replyTo: String, val group: Boolean)
{
    companion object {
        fun toDocument(timerData: TimerData): Document {
            val doc = Document().apply {
                put("endTime", timerData.endTime)
                put("reminder", timerData.reminder)
                put("replyTo", timerData.replyTo)
                put("group", timerData.group)
            }
            return doc
        }

        fun fromDocument(document: Document): TimerData {
            return TimerData(
                endTime = document.get("endTime") as Long,
                reminder = document.get("reminder", String::class.java),
                replyTo = document.get("replyTo", String::class.java),
                group = document.get("group") as Boolean
            )
        }
    }
}

class TimerBot{
    companion object {
        val prefix = "TimerBot"

        fun init(client: Client){
            client.onPeriodicTimer(1000){ client ->
                println("checking timers")
                checkTimers(client)
            }
        }

        fun checkMessage(client: Client, message: Message){
            val regex = Regex("""^!remind ?me (\d+) (.*)""")
            regex.ifFound(message.content){
                val timerLengthSeconds = it.groupValues[1].toLong()
                val timerMessage = it.groupValues[2]

                registerTimer(client, message, timerLengthSeconds, timerMessage)
                client.replyTo(message, "Ok, I'll remind you in ${timerLengthSeconds}s")
            }
        }

        fun registerTimer(client: Client, message: Message, timerLengthSeconds: Long, reminder: String){
            val endTime = System.currentTimeMillis() + (timerLengthSeconds*1000)
            if(message.groupInfo != null){
                val timerData = TimerData(endTime, reminder, message.groupInfo.id, group = true)
                val doc = TimerData.toDocument(timerData)
                client.saveDocument(prefix, doc)
            }
            else{
                val timerData = TimerData(endTime, reminder, message.author, group = false)
                val doc = TimerData.toDocument(timerData)
                client.saveDocument(prefix, doc)
            }
        }

        fun checkTimers(client: Client){
            val now = System.currentTimeMillis()

            val documents = client.loadDocuments(prefix, lte("endTime", now))
            val expiredTimers = documents.map { TimerData.fromDocument(it) }

            expiredTimers.forEach { timerData ->
                val content = "Reminder: ${timerData.reminder}"
                client.send(content, timerData.replyTo, timerData.group)
            }

            documents.forEach { client.removeDocument(prefix, it) }
        }
    }
}