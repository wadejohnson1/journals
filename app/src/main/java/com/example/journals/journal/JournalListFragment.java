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
import android.accounts.Account;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.example.journals.JournalDialogFragment;
import com.example.journals.JournalDialogFragment.DialogListener;
import com.example.journals.R;
import com.example.journals.account.AccountUtils;
import com.example.journals.account.AccountUtils.AuthFailureErrorListener;
import com.example.journals.network.GsonRequest;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.Activity;
import com.example.journals.network.NetworkUtils.ConnectionRequest;
import com.example.journals.network.NetworkUtils.CreateJournalRequest;
import com.example.journals.network.NetworkUtils.CreateJournalResponse;
import com.example.journals.network.NetworkUtils.DeleteJournalRequest;
import com.example.journals.network.NetworkUtils.DeleteJournalResponse;
import com.example.journals.network.NetworkUtils.GetJournalsRequest;
import com.example.journals.network.NetworkUtils.GetJournalsResponse;
import com.example.journals.network.NetworkUtils.Media;
import com.example.journals.network.NetworkUtils.SearchJournalsRequest;
import com.example.journals.network.VolleySingleton;
import com.example.journals.provider.JournalContract;
import com.example.journals.provider.JournalContract.Journals;
import com.example.journals.provider.QueryHandler;
import com.example.journals.provider.QueryHandler.SimpleQueryListener;
import com.example.journals.widget.FooterAdapterWrapper;
import com.example.journals.widget.ListAdapter;
import com.example.journals.widget.RecyclerViewFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static android.app.Activity.RESULT_OK;
import static com.example.journals.network.NetworkUtils.Journal;
import static com.example.journals.network.NetworkUtils.MyJournalsRequest;
import static com.example.journals.network.NetworkUtils.SearchJournalsResponse;
import static com.example.journals.network.NetworkUtils.getDataTransferHeaders;

/**
 * A fragment representing the contents of a journal directory. This fragment also supports tablet
 * devices by allowing list items to be given an 'activated' state upon selection. This helps
 * indicate which item is currently being viewed in a {@link JournalDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link JournalListFragmentListener}
 * interface.
 */
public class JournalListFragment extends RecyclerViewFragment implements DialogListener {

    /**
     * Argument designating the mode to display journals in.
     * <p/>
     * Type: int
     */
    public static final String ARG_NAVIGATION_MODE = "ARG_NAVIGATION_MODE";
    /**
     * Argument designating the search query if fragment is in search mode.
     * <p/>
     * Type: String
     */
    public static final String ARG_SEARCH_QUERY = "ARG_SEARCH_QUERY";
    /**
     * Reason for requesting an auth token.
     */
    public static final int AUTH_REASON_GET_JOURNALS = 0, AUTH_REASON_YOUR_JOURNALS = 1,
            AUTH_REASON_SEARCH_JOURNALS = 2, AUTH_REASON_CREATE_JOURNAL = 3,
            AUTH_REASON_DELETE_JOURNAL = 4;

    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = JournalListFragment.class.getSimpleName();
    /**
     * Dialog types for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_ADD_JOURNAL = 0, DIALOG_TYPE_EDIT_JOURNAL = 1,
            DIALOG_TYPE_CONFIRM_DELETE_JOURNAL = 2, DIALOG_TYPE_INSERT_FAILED_JOURNAL = 3,
            DIALOG_TYPE_MESSAGE = 4;
    /**
     * Number of journals to download from the server at a time.
     */
    private static final int PAGE_COUNT = 10;
    /**
     * Request code for obtaining permissions to access location services.
     */
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 1;
    /**
     * Flags used for marking the current state of inserting a new journal into the list.
     * <p/>
     * Used for automatically opening the new journal after it has been loaded into the list.
     */
    private static final long NEW_JOURNAL_LOAD_NONE = -1L, NEW_JOURNAL_LOAD_PENDING = -2L,
            NEW_JOURNAL_LOAD_COMPLETE = -3L;
    /**
     * Tag for dialogs launched from this fragment.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Saved instance state key for storing journal state downloaded from the server.
     * <p/>
     * Type: Parcelable
     */
    private static final String SAVE_STATE_JOURNALS = "com:blyspot:blyspot:SAVE_STATE_JOURNALS";
    /**
     * Columns used in the journal cursor projection.
     */
    private static final String COLUMN_SERVER_ID = "server_id", COLUMN_PROVIDER_ID = "provider_id",
            COLUMN_JOURNAL_IMAGE = "image_url", COLUMN_GENDER = "gender",
            COLUMN_IS_EDITABLE = "is_editable";
    /**
     * Columns in a journal matrix cursor.
     */
    private static final String[] JOURNAL_COLUMNS =
            new String[]{Journals.COLUMN_ID, Journals.COLUMN_ACCOUNT_NAME, Journals.COLUMN_TITLE,
                    COLUMN_JOURNAL_IMAGE, COLUMN_PROVIDER_ID, COLUMN_GENDER, COLUMN_IS_EDITABLE};
    /**
     * Projection for building a journal cursor loader.
     */
    private static final String[] JOURNAL_QUERY_PROJECTION =
            new String[]{Journals.COLUMN_ID, Journals.COLUMN_TITLE, Journals.COLUMN_SERVER_ID,
                    Journals.COLUMN_IS_DELETED};
    /**
     * Selection argument for building a journal cursor loader.
     */
    private static final String JOURNAL_QUERY_SELECTION = Journals.COLUMN_ACCOUNT_NAME + "=?";
    /**
     * Sort order for building a journal cursor loader.
     */
    private static final String JOURNAL_QUERY_SORT_ORDER = Journals.COLUMN_CREATED + " ASC";

