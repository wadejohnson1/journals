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
package com.example.journals.journal;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.example.journals.journal.JournalDialogFragment.DialogListener;
import com.example.journals.widget.MediaActivity;
import com.example.journals.R;
import com.example.journals.account.AccountUtils;
import com.example.journals.account.AccountUtils.AuthFailureErrorListener;
import com.example.journals.image.ImageUtils;
import com.example.journals.network.GsonRequest;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.Activity;
import com.example.journals.network.NetworkUtils.CreateActivityResponse;
import com.example.journals.network.NetworkUtils.DeleteJournalRequest;
import com.example.journals.network.NetworkUtils.DeleteJournalResponse;
import com.example.journals.network.VolleySingleton;
import com.example.journals.provider.JournalContract;
import com.example.journals.provider.JournalContract.Activities;
import com.example.journals.provider.JournalContract.Images;
import com.example.journals.provider.QueryHandler;
import com.example.journals.provider.QueryHandler.SimpleQueryListener;
import com.example.journals.widget.AuthHandlerFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

/**
 * A class for editing spot details and associated data.
 * <p/>
 * If creating a new spot, this fragment takes a content URI of type {@link
 * Activities#CONTENT_TYPE_DIR}. If updating an existing spot, this fragment takes an content URI of
 * type {@link Activities#CONTENT_TYPE_ITEM}.
 */
public class SpotEditorFragment extends AuthHandlerFragment implements DialogListener {

    /**
     * Intent request code for starting an external activity or receiving system permissions.
     */
    private static final int REQUEST_CODE_GET_IMAGE = 1, REQUEST_CODE_GET_VIDEO = 2,
            REQUEST_CODE_MEDIA_BROWSER = 3, REQUEST_CODE_PLACE = 4;
    /**
     * Reason for requesting an auth token.
     */
    private static final int AUTH_REASON_CREATE_ACTIVITY = 0, AUTH_REASON_DELETE_ACTIVITY = 1;
    /**
     * Dialog type for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_HAS_MEDIA = 0,
            DIALOG_TYPE_GET_MEDIA_NO_CAMERA_HAS_MEDIA = 1,
            DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_NO_MEDIA = 2, DIALOG_TYPE_CONFIRM_DELETE = 3,
            DIALOG_TYPE_CONFIRM_DISCARD_CHANGES = 4, DIALOG_TYPE_ERROR_OPEN_CAMERA = 5,
            DIALOG_TYPE_ERROR_OPEN_IMAGE = 6, DIALOG_TYPE_ERROR_OPEN_STORAGE = 7;
    /**
     * Flags for recording changes in data properties.
     */
    private static final int FLAG_IS_LOADED = 1, FLAG_IS_DELETED = 1 << 1, FLAG_IS_CHANGED_TITLE =
            1 << 2, FLAG_IS_CHANGED_DESCRIPTION = 1 << 3, FLAG_IS_CHANGED_LOCATION = 1 << 4,
            FLAG_IS_CHANGED_MEDIA_URI = 1 << 5;
    /**
     * Mappings used for the image select dialog.
     */
    private static final int DIALOG_CHOICE_IMAGE_LIBRARY = 0, DIALOG_CHOICE_VIDEO_LIBRARY = 1,
            DIALOG_CHOICE_RECORD_IMAGE = 2, DIALOG_CHOICE_RECORD_VIDEO = 3,
            DIALOG_CHOICE_DELETE_MEDIA = 4;
    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = SpotEditorFragment.class.getSimpleName();
    /**
     * Tag for dialogs.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Default file extension for new files.
     */
    private static final String FILE_EXTENSION_DEFAULT = "jpeg";
    /**
     * Saved instance state key for storing the "title" text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_TITLE = "SAVE_STATE_TITLE";
    /**
     * Saved instance state key for storing the "description" text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_DESCRIPTION = "SAVE_STATE_DESCRIPTION";
    /**
     * Saved instance state key for storing the location description text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_LOCATION = "SAVE_STATE_LOCATION";
    /**
     * Saved instance state key for storing the latitude component of the activity location
     * coordinates.
     * <p/>
     * Type: double
     */
    private static final String SAVE_STATE_LATITUDE = "SAVE_STATE_LATITUDE";
    /**
     * Saved instance state key for storing the longitude component of the activity location
     * coordinates.
     * <p/>
     * Type: double
     */
    private static final String SAVE_STATE_LONGITUDE = "SAVE_STATE_LONGITUDE";
    /**
     * Saved instance state key for storing the camera URI.
     * <p/>
     * Type: Parcelable
     */
    private static final String SAVE_STATE_CAMERA_URI = "SAVE_STATE_CAMERA_URI";
    /**
     * Saved instance state key for storing the media URI.
     * <p/>
     * Type: Parcelable
     */
    private static final String SAVE_STATE_MEDIA_URI = "SAVE_STATE_MEDIA_URI";
    /**
     * Saved instance state key for storing the media URI.
     * <p/>
     * Type: Parcelable
     */
    private static final String SAVE_STATE_MEDIA_SELECT_MAP = "SAVE_STATE_MEDIA_SELECT MAP";
    /**
     * Saved instance state key for storing flags.
     */
    private static final String SAVE_STATE_FLAGS = "SAVE_STATE_FLAGS";

    /**
     * Projection for building a cursor loader.
     */
    private static final String[] QUERY_PROJECTION =
            new String[]{Activities.COLUMN_ID, Activities.COLUMN_TITLE,
                    Activities.COLUMN_DESCRIPTION, Activities.COLUMN_LATITUDE,
                    Activities.COLUMN_LONGITUDE, Activities.COLUMN_IMAGE_URI};

