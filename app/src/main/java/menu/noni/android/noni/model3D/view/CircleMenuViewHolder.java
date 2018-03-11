package menu.noni.android.noni.model3D.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.storage.StorageReference;
import com.joooonho.SelectableRoundedImageView;

import java.io.File;

import menu.noni.android.noni.R;

/**
 * Created by Wiita on 3/8/2018.
 */

public class CircleMenuViewHolder extends RecyclerView.ViewHolder {

    SelectableRoundedImageView sriv;
    TextView textView;


    public CircleMenuViewHolder(View itemView) {
        super(itemView);
        sriv = itemView.findViewById(R.id.circle_image);
        textView = itemView.findViewById(R.id.circle_text);
    }
}
