package com.example.hmusicv2

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "HMusicOffline.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "OfflineSongs"

        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_ARTIST = "artist"
        const val COL_COVER = "cover"
        const val COL_LOCAL_PATH = "local_path"
    }
    // Hàm này chạy đầu tiên khi Database được gọi
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // TẮT CHẾ ĐỘ GHI NHÁP (WAL) -> ÉP GHI THẲNG VÀO FILE .DB CHÍNH
        db.disableWriteAheadLogging()
    }
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COL_ID TEXT PRIMARY KEY,"
                + "$COL_TITLE TEXT,"
                + "$COL_ARTIST TEXT,"
                + "$COL_COVER TEXT,"
                + "$COL_LOCAL_PATH TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertOfflineSong(song: Song, localPath: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_ID, song.id)
        contentValues.put(COL_TITLE, song.title)
        contentValues.put(COL_ARTIST, song.artist)
        contentValues.put(COL_COVER, song.cover)
        contentValues.put(COL_LOCAL_PATH, localPath)

        // Dùng replace để nếu trùng ID thì nó ghi đè lên luôn, không báo lỗi
        val result = db.insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)

        // QUAN TRỌNG: Phải đóng db để nó xác nhận ghi xuống file vật lý
        //db.close()
        return result != -1L
    }

    fun getAllOfflineSongs(): ArrayList<Song> {
        val songList = ArrayList<Song>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTIST))
                val cover = cursor.getString(cursor.getColumnIndexOrThrow(COL_COVER))
                val localPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCAL_PATH))

                // Tráo link mạng bằng đường dẫn local
                val song = Song(id = id, title = title, artist = artist, cover = cover, audio = localPath)
                songList.add(song)
            } while (cursor.moveToNext())
        }
        cursor.close()
     //   db.close()
        return songList
    }

    fun getLocalPath(songId: String): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_LOCAL_PATH FROM $TABLE_NAME WHERE $COL_ID = ?", arrayOf(songId))
        var path: String? = null
        if (cursor.moveToFirst()) {
            path = cursor.getString(0)
        }
        cursor.close()
      //  db.close()
        return path
    }
    // Hàm cập nhật tên bài hát trong SQLite
    fun updateOfflineSongTitle(songId: String, newTitle: String): Boolean {
        val db = this.writableDatabase
        val contentValues = android.content.ContentValues()
        contentValues.put(COL_TITLE, newTitle)

        val result = db.update(TABLE_NAME, contentValues, "$COL_ID = ?", arrayOf(songId))
        // Nhớ bài học cũ: Không dùng db.close() ở đây nhé!
        return result > 0
    }
    fun deleteOfflineSong(songId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(songId))
        // KHÔNG dùng db.close() ở đây để App Inspection vẫn soi được
    }
}