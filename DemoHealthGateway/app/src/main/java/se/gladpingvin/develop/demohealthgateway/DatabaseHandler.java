package se.gladpingvin.develop.demohealthgateway;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper class for handling the application's internal database.
 * Created by SEprjASv on 2015-10-28.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String TAG = "DBHandler";
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "measurements";

    public static final String TABLE_NAME_OXIMETRY = "oximetryMeasurements";
    public static final String COLUMN_NAME_HEARTRATE = "heartRate";
    public static final String COLUMN_NAME_HEARTRATEUNIT = "heartRateUnit";
    public static final String COLUMN_NAME_SATURATION = "bloodOxygenSaturation";
    public static final String COLUMN_NAME_SATURATIONUNIT = "bloodOxygenSaturationUnit";
    public static final String COLUMN_NAME_TIMESTAMP = "timeStamp";
    public static final String COLUMN_NAME_PATIENT = "patientIdentification";
    private static final String OXIMETRY_TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME_OXIMETRY + " (id INTEGER, " + COLUMN_NAME_HEARTRATE +
                    " REAL, " + COLUMN_NAME_HEARTRATEUNIT + " TEXT, " + COLUMN_NAME_SATURATION +
                    " REAL, " + COLUMN_NAME_SATURATIONUNIT + " TEXT, " + COLUMN_NAME_TIMESTAMP +
                    " TEXT, " + COLUMN_NAME_PATIENT + " TEXT, PRIMARY KEY(id ASC))";

    public static final String TABLE_NAME_SETTINGS = "settings";
    public static final String COLUMN_NAME_URL = "url";
    public static final String COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW = "automatic_flow";
    private static final String SETTINGS_TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME_SETTINGS + " (id INTEGER, " + COLUMN_NAME_PATIENT +
                    " TEXT, " + COLUMN_NAME_URL + " TEXT, " + COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW +
                    " INTEGER, PRIMARY KEY(id ASC))";

    DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(OXIMETRY_TABLE_CREATE);
        db.execSQL(SETTINGS_TABLE_CREATE);

        ContentValues values = new ContentValues();
        values.put(DatabaseHandler.COLUMN_NAME_PATIENT, "Default Patient");
        values.put(DatabaseHandler.COLUMN_NAME_URL, "http://example.com/");
        values.put(DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW, 0);
        long id = db.insert(DatabaseHandler.TABLE_NAME_SETTINGS, null, values);

        Log.v(TAG, id == -1 ? "Failed initialising settings" : "Initialised settings");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 4) {
            db.execSQL(SETTINGS_TABLE_CREATE);

            ContentValues values = new ContentValues();
            values.put(DatabaseHandler.COLUMN_NAME_PATIENT, "Default Patient");
            values.put(DatabaseHandler.COLUMN_NAME_URL, "http://example.com/");
            values.put(DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW, 0);
            long id = db.insert(DatabaseHandler.TABLE_NAME_SETTINGS, null, values);

            Log.v(TAG, id == -1 ? "Failed initialising settings" : "Initialised settings");
        } else if (oldVersion == 2 && newVersion == 4) {
            String SETTINGS_TABLE_ALTER = "ALTER TABLE " + TABLE_NAME_SETTINGS + " ADD COLUMN " +
                    COLUMN_NAME_URL + " TEXT";
            db.execSQL(SETTINGS_TABLE_ALTER);
            SETTINGS_TABLE_ALTER = "ALTER TABLE " + TABLE_NAME_SETTINGS + " ADD COLUMN " +
                    COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW + " INTEGER";
            db.execSQL(SETTINGS_TABLE_ALTER);

            String whereClause = "id = ?";
            String[] args = { "1" };

            ContentValues values = new ContentValues();
            values.put(DatabaseHandler.COLUMN_NAME_URL, "http://example.com/");
            values.put(DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW, 0);
            long id = db.update(DatabaseHandler.TABLE_NAME_SETTINGS, values, whereClause, args);

            Log.v(TAG, id == -1 ? "Failed initialising settings" : "Initialised settings");
        } else if (oldVersion == 3 && newVersion == 4) {
            String SETTINGS_TABLE_ALTER = "ALTER TABLE " + TABLE_NAME_SETTINGS + " ADD COLUMN " +
                    COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW + " INTEGER";
            db.execSQL(SETTINGS_TABLE_ALTER);

            String whereClause = "id = ?";
            String[] args = { "1" };

            ContentValues values = new ContentValues();
            values.put(DatabaseHandler.COLUMN_NAME_AUTOMATIC_PROGRAM_FLOW, 0);
            long id = db.update(DatabaseHandler.TABLE_NAME_SETTINGS, values, whereClause, args);

            Log.v(TAG, id == -1 ? "Failed initialising settings" : "Initialised settings");
        }
    }
}