    /**
     * Array of journals managed by this fragment's adapter.
     * <p/>
     * Only used if journals are downloaded from server.
     */
    private final ArrayList<JournalInfo> mJournals = new ArrayList<>();
    /**
     * The action mode that the fragment is currently in.
     */
    private ActionMode mActionMode = null;
    /**
     * Set whether there are more journals available to download from the server.
     */
    private boolean mHasMoreJournals;
    /**
     * Set whether journals are currently being downloaded from the network.
     */
    private boolean mIsLoading = false;
    /**
     * Parcelable containing recycler fragment saved state. Used to restore state when adapter is
     * finished loading asynchronously on fragment restart.
     */
    private Bundle mRecyclerFragmentSavedState = null;
    /**
     * Cursor obtained from the provider using a cursor loader.
     */
    private Cursor mProviderCursor = null;
    /**
     * Cursor containing data about the user this fragment is associated with.
     */
    //    private Cursor mAccountCursor; TODO
    /**
     * Client for accessing Google API location manager.
     */
    private GoogleApiClient mGoogleApiClient = null;
    /**
     * The currently selected journal for editing or deletion.
     */
    private int mSelectedJournal = -1;
    /**
     * The fragment's callback object, which is notified of changes to the fragment.
     */
    private JournalListFragmentListener mListener = null;
    /**
     * Current location of device.
     * <p/>
     * Only used when navigation mode is {@link Constants#NAVIGATION_MODE_NEARBY}.
     */
    private Location mCurrentLocation = null;
    /**
     * ID of a newly created journal.  Can be the journal ID itself or {@link
     * #NEW_JOURNAL_LOAD_NONE}, {@link #NEW_JOURNAL_LOAD_PENDING} or {@link
     * #NEW_JOURNAL_LOAD_COMPLETE}.
     */
    private long mNewJournalId = NEW_JOURNAL_LOAD_NONE;
    /**
     * Layout for handling swipe refreshes.
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * Move journal media information from a list of journals into the activity it is associated
     * with.
     *
     * @param journals list of journals to move media information from
     */
    private static void putJournalMediaInActivities(@NonNull ArrayList<Journal> journals) {
        for (Journal j : journals) {
            if (j.activities != null) { // TODO shouldn't need this
                for (Activity a : j.activities) {
                    a.media = new ArrayList<>();
                    for (Iterator<Media> iterator = j.media.iterator(); iterator.hasNext(); ) {
                        final Media media = iterator.next();
                        if (media.activityId == a.activityId) {
                            a.media.add(media);
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        // Set the layout manager.
        final int spanCount = getResources().getInteger(R.integer.column_count) / 4;
        //        final SpanSizeLookup lookup = new SpanSizeLookup(spanCount); TODO
        final StaggeredGridLayoutManager manager =
                new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        //        manager.setSpanSizeLookup(lookup); TODO
        getRecyclerView().setLayoutManager(manager);
        // Set recycler view padding.
        final int padding = getResources().getDimensionPixelOffset(R.dimen.spacing_grid_item);
        getRecyclerView().setPadding(padding, padding, padding, padding);
        // Set up the swipe to refresh layout.
        mSwipeRefreshLayout =
                (SwipeRefreshLayout) getView().findViewById(R.id.recycler_listContainer);
        mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener());

        // *****Load journals from saved state.***** //
        if (savedInstanceState != null) {
            final ArrayList<JournalInfo> savedJournals =
                    savedInstanceState.getParcelableArrayList(SAVE_STATE_JOURNALS);
            mJournals.addAll(savedJournals);
            // Create and set the adapter.
            final JournalFooterAdapter adapter =
                    new JournalFooterAdapter(new JournalListAdapter(mJournals));
            setAdapter(adapter);
            // Show footer if there might be more journals available.
            if (getArguments().getInt(ARG_NAVIGATION_MODE) ==
                    Constants.NAVIGATION_MODE_TOP_JOURNALS) {
                mHasMoreJournals = true;
                adapter.setHasExtraItem(true);
            }
        }

        // *****Get "nearby" journals from the server.***** //
        else if (getArguments().getInt(ARG_NAVIGATION_MODE) == Constants.NAVIGATION_MODE_NEARBY) {
            // Set up Google API client for interacting with location services.
            if (mGoogleApiClient == null) {
                final GoogleApiCallbacks callbacks = new GoogleApiCallbacks();
                mGoogleApiClient =
                        new GoogleApiClient.Builder(getContext()).addApi(LocationServices.API)
                                .addConnectionCallbacks(callbacks)
                                .addOnConnectionFailedListener(callbacks).build();
            }
        }

        // *****Get all other journal types from the server.***** //
        else {
            // Load journals in the provider.
            if (getArguments().getString(Constants.ARG_ACCOUNT_NAME) != null) {
                getLoaderManager().initLoader(0, null, new JournalLoaderCallbacks());
            }
            startJournalsRequest();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DIALOG_TYPE_ADD_JOURNAL:
                if (resultCode == RESULT_OK) {
                    // Upload journal to server.
                    final CreateJournalRequest request = new CreateJournalRequest();
                    request.title = data.getStringExtra(NewJournalDialogFragment.ARG_JOURNAL_TITLE);
                    // TODO constant value
                    request.isPublic = data.getIntExtra(NewJournalDialogFragment.ARG_PRIVACY, 1);
                    AccountUtils.getAuthToken(getActivity(), AUTH_REASON_CREATE_JOURNAL,
                            getArguments().getString(Constants.ARG_ACCOUNT_NAME),
                            new AuthTokenListener(request));
                }
                break;
            case DIALOG_TYPE_EDIT_JOURNAL:
                if (resultCode == RESULT_OK) {
                    // Update journal on server.
                    final CreateJournalRequest request = new CreateJournalRequest();
                    request.title = data.getStringExtra(NewJournalDialogFragment.ARG_JOURNAL_TITLE);
                    request.isPublic = data.getIntExtra(NewJournalDialogFragment.ARG_PRIVACY, 1);
                    request.id = mJournals.get(mSelectedJournal).journal.journalId;
                    AccountUtils
                            .getAuthToken(getActivity(), AUTH_REASON_CREATE_JOURNAL,
                                    getArguments().getString(Constants.ARG_ACCOUNT_NAME),
                                    new AuthTokenListener(request));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Attach this fragment's context as a callback.
        mListener = (JournalListFragmentListener) context;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Set journal directory menu items.
        if (getArguments().getString(Constants.ARG_ACCOUNT_NAME) != null) {
            inflater.inflate(R.menu.menu_journal_directory, menu);
        }
        // Set "search" action menu item.
        inflater.inflate(R.menu.menu_search, menu);
        final MenuItem search = menu.findItem(R.id.action_search);
        final SearchManager searchManager = (SearchManager) getContext()
                .getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) search.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));
        // Set "refresh" action menu item.
        inflater.inflate(R.menu.menu_refresh, menu);
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
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent loader from restoring saved state if it has not completed before view is
        // destroyed.
        if (mRecyclerFragmentSavedState != null) {
            mRecyclerFragmentSavedState = null;
        }
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
            case DIALOG_TYPE_ADD_JOURNAL:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    final String title = data.getStringExtra(JournalDialogFragment.DIALOG_DATA);
                    //                    insertJournalAndOpen(title); TODO
                    // Upload journal to server.
                    final CreateJournalRequest request = new CreateJournalRequest();
                    request.title = title;
                    request.isPublic = 1;
                    AccountUtils
                            .getAuthToken(getActivity(), AUTH_REASON_CREATE_JOURNAL,
                                    getArguments().getString(Constants.ARG_ACCOUNT_NAME),
                                    new AuthTokenListener(request));
                }
                break;
            case DIALOG_TYPE_EDIT_JOURNAL:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    final String title = data.getStringExtra(JournalDialogFragment.DIALOG_DATA);
                    // Update journal on server.
                    final CreateJournalRequest request = new CreateJournalRequest();
                    request.title = (!TextUtils.isEmpty(title)) ? title : null;
                    request.isPublic = 1;
                    request.id = mJournals.get(mSelectedJournal).journal.journalId;
                    AccountUtils.getAuthToken(getActivity(), AUTH_REASON_CREATE_JOURNAL,
                            getArguments().getString(Constants.ARG_ACCOUNT_NAME),
                            new AuthTokenListener(request));
                }
                break;
            case DIALOG_TYPE_CONFIRM_DELETE_JOURNAL:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    //                    deleteJournal();
                    // Delete journal from server.
                    final DeleteJournalRequest request = new DeleteJournalRequest();
                    request.id = mJournals.get(mSelectedJournal).journal.journalId;
                    AccountUtils
                            .getAuthToken(getActivity(), AUTH_REASON_DELETE_JOURNAL,
                                    getArguments().getString(Constants.ARG_ACCOUNT_NAME),
                                    new AuthTokenListener(request));
                }
                break;
            case DIALOG_TYPE_INSERT_FAILED_JOURNAL:
            case DIALOG_TYPE_MESSAGE:
                // Do nothing.
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mSwipeRefreshLayout.setRefreshing(true);
                new OnRefreshListener().onRefresh();
                return true;
            case R.id.action_search:
                // Do nothing, action handled directly.
                return true;
            case R.id.action_new:
                showDialog(DIALOG_TYPE_ADD_JOURNAL, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (mGoogleApiClient.isConnected()) {
                        mCurrentLocation =
                                LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        if (mCurrentLocation != null) {
                            startJournalsRequest();
                        }
                        // The current location could not be determined.
                        else {
                            showDialog(DIALOG_TYPE_MESSAGE,
                                    getString(R.string.dialog_message_error_locationUnknown));
                            // Show an empty list.
                            handleResponse(new ArrayList<Journal>(0), 0);
                        }
                    }
                }
                // Permission to access location was denied.
                else {
                    // TODO Show an error message indicating permission is required for this feature
                    showDialog(DIALOG_TYPE_MESSAGE,
                            getString(R.string.dialog_message_error_locationUnknown));
                    // Show an empty list.
                    handleResponse(new ArrayList<Journal>(0), 0);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(SAVE_STATE_JOURNALS, mJournals);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Delete the selected journal from the provider.
     */
    private void deleteJournal() {
        final JournalInfo journalInfo = mJournals.get(mSelectedJournal);
        final Account account = AccountUtils
                .getAccountFromName(getContext(),
                        getArguments().getString(Constants.ARG_ACCOUNT_NAME));
        final QueryHandler handler = new QueryHandler(getContext().getContentResolver());

        // *****The journal has an existing entry in the provider.***** //
        if (journalInfo.providerId != RecyclerView.NO_ID) {
            final Uri uri =
                    ContentUris.withAppendedId(Journals.CONTENT_URI, journalInfo.providerId);
            // The journal also has a corresponding entry on the server.
            if (journalInfo.journal.journalId != RecyclerView.NO_ID) {
                final ContentValues values = new ContentValues(1);
                values.put(Journals.COLUMN_IS_DELETED, 2);
                handler.setQueryListener(new RequestSyncQueryListener(account));
                handler.startUpdate(0, null, uri, values, null, null);
            }
            // The journal only exists in the provider (not yet uploaded).
            else {
                handler.startDelete(0, null, uri, null, null);
            }

        }

        // *****The journal does not have an existing entry in the provider.***** //
        else {
            // Create new provider entry and mark as deleted.
            final ContentValues values = new ContentValues(3);
            values.put(Journals.COLUMN_ACCOUNT_NAME,
                    getArguments().getString(Constants.ARG_ACCOUNT_NAME));
            values.put(Journals.COLUMN_SERVER_ID, journalInfo.journal.journalId);
            values.put(Journals.COLUMN_IS_DELETED, 2);
            handler.setQueryListener(new RequestSyncQueryListener(account));
            handler.startInsert(0, null, Journals.CONTENT_URI, values);
        }

        mJournals.remove(mSelectedJournal);
        getAdapter().notifyItemRemoved(mSelectedJournal);
        mSelectedJournal = -1; // TODO constant value
    }

    /**
     * Start a new request.  Used for request types that will download a list of journals from the
     * server.
     */
    private void startJournalsRequest() {
        final String accountName = getArguments().getString(Constants.ARG_ACCOUNT_NAME);
        switch (getArguments().getInt(ARG_NAVIGATION_MODE)) {
            // Start a "get journals" request.
            case Constants.NAVIGATION_MODE_TOP_JOURNALS:
                final GetJournalsRequest getRequest = new GetJournalsRequest();
                if (mSwipeRefreshLayout.isRefreshing()) {
                    getRequest.pageIndex = 1;
                } else {
                    getRequest.pageIndex = mJournals.size() + 1;
                }
                getRequest.pageCount = PAGE_COUNT;
                if (accountName != null) {
                    AccountUtils.getAuthToken(getActivity(), AUTH_REASON_GET_JOURNALS, accountName,
                            new AuthTokenListener(getRequest));
                } else {
                    final GetJournalsResponseListener listener = new GetJournalsResponseListener();
                    final GsonRequest gsonRequest =
                            new GsonRequest<>(getRequest.getUrl(getContext()),
                                    GetJournalsResponse.class,
                                    NetworkUtils.getDataTransferHeaders(getContext(), null),
                                    new Gson().toJson(getRequest), listener, listener);
                    startRequest(gsonRequest);
                }
                break;
            // Start a "your journals" request.
            case Constants.NAVIGATION_MODE_YOUR_JOURNALS:
                final MyJournalsRequest myRequest = new MyJournalsRequest();
                if (accountName != null) {
                    AccountUtils.getAuthToken(getActivity(), AUTH_REASON_YOUR_JOURNALS, accountName,
                            new AuthTokenListener(myRequest));
                } else {
                    throw new IllegalArgumentException(); // Should never happen.
                }
                break;
            // Start a "nearby journals" request.
            case Constants.NAVIGATION_MODE_NEARBY:
                if (mCurrentLocation != null) {
                    final SearchJournalsRequest nearbyRequest = new SearchJournalsRequest();
                    nearbyRequest.pageIndex = 1;
                    nearbyRequest.pageCount = PAGE_COUNT;
                    nearbyRequest.latitude = mCurrentLocation.getLatitude();
                    nearbyRequest.longitude = mCurrentLocation.getLongitude();
                    if (accountName != null) {
                        AccountUtils.getAuthToken(getActivity(), AUTH_REASON_SEARCH_JOURNALS,
                                AccountUtils.getActiveAccount(getContext()),
                                new AuthTokenListener(nearbyRequest));
                    } else {
                        final SearchJournalsResponseListener listener =
                                new SearchJournalsResponseListener();
                        final GsonRequest gsonRequest =
                                new GsonRequest<>(nearbyRequest.getUrl(getContext()),
                                        SearchJournalsResponse.class,
                                        getDataTransferHeaders(getContext(), null),
                                        new Gson().toJson(nearbyRequest), listener, listener);
                        startRequest(gsonRequest);
                    }
                } else {
                    handleResponse(new ArrayList<Journal>(0), 0);
                }
                break;
            // Start a "search journal" request.
            case Constants.NAVIGATION_MODE_SEARCH_RESULTS:
                final SearchJournalsRequest searchRequest = new SearchJournalsRequest();
                searchRequest.searchText = getArguments().getString(ARG_SEARCH_QUERY);
                if (mSwipeRefreshLayout.isRefreshing()) {
                    searchRequest.pageIndex = 1;
                } else {
                    searchRequest.pageIndex = mJournals.size() + 1;
                }
                searchRequest.pageCount = PAGE_COUNT;
                if (accountName != null) {
                    AccountUtils
                            .getAuthToken(getActivity(), AUTH_REASON_SEARCH_JOURNALS, accountName,
                                    new AuthTokenListener(searchRequest));
                } else {
                    final SearchJournalsResponseListener listener =
                            new SearchJournalsResponseListener();
                    final GsonRequest gsonRequest =
                            new GsonRequest<>(searchRequest.getUrl(getContext()),
                                    SearchJournalsResponse.class,
                                    getDataTransferHeaders(getContext(), null),
                                    new Gson().toJson(searchRequest), listener, listener);
                    startRequest(gsonRequest);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Insert a new journal into the provider.
     * <p/>
     * The journal will be opened if this fragment is still visible when the insert is complete.
     *
     * @param title the title of the new journal
     */
    private void insertJournalAndOpen(String title) {
        // Let fragment know a new journal insert is pending.
        mNewJournalId = NEW_JOURNAL_LOAD_PENDING;
        // Insert new journal on background thread.
        final ContentValues values = new ContentValues(2);
        values.put(Journals.COLUMN_TITLE, title);
        final String accountName = getArguments().getString(Constants.ARG_ACCOUNT_NAME);
        values.put(Journals.COLUMN_ACCOUNT_NAME, accountName);
        final QueryHandler handler = new QueryHandler(getContext().getContentResolver());
        handler.setQueryListener(new SimpleQueryListener() {

            @Override
            public void onInsertComplete(int token, Object cookie, Uri uri) {
                // Check whether fragment is still resumed.
                if (isResumed()) {
                    if (uri != null) {
                        final long id = ContentUris.parseId(uri);
                        // The adapter has not been reloaded yet.
                        if (mNewJournalId == NEW_JOURNAL_LOAD_PENDING) {
                            // Pass the new journal ID to the loader.
                            mNewJournalId = id;
                        }
                        // The list has been reloaded to reflect the insert.
                        else if (mNewJournalId == NEW_JOURNAL_LOAD_COMPLETE) {
                            // Find the item with the given ID in the adapter.
                            final Adapter adapter = getAdapter();
                            final int count = adapter.getItemCount();
                            for (int i = 0; i < count; i++) {
                                if (adapter.getItemId(i) == id) {
                                    // Update the state of the selected journal.
                                    //                                    setActivatedPosition(i); TODO
                                    break;
                                }
                            }
                            mNewJournalId = NEW_JOURNAL_LOAD_NONE;
                        }
                        // Open the new journal.
                        mListener.onItemClicked(JournalListFragment.this, uri);
                    }
                    // The journal insertion failed.
                    else {
                        mNewJournalId = NEW_JOURNAL_LOAD_NONE;
                        showDialog(DIALOG_TYPE_INSERT_FAILED_JOURNAL, null);
                    }
                }
                // The fragment is not resumed, do not open journal.
                else {
                    mNewJournalId = NEW_JOURNAL_LOAD_NONE;
                }
            }

        });
        handler.startInsert(0, null, Journals.CONTENT_URI, values);
    }

    /**
     * Update the specified list of journals with data from the provider.
     *
     * @param journals        list of journals to update
     * @param isUpdateAdapter {@code true} if adapter should notify observers as changes are made,
     *                        {@code false} if adapter should not notify observers of changes
     */
    private void updateJournalListRange(@NonNull List<Journal> journals,
                                        boolean isUpdateAdapter) {
        //        if (mProviderCursor != null) { TODO
        //            final Adapter adapter = getAdapter();
        //            for (Iterator<JournalInfo> iterator = journals.iterator(); iterator.hasNext(); ) {
        //                final JournalInfo info = iterator.next();
        //                mProviderCursor.moveToPosition(-1);
        //                while (mProviderCursor.moveToNext()) {
        //                    final long serverId = mProviderCursor.getLong(
        //                            mProviderCursor.getColumnIndexOrThrow(Journals.COLUMN_SERVER_ID));
        //                    if (info.journal.journalId == serverId) {
        //                        // Delete journal if marked for deletion.
        //                        if (mProviderCursor.getInt(mProviderCursor
        //                                .getColumnIndexOrThrow(Journals.COLUMN_IS_DELETED)) == 2) {
        //                            iterator.remove();
        //                        }
        //                        // Update journal with provider data.
        //                        else {
        //                            info.journal.title = mProviderCursor.getString(
        //                                    mProviderCursor.getColumnIndexOrThrow(Journals.COLUMN_TITLE));
        //                            info.providerId = mProviderCursor
        //                                    .getLong(mProviderCursor
        //                                            .getColumnIndexOrThrow(Journals.COLUMN_ID));
        //                            if (isUpdateAdapter && (adapter != null)) {
        //                            }
        //                        }
        //                        break;
        //                    }
        //                }
        //            }
        //        }
    }

    /**
     * Add any provider-only journals (journals not yet uploaded to the server) to the beginning of
     * the journal list, replacing any existing provider-only journals that may already be present.
     * <p/>
     * This method will notify the adapter of any changes made, if applicable.
     *
     * @return the number of journals inserted at the beginning of the list
     */
    private int replaceProviderOnlyJournals() { // TODO
        //        final JournalFooterAdapter adapter = (JournalFooterAdapter) getAdapter();
        //        int insertPosition = 0;
        //        // Remove any existing provider-only journals.
        //        final Iterator<JournalInfo> iterator = mJournals.iterator();
        //        while (iterator.hasNext()) {
        //            final JournalInfo info = iterator.next();
        //            if (info.journal.journalId == RecyclerView.NO_ID) {
        //                iterator.remove();
        //                if (adapter != null) {
        //                    adapter.notifyItemRemoved(0);
        //                }
        //            }
        //            // No more provider-only entries are present.
        //            else {
        //                break;
        //            }
        //        }
        //        // Add new provider-only journals to beginning of list.
        //        final String email = getArguments().getString(ARG_ACCOUNT_NAME);
        //        mProviderCursor.moveToPosition(-1);
        //        while (mProviderCursor.moveToNext()) {
        //            if (mProviderCursor
        //                    .isNull(mProviderCursor.getColumnIndexOrThrow(Journals.COLUMN_SERVER_ID))) {
        //                // Create a new journal from the provider data.
        //                final Journal journal = new Journal();
        //                journal.userAlias = AccountUtils.getUserDataAlias(getContext(), email);
        //                journal.userId = AccountUtils.getUserDataId(getContext(), email);
        //                journal.journalId = RecyclerView.NO_ID;
        //                journal.title = mProviderCursor.getString(
        //                        mProviderCursor.getColumnIndexOrThrow(Journals.COLUMN_TITLE));
        //                journal.userGender = AccountUtils.getUserDataGender(getContext(), email);
        //                // Create a new journal info and add to the list.
        //                final JournalInfo info = new JournalInfo();
        //                info.journal = journal;
        //                info.providerId = mProviderCursor
        //                        .getLong(mProviderCursor.getColumnIndexOrThrow(Journals.COLUMN_ID));
        //                info.isEditable = true;
        //                mJournals.add(insertPosition, info);
        //                if (adapter != null) {
        //                    adapter.notifyItemInserted(insertPosition);
        //                }
        //                insertPosition++;
        //            }
        //        }
        //        return insertPosition;
        return 0;
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
            case DIALOG_TYPE_ADD_JOURNAL:
                dialog = new NewJournalDialogFragment();
                Bundle args = new Bundle();
                args.putString(NewJournalDialogFragment.ARG_DIALOG_TITLE,
                        getString(R.string.dialog_message_newJournal));
                args.putString(NewJournalDialogFragment.ARG_POSITIVE_BUTTON_TEXT,
                        getString(R.string.dialog_positiveButton_create));
                dialog.setArguments(args);
                break;
            case DIALOG_TYPE_EDIT_JOURNAL:
                dialog = new NewJournalDialogFragment();
                final Journal journal = mJournals.get(mSelectedJournal).journal;
                args = new Bundle();
                args.putString(NewJournalDialogFragment.ARG_DIALOG_TITLE,
                        getString(R.string.dialog_message_editJournal));
                args.putString(NewJournalDialogFragment.ARG_JOURNAL_TITLE, journal.title);
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
            case DIALOG_TYPE_INSERT_FAILED_JOURNAL:
                dialog = JournalDialogFragment.newMessageDialog
                        (dialogType, R.string.dialog_message_error_insertJournal,
                                R.string.dialog_positiveButton_ok, false);
                break;
            case DIALOG_TYPE_MESSAGE:
                dialog = JournalDialogFragment
                        .newMessageDialog(dialogType, message, R.string.dialog_positiveButton_ok,
                                false);
                break;
            default:
                throw new IllegalArgumentException();
        }
        dialog.setTargetFragment(this, dialogType);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Update a journal in the provider.
     * <p/>
     * This method should only be called when there is exactly one journal selected in the list.
     *
     * @param title the updated journal title
     */
    private void updateJournal(String title) {
        final JournalInfo journalInfo = mJournals.get(mSelectedJournal);
        final Account account = AccountUtils
                .getAccountFromName(getContext(),
                        getArguments().getString(Constants.ARG_ACCOUNT_NAME));
        final QueryHandler handler = new QueryHandler(getContext().getContentResolver());
        handler.setQueryListener(new RequestSyncQueryListener(account));

        // *****The journal has an existing entry in the provider.***** //
        if (journalInfo.providerId != RecyclerView.NO_ID) {
            final Uri uri =
                    ContentUris.withAppendedId(Journals.CONTENT_URI, journalInfo.providerId);
            // The journal also has a corresponding entry on the server.
            final ContentValues values = new ContentValues(1);
            values.put(Journals.COLUMN_TITLE, title);
            handler.startUpdate(0, null, uri, values, null, null);
        }

        // *****The journal does not have an existing entry in the provider.***** //
        else {
            // Create new provider entry.
            final ContentValues values = new ContentValues(3);
            values.put(Journals.COLUMN_ACCOUNT_NAME, account.name);
            values.put(Journals.COLUMN_TITLE, title);
            values.put(Journals.COLUMN_SERVER_ID, journalInfo.journal.journalId);
            handler.startInsert(0, null, Journals.CONTENT_URI, values);
        }

        journalInfo.journal.title = title;
        getAdapter().notifyItemChanged(mSelectedJournal);
        mSelectedJournal = -1; // TODO constant value
    }

    private void handleResponse(ArrayList<Journal> journals, int totalCount) {
        mIsLoading = false;
        // Recreate the journal list data if refreshing.
        if (mSwipeRefreshLayout.isRefreshing()) {
            mJournals.clear();
            // Add journals that only exist in the provider.
            if (getArguments().getInt(ARG_NAVIGATION_MODE) ==
                    Constants.NAVIGATION_MODE_YOUR_JOURNALS) {
                replaceProviderOnlyJournals();
            }
        }
        // Add newly downloaded journals to the existing data.
        JournalFooterAdapter adapter = (JournalFooterAdapter) getAdapter();
        if (journals != null) {
            // Move the activity media into the "activity" objects.
            putJournalMediaInActivities(journals);
            // Update new journals with provider data.
            updateJournalListRange(journals, false);
            // Add journals to the list, marking as editable if created by current user.
            final String accountName = getArguments().getString(Constants.ARG_ACCOUNT_NAME);
            long userId = AccountUtils.NO_USER_DATA;
            if (accountName != null) {
                userId = AccountUtils.getUserDataId(getContext(), accountName);
            }
            for (Journal j : journals) {
                final JournalInfo info = new JournalInfo();
                info.journal = j;
                if (j.userId == userId) {
                    info.isEditable = true;
                }
                mJournals.add(info);
            }
            // Update the adapter, creating it if required.
            if (adapter == null) {
                final JournalListAdapter wrappedAdapter = new JournalListAdapter(mJournals);
                adapter = new JournalFooterAdapter(wrappedAdapter);
                setAdapter(adapter);
            } else if (mSwipeRefreshLayout.isRefreshing()) {
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyItemRangeInserted(mJournals.size() - journals.size(),
                        journals.size());
            }
            // Show a indeterminant progress footer if there are more items.
            if ((getArguments().getString(Constants.ARG_ACCOUNT_NAME) != null) &&
                    (totalCount > mJournals.size())) {
                adapter.setHasExtraItem(true);
                mHasMoreJournals = true;
            } else {
                adapter.setHasExtraItem(false);
                mHasMoreJournals = false;
            }
        }
        // The response is "null" (no journals were returned).
        else {
            // Create an empty adapter.
            if (adapter == null) {
                final JournalListAdapter wrappedAdapter = new JournalListAdapter(mJournals);
                adapter = new JournalFooterAdapter(wrappedAdapter);
                setAdapter(adapter);
            }
            adapter.setHasExtraItem(false);
            mHasMoreJournals = false;
        }
        // Clear the swipe refresh animation.
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * A callback to parent activities of this fragment to notify them of changes to the fragment.
     * <p/>
     * All activities containing this fragment must implement this callback interface.
     */
    public interface JournalListFragmentListener {

        /**
         * Callback to be invoked when an item in the recycler view has been clicked.
         *
         * @param fragment the fragment containing the list item that was clicked
         * @param uri      the URI associated with the item that was clicked
         */
        void onItemClicked(@NonNull JournalListFragment fragment, @NonNull Uri uri);

        /**
         * Callback to be invoked when an item in the recycler view has been clicked.
         *
         * @param fragment       the fragment containing the list item that was clicked
         * @param journal        the journal associated with the item that was clicked
         * @param transitionView view used to supply a transition animation to the next activity, if
         *                       applicable
         */
        void onItemClicked(@NonNull JournalListFragment fragment, @NonNull Journal
                journal, @Nullable View transitionView);

    }

    /**
     * Fragment for showing a new journal dialog.
     */
    public static class NewJournalDialogFragment extends DialogFragment {

        public static final String ARG_DIALOG_TITLE = "ARG_DIALOG_TITLE";
        public static final String ARG_JOURNAL_TITLE = "ARG_JOURNAL_TITLE";
        public static final String ARG_PRIVACY = "ARG_PRIVACY";
        public static final String ARG_POSITIVE_BUTTON_TEXT = "ARG_POSITIVE_BUTTON_TEXT";

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // Set up the toolbar.
            if (!getShowsDialog()) {
                final Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);
                final ItemClickListener listener = new ItemClickListener();
                toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp);
                toolbar.setNavigationContentDescription(R.string.contentDescription_cancel);
                toolbar.setNavigationOnClickListener(listener);
                toolbar.setTitle(getArguments().getString(ARG_DIALOG_TITLE));
                toolbar.inflateMenu(R.menu.menu_save);
                toolbar.setOnMenuItemClickListener(listener);
            }
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            // Set up the dialog view.
            final View view = LayoutInflater.from(getContext())
                    .inflate(R.layout.journal_dialog_fragment_edittext, null);
            if (savedInstanceState == null) {
                // Set the journal title.
                final EditText text = (EditText) view.findViewById(R.id.text_title);
                text.setText(args.getString(ARG_JOURNAL_TITLE));
                // Set the privacy.
                final RadioGroup radio = (RadioGroup) view.findViewById(R.id.layout_radio);
                switch (args.getInt(ARG_PRIVACY, 1)) {
                    case 1:
                        radio.check(R.id.radio_public);
                        break;
                    case 2:
                        radio.check(R.id.radio_private);
                    default:
                        throw new IllegalArgumentException();
                }

            }
            return new Builder(getActivity()).setTitle(args.getString(ARG_DIALOG_TITLE))
                    .setView(view).setPositiveButton(args.getString(ARG_POSITIVE_BUTTON_TEXT),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent data = new Intent();
                                    // Add title information to intent.
                                    final EditText title =
                                            (EditText) getDialog().findViewById(R.id.text_title);
                                    data.putExtra(ARG_JOURNAL_TITLE, title.getText().toString());
                                    // Add privacy information to intent.
                                    final RadioGroup radio = (RadioGroup) getDialog()
                                            .findViewById(R.id.layout_radio);
                                    data.putExtra(ARG_PRIVACY,
                                            (radio.getCheckedRadioButtonId() == R.id.radio_public) ?
                                                    1 : 2);
                                    getTargetFragment()
                                            .onActivityResult(getTargetRequestCode(), RESULT_OK,
                                                    data);
                                }

                            })
                    .setNegativeButton(R.string.dialog_negativeButton, null).create();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            // Fragment will be embedded in its activity.
            if (!getShowsDialog()) {
                final View parent = inflater.inflate(R.layout.toolbar_content, container, false);
                final ViewGroup content = (ViewGroup) parent.findViewById(R.id.content_container);
                inflater.inflate(R.layout.journal_dialog_fragment_edittext, content, true);
                return parent;
            }
            // Fragment will be shown as a dialog.
            else {
                return super.onCreateView(inflater, container, savedInstanceState);
            }
        }

        private class ItemClickListener implements OnMenuItemClickListener, OnClickListener {

            @Override
            public void onClick(View v) {
                getActivity().finish();
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_save:
                        return true;
                    default:
                        return false;
                }
            }

        }

    }

