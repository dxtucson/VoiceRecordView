package com.example.voicerecordview

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    lateinit var start: Button
    lateinit var stop: Button
    lateinit var voiceRecordView: VoiceRecordView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start = findViewById(R.id.start_button)
        stop = findViewById(R.id.stop_button)
        voiceRecordView = findViewById(R.id.voiceRecordView)

        start.setOnClickListener {
            if (voiceRecordView.tag == null || voiceRecordView.tag == "Stopped") {
                val result = checkCallingOrSelfPermission("android.permission.RECORD_AUDIO")
                if (result == PackageManager.PERMISSION_GRANTED) {
                    voiceRecordView.startRecord()
                    voiceRecordView.tag = "Started"
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 123)
                }
            }
        }

        stop.setOnClickListener {
            if (voiceRecordView.tag == "Started") {
                voiceRecordView.stopRecord()
                //voiceRecordView.tag = "Stopped"
            }
        }
    }
}
