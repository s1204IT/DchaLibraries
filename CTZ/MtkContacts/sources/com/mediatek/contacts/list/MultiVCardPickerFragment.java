package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import com.mediatek.contacts.util.Log;

public class MultiVCardPickerFragment extends MultiBasePickerFragment {
    private static final String[] LOOKUPPROJECT = {"lookup"};

    @Override
    public void onOptionAction() {
        long[] checkedItemIds = getCheckedItemIds();
        if (checkedItemIds == null) {
            Log.w("MultiVCardPickerFragment", "[onOptionAction]idArray is null!");
            return;
        }
        Uri lookupUriForEmail = getLookupUriForEmail("Multi_Contact", checkedItemIds);
        Log.d("MultiVCardPickerFragment", "[onOptionAction] The result uri is " + lookupUriForEmail);
        Intent intent = new Intent();
        Activity activity = getActivity();
        intent.putExtra("com.mediatek.contacts.list.pickcontactsresult", lookupUriForEmail);
        activity.setResult(-1, intent);
        activity.finish();
    }

    private Uri getLookupUriForEmail(String str, long[] jArr) {
        Uri uriWithAppendedPath;
        Log.d("MultiVCardPickerFragment", "[getLookupUriForEmail]type :" + str);
        Cursor cursorQuery = null;
        if ("Single_Contact".equals(str)) {
            uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, Long.toString(jArr[0]));
            cursorQuery = getActivity().getContentResolver().query(uriWithAppendedPath, LOOKUPPROJECT, null, null, null);
            if (cursorQuery != null && cursorQuery.moveToNext()) {
                Log.i("MultiVCardPickerFragment", "Single_Contact  cursor.getCount() is " + cursorQuery.getCount());
                uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, cursorQuery.getString(0));
            }
        } else if ("Multi_Contact".equals(str)) {
            StringBuilder sb = new StringBuilder("");
            for (long j : jArr) {
                if (j == jArr[jArr.length - 1]) {
                    sb.append(j);
                } else {
                    sb.append(j + ",");
                }
            }
            Cursor cursorQuery2 = getActivity().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, LOOKUPPROJECT, "_id in (" + sb.toString() + ")", null, null);
            if (cursorQuery2 == null) {
                return null;
            }
            Log.i("MultiVCardPickerFragment", "[getLookupUriForEmail]  cursor.getCount() is " + cursorQuery2.getCount());
            if (!cursorQuery2.moveToFirst()) {
                cursorQuery2.close();
                return null;
            }
            StringBuilder sb2 = new StringBuilder();
            int i = 0;
            while (!cursorQuery2.isAfterLast()) {
                if (i != 0) {
                    sb2.append(':');
                }
                sb2.append(cursorQuery2.getString(0));
                i++;
                cursorQuery2.moveToNext();
            }
            cursorQuery = cursorQuery2;
            uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(sb2.toString()));
        } else {
            uriWithAppendedPath = null;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return uriWithAppendedPath;
    }
}
