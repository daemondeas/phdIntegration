package se.gladpingvin.develop.demohealthgateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Set;

/**
 * The Activity class that is first presented when the application is started
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private BluetoothAdapter bluetoothAdapter;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initiateSpinner();
    }

    /**
     * A method that starts the PulseOximetryListActivity, intended to be used as an onClick method,
     * hence a View is taken as a parameter.
     * @param view Generated automatically when used as an onClick method, is never used.
     */
    public void startList(View view) {
        Intent startListActivity = new Intent(this, PulseOximetryListActivity.class);
        startActivity(startListActivity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
        } else if (id == R.id.action_seed_database) {
            Intent startSeedingActivity = new Intent(this, SeedDatabaseActivity.class);
            startActivity(startSeedingActivity);
        } else if (id == R.id.action_show_licenses) {
            Intent startLicenseActivity = new Intent(this, LicenseActivity.class);
            startActivity(startLicenseActivity);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Fills the device choice spinner with all medical devices that are paired with the device
     * running the application.
     */
    private void initiateSpinner() {
        Spinner deviceSpinner = (Spinner) findViewById(R.id.deviceSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.device_name);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getBluetoothClass().getMajorDeviceClass() ==
                        BluetoothClass.Device.Major.HEALTH) {
                    adapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }

        deviceSpinner.setAdapter(adapter);
        deviceSpinner.setOnItemSelectedListener(this);
    }

    /**
     * Starts the right listening activity depending on the device chosen in the device spinner.
     * This method is intended to be used as an onClick method, hence the unused View parameter.
     * @param v generated automatically when used as an onClick method, is never used.
     */
    public void startListening(View v) {
        if (address == null) {
            Toast.makeText(this, R.string.warning_choose_device, Toast.LENGTH_LONG).show();
            return;
        }

        Intent activityToStart = null;

        switch (bluetoothAdapter.getRemoteDevice(address).getBluetoothClass().getDeviceClass()) {
            case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                break;
            case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                break;
            case BluetoothClass.Device.HEALTH_GLUCOSE:
                break;
            case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                activityToStart = new Intent(this, PulseOximeterActivity.class);
                break;
            case BluetoothClass.Device.HEALTH_PULSE_RATE:
                break;
            case BluetoothClass.Device.HEALTH_THERMOMETER:
                break;
            case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                break;
            case BluetoothClass.Device.HEALTH_WEIGHING:
                break;
            default:
                // This really shouldn't happen, but it's better to put an error message than
                // crashing. We could maybe get here if new device classes are added without this
                // software being updated..
                Toast.makeText(this, R.string.health_device_is_no_health_device,
                        Toast.LENGTH_LONG).show();
        }

        if (activityToStart != null) {
            startActivity(activityToStart);
        } else {
            Toast.makeText(this, R.string.warning_choose_device, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Takes a String from the device spinner list and returns the Bluetooth MAC address from it
     * @param device the device String to extract the Bluetooth MAC address from
     * @return a String only containing the Bluetooth MAC address from device
     */
    private String getAddress(String device) {
        return device.length() >= 17 ? device.substring(device.length() - 17) :
                null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        address = getAddress((String)parent.getItemAtPosition(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing :>
    }
}
