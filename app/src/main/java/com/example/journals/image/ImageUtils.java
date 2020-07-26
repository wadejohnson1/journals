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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.example.journals.R;
import com.example.journals.provider.JournalContract.Images;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

/**
 * A class containing static methods for image manipulation.
 */
public class ImageUtils {

    /**
     * Maximum number of attempts to generate a random filename.
     *
     * @see #generateRandomFileName(File, String)
     */
    private static final int MAX_ATTEMPTS = 100;
    /**
     * The size of the stream buffer used to copy files.
     *
     * @see #copyFileFromUri(Uri, ContentResolver, File)
     */
    private static final int STREAM_BUFFER_SIZE = 1024;

    /**
     * Private constructor.
     */
    private ImageUtils() {
    }

    /**
     * Calculate the sample size for scaling a rectangle based on the specified original and final
     * dimensions.
     * <p/>
     * Sample size will be a power of 2 and result in the closest scaled rectangle with dimensions
     * greater than or equal to the requested dimensions. If any original dimension is less than the
     * corresponding requested dimension, 1 is returned.
     *
     * @param origWidth  the original rectangle width
     * @param origHeight the original rectangle height
     * @param reqWidth   the requested width of the final rectangle, must be greater than 0
     * @param reqHeight  the requested height of the final rectangle, must be greater than 0
     * @return the sample size for scaling a bitmap
     * @throws IllegalArgumentException if {@code reqWidth} or {@code reqHeight} is not greater than
     *                                  0
     */
    public static int calculateSampleSize(int origWidth, int origHeight,
                                          int reqWidth, int reqHeight) {
        if ((reqWidth <= 0) || (reqHeight <= 0)) {
            throw new IllegalArgumentException(
                    "width and height must be greater than 0");
        } else {
            // Calculate the largest sample size that is a power of 2 and keeps
            // both height and width larger than the requested height and width.
            int sampleSize = 1;
            if ((origWidth > reqWidth) && (origHeight > reqHeight)) {
                while ((origHeight / (2 * sampleSize)) > reqHeight
                        && (origWidth / (2 * sampleSize)) > reqWidth) {
                    sampleSize *= 2;
                }
            }
            return sampleSize;
        }
    }

    /**
     * Copy a file from the specified URI to the specified location.
     * <p/>
     * The name of the copied file will be randomly generated.
     *
     * @param sourceUri     the content:// style URI of the file to copy
     * @param resolver      the {@link ContentResolver} used to resolve the source URI
     * @param destDirectory the directory of the destination file
     * @return the copied file, or {@code null} if the file could not be copied
     */
    @Nullable
    public static File copyFileFromUri(@NonNull Uri sourceUri, @NonNull ContentResolver resolver,
                                       @NonNull File destDirectory) {
        // Check for content:// style URI.
        if (!"content".equals(sourceUri.getScheme())) {
            throw new IllegalArgumentException("Source must be content:// style URI.");
        }
        // Create the destination directory, if required.
        else if (!destDirectory.exists() && !destDirectory.mkdirs()) {
            return null;
        } else {
            // Get the MIME type of the file.
            final String mimeType = resolver.getType(sourceUri);
            // Convert the MIME type to a file extension.
            final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            // Create a file with a unique name in the destination directory.
            final String fileName = generateRandomFileName(destDirectory, extension);
            if (fileName == null) {
                return null;
            } else {
                final File copyFile = new File(fileName);
                // Copy the image.
                boolean isCopied = true;
                try {
                    final InputStream input = resolver.openInputStream(sourceUri);
                    final OutputStream output = new FileOutputStream(copyFile);
                    byte[] buffer = new byte[STREAM_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    input.close();
                    output.close();
                } catch (IOException e) {
                    isCopied = false;
                }
                // Return the copied file or delete file if there was an error.
                if (isCopied) {
                    return copyFile;
                } else {
                    copyFile.delete();
                    return null;
                }

            }
        }
    }

    /**
     * Create a new file with a collision-resistant name.
     *
     * @param extension the file extension to add to the filename
     * @return a new file with a collision-resistant name
     * @throws IOException if an error occurs creating the file
     */
    public static File createFile(File directory, String extension) throws IOException {
        // Create a filename for the image.
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date()); // TODO locale
        final String imageFileName = R.string.app_name + "_" + timeStamp;
        return File.createTempFile(imageFileName, "." + extension, directory);
    }

