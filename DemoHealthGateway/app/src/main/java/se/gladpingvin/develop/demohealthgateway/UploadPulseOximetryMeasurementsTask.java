package se.gladpingvin.develop.demohealthgateway;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Asynchronous task for uploading PulseOximetryMeasurement objects to an OData web service.
 * Created by SEprjASv on 2015-11-25.
 */
public class UploadPulseOximetryMeasurementsTask extends AsyncTask<PulseOximetryMeasurement,
        Integer, Boolean> {
    private static final String TAG ="UMTask";
    private static final String URLEnding = "/PulseOximetryMeasurements";

    /**
     * Uploads an arbitrary number of PulseOximetryMeasurement objects to the web service with the
     * url defined in the Settings.
     * @param measurements the PulseOximetryMeasurements to upload
     * @return true if all measurements were successfully uploaded to the web service, otherwise
     * false
     */
    @Override
    protected Boolean doInBackground(PulseOximetryMeasurement... measurements) {
        boolean result = true;
        int finished = 0;

        for (PulseOximetryMeasurement measurement : measurements) {
            try {
                String data = URLEncoder.encode("BloodOxygenSaturation", "UTF-8") + "="
                        + URLEncoder.encode(
                        Float.toString(measurement.getBloodOxygenSaturation()), "UTF-8") +
                        "&" + URLEncoder.encode("BloodOxygenSaturationUnit", "UTF-8") + "="
                        + URLEncoder.encode(measurement.getBloodOxygenSaturationUnit(), "UTF-8") +
                        "&" + URLEncoder.encode("HeartRate", "UTF-8") + "=" + URLEncoder.encode(
                        Float.toString(measurement.getHeartRate()), "UTF-8") + "&" +
                        URLEncoder.encode("HeartRateUnit", "UTF-8") + "=" +
                        URLEncoder.encode(measurement.getHeartRateUnit(), "UTF-8") + "&" +
                        URLEncoder.encode("PatientIdentification", "UTF-8") + "=" +
                        URLEncoder.encode(measurement.getPatient(), "UTF-8") + "&" +
                        URLEncoder.encode("TimeStamp", "UTF-8") + "=" +
                        URLEncoder.encode(AntidoteHelper.timeStampAsHtmlString(
                                        measurement.getTimeStamp()),
                                "UTF-8");

                String urlString = Settings.getInstance(null).getBackendUrl()
                        + URLEnding;

                URL url = new URL(urlString);

                Log.v(TAG, "Uploading to " + urlString);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true); // POST
                connection.setChunkedStreamingMode(0);

                OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());

                outputStream.write(data.getBytes("UTF-8"));
                outputStream.flush();

                // For some reason, we have to read the response for the measurement to actually be
                // POSTed when using IIS/C#/.NET, it works perfectly without this part when using
                // Apache/PHP
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
                String line;

                StringBuilder jsonString = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    jsonString.append(line);
                }

                Log.v(TAG, "JSON String: " + jsonString.toString());

                JSONObject created = new JSONObject(jsonString.toString());

                PulseOximetryMeasurement createdMeasurement = new PulseOximetryMeasurement(created);

                boolean localResult = measurement.equals(createdMeasurement);

                result = localResult && result;

                if (localResult) {
                    finished++;
                }

                outputStream.close();
                bufferedReader.close();
                connection.disconnect();

                publishProgress(finished, measurements.length);
            } catch (MalformedURLException mfue) {
                Log.e(TAG, "Url was malformed: " + mfue.getMessage());
                result = false;
            } catch (IOException ioe) {
                Log.e(TAG, "IOException when trying to open connection.");
                result = false;
            } catch (JSONException je) {
                Log.e(TAG, "JSONException when receiving measurement.");
                result = false;
            }
        }

        return result;
    }
}
