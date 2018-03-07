package menu.noni.android.noni.model3D.view;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.joooonho.SelectableRoundedImageView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.Menu;

/**
 * This activity represents the container for our 3D viewer.
 * TODO List:
 *
 * 		X* first, by using the known location key of the restaurant picked, access firebase and in order (0-1) (gross, should be changed..)
 * 	          Make an ArrayList that holds the names of the restaurant menu.
 *
 * 	    * Implement latest UI design, floating circle back button on top left, Name of item on top right with a clickable text for details for later
 *
 * 	    * 3d Model Viewer, if zooming in and out, do not allow user to rotate the screen !
 * 	                       When user has 2 fingers not zooming on the screen, allow user to move camera position? maybe not
 *
 * 	    * After the final 3d model production is decided, will need to change how xyz-axis are disaplyed so model is shown from the front.
 *
 * 	    * Make a onCreateOptionsMenu with a back button  at top left corner of screen
 * 	    * Add item name and description option and have a textView float for at least the name
 *
 * 	    *Handle Different category options, right now just assumed first category
 */
public class ModelActivity extends FragmentActivity implements MyCircleAdapter.AdapterCallback{

	// Used to load the 'native-lib' library on application startup.
	static {
		System.loadLibrary("native-lib");
	}

	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native String stringFromJNI();
	//call method as normal with stringFromJNI()

