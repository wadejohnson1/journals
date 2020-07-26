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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * Class for managing image loading into {@link ImageView}s.
 * <p/>
 * Images are loaded on a background thread and stored in an optional LRU cache to use again later.
 * <p/>
 * Note: This class uses the supplied image views' tag property to store information. In order to
 * ensure correct functionality, views managed by this class should not have their tags edited from
 * other locations.
 */
public class ImageLoaderOld {

    /**
     * A value indicating the image loader has no cache.
     */
    public static final int NO_CACHE = 0;
    /**
     * The size of the bitmap cache.
     */
    public static final int BITMAP_CACHE_SIZE = 0; // TODO

    /**
     * Singleton image loader.
     */
    private static ImageLoaderOld sImageLoader;

    /**
     * Set whether any EXIF rotation data contained in JPEG images is used when displaying the
     * images.
     */
    private final boolean mIsUsingExifRotation = true; // TODO
    /**
     * Listener for bitmap load completion.
     */
    private final OnBitmapLoadedListener mBitmapLoadedListener = new OnBitmapLoadedListener();
    /**
     * A cache for storing the most recently loaded images.
     */
    private final BitmapLruCache<Uri> mBitmapCache = null; // TODO
    /**
     * The context in which to access resources.
     */
    private final Context mContext;
    /**
     * The bitmap to display while background loading is in progress.
     */
    private final Drawable mInProgressDrawable = null;// TODO
    /**
     * The bitmap to display if there is a loading error.
     */
    private final Drawable mLoadFailedDrawable = null;// TODO

    /**
     * Private constructor.
     */
    private ImageLoaderOld(Context context) {
        mContext = context;
    }

    /**
     * Construct a new image loader from an image loader builder.
     *
     * @param builder the builder to create this image loader from
     */
    private ImageLoaderOld(Builder builder) { // TODO
        mContext = builder.context;
//        mInProgressDrawable = builder.inProgressDrawable;
//        mLoadFailedDrawable = builder.loadFailedDrawable;
//        mBitmapCache = (builder.cacheSize != NO_CACHE) ? new BitmapLruCache<Uri>(
//                builder.cacheSize) : null;
//        mIsUsingExifRotation = builder.isUsingExifRotation;
    }

    /**
     * Asynchronously load a one-off image specified by the supplied URI into the specified image
     * view. The image retrived will not be cached.
     * <p/>
     * If a task associated with the image view already exists, that task will be cancelled.
     *
     * @param context             the context used to access resources
     * @param imageUri            the URI of the image to load
     * @param imageView           the view in which to place the loaded image
     * @param inProgressDrawable  the drawable to display while background loading is in progress
     * @param loadFailedDrawable  the drawable to display if there is a loading error
     * @param isUsingExifRotation {@code true} if any EXIF rotation data contained in loaded JPEG
     *                            images is used when displaying the images, {@code false}
     *                            otherwise
     * @throws IllegalStateException if {@code imageView} length or width is <= 0
     */
    public static void loadImage(@NonNull Context context, @NonNull Uri imageUri,
                                 @NonNull ImageView imageView, Drawable inProgressDrawable,
                                 Drawable loadFailedDrawable, boolean isUsingExifRotation) {
        // Make sure the image view has already been laid out.
        if ((imageView.getWidth() <= 0) || (imageView.getHeight() <= 0)) {
            throw new IllegalStateException();
        }
        // Cancel any previously running task.
        final BitmapAsyncTask oldTask = getBitmapAsyncTask(imageView);
        if (oldTask != null) {
            oldTask.cancel(true);
        }
        // Create listener for bitmap load completion.
        OnBitmapLoadedListenerStatic listener =
                new OnBitmapLoadedListenerStatic(loadFailedDrawable);
        // Create new task to load bitmap in the background.
        final BitmapAsyncTask task = new BitmapAsyncTask(context, imageUri,
                imageView.getWidth(), imageView.getHeight(), imageView, listener,
                isUsingExifRotation);
        // Keep a reference to task in image view tag.
        imageView.setTag(task);
        // Load image view with "in progress" bitmap.
        imageView.setImageDrawable(inProgressDrawable);
        task.execute();
    }

