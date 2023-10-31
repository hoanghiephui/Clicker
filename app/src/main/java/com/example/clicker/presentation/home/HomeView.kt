package com.example.clicker.presentation.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.clicker.R
import com.example.clicker.presentation.authentication.AuthenticationViewModel
import com.example.clicker.presentation.stream.StreamViewModel
import com.example.clicker.util.Response
import com.example.clicker.util.rememberNestedScrollConnection
import com.example.clicker.util.rememberPullToRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ValidationView(
    homeViewModel: HomeViewModel,
    streamViewModel: StreamViewModel,
    authenticationViewModel: AuthenticationViewModel,
    loginWithTwitch: () -> Unit,
    onNavigate: (Int) -> Unit,
    addToLinks: () -> Unit
) {
    val bottomModalState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val modalText = authenticationViewModel.authenticationUIState.value.modalText
    val showModalState = authenticationViewModel.authenticationUIState.value.showLoginModal
    val domainIsRegistered = homeViewModel.state.value.domainIsRegistered
    if (showModalState) {
        LaunchedEffect(bottomModalState) { // the key define when the block is relaunched
            // Your coroutine code here
            scope.launch {
                bottomModalState.show()
            }
        }
    } else {
        LaunchedEffect(bottomModalState) { // the key define when the block is relaunched
            // Your coroutine code here
            scope.launch {
                bottomModalState.hide()
            }
        }
    }

    val userIsAuthenticated = authenticationViewModel.authenticationUIState.value.authenticated
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

    ModalBottomSheetLayout(
        sheetState = bottomModalState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.DarkGray),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modalText,
                    color = Color.White,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Button(onClick = { loginWithTwitch() }) {
                    Text(text = "Login with Twitch")
                }
            }
        }
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                ScaffoldDrawer(
                    logout = {
                        authenticationViewModel.beginLogout()
                    },
                    loginWithTwitch = {
                        loginWithTwitch()
                    },
                    scaffoldState = scaffoldState,
                    userIsLoggedIn = userIsAuthenticated
                )
            },
            topBar = {
                CustomTopBar(
                    scaffoldState = scaffoldState
                )
            }
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(Color.DarkGray)
            ) {
                if (userIsAuthenticated) {
                    val certifiedUser = authenticationViewModel.validatedUser()
                    homeViewModel.updateAuthenticatedUser(certifiedUser)
                }

                HomeView(
                    homeViewModel,
                    streamViewModel,
                    onNavigate
                )
            }
        }
    }

    if (!domainIsRegistered) {
        DisableForceRegister(
            addToLinks = { addToLinks() }
        )
    }
}

@Composable
fun DisableForceRegister(
    addToLinks: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .disableClickAndRipple()
                .background(
                    color = Color.Gray.copy(alpha = .7f)
                )
                .matchParentSize()
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
                .align(Alignment.Center)
                .clickable { },
            backgroundColor = Color.DarkGray,
            elevation = 10.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You must add",
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Text(
                    "com.example.clicker",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    " to links to enable login with Twitch on Android 12 and higher",
                    fontSize = 25.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { addToLinks() },
                    modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text(text = "Add to links", fontSize = 25.sp, color = Color.White)
                }
            }
        }
    } // end of the box
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun HomeView(
    homeViewModel: HomeViewModel,
    streamViewModel: StreamViewModel,
    onNavigate: (Int) -> Unit
) {
    val urlListLoading = homeViewModel.state.value.streamersListLoading
    // todo: home pager page goes here
    UrlImages(
        urlList = homeViewModel.newUrlList.collectAsState().value,
        onNavigate = { onNavigate(R.id.action_homeFragment_to_streamFragment) },
        updateStreamerName = {
                streamerName, clientId, broadcasterId, userId ->
            streamViewModel.updateChannelNameAndClientIdAndUserId(
                streamerName,
                clientId,
                broadcasterId,
                userId
            )
        },
        clientId = homeViewModel.authenticatedUser.value?.clientId ?: "",
        userId = homeViewModel.authenticatedUser.value?.userId ?: "",
        networkRequest = {
                resetUI: suspend () -> Unit ->
            homeViewModel.pullToRefreshGetLiveStreams(
                resetUI = resetUI
            )
        },
        showFailedNetworkRequestMessage = homeViewModel.state.value.failedNetworkRequest,
        height = homeViewModel.state.value.aspectHeight,
        width = homeViewModel.state.value.width,
        urlListLoading = urlListLoading
    )
} // END OF THE HOME VIEW

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTopBar(
    scaffoldState: ScaffoldState

) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.red))
            .padding(vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Menu,
                "menu",
                modifier = Modifier
                    .size(35.dp)
                    .clickable { scope.launch { scaffoldState.drawerState.open() } },
                tint = Color.White
            )
            Text(
                "Live channels",
                fontSize = 25.sp,
                modifier = Modifier.padding(start = 20.dp),
                color = Color.White
            )
        }
    }
}

@Composable
fun ScaffoldDrawer(
    logout: () -> Unit,
    loginWithTwitch: () -> Unit,
    scaffoldState: ScaffoldState,
    userIsLoggedIn: Boolean
) {
    if (userIsLoggedIn) {
        LogoutCard(scaffoldState, logout = { logout() })
    } else {
        LoginCard(scaffoldState, loginWithTwitch = { loginWithTwitch() })
    }
}

