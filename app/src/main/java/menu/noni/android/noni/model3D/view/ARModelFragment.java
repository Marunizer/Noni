package menu.noni.android.noni.model3D.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.rendering.ObjectRenderer;
import menu.noni.android.noni.model3D.rendering.ObjectRendererFactory;
import menu.noni.android.noni.model3D.rendering.Scene;
import menu.noni.android.noni.model3D.rendering.XmlLayoutRenderer;
import menu.noni.android.noni.model3D.util.CameraPermissionHelper;
import menu.noni.android.noni.model3D.util.Menu;

/**
 * Created by mende on 1/24/2018.
 *
 * Purpose of this Class is to be the entry point and Manager for models to be rendered and drawn on Camera
 * while using ARCore to track its position in space, and provide 'natural' lighting
 *
 *    Concern: If it seems that an obj is not properly rendering because of it's file size,try implementing a complex object renderer
 *        - https://github.com/google-ar/arcore-android-sdk/issues/136#issuecomment-364857183
 *
 *    Current Issues/Tasks:
 *       * AR Models are not being dynamically sized.
 *        - I believe there is a dynamic sizing algorithm in the 3D model view renderer but requires quite a bit of the obj
 *          data to be kept and calculated, Probably can be found in 'model3D/model' folder
 *               - This can also be solved by having a standard on how the object files are created
 *
 *       * We need to render the models in a separate thread as to not disrupt the UI only AFTER the model is done being downloaded
 *
 *       * We need to create a separate thread that can Draw the model to screen only AFTER the model is done being rendered
 *
 *       * Low priority : Make an mtl reader and use that information in renderer instead of the hardcoded mtl information
 *                        Provide a different png to detect planes as to not use ARCores sample plane, a make it more transparent
 *
 *       *Provide first time special on screen instructions on what to do with your camera and when to click phone for new users to learn!
 *        - This should probably be implemented within ModelActivity.
 *
 *        * When you finish with this Fragment, if the references does not save, reset all render booleans to false
 */

public class ARModelFragment extends Fragment {

    private static final String TAG = ARModelFragment.class.getSimpleName();

    //Make local if possible
    //The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested = false;

    private Scene scene;
    //make local if possible
    private Config defaultConfig;
    private Session session;
    //private GestureDetector gestureDetector;
    private Snackbar loadingMessageSnackbar = null;
    private View view;

    //Used to track the models to make sure whether they are rendered or not and then to draw them
    //I don't think we actually want to use this, Maybe a synchronized list of list ???????
    List<ObjectRenderer> models =
            Collections.synchronizedList(new ArrayList<ObjectRenderer>());
    // private ArrayList<ObjectRenderer> models= new ArrayList<>();
    //method to be called is .isInitialized(), returns boolean

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> queuedTaps = new ArrayBlockingQueue<>(16);

    Menu menu;
    Menu.Categories.MenuItem model_prototype;
    private String objFile;
    private String textureFile;
    private int modelIndex;

