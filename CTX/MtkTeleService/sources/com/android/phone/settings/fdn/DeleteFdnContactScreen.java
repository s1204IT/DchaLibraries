package com.android.phone.settings.fdn;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;

public class DeleteFdnContactScreen extends Activity {
    private String mName;
    private String mNumber;
    private String mPin2;
    protected QueryHandler mQueryHandler;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private boolean mHasShownPin2Dialog = false;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        resolveIntent();
        if (!(bundle != null && true == bundle.getBoolean("state"))) {
            authenticatePin2();
        }
        getWindow().requestFeature(5);
        setContentView(R.layout.delete_fdn_contact_screen);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        log("onActivityResult");
        if (i == 100) {
            Bundle extras = intent != null ? intent.getExtras() : null;
            if (extras != null) {
                this.mPin2 = extras.getString("pin2");
                showStatus(getResources().getText(R.string.deleting_fdn_contact));
                deleteContact();
            } else {
                log("onActivityResult: CANCELLED");
                displayProgress(false);
                finish();
            }
        }
    }

    private void resolveIntent() {
        Intent intent = getIntent();
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, intent);
        this.mName = intent.getStringExtra("name");
        this.mNumber = intent.getStringExtra("number");
    }

    private void deleteContact() {
        StringBuilder sb = new StringBuilder();
        if (TextUtils.isEmpty(this.mName)) {
            sb.append("number='");
        } else {
            sb.append("tag='");
            sb.append(this.mName);
            sb.append("' AND number='");
        }
        sb.append(this.mNumber);
        sb.append("' AND pin2='");
        sb.append(this.mPin2);
        sb.append("'");
        Uri contentUri = FdnList.getContentUri(this.mSubscriptionInfoHelper);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.mQueryHandler.startDelete(0, null, contentUri, sb.toString(), null);
        displayProgress(true);
    }

    private void authenticatePin2() {
        Intent intent = new Intent();
        intent.setClass(this, GetPin2Screen.class);
        intent.setData(FdnList.getContentUri(this.mSubscriptionInfoHelper));
        intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mSubscriptionInfoHelper.getSubId());
        startActivityForResult(intent, 100);
        this.mHasShownPin2Dialog = true;
    }

    private void displayProgress(boolean z) {
        getWindow().setFeatureInt(5, z ? -1 : -2);
    }

    private void showStatus(CharSequence charSequence) {
        if (charSequence != null) {
            Toast.makeText(this, charSequence, 0).show();
        }
    }

    private void handleResult(boolean z) {
        if (z) {
            log("handleResult: success!");
            showStatus(getResources().getText(R.string.fdn_contact_deleted));
        } else {
            log("handleResult: failed!");
            showStatus(getResources().getText(R.string.pin2_invalid));
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DeleteFdnContactScreen.this.finish();
            }
        }, 2000L);
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
        }

        @Override
        protected void onInsertComplete(int i, Object obj, Uri uri) {
        }

        @Override
        protected void onUpdateComplete(int i, Object obj, int i2) {
        }

        @Override
        protected void onDeleteComplete(int i, Object obj, int i2) {
            DeleteFdnContactScreen.this.log("onDeleteComplete");
            DeleteFdnContactScreen.this.displayProgress(false);
            DeleteFdnContactScreen.this.handleResult(i2 > 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("state", this.mHasShownPin2Dialog);
    }

    private void log(String str) {
        Log.d(PhoneGlobals.LOG_TAG, "[DeleteFdnContact] " + str);
    }
}
