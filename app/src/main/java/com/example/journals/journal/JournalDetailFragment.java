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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.journals.journal.JournalDialogFragment.DialogListener;
import com.example.journals.R;
import com.example.journals.account.AccountUtils;
import com.example.journals.account.AccountUtils.AuthFailureErrorListener;
import com.example.journals.journal.JournalListFragment.NewJournalDialogFragment;
import com.example.journals.network.GsonRequest;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.Activity;
import com.example.journals.network.NetworkUtils.ConnectionRequest;
import com.example.journals.network.NetworkUtils.CreateJournalRequest;
import com.example.journals.network.NetworkUtils.CreateJournalResponse;
import com.example.journals.network.NetworkUtils.DeleteJournalResponse;
import com.example.journals.network.NetworkUtils.GetJournalDetailsRequest;
import com.example.journals.network.NetworkUtils.GetJournalDetailsResponse;
import com.example.journals.network.NetworkUtils.Journal;
import com.example.journals.network.NetworkUtils.Media;
import com.example.journals.provider.JournalContract.Journals;
import com.example.journals.provider.QueryHandler;
import com.example.journals.provider.QueryHandler.SimpleQueryListener;
import com.example.journals.widget.AuthHandlerFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import static android.app.Activity.RESULT_OK;

/**
 * A fragment representing the contents of a single journal.
 * <p/>
 * Activities containing this fragment MUST implement the {@link JournalDetailFragmentListener}
 * interface.
 */
public class JournalDetailFragment extends AuthHandlerFragment implements DialogListener {

    /**
     * Argument designating whether the fragment toolbar displays back arrow navigation. If true,
     * the fragment will call its listener when the back arrow is clicked.
     * <p/>
     * Type: boolean (default {@code false})
     */
    public static final String ARG_HAS_TOOLBAR_NAVIGATION = "ARG_HAS_TOOLBAR_NAVIGATION";

    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = JournalDetailFragment.class.getSimpleName();
    /**
     * Reason for requesting an auth token.
     */
    private static final int AUTH_REASON_GET_JOURNAL_DETAILS = 0, AUTH_REASON_EDIT_JOURNAL = 1,
            AUTH_REASON_DELETE_JOURNAL = 2;
    /**
     * Dialog types for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_EDIT_JOURNAL = 0, DIALOG_TYPE_CONFIRM_DELETE_JOURNAL = 1,
            DIALOG_TYPE_MESSAGE = 2;
    /**
     * Tag for dialogs.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Projection for building a cursor loader.
     */
    private static final String[] QUERY_PROJECTION =
            new String[]{Journals.COLUMN_ID, Journals.COLUMN_ACCOUNT_NAME, Journals.COLUMN_TITLE,
                    Journals.COLUMN_MAKE_PUBLIC, Journals.COLUMN_SERVER_ID};

    /**
     * The action mode that the fragment is currently in.
     */
    private ActionMode mActionMode = null; // TODO
    /**
     * Set whether the journal is editable.
     */
    private boolean mIsEditable = false;
    /**
     * Journal used to provide data to the entry lists.
     */
    private GetJournalDetailsResponse mJournal = null;
    /**
     * The current position of the view pager.
     */
    private int mCurrentPagerPosition = -1;
    /**
     * The fragment's current callback object, which is notified of journal deletion.
     */
    private JournalDetailFragmentListener mListener = null;
    /**
     * The adapter managing the view pager data.
     */
    private JournalPagerAdapter mAdapter;
    /**
     * The view pager this fragment is managing.
     */
    private ViewPager mViewPager;

