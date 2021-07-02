package postpc.postpc.ex10.server

import android.app.Application
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val url = "https://hujipostpc2019.pythonanywhere.com"

class Server: Application() {
    private val retrofit: Retrofit = Retrofit.Builder()
            .client(OkHttpClient.Builder().build())
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val serverInterface: ServerInterface = retrofit.create(ServerInterface::class.java)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: Server
    }
}