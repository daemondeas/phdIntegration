package se.gladpingvin.develop.demohealthgateway;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Class for handling the application's settings.
 * Created by SEprjASv on 2015-11-16.
 */
public class Settings {
    private String patient;
    private String backendUrl;
    private boolean automaticProgramFlow;
    private static Settings instance = null;
    private Context context;

    /**
     * Getter method for patient identification
     * @return the patient identifier set in the Settings
     */
    public String getPatient() {
        return patient;
    }

    /**
     * Setter method for patient identification, also updates the Settings table in the database
     * @param patient the new patient identifier to set
     */
    public void setPatient(String patient) {
        // The trimming is done to remove the ending space that many new Android devices put after
        // a word, and an identifier is unlikely to end with a whitespace of any kind
        this.patient = patient.trim();

        DatabaseHandler dbHandler = new DatabaseHandler(context);
        SQLiteDatabase db = dbHandler.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHandler.COLUMN_NAME_PATIENT, this.patient);

        String whereClause = "id = ?";

        String[] whereArgs = { "1" };

        db.update(DatabaseHandler.TABLE_NAME_SETTINGS, contentValues, whereClause, whereArgs);
        db.close();
    }

    /**
     * Getter method for backend url
     * @return the backend url set in the Settings
     */
    public String getBackendUrl() {
        return backendUrl;
    }

    /**
     * Setter method for backend url, also updates the Settings table in the database
     * @param url the new backend url to set
     */
    public void setBackendUrl(String url) {
        backendUrl = url;

        DatabaseHandler dbHandler = new DatabaseHandler(context);
        SQLiteDatabase db = dbHandler.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHandler.COLUMN_NAME_URL, backendUrl);

        String whereClause = "id = ?";

        String[] whereArgs = { "1" };

        db.update(DatabaseHandler.TABLE_NAME_SETTINGS, contentValues, whereClause, whereArgs);
        db.close();
    }

    /**
     * Getter method for automatic program flow
     * @return the boolean value for automatic program flow set in the Settings
     */
    public boolean isAutomaticProgramFlow() {
        return automaticProgramFlow;
    }

    /**
     * Setter method for automatic program flow, also updates the Settings table in the database
     * @param automaticProgramFlow whether program flow should be automatic (true) or manual (false)
     */
    public void setAutomaticProgramFlow(boolean automaticProgramFlow) {
        this.automaticProgramFlow = automaticProgramFlow;

        DatabaseHandler dbHandler = new DatabaseHandler(context);
        SQLiteDatabase db = dbHandler.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW,
                automaticProgramFlow ? 1 : 0);

        String whereClause = "id = ?";

        String[] whereArgs = { "1" };

        db.update(DatabaseHandler.TABLE_NAME_SETTINGS, contentValues, whereClause, whereArgs);
        db.close();
    }

    private Settings () {

    }

    /**
     * Getter method for the singleton instance of Settings, it has to be called from some kind of
     * graphical class to begin with, as the Context to fetch the application's database is set on
     * the first call to getInstance(). This design enables backend code to get the settings as
     * well, using a null reference as parameter.
     * @param context A Context for fetching the database
     * @return the singleton instance of the Settings class
     */
    public static Settings getInstance(Context context) {
        if (instance == null) {
            instance = new Settings();
            instance.context = context;
            readSettings(instance);
        }

        return instance;
    }

    private static void readSettings(Settings settings) {
        DatabaseHandler dbHandler = new DatabaseHandler(settings.context);
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        String[] projection = {
                DatabaseHandler.COLUMN_NAME_PATIENT,
                DatabaseHandler.COLUMN_NAME_URL,
                DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW
        };

        String whereClause = "id = ?";

        String[] whereArgs = {
                "1"
        };

        Cursor cursor = db.query(
                DatabaseHandler.TABLE_NAME_SETTINGS,
                projection,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            settings.patient = cursor.getString(cursor.getColumnIndex(
                    DatabaseHandler.COLUMN_NAME_PATIENT));
            settings.backendUrl = cursor.getString(cursor.getColumnIndex(
                    DatabaseHandler.COLUMN_NAME_URL));
            settings.automaticProgramFlow = cursor.getInt(cursor.getColumnIndex(
                    DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW)) == 1;
        }

        cursor.close();
        db.close();
    }
}
