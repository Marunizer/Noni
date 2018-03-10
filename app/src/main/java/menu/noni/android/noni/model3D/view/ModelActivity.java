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
 * 	    * 3d Model Viewer, if zooming in and out, do not allow user to rotate the screen ! (Medium Priority)
 * 	           When user has 2 fingers not zooming on the screen, allow user to move camera position if possible ( Low Priority)
 *
 * 	    * After the final 3d model production is decided, will need to change how xyz-axis are displayed so model is shown from the front.
 * 	     - UPDATE: This is HIGHLY dependant on how an .obj is created so do not do this until a standard is set
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
public class ModelActivity extends FragmentActivity implements MyCircleAdapter.AdapterCallback{

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
	private int categoryIndex = 0;
	private String coordinateKey;
	private String restaurantName;

    Menu menu;
	Menu.Categories.MenuItem modelItem;
	ArrayList<Menu.Categories> listOfCategories = new ArrayList<>();
	TextView foodTitle;
	TextView foodCost;
	TextView menuTitle;
	TextView categoryTitle;

	//Keeps track of which circle is hit, then is converted to the key of the model
	private int menuIndex = 0;

	//First time user is opening Activity
	boolean firstAccess = true;
    boolean firstDownloadAndLoad = true;

    //If first time, may need to load model right after downloading
	boolean needLoad = true;
	/**
	 * The file to load. Passed as input parameter
	 */
	private String paramFilename;

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

		if (b != null)
		{
			this.paramAssetDir = getFilesDir().getAbsolutePath();
			this.coordinateKey = b.getString("coordinateKey");
			this.restaurantName = b.getString("restaurantName");
			this.paramFilename = b.getString("uri");//the important one
		}

		//Initiate the Menu class to be used that will hold all the menu items
		menu = new Menu(this.coordinateKey);
		menu.setRestName(restaurantName);

		//Check permission and request for storage options... Might not be necessary but expected of API 23+
		//TODO: Properly ask for permission before downloading, so we atleast get firebase data, then wait
		// verifyStoragePermissions(this);

		prepareMenu();

		setContentView(R.layout.activity_model_viewer);
		foodTitle = findViewById(R.id.title_text);
		foodCost = findViewById(R.id.item_cost);
		menuTitle = findViewById(R.id.store_name);
		categoryTitle = findViewById(R.id.category_name);

	}

	//Prepares The Categories, and Menu items in that category. Fills the Menu class with data
	private void prepareMenu() {

		FirebaseDatabase database = FirebaseDatabase.getInstance();
		final DatabaseReference myRef = database.getReference();

		myRef.child("menus/" + this.coordinateKey +"/Categories").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(final DataSnapshot dataSnapshot) {

				//Will traverse every Category
				for (DataSnapshot categories: dataSnapshot.getChildren()) {

					//Create the Category, and keep reference
					Menu.Categories category = new Menu.Categories(categories.getKey());

					//Add category to our table, at the same time, we are naming the category
					menu.allCategories.put(categories.getKey(),category);

					//Add category to our list to be able to easily traverse list by index

                    listOfCategories.add(category);

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
							menuTitle.setText(restaurantName);
							categoryTitle.setText(categoryKey);
							setModelItem(menuItem);

							//Would rather start downloading/loading first model here, TESTING at the moment, may be better method
							firstAccess();
						}
					}
				}
