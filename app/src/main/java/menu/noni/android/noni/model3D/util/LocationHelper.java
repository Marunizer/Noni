package menu.noni.android.noni.model3D.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;

/**
 * Created by marunizer on 10/30/2017.
 * Purpose of this Class is to always keep track of our users location data
 *
 * Concern: Geocoder sometimes times out and stops whole app - should do on a separate thread
 *   - research makes me think this is only an emulator problem and not a problem in real world, but something to keep in mind
 *
 *   Idea: as firebaseHelper does, keep an instance of location provider.
 *   I attempted this already but had problems with leaks since an instance of the client is tied to a single Activity... So could not be static
 */

public class LocationHelper {

    //False: User is using zipcode   True: User has allowed location permissions
    private static boolean LocationMethod = false;
    private static Location location;
    private static float latitude;
    private static float longitude;
    private static String address;
    private static String city;
    private static String state;
    private static String zipcode;
    private static String streetName;
    private static double radius = 8; //In Kilometers = 5 miles

    public static double getRadius() {
        return radius;
    }

    public static void setRadius(double radius) {

        double milesToKm = 1.621;

        LocationHelper.radius = milesToKm*radius;
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
            setLocationMethod(false);
            setLongitude((float) address.getLongitude());
            setLatitude((float) address.getLatitude());
            setAddress(address.getAddressLine(0));
            setStreetName(address.getThoroughfare());
        }
    }

    public static String findStreedAddress(final Location location, Context context){

        final Geocoder geocoder = new Geocoder(context);

        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty())
        {
            Address address = addresses.get(0);

            return address.getThoroughfare();
        }
        else
            return null;
    }

    public static boolean isLocationMethod() {
        return LocationMethod;
    }

    public static void setLocationMethod(boolean locationMethod) {
        LocationMethod = locationMethod;
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

    public static String getStreetName() {
        return streetName;
    }

    public static void setStreetName(String streetName) {
        LocationHelper.streetName = streetName;
    }
}
