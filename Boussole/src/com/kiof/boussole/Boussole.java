package com.kiof.boussole;

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Boussole extends Activity {
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	private CompassView mCompassView;
	private SensorManager mSensorManager;
	private Sensor sensor;
	private MediaPlayer mMediaPlayer;
	
	private int angle;

	private static final String SOUND = "sound";
	private static int RETURN_SETTING = 1;
	private static final int MARGE = 2;
	private static final String TAG = "BOUSSOLE";
	
	//Notre listener sur le capteur de la boussole numérique
	private final SensorEventListener sensorListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			updateOrientation(event.values[SensorManager.DATA_X]);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
		// Get Preferences
		PreferenceManager.setDefaultValues(mContext, R.xml.setting, false);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		setContentView(R.layout.main);

		// Display change log if new version
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun())
			new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title),
				android.R.drawable.ic_menu_info_details).show();

		// Background music
		if (mSharedPreferences.getBoolean(SOUND, false))
			playSound(R.raw.bgmusic);

        mCompassView = (CompassView)findViewById(R.id.compassView);
        //Récupération du gestionnaire de capteurs
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //Demander au gestionnaire de capteur de nous retourner les capteurs de type boussole
        List<Sensor> sensors =mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        //s’il y a plusieurs capteurs de ce type on garde uniquement le premier
        if (sensors.size() > 0) {
        	sensor = sensors.get(0);
        }
        
    }
    
	//Mettre a jour l'orientation
    protected void updateOrientation(float rotation) {
//    	Log.d(TAG, "Rotation : " + (int) rotation);
		if (Math.abs((angle - (int) rotation)) > MARGE)
			if (mSharedPreferences.getBoolean(SOUND, false)) {
				if (mMediaPlayer == null) {
					mMediaPlayer = MediaPlayer.create(mContext, R.raw.turn);
				}
				try {
					mMediaPlayer.start();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
		mCompassView.setNorthOrientation(rotation);
		angle = (int) (rotation);
	}

	@Override
    protected void onResume(){
    	super.onResume();
    	//Lier les évènements de la boussole numérique au listener
    	mSensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		//Retirer le lien entre le listener et les évènements de la boussole numérique
		mSensorManager.unregisterListener(sensorListener);
		if (mMediaPlayer != null) mMediaPlayer.release();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.setting:
			startActivityForResult(new Intent(Boussole.this, Setting.class), RETURN_SETTING);
			return true;
		case R.id.share:
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title));
			sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
			sharingIntent.putExtra(Intent.EXTRA_TEMPLATE, Html.fromHtml(getString(R.string.share_link)));
			sharingIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getString(R.string.share_link)));
			startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_with)));
			return true;
		case R.id.about:
			new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title), android.R.drawable.ic_menu_info_details).show();
			return true;
		case R.id.other:
			Intent otherIntent = new Intent(Intent.ACTION_VIEW);
			otherIntent.setData(Uri.parse(getString(R.string.other_link)));
			startActivity(otherIntent);
			return true;
		case R.id.quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	int playSound(int soundId) {
		MediaPlayer mp = MediaPlayer.create(mContext, soundId);
		if (mp == null) return 0;
		mp.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				mp.release();
			};
		});
		mp.start();
		return mp.getDuration();
	}

}
