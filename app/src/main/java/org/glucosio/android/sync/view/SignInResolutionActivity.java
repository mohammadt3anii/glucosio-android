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

package org.glucosio.android.sync.view;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.glucosio.android.sync.SyncHelper;

/**
 * {@link AppCompatActivity} used to request permission for Google Drive account
 * to be called from the SyncAdapter only.
 */
public class SignInResolutionActivity extends AppCompatActivity {

    private static final String TAG = "GoogleResolution";
    private static final String EXTRA_CONNECTION_RESULT = "extraConnectionResult";
    private static final int RC_RESOLUTION = 9001;
    private static final String EXTRA_FILE_TITLE = "extraFileTitle";
    private static final String EXTRA_LOCAL_FILE_PATH = "extraLocalFilePath";

    private ConnectionResult mConnectionResult;
    private String mFileTitle;
    private String mLocalFilePath;

    public static Intent newStartIntent(Context context, ConnectionResult result, String fileTitle,
                                        String localFilePath) {

        Intent startIntent = new Intent(context, SignInResolutionActivity.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent.putExtra(EXTRA_CONNECTION_RESULT, result);
        startIntent.putExtra(EXTRA_FILE_TITLE, fileTitle);
        startIntent.putExtra(EXTRA_LOCAL_FILE_PATH, localFilePath);
        return startIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mConnectionResult = (ConnectionResult) getIntent().getExtras().get(EXTRA_CONNECTION_RESULT);
            mFileTitle = getIntent().getStringExtra(EXTRA_FILE_TITLE);
            mLocalFilePath = getIntent().getStringExtra(EXTRA_FILE_TITLE);
        }
        showResolutionDialog(mConnectionResult);
    }

    private void showResolutionDialog(ConnectionResult result) {
        GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
        try {
            result.startResolutionForResult(this, RC_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity " + e);
        }
    }

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_RESOLUTION && resultCode == RESULT_OK) {
            Log.d(TAG, "Resolution OK");
            SyncHelper.initializeSync(getApplicationContext(), mFileTitle, mLocalFilePath);
        }
        finish();
    }

}
