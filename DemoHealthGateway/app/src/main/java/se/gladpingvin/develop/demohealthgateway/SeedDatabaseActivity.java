package se.gladpingvin.develop.demohealthgateway;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity class for seeding the application's database with pseudo-random PulseOximetryMeasurement
 * objects
 */
public class SeedDatabaseActivity extends AppCompatActivity {
    EditText numberOfSeeds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seed_database);

        numberOfSeeds = (EditText)findViewById(R.id.seedNumber);
    }

    /**
     * Seeds the database with the number of pseudo-random PulseOximetryMeasurements put into the
     * EditText field of the Activity. This method is intended to be used as an onClick method,
     * hence the View parameter.
     * @param view automatically generated when used as an onClick method, never used
     */
    public void seed(View view) {
        int amount = Integer.parseInt(numberOfSeeds.getText().toString());

        for (int i = 0; i < amount; i++) {
            if(!AntidoteHelper.writeMeasurementToDatabase(this, new PulseOximetryMeasurement())) {
                return;
            }
        }

        Toast.makeText(this, String.format(getResources().getString(R.string.seeding_done),
                amount), Toast.LENGTH_LONG).show();
    }
}
