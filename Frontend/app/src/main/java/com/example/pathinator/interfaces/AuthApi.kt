package com.example.pathinator.interfaces


import retrofit2.Response
import retrofit2.http.*

data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val access_token: String)
data class Token(val access_token: String, val refresh_token: String)

data class SessionCreateRequest(val name: String)
data class SessionResponse(val name: String, val created_at: String)
data class PathPointRequest(val latitude: Double, val longitude: Double)
data class PathPointResponse(val latitude: Double, val longitude: Double)

interface AuthApi {

    @POST("/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<Token>

    @POST("/auth/register")
    suspend fun signup(@Body request: AuthRequest): Response<Token>


    @POST("/sessions/create")
    suspend fun createSession(
        @Header("Authorization") token: String,
        @Body request: SessionCreateRequest
    ): Response<SessionResponse>

    @GET("/sessions/get_all")
    suspend fun getSessions(
        @Header("Authorization") token: String
    ): Response<List<SessionResponse>>

    @GET("/sessions/{sessionName}/path")
    suspend fun getSessionPath(
        @Header("Authorization") token: String,
        @Path("sessionName") sessionName: String
    ): Response<List<PathPointResponse>>

    @POST("/sessions/{sessionName}/add_point")
    suspend fun addPathPoint(
        @Header("Authorization") token: String,
        @Path("sessionName") sessionName: String,
        @Body point: PathPointRequest
    ): Response<PathPointResponse>

    @POST("/sessions/stop")
    suspend fun stopSession(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST("/auth/refresh")
    suspend fun refreshToken(
        @Body body: Map<String, String>
    ): Response<Token>


}


