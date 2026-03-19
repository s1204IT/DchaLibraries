package com.android.contacts.group;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.mediatek.contacts.util.Log;

public class UpdateGroupMembersAsyncTask extends AsyncTask<Void, Void, Intent> {
    private final String mAccountName;
    private final String mAccountType;
    private final long[] mContactIds;
    private final Context mContext;
    private final String mDataSet;
    private final long mGroupId;
    private final int mType;

    public UpdateGroupMembersAsyncTask(int i, Context context, long[] jArr, long j, String str, String str2, String str3) {
        this.mContext = context;
        this.mType = i;
        this.mContactIds = jArr;
        this.mGroupId = j;
        this.mAccountName = str;
        this.mAccountType = str2;
        this.mDataSet = str3;
    }

    @Override
    protected Intent doInBackground(Void... voidArr) {
        long[] jArr;
        String str;
        long[] jArr2;
        long[] rawContactIds = getRawContactIds();
        Log.d("UpdateGroupMembersAsyncTask", "[doInBackground] rawContactIds count = " + rawContactIds.length + ", mType = " + this.mType + ", mGroupId = " + this.mGroupId + ", mAccountName = " + Log.anonymize(this.mAccountName) + ", mAccountType = " + this.mAccountType);
        if (rawContactIds.length == 0) {
            return null;
        }
        if (this.mType != 0) {
            if (this.mType != 1) {
                throw new IllegalStateException("Unrecognized type " + this.mType);
            }
            jArr = rawContactIds;
            str = GroupUtil.ACTION_REMOVE_FROM_GROUP;
            jArr2 = null;
        } else {
            jArr2 = rawContactIds;
            str = GroupUtil.ACTION_ADD_TO_GROUP;
            jArr = null;
        }
        return ContactSaveService.createGroupUpdateIntent(this.mContext, this.mGroupId, null, jArr2, jArr, PeopleActivity.class, str);
    }

    private long[] getRawContactIds() {
        Uri.Builder builderBuildUpon = ContactsContract.RawContacts.CONTENT_URI.buildUpon();
        if (this.mAccountName != null) {
            builderBuildUpon.appendQueryParameter("account_name", this.mAccountName);
            builderBuildUpon.appendQueryParameter("account_type", this.mAccountType);
        }
        if (this.mDataSet != null) {
            builderBuildUpon.appendQueryParameter("data_set", this.mDataSet);
        }
        Uri uriBuild = builderBuildUpon.build();
        String[] strArr = {"_id"};
        StringBuilder sb = new StringBuilder();
        sb.append("contact_id IN (");
        for (int i = 0; i < this.mContactIds.length; i++) {
            sb.append(String.valueOf(this.mContactIds[i]));
            if (i < this.mContactIds.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        Log.d("UpdateGroupMembersAsyncTask", "[getRawContactIds] selection=" + sb.toString());
        Cursor cursorQuery = this.mContext.getContentResolver().query(uriBuild, strArr, sb.toString(), null, null);
        long[] jArr = new long[cursorQuery.getCount()];
        int i2 = 0;
        while (cursorQuery.moveToNext()) {
            try {
                jArr[i2] = cursorQuery.getLong(0);
                i2++;
            } finally {
                cursorQuery.close();
            }
        }
        return jArr;
    }

    @Override
    protected void onPostExecute(Intent intent) {
        if (intent == null) {
            Toast.makeText(this.mContext, R.string.groupSavedErrorToast, 0).show();
        } else {
            this.mContext.startService(intent);
        }
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected int getType() {
        return this.mType;
    }

    protected long[] getContactIds() {
        return this.mContactIds;
    }

    protected long getGroupId() {
        return this.mGroupId;
    }

    protected String getAccountName() {
        return this.mAccountName;
    }

    protected String getAccountType() {
        return this.mAccountType;
    }

    protected String getDataSet() {
        return this.mDataSet;
    }

    protected boolean isTypeAdd() {
        return getType() == 0;
    }

    protected boolean isTypeRemove() {
        return getType() == 1;
    }
}
