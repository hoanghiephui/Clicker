package com.example.clicker.presentation.stream

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clicker.domain.TwitchDataStore
import com.example.clicker.network.BanUser
import com.example.clicker.network.BanUserData
import com.example.clicker.network.domain.TwitchStream
import com.example.clicker.network.models.twitchStream.ChatSettingsData
import com.example.clicker.network.models.twitchStream.UpdateChatSettings
import com.example.clicker.network.websockets.MessageType
import com.example.clicker.network.websockets.domain.TwitchSocket
import com.example.clicker.network.websockets.models.LoggedInUserData
import com.example.clicker.network.websockets.models.TwitchUserData
import com.example.clicker.presentation.stream.views.ChatSettingsContainer
import com.example.clicker.util.Response
import com.example.clicker.util.objectMothers.TwitchUserDataObjectMother
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChattingUser(
    val username: String,
    val message: String
)

/**
 * ChatSettings holds all the data representing the current mod related chat settings
 * */
data class ModChatSettings(
    val showChatSettingAlert: Boolean = false,
    val showUndoButton:Boolean = false,
    val data: ChatSettingsData = ChatSettingsData(
        slowMode = false,slowModeWaitTime = null,
        followerMode = false, followerModeDuration =null ,
        subscriberMode = false,emoteMode=false
    ),

    val switchesEnabled:Boolean = true
)
/**
 * AdvancedChatSettings holds all the data representing the current advanced settings relating to the chat messages
 * */
