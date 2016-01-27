package se.gladpingvin.develop.demohealthgateway;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Activity class for presenting the LGPL license.
 */
public class LgplActivity extends AppCompatActivity {
    private WebView license;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lgpl);
        license = (WebView)findViewById(R.id.webViewLgpl);

        setLicenseText();
    }

    /**
     * Sets the content of the WebView in the activity to an html version of the LGPL
     */
    private void setLicenseText() {
        license.loadUrl("file:///android_asset/lgpl.html");
    }
}
