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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.example.journals.R;
import com.example.journals.journal.Constants;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Account and login utilities. This class manages a local shared preferences object that stores
 * which account is currently active.
 */
public class AccountUtils {

    /**
     * Value representing a lack of a navigation mode.
     */
    public static final int NO_NAVIGATION_MODE = 0;
    /**
     * Value representing a lack of an account ID.
     */
    public static final int NO_USER_DATA = -1;

    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = AccountUtils.class.getSimpleName();

    /**
     * Clear the password for the account with the specified name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     */
    public static void clearPassword(@NonNull Context context, @NonNull String accountName) {
        final AccountManager manager = AccountManager.get(context);
        manager.clearPassword(new Account(accountName,
                context.getString(R.string.authenticator_accountType)));
    }

    /**
     * Get the navigation mode.
     *
     * @param context  the context used to access resources
     * @param defValue default value to return if no navigation mode is set
     * @return the navigation mode, or {@code defValue} if no navigation mode is set
     */
    public static int getNavigationMode(@NonNull Context context, int defValue) {
        final SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getInt(context.getString(R.string.pref_navigationMode), defValue);
    }

    /**
     * Get an account from the specified account name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @return an account with the specified name
     */
    public static Account getAccountFromName(@NonNull Context context,
                                             @NonNull String accountName) {
        return new Account(accountName, context.getString(R.string.authenticator_accountType));
    }

    /**
     * Get the ID for the account with the specified name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @return the ID for the account with the specified name, or {@link #NO_USER_DATA} if no data
     * exists
     */
    public static long getUserDataId(@NonNull Context context, @NonNull String accountName) {
        final String id = getUserData(context, accountName, AccountAuthenticator.USER_DATA_ID);
        return (id != null) ? Long.decode(id) : NO_USER_DATA;
    }

