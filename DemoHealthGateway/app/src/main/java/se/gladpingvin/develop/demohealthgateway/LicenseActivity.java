package se.gladpingvin.develop.demohealthgateway;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

/**
 * Activity class for showing license information.
 */
public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
    }

    /**
     * Starts an instance of the LgplActivity. This method is meant to be used as an onClick method
     * by a button, therefore, to match the onClick pattern, it takes a View object (which isn't
     * used) as a parameter.
     * @param view sent automatically by the onClick action, not used.
     */
    public void startLgplActivity (View view) {
        Intent lgpl = new Intent(this, LgplActivity.class);
        startActivity(lgpl);
    }

    /**
     * Starts an instance of the GplActivity. This method is meant to be used as an onClick method
     * by a button, therefore, to match the onClick pattern, it takes a View object (which isn't
     * used) as a parameter.
     * @param view sent automatically by the onClick action, not used.
     */
    public void startGplActivity (View view) {
        Intent gpl = new Intent(this, GplActivity.class);
        startActivity(gpl);
    }
}
