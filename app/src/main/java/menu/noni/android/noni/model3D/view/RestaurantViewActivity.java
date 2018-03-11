package menu.noni.android.noni.model3D.view;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.LocationHelper;
import menu.noni.android.noni.model3D.util.Restaurant;
import menu.noni.android.noni.util.Utils;

import static android.content.ContentValues.TAG;

/**
 * Created by mende on 12/16/2017.
 */

/**
 * In this Activity, the list of available restaurants nea the user is displayed
 * TODO List:
 *     * (IGNORE FOR NOW) When checking if the restaurant found is already within the ArrayList, check based on
 *      2 parameters (name AND location) instead of just one. (Just in case for the future, multiple restaurants with same geoLocation)
 *
 *     * (High Priority) We actually don't want our real-time database to have a real-time listener. Right now if any change is made to database,
 *       app will crash because it noticed the change. Just make one call to a snapshot of data, do not remain listening
 *
 *     * When a default picture was added to the imageView inside card_view_restaurant,
 *      it seems to delay the process of showing items, and does it all at once instead.
 *          - Replace default image with a very small default picture unlike the one currently there
 *
 *    * Make images appear fluidly with little to no delay
 *
 *    * Right now Location that is Displayed is the full Address, might be too specific and therefor creep users out, might instead want to
 *      only show city, and state
 *      OR
 *      ask for an address instead of asking for a zipcode so its not unwarranted.
 *
 *    *Maybe have access firebase data before this class, and just pass it in
 *
 *    Low priority Ideal functionality: Let user manage the order of what comes up based on average price, distance, alphabetical, etc...
 *    - Will need to make a UI change to add this
 *
 *
 *    *Scale Concern (low priority - for now) Do not show all options at same time, but only show maybe 15,
 *     then keep showing more, the farther down a user goes
 */

public class RestaurantViewActivity extends AppCompatActivity implements MyAdapter.AdapterCallback, LocationDialogFragment.NoticeDialogListener {

    private ArrayList<Restaurant> restaurant = new ArrayList<>();
    private ArrayList<GeoLocation> restaurantGeoChecker = new ArrayList<>();
    private RecyclerView mRecyclerView;

    //may want to make local where called
    private MyAdapter mAdapter;
    //may want to make local where called
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    Toolbar toolbar;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Set up UI components
        toolbar= findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        setContentView(R.layout.activity_restaurant_select);
        textView = findViewById(R.id.address_text);
        textView.setText(LocationHelper.getAddress());
        textView.setPaintFlags(textView.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);

