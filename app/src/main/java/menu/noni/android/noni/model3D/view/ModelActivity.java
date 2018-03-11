package menu.noni.android.noni.model3D.view;

import android.Manifest;
import android.app.DialogFragment;
import android.app.Fragment;
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
import android.widget.Button;
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

    private static final String TAG = ModelActivity.class.getSimpleName();
    //eventually try to have deeper levels of control, but for now one folder for all works
    private File storageFile;

	// Storage Permissions
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

    private Menu menu;
    private Menu.Categories.MenuItem modelItem;
    private ArrayList<Menu.Categories> listOfCategories = new ArrayList<>();
    private String paramFilename;
    private String paramAssetDir;
	private String textureFilename;
	private String coordinateKey;
	private String restaurantName;
    private String categoryKey;
    private int categoryIndex;
    private int menuIndex;

	//First time user is opening Activity / first time download AND load
	private boolean firstAccess = true;
    private boolean firstDownloadAndLoad = true;
    //If viewFlag = false -> 3D viewer (default)|| If viewFlag = true -> AR viewer
    private boolean viewFlag = false;

	private FragmentManager fragMgr;
	private ModelFragment modelFragment;
	private ARModelFragment arModelFragment;
    private static final String CONTENT_VIEW_TAG = "MODEL_FRAG";

	private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();
    private RecyclerView mRecyclerView;
    private TextView foodTitle;
    private TextView foodCost;
    private TextView menuTitle;
    private Button categoryButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Retrieve important data from RestaurantActivity
		Bundle b = getIntent().getExtras();
		if (b != null)
		{
			this.paramAssetDir = getFilesDir().getAbsolutePath();
            this.storageFile = new File(getFilesDir() + File.separator + "model");
			this.coordinateKey = b.getString("coordinateKey");
			this.restaurantName = b.getString("restaurantName");
		}

		//Initiate the Menu class to be used that will hold all the menu items
		menu = new Menu(this.coordinateKey);
		menu.setRestName(restaurantName);

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

		//Check permission and request for storage options... Might not be necessary but expected of API 23+
		//TODO: Properly ask for permission before downloading, so we atleast get firebase data, then wait
        //TODO: Find out if device supports google ARCore : If not -> remove AR Button
		// verifyStoragePermissions(this);

        //For testing, I delete my folder with all models on one run, then download them with prepareMenu on other run. One always commented
        //deleteFiles();
        prepareMenu();

	}

	//Prepares The Categories, and Menu items in that category. Fills the Menu class with data
	private void prepareMenu() {

		FirebaseDatabase database = FirebaseDatabase.getInstance();
		final DatabaseReference myRef = database.getReference();
		final String databaseTitle = "menus";
		final String databaseSubTitle = "Categories";

		myRef.child(databaseTitle + File.separator + this.coordinateKey + File.separator + databaseSubTitle)
                .addValueEventListener(new ValueEventListener() {
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
							descriptionText = model.getKey();
						else
							descriptionText = model.child("description").getValue().toString();

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
						System.out.println(TAG + " Adding item : " + menuItem.getName());

						//If first item to even be looked at, Begin Activity progress
						if(firstAccess)
						{
							firstAccess = false;
							categoryKey = categories.getKey();
							categoryIndex = 0;
							foodTitle.setText(menuItem.getName());
							foodCost.setText(menuItem.getCost());
							menuTitle.setText(restaurantName);
							categoryButton.setText(categoryKey);
							setModelItem(menuItem);

							//Would rather start downloading/loading first model here, TESTING at the moment, may be better method
							firstAccess();
						}
					}
				}
				// If a thread started here would interfere with first thread
				//FINISHED MAKING LIST, Start downloading everything
                //this would not be called here, I think It would be called onMethodCallBack with position of download
                downloadAll(categoryIndex, menuIndex);
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Failed to read value
				Log.w(ContentValues.TAG, "Failed to read value.", error.toException());
			}
		});
	}

    //very first instructions called when Activity is accessed
	private void firstAccess() {

        //Fill Adapter with list of all menu items
        MyCircleAdapter mAdapter = new MyCircleAdapter(this.menu.allCategories.get(categoryKey).allItems,
                this.menu.allCategories.get(categoryKey).keyConverter, this);
        mRecyclerView.setAdapter(mAdapter);


        this.paramFilename = modelItem.getObjPath();
        this.textureFilename = modelItem.getJpgPath();

        //Prepare bundle to pass on to 3DViewer
		Bundle b= new Bundle();
		b.putString("assetDir",getParamAssetDir());
		b.putString("fileName", getParamFilename());

        //START DOWNLOADING/LOADING FOR FIRST MODEL
        prepareDownload(modelItem);

		//Start with 3D model Viwer upon firstAccess*******************************************
		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);

		fragMgr = getSupportFragmentManager();
		FragmentTransaction xact = fragMgr.beginTransaction();
		if (null == fragMgr.findFragmentByTag(CONTENT_VIEW_TAG))
			xact.add(R.id.modelFrame,  modelFragment ,CONTENT_VIEW_TAG).commit();
		//3D model Viwer***********************************************************************
	}

	//Download System that goes through and downloads menu item on + next 3 items
	private void downloadAll(int categoryPosition, int menuPosition){

        ArrayList<String> allItemsList = menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter;

        //goes through 4 times, 1 for current position, then the next 3 positions
        //j makes sure we go through 4 times, k makes sure we don't go to a null item
        for (int j = 0, k = menuPosition; j < 5 && k < allItemsList.size(); j++,k++)
        {
            //make sure we are not already dealing with firs item
            if(menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter.get(k) != modelItem.getName())
            {
                Menu.Categories.MenuItem models = menu.allCategories.get(listOfCategories.get(categoryPosition).getName())
                        .allItems.get(menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter.get(k));

                System.out.println(TAG + " DOWNLOADING: " + models.getName() + "At Category: "  + listOfCategories.get(categoryPosition).getName());
                if(!models.isDownloaded())
                    prepareDownload(models);
            }
        }
	}

	//Prepares and checks Downloads to be done, then starts eahc download in a thread if necessary
	private void prepareDownload(final Menu.Categories.MenuItem modelToDownload){

		// TODO: Do not download obj, instead download draco, then convert to obj, Implement AFTER Draco use is finished
        // String drcPath = getFilesDir().toString() + "/model/" + modelToDownload.getDrcPath();
		String objPath = getFilesDir().toString() + "/model/" + modelToDownload.getObjPath();
		String mtlPath = getFilesDir().toString() + "/model/" + modelToDownload.getMtlPath();
		String jpgPath = getFilesDir().toString() + "/model/" + modelToDownload.getJpgPath();

		//File drcFile = new File(drcPath);
		final File objFile = new File(objPath);
		final File mtlFile = new File(mtlPath);
		final File jpgFile = new File(jpgPath);

		if(!objFile.exists() && !mtlFile.exists() && !jpgFile.exists())
		{
		  //  dracoDownload(drcFile, modelToDownload.getDrcPath());

            Thread downloadObjThread = new Thread(){
                public void run(){
                    downloadModel(objFile, modelToDownload.getObjPath(), modelToDownload);
                }
            };

            Thread downloadMtlThread = new Thread(){
                public void run(){
                    downloadModel(mtlFile, modelToDownload.getMtlPath(), modelToDownload);
                }
            };

            Thread downloadJpgThread = new Thread(){
                public void run(){
                    downloadModel(jpgFile, modelToDownload.getJpgPath(), modelToDownload);
                }
            };
            downloadObjThread.start();
            downloadMtlThread.start();
            downloadJpgThread.start();
		}
		else
			modelToDownload.setDownloaded(true);
	}

	//Final download stage: Downloads requested file from Firebase storage
	private void downloadModel(File files_folder, String imageKey, final  Menu.Categories.MenuItem targetModel) {

		//If file already exists, Do nothing
		if(!files_folder.exists())
		{
			String path = "Home" + File.separator + targetModel.getBucketPath() + File.separator +
                    targetModel.getBucketPath() + "Android" + File.separator + imageKey;

			final StorageReference fileToDownload = fbStorageReference.child(path);

			if (!storageFile.exists())
				    storageFile.mkdirs();

			//File has finished downloading, atomic variable lets us keep track of the separate downloads for each model
			fileToDownload.getFile(files_folder).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
				@Override
				public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
					targetModel.incrementAtomicDownloadCheck();

					if(targetModel.getAtomicDownloadCheck() == 3)
					{
					    //model fully downloaded, set reference to true
						getModelItem().setDownloaded(true);

						// First model to be downloaded, load ASAP
                        if (firstDownloadAndLoad)
                        {
                            beginLoadingModel();
                            setFirstDownloadAndLoad(false);
                        }
					}
					System.out.println(TAG + " FINISHED DOWNLOADING... " + targetModel.getName()+ "    downlaodCheck = " + targetModel.getAtomicDownloadCheck());

				}
			}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception exception) {
					System.out.println("DOWNLOAD FAILED for item:" + targetModel.getName());
				}
			});
		}
	}

	//To be used to download Draco file
	private void dracoDownload(File files_folder, String imageKey) {

		//TODO: Download Draco to external folder, then decompress for obj
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

	//Deletes entire folder, useful for testing
	public void deleteFiles()  {
		File file = new File(getFilesDir().toString() + "/model");
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public ArrayList<Menu.Categories> getListOfCategories() {
        return listOfCategories;
    }
	//  _   _ ___   _____                 _
	// | | | |_ _| | ____|_   _____ _ __ | |_ ___
	// | | | || |  |  _| \ \ / / _ \ '_ \| __/ __|
	// | |_| || |  | |___ \ V /  __/ | | | |_\__ \
	//  \___/|___| |_____| \_/ \___|_| |_|\__|___/
	//

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
        MyCircleAdapter mAdapter = new MyCircleAdapter(this.menu.allCategories.get(categoryPosition).allItems,
                this.menu.allCategories.get(categoryPosition).keyConverter, this);
        mRecyclerView.setAdapter(mAdapter);

        downloadAll(categoryPosition,0);
        }


	//When bubble gets hit, find its key, and model information, depending on what view we're in , pass to right Fragment
	@Override
	public void onMethodCallback(int key) {

	    //Set all our new important references to the model selected
		this.menuIndex = key;
		this.modelItem = menu.allCategories.get(categoryKey).
                allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

		this.paramFilename = modelItem.getObjPath();
		this.textureFilename = modelItem.getJpgPath();

		//Update UI
		foodCost.setText(modelItem.getCost());
		foodTitle.setText(modelItem.getName());

		downloadAll(categoryIndex, menuIndex);

		if(modelItem.isDownloaded())
        {
            //We are in AR, so pass data
            if (viewFlag)
                arModelFragment.passData(this.modelItem, this.paramFilename, this.textureFilename);
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
