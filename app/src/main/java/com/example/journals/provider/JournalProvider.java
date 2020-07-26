/*
 * MIT License
 *
 * Copyright (c) 2020 Wade Johnson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.example.journals.provider;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.example.journals.provider.JournalContract.Accounts;
import com.example.journals.provider.JournalContract.Activities;
import com.example.journals.provider.JournalContract.Contacts;
import com.example.journals.provider.JournalContract.Images;
import com.example.journals.provider.JournalContract.Journals;
import com.example.journals.widget.CountCursorWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import static com.example.journals.provider.JournalContract.AUTHORITY;

/**
 * Content provider for journals and associated data.
 */
public class JournalProvider extends FileProvider {

    /**
     * Set whether debug messages should be logged.
     */
    private static final boolean DEBUG = true; // TODO set to false
    /**
     * Tag for displaying debug messages.
     */
    private static final String DEBUG_TAG = JournalProvider.class.getSimpleName();
    /**
     * The database version.
     */
    private static final int DATABASE_VERSION = 1;
    /**
     * URI pattern matching ID.
     * <p/>
     * Used with {@link #sUriMatcher}.
     */
    private static final int ACCOUNT_DIRECTORY = 1, ACCOUNT = 2, JOURNAL_DIRECTORY = 3, JOURNAL = 4,
            ACTIVITY_DIRECTORY = 5, ACTIVITY_DIRECTORY_FROM_JOURNAL = 6, ACTIVITY = 7,
            CONTACT_DIRECTORY = 8, CONTACT_DIRECTORY_FROM_JOURNAL = 9, CONTACT = 10,
            IMAGE = 11;
    /**
     * The name of the SQLite database this provider uses as its underlying data store.
     */
    private static final String DATABASE_NAME = "Journals.db";
    /**
     * A URI matcher for content offered by this provider.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, Accounts.CONTENT_DIRECTORY, ACCOUNT_DIRECTORY);
        sUriMatcher.addURI(AUTHORITY, Accounts.CONTENT_DIRECTORY + "/#", ACCOUNT);
        sUriMatcher.addURI(AUTHORITY, Journals.CONTENT_DIRECTORY, JOURNAL_DIRECTORY);
        sUriMatcher.addURI(AUTHORITY, Journals.CONTENT_DIRECTORY + "/#", JOURNAL);
        sUriMatcher.addURI(AUTHORITY, Activities.CONTENT_DIRECTORY, ACTIVITY_DIRECTORY);
        sUriMatcher.addURI(AUTHORITY,
                Journals.CONTENT_DIRECTORY + "/#/" + Activities.CONTENT_DIRECTORY,
                ACTIVITY_DIRECTORY_FROM_JOURNAL);
        sUriMatcher.addURI(AUTHORITY, Activities.CONTENT_DIRECTORY + "/#", ACTIVITY);
        sUriMatcher.addURI(AUTHORITY,
                Journals.CONTENT_DIRECTORY + "/#/" + Activities.CONTENT_DIRECTORY + "/#", ACTIVITY);
        sUriMatcher.addURI(AUTHORITY, Contacts.CONTENT_DIRECTORY, CONTACT_DIRECTORY);
        sUriMatcher.addURI(AUTHORITY,
                Journals.CONTENT_DIRECTORY + "/#/" + Contacts.CONTENT_DIRECTORY,
                CONTACT_DIRECTORY_FROM_JOURNAL);
        sUriMatcher.addURI(AUTHORITY, Contacts.CONTENT_DIRECTORY + "/#", CONTACT);
        sUriMatcher.addURI(AUTHORITY,
                Journals.CONTENT_DIRECTORY + "/#/" + Contacts.CONTENT_DIRECTORY + "/#", CONTACT);
        sUriMatcher.addURI(AUTHORITY, Images.CONTENT_DIRECTORY + "/*", IMAGE);
    }

    /**
     * A helper class to manage database creation and version management.
     */
    private DatabaseHelper mDatabaseHelper;

