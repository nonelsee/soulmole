package com.example.soulmole.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.soulmole.model.GameSession
import com.example.soulmole.model.Player

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game_database.db"
        private const val DATABASE_VERSION = 7

        const val TABLE_PLAYER = "Player"
        const val COLUMN_PLAYER_ID = "player_id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_SCORE = "score"
        const val COLUMN_HEALTH = "health"
        const val COLUMN_DEPTH = "depth"

        const val TABLE_GAME_SESSIONS = "game_sessions"
        const val COLUMN_SESSION_ID = "sessionId"
        const val COLUMN_FINAL_SCORE = "finalScore"
        const val COLUMN_DISTANCE_DUG = "distanceDug"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createPlayerTable = """
            CREATE TABLE $TABLE_PLAYER (
                $COLUMN_PLAYER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_SCORE INTEGER DEFAULT 0,
                $COLUMN_HEALTH INTEGER DEFAULT 100,
                $COLUMN_DEPTH INTEGER DEFAULT 0
            )
        """
        db.execSQL(createPlayerTable)

        val createGameSessionsTable = """
            CREATE TABLE $TABLE_GAME_SESSIONS (
                $COLUMN_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLAYER_ID INTEGER,
                $COLUMN_FINAL_SCORE INTEGER,
                $COLUMN_DISTANCE_DUG INTEGER,
                FOREIGN KEY($COLUMN_PLAYER_ID) REFERENCES $TABLE_PLAYER($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createGameSessionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GAME_SESSIONS")
        onCreate(db)
    }

    fun deleteAllPlayers(): Int {
        val db = writableDatabase
        return db.delete(TABLE_PLAYER, null, null)
    }

    fun insertPlayer(player: Player): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, player.username)
            put(COLUMN_SCORE, player.score)
            put(COLUMN_HEALTH, player.health)
            put(COLUMN_DEPTH, player.depth)
        }
        val id = db.insert(TABLE_PLAYER, null, values)
        db.close()
        return id
    }

    fun getPlayerByUsername(username: String): Player? {
        val db = readableDatabase
        var player: Player? = null
        val cursor = db.query(
            TABLE_PLAYER,
            null,
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            val playerId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID))
            val score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE))
            val health = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HEALTH))
            val depth = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DEPTH))
            player = Player(playerId, username, score, health, depth)
        }
        cursor.close()
        Log.d("DatabaseHelper", "getPlayerByUsername: ${player != null}")
        return player
    }

    fun getTop5PlayersWithScores(): List<Pair<String, Int>> {
        val topPlayers = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase

        // Truy vấn top 5 người chơi dựa trên finalScore và player_id
        val query = """
        SELECT P.$COLUMN_USERNAME, GS.$COLUMN_FINAL_SCORE
        FROM $TABLE_GAME_SESSIONS GS
        INNER JOIN $TABLE_PLAYER P ON GS.$COLUMN_PLAYER_ID = P.$COLUMN_PLAYER_ID
        ORDER BY GS.$COLUMN_FINAL_SCORE DESC, P.$COLUMN_PLAYER_ID ASC
        LIMIT 5
    """
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
                val finalScore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FINAL_SCORE))
                topPlayers.add(Pair(username, finalScore))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return topPlayers
    }

}
