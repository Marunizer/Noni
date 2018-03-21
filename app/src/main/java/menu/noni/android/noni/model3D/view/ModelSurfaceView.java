package menu.noni.android.noni.model3D.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import menu.noni.android.noni.model3D.controller.TouchController;

public class ModelSurfaceView extends GLSurfaceView {

	private static ModelFragment parent;
	private TouchController touchHandler;

	public ModelSurfaceView(Context context, AttributeSet attrs) {
		super(context,attrs);

		// Create an OpenGL ES 2.0 context.
		setEGLContextClientVersion(2);

		// This is the actual renderer of the 3D space
		ModelRenderer mRenderer = new ModelRenderer(this);
		setRenderer(mRenderer);

		// Render the view only when there is a change in the drawing data
		// TODO: enable this again
		 //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		touchHandler = new TouchController(this, mRenderer);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return touchHandler.onTouchEvent(event);
	}

	public ModelFragment getModelActivity() {
		return parent;
	}

	public void setModelActivity(ModelFragment model) {
		parent = model;
	}
}