//				//TODO: Threads seem to interfere with each other, maybe too many at a time, NO BUENO
//				//FINISHED MAKING LIST, Start downloading everything
//                Thread downloadAll = new Thread(){
//                    public void run(){
//                        downloadAll();
//                    }
//                };
//                downloadAll.start();
//                downloadAll();
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

        this.paramFilename = modelItem.getObjPath();
        this.textureFilename = modelItem.getJpgPath();

        System.out.println("obj: " + paramFilename + "  jpg: " + textureFilename);

		Bundle b= new Bundle();

		b.putString("assetDir",getParamAssetDir());
		b.putString("textureName", getTextureFilename());
		b.putString("fileName", getParamFilename());


		//Always start out with 3D-Viewer for direct access to user

		//3D model Viwer*******************************************

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);

		fragMgr = getSupportFragmentManager(); //getFragmentManager();
		FragmentTransaction xact = fragMgr.beginTransaction();//.beginTransaction();
		if (null == fragMgr.findFragmentByTag(CONTENT_VIEW_TAG)) {
			xact.add(R.id.modelFrame,  modelFragment ,CONTENT_VIEW_TAG).commit();
		}

		//3D model Viwer*******************************************


		//Sets up recycler view of circle buttons at bottom of screen
		mRecyclerView = findViewById(R.id.model_recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new MyCircleAdapter(this.menu.allCategories.get(categoryKey).allItems,
				   this.menu.allCategories.get(categoryKey).keyConverter, this);
		mRecyclerView.setAdapter(mAdapter);


		//****************************START DOWNLOADING/LOADING FOR FIRST MODEL
		//TODO:Also start whole downloading process here within a thread, we can make multiple threads, one for each model dynamically
		//Easy to do all at once, hard to manage and give priority
		//Make easy way first to atleast have a way to have downloads (:
		//Download First Model, Displays if already downloaded
		//testing number can be used to add to a thread name although better to use model name

                                               //can probably replace with modelItem
        final Menu.Categories.MenuItem model = menu.allCategories.get(categoryKey).allItems.
                get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

		Thread downloadFirstModelThread = new Thread(){
			public void run(){
				System.out.println("Download first model Thread Running");
				prepareDownload(model);
			}
		};
		downloadFirstModelThread.start();

		//****************************END DOWNLOADING/LOADING FOR FIRST MODEL
	}

	//TODO: Make a normal download system that downloads everything
	//Probably need to look at Farzas to know how he handles all events such as having priority
	//although a quick fix, would be to just have a loading animation (:
	//refer to above for quick fix on using for loop to make dynamic threads for each downlaod for simple solution
    //Right now, This is not enough, downloads interfere with each other. too much going on at once
    //Split downloads up, and wait on a few before starting more
	private void downloadAll(){


		for (int i = 0; i < listOfCategories.size(); i++)
        {
            ArrayList<String> allItemsList = menu.allCategories.get(listOfCategories.get(i).getName()).keyConverter;

            for (int j = 0; j < allItemsList.size(); j++)
            {
                //make sure we are not already dealing with firs item
                if(menu.allCategories.get(listOfCategories.get(i).getName()).keyConverter.get(j) != modelItem.getName())
                {
                    Menu.Categories.MenuItem models = menu.allCategories.get(listOfCategories.get(i).getName())
                            .allItems.get(menu.allCategories.get(listOfCategories.get(i).getName()).keyConverter.get(j));

                    System.out.println("WHAT WE DOWNLOADING?: " + models.getName() + "At Category: "  + listOfCategories.get(i).getName());
                    if(!models.isDownloaded())
                        prepareDownload(models);
                }
            }
        }
	}

	//Prepares and checks Downloads to be done
    //TODO: What we could do instead, is start three separate threads from here instead of 1 thread to download first, and one to download all
    //Problem: Maybe too many threads running at a time - low concern
    //Problem: for first Model, difficult to track when it should be downloaded, so we should not start all until we know that first is done
	private void prepareDownload(Menu.Categories.MenuItem modelToDownload){

		// TODO: Do not download obj, instead download draco, then convert to obj, Implement AFTER Draco use is finished
        // String drcPath = getFilesDir().toString() + "/model/" + modelToDownload.getDrcPath();
		String objPath = getFilesDir().toString() + "/model/" + modelToDownload.getObjPath();
		String mtlPath = getFilesDir().toString() + "/model/" + modelToDownload.getMtlPath();
		String jpgPath = getFilesDir().toString() + "/model/" + modelToDownload.getJpgPath();

		//File drcFile = new File(drcPath);
		File objFile = new File(objPath);
		File mtlFile = new File(mtlPath);
		File jpgFile = new File(jpgPath);

		if(!objFile.exists() && !mtlFile.exists() && !jpgFile.exists())
		{
		  //  dracoDownload(drcFile, modelToDownload.getDrcPath());
            System.out.println("CHECK POINT : line 383");
			downloadModel(objFile, modelToDownload.getObjPath(), modelToDownload);
			downloadModel(mtlFile, modelToDownload.getMtlPath(), modelToDownload);
			downloadModel(jpgFile, modelToDownload.getJpgPath(), modelToDownload);
		}
		else
		{
		    System.out.println("CHECK POINT : line 389");
		    //This will be redundantly set to false each download
			needLoad = false;
			modelToDownload.setDownloaded(true);
		}
	}

	//Final download stage: Downloads requested file from Firebase storage
	private void downloadModel(File files_folder, String imageKey, final  Menu.Categories.MenuItem targetModel) {

		//If file already exists, Do nothing
		if(!files_folder.exists())
		{
			String path = "Home" + File.separator + targetModel.getBucketPath() + File.separator + targetModel.getBucketPath()+"Android" + File.separator + imageKey;

			//If the path does not exist, I don't believe anything negative happens other than no download
			final StorageReference fileToDownload = fbStorageReference.child(path);

			//Make a folder  under files/model to download file into if one does not exist
			final File folder = new File(getFilesDir() + File.separator + "model");
			if (!folder.exists())
			{
				folder.mkdirs();
			}

			//File has finished downloading, atomic variable lets us keep track of downloads
			fileToDownload.getFile(files_folder).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
				@Override
				public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
					targetModel.incrementAtomicDownloadCheck();

					if(targetModel.getAtomicDownloadCheck() == 3)
					{
					    //model fully downloaded, set reference to true
						getModelItem().setDownloaded(true);

						// First model to be downloaded, load ASAP
                        if (firstDownloadAndLoad && needLoad)
                        {
                            beginLoadingModel();
                            setFirstDownloadAndLoad(false);
                            setNeedLoad(false);
                        }
					}

					System.out.println("FINISHED DOWNLOADING..." + "    downlaodCheck = " + targetModel.getAtomicDownloadCheck());

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
	private void dracoDownload(File files_folder, String imageKey) {

		//TODO: Download Draco to external folder, then decompress for obj
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
	}

	//Deletes entire folder, useful for testing
	public void deleteFiles() throws IOException {
		File file = new File(getFilesDir().toString() + "/model");
		FileUtils.deleteDirectory(file);
	}
	//Used for deleting  files on non emulators, easier to use Device Monitor, just replace filename
//		File file = new File(getFilesDir().toString() + "/model/ryan.mtl");
//		File file2 = new File(getFilesDir().toString() + "/model/ryan.obj");
//		File file3 = new File(getFilesDir().toString() + "/model/ryan.jpg");
//		file.delete();
//		file2.delete();
//		file3.delete();

	public String getParamAssetDir() {
		return this.paramAssetDir;
	}

	public String getParamFilename() {
		return this.paramFilename;
	}

	public String getTextureFilename() { return this.textureFilename;}

    public void setFirstDownloadAndLoad(boolean firstDownloadAndLoad) {
        this.firstDownloadAndLoad = firstDownloadAndLoad;
    }

    public void setNeedLoad(boolean needLoad) {
        this.needLoad = needLoad;
    }

	public Menu.Categories.MenuItem getModelItem() {
		return modelItem;
	}

	public void setModelItem(Menu.Categories.MenuItem modelItem) {
		this.modelItem = modelItem;
	}

	public Menu getMenuObject()
	{
		return this.menu;
	}

	//  _   _ ___   _____                 _
	// | | | |_ _| | ____|_   _____ _ __ | |_ ___
	// | | | || |  |  _| \ \ / / _ \ '_ \| __/ __|
	// | |_| || |  | |___ \ V /  __/ | | | |_\__ \
	//  \___/|___| |_____| \_/ \___|_| |_|\__|___/
	//

	//When bubble gets hit, find its key, and model information, depending on what view we're in , pass to right Fragment
	@Override
	public void onMethodCallback(int key) {

		this.menuIndex = key;
		this.modelItem = menu.allCategories.get(categoryKey).
                allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

		this.paramFilename = modelItem.getObjPath();
		this.textureFilename = modelItem.getJpgPath();
		foodCost.setText(modelItem.getCost());
		foodTitle.setText(modelItem.getName());

		//We are in AR, so pass data
		if (viewFlag)
		{
			arModelFragment.passData(this.modelItem, this.paramFilename, this.textureFilename);
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
				bundle.putString("coordinateKey", coordinateKey);
				bundle.putString("catKey", categoryKey);
				bundle.putString("modelKey", menu.allCategories.get(categoryKey).allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex)).getName());
				bundle.putInt("catIndex", categoryIndex);
				bundle.putInt("modelIndex", menuIndex);

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



//This adapter is for the recycler view holding all the circle buttons at bottom of screen
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
