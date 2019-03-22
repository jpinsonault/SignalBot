package signalbot

fun main(args: Array<String>){
    val signalClient = Client("")

    signalClient.onMessage { client, message ->
        PlusPlus.checkMessage(client, message)
        Factoids.checkMessage(client, message)
        MediaPrieviewBot.checkMessage(client, message)

        println("message: ${message.content}")
        println("group id: ${message.groupInfo?.id}")
    }

    signalClient.start()
}