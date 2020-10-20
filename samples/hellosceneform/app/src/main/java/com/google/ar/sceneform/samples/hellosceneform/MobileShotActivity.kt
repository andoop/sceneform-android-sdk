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

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene.OnUpdateListener
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_ux.*
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
class MobileShotActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private val viewRenderables: MutableList<ViewRenderable> = ArrayList()
    private val viewRenderables2: MutableList<ViewRenderable> = ArrayList()
    private var hasCreated = false
    private var hasStartShot = false
    private var mRootNode: Node? = null
    private val timer = Timer()
    private val soundPlayer = SoundPlayer()
    private var flashSound = 0
    private var anchorNode: AnchorNode? = null
    private var shotOnce = false //拍摄一次，首先要移除所有node = false
    private var takeOnce = false//获取一下图片，然后重新添加node = false
    private var dis = 0.5f
    private var pointsArray = mutableListOf<Map<String, Double>>()
    private var picInfoString = ""
    private var willShotName = ""

    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_ux)

        pointsArray.add(mapOf("pointCount" to 12.0, "pitchAngle" to 30.0))
        pointsArray.add(mapOf("pointCount" to 12.0, "pitchAngle" to -30.0))
        var totalPointCount = pointsArray[0]["pointCount"]!! + pointsArray[1]["pointCount"]!!

        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                arFragment!!.arSceneView.renderer!!.startMirroring(Surface(surface), 0, 0, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                arFragment!!.arSceneView.renderer!!.stopMirroring(Surface(surface))
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
        //初始化24个纹理
        for (i in 0 until totalPointCount.toInt()) {
            ViewRenderable.builder()
                    .setView(this, R.layout.layout_aim_shot)
                    .setSizer {
                        val vector3 = Vector3()
                        vector3.x = 0.055f
                        vector3.y = 0.055f
                        vector3
                    }
                    .build()
                    .thenAccept { renderable: ViewRenderable -> viewRenderables.add(renderable) }
        }
        //初始化24个 circle 纹理
        for (i in 0 until totalPointCount.toInt()) {
            ViewRenderable.builder()
                    .setView(this, R.layout.layout_shot_circle)
                    .setSizer {
                        val vector3 = Vector3()
                        vector3.x = 0.045f
                        vector3.y = 0.045f
                        vector3
                    }
                    .build()
                    .thenAccept { renderable: ViewRenderable -> viewRenderables2.add(renderable) }
        }
        arFragment!!.setOnSessionInitializationListener {

        }

        //new SurfaceTexture()

        //关闭投影
        arFragment!!.arSceneView.scene.sunlight!!.light = Light.builder(Light.Type.POINT).setShadowCastingEnabled(false).build()
        arFragment!!.arSceneView.scene.addOnUpdateListener(OnUpdateListener {
            if (arFragment!!.arSceneView.arFrame!!.camera.trackingState != TrackingState.TRACKING) {
                return@OnUpdateListener
            }
            if (!hasCreated) {
                createNodes()
                hasCreated = true
            }
            if (hasCreated) {
                updateNodes()
            }
            if (takeOnce) {
                takeOnce = false
                takePic()
                anchorNode!!.addChild(mRootNode)
            }
            if (shotOnce) {
                mRootNode!!.parent!!.removeChild(mRootNode)
                shotOnce = false
                takeOnce = true
            }
        })
        findViewById<View>(R.id.btShot).setOnClickListener { v ->
            hasStartShot = true
            v.visibility = View.GONE
            timer.star()
        }
        timer.callback = object : Timer.Callback {
            override fun onTick() {
                if (hasStartShot) {
                    tryToShot()
                }
            }
        }
    }

    private fun takePic() {
        val bitmap = textureView.bitmap
        ivPreView.setImageBitmap(bitmap)
        //保持图片到本地

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
        val eulerAngles = com.google.ar.sceneform.samples.hellosceneform.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()).ToEulerAngles()
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

        Log.e("--------", picInfoString)
    }

    private fun tryToShot() {
        val arFrame = arFragment!!.arSceneView.arFrame
        if (arFrame!!.camera.trackingState == TrackingState.TRACKING) {
            val hitTestResults = arFragment!!.arSceneView.scene.hitTestAll(arFragment!!.arSceneView.scene.camera.screenPointToRay(ScreenUtils.getScreenWidth(this) / 2.toFloat(), ScreenUtils.getScreenHeight(this) / 2.toFloat()))
            for (i in hitTestResults.indices) {
                val hitTestResult = hitTestResults[i]
                val renderable = hitTestResult.node!!.renderable
                if (renderable is ViewRenderable) {
                    val layer = renderable.view.findViewById<ImageView>(R.id.ivLayer)
                    val alpha = ObjectAnimator.ofFloat(layer, "alpha", 0f, 1f)
                    alpha.duration = 100
                    alpha.repeatCount = 1
                    alpha.start()
                }
            }
            if (hitTestResults.size == 2 && hitTestResults[0].node!!.name.endsWith("-x") && hitTestResults[1].node!!.name.endsWith("-0")) {
                for (i in hitTestResults.indices) {
                    val hitTestResult = hitTestResults[i]
                    val node = hitTestResult.node
                    if (node!!.name.endsWith("-0")) {
                        val renderable = node.renderable
                        if (renderable is ViewRenderable) {
                            val layer = renderable.view.findViewById<ImageView>(R.id.ivLayer)
                            layer.setImageResource(R.drawable.already_shot)
                            willShotName = node.name
                            node.name = node.name+"_shot"
                            soundPlayer.play(flashSound)
                            shotOnce = true
                        }
                    }
                }
            }
        }
    }

    private fun createNodes() {
        val translate = floatArrayOf(0f, 0f, 0f)
        val rotate = floatArrayOf(0f, 0f, 0f, 0f)
        val anchor = arFragment!!.arSceneView.session!!.createAnchor(Pose(translate, rotate))
        anchorNode = AnchorNode(anchor)
        anchorNode?.setParent(arFragment!!.arSceneView.scene)
        mRootNode = Node()

        for (j in 0 until pointsArray.size) {
            val pitchAngle = pointsArray[j]["pitchAngle"] ?: continue
            val pointCount = pointsArray[j]["pointCount"] ?: continue
            var angle = Math.toRadians(pitchAngle)
            val perAngle: Double = PI * 2.0f / pointCount
            for (i in 0 until pointCount.toInt()) {
                val x = dis * cos(angle) * cos(i * perAngle)
                val y = dis * sin(angle)
                val z = dis * cos(angle) * sin(i * perAngle)

                val node = Node()
                node.setParent(mRootNode)
                node.localPosition = Vector3(x.toFloat(), y.toFloat(), z.toFloat())
                val eulerAngles = EulerAngles(Math.toRadians(360 - abs(pitchAngle) * i - 90.toDouble()).toFloat(), 0F, Math.toRadians(pitchAngle).toFloat())
                val quaternion = eulerAngles.ToQuaternion()
                node.localRotation = Quaternion(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
                node.name = "${j}_${i}-0"
                node.renderable = viewRenderables[(j * pointCount + i).toInt()]


                val x1 = x * 0.7f
                val y1 = y * 0.7f
                val z1 = z * 0.7f
                val node1 = Node()
                node1.setParent(mRootNode)
                node1.localPosition = Vector3(x1.toFloat(), y1.toFloat(), z1.toFloat())
                node1.localRotation = Quaternion(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
                node1.name = "${j}_${i}-x"
                node1.renderable = viewRenderables2[(j * pointCount + i).toInt()]
            }
        }

        anchorNode?.addChild(mRootNode)

    }

    private fun updateNodes() {
        if (!hasStartShot) {
            val localPosition = arFragment!!.arSceneView.scene.camera.localPosition
            mRootNode!!.localPosition = localPosition
        }
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
        super.onDestroy()
        soundPlayer.release()
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