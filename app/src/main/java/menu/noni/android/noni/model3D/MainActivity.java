package menu.noni.android.noni.model3D;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.Window;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import menu.noni.android.noni.model3D.util.LocationHelper;
import menu.noni.android.noni.model3D.view.LocationActivity;
import menu.noni.android.noni.model3D.view.RestaurantViewActivity;

/**
 * The purpose of this Activity is to be the very first Screen the user see's and find location or choose to include their own.
 *
 * TODO:
 * Implement using WIFI (network calls) over gps when available, consumes less battery, better accuracy
 *
 *
 * When asking for Permissions, ask for external Storage permissions as well as for location to get them both out of the way
 * I believe an example of this is set up in Model Activity, but maybe not fully implemented
 * -Would still want to keep permission check at Model Activity in case user Declines location, therefor
 *  declining both, unless asked one at a time, in which case, remove Permission check in ModelActivity
 *
 *
 *  IDEA: Might want to consider accessing Firebase here and in Location Activity to already fill Restaurant class before
 *  RestaurantViewActivity is accessed
 *        - reason: It takes Firebase a bit of time to connect sometimes, leads to a long blank screen
 *                  Although A reason for this may be because of Glide caching images, not the database query. Must check
 *
 */

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	GoogleApiClient mGoogleApiClient;
	Context context;
	static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

	//Not yet applied, I have been turning on storage through phone settings
	static final int MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE = 1;

	//Stores the Location of user, involving specific coordinates and such
	Location mLastLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Create an instance of GoogleAPIClient.
		createGoogleAPIClient();
	}


	private void createGoogleAPIClient() {
		// Create an instance of GoogleAPIClient.
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
		}
	}
	//------------------------------------------------------------------------------
	//ref: Requesting Permissions at Run Time
	//http://developer.android.com/training/permissions/requesting.html
	//------------------------------------------------------------------------------
	private void getMyLocation() {

		//If Permissions are not granted, as for permission
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(MainActivity.this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

			return;
		}

		//TODO: FusedLocationApi is deprecated, find replacement/solution/work around. could effect app in future
		this.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

		//Location exists, set it as our used location, move on to Restaurant View
		if (mLastLocation != null) {
			try {
				sendLocation();
				Intent intent = new Intent(MainActivity.this.getApplicationContext(), RestaurantViewActivity.class);
				MainActivity.this.startActivity(intent);
				finish();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//Location could not be accessed, therefor try using zipcode.
		else{
			//This variable keeps last used zipcode user has used in the past
            SharedPreferences sharedZip = getSharedPreferences("ZIP_PREF",MODE_PRIVATE);
            final String restoredZip = sharedZip.getString("zipCode", null);

            //If user has a saved zipcode, move on to Restaurant View
            if (restoredZip != null)
            {
				try {
				LocationHelper.setZipcodeAndAll(restoredZip,context);
				Intent intent = new Intent(MainActivity.this.getApplicationContext(), RestaurantViewActivity.class);
				MainActivity.this.startActivity(intent);
				finish();
					} catch (IOException e) {
						e.printStackTrace();
						}
            }
            //No saved zipcode, go to LocationActivity to ask for one
            else
            {
                Intent intent = new Intent(MainActivity.this.getApplicationContext(), LocationActivity.class);
                MainActivity.this.startActivity(intent);
                finish();
            }
		}
	}

	@Override
	public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {

				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					//Permission is granted, move on
					getMyLocation();

					//TODO: I believe These lines happen in 'getMyLocation', so probably safe to delete, must test
					//********************************************************
					Intent intent = new Intent(MainActivity.this.getApplicationContext(), RestaurantViewActivity.class);
					MainActivity.this.startActivity(intent);
					finish();
					//*********************************************************
				}

				//permission not granted, see if have saved zip and move on. if no saved zip, ask for one in LocationActivity
				else {
					SharedPreferences sharedZip = getSharedPreferences("ZIP_PREF",MODE_PRIVATE);

					final String restoredZip = sharedZip.getString("zipCode", null);
					if (restoredZip != null)
					{
						try {
							LocationHelper.setZipcodeAndAll(restoredZip,context);
							Intent intent = new Intent(MainActivity.this.getApplicationContext(), RestaurantViewActivity.class);
							MainActivity.this.startActivity(intent);
							finish();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else
					{
						Intent intent = new Intent(MainActivity.this.getApplicationContext(), LocationActivity.class);
						MainActivity.this.startActivity(intent);
						finish();
					}
				}
			}
		}
	}

	//Assuming we have a known live location, set the data to our LocationHelper
	//way want to consider having to pass mLastLocation as a parameter instead of accessing global one
	void sendLocation() throws IOException {
		Geocoder geocoder;
		List<Address> addresses;
		geocoder = new Geocoder(this, Locale.getDefault());

		addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);

		LocationHelper.setLocation(mLastLocation);
		LocationHelper.setLongitude((float)mLastLocation.getLongitude());
		LocationHelper.setLatitude((float)mLastLocation.getLatitude());
		LocationHelper.setAddress(addresses.get(0).getAddressLine(0));
		LocationHelper.setState(addresses.get(0).getAdminArea());
		LocationHelper.setCity(addresses.get(0).getLocality());
		LocationHelper.setZipcode(addresses.get(0).getPostalCode());
	}

	@Override
	protected void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();
		super.onStop();
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		getMyLocation();
	}

	@Override
	public void onConnectionSuspended(int i) {
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
	}
}
