package com.android.contacts.group;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.ContactsUtils;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.group.GroupMembersFragment;
import com.android.contacts.list.ContactsSectionIndexer;
import com.android.contacts.model.account.GoogleAccountType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class GroupUtil {
    public static final String ACTION_ADD_TO_GROUP = "addToGroup";
    public static final String ACTION_CREATE_GROUP = "createGroup";
    public static final String ACTION_DELETE_GROUP = "deleteGroup";
    public static final String ACTION_REMOVE_FROM_GROUP = "removeFromGroup";
    public static final String ACTION_SWITCH_GROUP = "switchGroup";
    public static final String ACTION_UPDATE_GROUP = "updateGroup";
    public static final String ALL_GROUPS_SELECTION = "account_type NOT NULL AND account_name NOT NULL AND deleted=0";
    public static final String DEFAULT_SELECTION = "account_type NOT NULL AND account_name NOT NULL AND deleted=0 AND auto_add=0 AND favorites=0";
    private static final Set<String> FFC_GROUPS = new HashSet(Arrays.asList("Friends", "Family", "Coworkers"));
    public static final int RESULT_SEND_TO_SELECTION = 100;

    private GroupUtil() {
    }

    public static GroupListItem getGroupListItem(Cursor cursor, int i) {
        if (cursor == null || cursor.isClosed() || !cursor.moveToPosition(i)) {
            return null;
        }
        boolean z = false;
        String string = cursor.getString(0);
        String string2 = cursor.getString(1);
        String string3 = cursor.getString(2);
        long j = cursor.getLong(3);
        String string4 = cursor.getString(4);
        int i2 = cursor.getInt(5);
        boolean z2 = cursor.getInt(6) == 1;
        String string5 = cursor.getString(7);
        int i3 = i - 1;
        if (i3 >= 0 && cursor.moveToPosition(i3)) {
            String string6 = cursor.getString(0);
            String string7 = cursor.getString(1);
            String string8 = cursor.getString(2);
            if (!TextUtils.equals(string, string6) || !TextUtils.equals(string2, string7) || !TextUtils.equals(string3, string8)) {
            }
        } else {
            z = true;
        }
        return new GroupListItem(string, string2, string3, j, string4, z, i2, z2, string5);
    }

    public static List<String> getSendToDataForIds(Context context, long[] jArr, String str) {
        String str2;
        ArrayList arrayList = new ArrayList();
        String strConvertArrayToString = convertArrayToString(jArr);
        if (ContactsUtils.SCHEME_MAILTO.equals(str)) {
            str2 = "mimetype='vnd.android.cursor.item/email_v2' AND _id IN (" + strConvertArrayToString + ")";
        } else {
            str2 = "mimetype='vnd.android.cursor.item/phone_v2' AND _id IN (" + strConvertArrayToString + ")";
        }
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, ContactsUtils.SCHEME_MAILTO.equals(str) ? GroupMembersFragment.Query.EMAIL_PROJECTION : GroupMembersFragment.Query.PHONE_PROJECTION, str2, null, null);
        if (cursorQuery == null) {
            return arrayList;
        }
        try {
            cursorQuery.moveToPosition(-1);
            while (cursorQuery.moveToNext()) {
                String string = cursorQuery.getString(4);
                if (!TextUtils.isEmpty(string)) {
                    arrayList.add(string);
                }
            }
            return arrayList;
        } finally {
            cursorQuery.close();
        }
    }

    public static void startSendToSelectionActivity(Fragment fragment, String str, String str2, String str3) {
        fragment.startActivityForResult(Intent.createChooser(new Intent("android.intent.action.SENDTO", Uri.fromParts(str2, str, null)), str3), 100);
    }

    public static Intent createSendToSelectionPickerIntent(Context context, long[] jArr, long[] jArr2, String str, String str2, int i) {
        String str3;
        Intent intent = new Intent(context, (Class<?>) ContactSelectionActivity.class);
        intent.setAction("com.android.contacts.action.ACTION_SELECT_ITEMS");
        if (ContactsUtils.SCHEME_MAILTO.equals(str)) {
            str3 = "vnd.android.cursor.dir/email_v2";
        } else {
            str3 = "vnd.android.cursor.dir/phone_v2";
        }
        intent.setType(str3);
        intent.putExtra("com.android.contacts.extra.SELECTION_ITEM_LIST", jArr);
        intent.putExtra("com.android.contacts.extra.SELECTION_DEFAULT_SELECTION", jArr2);
        intent.putExtra("com.android.contacts.extra.SELECTION_SEND_SCHEME", str);
        intent.putExtra("com.android.contacts.extra.SELECTION_SEND_TITLE", str2);
        intent.putExtra("com.android.contacts.extra.GROUP_ACCOUNT_SUBID", i);
        return intent;
    }

    public static Intent createPickMemberIntent(Context context, GroupMetaData groupMetaData, ArrayList<String> arrayList, int i) {
        Intent intent = new Intent(context, (Class<?>) ContactSelectionActivity.class);
        intent.setAction("android.intent.action.PICK");
        intent.setType("vnd.android.cursor.dir/group");
        intent.putExtra("com.android.contacts.extra.GROUP_ACCOUNT_NAME", groupMetaData.accountName);
        intent.putExtra("com.android.contacts.extra.GROUP_ACCOUNT_TYPE", groupMetaData.accountType);
        intent.putExtra("com.android.contacts.extra.GROUP_ACCOUNT_DATA_SET", groupMetaData.dataSet);
        intent.putExtra("com.android.contacts.extra.GROUP_CONTACT_IDS", arrayList);
        intent.putExtra("com.android.contacts.extra.GROUP_ACCOUNT_SUBID", i);
        return intent;
    }

    public static String convertArrayToString(long[] jArr) {
        if (jArr == null || jArr.length == 0) {
            return "";
        }
        return Arrays.toString(jArr).replace("[", "").replace("]", "");
    }

    public static long[] convertLongSetToLongArray(Set<Long> set) {
        Long[] lArr = (Long[]) set.toArray(new Long[set.size()]);
        long[] jArr = new long[lArr.length];
        for (int i = 0; i < lArr.length; i++) {
            jArr[i] = lArr[i].longValue();
        }
        return jArr;
    }

    public static long[] convertStringSetToLongArray(Set<String> set) {
        String[] strArr = (String[]) set.toArray(new String[set.size()]);
        long[] jArr = new long[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            try {
                jArr[i] = Long.parseLong(strArr[i]);
            } catch (NumberFormatException e) {
                jArr[i] = -1;
            }
        }
        return jArr;
    }

    public static boolean isEmptyFFCGroup(GroupListItem groupListItem) {
        return groupListItem.isReadOnly() && isSystemIdFFC(groupListItem.getSystemId()) && groupListItem.getMemberCount() <= 0;
    }

    private static boolean isSystemIdFFC(String str) {
        return !TextUtils.isEmpty(str) && FFC_GROUPS.contains(str);
    }

    public static boolean isGroupUri(Uri uri) {
        return uri != null && uri.toString().startsWith(ContactsContract.Groups.CONTENT_URI.toString());
    }

    public static String getGroupsSortOrder() {
        return "title COLLATE LOCALIZED ASC";
    }

    public static boolean needTrimming(int i, int[] iArr, int[] iArr2) {
        return iArr2.length > 0 && iArr.length > 0 && i <= iArr[iArr.length - 1] + iArr2[iArr2.length - 1];
    }

    public static void updateBundle(Bundle bundle, ContactsSectionIndexer contactsSectionIndexer, List<Integer> list, String[] strArr, int[] iArr) {
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            int sectionForPosition = contactsSectionIndexer.getSectionForPosition(it.next().intValue());
            if (sectionForPosition < iArr.length && sectionForPosition >= 0) {
                iArr[sectionForPosition] = iArr[sectionForPosition] - 1;
                if (iArr[sectionForPosition] == 0) {
                    strArr[sectionForPosition] = "";
                }
            }
        }
        bundle.putStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES", clearEmptyString(strArr));
        bundle.putIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS", clearZeros(iArr));
    }

    private static String[] clearEmptyString(String[] strArr) {
        ArrayList arrayList = new ArrayList();
        for (String str : strArr) {
            if (!TextUtils.isEmpty(str)) {
                arrayList.add(str);
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    private static int[] clearZeros(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        for (int i : iArr) {
            if (i > 0) {
                arrayList.add(Integer.valueOf(i));
            }
        }
        int[] iArr2 = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr2[i2] = ((Integer) arrayList.get(i2)).intValue();
        }
        return iArr2;
    }

    public static final class GroupsProjection {
        public final int accountName;
        public final int accountType;
        public final int autoAdd;
        public final int dataSet;
        public final int deleted;
        public final int favorites;
        public final int groupId;
        public final int isReadOnly;
        public final int summaryCount;
        public final int systemId;
        public final int title;

        public GroupsProjection(Cursor cursor) {
            this.groupId = cursor.getColumnIndex("_id");
            this.title = cursor.getColumnIndex("title");
            this.summaryCount = cursor.getColumnIndex("summ_count");
            this.systemId = cursor.getColumnIndex("system_id");
            this.accountName = cursor.getColumnIndex("account_name");
            this.accountType = cursor.getColumnIndex("account_type");
            this.dataSet = cursor.getColumnIndex("data_set");
            this.autoAdd = cursor.getColumnIndex("auto_add");
            this.favorites = cursor.getColumnIndex("favorites");
            this.isReadOnly = cursor.getColumnIndex("group_is_read_only");
            this.deleted = cursor.getColumnIndex("deleted");
        }

        public String getTitle(Cursor cursor) {
            return cursor.getString(this.title);
        }

        public boolean isEmptyFFCGroup(Cursor cursor) {
            if (this.accountType == -1 || this.isReadOnly == -1 || this.systemId == -1 || this.summaryCount == -1) {
                throw new IllegalArgumentException("Projection is missing required columns");
            }
            return GoogleAccountType.ACCOUNT_TYPE.equals(cursor.getString(this.accountType)) && cursor.getInt(this.isReadOnly) != 0 && GroupUtil.isSystemIdFFC(cursor.getString(this.systemId)) && cursor.getInt(this.summaryCount) <= 0;
        }
    }
}
