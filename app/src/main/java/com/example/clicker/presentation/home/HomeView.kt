package com.example.clicker.presentation.home

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import coil.compose.AsyncImage
import com.example.clicker.util.Response
import com.example.clicker.R
import com.example.clicker.presentation.stream.StreamViewModel
import com.example.clicker.util.rememberPullToRefreshState
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeView(
    homeViewModel: HomeViewModel,
    streamViewModel: StreamViewModel,
    loginWithTwitch:() -> Unit,
    onNavigate: (Int) -> Unit,
    workerViewModel:WorkerViewModel
){
    val hideModal = homeViewModel.state.value.hideModal
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val scope = rememberCoroutineScope()



    val bottomSheetValue = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
//    if(hideModal){
//        LaunchedEffect(key1 = bottomSheetValue){
//            Log.d("GITHUB","LaunchedEffect RECOMP")
//            bottomSheetValue.hide()
//        }
//    }




    var state by remember { mutableIntStateOf(0) }

    val pagerState = rememberPagerState(pageCount = {
        2
    })

    Box(modifier = Modifier.fillMaxSize()){

        ModalBottomSheetLayout(
            sheetState = bottomSheetValue,
            //TODO: THIS sheetContent WILL GET CHANGED OUT TO BE THE STREAMING VIDEO
            sheetContent = {


            }
        ){

            Scaffold(
                scaffoldState = scaffoldState,
                drawerContent = {
                    ScaffoldDrawer(
                        logout ={ homeViewModel.beginLogout() },
                        scaffoldState = scaffoldState
                    )
                                },
                topBar = {
                    CustomTopBar(
                        state = state,
                        { index -> scope.launch {pagerState.scrollToPage(index)  } },
                        pagerState = pagerState,
                        scaffoldState = scaffoldState,


                    )
                }
            ){contentPadding->

                    //todo: home pager page goes here
//                FilterPager(
//                    contentPadding,
//                    pagerState,
//                    filteredModList = streamViewModel.exposedModList
//                ){
                    UrlImages(
                        contentPadding = contentPadding,
                        urlList =homeViewModel.newUrlList.collectAsState().value,
                        onNavigate ={onNavigate(R.id.action_homeFragment_to_streamFragment)},
                        updateStreamerName={
                                streamerName,clientId,broadcasterId,userId -> streamViewModel.updateChannelNameAndClientIdAndUserId(
                            streamerName,clientId,broadcasterId,userId
                        )
                        },
                        clientId = homeViewModel.state.value.clientId,
                        userId = homeViewModel.state.value.userId
                    )
//                }



            }
            //THIS IS WHAT WILL GET COVERED
        }


        if(homeViewModel.loginState.value.showLoginModal){
            LoginModal(
                modifier = Modifier.matchParentSize(),
                align = Modifier.align(Alignment.Center),
                loginWithTwitch = { loginWithTwitch() },
                loginStatusText = homeViewModel.loginState.value.loginStatusText,
                loginStep1 = homeViewModel.loginState.value.loginStep1,
                loginStep2 = homeViewModel.loginState.value.loginStep2,
                loginStep3 = homeViewModel.loginState.value.loginStep3,
                logoutError = homeViewModel.loginState.value.logoutError,
                logout = {homeViewModel.beginLogout()}
            )
        }





} // end of the box

}// END OF THE HOME VIEW
//BUILDING THE PAGER
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterPager(
    contentPadding: PaddingValues,
    pagerState: PagerState,
    filteredModList:List<String?>,
    pageOne: @Composable () -> Unit
){


    HorizontalPager(
        state = pagerState,
        modifier = Modifier.padding(contentPadding)

    ) { page ->
        // Our page content


        when(page){
            0 ->{

                pageOne()
            }
            1 ->{

                SecondTesting(
                    filteredModList =filteredModList
                )
            }
        }
    }
}

