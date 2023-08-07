package com.example.clicker.network.websockets

import android.util.Log
import com.example.clicker.data.TokenDataStore
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

data class TwitchUserData(
    val badgeInfo: String?,
    val badges: String?,
    val clientNonce: String?,
    val color: String?,
    val displayName: String?,
    val emotes: String?,
    val firstMsg: String?,
    val flags: String?,
    val id: String?,
    val mod: String?,
    val returningChatter: String?,
    val roomId: String?,
    val subscriber: Boolean,
    val tmiSentTs: Long?,
    val turbo: Boolean,
    val userId: String?,
    var userType: String?
)

class TwitchWebSocket @Inject constructor(
    private val tokenDataStore: TokenDataStore
): WebSocketListener() {

    private val webSocketScope = CoroutineScope(Dispatchers.Default + CoroutineName("webSocketScope"))

    private val initialValue =TwitchUserData(
        badgeInfo = "subscriber/77",
        badges = "subscriber/36,sub-gifter/50",
        clientNonce = "d7a543c7dc514886b439d55826eeeb5b",
        color = "FF0000",
        displayName = "marc_malabanan",
        emotes = "",
        firstMsg = "0",
        flags = "",
        id = "fd594314-969b-4f5e-a83f-5e2f74261e6c",
        mod = "0",
        returningChatter = "0",
        roomId = "19070311",
        subscriber = true,
        tmiSentTs = 1690747946900L,
        turbo = false,
        userId = "144252234",
        userType = "Connecting to chat"
    )


    private val webSocketURL = "wss://irc-ws.chat.twitch.tv:443"
    var streamerChannelName = ""


    private val _state = MutableStateFlow(initialValue)
    val state = _state.asStateFlow()

    private var client: OkHttpClient = OkHttpClient.Builder().build()
    var webSocket:WebSocket? = null





     fun run(channelName:String?) {
        if(channelName !=null){
            streamerChannelName = channelName
            if(webSocket != null){
                close()
                newWebSocket()
            }else{
                newWebSocket()
            }
        }else{

        }

    }
     fun close(){
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher.executorService.shutdown()
        webSocket?.close(1009,"Manually closed ")
         webSocket = null


    }
    private fun newWebSocket(){
        val request: Request = Request.Builder()
            .url(webSocketURL)
            .build()
        client = OkHttpClient.Builder()
            .readTimeout(1000,TimeUnit.MILLISECONDS)
            .writeTimeout(1000,TimeUnit.MILLISECONDS)
            .build()

        webSocket =client.newWebSocket(request, this)
    }


    override fun onOpen(webSocket: WebSocket, response: Response){
        super.onOpen(webSocket, response)

        //todo: I think I am going to create a custom scope tied to the lifecycle of this websocket
        openChat(webSocket)




    }

    fun openChat(webSocket: WebSocket) = GlobalScope.launch{
        webSocket.send("CAP REQ :twitch.tv/tags twitch.tv/commands");
        tokenDataStore.getOAuthToken().collect{oAuthToken ->
            Log.d("OAuthtokenStoof",oAuthToken)
            webSocket.send("PASS oauth:$oAuthToken");
            webSocket.send("NICK theplebdev");
            webSocket.send("JOIN #$streamerChannelName");
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("websocketStoof","onMessage()byte: ${bytes.hex()}")
    }

     override fun onMessage(webSocket: WebSocket, text: String) {


         Log.d("websocketStoofs","onMessage(): $text")
         val anotherTesting = parseStringBaby(text)
         val mappedString = mapToTwitchUserData(anotherTesting, channelName = streamerChannelName)
         _state.tryEmit(mappedString)


    }


    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        println("CLOSE: $code $reason")
        Log.d("websocketStoof","onClosing: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        //t.printStackTrace()
        Log.d("websocketStoof","onFailure: ${t.printStackTrace()}")
        Log.d("websocketStoof","onFailure: ${t.message.toString()}")
        Log.d("websocketStoof","onFailure: ${webSocket.toString()}")
    }


    fun sendMessage(chatMessage:String):Boolean{
       val sendingText = webSocket?.send("PRIVMSG #$streamerChannelName :$chatMessage")
       // 'PRIVMSG #$channelName :$message'
        return sendingText ?: false
    }


}/*********END OF THE TwitchWebSocket CLASS*******/


fun parseStringBaby(input: String): Map<String, String> {
    val pattern = "([^;@]+)=([^;]+)".toRegex()
    val matchResults = pattern.findAll(input)

    val parsedData = mutableMapOf<String, String>()


    for (matchResult in matchResults) {
        val (key, value) = matchResult.destructured
        parsedData[key] = value
    }

    return parsedData
}

fun mapToTwitchUserData(parsedData: Map<String, String>,channelName: String): TwitchUserData {
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
        userType = filterText(parsedData["user-type"].toString(),channelName),
        userId = parsedData["user-id"],
    )
}


fun filterText(chatText:String,streamerName:String):String{

    val regex = ":(.*?):(.*)".toRegex()
    val matchResult = regex.find(chatText)
    //Log.d("websocketStoofs","onMessageLoggers-> $streamerName")

    return matchResult?.groupValues?.getOrNull(2)?.trim() ?: ""
}