    /**
     * A class representing the state associated with a journal.
     */
    private static class JournalInfo implements Parcelable {

        public static final Creator<JournalInfo> CREATOR = new Creator<JournalInfo>() {

            @Override
            public JournalInfo createFromParcel(Parcel source) {
                return new JournalInfo(source);
            }

            @Override
            public JournalInfo[] newArray(int size) {
                return new JournalInfo[size];
            }

        };

        /**
         * Journal to provide additional info for.
         */
        public Journal journal;
        /**
         * Content provider ID associated with this journal.
         */
        public long providerId = RecyclerView.NO_ID;
        /**
         * Set whether the journal is editable.
         */
        public boolean isEditable = false;

        /**
         * Create a new journal info object.
         */
        public JournalInfo() {
        }

        /**
         * Create a new journal info object from a parcel.
         *
         * @param source the parcel to create a journal from
         */
        private JournalInfo(Parcel source) {
            journal = source.readParcelable(Journal.class.getClassLoader());
            providerId = source.readLong();
            isEditable = (source.readInt() == 1);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(journal, flags);
            dest.writeLong(providerId);
            dest.writeInt(isEditable ? 1 : 0);
        }

    }

    /**
     * A view holder for handling journal views.
     */
    private static class JournalViewHolder extends ViewHolder {

