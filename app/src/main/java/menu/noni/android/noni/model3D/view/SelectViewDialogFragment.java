package menu.noni.android.noni.model3D.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.DialogFragment;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import menu.noni.android.noni.R;

@SuppressLint("ValidFragment")
public class SelectViewDialogFragment extends DialogFragment {

    CardView select3DCardView;
    CardView selectARCardView;

    //Helps pass along data
    String key;
    String restName;

    public SelectViewDialogFragment(String key, String restName) {
        this.key = key;
        this.restName = restName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);

        final View rootView=inflater.inflate(R.layout.fragment_dialog_choose_view,container, false);

        select3DCardView = rootView.findViewById(R.id.selection_3d);
        selectARCardView = rootView.findViewById(R.id.selection_ar);

        select3DCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TextView tv = rootView.findViewById(R.id.three_d_view_text);
                ((RestaurantViewActivity)getActivity()).selectView(key,restName,tv.getText().toString());
                dismiss();
            }
        });

        selectARCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TextView tv = rootView.findViewById(R.id.ar_view_text);
                ((RestaurantViewActivity)getActivity()).selectView(key,restName,tv.getText().toString());
                dismiss();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        Window window = getDialog().getWindow();
        //TODO:Concern: This may not look well for any/all screens. Must Test !
        assert window != null;
        window.setLayout(width-150, height-630);
        window.setGravity(Gravity.CENTER);
    }
}
