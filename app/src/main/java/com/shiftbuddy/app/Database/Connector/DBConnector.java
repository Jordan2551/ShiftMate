package com.shiftbuddy.app.Database.Connector;

import android.app.backup.BackupManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.shiftbuddy.app.Database.Tables.Shifts.Shifts;
import com.shiftbuddy.app.Database.Tables.TableItem;
import com.shiftbuddy.app.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jorda_000 on 2/3/2016.
 * This class manages connections with the SQLite DB, re-creates, backs up and upgrades the DB
 */
public class DBConnector extends SQLiteOpenHelper {

    //Consts
    public static final String LOGTAG = "DBConnector";
    public static final String DB_NAME = "shiftMateDB";
    private static final int DB_VERSION = 49;

    //Vars
    private List<TableItem> dbDSTableList = new ArrayList<TableItem>();//Contains all the table data sources(which extend TableItem)

    public static SQLiteDatabase database;//A reference to the database

    // Object for intrinsic lock
    //public static final Object sDataLock = new Object();

    public DBConnector(Context context) {

        super(context, DB_NAME, null, DB_VERSION);

        //ADD DB TABLES HERE
        dbDSTableList.add(new Shifts());

    }

    public static void requestBackup() {
        BackupManager bm = new BackupManager(MainActivity.getContext());
        bm.dataChanged();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //Loop through each TableItem, execute create query and log successful creation
        for (TableItem tableItem : dbDSTableList) {
            db.execSQL(tableItem.createQuery);
            Log.i(tableItem.logTag, "Table created!");
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //Loop through each TableItem, execute drop query, log successful drop
        //and call onCreate to re-create the tables
        for (TableItem tableItem : dbDSTableList) {
            db.execSQL("DROP TABLE IF EXISTS " + tableItem.tableName);
            Log.i(tableItem.logTag, "Table Dropped!");
        }
        onCreate(db);

    }

    public void openConnection(){
        database = getWritableDatabase();//Save the reference of the database
        Log.i(LOGTAG, "Connection Open!");
    }

    public void closeConnection(){
        close();
        Log.i(LOGTAG, "Connection Closed!");
    }

}
