package com.google.ar.sceneform.samples.dice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.ar.sceneform.samples.hellosceneform.R;

/**
 * 3Dɫ����Activity
 * @author Yue Jinbiao
 *
 */
public class DiceActivity extends Activity {
	private DiceSurfaceView mGLView;
	private int bgIndex = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("tg","activity created");
		bgIndex = getBgSetting();
		if (bgIndex != 0) {
			TextureManager.bgIndex = bgIndex;
		}

		mGLView = new DiceSurfaceView(this);
		setContentView(mGLView);
		this.setTitle(R.string.risk);

		if (getWarnSetting()) {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.warn);
			dialog.setTitle("Hello,�� ��");
			final CheckBox cb = (CheckBox)dialog.findViewById(R.id.checkbox);
			Button bt = (Button)dialog.findViewById(R.id.ok);
			bt.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					if(cb.isChecked()){
						saveWarnSetting(false);
					}
					dialog.cancel();
				}
			});
			dialog.show();
		}

	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		System.exit(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 0, 0, "���ñ���ǽ");
		menu.add(0, 1, 0, R.string.about);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id = item.getItemId();
		switch (id) {
		//��ʾ����
		case 1:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = LayoutInflater.from(this);
			View view = inflater.inflate(R.layout.about, null);
			builder.setView(view);
			builder.create().show();
			break;
		//��ʾ��������
		case 0:
			CharSequence[] items = { "��ά�ռ�", "��Ϣ����", "����ݼ�" };
			AlertDialog.Builder builder01 = new AlertDialog.Builder(this);
			builder01.setTitle("ѡ�񱳾�ǽ");
			builder01.setSingleChoiceItems(items, bgIndex,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							// Toast.makeText(getApplicationContext(),
							// items[item], Toast.LENGTH_SHORT).show();
							if (bgIndex != item) {
								bgIndex = item;
								mGLView.resetBackground(item);
								saveBgSetting(item);
							}
							dialog.cancel();
						}
					});
			AlertDialog alert = builder01.create();
			alert.show();
			break;
		}

		return true;
	}
/**
 * ���汳��������
 * @param bgIndex
 */
	private void saveBgSetting(int bgIndex) {
		SharedPreferences settings = getSharedPreferences("DiceSetting", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("bgIndex", bgIndex);
		editor.commit();
	}
/**
 * ȡ�ñ�������������
 * @return
 */
	private int getBgSetting() {
		SharedPreferences settings = getSharedPreferences("DiceSetting", 0);
		return settings.getInt("bgIndex", 0);
	}
/**
 * ������������
 * @param warn
 */
	private void saveWarnSetting(boolean warn) {
		SharedPreferences settings = getSharedPreferences("DiceSetting", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("canWarn", warn);
		editor.commit();
	}
/**
 * �����Ƿ���ʾ����
 * @return
 */
	private boolean getWarnSetting() {
		SharedPreferences settings = getSharedPreferences("DiceSetting", 0);
		return settings.getBoolean("canWarn", true);
	}
}
