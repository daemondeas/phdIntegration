package se.gladpingvin.develop.demohealthgateway;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

/**
 * Activity class for presenting the GPL license.
 */
public class GplActivity extends AppCompatActivity {
    private WebView license;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpl);
        license = (WebView)findViewById(R.id.webViewGpl);

        setLicenseText();
    }

    /**
     * Sets the content of the WebView in the activity to an html version of the GPL
     */
    private void setLicenseText() {
        license.loadUrl("file:///android_asset/gpl.html");
    }
}