    private final View.OnTouchListener tapListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Queue tap if there is space. Tap is lost if queue is full.
                queuedTaps.offer(event);
            }
            return true;
        }
    };

    private final Scene.DrawingCallback drawCallback = new Scene.DrawingCallback() {
        @Override
        public void onDraw(Frame frame) {
            handleTap(frame);
        }

        @Override
        public void trackingPlane() {
            hideLoadingMessage();
        }
    };

    private ObjectRendererFactory objectFactory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Bundle b = this.getArguments();

        if (b != null)
        {
            this.objFile = b.getString("fileName");
            this.textureFile = b.getString("textureName");
            this.modelIndex = b.getInt("modelIndex");
        }

        this.menu =  ((ModelActivity)getActivity()).getMenuObject();

        //Check our variables: Delete later
        System.out.println(TAG + " Object File: "  + objFile);
        System.out.println(TAG + " Texture File: " + textureFile);
        System.out.println(TAG + " Menu Index: " + modelIndex);
        System.out.println(TAG + " Menu name: " + menu.getRestName());

        model_prototype = ((ModelActivity)getActivity()).getModelItem();

        View v= inflater.inflate(R.layout.fragment_model_view_ar, container, false);

        init(v);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    private void init(View v) {

        view = v;
        //Here we set up necessary UI components

        surfaceView = v.findViewById(R.id.surfaceview);

        scene = new Scene(getContext(), surfaceView,drawCallback);// session, drawCallback);

        // Set up tap listener.
        surfaceView.setOnTouchListener(tapListener);
        copyAssetsToSdCard();

        String extPath = getContext().getExternalFilesDir(null).getAbsolutePath();
        objectFactory = new ObjectRendererFactory(getContext().getFilesDir().getAbsolutePath()+"/model/", extPath);

        // first model - assume is downloaded
        // Sets up Factory, does not render or draw
        Thread setUpFactoryModelThread = new Thread(){
            public void run(){
                ObjectRenderer object;

                object = objectFactory.create(objFile, textureFile);

                model_prototype.setModelAR(object);
                model_prototype.setFactory(true);
            }
        };

        //Run thread
        setUpFactoryModelThread.start();

        //TODO: Implement a version of this: to set up factory ready models (wont take up too much processing) then, can focus on rendering
//        Thread renderRemainingModelsThread = new Thread(){
//            public void run(){
//
//                System.out.println(TAG + " Rendering remaining models");
//
//                for (int i = 0; i < menu.allCategories.get(categoryKey).allItems.size(); i++ )
//                {
//                    Menu.Categories.MenuItem model = menu.allCategories.get(categoryKey).allItems.get(menu.allCategories.get(categoryKey).keyConverter.get(i));
//
//                    //isDownloaded should stall until it is downloaded
//                    if (model.isDownloaded())
//                    {
//                        //makes sure we do not already have a reference
//                        if (!model.isRendered())
//                        {
//                            ObjectRenderer object;
//
//                            object = objectFactory.create(model.getObjPath(), model.getJpgPath());
//
//                            model.setModelAR(object);
//
//                            // Concern: Not sure if rendering happens on separate thread, in which case,
//                            // possible that model won't be rendered by the time we reach this statement
//                            model.setRendered(true);
//                        }
//                        //else, nothing happens. Already rendered !
//                    }
//                    //else, should actually stall until it is able to return true, never return false (,:
//                    //should split that in two methods, where isDownloaded is called by the staller, and we only call staller
//                    //for now returns false since im testing and don't plan to download everything al at once until that's done being set up
//                }
//            }
//        };

      //  renderRemainingModelsThread.start();

    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if AR Core is installed
        switch (ArCoreApk.getInstance().requestInstall(getActivity(), !installRequested)) {
            case INSTALL_REQUESTED:
                installRequested = true;
                return;
            case INSTALLED:
                break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(getActivity())) {
            showLoadingMessage();
            // Note that order matters - see the note in onPause(), the reverse applies here.
            session = new Session(getContext());
            // Create default config, check is supported, create session from that config.
            defaultConfig = new Config(session);

            //I have reason to believe this will disable light estimation for the Model
            //Need to test if it removes any extra lighting, if does not, then delete
            //defaultConfig.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

            if (!session.isSupported(defaultConfig)) {
                Toast.makeText(getContext(), "This device does not support AR", Toast.LENGTH_LONG).show();
                getActivity().finish();
                return;
            }

            session.resume();
            scene.bind(session);
        } else {
            CameraPermissionHelper.requestCameraPermission(getActivity());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and create a SessionPausedException.
        scene.unbind();
        if (session != null) {
            session.pause();
        }
    }

    private void handleTap(Frame frame) {
        // Handle taps. Handling only one tap per frame, as taps are usually low frequency
        // compared to frame rate.
        MotionEvent tap = queuedTaps.poll();
        Camera camera = frame.getCamera();

        if (tap != null && tap.getAction() == MotionEvent.ACTION_UP
                && camera.getTrackingState() == TrackingState.TRACKING) {

            for (HitResult hit : frame.hitTest(tap)) {

                // Check if any plane was hit, and if it was hit inside the plane polygon.
                Trackable trackable = hit.getTrackable();

                Anchor anchor = hit.createAnchor();

                if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {

                    /*********************************************************
                     *
                     *  DRAW THE MODEL OR THE DESCRIPTION
                     *
                     *  TODO:
                     *        - Handle a case where not yet downloaded or not yet rendered, usually the solution will be to wait
                     *
                     *********************************************************/

                    System.out.println(TAG + " ATTEMPT TO DRAW OBJECT");

                    Menu.Categories.MenuItem model = model_prototype;

                    ObjectRenderer object;

                    System.out.println(TAG + " MODEL DOWNLOADED: " + model.isDownloaded());
                    System.out.println(TAG + " MODEL RENDERED: " + model.isFactory());
                    System.out.println(TAG + " MODEL DRAWN: " + !model.isDrawn());

                    if(model.isDownloaded() && model.isFactory() && !model.isDrawn())
                    {
                        object = model.getModelAR();
                        if (object != null) {

                            scene.removeModel();
                            //This is what draws a model to the screen! Allots an anchor point to the model
                            scene.addRenderer(
                                    object,
                                    trackable,
                                    anchor
                            );
                            model.setDrawn(true);
                            model.setTrackableReference(trackable);
                            model.setAnchorReference(anchor);
                        }
                    }
                    else if(model.isDownloaded() && model.isFactory() && model.isDrawn())
                    {
                        object = new XmlLayoutRenderer(getContext(), R.layout.model_ingredient_view, model_prototype.getDescription());

                        System.out.println(TAG + " Rendering XML Description");

                        //This is what draws a model to the screen! Allots an anchor point to the model
                        scene.addRenderer(
                                object,
                                model.getTrackableReference(),
                                model.getAnchorReference(),
                                true
                        );
                        model.setDrawn(false);
                    }

                    System.out.println(TAG + " DONE DRAWING");

                    // Hits are sorted by depth. Consider only closest hit on a plane.
                    break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(getActivity())) {
            Toast.makeText(getActivity(), "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(getActivity())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(getActivity());
            }
            getActivity().finish();
        }
    }


    private void showLoadingMessage() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingMessageSnackbar = Snackbar.make(
                        getActivity().findViewById(android.R.id.content),
                        "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE);
                loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
                loadingMessageSnackbar.show();
            }
        });
    }

    private void hideLoadingMessage() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadingMessageSnackbar != null) {
                    loadingMessageSnackbar.dismiss();
                    loadingMessageSnackbar = null;
                }
            }
        });
    }

    //Needed for the assets to be included such as shaders
    private void copyAssetsToSdCard() {
        final AssetManager assets = getActivity().getAssets();
        final String[] assetArray;
        try {
            assetArray = assets.list("");
        } catch (IOException e) {
            Log.e(TAG, "Could not list assets.", e);
            return;
        }

        final File outputDir = getActivity().getExternalFilesDir(null);
        if (outputDir == null) {
            Log.e(TAG, "Could not find default external directory");
            return;
        }

        for (final String file : assetArray) {
            final String localCopyName = outputDir.getAbsolutePath() + "/" + file;

            // ignore files without an extension (mostly folders)
            if (!localCopyName.contains("")) {
                continue;
            }

            OutputStream outputStream;
            try {
                outputStream = new FileOutputStream(localCopyName);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not open copy file: '" + localCopyName + "'.");
                outputStream = null;
            }

            if (outputStream != null) {
                try {
                    IOUtils.copy(assets.open(file), outputStream);
                } catch (IOException e) {
                    Log.i(TAG, "Could not open asset file: '" + file + "'.");
                }
            }
        }
    }

    //Whenever a circle is hit, assign the new intended model to be used
    //Currently just replacing variable to be rendered
    //Possible solution:
     //Make a different method that also gets called and use that to render object in a thread
    //While this ones only purpose is as a UI component to know which already rendered model to display,
    //if possible, make it replace, the current model displayed
    //will need: maybe some type of 2D structure to make sure model is already rendered,
    //a check to make sure it's downloaded, and if not, do something about it  - communicate back to start download, show animation
    public void passData(Menu.Categories.MenuItem menuItem,String obj, String texture) {

        //Lets pass on the 'key' category we are on, currently a string, possible we will need a key converter for this as well
        //This way, we know what Category we are currently looking at
        // Note: may be possible that we don't need index, but just the key

        //Pass in the index of the menu item we are looking at (to be keyConverted)
        //Let's us access the correct reference to
        // * Note: We can probably just pass in the name (key) instead of an index from ModelActivity to avoid using key-converter
        //With this information we can find the .obj and the .jpg
        this.model_prototype = menuItem;
        this.objFile = obj;
        this.textureFile = texture;

        if (!model_prototype.isFactory())
        {
            Thread setUpFactoryModelThread = new Thread(){
                public void run(){
                    ObjectRenderer object;

                    object = objectFactory.create(objFile, textureFile);

                    model_prototype.setModelAR(object);
                    model_prototype.setFactory(true);
                }
            };

            //Run thread
            setUpFactoryModelThread.start();
        }
//        this.categoryKey= b.getString("catKey");
//        this.modelKey = b.getString("modelKey");
//        this.categoryIndex = b.getInt("catIndex");
//        this.modelIndex = b.getInt("modelIndex");
      //  System.out.println("The anchor that has been hit " + anchors.get(anchors.size()-1));

    }
}
