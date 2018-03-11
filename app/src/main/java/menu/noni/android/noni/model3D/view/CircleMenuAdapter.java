package menu.noni.android.noni.model3D.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.RestaurantMenuItem;

/**
 * Created by Wiita on 3/8/2018.
 */

public class CircleMenuAdapter extends RecyclerView.Adapter<CircleMenuViewHolder> {

    private StorageReference fbStorageReference = FirebaseStorage.getInstance().getReference();
    private AdapterCallback adapterCallback;
    private Hashtable<String, RestaurantMenuItem> modelDataSet;
    private ArrayList<String> keyConverter;
    private Context context;


    // Provide a suitable constructor (depends on the kind of dataset)
    public CircleMenuAdapter(Hashtable myDataset, ArrayList keyConverter, Context context, AdapterCallback adapterCallback) {
        this.modelDataSet = myDataset;
        this.keyConverter = keyConverter;
        this.context = context;
        this.adapterCallback = adapterCallback;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public CircleMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.circle_floating_view, parent, false);

       return new CircleMenuViewHolder(view);

    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull final CircleMenuViewHolder holder, int position) {

        //If icon exists, then make it the icon image
        String iconPath = "Home" + File.separator + modelDataSet.get(keyConverter.get(position)).getBucketPath() +
                File.separator + modelDataSet.get(keyConverter.get(position)).getBucketPath()+"Android"+
                File.separator + modelDataSet.get(keyConverter.get(position)).getIconPath();

        StorageReference image = fbStorageReference.child(iconPath);

        GlideApp.with(context).asBitmap().load(image)
                .into(holder.sriv);

        GlideApp.with(context).asBitmap().load(image)
                .into(holder.sriv);

        holder.textView.setText(modelDataSet.get(keyConverter.get(position)).getName());

        holder.sriv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                adapterCallback.onMethodCallback(holder.getAdapterPosition());
            }}
        );
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return modelDataSet.size();
    }

    public interface AdapterCallback {
        void onMethodCallback(int key);
    }
}
