package com.cs408.studybuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Manages loading and saving chat message history.
 * Created by Aaron on 3/10/2015.
 */
public class ChatMessageHistory {
    private static final String TAG = "ChatMessageHistory";

    private static final String DATABASE_NAME = "StudyBuddy"; // SQLite database name
    private static final int DATABASE_VERSION = 2;            // Database version number
    private static final String TABLE_NAME = "messages";      // Message history table name

    private String userId;
    private ChatMessageHistoryOpenHelper openHelper;
    private SQLiteDatabase db;
    private ArrayList<ChatMessage> savedMessages = new ArrayList<>();

    /**
     * Loads the chat history.
     * @param context The context to use.
     * @param userId The ID of the user to load chat history for.
     * @return The loaded chat history.
     */
    public static ChatMessageHistory load(Context context, String userId) {
        return new ChatMessageHistory(new ChatMessageHistoryOpenHelper(context), userId);
    }

    /**
     * Gets the messages in the chat history, in no particular order.
     * @return The messages in the chat history.
     */
    public synchronized List<ChatMessage> getMessages() {
        return savedMessages;
    }

    /**
     * Saves a message to the chat history.
     * @param message The message to save.
     * @return True if the message was saved successfully.
     */
    public synchronized boolean saveMessage(ChatMessage message) {
        // Insert the message into the table
        SQLiteDatabase db = openHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", message.getId());
        values.put("user", userId);
        values.put("sender", message.getSender());
        values.put("message", message.getBody());
        values.put("date", message.getDate().getTime());
        values.put("direction", message.getDirection().ordinal());
        if (db.insert(TABLE_NAME, null, values) == -1) {
            return false; // Failed to insert the message - it might already exist
        }
        savedMessages.add(message);
        Log.d(TAG, "Saved message " + message.getId() + " to chat history for user " + userId);
        return true;
    }

    /**
     * Closes the message history database.
     */
    public synchronized void close() {
        openHelper.close();
    }

    /**
     * Constructs a ChatMessageHistory object.
     * @param openHelper The open helper to use.
     * @param userId The ID of the user to load chat history for.
     */
    private ChatMessageHistory(ChatMessageHistoryOpenHelper openHelper, String userId) {
        this.openHelper = openHelper;
        this.userId = userId;
        read();
    }

    /**
     * Reads chat history from the database.
     */
    private void read() {
        // Open the database and construct a query to look up the user's saved messages
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME,              // Table
                new String[] {           // Columns
                        "id",       // 0
                        "sender",   // 1
                        "message",  // 2
                        "date",     // 3
                        "direction" // 4
                },
                "user=?",                // Selection
                new String[] { userId }, // Selection args
                null,                    // Group by
                null,                    // Having
                null                     // Order by
        );

        // Get each result and make a ChatMessage object from it
        while (c.moveToNext()) {
            String id = c.getString(0);
            String sender = c.getString(1);
            String body = c.getString(2);
            Date date = new Date(c.getLong(3));
            ChatMessage.Direction direction = ChatMessage.Direction.values()[c.getInt(4)];
            savedMessages.add(new ChatMessage(id, sender, body, direction, date));
        }
    }

    private static class ChatMessageHistoryOpenHelper extends SQLiteOpenHelper {
        private static final String TAG = "ChatMessageHistoryOpenHelper";

        public ChatMessageHistoryOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db, TABLE_NAME);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for (int upgradeTo = oldVersion + 1; upgradeTo <= newVersion; upgradeTo++) {
                Log.d(TAG, "Upgrading message database from version " + (upgradeTo - 1) + " to version " + upgradeTo);
                switch (upgradeTo) {
                    case 2:
                        // Version 2 makes the user part of the primary key,
                        // so the table has to be duplicated and then renamed
                        String tempTableName = TABLE_NAME + "_temp";
                        createTable(db, tempTableName);
                        db.execSQL("INSERT INTO " + tempTableName + " SELECT * FROM " + TABLE_NAME);
                        db.execSQL("DROP TABLE " + TABLE_NAME);
                        db.execSQL("ALTER TABLE " + tempTableName + " RENAME TO " + TABLE_NAME);
                        break;
                }
                Log.d(TAG, "Upgraded message database to version " + upgradeTo);
            }
        }

        private void createTable(SQLiteDatabase db, String name) {
            db.execSQL("CREATE TABLE " + name + " (" +
                            "id TEXT NOT NULL," +
                            "user TEXT NOT NULL," +
                            "sender TEXT NOT NULL," +
                            "message TEXT NOT NULL," +
                            "date INTEGER NOT NULL," +
                            "direction INTEGER NOT NULL," +
                            "PRIMARY KEY (id, user)" +
                    ");");
        }
    }
}
