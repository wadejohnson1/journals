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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * A contract between the journal content provider and its clients.
 * <p/>
 * Defines the information that a client needs to access the provider as one or more data tables.
 */
public final class JournalContract {

    /**
     * Symbolic name of the entire content provider.
     */
    public static final String AUTHORITY = "com.example.journals.provider";
    /**
     * A {@code content://} style URI to the content provider authority.
     */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Private constructor.
     */
    private JournalContract() {
    }

    /**
     * Contract elements specific to the accounts table.
     */
    public static final class Accounts implements BaseColumns {

        // *****URI definitions.***** //

        /**
         * Base path for the accounts directory.
         */
        public static final String CONTENT_DIRECTORY = "accounts";
        /**
         * The {@code content://} style URI for the account directory.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, CONTENT_DIRECTORY);

        // *****MIME types.***** //

        /**
         * The MIME type of a journal directory.
         */
        public static final String CONTENT_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd." + AUTHORITY + ".account";
        /**
         * The MIME type of a journal item.
         */
        public static final String CONTENT_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd." + AUTHORITY + ".account";

        // *****Columns for accessing provider data.***** //

        /**
         * Unique ID for a journal in the provider.
         * <p/>
         * The ID is created automatically when a new record is inserted. It is guaranteed to be
         * unique and stable for all the records currently in the provider, but ID's may be recycled
         * if records in the provider are deleted.
         * <p/>
         * Note this field has the same value as {@link #_ID}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_ID = _ID;
        /**
         * Count of all rows returned from a query.
         * <p/>
         * This column can be used as a quick reference to determine the total number of rows in the
         * cursor returned by a query. The value in this column will be the same for all rows.
         * <p/>
         * Note this field has the same value as {@link #_COUNT}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: int
         */
        public static final String COLUMN_COUNT = _COUNT;
        /**
         * The server ID of the user this account is associated with.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_USER_ID = "user_id";
        /**
         * The name of the account.
         * <p/>
         * Must be unique across all rows.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_NAME = "name";
        /**
         * Alias for the account.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_ALIAS = "alias";
        /**
         * Gender of the user the account is associated with.
         * <p/>
         * Type: int (0 = male, 1 = female, 2 = other)
         */
        public static final String COLUMN_GENDER = "gender";
        /**
         * URI of the account avatar.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_AVATAR_URI = "avatar_uri";
        /**
         * Creation date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_CREATED = "created";
        /**
         * Last modified date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_MODIFIED = "modified";

    }

    /**
     * Contract elements specific to the journals table.
     */
    public static final class Journals implements BaseColumns {

        // *****URI definitions.***** //

        /**
         * Base path for the journal directory.
         */
        public static final String CONTENT_DIRECTORY = "journals";
        /**
         * The {@code content://} style URI for the journal directory.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, CONTENT_DIRECTORY);

        // *****MIME types.***** //

        /**
         * The MIME type of a journal directory.
         */
        public static final String CONTENT_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd." + AUTHORITY + ".journal";
        /**
         * The MIME type of a journal item.
         */
        public static final String CONTENT_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd." + AUTHORITY + ".journal";

        // *****Columns for accessing provider data.***** //

        /**
         * Unique ID for a journal in the provider.
         * <p/>
         * The ID is created automatically when a new record is inserted. It is guaranteed to be
         * unique and stable for all the records currently in the provider, but ID's may be recycled
         * if records in the provider are deleted.
         * <p/>
         * Note this field has the same value as {@link #_ID}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_ID = _ID;
        /**
         * Count of all rows returned from a query.
         * <p/>
         * This column can be used as a quick reference to determine the total number of rows in the
         * cursor returned by a query. The value in this column will be the same for all rows.
         * <p/>
         * Note this field has the same value as {@link #_COUNT}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: int
         */
        public static final String COLUMN_COUNT = _COUNT;
        /**
         * Name of the account managing this journal.
         * <p/>
         * Cannot be {@code null}.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_ACCOUNT_NAME = "account_name";
        /**
         * Title of the journal.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_TITLE = "title";
        /**
         * A description of the journal content.
         * <p/>
         * Cannot be {@code null}.
         * <p/>
         * Type: int; 1 = public (default), 2 = private
         */
        public static final String COLUMN_MAKE_PUBLIC = "make_public";
        /**
         * The ID for this journal on the server.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_SERVER_ID = "server_id";
        /**
         * Set whether this journal should be deleted during the next network sync.
         * <p/>
         * Cannot be {@code null}.
         * <p/>
         * Type: int; 1 = false (default), 2 = true
         */
        public static final String COLUMN_IS_DELETED = "is_deleted";
        /**
         * Creation date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_CREATED = "created";
        /**
         * Last modified date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_MODIFIED = "modified";

        /**
         * Private constructor.
         */
        private Journals() {
        }

    }

