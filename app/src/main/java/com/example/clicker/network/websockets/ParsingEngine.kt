package com.example.clicker.network.websockets

import android.util.Log
import com.example.clicker.network.websockets.models.LoggedInUserData
import com.example.clicker.network.websockets.models.TwitchUserData
import com.example.clicker.util.objectMothers.TwitchUserDataObjectMother
import java.util.regex.Pattern


class ParsingEngine {
//THIS IS TO CLEAR EVERYTHING @room-id=520593641;tmi-sent-ts=1696019043159 :tmi.twitch.tv CLEARCHAT #theplebdev
    //THIS IS TO BAN USER @room-id=520593641;target-user-id=949335660;tmi-sent-ts=1696019132494 :tmi.twitch.tv CLEARCHAT #theplebdev :meanermeeny
    //todo: SO I THINK WE NEED TO PARSE OUT THE target-user-id=949335660
    //TODO: IF THAT IS FOUND WE NOW KNOW THAT THIS IS A BAN AND NOT A CLEAR ALL MESSAGES REQUEST
    public fun clearChat(text:String,streamerChannelName:String):TwitchUserData{
    val banDurationPattern = "ban-duration=(\\d+)".toRegex()

    val banDurationMatch = banDurationPattern.find(text)
    val foundDuration = banDurationMatch?.groupValues?.last()?.toInt()

    val userData: TwitchUserData



    val pattern2 = "#$streamerChannelName$".toRegex()
    val matcher2 = pattern2.find(text)
    val found = matcher2?.value
    if(found !=null){
         userData = TwitchUserData(
            badgeInfo = null,
            badges = null,
            clientNonce = null,
            color = "#000000",
            displayName = null,
            emotes = null,
            firstMsg = null,
            flags = null,
            id = null,
            mod = null,
            returningChatter = null,
            roomId = null,
            subscriber = false,
            tmiSentTs = null,
            turbo = false,
            userId = null,
            userType = "Connected to chat!",
            messageType = MessageType.CLEARCHAT,
            bannedDuration = foundDuration
        )

    }else{
        val pattern3 = ":(\\w+)\\s*$".toRegex()

// Use a Matcher to find the pattern in the input string
        val matcher3 = pattern3.find(text)
        val username = matcher3?.groupValues?.last()
         userData = TwitchUserData(
            badgeInfo = null,
            badges = null,
            clientNonce = null,
            color = "#000000",
            displayName = username,
            emotes = null,
            firstMsg = null,
            flags = null,
            id = null,
            mod = null,
            returningChatter = null,
            roomId = null,
            subscriber = false,
            tmiSentTs = null,
            turbo = false,
            userId = null,
            userType = "Connected to chat!",
            messageType = MessageType.CLEARCHAT,
            bannedDuration = foundDuration
        )

    }
    return userData

    }

