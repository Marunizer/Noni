package menu.noni.android.noni.model3D.util;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by mende on 12/13/2017.
 *
 * Purpose of this class is to keep track of the Restaurant information gathered from Database
 * Currently used only in RestaurantViewActivity, also that is where the data is filled
 *
 * Idea : Could maintain within an Arraylist and pass it on through Activities if want to fill info before
 * RestaurantViewActivity
 */

public class Restaurant {

    private String name;
    private String coordinateKey;
    private float distanceAway; //to be used when finally calculating distance from user
    private int generalCost; //assigns number of '$'
    private String streetAddress;

    //Will hold keywords that will be used for emojis, Just text for now
    private ArrayList emojiList = new ArrayList<String>() ;

    public Restaurant(String name,Location location,String coordinateKey,int cost, String address, Location userLocation) {

        this.name = name;
        this.coordinateKey  = coordinateKey;
        this.distanceAway = location.distanceTo(userLocation);
        this.generalCost = cost;
        this.streetAddress = address;
    }

    public String getName() {
        return name;
    }

    public float getDistanceAway() {
        return distanceAway;
    }

    public String getCoordinateKey() {
        return coordinateKey;
    }

    public int getGeneralCost(){
        return generalCost;
    }

    public String getStreetAddress() {
        return streetAddress;
    }
}
