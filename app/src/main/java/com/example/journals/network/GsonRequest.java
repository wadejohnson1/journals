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

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utilities for accessing a network and performing operations.
 */
public class GsonRequest<T> extends JsonRequest<T> {

    /**
     * Set whether to show debug information.
     */
    private static final boolean DEBUG = true; // TODO change to false
    /**
     * Set whether to show debug information.
     */
    private static final String DEBUG_TAG = GsonRequest.class.getSimpleName();

    /**
     * Class object for GSON reflection.
     */
    private final Class<T> mClass;
    /**
     * Headers to use with the request.
     */
    private final Map<String, String> mHeaders;

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param clazz         class object for GSON reflection
     * @param headers       request headers
     * @param requestBody   parameters to post with the request, or {@code null} indicating no
     *                      parameters will be posted along with request
     * @param listener      listener to receive response
     * @param errorListener listener to receive errors, or {@code null} to ignore errors
     */
    public GsonRequest(int method, @NonNull String url, @NonNull Class<T> clazz,
                       @Nullable Map<String, String> headers, @Nullable String requestBody,
                       @Nullable Response.Listener<T> listener,
                       @Nullable Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        mClass = clazz;
        mHeaders = headers;
        if (DEBUG) {
            Log.i(DEBUG_TAG,
                    "Sending " + ((method == Method.POST) ? "POST" : "GET") + " to " + url);
            if (headers != null) {
                Log.i(DEBUG_TAG, headers.toString());
            }
            if (requestBody != null) {
                Log.i(DEBUG_TAG, requestBody);
            }
        }
    }

    /**
     * Constructor which defaults to {@code GET} if {@code requestBody} is {@code null}, {@code
     * POST} otherwise.
     *
     * @param url           URL to fetch the JSON from
     * @param clazz         class object for GSON reflection
     * @param headers       request headers
     * @param requestBody   parameters to post with the request, or {@code null} indicating no
     *                      parameters will be posted along with request
     * @param listener      listener to receive response
     * @param errorListener listener to receive errors, or {@code null} to ignore errors
     * @see #GsonRequest(int, String, Class, Map, String, Response.Listener, Response.ErrorListener)
     */
    public GsonRequest(@NonNull String url, @NonNull Class<T> clazz,
                       @Nullable Map<String, String> headers, @Nullable String requestBody,
                       @Nullable Response.Listener<T> listener,
                       @Nullable Response.ErrorListener errorListener) {
        this((requestBody == null) ? Method.GET : Method.POST, url, clazz, headers, requestBody,
                listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            final String json =
                    new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            if (DEBUG) {
                Log.i(DEBUG_TAG, response.headers.toString());
                Log.v(DEBUG_TAG, json);
            }
            return Response.success(new Gson().fromJson(json, mClass),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

}