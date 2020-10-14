package com.google.ar.sceneform.samples.dice;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.os.Handler;
import android.os.Message;
/**
 * ��ά��Ⱦ��
 * @author Yue Jinbiao
 *
 */
public class DiceRenderer implements Renderer {

	//90�Ƚǵ�������
	private static final float NORMALS_COS = (float) Math.cos(Math.PI/2);
	private static final float NORMALS_SIN = (float)Math.sin(Math.PI/2);
	private static final int MSG_ROTATE_STOP = 1;
	
	private DiceSurfaceView surface = null;
	private Handler handler = null;
	private Dice dice = null;
	private BackWall back = null;
	//ת��ʱ�ٶ�ʸ��
	private float rotateV = 0;
	//����ת�Ƕ�
	private float rotated = 0;
	//��ǰ��ת��
	private float axisX = 0;
	private float axisY = 0;
	private RotateTask rTask = null;
	
	/**��Ⱦ��*/
	public DiceRenderer(DiceSurfaceView surface){
//		Log.i("tg","Renderer ���졣");
		this.surface = surface;
		// ��ʼ������
		dice = new Dice();
		back = new BackWall();
		handler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				if(msg.what == MSG_ROTATE_STOP){
					DiceRenderer.this.surface.setAutoRender(false);//���÷��Զ�������Ⱦ
				}
			}
		};
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//		Log.i("tg","Surface created.config/" + config);
		
		// Set the background frame color
		gl.glClearColor(0.3f, 0.3f, 0.4f, 0.7f);
		// ������Ȳ���, ������ʱ������Զ�����󻭵ĻḲ��֮ǰ���ģ�
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);// ���ö�����������
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);// �򿪷�������
		//��ʼ������
		TextureManager.initTexture(gl, this.surface.getResources());
		initLight(gl);
		initMaterial(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
//		Log.i("tg","Surface changed.��");
		//�����Ӵ�
		gl.glViewport(0, 0, width, height);
        // ��Ӧ��Ļ����
        float ratio = (float) width / height;
        //���þ���ΪͶ��ģʽ
        gl.glMatrixMode(GL10.GL_PROJECTION);        // set matrix to projection mode
        //���þ���
        gl.glLoadIdentity();                        // reset the matrix to its default state
        //����Ͷ��׵�� // apply the projection matrix
        if(ratio < 1 ){
        	gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7); 
        }else{
        	gl.glFrustumf(-ratio, ratio, -1, 1, 4, 8); 
//        	gl.glFrustumf(-ratio*1.5f, ratio*1.5f, -1*1.5f, 1*1.5f, 4, 8); 
        }
        
	}

	@Override
	public void onDrawFrame(GL10 gl) {
//		Log.i("tg","draw a frame..");
		// �ػ�����,  ˢ��
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// ���� GL_MODELVIEW(ģ�͹۲�) ת��ģʽ
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		// ���þ������õ�ǰ����Ϊ��λ�����൱����Ⱦ֮ǰ����
		gl.glLoadIdentity();

		// ʹ��GL_MODELVIEW ģʽʱ, ���������ӵ�
//		GLU.gluLookAt(gl, 3,3,3, 1f, 1f, 1f, 0f, 1.0f, 0f);
		GLU.gluLookAt(gl, 0, 0, 5, 0f, 0f, -1f, 0f, 1.0f, 0.0f);
		
		// ���Ʊ���ǽ
		gl.glPushMatrix();
		back.drawSelf(gl);
		gl.glPopMatrix();

		// ����ɫ��
		gl.glPushMatrix();

		if(rotated != 0){
			RotateOnTouch(gl);
		}
		gl.glRotatef(45, 1, 1, 0);
		dice.drawSelf(gl);
		gl.glPopMatrix();

	}
	/**������ת��*/
	private void RotateOnTouch(GL10 gl){
		this.rotated += rotateV;
		gl.glRotatef(rotated, axisX, axisY, 0);
		if(rotateV>0){
//			Log.i("tg","GL rotateV/" + rotateV);
//			Log.i("tg","GL rotated/" + rotated + "/" + rotateV);
		}
	}
	/**
	 * ��Ӧ�����ƶ�
	 * @param dx
	 * @param dy
	 */
	public void onTouchMove(float dx,float dy){
		rotateV = Math.abs(dx) + Math.abs(dy);
//		Log.i("tg","GL rotateV/" + rotateV);
		rotated += rotateV;
		setAxisLine(dx,dy);
	}
	/**����ת����*/
	public void setAxisLine(float dx ,float dy){
		//x1 = x0 * cosB - y0 * sinB		y1 = x0 * sinB + y0 * cosB
		this.axisX = dx*NORMALS_COS - dy*NORMALS_SIN;
		this.axisY= dx*NORMALS_SIN + dy*NORMALS_COS;
	}
	/**������ת�߳�*/
	public void startRotate(){
		if(rTask != null){
			rTask.running = false;
		}
		rTask = new RotateTask();
		rTask.start();
	}
	/**
	 * ��ת�߳���
	 *
	 */
	class RotateTask extends Thread{
		boolean running = true;
		@Override
		public void run() {
			while(running && rotateV > 0){
				if(rotateV>50){
					rotateV -= 7;
				}else if(rotateV>20){
					rotateV -= 3;
				}else{
					rotateV --;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(rotateV<=0){
				handler.sendEmptyMessage(MSG_ROTATE_STOP);
			}
		}
	}

	/** ��ʼ���ƹ�
	 * ����������͹�Ĺ���
	 * */
	private void initLight(GL10 gl) {
		gl.glEnable(GL10.GL_LIGHTING);		//�������ܿ���
		gl.glEnable(GL10.GL_LIGHT1);		// ��1�ŵ�

		// ����������
		float[] ambientParams = { 0.7f, 0.7f, 0.7f, 1.0f };// ����� RGBA
		gl.glLightfv(GL10.GL_LIGHT1,		//��Դ���
				GL10.GL_AMBIENT, 			//���ղ�����-������
				ambientParams, 				//����ֵ
				0							//ƫ��
				);
		// ɢ�������
		float[] diffuseParams = { 0.7f, 0.7f, 0.7f, 1.0f };// ����� RGBA
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_DIFFUSE, diffuseParams, 0);
		// ���������
		float[] specularParams = { 1f, 1f, 1f, 1.0f };// ����� RGBA
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPECULAR, specularParams, 0);
		//��Դλ��
		float[] positionParams = { 0,0,9,1 };
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, positionParams, 0);
		//�۹�Ʒ���
		float[] directionParams = {0,0,-1};
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPOT_DIRECTION , directionParams, 0);
		//�۹�Ƕȣ�0-90����
		gl.glLightf(GL10.GL_LIGHT1, GL10.GL_SPOT_CUTOFF , 30);
		//�۹�̶ȣ�0-128��ʵ�־۽�
		gl.glLightf(GL10.GL_LIGHT1, GL10.GL_SPOT_EXPONENT  , 10);
	}

	/** ��ʼ������ 
	 * ����ƽ��Ը������͹�ķ������
	 * */
	private void initMaterial(GL10 gl) {
		//���ƻ�������ƽ���ϵķ�������					
		float ambientMaterial[] = { 0.4f, 0.5f, 0.6f, 0.3f };
		gl.glMaterialfv(
				GL10.GL_FRONT_AND_BACK, //�����棬���棬���棬�����棨android)ֻ֧������
				GL10.GL_AMBIENT,		//��������ͣ�������
				ambientMaterial, 		//�������ֵ
				0						//ƫ��
				);
		//���Ʒ���ɢ���
		float diffuseMaterial[] = { 0.7f, 0.6f, 0.7f, 0.8f };
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE,
				diffuseMaterial, 0);
		//���Ʒ����
		float specularMaterial[] = { 0.9f, 0.9f, 0.9f, 0.8f };
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR,
				specularMaterial, 0);
		//�Ը߹�ķ���ָ����0-128��ֵԽ����ɢ��ԽС
		gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 120f);
	}
	
}