    /**
     * Move journal media information from a journal into the activity it is associated with.
     *
     * @param journal journal to move media information from
     */
    public static void putJournalMediaInActivities(@NonNull GetJournalDetailsResponse journal) {
        for (Activity a : journal.activities) {
            a.media = new ArrayList<>();
            for (Iterator<Media> iterator = journal.media.iterator(); iterator.hasNext(); ) {
                final Media media = iterator.next();
                if (media.activityId == a.activityId) {
                    a.media.add(media);
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // *****Set up toolbar.***** //
        final Toolbar toolbar = getView().findViewById(R.id.toolbar);
        // Set "up" navigation.
        if (getArguments().getBoolean(ARG_HAS_TOOLBAR_NAVIGATION, false)) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setNavigationOnClickListener(new NavigationOnClickListener());
        }

        // *****Set up tab layout and view pager.***** //
        mViewPager = getView().findViewById(R.id.viewPager);
        mAdapter = new JournalPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mAdapter);
        // Set up view pager tabs.
        final TabLayout tabLayout = getView().findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        // Add view pager page change listener.
        mViewPager.addOnPageChangeListener(new OnPageChangeListener());

        // *****Set up journal detail based on type of data received.***** //
        switch (getArguments().getInt(Constants.ARG_DATA_TYPE)) {
            //            case Constants.DATA_TYPE_CONTENT_URI: TODO
            //                // Retrieve journal title to display in toolbar.
            //                getLoaderManager().initLoader(0, null, new LoaderCallbacks());
            //                break;
            case Constants.DATA_TYPE_PARCELED_OBJECT:
                final Journal journal = getArguments().getParcelable(Constants.ARG_DATA);
                mIsEditable = Constants.isCurrentUser(getContext(), journal.userId);
                // Retrieve journal title to display in toolbar.
                final String title = (!TextUtils.isEmpty(journal.title)) ? journal.title
                        : getString(R.string.defaultText_title);
                toolbar.setTitle(title);
                // Set up the toolbar menu for editing the journal.
                if (mIsEditable) {
                    toolbar.inflateMenu(R.menu.menu_journal_detail);
                    toolbar.setOnMenuItemClickListener(new OnMenuItemClickListener());
                }
                // Get journal information from server.
                final GetJournalDetailsRequest getRequest = new GetJournalDetailsRequest();
                getRequest.journalId = journal.journalId;
                final String accountName = getArguments().getString(Constants.ARG_ACCOUNT_NAME);
                if (accountName != null) {
                    AccountUtils.getAuthToken(getActivity(), AUTH_REASON_GET_JOURNAL_DETAILS,
                            accountName, new AuthTokenListener(getRequest));
                } else {
                    final GetJournalDetailsResponseListener listener =
                            new GetJournalDetailsResponseListener();
                    final GsonRequest request = new GsonRequest<>(getRequest.getUrl(getContext()),
                            GetJournalDetailsResponse.class,
                            NetworkUtils.getDataTransferHeaders(getContext(), null),
                            new Gson().toJson(getRequest), listener, listener);
                    startRequest(request);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DIALOG_TYPE_EDIT_JOURNAL:
                if (resultCode == RESULT_OK) {
                    // Update journal on server.
                    final CreateJournalRequest request = new CreateJournalRequest();
                    request.title = data.getStringExtra(NewJournalDialogFragment.ARG_JOURNAL_TITLE);
                    request.isPublic = data.getIntExtra(NewJournalDialogFragment.ARG_PRIVACY, 1);
                    request.id = mJournal.journal.get(0).journalId;
                    AccountUtils.getAuthToken(getActivity(), AUTH_REASON_EDIT_JOURNAL,
                            AccountUtils.getActiveAccount(getContext()),
                            new AuthTokenListener(request));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Attach this fragment's context as a callback.
        if (context instanceof JournalDetailFragmentListener) {
            mListener = (JournalDetailFragmentListener) context;
        } else {
            throw new IllegalStateException("Activity must implement callback.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.toolbar_tabs_viewpager_fab, container, false);
    }

    @Override
    public void onDestroy() {
        // Close the CAB, if still active.
        if (mActionMode != null) {
            mActionMode.finish();
        }
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        // Remove this fragment's activity as a callback.
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_EDIT_JOURNAL:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    final String title = data.getStringExtra(JournalDialogFragment.DIALOG_DATA);
                    updateJournal(title);
                }
                break;
            case DIALOG_TYPE_CONFIRM_DELETE_JOURNAL:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    deleteJournal();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    //    @Override TODO
    //    public void onViewStateRestored(Bundle savedInstanceState) {
    //        super.onViewStateRestored(savedInstanceState);
    //        // Set up the floating action button.
    //        if (mIsEditable) {
    //            mFloatingButton = (FloatingActionButton) getView().findViewById(R.id.floatingButton);
    //            mFloatingButton.setOnClickListener(new OnFloatingButtonClickListener());
    //            switch (mViewPager.getCurrentItem()) {
    //                case JournalPagerAdapter.POSITION_SPOTS:
    //                    mFloatingButton.setImageResource(R.drawable.ic_add_to_photos_white_24dp);
    //                    mFloatingButton.show();
    //                    break;
    //                case JournalPagerAdapter.POSITION_CONTACTS:
    //                    mFloatingButton.setImageResource(R.drawable.ic_person_add_white_24dp);
    //                    mFloatingButton.show();
    //                    break;
    //                default:
    //                    mFloatingButton.hide();
    //                    break;
    //            }
    //        }
    //    }

    /**
     * Delete the journal associated with this fragment.
     */
    private void deleteJournal() {
        final QueryHandler handler = new QueryHandler(getActivity().getContentResolver());
        handler.setQueryListener(new SimpleQueryListener() {

            @Override
            public void onDeleteComplete(int token, Object cookie, int result) {
                if (mListener != null) {
                    mListener.onJournalDeleted(JournalDetailFragment.this);
                }
            }

        });
        final Uri journalUri = getArguments().getParcelable(Constants.ARG_DATA);
        handler.startDelete(0, null, journalUri, null, null);
    }

    /**
     * Create and show a dialog of the specified type.
     * <p/>
     * {@link #onDialogResult(int, int, Intent)} will be called with the dialog result.
     *
     * @param dialogType the type of dialog to display
     * @param message    message to display when dialog type is {@link #DIALOG_TYPE_MESSAGE},
     *                   ignored otherwise
     */
    private void showDialog(int dialogType, @Nullable String message) {
        final DialogFragment dialog;
        switch (dialogType) {
            case DIALOG_TYPE_EDIT_JOURNAL:
                dialog = new NewJournalDialogFragment();
                final Bundle args = new Bundle();
                args.putString(NewJournalDialogFragment.ARG_DIALOG_TITLE,
                        getString(R.string.dialog_message_editJournal));
                args.putString(NewJournalDialogFragment.ARG_JOURNAL_TITLE,
                        mJournal.journal.get(0).title);
                args.putInt(NewJournalDialogFragment.ARG_PRIVACY, 1);// TODO journal.privacy
                args.putString(NewJournalDialogFragment.ARG_POSITIVE_BUTTON_TEXT,
                        getString(R.string.dialog_positiveButton_update));
                dialog.setArguments(args);
                break;
            case DIALOG_TYPE_CONFIRM_DELETE_JOURNAL:
                dialog = JournalDialogFragment.newMessageDialog
                        (dialogType, R.string.dialog_message_deleteJournal,
                                R.string.dialog_positiveButton_delete, true);
                break;
            case DIALOG_TYPE_MESSAGE:
                dialog = JournalDialogFragment
                        .newMessageDialog(dialogType, message, R.string.dialog_positiveButton_ok,
                                false);
                break;
            default:
                throw new IllegalArgumentException();
        }
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Update the journal associated with this fragment.
     *
     * @param title the updated journal title
     */
    private void updateJournal(String title) {
        final ContentValues values = new ContentValues(1);
        values.put(Journals.COLUMN_TITLE, title);
        final QueryHandler handler = new QueryHandler(getActivity().getContentResolver());
        handler.startUpdate(0, null, (Uri) getArguments().getParcelable(Constants.ARG_DATA), values,
                null, null);
    }

    /**
     * A callback to parent activities of this fragment to notify them of changes to the fragment.
     * <p/>
     * All activities containing this fragment must implement this callback interface.
     */
    public interface JournalDetailFragmentListener {

        /**
         * Callback to be invoked when a journal has been deleted.
         *
         * @param fragment the fragment managing the deleted journal
         */
        void onJournalDeleted(JournalDetailFragment fragment);

        /**
         * Callback to be invoked when the toolbar navigation icon is clicked.
         *
         * @param fragment the fragment containing the toolbar navigation icon that was clicked
         */
        void onNavigationClick(JournalDetailFragment fragment);

    }

    /**
     * A callback to pager adapter fragments to inform them of changes to their status.
     */
    public interface OnPageStatusChangedListener {

        /**
         * Callback to be invoked when the data backing a page has changed.
         *
         * @param data the new data for the page, or {@code null} if the data has been reset or an
         *             error occurred
         */
        void onDataChanged(@Nullable List<? extends Parcelable> data);

        /**
         * Callback to be invoked when a page is selected.
         */
        void onPageSelected();

        /**
         * Callback to be invoked when a page is unselected.
         */
        void onPageUnselected();

    }

    /**
     * Class for transferring data with the server after receiving an auth token.
     */
    private class AuthTokenListener implements AccountUtils.AuthTokenListener {

        /**
         * Request for transferring data with the server.
         */
        private final ConnectionRequest mRequest;

        /**
         * Create a new auth token listener.
         *
         * @param request request for transferring data with the server
         */
        AuthTokenListener(@NonNull ConnectionRequest request) {
            mRequest = request;
        }

        @Override
        public void onAuthTokenReceived(int requestCode, String accountName, String authToken) {
            if (isStarted()) {
                final GsonRequest request;
                switch (requestCode) {
                    case AUTH_REASON_GET_JOURNAL_DETAILS:
                        final GetJournalDetailsResponseListener getListener =
                                new GetJournalDetailsResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                GetJournalDetailsResponse.class,
                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(mRequest), getListener, getListener);
                        break;
                    case AUTH_REASON_EDIT_JOURNAL:
                        final CreateJournalResponseListener createListener =
                                new CreateJournalResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                CreateJournalResponse.class,
                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(mRequest), createListener, createListener);
                        break;
                    case AUTH_REASON_DELETE_JOURNAL:
                        final DeleteJournalResponseListener deleteListener =
                                new DeleteJournalResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                DeleteJournalResponse.class,
                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(mRequest), deleteListener, deleteListener);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                startRequest(request);
            } else if (Constants.DEBUG) {
                Log.e(DEBUG_TAG, "Auth token received after onStop().");
            }
        }

        @Override
        public void onAuthenticatorError(int requestCode) { // TODO better message
            showDialog(DIALOG_TYPE_MESSAGE,
                    getString(R.string.dialog_message_error_serverNotResponding));
        }

        @Override
        public void onOperationCancelled(int requestCode) {
            // No action.
        }

        @Override
        public void onIOError(int requestCode) {
            showDialog(DIALOG_TYPE_MESSAGE,
                    getString(R.string.dialog_message_error_serverNotResponding));
        }

    }

    /**
     * Class for processing "delete journal" responses.
     */
    private class DeleteJournalResponseListener extends AuthFailureErrorListener
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
        DeleteJournalResponseListener(@NonNull android.app.Activity activity, int requestCode,
                                      @NonNull String accountName,
                                      @NonNull AuthTokenListener listener,
                                      @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(DeleteJournalResponse response) {
            if (mListener != null) {
                mListener.onJournalDeleted(JournalDetailFragment.this);
            }
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showDialog(DIALOG_TYPE_MESSAGE, message);
            }
        }

    }

    /**
     * Class for processing "create journal" responses.
     */
    private class CreateJournalResponseListener extends AuthFailureErrorListener
            implements Response.Listener<CreateJournalResponse> {

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
        CreateJournalResponseListener(@NonNull android.app.Activity activity, int requestCode,
                                      @NonNull String accountName,
                                      @NonNull AuthTokenListener listener,
                                      @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(CreateJournalResponse response) {
            // TODO only title needs to be refreshed?
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showDialog(DIALOG_TYPE_MESSAGE, message);
            }
        }

    }

    /**
     * Class for processing "get journal details" responses.
     */
    private class GetJournalDetailsResponseListener extends AuthFailureErrorListener
            implements Response.Listener<GetJournalDetailsResponse> {

        /**
         * Create a new response listener.
         */
        GetJournalDetailsResponseListener() {
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
        GetJournalDetailsResponseListener(@NonNull android.app.Activity activity,
                                          int requestCode, @NonNull String accountName,
                                          @NonNull AuthTokenListener listener,
                                          @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(GetJournalDetailsResponse response) {
            mJournal = response;
            // Move journal media into the corresponding activity.
            putJournalMediaInActivities(mJournal);
            // Notify the spots fragment of data changes.
            final Fragment spotFragment =
                    mAdapter.getFragmentAtPosition(JournalPagerAdapter.POSITION_SPOTS);
            if (spotFragment != null) {
                ((OnPageStatusChangedListener) spotFragment).onDataChanged(mJournal.activities);
            }
            // Notify the contacts fragment of data changes.
            final Fragment contactFragment =
                    mAdapter.getFragmentAtPosition(JournalPagerAdapter.POSITION_CONTACTS);
            if (contactFragment != null) {
                ((OnPageStatusChangedListener) contactFragment).onDataChanged(mJournal.contacts);
            }
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showDialog(DIALOG_TYPE_MESSAGE, message);
            }
        }

    }

    /**
     * Adapter for displaying journal detail pages in a pager view.
     */
    private class JournalPagerAdapter extends FragmentStatePagerAdapter {

        /**
         * The total number of items in this adapter.
         */
        static final int ITEM_COUNT = 2;
        /**
         * Position of the spots fragment in the adapter.
         */
        static final int POSITION_SPOTS = 0;
        /**
         * Position of the contacts fragment in the adapter.
         */
        static final int POSITION_CONTACTS = 1;

        /**
         * A mapping of fragments to their position in the adapter.  Used for sending messages to
         * the adapter's fragments.
         */
        private final SparseArray<Fragment> mFragments = new SparseArray<>(ITEM_COUNT);

        /**
         * Construct a new journal pager adapter.
         *
         * @param fragmentManager the fragment manager used for interacting with fragments
         */
        JournalPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                                @NonNull Object object) {
            mFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return ITEM_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            final Bundle args = new Bundle();
            // Set whether journal is editable.
            args.putBoolean(Constants.ARG_IS_EDITABLE, mIsEditable);
            // Set the data type argument.
            final int dataType = getArguments().getInt(Constants.ARG_DATA_TYPE);
            args.putInt(Constants.ARG_DATA_TYPE, dataType);
            final Fragment fragment = new EntryListFragment();
            switch (position) {
                // Initialize the "spots" fragment.
                case POSITION_SPOTS:
                    args.putInt(Constants.ARG_ENTRY_TYPE, Constants.ENTRY_TYPE_SPOT);
                    switch (dataType) {
                        //                        case Constants.DATA_TYPE_CONTENT_URI: TODO
                        //                            final Uri uri =
                        //                                    Uri.withAppendedPath((Uri) data, Activities.CONTENT_DIRECTORY);
                        //                            args.putParcelable(Constants.ARG_DATA, uri);
                        //                            break;
                        case Constants.DATA_TYPE_PARCELED_OBJECT:
                            // Add the journal ID argument.
                            final Journal journal =
                                    getArguments().getParcelable(Constants.ARG_DATA);
                            args.putLong(Constants.ARG_JOURNAL_ID, journal.journalId);
                            // Notify the fragment of new data, if applicable.
                            if (mJournal != null) {
                                ((OnPageStatusChangedListener) fragment)
                                        .onDataChanged(mJournal.activities);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    break;
                // Initialize the "contacts" fragment.
                case POSITION_CONTACTS:
                    args.putInt(Constants.ARG_ENTRY_TYPE, Constants.ENTRY_TYPE_CONTACT);
                    switch (dataType) {
                        //                        case Constants.DATA_TYPE_CONTENT_URI: TODO
                        //                            final Uri uri = Uri.withAppendedPath((Uri) data,
                        //                                    Contacts.CONTENT_DIRECTORY);
                        //                            args.putParcelable(Constants.ARG_DATA, uri);
                        //                            break;
                        case Constants.DATA_TYPE_PARCELED_OBJECT:
                            // Add the journal ID argument.
                            final Journal journal =
                                    getArguments().getParcelable(Constants.ARG_DATA);
                            args.putLong(Constants.ARG_JOURNAL_ID, journal.journalId);
                            // Notify the fragment of new data, if applicable.
                            if (mJournal != null) {
                                ((OnPageStatusChangedListener) fragment)
                                        .onDataChanged(mJournal.contacts);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public String getPageTitle(int position) {
            switch (position) {
                case POSITION_SPOTS:
                    return getString(R.string.label_tab_activities);
                case POSITION_CONTACTS:
                    return getString(R.string.label_tab_contacts);
                default:
                    throw new IllegalArgumentException();
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            // Add the instantiated fragment to the local fragment map.
            final Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, fragment);
            return fragment;
        }

        /**
         * Get the fragment at the currently selected position.
         *
         * @return the fragment at the currently selected position, or {@code null} if the fragment
         * does not exist
         */
        @Nullable
        Fragment getCurrentFragment() {
            return mFragments.get(mViewPager.getCurrentItem());
        }

        /**
         * Get the fragment at the specified position.
         *
         * @return the fragment at the specified position, or {@code null} if the fragment does not
         * currently exist
         */
        @Nullable
        Fragment getFragmentAtPosition(int position) {
            return mFragments.get(position);
        }

    }

    /**
     * Callback for interacting with a cursor loader manager.
     */
    private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> { // TODO

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final Uri journalUri = getArguments().getParcelable(Constants.ARG_DATA);
            return new CursorLoader(getActivity(), journalUri, QUERY_PROJECTION, null, null, null);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            // Set the activity title to the title of the journal.
            if (data.moveToFirst()) {
                final String title = data.getString(0);
                final Toolbar toolbar = getView().findViewById(R.id.toolbar);
                if (!TextUtils.isEmpty(title)) {
                    toolbar.setTitle(title);
                } else {
                    toolbar.setTitle(R.string.defaultText_title);
                }
            }// TODO journal failed to load or was deleted
        }

    }

    /**
     * Listener for toolbar navigation icon clicks.
     */
    private class NavigationOnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onNavigationClick(JournalDetailFragment.this);
            }
        }

    }

    /**
     * Listener for floating action button clicks.
     */
    //    private class OnFloatingButtonClickListener implements OnClickListener { TODO
    //
    //        @Override
    //        public void onClick(View v) {
    //            final JournalPagerAdapter adapter = (JournalPagerAdapter) mViewPager.getAdapter();
    //            final Fragment fragment = adapter.getCurrentFragment();
    //            if ((fragment != null) && (fragment instanceof EntryListFragment)) {
    //                ((EntryListFragment) fragment).onParentFABClicked();
    //            }
    //        }
    //
    //    }

    /**
     * Listener for toolbar menu item clicks.
     */
    private class OnMenuItemClickListener implements Toolbar.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_newJournalSpot:
                    JournalPagerAdapter adapter = (JournalPagerAdapter) mViewPager.getAdapter();
                    Fragment fragment =
                            adapter.getFragmentAtPosition(JournalPagerAdapter.POSITION_SPOTS);
                    if (fragment instanceof EntryListFragment) {
                        ((EntryListFragment) fragment).onParentFABClicked();
                    }
                    return true;
                case R.id.action_newJournalPoc:
                    adapter = (JournalPagerAdapter) mViewPager.getAdapter();
                    fragment = adapter.getFragmentAtPosition(JournalPagerAdapter.POSITION_CONTACTS);
                    if (fragment instanceof EntryListFragment) {
                        ((EntryListFragment) fragment).onParentFABClicked();
                    }
                    return true;
                case R.id.action_edit:
                    showDialog(DIALOG_TYPE_EDIT_JOURNAL, null);
                    return true;
                case R.id.action_delete:
                    showDialog(DIALOG_TYPE_CONFIRM_DELETE_JOURNAL, null);
                    return true;
                default:
                    return false;
            }
        }

    }

    /**
     * Listener for view pager page changes.
     */
    private class OnPageChangeListener extends SimpleOnPageChangeListener {

        @Override
        public void onPageSelected(int position) {
            if (mCurrentPagerPosition >= 0) {
                final Fragment oldFragment = mAdapter.getFragmentAtPosition(mCurrentPagerPosition);
                if (oldFragment instanceof OnPageStatusChangedListener) {
                    ((OnPageStatusChangedListener) oldFragment).onPageUnselected();
                }
            }
            mCurrentPagerPosition = position;
            final Fragment newFragment = mAdapter.getCurrentFragment();
            if (newFragment instanceof OnPageStatusChangedListener) {
                ((OnPageStatusChangedListener) newFragment).onPageSelected();
            }
        }

        //        @Override TODO
        //        public void onPageScrollStateChanged(int state) {
        //            // Update the floating action button visibility/icon.
        //            if (mFloatingButton != null) {
        //                switch (state) {
        //                    case ViewPager.SCROLL_STATE_IDLE:
        //                        final int position = mViewPager.getCurrentItem();
        //                        if (position == JournalPagerAdapter.POSITION_SPOTS) {
        //                            mFloatingButton
        //                                    .setImageResource(R.drawable.ic_add_to_photos_white_24dp);
        //                        } else {
        //                            mFloatingButton.setImageResource(R.drawable.ic_person_add_white_24dp);
        //                        }
        //                        mFloatingButton.show();
        //                        break;
        //                    default:
        //                        if (mFloatingButton.isShown()) {
        //                            mFloatingButton.hide();
        //                        }
        //                        break;
        //                }
        //            }
        //        }

    }

}