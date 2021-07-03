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
    private lateinit var prettyNameInput: TextView
    private lateinit var userImage: ImageView
    private var currToken: String? = null
    private lateinit var connectButton: Button
    private lateinit var userNameInput: EditText
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
                    .putString("token", currToken)
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
                    toggleLoading(false)
                    renderUserInfo(user)
                })
    }

    /**
     * this methods sends get requests for users and tokens
     */
    private fun getUserOrTokenRequest(username: String?=null) {
        if(username == ""){
            Toast.makeText(applicationContext, "Please enter a valid username!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if(username == null){
            userNameInput.visibility = View.INVISIBLE
            connectButton.visibility = View.INVISIBLE
            toggleLoading(true)
        }
        val id = UUID.randomUUID()
        val workerType =  if (username == null) GetUserWorker::class.java else GetTokenWorker::class.java
        val getRequestWork = OneTimeWorkRequest.Builder(workerType)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(Data.Builder()
                .putString("user_name", username)
                .putString("token", currToken)
                .build())
            .addTag(id.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getRequestWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(id.toString()).observe(this,
                Observer {
                    if (it == null || it.isEmpty()) return@Observer
                    val res = it[0].outputData.getString("output")
                    if (res == null || res == "") {
                        return@Observer
                    }
                    if(username == null){
                        val userResponse: UserResponse = Gson().fromJson(res, UserResponse::class.java)
                        val user: User = userResponse.data
                        toggleLoading(false)
                        renderUserInfo(user)
                    }
                    else {
                        val token: TokenResponse = Gson().fromJson(res, TokenResponse::class.java)
                        if (token.data == "") {
                            return@Observer
                        }
                        saveToken(token.data)
                        getUserOrTokenRequest()
                    }
                })
    }

    /**
     *  renders all image buttons
     */
    private fun loadImages() {
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_ALIEN)
            .into(findViewById<View>(R.id.alien_img) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_CRAB)
            .into(findViewById<View>(R.id.crab_img) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_FROG)
            .into(findViewById<View>(R.id.frog_img) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_OCTOPUS)
            .into(findViewById<View>(R.id.octopus_img) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_ROBOT)
            .into(findViewById<View>(R.id.robot_img) as ImageView)
        Glide.with(this).load(Paths.BASE_URL + Paths.IMG_UNICORN)
            .into(findViewById<View>(R.id.unicorn_img) as ImageView)
        findViewById<View>(R.id.alien_img).setOnClickListener {
            updateUser(url=Paths.IMG_ALIEN)
        }
        findViewById<View>(R.id.crab_img).setOnClickListener {
            updateUser(url=Paths.IMG_CRAB)
        }
        findViewById<View>(R.id.frog_img).setOnClickListener {
            updateUser(url=Paths.IMG_FROG)
        }
        findViewById<View>(R.id.octopus_img).setOnClickListener {
            updateUser(url=Paths.IMG_OCTOPUS)
        }
        findViewById<View>(R.id.robot_img).setOnClickListener {
            updateUser(url=Paths.IMG_ROBOT)
        }
        findViewById<View>(R.id.unicorn_img).setOnClickListener {
            updateUser(url=Paths.IMG_UNICORN)
        }
    }

    private fun renderUserInfo(user: User) {
        val name: String? = if (user.pretty_name == null || user.pretty_name.equals("")) user.username.toString() else user.pretty_name
        welcomeText.text = String.format(Paths.WELCOME_MSG, name)
        welcomeText.visibility = View.VISIBLE
        updatePrettyName.visibility = View.VISIBLE
        prettyNameInput.visibility = View.VISIBLE
        Glide.with(this).load(Paths.BASE_URL + user.image_url).into(userImage)
        Glide.with(this).load(Paths.BASE_URL + user.image_url).into(userImage)
        userImage.visibility = View.VISIBLE
        loadImages()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize UI
        userNameInput = findViewById(R.id.username)
        connectButton = findViewById(R.id.connect)
        prettyNameInput = findViewById(R.id.new_name)
        prettyNameInput.visibility = View.INVISIBLE
        updatePrettyName = findViewById(R.id.change_name)
        updatePrettyName.visibility = View.INVISIBLE
        welcomeText = findViewById(R.id.welcome)
        welcomeText.visibility = View.INVISIBLE
        userImage = findViewById(R.id.img)
        userImage.visibility = View.INVISIBLE
        requestPermission()

        /* define on-click listeners */
        connectButton.setOnClickListener {
            userNameInput.clearFocus()
            hideSoftKeyboard(userNameInput)
            getUserOrTokenRequest(username=userNameInput.text.toString())
        }
        updatePrettyName.setOnClickListener {
            prettyNameInput.clearFocus()
            hideSoftKeyboard(prettyNameInput)
            updateUser(prettyName=prettyNameInput.text.toString())
        }
    }

    private fun toggleLoading(isVisible: Boolean) {
        val loadingElement = findViewById<LinearLayout>(R.id.loading)
        loadingElement.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 100)
        }
    }

    private fun saveToken(token: String) {
        currToken = token
    }
}