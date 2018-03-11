package menu.noni.android.noni.model3D.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Created by mende on 3/10/2018.
 *
 * Purpose of this class is to let user select a different Category to view from a Menu
 */

public class CategoryDialogFragment extends DialogFragment implements CategoryPickerAdapter.AdapterCallback{

    private Context context;
    Activity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = getActivity();
        this.activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView=inflater.inflate(R.layout.fragment_dialog_category,container, false);

        ArrayList<Menu.Categories> categoryList = ((ModelActivity) getActivity()).getListOfCategories();

        RecyclerView cardRecycler = rootView.findViewById(R.id.category_recycler_view);
        cardRecycler .setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        cardRecycler.setLayoutManager(mLayoutManager);
        cardRecycler.setItemAnimator(new DefaultItemAnimator());

        CategoryPickerAdapter cardAdapter = new CategoryPickerAdapter(categoryList, this.getActivity());
        cardRecycler.setAdapter(cardAdapter);

        return rootView;
    }

//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), android.R.style.ThemeOverlay_Material_Dialog_Alert);
//
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//        View rootView = inflater.inflate(R.layout.fragment_dialog_category,null);
//
//        ArrayList<Menu.Categories> categoryList = ((ModelActivity) getActivity()).getListOfCategories();
//
//        RecyclerView cardRecycler = rootView.findViewById(R.id.category_recycler_view);
//        cardRecycler .setHasFixedSize(true);
//        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this.getActivity());
//        cardRecycler.setLayoutManager(mLayoutManager);
//        cardRecycler.setItemAnimator(new DefaultItemAnimator());
//
//        CategoryPickerAdapter cardAdapter = new CategoryPickerAdapter(categoryList, this.getActivity());
//        cardRecycler.setAdapter(cardAdapter);
//
//        builder.setView(rootView);
//
//        return builder.create();
//    }


    @Override
    public void onMethodCallback(int index) {
        ((ModelActivity) getActivity()).onCategorySelect(index);
    }
}


class ViewHolder extends RecyclerView.ViewHolder {

    ImageView categoryIcon;
    TextView categoryName;

    public ViewHolder(View catView) {
        super(catView);
        categoryIcon = catView.findViewById(R.id.category_image);
        categoryName = catView.findViewById(R.id.category_name);
    }
}




//Recycler view to Handle each Category
class CategoryPickerAdapter extends RecyclerView.Adapter<ViewHolder> {

    private AdapterCallback adapterCallback;
    private Context context;
    private ArrayList<Menu.Categories> mDataset;

    //used to for Glide to cache images from firebase storage
    private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();


//    static class ViewHolder extends RecyclerView.ViewHolder {
//
//        ImageView categoryIcon;
//        TextView categoryName;
//
//        ViewHolder(final View catView){
//            super(catView);
//            categoryIcon = catView.findViewById(R.id.category_image);
//            categoryName = catView.findViewById(R.id.category_name);
//        }
//    }

    // Provide a suitable constructor (depends on the kind of dataset)
    CategoryPickerAdapter(ArrayList<Menu.Categories> myDataset, Context context) {

        this.mDataset = myDataset;
        this.context = context;
 //       this.adapterCallback = ((AdapterCallback) context);
//        try {
//            this.adapterCallback = ((AdapterCallback) context);
//        } catch (ClassCastException e) {
//            throw new ClassCastException("Activity must implement AdapterCallback.");
//        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_category,parent,false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }


//    // Create new views (invoked by the layout manager)
//    @Override
//    public CategoryPickerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
//                                                   int viewType) {
//        // create a new view
//        View v = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.card_view_category, parent, false);
//        ViewHolder vh = new ViewHolder(v);
//        return vh;
//    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        //Get the path to the file from the data set
        String path = "Home" + File.separator + "CategoryData" + File.separator +  mDataset.get(position).getCategoryIconName();
        //Create a StorageReference variable to store the path to the image
        StorageReference image = fbStorageReference.child(path);


        //Serve this path to Glide which is put into the image holder and cached for us
        //Can change withCrossFade timer to change fade in time, in milliseconds.
        GlideApp.with(context)
                .load(image)
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .override(600,600)
                .into(holder.categoryIcon);

        holder.categoryName.setText(mDataset.get(position).getName());

        holder.categoryIcon.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                adapterCallback.onMethodCallback(position);
            }}
        );

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface AdapterCallback {
        void onMethodCallback(int index);
    }
}
