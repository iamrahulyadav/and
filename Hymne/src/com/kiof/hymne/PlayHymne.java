package com.kiof.hymne;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class PlayHymne extends Activity implements LocationListener {
	private Context mContext;
	private Resources mResources;
	private SharedPreferences mSharedPreferences;
	private LocationManager mLocationManager;
	private MediaPlayer mMediaPlayer = null;
	private NmeaListener mNmeaListener;
	
	protected static final String MY_COUNTRY = "mycountry";
	protected static final String AUTO_PLAY = "autoplay";
	protected static final String SYNCHRO = "synchro";
	protected static final int TIME_WAIT = 3;
	protected static final String TAG = "PlayHymne";

	private int myCountry;
	private long absTime = 0;
	private TypedArray flags;
	private TypedArray sounds;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);

		mContext = getApplicationContext();
		mResources = getResources();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		setContentView(R.layout.flag);

//		countries = mResources.getStringArray(R.array.countries);
		flags = mResources.obtainTypedArray(R.array.flags);
		sounds = mResources.obtainTypedArray(R.array.sounds);

		// Get MyFlag for shared Preferences
		myCountry = mSharedPreferences.getInt(MY_COUNTRY, -1);
		
		ImageView mView = (ImageView) findViewById(R.id.flag);
		mView.setImageResource(flags.getResourceId(myCountry, 0));
		mView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}

	protected void onStart() {
		super.onStart();
		if (mSharedPreferences.getBoolean(AUTO_PLAY, false)) {
//			if (mSharedPreferences.getBoolean(SYNCHRO, false)) {
//				int i = 0;
//				Log.d(TAG, "absTime : " + absTime);
//				while (absTime == 0 && i < TIME_WAIT) {
////					Toast.makeText(mContext, R.string.wait_synchro, Toast.LENGTH_SHORT).show();
////					try {
////						Thread.sleep(3000);
////					} catch (InterruptedException e) {
////						e.printStackTrace();
////					}
//					i++;
//				}
//				Log.d(TAG, "i : " + i);
//				Log.d(TAG, "absTime : " + absTime);
//				if (absTime == 0) {
//					Toast.makeText(mContext, R.string.pb_synchro, Toast.LENGTH_SHORT).show();
//				}

			if (mSharedPreferences.getBoolean(SYNCHRO, false)) {
				// Register NMEA listener
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

				mNmeaListener = new GpsStatus.NmeaListener() {
					private static final String NMEA = "$GPRMC";

					public void onNmeaReceived(long timestamp, String nmea) {
						// check that this is an RMC string
						if (!nmea.startsWith(NMEA)) return;
						if (absTime != 0) return;

						Log.d(TAG, "Timestamp : " + timestamp + " NMEA : " + nmea);

						// extract time, date
						String[] tokens = nmea.split(",");
						String utcTime = tokens[1];
						String utcDate = tokens[9];
						Log.d(TAG, "utcTime : " + utcTime);
						Log.d(TAG, "utcDate : " + utcDate);
						// parse
						SimpleDateFormat df = new SimpleDateFormat("HHmmss.S ddMMyy Z");
						String dateStr = utcTime + " " + utcDate + " +0000";
						Log.d(TAG, "dateStr : " + dateStr);

						try {
							Date date = df.parse(dateStr);
							// do something with date here ...
							Log.d(TAG, "Date : " + date.toString());
							absTime = date.getTime();
							Log.d(TAG, "absTime : " + Long.toString(absTime));
						} catch (java.text.ParseException e) {
							e.printStackTrace();
						}
						mLocationManager.removeNmeaListener(mNmeaListener);
						// mLocationManager.removeNmeaListener(this);

						mMediaPlayer = MediaPlayer.create(mContext, sounds.getResourceId(myCountry, 0));
						if (mMediaPlayer != null) {
							mMediaPlayer.setLooping(true);
							int duration = mMediaPlayer.getDuration();
							Log.d(TAG, "duration : " + duration);
							long msec = absTime % duration;
							Log.d(TAG, "msec : " + Long.toString(msec));
							mMediaPlayer.seekTo((int) msec);
							mMediaPlayer.start();
						}
						Toast.makeText(mContext, R.string.message_flag, Toast.LENGTH_SHORT).show();

					}
				};
				
				Toast.makeText(mContext, R.string.wait_synchro, Toast.LENGTH_SHORT).show();
				mLocationManager.addNmeaListener(mNmeaListener);
			} else { 
				mMediaPlayer = MediaPlayer.create(mContext, sounds.getResourceId(myCountry, 0));
				if (mMediaPlayer != null) {
					mMediaPlayer.setLooping(true);
					int duration = mMediaPlayer.getDuration();
					Log.d(TAG, "duration : " + duration);
					long msec = absTime % duration;
					Log.d(TAG, "msec : " + Long.toString(msec));
					mMediaPlayer.seekTo((int) msec);
					mMediaPlayer.start();
				}
				Toast.makeText(mContext, R.string.message_flag, Toast.LENGTH_SHORT).show();		
			}				
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
		}
		mLocationManager.removeNmeaListener(mNmeaListener);
		mLocationManager.removeUpdates(this);
	}
	
	@Override
	public void onLocationChanged(Location location) {		
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
}
