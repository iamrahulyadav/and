package com.kiof.hymne;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HymneActivity extends Activity {
	private Context mContext;
	private Resources mResources;
	private SharedPreferences mSharedPreferences;
	private AudioManager mAudioManager;
	private static final String MY_COUNTRY = "mycountry";
	private static final String CHECK_VOLUME = "checkvolume";
	private static final String VOLUME_MAX = "volumemax";
	private static final String VOLUME_RESTORE = "volumerestore";
	private static final String KEEP_MY_COUNTRY = "keepmycountry";
	private int myCountry;
//	private Boolean myCheckVolume, myVolumeMax, myVolumeRestore, myAutoPlay;

	private String[] countries;
	private TypedArray flags;
	// private TypedArray sounds;

	private static int RETURN_SETTING = 1;
	private int initVolume, maxVolume;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this.getApplicationContext();
		mResources = this.getResources();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		setContentView(R.layout.main);

		// Get Preferences
		PreferenceManager.setDefaultValues(this, R.xml.setting, false);
//		getSetting();
		myCountry = mSharedPreferences.getInt(MY_COUNTRY, -1);

		countries = mResources.getStringArray(R.array.countries);
		flags = mResources.obtainTypedArray(R.array.flags);
		// sounds = mResources.obtainTypedArray(R.array.sounds);

		Gallery gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new ImageAdapter(this));

		gallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				setMyFlag(position);
			}
		});

		if (myCountry >= 0) {
			setMyFlag(myCountry);
		}

		ImageView mView = (ImageView) findViewById(R.id.myflag);
		mView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Create an Intent to launch PlayHymne
				startActivity(new Intent(HymneActivity.this, PlayHymne.class));
			}
		});

		// Display change log if new version
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun()) new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title), android.R.drawable.ic_menu_info_details).show();

		// Audio management for initVolume control
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// Propose to set initVolume to max if it is not loud enough
		initVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		if (mSharedPreferences.getBoolean(CHECK_VOLUME, false)) {
			if ((2 * initVolume / maxVolume) < 1) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.volume_title);
				builder.setIcon(android.R.drawable.ic_menu_preferences);
				builder.setMessage(R.string.volume_question);
				builder.setNegativeButton(R.string.volume_no, null);
				builder.setPositiveButton(R.string.volume_yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mAudioManager.setStreamVolume(
								AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
						}
					});
				builder.create();
				builder.show();
			}
		} else {
			if (mSharedPreferences.getBoolean(VOLUME_MAX, false)) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
			}
		}

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
			startActivityForResult(new Intent(HymneActivity.this, Setting.class), RETURN_SETTING);
			return true;
		case R.id.about:
			new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title), android.R.drawable.ic_menu_info_details).show();
			return true;
		case R.id.quit:
			// Create out AlterDialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.quit_title);
			builder.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
			builder.setMessage(R.string.quit_message);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			builder.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(mContext, R.string.goingon,
									Toast.LENGTH_SHORT).show();
						}
					});
			builder.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RETURN_SETTING) {
			Toast.makeText(this, R.string.setting_saved, Toast.LENGTH_SHORT).show();
//			getSetting();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	protected void onDestroy() {
		if (!mSharedPreferences.getBoolean(CHECK_VOLUME, false) && mSharedPreferences.getBoolean(VOLUME_MAX, false) && mSharedPreferences.getBoolean(VOLUME_RESTORE, false)) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initVolume, AudioManager.FLAG_SHOW_UI);
		}
		if (!mSharedPreferences.getBoolean(KEEP_MY_COUNTRY, false)) {
			myCountry = -1;
//			setSetting();
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putInt(MY_COUNTRY, myCountry);
			editor.commit();
		}
		super.onDestroy();
	}

	private void setMyFlag(int position) {
		// Save country to preferences
		this.myCountry = position;
//		setSetting();
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(MY_COUNTRY, myCountry);
		editor.commit();

		ImageView mView = (ImageView) findViewById(R.id.myflag);
		mView.setImageResource(flags.getResourceId(position, 0));

		TextView mTextView = (TextView) findViewById(R.id.mycountry);
		mTextView.setText(countries[position]);

		TextView mTextView1 = (TextView) findViewById(R.id.comment);
		mTextView1.setText(R.string.flag_set);
	}

//	private void getSetting() {
//		myCountry = mSharedPreferences.getInt(MY_COUNTRY, -1);
//		myCheckVolume = mSharedPreferences.getBoolean(CHECK_VOLUME, true);
//		myVolumeMax = mSharedPreferences.getBoolean(VOLUME_MAX, false);
//		myVolumeRestore = mSharedPreferences.getBoolean(VOLUME_RESTORE, false);
//		myAutoPlay = mSharedPreferences.getBoolean(AUTO_PLAY, true);
//		myKeepMyCountry = mSharedPreferences.getBoolean(KEEP_MY_COUNTRY, true);
//	}
//
//	private void setSetting() {
//		SharedPreferences.Editor editor = mSharedPreferences.edit();
//		editor.putInt(MY_COUNTRY, myCountry);
//		editor.putBoolean(CHECK_VOLUME, myCheckVolume);
//		editor.putBoolean(VOLUME_MAX, myVolumeMax);
//		editor.putBoolean(VOLUME_RESTORE, myVolumeRestore);
//		editor.putBoolean(AUTO_PLAY, myAutoPlay);
//		editor.putBoolean(KEEP_MY_COUNTRY, myKeepMyCountry);
//		editor.commit();
//	}

	public class ImageAdapter extends BaseAdapter {
		int mGalleryItemBackground;
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
			TypedArray attr = mContext
					.obtainStyledAttributes(R.styleable.Hymne);
			mGalleryItemBackground = attr.getResourceId(
					R.styleable.Hymne_android_galleryItemBackground, 0);
			attr.recycle();
		}

		public int getCount() {
			return flags.length();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemRessourceId(int position) {
			return flags.getResourceId(position, 0);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView = new ImageView(mContext);

			imageView.setImageResource(flags.getResourceId(position, 0));
			imageView.setLayoutParams(new Gallery.LayoutParams(300, 200));
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setBackgroundResource(mGalleryItemBackground);

			return imageView;
		}
	}


}