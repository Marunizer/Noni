package menu.noni.android.noni.model3D.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.LocationHelper;

/**
 * Created by mauricio mendez on 10/20/2017.
 */

public class LocationActivity  extends Activity{

    TextView queryText;
    EditText zipcodeText;
    Button enterQuery;
    String zip;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_query);
        queryText = findViewById(R.id.zipcode_text);
        zipcodeText = findViewById(R.id.add_zip);
        enterQuery = findViewById(R.id.zipcode_button);
        enterQuery.setOnClickListener(btnCheckDownloadLocationOnClickListener);
        context = this;
    }

    View.OnClickListener btnCheckDownloadLocationOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {

            zip = zipcodeText.getText().toString();

            if (zip.length() == 0)
                Toast.makeText(LocationActivity.this, "Field is empty", Toast.LENGTH_SHORT).show();
            else if(zip.length() != 5)
                Toast.makeText(LocationActivity.this, "Please enter a full zip code", Toast.LENGTH_LONG).show();
            else
            {
                try {
                    LocationHelper.setZipcodeAndAll(zip, context);
                    LocationHelper.setLocationPermission(false);

                    SharedPreferences.Editor editor = getSharedPreferences("ZIP_PREF", MODE_PRIVATE).edit();
                    editor.putString("zipCode", zip);
                    editor.apply();

                    Intent intent = new Intent(LocationActivity.this.getApplicationContext(), RestaurantViewActivity.class);
                    LocationActivity.this.startActivity(intent);
                }
                catch (IOException e) {
                    Toast.makeText(LocationActivity.this, "Unable to find location with zipcode, Please allow the noni to access location",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    };
}
