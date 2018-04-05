package menu.noni.android.noni.model3D.view;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.Menu;

/**
 * Created by marunizer on 3/10/2018.
 *
 * Purpose of this class is to let user select a different Category to view from a Menu
 */

public class CategoryDialogFragment extends DialogFragment {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);

        View rootView=inflater.inflate(R.layout.fragment_dialog_category,container, false);

        ArrayList<Menu.Categories> categoryList = ((ModelActivity) getActivity()).getListOfCategories();

        RecyclerView cardRecycler = rootView.findViewById(R.id.category_recycler_view);
        cardRecycler.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        cardRecycler.setLayoutManager(mLayoutManager);
        cardRecycler.setItemAnimator(new DefaultItemAnimator());

        CategoryPickerAdapter cardAdapter = new CategoryPickerAdapter(categoryList, this.getActivity());
        cardRecycler.setAdapter(cardAdapter);

        return rootView;
    }

    public void onResume()
    {
        super.onResume();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        System.out.println("Maru testing: width: " + width + "   height: " + height + "   In pixels !");

        Window window = getDialog().getWindow();
        //TODO:Concern: This may not look well for any/all screens. Must Test !
        assert window != null;
        window.setLayout(width-150, height-300);
        window.setGravity(Gravity.CENTER);
    }
}

//Recycler view to Handle each Category
class CategoryPickerAdapter extends RecyclerView.Adapter<CategoryPickerAdapter.ViewHolder> {

    private AdapterCallbackCategory adapterCallback;
    private Context context;
    private ArrayList<Menu.Categories> mDataset;

    //used to for Glide to cache images from firebase storage
    private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView cv;
        ImageView categoryIcon;
        TextView categoryName;

        ViewHolder(View catView) {
            super(catView);
            cv = catView.findViewById(R.id.category_card);
            categoryIcon = catView.findViewById(R.id.category_image);
            categoryName = catView.findViewById(R.id.category_name);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    CategoryPickerAdapter(ArrayList<Menu.Categories> myDataset, Context context) {

        this.mDataset = myDataset;
        this.context = context;
        this.adapterCallback = ((AdapterCallbackCategory) context);
        try {
            this.adapterCallback = ((AdapterCallbackCategory) context);
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement AdapterCallback.");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_category,parent,false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        //Get the path to the file from the data set
        String path = "Home" + File.separator + "CategoryData" + File.separator +  mDataset.get(position).getCategoryIconName();
        //Create a StorageReference variable to store the path to the image
        StorageReference image = fbStorageReference.child(path);


        //Serve this path to Glide which is put into the image holder and cached for us
        //Can change withCrossFade timer to change fade in time, in milliseconds.
        GlideApp.with(context)
                .load(image)
                .transition(DrawableTransitionOptions.withCrossFade(800))
                .override(600,600)
                .into(holder.categoryIcon);

        holder.categoryName.setText(mDataset.get(position).getName());

        holder.cv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                adapterCallback.onMethodCallbackCategory(position);
            }}
        );
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface AdapterCallbackCategory {
        void onMethodCallbackCategory(int index);
    }
}
