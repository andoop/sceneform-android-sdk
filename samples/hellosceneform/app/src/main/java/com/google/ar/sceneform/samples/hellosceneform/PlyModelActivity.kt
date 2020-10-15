package com.google.ar.sceneform.samples.hellosceneform

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene.OnUpdateListener
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh
import com.google.ar.sceneform.samples.utils.ModelUtils
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_ply_model.*
import java.util.*

class PlyModelActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var mMaterial: Material? = null
    private var modelRenderable: ModelRenderable? = null
    private var hasLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ply_model)
        arFragment = uxFragment as ArFragment
        arFragment?.planeDiscoveryController?.apply {
            hide()
            setInstructionView(null)
        }

        //去除平面纹理
        arFragment?.arSceneView?.planeRenderer?.isEnabled = false
        MaterialFactory.makeOpaqueWithColor(this, Color()).thenAccept { material: Material ->
            mMaterial = material
            // mMaterial.set
            val submeshes: MutableList<Submesh> = ArrayList()
            val vertices: MutableList<Vertex> = ArrayList()
            ModelUtils.loadPly(material, vertices, submeshes, filesDir.absolutePath + "/plys/bunny.ply")
            val renderableDefinition = RenderableDefinition.builder()
                    .setSubmeshes(submeshes)
                    .setVertices(vertices)
                    .build()
            ModelRenderable.builder()
                    .setSource(renderableDefinition)
                    .build()
                    .thenAccept { renderable: ModelRenderable -> modelRenderable = renderable }
        }

        //关闭投影
        arFragment?.arSceneView?.scene?.sunlight?.light = Light.builder(Light.Type.POINT).setShadowCastingEnabled(false).build()
        arFragment?.arSceneView?.scene?.addOnUpdateListener(OnUpdateListener {
            if (arFragment?.arSceneView?.arFrame?.camera?.trackingState != TrackingState.TRACKING) {
                return@OnUpdateListener
            }
            if (modelRenderable != null && !hasLoaded) {
                hasLoaded = true
                addModelToNode()
            }

        })
    }

    private fun addModelToNode() {
        val translate = floatArrayOf(0f, 0f, 0f)
        val rotate = floatArrayOf(0f, 0f, 0f, 0f)

        val anchor = arFragment?.arSceneView?.session?.createAnchor(Pose(translate, rotate))?:return
        var anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment?.arSceneView?.scene)
        var rootNode = Node()
        val node = Node()
        node.setParent(rootNode)
        node.localPosition = Vector3(0f, 0f, 0f)
        node.localRotation = Quaternion(0f, 0f, 0f, 0f)
        node.localScale = Vector3(0.1f, 0.1f, 0.1f)
        node.renderable = modelRenderable
        anchorNode.addChild(rootNode)
    }
}