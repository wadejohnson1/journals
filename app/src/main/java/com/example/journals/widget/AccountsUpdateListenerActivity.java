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
package com.example.journals.widget;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Intent;
import android.os.Bundle;

import com.example.journals.account.AccountUtils;
import com.example.journals.journal.JournalListActivity;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An activity that registers an accounts update listener on creation, automatically clearing the
 * task stack and restarting the base activity if the active account is deleted while it is
 * running.
 */
public class AccountsUpdateListenerActivity extends AppCompatActivity
        implements OnAccountsUpdateListener {

    /**
     * Set whether the accounts updated listener has been set on the account manager.
     */
    private boolean mIsListenerSet = false;

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        final String accountName = AccountUtils.getActiveAccount(this);
        if (accountName != null) {
            final Account activeAccount = AccountUtils.getAccountFromName(this, accountName);
            for (Account a : accounts) {
                if (activeAccount.equals(a)) {
                    return;
                }
            }
            // The active account has been deleted.
            clearTaskAndRestart();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restart task if the active account was deleted while the activity was destroyed.
        if (AccountUtils.isActiveAccountDeleted(this)) {
            clearTaskAndRestart();
        } else {
            AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);
            mIsListenerSet = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsListenerSet) {
            AccountManager.get(this).removeOnAccountsUpdatedListener(this);
            mIsListenerSet = false;
        }
    }

    /**
     * Clear the task stack and restart the base activity with no active account set.
     */
    private void clearTaskAndRestart() {
        // Clear the active account information.
        AccountUtils.setActiveAccount(this, null);
        AccountUtils.setActiveAccountDeleted(this, false);
        // Restart base activity.
        final Intent intent = new Intent(this, JournalListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

}
