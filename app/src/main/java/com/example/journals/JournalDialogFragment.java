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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

/**
 * A fragment that displays journal dialogs.
 */
public class JournalDialogFragment extends DialogFragment {

    /**
     * Dialog action performed.
     */
    public static final int DIALOG_ACTION_POSITIVE_CLICK = 1, DIALOG_ACTION_NEGATIVE_CLICK = 2,
            DIALOG_ACTION_CONTENT_CLICK = 3, DIALOG_ACTION_DISMISSED = 4;
    /**
     * Key for accessing result data from the intent returned to the dialog listener.
     */
    public static final String DIALOG_DATA = "data";

    /**
     * Dialog type to display.
     */
    private static final int DIALOG_TYPE_MESSAGE_FROM_RESOURCE = 0,
            DIALOG_TYPE_MESSAGE_FROM_TEXT = 1, DIALOG_TYPE_TEXT_ENTRY = 2, DIALOG_TYPE_CHOICE = 3;
    /**
     * Argument indicating a list of choices to display in the dialog.
     * <p/>
     * Type: int
     */
    private static final String ARG_CHOICES = "choices";
    /**
     * Argument indicating a token used to identify this dialog.
     * <p/>
     * Type: int
     */
    private static final String ARG_TOKEN = "token";
    /**
     * Argument indicating if a negative button is displayed in the dialog.
     * <p/>
     * Type: boolean
     */
    private static final String ARG_SHOW_NEGATIVE_BUTTON = "showNegativeButton";
    /**
     * Argument indicating the resource ID of the dialog message.
     * <p/>
     * Type: int or String
     */
    private static final String ARG_MESSAGE = "message";
    /**
     * Argument indicating the resource ID of the text displayed in the positive button.
     * <p/>
     * Type: int
     */
    private static final String ARG_POSITIVE_BUTTON_TEXT = "positiveButtonText";
    /**
     * Argument indicating the resource ID of the text hint to display in an edit text used for user
     * input.
     * <p/>
     * Type: int
     */
    private static final String ARG_TEXT_HINT = "textHint";
    /**
     * Argument indicating the resource ID of the dialog title.
     * <p/>
     * Type: int
     */
    private static final String ARG_TITLE = "title";
    /**
     * An argument key used to specify the type of dialog to display.
     */
    private static final String DIALOG_TYPE = "dialogType";
    /**
     * Listener for dialog button clicks.
     */
    private DialogListener mListener = null;

