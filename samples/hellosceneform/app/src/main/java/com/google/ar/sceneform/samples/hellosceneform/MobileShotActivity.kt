/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.Scene.OnUpdateListener
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.samples.scene.MobileShotScene
import com.google.ar.sceneform.samples.utils.BitmapUtils
import com.google.ar.sceneform.samples.utils.FileUtils
import com.google.ar.sceneform.samples.utils.ScreenUtils
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_ux.*
import java.io.File
import kotlin.math.abs

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
class MobileShotActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var hasStartShot = false
    private val timer = Timer()
    private val soundPlayer = SoundPlayer()
    private var flashSound = 0
    private var picInfoString = ""
    private var shotCount = 0
    private var filePath = ""
    private var imageFolderPath = ""
    private var imageFolderHdPath = ""
    private var cachedSurface: Surface? = null
    private var mobileShotScene: MobileShotScene? = null
    private var opt = 0.0


    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_ux)
        BitmapUtils.init(applicationContext, this)
        filePath = filesDir.absolutePath + File.separator + "project/test"
        imageFolderPath = filePath + File.separator + "tmp_source_material"
        imageFolderHdPath = filePath + File.separator + "tmp_source_high"
        FileUtils.deleteDirection(File(imageFolderPath))
        FileUtils.deleteDirection(File(imageFolderHdPath))
        FileUtils.createFolder(imageFolderPath)
        FileUtils.createFolder(imageFolderHdPath)

        //控制 surface 宽度为屏幕宽度,设置 surface 高宽比为 1.333
        layoutBottom.layoutParams.height = ScreenUtils.getScreenHeight(this) - (ScreenUtils.getScreenWidth(this) * 1.333f).toInt()
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                mobileShotScene?.onSurfaceSizeChanged(width, height)
                cachedSurface = Surface(surface)
                arFragment!!.arSceneView.renderer!!.startMirroring(cachedSurface, 0, 0, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?

        if (arFragment!!.planeDiscoveryController != null) {
            arFragment!!.planeDiscoveryController.hide()
            arFragment!!.planeDiscoveryController.setInstructionView(null)
        }
        flashSound = soundPlayer.preLoad(this, R.raw.flash)

        //去除平面纹理
        arFragment!!.arSceneView.planeRenderer.isEnabled = false
        //关闭投影
        arFragment!!.arSceneView.scene.sunlight!!.light = Light.builder(Light.Type.POINT).setShadowCastingEnabled(false).build()
        arFragment!!.arSceneView.scene.addOnUpdateListener(OnUpdateListener {
            lifecycleScope.launchWhenStarted {
                arFrameUpdate()
            }
        })

        showShotTip()
        initViews()
        initShotScene()
    }

    private fun initViews() {
        findViewById<View>(R.id.btShot).setOnClickListener { v ->
            hasStartShot = true
            v.visibility = View.GONE
            tvProgress.visibility = View.VISIBLE
            tvProgress.text = "拍摄进度 $shotCount/${mobileShotScene?.totalPointCount ?: 0}"
            dismissTipCard()
            timer.star()
            mobileShotScene?.startShot()
        }
        timer.callback = object : Timer.Callback {
            override fun onTick() {
                if (hasStartShot && !isShowTip()) {
                    mobileShotScene?.tryToShot()
                }
            }
        }
        btBack.setOnClickListener {
            finish()
        }
        videoGuide.setOnClickListener {

        }
        arGuide.setOnClickListener {

        }
    }

    private fun initShotScene() {
        mobileShotScene = MobileShotScene.Builder()
                .arFragment(arFragment)
                .pointNum(24)
                .pitchAngle(30)
                .shotLayoutRes(R.layout.layout_aim_shot)
                .shotAimLayoutRes(R.layout.layout_shot_circle)
                .leanCall {
                    onLean(it)
                }
                .positionCall {
                    onDistanceChange(it)
                }
                .shotCall {
                    takePic(it)
                }
                .build()
        mobileShotScene?.init(this)
    }

    private fun onDistanceChange(dis: Double) {
        if (dis > 0.02) {
            showPositionTip()
        } else if (opt > 0) {
            dismissTipCard()
        }
    }

    private fun onLean(rotate: Double) {
        opt = if (rotate > 15 || rotate < -15) {
            0.0
        } else {
            1 - (abs(rotate) / 15);
        }

        if (abs(rotate) > 15) {
            pieProgress.setPieColor(Color.parseColor("#ff0000"))
        } else {
            pieProgress.setPieColor(Color.parseColor("#0000ff"))
        }
        pieProgress.progress = ((rotate * 100 / 360).toInt())
        if (opt <= 0) {
            showLeanTip()
        } else {
            dismissTipCard()
        }
    }

    private fun arFrameUpdate() {
        if (arFragment!!.arSceneView.arFrame!!.camera.trackingState != TrackingState.TRACKING) {
            return
        }
        mobileShotScene?.onFrameUpdate()
    }

    private fun isShowTip(): Boolean {
        return tipCard.visibility == View.VISIBLE
    }

    //倾斜提示
    private fun showLeanTip() {
        tipCard.visibility = View.VISIBLE
        tipCard.setCardBackgroundColor(Color.parseColor("#d97635"))
        tvTip.text = "请保持手机竖直拍摄"
    }

    //位置偏移提示
    private fun showPositionTip() {
        tipCard.visibility = View.VISIBLE
        tipCard.setCardBackgroundColor(Color.parseColor("#d97635"))
        tvTip.text = "保持手机位置不变，拍摄者围绕手机转动"
    }

    private fun showShotTip() {
        tipCard.visibility = View.VISIBLE
    }

    private fun dismissTipCard() {
        tipCard.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (hasStartShot) {
            timer.star()
        }
    }

    override fun onPause() {
        super.onPause()
        timer.stop()
    }

    override fun onDestroy() {
        soundPlayer.release()
        cachedSurface?.let {
            arFragment!!.arSceneView.renderer!!.stopMirroring(cachedSurface)
        }
        super.onDestroy()
    }

    private fun takePic(willShotName: String) {
        soundPlayer.play(flashSound)
        shotCount++
        tvProgress.text = "拍摄进度 $shotCount/${mobileShotScene?.totalPointCount ?: 0}"
        val bitmap = textureView.bitmap
        ivPreView.setImageBitmap(bitmap)
        //保持图片到本地
        lifecycleScope.launchWhenStarted {
            var imageHdPath = imageFolderHdPath + File.separator + "$willShotName.jpg"
            BitmapUtils.saveFile(imageHdPath, bitmap)

            var imagePath = imageFolderPath + File.separator + "$willShotName.jpg"
            var height = 240
            var width = bitmap.width * height / bitmap.height
            BitmapUtils.saveFile(imagePath, Bitmap.createScaledBitmap(bitmap, width, height, false))
        }

        var camera = arFragment?.arSceneView?.arFrame?.camera ?: return
        val pose = camera.pose
        var viewMatrix = FloatArray(16)
        var projectMatrix = FloatArray(16)

        camera.getViewMatrix(viewMatrix, 0)
        camera.getViewMatrix(projectMatrix, 0)

        if (TextUtils.isEmpty(picInfoString)) {
            //增加相机参数
            picInfoString = "imageResolution\n" +
                    "${bitmap.width} ${bitmap.height}\n" +
                    "intrinsics\n" +
                    "${viewMatrix[0]} ${viewMatrix[1]} ${viewMatrix[2]}\n" +
                    "${viewMatrix[4]} ${viewMatrix[5]} ${viewMatrix[6]}\n" +
                    "${viewMatrix[8]} ${viewMatrix[9]} ${viewMatrix[10]}\n" +
                    "${viewMatrix[12]} ${viewMatrix[13]} ${viewMatrix[14]}\n"
        }
        val eulerAngles = com.google.ar.sceneform.samples.hellosceneform.Quaternion(pose.qw(), pose.qx(), pose.qy(), pose.qz()).ToEulerAngles()
        picInfoString += "$willShotName\n" +
                "${pose.tx()} ${pose.ty()} ${pose.tz()} ${eulerAngles.pitch} ${eulerAngles.yaw} ${eulerAngles.roll}\n" +
                "${projectMatrix[0]} ${projectMatrix[1]} ${projectMatrix[2]} ${projectMatrix[3]}\n" +
                "${projectMatrix[4]} ${projectMatrix[5]} ${projectMatrix[6]} ${projectMatrix[7]}\n" +
                "${projectMatrix[8]} ${projectMatrix[9]} ${projectMatrix[10]} ${projectMatrix[11]}\n" +
                "${projectMatrix[12]} ${projectMatrix[13]} ${projectMatrix[14]} ${projectMatrix[15]}\n" +
                "${viewMatrix[0]} ${viewMatrix[1]} ${viewMatrix[2]}\n" +
                "${viewMatrix[4]} ${viewMatrix[5]} ${viewMatrix[6]}\n" +
                "${viewMatrix[8]} ${viewMatrix[9]} ${viewMatrix[10]}\n" +
                "${viewMatrix[12]} ${viewMatrix[13]} ${viewMatrix[14]}\n"

        if (shotCount >= mobileShotScene!!.totalPointCount) {
            Toast.makeText(this, "拍摄完成", Toast.LENGTH_SHORT).show()
            //将 info 写入文件
            FileUtils.writeFile(picInfoString, imageFolderHdPath + File.separator + "info.txt", false)
        }
    }

    companion object {
        private val TAG = MobileShotActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0

        /**
         * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
         * on this device.
         *
         *
         * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
         *
         *
         * Finishes the activity if Sceneform can not run
         */
        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show()
                activity.finish()
                return false
            }
            return true
        }
    }
}