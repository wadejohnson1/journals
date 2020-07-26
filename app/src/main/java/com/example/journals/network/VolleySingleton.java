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

import android.content.Context;
import android.graphics.Bitmap;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.example.journals.image.BitmapLruCache;

import java.io.File;

/**
 * A request queue singleton for accessing network resources.
 */
public class VolleySingleton {

    /**
     * Amount of memory to use for bitmap image caching (in bytes).
     */
    private static final int BITMAP_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 8);
    /**
     * Directory for storing cache files.
     */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /**
     * The singleton instance of this class.
     */
    private static VolleySingleton mInstance;

    /**
     * Background loader for images.
     */
    private final ImageLoader mImageLoader;
    /**
     * Queue for dispatching volley requests.
     */
    private final RequestQueue mRequestQueue;

    /**
     * Create a new volley singleton
     *
     * @param appContext the application context, used to access resources
     */
    private VolleySingleton(Context appContext) {
        mRequestQueue = newRequestQueue(appContext);
        mImageLoader = new ImageLoader(mRequestQueue, new ImageCache());
    }

    /**
     * Get the instance of the Volley singleton.
     *
     * @param context the context used to access resources; the application context will be used
     *                regardless of what context is passed in here
     * @return the instance of the Volley singleton
     */
    public static synchronized VolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleySingleton(context.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context context used to access resources
     * @return a started {@link RequestQueue} instance
     */
    private static RequestQueue newRequestQueue(Context context) {
        final File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        final Network network = new ContentUriAwareNetwork(new HurlStack(), context);
        final RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        queue.start();
        return queue;
    }

    /**
     * Convenience method for adding a request to the request queue.
     *
     * @param request the request to service
     * @param <T>     the passed in request
     */
    public <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }

    /**
     * Get the image loader for handling images.
     *
     * @return the image loader for handling images
     */
    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    /**
     * Get the request queue for handling requests.
     *
     * @return the request queue for handling requests
     */
    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    /**
     * Class used to store bitmaps in an LRU cache.
     */
    private static class ImageCache implements ImageLoader.ImageCache {

        /**
         * Underlying LRU cache holding bitmaps.
         */
        private final BitmapLruCache<String> mCache = new BitmapLruCache<>(BITMAP_CACHE_SIZE);

        @Override
        public Bitmap getBitmap(String url) {
            return mCache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            mCache.put(url, bitmap);
        }

    }

}
