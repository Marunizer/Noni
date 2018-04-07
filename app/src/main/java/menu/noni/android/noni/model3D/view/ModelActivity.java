package menu.noni.android.noni.model3D.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.ArCoreApk;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
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
import menu.noni.android.noni.model3D.util.FireBaseHelper;
import menu.noni.android.noni.model3D.util.Menu;

/**
 *
 *  * Created by marunizer on 2017 - 2018.
 *
 * This activity represents the container for our 3D and AR viewer fragments.
 * TODO List:
 *
 * 	    * Firebase gets accessed before UI is set up: result : ugly transition into activity, maybe re-arranging
 * 	        or something else can lead to a nicer transition to new screen
 *
 * 	    *If AR button is checked, we must check if user has ARCore installed on their phone, if not, lead them to the playstore
 * 	    - https://github.com/google-ar/arcore-android-sdk/issues/162#event-1489578234
 *
 */
public class ModelActivity extends FragmentActivity implements MyCircleAdapter.AdapterCallback, CategoryPickerAdapter.AdapterCallbackCategory{

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

    private Menu menu;
    private Menu.Categories.MenuItem modelItem;
    private ArrayList<Menu.Categories> listOfCategories = new ArrayList<>();
    private MyCircleAdapter mAdapter;
    private String paramFilename;
    private String paramAssetDir;
	private String textureFilename;
	private String coordinateKey;
	private String restaurantName;
    private String categoryKey;
    private int categoryIndex;
    private int menuIndex;
    private int category_MostPopular;
	int check = 0;

	private boolean firstAccess = true; // first time opening Activity
    private boolean firstDownloadAndLoad = true; //first time download AND load
    private boolean viewFlag = false;//false -> 3D viewer (default)|| true -> AR viewer
    private boolean snackBarChange = false; // Have we change AR views snack bar only for when we have a button allwing a change between views
	private boolean categoryChange = false; // Keeps track if we have changed Category when changing items and views
	private boolean categoryOrganized = false; //lets us know if we have started organizing items by categories to not repeat

	private FragmentManager fragMgr;
	private ModelFragment modelFragment;
	private static ARModelFragment arModelFragment;
	private CategoryDialogFragment categoryFragment;
    private static final String CONTENT_VIEW_TAG = "MODEL_FRAG";
    private static final String CATEGORY_VIEW_TAG = "CATEGORY_FRAG";

	private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();
	private FrameLayout gradientFrameTop;
	LinearLayout recyclerLayout;
    private RecyclerView mRecyclerView;
	private ImageView gifView;
	private TextView downloadText;
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

