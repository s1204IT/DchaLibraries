package com.mediatek.contacts.util;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.FavoritesAndContactsLoader;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.util.AccountFilterUtil;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import java.util.ArrayList;
import java.util.List;

public class ContactsCommonListUtils {
    public static void setAccountTypeText(Context context, AccountType accountType, TextView textView, TextView textView2, ContactListFilter contactListFilter) {
        String accountDisplayNameByAccount = AccountFilterUtil.getAccountDisplayNameByAccount(contactListFilter.accountType, contactListFilter.accountName);
        if (TextUtils.isEmpty(accountDisplayNameByAccount)) {
            textView.setText(contactListFilter.accountName);
        } else {
            textView.setText(accountDisplayNameByAccount);
        }
        if (AccountWithDataSetEx.isLocalPhone(accountType.accountType)) {
            textView2.setVisibility(8);
            textView.setText(accountType.getDisplayLabel(context));
        }
    }

    public static void configureOnlyShowPhoneContactsSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        Log.d("ContactsCommonListUtils", "[configureOnlyShowPhoneContactsSelection] directoryId :" + j + ",filter : " + contactListFilter);
        if (contactListFilter != null && j == 0 && ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            StringBuilder sb = new StringBuilder();
            ArrayList arrayList = new ArrayList();
            sb.append("indicate_phone_or_sim_contact= ?");
            arrayList.add("-1");
            cursorLoader.setSelection(sb.toString());
            cursorLoader.setSelectionArgs((String[]) arrayList.toArray(new String[0]));
        }
    }

    public static void buildSelectionForFilterAccount(ContactListFilter contactListFilter, StringBuilder sb, List<String> list) {
        list.add(contactListFilter.accountType);
        list.add(contactListFilter.accountName);
        if (contactListFilter.dataSet != null) {
            sb.append(" AND data_set=? )");
            list.add(contactListFilter.dataSet);
        } else {
            sb.append(" AND data_set IS NULL )");
        }
        sb.append("))");
    }

    private static Cursor loadSDN(Context context, FavoritesAndContactsLoader favoritesAndContactsLoader) {
        Log.d("ContactsCommonListUtils", "[loadSDN]...");
        if (favoritesAndContactsLoader.getSelection() != null && favoritesAndContactsLoader.getSelection().indexOf("is_sdn_contact < 1") >= 0) {
            Uri uri = favoritesAndContactsLoader.getUri();
            String[] projection = favoritesAndContactsLoader.getProjection();
            Cursor cursorQuery = context.getContentResolver().query(uri, projection, favoritesAndContactsLoader.getSelection().replace("is_sdn_contact < 1", "is_sdn_contact = 1"), favoritesAndContactsLoader.getSelectionArgs(), favoritesAndContactsLoader.getSortOrder());
            if (cursorQuery == null) {
                Log.w("ContactsCommonListUtils", "[loadSDN]sdnCursor is null need to check");
                return null;
            }
            MatrixCursor matrixCursor = new MatrixCursor(projection);
            try {
                Object[] objArr = new Object[projection.length];
                while (cursorQuery.moveToNext()) {
                    for (int i = 0; i < objArr.length; i++) {
                        objArr[i] = cursorQuery.getString(i);
                    }
                    matrixCursor.addRow(objArr);
                }
                return matrixCursor;
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        Log.d("ContactsCommonListUtils", "[loadSDN] return null");
        return null;
    }

    public static int addCursorAndSetSelection(Context context, FavoritesAndContactsLoader favoritesAndContactsLoader, List<Cursor> list, int i) {
        String selection = favoritesAndContactsLoader.getSelection();
        Cursor cursorLoadSDN = loadSDN(context, favoritesAndContactsLoader);
        if (cursorLoadSDN != null) {
            i = cursorLoadSDN.getCount();
        }
        if (cursorLoadSDN != null) {
            list.add(cursorLoadSDN);
        }
        favoritesAndContactsLoader.setSelection(selection);
        return i;
    }

    public static ContactPhotoManager.DefaultImageRequest getDefaultImageRequest(Cursor cursor, String str, String str2, boolean z) {
        int i;
        ContactPhotoManager.DefaultImageRequest defaultImageRequest = new ContactPhotoManager.DefaultImageRequest(str, str2, z);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT && (i = cursor.getInt(cursor.getColumnIndexOrThrow("indicate_phone_or_sim_contact"))) > 0) {
            defaultImageRequest.subId = i;
            defaultImageRequest.photoId = getSdnPhotoId(cursor);
        }
        return defaultImageRequest;
    }

    private static long getSdnPhotoId(Cursor cursor) {
        if (cursor.getInt(cursor.getColumnIndexOrThrow("is_sdn_contact")) > 0) {
            return -14L;
        }
        return 0L;
    }

    public static boolean isSdnPhotoId(long j) {
        return j == -14;
    }
}
