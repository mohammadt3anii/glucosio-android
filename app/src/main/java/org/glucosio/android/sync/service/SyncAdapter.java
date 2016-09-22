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

package org.glucosio.android.sync.service;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;

import org.glucosio.android.sync.SyncHelper;
import org.glucosio.android.sync.drive.UploadToFolderTask;
import org.glucosio.android.sync.view.FolderPickerActivity;
import org.glucosio.android.sync.view.SignInResolutionActivity;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private String TAG = "SyncAdapter";

    // Global variables
    // Define a variable to contain a content resolver instance
    private ContentResolver mContentResolver;
    private GoogleApiClient mGoogleApiClient;

    private String mFolderId;
    private String mFileTitle;
    private String mLocalFilePath;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s,
                              ContentProviderClient contentProviderClient, SyncResult syncResult) {

        Log.d(TAG, "on perform sync");
        mFolderId = bundle.getString(SyncHelper.SYNC_DRIVE_FOLDER_ID, "");
        mFileTitle = bundle.getString(SyncHelper.SYNC_FILE_TITLE, "");
        mLocalFilePath = bundle.getString(SyncHelper.SYNC_LOCAL_FILE_PATH, "");

        if (mGoogleApiClient == null) {
            initGoogleApiClient(getContext(), account.name);
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }
        sync();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected()");
        sync();
    }

    /**
     * Where the magic happens, extracted to a method due to GoogleApiClient connection
     * connected, this way we can control what happens when the client gets connected without
     * 2x code.
     */
    private void sync() {
        validateAgainstSharedPreferences();
        if (TextUtils.isEmpty(mFolderId) && mGoogleApiClient.isConnected()) {
            startPickerActivity();
            Log.e(TAG, "onPerformSync(): startPickerActivity");
            return;
        }
        new UploadToFolderTask(mFileTitle, mLocalFilePath, mFolderId, mGoogleApiClient, null);
        Log.e(TAG, "onPerformSync(): uploadingToFolder");
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());
        if (connectionResult.hasResolution()) {
            getContext().startActivity(SignInResolutionActivity.newStartIntent(getContext(), connectionResult,
                    mFileTitle, mLocalFilePath));
        } else {
            Log.e(TAG, "onConnectionFailed() no resolution " + connectionResult);
        }
    }

    private void initGoogleApiClient(Context context, String accountName) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
               // .setAccountName(accountName) // needs permission from debug app
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void startPickerActivity() {
        IntentSender folderPickerIntent = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{DriveFolder.MIME_TYPE})
                .build(mGoogleApiClient);
        getContext().startActivity(FolderPickerActivity.newStartIntent(getContext(), folderPickerIntent));
    }

    /**
     * If bundle passed null/empty values try to get from sp
     */
    private void validateAgainstSharedPreferences() {
        SharedPreferences sharedPreferences =
                getContext().getSharedPreferences(SyncHelper.SYNC_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        if (TextUtils.isEmpty(mFileTitle)) {
            mFileTitle = sharedPreferences.getString(SyncHelper.SYNC_FILE_TITLE, "");
        }

        if (TextUtils.isEmpty(mFolderId)) {
            mFolderId = sharedPreferences.getString(SyncHelper.SYNC_DRIVE_FOLDER_ID, "");
        }

        if (TextUtils.isEmpty(mLocalFilePath)) {
            mLocalFilePath = sharedPreferences.getString(SyncHelper.SYNC_LOCAL_FILE_PATH, "");
        }
    }
}