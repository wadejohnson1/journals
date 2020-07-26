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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.journals.account.AccountUtils;
import com.example.journals.journal.JournalListActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

/**
 * An activity that presents a set of application settings.
 */
public class SettingsActivity extends AppCompatActivity {

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Display the preference fragment.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, new SettingsFragment()).commit();
    }

    /**
     * A fragment for displaying settings.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat
            implements JournalDialogFragment.DialogListener {

        /**
         * Request code for calling an activity.
         */
        public static final int REQUEST_CODE_CHOOSE_ACCOUNT = 1;
        /**
         * Dialog type for displaying a dialog fragment.
         */
        private static final int DIALOG_TYPE_CONFIRM_SIGNOUT = 0;
        /**
         * Tag for dialogs launched from this fragment.
         */
        private static final String DIALOG_TAG = "dialog";

        /**
         * The account name of the currently selected account.
         * <p/>
         * When the user opens the account picker, the selected account is compared to this value to
         * determine if a new account was actually picked.
         */
        private Account mAccount = null;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_CHOOSE_ACCOUNT:
                    if (resultCode == Activity.RESULT_OK) {
                        // Get the account that the user selected.
                        final String accountName = data.getExtras().getString(
                                AccountManager.KEY_ACCOUNT_NAME);
                        if ((accountName != null) && !accountName.equals(AccountUtils
                                .getActiveAccount(getActivity()))) {
                            // Change the active account and restart the app from the beginning.
                            changeActiveAccountAndRestart(accountName);
                        }
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
            // Get the currently active account.
            //            mAccount = AccountUtils.getActiveAccount(getActivity()); TODO

            // *****Setup the account picker preference.***** //
            final Preference chooseAccount =
                    findPreference(getString(R.string.setting_chooseAccount));
            if (mAccount != null) {
                chooseAccount.setSummary(mAccount.name);
            }
            // Set listener for account picker.
            chooseAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final String accountType = getString(R.string.authenticator_accountType);
                    final Intent intent = AccountManager.newChooseAccountIntent(null, null,
                            new String[]{accountType}, true, null, null, null, null);
                    startActivityForResult(intent, REQUEST_CODE_CHOOSE_ACCOUNT);
                    return true;
                }

            });

            // *****Setup the logout setting.***** //
            final Preference logout = findPreference(getString(R.string.setting_logout));
            // Hide setting if no account currently in use.
            if (mAccount == null) {
                getPreferenceScreen().removePreference(logout);
            }
            // Set listener for logout setting.
            else {
                logout.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final JournalDialogFragment dialog = JournalDialogFragment
                                .newMessageDialog(DIALOG_TYPE_CONFIRM_SIGNOUT, R.string
                                                .dialog_message_signOut,
                                        R.string.dialog_positiveButton_signOut, true);
                        //                        dialog.setTargetFragment(SettingsFragment.this, 0); TODO
                        //                        dialog.show(getSupportFragmentManager(), DIALOG_TAG);
                        return true;
                    }
                });

            }

        }

        @Override
        public void onDialogResult(int dialogType, int action, Intent data) {
            switch (dialogType) {
                case DIALOG_TYPE_CONFIRM_SIGNOUT:
                    if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                        logoutAccount();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown dialog type " + dialogType);
            }
        }

        /**
         * Change the account that is currently active and restart the application.
         *
         * @param accountName the name of the account that will be made active, or {@code null} if
         *                    no account should be active
         */
        private void changeActiveAccountAndRestart(String accountName) {
            // Save the active account information.
            AccountUtils.setActiveAccount(getActivity(), accountName);
            // Clear all open activities and restart application.
            final Intent intent = new Intent(getActivity(), JournalListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        /**
         * Logout the currently used account.
         */
        private void logoutAccount() {
            final AccountManager manager = AccountManager.get(getActivity());
            // Check if the account has an existing auth token and invalidate it.
            final String authTokenType = getString(R.string.authenticator_authTokenType);
            final String authToken = manager.peekAuthToken(mAccount, authTokenType);
            if (authToken != null) {
                manager.invalidateAuthToken(mAccount.type, authToken);
            }
            // Clear the account's password.
            manager.clearPassword(mAccount);
            // Clear the active account and restart the app from the beginning.
            changeActiveAccountAndRestart(null);
        }

    }

}