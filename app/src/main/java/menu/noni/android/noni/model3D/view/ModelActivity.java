package menu.noni.android.noni.model3D.view;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.RestaurantMenu;
import menu.noni.android.noni.model3D.util.RestaurantMenuCategory;
import menu.noni.android.noni.model3D.util.RestaurantMenuItem;

import static menu.noni.android.noni.model3D.view.ModelFragment.CONTENT_VIEW_TAG;

/**
 * This activity represents the container for our 3D viewer.
 * TODO List:
 *
 * 	    * Firebase gets accessed before UI is set up: result : ugly transition into activity, maybe re-arranging
 * 	        or something else can lead to a nicer transition to new screen

 * 	    *Handle Different category options, right now just assumed first category
 * 	    *Will Find out how we change Categories, talk to Farza on how UI should be made
 * 	    Probably Solution: Make a Dialog Fragment Class similar to LocationDialogFragment, gets created upon user touching Category textView(to be button)
 *
 * 	    *We should know by the onCreate() if the device supports AR at all, this way, we can hide the AR View option if not needed
 * 	    *If AR button is checked, we must check if user has ARCore installed on their phone, if not, lead them to the playstore
 * 	    - https://github.com/google-ar/arcore-android-sdk/issues/162#event-1489578234
 *
 * 	    + Further more within code
 */
public class ModelActivity extends FragmentActivity implements CircleMenuAdapter.AdapterCallback,
		ModelRepository.ModelRepositoryListener,
		ModelDownloadHandler.ModelDownloadHandlerListener {

//	// Used to load the 'native-lib' library on application startup.
//	static {
//		System.loadLibrary("native-lib");
//	}
//
//	/**
//	 * A native method that is implemented by the 'native-lib' native library,
//	 * which is packaged with this application.
//	 */
//	public native String stringFromJNI();
//	//call method as normal with stringFromJNI()

	//eventually try to have deeper levels of control, but for now one folder for all works
	private File storageFile;

	// Storage Permissions
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	private RestaurantMenu menu;
	private RestaurantMenuItem menuItem;
	private ArrayList<RestaurantMenuCategory> listOfCategories = new ArrayList<>();
	private String paramFilename;
	private String paramAssetDir;
	private String textureFilename;
	private String coordinateKey;
	private String restaurantName;

	private RestaurantMenu restaurantMenu;
	//convert to local if works
	private CircleMenuAdapter mAdapter;
	//convert to local if works
	private RecyclerView.LayoutManager mLayoutManager;

	private String categoryKey;
	private int categoryIndex;
	private int menuIndex;

	//If viewFlag = false -> 3D viewer (default)|| If viewFlag = true -> AR viewer
	private boolean viewFlag = false;

	private ModelFragment modelFragment;
	private ARModelFragment arModelFragment;

	private ModelRepository modelRepo;
	private ModelDownloadHandler downloadHandler;

	private RecyclerView mRecyclerView;
	private TextView foodTitle;
	private TextView foodCost;
	private TextView menuTitle;
	private Button categoryButton;

	private FragmentManager fragMgr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Retrieve important data from RestaurantActivity
		Bundle b = getIntent().getExtras();
		if (b != null) {
			this.paramAssetDir = getFilesDir().getAbsolutePath();
			this.storageFile = new File(getFilesDir() + File.separator + "model");
			this.coordinateKey = b.getString("coordinateKey");
			this.restaurantName = b.getString("restaurantName");
		}

		fragMgr = getSupportFragmentManager();

		//Initiate the RestaurantMenu class to be used that will hold all the restaurantMenu items
		restaurantMenu = new RestaurantMenu(this.coordinateKey);
		restaurantMenu.setRestName(restaurantName);

		modelRepo = new ModelRepository(coordinateKey, restaurantName, paramAssetDir, paramFilename, this, restaurantMenu, this);
		downloadHandler = new ModelDownloadHandler(this);


		setContentView(R.layout.activity_model_viewer);
		foodTitle = findViewById(R.id.title_text);
		foodCost = findViewById(R.id.item_cost);
		menuTitle = findViewById(R.id.store_name);
		categoryButton = findViewById(R.id.category_button);
		mRecyclerView = findViewById(R.id.model_recycler_view);

		//Set up the recyclerView
		mRecyclerView.setHasFixedSize(true);
		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		//For testing, I delete my folder with all models on one run, then download them with prepareMenu on other run. One always commented
		//deleteFiles();
		prepareMenu();

	}

	//Prepares The Categories, and RestaurantMenu items in that category. Fills the RestaurantMenu class with data
	private void prepareMenu() {
		modelRepo.getMenuItemModels();
	}

	@Override
	public void onFirstItemLoaded(RestaurantMenuItem menuItem, String categoryKey) {
		this.categoryKey = categoryKey;
		foodTitle.setText(menuItem.getName());
		foodCost.setText(menuItem.getCost());
		menuTitle.setText(restaurantName);
		categoryButton.setText(categoryKey);
		//Would rather start downloading/loading first model here, TESTING at the moment, may be better method
		firstAccess();
	}

	@Override
	public void onModelDownloaded() {
		beginLoadingModel();
	}

	//Used for deleting  files on non emulators, easier to use Device Monitor, just replace filename
