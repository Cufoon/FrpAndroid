package cufoon.frp.android

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File


class MainActivity : AppCompatActivity() {
    private val filename = "libfrpc.so"
    private val frpVersion = "v0.54.0"
    private val logFileName = "frpc.log"
    private val configFileName = "config.toml"

    private lateinit var stateSwitch: SwitchCompat

    private lateinit var mService: ShellService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as ShellService.LocalBinder
            mService = binder.getService()
            mBound = true
            stateSwitch.isChecked = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            stateSwitch.isChecked = false
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "frp for Android: v${versionName}\nfrp: $frpVersion"

        checkConfig()
        createBGNotificationChannel()

        mBound = isServiceRunning(ShellService::class.java)
        stateSwitch = findViewById(R.id.state_switch)
        stateSwitch.isChecked = mBound
        stateSwitch.setOnCheckedChangeListener { _, isChecked -> if (isChecked) (startShell()) else (stopShell()) }
        if (mBound) {
            val intent = Intent(this, ShellService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        setListener()
    }

    private fun setListener() {
        val configButton = findViewById<Button>(R.id.configButton)
        configButton.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }
        val aboutButton = findViewById<Button>(R.id.aboutButton)
        aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            readLog()
        }
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            val logfile = File(this.filesDir.toString() + "/$logFileName")
            Log.d("cufoon_log", logfile.absoluteFile.toString())
            logfile.delete()
            readLog()
        }
    }

    private fun readLog() {
        val files: Array<String> = this.fileList()
        Log.d("cufoon_log", files.joinToString("---"))
        val logTextView = findViewById<TextView>(R.id.logTextView)
        if (files.contains(logFileName)) {
            val mReader = this.openFileInput(logFileName).bufferedReader()
            val mRespBuff = StringBuffer()
            val buff = CharArray(1024)
            var ch: Int
            while (mReader.read(buff).also { ch = it } != -1) {
                mRespBuff.append(buff, 0, ch)
            }
            mReader.close()
            logTextView.text = mRespBuff.toString()
        } else {
            logTextView.text = "无日志"
        }
    }

    private fun checkConfig() {
        val files: Array<String> = this.fileList()
        Log.d("cufoon_log", files.joinToString(","))
        if (!files.contains(configFileName)) {
            val assetManager = resources.assets
            this.openFileOutput(configFileName, Context.MODE_PRIVATE).use {
                it.write(assetManager.open((configFileName)).readBytes())
            }
        }
    }

    private fun startShell() {
        val intent = Intent(this, ShellService::class.java)
        intent.putExtra("filename", filename)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun stopShell() {
        val intent = Intent(this, ShellService::class.java)
        unbindService(connection)
        stopService(intent)
    }

    private fun createBGNotificationChannel() {
        val name = "frp后台服务"
        val descriptionText = "frp后台服务通知"
        val importance = NotificationManager.IMPORTANCE_MIN
        val channel = NotificationChannel("shell_bg", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}