    fun clearChatTesting(text:String,streamerName:String):TwitchUserData{

        //THIS IS TO CLEAR EVERYTHING @room-id=520593641;tmi-sent-ts=1696019043159 :tmi.twitch.tv CLEARCHAT #theplebdev
    //THIS IS TO BAN USER @room-id=520593641;target-user-id=949335660;tmi-sent-ts=1696019132494 :tmi.twitch.tv CLEARCHAT #theplebdev :meanermeeny
        val streamerNamePattern = "$streamerName$".toRegex()


        val clearChat = streamerNamePattern.find(text)
        val channelNameFound = clearChat?.value

        if(channelNameFound == null){
            return banUserParsing(text)

        }else{
            //todo: this should return a TwitchUserData object
            //todo: 1) Basically I want this to send a message to clear the chat
            //todo: 2) Basically I want this to display a message stating that the chat was cleared
            return clearChatParsing(channelNameFound)
        }


    }
    private fun banUserParsing(text: String):TwitchUserData{
        //THIS IS TO BAN USER @room-id=520593641;target-user-id=949335660;tmi-sent-ts=1696019132494 :tmi.twitch.tv CLEARCHAT #theplebdev :meanermeeny
        //todo: I want this to modify the messages like it currently is but also sent a little message stating what user was banned
        //todo: I need to parse out the userId. target-user-id=949335660
        val banUserPattern = "([^:]+)$".toRegex()
        val bannedUserIdPattern = "target-user-id=(\\d+)".toRegex()

        val bannedUserUsername = banUserPattern.find(text)
        val bannedUserId = bannedUserIdPattern.find(text)


        val usernameFound = bannedUserUsername?.value ?: "User"
        val bannedUserIdFound = bannedUserId?.groupValues?.get(1)

        return TwitchUserData(
            badgeInfo = null,
            badges = null,
            clientNonce = null,
            color = "#000000",
            displayName = usernameFound,
            emotes = null,
            firstMsg = null,
            flags = null,
            id = bannedUserIdFound,
            mod = null,
            returningChatter = null,
            roomId = null,
            subscriber = false,
            tmiSentTs = null,
            turbo = false,
            userId = null,
            userType = "$usernameFound banned by moderator",
            messageType = MessageType.CLEARCHAT,
            bannedDuration = null
        )

    }
    private fun clearChatParsing(channelName:String):TwitchUserData{
        return TwitchUserData(
            badgeInfo = null,
            badges = null,
            clientNonce = null,
            color = "#000000",
            displayName = null,
            emotes = null,
            firstMsg = null,
            flags = null,
            id = null,
            mod = null,
            returningChatter = null,
            roomId = null,
            subscriber = false,
            tmiSentTs = null,
            turbo = false,
            userId = null,
            userType = "Chat cleared by moderator",
            messageType = MessageType.CLEARCHAT,
            bannedDuration = null
        )


    }


    /**
     * Parses the websocket data sent from twitch. should run when a USERSTATE command is sent
     * @return [LoggedInUserData] Which represents the state of the current logged in user
     */
    fun userStateParsing(text:String): LoggedInUserData {
        val colorPattern = "color=([^;]+)".toRegex()
        val displayNamePattern = "display-name=([^;]+)".toRegex()
        val modStatusPattern = "mod=([^;]+)".toRegex()
        val subStatusPattern = "subscriber=([^;]+)".toRegex()

        val colorMatch = colorPattern.find(text)
        val displayNameMatch = displayNamePattern.find(text)
        val modStatusMatch = modStatusPattern.find(text)
        val subStatusMatch = subStatusPattern.find(text)


        val loggedData =LoggedInUserData(
            color =colorMatch?.groupValues?.get(1),
            displayName = displayNameMatch?.groupValues?.get(1)!!,
            mod = modStatusMatch?.groupValues?.get(1)!! == "1",
            sub = subStatusMatch?.groupValues?.get(1)!! == "1"

        )


        return loggedData


    }
    /**
     * Parses the websocket data sent from twitch. Will run when a CLEARMSG command is sent
     * @property text the string to be parsed
     * @return a nullable string containing the id of the message to be deleted
     */
    fun clearMsgParsing(text:String):String?{
        val pattern = "target-msg-id=([^;]+)".toRegex()

        val messageId = pattern.find(text)?.groupValues?.get(1)
        return messageId

    }

    /**
     * Creates a [TwitchUserData] that will be sent when a JOIN command is sent
     *
     * @return a [TwitchUserData] used to notify the user that they have connected to a streamer's chat room
     */
    fun createJoinObject():TwitchUserData{

        return TwitchUserDataObjectMother.addColor("#000000")
            .addDisplayName("Room update")
            .addUserType("Connected to chat!")
            .addMessageType(MessageType.JOIN)
            .build()

    }

    fun noticeParsing(text:String,streamerChannelName: String):TwitchUserData{
        val pattern = "#$streamerChannelName\\s*:(.+)".toRegex()
        val matchResult = pattern.find(text)
        val extractedInfo = matchResult?.groupValues?.get(1)?.trim() ?: "Room information updated"

        return TwitchUserDataObjectMother
            .addColor("#000000")
            .addDisplayName("Room update")
            .addUserType(extractedInfo)
            .addMessageType(MessageType.NOTICE)
            .build()
    }

