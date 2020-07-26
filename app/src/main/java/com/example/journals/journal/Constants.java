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

import android.content.Context;

import com.example.journals.account.AccountUtils;

import androidx.annotation.NonNull;

/**
 * Constant values used across multiple activities/fragments.
 */
public class Constants {

    /**
     * Set whether to show debug information.
     */
    public static final boolean DEBUG = true; // TODO change to false

    /**
     * Argument designating the name of the account being used
     * <p/>
     * Type: String
     */
    public static final String ARG_ACCOUNT_NAME = "ARG_ACCOUNT_NAME";
    /**
     * Argument designating reason authenticator is being tasked.  Must be one of {@link
     * #AUTHENTICATOR_REASON_ADD_ACCOUNT}, {@link #AUTHENTICATOR_REASON_CONFIRM_CREDENTIALS}, {@link
     * #AUTHENTICATOR_REASON_GET_AUTH_TOKEN} or {@link #AUTHENTICATOR_REASON_UPDATE_CREDENTIALS}.
     * <p/>
     * Type: int
     */
    public static final String ARG_AUTHENTICATOR_REASON = "ARG_AUTHENTICATOR_REASON";
    /**
     * Argument designating the data type used to store the journal, can be one of {@link
     * #DATA_TYPE_CONTENT_URI} or {@link #DATA_TYPE_PARCELED_OBJECT}.
     * <p/>
     * Type: int
     */
    public static final String ARG_DATA_TYPE = "ARG_DATA_TYPE";
    /**
     * Argument designating the data to operate on.
     * <p/>
     * Type: Parcelable
     */
    public static final String ARG_DATA = "ARG_DATA";
    /**
     * Argument designating the entry type being used, can be one of {@link #ENTRY_TYPE_CONTACT} or
     * {@link #ENTRY_TYPE_SPOT}.
     * <p/>
     * Type: int
     */
    public static final String ARG_ENTRY_TYPE = "ARG_ENTRY_TYPE";
    /**
     * Argument designating the image to display for the toolbar home button.
     * <p/>
     * Type: int (drawable resource ID)
     */
    public static final String ARG_HOME_BUTTON_IMAGE = "ARG_HOME_BUTTON_IMAGE";
    /**
     * Argument designating the journal ID.
     * <p/>
     * Type: long
     */
    public static final String ARG_JOURNAL_ID = "ARG_JOURNAL_ID";
    /**
     * Argument designating whether the specified data item is editable.
     * <p/>
     * Type: boolean
     */
    public static final String ARG_IS_EDITABLE = "ARG_IS_EDITABLE";
    /**
     * Authenticator reason.
     */
    public static final int AUTHENTICATOR_REASON_ADD_ACCOUNT = 1,
            AUTHENTICATOR_REASON_CONFIRM_CREDENTIALS = 2, AUTHENTICATOR_REASON_GET_AUTH_TOKEN = 3,
            AUTHENTICATOR_REASON_UPDATE_CREDENTIALS = 4;
    /**
     * Data type indicating the journal can be retrieved from the local provider via a content URI.
     * This data type is considered editable.
     */
    public static final int DATA_TYPE_CONTENT_URI = 1;
    /**
     * Data type indicating the journal can be retrieved from a parceled object. This data type is
     * considered read-only.
     */
    public static final int DATA_TYPE_PARCELED_OBJECT = 2;
    /**
     * Entry type indicating a local contact.
     */
    public static final int ENTRY_TYPE_CONTACT = 1;
    /**
     * Entry type indicating a spot.
     */
    public static final int ENTRY_TYPE_SPOT = 2;
    /**
     * Constant indicating male gender.
     */
    public static final String GENDER_MALE = "1";
    /**
     * Constant indicating female gender.
     */
    public static final String GENDER_FEMALE = "2";
    /**
     * Constant indicating other gender.
     */
    public static final String GENDER_OTHER = "3";
    /**
     * Navigation mode showing top journals.
     */
    public static final int NAVIGATION_MODE_TOP_JOURNALS = 1;
    /**
     * Navigation mode showing user journals.
     */
    public static final int NAVIGATION_MODE_YOUR_JOURNALS = 2;
    /**
     * Navigation mode showing journals located nearby.
     */
    public static final int NAVIGATION_MODE_NEARBY = 3;
    /**
     * Navigation mode showing search results.
     */
    public static final int NAVIGATION_MODE_SEARCH_RESULTS = 4;

    /**
     * Private constructor.
     */
    private Constants() {
    }

    /**
     * Get whether the specified user ID belongs to the current logged in user.
     *
     * @param context the context used to access resources
     * @param userId  the ID of the user
     * @return {@code true} if {@code ID} is the ID of the current logged in user, {@code false}
     * otherwise
     */
    public static boolean isCurrentUser(@NonNull Context context, long userId) {
        final String accountName = AccountUtils.getActiveAccount(context);
        return ((accountName != null) &&
                (AccountUtils.getUserDataId(context, accountName) == userId));
    }

}
