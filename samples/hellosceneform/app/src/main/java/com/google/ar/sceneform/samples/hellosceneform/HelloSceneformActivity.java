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
package com.google.ar.sceneform.samples.hellosceneform;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableDefinition;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.Vertex;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.rendering.ViewSizer;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
    private static final String TAG = HelloSceneformActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private List<ViewRenderable> viewRenderables = new ArrayList<>();
    private List<ViewRenderable> viewRenderables2 = new ArrayList<>();
    private ModelRenderable modelRenderable;

    private List<CustomPose> poses = new ArrayList<>();
    private List<CustomPose> poses2 = new ArrayList<>();
    private boolean hasCreated;
    private boolean hasStartShot;
    private Node mRootNode;
    private Timer timer = new Timer();
    private SoundPlayer soundPlayer = new SoundPlayer();
    private int flashSound;
    private TextureView textureView;
    private ImageView ivPreview;
    private AnchorNode anchorNode;
    boolean shotOnce;//拍摄一次，首先要移除所有node
    boolean takeOnce;//获取一下图片，然后重新添加node
    private Material mMaterial;


    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ux);
        textureView = findViewById(R.id.textureView);
        ivPreview = findViewById(R.id.ivPreView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                arFragment.getArSceneView().getRenderer().startMirroring(new Surface(surface), 0, 0, width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                arFragment.getArSceneView().getRenderer().stopMirroring(new Surface(surface));
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        if (arFragment.getPlaneDiscoveryController() != null) {
            arFragment.getPlaneDiscoveryController().hide();
            arFragment.getPlaneDiscoveryController().setInstructionView(null);
        }

        flashSound = soundPlayer.preLoad(this, R.raw.flash);

        //去除平面纹理
        arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        //初始化24个纹理
        for (int i = 0; i < 24; i++) {
            ViewRenderable.builder()
                .setView(this, R.layout.layout_aim_shot)
                .setSizer(new ViewSizer() {
                    @Override
                    public Vector3 getSize(View view) {
                        Vector3 vector3 = new Vector3();
                        vector3.x = 0.055f;
                        vector3.y = 0.055f;
                        return vector3;
                    }
                })
                .build()
                .thenAccept(renderable -> viewRenderables.add(renderable));

        }
        //初始化24个 circle 纹理
        for (int i = 0; i < 24; i++) {
            ViewRenderable.builder()
                .setView(this, R.layout.layout_shot_circle)
                .setSizer(new ViewSizer() {
                    @Override
                    public Vector3 getSize(View view) {
                        Vector3 vector3 = new Vector3();
                        vector3.x = 0.045f;
                        vector3.y = 0.045f;
                        return vector3;
                    }
                })
                .build()
                .thenAccept(renderable -> viewRenderables2.add(renderable));

        }

        MaterialFactory.makeOpaqueWithColor(this, new Color()).thenAccept(material -> {
            mMaterial = material;
           // mMaterial.set
            List<RenderableDefinition.Submesh> submeshes = new ArrayList<>();
            List<Vertex> vertices = new ArrayList<>();
            vertices.add(buildVertex(0, 0, 0));
            vertices.add(buildVertex(0, 0, 1));
            vertices.add(buildVertex(0, 1, 1));
            vertices.add(buildVertex(0, 1, 0));
            vertices.add(buildVertex(0, 1, 0));
            vertices.add(buildVertex(1, 0, 0));
            vertices.add(buildVertex(0, 0, 1));
            vertices.add(buildVertex(1, 1, 1));


            submeshes.add(buildSubmesh(0, 1, 3));
            submeshes.add(buildSubmesh(1, 2, 3));
            submeshes.add(buildSubmesh(0, 7, 4));
            submeshes.add(buildSubmesh(0, 3, 7));
            submeshes.add(buildSubmesh(5, 4, 6));
            submeshes.add(buildSubmesh(6, 4, 7));
            submeshes.add(buildSubmesh(2, 6, 3));
            submeshes.add(buildSubmesh(6, 7, 3));
            submeshes.add(buildSubmesh(1, 5, 2));
            submeshes.add(buildSubmesh(2, 5, 6));
            submeshes.add(buildSubmesh(1, 0, 5));
            submeshes.add(buildSubmesh(5, 0, 4));

            RenderableDefinition build = RenderableDefinition.builder()
                .setSubmeshes(submeshes)
                .setVertices(vertices)
                .build();
            ModelRenderable.builder()
                .setSource(build)
                .build()
                .thenAccept(renderable -> {
                    modelRenderable = renderable;
                });

        });

        arFragment.setOnSessionInitializationListener(new BaseArFragment.OnSessionInitializationListener() {
            @Override
            public void onSessionInitialization(Session session) {
                initPoses();
                initPoses2();
            }
        });

        arFragment.getArSceneView().getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        //new SurfaceTexture()

        //关闭投影
        arFragment.getArSceneView().getScene().getSunlight().setLight(Light.builder(Light.Type.POINT).setShadowCastingEnabled(false).build());
        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
                    return;
                }

                if (!hasCreated) {
                    createNodes();
                    hasCreated = true;
                }
                if (hasCreated) {
                    updateNodes();
                }

                if (takeOnce) {
                    takeOnce = false;
                    ivPreview.setImageBitmap(textureView.getBitmap());
                    anchorNode.addChild(mRootNode);
                }
                if (shotOnce) {
                    mRootNode.getParent().removeChild(mRootNode);
                    shotOnce = false;
                    takeOnce = true;
                }
            }
        });

        findViewById(R.id.btShot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hasStartShot = true;
                v.setVisibility(View.GONE);
                timer.star();
            }
        });

        timer.setCallback(new Timer.Callback() {
            @Override
            public void onTick() {
                if (hasStartShot) {
                    tryToShot();
                }
            }
        });
    }

    private Vertex buildVertex(int i, int i1, int i2) {
        Vector3 vector3 = new Vector3(i, i1, i2);
        return Vertex.builder()
            .setPosition(vector3)
            .setColor(new Color())
            //.setNormal(vector3)
            .build();
    }

    private RenderableDefinition.Submesh buildSubmesh(int i, int i1, int i2) {
        List<Integer> triangleIndices = new ArrayList<>();
        triangleIndices.add(i);
        triangleIndices.add(i1);
        triangleIndices.add(i2);
        return RenderableDefinition.Submesh.builder()
            .setMaterial(mMaterial)
            .setTriangleIndices(triangleIndices)
            .build();
    }

    private void tryToShot() {
        Frame arFrame = arFragment.getArSceneView().getArFrame();
        if (arFrame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            ArrayList<HitTestResult> hitTestResults = arFragment.getArSceneView().getScene().hitTestAll(arFragment.getArSceneView().getScene().getCamera().screenPointToRay(ScreenUtils.getScreenWidth(this) / 2, ScreenUtils.getScreenHeight(this) / 2));
            for (int i = 0; i < hitTestResults.size(); i++) {
                HitTestResult hitTestResult = hitTestResults.get(i);
                Renderable renderable = hitTestResult.getNode().getRenderable();
                if (renderable instanceof ViewRenderable) {
                    ViewRenderable viewRenderable = (ViewRenderable) renderable;
                    ImageView layer = viewRenderable.getView().findViewById(R.id.ivLayer);
                    ObjectAnimator alpha = ObjectAnimator.ofFloat(layer, "alpha", 0f, 1f);
                    alpha.setDuration(100);
                    alpha.setRepeatCount(1);
                    alpha.start();
                }
            }

            if (hitTestResults.size() == 2 && hitTestResults.get(0).getNode().getName().startsWith("b") && hitTestResults.get(1).getNode().getName().startsWith("a")) {
                for (int i = 0; i < hitTestResults.size(); i++) {
                    HitTestResult hitTestResult = hitTestResults.get(i);
                    Node node = hitTestResult.getNode();
                    if (node.getName().startsWith("a")) {
                        Renderable renderable = node.getRenderable();
                        if (renderable instanceof ViewRenderable) {
                            ViewRenderable viewRenderable = (ViewRenderable) renderable;
                            ImageView layer = viewRenderable.getView().findViewById(R.id.ivLayer);
                            layer.setImageResource(R.drawable.already_shot);
                            node.setName("shot_" + node.getName());
                            soundPlayer.play(flashSound);
                            shotOnce = true;
                        }
                    }
                }
            }

        }
    }

    private void createNodes() {
        float[] translate = new float[]{0f, 0f, 0f};
        float[] rotate = new float[]{0f, 0f, 0f, 0f};

        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(new Pose(translate, rotate));
        anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        mRootNode = new Node();

        for (int i = 0; i < poses.size(); i++) {
            CustomPose pose = poses.get(i);
            Node node = new Node();
            node.setName("a" + i);
            node.setParent(mRootNode);
            node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
            node.setLocalRotation(new com.google.ar.sceneform.math.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
            node.setRenderable(viewRenderables.get(i));
        }
        for (int i = 0; i < poses2.size(); i++) {
            CustomPose pose = poses2.get(i);
            Node node = new Node();
            node.setName("b" + i);
            node.setParent(mRootNode);
            node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
            node.setLocalRotation(new com.google.ar.sceneform.math.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
            //node.setRenderable(viewRenderables2.get(i));
            node.setLocalScale(new Vector3(0.1f,0.1f,0.1f));
            node.setRenderable(modelRenderable);
        }

        anchorNode.addChild(mRootNode);
    }

    private void updateNodes() {
        if (!hasStartShot) {
            Vector3 localPosition = arFragment.getArSceneView().getScene().getCamera().getLocalPosition();
            mRootNode.setLocalPosition(localPosition);
        }
    }


    private void initPoses() {
        int count = 12;
        float dis = 0.55f;
        double angle = Math.toRadians(30);
        double angle2 = Math.toRadians(60);
        for (int i = 0; i < count; i++) {
            double v = dis * Math.cos(angle * i);
            double v1 = dis * Math.sin(angle * i);
            float[] translate = new float[]{(float) v, (float) (dis * Math.sin(angle)), (float) v1};
            EulerAngles eulerAngles = new EulerAngles((float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(30));
            Quaternion quaternion = eulerAngles.ToQuaternion();
            float[] rotate = new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};
            poses.add(new CustomPose(translate, rotate));

            float[] translate2 = new float[]{-(float) v, -(float) (dis * Math.sin(angle)), (float) v1};
            EulerAngles eulerAngles2 = new EulerAngles(-(float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(-30));
            Quaternion quaternion2 = eulerAngles2.ToQuaternion();
            float[] rotate2 = new float[]{quaternion2.x, quaternion2.y, quaternion2.z, quaternion2.w};
            poses.add(new CustomPose(translate2, rotate2));
        }
    }


    private void initPoses2() {
        int count = 12;
        float dis = 0.55f * 0.7f;
        double angle = Math.toRadians(30);
        double angle2 = Math.toRadians(60);
        for (int i = 0; i < count; i++) {
            double v = dis * Math.cos(angle * i);
            double v1 = dis * Math.sin(angle * i);
            float[] translate = new float[]{(float) v, (float) (dis * Math.sin(angle)), (float) v1};
            EulerAngles eulerAngles = new EulerAngles((float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(30));
            Quaternion quaternion = eulerAngles.ToQuaternion();
            float[] rotate = new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};
            poses2.add(new CustomPose(translate, rotate));

            float[] translate2 = new float[]{-(float) v, -(float) (dis * Math.sin(angle)), (float) v1};
            EulerAngles eulerAngles2 = new EulerAngles(-(float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(-30));
            Quaternion quaternion2 = eulerAngles2.ToQuaternion();
            float[] rotate2 = new float[]{quaternion2.x, quaternion2.y, quaternion2.z, quaternion2.w};
            poses2.add(new CustomPose(translate2, rotate2));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStartShot) {
            timer.star();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundPlayer.release();
    }


    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
            ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
