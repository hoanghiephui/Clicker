package com.example.clicker.network.repository

import android.util.Log
import com.example.clicker.network.BanUser
import com.example.clicker.network.BanUserResponse
import com.example.clicker.network.TwitchClient
import com.example.clicker.network.domain.TwitchRepo
import com.example.clicker.network.models.ChatSettings
import com.example.clicker.network.models.FollowedLiveStreams
import com.example.clicker.network.models.UpdateChatSettings
import com.example.clicker.network.models.ValidatedUser
import com.example.clicker.network.models.toStreamInfo
import com.example.clicker.presentation.home.StreamInfo
import com.example.clicker.util.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.net.UnknownHostException
import javax.inject.Inject

class TwitchRepoImpl @Inject constructor(
    private val twitchClient: TwitchClient
): TwitchRepo {

    override suspend fun validateToken(token:String):Flow<Response<ValidatedUser>> = flow{
        emit(Response.Loading)
        Log.d("VALIDATINGTHETOKEN","LOADING")
       val response= twitchClient.validateToken(
            authorization = "OAuth $token"
        )
        if(response.isSuccessful){
           emit(Response.Success(response.body()!!))
        }else{
            emit(Response.Failure(Exception("Error! Please login again")))
            Log.d("VALIDATINGTHETOKEN","ERROR")
        }

    }.catch { cause ->
        if(cause is UnknownHostException){
            emit(Response.Failure(Exception("Network Error! Please check your connection and try again")))
        }else{
            emit(Response.Failure(Exception("Error! Please try again")))
        }


    }

    override suspend fun getFollowedLiveStreams(
        authorizationToken: String,
        clientId: String,
        userId: String
    ): Flow<Response<List<StreamInfo>>> = flow{
        emit(Response.Loading)
        Log.d("GETTINGLIVESTREAMS","getFollowedLiveStreams() IS RUN")

        val response = twitchClient.getFollowedStreams(
            authorization = "Bearer $authorizationToken",
            clientId = clientId,
            userId = userId
        )
        if (response.isSuccessful){

            val transformedData = response.body()!!.data.map { it.toStreamInfo() }
            emit(Response.Success(transformedData))
        }else{

            emit(Response.Failure(Exception("Error!, code: {${response.code()}}")))

        }
    }.catch { cause ->
        Log.d("GETTINGLIVESTREAMS","CAUSE IS CAUSE")
        //Log.d("GETTINGLIVESTREAMS","RUNNING THE METHOD USER--> $user ")
        if(cause is UnknownHostException){
            emit(Response.Failure(Exception("Network Error! Please check your connection and try again")))
        }else{
            emit(Response.Failure(Exception("Error getting streams! Please try again")))
        }


    }

    override fun logout(clientId:String,token:String): Flow<Response<String>> = flow{
        emit(Response.Loading)

        val response = twitchClient.logout(clientId = clientId, token = token)
        if (response.isSuccessful){
            Log.d("logoutResponse", "SUCCESS ->${response.message()}")
            emit(Response.Success("true"))
        }else{

            Log.d("logoutResponse", "FAILED ->${response.message()}")
            emit(Response.Failure(Exception("Error!, code: {${response.code()}}")))

        }
    }.catch { cause ->
        Log.d("GETTINGLIVESTREAMS","CAUSE IS CAUSE")
        //Log.d("GETTINGLIVESTREAMS","RUNNING THE METHOD USER--> $user ")
        if(cause is UnknownHostException){
            emit(Response.Failure(Exception("Network Error! Please check your connection and try again")))
        }else{
            emit(Response.Failure(Exception("Logout Error! Please try again")))
        }


    }

    override suspend fun getChatSettings(
        oAuthToken: String,
        clientId: String,
        broadcasterId: String
    )= flow {
        emit(Response.Loading)

         val response = twitchClient.getChatSettings(
            authorization = oAuthToken,
            clientId = clientId,
            broadcasterId = broadcasterId
        )
        if (response.isSuccessful){
            emit(Response.Success(response.body()!!))
        }else{

            emit(Response.Failure(Exception(response.message())))
        }
    }.catch { cause ->
        Log.d("GETTINGLIVESTREAMS","CAUSE IS CAUSE")
        //Log.d("GETTINGLIVESTREAMS","RUNNING THE METHOD USER--> $user ")
        if(cause is UnknownHostException){
            emit(Response.Failure(Exception("Network Error! Please check your connection and try again")))
        }else{
            emit(Response.Failure(Exception("Logout Error! Please try again")))
        }


    }

    override suspend fun updateChatSettings(
        oAuthToken: String,
        clientId: String,
        broadcasterId: String,
        moderatorId: String,
        body: UpdateChatSettings
    ): Flow<Response<Boolean>>  = flow{
        emit(Response.Loading)
        throw Exception("IT DO BE LIKE THAT SOMETIMES")
        val response =twitchClient.updateChatSettings(
            authorizationToken = "Bearer $oAuthToken",
            clientId = clientId,
            broadcasterId = broadcasterId,
            moderatorId= moderatorId,
            body = body
        )
        Log.d("changeChatSettingsUpdate","REQUEST MADE")
        if (response.isSuccessful){
            emit(Response.Success(true))
        }else{
            emit(Response.Failure(Exception(response.message())))
        }
    }.catch { cause ->
        Log.d("GETTINGLIVESTREAMS","CAUSE IS CAUSE")
        //Log.d("GETTINGLIVESTREAMS","RUNNING THE METHOD USER--> $user ")
        if(cause is UnknownHostException){
            emit(Response.Failure(Exception("response.message()")))
        }else{
            emit(Response.Failure(Exception("response.message()")))
        }


    }

    override suspend fun deleteChatMessage(
        oAuthToken: String,
        clientId: String,
        broadcasterId: String,
        moderatorId: String,
        messageId: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        val response = twitchClient.deleteChatMessage(
            authorizationToken = "Bearer ${oAuthToken}",
            clientId = clientId,
            broadcasterId = broadcasterId,
            moderatorId = moderatorId,
            messageId = messageId
        )
        if(response.isSuccessful){
            emit(Response.Success(true))
        }else{
            Log.d("deleteChatMessageException",response.message())
            Log.d("deleteChatMessageException",response.code().toString())
            emit(Response.Failure(Exception("MESSAGE NOT DELETED")))
        }
    }

    override suspend fun banUser(
        oAuthToken: String,
        clientId: String,
        broadcasterId: String,
        moderatorId: String,
        body:BanUser
    ): Flow<Response<BanUserResponse>> = flow{
        emit(Response.Loading)
        val response = twitchClient.banUser(
            authorizationToken = "Bearer ${oAuthToken}",
            clientId = clientId,
            broadcasterId = broadcasterId,
            moderatorId = moderatorId,
            body = body
        )
//        Log.d("BANUSERRESPONSE","moderatorId --> ${moderatorId}")
//        Log.d("BANUSERRESPONSE","oAuthToken --> ${oAuthToken}")
//        Log.d("BANUSERRESPONSE","clientId --> ${clientId}")
//        Log.d("BANUSERRESPONSE","broadcasterId --> ${broadcasterId}")
//        Log.d("BANUSERRESPONSE","body.data --> ${body.data}")
//        Log.d("BANUSERRESPONSE","message --> ${response.message()}")


        if(response.isSuccessful){
            val data = response.body()
            data?.let{
                emit(Response.Success(it))
            }
        }else{
            Log.d("BANUSERRESPONSE","code --> ${response.code()}")
            Log.d("BANUSERRESPONSE","message --> ${response.message()}")
            Log.d("BANUSERRESPONSE","body --> ${response.body()}")
            Log.d("BANUSERRESPONSE","errorBody --> ${response.errorBody()}")
            emit(Response.Failure(Exception("Ban User exception")))
        }
    }

    override suspend fun unBanUser(
        oAuthToken: String,
        clientId: String,
        broadcasterId: String,
        moderatorId: String,
        userId: String
    ): Flow<Response<Boolean>> = flow{
        emit(Response.Loading)
        val response = twitchClient.unBanUser(
            authorizationToken = "Bearer ${oAuthToken}",
            clientId = clientId,
            broadcasterId = broadcasterId,
            moderatorId = moderatorId,
            userId = userId
        )
        if(response.isSuccessful){
            emit(Response.Success(true))
            Log.d("UNBANUSERRESPONSE","code --> ${response.message()}")
        }else{
            Log.d("UNBANUSERRESPONSE","code --> ${response.code()}")
            Log.d("UNBANUSERRESPONSE","headers --> ${response.headers()}")
            Log.d("UNBANUSERRESPONSE","body --> ${response.body()}")
            Log.d("UNBANUSERRESPONSE","errorBody --> ${response.errorBody()}")

            emit(Response.Failure(Exception("ERROR BANNING USER")))
        }
    }


}