package august.com.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Guze.db";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS history(time TEXT , LeakSize TEXT)");//建表，对数据库进行操作等
        db.execSQL("CREATE TABLE IF NOT EXISTS calibration(pressure TEXT , LeakSize TEXT)");//建表，对数据库进行操作等
        Log.i("database", "create table------------->");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("database","update table-------------->");
    }
}