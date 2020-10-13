package com.google.ar.sceneform.samples.hellosceneform;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {
    private SoundPool soundpool;

    public SoundPlayer() {
        SoundPool.Builder builder = new SoundPool.Builder();
        //传入音频数量
        builder.setMaxStreams(5);
        //AudioAttributes是一个封装音频各种属性的方法
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        //设置音频流的合适的属性
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);//STREAM_MUSIC
        //加载一个AudioAttributes
        builder.setAudioAttributes(attrBuilder.build());
        soundpool = builder.build();
    }

    public int preLoad(Context context, int res) {
        return soundpool.load(context, res, 1);
    }

    public void play(int id) {
        soundpool.play(id, 1, 1, 0, 0, 1);
    }

    public void release() {
        soundpool.release();
    }
}
