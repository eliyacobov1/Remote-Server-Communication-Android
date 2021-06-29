package postpc.ex7.ex10

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.work.*
import com.bumptech.glide.Glide
import postpc.ex7.ex10.workers.GetTokenWorker
import postpc.ex7.ex10.workers.GetUserWorker
import com.google.gson.Gson
import postpc.ex7.ex10.data.TokenResponse
import postpc.ex7.ex10.data.User
import postpc.ex7.ex10.data.UserResponse
import postpc.ex7.ex10.workers.SetUserWorker
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var welcomeText: TextView
    private lateinit var updatePrettyName: Button
    private lateinit var prettyNameText: TextView
    private lateinit var userImage: ImageView
    private lateinit var savedToken: String
    private lateinit var connect: Button
    private lateinit var usernameInput: EditText
    private var sp: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize UI
        usernameInput = findViewById(R.id.username)
        connect = findViewById(R.id.connect)
        prettyNameText = findViewById(R.id.new_name)
        prettyNameText.visibility = View.INVISIBLE
        updatePrettyName = findViewById(R.id.change_name)
        updatePrettyName.visibility = View.INVISIBLE
        welcomeText = findViewById(R.id.welcome)
        welcomeText.visibility = View.INVISIBLE
        userImage = findViewById(R.id.user_image)
        userImage.visibility = View.INVISIBLE
        requestPermission()
