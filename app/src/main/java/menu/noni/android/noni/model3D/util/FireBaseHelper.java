package menu.noni.android.noni.model3D.util;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FireBaseHelper {

    private static FirebaseDatabase database = null;
    private static DatabaseReference myRef = null;

    //Will only create a connection, if connection does not exist
    public void createInstance(Context context)
    {
        if (database == null)
        {
            FirebaseApp.initializeApp(context);
            database = FirebaseDatabase.getInstance();
            myRef = database.getReference();
        }
    }

    public DatabaseReference getMyRef() {
        return myRef;
    }
}
