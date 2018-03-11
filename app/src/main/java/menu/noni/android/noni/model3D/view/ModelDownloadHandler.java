package menu.noni.android.noni.model3D.view;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import menu.noni.android.noni.model3D.util.RestaurantMenuItem;

/**
 * Created by Wiita on 3/11/2018.
 */

public class ModelDownloadHandler {

    private Context context;
    private int downloadCheck = 0;
    private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();
    private int testingNumber = 0;

    public ModelDownloadHandler(Context context) {
        this.context = context;
    }

    //TODO: If file is a draco file, download to external storage, probably use override
    //Final download stage: Downloads requested file from Firebase storage
    //Some functionality in here only works for first model
    public void downloadModel(File files_folder,
                               String imageKey,
                               RestaurantMenuItem menuItemModel) {

        //If file already exists, Do nothing
        if(!files_folder.exists()) {

            //Reference to the current model, Maybe should delete, and instead have the path as a parameter
            RestaurantMenuItem model = menuItemModel;

            String path = "Home" + File.separator + model.getBucketPath() + File.separator + model.getBucketPath()+"Android" + File.separator + imageKey;

            //If the path does not exist, I don't believe anything negative happens other than no download
            final StorageReference fileToDownload = fbStorageReference.child(path);

            //Make a folder  under files/model to download file into if one does not exist
            final File folder = new File(context.getFilesDir() + File.separator + "model");
            if (!folder.exists())
            {
                folder.mkdirs();
            }

            //File has finished downloading ! If it is the 3rd file to finish, then we load model, only works for first download
            fileToDownload.getFile(files_folder).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    downloadCheck++;
                    // downloadCheck listens to make sure all three files are ready
                    //doesnt seem to work file number is never 3 because it is a final variable, maybe cause not atomic
                    //fileNumber == 3 &&
                    if(downloadCheck == 3)


                    System.out.println("FINISHED DOWNLOADING..." + "    downlaodCheck = " + downloadCheck);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    System.out.println("DOWNLOAD FAILED");
                }
            });
        }
    }

    public void startFirstModelDownloadThread(final RestaurantMenuItem model){
        //****************************START DOWNLOADING/LOADING FOR FIRST MODEL
        //TODO:Also start whole downloading process here within a thread, we can make multiple threads, one for each model dynamically
        //Easy to do all at once, hard to manage and give priority
        //Make easy way first to atleast have a way to have downloads (:
        //Download First Model, Displays if already downloaded
        //testing number can be used to add to a thread name although better to use model name
        Thread downloadFirstModelThread = new Thread(){
            public void run(){
                System.out.println("Download first model Thread Running");
                downloadOneModel(model);
                testingNumber++;
            }
        };

        //Run a download
        downloadFirstModelThread.start();
        //****************************END DOWNLOADING/LOADING FOR FIRST MODEL
    }

    //To be used to download Draco file
    private void dracoDownlaod(File files_folder, String imageKey, final int fileNumber) {

        //TODO: Download Draco to external folder, then decompress for obj
    }

    public void downloadOneModel(RestaurantMenuItem menuItem){

        RestaurantMenuItem model = menuItem;


        //TODO: Do not download obj, instead download draco, then convert to obj, Implement AFTER Draco use is finished
        String objPath = context.getFilesDir().toString() + "/model/" + model.getObjPath();
        String mtlPath = context.getFilesDir().toString() + "/model/" + model.getMtlPath();
        String jpgPath = context.getFilesDir().toString() + "/model/" + model.getJpgPath();

        File objFolder = new File(objPath);
        File mtlFolder = new File(mtlPath);
        File jpgFolder = new File(jpgPath);

        //file object does not exist, so download it ALL -> checks specific folder later!
        if(!objFolder.exists()) {
            downloadModel(objFolder, model.getObjPath(),model);
            downloadModel(mtlFolder, model.getMtlPath(),model);
            downloadModel(jpgFolder, model.getJpgPath(),model);
        }
    }

    //Deletes entire folder, useful for testing
    public void deleteFiles() throws IOException {
        File file = new File(context.getFilesDir().toString() + "/model");
        FileUtils.deleteDirectory(file);
    }

    public interface ModelDownloadHandlerListener{
        public void onModelDownloaded();
    }
}
