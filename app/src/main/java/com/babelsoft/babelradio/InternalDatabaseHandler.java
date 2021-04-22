package com.babelsoft.babelradio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class InternalDatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "radiosManager";
    private static final String TABLE_RADIOS = "radios";
    private static final String KEY_RADIO_INTERNAL_ID = "radio_internal_id";
    private static final String KEY_RADIO_ID = "radio_id";
    private static final String KEY_RADIO_NAME = "radio_name";
    private static final String KEY_RADIO_TAG = "radio_tag";
    private static final String KEY_RADIO_LOGO = "radio_logo";
    private static final String KEY_RADIO_STREAM = "radio_stream";

    public InternalDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RADIOS_TABLE = "CREATE TABLE " + TABLE_RADIOS + "("
                + KEY_RADIO_INTERNAL_ID + " INTEGER PRIMARY KEY," + KEY_RADIO_ID + " INTEGER,"
                + KEY_RADIO_NAME + " TEXT," + KEY_RADIO_TAG + " TEXT,"
                + KEY_RADIO_LOGO + " TEXT," + KEY_RADIO_STREAM + " TEXT"+ ")";
        db.execSQL(CREATE_RADIOS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RADIOS);

        // Create tables again
        onCreate(db);
    }

    // code to add the new radio
    void addRadio(Radio radio) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RADIO_ID, radio.getRadioId());
        values.put(KEY_RADIO_NAME, radio.getRadioName());
        values.put(KEY_RADIO_TAG, radio.getRadioTag());
        values.put(KEY_RADIO_LOGO, radio.getRadioImage());
        values.put(KEY_RADIO_STREAM, radio.getRadioStream());

        // Inserting Row
        db.insert(TABLE_RADIOS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // code to get the single radio
    Radio getRadio(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RADIOS, new String[] { KEY_RADIO_INTERNAL_ID,
                        KEY_RADIO_ID, KEY_RADIO_NAME, KEY_RADIO_TAG, KEY_RADIO_LOGO,
                        KEY_RADIO_STREAM}, KEY_RADIO_INTERNAL_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Radio radio = new Radio(Integer.parseInt(cursor.getString(0)),
                Integer.parseInt(cursor.getString(1)), cursor.getString(2),
                cursor.getString(3), cursor.getInt(4), cursor.getString(5));
        // return contact
        return radio;
    }

    // code to get all radios in a list view
    public List<Radio> getAllRadios() {
        List<Radio> radioList = new ArrayList<Radio>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_RADIOS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Radio radio = new Radio(Integer.parseInt(cursor.getString(0)),
                        Integer.parseInt(cursor.getString(1)), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4), cursor.getString(5));
                // Adding radio to list
                radioList.add(radio);
            } while (cursor.moveToNext());
        }

        // return radio list
        return radioList;
    }
/*
    // code to update the single contact
    public int updateContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, contact.getName());
        values.put(KEY_PH_NO, contact.getPhoneNumber());

        // updating row
        return db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(contact.getID()) });
    }
*/
    // Deleting single radio
    public void deleteRadio(Radio radio) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RADIOS, KEY_RADIO_INTERNAL_ID + " = ?",
                new String[] { String.valueOf(radio.getRadioInternalId()) });
        db.close();
    }

    // Getting radios Count
    public int count() {
        int count;
        String countQuery = "SELECT  * FROM " + TABLE_RADIOS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        count = cursor.getCount();
        cursor.close();

        // return count
        return count;
    }

}