@Composable
fun LoginCard(
    scaffoldState: ScaffoldState,
    loginWithTwitch: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable {
                Log.d("TRYINGtOlOGoUT", "IN")
                scope.launch {
                    scaffoldState.drawerState.close()
                }
                loginWithTwitch()
            },
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("Login with Twitch", fontSize = 20.sp)
            Icon(
                Icons.Default.AccountCircle,
                "Logout",
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
fun LogoutCard(
    scaffoldState: ScaffoldState,
    logout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable {
                Log.d("TRYINGtOlOGoUT", "OUT")
                scope.launch {
                    scaffoldState.drawerState.close()
                }
                logout()
            },
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("Logout", fontSize = 20.sp)
            Icon(
                Icons.Default.ExitToApp,
                "Logout",
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
fun UrlImages(
    urlList: List<StreamInfo>?,
    onNavigate: (Int) -> Unit,
    updateStreamerName: (String, String, String, String) -> Unit,
    clientId: String,
    userId: String,
    networkRequest: (suspend () -> Unit) -> Unit,
    showFailedNetworkRequestMessage: Boolean,
    height: Int,
    width: Int,
    urlListLoading: Response<Boolean>
) {
    val scope = rememberCoroutineScope()
    val initialColor = colorResource(R.color.red)

    var pullColor by remember { mutableStateOf(initialColor) }
    val configuration = LocalConfiguration.current

    val quarterTotalScreenHeight = configuration.screenHeightDp / 8

    var request by remember { mutableStateOf(false) }
    var pullingState = rememberPullToRefreshState()

    val nestedScrollConnection = rememberNestedScrollConnection(
        state = pullingState,
        scope = scope,
        animationMidPoint = (quarterTotalScreenHeight).toFloat(),
        quarterScreenHeight = quarterTotalScreenHeight.toFloat(),
        changeColor = { color -> pullColor = color },

        changeRequest = { boolean -> request = boolean },
        changeIsRefreshing = { boolean -> pullingState.isRefreshing = boolean }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(Color.DarkGray)

    ) {
        if (request) {
            // then we can also make the request here
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (quarterTotalScreenHeight / 14).dp),
                color = Color.White
            )
            networkRequest {
                pullingState.dispatchToResting()
                pullingState.isRefreshing = false
                request = false
                pullColor = Color.White
            }
        } else {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                "contentDescription",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, pullingState.contentOffset.toInt() - 140) },
                tint = pullColor

            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullingState.contentOffset.toInt()) }
                .background(Color.DarkGray)
                .padding(start = 5.dp, end = 5.dp)

        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                when (urlListLoading) {
                    is Response.Loading -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = Color.Red
                                )
                            }
                        }
                    }
                    is Response.Success -> {
                        if (urlList != null) {
                            Log.d("UrlImagesListSize", urlList.size.toString())

                            if (urlList.isEmpty()) {
                                item {
                                    EmptyFollowingList()
                                }
                            }

                            items(urlList) { streamItem ->
                                Log.d("urlListImageUrl", streamItem.url)
                                Row(
                                    modifier = Modifier.clickable {
                                        Log.d(
                                            "streamer",
                                            "broadcasterIdClicked -->  ${streamItem.url}"
                                        )
                                        updateStreamerName(
                                            streamItem.streamerName,
                                            clientId,
                                            streamItem.broadcasterId,
                                            userId
                                        )
                                        onNavigate(R.id.action_homeFragment_to_streamFragment)
                                    }
                                ) {
                                    Box() {
                                        SubcomposeAsyncImage(
                                            model = streamItem.url,
                                            loading = {
                                                Card(
                                                    modifier = Modifier
                                                        .height((height / 2.8).dp)
                                                        .width((width / 2.8).dp),
                                                    backgroundColor = Color.DarkGray
                                                ) {
                                                }
                                            },
                                            contentDescription = "stringResource(R.string.description)"
                                        )
                                        Text(
                                            "${streamItem.views}",
                                            style = TextStyle(
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(5.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.padding(start = 10.dp)) {
                                        Text(
                                            streamItem.streamerName,
                                            fontSize = 20.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            streamItem.streamTitle,
                                            fontSize = 15.sp,
                                            modifier = Modifier.alpha(0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color.White
                                        )
                                        Text(
                                            streamItem.gameTitle,
                                            fontSize = 15.sp,
                                            modifier = Modifier.alpha(0.7f),
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                )
                            }
                            // end of the lazy column
                        }
                    }
                    is Response.Failure -> {
                        item {
                            GettingStreamsError()
                        }
                    }
                }
            }

            // apparently this is the code I am using to make the message disappear

            AnimatedVisibility(
                visible = showFailedNetworkRequestMessage,
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    elevation = 10.dp,
                    backgroundColor = Color.LightGray
                ) {
                    Text(
                        "Failed request. Please try again",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFollowingList() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "No live streams!",
                tint = Color.Black,
                modifier = Modifier.size(35.dp)
            )
            Text("No live streams. ", fontSize = 20.sp)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "No live streams! pull to refresh",
                tint = Color.Black,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
fun GettingStreamsError() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "pull to refresh",
                tint = Color.Black,
                modifier = Modifier.size(35.dp)
            )
            Text("Error! Pull to refresh ", fontSize = 20.sp)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "pull to refresh",
                tint = Color.Black,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

fun Modifier.disableClickAndRipple(): Modifier = composed {
    clickable(
        enabled = false,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = { }
    )
}
