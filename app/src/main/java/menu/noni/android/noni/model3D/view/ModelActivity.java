package menu.noni.android.noni.model3D.view;

import android.Manifest;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.RestaurantMenu;
import menu.noni.android.noni.model3D.util.RestaurantMenuItem;

import static menu.noni.android.noni.model3D.view.ModelFragment.CONTENT_VIEW_TAG;

/**
 * This activity represents the container for our 3D viewer.
 * TODO List:
 *
 * 	    * 3d Model Viewer, if zooming in and out, do not allow user to rotate the screen ! (Medium Priority)
 * 	           When user has 2 fingers not zooming on the screen, allow user to move camera position if possible ( Low Priority)
 *
 * 	    * After the final 3d model production is decided, will need to change how xyz-axis are displayed so model is shown from the front.
 * 	     - UPDATE: This is HIGHLY dependant on how an .obj is created so do not do this until a standard is set
 *
 * 	    * Make a onCreateOptionsMenu with a back button  at top left corner of screen
 * 	    * Add item name and description option and have a textView float for at least the name
 * 	    - UPDATE: May not be necessary as to not take ip a bunch of room on the screen
 *
 * 	    *Handle Different category options, right now just assumed first category
 * 	    *Will Find out how we change Categories, talk to Farza on how UI should be made
 *
 * 	    *We should know by the onCreate() if the device supports AR at all, this way, we can hide the AR View option if not needed
 * 	    *If AR button is checked, we must check if user has ARCore installed on their phone, if not, lead them to the playstore
 * 	    - https://github.com/google-ar/arcore-android-sdk/issues/162#event-1489578234
 *
 * 	    * Set up a thread based downloading Manager to download models, and have priority to download
 * 	      models User has clicked but we have not gotten to yet
 */
public class ModelActivity extends FragmentActivity implements CircleMenuAdapter.AdapterCallback,
		ModelRepository.ModelRepositoryListener,
		ModelDownloadHandler.ModelDownloadHandlerListener{

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

	// Storage Permissions
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	private String paramAssetDir;
	private String textureFilename;
	private String categoryKey = "Most Popular";
	private String coordinateKey;
	private String restaurantName;
	RestaurantMenu restaurantMenu;
	TextView foodTitle;
	TextView foodCost;

	//Keeps track of which circle is hit, then is converted to the key of the model
	private int menuIndex = 0;

	/**
	 * The file to load. Passed as input parameter
	 */
	private String paramFilename;

	//convert to local if works
	private RecyclerView mRecyclerView;

	//convert to local if works
	private CircleMenuAdapter mAdapter;
	//convert to local if works
	private RecyclerView.LayoutManager mLayoutManager;
	;
	private FragmentManager fragMgr;
	private ModelFragment modelFragment;
	private ARModelFragment arModelFragment;

	//If viewFlag = false -> 3D viewer (default)|| If viewFlag = true -> AR viewer
	private boolean viewFlag = false;



	private ModelRepository modelRepo;
	private ModelDownloadHandler downloadHandler;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle b = getIntent().getExtras();

		if (b != null) {
			this.paramAssetDir = getFilesDir().getAbsolutePath();
			this.coordinateKey = b.getString("coordinateKey");
			this.restaurantName = b.getString("restaurantName");
			this.paramFilename = b.getString("uri");//the important one
		}

		//Initiate the RestaurantMenu class to be used that will hold all the restaurantMenu items
		restaurantMenu = new RestaurantMenu(this.coordinateKey);
		restaurantMenu.setRestName(restaurantName);

		modelRepo = new ModelRepository(coordinateKey,restaurantName,paramAssetDir,paramFilename,this,restaurantMenu);
		downloadHandler = new ModelDownloadHandler(this);


		//Check permission and request for storage options... Might not be necessary but expected of API 23+
		//TODO: Properly ask for permission before downloading, so we atleast get firebase data, then wait
		// verifyStoragePermissions(this);

		prepareMenu();

		setContentView(R.layout.activity_model_viewer);
		foodTitle = findViewById(R.id.title_text);
		foodCost = findViewById(R.id.item_cost);
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
		//Would rather start downloading/loading first model here, TESTING at the moment, may be better method
		firstAccess();
	}

	//Used for deleting  files on non emulators, easier to use Device Monitor, just replace filename
