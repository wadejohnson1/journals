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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.journals.JournalDialogFragment;
import com.example.journals.R;
import com.example.journals.journal.Constants;
import com.example.journals.network.GsonRequest;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.LoginRequest;
import com.example.journals.network.NetworkUtils.LoginResponse;
import com.example.journals.network.NetworkUtils.SignUpRequest;
import com.example.journals.network.NetworkUtils.SignUpResponse;
import com.example.journals.network.VolleySingleton;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

/**
 * A login screen that offers login via username/password.
 */
public class AuthenticatorFragment extends Fragment {

    /**
     * Set whether to show the new account dialog, when creating a new account on the server vice
     * logging into an existing account.
     * <p/>
     * Should only be used if authenticator reason is {@link Constants#AUTHENTICATOR_REASON_ADD_ACCOUNT}.
     * <p/>
     * Type: boolean
     */
    public static final String ARG_SHOW_NEW_ACCOUNT_UI = "ARG_SHOW_NEW_ACCOUNT_UI";

    /**
     * Tag to display with debug messages.
     */
    private static final String DEBUG_TAG = AuthenticatorFragment.class.getSimpleName();
    /**
     * Tag for dialogs launched from this fragment.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";

    /**
     * Helper class for managing this fragment's layout.
     */
    private LayoutHelper mLayoutHelper;
    /**
     * The fragment's callback object, which is notified of changes to the fragment.
     */
    private LoginFragmentListener mListener;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLayoutHelper.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mLayoutHelper.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Attach this fragment's activity as a callback.
        if (context instanceof LoginFragmentListener) {
            mListener = (LoginFragmentListener) context;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Determine which layout helper to create.
        if (getArguments().getBoolean(ARG_SHOW_NEW_ACCOUNT_UI, false)) {
            mLayoutHelper = new NewAccountHelper(getActivity());
        } else {
            mLayoutHelper = new LoginHelper(getActivity());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return mLayoutHelper.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Remove this fragment's activity as a callback.
        mListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        // Cancel any running network tasks.
        VolleySingleton.getInstance(getActivity()).getRequestQueue().cancelAll(this);
    }

    /**
     * Create and show a message dialog.
     *
     * @param message message to display in dialog
     */
    private void showDialog(@NonNull String message) {
        final DialogFragment dialog = JournalDialogFragment
                .newMessageDialog(0, message, R.string.dialog_positiveButton_ok, false);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * A callback to parent activities of this fragment to notify them that a result to pass back to
     * the account manager was received.
     * <p/>
     * All activities containing this fragment must implement this callback interface.
     */
    public interface LoginFragmentListener {

        /**
         * Callback to be invoked when the "new account" button is clicked.
         */
        void onClickNewAccountButton();

        /**
         * Callback to be invoked when the fragment has received a result.
         *
         * @param result the result to pass back to the account manager, will not be {@code null}
         */
        void onResultReceived(@NonNull Bundle result);

    }

    /**
     * Base helper class for managing the layout for an authenticator fragment.
     */
    private static abstract class LayoutHelper {

        /**
         * Resource ID of the container for subclasses to place their form views.
         */
        public static final int FORM_CONTAINER = R.id.content_container;

        /**
         * Context used to access resources.
         */
        private final Context mContext;
        /**
         * View for showing the log in form.
         */
        private View mFormContainerView;
        /**
         * Indeterminant progress view to show while exchanging data with server.
         */
        private View mProgressContainerView;

        public LayoutHelper(Context context) {
            mContext = context;
        }

        /**
         * Get whether the specified string represents a valid email address.
         *
         * @param email the string to check
         * @return {@code true} if {@code email} represents a valid email address, {@code false}
         * otherwise
         */
        public static boolean isValidEmailAddress(String email) {
            return Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }

        /**
         * Get whether the specified string represents a valid password.
         *
         * @param password the string to check
         * @return {@code true} if {@code email} represents a valid password, {@code false}
         * otherwise
         */
        public static boolean isPasswordValid(String password) {
            // TODO: Replace this with your own logic
            return password.length() > 4;
        }

        /**
         * Call this method from {@link Fragment#onActivityCreated(Bundle)} to perform layout
         * initialization for this helper.
         *
         * @param savedInstanceState if the fragment is being re-created from a previous state, this
         *                           is the state
         */
        public abstract void onActivityCreated(Bundle savedInstanceState);

        /**
         * Call this method from {@link Fragment#onActivityResult(int, int, Intent)} to perform
         * appropriate activity result callbacks.
         *
         * @param requestCode The integer request code originally supplied to
         *                    startActivityForResult(), allowing you to identify who this result
         *                    came from.
         * @param resultCode  The integer result code returned by the child activity through its
         *                    setResult().
         * @param data        An Intent, which can return result data to the caller (various data
         *                    can be attached to Intent "extras").
         * @return {@code true} if activity result is handled, {@code false} otherwise
         */
        public abstract boolean onActivityResult(int requestCode, int resultCode, Intent data);

        /**
         * Call this method from {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)} to
         * create the layout helper's view.
         * <p/>
         * The base view returned here contains an empty form view and a progress view to show when
         * authentication is being performed. Subclasses can add their own form views by calling to
         * the super implementation and attaching their view to the {@link ViewGroup} with ID {@link
         * #FORM_CONTAINER}.
         *
         * @param inflater           The LayoutInflater object that can be used to inflate any views
         *                           in the fragment
         * @param container          If non-null, this is the parent view that the fragment's UI
         *                           should be attached to.  The fragment should not add the view
         *                           itself, but this can be used to generate the LayoutParams of
         *                           the view.
         * @param savedInstanceState If non-null, this fragment is being re-constructed from a
         *                           previous saved state as given here.
         */
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
                savedInstanceState) {
            final View view = inflater.inflate(R.layout.authenticator_fragment_base, container,
                    false);
            mFormContainerView = view.findViewById(R.id.content_container);
            mProgressContainerView = view.findViewById(R.id.progress_container);
            return view;
        }

        /**
         * Show or hide a progress indicator over the form view.
         *
         * @param isShown {@code true} to show the progress indicator, {@code false} to hide the
         *                indicator
         */
        public void showProgressIndicator(final boolean isShown) {
            final int animTime = mContext.getResources().getInteger(android.R.integer
                    .config_shortAnimTime);

            mFormContainerView.setVisibility(isShown ? View.GONE : View.VISIBLE);
            mFormContainerView.animate().setDuration(animTime).alpha(isShown ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFormContainerView.setVisibility(isShown ? View.GONE : View.VISIBLE);
                        }
                    });

            mProgressContainerView.setVisibility(isShown ? View.VISIBLE : View.GONE);
            mProgressContainerView.animate().setDuration(animTime).alpha(isShown ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mProgressContainerView.setVisibility(isShown ? View.VISIBLE : View
                                    .GONE);
                        }
                    });

        }

    }

    /**
     * Callback for interacting with a cursor loader manager.
     */
    //    private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> { TODO
    //
    //        @Override
    //        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    //            return new CursorLoader(getActivity(),
    //                    // Retrieve data rows for the device user's 'profile' contact.
    //                    Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
    //                            ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
    //                    ProfileQuery.PROJECTION,
    //
    //                    // Select only userName addresses.
    //                    ContactsContract.Contacts.Data.MIMETYPE + " = ?",
    //                    new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
    //
    //                    // Show primary userName addresses first. Note that there won't be
    //                    // a primary userName address if the user hasn't specified one.
    //                    ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    //        }

    //        @Override
    //        public void onLoaderReset(Loader<Cursor> loader) {
    //        }
    //
    //        @Override
    //        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
    //            List<String> emails = new ArrayList<>();
    //            data.moveToFirst();
    //            while (!data.isAfterLast()) {
    //                emails.add(data.getString(ProfileQuery.ADDRESS));
    //                data.moveToNext();
    //            }
    //
    //            addEmailsToAutoComplete(emails);
    //        }
    //
    //    }

    /**
     * Helper class for an authenticator fragment showing the "login" screen for an existing
     * account.
     */
    private class LoginHelper extends LayoutHelper {

        /**
         * Status code for login error.
         */
        private static final int STATUS_CODE_INACTIVE_ACCOUNT = 412;

        /**
         * Set whether a request is currently in progress.
         */
        private boolean mIsRequestInProgress = false;
        /**
         * Callback manager for Facebook login.
         */
        private CallbackManager mCallbackManager = null;
        /**
         * View for holding account name.
         */
        private TextInputLayout mAccountNameLayout;
        /**
         * View for holding account password.
         */
        private TextInputLayout mPasswordLayout;

        /**
         * Create a new login helper.
         *
         * @param context the context used to access resources
         */
        public LoginHelper(Context context) {
            super(context);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            // Set the action bar title.
            final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.label_toolbar_signIn));
            }
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            if (mCallbackManager != null) {
                return mCallbackManager.onActivityResult(requestCode, resultCode, data);
            } else {
                return false;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle
                savedInstanceState) {
            final View baseView = super.onCreateView(inflater, root, savedInstanceState);
            final ViewGroup formContainer = (ViewGroup) baseView.findViewById(FORM_CONTAINER);
            final View formView;

            // *****Set up sign in page based on context of new or existing account.***** //
            switch (getArguments().getInt(Constants.ARG_AUTHENTICATOR_REASON)) {
                case Constants.AUTHENTICATOR_REASON_ADD_ACCOUNT:
                    formView = inflater.inflate(
                            R.layout.authenticator_fragment_newaccount_signin, formContainer, true);
                    // Set up Facebook login.
                    final LoginButton facebookButton =
                            (LoginButton) formView.findViewById(R.id.button_facebookLogin);
                    facebookButton.setFragment(AuthenticatorFragment.this);
                    mCallbackManager = CallbackManager.Factory.create();
                    facebookButton.registerCallback(mCallbackManager, new FacebookLoginCallback());
                    // Give user access to the create new account screen.
                    final Button newAccountButton = (Button) formView
                            .findViewById(R.id.button_newAccount);
                    newAccountButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            if (mListener != null) {
                                mListener.onClickNewAccountButton();
                            }
                        }

                    });
                    break;
                case Constants.AUTHENTICATOR_REASON_CONFIRM_CREDENTIALS:
                case Constants.AUTHENTICATOR_REASON_GET_AUTH_TOKEN:
                case Constants.AUTHENTICATOR_REASON_UPDATE_CREDENTIALS:
                    formView = inflater.inflate(R.layout.authenticator_fragment_confirm_credentials,
                            formContainer, true);
                    // Set the account name field.
                    final TextView accountName =
                            (TextView) formView.findViewById(R.id.text_accountName);
                    accountName.setText(getArguments().getString(Constants.ARG_ACCOUNT_NAME));
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            // Set up the account name field.
            mAccountNameLayout = (TextInputLayout) formView.findViewById(R.id
                    .textInputLayout_accountName);
            // Set up the password field.
            mPasswordLayout = (TextInputLayout) formView.findViewById(R.id
                    .textInputLayout_password);
            mPasswordLayout.getEditText().setOnEditorActionListener(
                    new TextView.OnEditorActionListener() {

                        @Override
                        public boolean onEditorAction(TextView textView, int id, KeyEvent
                                keyEvent) {
                            attemptAuthentication();
                            return true;
                        }

                    });
            // Set up the "sign in" button to interact with the server.
            final Button signInButton = (Button) formView.findViewById(R.id.button_signIn);
            signInButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    attemptAuthentication();
                }

            });
            return baseView;
        }

        /**
         * Attempt to authenticate the account specified by the login form. If there are form errors
         * (invalid userName, missing fields, etc.), the errors are presented and no actual login
         * attempt is made.
         */
        private void attemptAuthentication() {
            // Do not execute if an authentication attempt is already in progress.
            if (!mIsRequestInProgress) {
                // Reset errors.
                if (mAccountNameLayout != null) {
                    mAccountNameLayout.setErrorEnabled(false);
                }
                mPasswordLayout.setError(null);

                boolean isValidInput = true;
                View focusView = null;

                // *****Check for a valid account name.***** //
                if (mAccountNameLayout != null) {
                    final String accountName =
                            mAccountNameLayout.getEditText().getText().toString();
                    if (TextUtils.isEmpty(accountName)) {
                        mAccountNameLayout
                                .setError(getString(R.string.userInput_error_requiredField));
                        focusView = mAccountNameLayout;
                        isValidInput = false;
                    } else if (!isValidEmailAddress(accountName)) {
                        mAccountNameLayout
                                .setError(getString(R.string.userInput_error_emailInvalid));
                        focusView = mAccountNameLayout;
                        isValidInput = false;
                    }
                }

                // *****Check for a valid password.***** //
                final String password = mPasswordLayout.getEditText().getText().toString();
                if (TextUtils.isEmpty(password)) {
                    mPasswordLayout.setError(getString(R.string.userInput_error_requiredField));
                    if (isValidInput) {
                        focusView = mPasswordLayout;
                        isValidInput = false;
                    }
                } else if (!isPasswordValid(password)) {
                    mPasswordLayout.setError(getString(R.string.userInput_error_passwordTooShort));
                    if (isValidInput) {
                        focusView = mPasswordLayout;
                        isValidInput = false;
                    }
                }

                // *****Input is valid, attempt authentication.***** //
                if (isValidInput) {

                    // *****Create the network request headers and response listener.***** //
                    final LoginRequest request = new LoginRequest();
                    final Map<String, String> headers = NetworkUtils.getAuthenticationHeaders
                            (getActivity());
                    final LoginResponseListener listener = new LoginResponseListener();

                    // *****Create network request body.***** //
                    request.pushRegistrationId = "12"; // TODO
                    request.loginType = 1;
                    request.email =
                            ((TextView) getView().findViewById(R.id.text_accountName)).getText()
                                    .toString();
                    request.password = mPasswordLayout.getEditText().getText().toString();

                    // *****Launch the network request.***** //
                    final GsonRequest<LoginResponse> gson =
                            new GsonRequest<>(request.getUrl(getContext()),
                                    LoginResponse.class, headers, new Gson().toJson(request),
                                    listener, listener);
                    // Set the tag used to cancel in-flight requests if fragment is stopped.
                    gson.setTag(AuthenticatorFragment.this);
                    mIsRequestInProgress = true;
                    mLayoutHelper.showProgressIndicator(true);
                    VolleySingleton.getInstance(getActivity()).addToRequestQueue(gson);
                    // Simulate network access. TODO
                    //                    try {
                    //                        Thread.sleep(2000);
                    //                    } catch (InterruptedException e) {
                    //                    }
                    //                    final ConnectionResponse response = new ConnectionResponse();
                    //                    response.status = 200;
                    //                    response.authToken = "token";
                    //                    response.alias = "Test";
                    //                    response.userName = request.userName;
                    //                    response.gender = "M";
                    //                    response.error = "An error occurred.";
                    //                    new LoginResponseListener().onResponse(response);
                }

                // *****Input is invalid, allow user to correct errors.***** //
                else {
                    // Focus the first form field with an error.
                    focusView.requestFocus();
                }
            }

        }

        /**
         * Update account user meta data based on the specified login response.
         *
         * @param response the response to use to update user data
         */
        private void updateUserData(LoginResponse response) {
            final AccountManager manager = AccountManager.get(getContext());
            final String accountName = response.userName;
            final Account account = AccountUtils.getAccountFromName(getContext(), accountName);
            manager.setUserData(account, AccountAuthenticator.USER_DATA_ID,
                    Integer.toString(response.userId));
            manager.setUserData(account, AccountAuthenticator.USER_DATA_ALIAS, response.alias);
            manager.setUserData(account, AccountAuthenticator.USER_DATA_GENDER,
                    Integer.toString(response.gender));
            manager.setUserData(account, AccountAuthenticator.USER_DATA_AVATAR_URI,
                    response.imageUrl);
            manager.setUserData(account, AccountAuthenticator.USER_DATA_AVATAR_URI,
                    response.imageUrl);
        }

        /**
         * Class for handling Facebook logins.
         */
        private class FacebookLoginCallback implements FacebookCallback<LoginResult> {

            @Override
            public void onSuccess(LoginResult loginResult) {
                // Close the login screen, reporting the login operation as cancelled.
                if (mListener != null) {
                    final Bundle result = new Bundle();
                    result.putInt(AccountManager.KEY_ERROR_CODE,
                            AccountManager.ERROR_CODE_CANCELED);
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, "Cancelled"); // TODO
                    mListener.onResultReceived(result);
                }
            }

            @Override
            public void onCancel() {
                // No operation.
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT); // TODO
            }

        }

        /**
         * Class for handling network responses.
         */
        private class LoginResponseListener implements Response.Listener<LoginResponse>,
                Response.ErrorListener {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (Constants.DEBUG && (error.networkResponse != null)) {
                    Log.d(DEBUG_TAG, "Status Code " + error.networkResponse.statusCode);
                    Log.d(DEBUG_TAG, new String(error.networkResponse.data));
                }
                // Clear request and hide progress indicator.
                mIsRequestInProgress = false;
                mLayoutHelper.showProgressIndicator(false);
                // Show the response error message.
                final String message;
                switch (error.networkResponse.statusCode) {
                    case STATUS_CODE_INACTIVE_ACCOUNT:
                        message = getString(R.string.message_account_inactive);
                        break;
                    default:
                        message = NetworkUtils.getErrorMessage(error);
                        break;
                }
                if (!TextUtils.isEmpty(message)) {
                    showDialog(message);
                }
            }

            @Override
            public void onResponse(LoginResponse response) {
                // Clear request and hide progress indicator.
                mIsRequestInProgress = false;
                mLayoutHelper.showProgressIndicator(false);
                // Determine the contents of the response.
                final AccountManager manager = AccountManager.get(getActivity());
                final Account account = AccountUtils.getAccountFromName(getActivity(),
                        response.userName);
                final String password = mPasswordLayout.getEditText().getText().toString();
                final String authTokenType = getString(R.string
                        .authenticator_authTokenType);
                switch (getArguments().getInt(Constants.ARG_AUTHENTICATOR_REASON, -1)) {
                    case Constants.AUTHENTICATOR_REASON_ADD_ACCOUNT:
                        // Add a new account to the account manager.
                        manager.addAccountExplicitly(account, password, null);
                        manager.setAuthToken(account, authTokenType, response.authToken);
                        // Add user data from the response.
                        updateUserData(response);
                        // Notify listener of new account.
                        if (mListener != null) {
                            final Bundle result = new Bundle();
                            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                            mListener.onResultReceived(result);
                        }
                        break;
                    case Constants.AUTHENTICATOR_REASON_CONFIRM_CREDENTIALS:
                        // Update login credentials.
                        manager.setPassword(account, password);
                        manager.setAuthToken(account, authTokenType, response.authToken);
                        // Update user data from the response.
                        updateUserData(response);
                        // Notify listener of successful credentials confirmation.
                        if (mListener != null) {
                            final Bundle result = new Bundle();
                            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                            mListener.onResultReceived(result);
                        }
                        break;
                    case Constants.AUTHENTICATOR_REASON_GET_AUTH_TOKEN:
                        // Save new auth token.
                        manager.setAuthToken(account, authTokenType, response.authToken);
                        // Update user data from the response.
                        updateUserData(response);
                        // Notify listener of new auth token.
                        if (mListener != null) {
                            final Bundle result = new Bundle();
                            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                            result.putString(AccountManager.KEY_AUTHTOKEN, response.authToken);
                            mListener.onResultReceived(result);
                        }
                        break;
                    // case AuthenticatorActivity.REASON_UPDATE_CREDENTIALS: // TODO
                    // result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    // result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    // break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }

    }

    /**
     * Helper class for an authenticator fragment showing the "new account" screen for creating a
     * new account from scratch.
     */
    private class NewAccountHelper extends LayoutHelper {

        /**
         * Set whether a server request is currently in progress.
         */
        private boolean mIsRequestInProgress = false;
        /**
         * View for holding gender selection.
         */
        private RadioGroup mGenderLayout;

        /**
         * View for holding user location.
         */
        private TextInputLayout mAddressLayout;
        /**
         * View for holding account password confirmation.
         */
        private TextInputLayout mConfirmPasswordLayout;
        /**
         * View for holding account email address.
         */
        private TextInputLayout mEmailLayout;
        /**
         * View for holding account password.
         */
        private TextInputLayout mPasswordLayout;
        /**
         * View for holding account user name.
         */
        private TextInputLayout mUsernameLayout;

        /**
         * Default constructor.
         */
        public NewAccountHelper(Context context) {
            super(context);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            // Set the action bar title.
            final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.label_toolbar_newAccount));
            }
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            return false;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle
                savedInstanceState) { // TODO gender
            final View baseView = super.onCreateView(inflater, root, savedInstanceState);
            final ViewGroup formContainer = (ViewGroup) baseView.findViewById(FORM_CONTAINER);
            final View formView =
                    inflater.inflate(R.layout.authenticator_fragment_newaccount_signup,
                            formContainer, true);
            // Get form fields.
            mUsernameLayout = (TextInputLayout) formView
                    .findViewById(R.id.textInputLayout_username);
            mEmailLayout = (TextInputLayout) formView.findViewById(R.id.textInputLayout_email);
            mPasswordLayout = (TextInputLayout) formView
                    .findViewById(R.id.textInputLayout_password);
            mConfirmPasswordLayout = (TextInputLayout) formView
                    .findViewById(R.id.textInputLayout_passwordConfirm);
            mAddressLayout =
                    (TextInputLayout) formView.findViewById(R.id.textInputLayout_userAddress);
            mGenderLayout = (RadioGroup) formView.findViewById(R.id.radioGroup_gender);
            // Set up the "sign up" button to interact with the server.
            final Button signUpButton = (Button) formView.findViewById(R.id.button_signUp);
            signUpButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    attemptAuthentication();
                }

            });
            return baseView;
        }

        /**
         * Attempt to authenticate the account specified by the login form. If there are form errors
         * (invalid username, missing fields, etc.), the errors are presented and no actual login
         * attempt is made.
         */
        private void attemptAuthentication() {
            // Do not execute if an authentication attempt is already in progress.
            if (!mIsRequestInProgress) {
                // Reset errors.
                mUsernameLayout.setErrorEnabled(false);
                mEmailLayout.setErrorEnabled(false);
                mPasswordLayout.setErrorEnabled(false);
                mConfirmPasswordLayout.setErrorEnabled(false);

                boolean isValidInput = true;
                View focusView = null;

                // *****Check for a valid username.***** //
                final String username = mUsernameLayout.getEditText().getText().toString();
                if (TextUtils.isEmpty(username)) {
                    mUsernameLayout.setError(getString(R.string.userInput_error_requiredField));
                    focusView = mUsernameLayout;
                    isValidInput = false;
                }

                // *****Check for a valid account name.***** //
                final String email = mEmailLayout.getEditText().getText().toString();
                if (TextUtils.isEmpty(email)) {
                    mEmailLayout.setError(getString(R.string.userInput_error_requiredField));
                    focusView = mEmailLayout;
                    isValidInput = false;
                } else if (!isValidEmailAddress(email)) {
                    mEmailLayout.setError(getString(R.string.userInput_error_emailInvalid));
                    focusView = mEmailLayout;
                    isValidInput = false;
                }

                // *****Check for a valid password.***** //
                final String password = mPasswordLayout.getEditText().getText().toString();
                if (TextUtils.isEmpty(password)) {
                    mPasswordLayout.setError(getString(R.string.userInput_error_requiredField));
                    if (isValidInput) {
                        focusView = mPasswordLayout;
                        isValidInput = false;
                    }
                } else if (!isPasswordValid(password)) {
                    mPasswordLayout.setError(getString(R.string.userInput_error_passwordTooShort));
                    if (isValidInput) {
                        focusView = mPasswordLayout;
                        isValidInput = false;
                    }
                }

                // *****Check that the password confirmation matches the password.***** //
                final String confirmPassword = mConfirmPasswordLayout.getEditText().getText()
                        .toString();
                if (TextUtils.isEmpty(confirmPassword)) {
                    mConfirmPasswordLayout
                            .setError(getString(R.string.userInput_error_requiredField));
                    if (isValidInput) {
                        focusView = mConfirmPasswordLayout;
                        isValidInput = false;
                    }
                } else if (!confirmPassword.equals(password)) {
                    mConfirmPasswordLayout
                            .setError(getString(R.string.userInput_error_passwordMismatch));
                    if (isValidInput) {
                        focusView = mConfirmPasswordLayout;
                        isValidInput = false;
                    }
                }

                // *****Input is valid, attempt authentication.***** //
                if (isValidInput) {
                    // Create the network request.
                    final SignUpRequest request = new SignUpRequest();
                    request.loginType = 1;
                    request.username = username;
                    request.email = email;
                    request.password = password;
                    final String userAddress = mAddressLayout.getEditText().getText().toString();
                    if (!TextUtils.isEmpty(userAddress)) {
                        request.userAddress = userAddress;
                    }
                    switch (mGenderLayout.getCheckedRadioButtonId()) {
                        case -1:
                            break;
                        case R.id.radio_male:
                            request.gender = "1";
                            break;
                        case R.id.radio_female:
                            request.gender = "2";
                            break;
                        case R.id.radio_other:
                            request.gender = "3";
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    final Map<String, String> headers = NetworkUtils.getAuthenticationHeaders
                            (getActivity());
                    final SignUpResponseListener listener = new SignUpResponseListener();
                    // Start the network request.
                    final GsonRequest<SignUpResponse> gson =
                            new GsonRequest<>(request.getUrl(getContext()),
                                    SignUpResponse.class, headers, new Gson().toJson(request),
                                    listener, listener);
                    // Set the tag used to cancel in-flight requests if fragment is stopped.
                    gson.setTag(AuthenticatorFragment.this);
                    mIsRequestInProgress = true;
                    mLayoutHelper.showProgressIndicator(true);
                    VolleySingleton.getInstance(getActivity()).addToRequestQueue(gson);
                }

                // *****Input was invalid, allow user to correct errors.***** //
                //                else { TODO do we want to do this?
                //                    focusView.requestFocus();
                //                }
            }
        }

        /**
         * Class for handling network responses.
         */
        private class SignUpResponseListener implements Response.Listener<SignUpResponse>,
                Response.ErrorListener {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (Constants.DEBUG && (error.networkResponse != null)) {
                    Log.d(DEBUG_TAG, "Status Code " + error.networkResponse.statusCode);
                    Log.d(DEBUG_TAG, new String(error.networkResponse.data));
                }
                // Clear request and hide progress indicator.
                mIsRequestInProgress = false;
                showProgressIndicator(false);
                // Extract and display error message from returned data.
                final String message = NetworkUtils.getErrorMessage(error);
                if (!TextUtils.isEmpty(message)) {
                    showDialog(message);
                }
            }

            @Override
            public void onResponse(SignUpResponse response) {
                // Clear request and hide progress indicator.
                mIsRequestInProgress = false;
                mLayoutHelper.showProgressIndicator(false);
                // Determine the contents of the response.
                final AccountManager manager = AccountManager.get(getContext());
                final String accountName = mEmailLayout.getEditText().getText().toString();
                final Account account = AccountUtils.getAccountFromName(getContext(), accountName);
                final String password = mPasswordLayout.getEditText().getText().toString();
                manager.addAccountExplicitly(account, password, null);
                // Send result bundle to the response listener.
                if (mListener != null) {
                    final Bundle bundle = new Bundle();
                    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    mListener.onResultReceived(bundle);
                }
            }

        }

        //        /**
        //         * Represents an asynchronous login/registration task used to authenticate the user.
        //         */
        //        private class SignUpTask extends AsyncTask<Void, Void, ConnectionResponse> {
        //
        //            /**
        //             * The account name.
        //             */
        //            private final SignUpRequest mRequest;
        //
        //            /**
        //             * Create a new user login task.
        //             *
        //             * @param request the request to send to the server
        //             */
        //            public SignUpTask(SignUpRequest request) {
        //                mRequest = request;
        //            }
        //
        //            @Override
        //            protected void onPreExecute() {
        //                // Show a progress spinner.
        //                mLayoutHelper.showProgressIndicator(true);
        //            }
        //
        //            @Override
        //            protected ConnectionResponse doInBackground(Void... params) {
        //                // Get the response from the server.
        //                return NetworkUtils.authenticateWithServer(getActivity(), mRequest);
        //            }
        //
        //            @Override
        //            protected void onCancelled() {
        //                mSignUpTask = null;
        //                mLayoutHelper.showProgressIndicator(false);
        //            }
        //
        //            @Override
        //            protected void onPostExecute(final ConnectionResponse response) {
        //                // Clear task and hide progress indicator.
        //                mSignUpTask = null;
        //                mLayoutHelper.showProgressIndicator(false);
        //                // Determine the contents of the response.
        //                if (response != null) {
        //                    final Bundle args = getArguments();
        //
        //                    // *****Authentication with the server was successful.***** //
        //                    if (response.status == 200) {
        //                        final AccountManager manager = AccountManager.get(getActivity());
        //                        final String accountType = args.getString(ARG_ACCOUNT_TYPE);
        //                        final Account account = new Account(response.userName, accountType);
        //                        final String password = mPasswordView.getText().toString();
        //                        manager.addAccountExplicitly(account, password, null);
        //                        // Send result bundle to the response listener.
        //                        if (mListener != null) {
        //                            final Bundle bundle = new Bundle();
        //                            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, response
        // .userName);
        //                            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        //                            mListener.onResultReceived(bundle);
        //                        }
        //                    }
        //
        //                    // *****Authentication with the server was not successful.***** //
        //                    else {
        //                        // Show the response error message. TODO
        //                        final Bundle message = new Bundle();
        //                        message.putString(JournalDialogFragment.ARG_FREETEXT_MESSAGE, response
        // .error);
        //                        message.putString(JournalDialogFragment.ARG_FREETEXT_POSITIVE_BUTTON_TEXT,
        //                                getActivity().getString(R.string.dialog_positiveButton_ok));
        //                        message.putBoolean(JournalDialogFragment
        //                                .ARG_FREETEXT_INCLUDE_NEGATIVE_BUTTON, false);
        //                        JournalDialogFragment.newInstance(message).show(getFragmentManager(),
        //                                null);
        //                    }
        //                }
        //            }
        //        }
        //
        //    }
        //
        //    /**
        //     * Use an AsyncTask to fetch the user's userName addresses on a background thread, and update the
        //     * userName text field with results on the main UI thread.
        //     */
        //    class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> { TODO
        //
        //        @Override
        //        protected List<String> doInBackground(Void... voids) {
        //            ArrayList<String> emailAddressCollection = new ArrayList<String>();
        //
        //            // Get all emails from the user's contacts and copy them to a list.
        //            ContentResolver cr = getContentResolver();
        //            Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        // null, null, null, null);
        //            while (emailCur.moveToNext()) {
        //                String userName = emailCur.getString(emailCur.getColumnIndex(ContactsContract
        // .CommonDataKinds.Email.DATA));
        //                emailAddressCollection.add(userName);
        //            }
        //            emailCur.close();
        //
        //            return emailAddressCollection;
        //        }
        //
        //        @Override
        //        protected void onPostExecute(List<String> emailAddressCollection) {
        //            addEmailsToAutoComplete(emailAddressCollection);
        //        }
    }

}