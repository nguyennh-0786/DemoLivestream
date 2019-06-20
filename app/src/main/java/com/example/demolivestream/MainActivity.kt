package com.example.demolivestream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtplibrary.rtsp.RtspCamera1
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), ConnectCheckerRtsp, SurfaceHolder.Callback {

    private lateinit var rtspCamera: RtspCamera1
    private lateinit var filePath: File
    private var currentMillis: String = ""
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

        filePath = File(Environment.getExternalStorageDirectory().absolutePath.plus(PREFIX_FILE_PATH))
        rtspCamera = RtspCamera1(cameraView, this)
        rtspCamera.setReTries(RETRY_COUNT)
        cameraView.holder.addCallback(this)

        btn_live.setOnClickListener {
            if (rtspCamera.isStreaming) {
                btn_live.setText(R.string.start_live)
                rtspCamera.stopStream()
            } else {
                if (rtspCamera.isRecording || rtspCamera.prepareAudio() && rtspCamera.prepareVideo()) {
                    btn_live.setText(R.string.stop_live)
                    rtspCamera.startStream(URL_LIVE_STREAM.plus(STREAM_NAME))
                } else {
                    Toast.makeText(
                        this, "Error preparing stream, This device cant do it",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btn_switch_camera.setOnClickListener {
            try {
                rtspCamera.switchCamera()
            } catch (e: CameraOpenException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }

        }

        btn_record.setOnClickListener {
            if (!rtspCamera.isRecording) {
                try {
                    if (!filePath.exists()) {
                        filePath.mkdir()
                    }
                    currentMillis = System.currentTimeMillis().toString()
                    if (!rtspCamera.isStreaming) {
                        if (rtspCamera.prepareAudio() && rtspCamera.prepareVideo()) {
                            rtspCamera.startRecord(
                                filePath.absolutePath
                                        + "/"
                                        + currentMillis
                                        + ".mp4"
                            )
                            btn_record.setText(R.string.stop_record_video)
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(
                                this,
                                "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        rtspCamera.startRecord(
                            (filePath.absolutePath
                                    + "/"
                                    + currentMillis
                                    + ".mp4")
                        )
                        btn_record.setText(R.string.stop_record_video)
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    rtspCamera.stopRecord()
                    btn_record.setText(R.string.start_record_video)
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }

            } else {
                rtspCamera.stopRecord()
                btn_record.setText(R.string.start_record_video)
                Toast.makeText(
                    this, ("file "
                            + currentMillis
                            + ".mp4 saved in "
                            + filePath.absolutePath), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onAuthErrorRtsp() {
        runOnUiThread {
            Toast.makeText(this, "onAuthErrorRtsp", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this, "onConnectionSuccessRtsp", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnectRtsp() {
        runOnUiThread {
            Toast.makeText(this, "onDisconnectRtsp", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this, "onAuthSuccessRtsp", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtsp(reason: String?) {
        runOnUiThread {
            Toast.makeText(this, "onConnectionFailedRtsp: $reason", Toast.LENGTH_SHORT).show()
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        Toast.makeText(this, "surfaceChanged", Toast.LENGTH_SHORT).show()
        rtspCamera.startPreview()
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        Toast.makeText(this, "surfaceDestroyed", Toast.LENGTH_SHORT).show()
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        Toast.makeText(this, "surfaceCreated", Toast.LENGTH_SHORT).show()
    }

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, permission)) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        const val URL_LIVE_STREAM = "rtsp://f41209.entrypoint.cloud.wowza.com/app-2646/"
        const val STREAM_NAME = "d7ce5c25"
        const val PREFIX_FILE_PATH = "/demo_live_stream"
        const val RETRY_COUNT = 10
    }
}