        mRecyclerView = findViewById(R.id.restaurant_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mySwipeRefreshLayout = findViewById(R.id.swiperefresh);

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        swipeUpdate();
                    }
                }
        );

        //Access Firebase and gather restaurant data
        prepareRestaurantArray();
    }

    //When user swipes down at top of screen, update information
    public void swipeUpdate(){
        //Toast to notify a refresh has started, calls the prepareRestaurantArray() method again
        Toast.makeText(getApplicationContext(),"Refreshing",Toast.LENGTH_SHORT).show();
        prepareRestaurantArray();
    }

    //Communicates with Adapter
    @Override
    public void onMethodCallback(String key, String name) {
        onRestaurantClick(key, name);
    }

    //  Access Firebase
    //  Access GeoFire
    //  Prepare List to display
    void prepareRestaurantArray()
    {
        //Prepare Firebase references and keys
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference();
        final String NAME_KEY = "restaurant_name";
        final String LAT_KEY = "lat";
        final String LONG_KEY = "long";
        final String COST_KEY = "dollar_signs";

        //reset in case there is an update. Make sure everything is clean slate so no redundant data
        restaurant.clear();
        restaurantGeoChecker.clear();

        myRef.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                GeoFire geoFire;
                geoFire = new GeoFire(myRef.child("geofire"));

                //Purpose of this is to know what area to look around for finding restaurants
                double latitude = LocationHelper.getLatitude();
                double longitude  = LocationHelper.getLongitude();

                //this will be the initial radius always at 5-10 or some standard, and then whatever changed to after
                double radius = LocationHelper.getRadius();

                //hardcoded values for testing purposes, eventually remove
                //Set to My/Farzas house
                latitude = 28.546373;
                longitude = -81.162192;

                //A GeoFire GeoQuery takes in the latitude, longitude, and finally the radius based on kilometers.
                //Probably want to make multiple queries incrementing the radius based on some calculation
                GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latitude,longitude), radius);
                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {

                        //Set the lat,long to compare to, geofire will help us figure out what restaurants
                        //are closest to us using geo-coordinates and radius
                        String myLat = String.format(java.util.Locale.US,"%.6f", location.latitude);
                        String myLong =  String.format(java.util.Locale.US,"%.6f", location.longitude);
                        String locationKey = myLat + myLong;
                        locationKey = Utils.cleanLatLongKey(locationKey);

                        System.out.println("Looking for Location Key : " + locationKey);

                        for (DataSnapshot item: dataSnapshot.getChildren()) {
                            if (item.getKey().compareTo(locationKey) >= 0)
                            {
                                String name = item.child(NAME_KEY).getValue().toString();
                                String item_lat = item.child(LAT_KEY).getValue().toString();
                                String item_long = item.child(LONG_KEY).getValue().toString();
                                int dollar_signs =Integer.valueOf(item.child(COST_KEY).getValue().toString());
                                Location rest_location = new Location(String.valueOf(location));
                                rest_location.setLongitude(Double.parseDouble(item_long));
                                rest_location.setLatitude(Double.parseDouble(item_lat));


                                //If we have not already accounted for this restaurant, add it, else ignore
                                if(!restaurantGeoChecker.contains(location)){

                                    restaurant.add(new Restaurant(name, rest_location, item.getKey(),dollar_signs));
                                    restaurantGeoChecker.add(location);
                                    System.out.println("RestaurantViewActivity: ADDING NEW RESTAURANT : " + name + ", " + item_lat + ", " + item_long + "  item.getKey() = " + item.getKey() + " location = " + location);

                                    //Updates entire arraylist
                                    setRestaurant(restaurant);
                                }
                            }
                        }
                    }

                    @Override
                    public void onKeyExited(String key) {
                               System.out.println(String.format("Key %s is no longer in the search area", key));
                    }

                    @Override
                    public void onKeyMoved(String key, GeoLocation location) {
                               System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
                    }

                    @Override
                    public void onGeoQueryReady() {
                        //stackoverflow.com/questions/4066538/sort-an-arraylist-based-on-an-object-field
                        //Sort Restaurant arraylist based on distance
                        Collections.sort(restaurant, new Comparator<Restaurant>(){
                            public int compare(Restaurant o1, Restaurant o2){
                                if(o1.getDistanceAway() == o2.getDistanceAway())
                                    return 0;
                                return o1.getDistanceAway() < o2.getDistanceAway() ? -1 : 1;
                            }
                        });

                        //Reload list with appropriate distance order
                        reloadData();
                        System.out.println("All initial data has been loaded and events have been fired!");
                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {
                        System.err.println("There was an error with this query: " + error);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    //Set new list of restaurants
    void setRestaurant(ArrayList restaurant)
    {
        this.restaurant = restaurant;
    }

    //Fills Adapter (Recycler Card View) with data and displays it
    void reloadData()
    {
        textView.setText(LocationHelper.getAddress());

        final Context context = this;
        if (mySwipeRefreshLayout.isRefreshing())
        {
            mySwipeRefreshLayout.setRefreshing(false);
        }
        mAdapter = new MyAdapter(restaurant, context);
        mRecyclerView.setAdapter(mAdapter);
    }

    public void onRestaurantClick(String key, String restName){

        Intent intent = new Intent(RestaurantViewActivity.this.getApplicationContext(), ModelActivity.class);
        Bundle b = new Bundle();
        b.putString("assetDir", getFilesDir().getAbsolutePath());
        b.putString("coordinateKey", key);
        b.putString("restaurantName", restName);
        intent.putExtras(b);
        RestaurantViewActivity.this.startActivity(intent);
    }

    @SuppressLint("WrongConstant")
    public void changeAddress(View view) {
        DialogFragment newFragment = new LocationDialogFragment();
        newFragment.show(getFragmentManager(), "locationMenu");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        prepareRestaurantArray();
        //reloadData();
    }
}

class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private AdapterCallback adapterCallback;
    private Context context;

    //used to for Glide to cache images from firebase storage
    private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();

    //This is the restaurant data
    private ArrayList mDataset;

     static class ViewHolder extends RecyclerView.ViewHolder {

         //Declare and then initiate UI components of CardView
         CardView cv;
         TextView restName;
         TextView restDistance;
         ImageView restImage;
         TextView restCost;
         String coordinateKey = " ";

         ViewHolder(final View itemView){
            super(itemView);
            cv = itemView.findViewById(R.id.restaurant_card);
            restName = itemView.findViewById(R.id.restaurant_name);
            restDistance = itemView.findViewById(R.id.restaurant_distance);
            restImage = itemView.findViewById(R.id.restaurant_image);
            restCost = itemView.findViewById(R.id.restaurant_cost);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    MyAdapter(ArrayList myDataset, Context context) {

        this.mDataset = myDataset;
        this.context = context;
        try {
            this.adapterCallback = ((AdapterCallback) context);
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement AdapterCallback.");
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_view_restaurant, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        //Get the path to the file from the data set
        String path = "restsTableImages" + File.separator + ((Restaurant) mDataset.get(position)).getName() + "_main_image.png";
        //Create a StorageReference variable to store the path to the image
        StorageReference image = fbStorageReference.child(path);


        //Serve this path to Glide which is put into the image holder and cached for us
        //Can change withCrossFade timer to change fade in time, in milliseconds.
        GlideApp.with(context)
                .load(image)
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .override(600,600)
                .into(holder.restImage);

        holder.restName.setText(((Restaurant) mDataset.get(position)).getName());

        //Add '$' based on the number provided
        int dollar_cost = ((Restaurant) mDataset.get(position)).getGeneralCost();
        StringBuilder cost = new StringBuilder("$");
        if (dollar_cost != 1)
        {
            for (int i = 1 ; i< dollar_cost;i++)
            {
                cost.append("$");
            }
        }
        holder.restCost.setText(cost);

        //Handle possible cases for distance away and display
        float milesAway = metersToMiles(((Restaurant) mDataset.get(position)).getDistanceAway());
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        milesAway = Float.valueOf(decimalFormat.format(milesAway));

        if (milesAway < 1)
        {
            holder.restDistance.setText("less than a mile away");
        }
        else if (milesAway == 1)
        {
            holder.restDistance.setText(String.valueOf(milesAway) + " mile away");
        }
        else
        {
            holder.restDistance.setText(String.valueOf(milesAway) + " miles away");
        }

        holder.coordinateKey = (((Restaurant) mDataset.get(position)).getCoordinateKey());

        holder.cv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                adapterCallback.onMethodCallback(holder.coordinateKey, holder.restName.getText().toString());
            }}
        );
    }

    private float metersToMiles(float meters)
    {
        return (float) (meters*0.000621371192);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface AdapterCallback {
        void onMethodCallback(String key, String name);
    }
}