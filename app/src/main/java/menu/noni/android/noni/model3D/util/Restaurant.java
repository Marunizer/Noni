package menu.noni.android.noni.model3D.util;

import android.location.Location;

import java.util.ArrayList;

/**
 * Created by mende on 12/13/2017.
 */

public class Restaurant {

    private String name;
    private String coordinateKey;
    private float distanceAway; //to be used when finally calculating distance from user
    private ArrayList emojiList = new ArrayList<String>() ;//Will hold keywords that will be used for emojis
    private int generalCost;

    public Restaurant(String name,Location location,String coordinateKey,int cost) {
        this.name = name;
        this.coordinateKey  = coordinateKey;
        this.distanceAway = location.distanceTo(LocationHelper.getLocation());
        this.generalCost = cost;
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
}
