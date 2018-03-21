package menu.noni.android.noni.model3D.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.display.DisplayManager;
import android.support.annotation.LayoutRes;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import java.io.FileNotFoundException;

import menu.noni.android.noni.R;

import static menu.noni.android.noni.model3D.rendering.ShaderUtil.normalizeFileName;

/**
 * This class is in charge of setting up and rendering an XML Layout as an object to be projected in AR
 *
 *  Goals:
 *      * Make it look pretty (-: (To be done in the XML itself)
 *
 *      * Have the XML rotate in the direction of the user Camera. This won't necessarily be implemented here
 *       - Check : https://github.com/google-ar/arcore-android-sdk/issues/144#issuecomment-370535477
 *                 https://github.com/inio/arcore-android-sdk/blob/32e29839fb0ecf68e406c16344fc5be979c530e3/samples/hello_ar_java/app/src/main/java/org/inio/arcore/compassdemo/MathHelpers.java#L13-L35
 *                 https://github.com/google-ar/arcore-android-sdk/issues/174#issuecomment-368168531
 *
 *      * Place the XML on TOP of our food model (assume already placed)
 *      right now I'm working on this: the position can be translated in TrackableAttachment.Java
 *      I can place the description at the same exact trackable and anchor but I can't move it up
 *                https://github.com/google-ar/arcore-android-sdk/issues/110#issuecomment-366435083
 *
 */
public class XmlLayoutRenderer extends ObjectRenderer {
  private Bitmap bitmap;

  public XmlLayoutRenderer(Context context, @LayoutRes int xmlLayoutResource, String description) {
    super(
            normalizeFileName("plane.obj", basepath(context)),
            "",
            normalizeFileName(ObjectRendererFactory.DEFAULT_FRAGMENT_SHADER_FILE_NAME, basepath(context)),
            normalizeFileName(ObjectRendererFactory.DEFAULT_VERTEX_SHADER_FILE_NAME, basepath(context)));

    loadTexture(context, xmlLayoutResource, description);

    setBlendMode(BlendMode.Grid);
  }

  private static String basepath(Context context) {
    return context.getExternalFilesDir(null).getAbsolutePath();
  }

  @Override protected Bitmap readTexture() throws FileNotFoundException {
    return bitmap;
  }

  private void loadTexture(Context context, @LayoutRes int layout, String description) {
    final DisplayMetrics displayMetrics = new DisplayMetrics();
    final DisplayManager manager = context.getSystemService(DisplayManager.class);
    assert manager != null;
    manager.getDisplays()[0].getMetrics(displayMetrics);
    final int height = displayMetrics.heightPixels;
    //This is a hardcoded value 500 that just happens to work nicely here. Must test on other phones
    final int width = displayMetrics.widthPixels +500;

    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);

    int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
    int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(layout, null, false);
  //  TextView xmlText = view.findViewById(R.id.ingredient_text);
    Button xmlButton = view.findViewById(R.id.xml_button);
   // xmlText.setText(description);
    xmlButton.setText(description);

    view.measure(measuredWidth, measuredHeight);
    view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    view.draw(canvas);
  }
}
