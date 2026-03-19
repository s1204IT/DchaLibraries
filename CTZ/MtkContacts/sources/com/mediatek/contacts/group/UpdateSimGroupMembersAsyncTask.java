package com.mediatek.contacts.group;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.group.UpdateGroupMembersAsyncTask;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public class UpdateSimGroupMembersAsyncTask extends UpdateGroupMembersAsyncTask {
    private String mGroupName;
    private int mSubId;

    public UpdateSimGroupMembersAsyncTask(int i, Context context, long[] jArr, long j, String str, String str2, String str3, String str4, int i2) {
        super(i, context, jArr, j, str, str2, str3);
        this.mSubId = SubInfoUtils.getInvalidSubId();
        this.mGroupName = str4;
        this.mSubId = i2;
    }

    private class Member {
        private final int mIndexInSim;
        private final long mRawContactId;

        public Member(long j, int i) {
            this.mRawContactId = j;
            this.mIndexInSim = i;
        }
    }

    @Override
    protected Intent doInBackground(Void... voidArr) {
        String str;
        int[] iArr;
        long[] jArr;
        long[] jArr2;
        int[] iArr2;
        List<Member> members = getMembers();
        Log.d("UpdateSimGroupMembersAsyncTask", "[doInBackground] members count = " + members.size() + ", mType = " + getType() + ", mGroupId = " + getGroupId() + ", mGroupName = " + Log.anonymize(this.mGroupName) + ", mSubId = " + this.mSubId + ", mAccountName = " + Log.anonymize(getAccountName()) + ", mAccountType = " + getAccountType());
        if (members.isEmpty()) {
            return null;
        }
        long[] rawContactIdArray = getRawContactIdArray(members);
        int[] indexInSimArray = getIndexInSimArray(members);
        if (isTypeAdd()) {
            str = GroupUtil.ACTION_ADD_TO_GROUP;
            iArr2 = indexInSimArray;
            jArr2 = rawContactIdArray;
            jArr = null;
            iArr = null;
        } else if (isTypeRemove()) {
            str = GroupUtil.ACTION_REMOVE_FROM_GROUP;
            iArr = indexInSimArray;
            jArr = rawContactIdArray;
            jArr2 = null;
            iArr2 = null;
        } else {
            throw new IllegalStateException("Unrecognized type " + getType());
        }
        return SimGroupUtils.createGroupUpdateIntentForIcc(getContext(), getGroupId(), null, jArr2, jArr, PeopleActivity.class, str, this.mGroupName, this.mSubId, iArr2, iArr, new AccountWithDataSet(getAccountName(), getAccountType(), getDataSet()));
    }

    private List<Member> getMembers() {
        Uri.Builder builderBuildUpon = ContactsContract.RawContacts.CONTENT_URI.buildUpon();
        if (getAccountName() != null) {
            builderBuildUpon.appendQueryParameter("account_name", getAccountName());
            builderBuildUpon.appendQueryParameter("account_type", getAccountType());
        }
        if (getDataSet() != null) {
            builderBuildUpon.appendQueryParameter("data_set", getDataSet());
        }
        Uri uriBuild = builderBuildUpon.build();
        String[] strArr = {"_id", "index_in_sim"};
        StringBuilder sb = new StringBuilder();
        long[] contactIds = getContactIds();
        String[] strArr2 = new String[contactIds.length];
        for (int i = 0; i < contactIds.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("contact_id");
            sb.append("=?");
            strArr2[i] = Long.toString(contactIds[i]);
        }
        Cursor cursorQuery = getContext().getContentResolver().query(uriBuild, strArr, sb.toString(), strArr2, null, null);
        ArrayList arrayList = new ArrayList(cursorQuery.getCount());
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(new Member(cursorQuery.getLong(0), cursorQuery.getInt(1)));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        Log.d("UpdateSimGroupMembersAsyncTask", "[getMembers] members count is " + arrayList.size());
        return arrayList;
    }

    private static long[] getRawContactIdArray(List<Member> list) {
        int size = list.size();
        long[] jArr = new long[size];
        for (int i = 0; i < size; i++) {
            jArr[i] = list.get(i).mRawContactId;
        }
        return jArr;
    }

    private static int[] getIndexInSimArray(List<Member> list) {
        int size = list.size();
        int[] iArr = new int[size];
        for (int i = 0; i < size; i++) {
            iArr[i] = list.get(i).mIndexInSim;
        }
        return iArr;
    }
}