        /**
         * View for holding the user avatar.
         */
        public final NetworkImageView avatar;
        /**
         * View for holding journal image.
         */
        public final NetworkImageView image;
        /**
         * View for holding journal title text.
         */
        public final TextView title;
        /**
         * View for holding journal description text.
         */
        public final TextView subtitle;

        /**
         * Create a new view holder.
         *
         * @param itemView the view being managed by this view holder
         */
        JournalViewHolder(View itemView) {
            super(itemView);
            avatar = (NetworkImageView) itemView.findViewById(R.id.avatar);
            image = (NetworkImageView) itemView.findViewById(R.id.image);
            title = (TextView) itemView.findViewById(R.id.text_title);
            subtitle = (TextView) itemView.findViewById(R.id.text_subtitle);
        }

    }

    /**
     * Query listener used to request account sync.
     */
    private static class RequestSyncQueryListener extends SimpleQueryListener {

        /**
         * Account to request a sync for.
         */
        private final Account mAccount;

        /**
         * Create a new query listener.
         *
         * @param account the account to request a sync for
         */
        RequestSyncQueryListener(@NonNull Account account) {
            mAccount = account;
        }

        @Override
        public void onInsertComplete(int token, Object cookie, Uri uri) {
            final Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            if (Constants.DEBUG) {
                Log.i(DEBUG_TAG, "Requesting SyncAdapter.");
            }
            ContentResolver.requestSync(mAccount, JournalContract.AUTHORITY, extras);
        }

