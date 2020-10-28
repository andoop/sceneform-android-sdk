package com.google.ar.sceneform.samples.scene

import android.animation.ObjectAnimator
import android.content.Context
import android.widget.ImageView
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.samples.hellosceneform.EulerAngles
import com.google.ar.sceneform.samples.hellosceneform.R
import com.google.ar.sceneform.ux.ArFragment
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MobileShotScene private constructor() {
    private val viewRenderables: MutableList<ViewRenderable> = ArrayList()
    private val viewRenderables2: MutableList<ViewRenderable> = ArrayList()
    private var pointsArray = mutableListOf<Map<String, Int>>()
    var totalPointCount = 0
    private var shotLayoutRes = -1
    private var shotAimLayoutRes = -1
    private var hasCreated = false
    private var dis = 0.5f
    private var arFragment: ArFragment? = null
    private var mRootNode: Node? = null
    private var anchorNode: AnchorNode? = null
    private var hasStartShot = false
    private var willShotName = ""
    private var shotOnce = false //拍摄一次，首先要移除所有node = false
    private var takeOnce = false//获取一下图片，然后重新添加node = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var leanCallback: ((rotate: Double) -> Unit)? = null
    private var positionCallback: ((distance: Double) -> Unit)? = null
    private var shotCallback: ((name: String) -> Unit)? = null

    fun init(context: Context) {
        for (i in 0 until totalPointCount.toInt()) {
            ViewRenderable.builder()
                    .setView(context, shotLayoutRes)
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
                    .setView(context, shotAimLayoutRes)
                    .setSizer {
                        val vector3 = Vector3()
                        vector3.x = 0.045f
                        vector3.y = 0.045f
                        vector3
                    }
                    .build()
                    .thenAccept { renderable: ViewRenderable -> viewRenderables2.add(renderable) }
        }
    }

    fun onFrameUpdate() {
        if (!hasCreated) {
            createNodes()
            hasCreated = true
        }
        if (hasCreated) {
            updateNodes()
        }

        if (hasStartShot) {
            tryToTip()
        }
        if (takeOnce) {
            takeOnce = false
            shotCallback?.invoke(willShotName)
            anchorNode!!.addChild(mRootNode)
        }
        if (shotOnce) {
            mRootNode!!.parent!!.removeChild(mRootNode)
            shotOnce = false
            takeOnce = true
        }
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    fun startShot() {
        hasStartShot = true
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
            var angle = Math.toRadians(pitchAngle.toDouble())
            val perAngle: Double = PI * 2.0f / pointCount
            for (i in 0 until pointCount.toInt()) {
                val x = dis * cos(angle) * cos(i * perAngle)
                val y = dis * sin(angle)
                val z = dis * cos(angle) * sin(i * perAngle)

                val node = Node()
                node.setParent(mRootNode)
                node.localPosition = Vector3(x.toFloat(), y.toFloat(), z.toFloat())
                val eulerAngles = EulerAngles(Math.toRadians(360 - abs(pitchAngle) * i - 90.toDouble()).toFloat(), 0F, Math.toRadians(pitchAngle.toDouble()).toFloat())
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

    fun tryToShot() {
        val arFrame = arFragment!!.arSceneView.arFrame
        if (arFrame!!.camera.trackingState == TrackingState.TRACKING) {
            val hitTestResults = arFragment!!.arSceneView.scene.hitTestAll(arFragment!!.arSceneView.scene.camera.screenPointToRay(surfaceWidth / 2.toFloat(), surfaceHeight / 2.toFloat()))
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
                            node.name = node.name + "_shot"
                            shotOnce = true
                        }
                    }
                }
            }
        }
    }

    private fun tryToTip() {
        var camera = arFragment?.arSceneView?.arFrame?.camera ?: return
        val pose = camera.pose
        val eulerAngles1 = com.google.ar.sceneform.samples.hellosceneform.Quaternion(pose.qw(), pose.qx(), pose.qy(), pose.qz()).ToEulerAngles()
        val rotateX1: Double = (eulerAngles1.roll + PI / 2) * (180.0 / PI)

        var axisAngle = Quaternion.axisAngle(Vector3.up(), rotateX1.toFloat())
        var axisAngle2 = Quaternion(pose.qw(), pose.qx(), pose.qy(), pose.qz())
        axisAngle = Quaternion.multiply(axisAngle2, axisAngle)
        val eulerAngles = com.google.ar.sceneform.samples.hellosceneform.Quaternion(axisAngle.w, axisAngle.x, axisAngle.y, axisAngle.z).ToEulerAngles()
        val rotateY: Double = (eulerAngles.pitch + PI / 2) * (180.0 / PI)

        var rotate = 90 - rotateY

        leanCallback?.invoke(rotate)

        mRootNode?.localPosition?.let {
            positionCallback?.invoke(((it.x - pose.tx()) * (it.x - pose.tx()) +
                    (it.y - pose.ty()) * (it.y - pose.ty()) +
                    (it.z - pose.tz()) * (it.z - pose.tz())).toDouble())
        }
    }

    class Builder {
        private var pointNum = 0
        private var pitchAngle = 0
        private var shotLayoutRes = -1
        private var shotAimLayoutRes = -1
        private var arFragment: ArFragment? = null
        private var leanCallback: ((rotate: Double) -> Unit)? = null
        private var positionCallback: ((distance: Double) -> Unit)? = null
        private var shotCallback: ((name: String) -> Unit)? = null

        fun pointNum(pointNum: Int): Builder {
            this.pointNum = pointNum
            return this
        }

        fun pitchAngle(pitchAngle: Int): Builder {
            this.pitchAngle = pitchAngle
            return this
        }

        fun shotLayoutRes(shotLayoutRes: Int): Builder {
            this.shotLayoutRes = shotLayoutRes
            return this
        }

        fun shotAimLayoutRes(shotAimLayoutRes: Int): Builder {
            this.shotAimLayoutRes = shotAimLayoutRes
            return this
        }

        fun arFragment(arFragment: ArFragment?): Builder {
            this.arFragment = arFragment
            return this
        }

        fun leanCall(block: (rotate: Double) -> Unit): Builder {
            leanCallback = block
            return this
        }

        fun positionCall(block: (distance: Double) -> Unit): Builder {
            positionCallback = block
            return this
        }

        fun shotCall(block: (name: String) -> Unit): Builder {
            shotCallback = block
            return this
        }

        fun build(): MobileShotScene {
            return MobileShotScene().apply {
                pointsArray.add(mapOf("pointCount" to pointNum / 2, "pitchAngle" to 30))
                pointsArray.add(mapOf("pointCount" to pointNum / 2, "pitchAngle" to -30))
                totalPointCount = pointNum
                shotAimLayoutRes = this@Builder.shotAimLayoutRes
                shotLayoutRes = this@Builder.shotLayoutRes
                arFragment = this@Builder.arFragment
                leanCallback = this@Builder.leanCallback
                positionCallback = this@Builder.positionCallback
                shotCallback = this@Builder.shotCallback
            }
        }

    }

}