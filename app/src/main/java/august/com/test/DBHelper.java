package august.com.test;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
        db.execSQL("CREATE TABLE IF NOT EXISTS detection(keyName TEXT , value TEXT)");//建表，对数据库进行操作等
        Log.i("database", "create table------------->");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("database", "update table-------------->");
    }

    public float getConfig(String key, float def) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * from detection where keyName = ?",  new String[] {key});
        c.moveToFirst();
        if (c.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put("keyName", key);
            values.put("value", def);
            getWritableDatabase().insert("detection", null, values);
            return def;
        }
        return c.getFloat(1);
    }

    public void putConfig(String key, float val) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * from detection where keyName = ?",  new String[] {key});
        ContentValues values = new ContentValues();

        values.put("keyName", key);
        values.put("value", val);
        if (c.getCount() == 0) {
            getWritableDatabase().insert("detection", null, values);
        } else {
            getWritableDatabase().update("detection", values, "keyName = ?", new String[]{key});
        }
    }
}