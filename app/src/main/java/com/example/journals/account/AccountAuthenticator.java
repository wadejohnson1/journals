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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.example.journals.R;
import com.example.journals.journal.Constants;
import com.example.journals.network.GsonRequest;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.LoginRequest;
import com.example.journals.network.NetworkUtils.LoginResponse;
import com.example.journals.network.VolleySingleton;
import com.google.gson.Gson;

import java.util.concurrent.ExecutionException;

/**
 * Account authenticator for journal accounts.
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    /**
     * User data saved with an account representing the user's alias.
     */
    public static final String USER_DATA_ALIAS = "USER_DATA_ALIAS";
    /**
     * User data saved with an account representing the URI of the user's avatar.
     */
    public static final String USER_DATA_AVATAR_URI = "USER_DATA_AVATAR_URI";
    /**
     * User data saved with an account representing the user's gender.
     */
    public static final String USER_DATA_GENDER = "USER_DATA_GENDER";
    public static final String USER_DATA_ID = "USER_DATA_ID";

    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = AccountAuthenticator.class.getSimpleName();

    /**
     * The context in which to run.
     */
    private Context mContext;

    /**
     * Construct a new account authenticator.
     *
     * @param context the context in which to access resources
     */
    public AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        // Create intent for launching new account creation activity.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(Constants.ARG_AUTHENTICATOR_REASON,
                Constants.AUTHENTICATOR_REASON_ADD_ACCOUNT);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        // Launch the authenticator activity to get user credentials.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(Constants.ARG_ACCOUNT_NAME, account.name);
        intent.putExtra(Constants.ARG_AUTHENTICATOR_REASON,
                Constants.AUTHENTICATOR_REASON_CONFIRM_CREDENTIALS);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        final Bundle bundle = new Bundle();
        bundle.putInt(AccountManager.KEY_ERROR_CODE,
                AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
        bundle.putString(AccountManager.KEY_ERROR_MESSAGE,
                mContext.getString(R.string.dialog_message_error_unsupportedOperation));
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        // Check if an auth token already exists.
        final AccountManager manager = AccountManager.get(mContext);
        String authToken = manager.peekAuthToken(account, authTokenType);

        // *****No token exists, attempt to get one using the saved password.***** //
        if (TextUtils.isEmpty(authToken)) {
            final String password = manager.getPassword(account);
            if (!TextUtils.isEmpty(password)) {
                // Create JSON string for request body.
                final LoginRequest request = new LoginRequest();
                request.pushRegistrationId = "0"; // TODO
                request.loginType = 1;
                request.email = account.name;
                request.password = password;
                // Create a future to run request synchronously.
                final RequestFuture<LoginResponse> future = RequestFuture.newFuture();
                // Start the login request.
                final GsonRequest<LoginResponse> gsonRequest =
                        new GsonRequest<>(request.getUrl(mContext), LoginResponse.class,
                                NetworkUtils.getAuthenticationHeaders(mContext),
                                new Gson().toJson(request), future, future);
                VolleySingleton.getInstance(mContext).addToRequestQueue(gsonRequest);
                // Get the login response.
                try {
                    final LoginResponse loginResponse = future.get();
                    // Add the auth token to the account manager.
                    authToken = loginResponse.authToken;
                    manager.setAuthToken(account, authTokenType, authToken);
                    // Use login response to update the user meta data in the account.
                    manager.setUserData(account, USER_DATA_ID,
                            Integer.toString(loginResponse.userId));
                    manager.setUserData(account, USER_DATA_ALIAS, loginResponse.alias);
                    manager.setUserData(account, USER_DATA_GENDER,
                            Integer.toString(loginResponse.gender));
                    manager.setUserData(account, USER_DATA_AVATAR_URI, loginResponse.imageUrl);
                }
                // The request was cancelled.
                catch (InterruptedException e) {
                    final Bundle bundle = new Bundle();
                    bundle.putInt(AccountManager.KEY_ERROR_CODE,
                            AccountManager.ERROR_CODE_CANCELED);
                    bundle.putString(AccountManager.KEY_ERROR_MESSAGE,
                            mContext.getString(R.string.dialog_message_error_cancelledOperation));
                    return bundle;
                }
                // An error occurred while retrieving the auth token.
                catch (ExecutionException e) {
                    final VolleyError cause = (VolleyError) e.getCause();
                    if (Constants.DEBUG && (cause.networkResponse != null)) {
                        Log.d(DEBUG_TAG, "Status Code " + cause.networkResponse.statusCode);
                        Log.d(DEBUG_TAG, new String(cause.networkResponse.data));
                    }
                    throw new NetworkErrorException(e);
                }
            }
        }

        // *****A current auth token is available.***** //
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle bundle = new Bundle();
            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return bundle;
        }

        // *****No auth token is available.***** //
        else {
            // Launch the authenticator activity to get user credentials.
            final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(Constants.ARG_ACCOUNT_NAME, account.name);
            intent.putExtra(Constants.ARG_AUTHENTICATOR_REASON,
                    Constants.AUTHENTICATOR_REASON_GET_AUTH_TOKEN);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        final String tokenType = mContext
                .getString(R.string.authenticator_authTokenType);
        if (authTokenType.equals(tokenType)) {
            return mContext.getString(R.string.label_account_authTokenType);
        } else {
            return null;
        }
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        // Return that no features are supported.
        final Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return bundle;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                                    String authTokenType, Bundle options)
            throws NetworkErrorException {
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(Constants.ARG_ACCOUNT_NAME, account.name);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

}