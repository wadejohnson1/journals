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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.journals.journal.JournalDialogFragment.DialogListener;
import com.example.journals.R;
import com.example.journals.account.AccountUtils;
import com.example.journals.network.GsonRequest;
import com.example.journals.network.NetworkUtils;
import com.example.journals.network.NetworkUtils.Contact;
import com.example.journals.network.NetworkUtils.CreateContactRequest;
import com.example.journals.network.NetworkUtils.CreateContactResponse;
import com.example.journals.network.NetworkUtils.DeleteJournalRequest;
import com.example.journals.network.NetworkUtils.DeleteJournalResponse;
import com.example.journals.provider.JournalContract.Contacts;
import com.example.journals.provider.QueryHandler;
import com.example.journals.provider.QueryHandler.SimpleQueryListener;
import com.example.journals.widget.AuthHandlerFragment;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

/**
 * A fragment for editing local contact details and associated data.
 * <p/>
 * If creating a new contact, this fragment takes a content URI of type {@link
 * Contacts#CONTENT_TYPE_DIR}. If updating an existing contact, this fragment takes an content URI
 * of type {@link Contacts#CONTENT_TYPE_ITEM}.
 */
public class ContactEditorFragment extends AuthHandlerFragment implements DialogListener {

    /**
     * Reason for requesting an auth token.
     */
    private static final int AUTH_REASON_CREATE_CONTACT = 0, AUTH_REASON_DELETE_CONTACT = 1;
    /**
     * Dialog type for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_CONFIRM_DELETE = 0,
            DIALOG_TYPE_CONFIRM_DISCARD_CHANGES = 1;
    /**
     * Flags for recording changes in data properties.
     */
    private static final int FLAG_IS_LOADED = 1, FLAG_IS_DELETED = 1 << 1,
            FLAG_IS_CHANGED_NAME = 1 << 2, FLAG_IS_CHANGED_EMAIL = 1 << 3,
            FLAG_IS_CHANGED_PHONE = 1 << 4, FLAG_IS_CHANGED_DESCRIPTION = 1 << 5;
    /**
     * Tag for dialogs.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Saved instance state key for storing the "description" text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_DESCRIPTION = "SAVE_STATE_DESCRIPTION";
    /**
     * Saved instance state key for storing the "email" text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_EMAIL = "SAVE_STATE_EMAIL";
    /**
     * Saved instance state key for storing the "name" text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_NAME = "SAVE_STATE_NAME";
    /**
     * Saved instance state key for storing the "phone" text.
     * <p/>
     * Type: String
     */
    private static final String SAVE_STATE_PHONE = "SAVE_STATE_PHONE";
    /**
     * Saved instance state key for storing flags.
     */
    private static final String SAVE_STATE_FLAGS = "SAVE_STATE_FLAGS";
    /**
     * Projection for building a cursor loader.
     */
    private static final String[] QUERY_PROJECTION = new String[]{
            Contacts.COLUMN_ID, Contacts.COLUMN_NAME,
            Contacts.COLUMN_DESCRIPTION,
            Contacts.COLUMN_EMAIL, Contacts.COLUMN_PHONE};