//        sp = getPreferences(MODE_PRIVATE)
//        savedToken = sp.getString(TOKEN, null)
//        if (savedToken != null) {
//            userInfo
//        }
        connect.setOnClickListener {
            getUserToken(usernameInput.text.toString())
        }
        updatePrettyName.setOnClickListener { updateUser(prettyNameText.text.toString()) }
    }

    private fun getUserToken(username: String) {
        val id = UUID.randomUUID()
        val checkConnectivityWork = OneTimeWorkRequest.Builder(GetTokenWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(Data.Builder().putString("user_name", username).build())
            .addTag(id.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(checkConnectivityWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(id.toString()).observe(this,
                Observer {
                    if (it == null || it.isEmpty()) return@Observer
                    val info = it[0]
                    val tokenAsJson = info.outputData.getString("output")
                    if (tokenAsJson == null || tokenAsJson == "") {
                        return@Observer
                    }
                    val token: TokenResponse = Gson().fromJson(tokenAsJson, TokenResponse::class.java)
                    if (token.data == "") {
                        return@Observer
                    }
                    saveToken(token.data)
                    userInfo()
                })
    }

    private fun toggleConnectionUI() {
        usernameInput.visibility = View.INVISIBLE
        connect.visibility = View.INVISIBLE
    }

    private fun toggleLoadingStatus(show: Boolean) {
        val load = findViewById<LinearLayout>(R.id.loading_progress)
        if (show) {
            load.visibility = View.VISIBLE
        } else {
            load.visibility = View.INVISIBLE
        }
    }

    private fun highLightImage(id: Int) {
        (findViewById<View>(R.id.image_alien) as ImageView).setColorFilter(Color.argb(0, 0, 0, 0))
        (findViewById<View>(R.id.image_crab) as ImageView).setColorFilter(Color.argb(0, 0, 0, 0))
        (findViewById<View>(R.id.image_frog) as ImageView).setColorFilter(Color.argb(0, 0, 0, 0))
        (findViewById<View>(R.id.image_octopus) as ImageView).setColorFilter(Color.argb(0, 0, 0, 0))
        (findViewById<View>(R.id.image_robot) as ImageView).setColorFilter(Color.argb(0, 0, 0, 0))
        (findViewById<View>(R.id.image_unicorn) as ImageView).setColorFilter(Color.argb(0, 0, 0, 0))
        (findViewById<View>(id) as ImageView).setColorFilter(Color.argb(75, 0, 0, 0))
    }

    private fun toggleImageLayout() {
        Glide.with(this).load(URL + IMG_ALIEN)
            .into(findViewById<View>(R.id.image_alien) as ImageView)
        Glide.with(this).load(URL + IMG_CRAB).into(findViewById<View>(R.id.image_crab) as ImageView)
        Glide.with(this).load(URL + IMG_FROG).into(findViewById<View>(R.id.image_frog) as ImageView)
        Glide.with(this).load(URL + IMG_OCTOPUS)
            .into(findViewById<View>(R.id.image_octopus) as ImageView)
        Glide.with(this).load(URL + IMG_ROBOT)
            .into(findViewById<View>(R.id.image_robot) as ImageView)
        Glide.with(this).load(URL + IMG_UNICORN)
            .into(findViewById<View>(R.id.image_unicorn) as ImageView)
        findViewById<View>(R.id.image_alien).setOnClickListener {
            highLightImage(R.id.image_alien)
            updateImageUrl(IMG_ALIEN)
        }
        findViewById<View>(R.id.image_crab).setOnClickListener {
            highLightImage(R.id.image_crab)
            updateImageUrl(IMG_CRAB)
        }
        findViewById<View>(R.id.image_frog).setOnClickListener {
            highLightImage(R.id.image_frog)
            updateImageUrl(IMG_FROG)
        }
        findViewById<View>(R.id.image_octopus).setOnClickListener {
            highLightImage(R.id.image_octopus)
            updateImageUrl(IMG_OCTOPUS)
        }
        findViewById<View>(R.id.image_robot).setOnClickListener {
            highLightImage(R.id.image_robot)
            updateImageUrl(IMG_ROBOT)
        }
        findViewById<View>(R.id.image_unicorn).setOnClickListener {
            highLightImage(R.id.image_unicorn)
            updateImageUrl(IMG_UNICORN)
        }
    }

    private fun saveToken(token: String) {
        savedToken = token
        val editor = sp!!.edit()
        editor.putString(TOKEN, token)
        editor.apply()
    }

    private fun userInfo() {
        toggleConnectionUI()
        toggleLoadingStatus(true)
        user()
    }

    private fun user() {
        val workTagUniqueId = UUID.randomUUID()
        val getUserWork = OneTimeWorkRequest.Builder(GetUserWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(Data.Builder().putString("token", savedToken).build())
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getUserWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString()).observe(this,
                Observer {
                    if (it == null || it.isEmpty()) return@Observer
                    val info = it[0]
                    val userResponseAsJson = info.outputData.getString("output")
                    if (userResponseAsJson == null || userResponseAsJson == "") {
                        return@Observer
                    }
                    val userResponse: UserResponse =
                        Gson().fromJson(userResponseAsJson, UserResponse::class.java)
                    val user: User = userResponse.data
                    toggleLoadingStatus(false)
                    showUserInfo(user)
                })
        }

    private fun showUserInfo(user: User) {
        val name: String? = if (user.prettyName == null || user.prettyName.equals("")) user.name.toString() else user.prettyName
        welcomeText.text = String.format(WELCOME_MSG, name)
        welcomeText.visibility = View.VISIBLE
        updatePrettyName.visibility = View.VISIBLE
        prettyNameText.visibility = View.VISIBLE
        Glide.with(this).load(URL + user.imageURL).into(userImage)
        userImage.visibility = View.VISIBLE
        toggleImageLayout()
    }

    private fun updateUser(prettyName: String) {
        val workTagUniqueId = UUID.randomUUID()
        val updateUserWork = OneTimeWorkRequest.Builder(SetUserWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(
                Data.Builder()
                    .putString("token", savedToken)
                    .putString("image_url", null)
                    .putString("pretty_name", prettyName).build()
            )
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(updateUserWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString()).observe(this,
                Observer {
                    if (it == null || it.isEmpty()) return@Observer
                    val info = it[0]
                    if (info.state == WorkInfo.State.FAILED) {
                        Toast.makeText(applicationContext, "Could not Update User Info", Toast.LENGTH_SHORT)
                            .show()
                        return@Observer
                    }

                    val userResponseAsJson = info.outputData.getString("output")
                    if (userResponseAsJson == null || userResponseAsJson == "") {
                        return@Observer
                    }
                    Log.d("ex7Tag", "got user: $userResponseAsJson")
                    val userResponse: UserResponse =
                        Gson().fromJson(userResponseAsJson, UserResponse::class.java)
                    val user: User = userResponse.data
                    Log.d("ex7Tag", "got user: $user")
                    toggleLoadingStatus(false)
                    showUserInfo(user)
                })
    }

    private fun updateImageUrl(url: String) {
        toggleLoadingStatus(true)
        val workTagUniqueId = UUID.randomUUID()
        val setUserImageUrl = OneTimeWorkRequest.Builder(SetUserWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(
                Data.Builder().putString("key_token", savedToken)
                    .putString("key_image_url", url)
                    .build()
            )
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(this).enqueue(setUserImageUrl)
        WorkManager.getInstance(this).getWorkInfosByTagLiveData(workTagUniqueId.toString())
            .observe(this,
                Observer {
                    if (it == null || it.isEmpty()) return@Observer
                    val info = it[0]
                    if (info.state == WorkInfo.State.FAILED) {
                        Toast.makeText(applicationContext, "Failed updating user",
                            Toast.LENGTH_SHORT).show()
                        return@Observer
                    }

                    val userResponseAsJson = info.outputData.getString("key_output_user")
                    if (userResponseAsJson == null || userResponseAsJson == "") {
                        return@Observer
                    }
                    Log.d("ex7Tag", "got user: $userResponseAsJson")
                    val userResponse: UserResponse =
                        Gson().fromJson(userResponseAsJson, UserResponse::class.java)
                    val user: User = userResponse.data
                    Log.d("ex7Tag", "got user: $user")
                    toggleLoadingStatus(false)
                    showUserInfo(user)
                })
    }

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 100)
        }
    }

    companion object {
        private const val URL = "https://hujipostpc2019.pythonanywhere.com/"
        private const val WELCOME_MSG = "Welcome Back,\n %s!"
        private const val IMG_CRAB = "/images/crab.png"
        private const val IMG_UNICORN = "/images/unicorn.png"
        private const val IMG_ALIEN = "/images/alien.png"
        private const val IMG_ROBOT = "/images/robot.png"
        private const val IMG_OCTOPUS = "/images/octopus.png"
        private const val IMG_FROG = "/images/frog.png"
        private const val TOKEN = "key_token"
    }
}