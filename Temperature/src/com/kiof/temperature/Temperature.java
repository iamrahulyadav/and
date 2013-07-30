package com.kiof.temperature;

import org.json.JSONException;

import com.kiof.weather.R;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Temperature extends Activity {

	private Context mContext;
	private Resources mResources;
	private SharedPreferences mSharedPreferences;
	private LocationManager mLocationManager;
	private Location lastKnownLocation;

	protected static final String MY_COUNTRY = "mycountry";
	protected static final String CHECK_VOLUME = "checkvolume";
	protected static final String VOLUME_MAX = "volumemax";
	protected static final String VOLUME_RESTORE = "volumerestore";
	protected static final String KEEP_MY_COUNTRY = "keepmycountry";
	protected static final String AUTO_PLAY = "autoplay";
	protected static final String SYNCHRO = "synchro";
	protected static final String TAG = "HymneActivity";
	protected static final String NTP_SERVER = "pool.ntp.org";
	// protected static final String NTP_SERVER = "fr.ntp.org";
	// protected static final String NTP_SERVER = "canon.inria.fr";
	protected static final int NTP_NB_TRY = 5;
	protected static final int NTP_SLEEP_TIME = 1000;
	protected static final int TIME_WAIT = 3;
	protected static final int RETURN_SETTING = 1;
	protected static final int NETWORK_TIMEOUT = 10000;
	private String[] countries;
	private TypedArray flags;
	private TypedArray sounds;
	private int myCountry;
	private int initVolume, maxVolume;
	private long gpsTime = 0, gpsDelta = 0, ntpTime = 0, ntpDelta = 0,
			newDelta = 0, sysTime = 0;

	private TextView cityText;
	private TextView condDescr;
	private TextView temp;
	private TextView press;
	private TextView windSpeed;
	private TextView windDeg;

	private TextView hum;
	private ImageView imgView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		String initRequest = "?lat=35&lon=139";

		cityText = (TextView) findViewById(R.id.cityText);
		condDescr = (TextView) findViewById(R.id.condDescr);
		temp = (TextView) findViewById(R.id.temp);
		hum = (TextView) findViewById(R.id.hum);
		press = (TextView) findViewById(R.id.press);
		windSpeed = (TextView) findViewById(R.id.windSpeed);
		windDeg = (TextView) findViewById(R.id.windDeg);
		imgView = (ImageView) findViewById(R.id.condIcon);

		mContext = this.getApplicationContext();
		mResources = this.getResources();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		// Acquire a reference to the system Location Manager
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Register the listener with the Location Manager to receive location updates
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

		lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		JSONWeatherTask weatherTask = new JSONWeatherTask();
		weatherTask.execute(new String[] { initRequest });
	}
	
	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener() {
	    public void onLocationChanged(Location location) {
	      // Called when a new location is found by the network location provider.
	      makeUseOfNewLocation(location);
	    }

	    private void makeUseOfNewLocation(Location location) {
			// TODO Auto-generated method stub
			Toast.makeText(mContext, "Location updated", Toast.LENGTH_SHORT).show();
			String request = "?lat=" + location.getLatitude() + "&lon=" + location.getLongitude();

			JSONWeatherTask weatherTask = new JSONWeatherTask();
			weatherTask.execute(new String[] { request });
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

	    public void onProviderEnabled(String provider) {}

	    public void onProviderDisabled(String provider) {}
	  };

	private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

		@Override
		protected Weather doInBackground(String... params) {
			Weather weather = new Weather();
			String data = ((new WeatherHttpClient()).getWeatherData(params[0]));

			try {
				weather = JSONWeatherParser.getWeather(data);

				// Let's retrieve the icon
				weather.iconData = ((new WeatherHttpClient()).getImage(weather.currentCondition.getIcon()));

			} catch (JSONException e) {
				e.printStackTrace();
			}
			return weather;

		}

		@Override
		protected void onPostExecute(Weather weather) {
			super.onPostExecute(weather);

			if (weather.iconData != null && weather.iconData.length > 0) {
				Bitmap img = BitmapFactory.decodeByteArray(weather.iconData, 0, weather.iconData.length);
				imgView.setImageBitmap(img);
			}

			cityText.setText(weather.location.getCity() + "," + weather.location.getCountry());
			condDescr.setText(weather.currentCondition.getCondition() + "(" + weather.currentCondition.getDescr() + ")");
			temp.setText("" + Math.round((weather.temperature.getTemp() - 275.15)) + "°C");
			hum.setText("" + weather.currentCondition.getHumidity() + "%");
			press.setText("" + weather.currentCondition.getPressure() + " hPa");
			windSpeed.setText("" + weather.wind.getSpeed() + " mps");
			windDeg.setText("" + weather.wind.getDeg() + "°");

		}

	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.setting:
			startActivityForResult(new Intent(Temperature.this, Setting.class),
					RETURN_SETTING);
			return true;
		case R.id.share:
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(Intent.EXTRA_TITLE,
					getString(R.string.share_title));
			sharingIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.share_title));
			sharingIntent.putExtra(Intent.EXTRA_TEMPLATE,
					Html.fromHtml(getString(R.string.share_link)));
			sharingIntent.putExtra(Intent.EXTRA_TEXT,
					Html.fromHtml(getString(R.string.share_link)));
			startActivity(Intent.createChooser(sharingIntent,
					getString(R.string.share_with)));
			return true;
		case R.id.about:
			new HtmlAlertDialog(this, R.raw.about,
					getString(R.string.about_title),
					android.R.drawable.ic_menu_info_details).show();
			return true;
		case R.id.other:
			Intent otherIntent = new Intent(Intent.ACTION_VIEW);
			otherIntent.setData(Uri.parse(getString(R.string.other_link)));
			startActivity(otherIntent);
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
	protected void onStop() {
		super.onStop();
		// Remove the listener you previously added
		mLocationManager.removeUpdates(locationListener);
	}

	protected void onDestroy() {
		super.onDestroy();
//		if (!mSharedPreferences.getBoolean(KEEP_MY_COUNTRY, false)) {
//			myCountry = -1;
//			// setSetting();
//			SharedPreferences.Editor editor = mSharedPreferences.edit();
//			editor.putInt(MY_COUNTRY, myCountry);
//			editor.commit();
//		}
	}

}