    /**
     * Get the singleton instance of the image loader.
     *
     * @param context the context used to access resources; the image loader internally holds a
     *                reference to the application context of the passed in parameter to avoid
     *                memory leaks
     * @return the singleton instance of the image loader
     */
    public static ImageLoaderOld getInstance(Context context) {
        if (sImageLoader == null) {
            sImageLoader = new ImageLoaderOld(context.getApplicationContext());
        }
        return sImageLoader;
    }

    /**
     * Get the {@link BitmapAsyncTask} associated with the specified image view.
     *
     * @param imageView the view to get the task from
     * @return the task associated with {@code imageView}, or {@code null} if {@code imageView} does
     * not have a task associated with it
     */
    private static BitmapAsyncTask getBitmapAsyncTask(ImageView imageView) {
        final Object tag = imageView.getTag();
        return (tag instanceof BitmapAsyncTask) ? (BitmapAsyncTask) tag : null;
    }

    /**
     * Remove all items from the cache.
     */
    public void clearCache() {
        if (mBitmapCache != null) {
            mBitmapCache.evictAll();
        }
    }

    /**
     * Asynchronously load the image specified by the supplied URI into the specified image view.
     *
     * @param imageUri  the URI of the image to load
     * @param imageView the view in which to place the loaded image
     * @throws IllegalStateException if {@code imageView} length or width is <= 0
     */
    public void loadImage(Uri imageUri, ImageView imageView) {
        // Make sure the image view has already been laid out.
        if ((imageView.getWidth() <= 0) || (imageView.getHeight() <= 0)) {
            throw new IllegalStateException();
        }
        // Load bitmap from cache if it exists, checking to see if the saved bitmap is at least
        // as large as the image view.
        final Bitmap bitmap = (mBitmapCache != null) ? mBitmapCache.get(imageUri) : null;
        if ((bitmap != null) && (bitmap.getWidth() >= imageView.getWidth()) && (bitmap.getHeight()
                >= imageView.getHeight())) {
            imageView.setImageBitmap(bitmap);
        }
        // The requested bitmap is not in the cache.
        else {
            final BitmapAsyncTask oldTask = getBitmapAsyncTask(imageView);
            if ((oldTask == null) || !oldTask.getBitmapUri().equals(imageUri)) {
                // Cancel the old task.
                if (oldTask != null) {
                    oldTask.cancel(true);
                }
                // Create new task to load bitmap in the background.
                final BitmapAsyncTask task = new BitmapAsyncTask(mContext,
                        imageUri, imageView.getWidth(), imageView.getHeight(), imageView,
                        mBitmapLoadedListener, mIsUsingExifRotation);
                // Keep a reference to task in image view tag.
                imageView.setTag(task);
                // Load image view with "in progress" bitmap.
                imageView.setImageDrawable(mInProgressDrawable);
                task.execute();
            }
        }
    }

    /**
     * A builder object used to create a new {@link ImageLoaderOld}.
     * <p/>
     * Use {@link #build()} to create a new image loader from the builder.
     */
    public static final class Builder {

        private final Context context;
        private boolean isUsingExifRotation = false;
        private Drawable inProgressDrawable = null;
        private Drawable loadFailedDrawable = null;
        private int cacheSize = NO_CACHE;

        /**
         * Create a new builder instance.
         *
         * @param context the context to use to access resources
         */
        public Builder(@NonNull Context context) {
            this.context = context;
        }

        /**
         * Create a new image loader from this builder.
         *
         * @return a new image loader instance
         */
        public ImageLoaderOld build() {
            return new ImageLoaderOld(this);
        }

