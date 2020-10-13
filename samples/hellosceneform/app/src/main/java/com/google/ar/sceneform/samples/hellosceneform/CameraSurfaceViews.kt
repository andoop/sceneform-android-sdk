package com.google.ar.sceneform.samples.hellosceneform

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

import java.io.IOException

class CameraSurfaceViews
//获取surfaceView的SurfaceHolder对象和接口

    (internal var mContext: Context, attrs: AttributeSet) : SurfaceView(mContext, attrs),
    SurfaceHolder.Callback, Camera.PreviewCallback {
    internal var mSurfaceHolder: SurfaceHolder//surface的控制器
    internal var mCamera: Camera? = null//相机类
    internal var mCb: FrameCallback? = null//数据回调接口

    //打开相机
    private val camera: Camera? get() {
            var camera: Camera? = null
            val cameraId = findCamera(false)
            try {
                if (cameraId == 1) {
                    camera = Camera.open(cameraId)
                } else if (cameraId == 0) {
                    camera = Camera.open(0)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                camera = null
            }

            return camera
        }

    fun setmCb(mCb: FrameCallback) {
        this.mCb = mCb
    }

    init {
        mSurfaceHolder = holder
        mSurfaceHolder.addCallback(this)

    }


    public fun camera():Camera?{
        return mCamera
    }


    //寻找相机
    private fun findCamera(isfront: Boolean): Int {
        var cameraCount = 0
        val cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()

        for (camIdx in 0 until cameraCount) {
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (isfront) {
                // CAMERA_FACING_FRONT前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    return camIdx
                }
            } else {
                // CAMERA_FACING_BACK后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return camIdx
                }
            }

        }
        return -1
    }

    //设置预览旋转的角度
    private fun setCameraDisplayOrientation(activity: Activity) {
        var info = Camera.CameraInfo()
        val mCameraFacing = 0
        Camera.getCameraInfo(mCameraFacing, info)
        val rotation = activity.windowManager.defaultDisplay.rotation

        var screenDegree = 0
        when (rotation) {
            Surface.ROTATION_0 -> screenDegree = 0
            Surface.ROTATION_90 -> screenDegree = 90
            Surface.ROTATION_180 -> screenDegree = 180
            Surface.ROTATION_270 -> screenDegree = 270
        }

        var mDisplayOrientation: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + screenDegree) % 360
            mDisplayOrientation = (360 - mDisplayOrientation) % 360          // compensate the mirror
        } else {
            mDisplayOrientation = (info.orientation - screenDegree + 360) % 360
        }
        mCamera?.setDisplayOrientation(mDisplayOrientation)
    }

    //surface被创建时调用
    override fun surfaceCreated(holder: SurfaceHolder) {
        mCamera = camera
        setCameraDisplayOrientation(mContext as Activity)
    }

    //surface大小被改变时调用
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        setStartPreview(mCamera!!, mSurfaceHolder)
    }

    //surface被销毁时调用
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    //开启相机预览
    private fun setStartPreview(camera: Camera, holder: SurfaceHolder) {
        try {

            mCamera!!.setPreviewDisplay(holder)
            mCamera!!.setPreviewCallback(this)
            camera.startPreview()
        } catch (e: IOException) {
        }

    }

    //释放Camera
    fun releaseCamera() {
        if (mCamera != null) {
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()// 停掉摄像头的预览
            mCamera!!.release()
            mCamera = null
        }
    }

    //预览回调，传递yuv视频流数据
    override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
        //    Log.i("onPreviewFrame",""+ bytes);
        if (mCb != null) {
            mCb!!.onDecodeFrame(bytes)
        }
    }

    open interface FrameCallback {
        fun onDecodeFrame(data: ByteArray)
    }
}