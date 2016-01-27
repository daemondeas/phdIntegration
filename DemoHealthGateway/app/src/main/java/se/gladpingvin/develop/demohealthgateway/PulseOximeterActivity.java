package se.gladpingvin.develop.demohealthgateway;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;

/**
 * Activity class for receiving measurement data from pulse oximeters compliant with the IEEE 11073
 * standard (via Bluetooth).
 */
public class PulseOximeterActivity extends AppCompatActivity {
    private int[] specifications = {0x1004};
    private Handler handler;
    private HealthServiceAPI healthServiceAPI;
    private ProgressBar progressBar;
    private TextView tv;
    private PulseOximetryMeasurement measurement;

    private static final String TAG = "POAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_oximeter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        measurement = null;

        progressBar = (ProgressBar)findViewById(R.id.progressBarOxim);
        tv = (TextView)findViewById(R.id.textView2);

        handler = new Handler();
        Intent startHealthServiceIntent = new Intent(this, HealthService.class);
        startService(startHealthServiceIntent);
        bindService(startHealthServiceIntent, serviceConnection, 0);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            healthServiceAPI = HealthServiceAPI.Stub.asInterface(service);

            try {
                healthServiceAPI.ConfigurePassive(agent, specifications);
            } catch (RemoteException re) {
                Toast.makeText(getBaseContext(), R.string.agent_config_failed, Toast.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getBaseContext(), R.string.health_service_disconnected,
                    Toast.LENGTH_LONG).show();
        }
    };

    private HealthAgentAPI.Stub agent = new HealthAgentAPI.Stub() {
        @Override
        public void Connected(String dev, String addr) throws RemoteException {
            Log.v("POA", "Connected to " + dev);
            Log.v("POA", "..." + addr);
            progressBar.setVisibility(View.VISIBLE);
            tv.setText(getString(R.string.receiving_measurement));
        }

        @Override
        public void Associated(String dev, String xmldata) throws RemoteException {
            Log.v("POA", "Associated to " + dev);
            Log.v("POA", "..." + xmldata);
            final String idev = dev;
            Runnable req1 = new Runnable() {
                @Override
                public void run() {
                    requestConfig(idev);
                }
            };

            Runnable req2 = new Runnable() {
                @Override
                public void run() {
                    requestDeviceAttributes(idev);
                }
            };

            handler.postDelayed(req1, 1);
            handler.postDelayed(req2, 500);
        }

        @Override
        public void MeasurementData(String dev, String xmldata) throws RemoteException {
            Log.v("POA", "Measurement from " + dev);
            Log.v("POA", "..." + xmldata);
            handleMeasurement(dev, xmldata);
        }

        @Override
        public void DeviceAttributes(String dev, String xmldata) throws RemoteException {
            Log.v("POA", "Device attributes for " + dev);
            Log.v("POA", "..." + xmldata);
            // Not interesting atm
        }

        @Override
        public void Disassociated(String dev) throws RemoteException {
            Log.v("POA", "Disassociated from " + dev);
            // Not interesting atm
        }

        @Override
        public void Disconnected(String dev) throws RemoteException {
            Log.v("POA", "Disconnected from " + dev);
            // Not interesting atm
        }
    };

    private void requestConfig(String dev) {
        try {
            healthServiceAPI.GetConfiguration(dev);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't get device configuration.");
        }
    }

    private void requestDeviceAttributes(String dev) {
        try {
            healthServiceAPI.RequestDeviceAttributes(dev);
        } catch (RemoteException re) {
            Log.e(TAG, "Requesting the device's attributes caused an exception: " + re.toString());
        }
    }

    private void handleMeasurement(String path, String xml) {
        Document document = AntidoteHelper.parseXml(xml);

        Log.v(TAG, "XML for " + path + " was successfully parsed.");

        PulseOximetryMeasurement measurement = PulseOximetryMeasurement.fromXml(document,
                Settings.getInstance(this).getPatient());

        this.measurement = measurement;

        if (Settings.getInstance(this).isAutomaticProgramFlow()) {
            tv.setText(getString(R.string.uploading_measurements_start));
            saveToBackend(measurement);
        } else {
            progressBar.setVisibility(View.GONE);
            AntidoteHelper.writeMeasurementToDatabase(this, measurement);
            tv.setText(measurement.toString());
        }
    }

    /**
     * Saves the current measurement to the application's local database. Intended to be used as an
     * onClick method, hence the View parameter.
     * @param view generated automatically when used as an onClick method, not actually used
     */
    public void save(View view) {
        if (measurement == null) {
            Toast.makeText(this, R.string.no_measurement_yet, Toast.LENGTH_SHORT).show();
            return;
        }

        saveToBackend(measurement);
    }

    private void afterUpload(boolean result) {
        progressBar.setVisibility(View.GONE);

        Toast.makeText(this,
                result ? getString(R.string.upload_success) :
                        getString(R.string.upload_fail),
                Toast.LENGTH_SHORT).show();

        if (result) {
            if (!AntidoteHelper.deleteMeasurementFromDatabase(this, measurement)) {
                Log.w(TAG, "Failed to remove uploaded measurement..");
            }
        } else {
            AntidoteHelper.writeMeasurementToDatabase(this, measurement);
        }

        tv.setText(measurement.toString());
    }

    private void saveToBackend(PulseOximetryMeasurement measurement) {
        UploadPulseOximetryMeasurementsTask task = new UploadPulseOximetryMeasurementsTask() {
            @Override
            protected void onPostExecute(Boolean result) {
                afterUpload(result);
            }
        };

        task.execute(measurement);
    }
}
