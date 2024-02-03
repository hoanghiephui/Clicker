package com.example.clicker.presentation.modChannels.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clicker.R

import com.example.clicker.presentation.modChannels.views.ModChannelComponents.Parts.EmptyList

import kotlinx.coroutines.launch

/**
 * - Contains 1 implementation:
 * 1) [MainModView]
 *
 * - ModChannelComponents represents all the UI composables that create the UI experience for the ModChannelView
 *
 *
 * */

object ModChannelComponents{
    /**THIS IS THE MAIN IMPLEMENTATION LEVEL*/

    /**
     * - Implementation of [Builders.ScaffoldBuilder].
     * - Contains 3 parts:
     * 1) [CustomTopBar][Parts.CustomTopBar]
     * 2) [CustomBottomBar][Parts.CustomBottomBar]
     * 3) [ModChannelsList][Parts.ModChannelsList]
     *
     * @param onNavigate a function used to navigate from the home page to the individual stream view
     * @param height a Int representing the height in a aspect ratio that will make the images look nice
     * @param width a Int representing the width in a aspect ratio that will make the images look nice
     * @param density a float meant to represent the screen density of the current device
     * */
    @Composable
    fun MainModView(
        onNavigate: (Int) -> Unit,
        height: Int,
        width: Int,
        density:Float
    ){
        Builders.ScaffoldBuilder(
            topBar = {
                Parts.CustomTopBar(
                    onNavigate ={destination -> onNavigate(destination)}
                )
            },
            bottomBar={
                Parts.CustomBottomBar(
                    onNavigate ={destination -> onNavigate(destination)}
                )
            },
            modChannelList={ contentPadding ->
                Parts.ModChannelsList(
                    contentPadding,height, width, density
                )
            }
        )
    }


    /**BUILDERS BELOW THIS*/
    /**
     * Builder represents the most generic parts of [ModChannelComponents] and should be thought of as layout guides used
     * by the implementations above
     * */
   private object Builders{

        /**
         * - ScaffoldBuilder is used inside of  [ModChannelComponents].
         *
         *
         * @param topBar a composable meant to act as the UI for this Scaffolds top bar
         * @param bottomBar a composable meant to act as the UI for this Scaffolds bottom bar
         * This will get covered by the scaffold
         * @param modChannelList a composable that will act as the list to display all the items shown to the user
         * */
        @Composable
        fun ScaffoldBuilder(
            topBar:@Composable () -> Unit,
            bottomBar:@Composable () -> Unit,
            modChannelList:@Composable (contentPadding:PaddingValues) -> Unit,
        ){

            Scaffold(
                backgroundColor= MaterialTheme.colorScheme.primary,
                topBar = {
                    topBar()
                },
                bottomBar = {
                    bottomBar()
                },
            ) { contentPadding ->
                modChannelList(contentPadding)
            }
        }
    } /***END OF THE BUILDERS****/



    /**
     * Parts represents the most individual parts of [ModChannelComponents] and should be thought of as the individual
     * pieces that are used inside of a [Builders] to create a [ModChannelComponents] implementation
     * */
    private object Parts{

        /**
         * - Contains 3 extra parts:
         * 1) [ModHeader]
         * 2) [EmptyList]
         * 3) [LiveModChannelItem]
         *
         * - LiveChannelRowItem is a composable function that will show the individual information for each live stream
         * retrieved from the Twitch server
         *
         * @param contentPadding it is a nullable list of all the live channels returned by the twitch server
         * @param density a Float representing the screen density of the current device
         * @param height a Int representing the height of the image. The height is in a 9/16 aspect ration
         * @param width a Int representing the width of the image. The width is in a 9/16 aspect ration
         * @param liveList subject to change
         * @param offLineList subject to change
         * */
        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ModChannelsList(
            contentPadding: PaddingValues,
            height: Int,
            width: Int,
            density:Float,
            liveList:List<String> = listOf("Bob","Soda","Urza","Poppin","Hasan","CohCarnage"),
            offLineList:List<String> = listOf("Bob","Soda","Urza","Poppin","Hasan","CohCarnage")

        ){
            LazyColumn(modifier = Modifier.padding(contentPadding)) {
                stickyHeader {
                    ModHeader("Live")
                }
                if(liveList.isEmpty()){
                    item{
                        EmptyList(
                            message ="No live moderated channels found"
                        )
                    }
                }
                items(liveList){channelName ->

                    LiveModChannelItem(
                        height,
                        width,
                        density,
                        channelName = channelName,
                        streamTitle="It do be like that ",
                        gameTitle ="Panzer another"
                    )
                }

                stickyHeader {
                    ModHeader("Offline")
                }
                if(offLineList.isEmpty()){
                    item{
                        EmptyList(
                            message ="No offline moderated channels found"
                        )
                    }
                }


                items(offLineList){channelName ->
                    OfflineModChannelItem(
                        height,
                        width,
                        density,
                        channelName = channelName
                    )
                }



            }
        }

