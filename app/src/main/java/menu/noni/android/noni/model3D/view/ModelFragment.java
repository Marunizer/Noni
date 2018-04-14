package menu.noni.android.noni.model3D.view;

import android.content.Context;
import android.graphics.ColorSpace;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.services.SceneLoader;

/**
 * Created by marunizer on 1/24/2018.
 *
 * The purpose of this class is to be hold the scene which in turn holds the surface
 * view of the 3D modeling screen
 *
 * 	    * After the final 3d model production is decided, will need to change how xyz-axis are displayed so model is shown from the front.
 * 	     - UPDATE: This is HIGHLY dependant on how an .obj is created so do not do this until a standard is set
 *
 * 	    * 3d Model Viewer, if zooming in and out, do not allow user to rotate the screen ! (Medium Priority)
 * 	       When user has 2 fingers not zooming on the screen, allow user to move camera position if possible ( Low Priority)
 *
 *
 * Possible solutions to zooming limit
 *   https://github.com/andresoviedo/android-3D-model-viewer/issues/25#issuecomment-357078373
 */

public class ModelFragment extends Fragment {

    private float[] backgroundColor = new float[]{1.0f,1.0f,1.0f,1.0f};// new float[]{0.2f, 0.2f, 0.2f, 1.0f}; //gray
    //new float[] {0.0f, 0.0f, 0.0f, 0.0f}; //black

    private ModelSurfaceView gLView;
    private SceneLoader scene;
    private String paramAssetDir;
    private String paramFilename;

    //This is literally empty, but 3D modeler relies on this variable in many areas
    //So cannot delete unless we deal with those parameters deeper in the renderer
    private String paramAssetFilename;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Bundle b = this.getArguments();

        if (b != null) {
            this.paramAssetDir = b.getString("assetDir");
            this.paramFilename = b.getString("fileName");
        }

        //return the inflated view
        View v= inflater.inflate(R.layout.fragment_model_view_3d, container, false);
        init(v);

        return v;
    }

    private void init(View v)
    {
        gLView = v.findViewById(R.id.myglsurfaceFragView);
        gLView.setModelActivity(this);
        scene= new SceneLoader(this);
        scene.init();
        scene.toggleLighting();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //context = getActivity();
        //for interface
        //((ModelActivity)context).fragmentCommunicator = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public File getParamFile() {
        return getParamFilename() != null ? new File(getParamFilename()) : null;
    }

    public String getParamAssetDir() {
        return paramAssetDir;
    }

    public String getParamAssetFilename() {
        return paramAssetFilename;
    }

    public String getParamFilename() {
        return paramFilename;
    }

    public float[] getBackgroundColor(){
        return backgroundColor;
    }

    public SceneLoader getScene() {
        return scene;
    }

    public GLSurfaceView getgLView() {
        return gLView;
    }

    public void beginDownloadGif() {
        ((ModelActivity)getActivity()).onDownloadGifStart();
    }

    public void endDownloadGif()
    {
        ((ModelActivity)getActivity()).onDownloadGifEnd();
    }

    //FragmentCommunicator interface implementation
    //Keeping just in case is useful in the future
//    @Override
//    public void passDataToFragment(String someValue){
//        this.paramFilename = someValue;
//    }

}