    /**
     * Get the account that is currently active.
     *
     * @param context the context used to access resources
     * @return the account that is currently active, or {@code null} if no account is currently
     * active
     */
    public static String getActiveAccount(@NonNull Context context) {
        final SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_activeAccount), null);
    }

    /**
     * Get an auth token for the specified account.
     *
     * @param activity    activity used to access resources and launch a user credentials dialog, if
     *                    required
     * @param requestCode request code used to identify the request
     * @param accountName the name of the account
     * @param listener    listener for receiving auth token information
     * @return an {@code AccountManagerFuture} containing auth token information; if used, cannot
     * call {@link AccountManagerFuture#getResult()} on the main thread
     * @see AccountManager#getAuthToken(Account, String, Bundle, Activity, AccountManagerCallback,
     * Handler)
     */
    public static AccountManagerFuture<Bundle> getAuthToken(@NonNull Activity activity,
                                                            int requestCode,
                                                            @NonNull String accountName,
                                                            @Nullable AuthTokenListener listener) {
        final AccountManager manager = AccountManager.get(activity);
        final Account account = getAccountFromName(activity, accountName);
        final String authTokenType = activity.getString(R.string.authenticator_authTokenType);
        final AuthTokenCallback callback =
                (listener != null) ? new AuthTokenCallback(listener, requestCode) : null;
        return manager.getAuthToken(account, authTokenType, null, activity, callback, null);
    }

    /**
     * Get the password for the account with the specified name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @return the password for the account with the specified name, or {@code null} if no password
     * exists
     */
    public static String getPassword(@NonNull Context context, @NonNull String accountName) {
        final AccountManager manager = AccountManager.get(context);
        return manager.getPassword(getAccountFromName(context, accountName));
    }

    /**
     * Get whether the currently active account has been deleted from the {@link AccountManager}.
     *
     * @param context the context used to access resources
     * @return {@code true} if account that is currently active has been deleted from the account
     * manager, {@code false} otherwise
     */
    public static boolean isActiveAccountDeleted(@NonNull Context context) {
        final SharedPreferences preferences = getSharedPreferences(context);
        return preferences
                .getBoolean(context.getString(R.string.pref_isActiveAccountDeleted), false);
    }

    /**
     * Get whether the account manager contains the specified account.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account to check the validity of
     * @return {@code true} if an account with the specified name exists in the account manager,
     * {@code false} if an account does not exist in the account manager or {@code email} is {@code
     * null}
     */
    public static boolean isValidAccount(@NonNull Context context, @Nullable String accountName) {
        if (accountName != null) {
            final Account account = getAccountFromName(context, accountName);
            final Account[] accounts = AccountManager.get(context).getAccountsByType(account.type);
            for (Account a : accounts) {
                if (a.equals(account)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Invalidate the auth token for the account with the specified name.
     *
     * @param context   the context used to access resources
     * @param authToken the auth token to invalidate
     */
    public static void invalidateAuthToken(@NonNull Context context, @Nullable String authToken) {
        final AccountManager manager = AccountManager.get(context);
        final String accountType = context.getString(R.string.authenticator_accountType);
        manager.invalidateAuthToken(accountType, authToken);
    }

    /**
     * Get the locally saved auth token for the account with the specified name.  This method only
     * checks for a locally saved token, it will not get a new auth token if one does not already
     * exist.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @return the locally saved auth token
     */
    public static String peekAuthToken(@NonNull Context context, @NonNull String accountName) {
        final AccountManager manager = AccountManager.get(context);
        final String authTokenType = context.getString(R.string.authenticator_authTokenType);
        return manager.peekAuthToken(getAccountFromName(context, accountName), authTokenType);
    }

    /**
     * Set the active account.
     * <p>
     * Changing the active account automatically removes any previously saved navigation mode
     * preference.
     *
     * @param context the context used to access resources
     * @param account account to set as active, or {@code null} if no account is active
     */
    public static void setActiveAccount(@NonNull Context context, String account) {
        final Editor editor = getSharedPreferences(context).edit();
        // Set the active account preference.
        final String key = context.getString(R.string.pref_activeAccount);
        if (account != null) {
            editor.putString(key, account);
        } else {
            editor.remove(key);
        }
        // Reset the navigation mode preference.
        setNavigationModeInternal(editor, context, NO_NAVIGATION_MODE);
        editor.apply();
    }

    /**
     * Set whether the active account was deleted.
     *
     * @param context   the context used to access resources
     * @param isDeleted {@code true} if the account currently set as active has been deleted from
     *                  the account manager, {@code false} otherwise
     */
    public static void setActiveAccountDeleted(@NonNull Context context, boolean isDeleted) {
        final Editor editor = getSharedPreferences(context).edit();
        // Set the preference.
        final String key = context.getString(R.string.pref_isActiveAccountDeleted);
        editor.putBoolean(key, isDeleted);
        editor.apply();
    }

    /**
     * Set the navigation mode.
     *
     * @param context the context used to access resources
     * @param mode    navigation mode to set as active, or {@link #NO_NAVIGATION_MODE} to remove the
     *                preference
     */
    public static void setNavigationMode(@NonNull Context context, int mode) {
        final Editor editor = getSharedPreferences(context).edit();
        setNavigationModeInternal(editor, context, mode);
        editor.apply();
    }

    /**
     * Set the navigation mode using the provided shared preferences editor.  Preference changes
     * will not be committed.
     *
     * @param editor  the editor to use to set the navigation mode
     * @param context the context used to access resources
     * @param mode    navigation mode to set as active, or {@link #NO_NAVIGATION_MODE} to remove the
     *                preference
     */
    private static void setNavigationModeInternal(@NonNull Editor editor,
                                                  @NonNull Context context, int mode) {
        final String key = context.getString(R.string.pref_navigationMode);
        if (mode != NO_NAVIGATION_MODE) {
            editor.putInt(key, mode);
        } else {
            editor.remove(key);
        }
    }

    /**
     * Get an account specific preference from shared preferences for the account with the specified
     * name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @param prefixId    the resource ID of the preference prefix to add to the account name
     * @return the value saved in the shared preferences, or -1 if no value exists
     */
    private static int getAccountSpecificIntPreference(@NonNull Context context, @NonNull String
            accountName, @StringRes int prefixId) {
        return getSharedPreferences(context).getInt(makeAccountSpecificPrefKey(context,
                accountName, prefixId), -1);
    }

    /**
     * Get an account specific preference from shared preferences for the account with the specified
     * name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @param prefixId    the resource ID of the preference prefix to add to the account name
     * @return the value saved in the shared preferences, or {@code null} if no value exists
     */
    private static String getAccountSpecificPreference(@NonNull Context context, @NonNull String
            accountName, @StringRes int prefixId) {
        return getSharedPreferences(context).getString(makeAccountSpecificPrefKey(context,
                accountName, prefixId), null);
    }

    /**
     * Get an instance of the default shared preferences.
     *
     * @param context the context used to access resources
     * @return an instance of the default shared preferences
     */
    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Get the user data value for the specified key.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @param key         the user data key to retrieve the value for
     * @return the user data value for the specified key, or {@code null} if no value exists
     */
    private static String getUserData(@NonNull Context context, @NonNull String accountName,
                                      @NonNull String key) {
        final AccountManager manager = AccountManager.get(context);
        return manager.getUserData(getAccountFromName(context, accountName), key);
    }

    /**
     * Make a preference key for the given prefix for the account with the specified name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @param prefixId    the resource ID of the prefix to add to the account name
     * @return a preference key for the given prefix specific {@code account}
     */
    private static String makeAccountSpecificPrefKey(@NonNull Context context,
                                                     @NonNull String accountName,
                                                     @StringRes int prefixId) {
        return context.getString(prefixId) + accountName;
    }

    /**
     * Set an account specific preference in shared preferences for the account with the specified
     * name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @param prefixId    the resource ID of the preference prefix to add to the account name
     * @param value       the value to save to shared preferences, or {@code null} to remove the
     *                    preference
     */
    private static void setAccountSpecificPreference(@NonNull Context context,
                                                     @NonNull String accountName,
                                                     @StringRes int prefixId,
                                                     @Nullable String value) {
        final Editor editor = getSharedPreferences(context).edit();
        final String prefKey = makeAccountSpecificPrefKey(context, accountName, prefixId);
        if (value != null) {
            editor.putString(prefKey, value);
        } else {
            editor.remove(prefKey);
        }
        editor.apply();
    }

    /**
     * Set an account specific preference in shared preferences for the account with the specified
     * name.
     *
     * @param context     the context used to access resources
     * @param accountName the name of the account
     * @param prefixId    the resource ID of the preference prefix to add to the account name
     * @param value       the value to save to shared preferences, or a negative value to remove the
     *                    preference
     */
    private static void setAccountSpecificPreference(@NonNull Context context,
                                                     @NonNull String accountName,
                                                     @StringRes int prefixId, int value) {
        final Editor editor = getSharedPreferences(context).edit();
        final String prefKey = makeAccountSpecificPrefKey(context, accountName, prefixId);
        if (value >= 0) {
            editor.putInt(prefKey, value);
        } else {
            editor.remove(prefKey);
        }
        editor.apply();
    }

    /**
     * Interface for auth token request listeners.
     */
    public interface AuthTokenListener {

        /**
         * Callback for receiving an auth token.
         *
         * @param requestCode request code used to identify the request
         * @param accountName account name used to retrieve auth token
         * @param authToken   auth token retrieved from the account manager
         */
        void onAuthTokenReceived(int requestCode, String accountName, String authToken);

        /**
         * Callback invoked if the authenticator failed to respond.
         *
         * @param requestCode request code used to identify the request
         */
        void onAuthenticatorError(int requestCode);

        /**
         * Callback invoked if the operation is canceled for any reason, including the user
         * canceling a credential request.
         *
         * @param requestCode request code used to identify the request
         */
        void onOperationCancelled(int requestCode);

        /**
         * Callback invoked if the authenticator experienced an I/O problem creating a new auth
         * token, usually because of network trouble.
         *
         * @param requestCode request code used to identify the request
         */
        void onIOError(int requestCode);

    }

    /**
     * An {@link ErrorListener} that will automatically request a new auth token in the event of an
     * auth token failure during a request.
     * <p>
     * Subclasses must implement the {@link #onUnhandledErrorResponse(VolleyError)} method to handle
     * any errors not related to auth token failure.
     */
    public static abstract class AuthFailureErrorListener implements ErrorListener {

        /**
         * Activity used to access resources and launch a user credentials dialog in the event of an
         * auth failure error.
         */
        private final Activity mActivity;
        /**
         * Listener for receiving auth token information
         */
        private final AuthTokenListener mListener;
        /**
         * Set whether to handle auth failure errors.
         */
        private final boolean mIsHandlingAuthError;
        /**
         * Request code used to identify the request.
         */
        private final int mRequestCode;
        /**
         * The name of the account to request an auth token for.
         */
        private final String mAccountName;
        /**
         * The auth token to invalidate in the event of an auth failure error
         */
        private final String mAuthToken;

        /**
         * Create a new error listener that will not perform any auth failure error handling.  If an
         * error is received, {@link #onUnhandledErrorResponse(VolleyError)} will be called
         * immediately.
         */
        public AuthFailureErrorListener() {
            mActivity = null;
            mRequestCode = 0;
            mAccountName = null;
            mListener = null;
            mAuthToken = null;
            mIsHandlingAuthError = false;
        }

        /**
         * Create a new auth failure error listener.
         *
         * @param activity    activity used to access resources and launch a user credentials dialog
         *                    in the event of an auth failure error
         * @param requestCode request code used to identify the request
         * @param accountName the name of the account to request an auth token for
         * @param listener    listener for receiving auth token information
         * @param authToken   the auth token to invalidate in the event of an auth failure error
         */
        public AuthFailureErrorListener(@NonNull Activity activity, int requestCode,
                                        @NonNull String accountName,
                                        @NonNull AuthTokenListener listener,
                                        @NonNull String authToken) {
            mActivity = activity;
            mRequestCode = requestCode;
            mAccountName = accountName;
            mListener = listener;
            mAuthToken = authToken;
            mIsHandlingAuthError = true;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (Constants.DEBUG && (error.networkResponse != null)) {
                Log.d(DEBUG_TAG, "Status Code " + error.networkResponse.statusCode);
                Log.d(DEBUG_TAG, new String(error.networkResponse.data));
            }
            if (mIsHandlingAuthError && (error instanceof AuthFailureError)) {
                invalidateAuthToken(mActivity, mAuthToken);
                getAuthToken(mActivity, mRequestCode, mAccountName, mListener);
            } else {
                onUnhandledErrorResponse(error);
            }
        }

        /**
         * Callback method that a non-auth failure error has occurred.
         *
         * @param error the error that occurred
         */
        public abstract void onUnhandledErrorResponse(VolleyError error);

    }

    /**
     * Class for internally handling account auth tokens.
     */
    private static class AuthTokenCallback implements AccountManagerCallback<Bundle> {

        /**
         * Listener for receiving actions related to auth token.
         */
        private final AuthTokenListener mListener;
        /**
         * Optional request code used by the listener for identifying this callback.
         */
        private final int mRequestCode;

        /**
         * Create a new auth token callback.
         *
         * @param listener Listener to receive callbacks from the result of the future.
         */
        public AuthTokenCallback(@NonNull AuthTokenListener listener, int requestCode) {
            mListener = listener;
            mRequestCode = requestCode;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                final Bundle result = future.getResult();
                mListener.onAuthTokenReceived(mRequestCode,
                        result.getString(AccountManager.KEY_ACCOUNT_NAME),
                        result.getString(AccountManager.KEY_AUTHTOKEN));
            } catch (AuthenticatorException e) {
                mListener.onAuthenticatorError(mRequestCode);
            } catch (OperationCanceledException e) {
                mListener.onOperationCancelled(mRequestCode);
            } catch (IOException e) {
                mListener.onIOError(mRequestCode);
            }
        }

    }

}

