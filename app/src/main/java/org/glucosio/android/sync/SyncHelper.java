/*
 * Copyright (c) Joaquim Ley 2016. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glucosio.android.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.Log;

import org.glucosio.android.R;

public final class SyncHelper {

    private static final String TAG = "SyncHelper";

    private static final int SYNC_INTERVAL = (int) (DateUtils.DAY_IN_MILLIS / 1000); // Once per day
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final int SYNCABLE_TRUE = 1;
    private static final int SYNCABLE_FALSE = 0;

    public static final String SYNC_SHARED_PREFERENCES_NAME = "glucosioSyncServiceSharedPreferencesName";
    public static final String SYNC_DRIVE_FOLDER_ID = "glucosioSyncServiceFolderId";
    public static final String SYNC_FILE_TITLE = "glucosioSyncServiceFileTitle";
    public static final String SYNC_LOCAL_FILE_PATH = "glucosioSyncServiceFilePath";

    private SyncHelper() {
    }

    public static boolean initializeSync(Context context, String fileTitle, String localFilePath) {
        Account syncAccount = getSyncAccount(context);
        if (syncAccount != null) {
            onAccountCreated(context, syncAccount, fileTitle, localFilePath);
            Log.v(TAG, "Syncing initialized");
            return true;
        }
        Log.v(TAG, "Syncing failed, no account permissions.");
        return false;
    }

    /**
     * Get a dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account getSyncAccount(Context context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "We do not have accounts permission, canceling sync");
            return null;
        }

        Account[] accounts = AccountManager.get(context)
                .getAccountsByType(context.getString(R.string.sync_account_type));
        if (accounts.length == 0) {
            Log.d(TAG, "Must have a Google account installed");
            return null;
        }
        return accounts[0];
    }

    private static void onAccountCreated(Context context, Account newAccount, String fileTitle,
                                         String localFilePath) {
        /*
         * Inform the system that this account supports sync
         */
        ContentResolver.setIsSyncable(newAccount, context.getString(R.string.sync_authority), SYNCABLE_TRUE);
        /*
         * Enable our periodic sync.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);
        /*
         * Since we've created an account
         */
        configurePeriodicSync(context, newAccount);
        /*
         * Finally, let's do a sync to get things started
         */
        syncToDriveFolder(context, "", fileTitle, localFilePath);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    private static void configurePeriodicSync(Context context, Account account) {
        final String authority = context.getString(R.string.sync_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // We can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .setExtras(Bundle.EMPTY)
                    .syncPeriodic(SYNC_INTERVAL, SYNC_FLEXTIME)
                    .setSyncAdapter(account, authority)
                    .build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, SYNC_INTERVAL);
        }
        Log.d(TAG, "Periodic sync configured with " + SYNC_INTERVAL + " interval and " + SYNC_FLEXTIME + " flextime");
    }

    /**
     * Helper method to disable automatic sync
     */
    public static void cancelSyncService(Context context) {
        ContentResolver.setIsSyncable(getSyncAccount(context),
                context.getString(R.string.sync_authority), SYNCABLE_FALSE);
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncNow(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.sync_authority), bundle);
    }

    public static void syncToDriveFolder(Context context, String driveFolderId, String fileTitle, String filePath) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putString(SYNC_DRIVE_FOLDER_ID, driveFolderId);
        bundle.putString(SYNC_FILE_TITLE, fileTitle);
        bundle.putString(SYNC_LOCAL_FILE_PATH, filePath);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.sync_authority), bundle);
    }
}