data class AdvancedChatSettings(
    val noChatMode:Boolean = false,

    val showSubs:Boolean = true,
    val showReSubs:Boolean = true,
    val showAnonSubs:Boolean = true,
    val showGiftSubs:Boolean = true,
)
data class StreamUIState(
    val chatSettings: Response<ChatSettingsData> = Response.Loading, //websocket twitchImpl
    val loggedInUserData: LoggedInUserData? = null, //websocket

    val clientId: String = "", //twitchRepoImpl
    val broadcasterId: String = "", //twitchRepoImpl
    val userId: String = "", //twitchRepoImpl
    val oAuthToken: String = "", //twitchRepoImpl


    val oneClickActionsChecked:Boolean = true,
    val noChatMode:Boolean = false,


    val banDuration: Int = 0, //twitchRepoImpl
    val banReason: String = "", //twitchRepoImpl
    val timeoutDuration: Int = 10, //twitchRepoImpl
    val timeoutReason: String = "", //twitchRepoImpl
    val banResponse: Response<Boolean> = Response.Success(false), //twitchRepoImpl
    val banResponseMessage: String = "", //twitchRepoImpl
    val undoBanResponse: Boolean = false, //twitchRepoImpl
    val showStickyHeader: Boolean = false, //twitchRepoImpl

    val chatSettingsFailedMessage: String = ""
)
data class ClickedUIState(
    val clickedUsername:String ="", //websocket
    val clickedUserId: String ="",
    val clickedUsernameBanned: Boolean=false,
    val clickedUsernameIsMod:Boolean =false
)
data class ForwardSlashCommands(
    val title:String,
    val subtitle:String,
    val clickValue:String
)

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val webSocket: TwitchSocket,
    private val tokenDataStore: TwitchDataStore,
    private val twitchRepoImpl: TwitchStream,
    private val ioDispatcher: CoroutineDispatcher,
    private val autoCompleteChat: AutoCompleteChat
) : ViewModel() {

    /**
     * The name of the channel that this chat is connecting to
     * */
    private val _channelName: MutableStateFlow<String?> = MutableStateFlow(null)
    val channelName: StateFlow<String?> = _channelName

    private val _clientId: MutableState<String?> = mutableStateOf(null)
    val clientId: State<String?> = _clientId

    /**
     * A list representing all the chats users have sent
     * */
    val listChats = mutableStateListOf<TwitchUserData>()


    private var _uiState: MutableState<StreamUIState> = mutableStateOf(StreamUIState())
    val state: State<StreamUIState> = _uiState

    private val _clickedUIState = mutableStateOf(ClickedUIState())
    val clickedUIState = _clickedUIState

    //all the related chat settings code
    private val _modChatSettingsState = mutableStateOf(ModChatSettings())
    val modChatSettingsState = _modChatSettingsState

    /**
     * The UI state that represents all the data meant for the [ChatSettingsContainer.EnhancedChatSettingsBox] composable
     * */
    private val _advancedChatSettingsState = mutableStateOf(AdvancedChatSettings())
    val advancedChatSettingsState = _advancedChatSettingsState

    /**
     * determine if the user is user is trying to message another user
     * */
    private val _parsing = mutableStateOf(false)
    val parsing = _parsing



    private var currentUsername: String = ""

    /**
     * represents what the user is typing in the text field
     * */
    val textFieldValue = mutableStateOf(
        TextFieldValue(
            text = "",
            selection = TextRange(0)
        )
    )
    val openTimeoutDialog = mutableStateOf(false)
    val openBanDialog = mutableStateOf(false)


    /**
     * A list of Strings that represents the list of users that are being searched when the user enters the ***@***
     * into the text box
     * */
    var filteredChatList = mutableStateListOf<String>()

     val forwardSlashCommands = mutableStateListOf<ForwardSlashCommands>()

    private var _deviceIsHorizontal = mutableStateOf(false)
    val deviceIsHorizontal:State<Boolean> = _deviceIsHorizontal

    fun setOrientation(isHorizontal:Boolean){
        _deviceIsHorizontal.value = isHorizontal
    }






    val clickedUsernameChats = mutableStateListOf<String>()

    private val allChatters = mutableStateListOf<String>()




    /**
     * updateAdvancedChatSettings is used to update the [_advancedChatSettingsState] UI state
     *
     * @param advancedChatSettings the new state that will now represent the [_advancedChatSettingsState] UI state
     */
    fun updateAdvancedChatSettings(advancedChatSettings: AdvancedChatSettings){
        _advancedChatSettingsState.value =advancedChatSettings
    }


    init{
//        getAutoModStatus()
    }



    /**THis is the data for the new filter methods*/

    init {
        viewModelScope.launch {
            withContext(ioDispatcher + CoroutineName("MessageToDeleteId")) {
                webSocket.messageToDeleteId.collect { nullableMsgId ->
                    nullableMsgId?.let { nonNullMsgId ->
                        filterMessages(nonNullMsgId)
                    }
                }
            }
        }
    }


    private val _idOfLatestBan = mutableStateOf("")
    val idOfLatestBan = _idOfLatestBan
    /**
     * This is meant to monitor of the latest ban/timeout messages
     *
     * */
    init{
        viewModelScope.launch {
            webSocket.latestBannedUserId.collect{latestBannedId ->
                latestBannedId?.also{
                    Log.d("latestBannedId", "latestBannedId --> ${latestBannedId}")
                    _idOfLatestBan.value = latestBannedId
                }

            }
        }

    }

    /**
     * showUndoButton() is function used by [SettingsSwitches][com.example.clicker.presentation.stream.views.ChatSettingsContainer.SettingsSwitches]
     * composable to hide or show the [DraggableUndoButton][com.example.clicker.presentation.stream.views.MainChat.MainChatParts.DraggableUndoButton]
     *
     * @param status a boolean representing the current state of the switch being clicked
     * */
    fun showUndoButton(status:Boolean){

        _modChatSettingsState.value = _modChatSettingsState.value.copy(
            showUndoButton = status
        )
    }
    fun setNoChatMode(status: Boolean){
        _advancedChatSettingsState.value = _advancedChatSettingsState.value.copy(
            noChatMode = status
        )
        if(status){
            webSocket.close()
            listChats.clear()

        }else{
            startWebSocket(channelName.value ?:"")
        }
        viewModelScope.launch {
            delay(200)
            listChats.clear()
        }


    }
    fun closeStickyHeader() {
        _uiState.value = _uiState.value.copy(
            showStickyHeader = false
        )
    }

    fun changeTimeoutDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(
            timeoutDuration = duration
        )
    }
    fun changeTimeoutReason(reason: String) {
        _uiState.value = _uiState.value.copy(
            timeoutReason = reason
        )
    }
    fun changeBanDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(
            banDuration = duration
        )
    }
    fun changeBanReason(reason: String) {
        _uiState.value = _uiState.value.copy(
            banReason = reason
        )
    }

    // TODO: NOTES FOR WHEN I COME BACK
    // this should be hooked up to a hot flow and run eachtime a new messageId is sent to it
    //todo:chat method
    fun filterMessages(messageId: String) {
        try{
            val found = listChats.first { it.id == messageId }
            val foundIndex = listChats.indexOf(found)
            listChats[foundIndex] = found.copy(
                deleted = true
            )
        }catch (e:Exception){
            Log.d("FilterMessageCrash","messageId-----> $messageId")
            Log.d("FilterMessageCrash","messageId-----> ${e.message}")
        }

    }


    //TWITCH METHOD
    fun deleteChatMessage(messageId: String) = viewModelScope.launch {
        withContext(ioDispatcher + CoroutineName("DeleteChatMessage")) {
            twitchRepoImpl.deleteChatMessage(
                oAuthToken = _uiState.value.oAuthToken,
                clientId = _uiState.value.clientId,
                broadcasterId = _uiState.value.broadcasterId,
                moderatorId = _uiState.value.userId,
                messageId = messageId
            ).collect { response ->

                when (response) {
                    is Response.Loading -> {
                        Log.d("deleteChatMessage", "LOADING")
                    }
                    is Response.Success -> {
                        Log.d("deleteChatMessage", "SUCCESS")
                    }
                    is Response.Failure -> {
                        Log.d("deleteChatMessage", "FAILURE")
                        _uiState.value = _uiState.value.copy(
                            showStickyHeader = true,
                            undoBanResponse = false,
                            banResponseMessage = "${response.e.message}"
                        )
                    }
                }
            }
        }
    }

    //CHAT METHOD
    fun addChatter(username: String, message: String) {
        if (!allChatters.contains(username)) {
            allChatters.add(username)
        }
    }
    //CHAT METHOD
    fun updateClickedChat(
        clickedUsername: String,
        clickedUserId: String,
        banned: Boolean,
        isMod: Boolean
    ) {

        clickedUsernameChats.clear()
        val messages = listChats.filter { it.displayName == clickedUsername }.map { if (it.deleted) it.userType!! + " (deleted by mod)" else it.userType!! }

        clickedUsernameChats.addAll(messages)
        _clickedUIState.value = _clickedUIState.value.copy(
            clickedUsername = clickedUsername,
            clickedUserId = clickedUserId,
            clickedUsernameBanned = banned,
            clickedUsernameIsMod = isMod
        )

    }


    var parsingIndex:Int =0
    var startParsing:Boolean = false

    var slashCommandState:Boolean = false
    var slashCommandIndex:Int =0



    fun newParsingAgain(textFieldValue: TextFieldValue){
        try{
            val selectedText = textFieldValue.getSelectedText() //this is only triggered if the user selects and highlights text
            val afterSelection = textFieldValue.getTextAfterSelection(1)
            val currentCharacter = textFieldValue.getTextBeforeSelection(1)  // this is the current text
            val annotatedString = textFieldValue.annotatedString


            if(currentCharacter.toString()==""){
                endParsingNClearFilteredChatList()
            }


            if(textFieldValue.selection.start < parsingIndex && startParsing){
                endParsingNClearFilteredChatList()

            }

            if (currentCharacter.toString() == " " && startParsing){
                endParsingNClearFilteredChatList()

            }
            /**---------set parsing to false should be above this line----------------*/
            if(startParsing){
                parseNFilterChatList(textFieldValue)
            }

            if(currentCharacter.toString() == "@"){
                showFilteredChatListNStartParsing(textFieldValue)
            }


        }catch (e:Exception){
            endParsingNClearFilteredChatList()
            negateSlashCommandStateNClearForwardSlashCommands()
        }


    }



    /**
     * showFilteredChatListNStartParsing is a private function called when the current character the user is
     * typing is equal to ***@***. It sets [parsingIndex] to the current character index,[startParsing] to true
     * and adds all the current usernames in chat to [filteredChatList]
     *
     * @param textFieldValue a [TextFieldValue] that represents what the user is currently typing
     * */
    private fun showFilteredChatListNStartParsing(textFieldValue: TextFieldValue){
        Log.d("newParsingAgain","-----------BEGIN PARSING----------")
        filteredChatList.clear()
        filteredChatList.addAll(allChatters.toList())
        parsingIndex =textFieldValue.selection.start
        startParsing = true
    }
    /**
     * parseNFilterChatList is a private function called when [startParsing] is set to true. Its main
     * goal is to parse out the ***username*** from the [textFieldValue]. Then take that ***username***
     * and filter everything out of [filteredChatList] that does not match the ***username***
     *
     * @param textFieldValue a [TextFieldValue] that represents what the user is currently typing
     * */
    private fun parseNFilterChatList(textFieldValue: TextFieldValue){
        val username =textFieldValue.text.subSequence(parsingIndex,textFieldValue.selection.end)

        val usernameRegex = Regex("^$username",RegexOption.IGNORE_CASE)
        filteredChatList.removeIf{
            !it.contains(usernameRegex)
        }



    }
    /**
     * endParsingNClearFilteredChatList is a private function meant to call ***.clear()*** on [filteredChatList] and
     * set [startParsing] to false
     * */
    private fun endParsingNClearFilteredChatList(){
        filteredChatList.clear()
        startParsing = false
    }
    /**
     * negateSlashCommandStateNClearForwardSlashCommands is a private function meant to call ***.clear()*** on [forwardSlashCommands] and
     * set [slashCommandState] to false
     * */
    private fun negateSlashCommandStateNClearForwardSlashCommands(){
        slashCommandState = false
        forwardSlashCommands.clear()
    }
    /**
     * autoTextChange is function that is used to change the value of [textFieldValue] with [username]
     *
     * @param username  a string meant to represent the username that was clicked on by the user
     * */
    fun autoTextChange(username: String) {
        val currentCharacterIndex = textFieldValue.value.selection.end

        val replacedString =textFieldValue.value.text.replaceRange(parsingIndex,currentCharacterIndex,"$username ")
        textFieldValue.value = textFieldValue.value.copy(
            text = replacedString,
            selection = TextRange(replacedString.length)
        )
        filteredChatList.clear()

    }

    init {
        // todo: NEED TO COPY THIS VALUE OVER TO THE loggedInUserData
        viewModelScope.launch {
            webSocket.loggedInUserUiState.collect {
                it?.let {
                    _uiState.value = _uiState.value.copy(
                        loggedInUserData = it
                    )

                }
            }
        }
    }

    init {
        Log.d("twitchNameonCreateViewVIewModel", "CREATED")
    }

    /**
     * This is the hot state receiving the main chat messages
     * //TODO: SOCKET METHOD
     * */
    init {
        viewModelScope.launch {
            // withContext(Dispatchers.IO + CoroutineName("ChatMessages")){
            monitorSocketForChatMessages()
             //}
        }
    }
    init {
        //TODO: SOCKET METHOD
        viewModelScope.launch {
            withContext(ioDispatcher + CoroutineName("StartingWebSocket")) {
                _channelName.collect { channelName ->
                    channelName?.let {

                            startWebSocket(channelName)


                    }
                }
            }
        }
    }
    init {
        //TODO: SOCKET METHOD
        viewModelScope.launch {
            withContext(ioDispatcher + CoroutineName("RoomState")) {
                monitorSocketRoomState()
            }
        }
    }

    private suspend fun monitorSocketForChatMessages(){
        webSocket.state.collect { twitchUserMessage ->
            Log.d("loggedMessage", "${twitchUserMessage.id}")

            if (twitchUserMessage.displayName == _clickedUIState.value.clickedUsername) {

                clickedUsernameChats.add(twitchUserMessage.userType!!)
            }
            when(twitchUserMessage.messageType){
                MessageType.CLEARCHAT ->{
                    notifyChatOfBanTimeoutEvent(listChats,twitchUserMessage.userType)
                }
                MessageType.CLEARCHATALL->{
                    clearAllChatMessages(listChats)
                }
                MessageType.USER ->{
                    Log.d("CheckingChattersNmae","${twitchUserMessage.displayName!!}")
                    Log.d("CheckingChattersNmae","${twitchUserMessage.userType!!}")
                    autoCompleteChat.addChatter(twitchUserMessage.displayName!!)
                    addChatter(twitchUserMessage.displayName!!,twitchUserMessage.userType!!)
                    listChats.add(twitchUserMessage)
                }
                MessageType.SUB ->{
                    if(_advancedChatSettingsState.value.showSubs){
                        listChats.add(twitchUserMessage)
                    }

                }
                MessageType.RESUB ->{
                    if(_advancedChatSettingsState.value.showReSubs){
                        listChats.add(twitchUserMessage)
                    }
                }
                MessageType.GIFTSUB ->{
                    if(_advancedChatSettingsState.value.showGiftSubs){
                        listChats.add(twitchUserMessage)
                    }
                }
                MessageType.MYSTERYGIFTSUB ->{
                    if(_advancedChatSettingsState.value.showAnonSubs){
                        listChats.add(twitchUserMessage)
                    }
                }
                else -> {
                    listChats.add(twitchUserMessage)
                }
            }


            //todo:CLEAR THIS MESS OUT ABOVE
        }
    }
