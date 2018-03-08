package menu.noni.android.noni.model3D.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;

/**
 * Created by mende on 10/30/2017.
 * Purpose of this Class is to always keep track of our users location data
 */

public final class LocationHelper {

    private static Location location;
    private static float latitude;
    private static float longitude;
    private static String address;
    private static String city;
    private static String state;
    private static String zipcode;
    private static int radius = 8; //8 gathers the most restaurant , Not certain what 8 truly means 8 meters? 8 miles? Must look up Documentation that geoFire uses

    public static int getRadius() {
        return radius;
    }

    public static void setRadius(int radius) {
        LocationHelper.radius = radius;
    }

    public static String getZipcode() {
        return zipcode;
    }

    public static void setZipcode(String zipcode) {
        LocationHelper.zipcode = zipcode;
    }

    //Assuming we were only given a zipcode, turn that zipcode into the most specific location data we can muster
    public static void setZipcodeAndAll(final String zipcode, Context context) throws IOException {

        LocationHelper.zipcode = zipcode;
        final Location[] mLastLocation = new Location[1];

        final Geocoder geocoder = new Geocoder(context);

        //TODO: This sometimes times out! Possible Solution: Have permission to Network State
        //This seems to only time out on an emulator. Resetting emulator typically fixes this
        //However, if it is not just an Emulator problem. FIX ASAP.

        List<Address> addresses = geocoder.getFromLocationName(zipcode, 1);
        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);

            mLastLocation[0] = new Location(zipcode);
            mLastLocation[0].setLatitude((float) address.getLatitude());
            mLastLocation[0].setLongitude((float) address.getLongitude());

            setLocation(mLastLocation[0]);
            setLongitude((float) address.getLongitude());
            setLatitude((float) address.getLatitude());
            setAddress(address.getAddressLine(0));
        }
    }

    public static float getLatitude() {
        return latitude;
    }

    public static void setLatitude(float latitude) {
        LocationHelper.latitude = latitude;
    }

    public static float getLongitude() {
        return longitude;
    }

    public static void setLongitude(float longitude) {
        LocationHelper.longitude = longitude;
    }

    public static String getAddress() {
        return address;
    }

    public static void setAddress(String address) {
        LocationHelper.address = address;
    }

    public static String getCity() {
        return city;
    }

    public static void setCity(String city) {
        LocationHelper.city = city;
    }

    public static String getState() {
        return state;
    }

    public static void setState(String state) {
        LocationHelper.state = state;
    }

    public static Location getLocation() {
        return location;
    }

    public static void setLocation(Location location) {
        LocationHelper.location = location;
    }
}
