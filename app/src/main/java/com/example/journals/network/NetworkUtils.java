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
package com.example.journals.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.example.journals.R;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utilities for accessing a network and performing operations.
 */
public class NetworkUtils {

    /**
     * The resource ID for the base URL of the Blyspot server.
     */
    private static final int URL_SERVER = R.string.network_url_server;

    /**
     * Private constructor.
     */
    private NetworkUtils() {
    }

    /**
     * Get the availability of the network.
     *
     * @param context the context in which to access resources
     * @return {@code true} if network is available, {@code false} otherwise
     */
    public static boolean isNetworkAvailable(@NonNull Context context) {
        final ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        return ((networkInfo != null) && networkInfo.isConnected());
    }

    /**
     * Get a map of headers for server authentication (login, account creation).
     * <p>
     * Designed to be used with a Volley {@link Request}.
     *
     * @param context the context used to access resources
     * @return a map of headers for server authentication
     */
    @NonNull
    public static Map<String, String> getAuthenticationHeaders(@NonNull Context context) {
        final Map<String, String> headers = new HashMap<>(4);
        headers.put(context.getString(R.string.network_header_apiKey),
                context.getString(R.string.authenticator_apiKey));
        headers.put(context.getString(R.string.network_header_deviceType), "1");
        headers.put(context.getString(R.string.network_header_deviceId), "0");
        return headers;
    }

    /**
     * Get a map of headers for transferring data with the server (downloading journals, etc.). *
     * <p>
     * Designed to be used with a Volley {@link Request}.
     *
     * @param context   the context used to access resources
     * @param authToken optional auth token to authenticate user with the server
     * @return a map of headers for transferring data with the server
     */
    @NonNull
    public static Map<String, String> getDataTransferHeaders(@NonNull Context context,
                                                             @Nullable String authToken) {
        final Map<String, String> headers = new HashMap<>(3);
        if (authToken != null) {
            headers.put(context.getString(R.string.network_header_authToken), authToken);
        }
        headers.put(context.getString(R.string.network_header_apiKey),
                context.getString(R.string.authenticator_apiKey));
        headers.put(context.getString(R.string.network_header_deviceType), "1");
        return headers;
    }

