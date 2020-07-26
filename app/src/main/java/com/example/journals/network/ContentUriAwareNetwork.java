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
package com.example.journals.network;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.ByteArrayPool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link Network} implementation that handles requests with a {@code content://} style request
 * URI.
 */
public class ContentUriAwareNetwork extends BasicNetwork {

    /**
     * Buffer size for loading files from a content provider into memory.
     */
    private static final int BUFFER_SIZE = 8192;
    /**
     * Scheme for {@code content://} style URIs.
     */
    private static final String URI_SCHEME_CONTENT = "content";

    /**
     * Context used to access content provider.
     */
    private final Context mContext;

    /**
     * Create a new network.
     *
     * @param httpStack HTTP stack to be used
     * @param context   context used to access resources
     */
    public ContentUriAwareNetwork(BaseHttpStack httpStack, Context context) {
        super(httpStack);
        mContext = context;
    }

    /**
     * Create a new network.
     *
     * @param httpStack HTTP stack to be used
     * @param pool      a buffer pool that improves GC performance in copy operations
     * @param context   context used to access resources
     */
    public ContentUriAwareNetwork(BaseHttpStack httpStack, ByteArrayPool pool, Context context) {
        super(httpStack, pool);
        mContext = context;
    }

    @Override
    public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        final Uri requestUri = Uri.parse(request.getUrl());
        if (!requestUri.getScheme().equals(URI_SCHEME_CONTENT)) {
            return super.performRequest(request);
        } else {
            final ContentResolver resolver = mContext.getContentResolver();
            // Determine the size of the file being read.
            final Cursor cursor = resolver.query(requestUri, null, null, null, null);
            cursor.moveToFirst();
            final int size = cursor.getInt(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            // Create a byte array in memory out of the file.
            final ByteArrayOutputStream output;
            final byte[] buffer = new byte[BUFFER_SIZE];
            try {
                output = new ByteArrayOutputStream(size);
                final InputStream input = mContext.getContentResolver().openInputStream(requestUri);
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();
            } catch (IOException e) {
                throw new VolleyError();
            }
            return new NetworkResponse(output.toByteArray());
        }
    }

}