    /**
     * Set whether the fragment is updating an existing spot (as opposed to creating a new one).
     */
    private boolean mIsUpdating;
    /**
     * Latitude component of the activity location coordinates.
     */
    private double mLatitude = 0;
    /**
     * Longitude component of the activity location coordinates.
     */
    private double mLongitude = 0;
    /**
     * Flags for setting activity state.
     */
    private int mFlags = 0;
    /**
     * A map used to get the user choice from the image selection dialog.
     * <p/>
     * Used only when content is editable.
     */
    private SparseIntArray mImageSelectMap = null;
    /**
     * Text description of activity location.
     */
    private String mLocation;
    /**
     * Text input layout containing "description" edit text.
     */
    private TextInputLayout mLayoutDescription;
    /**
     * Text input layout containing "title" edit text.
     */
    private TextInputLayout mLayoutTitle;
    /**
     * The URI of the media captured by a camera intent.
     */
    private Uri mCameraUri = null;
    /**
     * The URI of device local (ie. available from content provider) media associated with this
     * spot.
     */
    private Uri mLocalMediaUri = null;
    /**
     * The image view used to display the image associated with this spot.
     */
    private NetworkImageView mImageView;

    /**
     * Load an image from the specified URI to the specified image view.
     *
     * @param context   the context used to access resources
     * @param imageUri  the URI of the image to load
     * @param imageView the image void to load the image into; the image will not be loaded if
     *                  {@code imageView} has not already been laid out (width/height == 0)
     */
    private static void loadImage(Context context, Uri imageUri, ImageView imageView) { // TODO
        //        final Drawable drawable = context.getResources().getDrawable(R.drawable.ic_blyspot_9patch);
        //        // If view is already sized, load image immediately.
        //        if (imageUri != null) {
        //            if ((imageView.getWidth() > 0) && (imageView.getHeight() > 0)) {
        //                ImageLoaderOld.loadImage(context, imageUri, imageView, drawable, null, true);
        //            }
        //        }
        //        // Clear the image from the image view.
        //        else {
        //            imageView.setImageDrawable(drawable);
        //        }
    }

