package se.gladpingvin.develop.demohealthgateway;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

/**
 * Asynchronous task class for checking if a PulseOximetryMeasurement already exists on the backend
 * saved in Settings
 * Created by SEprjASv on 2015-12-03.
 */
public class CheckIfPulseOximetryMeasurementExistsOnServerTask extends
        AsyncTask<PulseOximetryMeasurement, Void, Boolean> {
    private static final String TAG ="CMTask";
    private static final String URLEnding = "/PulseOximetryMeasurements/";

    /**
     * Checks if a specific PulseOximetryMeasurement exists on the backend server. It works by
     * generating an Odata format URL for querying the backend server for the specific measurement,
     * if at least one copy is found on the backend server, true is returned. If more than one
     * measurement is provided as input, only the first one is regarded.
     * @param measurements the PulseOximetryMeasurement to check for (only the first one is checked)
     * @return true if at least one copy of measurements[0] is found on the backend server,
     * otherwise false (an Exception thrown before the check has been completed also results in a
     * false return value).
     */
    @Override
    protected Boolean doInBackground(PulseOximetryMeasurement... measurements) {
        boolean result = false;

        PulseOximetryMeasurement measurement = measurements[0];
        try {
            String urlString = Settings.getInstance(null).getBackendUrl()
                    + URLEnding + "?$filter=" +
                    URLEncoder.encode("BloodOxygenSaturation eq ", "UTF-8") +
                    URLEncoder.encode(Float.toString(measurement.getBloodOxygenSaturation()),
                            "UTF-8") +
                    URLEncoder.encode(" and BloodOxygenSaturationUnit eq \'", "UTF-8") +
                    URLEncoder.encode(measurement.getBloodOxygenSaturationUnit(), "UTF-8")+
                    URLEncoder.encode("\' and HeartRate eq ", "UTF-8") +
                    URLEncoder.encode(Float.toString(measurement.getHeartRate()), "UTF-8") +
                    URLEncoder.encode(" and HeartRateUnit eq \'", "UTF-8") +
                    URLEncoder.encode(measurement.getHeartRateUnit(), "UTF-8") +
                    URLEncoder.encode("\' and PatientIdentification eq \'", "UTF-8") +
                    URLEncoder.encode(measurement.getPatient(), "UTF-8") +
                    URLEncoder.encode("\' and year(TimeStamp) eq ", "UTF-8") +
                    URLEncoder.encode(Integer.toString(
                            measurement.getTimeStamp().get(Calendar.YEAR)),
                            "UTF-8") +
                    URLEncoder.encode(" and month(TimeStamp) eq ", "UTF-8") +
                    URLEncoder.encode(Integer.toString(
                            measurement.getTimeStamp().get(Calendar.MONTH) + 1),
                            "UTF-8") +
                    URLEncoder.encode(" and day(TimeStamp) eq ", "UTF-8") +
                    URLEncoder.encode(Integer.toString(
                            measurement.getTimeStamp().get(Calendar.DAY_OF_MONTH)),
                            "UTF-8") +
                    URLEncoder.encode(" and hour(TimeStamp) eq ", "UTF-8") +
                    URLEncoder.encode(Integer.toString(
                            measurement.getTimeStamp().get(Calendar.HOUR_OF_DAY)),
                            "UTF-8") +
                    URLEncoder.encode(" and minute(TimeStamp) eq ", "UTF-8") +
                    URLEncoder.encode(Integer.toString(
                            measurement.getTimeStamp().get(Calendar.MINUTE)),
                            "UTF-8");

            URL url = new URL(urlString);

            Log.v(TAG, "Querying " + urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;

            StringBuilder jsonString = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                jsonString.append(line);
            }

            Log.v(TAG, "JSON String: " + jsonString.toString());

            JSONObject resultJSON = new JSONObject(jsonString.toString());

            result = resultJSON.length() > 0;

            bufferedReader.close();
            connection.disconnect();
        } catch (MalformedURLException mfue) {
            Log.e(TAG, "Url was malformed: " + mfue.getMessage());
        } catch (IOException ioe) {
            Log.e(TAG, "IOException when trying to open connection.");
        } catch (JSONException je) {
            Log.e(TAG, "JSONException when receiving measurement.");
        }

        return result;
    }
}