			//Go to AR Mode instantly if AR is requested
			String mode = b.getString("mode");
			if(mode.equals("AR MENU"))
				this.viewFlag = true;
		}

		//Initiate the Menu class to be used that will hold all the menu items
		menu = new Menu(this.coordinateKey);
		menu.setRestName(restaurantName);

        setContentView(R.layout.activity_model_viewer);
        foodTitle = findViewById(R.id.title_text);
        foodCost = findViewById(R.id.item_cost);
        menuTitle = findViewById(R.id.store_name);
        categoryButton = findViewById(R.id.category_button);
        downloadText = findViewById(R.id.loading_text);
		gifView = findViewById(R.id.download_gif);
		GlideApp.with(this)
				.load(R.drawable.watermelon_bites)
				.override(600,600)
				.into(gifView);
		onDownloadGifStart();

		recyclerLayout = findViewById(R.id.recycler_layout);
		recyclerLayout.setVisibility(View.GONE);
		gradientFrameTop = findViewById(R.id.gradient_frame_top);
		gradientFrameTop.getBackground().setAlpha(20);
		categoryButton.setVisibility(View.GONE);
		recyclerLayout.getBackground().setAlpha(100);//50% at 128, transparent 0% -> 255

        //Set up the recyclerView
		mRecyclerView = findViewById(R.id.model_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		if (!storageFile.exists())
			storageFile.mkdirs();

		//Check permission and request for storage options... Then proceed with application
		if(verifyStoragePermissions())
			prepareMenu();
	}

	public boolean verifyStoragePermissions(){

		//If Permissions are not granted, ask for permission
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(ModelActivity.this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
					REQUEST_EXTERNAL_STORAGE);
			return false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		switch (requestCode) {
			case REQUEST_EXTERNAL_STORAGE:
			{
				if (grantResults.length > 0)
				{
					boolean storagePermisssion = grantResults[0] == PackageManager.PERMISSION_GRANTED;

					//Permission is granted, move on
					if (storagePermisssion)
						prepareMenu();
					else //Permission not granted, Can't do anything
					{
						Toast.makeText(ModelActivity.this,
								"Storage Permissions required to continue\nTurn on by going to your phones Settings->Apps->Noni->Permissions", Toast.LENGTH_LONG).show();
						ModelActivity.this.finish();
					}
				}
			}
		}
	}

	//Prepares The Categories, and Menu items in that category. Fills the Menu class with data
	private void prepareMenu()
	{
		FireBaseHelper fireBaseHelper = new FireBaseHelper();
		fireBaseHelper.createInstance(getApplicationContext());

		final DatabaseReference myRef = fireBaseHelper.getMyRef();
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

                    //If we have not yet organized category and taken a look at Most popular, do so once
					if(categories.getKey().equals("Most Popular") && !categoryOrganized && firstAccess)
					{
						categoryOrganized = true;
						category_MostPopular = listOfCategories.indexOf(category);
					}

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
								model.child("icon_name").getValue().toString(),
								descriptionText,
								model.child("price").getValue().toString());

						//Add the menu item to the table AND the order added to our list
						category.allItems.put(model.getKey(),menuItem);
						category.keyConverter.add(model.getKey());
						System.out.println(TAG + " Adding item : " + menuItem.getName());

						//If first item to even be looked at, Begin Activity progress
						if(firstAccess && categoryOrganized)
						{
							organizeCategories();
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

				//TODO: Check if this interferes with download reference
				// Disconnect from firebase so sudden changes won't effect app
				myRef.onDisconnect();

				//FINISHED MAKING LIST, Start downloading everything
                //this would not be called here, I think It would be called onMethodCallBack with position of download
				beginBackroundDownload();
               // downloadAll(categoryIndex, menuIndex+1);
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Failed to read value
				Log.w(ContentValues.TAG, "Failed to read value.", error.toException());
			}
		});
	}

	//When called twice, we are ready to download the rest of the models the first time
	public void beginBackroundDownload()
	{
		check++;
		Log.i(TAG,"Should we begin??? " + check );
		if (check == 2)
		{
			downloadAll(categoryIndex, menuIndex+1);
		}
	}

	//For now the only purpose of this function is to make sure Most Popular Category pops up before anything else
	//Might want to make this a linked list so the operation isn't so heavy, but there won't ever be
	//too many categories so it doesn't matter that much lol
	private void organizeCategories()
	{
		Menu.Categories cat = listOfCategories.get(category_MostPopular);
		listOfCategories.remove(category_MostPopular);
		listOfCategories.add(0, cat);
	}

    //very first instructions called when Activity is accessed
	private void firstAccess() {

        //Fill Adapter with list of all menu items
        mAdapter = new MyCircleAdapter(this.menu.allCategories.get(categoryKey).allItems,
                this.menu.allCategories.get(categoryKey).keyConverter, this);
        mRecyclerView.setAdapter(mAdapter);

        this.paramFilename = modelItem.getObjPath();
        this.textureFilename = modelItem.getJpgPath();

        if(!viewFlag)//We are in 3D view
		{
			//Prepare bundle to pass on to 3DViewer
			Bundle b= new Bundle();
			b.putString("assetDir",getParamAssetDir());
			b.putString("fileName", getParamFilename());

			//Start with 3D model Viewer upon firstAccess*******************************************
			modelFragment = new ModelFragment();
			modelFragment.setArguments(b);

			fragMgr = getSupportFragmentManager();
			FragmentTransaction xact = fragMgr.beginTransaction();
			if (null == fragMgr.findFragmentByTag(CONTENT_VIEW_TAG)) {
				xact.add(R.id.modelFrame, modelFragment, CONTENT_VIEW_TAG).commit();
			}
			//3D model Viwer************************************************************************
		}
		else //We are in AR view
		{
			Bundle bundle = new Bundle();
			bundle.putString("fileName", getParamFilename());
			bundle.putString("textureName", getTextureFilename());

			foodTitle.setTextColor(Color.WHITE);
			foodCost.setTextColor(Color.WHITE);
			menuTitle.setTextColor(Color.WHITE);
			gradientFrameTop.setVisibility(View.VISIBLE);

			arModelFragment = new ARModelFragment();
			arModelFragment.setArguments(bundle);
			fragMgr = getSupportFragmentManager();
			if (null == fragMgr.findFragmentByTag(arModelFragment.getTag()))
				fragMgr.beginTransaction().setCustomAnimations(android.R.animator.fade_in,
						android.R.animator.fade_out).add(R.id.modelFrame, arModelFragment, arModelFragment.getTag()).commit();
		}
        //START DOWNLOADING/LOADING FOR FIRST MODEL
        prepareDownload(modelItem);
	}

	//Download System that goes through and downloads menu item on + next 2 items
	private void downloadAll(int categoryPosition, int menuPosition){

        ArrayList<String> allItemsList = menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter;

        //goes through 3 times, 1 for current position, then the next 2 positions
        //j makes sure we go through 3 times, k makes sure we don't go to a null item
        for (int j = 0, k = menuPosition; j < 4 && k < allItemsList.size(); j++,k++)
        {
            Menu.Categories.MenuItem models = menu.allCategories.get(listOfCategories.get(categoryPosition).getName())
                    .allItems.get(menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter.get(k));

            System.out.println(TAG + " DOWNLOADING: " + models.getName() + "At Category: "  + listOfCategories.get(categoryPosition).getName());
            if(!models.isDownloaded())
                prepareDownload(models);
        }
	}

	//Prepares and checks Downloads to be done, then starts eaCH download in a thread if necessary//basically a factory
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

		if(!objFile.exists() || !mtlFile.exists() || !jpgFile.exists())
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
        {
            modelToDownload.setDownloaded();
			onDownloadGifEnd();
			recyclerLayout.setVisibility(View.VISIBLE);
			categoryButton.setVisibility(View.VISIBLE);
        }
	}

	//Final download stage: Downloads requested file from Firebase storage
	private void downloadModel(final File files_folder, final String imageKey, final  Menu.Categories.MenuItem targetModel) {

		//If file already exists, Do nothing
		if(!files_folder.exists())
		{
			String path = "Home" + File.separator + targetModel.getBucketPath() + File.separator +
                    targetModel.getBucketPath() + "Android" + File.separator + imageKey;

			final StorageReference fileToDownload = fbStorageReference.child(path);

			//File has finished downloading, atomic variable lets us keep track of the separate downloads for each model
			fileToDownload.getFile(files_folder).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
				@Override
				public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
					targetModel.incrementAtomicDownloadCheck();

					if(targetModel.getAtomicDownloadCheck()%3==0)
					{
					    //model fully downloaded, set reference to true
						targetModel.setDownloaded();

						beginBackroundDownload();
						// First model to be downloaded, load ASAP
                        if (firstDownloadAndLoad)
                        {
                        	if (!viewFlag)//3D view
                            	beginLoadingModel();
                        	else
								arModelFragment.passData(modelItem, paramFilename, textureFilename, true);
                            setFirstDownloadAndLoad();
                            onDownloadGifEnd();
							recyclerLayout.setVisibility(View.VISIBLE);
							categoryButton.setVisibility(View.VISIBLE);
                        }
					}
					System.out.println(TAG + " FINISHED DOWNLOADING... " + targetModel.getName() + imageKey  + "    downloadCheck = " + targetModel.getAtomicDownloadCheck());
				}
			}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception exception) {
					System.out.println("DOWNLOAD FAILED for item:" + targetModel.getName() + imageKey);
					if(files_folder.exists()){
						try {
							FileUtils.deleteDirectory(files_folder);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}
			});
		}
		else //KINDA MADE THIS OUT OFFER, RE- GO THROUGH THIS TO SEE TO SEE IF NECESSARY, IN CASE FINAL FILE IS FOUND WITHOUT NEEDING DOWNLOAD
		{
			targetModel.incrementAtomicDownloadCheck();
			//Even if does not exist, should still increment if other files rely on it
			if(targetModel.getAtomicDownloadCheck()%3==0)
			{
				//model fully downloaded, set reference to true
				targetModel.setDownloaded();
				//getModelItem().setDownloaded();
				beginBackroundDownload();
				// First model to be downloaded, load ASAP
				if (firstDownloadAndLoad)
				{
					if (!viewFlag)//3D view
						beginLoadingModel();
					else
						arModelFragment.passData(modelItem, paramFilename, textureFilename, true);
					setFirstDownloadAndLoad();
					onDownloadGifEnd();
					recyclerLayout.setVisibility(View.VISIBLE);
					categoryButton.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	//To be used to download Draco file
	private void dracoDownload(File files_folder, String imageKey) {

		//TODO: Download Draco to external folder, then decompress for obj
	}

	//Assume in 3D view: loads model selected
	void beginLoadingModel()
	{
		ModelFragment temp = modelFragment;

		Bundle b= new Bundle();
		b.putString("assetDir",getParamAssetDir());
		b.putString("fileName", getParamFilename());

		modelFragment = new ModelFragment();
		modelFragment.setArguments(b);
		fragMgr.beginTransaction().add(R.id.modelFrame, modelFragment).commitNow();
		fragMgr.beginTransaction().remove(temp).commit();
	}

	public String getParamAssetDir() {
		return this.paramAssetDir;
	}

	public String getParamFilename() {
		return this.paramFilename;
	}

	public String getTextureFilename() { return this.textureFilename;}

    public void setFirstDownloadAndLoad() {
        this.firstDownloadAndLoad = false;
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

	public void onDownloadGifStart()
	{
		gifView.setVisibility(View.VISIBLE);
		downloadText.setVisibility(View.VISIBLE);
	}

	public void onDownloadGifEnd()
	{
		gifView.setVisibility(View.GONE);
		downloadText.setVisibility(View.GONE);
		//Apperantly this get's called a million times???? so not a good idea tO do this VVV
		//beginLoadingModel();
	}

    //Category Button : Shows DialogFragment
    public void onCategoryClick(View v)
    {
        categoryFragment = new CategoryDialogFragment();
        categoryFragment.show(getFragmentManager(), CATEGORY_VIEW_TAG);
    }

    //New Category selected, change all necessary variables
    public void onCategorySelect(int categoryPosition){

	    categoryFragment.dismiss();

	    //if no change in category, do nothing
        if (categoryIndex == categoryPosition)
            return;

        categoryChange = true;
        categoryKey = menu.allCategories.get(listOfCategories.get(categoryPosition).getName()).getName();
	    categoryIndex = categoryPosition;
        categoryButton.setText(categoryKey);
        modelItem = menu.allCategories.get(categoryKey).
                allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(0));//index is 0 to start from beginning
        paramFilename = modelItem.getObjPath();
        textureFilename = modelItem.getJpgPath();
        setModelItem(modelItem);
        mAdapter = new MyCircleAdapter(this.menu.allCategories.get(categoryKey).allItems,
                this.menu.allCategories.get(categoryKey).keyConverter, this);
        mRecyclerView.setAdapter(mAdapter);

        downloadAll(categoryPosition,0);
        }


	//When bubble gets hit, find its key, and model information, depending on what view we're in , pass to right Fragment
	@Override
	public void onMethodCallback(int key) {

		//User hit same item, don't do anything  | We compare keys because index may be the same
		if (!categoryChange && menuIndex == key)
			return;

		this.categoryChange = false;
		//Set all our new important references to the model selected
		this.menuIndex = key;
		this.modelItem = menu.allCategories.get(categoryKey).
				allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(menuIndex));

		this.paramFilename = modelItem.getObjPath();
		this.textureFilename = modelItem.getJpgPath();

		//Update UI
		this.foodCost.setText(modelItem.getCost());
		this.foodTitle.setText(modelItem.getName());

		downloadAll(categoryIndex, menuIndex);

		if(modelItem.isDownloaded())
		{
			//We are in AR, so pass data
			if (viewFlag)
				arModelFragment.passData(this.modelItem, this.paramFilename, this.textureFilename, false);
			else //Recreate 3D environment. Might want to consider just passing in new information.
				beginLoadingModel();
		}
		else
			Toast.makeText(ModelActivity.this,
					"Download in progress.. please wait and try again", Toast.LENGTH_LONG).show();
	}

	//For Back Button, go back to RestaurantViewActivity
	//TODO:Sometimes causes crash,, I think on emulator only though, if error reproduced, note here:
	public void onBackPress(View view)
	{
		this.finish();
	}

//	//Change between AR view and 3D view
//	public void onViewChange(View view) {
//
//        //Already in 3D view -> go to AR
//		if (!viewFlag) {
//			viewFlag = true;
//			gradientFrameTop.setVisibility(View.VISIBLE);
//
//			//This is the very first and only instance of the ARFragment we will have (:
//			if (!firstAR) {
//				Bundle bundle = new Bundle();
//				bundle.putString("fileName", getParamFilename());
//				bundle.putString("textureName", getTextureFilename());
//
//				firstAR = true;
//				arModelFragment = new ARModelFragment();
//				arModelFragment.setArguments(bundle);
//				fragMgr = getSupportFragmentManager();
//				fragMgr.beginTransaction().setCustomAnimations(android.R.animator.fade_in,
//						android.R.animator.fade_out).replace(R.id.modelFrame, arModelFragment, arModelFragment.getTag()).commit();
//			} else //AR Fragment has already been made ! So lets just keep it ;)
//			{
//				if (snackBarChange)//we messed with this
//				{
//					snackBarChange = false;
//					arModelFragment.showLoadingMessage();
//				}
//				FragmentTransaction ft = fragMgr.beginTransaction();
//				ft.remove(modelFragment);
//				ft.show(arModelFragment);
//				arModelFragment.passData(this.modelItem, this.paramFilename, this.textureFilename);
//				ft.commit();
//			}
//		}
//		else //We Are in AR View, go to 3DView
//		{
//			fragMgr.beginTransaction().hide(arModelFragment).commit();
//			if(arModelFragment.loadingMessageSnackbar!= null)
//			{
//				snackBarChange = true;
//				arModelFragment.hideLoadingMessage();
//			}
//			viewFlag = false;
//			gradientFrameTop.setVisibility(View.INVISIBLE);
//			beginLoadingModel();
//		}
//	}

    @Override
    public void onMethodCallbackCategory(int index) {
        onCategorySelect(index);
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

	MyCircleAdapter(Hashtable<String, Menu.Categories.MenuItem> myDataset,ArrayList<String> keyConverter, Context context) {
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
	@NonNull
    @Override
	public MyCircleAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
		// create a new view
		View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.circle_floating_view, parent, false);

        return new ViewHolder(v);
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, @SuppressLint("RecyclerView") final int position) {

		//If icon exists, then make it the icon image
		String iconPath = "Home" + File.separator + modelDataSet.get(keyConverter.get(position)).getBucketPath() +
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