//full screen loading animation
@Composable
fun LoginModal(
    modifier: Modifier,
    align: Modifier,
    loginWithTwitch:() -> Unit,
    loginStatusText:String,
    loginStep1:Response<Boolean>?,
    loginStep2:Response<Boolean>?,
    loginStep3:Response<Boolean>?,
    logoutError:Boolean,
    logout:()-> Unit

) {
            Spacer(
                modifier = modifier
                    .disableClickAndRipple()
                    .background(
                        color = Color.Gray.copy(alpha = .7f)
                    )
            )

        LoginCard(
            modifier = align,
            loginWithTwitch,
            loginStatusText,
            loginStep1,
            loginStep2,
            loginStep3,
            logoutError,
            logout = {logout()}


        )



}

@Composable
fun LoginCard(
    modifier:Modifier,
    loginWithTwitch:() -> Unit,
    statusText:String,
    loginStep1:Response<Boolean>?,
    loginStep2:Response<Boolean>?,
    loginStep3:Response<Boolean>?,
    logoutError:Boolean,
    logout:()-> Unit
){
    val configuration = LocalConfiguration.current

    val widthInDp = configuration.screenWidthDp.dp
    val halfScreenWidth = (widthInDp.value / 3.5).dp
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 10.dp
    ) {

        Column(modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                LoadingIcon(loginStep1)
                Divider(
                    color = Color.Red,
                    thickness = 1.dp,
                    modifier = Modifier
                        .width(halfScreenWidth)
                        .padding(10.dp)
                )
                LoadingIcon(loginStep2)
                Divider(
                    color = Color.Red,
                    thickness = 1.dp,
                    modifier = Modifier
                        .width(halfScreenWidth)
                        .padding(10.dp)
                )
                LoadingIcon(loginStep3)
            }
            Text(statusText, modifier = Modifier
                .padding(bottom = 10.dp),
                textAlign = TextAlign.Center
            )
            if(logoutError){
                Button(onClick = { logout() }) {
                    Text("Attempt logout again")
                }
            }else{
                Button(onClick = { loginWithTwitch() }) {
                    Text("Login with Twitch")
                }
            }

        }


    }
}

@Composable
fun LoadingIcon(response:Response<Boolean>?){
    when(response){
        is Response.Loading ->{
            CircularProgressIndicator()
        }
        is Response.Success ->{
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription ="Circle with checkmark indicating a process is complete",
                tint = Color.Green,
                modifier = Modifier.size(35.dp)
            )
        }
        is Response.Failure ->{
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription ="Circle with checkmark indicating a process is complete",
                tint = Color.Red,
                modifier = Modifier.size(40.dp)
            )
        }

        else -> {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription ="Circle with checkmark indicating a process is complete",
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        }
    }

}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTopBar(
    state: Int,
    changeState: (Int) -> Unit,
    pagerState: PagerState,
    scaffoldState: ScaffoldState,

){

    val scope = rememberCoroutineScope()
   // var state by remember { mutableStateOf(0) }
    Log.d("pagerStateCurrentPage",pagerState.currentPage.toString())

    val titles = listOf("Followed", "Mods")// I WAS USING A TABBED ROW FOR THIS
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colors.primary)
        .padding(vertical = 10.dp)){
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                Icons.Filled.Menu,
                "menu",
                modifier = Modifier
                    .size(35.dp)
                    .clickable { scope.launch { scaffoldState.drawerState.open() } },
                tint = Color.White)
            Text("Live channels", fontSize = 25.sp,modifier = Modifier.padding(start=20.dp), color = Color.White)
        }
//        TabRow(selectedTabIndex = pagerState.currentPage) {
//            titles.forEachIndexed { index, title ->
//                Tab(
//                    text = { Text(title) },
//                    selected = pagerState.currentPage == index,
//                    onClick = { changeState(index) }
//                )
//            }
//        }


    }


}

@Composable
fun ScaffoldDrawer(
    logout: () -> Unit,
    scaffoldState: ScaffoldState,
){
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable {
                scope.launch {
                    scaffoldState.drawerState.close()
                }
                logout()
            },
        elevation = 10.dp
    ){
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ){
            Text("Logout", fontSize = 20.sp)
            Icon(
                Icons.Default.ExitToApp,
                "Logout",
                modifier = Modifier.size(35.dp))

        }
    }


}




