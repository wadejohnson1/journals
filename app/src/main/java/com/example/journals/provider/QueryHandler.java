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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * A simple asynchronous query handler that forwards query results to a listener.
 * <p/>
 * Clients can receive query results by implementing the {@link QueryListener} interface.
 */
public class QueryHandler extends AsyncQueryHandler {

    /**
     * A reference to a query listener.
     */
    private Reference<QueryListener> mQueryListener = null;

    /**
     * Construct a new asynchronous query handler.
     *
     * @param cr the content resolver to use with this query handler
     */
    public QueryHandler(ContentResolver cr) {
        super(cr);
    }

    /**
     * Get the query listener for query callbacks.
     *
     * @return the query listener for query callbacks
     */
    private QueryListener getQueryListener() {
        return (mQueryListener == null) ? null : mQueryListener.get();
    }

    /**
     * Set a listener to receive query events from asynchronous calls.
     * <p/>
     * Any previously set listeners will be replaced.
     *
     * @param listener the listener to receive query events
     */
    public void setQueryListener(QueryListener listener) {
        mQueryListener = new WeakReference<>(listener);
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        final QueryListener listener = getQueryListener();
        if (listener != null) {
            listener.onDeleteComplete(token, cookie, result);
        }
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        final QueryListener listener = getQueryListener();
        if (listener != null) {
            listener.onInsertComplete(token, cookie, uri);
        }
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        final QueryListener listener = getQueryListener();
        if (listener != null) {
            listener.onQueryComplete(token, cookie, cursor);
        } else if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        final QueryListener listener = getQueryListener();
        if (listener != null) {
            listener.onUpdateComplete(token, cookie, result);
        }
    }

    /**
     * Interface to listen for completed query operations.
     */
    public interface QueryListener {

        /**
         * Called when an asynchronous delete is completed.
         *
         * @param token  the token to identify the query, passed in from {@link
         *               QueryHandler#startDelete(int, Object, Uri, String, String[])}
         * @param cookie the cookie object that's passed in from {@link QueryHandler#startDelete(int,
         *               Object, Uri, String, String[])}
         * @param result the result returned from the delete operation
         */
        void onDeleteComplete(int token, Object cookie, int result);

        /**
         * Called when an asynchronous insert is completed.
         *
         * @param token  the token to identify the query, passed in from {@link
         *               QueryHandler#startInsert(int, Object, Uri, ContentValues)}
         * @param cookie the cookie object that's passed in from {@link QueryHandler#startInsert(int,
         *               Object, Uri, ContentValues)}
         * @param uri    the uri returned from the insert operation
         */
        void onInsertComplete(int token, Object cookie, Uri uri);

        /**
         * Called when an asynchronous query is completed.
         *
         * @param token  the token to identify the query, passed in from {@link
         *               QueryHandler#startQuery(int, Object, Uri, String[], String, String[],
         *               String)} ()}
         * @param cookie the cookie object passed in from {@link QueryHandler#startQuery(int,
         *               Object, Uri, String[], String, String[], String)} ()}
         * @param cursor the cursor holding the results from the query
         */
        void onQueryComplete(int token, Object cookie, Cursor cursor);

        /**
         * Called when an asynchronous update is completed.
         *
         * @param token  the token to identify the query, passed in from {@link
         *               QueryHandler#startUpdate(int, Object, Uri, ContentValues, String,
         *               String[])}
         * @param cookie the cookie object that's passed in from {@link QueryHandler#startUpdate(int,
         *               Object, Uri, ContentValues, String, String[])}
         * @param result the result returned from the update operation
         */
        void onUpdateComplete(int token, Object cookie, int result);

    }

    /**
     * A simple implementation of {@link QueryListener} that performs no action.
     * <p/>
     * Subclasses can use this to implement only the listener methods that they need.
     */
    public static class SimpleQueryListener implements QueryListener {

        /**
         * Construct a new simple query listener.
         */
        public SimpleQueryListener() {
        }

        @Override
        public void onDeleteComplete(int token, Object cookie, int result) {
        }

        @Override
        public void onInsertComplete(int token, Object cookie, Uri uri) {
        }

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        }

        @Override
        public void onUpdateComplete(int token, Object cookie, int result) {
        }

    }

}