    /**
     * Contract elements specific to activities.
     */
    public static final class Activities implements BaseColumns {

        // *****URI definitions.***** //

        /**
         * Base path for the activity directory.
         */
        public static final String CONTENT_DIRECTORY = "activities";
        /**
         * The {@code content://} style URI for the activity directory.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, CONTENT_DIRECTORY);
        /**
         * The MIME type of a directory of activities.
         */
        public static final String CONTENT_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd." + AUTHORITY + ".activity";

        // *****MIME types.***** //
        /**
         * The MIME type of a single activity.
         */
        public static final String CONTENT_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd." + AUTHORITY + ".activity";

        // *****Columns for accessing provider data.***** //

        /**
         * Unique ID for an activity in the provider.
         * <p/>
         * The ID is created automatically when a new record is inserted. It is guaranteed to be
         * unique and stable for all the records currently in the provider, but ID's may be recycled
         * if records in the provider are deleted.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Note this field has the same value as {@link #_ID}.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_ID = _ID;
        /**
         * Count of all rows returned from a query.
         * <p/>
         * This column can be used as a quick reference to determine the total number of rows in the
         * cursor returned by a query. The value in this column will be the same for all rows.
         * <p/>
         * Note this field has the same value as {@link #_COUNT}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: int
         */
        public static final String COLUMN_COUNT = _COUNT;
        /**
         * The ID of the journal this activity is associated with.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_JOURNAL_ID = "journal_id";
        /**
         * The title of this activity.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_TITLE = "title";
        /**
         * A description of this activity.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_DESCRIPTION = "description";
        /**
         * The location of this activity.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_LOCATION = "location";
        /**
         * URI for the image associated with this activity.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_IMAGE_URI = "image_uri";
        /**
         * The latitude portion of a location associated with this activity.
         * <p/>
         * Type: double
         */
        public static final String COLUMN_LATITUDE = "latitude";
        /**
         * The longitude portion of a location associated with this activity.
         * <p/>
         * Type: double
         */
        public static final String COLUMN_LONGITUDE = "longitude";
        /**
         * The ID for this activity on the server.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_SERVER_ID = "server_id";
        /**
         * Set whether this activity should be deleted off the server during a sync update.  {@link
         * #COLUMN_SERVER_ID} must be set for this field to have any effect.
         * <p/>
         * Type: int; 0 = false (default), 1 = true
         */
        public static final String COLUMN_IS_DELETED = "is_deleted";
        /**
         * Creation date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_CREATED = "created";
        /**
         * Last modified date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_MODIFIED = "modified";

        /**
         * Private constructor.
         */
        private Activities() {
        }

        /**
         * Get a special-use content URI for an activity directory filtered on the specified journal
         * item ID.
         * <p/>
         * The journal item ID is automatically handled while performing operations on the content
         * provider when using this URI.
         *
         * @param journalId the journal item ID used to filter the activity directory
         */
        public static Uri getContentUriFromJournalId(int journalId) {
            return Uri.withAppendedPath(AUTHORITY_URI,
                    Journals.CONTENT_DIRECTORY + "/" + journalId + "/" + CONTENT_DIRECTORY);
        }

    }

    /**
     * Contract elements specific to contacts.
     */
    public static final class Contacts implements BaseColumns {

        // *****URI definitions.***** //

