package com.vcagora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vcagora.databinding.ActivityMainBinding
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val APP_ID = "3a4b7f6387b6431197132d414a965db1"
    private val CHANNEL_NAME = "absk"
    private val token = "007eJxTYJhVNe20tbL+haWv+OK0mlsYTJQ/L+ByMuPI/jJL7P3EXZ8UGIwTTZLM08yMLcyTzEyMDQ0tzQ2NjVJMDE0SLc1MU5IMK6LXJDcEMjIwyqsxA0kwBPFZGBKTirMZGACxlBxl"
    private val uId = 0
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null
    private var localSurface: SurfaceView? = null
    private val remoteSurface: SurfaceView? = null

    private val PERMISSION_REQ_ID = 100
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
        setupVideoSDKEngine()

        binding.apply {
            JoinButton.setOnClickListener {joinChannel() }
            LeaveButton.setOnClickListener {leaveChannel() }
        }
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(msg: String) { runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)

            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurface!!.visibility = GONE }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val remoteSurface = SurfaceView(baseContext)
        remoteSurface.setZOrderMediaOverlay(true)
        binding.remoteVideoViewContainer.addView(remoteSurface)
        agoraEngine!!.setupRemoteVideo(VideoCanvas(remoteSurface, VideoCanvas.RENDER_MODE_FIT, uid))

        remoteSurface.visibility = VISIBLE
    }

    private fun joinChannel() {
        if (checkSelfPermission()) {

            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

            setupLocalVideo()
            localSurface!!.visibility = VISIBLE

            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, CHANNEL_NAME, uId, options)
        } else {
            showMessage("Permissions was not granted")}
    }

    private fun setupLocalVideo() {
        localSurface = SurfaceView(baseContext)
        binding.localVideoViewContainer.addView(localSurface)
        agoraEngine!!.setupLocalVideo(VideoCanvas(localSurface, VideoCanvas.RENDER_MODE_FIT, 0))
    }


    private fun leaveChannel() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")

            if (remoteSurface != null) remoteSurface.visibility = GONE
            if (localSurface != null) localSurface!!.visibility = GONE
            isJoined = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null }.start()
    }
}