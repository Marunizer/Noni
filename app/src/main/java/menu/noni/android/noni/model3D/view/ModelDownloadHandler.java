package menu.noni.android.noni.model3D.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import menu.noni.android.noni.model3D.util.RestaurantMenu;
import menu.noni.android.noni.model3D.util.RestaurantMenuCategory;
import menu.noni.android.noni.model3D.util.RestaurantMenuItem;

/**
 * Created by Wiita on 3/11/2018.
 */

public class ModelDownloadHandler {

    private Context context;
    private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();
    private boolean firstDownloadAndLoad = true;
    private ModelDownloadHandlerListener listener;

    public ModelDownloadHandler(Context contex, ModelDownloadHandlerListener listener) {
        this.context = context;
        this.listener = listener;
    }

    //TODO: If file is a draco file, download to external storage, probably use override
    //Final download stage: Downloads requested file from Firebase storage
    //Some functionality in here only works for first model
    public void downloadModelFile(File files_folder,
                                  String imageKey,
                                  final RestaurantMenuItem targetModel,
                                  File storageFile) {

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
                        targetModel.setDownloaded(true);

                        // First model to be downloaded, load ASAP
                        if (firstDownloadAndLoad)
                        {
                            listener.onModelDownloaded();
                            firstDownloadAndLoad = false;
                        }
                    }
                    Log.d("downloadModelFile"," FINISHED DOWNLOADING... " + targetModel.getName()+ "    downlaodCheck = " + targetModel.getAtomicDownloadCheck());

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
    private void dracoDownload(File files_folder, String imageKey, final int fileNumber) {

        //TODO: Download Draco to external folder, then decompress for obj
    }

    public void prepareDownload(final RestaurantMenuItem modelToDownload){

        /// TODO: Do not download obj, instead download draco, then convert to obj, Implement AFTER Draco use is finished
        // String drcPath = getFilesDir().toString() + "/model/" + modelToDownload.getDrcPath();
        String objPath = context.getFilesDir().toString() + "/model/" + modelToDownload.getObjPath();
        String mtlPath = context.getFilesDir().toString() + "/model/" + modelToDownload.getMtlPath();
        String jpgPath = context.getFilesDir().toString() + "/model/" + modelToDownload.getJpgPath();

        //File drcFile = new File(drcPath);
        final File objFile = new File(objPath);
        final File mtlFile = new File(mtlPath);
        final File jpgFile = new File(jpgPath);

        if(!objFile.exists() && !mtlFile.exists() && !jpgFile.exists())
        {
            //  dracoDownload(drcFile, modelToDownload.getDrcPath());

            Thread downloadObjThread = new Thread(){
                public void run(){
                    downloadModelFile(objFile, modelToDownload.getObjPath(), modelToDownload);
                }
            };

            Thread downloadMtlThread = new Thread(){
                public void run(){
                    downloadModelFile(mtlFile, modelToDownload.getMtlPath(), modelToDownload);
                }
            };

            Thread downloadJpgThread = new Thread(){
                public void run(){
                    downloadModelFile(jpgFile, modelToDownload.getJpgPath(), modelToDownload);
                }
            };
            downloadObjThread.start();
            downloadMtlThread.start();
            downloadJpgThread.start();
        }
        else
            modelToDownload.setDownloaded(true);
    }

    //Download System that goes through and downloads menu item on + next 3 items
    public void downloadAll(int categoryPosition, int menuPosition, RestaurantMenu restaurantMenu, ArrayList<RestaurantMenuCategory> listOfCategories){

        ArrayList<String> allItemsList = restaurantMenu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter;

        //goes through 4 times, 1 for current position, then the next 3 positions
        //j makes sure we go through 4 times, k makes sure we don't go to a null item
        for (int j = 0, k = menuPosition; j < 5 && k < allItemsList.size(); j++,k++)
        {
            //make sure we are not already dealing with firs item
            if(restaurantMenu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter.get(k) != modelItem.getName())
            {
                RestaurantMenuItem models = restaurantMenu.allCategories.get(listOfCategories.get(categoryPosition).getName())
                        .allItems.get(restaurantMenu.allCategories.get(listOfCategories.get(categoryPosition).getName()).keyConverter.get(k));

                Log.d("downloadAll", " DOWNLOADING: " + models.getName() + "At Category: " + listOfCategories.get(categoryPosition).getName());
                if(!models.isDownloaded())
                    prepareDownload(models);
            }
        }
    }

    //Deletes entire folder, useful for testing
    public void deleteFiles()  {
        File file = new File(context.getFilesDir().toString() + "/model");
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface ModelDownloadHandlerListener{
        public void onModelDownloaded();
    }
}
