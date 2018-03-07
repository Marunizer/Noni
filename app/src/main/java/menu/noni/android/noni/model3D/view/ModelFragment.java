package menu.noni.android.noni.model3D.view;


import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.services.SceneLoader;

/**
 * Created by mende on 1/24/2018.
 */

public class ModelFragment extends Fragment {

    private float[] backgroundColor = new float[]{0.2f, 0.2f, 0.2f, 1.0f};

    private ModelSurfaceView gLView;
    private SceneLoader scene;
    private String paramAssetDir;
    private String paramAssetFilename;
    private String paramFilename;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Bundle b = this.getArguments();

        if (b != null) {
            this.paramAssetDir = b.getString("assetDir");
            this.paramAssetFilename = b.getString("assetFilename");
            this.paramFilename = b.getString("fileName");
        }

        //return the inflated view
        View v= inflater.inflate(R.layout.fragment_model_view_3d, container, false);
        init(v);

        return v;
    }

    private void init(View v) {

        gLView = v.findViewById(R.id.myglsurfaceFragView);
        gLView.setModelActivity(this);
        scene= new SceneLoader(this);
        scene.init();
        scene.toggleLighting();
    }

    @Override
    public void onStart() {
        //this is where I do my stuff (:
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

    //FragmentCommunicator interface implementation
    //Keeping just in case is useful in the future
//    @Override
//    public void passDataToFragment(String someValue){
//        this.paramFilename = someValue;
//    }

}
