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
import android.widget.Toast;

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

/**
 * Created by mende on 1/24/2018.
 *
 * This fragment doesn't just act as the content of SurfaceView but has the role of a renderer
 *
 */

public class ARModelFragment extends Fragment {

    private static final String TAG = ARModelFragment.class.getSimpleName();

    //Make local if possible
    //The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested = false;

    public boolean isObjectBuiltFlag() {
        return objectBuiltFlag;
    }

    public void setObjectBuiltFlag(boolean objectBuiltFlag) {
        this.objectBuiltFlag = objectBuiltFlag;
    }

    private boolean objectBuiltFlag = false;

    private Scene scene;
    //make local if possible
    private Config defaultConfig;
    private Session session;
    //private GestureDetector gestureDetector;
    private Snackbar loadingMessageSnackbar = null;

    //Used to track the models to make sure whether they are rendered or not and then to draw them
    List<ObjectRenderer> models =
            Collections.synchronizedList(new ArrayList<ObjectRenderer>());
    // private ArrayList<ObjectRenderer> models= new ArrayList<>();
    //method to be called is .isInitialized(), returns boolean

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> queuedTaps = new ArrayBlockingQueue<>(16);
    private String nextObject = "ryan.obj";// = "TheRyanBurger.obj";//"andy.obj";//default fets changed in onCreate, can remove

    private String objFile;
    private String textureFile;


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

        if (b != null) {
            this.objFile = b.getString("fileName");
            this.textureFile = b.getString("textureName");
        }
        nextObject = objFile;
        System.out.println("1:  obj: " + objFile + "    texture: " + textureFile);

        View v= inflater.inflate(R.layout.fragment_model_view_ar, container, false);

        init(v);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    private void init(View v) {

        //Here I would set up necassary UI componenets

        surfaceView = v.findViewById(R.id.surfaceview);

        String extPath = getContext().getExternalFilesDir(null).getAbsolutePath();
        objectFactory = new ObjectRendererFactory(getContext().getFilesDir().getAbsolutePath()+"/model/", extPath);
        scene = new Scene(getContext(), surfaceView,drawCallback);// session, drawCallback);

        // Set up tap listener.
        surfaceView.setOnTouchListener(tapListener);
        copyAssetsToSdCard();
        //}
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
        if (tap != null
                && tap.getAction() == MotionEvent.ACTION_UP
                && camera.getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon.
                Trackable trackable = hit.getTrackable();
                if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {

                    /*I have not included the andhy_shadow obj or texture file in the assets
                      They can be found through ARCore sample app

                      The reason I have disables using the shadow is because it seems to keep the shadow of the object
                      previously allotted to and thus makes two separate objects on the screen after first.
                      Might be worth looking into in the future but very low priority for now
                    */
//                    final ObjectRenderer shadow = objectFactory.create("andy_shadow.obj");
//                    if (shadow != null) {
//                        shadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
//                        scene.addRenderer(
//                                shadow,
//                                trackable,
//                                hit.createAnchor()
//                        );
//                    }

                    final ObjectRenderer object;
                    if (nextObject.length() != 0) {//&& !objectBuiltFlag) {
                        object = objectFactory.create(nextObject, textureFile);
                   //     this.objectBuiltFlag = true;
                    //    scene.setScaleFactor(.03f);
                    } else {
                        object = new XmlLayoutRenderer(getContext(), R.layout.model_ingredient_view);
                        System.out.println("Do we build the layout??");
                 //       this.objectBuiltFlag = false;
                  //      scene.setScaleFactor(1.0f);
                    }

                    if (object != null) {

                        //This is what draws a model to the screen! Allots an anchor point to the model
                        scene.addRenderer(
                                object,
                                trackable,
                                hit.createAnchor()
                        );
                    }

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
    public void passData(String obj, String texture) {

        nextObject = obj;
        textureFile = texture;
      //  System.out.println("The anchor that has been hit " + anchors.get(anchors.size()-1));

        this.objectBuiltFlag = false;
        System.out.println("We hit this B   " + nextObject);
    }
}