        @Override
        public void onUpdateComplete(int token, Object cookie, int result) {
            final Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            if (Constants.DEBUG) {
                Log.i(DEBUG_TAG, "Requesting SyncAdapter.");
            }
            ContentResolver.requestSync(mAccount, JournalContract.AUTHORITY, extras);
        }

    }

    /**
     * Callback for interacting with a cursor loader manager for getting account data.
     */
    //    private class AccountLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> { TODO
    //
    //        @Override
    //        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    //            return new CursorLoader(getContext(), Accounts.CONTENT_URI, ACCOUNT_QUERY_PROJECTION,
    //                    ACCOUNT_QUERY_SELECTION,
    //                    new String[]{getArguments().getString(ARG_ACCOUNT_NAME)}, null);
    //        }
    //
    //        @Override
    //        public void onLoaderReset(Loader<Cursor> loader) {
    //            mAccountCursor = null;
    //        }
    //
    //        @Override
    //        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    //            if (data.moveToFirst()) {
    //                mAccountCursor = data;
    //            }
    //            // There is no account associated with this fragment.
    //            else {
    //                mAccountCursor = null;
    //            }
    //        }
    //
    //    }

    /**
     * Class for syncing changes to provider content.
     */
    private static class SyncContentObserver extends ContentObserver {