    /**
     * Creates a [TwitchUserData] that will be sent when a USERNOTICE command is sent
     *
     * @return a [TwitchUserData] used to notify the chat that an certain event has occurred. Example events are,
     * announcement, resub, sub, submysterygift, subgift
     */
    fun userNoticeParsing(text: String,streamerChannelName: String): TwitchUserData {

        val displayNamePattern = "display-name=([^;]+)".toRegex()
        val messageIdPattern ="msg-id=([^;]+)".toRegex()
        val systemMessagePattern ="system-msg=([^;]+)".toRegex()
        val personalMessagePattern = "#$streamerChannelName :([^;]+)".toRegex()

        val displayNameMatch = displayNamePattern.find(text)
        val messageIdMatch = messageIdPattern.find(text)
        val systemMessageMatch = systemMessagePattern.find(text)
        val personalMessageMatch = personalMessagePattern.find(text)



        val displayName =displayNameMatch?.groupValues?.get(1) ?: "username"

        val messageType:MessageType = when(messageIdMatch?.groupValues?.get(1) ?: "announcement"){
            "announcement" ->{MessageType.ANNOUNCEMENT}
            "resub" ->MessageType.RESUB
            "sub" -> MessageType.SUB
            "submysterygift" ->MessageType.MYSTERYGIFTSUB
            "subgift" ->MessageType.GIFTSUB
            else ->MessageType.ANNOUNCEMENT

        }
        val systemMessage = systemMessageMatch?.groupValues?.get(1)?.replace("\\s"," ") ?: "Announcement!"
        val personalMessage = personalMessageMatch?.groupValues?.get(1)

        return TwitchUserDataObjectMother
            .addDisplayName(displayName)
            .addMessageType(messageType)
            .addUserType(personalMessage)
            .addSystemMessage(systemMessage)
            .build()

    }

    /**
     * Parses the websocket data sent from twitch. Should run when a PRIVMSG command is sent
     * @property text the string to be parsed
     * @return a [TwitchUserData] representing all the meta data from an individual chatter
     */
    fun privateMessageParsing(text:String):TwitchUserData{
        val pattern = "([^;@]+)=([^;]+)".toRegex()
        val privateMsgPattern = "([^:]+)$".toRegex()

        val matchResults = pattern.findAll(text)
        val privateMsgResult = privateMsgPattern.find(text)

        val parsedData = mutableMapOf<String, String>()
        val privateMsg = privateMsgResult?.groupValues?.get(1) ?: ""


        for (matchResult in matchResults) {
            val (key, value) = matchResult.destructured
            parsedData[key] = value
        }

        return TwitchUserData(
            badgeInfo = parsedData["badge-info"],
            badges = parsedData["badges"],
            clientNonce = parsedData["client-nonce"],
            color = parsedData["color"] ?: "#000000",
            displayName = parsedData["display-name"],
            emotes = parsedData["emotes"],
            firstMsg = parsedData["first-msg"],
            flags = parsedData["flags"],
            id = parsedData["id"],
            mod = parsedData["mod"],
            returningChatter = parsedData["returning-chatter"],
            subscriber = parsedData["subscriber"]?.toIntOrNull() == 1,
            roomId = parsedData["room-id"],
            tmiSentTs = parsedData["tmi-sent"]?.toLongOrNull(),
            turbo = parsedData["turbo"]?.toIntOrNull() == 1,
            userType = privateMsg,
            userId = parsedData["user-id"],
            messageType = MessageType.USER
        )

    }
    /**
     * Parses the information relate to the chat rooms state. Should be run when ROOMSTATE is sent
     * @property text the string to be parsed
     * @return a [RoomState] representing the current state of the chat room
     */
    fun roomStateParsing(text: String):RoomState{
        val slowMode= getValueFromInput(text,"slow")

        val emoteMode = getValueFromInput(text,"emote-only")
        val followersMode = getValueFromInput(text,"followers-only")


        val subMode = getValueFromInput(text,"subs-only")
        return RoomState(
            emoteMode=emoteMode,
            followerMode = followersMode,
            slowMode = slowMode,
            subMode = subMode
        )

    }
    private fun getValueFromInput(input: String, key: String): Boolean? {
        val pattern = "$key=([^;:\\s]+)".toRegex()
        val match = pattern.find(input)
        val returnedValue = match?.groupValues?.get(1) ?: return null
        if( returnedValue == "-1"){
            return false
        }
        if(key == "followers-only" && returnedValue == "0"){
            return true
        }
        else{
            return returnedValue != "0"
        }

    }


}