    /**
     * Check if a camera exists and there is an activity that can handle an image capture intent.
     *
     * @param context the context used to access resources
     * @return {@code true} if a camera is present and there is an activity that can handle an image
     * capture intent, {@code false} otherwise
     */
    private static boolean isCameraAndIntentAvailable(Context context) {
        final PackageManager manager = context.getPackageManager();
        // A camera is available.
        if (manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            final List<ResolveInfo> list = manager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        }
        // No camera and/or intent is available.
        else {
            return false;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getArguments().getParcelable(Constants.ARG_DATA);
        final View view = getView();
        // Set up toolbar.
        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getArguments().getInt(Constants.ARG_HOME_BUTTON_IMAGE));
        toolbar.setNavigationOnClickListener(new NavigationOnClickListener());
        toolbar.inflateMenu(R.menu.menu_spot_editor);
        toolbar.setOnMenuItemClickListener(new OnMenuItemClickListener());
        // If this is a new activity, hide the "OK" menu item and show the "edit location" item.
        if (activity == null) {
            final Menu menu = toolbar.getMenu();
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_editLocation).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        // Set up commonly used views.
        mLayoutTitle = view.findViewById(R.id.layout_title);
        mLayoutDescription = view.findViewById(R.id.layout_description);
        mImageView = view.findViewById(R.id.image);
        //        mImageView.setDefaultImageResId(R.drawable.image_default); TODO set default image
        // Set up the floating action button.
        final FloatingActionButton button = view.findViewById(R.id.floatingButton);
        button.setOnClickListener(new FloatingButtonListener());

        // *****Restore saved instance state, if applicable.***** //
        if (savedInstanceState != null) {
            // Determine if activity was saved before it finished loading.
            final int flags = savedInstanceState.getInt(SAVE_STATE_FLAGS);
            if ((flags & FLAG_IS_LOADED) != 0) {
                mFlags = flags;
                // Setup text fields.
                mLayoutTitle.getEditText().setText(savedInstanceState.getString(SAVE_STATE_TITLE));
                mLayoutDescription.getEditText()
                        .setText(savedInstanceState.getString(SAVE_STATE_DESCRIPTION));
                setupTextChangedListeners();
                // Setup location information.
                mLocation = savedInstanceState.getString(SAVE_STATE_LOCATION);
                mLatitude = savedInstanceState.getDouble(SAVE_STATE_LATITUDE);
                mLongitude = savedInstanceState.getDouble(SAVE_STATE_LONGITUDE);
                // Setup media.
                mCameraUri = savedInstanceState.getParcelable(SAVE_STATE_CAMERA_URI);
                mLocalMediaUri = savedInstanceState.getParcelable(SAVE_STATE_MEDIA_URI);
                loadMedia();
            }
        }

        // *****Set up the UI from passed in arguments or provider data.***** //
        // TODO does not take into account activities in provider that do not have server ID
        if ((mFlags & FLAG_IS_LOADED) == 0) {
            // An existing activity is being updated.
            if (activity != null) {
                // Load data from the provider, if any exists.
                final QueryHandler handler = new QueryHandler(getContext().getContentResolver());
                handler.setQueryListener(new QueryListener());
                handler.startQuery(0, null, Activities.CONTENT_URI, QUERY_PROJECTION,
                        Activities.COLUMN_SERVER_ID + "=?",
                        new String[]{activity.activityId.toString()}, null);
                // Disable updates until data is loaded.
                setEnabled(false);
            }
            // A new activity is being created.
            else {
                setupTextChangedListeners();
                mFlags |= FLAG_IS_LOADED;
            }
        }
        setLocationText();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_GET_IMAGE: // TODO do other way around, pic to public storage first
                // Update the media URI and copy to public storage.
                if (resultCode == android.app.Activity.RESULT_OK) {
                    mLocalMediaUri = mCameraUri;
                    mImageView.setImageUrl(mLocalMediaUri.toString(),
                            VolleySingleton.getInstance(getContext()).getImageLoader());
                    mImageView.setOnClickListener(null);
                    mFlags |= FLAG_IS_CHANGED_MEDIA_URI;
                    // TODO copy image to public storage
                }
                // Activity was cancelled, delete the camera image.
                else {
                    deleteLocalMedia(mCameraUri);
                }
                mCameraUri = null;
                break;
            case REQUEST_CODE_GET_VIDEO:
                if (resultCode == android.app.Activity.RESULT_OK) {
                    mLocalMediaUri = data.getData();
                    // TODO show play button icon
                    mImageView.setOnClickListener(new ImageViewOnClickListener());
                    mFlags |= FLAG_IS_CHANGED_MEDIA_URI;
                }
                break;
            case REQUEST_CODE_MEDIA_BROWSER:
                // Copy the selected file to private storage.
                if (resultCode == android.app.Activity.RESULT_OK) {
                    // Get the URI to work on.
                    new CopyAndDisplayImageTask(data.getData(), mLocalMediaUri).execute();
                }
                break;
            case REQUEST_CODE_PLACE:
                if (resultCode == android.app.Activity.RESULT_OK) {
                    final Place place = PlaceAutocomplete.getPlace(getContext(), data);
                    mLocation = place.getAddress().toString();
                    setLocationText();
                    final LatLng latLng = place.getLatLng();
                    mLatitude = latLng.latitude;
                    mLongitude = latLng.longitude;
                    // Update menu to show the "save" menu item.
                    final Menu menu = ((Toolbar) getView().findViewById(R.id.toolbar)).getMenu();
                    menu.findItem(R.id.action_save).setVisible(true);
                    menu.findItem(R.id.action_editLocation)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    mFlags |= FLAG_IS_CHANGED_LOCATION;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.spot_editor_fragment, container, false);
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_HAS_MEDIA:
            case DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_NO_MEDIA:
            case DIALOG_TYPE_GET_MEDIA_NO_CAMERA_HAS_MEDIA:
                if (action == JournalDialogFragment.DIALOG_ACTION_CONTENT_CLICK) {
                    // Retrieve and set the requested image.
                    final int choice = data.getIntExtra(JournalDialogFragment.DIALOG_DATA, -1);
                    switch (mImageSelectMap.get(choice)) {
                        case DIALOG_CHOICE_IMAGE_LIBRARY:
                            // Check for "read external storage" permission and start image browser.
                            if (ContextCompat.checkSelfPermission(getContext(),
                                    Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_GRANTED) {
                                getImageFromLibrary();
                            } else {
                                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                                        REQUEST_CODE_GET_IMAGE);
                            }
                            break;
                        case DIALOG_CHOICE_VIDEO_LIBRARY:
                            // Check for "read external storage" permission and start image browser.
                            if (ContextCompat.checkSelfPermission(getContext(),
                                    Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_GRANTED) {
                                getVideoFromLibrary();
                            } else {
                                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                                        REQUEST_CODE_GET_VIDEO);
                            }
                            break;
                        case DIALOG_CHOICE_RECORD_IMAGE:
                            dispatchTakePictureIntent();
                            break;
                        case DIALOG_CHOICE_RECORD_VIDEO:
                            dispatchRecordVideoIntent();
                            break;
                        case DIALOG_CHOICE_DELETE_MEDIA:
                            mFlags |= FLAG_IS_CHANGED_MEDIA_URI;
                            deleteLocalMedia(mLocalMediaUri);
                            mLocalMediaUri = null;
                            mImageView.setImageUrl(null, null);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    mImageSelectMap = null;
                }
                break;
            case DIALOG_TYPE_CONFIRM_DISCARD_CHANGES:
                // Clean up stored media and finish activity.
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    deleteLocalMedia(mLocalMediaUri);
                    getActivity().finish();
                }
                break;
            case DIALOG_TYPE_CONFIRM_DELETE:
                // Delete the spot from the provider.
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    // Mark as deleted.
                    mFlags |= FLAG_IS_DELETED;
                    // Remove existing activity from server and provider.
                    final Activity activity = getArguments().getParcelable(Constants.ARG_DATA);
                    if (activity != null) {
                        // Delete spot from the server.
                        final String accountName = AccountUtils.getActiveAccount(getContext());
                        AccountUtils.getAuthToken(getActivity(), AUTH_REASON_DELETE_ACTIVITY,
                                accountName,
                                new AuthTokenListener());
                        // Delete the spot from the provider if it exists.
                        final QueryHandler handler = new QueryHandler(getActivity().
                                getContentResolver());
                        final ContentValues values = new ContentValues(1);
                        values.put(Activities.COLUMN_IS_DELETED, 1);
                        handler.startUpdate(0, null, Activities.CONTENT_URI, values,
                                Activities.COLUMN_SERVER_ID + "=?",
                                new String[]{activity.activityId.toString()});
                        // Notify parent of changes.
                        getActivity().setResult(android.app.Activity.RESULT_OK);
                    }
                    //                    deleteLocalMedia(); TODO delete temporary media in private storage
                    getActivity().finish();
                }
                break;
            case DIALOG_TYPE_ERROR_OPEN_CAMERA:
            case DIALOG_TYPE_ERROR_OPEN_IMAGE:
            case DIALOG_TYPE_ERROR_OPEN_STORAGE:
                // Do nothing.
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_GET_IMAGE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getImageFromLibrary();
                }
                break;
            case REQUEST_CODE_GET_VIDEO:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getVideoFromLibrary();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Only save state if the activity has already finished loading.
        if ((mFlags & FLAG_IS_LOADED) != 0) {
            outState.putInt(SAVE_STATE_FLAGS, mFlags);
            outState.putString(SAVE_STATE_TITLE, mLayoutTitle.getEditText().getText().toString());
            outState.putString(SAVE_STATE_DESCRIPTION,
                    mLayoutDescription.getEditText().getText().toString());
            outState.putString(SAVE_STATE_LOCATION, mLocation);
            outState.putDouble(SAVE_STATE_LATITUDE, mLatitude);
            outState.putDouble(SAVE_STATE_LONGITUDE, mLongitude);
            outState.putParcelable(SAVE_STATE_CAMERA_URI, mCameraUri);
            outState.putParcelable(SAVE_STATE_MEDIA_URI, mLocalMediaUri);
        }
    }

    /**
     * Delete saved media from the content provider.
     *
     * @param uri the URI of the media to delete
     */
    private void deleteLocalMedia(Uri uri) {
        if (uri != null) {
            new QueryHandler(getContext().getContentResolver())
                    .startDelete(0, null, uri, null, null);
        }
    }

    /**
     * Create a file for the photo.
     */
    private void dispatchTakePictureIntent() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent.
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go.
            final File directory = new File(getContext().getFilesDir(),
                    getString(R.string.provider_filePath_images));
            // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); TODO
            // Check that the directory exists.
            if (!directory.exists() && !directory.mkdirs()) {
                showDialog(DIALOG_TYPE_ERROR_OPEN_STORAGE);
            } else {
                File photoFile;
                try {
                    photoFile = ImageUtils.createFile(directory, FILE_EXTENSION_DEFAULT);
                } catch (IOException ex) {
                    photoFile = null;
                }
                // Open the camera to take a picture.
                if (photoFile != null) {
                    mCameraUri = FileProvider.getUriForFile(getContext(),
                            getString(R.string.provider_authority), photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraUri);
                    startActivityForResult(intent, REQUEST_CODE_GET_IMAGE);
                } else {
                    showDialog(DIALOG_TYPE_ERROR_OPEN_STORAGE);
                }
            }
        }
        // The image capture intent could not be fulfilled.
        else {
            showDialog(DIALOG_TYPE_ERROR_OPEN_CAMERA);
        }
    }

    /**
     * Create a file for the photo.
     */
    private void dispatchRecordVideoIntent() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent.
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go.
            final File directory = new File(getContext().getFilesDir(),
                    getString(R.string.provider_filePath_images));
            // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); TODO
            // Check that the directory exists.
            if (!directory.exists() && !directory.mkdirs()) {
                showDialog(DIALOG_TYPE_ERROR_OPEN_STORAGE);
            } else {
                File photoFile;
                try {
                    photoFile = ImageUtils.createFile(directory, FILE_EXTENSION_DEFAULT);
                } catch (IOException ex) {
                    photoFile = null;
                }
                // Open the camera to take a picture.
                if (photoFile != null) {
                    mCameraUri = FileProvider.getUriForFile(getContext(),
                            getString(R.string.provider_authority), photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraUri);
                    startActivityForResult(intent, REQUEST_CODE_GET_IMAGE);
                } else {
                    showDialog(DIALOG_TYPE_ERROR_OPEN_STORAGE);
                }
            }
        }
        // The image capture intent could not be fulfilled.
        else {
            showDialog(DIALOG_TYPE_ERROR_OPEN_CAMERA);
        }
    }

    /**
     * Set whether the user input fields are enabled.
     *
     * @param isEnabled {@code true} if user input fields are enabled, {@code false} otherwise
     */
    private void setEnabled(boolean isEnabled) {
        // Set the "title" text.
        mLayoutTitle.setEnabled(isEnabled);
        // Set the "description" text.
        mLayoutDescription.setEnabled(isEnabled);
        // Set the floating action button.
        final FloatingActionButton button = getView().findViewById(R.id.floatingButton);
        if (isEnabled) {
            button.show();
        } else {
            button.hide();
        }
    }

    /**
     * Get an existing image from the device.
     */
    private void getImageFromLibrary() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(Images.CONTENT_TYPE_IMAGE);
        startActivityForResult(
                Intent.createChooser(intent, getString(R.string.label_chooser_selectImage)),
                REQUEST_CODE_MEDIA_BROWSER);
    }

    /**
     * Get an existing video from the device.
     */
    private void getVideoFromLibrary() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(Images.CONTENT_TYPE_VIDEO);
        startActivityForResult(
                Intent.createChooser(intent, getString(R.string.label_chooser_selectVideo)),
                REQUEST_CODE_MEDIA_BROWSER);
    }

    /**
     * Request a system permission.
     *
     * @param permission  the permission requested
     * @param requestCode code used to identify permission request in {@link
     *                    #onRequestPermissionsResult(int, String[], int[])}
     */
    private void requestPermission(String permission, int requestCode) {
        // Request permission to read external storage, if required.
        if (ContextCompat
                .checkSelfPermission(getContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) {
            // Show an explanation of why access is required.
            if (shouldShowRequestPermissionRationale(permission)) {
                // TODO Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                requestPermissions(new String[]{permission}, requestCode);
            } else {
                requestPermissions(new String[]{permission}, requestCode);
            }
        }
    }

    /**
     * Load media.
     */
    private void loadMedia() {
        // Get the media URI to load.
        final Uri mediaUri;
        if (mLocalMediaUri != null) {
            mediaUri = mLocalMediaUri;
        } else {
            final Activity activity = getArguments().getParcelable(Constants.ARG_DATA);
            if ((activity != null) && (activity.media != null) && (activity.media.size() > 0)) {
                mediaUri = Uri.parse(activity.media.get(0).path);
            } else {
                mediaUri = null;
            }
        }
        // Determine action to perform based on media mime type.
        if (mediaUri != null) {
            // URI is a video.
            final String mimeType = ImageUtils.getMimeType(getContext(), mediaUri);
            if ((mimeType != null) && mimeType.startsWith("video/")) {
                mImageView.setOnClickListener(new ImageViewOnClickListener());
                mImageView.setImageUrl(null, null);
            }
            // Uri is (hopefully) an image. TODO better way of determining image
            else {
                mImageView.setOnClickListener(null);
                mImageView.setImageUrl(mediaUri.toString(),
                        VolleySingleton.getInstance(getContext()).getImageLoader());
            }
        }
        // There is no media to show.
        else {
            mImageView.setOnClickListener(null);
            mImageView.setImageUrl(null, null);
        }

    }

    /**
     * Set the location text.
     */
    private void setLocationText() {
        final TextView location = getView().findViewById(R.id.text_location);
        if (TextUtils.isEmpty(mLocation)) {
            location.setText(R.string.defaultText_location);
        } else {
            location.setText(mLocation);
        }
    }

    /**
     * Setup text changed listeners.
     */
    private void setupTextChangedListeners() {
        mLayoutTitle.getEditText()
                .addTextChangedListener(new OnTextChangedListener(FLAG_IS_CHANGED_TITLE));
        mLayoutDescription.getEditText()
                .addTextChangedListener(new OnTextChangedListener(FLAG_IS_CHANGED_DESCRIPTION));
    }

    /**
     * Create and show a dialog of the specified type.
     * <p/>
     * {@link #onDialogResult(int, int, Intent)} will be called with the dialog result.
     *
     * @param dialogType the type of dialog to display
     */
    private void showDialog(int dialogType) {
        final JournalDialogFragment dialog;
        switch (dialogType) {
            case DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_HAS_MEDIA:
                dialog = JournalDialogFragment.newChoiceDialog(dialogType,
                        R.string.dialog_message_updateImage,
                        R.array.dialog_updateImage_hasCamera_hasMedia);
                mImageSelectMap = new SparseIntArray(5);
                mImageSelectMap.append(0, DIALOG_CHOICE_IMAGE_LIBRARY);
                mImageSelectMap.append(1, DIALOG_CHOICE_VIDEO_LIBRARY);
                mImageSelectMap.append(2, DIALOG_CHOICE_RECORD_IMAGE);
                mImageSelectMap.append(3, DIALOG_CHOICE_RECORD_VIDEO);
                mImageSelectMap.append(4, DIALOG_CHOICE_DELETE_MEDIA);
                break;
            case DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_NO_MEDIA:
                dialog = JournalDialogFragment.newChoiceDialog(dialogType,
                        R.string.dialog_message_updateImage,
                        R.array.dialog_updateImage_hasCamera_noMedia);
                mImageSelectMap = new SparseIntArray(4);
                mImageSelectMap.append(0, DIALOG_CHOICE_IMAGE_LIBRARY);
                mImageSelectMap.append(1, DIALOG_CHOICE_VIDEO_LIBRARY);
                mImageSelectMap.append(2, DIALOG_CHOICE_RECORD_IMAGE);
                mImageSelectMap.append(3, DIALOG_CHOICE_RECORD_VIDEO);
                break;
            case DIALOG_TYPE_GET_MEDIA_NO_CAMERA_HAS_MEDIA:
                dialog = JournalDialogFragment.newChoiceDialog(dialogType,
                        R.string.dialog_message_updateImage,
                        R.array.dialog_updateImage_noCamera_hasMedia);
                mImageSelectMap = new SparseIntArray(3);
                mImageSelectMap.append(0, DIALOG_CHOICE_IMAGE_LIBRARY);
                mImageSelectMap.append(1, DIALOG_CHOICE_VIDEO_LIBRARY);
                mImageSelectMap.append(2, DIALOG_CHOICE_DELETE_MEDIA);
                break;
            case DIALOG_TYPE_CONFIRM_DELETE:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_deleteSpot, R.string.dialog_positiveButton_delete,
                        true);
                break;
            case DIALOG_TYPE_CONFIRM_DISCARD_CHANGES:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_discardChanges,
                        R.string.dialog_positiveButton_discard, true);
                break;
            case DIALOG_TYPE_ERROR_OPEN_CAMERA:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_error_openCamera,
                        R.string.dialog_positiveButton_ok, false);
                break;
            case DIALOG_TYPE_ERROR_OPEN_IMAGE:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_error_openImage,
                        R.string.dialog_positiveButton_ok, false);
                break;
            case DIALOG_TYPE_ERROR_OPEN_STORAGE:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_error_externalStorage,
                        R.string.dialog_positiveButton_ok, false);
                break;
            default:
                throw new IllegalArgumentException();
        }
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Create and show a message dialog.
     */
    private void showMessageDialog(@NonNull String message) {
        final JournalDialogFragment dialog = JournalDialogFragment.newMessageDialog(0, message,
                R.string.dialog_positiveButton_ok, false);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Save updated data to the provider.
     */
    private void updateProvider() {
        // Prevent saving if spot has been deleted.
        if ((mFlags & FLAG_IS_DELETED) != FLAG_IS_DELETED) {
            final ContentValues values = new ContentValues(3);
            // Save the updated title.
            if ((mFlags & FLAG_IS_CHANGED_TITLE) == FLAG_IS_CHANGED_TITLE) {
                final EditText title = getView().findViewById(R.id.text_title);
                if (!TextUtils.isEmpty(title.getText())) {
                    values.put(Activities.COLUMN_TITLE, title.getText().toString());
                }
                // If the text is empty, only save if updating an existing entry.
                else if (mIsUpdating) {
                    values.putNull(Activities.COLUMN_TITLE);
                }
            }
            // Save the updated description.
            if ((mFlags & FLAG_IS_CHANGED_DESCRIPTION) == FLAG_IS_CHANGED_DESCRIPTION) {
                final EditText description = getView().findViewById(R.id
                        .text_description);
                if (!TextUtils.isEmpty(description.getText())) {
                    values.put(Activities.COLUMN_DESCRIPTION,
                            description.getText().toString());
                }
                // If the text is empty, only save if updating an existing entry.
                else if (mIsUpdating) {
                    values.putNull(Activities.COLUMN_DESCRIPTION);
                }
            }
            // Save the updated image URI.
            if ((mFlags & FLAG_IS_CHANGED_MEDIA_URI) == FLAG_IS_CHANGED_MEDIA_URI) {
                if (mLocalMediaUri != null) {
                    values.put(Activities.COLUMN_IMAGE_URI, mLocalMediaUri.toString());
                } else {
                    values.putNull(Activities.COLUMN_IMAGE_URI);
                }
            }
            // Update the content provider if any values have changed.
            //            if (values.size() > 0) { TODO
            //                final QueryHandler handler = new QueryHandler(getActivity().getContentResolver());
            //                if (mIsUpdating) {
            //                    handler.startUpdate(0, null, mContentUri, values, null, null);
            //                } else {
            //                    handler.startInsert(0, null, mContentUri, values);
            //                }
            //            }
        }
    }

    /**
     * TODO
     */
    private class UploadDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            final NetworkUtils.CreateActivityRequest request =
                    new NetworkUtils.CreateActivityRequest();
            HttpURLConnection connection = null;
            try {
                connection =
                        (HttpURLConnection) new URL(request.getUrl(getContext())).openConnection();
                connection.setDoOutput(true);
                connection.setChunkedStreamingMode(0);
                final Map<String, String> headers =
                        NetworkUtils.getDataTransferHeaders(getContext(), params[0]);
                headers.put("Content-Type", "application/json; charset=utf-8");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }

                // *****Write file to output.***** //
                final OutputStream outStream = connection.getOutputStream();
                //                final OutputStream outStream = getContext().getContentResolver()
                //                        .openOutputStream(Uri.parse(Images.CONTENT_URI + "/test.txt"));
                final BufferedWriter bufferedWriter =
                        new BufferedWriter(new OutputStreamWriter(outStream));
                final JsonWriter writer = new JsonWriter(bufferedWriter);
                writer.beginObject();
                writer.name("journal_id").value(getArguments().getLong(Constants.ARG_JOURNAL_ID));
                writer.name("activity_title").value(params[1]);
                writer.name("activity_description").value(params[2]);
                writer.name("activity_location").value(TextUtils.isEmpty(mLocation) ?
                        getString(R.string.defaultText_location) : mLocation);
                writer.name("activity_loc_latitude").value(mLatitude);
                writer.name("activity_loc_longitude").value(mLongitude);
                final Activity activity = getArguments().getParcelable(Constants.ARG_DATA);
                if (activity != null) {
                    writer.name("activity_id").value(activity.activityId);
                }
                // Upload new media.
                if (mLocalMediaUri != null) {
                    bufferedWriter.write(",\"activity_file_data\":\"");
                    bufferedWriter.flush();

                    Base64OutputStream b64Out = new Base64OutputStream(outStream,
                            Base64.NO_CLOSE | Base64.NO_WRAP);
                    final InputStream input =
                            getContext().getContentResolver().openInputStream(mLocalMediaUri);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        b64Out.write(buffer, 0, bytesRead);
                    }
                    input.close();
                    b64Out.close();

                    bufferedWriter.write("\"");
                }
                // Media did not change.
                else if ((mFlags & FLAG_IS_CHANGED_MEDIA_URI) == 0) {
                    writer.name("activity_file_data").value("FALSE");
                } else {
                    writer.name("activity_file_data").value("");
                }
                writer.endObject();
                writer.close();

                // *****Get server response***** //
                Log.d(DEBUG_TAG, "Checking response...");
                // Get the response message.
                final BufferedReader reader = new BufferedReader(new InputStreamReader(connection
                        .getInputStream()));
                Log.d(DEBUG_TAG, "Response Code: " + connection.getResponseCode());
                Log.d(DEBUG_TAG, "Headers: " + connection.getHeaderFields().toString());
                final StringBuilder builder = new StringBuilder();
                String result;
                while ((result = reader.readLine()) != null) {
                    builder.append(result);
                }
                connection.disconnect();
                return builder.toString();
            } catch (IOException exception) {
                if (connection != null) {
                    Log.i(DEBUG_TAG, "Exception: " + exception.getMessage());
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            final CreateActivityResponseListener listener = new CreateActivityResponseListener();
            if (result != null) {
                final CreateActivityResponse response =
                        new Gson().fromJson(result, CreateActivityResponse.class);
                listener.onResponse(response);
            } else {
                listener.onErrorResponse(new VolleyError()); // TODO better error
            }
        }

    }

    /**
     * Class for transferring data with the server after receiving an auth token.
     */
    private class AuthTokenListener implements AccountUtils.AuthTokenListener {

        @Override
        public void onAuthTokenReceived(int requestCode, String accountName, String authToken) {
            if (isStarted()) {
                final GsonRequest request;
                final Activity activity = getArguments().getParcelable(Constants.ARG_DATA);
                switch (requestCode) {
                    case AUTH_REASON_CREATE_ACTIVITY:
                        new UploadDataTask()
                                .execute(authToken, mLayoutTitle.getEditText().getText().toString(),
                                        mLayoutDescription.getEditText().getText().toString());
                        //                        final CreateActivityRequest createRequest = new CreateActivityRequest();
                        //                        createRequest.journalId = getArguments().getLong(Constants.ARG_JOURNAL_ID);
                        //                        createRequest.title = mLayoutTitle.getEditText().getText().toString();
                        //                        createRequest.description =
                        //                                mLayoutDescription.getEditText().getText().toString();
                        //                        createRequest.location = TextUtils.isEmpty(mLocation) ?
                        //                                getString(R.string.label_default_location) : mLocation;
                        //                        createRequest.latitude = 1;//mLatitude;
                        //                        createRequest.longitude = 1;//mLongitude;
                        //                        createRequest.fileData = "FALSE"; // TODO
                        //                        if (activity != null) {
                        //                            createRequest.activityId = activity.activityId;
                        //                        }
                        //                        final CreateActivityResponseListener createListener =
                        //                                new CreateActivityResponseListener(getActivity(), requestCode,
                        //                                        email, this, authToken);
                        //                        request = new GsonRequest<>(createRequest.getUrl(getContext()),
                        //                                CreateActivityResponse.class,
                        //                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                        //                                new Gson().toJson(createRequest), createListener, createListener);
                        break;
                    case AUTH_REASON_DELETE_ACTIVITY:
                        final DeleteJournalRequest deleteRequest = new DeleteJournalRequest();
                        deleteRequest.id = getArguments().getLong(Constants.ARG_JOURNAL_ID);
                        deleteRequest.activityId = activity.activityId;
                        final DeleteActivityResponseListener deleteListener =
                                new DeleteActivityResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(deleteRequest.getUrl(getContext()),
                                DeleteJournalResponse.class,
                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(deleteRequest), deleteListener, deleteListener);
                        startRequest(request);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                //                startRequest(request); TODO
            }
        }

        @Override
        public void onAuthenticatorError(int requestCode) { // TODO
        }

        @Override
        public void onOperationCancelled(int requestCode) {
        }

        @Override
        public void onIOError(int requestCode) {
        }

    }

    /**
     * Copy a file from an external location into the provider and display in the spot.
     */
    private class CopyAndDisplayImageTask extends AsyncTask<Void, Void, Uri> {

        /**
         * The URI of the current file for this spot.
         */
        private final Uri mOldFileUri;
        /**
         * The source URI for the new file to copy into the provider.
         */
        private final Uri mSourceUri;

        public CopyAndDisplayImageTask(Uri sourceUri, Uri oldFileUri) {
            mSourceUri = sourceUri;
            mOldFileUri = oldFileUri;
        }

        @Override
        protected Uri doInBackground(Void... params) {
            // Copy the source file to the destination directory.
            final ContentResolver resolver = getActivity().getContentResolver();
            final File destination = new File(getActivity().getFilesDir(),
                    getString(R.string.provider_filePath_images));
            final File copyFile = ImageUtils.copyFileFromUri(mSourceUri, resolver, destination);

            // *****Update journal spot.***** //
            if (copyFile != null) {
                // Get new image URI.
                final Uri newFileUri = FileProvider.getUriForFile(getActivity(),
                        JournalContract.AUTHORITY, copyFile);
                // Delete old file from the provider.
                if ((mOldFileUri != null) && "content".equals(mOldFileUri.getScheme())) {
                    resolver.delete(mOldFileUri, null, null);
                }
                return newFileUri;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri mediaUri) {
            // The file was successfully added to the provider.
            if (mediaUri != null) {
                // Delete the existing
                mLocalMediaUri = mediaUri;
                mFlags |= FLAG_IS_CHANGED_MEDIA_URI;
                loadMedia();
            }
            // The file creation was unsuccessful.
            else {
                showDialog(DIALOG_TYPE_ERROR_OPEN_IMAGE);
            }
        }

    }

    /**
     * Class for processing "create activity" responses.
     */
    private class CreateActivityResponseListener extends AuthFailureErrorListener
            implements Response.Listener<CreateActivityResponse> {

        /**
         * Create a new response listener.
         */
        public CreateActivityResponseListener() {
            super();
        }

        /**
         * Create a new response listener that automatically handles auth failure errors.
         *
         * @param activity    activity used to access resources and launch a user credentials
         *                    dialog, if required
         * @param requestCode request code used to identify the request
         * @param accountName the name of the account
         * @param listener    listener for receiving auth token information
         * @param authToken   the auth token to invalidate in the event of an auth failure error
         */
        public CreateActivityResponseListener(@NonNull android.app.Activity activity,
                                              int requestCode, @NonNull String accountName,
                                              @NonNull AuthTokenListener listener,
                                              @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(CreateActivityResponse response) {
            getActivity().finish();
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showMessageDialog(message);
            }
        }

    }

    /**
     * Class for processing "delete activity" responses.
     */
    private class DeleteActivityResponseListener extends AuthFailureErrorListener
            implements Response.Listener<DeleteJournalResponse> {

        /**
         * Create a new response listener that automatically handles auth failure errors.
         *
         * @param activity    activity used to access resources and launch a user credentials
         *                    dialog, if required
         * @param requestCode request code used to identify the request
         * @param accountName the name of the account
         * @param listener    listener for receiving auth token information
         * @param authToken   the auth token to invalidate in the event of an auth failure error
         */
        public DeleteActivityResponseListener(@NonNull android.app.Activity activity,
                                              int requestCode, @NonNull String accountName,
                                              @NonNull AuthTokenListener listener,
                                              @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(DeleteJournalResponse response) {
            getActivity().finish();
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showMessageDialog(message);
            }
        }

    }

    /**
     * Class for listening for floating action button clicks.
     */
    private class FloatingButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // Determine dialog to show based on presence of camera and media.
            if ((mFlags & FLAG_IS_LOADED) != 0) {
                // A camera and intent are available.
                if (isCameraAndIntentAvailable(getActivity())) {
                    // An image is already present.
                    if (mLocalMediaUri != null) {
                        showDialog(DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_HAS_MEDIA);
                    }
                    // There is no media already present.
                    else {
                        showDialog(DIALOG_TYPE_GET_MEDIA_HAS_CAMERA_NO_MEDIA);
                    }
                }
                // There is no camera and/or intent available.
                else {
                    // An image is already present.
                    if (mLocalMediaUri != null) {
                        showDialog(DIALOG_TYPE_GET_MEDIA_NO_CAMERA_HAS_MEDIA);
                    }
                    // No image is already present.
                    else {
                        getImageFromLibrary();
                    }
                }
            }
        }

    }

    /**
     * Listener for image view clicks.
     */
    private class ImageViewOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            final Intent intent = new Intent(getContext(), MediaActivity.class);
            intent.setData(mLocalMediaUri);
            startActivity(intent);
        }

    }

    /**
     * Listener for toolbar navigation icon clicks.
     */
    private class NavigationOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Show the confirm discard changes dialog.
            if ((mFlags &= ~FLAG_IS_LOADED) > 0) {
                showDialog(DIALOG_TYPE_CONFIRM_DISCARD_CHANGES);
            }
            // Finish the activity if nothing to save.
            else {
                getActivity().finish();
            }
        }

    }

    /**
     * Listener for toolbar menu item clicks.
     */
    private class OnMenuItemClickListener implements Toolbar.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_save:
                    boolean hasError = false;
                    // Check for proper "title" text input.
                    final EditText title = mLayoutTitle.getEditText();
                    if (TextUtils.isEmpty(title.getText())) {
                        final String error = getString(R.string.userInput_error_requiredField);
                        mLayoutTitle.setError(error);
                        hasError = true;
                    } else {
                        mLayoutTitle.setErrorEnabled(false);
                    }
                    // Check for proper "description" text input.
                    final EditText description = mLayoutDescription.getEditText();
                    if (TextUtils.isEmpty(description.getText())) {
                        final String error = getString(R.string.userInput_error_requiredField);
                        mLayoutDescription.setError(error);
                        hasError = true;
                    } else {
                        mLayoutDescription.setErrorEnabled(false);
                    }
                    // Check for location information.
                    if (TextUtils.isEmpty(mLocation)) {
                        showMessageDialog("No Location Specified.");// TODO
                        hasError = true;
                    }
                    // Save updated data to the server.
                    if (!hasError) {
                        if (((mFlags & FLAG_IS_CHANGED_TITLE) != 0) || (
                                ((mFlags & FLAG_IS_CHANGED_DESCRIPTION) != 0)) ||
                                ((mFlags & FLAG_IS_CHANGED_LOCATION) != 0) ||
                                ((mFlags & FLAG_IS_CHANGED_MEDIA_URI) != 0)) {
                            final String accountName = AccountUtils.getActiveAccount(getContext());
                            AccountUtils.getAuthToken(getActivity(), AUTH_REASON_CREATE_ACTIVITY,
                                    accountName, new AuthTokenListener());
                        }
                        // Finish the activity if nothing to save.
                        else {
                            getActivity().finish();
                        }
                    }
                    return true;
                case R.id.action_editLocation:
                    try {
                        Intent intent = new PlaceAutocomplete.IntentBuilder(
                                PlaceAutocomplete.MODE_OVERLAY).build(getActivity());
                        startActivityForResult(intent, REQUEST_CODE_PLACE);
                    } catch (GooglePlayServicesRepairableException e) {
                        // Show error dialog allowing user to enable Google Play services.
                        GoogleApiAvailability.getInstance()
                                .showErrorDialogFragment(getActivity(), e.getConnectionStatusCode(),
                                        0);
                    } catch (GooglePlayServicesNotAvailableException e) {
                        // Show error dialog allowing user to enable Google Play services.
                        GoogleApiAvailability.getInstance()
                                .showErrorDialogFragment(getActivity(), e.errorCode, 0);
                    }
                    return true;
                case R.id.action_discardChanges:
                    // Show the confirm discard changes dialog.
                    if ((mFlags &= ~FLAG_IS_LOADED) > 0) {
                        showDialog(DIALOG_TYPE_CONFIRM_DISCARD_CHANGES);
                    }
                    // Finish the activity if nothing to save.
                    else {
                        getActivity().finish();
                    }
                    return true;
                case R.id.action_delete:
                    // Show the confirm delete dialog.
                    showDialog(DIALOG_TYPE_CONFIRM_DELETE);
                    return true;
                default:
                    return false;
            }
        }

    }

    /**
     * Class used for listening for spot queries on the content provider.
     */
    private class QueryListener extends SimpleQueryListener {

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // Use data in the provider to populate the UI.
            if (cursor.moveToFirst()) {
                final String title =
                        cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_TITLE));
                mLayoutTitle.getEditText().setText(title);
                final String description = cursor.getString(
                        cursor.getColumnIndexOrThrow(Activities.COLUMN_DESCRIPTION));
                mLayoutDescription.getEditText().setText(description);
                mLocation =
                        cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_LOCATION));
                mLatitude =
                        cursor.getDouble(cursor.getColumnIndexOrThrow(Activities.COLUMN_LATITUDE));
                mLongitude =
                        cursor.getDouble(cursor.getColumnIndexOrThrow(Activities.COLUMN_LONGITUDE));
                final String uriString =
                        cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_IMAGE_URI));
                if (uriString != null) {
                    mLocalMediaUri = Uri.parse(uriString);
                }
            }
            // Use data from the passed in activity.
            else {
                final Activity activity = getArguments().getParcelable(Constants.ARG_DATA);
                mLayoutTitle.getEditText().setText(activity.title);
                mLayoutDescription.getEditText().setText(activity.description);
                mLocation = activity.location;
                setLocationText();
                mLatitude = activity.latitude;
                mLongitude = activity.longitude;
            }
            loadMedia();
            setupTextChangedListeners();
            setEnabled(true);
            mFlags |= FLAG_IS_LOADED;
        }

    }

    /**
     * Class used for listening for text changes.
     */
    private class OnTextChangedListener implements TextWatcher {

        /**
         * The flag this listener will set when it receives text changes.
         */
        private final int mFlag;

        /**
         * Create a new text changed listener.
         *
         * @param flag the flag this listener will set when it receives text changes
         */
        public OnTextChangedListener(int flag) {
            mFlag = flag;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mFlags |= mFlag;
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

    }

}