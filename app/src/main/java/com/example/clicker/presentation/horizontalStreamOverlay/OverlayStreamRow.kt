package com.example.clicker.presentation.horizontalStreamOverlay

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.clicker.R
import com.example.clicker.network.models.twitchRepo.StreamData
import com.example.clicker.presentation.home.HomeViewModel
import com.example.clicker.util.NetworkNewUserResponse


@Composable
fun OverlayStreamRow(
    homeViewModel:HomeViewModel
){
    val height = homeViewModel.state.value.aspectHeight
    val width = homeViewModel.state.value.width
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.7f))
    ){

    }
    LazyRowViewTesting(
        followedStreamerList = homeViewModel.state.value.streamersListLoading,
        height = height,
        width = width,
        density = homeViewModel.state.value.screenDensity
    )


    }


@Composable
fun LazyRowViewTesting(
    followedStreamerList: NetworkNewUserResponse<List<StreamData>>,
    height: Int,
    width: Int,
    density: Float
){
    LazyRow(modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)) {


        when (followedStreamerList) {
            is NetworkNewUserResponse.Success -> {
                val listData = followedStreamerList.data
                items(listData){streamItem ->
                    ImageWithViewCount(
                        url = streamItem.thumbNailUrl,
                        height = height,
                        width = width,
                        viewCount = streamItem.viewerCount,
                        density =density,
                        streamTitle = streamItem.title,
                        streamerName = streamItem.userLogin
                    )
                }


            }

            else -> {

            }
        }
    }

}

@Composable
fun IndivRowItem(){
    Row(){
        Column(modifier = Modifier.width(150.dp)) {
            Box(modifier= Modifier
                .height(80.dp)
                .width(150.dp)
                .background(Color.Blue)){}
            Text(
                "This is the title of the stream and everything else that it has to offer",
                maxLines = 1 ,
                overflow = TextOverflow.Ellipsis,
                color= Color.White
            )
            Text("Streamer name",color= Color.White)

        }

        Spacer(modifier =Modifier.width(10.dp))
    }
}

@Composable
fun ImageWithViewCount(
    url: String,
    height: Int,
    width: Int,
    viewCount:Int,
    density:Float,
    streamTitle:String,
    streamerName:String
){
    Log.d("ImageHeightWidth","url -> $url")
    val adjustedWidth = width / density
    Row() {

        Column(
            modifier = Modifier.width(adjustedWidth.dp)
        ) {

            Box() {
                val adjustedHeight = height / density

                SubcomposeAsyncImage(
                    model = url,
                    loading = {
                        Column(
                            modifier = Modifier
                                .height((adjustedHeight).dp)
                                .width((adjustedWidth).dp)
                                .background(MaterialTheme.colorScheme.primary),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    contentDescription = stringResource(R.string.sub_compose_async_image_description)
                )
                androidx.compose.material.Text(
                    "${viewCount}",
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                )
            }
            Text(streamTitle,color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(streamerName,color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier =Modifier.width(10.dp))
    }
}