package com.google.ar.sceneform.samples.hellosceneform;

public class CustomPose {
    private float[] translate,rotate;
    public CustomPose(float[] translate,float[] rotate){
        this.translate = translate;
        this.rotate = rotate;
    }

    public float tx(){
        return translate[0];
    }
    public float ty(){
        return translate[1];
    }
    public float tz(){
        return translate[2];
    }

    public float qx(){
        return rotate[0];
    }
    public float qy(){
        return rotate[1];
    }
    public float qz(){
        return rotate[2];
    }
    public float qw(){
        return rotate[3];
    }
}
