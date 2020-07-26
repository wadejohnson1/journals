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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.journals.JournalDialogFragment;
import com.example.journals.JournalDialogFragment.DialogListener;
import com.example.journals.R;
import com.example.journals.network.NetworkUtils.Contact;
import com.example.journals.provider.JournalContract;
import com.example.journals.provider.JournalContract.Contacts;
import com.example.journals.provider.QueryHandler;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

/**
 * A fragment for editing local contact details and associated data.
 * <p/>
 * If creating a new contact, this fragment takes a content URI of type {@link
 * Contacts#CONTENT_TYPE_DIR}. If updating an existing contact, this fragment takes an content URI
 * of type {@link JournalContract.Contacts#CONTENT_TYPE_ITEM}.
 */
public class ContactDetailFragment extends Fragment implements DialogListener {

    /**
     * Dialog type for displaying a dialog fragment.
     */
    private static final int DIALOG_TYPE_CONFIRM_DELETE = 0;
    /**
     * Tag for dialogs.
     */
    private static final String DIALOG_TAG = "DIALOG_TAG";
    /**
     * Projection for building a cursor loader.
     */
    private static final String[] QUERY_PROJECTION = new String[]{
            Contacts.COLUMN_ID, JournalContract.Contacts.COLUMN_NAME,
            JournalContract.Contacts.COLUMN_DESCRIPTION,
            JournalContract.Contacts.COLUMN_EMAIL, JournalContract.Contacts.COLUMN_PHONE};

    /**
     * The URI of the data associated with this fragment.
     * <p/>
     * Used only when content is editable.
     */
    private Uri mContentUri;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Bundle args = getArguments();

        // *****Set up toolbar.***** //
        final Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);
        // Set home navigation icon.
        toolbar.setNavigationIcon(args.getInt(Constants.ARG_HOME_BUTTON_IMAGE));
        toolbar.setNavigationOnClickListener(new NavigationOnClickListener());

        // *****Set up fragment data.***** //
        switch (args.getInt(Constants.ARG_DATA_TYPE)) {
            case Constants.DATA_TYPE_CONTENT_URI:
                mContentUri = args.getParcelable(Constants.ARG_DATA);
                // Set menu items.
                toolbar.inflateMenu(R.menu.menu_contact_detail);
                toolbar.setOnMenuItemClickListener(new MenuItemClickListener());
                // Load data from the content provider.
                getLoaderManager().initLoader(0, null, new LoaderCallbacks());
                break;
            case Constants.DATA_TYPE_PARCELED_OBJECT:
                // Set the content.
                final Contact contact = args.getParcelable(Constants.ARG_DATA);
                setContent(contact.name, contact.email, contact.phone, contact.description);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contact_detail_fragment, container, false);
    }

    @Override
    public void onDialogResult(int dialogType, int action, Intent data) {
        switch (dialogType) {
            case DIALOG_TYPE_CONFIRM_DELETE:
                // Delete the contact from the provider.
                if (action == JournalDialogFragment.DIALOG_ACTION_POSITIVE_CLICK) {
                    final QueryHandler handler =
                            new QueryHandler(getActivity().getContentResolver());
                    handler.startDelete(0, null, mContentUri, null, null);
                    // Close the activity.
                    getActivity().finish();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Set the content for the contact.
     *
     * @param name        the contact name
     * @param email       the contact email
     * @param phone       the contact phone
     * @param description the contact description
     */
    private void setContent(String name, String email, String phone, String description) {
        final View view = getView();
        // Name text.
        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(
                (!TextUtils.isEmpty(name)) ? name : getString(R.string.defaultText_name));
        // Email text.
        final TextView emailText = view.findViewById(R.id.text_email);
        emailText.setText(
                (!TextUtils.isEmpty(email) ? email : getString(R.string.defaultText_email)));
        // Phone text.
        final TextView phoneText = view.findViewById(R.id.text_phone);
        phoneText.setText(
                (!TextUtils.isEmpty(phone) ? phone : getString(R.string.defaultText_phone)));
        // Description text.
        final TextView descriptionText = view.findViewById(R.id.text_description);
        descriptionText.setText((!TextUtils.isEmpty(description) ? description :
                getString(R.string.defaultText_description)));
    }

    /**
     * Callback for interacting with a cursor loader manager.
     */
    private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), mContentUri, QUERY_PROJECTION, null, null, null);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            if (data.moveToFirst()) {
                // Get the contact content.
                final String name =
                        data.getString(
                                data.getColumnIndexOrThrow(JournalContract.Contacts.COLUMN_NAME));
                final String email =
                        data.getString(
                                data.getColumnIndexOrThrow(JournalContract.Contacts.COLUMN_EMAIL));
                final String phone =
                        data.getString(data.getColumnIndexOrThrow(Contacts.COLUMN_PHONE));
                final String description = data.getString(
                        data.getColumnIndexOrThrow(JournalContract.Contacts.COLUMN_DESCRIPTION));
                setContent(name, email, phone, description);
            }
            // The item has been deleted from the provider.
            else {
                getActivity().finish();
            }
        }

    }

    /**
     * Listener for toolbar menu item clicks.
     */
    private class MenuItemClickListener implements Toolbar.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    // TODO
                    return true;
                case R.id.action_delete:
                    // Show the confirm delete dialog.
                    final JournalDialogFragment dialog = JournalDialogFragment
                            .newMessageDialog(DIALOG_TYPE_CONFIRM_DELETE,
                                    R.string.dialog_message_deleteContact,
                                    R.string.dialog_positiveButton_delete, true);
                    dialog.setTargetFragment(ContactDetailFragment.this, 0);
                    dialog.show(getFragmentManager(), DIALOG_TAG);
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
            getActivity().finish();
        }

    }

}