fun clearAllChatMessages(chatList: SnapshotStateList<TwitchUserData>){
    chatList.clear()
    val data =TwitchUserDataObjectMother
        .addMessageType(MessageType.JOIN)
        .addUserType("Chat cleared by moderator")
        .addColor("#000000")
        .build()

    chatList.add(data)
}
    fun notifyChatOfBanTimeoutEvent(chatList: SnapshotStateList<TwitchUserData>,message: String?){
        val data = TwitchUserDataObjectMother
            .addMessageType(MessageType.JOIN)
            .addUserType(message)
            .addColor("#000000")
            .build()
        chatList.add(data)
    }
    suspend fun monitorSocketRoomState(){
        webSocket.roomState.collect { nullableRoomState ->
            nullableRoomState?.let { nonNullroomState ->
                Log.d("theCurrentRoomState","$nonNullroomState")
                // todo: update the _uiState chatSettings with these values
                _uiState.value = _uiState.value.copy(
                    chatSettings = Response.Success(
                        ChatSettingsData(
                            slowMode = nonNullroomState.slowMode,
                            slowModeWaitTime = nonNullroomState.slowModeDuration,
                            followerMode = nonNullroomState.followerMode,
                            followerModeDuration = nonNullroomState.followerModeDuration,
                            subscriberMode = nonNullroomState.subMode,
                            emoteMode = nonNullroomState.emoteMode,

                        )
                    )

                )

            }
        }

    }

    //TODO: CHAT METHOD
    fun closeChatSettingAlert() {
        _uiState.value = _uiState.value.copy(
            //showChatSettingAlert = false
        )
    }

    //TODO: SOCKET METHOD
    fun restartWebSocket() {
        val channelName = _channelName.value ?: ""
        startWebSocket(channelName)
    }

    //TODO: SOCKET METHOD
    /**
     * startWebSocket() is a private method meant to be called by methods inside of [StreamViewModel]
     * It is used to start and connect a Websocket using the [TwitchSocket]
     * */
    private fun startWebSocket(channelName: String) = viewModelScope.launch {
        if(_advancedChatSettingsState.value.noChatMode){
            //this is meant to be empty to represent doing nothing and the user being in no chat mode
        }else{
            tokenDataStore.getUsername().collect { username ->
                if (username.isNotEmpty()) {
                    currentUsername = username
                    Log.d("startWebSocket", "username --->$username")
                    webSocket.run(channelName, username)
                }
            }
        }

    }

    //TODO: SOCKET METHOD
    fun sendMessage(chatMessage: String) {
        val messageResult = webSocket.sendMessage(chatMessage)
        textFieldValue.value = TextFieldValue(
            text = "",
            selection = TextRange(0)
        )
        listChats.add(
            TwitchUserData(
                badgeInfo = null,
                badges = null,
                clientNonce = null,
                color = "#BF40BF",
                displayName = currentUsername,
                emotes = null,
                firstMsg = null,
                flags = null,
                id = null,
                mod = "mod",
                returningChatter = null,
                roomId = null,
                subscriber = false,
                tmiSentTs = null,
                turbo = false,
                userId = null,
                userType = chatMessage,
                messageType = MessageType.USER
            )
        )
        Log.d("messageResult", messageResult.toString())
    }


    /**
     * updateChannelNameAndClientIdAndUserId is the method that gets called whenever the user clicks on a stream title when
     * they want to navigate to the streamer's page. It updates the ***clientId*** ***broadcasterId*** and ***userId***
     * */
    fun updateChannelNameAndClientIdAndUserId(
        channelName: String,
        clientId: String,
        broadcasterId: String,
        userId: String
    ) {
        _channelName.tryEmit(channelName)

        _uiState.value = _uiState.value.copy(
            clientId = clientId,
            broadcasterId = broadcasterId,
            userId = userId
        )

        getChatSettings(clientId, broadcasterId)
        listChats.clear()

    }
    fun retryGettingChatSetting() {
        getChatSettings(
            clientId = _uiState.value.clientId,
            broadcasterId = _uiState.value.broadcasterId
        )
    }


    /**
     * getChatSettings() is a private function used by [updateChannelNameAndClientIdAndUserId] and [retryGettingChatSetting] to
     * get the chat settings of the current channel the viewer is viewing
     * */
    private fun getChatSettings(
        clientId: String,
        broadcasterId: String
    ) = viewModelScope.launch {

        withContext(Dispatchers.IO + CoroutineName("GetChatSettings")) {
            tokenDataStore.getOAuthToken().collect { oAuthToken ->

                if (oAuthToken.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        oAuthToken = oAuthToken
                    )
                    twitchRepoImpl.getChatSettings("Bearer $oAuthToken", clientId, broadcasterId).collect { response ->
                        when (response) {
                            is Response.Loading -> {
                                _modChatSettingsState.value = _modChatSettingsState.value.copy(
                                    switchesEnabled = false
                                )
                            }
                            is Response.Success -> {

                                val chatSettingsData = response.data.data[0]
                                _modChatSettingsState.value = _modChatSettingsState.value.copy(
                                    data = chatSettingsData,
                                    switchesEnabled = true
                                )
                            }
                            is Response.Failure -> {

                                _modChatSettingsState.value = _modChatSettingsState.value.copy(
                                    switchesEnabled = true,
                                    showChatSettingAlert = true

                                )
                            }
                        }
                    }
                }
            }
        }

    }

    fun closeSettingsAlertHeader(){
        _modChatSettingsState.value = _modChatSettingsState.value.copy(
            showChatSettingAlert = false
        )
    }

    //below is what I am building to create the new generic chat settings switch function
    /**
     * toggleChatSettings() is a generic function that is called when a [Switch][androidx.compose.material.Switch] inside of [ChatSettingsContainer][com.example.clicker.presentation.stream.views.ChatSettingsContainer]
     * is clicked. Once triggered this function will send a request to the Twitch server and attempt to change the current channel's chat settings
     *
     *
     * @param checkedBoolean a boolean that represents the current [Switch's][androidx.compose.material.Switch] state. True for clicked and false for not clicked
     * @param switchType is a Enum of type [ChatSettingsContainer.SwitchTypes] and represents which switch got toggled
     * */
    fun toggleChatSettings(chatSettingsData: ChatSettingsData){
        _modChatSettingsState.value = _modChatSettingsState.value.copy(
            switchesEnabled = false,
            showChatSettingAlert = false
        )
        // then we make the request
        // if the request is a success update the chatSettingsData, set switchesEnabled = true
        // if the request fails, do nothing except, switchesEnabled = true,showChatSettingAlert=true
        updateDateChatSettings(chatSettingsData)

    }


    /**
     * updateDateChatSettings() is a private function called by [toggleChatSettings] and it is making the actual request
     * to the Twitch server to update the channels chat settings
     *
     * @param updatedChatSettings this object represents the updated settings it is sending to the Twitch servers. The object
     * will differ depending on SwitchType passed to the [toggleChatSettings] function
     *
     * */
    private fun updateDateChatSettings(
        chatSettingsData: ChatSettingsData,
    ) = viewModelScope.launch{


        withContext(ioDispatcher + CoroutineName("updateDateChatSettings")) {
            twitchRepoImpl.updateChatSettings(
                oAuthToken = _uiState.value.oAuthToken,
                clientId = _uiState.value.clientId,
                moderatorId = _uiState.value.userId,
                broadcasterId = _uiState.value.broadcasterId,
                body = UpdateChatSettings(
                    emote_mode = chatSettingsData.emoteMode,
                    follower_mode = chatSettingsData.followerMode,
                    slow_mode = chatSettingsData.slowMode,
                    subscriber_mode = chatSettingsData.subscriberMode
                )
            ).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        Log.d("changeChatSettings", "LOADING")
                    }
                    is Response.Success -> {

                        _modChatSettingsState.value = _modChatSettingsState.value.copy(
                            data = chatSettingsData,
                            switchesEnabled = true,
                        )

                    }
                    is Response.Failure -> {
                        Log.d("changeChatSettings", "FAILED -> ${response.e.message}")
                        _modChatSettingsState.value = _modChatSettingsState.value.copy(
                            showChatSettingAlert = true,
                            switchesEnabled = true,
                        )

                    }
                }
            }
        }

    }



    fun timeoutUser() = viewModelScope.launch {
        withContext(ioDispatcher + CoroutineName("TimeoutUser")) {
            val timeoutUser = BanUser(
                data = BanUserData( //TODO: THIS DATA SHOULD BE PASSED INTO THE METHOD
                    user_id = _clickedUIState.value.clickedUserId,
                    reason = _uiState.value.timeoutReason,
                    duration = _uiState.value.timeoutDuration
                )
            )
            twitchRepoImpl.banUser(
                oAuthToken = _uiState.value.oAuthToken,
                clientId = _uiState.value.clientId,
                moderatorId = _uiState.value.userId,
                broadcasterId = _uiState.value.broadcasterId,
                body = timeoutUser
            ).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        Log.d("TIMEOUTUSERRESPONSE", "LOADING")
                    }
                    is Response.Success -> {
                        Log.d("TIMEOUTUSERRESPONSE", "SUCCESS")
                        _uiState.value = _uiState.value.copy(
                            banResponse = Response.Success(true),
                            timeoutReason = "",
                            undoBanResponse = false
                        )
                    }
                    is Response.Failure -> {
                        Log.d("TIMEOUTUSERRESPONSE", "FAILED")
                        _uiState.value = _uiState.value.copy(
                            showStickyHeader = true,
                            undoBanResponse = false,
                            banResponseMessage = "${response.e.message}"
                        )
                    }
                }
            }
        }
    }

