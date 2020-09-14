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
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Intents;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.example.journals.journal.JournalDialogFragment.DialogListener;
import com.example.journals.widget.MediaActivity;
import com.example.journals.R;
import com.example.journals.journal.JournalDetailFragment.OnPageStatusChangedListener;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.Contact;
import com.example.journals.network.NetworkUtils.Media;
import com.example.journals.network.VolleySingleton;
import com.example.journals.provider.JournalContract.Activities;
import com.example.journals.provider.JournalContract.Contacts;
import com.example.journals.widget.CheckableImageView;
import com.example.journals.widget.CursorAdapter;
import com.example.journals.widget.RecyclerViewFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import static com.example.journals.R.id.action_1;
import static com.example.journals.R.id.action_2;
import static com.example.journals.R.id.text_description;
import static com.example.journals.R.id.text_subtitle1;
import static com.example.journals.R.id.text_subtitle2;
import static com.example.journals.R.id.text_title;

/**
 * A fragment representing the contents of a spot or local contact directory. This fragment is
 * contained in a {@link JournalDetailFragment}.
 * <p/>
 * This fragment takes an argument URI of type {@link Activities#CONTENT_TYPE_DIR} or {@link
 * Contacts#CONTENT_TYPE_DIR}.
 */
public class EntryListFragment extends RecyclerViewFragment
        implements OnPageStatusChangedListener, DialogListener {

    /**
     * Dialog type for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_CONFIRM_DELETE = 0, DIALOG_TYPE_INSERT_FAILED = 1;
    /**
     * Tag for dialogs.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Set whether items in the adapter are editable.
     */
    private boolean mIsEditable;
    /**
     * The action mode that the fragment is currently in.
     */
    private ActionMode mActionMode = null;
    /**
     * Set whether the fragment is currently created. {@code true} from the beginning of {@link
     * #onActivityCreated(Bundle)}to the end of {@link #onDestroy()}.
     * <p/>
     * Used to determine if list data provided by this fragment's target fragment was received while
     * the fragment is active.
     */
    private boolean mIsCreated = false;
    /**
     * Adapter associated with this fragment's recycler view.
     */
    private CursorAdapter mAdapter = null;
    /**
     * Helper for managing the entry type associated with this fragment.
     */
    private EntryHelper mEntryHelper;
    /**
     * The fragment's callback object, which is notified of changes to the fragment.
     */
    private EntryListFragmentListener mListener = null;
    /**
     * Adapter data for list. Provided by parent fragment via calls to {@link     *
     * #onDataChanged(List)}.
     */
    private List<? extends Parcelable> mAdapterData = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mIsCreated = true;
        final Bundle args = getArguments();
        // Determine if data is editable.
        mIsEditable = args.getBoolean(Constants.ARG_IS_EDITABLE, false);
        // Set the appropriate entry helper.
        switch (args.getInt(Constants.ARG_ENTRY_TYPE)) {
            case Constants.ENTRY_TYPE_CONTACT:
                mEntryHelper = new ContactHelper();
                break;
            case Constants.ENTRY_TYPE_SPOT:
                mEntryHelper = new ActivityHelper();
                break;
            default:
                throw new IllegalArgumentException();
        }
        // Set up the recycler view layout.
        mEntryHelper.setupLayout(getRecyclerView());
        // Set up the adapter.
        switch (args.getInt(Constants.ARG_DATA_TYPE)) {
            //            case Constants.DATA_TYPE_CONTENT_URI: TODO
            //                // Set the adapter.
            //                mAdapter = mEntryHelper.getAdapter(this, null);
            //                // Initialize the cursor loader.
            //                getLoaderManager().initLoader(0, null, new LoaderCallbacks());
            //                break;
            case Constants.DATA_TYPE_PARCELED_OBJECT:
                if (mAdapterData != null) {
                    final Cursor cursor = mEntryHelper.getCursorFromParcelableList(mAdapterData,
                            args.getLong(Constants.ARG_JOURNAL_ID));
                    setAdapter(mEntryHelper.getAdapter(this, cursor));
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Attach this fragment's activity as a callback.
        if (context instanceof EntryListFragmentListener) {
            mListener = (EntryListFragmentListener) context;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onDataChanged(@Nullable List<? extends Parcelable> data) {
        mAdapterData = data;
        if (mIsCreated) {
            if (data != null) {
                final Cursor cursor = mEntryHelper.getCursorFromParcelableList(data,
                        getArguments().getLong(Constants.ARG_JOURNAL_ID));
                final CursorAdapter adapter = (CursorAdapter) getAdapter();
                if (adapter == null) {
                    setAdapter(mEntryHelper.getAdapter(this, cursor));
                } else {
                    adapter.swapCursor(cursor);
                }
            } else {
                setAdapter(null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close the CAB, if still active.
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
        mIsCreated = false;
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_DELETE:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    deleteEntries();
                    // Close the action mode.
                    mActionMode.finish();
                }
                break;
            case DIALOG_TYPE_INSERT_FAILED:
                // Do nothing.
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onPageSelected() {
        // No action.
    }

    @Override
    public void onPageUnselected() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    /**
     * Called when the floating action button in this fragment's parent is clicked.
     */
    public void onParentFABClicked() {
        dispatchShowEntry(null, true);
    }

    /**
     * Delete the selected entries from the provider.
     */
    private void deleteEntries() {
        //        // Get the items selected for deletion. TODO
        //        final long[] activatedIds = getActivatedAdapterIds();
        //        // Delete the selected items from the database.
        //        final StringBuilder builder = new StringBuilder();
        //        boolean isFirst = true;
        //        for (long l : activatedIds) {
        //            if (isFirst) {
        //                isFirst = false;
        //            } else {
        //                builder.append(", ");
        //            }
        //            builder.append(l);
        //        }
        //        final QueryHandler handler = new QueryHandler(getActivity().getContentResolver());
        //        final Uri uri = getArguments().getParcelable(Constants.ARG_DATA);
        //        handler.startDelete(0, null, uri, Activities.COLUMN_ID
        //                + " IN (" + builder.toString() + ")", null);
    }

    /**
     * Insert a new journal entry into the provider.
     * <p/>
     * The appropriate editor for the type of journal entry inserted will be started if this
     * fragment is still visible when the insert is complete.
     */
    //    private void insertEntryAndStartEditor() { TODO
    //        // Insert journal entry on background thread.
    //        final ContentValues values = new ContentValues(0);
    //        final QueryHandler handler = new QueryHandler(getActivity().getContentResolver());
    //        final Uri uri = getArguments().getParcelable(Constants.ARG_DATA);
    //        handler.setQueryListener(new StartEditorQueryListener());
    //        handler.startInsert(0, null, uri, values);
    //    }

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
            case DIALOG_TYPE_CONFIRM_DELETE:
                dialog = JournalDialogFragment.newMessageDialog
                        (dialogType, R.string.dialog_message_deleteSpots,
                                R.string.dialog_positiveButton_delete, true);
                break;
            case DIALOG_TYPE_INSERT_FAILED:
                dialog = JournalDialogFragment.newMessageDialog
                        (dialogType, R.string.dialog_message_error_insertSpot,
                                R.string.dialog_positiveButton_ok, false);
                break;
            default:
                throw new IllegalArgumentException();
        }
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Notify this fragment's listener to start an entry viewer or editor.
     *
     * @param data       data representing the item to edit (content URI or parcelled object)
     * @param isEditable {@code true} if entry is editable, {@code false} otherwise
     */
    private void dispatchShowEntry(@Nullable Parcelable data, boolean isEditable) {
        if (mListener != null) {
            final int entryType = getArguments().getInt(Constants.ARG_ENTRY_TYPE);
            final Bundle args = new Bundle();
            args.putLong(Constants.ARG_JOURNAL_ID,
                    getArguments().getLong(Constants.ARG_JOURNAL_ID));
            args.putInt(Constants.ARG_DATA_TYPE, getArguments().getInt(Constants.ARG_DATA_TYPE));
            if (data != null) {
                args.putParcelable(Constants.ARG_DATA, data);
            }
            mListener.showEntry(this, entryType, args, isEditable);
        }
    }

    /**
     * A callback to parent activities of this fragment to notify them of changes to the fragment.
     * <p/>
     * All activities containing this fragment must implement this callback interface.
     */
    public interface EntryListFragmentListener {

        /**
         * Callback to be invoked when an entry detail viewer is being requested.
         *
         * @param fragment   the fragment requesting an entry detail viewer
         * @param entryType  the type of entry to view, one of {@link Constants#ENTRY_TYPE_CONTACT}
         *                   or {@link Constants#ENTRY_TYPE_SPOT}
         * @param args       bundle containing the data and data type to pass to the viewer
         * @param isEditable {@code true} if entry is editable, {@code false} otherwise
         */
        void showEntry(@NonNull EntryListFragment fragment, int entryType, @NonNull Bundle args,
                       boolean isEditable);

    }

    /**
     * A view holder for handling journal activities.
     */
    private static class ActivityViewHolder extends ViewHolder {

        /**
         * View for showing image.
         */
        public final NetworkImageView image;
        /**
         * View for showing title text.
         */
        public final TextView title;
        /**
         * View for showing subtitle text.
         */
        public final TextView subtitle;
        /**
         * View for showing description text.
         */
        public final TextView description;
        /**
         * Button for editing the activity.
         */
        public final TextView editView;
        /**
         * Button for launching a map to show the activity location.
         */
        public final TextView mapView;
        /**
         * Toggle button for showing/hiding description text.
         */
        public CheckableImageView toggle;

        /**
         * Create a new view holder.
         *
         * @param itemView the view being managed by this view holder
         */
        public ActivityViewHolder(View itemView) {
            super(itemView);
            toggle = itemView.findViewById(R.id.action_toggle);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(text_title);
            subtitle = itemView.findViewById(R.id.text_subtitle);
            description = itemView.findViewById(R.id.text_description);
            editView = itemView.findViewById(action_2);
            mapView = itemView.findViewById(R.id.action_1);
        }

    }

    /**
     * A view holder for handling journal contacts.
     */
    private static class ContactViewHolder extends ViewHolder {

        /**
         * View for showing the contact name.
         */
        public final TextView name;
        /**
         * View for showing the contact email address.
         */
        public final TextView email;
        /**
         * View for showing the contact phone number.
         */
        public final TextView phone;
        /**
         * View for showing the contact description.
         */
        public final TextView description;
        /**
         * Button for adding this contact to the device contact list.
         */
        public final TextView addContactView;
        /**
         * Button for editing the activity.
         */
        public final TextView editView;

        /**
         * Create a new view holder.
         *
         * @param itemView the view being managed by this view holder
         */
        ContactViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(text_title);
            email = itemView.findViewById(text_subtitle1);
            phone = itemView.findViewById(text_subtitle2);
            description = itemView.findViewById(text_description);
            addContactView = itemView.findViewById(R.id.action_1);
            editView = itemView.findViewById(action_2);
        }

    }

    /**
     * Class for supplying entry-specific implementations for a recycler fragment.
     */
    private static abstract class EntryHelper {

        /**
         * Get an adapter for displaying entry items.
         *
         * @param fragment fragment managing the adapter
         * @param cursor   cursor containing adapter data
         * @return an adapter for displaying entry items
         */
        public abstract CursorAdapter<ViewHolder> getAdapter(@NonNull EntryListFragment fragment,
                                                             @Nullable Cursor cursor);

        /**
         * Get a cursor from a list of {@link Parcelable} objects.
         * <p/>
         * The parcelable object type allowed is implementation specific.
         *
         * @param list      the list of parcelable objects
         * @param journalId the ID of the journal that the items in {@code list} are from
         * @return a cursor representation of {@code list}
         */
        public abstract Cursor getCursorFromParcelableList(@NonNull List<? extends Parcelable> list,
                                                           long journalId);

        /**
         * Get the query loader sort order.
         */
        public String getLoaderSortOrder() {
            return null;
        }

        /**
         * Get the query loader projection.
         */
        public abstract String[] getLoaderProjection();

        /**
         * Set up recycler view properties for displaying entry items. This includes setting an
         * appropriate {@link LayoutManager} on the recycler view, if required.
         *
         * @param view the recycler view to set up
         */
        public abstract void setupLayout(RecyclerView view);

    }

    /**
     * An adapter for handling journal activities.
     */
    private class ActivityAdapter extends CursorAdapter<ViewHolder> implements OnClickListener {

        /**
         * Layout ID for an item view containing an image.
         */
        private static final int LAYOUT_ID_WITH_IMAGE =
                R.layout.list_item_card_media_title_action_text;
        /**
         * Layout ID for an item view without an image.
         */
        private static final int LAYOUT_ID_NO_IMAGE = R.layout.list_item_card_title_text_action;

        /**
         * Loader and cache for images.
         */
        private final ImageLoader mImageLoader =
                VolleySingleton.getInstance(getContext()).getImageLoader();
        /**
         * Set for keeping track of checked positions in the adapter.
         */
        private final Set<Integer> mCheckedPositions = new HashSet<>();

        /**
         * Create a new adapter.
         *
         * @param cursor the cursor to be managed by this adapter, may be {@code null} if the data
         *               is not yet available
         */
        public ActivityAdapter(@Nullable Cursor cursor) {
            super(cursor);
        }

        @Override
        public int getItemViewType(int position) {
            final Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            final String imageUri =
                    cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_IMAGE_URI));
            return TextUtils.isEmpty(imageUri) ? LAYOUT_ID_NO_IMAGE : LAYOUT_ID_WITH_IMAGE;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
            final ActivityViewHolder activity = (ActivityViewHolder) holder;
            // Set the title text.
            final String title =
                    cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_TITLE));
            if (!TextUtils.isEmpty(title)) {
                activity.title.setText(title);
            } else {
                activity.title.setText(R.string.defaultText_title);
            }
            // Set the subtitle text and visibility of the map button.
            final String subtitle = cursor.getString(
                    cursor.getColumnIndexOrThrow(Activities.COLUMN_LOCATION));
            if (!TextUtils.isEmpty(subtitle)) {
                activity.subtitle.setText(subtitle);
                activity.mapView.setVisibility(View.VISIBLE);
            } else {
                activity.subtitle.setText(null);
                activity.mapView.setVisibility(View.GONE);
            }
            // Set the description text.
            final String description = cursor.getString(
                    cursor.getColumnIndexOrThrow(Activities.COLUMN_DESCRIPTION));
            if (!TextUtils.isEmpty(description)) {
                activity.description.setText(description);
            } else {
                activity.description.setText(R.string.defaultText_description);
            }
            // Set the description toggle button.
            if (activity.toggle != null) {
                boolean isChecked = mCheckedPositions.contains(activity.getAdapterPosition());
                activity.toggle.setChecked(isChecked);
                activity.description.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            // Set the image.
            if (activity.image != null) {
                final String uriString =
                        cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_IMAGE_URI));
                // Determine if media is a video.
                final String extension = MimeTypeMap.getFileExtensionFromUrl(uriString);
                if (extension.equals("mp4")) {
                    // TODO show to user that this is a video.
                    activity.image.setImageUrl(null, mImageLoader);
                    activity.image.setOnClickListener(this);
                } else {
                    activity.image.setImageUrl(uriString, mImageLoader);
                    activity.image.setOnClickListener(null);
                }
            }
        }

        @NonNull
        @Override
        public ActivityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view =
                    LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            final ActivityViewHolder holder = new ActivityViewHolder(view);
            // Set listener for main action.
            //            final View main = view.findViewById(R.id.action_main); TODO
            //            main.setTag(holder);
            //            main.setOnClickListener(this);
            // Set text and listener for map button.
            holder.mapView.setText(R.string.action_label_showMap);
            holder.mapView.setTag(holder);
            holder.mapView.setOnClickListener(this);
            // Set text and listener for edit button.
            if (mIsEditable) {
                holder.editView.setText(R.string.action_label_edit);
                holder.editView.setTag(holder);
                holder.editView.setOnClickListener(this);
            } else {
                holder.editView.setVisibility(View.GONE);
            }
            // Set the default image.
            if (holder.image != null) {
                holder.image.setDefaultImageResId(R.drawable.image_default);
                holder.image.setTag(holder);
            }
            // Set listener for toggle button.
            final CheckableImageView toggle =
                    (CheckableImageView) view.findViewById(R.id.action_toggle);
            if (toggle != null) {
                toggle.setTag(holder);
                toggle.setOnClickListener(this);
            }
            return holder;
        }

        @Override
        public void onClick(View view) {
            final int position = ((ActivityViewHolder) view.getTag()).getAdapterPosition();
            switch (view.getId()) {
                // Open the video activity.
                case R.id.image:
                    Cursor cursor = getCursor();
                    cursor.moveToPosition(position);
                    final String imageUrl = cursor.getString(
                            cursor.getColumnIndexOrThrow(Activities.COLUMN_IMAGE_URI));
                    Intent intent = new Intent(getContext(), MediaActivity.class);
                    intent.setData(Uri.parse(imageUrl));
                    startActivity(intent);
                    break;
                // Open the map activity.
                case R.id.action_1:
                    cursor = getCursor();
                    cursor.moveToPosition(position);
                    final String location = cursor.getString(
                            cursor.getColumnIndexOrThrow(Activities.COLUMN_LOCATION));
                    intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("geo:0,0?q=" + location.replace(" ", "+")));
                    startActivity(intent);
                    break;
                // Edit the journal activity.
                case action_2:
                    dispatchShowEntry(getActivityFromCursor(position), true);
                    break;
                // Show/hide the description text.
                case R.id.action_toggle:
                    final CheckableImageView toggle = (CheckableImageView) view;
                    if (toggle.isChecked()) {
                        mCheckedPositions.remove(position);
                    } else {
                        mCheckedPositions.add(position);
                    }
                    notifyItemChanged(position);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * Create an activity from cursor data.
         *
         * @param position the position of the activity in the cursor
         */
        private NetworkUtils.Activity getActivityFromCursor(int position) {
            final Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            final NetworkUtils.Activity activity = new NetworkUtils.Activity();
            activity.activityId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(Activities.COLUMN_ID));
            activity.title =
                    cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_TITLE));
            activity.description =
                    cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_DESCRIPTION));
            activity.location =
                    cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_LOCATION));
            final String imageUri =
                    cursor.getString(cursor.getColumnIndexOrThrow(Activities.COLUMN_IMAGE_URI));
            if (!TextUtils.isEmpty(imageUri)) {
                final Media media = new Media();
                media.path = imageUri;
                activity.media = new ArrayList<>(1);
                activity.media.add(media);
            }
            activity.latitude =
                    cursor.getDouble(cursor.getColumnIndexOrThrow(Activities.COLUMN_LATITUDE));
            activity.longitude =
                    cursor.getDouble(cursor.getColumnIndexOrThrow(Activities.COLUMN_LONGITUDE));
            return activity;
        }

    }

    /**
     * Entry helper that manages a directory of spots.
     */
    private class ActivityHelper extends EntryHelper {

        /**
         * Create a new entry helper
         */
        public ActivityHelper() {
        }

        @Override
        public CursorAdapter<ViewHolder> getAdapter(@NonNull EntryListFragment fragment,
                                                    @Nullable Cursor cursor) {
            return new ActivityAdapter(cursor);
        }

        @Override
        public Cursor getCursorFromParcelableList(@NonNull List<? extends Parcelable> list,
                                                  long journalId) {
            final MatrixCursor cursor = new MatrixCursor(getLoaderProjection(), list.size());
            for (Parcelable parcelable : list) {
                final NetworkUtils.Activity response = (NetworkUtils.Activity) parcelable;
                String uriString = null;
                if (response.media.size() >= 1) {
                    uriString = response.media.get(0).path;
                }
                cursor.addRow(new Object[]{response.activityId, journalId, response.title,
                        response.description, response.location, uriString, response.latitude,
                        response.longitude});
            }
            return cursor;
        }

        @Override
        public String[] getLoaderProjection() {
            return new String[]{Activities.COLUMN_ID, Activities.COLUMN_JOURNAL_ID,
                    Activities.COLUMN_TITLE, Activities.COLUMN_DESCRIPTION,
                    Activities.COLUMN_LOCATION, Activities.COLUMN_IMAGE_URI,
                    Activities.COLUMN_LATITUDE, Activities.COLUMN_LONGITUDE};
        }

        @Override
        public String getLoaderSortOrder() {
            return Activities.COLUMN_CREATED + " DESC";
        }

        @Override
        public void setupLayout(RecyclerView view) {
            // Set the recycler view layout manager.
            view.setLayoutManager(new StaggeredGridLayoutManager(
                    (getResources().getInteger(R.integer.column_count) / 4),
                    StaggeredGridLayoutManager.VERTICAL));
            // Set the recycler view padding.
            final int padding = getResources().getDimensionPixelOffset(R.dimen.spacing_grid_item);
            view.setPadding(padding, padding, padding, padding);
        }

    }

    /**
     * An adapter for handling journal contacts.
     */
    private class ContactAdapter extends CursorAdapter<ViewHolder> implements OnClickListener {

        /**
         * Layout ID for the item view used in this adapter.
         */
        private static final int LAYOUT_ID = R.layout.list_item_card_three_line_header_text_action;

        /**
         * Create a new adapter.
         *
         * @param cursor the cursor to be managed by this adapter, may be {@code null} if the data
         *               is not yet available
         */
        ContactAdapter(@Nullable Cursor cursor) {
            super(cursor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
            final ContactViewHolder contact = (ContactViewHolder) holder;
            // Set the contact name.
            final String name =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_NAME));
            if (!TextUtils.isEmpty(name)) {
                contact.name.setText(name);
            } else {
                contact.name.setText(R.string.defaultText_name);
            }
            // Set the contact email address.
            final String email =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_EMAIL));
            if (!TextUtils.isEmpty(email)) {
                contact.email.setText(email);
            } else {
                contact.email.setText(R.string.defaultText_email);
            }
            // Set the contact phone number.
            final String phone =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_PHONE));
            if (!TextUtils.isEmpty(phone)) {
                contact.phone.setText(phone);
            } else {
                contact.phone.setText(R.string.defaultText_phone);
            }
            // Set the contact description.
            final String description =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_DESCRIPTION));
            if (!TextUtils.isEmpty(description)) {
                contact.description.setText(description);
            } else {
                contact.description.setText(R.string.defaultText_description);
            }

        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view =
                    LayoutInflater.from(parent.getContext()).inflate(LAYOUT_ID, parent, false);
            final ContactViewHolder holder = new ContactViewHolder(view);
            // Set text and listener for "add contact" button.
            holder.addContactView.setText(R.string.action_label_importContact);
            holder.addContactView.setTag(holder);
            holder.addContactView.setOnClickListener(this);
            // Set text and listener for edit button.
            if (mIsEditable) {
                holder.editView.setText(R.string.action_label_edit);
                holder.editView.setTag(holder);
                holder.editView.setOnClickListener(this);
            } else {
                holder.editView.setVisibility(View.GONE);
            }
            return holder;
        }

        @Override
        public void onClick(View view) {
            final ContactViewHolder holder = (ContactViewHolder) view.getTag();
            switch (view.getId()) {
                // Add a new contact.
                case action_1:
                    // Create an intent to insert a contact.
                    final Intent intent = new Intent(Intents.Insert.ACTION);
                    // Set the MIME type to match the contacts provider.
                    intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                    // Add the contact name.
                    final String name = holder.name.getText().toString();
                    if (!TextUtils.isEmpty(name)) {
                        intent.putExtra(Intents.Insert.NAME, name);
                    }
                    // Add the contact email address.
                    final String email = holder.email.getText().toString();
                    if (!TextUtils.isEmpty(email)) {
                        intent.putExtra(Intents.Insert.EMAIL, email);
                        intent.putExtra(Intents.Insert.EMAIL_TYPE, CommonDataKinds.Email.TYPE_WORK);
                    }
                    // Add the contact phone number.
                    final String phone = holder.phone.getText().toString();
                    if (!TextUtils.isEmpty(phone)) {
                        intent.putExtra(Intents.Insert.PHONE, phone);
                        intent.putExtra(Intents.Insert.PHONE_TYPE, CommonDataKinds.Phone.TYPE_WORK);
                    }
                    startActivity(intent);
                    break;
                // Edit the journal activity.
                case action_2:
                    dispatchShowEntry(getContactFromCursor(holder.getAdapterPosition()), true);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * Create an activity from cursor data.
         *
         * @param position the position of the activity in the cursor
         */
        private Contact getContactFromCursor(int position) {
            final Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            final Contact contact = new Contact();
            contact.contactId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(Contacts.COLUMN_ID));
            contact.name =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_NAME));
            contact.email =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_EMAIL));
            contact.phone =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_PHONE));
            contact.description =
                    cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_DESCRIPTION));
            return contact;
        }

    }

    /**
     * Entry helper that manages a directory of contacts.
     */
    private class ContactHelper extends EntryHelper {

        /**
         * Create a new contact helper.
         */
        public ContactHelper() {
        }

        @Override
        public CursorAdapter<ViewHolder> getAdapter(@NonNull EntryListFragment fragment,
                                                    @Nullable Cursor cursor) {
            return new ContactAdapter(cursor);
        }

        @Override
        public Cursor getCursorFromParcelableList(@NonNull List<? extends Parcelable> list,
                                                  long journalId) {
            final MatrixCursor cursor = new MatrixCursor(getLoaderProjection(), list.size());
            for (Parcelable p : list) {
                final Contact response = (Contact) p;
                cursor.addRow(new Object[]{response.contactId, journalId, response.name,
                        response.email, response.phone, response.description});
            }
            return cursor;
        }

        @Override
        public String[] getLoaderProjection() {
            return new String[]{Contacts.COLUMN_ID, Contacts.COLUMN_JOURNAL_ID,
                    Contacts.COLUMN_NAME, Contacts.COLUMN_EMAIL, Contacts.COLUMN_PHONE,
                    Contacts.COLUMN_DESCRIPTION};
        }

        @Override
        public String getLoaderSortOrder() {
            return Contacts.COLUMN_CREATED + " ASC";
        }

        @Override
        public void setupLayout(RecyclerView view) {
            // Set the recycler view layout manager.
            view.setLayoutManager(new StaggeredGridLayoutManager(
                    (getResources().getInteger(R.integer.column_count) / 4),
                    StaggeredGridLayoutManager.VERTICAL));
            // Set the recycler view padding.
            final int padding = getResources().getDimensionPixelOffset(R.dimen.spacing_grid_item);
            view.setPadding(padding, padding, padding, padding);
        }

    }

    /**
     * Callback for interacting with a cursor loader manager.
     */
    private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final Uri uri = getArguments().getParcelable(Constants.ARG_DATA);
            return new CursorLoader(getContext(), uri, mEntryHelper.getLoaderProjection(), null,
                    null, mEntryHelper.getLoaderSortOrder());
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            // Hide the list view.
            setAdapter(null);
            // Remove all data references from the adapter.
            mAdapter.swapCursor(null);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            // Associate new cursor with the list adapter.
            mAdapter.swapCursor(data);
            // Add adapter to list view, if applicable.
            if (getAdapter() == null) {
                setAdapter(mAdapter);
            }
        }

    }

}