        /**
         * Account associated with this observer.
         */
        private final Account mAccount;
        /**
         * Authority of the provider for making sync requests.
         */
        private final String mAuthority;

        /**
         * Create a new content observer.
         *
         * @param handler the handler to run {@link #onChange(boolean)} on, or {@code null} for no
         *                handler
         */
        public SyncContentObserver(@Nullable Handler handler, @NonNull Account account,
                                   @NonNull String authority) {
            super(handler);
            mAccount = account;
            mAuthority = authority;
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri changeUri) {
            ContentResolver.requestSync(mAccount, mAuthority, null);
        }

    }

    /**
     * Class for transferring data with the server after receiving an auth token.
     */
    private class AuthTokenListener implements AccountUtils.AuthTokenListener {

        /**
         * Request for transferring data with the server.
         */
        private final ConnectionRequest mRequest;

        AuthTokenListener(@NonNull ConnectionRequest request) {
            mRequest = request;
        }

        @Override
        public void onAuthTokenReceived(int requestCode, String accountName, String authToken) {
            if (isStarted()) {
                final GsonRequest request;
                switch (requestCode) {
                    case AUTH_REASON_GET_JOURNALS:
                        final GetJournalsResponseListener getListener =
                                new GetJournalsResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                GetJournalsResponse.class,
                                getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(mRequest), getListener, getListener);
                        break;
                    case AUTH_REASON_YOUR_JOURNALS:
                        final GetJournalsResponseListener myListener =
                                new GetJournalsResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                GetJournalsResponse.class,
                                getDataTransferHeaders(getContext(), authToken), null, myListener,
                                myListener);
                        break;
                    case AUTH_REASON_SEARCH_JOURNALS:
                        final SearchJournalsResponseListener searchListener =
                                new SearchJournalsResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                SearchJournalsResponse.class,
                                getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(mRequest), searchListener, searchListener);
                        break;
                    case AUTH_REASON_CREATE_JOURNAL:
                        final CreateJournalResponseListener createListener =
                                new CreateJournalResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                CreateJournalResponse.class,
                                getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(mRequest), createListener, createListener);
                        break;
                    case AUTH_REASON_DELETE_JOURNAL:
                        final DeleteJournalResponseListener deleteListener =
                                new DeleteJournalResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(mRequest.getUrl(getContext()),
                                DeleteJournalResponse.class,
                                getDataTransferHeaders(getContext(), authToken),
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
        public void onAuthenticatorError(int requestCode) { // TODO show error message
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onOperationCancelled(int requestCode) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onIOError(int requestCode) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

    /**
     * Class for processing "create journal" responses.
     */
    private class CreateJournalResponseListener extends AuthFailureErrorListener
            implements Response.Listener<CreateJournalResponse> {

        /**
         * Create a new response listener.
         *
         * @param activity    activity used to access resources and launch a user credentials
         *                    dialog, if required
         * @param requestCode request code used to identify the request
         * @param accountName the name of the account
         * @param listener    listener for receiving auth token information
         * @param authToken   the auth token to invalidate in the event of an auth failure error
         */
        CreateJournalResponseListener(@NonNull android.app.Activity activity,
                                      int requestCode, @NonNull String accountName,
                                      @NonNull AuthTokenListener listener,
                                      @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showDialog(DIALOG_TYPE_MESSAGE, message);
            }
        }

        @Override
        public void onResponse(CreateJournalResponse response) {
            mSwipeRefreshLayout.setRefreshing(true);
            new OnRefreshListener().onRefresh();
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
        DeleteJournalResponseListener(@NonNull android.app.Activity activity,
                                      int requestCode, @NonNull String accountName,
                                      @NonNull AuthTokenListener listener,
                                      @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(DeleteJournalResponse response) {
            mSwipeRefreshLayout.setRefreshing(true);
            new OnRefreshListener().onRefresh();
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
     * Class for processing "get journals" responses.
     */
    private class GetJournalsResponseListener extends AuthFailureErrorListener
            implements Response.Listener<GetJournalsResponse> {

        /**
         * Create a new response listener.
         */
        GetJournalsResponseListener() {
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
        GetJournalsResponseListener(@NonNull android.app.Activity activity, int requestCode,
                                    @NonNull String accountName,
                                    @NonNull AuthTokenListener listener,
                                    @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Stop the server download at its current state.
            final JournalFooterAdapter adapter = (JournalFooterAdapter) getAdapter();
            if (adapter == null) {
                setAdapter(new JournalFooterAdapter(new JournalListAdapter(null)));
            } else {
                adapter.setHasExtraItem(false);
            }
            // Update the network loading status.
            mIsLoading = false;
            mHasMoreJournals = false;
            mSwipeRefreshLayout.setRefreshing(false);
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showDialog(DIALOG_TYPE_MESSAGE, message);
            }
        }

        @Override
        public void onResponse(GetJournalsResponse response) {
            handleResponse(response.journals, response.totalCount);
        }

    }

    /**
     * Listener for changes in the Google API connection status.
     */
    private class GoogleApiCallbacks implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            // Do not perform any action if the current location is already known.
            // TODO update location after a certain amount of time?
            if (mCurrentLocation == null) {
                // Check whether location services permission has been granted.
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
                    mCurrentLocation =
                            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (mCurrentLocation != null) {
                        startJournalsRequest();
                    } else {
                        showDialog(DIALOG_TYPE_MESSAGE,
                                getString(R.string.dialog_message_error_locationUnknown));
                        // Show an empty list.
                        handleResponse(new ArrayList<Journal>(0), 0);
                    }
                }
                // Request permission to access location services.
                else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_PERMISSION_LOCATION);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            showDialog(DIALOG_TYPE_MESSAGE, connectionResult.getErrorMessage());
            mGoogleApiClient = null;
        }

    }

    /**
     * Class for processing "search journals" responses.
     */
    private class SearchJournalsResponseListener extends AuthFailureErrorListener
            implements Response.Listener<SearchJournalsResponse> {

        /**
         * Create a new response listener.
         */
        SearchJournalsResponseListener() {
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
        SearchJournalsResponseListener(@NonNull android.app.Activity activity,
                                       int requestCode, @NonNull String accountName,
                                       @NonNull AuthTokenListener listener,
                                       @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(SearchJournalsResponse response) {
            switch (getArguments().getInt(ARG_NAVIGATION_MODE)) {
                case Constants.NAVIGATION_MODE_NEARBY:
                    handleResponse(response.records.nearbyJournals, 0);
                    break;
                case Constants.NAVIGATION_MODE_SEARCH_RESULTS:
                    handleResponse(response.records.searchJournals, 0);
                    break;
                default:
                    throw new IllegalArgumentException();
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
     * Adapter for displaying a list of journals.
     */
    //    private class JournalCursorAdapter extends CursorAdapter<ViewHolder>
    //            implements OnClickListener {
    //
    //        /**
    //         * Default layout resource ID for journals.
    //         */
    //        private static final int LAYOUT_ID = R.layout.list_item_card_header_media_action;
    //        /**
    //         * View types for displaying journals.
    //         */
    //        private static final int VIEW_TYPE_DEFAULT = 1, VIEW_TYPE_EDITABLE = 2;
    //
    //        /**
    //         * Loader and cache for images.
    //         */
    //        private final ImageLoaderOld mImageLoader;
    //
    //        /**
    //         * Create a new adapter.
    //         *
    //         * @param cursor the cursor to be managed by this adapter, may be {@code null} if the data
    //         *               is not yet available
    //         */
    //        public JournalCursorAdapter(@Nullable Cursor cursor) {
    //            super(cursor);
    //            mImageLoader = VolleySingleton.getInstance(getContext()).getImageLoader();
    //        }
    //
    //        @Override
    //        public int getItemViewType(int position) {
    //            final Cursor cursor = getCursor();
    //            cursor.moveToPosition(position);
    //            return (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_EDITABLE)) != 0) ?
    //                    VIEW_TYPE_EDITABLE : VIEW_TYPE_DEFAULT;
    //        }
    //
    //        @Override
    //        public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
    //            Log.i(DEBUG_TAG, "Binding item ID " + cursor.getLong(0));
    //            final JournalViewHolder journal = (JournalViewHolder) holder;
    //            // Set the avatar icon.
    //            final int gender = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
    //            journal.avatar.setDefaultImageResId(
    //                    (gender == 0) ? R.drawable.gender_male : R.drawable.gender_female);
    //            // Set the title text.
    //            final String title = cursor.getString(
    //                    cursor.getColumnIndexOrThrow(Journals.COLUMN_TITLE));
    //            if (!TextUtils.isEmpty(title)) {
    //                journal.title.setText(title);
    //            } else {
    //                journal.title.setText(R.string.label_default_title);
    //            }
    //            // Set the subtitle text.
    //            final String subtitle =
    //                    cursor.getString(cursor.getColumnIndexOrThrow(Journals.COLUMN_ACCOUNT_NAME));
    //            journal.subtitle.setText(subtitle);
    //            // Set the image. TODO also load provider images
    //            final String imageUri =
    //                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JOURNAL_IMAGE));
    //            journal.image.setImageUrl(imageUri, mImageLoader);
    //
    //            // *****Load more images if at the last item.***** //
    //            Log.i(DEBUG_TAG,
    //                    "Binding item " + (cursor.getPosition() + 1) + " of " + cursor.getCount());
    //            if (cursor.isLast()) {
    //                if (DEBUG) {
    //                    Log.i(DEBUG_TAG, "Binding last item.");
    //                }
    //                // Load more data to add to the end of the list.
    //                if (!mIsLoading) {
    //                    mIsLoading = true;
    //                    final String email = getArguments().getString(ARG_ACCOUNT_NAME);
    //                    if (email != null) {
    //                        AccountUtils
    //                                .getAuthToken(getActivity(), email, new AuthTokenListener());
    //                    } else {
    //                        getJournals(null);
    //                    }
    //                } else if (DEBUG) {
    //                    Log.i(DEBUG_TAG, "Binding last item while loading new journals.");
    //                }
    //            }
    //        }
    //
    //        @Override
    //        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    //            final View view =
    //                    LayoutInflater.from(parent.getContext()).inflate(LAYOUT_ID, parent, false);
    //            final JournalViewHolder holder = new JournalViewHolder(view);
    //            // Set listener for main action.
    //            final View main = view.findViewById(R.id.action_main);
    //            main.setTag(holder);
    //            main.setOnClickListener(this);
    //            // Set text and listener for sub-actions.
    //            switch (viewType) {
    //                case VIEW_TYPE_DEFAULT:
    //                    view.findViewById(R.id.layout_actions).setVisibility(View.GONE);
    //                    break;
    //                case VIEW_TYPE_EDITABLE:
    //                    // Set on click listeners for sub actions.
    //                    final TextView editView = (TextView) view.findViewById(R.id.action_1);
    //                    editView.setText(R.string.action_label_edit);
    //                    editView.setTag(holder);
    //                    editView.setOnClickListener(this);
    //                    final TextView deleteView = (TextView) view.findViewById(R.id.action_2);
    //                    deleteView.setText(R.string.action_label_delete);
    //                    deleteView.setTag(holder);
    //                    deleteView.setOnClickListener(this);
    //                    break;
    //                default:
    //                    throw new IllegalArgumentException();
    //            }
    //            return holder;
    //        }
    //
    //        @Override
    //        public void onClick(View view) {
    //            switch (view.getId()) {
    //                case R.id.action_main:
    //                    if (mListener != null) {
    //                        final ViewHolder holder = (ViewHolder) view.getTag();
    //                        //                        switch (getArguments().getInt(ARG_NAVIGATION_MODE)) {
    //                        //                            case DISPLAY_MODE_TOP_JOURNALS:
    //                        mListener.onItemClicked(JournalListFragment.this,
    //                                mJournals.get(holder.getAdapterPosition()));
    //                        //                        break;
    //                        //                            case DISPLAY_MODE_YOUR_JOURNALS:
    //                        //                                final Uri journalUri =
    //                        //                                        ContentUris.withAppendedId(Journals.CONTENT_URI,
    //                        //                                                holder.getItemId() * -1);
    //                        //                                mListener.onItemClicked(JournalListFragment.this, journalUri);
    //                        //                                break;
    //                        //                            default:
    //                        //                                throw new IllegalArgumentException();
    //                        //                        }
    //                    }
    //                    break;
    //                case R.id.action_1:
    //                    // Show "edit" dialog.
    //                    showDialog(DIALOG_TYPE_UPDATE_JOURNAL, null);
    //                    break;
    //                case R.id.action_2:
    //                    // Show "delete" dialog.
    //                    showDialog(DIALOG_TYPE_CONFIRM_DELETE_JOURNAL, null);
    //                    break;
    //                default:
    //                    throw new IllegalArgumentException();
    //            }
    //        }

    //        @Override
    //        public void onItemClick(@NonNull AdapterItemClickHelper helper,
    //                                @NonNull ViewHolder holder) {
    //            if (mListener != null) {
    //                switch (getArguments().getInt(ARG_NAVIGATION_MODE)) {
    //                    case DISPLAY_MODE_TOP_JOURNALS:
    //                        mListener.onItemClicked(JournalListFragment.this,
    //                                mJournals.get(holder.getAdapterPosition()));
    //                        break;
    //                    case DISPLAY_MODE_YOUR_JOURNALS:
    //                        final Uri journalUri =
    //                                ContentUris.withAppendedId(Journals.CONTENT_URI,
    //                                        holder.getItemId() * -1);
    //                        mListener.onItemClicked(JournalListFragment.this, journalUri);
    //                        break;
    //                    default:
    //                        throw new IllegalArgumentException();
    //                }
    //            }
    //        }
    //    }

    /**
     * A recycler view adapter wrapper for displaying an indeterminant progress indicator at the
     * bottom of the list to show there are more items available.
     */
    private static class JournalFooterAdapter extends FooterAdapterWrapper<ViewHolder> {

        /**
         * Layout resource ID for adapter footer.
         */
        private static final int LAYOUT_ID = R.layout.list_item_progress_bar;

        /**
         * Create a new adapter wrapper.
         *
         * @param wrappedAdapter the base adapter to be wrapped by this adapter
         */
        JournalFooterAdapter(@NonNull Adapter<ViewHolder> wrappedAdapter) {
            super(wrappedAdapter);
        }

        @Override
        public int getExtraItemViewType() {
            return LAYOUT_ID;
        }

        @Override
        public void onBindExtraItemViewHolder(ViewHolder holder, int position) {
        }

        @Override
        public ViewHolder onCreateExtraItemViewHolder(ViewGroup parent, int viewType) {
            final View view =
                    LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            final StaggeredGridLayoutManager.LayoutParams params =
                    new StaggeredGridLayoutManager.LayoutParams(view.getLayoutParams());
            params.setFullSpan(true);
            view.setLayoutParams(params);
            return new ViewHolder(view) {
            };
        }

    }

    /**
     * Adapter for displaying a list of journals.
     */
    private class JournalListAdapter extends ListAdapter<JournalInfo, ViewHolder>
            implements OnClickListener {

        /**
         * Default layout resource ID for journals.
         */
        private static final int LAYOUT_ID = R.layout.list_item_card_header_media_action;
        /**
         * View type for displaying a read only journal.
         */
        private static final int VIEW_TYPE_READ_ONLY = 1;
        /**
         * View type for displaying an editable journal.
         */
        private static final int VIEW_TYPE_EDITABLE = 2;

        /**
         * Loader and cache for images.
         */
        private final ImageLoader mImageLoader;

        /**
         * Create a new adapter.
         *
         * @param list the list to be managed by this adapter, may be {@code null} if the data is
         *             not yet available
         */
        JournalListAdapter(@Nullable List<JournalInfo> list) {
            super(list);
            mImageLoader = VolleySingleton.getInstance(getContext()).getImageLoader();
        }

        @Override
        public int getItemViewType(int position) {
            final JournalInfo item = getList().get(position);
            return item.isEditable ? VIEW_TYPE_EDITABLE : VIEW_TYPE_READ_ONLY;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, JournalInfo data) {
            final JournalViewHolder journal = (JournalViewHolder) holder;
            // Set the avatar icon.
            final int gender = data.journal.userGender;
            journal.avatar.setDefaultImageResId(
                    (gender == 0) ? R.drawable.gender_male : R.drawable.gender_female);
            journal.avatar.setImageUrl(null, mImageLoader);
            // Set the title text.
            if (!TextUtils.isEmpty(data.journal.title)) {
                journal.title.setText(data.journal.title);
            } else {
                journal.title.setText(R.string.defaultText_title);
            }
            // Set the subtitle text.
            journal.subtitle.setText(data.journal.userAlias);
            // Set the image. TODO also load provider images
            journal.image.setImageUrl(data.journal.imageUrl, mImageLoader);

            // *****Load more journals if at the last item.***** //
            if (mHasMoreJournals && !mIsLoading && (data == getList().get(getList().size() - 1))) {
                // Load more data to add to the end of the list.
                mIsLoading = true;
                startJournalsRequest();
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view =
                    LayoutInflater.from(parent.getContext()).inflate(LAYOUT_ID, parent, false);
            final JournalViewHolder holder = new JournalViewHolder(view);
            // Set default image.
            holder.image.setDefaultImageResId(R.drawable.image_default);
            // Set listener for main action.
            final View main = view.findViewById(R.id.action_main);
            main.setTag(holder);
            main.setOnClickListener(this);
            // Set text and listener for sub-actions.
            switch (viewType) {
                case VIEW_TYPE_READ_ONLY:
                    view.findViewById(R.id.layout_actions).setVisibility(View.GONE);
                    break;
                case VIEW_TYPE_EDITABLE:
                    // Set on click listeners for sub actions.
                    final TextView editView = (TextView) view.findViewById(R.id.action_1);
                    editView.setText(R.string.action_label_edit);
                    editView.setTag(holder);
                    editView.setOnClickListener(this);
                    final TextView deleteView = (TextView) view.findViewById(R.id.action_2);
                    deleteView.setText(R.string.action_label_delete);
                    deleteView.setTag(holder);
                    deleteView.setOnClickListener(this);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            return holder;
        }

        @Override
        public void onClick(View view) {
            final ViewHolder holder = (ViewHolder) view.getTag();
            switch (view.getId()) {
                case R.id.action_main:
                    if (mListener != null) {
                        mListener.onItemClicked(JournalListFragment.this,
                                mJournals.get(holder.getAdapterPosition()).journal, view);
                    }
                    break;
                case R.id.action_1:
                    // Show "edit" dialog.
                    mSelectedJournal = holder.getAdapterPosition();
                    showDialog(DIALOG_TYPE_EDIT_JOURNAL, null);
                    break;
                case R.id.action_2:
                    // Show "delete" dialog.
                    mSelectedJournal = holder.getAdapterPosition();
                    showDialog(DIALOG_TYPE_CONFIRM_DELETE_JOURNAL, null);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    /**
     * Callback for interacting with a cursor loader manager for getting journal data.
     */
    private class JournalLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final String account = getArguments().getString(Constants.ARG_ACCOUNT_NAME);
            return new CursorLoader(getContext(), Journals.CONTENT_URI, JOURNAL_QUERY_PROJECTION,
                    JOURNAL_QUERY_SELECTION, new String[]{account}, JOURNAL_QUERY_SORT_ORDER);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            mProviderCursor = null;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            mProviderCursor = data;
            // Update list of journals with provider data.
            int replaced = 0;
            if (getArguments().getInt(ARG_NAVIGATION_MODE) ==
                    Constants.NAVIGATION_MODE_YOUR_JOURNALS) {
                replaced = replaceProviderOnlyJournals();
            }
            //            updateJournalListRange(replaced, mJournals.size() - replaced, true); TODO
            // Create a new adapter, if required.
            if (getAdapter() != null) {
                final JournalListAdapter wrappedAdapter = new JournalListAdapter(mJournals);
                setAdapter(new JournalFooterAdapter(wrappedAdapter));
            }
            // Restore recycler fragment saved state, if applicable.
            //            if (mRecyclerFragmentSavedState != null) {  TODO
            //                restoreInstanceState(mRecyclerFragmentSavedState);
            //                mRecyclerFragmentSavedState = null;
            //            }
            // Reload occurred due to new journal addition and journal ID is not yet known.
            if (mNewJournalId == NEW_JOURNAL_LOAD_PENDING) {
                mNewJournalId = NEW_JOURNAL_LOAD_COMPLETE;
            }
            // Reload occurred due to new journal addition and journal ID is known.
            else if (mNewJournalId >= 0) {
                //                for (int i = 0; i < adapter.getItemCount(); i++) { TODO
                //                    if (adapter.getItemId(i) == mNewJournalId) {
                //                        setActivatedPosition(i);
                //                        break;
                //                    }
                //                }
                mNewJournalId = NEW_JOURNAL_LOAD_NONE;
            }
        }

    }

    /**
     * A listener that receives action event callbacks when the recycler view is in action mode.
     */
    //    private class MultiChoiceModeListener implements ActionModeListener { TODO
    //
    //        @Override
    //        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    //            switch (item.getItemId()) {
    //                case R.id.action_edit:
    //                    showDialog(DIALOG_TYPE_UPDATE_JOURNAL, null);
    //                    return true;
    //                case R.id.action_share:
    //                    showDialog(DIALOG_TYPE_CONFIRM_SHARE_JOURNALS, null);
    //                    return true;
    //                case R.id.action_delete:
    //                    showDialog(DIALOG_TYPE_CONFIRM_DELETE_JOURNAL, null);
    //                    return true;
    //                default:
    //                    return false;
    //            }
    //        }
    //
    //        @Override
    //        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    //            mode.getMenuInflater().inflate(R.menu.menu_journal_detail, menu);
    //            // Save the current action mode to use later.
    //            mActionMode = mode;
    //            // Notify listener the fragment is entering action mode.
    //            mListener.onEnteringActionMode(JournalListFragment.this);
    //            return true;
    //        }
    //
    //        @Override
    //        public void onDestroyActionMode(ActionMode mode) {
    //            mActionMode = null;
    //        }
    //
    //        @Override
    //        public void onAdapterPositionActivatedStateChanged(ActionMode mode, int position,
    //                                                           boolean isActivated) {
    //            mode.invalidate();
    //        }
    //
    //        @Override
    //        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    //            final int noActivated = getActivatedAdapterPositionCount();
    //            // Do not update title if no items selected, to prevent artifact while action mode is
    //            // being removed.
    //            if (noActivated > 0) {
    //                // Set the title with the number of items selected.
    //                final String title = getString(R.string.label_cab_itemsSelected);
    //                mode.setTitle(String.format(Locale.US, title, noActivated));
    //                // Show or hide actions that can only be performed on a single item.
    //                final boolean isShown = (noActivated == 1);
    //                menu.setGroupEnabled(R.id.group_single_select_only, isShown);
    //                menu.setGroupVisible(R.id.group_single_select_only, isShown);
    //                return true;
    //            } else {
    //                return false;
    //            }
    //        }
    //
    //    }

    /**
     * Class for listening for swipe refresh events.
     */
    private class OnRefreshListener implements SwipeRefreshLayout.OnRefreshListener {

        @Override
        public void onRefresh() {
            startJournalsRequest();
        }

    }

    /**
     * Class for determining the span size of a view in the recycler fragment.
     */
    //    private class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup { TODO
    //
    //        /**
    //         * The number of spans in the layout manager.
    //         */
    //        private final int mSpanCount;
    //
    //        /**
    //         * Create a new span size lookup.
    //         *
    //         * @param spanCount the number of spans in the layout manager
    //         */
    //        public SpanSizeLookup(int spanCount) {
    //            mSpanCount = spanCount;
    //        }
    //
    //        @Override
    //        public int getSpanIndex(int position, int spanCount) {
    //            final JournalFooterAdapter adapter = (JournalFooterAdapter) getAdapter();
    //            if (adapter.hasExtraItem() && (position == adapter.getExtraItemPosition())) {
    //                return 0;
    //            } else {
    //                return position % spanCount;
    //            }
    //        }
    //
    //        @Override
    //        public int getSpanSize(int position) {
    //            final JournalFooterAdapter adapter = (JournalFooterAdapter) getAdapter();
    //            if (adapter.hasExtraItem() && (position == adapter.getExtraItemPosition())) {
    //                return mSpanCount;
    //            } else {
    //                return 1;
    //            }
    //        }
    //
    //    }

}
