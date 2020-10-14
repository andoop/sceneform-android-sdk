package com.google.ar.sceneform.samples.dice;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * ɫ�ӵ���Ⱦ��ͼ
 * @author Yue Jinbiao
 *
 */
class DiceSurfaceView extends GLSurfaceView {

	private DiceRenderer mRenderer = null;
	private float mPreviousX = 0;
	private float mPreviousY = 0;

	public DiceSurfaceView(Context context) {
		super(context);
		// ������Ⱦ����
		mRenderer = new DiceRenderer(this);
		setRenderer(mRenderer);
		// ������淽ʽ��
		setAutoRender(false);
		this.requestRender();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		//ת�����귽��
		y = -y;

		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:
			float dx = x - mPreviousX;
			float dy = y - mPreviousY;
			mRenderer.onTouchMove(dx, dy);
		case MotionEvent.ACTION_DOWN:
//			Log.i("tg","touch down/" + x + "/" + y);
			this.mPreviousX = x;
			this.mPreviousY = y;
			break;
		case MotionEvent.ACTION_UP:
//			Log.i("tg","touch up/" + x + "/" + y);
			this.mPreviousX = 0;
			this.mPreviousY = 0;
			setAutoRender(true);
			this.mRenderer.startRotate();
			break;
		}
		this.requestRender();
		return true;
	}
	/**
	 * �����Ƿ��Զ�������Ⱦ
	 * @param auto
	 */
	public void setAutoRender(boolean auto){
		// RENDERMODE_WHEN_DIRTY-�иı�ʱ�ػ�-�����requestRender()
		// RENDERMODE_CONTINUOUSLY -�Զ������ػ棨Ĭ�ϣ�
		if(auto){
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		}else{
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		}
	}

	//���ñ�����
	public void resetBackground(int optionalBg){
		TextureManager.bgIndex = optionalBg;
		this.requestRender();
	}
}
