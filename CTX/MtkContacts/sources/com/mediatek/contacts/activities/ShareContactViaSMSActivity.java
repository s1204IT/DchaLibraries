package com.mediatek.contacts.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShareContactViaSMSActivity extends Activity {
    static final String[] CONTACTS_PROJECTION = {"_id", "display_name", "display_name_alt", "sort_key", "display_name"};
    private String mAction;
    private Uri mDataUri;
    Intent mIntent;
    String mLookUpUris;
    private ProgressDialog mProgressDialog;
    private SearchContactThread mSearchContactThread;
    private int mSingleContactId = -1;
    private boolean mIsUserProfile = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mIntent = getIntent();
        this.mAction = this.mIntent.getAction();
        String stringExtra = this.mIntent.getStringExtra("contactId");
        String stringExtra2 = this.mIntent.getStringExtra("userProfile");
        if (stringExtra2 != null && "true".equals(stringExtra2)) {
            this.mIsUserProfile = true;
        }
        if (stringExtra != null && !"".equals(stringExtra)) {
            this.mSingleContactId = Integer.parseInt(stringExtra);
        }
        Uri uri = (Uri) this.mIntent.getExtra("android.intent.extra.STREAM");
        this.mLookUpUris = null;
        if (uri != null) {
            this.mLookUpUris = uri.getLastPathSegment();
        }
        if ((uri != null && uri.toString().startsWith("file") && this.mSingleContactId == -1 && !this.mIsUserProfile) || TextUtils.isEmpty(this.mLookUpUris)) {
            Log.i("ShareContactViaSMSActivity", "[onCreate]send file vis sms error,return.");
            Toast.makeText(getApplicationContext(), getString(R.string.send_file_sms_error), 0).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i("ShareContactViaSMSActivity", "[onBackPressed]In onBackPressed.");
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ("android.intent.action.SEND".equals(this.mAction) && this.mIntent.hasExtra("android.intent.extra.STREAM")) {
            createSearchContactThread();
            showProgressDialog();
        }
    }

    private void createSearchContactThread() {
        if (this.mSearchContactThread == null) {
            this.mSearchContactThread = new SearchContactThread();
        }
    }

    private void showProgressDialog() {
        if (this.mProgressDialog == null) {
            this.mProgressDialog = ProgressDialog.show(this, getString(R.string.please_wait), getString(R.string.please_wait), true, false);
            this.mProgressDialog.setOnCancelListener(this.mSearchContactThread);
            this.mSearchContactThread.start();
        }
    }

    private void shareViaSMS(String str) {
        long[] jArr;
        Cursor cursorQuery;
        StringBuilder sb = new StringBuilder();
        String vCardString = "";
        Log.i("ShareContactViaSMSActivity", "[shareViaSMS]mUserProfile = " + this.mIsUserProfile);
        if (this.mIsUserProfile) {
            Cursor cursorQuery2 = getContentResolver().query(ContactsContract.Profile.CONTENT_URI.buildUpon().appendPath("data").build(), new String[]{"contact_id", "mimetype", "data1"}, null, null, null);
            if (cursorQuery2 != null) {
                vCardString = getVCardString(cursorQuery2);
                cursorQuery2.close();
            }
        } else {
            if (this.mSingleContactId == -1) {
                String[] strArrSplit = str.split(":");
                StringBuilder sb2 = new StringBuilder("lookup in (");
                int i = 0;
                for (String str2 : strArrSplit) {
                    sb2.append("'" + str2 + "'");
                    if (i != strArrSplit.length - 1) {
                        sb2.append(",");
                    }
                    i++;
                }
                sb2.append(")");
                Cursor cursorQuery3 = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, CONTACTS_PROJECTION, sb2.toString(), null, "sort_key");
                if (cursorQuery3 != null) {
                    String string = null;
                    int i2 = 0;
                    while (cursorQuery3.moveToNext()) {
                        if (cursorQuery3 != null) {
                            string = cursorQuery3.getString(0);
                        }
                        int i3 = i2 + 1;
                        if (i2 != 0) {
                            sb.append("," + string);
                        } else {
                            sb.append(string);
                        }
                        i2 = i3;
                    }
                    cursorQuery3.close();
                }
            } else {
                sb.append(Integer.toString(this.mSingleContactId));
            }
            Log.i("ShareContactViaSMSActivity", "[shareViaSMS]contactsID:" + sb.toString());
            String string2 = sb.toString();
            if (string2 != null && !"".equals(string2)) {
                String[] strArrSplit2 = string2.split(",");
                Log.d("ShareContactViaSMSActivity", "[shareViaSMS]vCardConIds.length:" + strArrSplit2.length + ",contactsIDStr:" + string2);
                jArr = new long[strArrSplit2.length];
                for (int i4 = 0; i4 < strArrSplit2.length; i4++) {
                    try {
                        jArr[i4] = Long.parseLong(strArrSplit2[i4]);
                    } catch (NumberFormatException e) {
                        Log.e("ShareContactViaSMSActivity", "[shareViaSMS]NumberFormatException:" + e.toString());
                        jArr = null;
                    }
                }
                if (jArr != null) {
                    Log.d("ShareContactViaSMSActivity", "[shareViaSMS]contactsIds.length() = " + jArr.length);
                    StringBuilder sb3 = new StringBuilder("");
                    while (i < r2) {
                    }
                    String str3 = "contact_id in (" + sb3.toString() + ")";
                    Log.i("ShareContactViaSMSActivity", "[shareViaSMS]selection = " + str3);
                    Uri uri = Uri.parse("content://com.android.contacts/data");
                    Log.i("ShareContactViaSMSActivity", "[shareViaSMS]Before query to build contact name  string ");
                    cursorQuery = getContentResolver().query(uri, new String[]{"contact_id", "mimetype", "data1"}, str3, null, "sort_key , contact_id");
                    Log.i("ShareContactViaSMSActivity", "[shareViaSMS]After query to build contact name and  string ");
                    if (cursorQuery != null) {
                    }
                }
            } else {
                jArr = null;
                if (jArr != null && jArr.length > 0) {
                    Log.d("ShareContactViaSMSActivity", "[shareViaSMS]contactsIds.length() = " + jArr.length);
                    StringBuilder sb32 = new StringBuilder("");
                    for (long j : jArr) {
                        if (j == jArr[jArr.length - 1]) {
                            sb32.append(j);
                        } else {
                            sb32.append(j + ",");
                        }
                    }
                    String str32 = "contact_id in (" + sb32.toString() + ")";
                    Log.i("ShareContactViaSMSActivity", "[shareViaSMS]selection = " + str32);
                    Uri uri2 = Uri.parse("content://com.android.contacts/data");
                    Log.i("ShareContactViaSMSActivity", "[shareViaSMS]Before query to build contact name  string ");
                    cursorQuery = getContentResolver().query(uri2, new String[]{"contact_id", "mimetype", "data1"}, str32, null, "sort_key , contact_id");
                    Log.i("ShareContactViaSMSActivity", "[shareViaSMS]After query to build contact name and  string ");
                    if (cursorQuery != null) {
                        Log.i("ShareContactViaSMSActivity", "[shareViaSMS]Before getVCardString ");
                        vCardString = getVCardString(cursorQuery);
                        Log.i("ShareContactViaSMSActivity", "[shareViaSMS]After getVCardString ");
                        cursorQuery.close();
                    }
                }
            }
        }
        Log.i("ShareContactViaSMSActivity", "[shareViaSMS]textVCard is : \n" + vCardString);
        Intent intent = new Intent("android.intent.action.SENDTO", Uri.fromParts("sms", "", null));
        intent.putExtra("sms_body", vCardString);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e2) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ShareContactViaSMSActivity.this.getApplicationContext(), ShareContactViaSMSActivity.this.getString(R.string.quickcontact_missing_app), 0).show();
                }
            });
            Log.e("ShareContactViaSMSActivity", "ActivityNotFoundException :" + e2.toString());
        }
        finish();
    }

    private String getVCardString(Cursor cursor) {
        TextVCardContact textVCardContact = new TextVCardContact();
        StringBuilder sb = new StringBuilder();
        long j = 0;
        while (cursor.moveToNext()) {
            long j2 = cursor.getLong(0);
            String string = cursor.getString(1);
            if (j == 0) {
                j = j2;
            }
            if (j2 != j) {
                sb.append(textVCardContact.toString());
                textVCardContact.reset();
                j = j2;
            }
            if ("vnd.android.cursor.item/name".equals(string)) {
                textVCardContact.mName = cursor.getString(2);
            }
            if ("vnd.android.cursor.item/phone_v2".equals(string)) {
                textVCardContact.mNumbers.add(cursor.getString(2));
            }
            if ("vnd.android.cursor.item/email_v2".equals(string)) {
                textVCardContact.mOmails.add(cursor.getString(2));
            }
            if ("vnd.android.cursor.item/organization".equals(string)) {
                textVCardContact.mOrganizations.add(cursor.getString(2));
            }
            if (cursor.isLast()) {
                sb.append(textVCardContact.toString());
            }
        }
        return sb.toString();
    }

    private class SearchContactThread extends Thread implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        public SearchContactThread() {
        }

        @Override
        public void run() {
            String type = ShareContactViaSMSActivity.this.mIntent.getType();
            ShareContactViaSMSActivity.this.mDataUri = (Uri) ShareContactViaSMSActivity.this.mIntent.getParcelableExtra("android.intent.extra.STREAM");
            Log.i("ShareContactViaSMSActivity", "[run]dataUri is :" + Log.anonymize(ShareContactViaSMSActivity.this.mDataUri) + ",type:" + type);
            if (ShareContactViaSMSActivity.this.mDataUri != null && type != null) {
                ShareContactViaSMSActivity.this.shareViaSMS(ShareContactViaSMSActivity.this.mLookUpUris);
            }
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            ShareContactViaSMSActivity.this.finish();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                ShareContactViaSMSActivity.this.finish();
            }
        }
    }

    private class TextVCardContact {
        protected String mName;
        protected List<String> mNumbers;
        protected List<String> mOmails;
        protected List<String> mOrganizations;

        private TextVCardContact() {
            this.mName = "";
            this.mNumbers = new ArrayList();
            this.mOmails = new ArrayList();
            this.mOrganizations = new ArrayList();
        }

        protected void reset() {
            this.mName = "";
            this.mNumbers.clear();
            this.mOmails.clear();
            this.mOrganizations.clear();
        }

        public String toString() {
            String str = "";
            if (this.mName != null && !this.mName.equals("")) {
                str = "" + ShareContactViaSMSActivity.this.getString(R.string.nameLabelsGroup) + ": " + this.mName + "\n";
            }
            if (!this.mNumbers.isEmpty()) {
                if (this.mNumbers.size() > 1) {
                    Iterator<String> it = this.mNumbers.iterator();
                    int i = 1;
                    while (it.hasNext()) {
                        str = str + "Tel" + i + ": " + it.next() + "\n";
                        i++;
                    }
                } else {
                    str = str + "Tel: " + this.mNumbers.get(0) + "\n";
                }
            }
            if (!this.mOmails.isEmpty()) {
                if (this.mOmails.size() > 1) {
                    Iterator<String> it2 = this.mOmails.iterator();
                    int i2 = 1;
                    while (it2.hasNext()) {
                        str = str + ShareContactViaSMSActivity.this.getString(R.string.email_other) + i2 + ": " + it2.next() + "\n";
                        i2++;
                    }
                } else {
                    str = str + ShareContactViaSMSActivity.this.getString(R.string.email_other) + ": " + this.mOmails.get(0) + "\n";
                }
            }
            if (!this.mOrganizations.isEmpty()) {
                if (this.mOrganizations.size() > 1) {
                    Iterator<String> it3 = this.mOrganizations.iterator();
                    int i3 = 1;
                    while (it3.hasNext()) {
                        str = str + ShareContactViaSMSActivity.this.getString(R.string.organizationLabelsGroup) + i3 + ": " + it3.next() + "\n";
                        i3++;
                    }
                    return str;
                }
                return str + ShareContactViaSMSActivity.this.getString(R.string.organizationLabelsGroup) + ": " + this.mOrganizations.get(0) + "\n";
            }
            return str;
        }
    }
}
