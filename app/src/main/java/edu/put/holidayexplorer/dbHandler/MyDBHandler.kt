package edu.put.holidayexplorer.dbHandler

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDBHandler(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version:Int) : SQLiteOpenHelper(
    context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 1
        val DATABASE_NAME = "holidayExplorer.db"
        val TABLE_FAVOURITE = "favourite"
        val COLUMN_ID = "_id"
        val COLUMN_NAME = "name"
        val COLUMN_COUNTRY = "country"
        val COLUMN_DATE = "holidayDate"
        val COLUMN_ADDED = "addedDate"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_FAVOURITE_TABLE = ("CREATE TABLE " + TABLE_FAVOURITE + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_COUNTRY + " TEXT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_ADDED + " TEXT " + ")"
                )
        db.execSQL(CREATE_FAVOURITE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVOURITE")
        onCreate(db)
    }

    fun addRecord(name: String, country: String, date: String, added: String) {
        val values = ContentValues()
        values.put(COLUMN_NAME, name)
        values.put(COLUMN_COUNTRY, country)
        values.put(COLUMN_DATE, date)
        values.put(COLUMN_ADDED, added)
        val db = this.writableDatabase
        db.insert(TABLE_FAVOURITE, null, values)
        db.close()
    }

    fun showList(): MutableList<ArrayList<String>> {
        val query = "SELECT * FROM $TABLE_FAVOURITE"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query,null)
        val holidayList: MutableList<ArrayList<String>> = mutableListOf()
        while (cursor.moveToNext()) {
            val record = arrayListOf<String>()
            val id = cursor.getString(0)
            val name = cursor.getString(1)
            val country = cursor.getString(2)
            val date = cursor.getString(3)
            val added = cursor.getString(4)
            record.add(id)
            record.add(name)
            record.add(country)
            record.add(date)
            record.add(added)

            holidayList.add(record)
        }

        cursor.close()
        db.close()
        return holidayList
    }

    fun deleteFromDB(id: String): Boolean  {
        val db = this.writableDatabase
        if (id == "all") {
            val deletedRows = db.delete(TABLE_FAVOURITE, null, null)
            db.close()
            return deletedRows > 0
        }
        else {
            val whereClause = "$COLUMN_ID = ?"
            val whereArgs = arrayOf(id)
            val deletedRows = db.delete(TABLE_FAVOURITE, whereClause, whereArgs)
            db.close()
            return deletedRows > 0
        }
    }

}