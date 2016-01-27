package se.gladpingvin.develop.demohealthgateway;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Activity class for showing all PulseOximetryMeasurement objects stored in the application's local
 * database.
 */
public class PulseOximetryListActivity extends AppCompatActivity {
    private static final String TAG = "POLActivity";
    private ProgressBar progressBar;
    private TextView progressText;
    private ArrayAdapter<String> measurementArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_oximetry_list);

        progressBar = (ProgressBar) findViewById(R.id.progressBarList);
        progressText = (TextView) findViewById(R.id.progressTextListActivity);

        ArrayList<PulseOximetryMeasurement> measurements = (ArrayList<PulseOximetryMeasurement>)
                AntidoteHelper.readPulseOximetryMeasurementsFromDatabase(this);

        measurementArrayAdapter = new ArrayAdapter<>(this,
                R.layout.list_item_pulse_oximetry);

        ListView listView = (ListView) findViewById(R.id.pulseOximetryListView);
        listView.setAdapter(measurementArrayAdapter);

        for (PulseOximetryMeasurement measurement : measurements) {
            measurementArrayAdapter.add(getString(R.string.saturation) +
                    (int) measurement.getBloodOxygenSaturation() +
                    measurement.getBloodOxygenSaturationUnit() + getString(R.string.heart_rate) +
                    (int) measurement.getHeartRate() + measurement.getHeartRateUnit() + "\n" +
                    niceDate(measurement.getTimeStamp()) + " " + measurement.getPatient());
        }
    }

    /**
     * Converts a GregorianCalendar into a String representation of its time in human readable form
     * (D/M - YYYY HH:MM:SS) where D and M can be either one or two digits, depending on their
     * values (i.e. only hours, minutes and seconds get a leading '0' when they are smaller than 10)
     * @param calendar the GregorianCalendar to convert
     * @return a human readable String representation of calendar
     */
    private String niceDate(GregorianCalendar calendar) {

        return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                (calendar.get(Calendar.MONTH) + 1) + " - " + calendar.get(Calendar.YEAR) + " " +
                (calendar.get(Calendar.HOUR_OF_DAY) < 10 ?
                        "0" + calendar.get(Calendar.HOUR_OF_DAY) :
                        calendar.get(Calendar.HOUR_OF_DAY)) + ":" +
                (calendar.get(Calendar.MINUTE) < 10 ?
                        "0" + calendar.get(Calendar.MINUTE) :
                        calendar.get(Calendar.MINUTE)) + ":" +
                (calendar.get(Calendar.SECOND) < 10 ?
                        "0" + calendar.get(Calendar.SECOND) :
                        calendar.get(Calendar.SECOND));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pulse_oximetry_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_upload) {
            uploadAllMeasurements();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows uploading progress
     * @param finished number of acknowledged uploaded measurements
     * @param total number of measurements to upload in total
     */
    private void showProgress(int finished, int total) {
        progressText.setText(String.format(getResources().getString(R.string.uploading_measurements),
                finished, total));
        progressBar.setProgress(finished);
    }

    /**
     * Removes the measurements that have been uploaded to the backend server from the application's
     * local database, clears the list of measurements in the activity and then adds any
     * measurements still in the database to it.
     * @param result if true all PulseOximetryMeasurements in measurements will be removed from the
     *               local database, if false each measurement will be checked for at the backend
     *               server and only deleted if it was found
     * @param measurements the list of PulseOximetryMeasurements to delete from the database or to
     *                     check if they should be deleted from the database (the measurements in
     *                     this list should have been uploaded to the backend server if this method
     *                     is called)
     */
    private void reloadList(boolean result, List<PulseOximetryMeasurement> measurements) {
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);

        measurementArrayAdapter.clear();

        // If all measurements were successfully uploaded to server
        if (result) {
            for (PulseOximetryMeasurement measurement : measurements) {
                AntidoteHelper.deleteMeasurementFromDatabase(this, measurement);
            }
        } else {
            for (PulseOximetryMeasurement measurement : measurements) {
                try {
                    if (new CheckIfPulseOximetryMeasurementExistsOnServerTask().execute(measurement)
                            .get(7, TimeUnit.SECONDS)) {
                        AntidoteHelper.deleteMeasurementFromDatabase(this, measurement);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

        for (PulseOximetryMeasurement measurement :
                AntidoteHelper.readPulseOximetryMeasurementsFromDatabase(this)) {
            measurementArrayAdapter.add(getString(R.string.saturation) +
                    (int) measurement.getBloodOxygenSaturation() +
                    measurement.getBloodOxygenSaturationUnit() + getString(R.string.heart_rate) +
                    (int) measurement.getHeartRate() + measurement.getHeartRateUnit() + "\n" +
                    niceDate(measurement.getTimeStamp()) + " " + measurement.getPatient());
        }
    }

    /**
     * Uploads all measurements listed in this Activity to the backend server. Also sets up a
     * progress circle to show the uploading progress to the user.
     */
    private void uploadAllMeasurements() {
        Settings.getInstance(this); // Initiate Settings object
        final List<PulseOximetryMeasurement> measurementsList = AntidoteHelper.
                readPulseOximetryMeasurementsFromDatabase(this);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(measurementsList.size());
        progressText.setVisibility(View.VISIBLE);

        UploadPulseOximetryMeasurementsTask task = new UploadPulseOximetryMeasurementsTask() {
            @Override
            protected void onProgressUpdate(Integer... progress) {
                showProgress(progress[0], progress[1]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                reloadList(result, measurementsList);
            }
        };

        PulseOximetryMeasurement[] measurements = measurementsList.toArray(
                new PulseOximetryMeasurement[measurementsList.size()]);

        task.execute(measurements);
    }
}
