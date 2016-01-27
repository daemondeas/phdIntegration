package se.gladpingvin.develop.demohealthgateway;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Helper class containing static help methods for other classes in the app to use.
 * Created by SEprjASv on 2015-10-22.
 */
public class AntidoteHelper {
    private static final String TAG = "AntidoteHelper";

    /**
     * Transforms an xml Node object into an xml String
     * @param node the Node to transform
     * @return the input Node as an xml String
     */
    public static String getXmlText(Node node) {
        String s = null;
        NodeList text = node.getChildNodes();

        for (int l = 0; l < text.getLength(); ++l) {
            Node txt = text.item(l);
            if (txt.getNodeType() == Node.TEXT_NODE) {
                if (s == null) {
                    s = "";
                }
                s += txt.getNodeValue();
            }
        }

        return s;
    }

    /**
     * Generates an xml Document object from an xml String
     * @param xml the String to be transformed
     * @return the input String as a Document object
     */
    public static Document parseXml(String xml) {
        Document document = null;

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (ParserConfigurationException pce) {
            Log.e(TAG, "Something went wrong with the xml parser's configuration: " + pce.toString());
        } catch (SAXException se) {
            Log.e(TAG, "XML parsing caused an exception: " + se.toString());
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't parse xml: " + ioe.toString());
        }

        return document;
    }

