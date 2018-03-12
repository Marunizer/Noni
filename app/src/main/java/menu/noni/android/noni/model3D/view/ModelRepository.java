package menu.noni.android.noni.model3D.view;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import menu.noni.android.noni.model3D.util.RestaurantMenu;
import menu.noni.android.noni.model3D.util.RestaurantMenuCategory;
import menu.noni.android.noni.model3D.util.RestaurantMenuItem;

/**
 * Created by Wiita on 3/10/2018.
 */

public class ModelRepository implements ValueEventListener {

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference myRef = database.getReference();

    private String coordinateKey;
    private String restaurantName;
    private String paramAssetDir;
    private boolean firstAccess = true;
    private ModelRepositoryListener listener;
    private RestaurantMenu restaurantMenu;
    final String databaseTitle = "menus";
    final String databaseSubTitle = "Categories";
    ModelDownloadHandler downloadHandler;

    /**
     * The file to load. Passed as input parameter
     */
    private String paramFilename;

    public ModelRepository(String coordinateKey,
                           String restaurantName,
                           String paramAssetDir,
                           String paramFilename,
                           ModelRepositoryListener listener,
                           RestaurantMenu restaurantMenu,
                           Context context) {

        this.coordinateKey = coordinateKey;
        this.restaurantName = restaurantName;
        this.paramAssetDir = paramAssetDir;
        this.paramFilename = paramFilename;
        this.listener = listener;
        this.restaurantMenu = restaurantMenu;
        downloadHandler = new ModelDownloadHandler(context);
    }

    public void getMenuItemModels(){
        myRef.child("menus/" + this.coordinateKey +"/Categories").addValueEventListener(this);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        //Will traverse every Category
        for (DataSnapshot categories : dataSnapshot.getChildren()) {     //categories = Most Popular

            //Create the Category, and keep reference
            RestaurantMenuCategory category = new RestaurantMenuCategory(categories.getKey());

            //Add category to our table, at the same time, we are naming the category
            restaurantMenu.allCategories.put(categories.getKey(), category);

            //Add category to our list to be able to easily traverse list by index
            listOfCategories.add(category);

            //get the icon name for the category, we go in one level deeper
            category.setCategoryIconName((String) (categories.child("category_icon_name").getValue()));

            //make reference fo the reference containing the items one level deeper
            DataSnapshot items = categories.child("items");

            //For each model in this category Add to allItems table
            for (DataSnapshot model : items.getChildren()) {
                //keep reference for deeper level of model data for each model
                DataSnapshot data = model.child("model_data_android");

                String descriptionText;

                //check if item has a description, Database is not consistent at the moment
                if (!model.child("description").exists()) {
                    descriptionText = model.getKey();
                } else {
                    descriptionText = (String) model.child("description").getValue();
                }

                //Create the restaurantMenuItem
                RestaurantMenuItem menuItem = new RestaurantMenuItem(
                        model.getKey(),
                        (String) (data.child("drc_name").getValue()),
                        (String) (data.child("mtl_name").getValue()),
                        (String) (data.child("texture_name").getValue()),
                        (String) (data.child("icon_name").getValue()),
                        descriptionText,
                        (String) (model.child("price").getValue()));

                //Add the restaurantMenu item to the table AND the order added to our list
                category.allItems.put(model.getKey(), menuItem);
                category.keyConverter.add(model.getKey());

                Log.d("prepareMenu - New Item", "ADDING NEW item : " + menuItem.getName() + " to the RestaurantMenu at coordinate: " + coordinateKey);

                if (firstAccess) {
                    firstAccess = false;
                    listener.onFirstItemLoaded(menuItem,categories.getKey());
                }
            }
        }

        downloadHandler.downloadAll();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    public interface ModelRepositoryListener{
        public void onFirstItemLoaded(RestaurantMenuItem item, String categoryKey);
    }
}
