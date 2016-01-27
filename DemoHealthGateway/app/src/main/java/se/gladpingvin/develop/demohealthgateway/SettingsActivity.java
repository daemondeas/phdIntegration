package se.gladpingvin.develop.demohealthgateway;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

/**
 * Activity class for altering the application's Settings
 */
public class SettingsActivity extends AppCompatActivity {
    private EditText patientName;
    private EditText backendUrl;
    private boolean automaticProgramFlowValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        patientName = (EditText)findViewById(R.id.patientName);
        patientName.setText(Settings.getInstance(this).getPatient());

        backendUrl = (EditText)findViewById(R.id.url);
        backendUrl.setText(Settings.getInstance(this).getBackendUrl());

        automaticProgramFlowValue = Settings.getInstance(this).isAutomaticProgramFlow();

        Switch automaticProgramFlow = (Switch) findViewById(R.id.automaticProgramFlow);
        automaticProgramFlow.setChecked(automaticProgramFlowValue);

        automaticProgramFlow.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        automaticProgramFlowValue = isChecked;
                    }
                });
    }

    /**
     * Saves the settings put into the fields in this Activity to the Settings instance and database
     * table. This method is designed to be used as an onClick method, hence the View parameter.
     * @param view generated automatically when used as an onClick method, never used.
     */
    public void saveSettings(View view) {
        Settings.getInstance(this).setPatient(patientName.getText().toString());
        Settings.getInstance(this).setBackendUrl(backendUrl.getText().toString());
        Settings.getInstance(this).setAutomaticProgramFlow(automaticProgramFlowValue);

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
}
