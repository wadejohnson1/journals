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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

/**
 * A class for loading bitmaps from a content provider on a background thread.
 * <p/>
 * The returned bitmap will be sampled by a power of 2 from the original bitmap contained in the
 * provider such that the scale used will result in a final bitmap with dimensions as close to the
 * requested dimensions as possible without going under. If either dimension of the original bitmap
 * are less than the corresponding requested dimension, the full-sized bitmap will be returned.
 * <p/>
 * All objects passed to the constructor are kept as strong references for the duration of the task.
 * Keep memory leaks in mind when providing the {@link OnBitmapLoadedListener}.
 */
public class BitmapAsyncTask extends AsyncTask<Void, Void, Bitmap> {

    private static final String MIME_TYPE_JPEG = "image/jpeg";

    /**
     * Set whether any EXIF rotation data contained in JPEG images is used when displaying the
     * images.
     */
    private final boolean mIsUsingExifRotation;
    /**
     * Context used to access resources.
     */
    private final Context mContext;
    /**
     * The requested dimension of the bitmap.
     */
    private final int mReqWidth, mReqHeight;
    /**
     * The callback to invoke when the bitmap is loaded.
     */
    private final OnBitmapLoadedListener mListener;
    /**
     * The URI of the bitmap to load.
     */
    private final Uri mBitmapUri;
    /**
     * A reference to an optional token used to identify this task.
     */
    private Reference<Object> mTokenReference;

    /**
     * Create a new task to load a bitmap.
     * <p/>
     * The returned bitmap will be sampled by a power of 2 from the original bitmap contained in the
     * provider such that the scale used will result in a final bitmap with dimensions as close to
     * the requested dimensions as possible without going under. If either dimension of the original
     * bitmap are less than the corresponding requested dimension, the full-sized bitmap will be
     * returned.
     *
     * @param context   the context to use to access resources, cannot be {@code null}
     * @param bitmapUri the URI of the bitmap to load, cannot be {@code null}
     * @param reqWidth  the requested width of the loaded bitmap, must be greater than 0
     * @param reqHeight the requested height of the loaded bitmap, must be greater than 0
     * @param token     an optional, weakly-referenced token to identify this task
     * @param listener  callback for receiving the loaded bitmap, cannot be {@code null}
     */
    public BitmapAsyncTask(@NonNull Context context, @NonNull Uri bitmapUri, int reqWidth,
                           int reqHeight, Object token, @NonNull OnBitmapLoadedListener listener,
                           boolean isUsingExifRotation) {
        if ((reqWidth <= 0) || (reqHeight <= 0)) {
            throw new IllegalArgumentException();
        } else {
            mContext = context;
            mBitmapUri = bitmapUri;
            mReqWidth = reqWidth;
            mReqHeight = reqHeight;
            if (token != null) {
                mTokenReference = new WeakReference<>(token);
            }
            mListener = listener;
            mIsUsingExifRotation = isUsingExifRotation;
        }
    }

    /**
     * Get the URI associated with this task.
     */
    public Uri getBitmapUri() {
        return mBitmapUri;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        final ContentResolver resolver = mContext.getContentResolver();
        AssetFileDescriptor descriptor;
        try {
            descriptor = resolver.openAssetFileDescriptor(mBitmapUri, "r");
        } catch (FileNotFoundException e) {
            return null;
        }
        if (descriptor != null) {
            // Get the dimensions of the bitmap.
            final Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(), null, options);
            // Decode a bitmap sized to fill the view.
            options.inSampleSize = ImageUtils.calculateSampleSize(
                    options.outWidth, options.outHeight, mReqWidth, mReqHeight);
            options.inJustDecodeBounds = false;
            final Bitmap bitmap = BitmapFactory.decodeFileDescriptor(
                    descriptor.getFileDescriptor(), null, options);
            if (mIsUsingExifRotation && (bitmap != null)
                    && (MIME_TYPE_JPEG.equals(resolver.getType(mBitmapUri)))) {
                final File imageFile = ImageUtils.getFileForUri(mContext, mBitmapUri);
                final ExifInterface exif;
                try {
                    exif = new ExifInterface(imageFile.getPath());
                } catch (IOException e) {
                    return bitmap;
                }
                final int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
                return ImageUtils.rotateBitmap(bitmap, orientation);
            } else {
                return bitmap;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        final Object token = (mTokenReference != null) ? mTokenReference.get() : null;
        mListener.onBitmapLoaded(this, token, bitmap);
    }

    /**
     * A listener for receiving notification of bitmap loading completion.
     */
    public interface OnBitmapLoadedListener {

        /**
         * Called when the bitmap is loaded.
         *
         * @param task   the {@link BitmapAsyncTask} that called this method
         * @param token  the optional, weakly-referenced token to identify this task
         * @param bitmap the bitmap that was loaded, or {@code null} if an error occurred while
         *               attempting to load the bitmap
         */
        void onBitmapLoaded(BitmapAsyncTask task, Object token, Bitmap bitmap);

    }

}