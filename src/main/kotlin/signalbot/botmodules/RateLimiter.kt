package signalbot.botmodules

import java.time.LocalDateTime

class RateLimiter(val period: Long){
    var lastStartTime: Long = 0

    fun tryOrSkip(block: ()->Unit){
        val now = System.currentTimeMillis()

        if(now - lastStartTime > period){
            lastStartTime = now
            block()
        }
    }
}