//		File file = new File(getFilesDir().toString() + "/model/ryan.mtl");
//		File file2 = new File(getFilesDir().toString() + "/model/ryan.obj");
//		File file3 = new File(getFilesDir().toString() + "/model/ryan.jpg");
//		file.delete();
//		file2.delete();
	//  _   _ ___   _____                 _
	// | | | |_ _| | ____|_   _____ _ __ | |_ ___
	// | | | || |  |  _| \ \ / / _ \ '_ \| __/ __|
	// | |_| || |  | |___ \ V /  __/ | | | |_\__ \
	//  \___/|___| |_____| \_/ \___|_| |_|\__|___/

	private void firstAccess() {
		//very first instructions called when Activity is accessed
		//first time this Activity is created, should just load the very first model
		//in the restaurant so what should be loaded here is should probably just be the very first model

		//reference the model currently looking at
		final RestaurantMenuItem model = restaurantMenu.allCategories
				.get(categoryKey).allItems
				.get(restaurantMenu.allCategories.get(categoryKey).keyConverter
				.get(menuIndex));

		this.paramFilename = model.getObjPath();
		this.textureFilename = model.getJpgPath();

		Bundle b= new Bundle();

		b.putString("assetDir",getParamAssetDir());
		b.putString("textureName", getTextureFilename());
		b.putString("fileName", getParamFilename());


		//Always start out with 3D-Viewer for direct access to user

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);

		fragMgr = getSupportFragmentManager(); //getFragmentManager();
		FragmentTransaction xact = fragMgr.beginTransaction();//.beginTransaction();
		if (null == fragMgr.findFragmentByTag(CONTENT_VIEW_TAG)) {
			xact.add(R.id.modelFrame,  modelFragment ,CONTENT_VIEW_TAG).commit();
		}

		setUpRecyclerView();

		downloadHandler.startFirstModelDownloadThread(model);
	}

	private void setUpRecyclerView(){
		//Sets up recycler view of circle buttons at bottom of screen
		mRecyclerView = findViewById(R.id.model_recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new CircleMenuAdapter(this.restaurantMenu.allCategories.get(categoryKey).allItems,
				this.restaurantMenu.allCategories.get(categoryKey).keyConverter, this,this);
		mRecyclerView.setAdapter(mAdapter);
	}

	@Override
	public void onModelDownloaded() {
		beginLoadingModel();
	}

	//TODO: Make a normal download system that downloads everything
	//Probably need to look at Farzas to know how he handles all events such as having priority
	//although a quick fix, would be to just have a loading animation (:
	//refer to above for quick fix on using for loop to make dynamic threads for each downlaod for simple solutopn
	private void downloadAll(){
	}


	//Currently assumes that there should be a 3D view
	//Fills our Activity frame with the appropriate model selected
	void beginLoadingModel()
	{
		Bundle b= new Bundle();
		b.putString("assetDir",getParamAssetDir());
		b.putString("fileName", getParamFilename());
		b.putString("textureName", getTextureFilename());

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);
		fragMgr.beginTransaction().replace(R.id.modelFrame, modelFragment).commit();

		//Now that the first model has been downloaded and loaded, call DownloadALL(); or
		//either implement donwloads here, OR implement in the firstaccess() method
		//Threads for the purpose of running multiple (3 at a time) downloads at the same time
//			Thread thread1 = new Thread(){
//				public void run(){
//					System.out.println("Thread1 Running");
//					downloadOneModel(testingNumber);
//					testingNumber++;
//				}
//			};
//
//			thread1.start();

	}


//		file3.delete();

	public String getParamAssetDir() {
		return this.paramAssetDir;
	}

	public String getParamFilename() {
		return this.paramFilename;
	}

	public String getTextureFilename() { return this.textureFilename;}

	//

	//When bubble gets hit, find its key, and model information, depending on what view we're in , pass to right Fragment
	@Override
	public void onMethodCallback(int key) {

		this.menuIndex = key;

		this.paramFilename = restaurantMenu.allCategories.get(categoryKey).
				allItems.get(restaurantMenu.allCategories.get(categoryKey).keyConverter.get(menuIndex)).getObjPath();
		this.textureFilename = restaurantMenu.allCategories.get(categoryKey).
				allItems.get(restaurantMenu.allCategories.get(categoryKey).keyConverter.get(menuIndex)).getJpgPath();

		//We are in AR, so pass data
		if (viewFlag)
		{
			arModelFragment.passData(this.paramFilename, this.textureFilename);
		}
		//Recreate 3D environment. Might want to consider just passing in new information,
		//I think i may have tested before though and didnt work
		else
			beginLoadingModel();
	}

	//for back button on top left of screen, just close activity to go back to RestaurantViewActivity
	//TODO:Sometimes causes crash, probably because restaurant activity doesn't know what to do
	public void onBackPress(View view)
	{
		finish();
	}

	//AR Button on top right of screen
	//Should be made into a flag system, to go from AR to 3D view interchangeably
	//Should deal with permissions appropriately
	public void loadMode(View view) {

		//MARU - made a change here where AR now 'replaces' the 3d view frag instead of technically creating one over it.
		//Also made it so it goes back to the 3D model view if it's already in AR view

		if (!viewFlag)
		{
			//TODO: Change how the check is done, don't even have a button there if phone does not support AR
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N){
				System.out.println(Build.VERSION.SDK_INT);

				viewFlag = true;

				Bundle bundle= new Bundle();

				bundle.putString("fileName", getParamFilename());
				bundle.putString("textureName", getTextureFilename());

				//*******************AR
				arModelFragment = new ARModelFragment();
				arModelFragment.setArguments(bundle);

				fragMgr = getSupportFragmentManager();
				fragMgr.beginTransaction()
						.replace(R.id.modelFrame, arModelFragment, ARModelFragment.AR_MODEL_FRAGMENT_TAG)
						.commit();
				//*******************AR

			} else{
				Toast.makeText(ModelActivity.this,
						"Sorry, This Device does not support Augmented Reality", Toast.LENGTH_LONG).show();
			}
		}
		else
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
