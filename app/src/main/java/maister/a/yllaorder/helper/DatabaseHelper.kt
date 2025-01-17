package maister.a.yllaorder.helper

import android.app.Activity
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DatabaseHelper(activity: Activity) :
    SQLiteOpenHelper(activity, DATABASE_NAME, null, DATABASE_VERSION) {
    val TABLE_CART_NAME = "tblcart"
    val PID = "pid"
    val VID = "vid"
    val QTY = "qty"
    val FavoriteTableInfo = TABLE_FAVORITE_NAME + "(" + KEY_ID + " TEXT" + ")"
    val SaveForLaterTableInfo =
        TABLE_SAVE_FOR_LATER_NAME + "(" + VID + " TEXT ," + PID + " TEXT ," + QTY + " TEXT)"
    val CartTableInfo = "$TABLE_CART_NAME($VID TEXT ,$PID TEXT ,$QTY TEXT)"
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $FavoriteTableInfo")
        db.execSQL("CREATE TABLE $CartTableInfo")
        db.execSQL("CREATE TABLE $SaveForLaterTableInfo")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        replaceDataToNewTable(db, TABLE_FAVORITE_NAME, FavoriteTableInfo)
        replaceDataToNewTable(db, TABLE_CART_NAME, CartTableInfo)
        replaceDataToNewTable(db, TABLE_SAVE_FOR_LATER_NAME, SaveForLaterTableInfo)
        onCreate(db)
    }

    fun replaceDataToNewTable(db: SQLiteDatabase, tableName: String, tableString: String) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $tableString")
        val columns = getColumns(db, tableName)
        db.execSQL("ALTER TABLE $tableName RENAME TO temp_$tableName")
        db.execSQL("CREATE TABLE $tableString")
        columns.retainAll(getColumns(db, tableName))
        val cols = join(columns)
        db.execSQL(
            String.format(
                "INSERT INTO %s (%s) SELECT %s from temp_%s",
                tableName, cols, cols, tableName
            )
        )
        db.execSQL("DROP TABLE temp_$tableName")
    }

    fun getColumns(db: SQLiteDatabase, tableName: String): MutableList<String> {
        var ar = ArrayList<String>()
        try {
            db.rawQuery("SELECT * FROM $tableName LIMIT 1", null).use { c ->
                if (c != null) {
                    ar = ArrayList(Arrays.asList(*c.columnNames))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ar
    }

    fun join(list: List<String>?): String {
        val buf = StringBuilder()
        val num = list!!.size
        for (i in 0 until num) {
            if (i != 0) buf.append(",")
            buf.append(list[i])
        }
        return buf.toString()
    }

    /*      FAVORITE TABLE OPERATION      */
    fun getFavoriteById(pid: String): Boolean {
        var count = false
        val db = this.writableDatabase
        val args = arrayOf(pid)
        val cursor = db.rawQuery(
            "SELECT " + KEY_ID + " FROM " + TABLE_FAVORITE_NAME + " WHERE " + KEY_ID + "=? ",
            args
        )
        if (cursor.moveToFirst()) {
            count = true
        }
        cursor.close()
        db.close()
        return count
    }

    fun addOrRemoveFavorite(id: String, isAdd: Boolean) {
        val db = this.writableDatabase
        if (isAdd) {
            addFavorite(id)
        } else {
            db.execSQL("DELETE FROM  " + TABLE_FAVORITE_NAME + " WHERE " + KEY_ID + " = " + id)
        }
        db.close()
    }

    fun addFavorite(id: String?) {
        val fav = ContentValues()
        fav.put(KEY_ID, id)
        val db = this.writableDatabase
        db.insert(TABLE_FAVORITE_NAME, null, fav)
    }

    fun favorite(): ArrayList<String> {
            val ids = ArrayList<String>()
            val selectQuery = "SELECT *  FROM " + TABLE_FAVORITE_NAME
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                do {
                    ids.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return ids
        }

    fun DeleteAllFavoriteData() {
        val database = this.writableDatabase
        database.execSQL("DELETE FROM " + TABLE_FAVORITE_NAME)
        database.close()
    }

    /*      CART TABLE OPERATION      */
    fun cartList(): ArrayList<String> {
            val ids = ArrayList<String>()
            val selectQuery = "SELECT *  FROM $TABLE_CART_NAME"
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                do {
                    val count = cursor.getString(cursor.getColumnIndex(QTY))
                    if (count == "0") {
                        db.execSQL(
                            "DELETE FROM $TABLE_CART_NAME WHERE $VID = ? AND $PID = ?",
                            arrayOf(
                                cursor.getString(cursor.getColumnIndexOrThrow(VID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(PID))
                            )
                        )
                    } else ids.add(cursor.getString(cursor.getColumnIndexOrThrow(VID)))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return ids
        }

    fun cartData(): MutableMap<String, String> {
        val ids = HashMap<String, String>()
        val selectQuery = "SELECT *  FROM $TABLE_CART_NAME"
        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val count = cursor.getString(cursor.getColumnIndex(QTY))
                if (count == "0") {
                    db.execSQL(
                        "DELETE FROM $TABLE_CART_NAME WHERE $VID = ? AND $PID = ?",
                        arrayOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(VID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(PID))
                        )
                    )
                } else ids[cursor.getString(cursor.getColumnIndexOrThrow(VID))] =
                    cursor.getString(cursor.getColumnIndexOrThrow(QTY))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return ids
    }

    fun getTotalItemOfCart(activity: Activity): Int {
        val countQuery = "SELECT  * FROM $TABLE_CART_NAME"
        val db = this.readableDatabase
        val cursor = db.rawQuery(countQuery, null)
        val count = cursor.count
        cursor.close()
        Constant.TOTAL_CART_ITEM = count
        activity.invalidateOptionsMenu()
        return count
    }

    fun AddToCart(vid: String?, pid: String?, qty: String) {
        try {
            if (!CheckCartItemExist(vid, pid).equals("0", ignoreCase = true)) {
                UpdateCart(vid, pid, qty)
            } else {
                val db = this.writableDatabase
                val values = ContentValues()
                values.put(VID, vid)
                values.put(PID, pid)
                values.put(QTY, qty)
                db.insert(TABLE_CART_NAME, null, values)
                db.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun UpdateCart(vid: String?, pid: String?, qty: String) {
        val db = this.writableDatabase
        if (qty == "0") {
            RemoveFromCart(vid, pid)
        } else {
            val values = ContentValues()
            values.put(QTY, qty)
            db.update(TABLE_CART_NAME, values, "$VID = ? AND $PID = ?", arrayOf(vid, pid))
        }
        db.close()
    }

    fun RemoveFromCart(vid: String?, pid: String?) {
        val database = this.writableDatabase
        database.execSQL(
            "DELETE FROM $TABLE_CART_NAME WHERE $VID = ? AND $PID = ?",
            arrayOf(vid, pid)
        )
        database.close()
    }

    fun CheckCartItemExist(vid: String?, pid: String?): String {
        var count = "0"
        val db = this.writableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CART_NAME WHERE $VID = ? AND $PID = ?",
            arrayOf(vid, pid)
        )
        if (cursor.moveToFirst()) {
            count = cursor.getString(cursor.getColumnIndex(QTY))
            if (count == "0") {
                db.execSQL(
                    "DELETE FROM $TABLE_CART_NAME WHERE $VID = ? AND $PID = ?",
                    arrayOf(vid, pid)
                )
            }
        }
        cursor.close()
        db.close()
        return count
    }

    fun ClearCart() {
        val database = this.writableDatabase
        database.execSQL("DELETE FROM $TABLE_CART_NAME")
        database.close()
    }

    /*      SAVE FOR LATER TABLE OPERATION      */
    fun saveForLaterList() : ArrayList<String>{
            val ids = ArrayList<String>()
            val selectQuery = "SELECT *  FROM " + TABLE_SAVE_FOR_LATER_NAME
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                do {
                    val count = cursor.getString(cursor.getColumnIndex(QTY))
                    if (count == "0") {
                        db.execSQL(
                            "DELETE FROM " + TABLE_SAVE_FOR_LATER_NAME + " WHERE " + VID + " = ? AND " + PID + " = ?",
                            arrayOf(
                                cursor.getString(cursor.getColumnIndexOrThrow(VID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(PID))
                            )
                        )
                    } else ids.add(cursor.getString(cursor.getColumnIndexOrThrow(VID)))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return ids
        }

    fun AddToSaveForLater(vid: String?, pid: String?, qty: String?) {
        try {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(VID, vid)
            values.put(PID, pid)
            values.put(QTY, qty)
            db.insert(TABLE_SAVE_FOR_LATER_NAME, null, values)
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveForLaterData(): MutableMap<String, String>{
            val ids = HashMap<String, String>()
            val selectQuery = "SELECT *  FROM " + TABLE_SAVE_FOR_LATER_NAME
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                do {
                    val count = cursor.getString(cursor.getColumnIndex(QTY))
                    if (count == "0") {
                        db.execSQL(
                            "DELETE FROM " + TABLE_SAVE_FOR_LATER_NAME + " WHERE " + VID + " = ? AND " + PID + " = ?",
                            arrayOf(
                                cursor.getString(cursor.getColumnIndexOrThrow(VID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(PID))
                            )
                        )
                    } else ids[cursor.getString(cursor.getColumnIndexOrThrow(VID))] =
                        cursor.getString(cursor.getColumnIndexOrThrow(QTY))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return ids
        }

    fun MoveToCartOrSaveForLater(vid: String?, pid: String?, from: String, activity: Activity) {
        if (from == "cart") {
            AddToSaveForLater(vid, pid, CheckCartItemExist(vid, pid))
            RemoveFromCart(vid, pid)
        } else {
            AddToCart(vid, pid, CheckSaveForLaterItemExist(vid, pid))
            RemoveFromSaveForLater(vid, pid)
        }
        getTotalItemOfCart(activity)
    }

    fun RemoveFromSaveForLater(vid: String?, pid: String?) {
        val database = this.writableDatabase
        database.execSQL(
            "DELETE FROM " + TABLE_SAVE_FOR_LATER_NAME + " WHERE " + VID + " = ? AND " + PID + " = ?",
            arrayOf(vid, pid)
        )
        database.close()
    }

    fun CheckSaveForLaterItemExist(vid: String?, pid: String?): String {
        var count = "0"
        val db = this.writableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_SAVE_FOR_LATER_NAME + " WHERE " + VID + " = ? AND " + PID + " = ?",
            arrayOf(vid, pid)
        )
        if (cursor.moveToFirst()) {
            count = cursor.getString(cursor.getColumnIndex(QTY))
            if (count == "0") {
                db.execSQL(
                    "DELETE FROM " + TABLE_SAVE_FOR_LATER_NAME + " WHERE " + VID + " = ? AND " + PID + " = ?",
                    arrayOf(vid, pid)
                )
            }
        }
        cursor.close()
        db.close()
        return count
    }

    fun ClearSaveForLater() {
        val database = this.writableDatabase
        database.execSQL("DELETE FROM " + TABLE_SAVE_FOR_LATER_NAME)
        database.close()
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "ekart.db"
        const val TABLE_FAVORITE_NAME = "tblfavourite"
        const val TABLE_SAVE_FOR_LATER_NAME = "tblsaveforlater"
        const val KEY_ID = "pid"
    }
}