fun oneClickBanUser(userId:String) = viewModelScope.launch{

    _clickedUIState.value = _clickedUIState.value.copy(
        clickedUserId =userId
    )

    val banUserNew = BanUser(
        data = BanUserData( //TODO: THIS DATA SHOULD BE PASSED INTO THE METHOD
            user_id = userId,
            reason = "",
            duration = 0
        )
    )

    withContext(ioDispatcher + CoroutineName("BanUser")) {
        twitchRepoImpl.banUser(
            oAuthToken = _uiState.value.oAuthToken,
            clientId = _uiState.value.clientId,
            moderatorId = _uiState.value.userId,
            broadcasterId = _uiState.value.broadcasterId,
            body = banUserNew
        ).collect { response ->
            when (response) {
                is Response.Loading -> {
                    Log.d("BANUSERRESPONSE", "LOADING")
                }
                is Response.Success -> {
                    Log.d("BANUSERRESPONSE", "SUCCESS")
                    _uiState.value = _uiState.value.copy(
                        banResponse = Response.Success(true),
                        banReason = "",
                        undoBanResponse = false
                    )
                }
                is Response.Failure -> {
                    Log.d("BANUSERRESPONSE", "FAILED")
                    _uiState.value = _uiState.value.copy(
                        showStickyHeader = true,
                        undoBanResponse = false,
                        banResponseMessage = "ban attempt unsuccessful"
                    )
                }
            }
        }
    }

}

    //TODO: TWICH METHOD
    fun banUser(banUser: BanUser) = viewModelScope.launch {
        val banUserNew = BanUser(
            data = BanUserData( //TODO:SHOULD BE PASSED IN
                user_id =_clickedUIState.value.clickedUserId,
                reason = banUser.data.reason,
                duration = _uiState.value.banDuration

            )
        )
        Log.d("deleteChatMessageException", "PbanUser.user_id---> ${banUserNew.data.user_id}")
        // Log.d("deleteChatMessageException", "clickedUserId ${clickedUserId}")
        withContext(ioDispatcher + CoroutineName("BanUser")) {
            twitchRepoImpl.banUser(
                oAuthToken = _uiState.value.oAuthToken,
                clientId = _uiState.value.clientId,
                moderatorId = _uiState.value.userId,
                broadcasterId = _uiState.value.broadcasterId,
                body = banUserNew
            ).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        Log.d("BANUSERRESPONSE", "LOADING")
                    }
                    is Response.Success -> {
                        Log.d("BANUSERRESPONSE", "SUCCESS")
                        _uiState.value = _uiState.value.copy(
                            banResponse = Response.Success(true),
                            banReason = "",
                            undoBanResponse = false
                        )
                    }
                    is Response.Failure -> {
                        Log.d("BANUSERRESPONSE", "FAILED")
                        _uiState.value = _uiState.value.copy(
                            showStickyHeader = true,
                            undoBanResponse = false,
                            banResponseMessage = "ban attempt unsuccessful"
                        )
                    }
                }
            }
        }
    }
    //TODO: TWICH METHOD
    fun unBanUser() = viewModelScope.launch {
        withContext(ioDispatcher + CoroutineName("UnBanUser")) {
            twitchRepoImpl.unBanUser(
                oAuthToken = _uiState.value.oAuthToken,
                clientId = _uiState.value.clientId,
                moderatorId = _uiState.value.userId,
                broadcasterId = _uiState.value.broadcasterId,
                userId = _idOfLatestBan.value //TODO:PASS IT IN

            ).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        Log.d("TESTINGTHEUNBANRESPONSE", "LOADING")
                    }
                    is Response.Success -> {
                        _uiState.value = _uiState.value.copy(
                            banResponse = Response.Success(true),
                            undoBanResponse = true
                        )

                        val unBanSuccessMessage =TwitchUserDataObjectMother.addColor("#FFBB86FC")
                            .addDisplayName("Room update")
                            .addUserType("Unban successful")
                            .addMessageType(MessageType.NOTICE)
                            .build()
                        listChats.add(unBanSuccessMessage)
                    }
                    is Response.Failure -> {
                        Log.d("TESTINGTHEUNBANRESPONSE", "FAILED")
                        _uiState.value = _uiState.value.copy(
                            showStickyHeader = true,
                            undoBanResponse = false,
                            banResponseMessage = "Fail. User may be unbanned"
                        )
                    }
                }
            }
        }
    }
    fun removeUnBanButton() {
        _uiState.value = _uiState.value.copy(
            banResponse = Response.Success(true),
            undoBanResponse = true
        )
    }


    override fun onCleared() {
        super.onCleared()
        webSocket.close()
    }





}