    /**
     * Set whether the fragment is updating an existing journal entry (as opposed to creating a new
     * one).
     * <p/>
     * Used only when content is editable.
     */
    private boolean mIsUpdating;
    /**
     * Flags for setting journal entry state.
     * <p/>
     * Used only when content is editable.
     */
    private int mFlags = 0;
    /**
     * Text input layout containing "description" edit text.
     */
    private TextInputLayout mLayoutDescription;
    /**
     * Text input layout containing "email" edit text.
     */
    private TextInputLayout mLayoutEmail;
    /**
     * Text input layout containing "name" edit text.
     */
    private TextInputLayout mLayoutName;
    /**
     * Text input layout containing "phone" edit text.
     */
    private TextInputLayout mLayoutPhone;
    /**
     * The URI of the data associated with this fragment.
     * <p/>
     * Used only when content is editable.
     */
    private Uri mContentUri;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Contact contact = getArguments().getParcelable(Constants.ARG_DATA);
        final View view = getView();
        // Set up toolbar.
        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getArguments().getInt(Constants.ARG_HOME_BUTTON_IMAGE));
        toolbar.setNavigationOnClickListener(new NavigationOnClickListener());
        toolbar.inflateMenu(R.menu.menu_contact_editor);
        toolbar.setOnMenuItemClickListener(new OnMenuItemClickListener());
        // Set up commonly used views.
        mLayoutName = view.findViewById(R.id.layout_name);
        mLayoutEmail = view.findViewById(R.id.layout_email);
        mLayoutPhone = view.findViewById(R.id.layout_phone);
        mLayoutDescription = view.findViewById(R.id.layout_description);

        // *****Restore saved instance state, if applicable.***** //
        if (savedInstanceState != null) {
            // Determine if journal entry was saved before it finished loading.
            final int flags = savedInstanceState.getInt(SAVE_STATE_FLAGS);
            if ((flags & FLAG_IS_LOADED) != 0) {
                mFlags = flags;
                // Setup text fields.
                mLayoutName.getEditText().setText(savedInstanceState.getString(SAVE_STATE_NAME));
                mLayoutEmail.getEditText().setText(savedInstanceState.getString(SAVE_STATE_EMAIL));
                mLayoutPhone.getEditText().setText(savedInstanceState.getString(SAVE_STATE_PHONE));
                mLayoutDescription.getEditText()
                        .setText(savedInstanceState.getString(SAVE_STATE_DESCRIPTION));
                setupTextChangedListeners();
            }
        }

        // *****Set up the UI from passed in arguments or provider data.***** //
        // TODO does not take into account journal entries in provider that do not have server ID
        if ((mFlags & FLAG_IS_LOADED) == 0) {
            // An existing journal entry is being updated.
            if (contact != null) {
                // Load data from the provider, if any exists.
                final QueryHandler handler = new QueryHandler(getContext().getContentResolver());
                handler.setQueryListener(new QueryListener());
                handler.startQuery(0, null, Contacts.CONTENT_URI, QUERY_PROJECTION,
                        Contacts.COLUMN_SERVER_ID + "=?",
                        new String[]{contact.contactId.toString()}, null);
                // Disable updates until data is loaded.
                setEnabled(false);
            }
            // A new journal entry is being created.
            else {
                setupTextChangedListeners();
                mFlags |= FLAG_IS_LOADED;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.contact_editor_fragment, container, false);
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_DELETE:
                // Delete the journal entry from the provider.
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    // Mark as deleted.
                    mFlags |= FLAG_IS_DELETED;
                    // Remove existing activity from server and provider.
                    final Contact contact = getArguments().getParcelable(Constants.ARG_DATA);
                    if (contact != null) {
                        // Delete journal entry from the server.
                        final String accountName = AccountUtils.getActiveAccount(getContext());
                        AccountUtils.getAuthToken(getActivity(), AUTH_REASON_DELETE_CONTACT,
                                accountName, new AuthTokenListener());
                        // Delete the journal entry from the provider if it exists.
                        final QueryHandler handler =
                                new QueryHandler(getActivity().getContentResolver());
                        final ContentValues values = new ContentValues(1);
                        values.put(Contacts.COLUMN_IS_DELETED, 1);
                        handler.startUpdate(0, null, Contacts.CONTENT_URI, values,
                                Contacts.COLUMN_SERVER_ID + "=?",
                                new String[]{contact.contactId.toString()});
                        // Notify parent of changes.
                        getActivity().setResult(android.app.Activity.RESULT_OK);
                    }
                    // Close the activity.
                    getActivity().finish();
                }
                break;
            case DIALOG_TYPE_CONFIRM_DISCARD_CHANGES:
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    getActivity().finish();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Only save state if the entry has already finished loading.
        if ((mFlags & FLAG_IS_LOADED) != 0) {
            outState.putInt(SAVE_STATE_FLAGS, mFlags);
            outState.putString(SAVE_STATE_NAME, mLayoutName.getEditText().getText().toString());
            outState.putString(SAVE_STATE_EMAIL, mLayoutEmail.getEditText().getText().toString());
            outState.putString(SAVE_STATE_PHONE, mLayoutPhone.getEditText().getText().toString());
            outState.putString(SAVE_STATE_DESCRIPTION,
                    mLayoutDescription.getEditText().getText().toString());
        }
    }

    /**
     * Set whether the user input fields are enabled.
     *
     * @param isEnabled {@code true} if user input fields are enabled, {@code false} otherwise
     */
    private void setEnabled(boolean isEnabled) {
        // Set the "name" text.
        mLayoutName.setEnabled(isEnabled);
        // Set the "email" text.
        mLayoutEmail.setEnabled(isEnabled);
        // Set the "phone" text.
        mLayoutPhone.setEnabled(isEnabled);
        // Set the "description" text.
        mLayoutDescription.setEnabled(isEnabled);
    }

    /**
     * Setup text changed listeners.
     */
    private void setupTextChangedListeners() {
        mLayoutName.getEditText()
                .addTextChangedListener(new TextChangedListener(FLAG_IS_CHANGED_NAME));
        mLayoutEmail.getEditText()
                .addTextChangedListener(new TextChangedListener(FLAG_IS_CHANGED_EMAIL));
        mLayoutPhone.getEditText()
                .addTextChangedListener(new TextChangedListener(FLAG_IS_CHANGED_PHONE));
        mLayoutDescription.getEditText()
                .addTextChangedListener(new TextChangedListener(FLAG_IS_CHANGED_DESCRIPTION));
    }

    /**
     * Create and show a dialog of the specified type.
     * <p/>
     * {@link #onDialogResult(int, int, Intent)} will be called with the dialog result.
     *
     * @param dialogType the type of dialog to display
     */
    private void showDialog(int dialogType) {
        final JournalDialogFragment dialog;
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_DELETE:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_deleteContact, R.string.dialog_positiveButton_delete,
                        true);
                break;
            case DIALOG_TYPE_CONFIRM_DISCARD_CHANGES:
                dialog = JournalDialogFragment.newMessageDialog(dialogType,
                        R.string.dialog_message_discardChanges,
                        R.string.dialog_positiveButton_discard, true);
                break;
            default:
                throw new IllegalArgumentException();
        }
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Create and show a message dialog.
     */
    private void showMessageDialog(@NonNull String message) {
        final JournalDialogFragment dialog = JournalDialogFragment.newMessageDialog(0, message,
                R.string.dialog_positiveButton_ok, false);
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Save the current data properties to the provider.
     */
    private void updateProvider() {
        // Prevent saving if journal entry has been deleted.
        if ((mFlags & FLAG_IS_DELETED) != FLAG_IS_DELETED) {
            final ContentValues values = new ContentValues(4);
            final View view = getView();
            // Save the updated name.
            if ((mFlags & FLAG_IS_CHANGED_NAME) == FLAG_IS_CHANGED_NAME) {
                final TextView name = view.findViewById(R.id.text_name);
                if (!TextUtils.isEmpty(name.getText())) {
                    values.put(Contacts.COLUMN_NAME, name.getText().toString());
                }
                // If the text is empty, only save if updating an existing entry.
                else if (mIsUpdating) {
                    values.putNull(Contacts.COLUMN_NAME);
                }
            }
            // Save the updated description.
            if ((mFlags & FLAG_IS_CHANGED_DESCRIPTION) == FLAG_IS_CHANGED_DESCRIPTION) {
                final TextView description = view.findViewById(R.id.text_description);
                if (!TextUtils.isEmpty(description.getText())) {
                    values.put(Contacts.COLUMN_DESCRIPTION,
                            description.getText().toString());
                }
                // If the text is empty, only save if updating an existing entry.
                else if (mIsUpdating) {
                    values.putNull(Contacts.COLUMN_DESCRIPTION);
                }
            }
            // Save the updated email.
            if ((mFlags & FLAG_IS_CHANGED_EMAIL) == FLAG_IS_CHANGED_EMAIL) {
                final TextView email = view.findViewById(R.id.text_email);
                if (!TextUtils.isEmpty(email.getText())) {
                    values.put(Contacts.COLUMN_EMAIL, email.getText().toString());
                }
                // If the text is empty, only save if updating an existing entry.
                else if (mIsUpdating) {
                    values.putNull(Contacts.COLUMN_EMAIL);
                }
            }
            // Save the updated phone.
            if ((mFlags & FLAG_IS_CHANGED_PHONE) == FLAG_IS_CHANGED_PHONE) {
                final TextView phone = (TextView) view.findViewById(R.id.text_phone);
                if (!TextUtils.isEmpty(phone.getText())) {
                    values.put(Contacts.COLUMN_PHONE, phone.getText().toString());
                }
                // If the text is empty, only save if updating an existing entry.
                else if (mIsUpdating) {
                    values.putNull(Contacts.COLUMN_PHONE);
                }
            }
            // Update the content provider if any values have changed.
            if (values.size() > 0) {
                final QueryHandler handler = new QueryHandler(getActivity().getContentResolver());
                if (mIsUpdating) {
                    handler.startUpdate(0, null, mContentUri, values, null, null);
                } else {
                    handler.startInsert(0, null, mContentUri, values);
                }
            }
        }
    }

    /**
     * Class for transferring data with the server after receiving an auth token.
     */
    private class AuthTokenListener implements AccountUtils.AuthTokenListener {

        @Override
        public void onAuthTokenReceived(int requestCode, String accountName, String authToken) {
            if (isStarted()) {
                final GsonRequest request;
                final Contact contact = getArguments().getParcelable(Constants.ARG_DATA);
                switch (requestCode) {
                    case AUTH_REASON_CREATE_CONTACT:
                        final CreateContactRequest createRequest = new CreateContactRequest();
                        createRequest.journalId = getArguments().getLong(Constants.ARG_JOURNAL_ID);
                        createRequest.name = mLayoutName.getEditText().getText().toString();
                        final String email = mLayoutEmail.getEditText().getText().toString();
                        if (!TextUtils.isEmpty(email)) {
                            createRequest.email = email;
                        }
                        final String phone = mLayoutPhone.getEditText().getText().toString();
                        if (!TextUtils.isEmpty(phone)) {
                            createRequest.phone = phone;
                        }
                        createRequest.description =
                                mLayoutDescription.getEditText().getText().toString();
                        if (contact != null) {
                            createRequest.contactId = contact.contactId;
                        }
                        final CreateContactResponseListener createListener =
                                new CreateContactResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(createRequest.getUrl(getContext()),
                                CreateContactResponse.class,
                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(createRequest), createListener, createListener);
                        startRequest(request);
                        break;
                    case AUTH_REASON_DELETE_CONTACT:
                        final DeleteJournalRequest deleteRequest = new DeleteJournalRequest();
                        deleteRequest.id = getArguments().getLong(Constants.ARG_JOURNAL_ID);
                        deleteRequest.contactId = contact.contactId;
                        final DeleteContactResponseListener deleteListener =
                                new DeleteContactResponseListener(getActivity(), requestCode,
                                        accountName, this, authToken);
                        request = new GsonRequest<>(deleteRequest.getUrl(getContext()),
                                DeleteJournalResponse.class,
                                NetworkUtils.getDataTransferHeaders(getContext(), authToken),
                                new Gson().toJson(deleteRequest), deleteListener, deleteListener);
                        startRequest(request);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }

        @Override
        public void onAuthenticatorError(int requestCode) { // TODO
        }

        @Override
        public void onOperationCancelled(int requestCode) {
        }

        @Override
        public void onIOError(int requestCode) {
        }

    }

    /**
     * Class for processing "create contact" responses.
     */
    private class CreateContactResponseListener extends AccountUtils.AuthFailureErrorListener
            implements Response.Listener<CreateContactResponse> {

        /**
         * Create a new response listener that automatically handles auth failure errors.
         *
         * @param activity    activity used to access resources and launch a user credentials
         *                    dialog, if required
         * @param requestCode request code used to identify the request
         * @param accountName the name of the account
         * @param listener    listener for receiving auth token information
         * @param authToken   the auth token to invalidate in the event of an auth failure error
         */
        public CreateContactResponseListener(@NonNull Activity activity, int requestCode,
                                             @NonNull String accountName,
                                             @NonNull AuthTokenListener listener,
                                             @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(NetworkUtils.CreateContactResponse response) {
            getActivity().finish();
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showMessageDialog(message);
            }
        }

    }

    /**
     * Class for processing "delete contact" responses.
     */
    private class DeleteContactResponseListener extends AccountUtils.AuthFailureErrorListener
            implements Response.Listener<DeleteJournalResponse> {

        /**
         * Create a new response listener that automatically handles auth failure errors.
         *
         * @param activity    activity used to access resources and launch a user credentials
         *                    dialog, if required
         * @param requestCode request code used to identify the request
         * @param accountName the name of the account
         * @param listener    listener for receiving auth token information
         * @param authToken   the auth token to invalidate in the event of an auth failure error
         */
        public DeleteContactResponseListener(@NonNull android.app.Activity activity,
                                             int requestCode, @NonNull String accountName,
                                             @NonNull AuthTokenListener listener,
                                             @NonNull String authToken) {
            super(activity, requestCode, accountName, listener, authToken);
        }

        @Override
        public void onResponse(DeleteJournalResponse response) {
            getActivity().finish();
        }

        @Override
        public void onUnhandledErrorResponse(VolleyError error) {
            // Extract and display error message from returned data.
            final String message = NetworkUtils.getErrorMessage(error);
            if (!TextUtils.isEmpty(message)) {
                showMessageDialog(message);
            }
        }

    }

    /**
     * Listener for toolbar menu item clicks.
     */
    private class OnMenuItemClickListener implements Toolbar.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_save:
                    boolean hasError = false;
                    // Check for proper "name" text input.
                    final EditText name = mLayoutName.getEditText();
                    if (TextUtils.isEmpty(name.getText())) {
                        final String error = getString(R.string.userInput_error_requiredField);
                        mLayoutName.setError(error);
                        hasError = true;
                    } else {
                        mLayoutName.setErrorEnabled(false);
                    }
                    // Check for proper "email" text input (acceptable to have no email).
                    final EditText email = mLayoutEmail.getEditText();
                    final String emailText = email.getText().toString();
                    if (!TextUtils.isEmpty(emailText) &&
                            !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                        final String error = getString(R.string.userInput_error_emailInvalid);
                        mLayoutEmail.setError(error);
                        hasError = true;
                    } else {
                        mLayoutEmail.setErrorEnabled(false);
                    }
                    // Check for proper "description" text input.
                    final EditText description = mLayoutDescription.getEditText();
                    if (TextUtils.isEmpty(description.getText())) {
                        final String error = getString(R.string.userInput_error_requiredField);
                        mLayoutDescription.setError(error);
                        hasError = true;
                    } else {
                        mLayoutDescription.setErrorEnabled(false);
                    }
                    // Save updated data to the server.
                    if (!hasError) {
                        if (((mFlags & FLAG_IS_CHANGED_NAME) != 0) || (
                                ((mFlags & FLAG_IS_CHANGED_EMAIL) != 0)) ||
                                ((mFlags & FLAG_IS_CHANGED_PHONE) != 0) ||
                                ((mFlags & FLAG_IS_CHANGED_DESCRIPTION) != 0)) {
                            final String accountName = AccountUtils.getActiveAccount(getContext());
                            AccountUtils.getAuthToken(getActivity(), AUTH_REASON_CREATE_CONTACT,
                                    accountName, new AuthTokenListener());
                        }
                        // Finish the activity if nothing to save.
                        else {
                            getActivity().finish();
                        }
                    }
                    return true;
                case R.id.action_discardChanges:
                    // Show the confirm discard changes dialog.
                    if ((mFlags &= ~FLAG_IS_LOADED) > 0) {
                        showDialog(DIALOG_TYPE_CONFIRM_DISCARD_CHANGES);
                    }
                    // Finish the activity if nothing to save.
                    else {
                        getActivity().finish();
                    }
                    return true;
                case R.id.action_delete:
                    // Show the confirm delete dialog.
                    showDialog(DIALOG_TYPE_CONFIRM_DELETE);
                    return true;
                default:
                    return false;
            }
        }

    }

    /**
     * Listener for toolbar navigation icon clicks.
     */
    private class NavigationOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Show the confirm discard changes dialog.
            if ((mFlags &= ~FLAG_IS_LOADED) > 0) {
                showDialog(DIALOG_TYPE_CONFIRM_DISCARD_CHANGES);
            }
            // Finish the activity if nothing to save.
            else {
                getActivity().finish();
            }
        }

    }

    /**
     * Class used for listening for queries on the content provider.
     */
    private class QueryListener extends SimpleQueryListener {

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // Use data in the provider to populate the UI.
            if (cursor.moveToFirst()) {
                // Contact name.
                final String name =
                        cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_NAME));
                mLayoutName.getEditText().setText(name);
                // Contact email.
                final String email =
                        cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_EMAIL));
                mLayoutEmail.getEditText().setText(email);
                // Contact phone.
                final String phone =
                        cursor.getString(cursor.getColumnIndexOrThrow(Contacts.COLUMN_PHONE));
                mLayoutPhone.getEditText().setText(phone);
                // Contact description.
                final String description = cursor.getString(cursor.getColumnIndexOrThrow(
                        Contacts.COLUMN_DESCRIPTION));
                mLayoutDescription.getEditText().setText(description);
            }
            // Use data from the passed in journal entry.
            else {
                final Contact contact = getArguments().getParcelable(Constants.ARG_DATA);
                mLayoutName.getEditText().setText(contact.name);
                mLayoutEmail.getEditText().setText(contact.email);
                mLayoutPhone.getEditText().setText(contact.phone);
                mLayoutDescription.getEditText().setText(contact.description);
            }
            setupTextChangedListeners();
            setEnabled(true);
            mFlags |= FLAG_IS_LOADED;
        }

    }

    /**
     * Class used for listening for text changes.
     */
    private class TextChangedListener implements TextWatcher {

        /**
         * The flag this listener will set when it receives text changes.
         */
        private final int mFlag;

        /**
         * Create a new text changed listener.
         *
         * @param flag the flag this listener with set when it receives text changes
         */
        public TextChangedListener(int flag) {
            mFlag = flag;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mFlags |= mFlag;
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

    }

}