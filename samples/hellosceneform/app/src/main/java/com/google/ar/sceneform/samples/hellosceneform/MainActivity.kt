package com.google.ar.sceneform.samples.hellosceneform

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.samples.utils.CommonUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btShot).setOnClickListener {
            startActivity(Intent(this@MainActivity, MobileShotActivity::class.java))
        }
        findViewById<Button>(R.id.btModel).setOnClickListener {
            startActivity(Intent(this@MainActivity, PlyModelActivity::class.java))
        }
        CommonUtils.copyAssetsDirToSDCard(this, "plys", filesDir.absolutePath)
    }
}