	// Storage Permissions
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};


	private String paramAssetDir;
	private String paramAssetFilename;
	private String textureFilename;
	private String categoryKey;
	private String coordinateKey;
	private String restaurantName;
	Menu menu;
	TextView foodTitle;
	TextView foodCost;

	//Keeps track of which circle is hit, then is converted to the key of the model
	private int menuIndex = 0;

	//Was used for testing, Possible that it should not be removed everywhere, no obvious purpose
	private int testingNumber = 0;

	//Purpose is to start loading model, after we have the 3 necessary files downloaded
	private int downloadCheck = 0;

	//First time user is opening Activity
	boolean firstAccess = true;
	/**
	 * The file to load. Passed as input parameter
	 */
	private String paramFilename;
	/**
	 * Background GL clear color. Default is light gray
	 */

	//convert to local if works
	private RecyclerView mRecyclerView;

	//convert to local if works
	private MyCircleAdapter mAdapter;
	//convert to local if works
	private RecyclerView.LayoutManager mLayoutManager;

	private static final String CONTENT_VIEW_TAG = "MODEL_FRAG";
	private FragmentManager fragMgr;
	private ModelFragment modelFragment;
	private ARModelFragment arModelFragment;

	//If viewFlag = false -> 3D viewer (default)|| If viewFlag = true -> AR viewer
	private boolean viewFlag = false;

	private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle b = getIntent().getExtras();

		if (b != null) {
			//TODO remove unnecassary variables
			this.paramAssetDir = b.getString("assetDir");//the directory where the files are stored perfectly
			this.coordinateKey = b.getString("coordinateKey");
			this.restaurantName = b.getString("restaurantName");
			this.paramAssetFilename = b.getString("assetFilename");//NULL should remove everywhere
			this.paramFilename = b.getString("uri");//the important one
		}

		//Initiate the Menu class to be used that will hold all the menu items
		menu = new Menu(this.coordinateKey);
		menu.setRestName(restaurantName);

		//Check permission and request for storage options... Might not be necessary but expected of API 23+
		// verifyStoragePermissions(this);

		prepareMenu();

		setContentView(R.layout.activity_model_viewer);
		foodTitle = findViewById(R.id.title_text);
		foodCost = findViewById(R.id.item_cost);
	}

	//Prepares The Categories, and Menu items in that category. Fills the Menu class with data
	private void prepareMenu() {

		FirebaseDatabase database = FirebaseDatabase.getInstance();
		final DatabaseReference myRef = database.getReference();

		myRef.child("menus/" + this.coordinateKey +"/Categories").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(final DataSnapshot dataSnapshot) {

				//Will traverse every Category
				for (DataSnapshot categories: dataSnapshot.getChildren()) {     //categories = Most Popular

					//Create the Category, and keep reference
					Menu.Categories category = new Menu.Categories(categories.getKey());

					//Add category to our table, at the same time, we are naming the category
					menu.allCategories.put(categories.getKey(),category);

					//get the icon name for the category, we go in one level deeper
					category.setCategoryIconName(categories.child("category_icon_name").getValue().toString());

                    //make reference fo the reference containing the items one level deeper
					DataSnapshot items = categories.child("items");

					//For each model in this category Add to allItems table
					for(DataSnapshot model: items.getChildren())
					{
						//keep reference for deeper level of model data for each model
						DataSnapshot data = model.child("model_data_android");

						String descriptionText;
						//check if item has a description, Database is not consistent at the moment
						if(!model.child("description").exists())
						{
							descriptionText = model.getKey();
						}
						else
						{
							descriptionText = model.child("description").getValue().toString();
						}

						//Create the menu item
						Menu.Categories.MenuItem menuItem = new Menu.Categories.MenuItem(
								model.getKey(),
								data.child("drc_name").getValue().toString(),
								data.child("mtl_name").getValue().toString(),
								data.child("texture_name").getValue().toString(),
								data.child("icon_name").getValue().toString(),
								descriptionText,
								model.child("price").getValue().toString());

						//Add the menu item to the table AND the order added to our list
						category.allItems.put(model.getKey(),menuItem);
						category.keyConverter.add(model.getKey());

						System.out.println("ADDING NEW item : " + menuItem.getName() + " to the Menu at coordinate: " + coordinateKey);

						//If first item to even be looked at, Begin Activity progress
						if(firstAccess)
						{
							firstAccess = false;
							categoryKey = categories.getKey();
							foodTitle.setText(menuItem.getName());
							foodCost.setText(menuItem.getCost());
							//Would rather access here, TESTING at the moment, so instead is called after list is complete
//							firstAccess();
						}
					}
				}
				//wait until list is done
				firstAccess();
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Failed to read value
				Log.w(ContentValues.TAG, "Failed to read value.", error.toException());
			}
		});
	}

	private void firstAccess() {
		//very first instructions called when Activity is accessed
		//first time this Activity is created, should just load the very first model
		//in the restaurant so what should be loaded here is should probably just be the very first model

		Menu.Categories.MenuItem model = menu.allCategories.get(categoryKey).allItems.
				get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

		this.paramFilename = model.getObjPath();

		Bundle b= new Bundle();

		b.putString("assetDir",getParamAssetDir());
		b.putString("assetFilename",getParamAssetFilename());
		b.putString("textureName", getTextureFilename());
		b.putString("fileName", getParamFilename());


		//Always start out with Viewer for direct access to user

		//3D model Viwer*******************************************
		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);

		fragMgr = getSupportFragmentManager(); //getFragmentManager();
		FragmentTransaction xact = fragMgr.beginTransaction();//.beginTransaction();
		if (null == fragMgr.findFragmentByTag(CONTENT_VIEW_TAG)) {
			xact.add(R.id.modelFrame,  modelFragment ,CONTENT_VIEW_TAG).commit();
		}
		//3D model Viwer*******************************************

		//Sets up Circle images at bottom of screen
		mRecyclerView = findViewById(R.id.model_recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new MyCircleAdapter(this.menu.allCategories.get(categoryKey).allItems,
				   this.menu.allCategories.get(categoryKey).keyConverter, this);
		mRecyclerView.setAdapter(mAdapter);

		//Download First Model, Displays if already downloaded
		Thread downlaodFirstModelThread = new Thread(){
			public void run(){
				System.out.println("Download first model Thread Running");
				downloadOneModel(testingNumber);

				testingNumber++;
			}
		};

		//Run a download
		downlaodFirstModelThread.start();
	}

	//TODO: Make a normal download system that downloads everything
	//Probably need to look at Farzas to know how he handles all events such as having priority
	//although a quick fix, would be to just have a loading animation (:
	private void downloadAll(){
	}

	private void downloadOneModel(final int testingNumb){

		Menu.Categories.MenuItem model = menu.allCategories.get(categoryKey).allItems.
				get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));


		//TODO: Do not download obj, instead download draco, then convert to obj, Implement AFTER Draco use is finished
		String objPath = getFilesDir().toString() + "/model/" + model.getObjPath();
		String mtlPath = getFilesDir().toString() + "/model/" + model.getMtlPath();
		String jpgPath = getFilesDir().toString() + "/model/" + model.getJpgPath();

		File objFolder = new File(objPath);
		File mtlFolder = new File(mtlPath);
		File jpgFolder = new File(jpgPath);

		//file object does not exist, so download it ALL -> checks specific folder later!
		if(!objFolder.exists()) {
			downloadModel(objFolder, model.getObjPath(), testingNumb);
			downloadModel(mtlFolder, model.getMtlPath(), testingNumb);
			downloadModel(jpgFolder, model.getJpgPath(), testingNumb);
		}
		//MAYBE REMOVE, I believe that the model auto loads anyway if the file exists, TEST
