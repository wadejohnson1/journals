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
package com.example.journals;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.example.journals.account.AccountUtils;
import com.example.journals.journal.Constants;
import com.example.journals.journal.JournalDetailActivity;
import com.example.journals.journal.JournalListFragment;
import com.example.journals.network.NetworkUtils;
import com.example.journals.widget.AccountsUpdateListenerActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

/**
 * An activity to search journal content and return the results.
 */
public class SearchableActivity extends AccountsUpdateListenerActivity implements
        JournalListFragment.JournalListFragmentListener {

    @Override
    public void onItemClicked(@NonNull JournalListFragment fragment, @NonNull Uri uri) {

    }

    @Override
    public void onItemClicked(@NonNull JournalListFragment fragment,
                              @NonNull NetworkUtils.Journal journal,
                              @Nullable View transitionView) {
        // Create the detail fragment arguments.
        final Bundle args = new Bundle();
        args.putInt(Constants.ARG_DATA_TYPE, Constants.DATA_TYPE_PARCELED_OBJECT);
        args.putParcelable(Constants.ARG_DATA, journal);
        // Set whether fragment is editable.
        final String account = AccountUtils.getActiveAccount(this);
        if ((account != null) &&
                (journal.userId == AccountUtils.getUserDataId(this, account))) {
            args.putBoolean(Constants.ARG_IS_EDITABLE, true);
        }

        // Start the journal detail activity.
        final Intent intent = new Intent(this, JournalDetailActivity.class);
        intent.putExtra(JournalDetailActivity.EXTRA_FRAGMENT_ARGS, args);
        if (transitionView != null) {
            final ActivityOptions options = ActivityOptions
                    .makeScaleUpAnimation(transitionView, 0, 0, transitionView.getWidth(),
                            transitionView.getHeight());
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toolbar_content);
        // Set the layout toolbar as the action bar.
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // Set up remaining parts of activity.
        if (savedInstanceState == null) {
            handleIntent();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
    }

    /**
     * Handle this activity's intent.
     */
    private void handleIntent() {
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            final Fragment fragment = new JournalListFragment();
            final Bundle args = new Bundle();
            // Add the active account argument.
            final String account = AccountUtils.getActiveAccount(this);
            if (AccountUtils.isValidAccount(this, account)) {
                args.putString(Constants.ARG_ACCOUNT_NAME, account);
            }
            // Add the display mode argument.
            args.putInt(JournalListFragment.ARG_NAVIGATION_MODE,
                    Constants.NAVIGATION_MODE_SEARCH_RESULTS);
            // Add the search query argument.
            args.putString(JournalListFragment.ARG_SEARCH_QUERY, query);
            fragment.setArguments(args);
            // Replace any existing fragments with this one.
            getSupportFragmentManager().beginTransaction().replace(R.id.content_container, fragment)
                    .commit();
        }
    }

}
