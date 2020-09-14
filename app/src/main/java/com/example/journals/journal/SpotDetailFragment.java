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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.journals.journal.JournalDialogFragment.DialogListener;
import com.example.journals.R;
import com.example.journals.image.ImageLoaderOld;
import com.example.journals.network.NetworkUtils.Activity;
import com.example.journals.network.VolleySingleton;
import com.example.journals.provider.JournalContract.Activities;
import com.example.journals.provider.QueryHandler;
import com.example.journals.widget.SizeListenerImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

/**
 * A class for editing spot details and associated data.
 * <p/>
 * If creating a new spot, this fragment takes a content URI of type {@link
 * Activities#CONTENT_TYPE_DIR}. If updating an existing spot, this fragment takes an content URI of
 * type {@link Activities#CONTENT_TYPE_ITEM}.
 */
public class SpotDetailFragment extends Fragment implements DialogListener {

    /**
     * Dialog type for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_CONFIRM_DELETE = 0;
    /**
     * Tag for dialogs.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Projection for building a cursor loader.
     */
    private static final String[] QUERY_PROJECTION =
            new String[]{Activities.COLUMN_ID, Activities.COLUMN_TITLE,
                    Activities.COLUMN_DESCRIPTION, Activities.COLUMN_LOCATION,
                    Activities.COLUMN_LATITUDE, Activities.COLUMN_LONGITUDE,
                    Activities.COLUMN_IMAGE_URI};

    /**
     * The URI of the media associated with this spot.
     */
    private Uri mMediaUri = null;
    /**
     * The image view used to display the image associated with this spot.
     */
    private SizeListenerImageView mImageView;