@Composable
fun SecondTesting(
    filteredModList: List<String?>
) {
    for(item in filteredModList){
        Log.d("loggedInUserUiStateViewModel","homeViewList --> ${item}")
    }
    if(filteredModList.isNotEmpty()){
        for(items in filteredModList){
            items?.let{channelTitle ->
                Row(){
                    Box(modifier = Modifier
                        .width(120.dp)
                        .height(80.dp)
                        .background(Color.Red)){

                    }
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(channelTitle, fontSize = 20.sp)
                        Text(
                            "streamItem.streamTitle",
                            fontSize = 15.sp,
                            modifier = Modifier.alpha(0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "streamItem.gameTitle",
                            fontSize = 15.sp,
                            modifier = Modifier.alpha(0.5f)
                        )
                    }
                }
            }
        }

    }else{
        Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.padding(10.dp)){
            Text("No live mod channels.", fontSize = 25.sp)
            Text("If you see channels you mod for in the followed section, please click on them. They will then be automatically added to your mod list",fontSize = 20.sp)
        }

    }



}





@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UrlImages(

    contentPadding: PaddingValues,
    urlList:List<StreamInfo>?,
    onNavigate: (Int) -> Unit,
    updateStreamerName: (String,String,String,String) -> Unit,
    clientId:String,
    userId:String
){
    val scope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { Log.d("refressingThings","REFRESHPULL") })
    var pullingState = rememberPullToRefreshState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {

                if(NestedScrollSource.Drag == source && available.y > 0){
                    Log.d("REFRESHINGSTATETHINGS","${available.y}")
                    scope.launch {
                        pullingState.dispatchScrollDelta(available.y *0.3f)
                    }
                }


                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                Log.d("REFRESHINGSTATETHINGS","POST-FLINGING!!!!!!!!!!!!!!")
                return super.onPostFling(consumed, available)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                Log.d("REFRESHINGSTATETHINGS","PRE-FLINGING!!!!!!!!!!!!!!")
                scope.launch {
                    pullingState.dispatchToResting()
                }

                return super.onPreFling(available)
            }

        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(Color.Blue)

    ) {


        CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter))




        Box(modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, pullingState.contentOffset.toInt()) }
            .background(Color.Blue)

        ){
            if (urlList != null) {
                if (urlList.isEmpty()){
                    //TODO: IMPLEMENT THE CLICK TO REFRESH
                    EmptyFollowingList()
                }


                Log.d("UrlImagesListSize", urlList.size.toString())
                LazyColumn(
                    modifier = Modifier.padding(contentPadding)
                ) {
                    items(urlList) { streamItem ->
                        Log.d("urlListImageUrl", streamItem.url)
                        Row(modifier = Modifier.clickable {

                            Log.d(
                                "broadcasterIdClicked",
                                "broadcasterIdClicked -->  ${streamItem.broadcasterId}"
                            )
                            updateStreamerName(
                                streamItem.streamerName, clientId, streamItem.broadcasterId, userId
                            )
                            onNavigate(R.id.action_homeFragment_to_streamFragment)
                        }
                        ) {
                            Box() {

                                AsyncImage(
                                    model = streamItem.url,
                                    contentDescription = null
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
                                Text(streamItem.streamerName, fontSize = 20.sp)
                                Text(
                                    streamItem.streamTitle,
                                    fontSize = 15.sp,
                                    modifier = Modifier.alpha(0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    streamItem.gameTitle,
                                    fontSize = 15.sp,
                                    modifier = Modifier.alpha(0.5f)
                                )
                            }

                        }

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                    }
                }// end of the lazy column
            }
        }





        PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))

    }



}

@Composable
fun EmptyFollowingList(){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(15.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text("No live followed channels", fontSize = 30.sp, textAlign = TextAlign.Center)

        }

    }
}




fun Modifier.disableClickAndRipple(): Modifier = composed {
    clickable(
        enabled = false,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = { },
    )
}