    /**
     * Get a {@link BitmapFactory.Options} object containing the dimensions of a bitmap at the
     * specified path.
     *
     * @param fileDescriptor a file descriptor of a bitmap
     * @return a {@code BitmapFactory.Options} object containing the dimensions of the specified
     * bitmap
     */
    public static BitmapFactory.Options decodeBitmap(
            FileDescriptor fileDescriptor) {
        // Get the dimensions of the bitmap.
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        return options;
    }

    /**
     * Get a randomly generated, unique filename for the specified directory.
     *
     * @param directory the directory to create the file
     * @param suffix    the file suffix (excluding the ".")
     * @return a randomly generated, unique filename for the specified directory, or {@code null} if
     * a filename could not be generated
     */
    @Nullable
    public static String generateRandomFileName(@NonNull File directory, @NonNull String suffix) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            final String name = directory.getPath() + File.separator
                    + UUID.randomUUID().toString() + "." + suffix;
            final File file = new File(name);
            if (!file.exists()) {
                return name;
            }
        }
        return null;
    }

    /**
     * Get the EXIF rotation data from a JPEG image, as defined in {@link ExifInterface}
     *
     * @param context  the context in which to access resources
     * @param imageUri the URI of the image
     * @return the rotation of the image
     */
    public static int getExifRotation(Context context, Uri imageUri) {
        final File imageFile = ImageUtils.getFileForUri(context, imageUri);
        try {
            final ExifInterface exif = new ExifInterface(imageFile.getPath());
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
        } catch (IOException e) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }

    /**
     * Get the absolute path of a file from a content:// style URI.
     *
     * @param context the context in which to access resources
     * @param uri     the content:// style URI to convert to a file
     * @return the path referenced in the specified URI
     */
    public static File getFileForUri(@NonNull Context context, @NonNull Uri uri) {
        final String uriPath = uri.getPath();
        final int pathEnd = uriPath.lastIndexOf('/');
        final String fileName = uriPath.substring(pathEnd + 1);
        final File root;
        switch (uriPath.substring(1, pathEnd)) {
            case Images.CONTENT_DIRECTORY:
                root = new File(context.getFilesDir().getPath(),
                        context.getString(R.string.provider_filePath_images));
                break;
            default:
                root = null;
        }
        if (root == null) {
            throw new IllegalArgumentException(
                    "Unable to find configured root for " + uri);
        }
        File file = new File(root, fileName);
        //        try {
        //            file = file.getCanonicalFile();
        //        } catch (IOException e) {
        //            throw new IllegalArgumentException(
        //                    "Failed to resolve canonical path for " + file);
        //        }
        if (!file.getPath().startsWith(root.getPath())) {
            throw new SecurityException(
                    "Resolved path jumped beyond configured root");
        }
        return file;
    }

    /**
     * Get the mime type of the specified URL.
     *
     * @param uri the URI to determine the mime type of
     * @return the mime type of the URI, or {@code null} if no mime type could be determined
     */
    @Nullable
    public static String getMimeType(@NonNull Context context, @NonNull Uri uri) {
        final String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = context.getContentResolver().getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    /**
     * Get a bitmap createdAt from a source bitmap that has been transposed by the specified
     * orientation.
     *
     * @param source      the bitmap to transpose
     * @param orientation the orientation of the final bitmap
     * @return a bitmap transposed by the specified orientation
     */
    public static Bitmap rotateBitmap(Bitmap source, int orientation) {
        final Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return source;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return source;
        }
        try {
            final Bitmap bmRotated = Bitmap.createBitmap(source, 0, 0,
                    source.getWidth(), source.getHeight(), matrix, true);
            source.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

}