    /**
     * Load an image from the specified URI to the specified image view.
     *
     * @param context   the context used to access resources
     * @param imageUri  the URI of the image to load
     * @param imageView the image void to load the image into; the image will not be loaded if
     *                  {@code imageView} has not already been laid out (width/height == 0)
     */
    private static void loadImage(@NonNull Context context, @Nullable Uri imageUri,
                                  @NonNull ImageView imageView) {
        final Drawable drawable = context.getResources().getDrawable(R.mipmap.ic_launcher);
        // If view is already sized, load image immediately.
        if (imageUri != null) {
            if ((imageView.getWidth() > 0) && (imageView.getHeight() > 0)) {
                ImageLoaderOld.loadImage(context, imageUri, imageView, drawable, null, true);
            }
        }
        // Clear the image from the image view.
        else {
            imageView.setImageDrawable(drawable);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Bundle args = getArguments();

        // *****Set up toolbar.***** //
        final Toolbar toolbar = getView().findViewById(R.id.toolbar);
        // Set home navigation icon.
        toolbar.setNavigationIcon(args.getInt(Constants.ARG_HOME_BUTTON_IMAGE));
        toolbar.setNavigationOnClickListener(new NavigationOnClickListener());

        // *****Get reference to the image view.***** //
        mImageView = getView().findViewById(R.id.image);

        // *****Set up fragment data.***** //
        switch (args.getInt(Constants.ARG_DATA_TYPE)) {
            case Constants.DATA_TYPE_CONTENT_URI:
                // Set menu items.
                toolbar.inflateMenu(R.menu.menu_spot_detail);
                toolbar.setOnMenuItemClickListener(new MenuItemClickListener());
                // Add listener to the image view.
                mImageView.setOnSizeChangedListener(new SpotImageSizeChangedListener());
                // Load data from the content provider.
                getLoaderManager().initLoader(0, null, new LoaderCallbacks());
                break;
            case Constants.DATA_TYPE_PARCELED_OBJECT:
                final Activity activity = args.getParcelable(Constants.ARG_DATA);
                // Set menu items.
                if (activity.latitude != 0 && activity.longitude != 0) {
                    toolbar.inflateMenu(R.menu.menu_spot_detail_readonly);
                    toolbar.setOnMenuItemClickListener(new MenuItemClickListener());
                }
                // Set the content.
                String mediaUri = null;
                if (activity.media.size() >= 1) {
                    mediaUri = activity.media.get(0).path;
                }
                setTextContent(activity.title, activity.description, activity.location);
                mImageView.setImageUrl(mediaUri,
                        VolleySingleton.getInstance(getActivity()).getImageLoader());
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.spot_detail_fragment, container, false);
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_DELETE:
                // Delete the spot from the provider.
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    final QueryHandler handler =
                            new QueryHandler(getActivity().getContentResolver());
                    final Uri contentUri = getArguments().getParcelable(Constants.ARG_DATA);
                    handler.startDelete(0, null, contentUri, null, null);
                    // Close the activity.
                    getActivity().finish();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Set the text content for the fragment.
     *
     * @param title       the title text
     * @param description the description text
     * @param location    the location text
     */
    private void setTextContent(String title, String description, String location) {
        final View view = getView();
        // Title text.
        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(
                (!TextUtils.isEmpty(title)) ? title : getString(R.string.defaultText_title));
        // Description text.
        final TextView descriptionText = view.findViewById(R.id.text_description);
        descriptionText.setText(!TextUtils.isEmpty(description) ? description :
                getString(R.string.defaultText_description));
        // Location text.
        final TextView locationText = view.findViewById(R.id.text_location);
        locationText.setText(!TextUtils.isEmpty(location) ? location :
                getString(R.string.defaultText_location));
    }

    /**
     * /** Create and show a dialog of the specified type.
     * <p/>
     * {@link #onDialogResult(int, int, Intent)} will be called with the dialog result.
     *
     * @param dialogType the type of dialog to display
     */
    private void showDialog(int dialogType) {
        final JournalDialogFragment dialog;
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_DELETE:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_deleteSpot, R.string.dialog_positiveButton_delete,
                        true);
                break;
            default:
                throw new IllegalArgumentException();
        }
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Callback for interacting with a cursor loader manager.
     */
    private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final Uri contentUri = getArguments().getParcelable(Constants.ARG_DATA);
            return new CursorLoader(getActivity(), contentUri, QUERY_PROJECTION, null, null, null);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            if (data.moveToFirst()) {
                final String title = data.getString(data.getColumnIndexOrThrow(
                        Activities.COLUMN_TITLE));
                final String description =
                        data.getString(data.getColumnIndexOrThrow(Activities.COLUMN_DESCRIPTION));
                final String location =
                        data.getString(data.getColumnIndexOrThrow(Activities.COLUMN_LOCATION));
                final String uriString =
                        data.getString(data.getColumnIndexOrThrow(Activities.COLUMN_IMAGE_URI));
                // Set up the fragment text.
                setTextContent(title, description, location);
                // Load the media.
                mMediaUri = (!TextUtils.isEmpty(uriString)) ? Uri.parse(uriString) : null;
                loadImage(getActivity(), mMediaUri, mImageView);
            }
            // The item has been deleted from the provider.
            else {
                getActivity().finish();
            }
        }

    }

    /**
     * Listener for toolbar menu item clicks.
     */
    private class MenuItemClickListener implements Toolbar.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    // TODO
                    return true;
                case R.id.action_showMap:
                    final double latitude, longitude;
                    final String location;
                    switch (getArguments().getInt(Constants.ARG_DATA_TYPE)) {
                        case Constants.DATA_TYPE_CONTENT_URI: // TODO
                            latitude = 0;
                            longitude = 0;
                            location = "Treasure";
                            break;
                        case Constants.DATA_TYPE_PARCELED_OBJECT:
                            final Activity activity =
                                    getArguments().getParcelable(Constants.ARG_DATA);
                            latitude = activity.latitude;
                            longitude = activity.longitude;
                            location = activity.location;
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    final Intent intent = new Intent(Intent.ACTION_VIEW,
                            //                            Uri.parse("geo:0,0?q=" + latitude + "," + longitude + "(" + location +
                            //                                    ")&z=1"));
                            Uri.parse("geo:0,0?q=" + location.replace(" ", "+")));
                    startActivity(intent);
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
     * Listener for toolbar navigation icon clicks.
     */
    private class NavigationOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            getActivity().finish();
        }

    }

    /**
     * Class used for reloading the spot image when the image view's size changes.
     */
    private class SpotImageSizeChangedListener implements SizeListenerImageView
            .OnSizeChangedListener {

        @Override
        public void onSizeChanged(SizeListenerImageView view, int w, int h, int oldw, int oldh) {
            loadImage(getActivity(), mMediaUri, mImageView);
        }

    }

}