        /**
         * - Contains 2 extra parts:
         * 1) [OfflineModChannelImage]
         * 2) [StreamerName]
         *
         * - OfflineModChannelItem is a composable function meant to show the individual mod channels that are offline
         *
         * @param height a Int representing the height in a aspect ratio that will make the images look nice
         * @param width a Int representing the width in a aspect ratio that will make the images look nice
         * @param density a float meant to represent the screen density of the current device
         * @param channelName a String meant to represent the name of the channel shown to the user
         * */
        @Composable
        fun OfflineModChannelItem(
            //updateStreamerName: (String, String, String, String) -> Unit,
            // streamItem: StreamInfo,
//    clientId: String,
//    userId:String,
//    onNavigate: (Int) -> Unit,
            height: Int,
            width: Int,
            density:Float,
            channelName:String


        ){
            Row(
                modifier = Modifier.padding(10.dp).clickable {}
            ){
                OfflineModChannelImage(height,width, density)
                StreamerName(channelName)
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
        }

        /**
         * - Contains 2 extra parts:
         * 1) [OfflineModChannelImage]
         * 2) [StreamInfo]
         *
         * - LiveModChannelItem is a composable function meant to show the individual mod channels that are live
         *
         * @param height a Int representing the height in a aspect ratio that will make the images look nice
         * @param width a Int representing the width in a aspect ratio that will make the images look nice
         * @param density a float meant to represent the screen density of the current device
         * @param channelName a String meant to represent the name of the channel shown to the user
         * @param streamTitle a String meant to represent the name of the title of the stream
         * @param gameTitle a String meant to represent the name of the title of the game
         * */
        @Composable
        fun LiveModChannelItem(
            height: Int,
            width: Int,
            density:Float,
            channelName:String,
            streamTitle:String,
            gameTitle:String,
        ){
            Row(
                modifier = Modifier.padding(10.dp).clickable {}
            ){
                OfflineModChannelImage(height,width, density)
                StreamInfo(channelName,streamTitle,gameTitle)
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
        }
        /**
         * - Contains 0 extra parts:

         *
         * - StreamInfo is a composable function meant to show the individual information about a stream
         *

         * @param streamTitle a String meant to represent the name of the title of the stream
         * @param gameTitle a String meant to represent the name of the title of the game
         * @param streamerName a String meant to represent the name of the streamer
         * */
        @Composable
        fun StreamInfo(
            streamerName:String,
            streamTitle:String,
            gameTitle:String,
        ){
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    streamerName,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    streamTitle,
                    fontSize = 15.sp,
                    modifier = Modifier.alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    gameTitle,
                    fontSize = 15.sp,
                    modifier = Modifier.alpha(0.7f),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }


        /**
         * - Contains 0 extra parts:
         *
         * - StreamerName is a composable function meant to show the individual streamer's name
         *
         * @param channelName a String meant to represent the name of the streamer
         * */
        @Composable
        fun StreamerName(channelName:String){
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    channelName,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        /**
         * - Contains 0 extra parts:
         *
         * - ModHeader is a composable function meant to act as a sticky header in a [LazyColumn]
         *
         * @param headerTitle a String meant to represent the name of the streamer
         * */
        @Composable
        fun ModHeader(
            headerTitle:String
        ){
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(shape = RoundedCornerShape(5.dp))
                    .background(color = MaterialTheme.colorScheme.primary)

            ){
                Text(
                    headerTitle,
                    color =MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
            }


        }

        /**
         * - Contains 0 extra parts:
         *
         * - EmptyList is a composable function meant to shown to the user when there is a empty [LazyColumn]
         *
         * @param message a String meant to represent a message shown directly to the user
         * */
        @Composable
        fun EmptyList(
            message:String
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
                    .clickable { },
                elevation = 10.dp,
                backgroundColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(2.dp,MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Text(message, fontSize = 20.sp,color = MaterialTheme.colorScheme.onPrimary)

                }
            }
        }

        /**
         * - Contains 0 extra parts:
         *
         * - CustomTopBar is a composable function meant to act as the top bar inside of a [Scaffold]
         *
         * @param onNavigate a function used to navigate between fragments
         * */
        @Composable
        fun CustomTopBar(
            onNavigate: (Int) -> Unit
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        "Navigate back to home Fragment",
                        modifier = Modifier
                            .size(35.dp)
                            .clickable {
                                onNavigate(R.id.action_modChannelsFragment_to_homeFragment)
                            },
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        "Channels you mod for",
                        fontSize = 25.sp,
                        modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }

        /**
         * - Contains 0 extra parts:
         *
         * - CustomBottomBar is a composable function meant to act as the bottom bar inside of a [Scaffold]
         *
         * @param onNavigate a function used to navigate between fragments
         * */
        @Composable
        fun CustomBottomBar(
            onNavigate: (Int) -> Unit,
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier=Modifier.clickable { onNavigate(R.id.action_modChannelsFragment_to_homeFragment) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(35.dp)
                    )
                    androidx.compose.material.Text("Home", color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.moderator_secondary_color),
                        "Moderation Icon",
                        tint= MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(35.dp)
                    )
                    androidx.compose.material.Text(
                        "Mod Channels",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }


            }
        }

        /**
         * - Contains 0 extra parts:
         *
         * - OfflineModChannelImage is a composable function meant to mimic a streamer's stream image
         *
         * @param height a Int representing the height in a aspect ratio that will make the images look nice
         * @param width a Int representing the width in a aspect ratio that will make the images look nice
         * @param density a float meant to represent the screen density of the current device
         * */
        @Composable
        fun OfflineModChannelImage(
            height: Int,
            width: Int,
            density:Float
        ){
            val adjustedHeight = height/density
            val adjustedWidth = width/density
            Column() {
                Box(
                    modifier = Modifier.height(adjustedHeight.dp).width(adjustedWidth.dp).clip(RectangleShape).background(Color.DarkGray)
                ){
                    Text("Offline",modifier = Modifier.align(Alignment.Center), fontSize = 20.sp,color = Color.White)
                }
                Spacer(modifier=Modifier.height(10.dp))
            }
        }

    }/***END OF THE PARTS****/

}



