        /**
         * Base path for the contact directory.
         */
        public static final String CONTENT_DIRECTORY = "contacts";
        /**
         * The {@code content://} style URI for the contact directory.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, CONTENT_DIRECTORY);
        /**
         * The MIME type of a directory of contacts.
         */
        public static final String CONTENT_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd." + AUTHORITY + ".contact";

        // *****MIME types.***** //

        /**
         * The MIME type of a single contact.
         */
        public static final String CONTENT_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd." + AUTHORITY + ".contact";

        // *****Columns for accessing provider data.***** //

        /**
         * Unique ID for a contact in the provider.
         * <p/>
         * The ID is created automatically when a new record is inserted. It is guaranteed to be
         * unique and stable for all the records currently in the provider, but ID's may be recycled
         * if records in the provider are deleted.
         * <p/>
         * Note this field has the same value as {@link #_ID}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_ID = _ID;
        /**
         * Count of all rows returned from a query.
         * <p/>
         * This column can be used as a quick reference to determine the total number of rows in the
         * cursor returned by a query. The value in this column will be the same for all rows.
         * <p/>
         * Note this field has the same value as {@link #_COUNT}.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: int
         */
        public static final String COLUMN_COUNT = _COUNT;
        /**
         * The ID of the journal this contact is associated with.
         * <p/>
         * Cannot be set directly in {@code ContentProvider.insert()}, but is taken from the journal
         * ID segment of the activity URI. The journal ID can be manually updated later with a call
         * to {@code update()}. Referential integrity is enforced by the provider so use caution
         * when setting.
         * <p/>
         * Cannot be {@code null}.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_JOURNAL_ID = "journal_id";
        /**
         * The name of this contact.
         * <p/>
         * May be {@code null}.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_NAME = "name";
        /**
         * A description of this contact.
         * <p/>
         * May be {@code null}.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_DESCRIPTION = "description";
        /**
         * The email address of this contact.
         * <p/>
         * May be {@code null}.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_EMAIL = "email";
        /**
         * The phone number of this contact.
         * <p/>
         * May be {@code null}.
         * <p/>
         * Type: String
         */
        public static final String COLUMN_PHONE = "phone";
        /**
         * The ID for this contact on the server.
         * <p/>
         * Type: long
         */
        public static final String COLUMN_SERVER_ID = "server_id";
        /**
         * Set whether this contact should be deleted off the server during a sync update.  {@link
         * #COLUMN_SERVER_ID} must be set for this field to have any effect.
         * <p/>
         * Type: int; 0 = false (default), 1 = true
         */
        public static final String COLUMN_IS_DELETED = "is_deleted";
        /**
         * Creation date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_CREATED = "created";
        /**
         * Last modified date and time.
         * <p/>
         * This column is read-only. Attempts to manually set this column are ignored.
         * <p/>
         * Type: String (in the format "YYYY-MM-DD HH:MM:SS")
         */
        public static final String COLUMN_MODIFIED = "modified";

        /**
         * Private constructor
         */
        private Contacts() {
        }

        /**
         * Get a special-use content URI for a contact directory filtered on the specified journal
         * item ID.
         * <p/>
         * The journal item ID is automatically handled while performing operations on the content
         * provider when using this URI.
         *
         * @param journalId the journal item ID used to filter the activity directory
         */
        public static Uri getContentUriFromJournalId(int journalId) {
            return Uri.withAppendedPath(AUTHORITY_URI,
                    Journals.CONTENT_DIRECTORY + "/" + journalId + "/" + CONTENT_DIRECTORY);
        }

    }

    /**
     * Contract elements specific to the activity images.
     */
    public static final class Images {

        // *****URI definitions.***** //

        /**
         * Base path for the images URI.
         */
        public static final String CONTENT_DIRECTORY = "images";
        /**
         * The {@code content://} style URI for images.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, CONTENT_DIRECTORY);

        // *****MIME types.***** //

        /**
         * The MIME type of an image.
         */
        public static final String CONTENT_TYPE_IMAGE = "image/*";
        /**
         * The MIME type of a video.
         */
        public static final String CONTENT_TYPE_VIDEO = "video/*";

        /**
         * Private constructor.
         */
        private Images() {
        }

    }

}