    /**
     * Extract error message from a returned {@link VolleyError}.
     *
     * @return the extracted error message, or {@code null} if there is no error message
     */
    @Nullable
    public static String getErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            final ErrorResponse response = new Gson()
                    .fromJson(new String(error.networkResponse.data), ErrorResponse.class);
            if (!TextUtils.isEmpty(response.error)) {
                return response.error;
            } else if (!TextUtils.isEmpty(response.message)) {
                return response.message;
            }
        }
        return null;
    }

    /**
     * Base class for a connection request.
     */
    public static abstract class ConnectionRequest {

        /**
         * Get the class type of the response that will be returned from this request.
         *
         * @return the class type of the response that will be returned from this request.
         */
        @NonNull
        public abstract Class<? extends ConnectionResponse> getResponseClass();

        /**
         * Get the URL to service the connection request.
         *
         * @param context the context used to access resources
         * @return the URL to service the connection request
         */
        @NonNull
        public abstract String getUrl(Context context);

    }

    /**
     * Class for uploading a journal.
     */
    public static class CreateJournalRequest extends ConnectionRequest {

        /**
         * Title of the journal.
         * <p>
         * Required.
         * <p>
         * Type: String
         */
        @SerializedName("journal_title")
        public String title;
        /**
         * Set whether the journal is public or private.
         * <p>
         * Optional.
         * <p>
         * Type: int; 1 - public (default), 2 - private
         */
        @SerializedName("make_public")
        public Integer isPublic;
        /**
         * ID of the journal.  Required when you want to edit the journal, If you passing this
         * meaning you are in edit mode.
         * <p>
         * Optional.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public Long id;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return CreateJournalResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_createJournal);
        }

    }

    /**
     * Request for uploading a journal activity.
     */
    public static class CreateActivityRequest extends ConnectionRequest {

        /**
         * ID of the journal.
         * <p>
         * Required.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long journalId;
        /**
         * Title of the activity.
         * <p>
         * Required.
         * <p>
         * Type: String
         */
        @SerializedName("activity_title")
        public String title;
        /**
         * Activity description.
         * <p>
         * Required.
         * <p>
         * Type: String
         */
        @SerializedName("activity_description")
        public String description;
        /**
         * Location of the activity. May be used to search for the location in a mapping app.
         * <p>
         * Required.
         * <p>
         * Type: String
         */
        @SerializedName("activity_location")
        public String location;
        /**
         * Latitude coordinate of the activity location.
         * <p>
         * Required.
         * <p>
         * Type: double
         */
        @SerializedName("activity_loc_latitude")
        public double latitude;
        /**
         * Longitude coordinate of the activity location.
         * <p>
         * Required.
         * <p>
         * Type: double
         */
        @SerializedName("activity_loc_longitude")
        public double longitude;
        /**
         * Base 64 encoded media file data, or "FALSE" if no file data is to be added. Allowed file
         * types are: gif|jpg|png|avi|mpeg|mp4.
         * <p>
         * Required.
         * <p>
         * Type: String
         */
        @SerializedName("activity_file_data")
        public String fileData;
        /**
         * ID of the activity.  Required when you want to edit the activity; if you are passing this
         * it means you are in edit mode.
         * <p>
         * Optional
         * <p>
         * Type: long
         */
        @SerializedName("activity_id")
        public Long activityId;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return CreateActivityResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_createJournalActivity);
        }

    }

    /**
     * Request for uploading a journal contact.
     */
    public static class CreateContactRequest extends ConnectionRequest {

        /**
         * ID of the journal.
         * <p>
         * Required.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long journalId;
        /**
         * Contact name.
         * <p>
         * Required
         * <p>
         * Type: String
         */
        @SerializedName("poc_name")
        public String name;
        /**
         * Contact description.
         * <p>
         * Required
         * <p>
         * Type: String
         */
        @SerializedName("poc_description")
        public String description;
        /**
         * Contact email address.
         * <p>
         * Optional
         * <p>
         * Type: String
         */
        @SerializedName("poc_email")
        public String email;
        /**
         * Contact phone number.
         * <p>
         * Optional
         * <p>
         * Type: String
         */
        @SerializedName("poc_phone")
        public String phone;
        /**
         * ID of the contact.  Required when you want to edit the contact; if you are passing this
         * it means you are in edit mode.
         * <p>
         * Optional
         * <p>
         * Type: long
         */
        @SerializedName("poc_id")
        public Long contactId;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return null;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_createJournalPoc);
        }

    }

    /**
     * Request for deleting a journal from the server.
     */
    public static class DeleteJournalRequest extends ConnectionRequest {

        /**
         * ID of the journal.
         * <p>
         * Required.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long id;
        /**
         * ID of the activity to delete.  Required to delete an activity within a journal.  Cannot
         * be used with {@link #contactId}.
         * <p>
         * Optional.
         * <p>
         * Type: long.
         */
        @SerializedName("activity_id")
        public Long activityId;
        /**
         * ID of the contact to delete.  Required to delete a contact within a journal.  Cannot be
         * used with {@link #activityId}.
         * <p>
         * Optional.
         * <p>
         * Type: long.
         */
        @SerializedName("poc_id")
        public Long contactId;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return GetJournalsResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_deleteJournal);
        }

    }

    /**
     * Request for getting journal details from the server.
     */
    public static class GetJournalDetailsRequest extends ConnectionRequest {

        /**
         * ID of the journal.
         * <p>
         * Required.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long journalId;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return GetJournalDetailsResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_getJournalDetails);
        }

    }

    /**
     * Request for getting journals from the server.
     */
    public static class GetJournalsRequest extends ConnectionRequest {

        /**
         * Start index of journal list (ex. 1, 11, 21, etc.).
         * <p/>
         * Required.
         * <p/>
         * Type: int
         */
        @SerializedName("page_index")
        public int pageIndex;
        /**
         * Total page count (ex. 10, 20, 30, etc.).
         * <p/>
         * Required.
         * <p/>
         * Type: int
         */
        @SerializedName("page_count")
        public int pageCount;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return GetJournalsResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_getJournals);
        }

    }

    /**
     * Request for sending login data to the server.
     */
    public static class LoginRequest extends ConnectionRequest {

        /**
         * Android : GCM registration ID, IOS : Apple push registration ID
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("push_registration_id")
        public String pushRegistrationId;
        /**
         * Login type of request.
         * <p/>
         * Required.
         * <p/>
         * Type: int (1-email, 2-Facebook)
         */
        @SerializedName("login_type")
        public int loginType;
        /**
         * Account email address.
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("email_id")
        public String email;
        /**
         * Account password.
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("password")
        public String password;
        /**
         * Facebook ID.  Required if login type is "Facebook".
         * <p/>
         * Optional.
         * <p/>
         * Type: String
         */
        @SerializedName("facebookId")
        public String facebookId;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return LoginResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_logIn);
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            if (pushRegistrationId != null) {
                builder.append("push_registration_id=").append(pushRegistrationId);
            }
            builder.append("loginType=").append(loginType);
            if (email != null) {
                builder.append("userName=").append(email);
            }
            if (password != null) {
                builder.append("password=").append(password);
            }
            if (facebookId != null) {
                builder.append("facebookId=").append(facebookId);
            }
            return builder.toString();
        }

    }

    /**
     * Request for getting journals associated with the logged in user from the server.
     */
    public static class MyJournalsRequest extends GetJournalsRequest {

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return GetJournalsResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_myJournals);
        }
    }

    /**
     * Request for searching journals.
     */
    public static class SearchJournalsRequest extends ConnectionRequest {

        /**
         * User search input.
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("search_text")
        public String searchText;
        /**
         * Start index of journal list (ex. 1, 11, 21, etc.).
         * <p/>
         * Required.
         * <p/>
         * Type: int
         */
        @SerializedName("page_index")
        public int pageIndex;
        /**
         * Total page count (ex. 10, 20, 30, etc.).
         * <p/>
         * Required.
         * <p/>
         * Type: int
         */
        @SerializedName("page_count")
        public int pageCount;
        /**
         * Longitude of centroid for location-based search.  Required when location-based search is
         * enabled.
         * <p/>
         * Optional.
         * <p/>
         * Type: double
         */
        @SerializedName("longitude")
        public Double longitude;
        /**
         * Latitude of centroid for location-based search.  Required when location-based search is
         * enabled.
         * <p/>
         * Optional.
         * <p/>
         * Type: double
         */
        @SerializedName("latitude")
        public Double latitude;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return SearchJournalsResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_searchJournals);
        }

    }

    /**
     * Request for sending sign up data to the server.
     */
    public static class SignUpRequest extends ConnectionRequest {

        /**
         * Login type of request.
         * <p/>
         * Required.
         * <p/>
         * Type: int (1-email, 2-Facebook)
         */
        @SerializedName("login_type")
        public int loginType;
        /**
         * Facebook ID.  Required if login type is "Facebook".
         * <p/>
         * Optional.
         * <p/>
         * Type: String
         */
        @SerializedName("facebookId")
        public String facebookId;
        /**
         * User display name.
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("alias_name")
        public String username;
        /**
         * User gender.
         * <p/>
         * Required.
         * <p/>
         * Type: String ("1"-Male, "2"-female, "3"-other)
         */
        @SerializedName("gender")
        public String gender;
        /**
         * Location of user.
         * <p/>
         * Optional.
         * <p/>
         * Type: String
         */
        @SerializedName("user_address")
        public String userAddress;
        /**
         * Account email address.
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("email_id")
        public String email;
        /**
         * Account password.
         * <p/>
         * Required.
         * <p/>
         * Type: String
         */
        @SerializedName("password")
        public String password;

        @Override
        @NonNull
        public Class<? extends ConnectionResponse> getResponseClass() {
            return SignUpResponse.class;
        }

        @Override
        @NonNull
        public String getUrl(Context context) {
            return context.getString(URL_SERVER) +
                    context.getString(R.string.network_path_signUp);
        }

    }

    /**
     * Base class for responses received from the server.
     */
    public static class ConnectionResponse {

        /**
         * The HTTP status of the response.
         */
        @SerializedName("status")
        public int status;

    }

    /**
     * Class representing error responses received from the server.
     */
    public static class ErrorResponse extends ConnectionResponse {

        /**
         * Error message.
         */
        @SerializedName("error")
        public String error;
        /**
         * Alternate key for error message.
         * <p/>
         * Currently only known use is user signup error 413 ("Email already registered.")
         */
        @SerializedName("message")
        public String message;

    }

    /**
     * Class representing response from a "create activity" request from the server.
     */
    public static class CreateActivityResponse extends ConnectionResponse {

        /**
         * ID of the journal this activity is in.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long journalId;
        /**
         * ID of the activity.
         * <p>
         * Type: long
         */
        @SerializedName("activity_id")
        public long id;
        /**
         * Path of the activity media.
         * <p>
         * Type: String
         */
        @SerializedName("activity_file_path")
        public String filePath;

    }

    /**
     * Class representing response from a "create contact" request from the server.
     */
    public static class CreateContactResponse extends ConnectionResponse {

        /**
         * ID of the journal this activity is in.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long journalId;
        /**
         * ID of the contact.
         * <p>
         * Type: long
         */
        @SerializedName("poc_id")
        public long id;

    }

    /**
     * Class representing response from a "create journal" request from the server.
     */
    public static class CreateJournalResponse extends ConnectionResponse {

        /**
         * ID of the journal.
         * <p>
         * Type: long
         */
        @SerializedName("journal_id")
        public long id;
        /**
         * Set whether the journal is public or private.
         * <p>
         * Type: int; 1 - public, 2 - private
         */
        @SerializedName("isPublic")
        public int isPublic;

    }

    /**
     * Class representing response from a "delete journal" request from the server.
     */
    public static class DeleteJournalResponse extends ConnectionResponse {

    }

    /**
     * Class representing response from a "get journal details" request from the server.
     */
    public static class GetJournalDetailsResponse extends ConnectionResponse {

        /**
         *
         */
        @SerializedName("records")
        public ArrayList<Journal> journal;
        /**
         *
         */
        @SerializedName("activities")
        public ArrayList<Activity> activities;
        /**
         *
         */
        @SerializedName("media")
        public ArrayList<Media> media;
        /**
         *
         */
        @SerializedName("poc")
        public ArrayList<Contact> contacts;

    }

    /**
     * Class representing response from a "get journals" request from the server.
     */
    public static class GetJournalsResponse extends ConnectionResponse {

        /**
         *
         */
        @SerializedName("total_count")
        public int totalCount;
        /**
         *
         */
        @SerializedName("records")
        public ArrayList<Journal> journals;

    }

    /**
     * Class representing the result of a server login.
     */
    public static class LoginResponse extends ConnectionResponse {

        /**
         *
         */
        @SerializedName("user_id")
        public int userId;
        /**
         *
         */
        @SerializedName("auth_key")
        public String authToken;
        /**
         *
         */
        @SerializedName("alias_name")
        public String alias;
        /**
         *
         */
        @SerializedName("email_id")
        public String userName;
        /**
         *
         */
        @SerializedName("gender")
        public int gender;
        /**
         *
         */
        @SerializedName("image_url")
        public String imageUrl;

    }

    /**
     * Class representing response from a "search journals" request from the server.
     */
    public static class SearchJournalsResponse extends ConnectionResponse {

        /**
         *
         */
        @SerializedName("records")
        public SearchRecords records;

        /**
         *
         */
        public static class SearchRecords {

            /**
             *
             */
            @SerializedName("journal")
            public ArrayList<Journal> searchJournals;
            /**
             *
             */
            @SerializedName("nearby")
            public ArrayList<Journal> nearbyJournals;

        }

    }

    /**
     * Class representing the result of a sign up request.
     */
    public static class SignUpResponse extends ConnectionResponse {

        /**
         *
         */
        @SerializedName("message")
        public String message;

    }

    /**
     * Class representing an activity downloaded from the server.
     */
    public static class Activity implements Parcelable {

        public static final Parcelable.Creator<Activity> CREATOR =
                new Parcelable.Creator<Activity>() {

                    @Override
                    public Activity createFromParcel(Parcel source) {
                        return new Activity(source);
                    }

                    @Override
                    public Activity[] newArray(int size) {
                        return new Activity[size];
                    }

                };

        /**
         *
         */
        @SerializedName("activity_id")
        public Long activityId;
        /**
         *
         */
        @SerializedName("title")
        public String title;
        /**
         *
         */
        @SerializedName("description")
        public String description;
        /**
         *
         */
        @SerializedName("location")
        public String location;
        /**
         *
         */
        @SerializedName("created_at")
        public String createdAt;
        /**
         *
         */
        @SerializedName("latitude")
        public double latitude;
        /**
         *
         */
        @SerializedName("longitude")
        public double longitude;
        /**
         *
         */
        @SerializedName("user_id")
        public int userId;
        /**
         * Field used for storing media information related to an activity.
         * <p>
         * Used by the app to make parsing media easier.
         */
        public ArrayList<Media> media;

        /**
         * Create a new spot.
         */
        public Activity() {
        }

        /**
         * Create a new spot from a parcel.
         *
         * @param source the parcel to create a spot from
         */
        private Activity(Parcel source) {
            activityId = source.readLong();
            title = source.readString();
            description = source.readString();
            location = source.readString();
            createdAt = source.readString();
            latitude = source.readDouble();
            longitude = source.readDouble();
            userId = source.readInt();
            media = new ArrayList<>();
            source.readTypedList(media, Media.CREATOR);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(activityId);
            dest.writeString(title);
            dest.writeString(description);
            dest.writeString(location);
            dest.writeString(createdAt);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            dest.writeInt(userId);
            dest.writeTypedList(media);
        }
    }

    /**
     * Class representing a local contact downloaded from the server.
     */
    public static class Contact implements Parcelable {

        public static final Parcelable.Creator<Contact> CREATOR = new Parcelable
                .Creator<Contact>() {

            @Override
            public Contact createFromParcel(Parcel source) {
                return new Contact(source);
            }

            @Override
            public Contact[] newArray(int size) {
                return new Contact[size];
            }

        };

        /**
         *
         */
        @SerializedName("contact_id")
        public Long contactId;
        /**
         *
         */
        @SerializedName("contact_name")
        public String name;
        /**
         *
         */
        @SerializedName("contact_email")
        public String email;
        /**
         *
         */
        @SerializedName("contact_phone")
        public String phone;
        /**
         *
         */
        @SerializedName("contact_desc")
        public String description;

        /**
         * Create a new contact.
         */
        public Contact() {
        }

        /**
         * Create a new local contact from a parcel.
         *
         * @param source the parcel to create a local contact from
         */
        private Contact(Parcel source) {
            contactId = source.readLong();
            name = source.readString();
            email = source.readString();
            phone = source.readString();
            description = source.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(contactId);
            dest.writeString(name);
            dest.writeString(email);
            dest.writeString(phone);
            dest.writeString(description);
        }

    }

    /**
     * Class representing a journal downloaded from the server.
     */
    public static class Journal implements Parcelable {

        public static final Creator<Journal> CREATOR = new Creator<Journal>() {

            @Override
            public Journal createFromParcel(Parcel source) {
                return new Journal(source);
            }

            @Override
            public Journal[] newArray(int size) {
                return new Journal[size];
            }

        };

        /**
         *
         */
        @SerializedName("user_alias")
        public String userAlias;
        /**
         *
         */
        @SerializedName("user_id")
        public long userId;
        /**
         *
         */
        @SerializedName("user_gender")
        public int userGender;
        /**
         *
         */
        @SerializedName("scrapbook_id")
        public long journalId;
        /**
         *
         */
        @SerializedName("title")
        public String title;
        /**
         *
         */
        @SerializedName("scrapbook_privacy")
        public int privacy;
        /**
         *
         */
        @SerializedName("prof_image")
        public String imageUrl;
        /**
         *
         */
        @SerializedName("contentType")
        public int contentType;
        /**
         *
         */
        @SerializedName("createdAt")
        public String createdAt;
        /**
         *
         */
        @SerializedName("activities")
        public ArrayList<Activity> activities;
        /**
         *
         */
        @SerializedName("media")
        public ArrayList<Media> media;
        /**
         *
         */
        @SerializedName("pocs")
        public ArrayList<Contact> contacts;

        /**
         * Create a new journal from a parcel.
         *
         * @param source the parcel to create a journal from
         */
        private Journal(Parcel source) {
            userAlias = source.readString();
            userId = source.readLong();
            userGender = source.readInt();
            journalId = source.readLong();
            title = source.readString();
            privacy = source.readInt();
            imageUrl = source.readString();
            contentType = source.readInt();
            createdAt = source.readString();
            activities = new ArrayList<>();
            source.readTypedList(activities, Activity.CREATOR);
            media = new ArrayList<>();
            source.readTypedList(media, Media.CREATOR);
            contacts = new ArrayList<>();
            source.readTypedList(contacts, Contact.CREATOR);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Journal) {
                final Journal response = (Journal) object;
                return journalId == response.journalId;
            }
            return false;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(userAlias);
            dest.writeLong(userId);
            dest.writeInt(userGender);
            dest.writeLong(journalId);
            dest.writeString(title);
            dest.writeInt(privacy);
            dest.writeString(imageUrl);
            dest.writeInt(contentType);
            dest.writeString(createdAt);
            dest.writeTypedList(activities);
            dest.writeTypedList(media);
            dest.writeTypedList(contacts);
        }

    }

    /**
     * Class representing location information downloaded from the server.
     */
    public static class Location implements Parcelable {

        public static final Parcelable.Creator<Location> CREATOR =
                new Parcelable.Creator<Location>() {

                    @Override
                    public Location createFromParcel(Parcel source) {
                        return new Location(source);
                    }

                    @Override
                    public Location[] newArray(int size) {
                        return new Location[size];
                    }

                };

        /**
         *
         */
        @SerializedName("address")
        public String address;
        /**
         *
         */
        @SerializedName("latitude")
        public String latitude;
        /**
         *
         */
        @SerializedName("longitude")
        public String longitude;

        /**
         * Create a new location response from a parcel.
         *
         * @param source the parcel to create a location response from
         */
        private Location(Parcel source) {
            address = source.readString();
            latitude = source.readString();
            longitude = source.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(address);
            dest.writeString(latitude);
            dest.writeString(longitude);
        }

    }

    /**
     * Class representing media information downloaded from the server.
     */
    public static class Media implements Parcelable {

        public static final Parcelable.Creator<Media> CREATOR =
                new Parcelable.Creator<Media>() {

                    @Override
                    public Media createFromParcel(Parcel source) {
                        return new Media(source);
                    }

                    @Override
                    public Media[] newArray(int size) {
                        return new Media[size];
                    }

                };

        /**
         *
         */
        @SerializedName("media_id")
        public long id;
        /**
         *
         */
        @SerializedName("activity_id")
        public long activityId;
        /**
         *
         */
        @SerializedName("media_path")
        public String path;
        /**
         *
         */
        @SerializedName("mdeia_name")
        public String title;
        /**
         *
         */
        @SerializedName("created_at")
        public String createdAt;

        /**
         * Create new media.
         */
        public Media() {
        }

        /**
         * Create new media from a parcel.
         *
         * @param source the parcel to create a user response from
         */
        private Media(Parcel source) {
            id = source.readLong();
            activityId = source.readLong();
            path = source.readString();
            title = source.readString();
            createdAt = source.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeLong(activityId);
            dest.writeString(path);
            dest.writeString(title);
            dest.writeString(createdAt);
        }

    }

    /**
     * Class representing user information downloaded from the server.
     */
    public static class User implements Parcelable {

        public static final Parcelable.Creator<User> CREATOR =
                new Parcelable.Creator<User>() {

                    @Override
                    public User createFromParcel(Parcel source) {
                        return new User(source);
                    }

                    @Override
                    public User[] newArray(int size) {
                        return new User[size];
                    }

                };

        /**
         *
         */
        @SerializedName("user_id")
        public long userId;
        /**
         *
         */
        @SerializedName("user_name")
        public String userName;

        /**
         * Create a new user response from a parcel.
         *
         * @param source the parcel to create a user response from
         */
        private User(Parcel source) {
            userId = source.readLong();
            userName = source.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(userId);
            dest.writeString(userName);
        }

    }

}