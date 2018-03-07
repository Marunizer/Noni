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
 */

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	GoogleApiClient mGoogleApiClient;
	Location mLastLocation;
	Context context;

	static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

	//Not yet applied, I have been turning on storage through phone settings
	static final int MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Create an instance of GoogleAPIClient.
		createGoogleAPIClient();
	}


	//    EVERYTHING
	//
	//              GOOGLE
	//
	//                       API
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
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {

			//------------------------------------------------------------------------------
			ActivityCompat.requestPermissions(MainActivity.this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

			return;
		}
		this.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (mLastLocation != null) {
			try {
				sendLocation();
				Intent intent = new Intent(MainActivity.this.getApplicationContext(), RestaurantViewActivity.class);
				MainActivity.this.startActivity(intent);
				finish();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
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

	@Override
	public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					getMyLocation();
					Intent intent = new Intent(MainActivity.this.getApplicationContext(), RestaurantViewActivity.class);
					MainActivity.this.startActivity(intent);
					finish();

				}
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
				return;
			}
		}
	}

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
		LocationHelper.setLocationPermission(true);
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
