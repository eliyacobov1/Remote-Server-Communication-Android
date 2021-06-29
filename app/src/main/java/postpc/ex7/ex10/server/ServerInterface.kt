package postpc.ex7.ex10.server

import com.google.gson.JsonObject
import postpc.ex7.ex10.data.TokenResponse
import postpc.ex7.ex10.data.User
import postpc.ex7.ex10.data.UserResponse
import retrofit2.Call
import retrofit2.http.*

interface ServerInterface {
    @GET("/users/0")
    fun connectivityCheck(): Call<User?>

    @GET("/users/{username}/token/")
    fun getUserToken(@Path("username") username: String?): Call<TokenResponse?>

    @GET("/user/")
    fun getUser(@Header("Authorization") token: String?): Call<UserResponse?>

    @Headers("Content-Type: application/json")
    @POST("/user/edit/")
    fun updateUser(
        @Header("Authorization") token: String?, @Body json: JsonObject?
    ): Call<UserResponse?>
}