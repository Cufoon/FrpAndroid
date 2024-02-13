package cufoon.frp.android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {
    private val configFileName = "config.toml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val saveConfigButton = findViewById<Button>(R.id.saveConfigButton)
        saveConfigButton.setOnClickListener { saveConfig();finish() }
        val notSaveConfigButton = findViewById<Button>(R.id.notSaveConfigButton)
        notSaveConfigButton.setOnClickListener { finish() }

        readConfig()
    }

    private fun readConfig() {
        val files: Array<String> = this.fileList()
        val configEditText = findViewById<EditText>(R.id.configEditText)
        if (files.contains(configFileName)) {
            val mReader = this.openFileInput(configFileName).bufferedReader()
            val mRespBuff = StringBuffer()
            val buff = CharArray(1024)
            var ch: Int
            while (mReader.read(buff).also { ch = it } != -1) {
                mRespBuff.append(buff, 0, ch)
            }
            mReader.close()
            configEditText.setText(mRespBuff.toString())
        } else {
            configEditText.setText("")
        }
    }

    private fun saveConfig() {
        val configEditText = findViewById<EditText>(R.id.configEditText)
        this.openFileOutput(configFileName, Context.MODE_PRIVATE).use {
            it.write(configEditText.text.toString().toByteArray())
            Log.d("cufoon_log", configEditText.text.toString())
        }
    }
}