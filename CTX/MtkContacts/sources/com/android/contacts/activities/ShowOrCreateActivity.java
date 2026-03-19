package com.android.contacts.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.NotifyingAsyncQueryHandler;
import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.util.Log;

public final class ShowOrCreateActivity extends ContactsActivity implements NotifyingAsyncQueryHandler.AsyncQueryListener {
    private String mCreateDescrip;
    private Bundle mCreateExtras;
    private boolean mCreateForce;
    private NotifyingAsyncQueryHandler mQueryHandler;
    static final String[] PHONES_PROJECTION = {"_id", "lookup"};
    static final String[] CONTACTS_PROJECTION = {"contact_id", "lookup"};

    @Override
    protected void onCreate(Bundle bundle) {
        String schemeSpecificPart;
        super.onCreate(bundle);
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        if (this.mQueryHandler == null) {
            this.mQueryHandler = new NotifyingAsyncQueryHandler(this, this);
        } else {
            this.mQueryHandler.cancelOperation(42);
        }
        Intent intent = getIntent();
        Uri data = intent.getData();
        String scheme = null;
        if (data != null) {
            scheme = data.getScheme();
            schemeSpecificPart = data.getSchemeSpecificPart();
        } else {
            schemeSpecificPart = null;
        }
        if (TextUtils.isEmpty(schemeSpecificPart)) {
            Log.w("ShowOrCreateActivity", "Invalid intent:" + getIntent());
            finish();
            return;
        }
        this.mCreateExtras = new Bundle();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            this.mCreateExtras.putAll(extras);
        }
        this.mCreateDescrip = intent.getStringExtra("com.android.contacts.action.CREATE_DESCRIPTION");
        if (this.mCreateDescrip == null) {
            this.mCreateDescrip = schemeSpecificPart;
        }
        this.mCreateForce = intent.getBooleanExtra("com.android.contacts.action.FORCE_CREATE", false);
        if (ContactsUtils.SCHEME_MAILTO.equals(scheme)) {
            this.mCreateExtras.putString("email", schemeSpecificPart);
            this.mQueryHandler.startQuery(42, null, Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(schemeSpecificPart)), CONTACTS_PROJECTION, null, null, null);
        } else {
            if ("tel".equals(scheme)) {
                if (ActivitiesUtils.checkSimNumberValid(this, schemeSpecificPart)) {
                    return;
                }
                this.mCreateExtras.putString("phone", schemeSpecificPart);
                this.mQueryHandler.startQuery(42, null, Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, schemeSpecificPart), PHONES_PROJECTION, null, null, null);
                return;
            }
            Log.w("ShowOrCreateActivity", "Invalid intent:" + getIntent());
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mQueryHandler != null) {
            this.mQueryHandler.cancelOperation(42);
        }
    }

    @Override
    public void onQueryComplete(int i, Object obj, Cursor cursor) {
        long j;
        if (cursor == null) {
            finish();
            return;
        }
        String string = null;
        try {
            int count = cursor.getCount();
            if (count == 1 && cursor.moveToFirst()) {
                j = cursor.getLong(0);
                string = cursor.getString(1);
            } else {
                j = -1;
            }
            if (count == 1 && j != -1 && !TextUtils.isEmpty(string)) {
                ImplicitIntentsUtil.startActivityInApp(this, new Intent("android.intent.action.VIEW", ContactsContract.Contacts.getLookupUri(j, string)));
                finish();
                return;
            }
            if (count > 1) {
                Intent intent = new Intent("android.intent.action.SEARCH");
                intent.setComponent(new ComponentName(this, (Class<?>) PeopleActivity.class));
                intent.putExtras(this.mCreateExtras);
                startActivity(intent);
                finish();
                return;
            }
            if (this.mCreateForce) {
                Intent intent2 = new Intent("android.intent.action.INSERT", ContactsContract.RawContacts.CONTENT_URI);
                intent2.putExtras(this.mCreateExtras);
                intent2.setType("vnd.android.cursor.dir/raw_contact");
                ImplicitIntentsUtil.startActivityInApp(this, intent2);
                finish();
                return;
            }
            if (!isFinishing()) {
                Log.w("ShowOrCreateActivity", "show Dialog.");
                showDialog(1);
            } else {
                Log.w("ShowOrCreateActivity", "Activity is Finishing.");
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        if (i == 1) {
            Intent intent = new Intent("android.intent.action.INSERT_OR_EDIT");
            intent.putExtras(this.mCreateExtras);
            intent.setType("vnd.android.cursor.item/raw_contact");
            return new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.add_contact_dlg_message_fmt, this.mCreateDescrip)).setPositiveButton(android.R.string.ok, new IntentClickListener(this, intent)).setNegativeButton(android.R.string.cancel, new IntentClickListener(this, null)).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    ShowOrCreateActivity.this.finish();
                }
            }).create();
        }
        return super.onCreateDialog(i);
    }

    private static class IntentClickListener implements DialogInterface.OnClickListener {
        private Intent mIntent;
        private Activity mParent;

        public IntentClickListener(Activity activity, Intent intent) {
            this.mParent = activity;
            this.mIntent = intent;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (this.mIntent != null) {
                ImplicitIntentsUtil.startActivityInApp(this.mParent, this.mIntent);
            }
            this.mParent.finish();
        }
    }
}
