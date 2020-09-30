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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
    private static final String TAG = HelloSceneformActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private ViewRenderable viewRenderable;
    private ViewRenderable viewRenderable2;

    private List<CustomPose> poses = new ArrayList<>();
    private List<CustomPose> poses2 = new ArrayList<>();


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

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build()
            .thenAccept(renderable -> andyRenderable = renderable)
            .exceptionally(
                throwable -> {
                    Toast toast =
                        Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                });

        ViewRenderable.builder()
            .setView(this, R.layout.layout_already_shot)
            .build()
            .thenAccept(renderable -> viewRenderable = renderable);

        ViewRenderable.builder()
            .setView(this, R.layout.layout_shot_circle)
            .build()
            .thenAccept(renderable -> viewRenderable2 = renderable);

        arFragment.setOnTapArPlaneListener(
            (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                if (andyRenderable == null) {
                    return;
                }

                // Create the Anchor.
         /* Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable andy and add it to the anchor.
          TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
          andy.setParent(anchorNode);
          andy.setRenderable(viewRenderable);
          andy.select();*/

                float[] translate = new float[]{0f, 0f, 0f};
                float[] rotate = new float[]{0f, 0f, 0f, 0f};

                Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(new Pose(translate, rotate));
                // Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
                Node root = new Node();
           /* Node node = new Node();
            node.setParent(root);
            node.setLocalPosition(new Vector3(0f,0.5f,0f));
            node.setRenderable(viewRenderable);

            Node node2 = new Node();
            node2.setParent(root);
            node2.setLocalPosition(new Vector3(0f,-0.5f,0f));
            node2.setRenderable(viewRenderable);

            Node node3 = new Node();
            node3.setParent(root);
            node3.setLocalPosition(new Vector3(0.5f,0f,0f));
            node3.setRenderable(viewRenderable);

            Node node4 = new Node();
            node4.setParent(root);
            node4.setLocalPosition(new Vector3(-0.5f,0f,0f));
            node4.setRenderable(viewRenderable);

            Node node5 = new Node();
            node5.setParent(root);
            node5.setLocalPosition(new Vector3(0f,0f,0.5f));
            node5.setRenderable(viewRenderable);

            Node node6 = new Node();
            node6.setParent(root);
            node6.setLocalPosition(new Vector3(-0f,0f,-0.5f));
            node6.setRenderable(viewRenderable);*/

                for (CustomPose pose : poses) {
                    Node node = new Node();
                    node.setParent(root);
                    node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
                    node.setLocalRotation(new com.google.ar.sceneform.math.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
                    node.setRenderable(viewRenderable);
                }

                for (CustomPose pose : poses2) {
                    Node node = new Node();
                    node.setParent(root);
                    node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
                    node.setLocalRotation(new com.google.ar.sceneform.math.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
                    node.setRenderable(viewRenderable2);
                }
                anchorNode.addChild(root);
            });

        arFragment.setOnSessionInitializationListener(new BaseArFragment.OnSessionInitializationListener() {
            @Override
            public void onSessionInitialization(Session session) {
                initPoses();
                initPoses2();
            }
        });

        // arFragment
    }

    private void initPoses() {
        int count = 12;
        float dis = 0.4f;
        double angle = Math.toRadians(30);
        double angle2 = Math.toRadians(60);
        for (int i = 0; i < count; i++) {
            float[] translate = new float[]{(float) (dis * Math.cos(angle * i)), (float) (dis * Math.sin(angle)), (float) (dis * Math.sin(angle * i))};
            EulerAngles eulerAngles = new EulerAngles((float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(30));
            Quaternion quaternion = eulerAngles.ToQuaternion();
            float[] rotate = new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};
            poses.add(new CustomPose(translate, rotate));

            float[] translate2 = new float[]{-(float) (dis * Math.cos(angle * i)), -(float) (dis * Math.sin(angle)), (float) (dis * Math.sin(angle * i))};
            EulerAngles eulerAngles2 = new EulerAngles(-(float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(-30));
            Quaternion quaternion2 = eulerAngles2.ToQuaternion();
            float[] rotate2 = new float[]{quaternion2.x, quaternion2.y, quaternion2.z, quaternion2.w};
            poses.add(new CustomPose(translate2, rotate2));
        }
    }


    private void initPoses2() {
        int count = 12;
        float dis = 0.4f * 0.7f;
        double angle = Math.toRadians(30);
        double angle2 = Math.toRadians(60);
        for (int i = 0; i < count; i++) {
            float[] translate = new float[]{(float) (dis * Math.cos(angle * i)), (float) (dis * Math.sin(angle)), (float) (dis * Math.sin(angle * i))};
            EulerAngles eulerAngles = new EulerAngles((float) (Math.toRadians(360 - 30 * i - 90)), 0, (float) Math.toRadians(30));
            Quaternion quaternion = eulerAngles.ToQuaternion();
            float[] rotate = new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};
            poses2.add(new CustomPose(translate, rotate));

            float[] translate2 = new float[]{-(float) (dis * Math.cos(angle * i)), -(float) (dis * Math.sin(angle)), (float) (dis * Math.sin(angle * i))};
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
