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
package com.example.journals.account;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.journals.R;
import com.example.journals.account.AuthenticatorFragment.LoginFragmentListener;
import com.example.journals.journal.Constants;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

/**
 * Activity that handles requests from the account authenticator service when it requires
 * interaction with the user.
 */
public class AuthenticatorActivity extends AppCompatActivity implements LoginFragmentListener {

    /**
     * Response for sending back to the account authenticator.
     */
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    /**
     * Bundle information to send with the account response.
     */
    private Bundle mResultBundle = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // The "home" button was clicked.
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClickNewAccountButton() {
        // Show the new account fragment.
        final Fragment fragment = new AuthenticatorFragment();
        final Bundle args = new Bundle();
        args.putBoolean(AuthenticatorFragment.ARG_SHOW_NEW_ACCOUNT_UI, true);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_container, fragment)
                .addToBackStack(null).commit();
    }

    @Override
    public void onResultReceived(@NonNull Bundle result) {
        mResultBundle = result;
        setResult(Activity.RESULT_OK);
        finish();
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    @Override
    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toolbar_content);
        // Set the layout toolbar as the action bar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // *****Set up the account authenticator response.***** //
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        // *****Add the login fragment to the UI.***** //
        if (savedInstanceState == null) {
            // Show the sign in fragment.
            final Fragment fragment = new AuthenticatorFragment();
            // Set fragment arguments.
            final Intent intent = getIntent();
            final Bundle args = new Bundle();
            args.putString(Constants.ARG_ACCOUNT_NAME,
                    intent.getStringExtra(Constants.ARG_ACCOUNT_NAME));
            args.putInt(Constants.ARG_AUTHENTICATOR_REASON,
                    intent.getIntExtra(Constants.ARG_AUTHENTICATOR_REASON, -1));
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.content_container,
                    fragment).commit();
        }
    }

}
