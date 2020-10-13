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
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.Renderable;
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

    private List<CustomPose> poses = new ArrayList<>();
    private List<CustomPose> poses2 = new ArrayList<>();
    private boolean hasCreated;
    private boolean hasStartShot;
    private Node mRootNode;
    private Timer timer = new Timer();


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
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        if (arFragment.getPlaneDiscoveryController() != null) {
            arFragment.getPlaneDiscoveryController().hide();
            arFragment.getPlaneDiscoveryController().setInstructionView(null);
        }

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


        arFragment.setOnSessionInitializationListener(new BaseArFragment.OnSessionInitializationListener() {
            @Override
            public void onSessionInitialization(Session session) {
                initPoses();
                initPoses2();
            }
        });


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
                            node.setName("shot_"+node.getName());
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
        AnchorNode anchorNode = new AnchorNode(anchor);
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
            node.setRenderable(viewRenderables2.get(i));
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
