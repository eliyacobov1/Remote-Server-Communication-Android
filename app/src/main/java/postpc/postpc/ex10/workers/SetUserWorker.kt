package postpc.postpc.ex10.workers

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonObject
import postpc.postpc.ex10.data.UserResponse
import postpc.postpc.ex10.server.ServerInterface
import postpc.postpc.ex10.server.Server
import retrofit2.Response
import java.io.IOException

class SetUserWorker (context: Context, workerParams: WorkerParameters):
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val serverInterface: ServerInterface = Server.instance.serverInterface
        val prettyName: String? = inputData.getString("pretty_name")
        val imageURL: String? = inputData.getString("image_url")
        var token: String? = inputData.getString("token")
        return try {
            token = "token $token"
            val jObj = JsonObject()
            if (prettyName != null) {
                jObj.addProperty("pretty_name", prettyName)
            } else if (imageURL != null) {
                jObj.addProperty("image_url", imageURL)
            }
            val response: Response<UserResponse?> = serverInterface.updateUser(token, jObj).execute()
            val user: UserResponse = response.body() ?: return Result.failure()
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