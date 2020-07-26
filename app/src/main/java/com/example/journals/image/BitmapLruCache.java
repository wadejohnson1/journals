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
package com.example.journals.image;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * A cache that holds strong references to a limited number of bitmaps. Each time a value is
 * accessed, it is moved to the head of a queue. When a value is added to a full cache, the value at
 * the end of that queue is evicted and may become eligible for garbage collection.
 */
public class BitmapLruCache<K> extends LruCache<K, Bitmap> {

    /**
     * Create a new bitmap LRU cache.
     *
     * @param maxSize the maximum sum of the sizes of the bitmaps in the cache (in bytes)
     */
    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    /**
     * Add a bitmap to the cache.
     * <p/>
     * If the specified key already exists, this call will do nothing.
     *
     * @param key    the key that will be used to access the cached bitmap, cannot be {@code null}
     * @param bitmap the bitmap to add to the cache, cannot be {@code null}
     */
    public void addBitmapToCache(K key, Bitmap bitmap) {
        // Synchronize on cache to perform multiple operations atomically.
        synchronized (this) {
            if (get(key) == null) {
                put(key, bitmap);
            }
        }
    }

    @Override
    protected int sizeOf(K key, Bitmap bitmap) {
        return bitmap.getByteCount();
    }

}