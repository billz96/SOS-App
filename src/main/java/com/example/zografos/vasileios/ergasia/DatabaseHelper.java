package com.example.zografos.vasileios.ergasia;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by vasilis on 8/1/18.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "MyDB.db"; // database name

    public DatabaseHelper(Context context) {
        // call the parent constructor
        super(context, DB_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create a new table
        db.execSQL("CREATE TABLE AppEvents " +
                   "(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, info TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // drop the old table and create the new one
        db.execSQL("DROP TABLE IF EXISTS AppEvents");
        onCreate(db);
    }


    // add a new row in AppEvent Table
    public boolean addEvent(String name, String info){

        SQLiteDatabase db = this.getWritableDatabase();

        // matches a key of type string with a value
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("info", info);

        long result = db.insert("AppEvents", null, contentValues);
        // check if the insertion had failed
        if (result == -1){
            return false;
        }else{
            return true;
        }

    }
}