    /**
     * Returns all PulseOximetryMeasurement objects stored in the applications local database
     * @param context a Context from the application is needed in order to fetch the database
     * @return all PulseOximetryMeasurements from the database of the input Context
     */
    public static List<PulseOximetryMeasurement> readPulseOximetryMeasurementsFromDatabase(
            Context context) {
        List<PulseOximetryMeasurement> list = new ArrayList<>();

        DatabaseHandler dbHelper = new DatabaseHandler(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseHandler.COLUMN_NAME_TIMESTAMP,
                DatabaseHandler.COLUMN_NAME_SATURATION,
                DatabaseHandler.COLUMN_NAME_SATURATIONUNIT,
                DatabaseHandler.COLUMN_NAME_HEARTRATE,
                DatabaseHandler.COLUMN_NAME_HEARTRATEUNIT,
                DatabaseHandler.COLUMN_NAME_PATIENT
        };
        String sortOrder = DatabaseHandler.COLUMN_NAME_TIMESTAMP + " DESC";

        Cursor cursor = db.query(
                DatabaseHandler.TABLE_NAME_OXIMETRY,
                projection,
                null,
                new String[]{},
                null,
                null,
                sortOrder
        );

        if (cursor.moveToFirst()) {
            do {
                PulseOximetryMeasurement measurement = new PulseOximetryMeasurement(
                    cursor.getFloat(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_HEARTRATE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_HEARTRATEUNIT)),
                    cursor.getFloat(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_SATURATION)),
                    cursor.getString(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_SATURATIONUNIT)),
                    gregorianCalendarFromString(cursor.getString(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_TIMESTAMP))),
                    cursor.getString(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_PATIENT))
                );
                list.add(measurement);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        Log.v(TAG, "Read measurements from database.");
        return list;
    }

    /**
     * Checks if a PulseOximetryMeasurement already exists in the local database.
     * It does check for identical measurements withing a time frame of +/- 1 second in the
     * database, because sometimes when multiple instances run in the background, they retrieve
     * the measurement with a few milliseconds difference, which can happen to occur around a
     * "second boundary", therefore only checking for the same second can still cause duplicates
     * in the database.
     * @param context Context to get the database
     * @param measurement the PulseOximetryMeasurement to check for
     * @return true if measurement already exists in the database, otherwise false
     */
    private static boolean measurementAlreadyExists(Context context,
                                                    PulseOximetryMeasurement measurement) {
        DatabaseHandler dbHelper = new DatabaseHandler(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseHandler.COLUMN_NAME_HEARTRATE,
                DatabaseHandler.COLUMN_NAME_SATURATION,
                DatabaseHandler.COLUMN_NAME_TIMESTAMP
        };

        String whereClause = DatabaseHandler.COLUMN_NAME_PATIENT + " = ?";

        String[] whereArgs = {
                measurement.getPatient()
        };

        String sortOrder = DatabaseHandler.COLUMN_NAME_SATURATION + " ASC";

        Cursor cursor = db.query(
                DatabaseHandler.TABLE_NAME_OXIMETRY,
                projection,
                whereClause,
                whereArgs,
                null,
                null,
                sortOrder
        );

        if (cursor.moveToFirst()) {
            do {
                if (cursor.getFloat(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_HEARTRATE))
                        == measurement.getHeartRate() &&
                        cursor.getFloat(cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_SATURATION))
                        == measurement.getBloodOxygenSaturation()){
                    GregorianCalendar ts1 = gregorianCalendarFromString(cursor.getString(
                            cursor.getColumnIndex(DatabaseHandler.COLUMN_NAME_TIMESTAMP)));
                    GregorianCalendar ts2 = measurement.getTimeStamp();
                    if (ts1.get(Calendar.YEAR) == ts2.get(Calendar.YEAR) &&
                            ts1.get(Calendar.MONTH) == ts2.get(Calendar.MONTH) &&
                            ts1.get(Calendar.DAY_OF_MONTH) == ts2.get(Calendar.DAY_OF_MONTH) &&
                            ts1.get(Calendar.HOUR_OF_DAY) == ts2.get(Calendar.HOUR_OF_DAY) &&
                            ts1.get(Calendar.MINUTE) == ts2.get(Calendar.MINUTE) &&
                            Math.abs(ts1.get(Calendar.SECOND) - ts2.get(Calendar.SECOND)) < 2) {
                        cursor.close();
                        db.close();
                        Log.v(TAG, "Copy found in db!");
                        return true;
                    }
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return false;
    }

    /**
     * Writes a PulseOximetryMeasurement object to the application's local database.
     * @param context Context to fetch the database from
     * @param measurement the PulseOximetryMeasurement to store
     * @return true if measurement was successfully stored in the database or already exists in the
     * database, otherwise false
     */
    public static boolean writeMeasurementToDatabase(Context context,
                                                  PulseOximetryMeasurement measurement) {
        if (!measurementAlreadyExists(context, measurement)) {
            DatabaseHandler dbHelper = new DatabaseHandler(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseHandler.COLUMN_NAME_HEARTRATE, measurement.getHeartRate());
            values.put(DatabaseHandler.COLUMN_NAME_HEARTRATEUNIT,
                    measurement.getHeartRateUnit());
            values.put(DatabaseHandler.COLUMN_NAME_SATURATION,
                    measurement.getBloodOxygenSaturation());
            values.put(DatabaseHandler.COLUMN_NAME_SATURATIONUNIT,
                    measurement.getBloodOxygenSaturationUnit());
            values.put(DatabaseHandler.COLUMN_NAME_TIMESTAMP,
                    gregorianCalendarToString(measurement.getTimeStamp()));
            values.put(DatabaseHandler.COLUMN_NAME_PATIENT, measurement.getPatient());

            long id = db.insert(DatabaseHandler.TABLE_NAME_OXIMETRY, null, values);

            Log.v(TAG, id == -1 ? "Failed writing to database" : "Successfully wrote to database");

            db.close();
            return id != -1;
        }

        return true;
    }

    /**
     * Deletes a PulseOximetryMeasurement from the application's local database.
     * @param context Context to fetch the database from
     * @param measurement the PulseOximetryMeasurement to delete
     * @return true if measurement was successfully deleted or if it didn't exist in the database
     * to begin with, otherwise false
     */
    public static boolean deleteMeasurementFromDatabase(Context context,
                                                     PulseOximetryMeasurement measurement) {
        if (measurementAlreadyExists(context, measurement)) {
            DatabaseHandler dbHelper = new DatabaseHandler(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String whereClause = DatabaseHandler.COLUMN_NAME_HEARTRATE + " = ? AND " +
                    DatabaseHandler.COLUMN_NAME_HEARTRATEUNIT + " = ? AND " +
                    DatabaseHandler.COLUMN_NAME_SATURATION + " = ? AND " +
                    DatabaseHandler.COLUMN_NAME_SATURATIONUNIT + " = ? AND " +
                    DatabaseHandler.COLUMN_NAME_TIMESTAMP + " = ? AND " +
                    DatabaseHandler.COLUMN_NAME_PATIENT + " = ?";

            String[] whereArgs = {
                    Float.toString(measurement.getHeartRate()),
                    measurement.getHeartRateUnit(),
                    Float.toString(measurement.getBloodOxygenSaturation()),
                    measurement.getBloodOxygenSaturationUnit(),
                    gregorianCalendarToString(measurement.getTimeStamp()),
                    measurement.getPatient()
            };

            boolean result = db.delete(DatabaseHandler.TABLE_NAME_OXIMETRY, whereClause, whereArgs)
                    > 0;

            db.close();

            Log.v(TAG, result ? "Deletion succeeded" : "Deletion failed");

            return result;
        }

        return true;
    }

    /**
     * Creates a String representation of a GregorianCalendar, for storing in the database.
     * The String is created on the form YYYYMMDDHHMMSS
     * @param calendar the GregorianCalendar to convert
     * @return a String representation of calendar
     */
    private static String gregorianCalendarToString(GregorianCalendar calendar) {

        return String.valueOf(calendar.get(Calendar.YEAR)) + (calendar.get(Calendar.MONTH) < 10 ?
                "0" + calendar.get(Calendar.MONTH) :
                calendar.get(Calendar.MONTH)) +
            (calendar.get(Calendar.DAY_OF_MONTH) < 10 ?
                "0" + calendar.get(Calendar.DAY_OF_MONTH) :
                calendar.get(Calendar.DAY_OF_MONTH)) +
            (calendar.get(Calendar.HOUR_OF_DAY) < 10 ?
                "0" + calendar.get(Calendar.HOUR_OF_DAY) :
                calendar.get(Calendar.HOUR_OF_DAY)) +
            (calendar.get(Calendar.MINUTE) < 10 ?
                "0" + calendar.get(Calendar.MINUTE) :
                calendar.get(Calendar.MINUTE)) +
            (calendar.get(Calendar.SECOND) < 10 ?
                "0" + calendar.get(Calendar.SECOND) :
                calendar.get(Calendar.SECOND));
    }

    /**
     * Converts a String on the form YYYYMMDDHHMMSS into a GregorianCalendar
     * @param time the String to convert
     * @return a GregorianCalendar corresponding to time, should time not be of the correct format,
     * the current time is returned instead
     */
    private static GregorianCalendar gregorianCalendarFromString(String time) {
        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;

        try {
            year = Integer.parseInt(time.substring(0, 4));
            month = Integer.parseInt(time.substring(4, 6));
            day = Integer.parseInt(time.substring(6, 8));
            hour = Integer.parseInt(time.substring(8, 10));
            minute = Integer.parseInt(time.substring(10, 12));
            second = Integer.parseInt(time.substring(12, 14));
        } catch (IndexOutOfBoundsException ioobe) {
            Log.v(TAG, "Timestamp string from database was too short.");
            Log.v(TAG, time);
            return new GregorianCalendar();
        } catch (NumberFormatException nfe) {
            Log.v(TAG, "Timestamp string from database contained non-numbers.");
            return new GregorianCalendar();
        }

        return new GregorianCalendar(year, month, day, hour, minute, second);
    }

    /**
     * Converts a String of the html datetime-local format (YYYY-MM-DDTHH:MM) into a
     * GregorianCalendar
     * @param time the String to convert
     * @return a GregorianCalendar corresponding to time, should time not be of the correct format,
     * the current time is returned instead
     */
    public static GregorianCalendar htmlStringToGregorianCalendar(String time) {
        int year;
        int month;
        int day;
        int hour;
        int minute;

        try {
            year = Integer.parseInt(time.substring(0, 4));
            month = Integer.parseInt(time.substring(5, 7));
            day = Integer.parseInt(time.substring(8, 10));
            hour = Integer.parseInt(time.substring(11, 13));
            minute = Integer.parseInt(time.substring(14, 16));
        } catch (IndexOutOfBoundsException ioobe) {
            Log.v(TAG, "Timestamp string from database was too short.");
            Log.v(TAG, time);
            return new GregorianCalendar();
        } catch (NumberFormatException nfe) {
            Log.v(TAG, "Timestamp string from database contained non-numbers.");
            return new GregorianCalendar();
        }

        month--;
        return new GregorianCalendar(year, month, day, hour, minute);
    }

    /**
     * Converts a GregorianCalendar into a String of the html datetime-local format
     * (YYYY-MM-DDTHH:MM)
     * @param timeStamp the GregorianCalendar to convert
     * @return a String representation of timeStamp in html datetime-local format
     */
    public static String timeStampAsHtmlString(GregorianCalendar timeStamp) {
        return String.valueOf(timeStamp.get(Calendar.YEAR)) + '-' +
                niceNumber(timeStamp.get(Calendar.MONTH) + 1) + '-' +
                niceNumber(timeStamp.get(Calendar.DAY_OF_MONTH)) + 'T' +
                niceNumber(timeStamp.get(Calendar.HOUR_OF_DAY)) + ':' +
                niceNumber(timeStamp.get(Calendar.MINUTE));
    }

    /**
     * Turns an int representing a month or day of month into a two-digit string, i.e. adds a 0 at
     * the beginning, if the month/day is between 1-9 inclusive.
     * @param n the month/day int to convert
     * @return a String representation of n
     */
    private static String niceNumber(int n) {
        return n < 10 ? "0" + n : Integer.toString(n);
    }
}