//		File file = new File(getFilesDir().toString() + "/model/ryan.mtl");
//		File file2 = new File(getFilesDir().toString() + "/model/ryan.obj");
//		File file3 = new File(getFilesDir().toString() + "/model/ryan.jpg");
//		file.delete();
//		file2.delete();

	//very first instructions called when Activity is accessed

	private void firstAccess() {

		//reference the model currently looking at
		final RestaurantMenuItem model = restaurantMenu.allCategories
				.get(categoryKey).allItems
				.get(restaurantMenu.allCategories.get(categoryKey).keyConverter
						.get(menuIndex));

		//Fill Adapter with list of all menu items
		CircleMenuAdapter mAdapter = new CircleMenuAdapter(this.menu.allCategories.get(categoryKey).allItems,
				this.menu.allCategories.get(categoryKey).keyConverter, this, this);
		mRecyclerView.setAdapter(mAdapter);


		this.paramFilename = model.getObjPath();
		this.textureFilename = model.getJpgPath();

		//Prepare bundle to pass on to 3DViewer
		Bundle b = new Bundle();
		b.putString("assetDir", getParamAssetDir());
		b.putString("fileName", getParamFilename());

		//START DOWNLOADING/LOADING FOR FIRST MODEL
		downloadHandler.prepareDownload(model);

		//Start with 3D model Viwer upon firstAccess*******************************************

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);

		FragmentTransaction xact = fragMgr.beginTransaction();
		if (null == fragMgr.findFragmentByTag(CONTENT_VIEW_TAG))
			xact.add(R.id.modelFrame, modelFragment, CONTENT_VIEW_TAG).commit();
	}

	//Assume in 3D view: loads model selected
	void beginLoadingModel()
	{
		Bundle b= new Bundle();
		b.putString("assetDir",getParamAssetDir());
		b.putString("fileName", getParamFilename());

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);
		fragMgr.beginTransaction().replace(R.id.modelFrame, modelFragment).commit();
	}


	public String getParamAssetDir() {
		return this.paramAssetDir;
	}

	public String getParamFilename() {
		return this.paramFilename;
	}

	public String getTextureFilename() { return this.textureFilename;}

	public RestaurantMenu getMenuObject()
	{
		return this.menu;
	}

    public ArrayList<RestaurantMenuCategory> getListOfCategories() {
        return listOfCategories;
    }

    public void onCategoryClick(View v)
    {
        CategoryDialogFragment newFragment = new CategoryDialogFragment();

        newFragment.show(getFragmentManager(), "categorySelect");
    }
    //Function for callback when I make a UI to have user select the category
    public void onCategorySelect(int categoryPosition){

        categoryKey = menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).getName();
	    categoryIndex = categoryPosition;
        categoryButton.setText(categoryKey);

        //Fill Adapter with list of all menu items
        CircleMenuAdapter mAdapter = new CircleMenuAdapter(this.menu.allCategories.get(categoryPosition).allItems,
                this.menu.allCategories.get(categoryPosition).keyConverter, this,this);
        mRecyclerView.setAdapter(mAdapter);

        downloadHandler.downloadAll(categoryPosition,0,restaurantMenu,listOfCategories);
        }


	//When bubble gets hit, find its key, and model information, depending on what view we're in , pass to right Fragment
	@Override
	public void onMethodCallback(int key) {

	    //Set all our new important references to the model selected
		this.menuIndex = key;
		RestaurantMenuItem menuItem = menu.allCategories.get(categoryKey).
                allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

		this.paramFilename = menuItem.getObjPath();
		this.textureFilename = menuItem.getJpgPath();

		//Update UI
		foodCost.setText(menuItem.getCost());
		foodTitle.setText(menuItem.getName());

		downloadHandler.downloadAll(categoryIndex, menuIndex,restaurantMenu,listOfCategories);

		if(menuItem.isDownloaded())
        {
            //We are in AR, so pass data
            if (viewFlag)
                arModelFragment.passData(menuItem, this.paramFilename, this.textureFilename);
            else //Recreate 3D environment. Might want to consider just passing in new information.
                beginLoadingModel();
        }
        else
            Toast.makeText(ModelActivity.this,
                    "Download in progress..", Toast.LENGTH_LONG).show();
	}

	//For Back Button, go back to RestaurantViewActivity
	//TODO:Sometimes causes crash, probably because restaurant activity doesn't know what to do? Only on emulator so far
	public void onBackPress(View view)
	{
		finish();
	}

	//Change AR view and 3D view
	public void onLoadMode(View view) {

		//MARU - made a change here where AR now 'replaces' the 3d view frag instead of technically creating one over it.

        //Already in 3D view -> go to AR
		if (!viewFlag)
		{
			//TODO: Change how the check is done, don't even have a button there if phone does not support AR
            //This check should not involve if phone is supported,  this check should instead involve it ARCore is downloaded
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N){
				System.out.println(Build.VERSION.SDK_INT);

				viewFlag = true;

				//TODO: Remove unnecessary values to send
				Bundle bundle= new Bundle();
				bundle.putString("fileName", getParamFilename());
				bundle.putString("textureName", getTextureFilename());
				bundle.putInt("modelIndex", menuIndex);

				//**********************************************************AR
				arModelFragment = new ARModelFragment();
				arModelFragment.setArguments(bundle);

				fragMgr = getSupportFragmentManager();

				fragMgr.beginTransaction()
						.replace(R.id.modelFrame, arModelFragment, ARModelFragment.AR_MODEL_FRAGMENT_TAG)
						.commit();
				//*******************AR
				fragMgr.beginTransaction().replace(R.id.modelFrame, arModelFragment, arModelFragment.getTag()).commit();
				//***********************************************************AR

			} else
				Toast.makeText(ModelActivity.this,
						"Sorry, This Device does not support Augmented Reality", Toast.LENGTH_LONG).show();
		}
		else //We Are in AR View, go to 3DView
		{
			viewFlag = false;
			beginLoadingModel();
		}
	}

	/**
	 * Checks if the app has permission to write to device storage
	 *
	 * If the app does not has permission then the user will be prompted to grant permissions
	 *
	 * @param activity
	 */
	public static void verifyStoragePermissions(FragmentActivity activity) {
		// Check if we have write permission
		int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permission != PackageManager.PERMISSION_GRANTED) {
			// We don't have permission so prompt the user
			ActivityCompat.requestPermissions(
					activity,
					PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE
			);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					return;

				} else {

					finish();
				}
				return;
			}
		}
	}
}
