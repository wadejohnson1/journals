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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.example.journals.HelpActivity;
import com.example.journals.JournalDialogFragment;
import com.example.journals.JournalDialogFragment.DialogListener;
import com.example.journals.R;
import com.example.journals.account.AccountAuthenticator;
import com.example.journals.account.AccountUtils;
import com.example.journals.exoplayer.ExoPlayerActivity;
import com.example.journals.journal.JournalDetailFragment.JournalDetailFragmentListener;
import com.example.journals.journal.JournalListFragment.JournalListFragmentListener;
import com.example.journals.network.NetworkUtils.Journal;
import com.example.journals.network.VolleySingleton;
import com.example.journals.provider.JournalContract.Accounts;
import com.example.journals.widget.AccountsUpdateListenerActivity;
import com.example.journals.widget.CheckableImageView;
import com.example.journals.widget.CheckableImageView.OnCheckedStateChangedListener;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * An activity representing a directory (list) of journals. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity presents a list of
 * items, which when touched, lead to a {@link JournalDetailActivity} representing journal details.
 * On tablets, the activity presents the list of journals and journal details side-by-side using two
 * vertical panes.
 */
public class JournalListActivity extends AccountsUpdateListenerActivity
        implements JournalListFragmentListener, JournalDetailFragmentListener, DialogListener {

    /**
     * Dialog type for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_CONFIRM_SIGNOUT = 0;
    /**
     * Request code for calling an activity.
     */
    private static final int REQUEST_CODE_CHOOSE_ACCOUNT = 1;
    /**
     * Mapping of navigation modes to their navigation item ID.
     */
    private static final SparseIntArray sNavigationModeMap = new SparseIntArray(3);
    /**
     * Tag for dialogs launched from this fragment.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Tag used to reference the detail content fragment when in dual pane mode.
     */
    private static final String FRAGMENT_TAG_DETAIL = "FRAGMENT_TAG_DETAIL";
    /**
     * Tag used to reference the master content fragment.
     */
    private static final String FRAGMENT_TAG_MASTER = "FRAGMENT_TAG_MASTER";
    /**
     * Tag used to reference the right nav content fragment.
     */
    private static final String FRAGMENT_TAG_RIGHT_NAV = "FRAGMENT_TAG_RIGHT_NAV";
    /**
     * Saved instance state key for storing the data of the current journal.
     */
    private static final String SAVED_STATE_CURRENT_JOURNAL_DATA =
            "com.blyspot.blyspot.SAVED_STATE_CURRENT_JOURNAL_DATA";
    /**
     * Saved instance state key for storing the current navigation drawer item.
     */
    private static final String SAVED_STATE_CURRENT_NAV_ITEM =
            "com.blyspot.blyspot.SAVED_STATE_CURRENT_NAV_ITEM";
    /**
     * Projection for retrieving information about the active account.
     */
    private static final String[] QUERY_PROJECTION = new String[]{Accounts.COLUMN_USER_ID,
            Accounts.COLUMN_NAME, Accounts.COLUMN_ALIAS, Accounts.COLUMN_GENDER};
    /**
     * Selection for retrieving information about the active account.
     */
    private static final String QUERY_SELECTION = Accounts.COLUMN_NAME + "=?";

    static {
        sNavigationModeMap.put(Constants.NAVIGATION_MODE_TOP_JOURNALS, R.id.action_showTopJournals);
        sNavigationModeMap
                .put(Constants.NAVIGATION_MODE_YOUR_JOURNALS, R.id.action_showYourJournals);
        sNavigationModeMap.put(Constants.NAVIGATION_MODE_NEARBY, R.id.action_showNearby);
    }

    /**
     * Set whether the activity is in two-pane mode.
     */
    private boolean mIsInTwoPaneMode;
    /**
     * Container for the navigation drawer.
     */
    private DrawerLayout mDrawerLayout;
    /**
     * The currently selected navigation drawer mode.
     * <p/>
     * Refers only to side nav menu items that have a highlighted state after being clicked.
     */
    private int mNavigationMode = 0;
    /**
     * Runnable for handling navigation drawer item clicks after the navigation drawer has closed.
     */
    private NavigationDrawerRunnable mSideNavRunnable = null;
    /**
     * The view holding the "side" navigation drawer content (which holds the primary navigation
     * items and settings).
     */
    private NavigationView mSideNavView;
    /**
     * The current journal selected in the journal master fragment.
     * <p/>
     * Only used if activity is in two-pane mode.
     */
    private Parcelable mCurrentJournal = null;

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_SIGNOUT:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    signOutAccount();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown dialog type " + dialogType);
        }
    }

    //    @Override
    //    public void onEnteringActionMode(@NonNull JournalListFragment fragment) { TODO
    //        // Remove the journal detail fragment, if present.
    //        if (mIsInTwoPaneMode) {
    //            mCurrentJournal = null;
    //            final FragmentManager manager = getSupportFragmentManager();
    //            final Fragment detail = manager.findFragmentByTag(FRAGMENT_TAG_DETAIL);
    //            if (detail != null) {
    //                manager.beginTransaction().remove(detail).commit();
    //            }
    //        }
    //    }

    @Override
    public void onItemClicked(@NonNull JournalListFragment fragment, @NonNull Journal journal,
                              @Nullable View transitionView) {
        final Intent intent = new Intent(this, ExoPlayerActivity.class);
        intent.setData(Uri.parse("http://blyspot.com/media/journals/146325027631220230310.mp4"))
                .setAction(Intent.ACTION_VIEW);
        startActivity(intent);
        //        if (!mIsInTwoPaneMode || !journal.equals(mCurrentJournal)) { TODO
        //            // Create the detail fragment arguments.
        //            final Bundle args = new Bundle();
        //            args.putInt(Constants.ARG_DATA_TYPE, Constants.DATA_TYPE_PARCELED_OBJECT);
        //            args.putParcelable(Constants.ARG_DATA, journal);
        //            // Set whether fragment is editable.
        //            final String account = AccountUtils.getActiveAccount(this);
        //            if ((account != null) &&
        //                    (journal.userId == AccountUtils.getUserDataId(this, account))) {
        //                args.putBoolean(Constants.ARG_IS_EDITABLE, true);
        //            }
        //
        //            // *****Activity is in two-pane (large screen) mode.***** //
        //            if (mIsInTwoPaneMode) {
        //                mCurrentJournal = journal;
        //                final JournalDetailFragment detail = new JournalDetailFragment();
        //                detail.setArguments(args);
        //                getSupportFragmentManager().beginTransaction()
        //                        .replace(R.id.content_container_detail, detail, FRAGMENT_TAG_DETAIL)
        //                        .commit();
        //            }
        //
        //            // *****Activity is in single-pane (small screen) mode.***** //
        //            else {
        //                // Start the journal detail activity.
        //                final Intent intent = new Intent(this, JournalDetailActivity.class);
        //                intent.putExtra(JournalDetailActivity.EXTRA_FRAGMENT_ARGS, args);
        //                if (transitionView != null) {
        //                    final ActivityOptions options = ActivityOptions
        //                            .makeScaleUpAnimation(transitionView, 0, 0, transitionView.getWidth(),
        //                                    transitionView.getHeight());
        //                    startActivity(intent, options.toBundle());
        //                } else {
        //                    startActivity(intent);
        //                }
        //            }
        //        }
    }

    @Override
    public void onItemClicked(@NonNull JournalListFragment fragment, @NonNull Uri journalUri) {
        if (!mIsInTwoPaneMode || !journalUri.equals(mCurrentJournal)) {
            // Create the detail fragment arguments.
            final Bundle args = new Bundle();
            args.putInt(Constants.ARG_DATA_TYPE, Constants.DATA_TYPE_CONTENT_URI);
            args.putParcelable(Constants.ARG_DATA, journalUri);

            // *****Activity is in two-pane (large screen) mode.***** //
            if (mIsInTwoPaneMode) {
                mCurrentJournal = journalUri;
                final JournalDetailFragment detail = new JournalDetailFragment();
                detail.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_container_detail, detail, FRAGMENT_TAG_DETAIL)
                        .commit();
            }

            // *****Activity is in single-pane (small screen) mode.***** //
            else {
                // Start the journal detail activity.
                final Intent intent = new Intent(this, JournalDetailActivity.class);
                intent.putExtra(JournalDetailActivity.EXTRA_FRAGMENT_ARGS, args);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onJournalDeleted(JournalDetailFragment fragment) {
        // Remove the journal entry fragment.
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onNavigationClick(JournalDetailFragment fragment) {
        // This method will not be called.
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    // Get the account that the user selected.
                    final String accountName =
                            data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (!accountName.equals(AccountUtils.getActiveAccount(this))) {
                        // The selected account is already signed in.
                        if (AccountUtils.getPassword(this, accountName) != null) {
                            changeAccount(accountName);
                        }
                        // The selected account was previously "signed out", confirm credentials.
                        else {
                            final Account account = AccountUtils.getAccountFromName
                                    (this, accountName);
                            AccountManager.get(this).confirmCredentials(account, null, this, new
                                    ConfirmCredentialsListener(), null);
                        }
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.journal_list_activity);
        // Set the layout toolbar as the action bar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        // Determine whether layout is in two-pane (large screen) mode.
        mIsInTwoPaneMode = (findViewById(R.id.content_container_detail) != null);
        // Set up navigation drawer.
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mDrawerLayout.addDrawerListener(new DrawerListener());
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        mSideNavView = (NavigationView) findViewById(R.id.side_nav_content);
        mSideNavView.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener());
        // Get active account information (if available) and set up navigation menu and fragments.
        final String account = AccountUtils.getActiveAccount(this);
        if (AccountUtils.isValidAccount(this, account)) {
            setupMenu(account);
        } else {
            // Current account was deleted, remove saved preferences.
            if (account != null) {
                AccountUtils.setActiveAccount(this, null);
            }
            setupMenu(null);
        }
        if (savedInstanceState == null) {
            setupFragments();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        final String account = AccountUtils.getActiveAccount(this);
        // Get account information (if required) and set up navigation menu and fragments.
        if (AccountUtils.isValidAccount(this, account)) {
            setupMenu(account);
        } else {
            // Current account was deleted, remove saved preferences.
            if (account != null) {
                AccountUtils.setActiveAccount(this, null);
            }
            setupMenu(null);
        }
        setupFragments();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save navigation mode to shared preferences.
        if (mNavigationMode != 0) {
            AccountUtils.setNavigationMode(this, mNavigationMode);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_STATE_CURRENT_JOURNAL_DATA, mCurrentJournal);
    }

    /**
     * Change the account that is currently active.
     *
     * @param account account to make active, or {@code null} if no account should be active
     */
    private void changeAccount(String account) {
        // Check whether the new account is already active.
        final String currentAccount = AccountUtils.getActiveAccount(this);
        if ((account == null) ? (currentAccount != null) : (!account.equals(currentAccount))) {
            // Save the new account information to shared preferences.
            AccountUtils.setActiveAccount(this, account);
            // Clear all open activities and restart application.
            final Intent intent = new Intent(this, JournalListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    /**
     * Set up the fragments associated with this activity.
     */
    private void setupFragments() {
        final Fragment master = new JournalListFragment();
        final Bundle args = new Bundle();
        // Add the account name argument.
        final String account = AccountUtils.getActiveAccount(this);
        if (account != null) {
            args.putString(Constants.ARG_ACCOUNT_NAME, account);
        }
        // Add the navigation mode argument.
        args.putInt(JournalListFragment.ARG_NAVIGATION_MODE, mNavigationMode);
        // Show the journal master fragment.
        master.setArguments(args);
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, master, FRAGMENT_TAG_MASTER);
        // Remove any existing journal detail fragment.
        if (mIsInTwoPaneMode) {
            final Fragment detail = getSupportFragmentManager().findFragmentByTag
                    (FRAGMENT_TAG_DETAIL);
            if (detail != null) {
                transaction.remove(detail);
            }
        }
        transaction.commit();
    }

    /**
     * Set up the navigation drawer menu.
     *
     * @param accountName the name of the active account, or {@code null} if there is no active
     *                    account
     */
    private void setupMenu(String accountName) {
        // Clear any existing menu header.
        final View oldHeader = mSideNavView.getHeaderView(0);
        if (oldHeader != null) {
            mSideNavView.removeHeaderView(oldHeader);
        }
        // Add the menu items.
        final Menu menu = mSideNavView.getMenu();
        menu.clear();
        mSideNavView.inflateMenu(R.menu.menu_drawer_main);

        // *****There is a valid account that is currently active.***** //
        if (accountName != null) {

            // *****Set up the navigation drawer header.***** //
            final View header = mSideNavView.inflateHeaderView(R.layout.drawer_header_signed_in);
            final AccountManager manager = AccountManager.get(this);
            final Account account = AccountUtils.getAccountFromName(this, accountName);
            // Set account name.
            ((TextView) header.findViewById(R.id.text_name)).setText(account.name);
            // Set account alias.
            ((TextView) header.findViewById(R.id.text_alias))
                    .setText(manager.getUserData(account, AccountAuthenticator.USER_DATA_ALIAS));
            // Set up the user image.
            final NetworkImageView imageView = (NetworkImageView) header.findViewById(R.id.avatar);
            final String gender =
                    manager.getUserData(account, AccountAuthenticator.USER_DATA_GENDER);
            if (gender != null) {
                switch (gender) {
                    case Constants.GENDER_MALE:
                        imageView.setDefaultImageResId(R.drawable.gender_male);
                        break;
                    case Constants.GENDER_FEMALE:
                        imageView.setDefaultImageResId(R.drawable.gender_female);
                        break;
                    case Constants.GENDER_OTHER:
                        imageView.setDefaultImageResId(R.drawable.gender_other);
                        break;
                }
            }
            final String imageUri =
                    manager.getUserData(account, AccountAuthenticator.USER_DATA_AVATAR_URI);
            imageView.setImageUrl(imageUri,
                    VolleySingleton.getInstance(JournalListActivity.this).getImageLoader());
            // Set up the account menu toggle button.
            final CheckableImageView toggle =
                    (CheckableImageView) header.findViewById(R.id.account_menu_toggle);
            toggle.setOnClickListener(new OnNavDrawerHeaderClickListener());
            toggle.setOnCheckedStateChangedListener(new OnAccountToggleChangedListener());

            // *****Set up the navigation drawer menu items and toolbar title.***** //
            menu.findItem(R.id.action_showYourJournals).setVisible(true).setEnabled(true);
            menu.findItem(R.id.action_showNearby).setVisible(true).setEnabled(true);
            mNavigationMode =
                    AccountUtils.getNavigationMode(this, Constants.NAVIGATION_MODE_TOP_JOURNALS);
            final MenuItem item;
            switch (mNavigationMode) {
                case Constants.NAVIGATION_MODE_TOP_JOURNALS:
                    item = menu.findItem(R.id.action_showTopJournals);
                    break;
                case Constants.NAVIGATION_MODE_YOUR_JOURNALS:
                    item = menu.findItem(R.id.action_showYourJournals);
                    break;
                case Constants.NAVIGATION_MODE_NEARBY:
                    item = menu.findItem(R.id.action_showNearby);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            item.setChecked(true);
            getSupportActionBar().setTitle(item.getTitle());
        }

        // *****No valid account is currently active.***** //
        else {
            // Set up the navigation drawer header.
            final View header = mSideNavView.inflateHeaderView(R.layout.drawer_header_signed_out);
            header.setOnClickListener(new OnNavDrawerHeaderClickListener());
            // Set up the navigation drawer menu items and toolbar title.
            menu.findItem(R.id.action_showYourJournals).setVisible(false).setEnabled(false);
            menu.findItem(R.id.action_showNearby).setVisible(false).setEnabled(false);
            mNavigationMode = Constants.NAVIGATION_MODE_TOP_JOURNALS;
            final MenuItem item = menu.findItem(R.id.action_showTopJournals).setChecked(true);
            getSupportActionBar().setTitle(item.getTitle());
        }
    }

    /**
     * Sign out the currently active account.
     */
    private void signOutAccount() {
        final String account = AccountUtils.getActiveAccount(this);
        if (account != null) {
            // Check if the account has an existing auth token and invalidate it.
            final String authToken = AccountUtils.peekAuthToken(this, account);
            if (!TextUtils.isEmpty(authToken)) {
                AccountUtils.invalidateAuthToken(this, authToken);
            }
            // Clear the account's password.
            AccountUtils.clearPassword(this, account);
        }
        // Clear the active account and restart the app from the beginning.
        changeAccount(null);
    }

    /**
     * A listener for credentials confirmation when attempting to use an account that has previously
     * been signed out of.
     */
    private class ConfirmCredentialsListener implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                final Bundle result = future.getResult();
                final String accountName = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                changeAccount(accountName);
            }
            // The account authenticator did not respond. TODO
            catch (AuthenticatorException e) {
                Toast.makeText(JournalListActivity.this, "The account manager stopped responding.",
                        Toast.LENGTH_SHORT).show();
            }
            // The operation was cancelled.
            catch (OperationCanceledException e) {
                // Do nothing.
            }
            // A network error occurred.
            catch (IOException e) {
                Toast.makeText(JournalListActivity.this, "A network error occurred.", Toast
                        .LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Listener for navigation drawer changes.
     */
    private class DrawerListener extends SimpleDrawerListener {

        @Override
        public void onDrawerClosed(View drawerView) {
            // Close the account menu, if open.
            final CheckableImageView view =
                    findViewById(R.id.account_menu_toggle);
            if ((view != null) && (view.isChecked())) {
                view.setChecked(false);
            }
            // Execute the side nav item runnable.
            if (drawerView == mSideNavView) {
                if (mSideNavRunnable != null) {
                    mSideNavRunnable.run();
                    mSideNavRunnable = null;
                }
            } else {
                throw new IllegalArgumentException();
            }
        }

    }

    /**
     * Listener for responding to changes in the account menu toggle view state.
     */
    private class OnAccountToggleChangedListener implements OnCheckedStateChangedListener {

        @Override
        public void onCheckStateChanged(CheckableImageView view, boolean isChecked) {
            mSideNavView.getMenu().clear();
            if (isChecked) {
                mSideNavView.inflateMenu(R.menu.menu_drawer_account);
            } else {
                mSideNavView.inflateMenu(R.menu.menu_drawer_main);
                mSideNavView.getMenu().findItem(sNavigationModeMap.get(mNavigationMode))
                        .setChecked(true);
            }
        }

    }

    /**
     * Listener for responding to clicks in the navigation drawer header.
     */
    private class OnNavDrawerHeaderClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            final int itemId = view.getId();
            switch (itemId) {
                case R.id.header:
                    mDrawerLayout
                            .setDrawerListener(new SimpleDrawerListener() { // TODO

                                @Override
                                public void onDrawerClosed(View drawerView) {
                                    executeDrawerHeaderItem(itemId);
                                }

                            });
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    break;
                case R.id.account_menu_toggle:
                    ((CheckableImageView) view).toggle();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * Execute a navigation drawer header item click action.
         *
         * @param itemId the ID of the item to execute
         */
        private void executeDrawerHeaderItem(int itemId) {
            mDrawerLayout.setDrawerListener(null);
            switch (itemId) {
                case R.id.header:
                    // Show account chooser dialog.
                    final String accountType = getString(R.string.authenticator_accountType);
                    Intent intent = AccountManager.newChooseAccountIntent(null, null,
                            new String[]{accountType}, true, null, null, null, null);
                    startActivityForResult(intent, REQUEST_CODE_CHOOSE_ACCOUNT);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    /**
     * A listener for navigation drawer menu item selection.
     */
    private class OnNavigationItemSelectedListener
            implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            final int itemId = menuItem.getItemId();
            switch (itemId) {
                case R.id.action_showTopJournals:
                case R.id.action_showYourJournals:
                case R.id.action_showNearby:
                case R.id.action_helpAndFeedback:
                case R.id.action_changeAccount:
                case R.id.action_signOut:
                    // Create a runnable to execute when the navigation drawer closes.
                    mSideNavRunnable = new NavigationDrawerRunnable(itemId);
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                default:
                    return false;
            }
        }

    }

    /**
     * A class for executing an action based on a navigation drawer menu click after the navigation
     * drawer closes.
     */
    private class NavigationDrawerRunnable implements Runnable {

        /**
         * The item ID of the navigation drawer runnable that was clicked.
         */
        private final int mItemId;

        /**
         * Create a new navigation drawer runnable.
         *
         * @param itemId the item ID of the navigation drawer menu item that was clicked
         */
        NavigationDrawerRunnable(int itemId) {
            mItemId = itemId;
        }

        @Override
        public void run() {
            switch (mItemId) {
                case R.id.action_showTopJournals:
                case R.id.action_showYourJournals:
                case R.id.action_showNearby:
                    if (sNavigationModeMap.get(mNavigationMode) != mItemId) {
                        // Update selected navigation item and action bar title.
                        mNavigationMode =
                                sNavigationModeMap.keyAt(sNavigationModeMap.indexOfValue(mItemId));
                        final MenuItem item = mSideNavView.getMenu().findItem(mItemId);
                        getSupportActionBar().setTitle(item.getTitle());
                        // Show the new journal master fragment.
                        setupFragments();
                    }
                    break;
                case R.id.action_helpAndFeedback:
                    // Open help activity.
                    final Context context = JournalListActivity.this;
                    Intent intent = new Intent(context, HelpActivity.class);
                    context.startActivity(intent);
                    break;
                case R.id.action_changeAccount:
                    // Open account chooser dialog.
                    final String accountType = getString(R.string.authenticator_accountType);
                    intent = AccountManager.newChooseAccountIntent(null, null,
                            new String[]{accountType}, true, null, null, null, null);
                    startActivityForResult(intent, REQUEST_CODE_CHOOSE_ACCOUNT);
                    break;
                case R.id.action_signOut:
                    // Open sign out dialog.
                    final JournalDialogFragment dialog = JournalDialogFragment
                            .newMessageDialog(DIALOG_TYPE_CONFIRM_SIGNOUT,
                                    R.string.dialog_message_signOut,
                                    R.string.dialog_positiveButton_signOut, true);
                    dialog.show(getSupportFragmentManager(), DIALOG_TAG);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

}