    /**
     * Create a new dialog fragment that will display a choice dialog.
     *
     * @param token a token used to identify this dialog
     * @param title the resource ID of the message to display in the dialog
     * @return a new dialog fragment that will display a message dialog
     */
    public static JournalDialogFragment newChoiceDialog(int token, @StringRes int title,
                                                        @ArrayRes int choices) {
        final JournalDialogFragment fragment = new JournalDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(DIALOG_TYPE, DIALOG_TYPE_CHOICE);
        args.putInt(ARG_TOKEN, token);
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_CHOICES, choices);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new dialog fragment that will display a message dialog.
     *
     * @param token              a token used to identify this dialog
     * @param message            the resource ID of the message to display in the dialog
     * @param positiveButtonText the resource ID of the text to display in the positive action
     *                           button
     * @param showNegativeButton {@code true} to display the negative action button, {@code false}
     *                           to hide it
     * @return a new dialog fragment that will display a message dialog
     */
    public static JournalDialogFragment newMessageDialog(int token, @StringRes int message,
                                                         @StringRes int positiveButtonText,
                                                         boolean showNegativeButton) {
        final JournalDialogFragment fragment = new JournalDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(DIALOG_TYPE, DIALOG_TYPE_MESSAGE_FROM_RESOURCE);
        args.putInt(ARG_TOKEN, token);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putBoolean(ARG_SHOW_NEGATIVE_BUTTON, showNegativeButton);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new dialog fragment that will display a message dialog.
     *
     * @param token              a token used to identify this dialog
     * @param message            the message to display in the dialog
     * @param positiveButtonText the resource ID of the text to display in the positive action
     *                           button
     * @param showNegativeButton {@code true} to display the negative action button, {@code false}
     *                           to hide it
     * @return a new dialog fragment that will display a message dialog
     */
    public static JournalDialogFragment newMessageDialog(int token, String message,
                                                         @StringRes int positiveButtonText,
                                                         boolean showNegativeButton) {
        final JournalDialogFragment fragment = new JournalDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(DIALOG_TYPE, DIALOG_TYPE_MESSAGE_FROM_TEXT);
        args.putInt(ARG_TOKEN, token);
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putBoolean(ARG_SHOW_NEGATIVE_BUTTON, showNegativeButton);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new dialog fragment that will display a text entry dialog.
     *
     * @param token              a token used to identify this dialog
     * @param title              the resource ID of the message to display in the dialog
     * @param textHint           the resource ID of the text hint
     * @param positiveButtonText the resource ID of the text to display in the positive action
     *                           button
     * @return a new dialog fragment that will display a message dialog
     */
    public static JournalDialogFragment newTextEntryDialog(int token, @StringRes int title,
                                                           @StringRes int textHint,
                                                           @StringRes int positiveButtonText) {
        final JournalDialogFragment fragment = new JournalDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(DIALOG_TYPE, DIALOG_TYPE_TEXT_ENTRY);
        args.putInt(ARG_TOKEN, token);
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_TEXT_HINT, textHint);
        args.putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the optional dialog listener to send dialog result to.
        if (getTargetFragment() instanceof DialogListener) {
            mListener = (DialogListener) getTargetFragment();
        } else if (getActivity() instanceof DialogListener) {
            mListener = (DialogListener) getActivity();
        }
        // Get the dialog to display.
        final Bundle args = getArguments();
        switch (getArguments().getInt(DIALOG_TYPE)) {
            case DIALOG_TYPE_MESSAGE_FROM_RESOURCE:
                int messageId = args.getInt(ARG_MESSAGE);
                int positiveButtonText = args.getInt(ARG_POSITIVE_BUTTON_TEXT);
                boolean showNegativeButton = args.getBoolean
                        (ARG_SHOW_NEGATIVE_BUTTON);
                return getMessageDialog(messageId, positiveButtonText, showNegativeButton);
            case DIALOG_TYPE_MESSAGE_FROM_TEXT:
                String messageText = args.getString(ARG_MESSAGE);
                positiveButtonText = args.getInt(ARG_POSITIVE_BUTTON_TEXT);
                showNegativeButton = args.getBoolean(ARG_SHOW_NEGATIVE_BUTTON);
                return getMessageDialog(messageText, positiveButtonText, showNegativeButton);
            case DIALOG_TYPE_TEXT_ENTRY:
                int title = args.getInt(ARG_TITLE);
                int textHint = args.getInt(ARG_TEXT_HINT);
                positiveButtonText = args.getInt(ARG_POSITIVE_BUTTON_TEXT);
                return getEditTextDialog(title, textHint, positiveButtonText);
            case DIALOG_TYPE_CHOICE:
                title = args.getInt(ARG_TITLE);
                int choices = args.getInt(ARG_CHOICES);
                return getChoiceDialog(title, choices);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        dispatchDialogResult(DIALOG_ACTION_DISMISSED, null);
    }

    /**
     * Dispatch a dialog result to its listener.
     *
     * @param resultCode the action performed on the dialog
     * @param data       dialog-specific extra information associated with the dialog result
     */
    private void dispatchDialogResult(int resultCode, Intent data) {
        if (mListener != null) {
            mListener.onDialogResult(getArguments().getInt(ARG_TOKEN), resultCode, data);
        }
    }

    /**
     * Create an alert dialog presenting a list of choices.
     * <p/>
     * The dialog includes a cancel button to avoid selecting any choices.
     *
     * @param titleId the resource ID of the title to display
     * @param itemsId the resource ID of an array of choices to display
     * @return an alert dialog presenting a list of choices
     */
    private Dialog getChoiceDialog(int titleId, int itemsId) {
        // Construct an alert dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleId);
        builder.setItems(itemsId, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                final Intent intent = new Intent();
                intent.putExtra(DIALOG_DATA, which);
                dispatchDialogResult(DIALOG_ACTION_CONTENT_CLICK, intent);
            }

        });
        builder.setNegativeButton(R.string.dialog_negativeButton,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dispatchDialogResult(DIALOG_ACTION_NEGATIVE_CLICK, null);
                    }

                });
        return builder.create();
    }

    /**
     * Create an alert dialog with editable text.
     * <p/>
     * The dialog includes a positive and negative button to confirm the entered text or cancel.
     *
     * @param titleId          the resource ID of the title to display
     * @param textHint         a hint to display in the text field when empty
     * @param positiveButtonId the resourceID of the positive button text
     * @return an alert dialog with editable text
     */
    private Dialog getEditTextDialog(@StringRes int titleId, @StringRes int textHint, @StringRes
            int positiveButtonId) {
        // Get view for text input.
        final TextInputLayout view = (TextInputLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.journal_dialog_fragment_edittext, null);
        view.setHint(getString(textHint));
        final TextView textView = view.findViewById(R.id.text_title);
        // Construct an alert dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleId);
        builder.setView(view);
        builder.setPositiveButton(positiveButtonId, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final Intent intent = new Intent();
                intent.putExtra(DIALOG_DATA, textView.getText().toString());
                dispatchDialogResult(DIALOG_ACTION_POSITIVE_CLICK, intent);
            }
        });
        builder.setNegativeButton(R.string.dialog_negativeButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dispatchDialogResult(DIALOG_ACTION_NEGATIVE_CLICK, null);
                    }
                });
        // Put focus on the edit text and open the soft keyboard, if applicable.
        showSoftKeyboard(textView);
        return builder.create();
    }

    /**
     * Create an alert dialog with the specified message.
     * <p/>
     * The dialog includes a positive button and optional negative button to confirm the message or
     * cancel.
     *
     * @param messageId             the resource ID of the message to display
     * @param positiveButtonText    the resource ID of the text to display in the positive button
     * @param includeNegativeButton {@code true} if dialog has negative button, {@code false} if
     *                              dialog only has a positive button
     * @return an alert dialog with the specified message
     */
    private Dialog getMessageDialog(@StringRes int messageId, @StringRes int
            positiveButtonText, boolean includeNegativeButton) {
        final String message = getString(messageId);
        return getMessageDialog(message, positiveButtonText, includeNegativeButton);
    }

    /**
     * Create an alert dialog with the specified message.
     * <p/>
     * The dialog includes a positive button and optional negative button to confirm the message or
     * cancel.
     *
     * @param message               the message to display
     * @param positiveButtonText    the resource ID of the text to display in the positive button
     * @param includeNegativeButton {@code true} if dialog has negative button, {@code false} if
     *                              dialog only has a positive button
     * @return an alert dialog with the specified message
     */
    private Dialog getMessageDialog(String message, @StringRes int positiveButtonText, boolean
            includeNegativeButton) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message);
        builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dispatchDialogResult(DIALOG_ACTION_POSITIVE_CLICK, null);
            }

        });
        if (includeNegativeButton) {
            builder.setNegativeButton(R.string.dialog_negativeButton,
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            dispatchDialogResult(DIALOG_ACTION_NEGATIVE_CLICK, null);
                        }

                    });
        }
        return builder.create();
    }

    /**
     * Set the focus on the specified view and show the soft keyboard, if appropriate.
     *
     * @param view the view to set the focus on
     */
    private void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            final InputMethodManager manager = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Callback for receiving events from this fragment's dialogs.
     **/
    public interface DialogListener {

        /**
         * Called when an action is performed on the dialog.
         * <p/>
         * Note: This method may be called multiple times for the same dialog, once when the user
         * confirms and action (optional) and once when the dialog is dismissed by the user (always
         * occurs).
         *
         * @param dialogType the type of dialog that was presented
         * @param action     the action performed on the dialog
         * @param data       extra information related to the dialog action, which is dialog type
         *                   dependent
         */
        void onDialogResult(int dialogType, int action, Intent data);

    }
}