    @Override
    @NonNull
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            final ContentProviderResult[] results = super.applyBatch(operations);
            database.setTransactionSuccessful();
            return results;
        } finally {
            database.endTransaction();
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // The incoming URI is for an image.
        final int uriMatch = sUriMatcher.match(uri);
        if (uriMatch == IMAGE) {
            // Use file provider to delete a file.
            return super.delete(uri, selection, selectionArgs);
        } else {
            final String table;
            // Set up the "where" clause.
            final StringBuilder where = new StringBuilder();
            if (selection != null) {
                where.append(selection);
            }
            switch (uriMatch) {
                case ACCOUNT_DIRECTORY:
                    table = DatabaseHelper.AccountsTable.TABLE_NAME;
                    break;
                case ACCOUNT:
                    table = DatabaseHelper.AccountsTable.TABLE_NAME;
                    // Append item ID to the "where" clause.
                    if (selection != null) {
                        where.append(" AND ");
                    }
                    where.append(Accounts.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                    break;
                case JOURNAL_DIRECTORY:
                    table = DatabaseHelper.JournalsTable.TABLE_NAME;
                    break;
                case JOURNAL:
                    table = DatabaseHelper.JournalsTable.TABLE_NAME;
                    // Append item ID to the "where" clause.
                    if (selection != null) {
                        where.append(" AND ");
                    }
                    where.append(Journals.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                    break;
                case ACTIVITY_DIRECTORY:
                case ACTIVITY_DIRECTORY_FROM_JOURNAL:
                    table = DatabaseHelper.ActivitiesTable.TABLE_NAME;
                    // Append journal ID to the "where" clause.
                    if (uriMatch == ACTIVITY_DIRECTORY_FROM_JOURNAL) {
                        if (selection != null) {
                            where.append(" AND ");
                        }
                        where.append(Activities.COLUMN_JOURNAL_ID).append("=")
                                .append(uri.getPathSegments().get(1));
                    }
                    break;
                case ACTIVITY:
                    table = DatabaseHelper.ActivitiesTable.TABLE_NAME;
                    // Append item ID to the "where" clause.
                    if (selection != null) {
                        where.append(" AND ");
                    }
                    where.append(Activities.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                    break;
                case CONTACT_DIRECTORY:
                case CONTACT_DIRECTORY_FROM_JOURNAL:
                    table = DatabaseHelper.ContactsTable.TABLE_NAME;
                    // Append journal ID to the "where" clause.
                    if (uriMatch == CONTACT_DIRECTORY_FROM_JOURNAL) {
                        if (selection != null) {
                            where.append(" AND ");
                        }
                        where.append(Contacts.COLUMN_JOURNAL_ID).append("=")
                                .append(uri.getPathSegments().get(1));
                    }
                    break;
                case CONTACT:
                    table = DatabaseHelper.ContactsTable.TABLE_NAME;
                    // Append item ID to the "where" clause.
                    if (selection != null) {
                        where.append(" AND ");
                    }
                    where.append(Contacts.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
            // Delete all images associated with entries to be deleted.
            deleteFilesInternal(uri, selection, selectionArgs);
            // Open the database in "write" mode and delete data.
            final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
            final int rowsDeleted = database.delete(table, where.toString(), selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return rowsDeleted;
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ACCOUNT_DIRECTORY:
                return Accounts.CONTENT_TYPE_DIR;
            case ACCOUNT:
                return Accounts.CONTENT_TYPE_ITEM;
            case JOURNAL_DIRECTORY:
                return Journals.CONTENT_TYPE_DIR;
            case JOURNAL:
                return Journals.CONTENT_TYPE_ITEM;
            case ACTIVITY_DIRECTORY:
            case ACTIVITY_DIRECTORY_FROM_JOURNAL:
                return Activities.CONTENT_TYPE_DIR;
            case ACTIVITY:
                return Activities.CONTENT_TYPE_ITEM;
            case CONTACT_DIRECTORY:
            case CONTACT_DIRECTORY_FROM_JOURNAL:
                return Contacts.CONTENT_TYPE_DIR;
            case CONTACT:
                return JournalContract.Contacts.CONTENT_TYPE_ITEM;
            case IMAGE:
                return super.getType(uri);
            default:
                return null;
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Create a copy of content values, as they may be modified.
        final ContentValues copiedValues = new ContentValues(values);
        final String table;
        final int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ACCOUNT_DIRECTORY:
                table = DatabaseHelper.AccountsTable.TABLE_NAME;
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.AccountsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case JOURNAL_DIRECTORY:
                table = DatabaseHelper.JournalsTable.TABLE_NAME;
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.JournalsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case ACTIVITY_DIRECTORY:
            case ACTIVITY_DIRECTORY_FROM_JOURNAL:
                table = DatabaseHelper.ActivitiesTable.TABLE_NAME;
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.ActivitiesTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                // Add the journal ID from the URI.
                if (uriMatch == ACTIVITY_DIRECTORY_FROM_JOURNAL) {
                    final long journalId = Long.valueOf(uri.getPathSegments().get(1));
                    copiedValues.put(Activities.COLUMN_JOURNAL_ID, journalId);
                }
                break;
            case CONTACT_DIRECTORY:
            case CONTACT_DIRECTORY_FROM_JOURNAL:
                table = DatabaseHelper.ContactsTable.TABLE_NAME;
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.ContactsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                // Add the journal ID from the URI.
                if (uriMatch == CONTACT_DIRECTORY_FROM_JOURNAL) {
                    final long journalId = Long.valueOf(uri.getPathSegments().get(1));
                    copiedValues.put(Contacts.COLUMN_JOURNAL_ID, journalId);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        // Open the database in "write" mode and insert data.
        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        final long rowId = database.insert(table, null, copiedValues);
        // The insert was successful.
        if (rowId > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, rowId);
        }
        // The insert was not successful.
        else {
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Incoming URI is for a file.
        final int uriMatch = sUriMatcher.match(uri);
        if (uriMatch == IMAGE) {
            return super.query(uri, projection, selection, selectionArgs, sortOrder);
        } else {
            // Construct a new query builder.
            final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            // Adjust query based on URI pattern matching.
            switch (uriMatch) {
                case ACCOUNT_DIRECTORY:
                    builder.setTables(DatabaseHelper.AccountsTable.TABLE_NAME);
                    break;
                case ACCOUNT:
                    builder.setTables(DatabaseHelper.AccountsTable.TABLE_NAME);
                    builder.appendWhere(Accounts.COLUMN_ID + "=" + uri.getLastPathSegment());
                    break;
                case JOURNAL_DIRECTORY:
                    builder.setTables(DatabaseHelper.JournalsTable.TABLE_NAME);
                    break;
                case JOURNAL:
                    builder.setTables(DatabaseHelper.JournalsTable.TABLE_NAME);
                    builder.appendWhere(Journals.COLUMN_ID + "=" + uri.getLastPathSegment());
                    break;
                case ACTIVITY_DIRECTORY:
                    builder.setTables(DatabaseHelper.ActivitiesTable.TABLE_NAME);
                    break;
                case ACTIVITY_DIRECTORY_FROM_JOURNAL:
                    builder.setTables(DatabaseHelper.ActivitiesTable.TABLE_NAME);
                    builder.appendWhere(
                            Activities.COLUMN_JOURNAL_ID + "=" + uri.getPathSegments().get(1));
                    break;
                case ACTIVITY:
                    builder.setTables(DatabaseHelper.ActivitiesTable.TABLE_NAME);
                    builder.appendWhere(Activities.COLUMN_ID + "=" + uri.getLastPathSegment());
                    break;
                case CONTACT_DIRECTORY:
                    builder.setTables(DatabaseHelper.ContactsTable.TABLE_NAME);
                    break;
                case CONTACT_DIRECTORY_FROM_JOURNAL:
                    builder.setTables(DatabaseHelper.ContactsTable.TABLE_NAME);
                    builder.appendWhere(
                            Contacts.COLUMN_JOURNAL_ID + "=" + uri.getPathSegments().get(1));
                    break;
                case CONTACT:
                    builder.setTables(DatabaseHelper.ContactsTable.TABLE_NAME);
                    builder.appendWhere(Contacts.COLUMN_ID + "=" + uri.getLastPathSegment());
                    break;
                default:
                    // URI doesn't match any of the known patterns.
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
            // Handle possible inclusion of BaseColumns._COUNT column in query.
            String[] countProjection = projection;
            int countIndex = -1;
            for (int i = 0; i < projection.length; i++) {
                if (BaseColumns._COUNT.equals(projection[i])) {
                    final List<String> list = new ArrayList<>(projection.length);
                    list.addAll(Arrays.asList(projection));
                    list.remove(i);
                    countProjection = list.toArray(new String[projection.length - 1]);
                    countIndex = i;
                    break;
                }
            }
            // Open the database in "read" mode (no writes need to be done).
            final SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
            final Cursor queryCursor =
                    builder.query(database, countProjection, selection, selectionArgs, null, null,
                            sortOrder);
            // Handle possible inclusion of BaseColumns._COUNT column in query.
            Cursor cursor = queryCursor;
            if (countIndex != -1) {
                cursor = new CountCursorWrapper(queryCursor, countIndex);
            }
            // Register cursor to listen for content changes.
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Create a copy of content values, as they may be modified.
        final ContentValues copiedValues = new ContentValues(values);
        final String table;
        // Set up the "where" clause.
        final StringBuilder where = new StringBuilder();
        if (selection != null) {
            where.append(selection);
        }
        final int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ACCOUNT_DIRECTORY:
                table = DatabaseHelper.AccountsTable.TABLE_NAME;
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.AccountsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case ACCOUNT:
                table = DatabaseHelper.AccountsTable.TABLE_NAME;
                // Append item ID to the "where" clause.
                if (selection != null) {
                    where.append(" AND ");
                }
                where.append(Accounts.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.JournalsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case JOURNAL_DIRECTORY:
                table = DatabaseHelper.JournalsTable.TABLE_NAME;
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.JournalsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case JOURNAL:
                table = DatabaseHelper.JournalsTable.TABLE_NAME;
                // Append item ID to the "where" clause.
                if (selection != null) {
                    where.append(" AND ");
                }
                where.append(Journals.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                // Prevent read-only columns from being modified, if present.
                for (String s : DatabaseHelper.JournalsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case ACTIVITY_DIRECTORY:
            case ACTIVITY_DIRECTORY_FROM_JOURNAL:
                table = DatabaseHelper.ActivitiesTable.TABLE_NAME;
                // Append journal ID to the "where" clause.
                if (uriMatch == ACTIVITY_DIRECTORY_FROM_JOURNAL) {
                    if (selection != null) {
                        where.append(" AND ");
                    }
                    where.append(Activities.COLUMN_JOURNAL_ID).append("=")
                            .append(uri.getPathSegments().get(1));
                }
                // Prevent read only columns from being modified, if present.
                for (String s : DatabaseHelper.ActivitiesTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case ACTIVITY:
                table = DatabaseHelper.ActivitiesTable.TABLE_NAME;
                // Append item ID to the "where" clause.
                if (selection != null) {
                    where.append(" AND ");
                }
                where.append(Activities.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                // Prevent read only columns from being modified, if present.
                for (String s : DatabaseHelper.ActivitiesTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case CONTACT_DIRECTORY:
            case CONTACT_DIRECTORY_FROM_JOURNAL:
                table = DatabaseHelper.ContactsTable.TABLE_NAME;
                // Append journal ID to the "where" clause.
                if (uriMatch == CONTACT_DIRECTORY_FROM_JOURNAL) {
                    if (selection != null) {
                        where.append(" AND ");
                    }
                    where.append(Contacts.COLUMN_JOURNAL_ID).append("=")
                            .append(uri.getPathSegments().get(1));
                }
                // Prevent read only columns from being modified, if present.
                for (String s : DatabaseHelper.ContactsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            case CONTACT:
                table = DatabaseHelper.ContactsTable.TABLE_NAME;
                // Append item ID to the "where" clause.
                if (selection != null) {
                    where.append(" AND ");
                }
                where.append(Contacts.COLUMN_ID).append("=").append(uri.getLastPathSegment());
                // Prevent read only columns from being modified, if present.
                for (String s : DatabaseHelper.ContactsTable.READ_ONLY_COLUMNS) {
                    copiedValues.remove(s);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // Open the database in "write" mode and update data.
        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        final int rowsUpdated =
                database.update(table, copiedValues, where.toString(), selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    /**
     * Internally delete files from the content provider.
     * <p/>
     * Called when journals or journal entries are deleted.
     *
     * @param uri           the URI whose associated images should be deleted, can be of type {@link
     *                      Journals#CONTENT_TYPE_DIR}, {@link Journals#CONTENT_TYPE_ITEM}, {@link
     *                      Activities#CONTENT_TYPE_DIR} or {@link Activities#CONTENT_TYPE_ITEM}
     * @param selection     an optional restriction to apply to rows when deleting
     * @param selectionArgs you may include ?s in selection, which will be replaced by the values,
     *                      in the order that they appear in the selection
     * @return the number of images deleted
     */
    private int deleteFilesInternal(Uri uri, String selection, String[] selectionArgs) {
        final String[] projection;
        Cursor cursor;
        int noDeleted = 0;
        switch (sUriMatcher.match(uri)) {
            case ACCOUNT_DIRECTORY:
            case ACCOUNT:
                return noDeleted;
            case JOURNAL_DIRECTORY:
            case JOURNAL:
                // Get a cursor of all journals being deleted.
                projection = new String[]{Journals.COLUMN_ID};
                cursor = query(uri, projection, selection, selectionArgs, null);
                if (cursor.getCount() > 0) {
                    // Build list of journal URI's from the query.
                    final StringBuilder builder = new StringBuilder();
                    boolean isFirst = true;
                    while (cursor.moveToNext()) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            builder.append(", ");
                        }
                        builder.append(cursor.getLong(0));
                    }
                    // Delete the entries associated with the selected journals.
                    noDeleted = deleteFilesInternal(Activities.CONTENT_URI,
                            Activities.COLUMN_JOURNAL_ID + " IN (?)",
                            new String[]{builder.toString()});
                }
                cursor.close();
                return noDeleted;
            case ACTIVITY_DIRECTORY:
            case ACTIVITY_DIRECTORY_FROM_JOURNAL:
            case ACTIVITY:
                projection = new String[]{Activities.COLUMN_IMAGE_URI};
                cursor = query(uri, projection, selection, selectionArgs, null);
                noDeleted = 0;
                while (cursor.moveToNext()) {
                    final String imageUri = cursor.getString(0);
                    if (imageUri != null) {
                        noDeleted += delete(Uri.parse(imageUri), null, null);
                    }
                }
                cursor.close();
                return noDeleted;
            case CONTACT_DIRECTORY:
            case CONTACT_DIRECTORY_FROM_JOURNAL:
            case CONTACT:
                return 0;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    /**
     * A helper class to manage database creation and version management.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        /**
         * Create a helper object to create, open, and/or manage a database.
         *
         * @param context the context to use to open or create the database
         */
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            // Enforce foreign key constraints.
            db.setForeignKeyConstraintsEnabled(true);
            // Turn off recursive triggers.
            db.execSQL("PRAGMA recursive_triggers = OFF");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create each of the database tables.
            AccountsTable.onCreate(db);
            JournalsTable.onCreate(db);
            ActivitiesTable.onCreate(db);
            ContactsTable.onCreate(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            AccountsTable.onUpgrade(db, oldVersion, newVersion);
            JournalsTable.onUpgrade(db, oldVersion, newVersion);
            ActivitiesTable.onUpgrade(db, oldVersion, newVersion);
            ContactsTable.onUpgrade(db, oldVersion, newVersion);
        }

        /**
         * Class for account table creation and upgrade.
         */
        private static class AccountsTable {

            /**
             * Name of table.
             */
            public static final String TABLE_NAME = "Accounts";
            /**
             * List of read only columns.
             */
            public static final String[] READ_ONLY_COLUMNS =
                    new String[]{Accounts.COLUMN_ID, Accounts.COLUMN_COUNT, Accounts.COLUMN_CREATED,
                            Accounts.COLUMN_MODIFIED};

            /**
             * Create the table and associated triggers.
             */
            public static void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                        + Accounts.COLUMN_ID + " INTEGER PRIMARY KEY, "
                        + Accounts.COLUMN_USER_ID + " INTEGER NOT NULL, "
                        + Accounts.COLUMN_NAME + " TEXT NOT NULL UNIQUE, "
                        + Accounts.COLUMN_ALIAS + " TEXT, "
                        + Accounts.COLUMN_GENDER + " INTEGER NOT NULL "
                        + "CHECK (" + Accounts.COLUMN_GENDER + " IN(0,1,2)), "
                        + Accounts.COLUMN_AVATAR_URI + " TEXT, "
                        + Accounts.COLUMN_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + Accounts.COLUMN_MODIFIED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ");");
                // Create trigger to update modified date.
                db.execSQL("CREATE TRIGGER trig_" + TABLE_NAME + "_AfterUpdate"
                        + " AFTER UPDATE ON " + TABLE_NAME + " FOR EACH ROW BEGIN"
                        + " UPDATE " + TABLE_NAME
                        + " SET " + Accounts.COLUMN_MODIFIED + " = CURRENT_TIMESTAMP"
                        + " WHERE " + Accounts.COLUMN_ID + " = new." + Accounts.COLUMN_ID + ";"
                        + " END");
            }

            /**
             * Upgrade the table.
             */
            public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }

        }

        /**
         * Class for journal table creation and upgrade.
         */
        private static class JournalsTable {

            /**
             * Name of table.
             */
            public static final String TABLE_NAME = "Journals";
            /**
             * List of read only columns.
             */
            public static final String[] READ_ONLY_COLUMNS =
                    new String[]{Journals.COLUMN_ID, Journals.COLUMN_COUNT, Journals.COLUMN_CREATED,
                            Journals.COLUMN_MODIFIED};

            /**
             * Create the table and associated triggers.
             */
            public static void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                        + Journals.COLUMN_ID + " INTEGER PRIMARY KEY, "
                        + Journals.COLUMN_ACCOUNT_NAME + " TEXT NOT NULL, "
                        + Journals.COLUMN_TITLE + " TEXT COLLATE NOCASE, "
                        + Journals.COLUMN_MAKE_PUBLIC + " INTEGER NOT NULL DEFAULT 1 "
                        + "CHECK (" + Journals.COLUMN_MAKE_PUBLIC + " IN(1,2)), "
                        + Journals.COLUMN_SERVER_ID + " INTEGER, "
                        + Journals.COLUMN_IS_DELETED + " INTEGER NOT NULL DEFAULT 1 "
                        + "CHECK (" + Journals.COLUMN_IS_DELETED + " IN(1,2)), "
                        + Journals.COLUMN_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + Journals.COLUMN_MODIFIED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);");
                // Create trigger to update modified date.
                db.execSQL("CREATE TRIGGER trig_" + TABLE_NAME + "_AfterUpdate"
                        + " AFTER UPDATE ON " + TABLE_NAME + " FOR EACH ROW BEGIN"
                        + " UPDATE " + TABLE_NAME
                        + " SET " + Journals.COLUMN_MODIFIED + " = CURRENT_TIMESTAMP"
                        + " WHERE " + Journals.COLUMN_ID + " = new." + Journals.COLUMN_ID + ";"
                        + " END");
            }

            /**
             * Upgrade the table.
             */
            public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }

        }

        /**
         * Class for activities table creation and upgrade.
         */
        private static class ActivitiesTable {

            /**
             * Name of table.
             */
            public static final String TABLE_NAME = "Activities";
            /**
             * A string for producing an inner join on the journals table and the activities table.
             */
            public static final String QUERY_INNER_JOIN =
                    JournalsTable.TABLE_NAME + " INNER JOIN "
                            + ActivitiesTable.TABLE_NAME + " ON "
                            + JournalsTable.TABLE_NAME + "." + Journals.COLUMN_ID + "="
                            + ActivitiesTable.TABLE_NAME + "." + Activities.COLUMN_JOURNAL_ID;
            /**
             * List of read only columns.
             */
            public static final String[] READ_ONLY_COLUMNS =
                    new String[]{Activities.COLUMN_ID, Activities.COLUMN_COUNT,
                            Activities.COLUMN_CREATED, Activities.COLUMN_MODIFIED};

            /**
             * Create the table.
             */
            public static void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                        + Activities.COLUMN_ID + " INTEGER PRIMARY KEY, "
                        + Activities.COLUMN_JOURNAL_ID + " INTEGER, "
                        + Activities.COLUMN_TITLE + " TEXT COLLATE NOCASE, "
                        + Activities.COLUMN_DESCRIPTION + " TEXT, "
                        + Activities.COLUMN_LOCATION + " TEXT, "
                        + Activities.COLUMN_IMAGE_URI + " TEXT, "
                        + Activities.COLUMN_LATITUDE + " REAL, "
                        + Activities.COLUMN_LONGITUDE + " REAL, "
                        + Activities.COLUMN_SERVER_ID + " INTEGER, "
                        + Activities.COLUMN_IS_DELETED + " INTEGER NOT NULL DEFAULT 0 "
                        + "CHECK (" + Activities.COLUMN_IS_DELETED + " IN(0,1)), "
                        + Activities.COLUMN_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + Activities.COLUMN_MODIFIED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY(" + Activities.COLUMN_JOURNAL_ID + ") "
                        + "REFERENCES " + JournalsTable.TABLE_NAME
                        + "(" + Journals.COLUMN_ID + ") ON DELETE CASCADE);");
                // Create trigger to update modified date.
                db.execSQL("CREATE TRIGGER trig_" + TABLE_NAME + "_AfterUpdate"
                        + " AFTER UPDATE ON " + TABLE_NAME + " FOR EACH ROW BEGIN"
                        + " UPDATE " + TABLE_NAME
                        + " SET " + Activities.COLUMN_MODIFIED + " = CURRENT_TIMESTAMP"
                        + " WHERE " + Activities.COLUMN_ID + " = new." + Activities.COLUMN_ID + ";"
                        + " END");
            }

            /**
             * Upgrade the table.
             */
            public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }

        }

        /**
         * Class for contacts table creation and upgrade.
         */
        private static class ContactsTable {

            /**
             * Table name for contacts table.
             */
            public static final String TABLE_NAME = "Contacts";
            /**
             * List of read only columns.
             */
            public static final String[] READ_ONLY_COLUMNS =
                    new String[]{Contacts.COLUMN_ID, Contacts.COLUMN_COUNT, Contacts.COLUMN_CREATED,
                            Contacts.COLUMN_MODIFIED};

            /**
             * Create the contacts table.
             */
            public static void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                        + Contacts.COLUMN_ID + " INTEGER PRIMARY KEY, "
                        + Contacts.COLUMN_JOURNAL_ID + " INTEGER NOT NULL, "
                        + Contacts.COLUMN_NAME + " TEXT COLLATE NOCASE, "
                        + Contacts.COLUMN_DESCRIPTION + " TEXT, "
                        + Contacts.COLUMN_EMAIL + " TEXT, "
                        + Contacts.COLUMN_PHONE + " TEXT, "
                        + Contacts.COLUMN_SERVER_ID + " INTEGER, "
                        + Contacts.COLUMN_IS_DELETED + " INTEGER NOT NULL DEFAULT 0 "
                        + "CHECK (" + Contacts.COLUMN_IS_DELETED + " IN(0,1)), "
                        + Contacts.COLUMN_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + Contacts.COLUMN_MODIFIED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY(" + Contacts.COLUMN_JOURNAL_ID + ") "
                        + "REFERENCES " + JournalsTable.TABLE_NAME
                        + "(" + Journals.COLUMN_ID + ") ON DELETE CASCADE);");
                // Create trigger to update modified date.
                db.execSQL("CREATE TRIGGER trig_" + TABLE_NAME + "_AfterUpdate"
                        + " AFTER UPDATE ON " + TABLE_NAME + " FOR EACH ROW BEGIN"
                        + " UPDATE " + TABLE_NAME
                        + " SET " + Contacts.COLUMN_MODIFIED + " = CURRENT_TIMESTAMP"
                        + " WHERE " + Contacts.COLUMN_ID + " = new." + Contacts.COLUMN_ID + ";"
                        + " END");
            }

            /**
             * Upgrade the contacts table.
             */
            public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }

        }

    }

}