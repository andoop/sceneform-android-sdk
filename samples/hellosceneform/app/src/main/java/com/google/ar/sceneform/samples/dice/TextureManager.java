package com.google.ar.sceneform.samples.dice;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;

import com.google.ar.sceneform.samples.hellosceneform.R;

/**
 * ���������
 * @author Yue Jinbiao
 *
 */
public class TextureManager {

	//����������
	public static final int TEXTURE_INDEX_DICE = 0;
	public static final int TEXTURE_INDEX_BG00 = 1;
	public static final int TEXTURE_INDEX_BG01 = 2;
	public static final int TEXTURE_INDEX_BG02 = 3;
	//������Դid
	private static int[] textureSrcs = {R.drawable.dice_map,R.drawable.bg00,R.drawable.bg01,R.drawable.bg02};
	//����id�洢
	private static int[] textureIds = new int[textureSrcs.length];
	
	private static GL10 gl = null;
	private static Resources res = null;

	//���������� 0-2��
	public static int bgIndex = 0;
	
	/**
	 * ȡ��ָ������������id
	 * @param index
	 * @return
	 */
	public static int getTextureId(int index){
//		Log.i("tg","TextureManager/getTextureId/" + textureIds[index]);
		if(textureIds[index] <= 0){
			Log.i("tg","TextureManager/getTextureId/" + textureIds[index]);
			gl.glGenTextures(1, textureIds, index);
			bindTexture(gl,res,index);
		}
		return textureIds[index];
	}
	/**��ʼ������*/
	public static void initTexture( GL10 gl, Resources res) {

		TextureManager.gl = gl;
		TextureManager.res = res;
		//��ȡδʹ�õ��������ID
		gl.glGenTextures(1, textureIds, TEXTURE_INDEX_DICE);
		bindTexture(gl,res,TEXTURE_INDEX_DICE);
		//��ȡδʹ�õ��������ID
		gl.glGenTextures(1, textureIds, bgIndex + 1);
		bindTexture(gl,res,bgIndex + 1);

		
//		for(int i=0;i<textureIds.length;i++){
//			bindTexture(gl,res,i);
//		}

	}
	/**
	 * Ϊ����id������
	 * @param gl
	 * @param res
	 * @param index
	 */
	private static void bindTexture(GL10 gl,Resources res,int index){
//		Log.i("tg","TextureManager/initTexture/" + textureIds[i]);
		//���������
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[index]);
		//����������ƣ�ָ��ʹ������ʱ�Ĵ���ʽ
		//��С���ˣ�һ�����ش��������ء�
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, 	//����Ŀ��
				GL10.GL_TEXTURE_MIN_FILTER,			//������С����
				GL10.GL_NEAREST								//ʹ�þ��뵱ǰ��Ⱦ�����������������
				);
		//�Ŵ���ˣ�һ��������һ�����ص�һ���֡�
		//�Ŵ����ʱ��ʹ�þ��뵱ǰ��Ⱦ�������ģ������4�����ؼ�Ȩƽ��ֵ��Ҳ��˫���Թ��ˡ�
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);		//
		//����������ͼ��ʽ��ָ���Գ�����0,1������������Ĵ���ʽ
		//���½��ǡ�0,0�������Ͻ��ǡ�1,1����������Sά��������Tά��android�����Ͻ�Ϊԭ��
		//Sά��ͼ��ʽ���ظ�ƽ��
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
				GL10.GL_REPEAT);
		//Tά��ͼ��ʽ���ظ�ƽ��
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
				GL10.GL_REPEAT);
		bindBitmap(index,res);
	}
	/**
	 * Ϊ�����λͼ
	 * @param index
	 * @param res
	 */
	private static void bindBitmap(int index,Resources res){
		Bitmap bitmap = null;
		InputStream is = res.openRawResource(textureSrcs[index]);
		try {
			bitmap = BitmapFactory.decodeStream(is);
		} finally {
			if(is != null){
				try {
					is.close();
					is = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//Ϊ�������ָ��λͼ
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
		//�ͷ�bitmap�����ڴ棬���������Դ��ڣ���Ӱ��ʹ�á�
		bitmap.recycle();
	}
}
