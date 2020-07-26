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
package com.example.journals.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.text.TextUtils;

import com.example.journals.R;
import com.example.journals.account.AccountUtils;
import com.example.journals.provider.JournalContract.Accounts;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

/**
 * Helper service that deletes any saved account-specific information when an account is deleted
 * from the {@link AccountManager}.
 */
public class DeletedAccountHelperService extends JobIntentService {

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // Get a list of the currently existing accounts.
        final String accountType = getString(R.string.authenticator_accountType);
        final Account[] accounts = AccountManager.get(this).getAccountsByType(accountType);
        // Create a list of account names.
        final String selection;
        final String[] selectionArgs;
        if (accounts.length > 0) {
            final List<String> accountNames = new ArrayList<>(accounts.length);
            for (Account a : accounts) {
                accountNames.add(a.name);
            }
            selection = Accounts.COLUMN_NAME + " NOT IN (?)";
            selectionArgs = new String[]{TextUtils.join(",", accountNames)};
            // Mark the currently active account as deleted from the account manager.
            final String activeAccount = AccountUtils.getActiveAccount(this);
            if ((activeAccount != null) && !accountNames.contains(activeAccount)) {
                AccountUtils.setActiveAccountDeleted(this, true);
            }
        } else {
            selection = null;
            selectionArgs = null;
        }
        // Remove deleted account information from the provider.
        getContentResolver().delete(Accounts.CONTENT_URI, selection, selectionArgs);
    }

}
