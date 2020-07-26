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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * Handle the transfer of data between a server and an app, using the Android sync adapter
 * framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    /**
     * Create a new sync adapter.
     *
     * @param context        the context the sync adapter is running in
     * @param autoInitialize if true then sync requests that have {@link ContentResolver#SYNC_EXTRAS_INITIALIZE}
     *                       set will be internally handled by the sync adapter by calling {@link
     *                       ContentResolver#setIsSyncable(Account, String, int)} with 1 if it is
     *                       currently set to <0.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Create a new sync adapter that allows for parallel account syncing.
     *
     * @param context            the {@link Context} that this is running within.
     * @param autoInitialize     if true then sync requests that have {@link ContentResolver#SYNC_EXTRAS_INITIALIZE}
     *                           set will be internally handled by the sync adapter by calling
     *                           {@link ContentResolver#setIsSyncable(Account, String, int)} with 1
     *                           if it is currently set to <0.
     * @param allowParallelSyncs if true then allow syncs for different accounts to run at the same
     *                           time, each in their own thread. This must be consistent with the
     *                           setting in the SyncAdapter's configuration file.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        // TODO
        Log.i("SyncAdapter","Performing sync.");
        Log.i("SyncAdapter",extras.toString());
    }

}