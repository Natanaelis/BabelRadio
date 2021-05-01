package com.babelsoft.babelradio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
    private static final String KEY_RADIO_IMAGE = "radio_image";
    private static final String KEY_RADIO_STREAM = "radio_stream";

    public InternalDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RADIOS_TABLE = "CREATE TABLE " + TABLE_RADIOS + "("
                + KEY_RADIO_INTERNAL_ID + " INTEGER PRIMARY KEY," + KEY_RADIO_ID + " INTEGER,"
                + KEY_RADIO_NAME + " TEXT," + KEY_RADIO_TAG + " TEXT,"
                + KEY_RADIO_IMAGE + " BLOB," + KEY_RADIO_STREAM + " TEXT"+ ")";
        db.execSQL(CREATE_RADIOS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RADIOS);
        // Create tables again
        onCreate(db);
    }

    public void addRadio(Radio radio) {
        if (!exists(radio)) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_RADIO_ID, radio.getRadioId());
            values.put(KEY_RADIO_NAME, radio.getRadioName());
            values.put(KEY_RADIO_TAG, radio.getRadioTag());
            values.put(KEY_RADIO_IMAGE, radio.getRadioImage());
            values.put(KEY_RADIO_STREAM, radio.getRadioStream());

            db.insert(TABLE_RADIOS, null, values);
            db.close();
        }
    }

    public Radio getRadio(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RADIOS, new String[] { KEY_RADIO_INTERNAL_ID,
                        KEY_RADIO_ID, KEY_RADIO_NAME, KEY_RADIO_TAG, KEY_RADIO_IMAGE,
                        KEY_RADIO_STREAM}, KEY_RADIO_INTERNAL_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Radio radio = new Radio(Integer.parseInt(cursor.getString(0)),
                Integer.parseInt(cursor.getString(1)), cursor.getString(2),
                cursor.getString(3), cursor.getBlob(4), cursor.getString(5));
        return radio;
    }

    public List<Radio> getAllRadios() {
        List<Radio> radioList = new ArrayList<Radio>();
        String selectQuery = "SELECT  * FROM " + TABLE_RADIOS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Radio radio = new Radio(Integer.parseInt(cursor.getString(0)),
                        Integer.parseInt(cursor.getString(1)), cursor.getString(2),
                        cursor.getString(3), cursor.getBlob(4), cursor.getString(5));
                radioList.add(radio);
            } while (cursor.moveToNext());
        }
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

    public void deleteRadio(Radio radio) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RADIOS, KEY_RADIO_INTERNAL_ID + " = ?",
                new String[] { String.valueOf(radio.getRadioInternalId()) });
        db.close();
    }


    public int count() {
        int count;
        String countQuery = "SELECT  * FROM " + TABLE_RADIOS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        count = cursor.getCount();
        cursor.close();
        return count;
    }

    public boolean exists(Radio newRadio) {
        boolean exists = false;
        List<Radio> allRadios = getAllRadios();
        for (Radio radio : allRadios) {
            if (radio.getRadioId() == newRadio.getRadioId())
                return true;
        }
        return exists;
    }
}