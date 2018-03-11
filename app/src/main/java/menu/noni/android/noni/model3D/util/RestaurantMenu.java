package menu.noni.android.noni.model3D.util;

import java.util.Hashtable;

/**
 * Created by mende on 12/12/2017.
 *
 * Purpose of Class is to keep track of the Restaurant RestaurantMenu that is split in Categories which contains the menu items
 * RestaurantMenu(Category(MenuItem()))
 *
 * Concern: Currently all data is thrown away after no longer at the restaurant, could link
 * to restaurant class and keep track of menu there to save data, however it takes up space, and
 * its inexpensive to query, way we have it now free's space. Just a thought.
 */

public class RestaurantMenu {

    public RestaurantMenu(String key) {
        this.key = key;
    }

    //May not be needed since we don't care about the RestaurantMenu when we are no longer at the restaurant
    private String key;
    private String restName;
    public Hashtable<String, RestaurantMenuCategory> allCategories = new Hashtable<>(); //Might wanna make private, just annoying to access

    //Probably should have a bar or some text at top of model Viewer where we display the resturant name
    public String getRestName() {
        return restName;
    }

    public void setRestName(String restName) {
        this.restName = restName;
    }

}