        /**
         * Set the bitmap to display while background loading is in progress.
         *
         * @param drawable the drawable to display while background loading is in progress
         * @return this builder object, for chaining calls
         */
        public Builder setInProgressDrawable(Drawable drawable) {
            inProgressDrawable = drawable;
            return this;
        }

        /**
         * Set the bitmap to display while background loading is in progress.
         *
         * @param resourceId the resource ID of the drawable to display while background loading is
         *                   in progress
         * @return this builder object, for chaining calls
         */
        public Builder setInProgressDrawable(@DrawableRes int resourceId) {
            inProgressDrawable = context.getResources().getDrawable(resourceId);
            return this;
        }

        /**
         * Set whether any EXIF rotation data contained in JPEG images is used when displaying the
         * images.
         *
         * @param isUsingExifRotation {@code true} if any EXIF rotation data contained in loaded
         *                            JPEG images is used when displaying the images, {@code false}
         *                            otherwise
         * @return this builder object, for chaining calls
         */
        public Builder setIsUsingExifRotation(boolean isUsingExifRotation) {
            this.isUsingExifRotation = isUsingExifRotation;
            return this;
        }

        /**
         * Set the bitmap to display if there is an error loading the bitmap.
         *
         * @param drawable the drawable to display if there is an error loading the bitmap
         * @return this builder object, for chaining calls
         */
        public Builder setLoadFailedDrawable(Drawable drawable) {
            loadFailedDrawable = drawable;
            return this;
        }

        /**
         * Set the bitmap to display if there is an error loading the bitmap.
         *
         * @param resourceId the resource ID of the drawable to display if there is an error loading
         *                   the bitmap
         * @return this builder object, for chaining calls
         */
        public Builder setLoadFailedDrawable(@DrawableRes int resourceId) {
            loadFailedDrawable = context.getResources().getDrawable(resourceId);
            return this;
        }

        /**
         * Set the memory cache size (in bytes).
         *
         * @param size the size of the memory cache (in bytes); no cache will be createdAt if {@code
         *             size} is less than 1
         * @return this builder object, for chaining calls
         */
        public Builder setMemoryCacheSize(int size) {
            cacheSize = (size >= 1) ? size : NO_CACHE;
            return this;
        }

    }

    /**
     * Static listener for bitmap load completion.
     */
    private static class OnBitmapLoadedListenerStatic implements BitmapAsyncTask
            .OnBitmapLoadedListener {

        /**
         * Drawable to use if bitmap load fails.
         */
        private Drawable mLoadFailedDrawable;

        public OnBitmapLoadedListenerStatic(Drawable loadFailedDrawable) {
            mLoadFailedDrawable = loadFailedDrawable;
        }

        @Override
        public void onBitmapLoaded(BitmapAsyncTask task, Object token, Bitmap bitmap) {
            if (token != null) {
                final ImageView imageView = (ImageView) token;
                final Object viewTask = imageView.getTag();
                if (task == viewTask) {
                    // Set the bitmap on the image view.
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageDrawable(mLoadFailedDrawable);
                    }
                    // Clear task reference.
                    imageView.setTag(null);
                }
            }
        }

    }

    /**
     * Listener for bitmap load completion.
     */
    private class OnBitmapLoadedListener implements BitmapAsyncTask.OnBitmapLoadedListener {

        @Override
        public void onBitmapLoaded(BitmapAsyncTask task, Object token,
                                   Bitmap bitmap) {
            if (token != null) {
                final ImageView imageView = (ImageView) token;
                final Object viewTask = imageView.getTag();
                if (task == viewTask) {
                    // Set the bitmap on the image view.
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageDrawable(mLoadFailedDrawable);
                    }
                    // Clear task reference.
                    imageView.setTag(null);
                }
            }
            // Update the bitmap cache, if applicable.
            if ((mBitmapCache != null) && (bitmap != null)) {
                mBitmapCache.addBitmapToCache(task.getBitmapUri(), bitmap);
            }
        }

    }

}
