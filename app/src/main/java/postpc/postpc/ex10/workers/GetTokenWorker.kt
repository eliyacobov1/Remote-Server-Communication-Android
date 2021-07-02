package postpc.postpc.ex10.workers

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import postpc.postpc.ex10.data.TokenResponse
import postpc.postpc.ex10.server.ServerInterface
import postpc.postpc.ex10.server.Server
import retrofit2.Response
import java.io.IOException

class GetTokenWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val serverInterface: ServerInterface = Server.instance.serverInterface
        return try {
            serverInterface.connectivityCheck()
            val response: Response<TokenResponse?> = serverInterface.getUserToken(inputData.getString("user_name")).execute()
            val token: TokenResponse? = response.body()
            val userAsJson = Gson().toJson(token)
            val outputData = Data.Builder()
                .putString("output", userAsJson)
                .build()
            Result.success(outputData)
        } catch (e: IOException) {
            e.printStackTrace()
            Result.retry()
        }
    }
}