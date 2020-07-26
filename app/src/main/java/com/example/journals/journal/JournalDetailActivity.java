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

import android.content.Intent;
import android.os.Bundle;

import com.example.journals.R;
import com.example.journals.journal.EntryListFragment.EntryListFragmentListener;
import com.example.journals.journal.JournalDetailFragment.JournalDetailFragmentListener;
import com.example.journals.provider.JournalContract;
import com.example.journals.provider.JournalContract.Activities;
import com.example.journals.provider.JournalContract.Contacts;
import com.example.journals.widget.AccountsUpdateListenerActivity;

import androidx.annotation.NonNull;

/**
 * An activity representing a single Journal detail screen. This activity is only used on handset
 * devices. On tablet-size devices, item details are presented side-by-side with a list of items in
 * a {@link JournalListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing more than a {@link
 * JournalDetailFragment}.
 * <p/>
 * This activity takes an intent data URI of type {@link JournalContract.Journals#CONTENT_TYPE_DIR}.
 */
public class JournalDetailActivity extends AccountsUpdateListenerActivity
        implements JournalDetailFragmentListener, EntryListFragmentListener {

    /**
     * Intent extra designating the journal data this activity is displaying.
     * <p/>
     * Type: Bundle
     */
    public static final String EXTRA_FRAGMENT_ARGS = "com.blyspot.blyspot.args";

    @Override
    public void onJournalDeleted(JournalDetailFragment fragment) {
        finish();
    }

    @Override
    public void onNavigationClick(JournalDetailFragment fragment) {
        finish();
    }

    @Override
    public void showEntry(@NonNull EntryListFragment fragment, int entryType, @NonNull Bundle args,
                          boolean isEditable) {
        final Intent intent = new Intent(this, EntryActivity.class);
        // Set the intent action.
        if (isEditable) {
            intent.setAction(Intent.ACTION_EDIT);
        } else {
            intent.setAction(Intent.ACTION_VIEW);
        }
        // Set the intent type.
        switch (entryType) {
            case Constants.ENTRY_TYPE_SPOT:
                intent.setType(Activities.CONTENT_TYPE_ITEM);
                break;
            case Constants.ENTRY_TYPE_CONTACT:
                intent.setType(Contacts.CONTENT_TYPE_ITEM);
                break;
            default:
                throw new IllegalArgumentException();
        }
        args.putInt(Constants.ARG_HOME_BUTTON_IMAGE, R.drawable.ic_arrow_back_white_24dp);
        intent.putExtras(args);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.journal_detail_activity);
        // Create the detail fragment and add it to the activity.
        if (savedInstanceState == null) {
            setupFragment();
        }
    }

    /**
     * Setup the fragment associated with this activity.
     */
    private void setupFragment() {
        final Bundle args = getIntent().getBundleExtra(EXTRA_FRAGMENT_ARGS);
        args.putBoolean(JournalDetailFragment.ARG_HAS_TOOLBAR_NAVIGATION, true);
        final JournalDetailFragment fragment = new JournalDetailFragment();
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_container,
                fragment).commit();
    }

    /**
     * Set up the fragments associated with this activity.
     */
    //    private void setupFragments() { TODO
    //        final Fragment master = new JournalListFragment();
    //        final Bundle args = new Bundle();
    //        // Add the account name argument.
    //        final String userName = AccountUtils.getActiveAccountEmail(this);
    //        if (userName != null) {
    //            args.putString(JournalListFragment.ARG_ACCOUNT_NAME, userName);
    //        }
    //        // Add the choice mode argument.
    //        args.putSerializable(JournalListFragment.ARG_CHOICE_MODE, mIsInTwoPaneMode ?
    //                RecyclerFragment.ChoiceMode.SINGLE : RecyclerFragment.ChoiceMode.NONE);
    //        // Add the display mode argument.
    //        if (mCurrentNavigationItem == R.id.action_showTopJournals) {
    //            args.putInt(JournalListFragment.ARG_NAVIGATION_MODE,
    //                    JournalListFragment.DISPLAY_MODE_TOP_JOURNALS);
    //        } else {
    //            args.putInt(JournalListFragment.ARG_NAVIGATION_MODE,
    //                    JournalListFragment.DISPLAY_MODE_YOUR_JOURNALS);
    //        }
    //        // Show the journal master fragment.
    //        master.setArguments(args);
    //        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    //        transaction.replace(R.id.content_container, master, FRAGMENT_TAG_MASTER);
    //        // Remove any existing journal detail fragment.
    //        if (mIsInTwoPaneMode) {
    //            final Fragment detail = getSupportFragmentManager().findFragmentByTag
    //                    (FRAGMENT_TAG_DETAIL);
    //            if (detail != null) {
    //                transaction.remove(detail);
    //            }
    //        }
    //        transaction.commit();
    //    }

}