//		else
//			beginLoadingModel();
	}

	//TODO: If file is a draco file, download to external storage, probably use override
	//Final download stage: Downloads requested file from Firebase storage
	private void downloadModel(File files_folder, String imageKey, final int fileNumber) {

		//If file already exists, Do nothing
		if(!files_folder.exists()) {

			//Reference to the current model, Maybe should delete, and instead have the path as a parameter
			Menu.Categories.MenuItem model = menu.allCategories.get(categoryKey).allItems.
					get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

			String path = "Home" + File.separator + model.getBucketPath() + File.separator + model.getBucketPath()+"Android" + File.separator + imageKey;

			//If the path does not exist, I don't believe anything negative happens other than no download
			final StorageReference fileToDownload = fbStorageReference.child(path);

			//Make a folder  under files/model to download file into if one does not exist
			final File folder = new File(getFilesDir() + File.separator + "model");
			if (!folder.exists())
			{
				folder.mkdirs();
			}

			//File has finished downloading !
			fileToDownload.getFile(files_folder).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
				@Override
				public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
					downloadCheck++;
					// downloadCheck listens to make sure all three files are ready
					//doesnt seem to work file number is never 3 because it is a final variable, maybe cause not atomic
					//fileNumber == 3 &&
					if(downloadCheck == 3)
							beginLoadingModel();

					System.out.println("FINISHED DOWNLOADING...fileNumber = " + fileNumber + "    downlaodCheck = " + downloadCheck);

				}
			}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception exception) {
					System.out.println("DOWNLOAD FAILED");
				}
			});
		}
	}

	//To be used to download Draco file
	private void dracoDownlaod(File files_folder, String imageKey, final int fileNumber) {

		//TODO: Download Draco to external folder, then decompress for obj
	}

	//Currently assumes that there should be a 3D view
	//Should check what view is currently on (3D or AR) then change appropriate fragment
	//Fills our Activity frame with the appropriate model selected
	void beginLoadingModel()
	{
		Bundle b= new Bundle();
		b.putString("assetDir",getParamAssetDir());
		b.putString("assetFilename",getParamAssetFilename());
		b.putString("fileName", getParamFilename());
		b.putString("textureName", getTextureFilename());

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);
		fragMgr.beginTransaction().replace(R.id.modelFrame, modelFragment).commit();

		//TODO: Handle normal download and loading handling
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

	//Deletes entire folder, useful for testing
	public void deleteFiles() throws IOException {
		File file = new File(getFilesDir().toString() + "/model");
		FileUtils.deleteDirectory(file);
	}
	//Used for deleting  files on non emulators, easier to use Device Monitor
