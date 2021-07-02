package postpc.postpc.ex10

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.work.*
import com.bumptech.glide.Glide
import postpc.postpc.ex10.workers.GetTokenWorker
import postpc.postpc.ex10.workers.GetUserWorker
import com.google.gson.Gson
import postpc.postpc.ex10.data.TokenResponse
import postpc.postpc.ex10.data.User
import postpc.postpc.ex10.data.UserResponse
import postpc.postpc.ex10.workers.SetUserWorker
import postpc.postpc.ex10.server.Paths
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var welcomeText: TextView
    private lateinit var updatePrettyName: Button
    private lateinit var prettyNameText: TextView
    private lateinit var userImage: ImageView
    private var savedToken: String? = null
    private lateinit var connectButton: Button
    private lateinit var usernameInput: EditText
    private var sp: SharedPreferences? = null

    /**
     *  this method hides the on-screen keyboard
     */
    private fun hideSoftKeyboard(view: View) {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     *  this method updates the current user's image or pretty-name
     */
    private fun updateUser(prettyName: String?=null, url: String?=null) {
        val workTagUniqueId = UUID.randomUUID()
        val updateUserWork = OneTimeWorkRequest.Builder(SetUserWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(
                Data.Builder()
                    .putString("token", savedToken)
                    .putString("image_url", url)
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
                    val userResponse: UserResponse =
                        Gson().fromJson(userResponseAsJson, UserResponse::class.java)
                    val user: User = userResponse.data
                    toggleLoadingStatus(false)
                    showUserInfo(user)
                })
    }

    /**
     *  renders all image buttons
     */
    private fun loadImages() {
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_ALIEN)
            .into(findViewById<View>(R.id.image_alien) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_CRAB)
            .into(findViewById<View>(R.id.image_crab) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_FROG)
            .into(findViewById<View>(R.id.image_frog) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_OCTOPUS)
            .into(findViewById<View>(R.id.image_octopus) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_ROBOT)
            .into(findViewById<View>(R.id.image_robot) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_UNICORN)
            .into(findViewById<View>(R.id.image_unicorn) as ImageView)
        findViewById<View>(R.id.image_alien).setOnClickListener {
            updateUser(url=Paths.IMG_ALIEN)
        }
        findViewById<View>(R.id.image_crab).setOnClickListener {
            updateUser(url=Paths.IMG_CRAB)
        }
        findViewById<View>(R.id.image_frog).setOnClickListener {
            updateUser(url=Paths.IMG_FROG)
        }
        findViewById<View>(R.id.image_octopus).setOnClickListener {
            updateUser(url=Paths.IMG_OCTOPUS)
        }
        findViewById<View>(R.id.image_robot).setOnClickListener {
            updateUser(url=Paths.IMG_ROBOT)
        }
        findViewById<View>(R.id.image_unicorn).setOnClickListener {
            updateUser(url=Paths.IMG_UNICORN)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize UI
        usernameInput = findViewById(R.id.username)
        connectButton = findViewById(R.id.connect)
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
//            userInfo()
//        }

        /* define on click listeners */
        connectButton.setOnClickListener { getUserToken(usernameInput.text.toString()) }
        updatePrettyName.setOnClickListener { updateUser(prettyName=prettyNameText.text.toString()) }
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
                    val tokenAsJson = it[0].outputData.getString("output")
                    if (tokenAsJson == null || tokenAsJson == "") {
                        return@Observer
                    }
                    val token: TokenResponse = Gson().fromJson(tokenAsJson, TokenResponse::class.java)
                    if (token.data == "") {
                        return@Observer
                    }
                    saveToken(token.data)
                    user()
                })
    }

    private fun toggleLoadingStatus(show: Boolean) {
        val load = findViewById<LinearLayout>(R.id.loading_progress)
        if (show) {
            load.visibility = View.VISIBLE
        } else {
            load.visibility = View.INVISIBLE
        }
    }

    private fun user() {
        usernameInput.visibility = View.INVISIBLE
        connectButton.visibility = View.INVISIBLE
        toggleLoadingStatus(true)
        val id =  UUID.randomUUID()
        val getUserWork = OneTimeWorkRequest.Builder(GetUserWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(Data.Builder().putString("token", savedToken).build())
            .addTag(id.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getUserWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(id.toString()).observe(this,
                Observer {
                    if (it == null || it.isEmpty()) return@Observer
                    val userResponseAsJson = it[0].outputData.getString("output")
                    if (userResponseAsJson == null || userResponseAsJson == "") {
                        return@Observer
                    }
                    val userResponse: UserResponse = Gson().fromJson(userResponseAsJson, UserResponse::class.java)
                    val user: User = userResponse.data
                    toggleLoadingStatus(false)
                    showUserInfo(user)
                })
    }

    private fun showUserInfo(user: User) {
        val name: String? = if (user.pretty_name == null || user.pretty_name.equals("")) user.username.toString() else user.pretty_name
        welcomeText.text = String.format(Paths.WELCOME_MSG, name)
        welcomeText.visibility = View.VISIBLE
        updatePrettyName.visibility = View.VISIBLE
        prettyNameText.visibility = View.VISIBLE
        Glide.with(this).load(Paths.BASE_URL + user.image_url).into(userImage)
        Glide.with(this).load(Paths.BASE_URL + user.image_url).into(userImage)
        userImage.visibility = View.VISIBLE
        loadImages()
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

    private fun saveToken(token: String) {
        savedToken = token
//        val editor = sp.edit()
//        editor.putString(TOKEN, token)
//        editor.apply()
    }
}