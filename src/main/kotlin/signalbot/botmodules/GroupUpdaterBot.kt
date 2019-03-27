package signalbot.botmodules

import signalbot.Client
import signalbot.Message
import signalbot.ifFound

class GroupUpdaterBot{
    companion object {
        fun checkMessage(client: Client, message: Message){
            if(message.groupInfo == null){
                return
            }

            val groupAvatar = Regex("""^!update group avatar ([^\s]+)""")
            val groupName = Regex("""^!update group name (.+)""")

            groupAvatar.ifFound(message.content) { matches ->
                val photoUrl = matches.groupValues[1]
                client.updateGroupPhoto(photoUrl, message.groupInfo.id)
            }

            groupName.ifFound(message.content) { matches ->
                val name = matches.groupValues[1]
                client.updateGroupName(name, message.groupInfo.id)
            }
        }
    }
}