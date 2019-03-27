package signalbot

import signalbot.botmodules.*

fun main(args: Array<String>){
    val signalClient = Client()

    signalClient.onReady { client ->
        TimerBot.init(client)
        MediaPrieviewBot.init(client)
        println("SignalBot started")
    }

    signalClient.onMessage { client, message ->
        KarmaBot.checkMessage(client, message)
        FactoidBot.checkMessage(client, message)
        MediaPrieviewBot.checkMessage(client, message)
        GroupUpdaterBot.checkMessage(client, message)
        DiceBot.checkMessage(client, message)
        TimerBot.checkMessage(client, message)
        StupidBot.checkMessage(client, message)
        StatusBot.checkMessage(client, message)

        println("message: ${message.content}")
        println("group id: ${message.groupInfo?.id}")
    }

    signalClient.onAttachmentInsertion { client, content ->
        UrlUtils.loadAttachment(content, client.tempDir)
    }

    signalClient.start()
}