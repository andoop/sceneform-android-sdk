package com.google.ar.sceneform.samples.hellosceneform

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.google.ar.sceneform.samples.dice.DiceActivity
import com.google.ar.sceneform.samples.modeltest.ModelActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btShot).setOnClickListener {
            startActivity(Intent(this@MainActivity,HelloSceneformActivity::class.java))
        }
        findViewById<Button>(R.id.btModel).setOnClickListener {
            startActivity(Intent(this@MainActivity,DiceActivity::class.java))
        }
    }
}