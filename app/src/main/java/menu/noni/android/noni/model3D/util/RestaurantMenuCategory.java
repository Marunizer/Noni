package menu.noni.android.noni.model3D.util;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Wiita on 3/9/2018.
 */

public class RestaurantMenuCategory {

    public RestaurantMenuCategory(String name)
    {
        this.name = name;
    }

    //Will need to be used after I know how the category is selected
    private String name;
    private String categoryIconName;

    //keeps a numbered index of the items entered to hashtable, used as key converter
    //when working with adapters that return the position, indicating a menu item
    //These probz need to be private, just annoying to set up for now
    public ArrayList<String> keyConverter = new ArrayList<>();
    public Hashtable<String, RestaurantMenuItem> allItems = new Hashtable<>();

    public String getName() {
        return name;
    }

    public void setCategoryIconName(String categoryIconName) {
        this.categoryIconName = categoryIconName;
    }

    public String getCategoryIconName() {
        return categoryIconName;
    }
}
