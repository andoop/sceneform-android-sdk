package com.google.ar.sceneform.samples.dice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
/**
 * �����࣬������Ϊһ��ǽ����Ⱦ
 * @author Yue Jinbiao
 *
 */
public class BackWall {
	/** �����������ݻ��� */
	private FloatBuffer mVertexBuffer;
	/** ���㷨�������ݻ��� */
	private FloatBuffer mNormalBuffer;
	/** �����������ݻ��壬�洢ÿ��������λͼ�е����� */
	private FloatBuffer mTextureBuffer;
	/**ɫ����*/
	public BackWall() {
		initDataBuffer();
	}
	/**��ʼ���������ݻ�����*/
	private void initDataBuffer(){
		float[] vertices = {-1.5f,2,-1,   -1.5f,-2,-1,   1.5f,-2,-1,   -1.5f,2,-1,  1.5f,2,-1,   1.5f,-2,-1 };
		float[] normals = {0,0,1,  0,0,1,  0,0,1,  0,0,1,  0,0,1,  0,0,1};
		float[] texST = {0,0,  0,1,  1,1,  0,0,  1,0,  1,1 };	//Ҫ����ͼ�������2��n ����
		// vertices.length*4����Ϊһ��Float�ĸ��ֽ�
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());// �����ֽ�˳��
		mVertexBuffer = vbb.asFloatBuffer();// ת��Ϊfloat�ͻ���
		mVertexBuffer.put(vertices);// �򻺳����з��붥����������
		mVertexBuffer.position(0);// ���û�������ʼλ��
		
		ByteBuffer nbb = ByteBuffer.allocateDirect(normals.length * 4);
		nbb.order(ByteOrder.nativeOrder());// �����ֽ�˳��
		mNormalBuffer = nbb.asFloatBuffer();// ת��Ϊint�ͻ���
		mNormalBuffer.put(normals);// �򻺳����з��붥����ɫ����
		mNormalBuffer.position(0);// ���û�������ʼλ��
		
		ByteBuffer tbb = ByteBuffer.allocateDirect(texST.length * 4);
		tbb.order(ByteOrder.nativeOrder());// �����ֽ�˳��
		mTextureBuffer = tbb.asFloatBuffer();// ת��Ϊint�ͻ���
		mTextureBuffer.put(texST);// �򻺳����з��붥����ɫ����
		mTextureBuffer.position(0);// ���û�������ʼλ��
	}
	/**����ɫ��*/
	public void drawSelf(GL10 gl) {
//		Log.i("tg","to draw dice..");
		// Ϊ����ָ��������������
		gl.glVertexPointer(3, 				// ÿ���������������Ϊ3 xyz
				GL10.GL_FLOAT,			 	// ��������ֵ������Ϊ GL_FIXED
				0, 										// ����������������֮��ļ��
				mVertexBuffer 					// ������������
		);

		// Ϊ����ָ�����㷨��������
		gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer);

		// ����������ͼ
		gl.glEnable(GL10.GL_TEXTURE_2D);
		// ����ʹ������ST���껺��
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		// ָ������ST���껺��
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
		// �󶨵�ǰ����
		gl.glBindTexture(GL10.GL_TEXTURE_2D, TextureManager.getTextureId(TextureManager.bgIndex + 1));
		
/*		//�������ĳ����
		gl.glEnable(GL10.GL_CULL_FACE);
		//����˳ʱ��Ϊǰ�棬GL_CCW-��ʱ�루Ĭ�ϣ���GL_CW-˳ʱ��
		gl.glFrontFace(GL10.GL_CW);
		//���Ժ��棬GL_FRONT-���棬GL_BACK-���档
		gl.glCullFace(GL10.GL_BACK);
*/
		// ����ͼ�� , �������η�ʽ���
		gl.glDrawArrays(GL10.GL_TRIANGLES, 	0, 	6 );
	}

}
