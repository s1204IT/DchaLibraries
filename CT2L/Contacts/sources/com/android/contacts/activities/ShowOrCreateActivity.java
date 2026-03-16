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
import android.util.Log;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.util.NotifyingAsyncQueryHandler;

public final class ShowOrCreateActivity extends ContactsActivity implements NotifyingAsyncQueryHandler.AsyncQueryListener {
    private String mCreateDescrip;
    private Bundle mCreateExtras;
    private boolean mCreateForce;
    private NotifyingAsyncQueryHandler mQueryHandler;
    static final String[] PHONES_PROJECTION = {"_id", "lookup"};
    static final String[] CONTACTS_PROJECTION = {"contact_id", "lookup"};

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (this.mQueryHandler == null) {
            this.mQueryHandler = new NotifyingAsyncQueryHandler(this, this);
        } else {
            this.mQueryHandler.cancelOperation(42);
        }
        Intent intent = getIntent();
        Uri data = intent.getData();
        String scheme = null;
        String ssp = null;
        if (data != null) {
            scheme = data.getScheme();
            ssp = data.getSchemeSpecificPart();
        }
        this.mCreateExtras = new Bundle();
        Bundle originalExtras = intent.getExtras();
        if (originalExtras != null) {
            this.mCreateExtras.putAll(originalExtras);
        }
        this.mCreateDescrip = intent.getStringExtra("com.android.contacts.action.CREATE_DESCRIPTION");
        if (this.mCreateDescrip == null) {
            this.mCreateDescrip = ssp;
        }
        this.mCreateForce = intent.getBooleanExtra("com.android.contacts.action.FORCE_CREATE", false);
        if ("mailto".equals(scheme)) {
            this.mCreateExtras.putString("email", ssp);
            Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(ssp));
            this.mQueryHandler.startQuery(42, null, uri, CONTACTS_PROJECTION, null, null, null);
        } else if ("tel".equals(scheme)) {
            this.mCreateExtras.putString("phone", ssp);
            Uri uri2 = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, ssp);
            this.mQueryHandler.startQuery(42, null, uri2, PHONES_PROJECTION, null, null, null);
        } else {
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
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (cursor == null) {
            finish();
            return;
        }
        long contactId = -1;
        String lookupKey = null;
        try {
            int count = cursor.getCount();
            if (count == 1 && cursor.moveToFirst()) {
                contactId = cursor.getLong(0);
                lookupKey = cursor.getString(1);
            }
            if (count == 1 && contactId != -1) {
                Uri contactUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
                Intent viewIntent = new Intent("android.intent.action.VIEW", contactUri);
                startActivity(viewIntent);
                finish();
                return;
            }
            if (count > 1) {
                Intent listIntent = new Intent("android.intent.action.SEARCH");
                listIntent.setComponent(new ComponentName(this, (Class<?>) PeopleActivity.class));
                listIntent.putExtras(this.mCreateExtras);
                startActivity(listIntent);
                finish();
                return;
            }
            if (this.mCreateForce) {
                Intent createIntent = new Intent("android.intent.action.INSERT", ContactsContract.RawContacts.CONTENT_URI);
                createIntent.putExtras(this.mCreateExtras);
                createIntent.setType("vnd.android.cursor.dir/raw_contact");
                startActivity(createIntent);
                finish();
                return;
            }
            showDialog(1);
        } finally {
            cursor.close();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                Intent createIntent = new Intent("android.intent.action.INSERT_OR_EDIT");
                createIntent.putExtras(this.mCreateExtras);
                createIntent.setType("vnd.android.cursor.item/raw_contact");
                CharSequence message = getResources().getString(R.string.add_contact_dlg_message_fmt, this.mCreateDescrip);
                return new AlertDialog.Builder(this).setMessage(message).setPositiveButton(android.R.string.ok, new IntentClickListener(this, createIntent)).setNegativeButton(android.R.string.cancel, new IntentClickListener(this, null)).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ShowOrCreateActivity.this.finish();
                    }
                }).create();
            default:
                return super.onCreateDialog(id);
        }
    }

    private static class IntentClickListener implements DialogInterface.OnClickListener {
        private Intent mIntent;
        private Activity mParent;

        public IntentClickListener(Activity parent, Intent intent) {
            this.mParent = parent;
            this.mIntent = intent;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (this.mIntent != null) {
                this.mParent.startActivity(this.mIntent);
            }
            this.mParent.finish();
        }
    }
}