//		File file = new File(getFilesDir().toString() + "/model/ryan.mtl");
//		File file2 = new File(getFilesDir().toString() + "/model/ryan.obj");
//		File file3 = new File(getFilesDir().toString() + "/model/ryan.jpg");
//		file.delete();
//		file2.delete();
//		file3.delete();

	public String getParamAssetDir() {
		return this.paramAssetDir;
	}

	public String getParamAssetFilename() {
		return this.paramAssetFilename;
	}

	public String getParamFilename() {
		return this.paramFilename;
	}

	public String getTextureFilename() { return this.textureFilename;}

	//  _   _ ___   _____                 _
	// | | | |_ _| | ____|_   _____ _ __ | |_ ___
	// | | | || |  |  _| \ \ / / _ \ '_ \| __/ __|
	// | |_| || |  | |___ \ V /  __/ | | | |_\__ \
	//  \___/|___| |_____| \_/ \___|_| |_|\__|___/
	//

	//When bubble gets hit
	@Override
	public void onMethodCallback(int key) {

		this.menuIndex = key;

		this.paramFilename = menu.allCategories.get(categoryKey).
				allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex)).getObjPath();
		this.textureFilename = menu.allCategories.get(categoryKey).
				allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex)).getJpgPath();

		if (viewFlag)
		{
			arModelFragment.passData(this.paramFilename, this.textureFilename);
		}
		else
			beginLoadingModel();
//		{
//			//Threads for the purpose of running multiple (3 at a time) downloads at the same time
//			Thread thread1 = new Thread(){
//				public void run(){
//					System.out.println("Thread1 Running");
//					downloadOneModel(testingNumber);
//					testingNumber++;
//				}
//			};
//
//			thread1.start();
//		}

	}

	public void onBackPress(View view)
	{
		finish();
	}

	//AR Button on top right of screen
	//Should be made into a flag system, to go from AR to 3D view interchangeably
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
				fragMgr.beginTransaction().replace(R.id.modelFrame, arModelFragment, arModelFragment.getTag()).commit();
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




class MyCircleAdapter extends RecyclerView.Adapter<MyCircleAdapter.ViewHolder> {

	private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();
	private AdapterCallback adapterCallback;
	private Hashtable<String, Menu.Categories.MenuItem>  modelDataSet;
	private ArrayList<String> keyConverter;
	private Context context;

	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	static class ViewHolder extends RecyclerView.ViewHolder {
		SelectableRoundedImageView sriv;
		TextView textView;

		ViewHolder(final View itemView){
			super(itemView);
			sriv = itemView.findViewById(R.id.circle_image);
			textView = itemView.findViewById(R.id.circle_text);
		}
	}

	// Provide a suitable constructor (depends on the kind of dataset)
	MyCircleAdapter(Hashtable myDataset,ArrayList keyConverter, Context context) {
		this.modelDataSet = myDataset;
		this.keyConverter = keyConverter;
		this.context = context;
		try {
			this.adapterCallback = ((AdapterCallback) context);
		} catch (ClassCastException e) {
			throw new ClassCastException("Activity must implement AdapterCallback.");
		}
	}

	// Create new views (invoked by the layout manager)
	@Override
	public MyCircleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
												   int viewType) {
		// create a new view
		View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.circle_floating_view, parent, false);

		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {

		//If icon exists, then make it the icon image
		String iconPath = "Home" + File.separator + modelDataSet.get(keyConverter.get(position)).getBucketPath() +
				File.separator + modelDataSet.get(keyConverter.get(position)).getBucketPath()+"Android"+
				File.separator + modelDataSet.get(keyConverter.get(position)).getIconPath();

		StorageReference image = fbStorageReference.child(iconPath);

		GlideApp.with(context).asBitmap().load(image)
				.into(holder.sriv);

		holder.textView.setText(modelDataSet.get(keyConverter.get(position)).getName());

		holder.sriv.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view)
			{
				adapterCallback.onMethodCallback(position);
			}}
		);
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return modelDataSet.size();
	}

	public interface AdapterCallback {
		void onMethodCallback(int key);
	}
}

