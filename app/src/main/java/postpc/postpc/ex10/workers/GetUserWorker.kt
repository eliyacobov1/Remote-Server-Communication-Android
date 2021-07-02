package postpc.postpc.ex10.workers

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import postpc.postpc.ex10.data.UserResponse
import postpc.postpc.ex10.server.ServerInterface
import postpc.postpc.ex10.server.Server
import retrofit2.Response
import java.io.IOException

class GetUserWorker (context: Context, workerParams: WorkerParameters):
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val serverInterface: ServerInterface = Server.instance.serverInterface
        val inputToken = inputData.getString("token")

        return try {
            val response: Response<UserResponse?> = serverInterface.getUser("token $inputToken").execute()
            val user: UserResponse? = response.body()
            val userAsJson = Gson().toJson(user)
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