package postpc.ex7.ex10.server

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor

const val url = "https://hujipostpc2020.pythonanywhere.com"

class ServerObj {
    companion object {
        private val okHttpClient: OkHttpClient by lazy {
            val okHttpClientBuilder = OkHttpClient.Builder()
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(interceptor)

            return@lazy okHttpClientBuilder.build()
        }

        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        val serverInterface: ServerInterface by lazy { retrofit.create(ServerInterface::class.java) }
    }
}