package com.android.mtkex.chips;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.mtkex.chips.BaseRecipientAdapter;
import com.android.mtkex.chips.Queries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipientAlternatesAdapter extends CursorAdapter {
    private static boolean DEBUG_LOG = true;
    private static boolean piLoggable = true ^ "user".equals(SystemProperties.get("ro.build.type", "user"));
    private OnCheckedItemChangedListener mCheckedItemChangedListener;
    private int mCheckedItemPosition;
    private final long mCurrentId;
    private final LayoutInflater mLayoutInflater;
    private Queries.Query mQuery;

    interface OnCheckedItemChangedListener {
        void onCheckedItemChanged(int i);
    }

    public interface RecipientMatchCallback {
        void matchesFound(Map<String, RecipientEntry> map);

        void matchesNotFound(Set<String> set);
    }

    private static boolean isEmailType(String str) {
        if (str != null && str.contains("@")) {
            return true;
        }
        return false;
    }

    public static void getMatchingRecipients(HashSet<String> hashSet, Context context, BaseRecipientAdapter baseRecipientAdapter, ArrayList<String> arrayList, Account account, RecipientMatchCallback recipientMatchCallback) throws Throwable {
        getMatchingRecipients(hashSet, context, baseRecipientAdapter, arrayList, 0, account, recipientMatchCallback);
    }

    public static void getMatchingRecipients(HashSet<String> hashSet, Context context, BaseRecipientAdapter baseRecipientAdapter, ArrayList<String> arrayList, int i, Account account, RecipientMatchCallback recipientMatchCallback) throws Throwable {
        Log.d("RecipAlternates", "[getMatchingRecipients] Start");
        int iMin = Math.min(100, arrayList.size());
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        int[] iArr = new int[iMin];
        splitAddressToEmailAndPhone(arrayList, arrayList2, arrayList3, iArr);
        HashMap map = new HashMap();
        Cursor cursorQueryAddressData = queryAddressData(context, arrayList2, 0);
        Cursor cursorQueryAddressData2 = queryAddressData(context, arrayList3, 1);
        if (cursorQueryAddressData != null && cursorQueryAddressData2 == null) {
            fillRecipientEntries(hashSet, cursorQueryAddressData, map, recipientMatchCallback);
            processMatchesNotFound(context, baseRecipientAdapter, account, Queries.EMAIL, arrayList, map, recipientMatchCallback);
            return;
        }
        if (cursorQueryAddressData == null && cursorQueryAddressData2 != null) {
            fillRecipientEntries(hashSet, cursorQueryAddressData2, map, recipientMatchCallback);
            processMatchesNotFound(context, baseRecipientAdapter, account, Queries.PHONE, arrayList, map, recipientMatchCallback);
            return;
        }
        if (cursorQueryAddressData != null && cursorQueryAddressData2 != null) {
            HashMap map2 = new HashMap();
            HashMap map3 = new HashMap();
            fillRecipientEntriesCompound(hashSet, cursorQueryAddressData, cursorQueryAddressData2, map, map2, map3, iMin, iArr, recipientMatchCallback);
            processMatchesNotFound(context, baseRecipientAdapter, account, Queries.EMAIL, arrayList2, map2, recipientMatchCallback);
            printSensitiveDebugLog("RecipAlternates", "phoneRecipientEntries.keySet() = " + map3.keySet() + " phoneAddressesList = " + arrayList3 + " emailRecipientEntries.keySet() = " + map2.keySet() + " emailAddressesList = " + arrayList2);
            processMatchesNotFound(context, baseRecipientAdapter, account, Queries.PHONE, arrayList3, map3, recipientMatchCallback);
        }
    }

    private static void splitAddressToEmailAndPhone(ArrayList<String> arrayList, ArrayList<String> arrayList2, ArrayList<String> arrayList3, int[] iArr) {
        int iMin = Math.min(100, arrayList.size());
        for (int i = 0; i < iMin; i++) {
            if (isEmailType(arrayList.get(i))) {
                arrayList2.add(arrayList.get(i));
                iArr[i] = 1;
            } else {
                arrayList3.add(arrayList.get(i));
                iArr[i] = 2;
            }
        }
    }

    private static Cursor queryAddressData(Context context, ArrayList<String> arrayList, int i) {
        Queries.Query query;
        Cursor cursorQuery;
        int iMin = Math.min(100, arrayList.size());
        StringBuilder sb = new StringBuilder();
        String[] strArr = new String[iMin];
        if (i == 0) {
            query = Queries.EMAIL;
        } else {
            query = Queries.PHONE;
        }
        String str = "";
        if (i != 0) {
            String str2 = "";
            for (int i2 = 0; i2 < iMin; i2++) {
                String strReplaceAll = arrayList.get(i2).replaceAll("([, ]+$)|([; ]+$)|([\"]+$)", "");
                if (!MTKRecipientEditTextView.PHONE_EX.matcher(strReplaceAll).matches()) {
                    Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(strReplaceAll);
                    if (rfc822TokenArr.length > 0) {
                        strReplaceAll = rfc822TokenArr[0].getAddress();
                    }
                }
                str2 = str2 + "\"" + strReplaceAll + "\"";
                sb.append("?");
                if (i2 < iMin - 1) {
                    str2 = str2 + ",";
                    sb.append(",");
                }
            }
            str = str2;
        } else {
            for (int i3 = 0; i3 < iMin; i3++) {
                Rfc822Token[] rfc822TokenArr2 = Rfc822Tokenizer.tokenize(arrayList.get(i3));
                strArr[i3] = rfc822TokenArr2.length > 0 ? rfc822TokenArr2[0].getAddress() : arrayList.get(i3);
                sb.append("?");
                if (i3 < iMin - 1) {
                    sb.append(",");
                }
            }
        }
        if (Log.isLoggable("RecipAlternates", 3)) {
            printSensitiveDebugLog("RecipAlternates", "Doing reverse lookup for " + strArr.toString());
        }
        if (arrayList.size() <= 0) {
            cursorQuery = null;
        } else if (i == 0) {
            String str3 = query.getProjection()[1] + " IN (" + sb.toString() + ")";
            printSensitiveDebugLog("RecipAlternates", "[queryAddressData] selection: " + str3);
            try {
                cursorQuery = context.getContentResolver().query(query.getContentUri(), query.getProjection(), str3, strArr, null);
                printSensitiveDebugLog("RecipAlternates", "addresses = " + strArr);
            } catch (IllegalArgumentException e) {
                Log.e("RecipAlternates", "QUERY_TYPE_EMAIL meet IllegalArgumentException " + e);
                return null;
            }
        } else {
            String str4 = query.getProjection()[1] + " IN (" + str + ")";
            printSensitiveDebugLog("RecipAlternates", "[queryAddressData] selection: " + str4);
            try {
                cursorQuery = context.getContentResolver().query(query.getContentUri(), query.getProjection(), str4, null, "display_name DESC");
            } catch (IllegalArgumentException e2) {
                Log.e("RecipAlternates", "QUERY_TYPE_PHONE meet IllegalArgumentException " + e2);
                return null;
            }
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("[queryAddressData] cursor count: ");
        sb2.append(cursorQuery != null ? Integer.valueOf(cursorQuery.getCount()) : "null");
        Log.d("RecipAlternates", sb2.toString());
        return cursorQuery;
    }

    private static void fillRecipientEntries(HashSet<String> hashSet, Cursor cursor, HashMap<String, RecipientEntry> map, RecipientMatchCallback recipientMatchCallback) {
        Log.d("RecipAlternates", "[fillRecipientEntries] start");
        try {
            if (cursor.moveToFirst()) {
                do {
                    if (hashSet.contains(cursor.getString(1))) {
                        String string = cursor.getString(1);
                        map.put(string, RecipientEntry.constructTopLevelEntry(cursor.getString(0), cursor.getInt(7), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), true, false));
                        printSensitiveDebugLog("RecipAlternates", "Received reverse look up information for " + string + " RESULTS:  NAME : " + cursor.getString(0) + " CONTACT ID : " + cursor.getLong(4) + " ADDRESS :" + cursor.getString(1));
                    }
                } while (cursor.moveToNext());
            }
            recipientMatchCallback.matchesFound(map);
        } finally {
            cursor.close();
        }
    }

    private static void fillRecipientEntriesCompound(HashSet<String> hashSet, Cursor cursor, Cursor cursor2, HashMap<String, RecipientEntry> map, HashMap<String, RecipientEntry> map2, HashMap<String, RecipientEntry> map3, int i, int[] iArr, RecipientMatchCallback recipientMatchCallback) {
        int i2;
        RecipientEntry recipientEntry;
        RecipientEntry recipientEntry2;
        Log.d("RecipAlternates", "[fillRecipientEntriesCompound] start");
        try {
            cursor.moveToFirst();
            cursor2.moveToFirst();
            int i3 = 0;
            int i4 = 1;
            int i5 = 0;
            boolean zMoveToNext = true;
            boolean zMoveToNext2 = true;
            while (i5 < i) {
                Log.d("RecipAlternates", "fillRecipientEntriesCompound addressesSize = " + i);
                if (iArr[i5] == i4 && zMoveToNext && cursor.getCount() != 0) {
                    if (!hashSet.contains(cursor.getString(i4))) {
                        zMoveToNext = cursor.moveToNext();
                    } else {
                        if (iArr[i5] != i4) {
                        }
                        if (zMoveToNext2) {
                        }
                        i2 = i4;
                        i5++;
                        i4 = i2;
                        i3 = 0;
                    }
                } else if (zMoveToNext2 && cursor2.getCount() != 0 && !hashSet.contains(cursor2.getString(i4))) {
                    zMoveToNext2 = cursor2.moveToNext();
                } else if (iArr[i5] != i4 && zMoveToNext && cursor.getCount() != 0) {
                    String string = cursor.getString(i4);
                    if (map.containsKey(string) && (recipientEntry2 = map.get(string)) != null && recipientEntry2.getDisplayName().equals(cursor.getString(i3))) {
                        zMoveToNext = cursor.moveToNext();
                    } else {
                        RecipientEntry recipientEntryConstructTopLevelEntry = RecipientEntry.constructTopLevelEntry(cursor.getString(i3), cursor.getInt(7), cursor.getString(i4), cursor.getInt(2), cursor.getString(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), true, false);
                        map.put(string, recipientEntryConstructTopLevelEntry);
                        map2.put(string, recipientEntryConstructTopLevelEntry);
                        printSensitiveDebugLog("RecipAlternates", "Received reverse look up information for " + string + " RESULTS:  NAME : " + cursor.getString(i3) + " CONTACT ID : " + cursor.getLong(4) + " ADDRESS :" + cursor.getString(i4));
                        zMoveToNext = cursor.moveToNext();
                        i2 = i4;
                        i5++;
                        i4 = i2;
                        i3 = 0;
                    }
                } else if (zMoveToNext2 || cursor2.getCount() == 0) {
                    i2 = i4;
                    i5++;
                    i4 = i2;
                    i3 = 0;
                } else {
                    String string2 = cursor2.getString(i4);
                    if (map.containsKey(string2) && (recipientEntry = map.get(string2)) != null && recipientEntry.getDisplayName().equals(cursor2.getString(i3))) {
                        zMoveToNext2 = cursor2.moveToNext();
                    } else {
                        RecipientEntry recipientEntryConstructTopLevelEntry2 = RecipientEntry.constructTopLevelEntry(cursor2.getString(i3), cursor2.getInt(7), cursor2.getString(i4), cursor2.getInt(2), cursor2.getString(3), cursor2.getLong(4), cursor2.getLong(5), cursor2.getString(6), true, false);
                        map.put(string2, recipientEntryConstructTopLevelEntry2);
                        map3.put(string2, recipientEntryConstructTopLevelEntry2);
                        StringBuilder sb = new StringBuilder();
                        sb.append("Received reverse look up information for ");
                        sb.append(string2);
                        sb.append(" RESULTS:  NAME : ");
                        sb.append(cursor2.getString(i3));
                        sb.append(" CONTACT ID : ");
                        sb.append(cursor2.getLong(4));
                        sb.append(" ADDRESS :");
                        i2 = 1;
                        sb.append(cursor2.getString(1));
                        printSensitiveDebugLog("RecipAlternates", sb.toString());
                        zMoveToNext2 = cursor2.moveToNext();
                        i5++;
                        i4 = i2;
                        i3 = 0;
                    }
                }
            }
            printSensitiveDebugLog("RecipAlternates", "recipientEntries.keySet() " + map.keySet());
            recipientMatchCallback.matchesFound(map);
        } finally {
            cursor.close();
            cursor2.close();
        }
    }

    private static void processMatchesNotFound(Context context, BaseRecipientAdapter baseRecipientAdapter, Account account, Queries.Query query, ArrayList<String> arrayList, HashMap<String, RecipientEntry> map, RecipientMatchCallback recipientMatchCallback) throws Throwable {
        Cursor cursorQuery;
        List<BaseRecipientAdapter.DirectorySearchParams> list;
        int i;
        Cursor cursor;
        String str;
        Iterator it;
        Map<String, RecipientEntry> map2;
        RecipientEntry recipientEntryByPhoneNumber;
        RecipientEntry recipientEntryByPhoneNumber2;
        Map<String, RecipientEntry> matchingRecipients;
        Log.d("RecipAlternates", "[processMatchesNotFound] start");
        Set<String> hashSet = new HashSet<>();
        if (map.size() < arrayList.size()) {
            try {
                cursorQuery = context.getContentResolver().query(BaseRecipientAdapter.DirectoryListQuery.URI, BaseRecipientAdapter.DirectoryListQuery.PROJECTION, null, null, null);
                if (cursorQuery == null) {
                    list = null;
                } else {
                    try {
                        list = BaseRecipientAdapter.setupOtherDirectories(context, cursorQuery, account);
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                HashSet hashSet2 = new HashSet();
                Iterator<String> it2 = arrayList.iterator();
                while (true) {
                    i = 0;
                    if (!it2.hasNext()) {
                        break;
                    }
                    String strReplaceAll = it2.next().replaceAll("([, ]+$)|([; ]+$)", "");
                    if (!Patterns.PHONE.matcher(strReplaceAll).matches()) {
                        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(strReplaceAll);
                        if (rfc822TokenArr.length > 0) {
                            strReplaceAll = rfc822TokenArr[0].getAddress();
                        }
                    }
                    printSensitiveDebugLog("RecipAlternates", "query address after parsed = " + strReplaceAll);
                    if (!map.containsKey(strReplaceAll)) {
                        hashSet2.add(strReplaceAll);
                    }
                }
                Log.d("RecipAlternates", "matchesNotFound = " + hashSet);
                hashSet.addAll(hashSet2);
                if (list != null) {
                    Map<String, RecipientEntry> map3 = new HashMap<>();
                    Iterator it3 = hashSet2.iterator();
                    Cursor cursor2 = null;
                    while (it3.hasNext()) {
                        String str2 = (String) it3.next();
                        if (query == Queries.PHONE && (recipientEntryByPhoneNumber2 = getRecipientEntryByPhoneNumber(context, str2)) != null) {
                            map3.put(str2, recipientEntryByPhoneNumber2);
                            hashSet.remove(str2);
                        } else {
                            Cursor cursor3 = cursor2;
                            int i2 = i;
                            while (true) {
                                if (i2 < list.size()) {
                                    try {
                                        cursor = cursor3;
                                        str = str2;
                                        it = it3;
                                        map2 = map3;
                                        try {
                                            Cursor cursorDoQuery = doQuery(str2, 1, Long.valueOf(list.get(i2).directoryId), account, context.getContentResolver(), query);
                                            if (cursorDoQuery == null || cursorDoQuery.getCount() != 0) {
                                                cursor3 = cursorDoQuery;
                                            } else {
                                                cursorDoQuery.close();
                                                cursor3 = null;
                                            }
                                            if (cursor3 == null) {
                                                i2++;
                                                str2 = str;
                                                map3 = map2;
                                                it3 = it;
                                            } else {
                                                cursor = cursor3;
                                                break;
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            if (cursor != null && cursor.getCount() == 0) {
                                                cursor.close();
                                                cursor = null;
                                            }
                                            if (cursor == null) {
                                                throw th;
                                            }
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        cursor = cursor3;
                                        str = str2;
                                        it = it3;
                                        map2 = map3;
                                    }
                                } else {
                                    cursor = cursor3;
                                    str = str2;
                                    it = it3;
                                    map2 = map3;
                                    break;
                                }
                            }
                            if (cursor != null) {
                                try {
                                    Map<? extends String, ? extends RecipientEntry> mapProcessContactEntries = processContactEntries(cursor, true);
                                    Iterator<? extends String> it4 = mapProcessContactEntries.keySet().iterator();
                                    while (it4.hasNext()) {
                                        hashSet.remove(it4.next());
                                    }
                                    map2.putAll(mapProcessContactEntries);
                                    Log.d("RecipAlternates", "entries.size(): " + mapProcessContactEntries.size());
                                } finally {
                                    cursor.close();
                                }
                            } else if (query == Queries.PHONE && (recipientEntryByPhoneNumber = getRecipientEntryByPhoneNumber(context, str)) != null) {
                                map2.put(str, recipientEntryByPhoneNumber);
                                hashSet.remove(str);
                            }
                            map3 = map2;
                            cursor2 = cursor;
                            it3 = it;
                            i = 0;
                        }
                    }
                    recipientMatchCallback.matchesFound(map3);
                }
            } catch (Throwable th4) {
                th = th4;
                cursorQuery = null;
            }
        }
        if (baseRecipientAdapter != null && (matchingRecipients = baseRecipientAdapter.getMatchingRecipients(hashSet)) != null && matchingRecipients.size() > 0) {
            recipientMatchCallback.matchesFound(matchingRecipients);
            Iterator<String> it5 = matchingRecipients.keySet().iterator();
            while (it5.hasNext()) {
                hashSet.remove(it5.next());
            }
        }
        recipientMatchCallback.matchesNotFound(hashSet);
    }

    private static HashMap<String, RecipientEntry> processContactEntries(Cursor cursor, Boolean bool) {
        HashMap<String, RecipientEntry> map = new HashMap<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String string = cursor.getString(1);
                map.put(string, getBetterRecipient(map.get(string), RecipientEntry.constructTopLevelEntry(cursor.getString(0), cursor.getInt(7), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), true, bool.booleanValue())));
                printSensitiveDebugLog("RecipAlternates", "Received reverse look up information for " + string + " RESULTS:  NAME : " + cursor.getString(0) + " CONTACT ID : " + cursor.getLong(4) + " ADDRESS :" + cursor.getString(1));
            } while (cursor.moveToNext());
        }
        return map;
    }

    static RecipientEntry getBetterRecipient(RecipientEntry recipientEntry, RecipientEntry recipientEntry2) {
        if (recipientEntry2 == null) {
            return recipientEntry;
        }
        if (recipientEntry == null) {
            return recipientEntry2;
        }
        if (!RecipientEntry.isCreatedRecipient(recipientEntry.getContactId()) && RecipientEntry.isCreatedRecipient(recipientEntry2.getContactId())) {
            return recipientEntry;
        }
        if (!RecipientEntry.isCreatedRecipient(recipientEntry2.getContactId()) && RecipientEntry.isCreatedRecipient(recipientEntry.getContactId())) {
            return recipientEntry2;
        }
        if (!TextUtils.isEmpty(recipientEntry.getDisplayName()) && TextUtils.isEmpty(recipientEntry2.getDisplayName())) {
            return recipientEntry;
        }
        if (!TextUtils.isEmpty(recipientEntry2.getDisplayName()) && TextUtils.isEmpty(recipientEntry.getDisplayName())) {
            return recipientEntry2;
        }
        if (!TextUtils.equals(recipientEntry.getDisplayName(), recipientEntry.getDestination()) && TextUtils.equals(recipientEntry2.getDisplayName(), recipientEntry2.getDestination())) {
            return recipientEntry;
        }
        if (!TextUtils.equals(recipientEntry2.getDisplayName(), recipientEntry2.getDestination()) && TextUtils.equals(recipientEntry.getDisplayName(), recipientEntry.getDestination())) {
            return recipientEntry2;
        }
        if ((recipientEntry.getPhotoThumbnailUri() != null || recipientEntry.getPhotoBytes() != null) && recipientEntry2.getPhotoThumbnailUri() == null && recipientEntry2.getPhotoBytes() == null) {
            return recipientEntry;
        }
        if ((recipientEntry2.getPhotoThumbnailUri() != null || recipientEntry2.getPhotoBytes() != null) && recipientEntry.getPhotoThumbnailUri() == null && recipientEntry.getPhotoBytes() == null) {
            return recipientEntry2;
        }
        return recipientEntry2;
    }

    private static Cursor doQuery(CharSequence charSequence, int i, Long l, Account account, ContentResolver contentResolver, Queries.Query query) {
        Uri.Builder builderAppendQueryParameter = query.getContentFilterUri().buildUpon().appendPath(charSequence.toString()).appendQueryParameter("limit", String.valueOf(i + 5));
        if (l != null) {
            builderAppendQueryParameter.appendQueryParameter("directory", String.valueOf(l));
        }
        if (account != null) {
            builderAppendQueryParameter.appendQueryParameter("name_for_primary_account", account.name);
            builderAppendQueryParameter.appendQueryParameter("type_for_primary_account", account.type);
        }
        return contentResolver.query(builderAppendQueryParameter.build(), query.getProjection(), null, null, null);
    }

    public RecipientAlternatesAdapter(Context context, long j, long j2, int i, OnCheckedItemChangedListener onCheckedItemChangedListener) {
        super(context, getCursorForConstruction(context, j, i), 0);
        this.mCheckedItemPosition = -1;
        Log.d("RecipAlternates", "[RecipientAlternatesAdapter] queryMode: " + i + " mCurrentId = " + j2);
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mCurrentId = j2;
        this.mCheckedItemChangedListener = onCheckedItemChangedListener;
        if (i == 0) {
            this.mQuery = Queries.EMAIL;
            return;
        }
        if (i == 1) {
            this.mQuery = Queries.PHONE;
            return;
        }
        this.mQuery = Queries.EMAIL;
        Log.e("RecipAlternates", "Unsupported query type: " + i);
    }

    private static Cursor getCursorForConstruction(Context context, long j, int i) {
        Cursor cursorQuery;
        if (i == 0) {
            cursorQuery = context.getContentResolver().query(Queries.EMAIL.getContentUri(), Queries.EMAIL.getProjection(), Queries.EMAIL.getProjection()[4] + " =?", new String[]{String.valueOf(j)}, null);
        } else {
            cursorQuery = context.getContentResolver().query(Queries.PHONE.getContentUri(), Queries.PHONE.getProjection(), Queries.PHONE.getProjection()[4] + " =?", new String[]{String.valueOf(j)}, null);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[getCursorForConstruction] cursor count: ");
        sb.append(cursorQuery != null ? Integer.valueOf(cursorQuery.getCount()) : "null");
        Log.d("RecipAlternates", sb.toString());
        Cursor cursorRemoveDuplicateDestinations = removeDuplicateDestinations(cursorQuery);
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return cursorRemoveDuplicateDestinations;
    }

    static Cursor removeDuplicateDestinations(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        MatrixCursor matrixCursor = new MatrixCursor(cursor.getColumnNames(), cursor.getCount());
        HashSet hashSet = new HashSet();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String string = cursor.getString(1);
            if (!hashSet.contains(string)) {
                hashSet.add(string);
                matrixCursor.addRow(new Object[]{cursor.getString(0), cursor.getString(1), Integer.valueOf(cursor.getInt(2)), cursor.getString(3), Long.valueOf(cursor.getLong(4)), Long.valueOf(cursor.getLong(5)), cursor.getString(6), Integer.valueOf(cursor.getInt(7))});
            }
        }
        return matrixCursor;
    }

    @Override
    public long getItemId(int i) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(i)) {
            cursor.getLong(5);
            return -1L;
        }
        return -1L;
    }

    public RecipientEntry getRecipientEntry(int i) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(i);
        return RecipientEntry.constructTopLevelEntry(cursor.getString(0), cursor.getInt(7), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), true, false);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(i);
        if (view == null) {
            view = newView();
        }
        Log.d("RecipAlternates", "getView cursor.getLong(Queries.Query.DATA_ID) " + cursor.getLong(5) + " ; mCurrentId " + this.mCurrentId + " position = " + i);
        if (cursor.getLong(5) == this.mCurrentId) {
            this.mCheckedItemPosition = i;
            if (this.mCheckedItemChangedListener != null) {
                Log.d("RecipAlternates", " getView call onCheckedItemChanged position = " + i);
                this.mCheckedItemChangedListener.onCheckedItemChanged(this.mCheckedItemPosition);
            }
        }
        bindView(view, view.getContext(), cursor);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int position = cursor.getPosition();
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        RecipientEntry recipientEntry = getRecipientEntry(position);
        if (position == 0) {
            textView.setText(cursor.getString(0));
            textView.setVisibility(0);
            imageView.setImageURI(recipientEntry.getPhotoThumbnailUri());
            imageView.setVisibility(0);
        } else {
            textView.setVisibility(8);
            imageView.setVisibility(8);
        }
        ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(1));
        TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
        if (textView2 != null) {
            textView2.setText(this.mQuery.getTypeLabel(context.getResources(), cursor.getInt(2), cursor.getString(3)).toString().toUpperCase());
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return newView();
    }

    private View newView() {
        return this.mLayoutInflater.inflate(R.layout.chips_recipient_dropdown_item, (ViewGroup) null);
    }

    public RecipientAlternatesAdapter(Context context, long j, long j2, int i, OnCheckedItemChangedListener onCheckedItemChangedListener, boolean z) {
        super(context, getCursorForConstruction(context, j, i, z), 0);
        this.mCheckedItemPosition = -1;
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mCurrentId = j2;
        Log.d("RecipAlternates", "RecipientAlternatesAdapter mCurrentId = " + this.mCurrentId);
        this.mCheckedItemChangedListener = onCheckedItemChangedListener;
        if (i == 0) {
            this.mQuery = Queries.EMAIL;
            return;
        }
        if (i == 1) {
            this.mQuery = Queries.PHONE;
            return;
        }
        this.mQuery = Queries.EMAIL;
        Log.e("RecipAlternates", "Unsupported query type: " + i);
    }

    private static Cursor getCursorForConstruction(Context context, long j, int i, boolean z) {
        Cursor mergeCursor;
        if (!z) {
            mergeCursor = context.getContentResolver().query(Queries.PHONE.getContentUri(), Queries.PHONE.getProjection(), Queries.PHONE.getProjection()[4] + " =?", new String[]{String.valueOf(j)}, null);
        } else {
            mergeCursor = new MergeCursor(new Cursor[]{context.getContentResolver().query(Queries.PHONE.getContentUri(), Queries.PHONE.getProjection(), Queries.PHONE.getProjection()[4] + " =?", new String[]{String.valueOf(j)}, null), context.getContentResolver().query(Queries.EMAIL.getContentUri(), Queries.EMAIL.getProjection(), Queries.EMAIL.getProjection()[4] + " =?", new String[]{String.valueOf(j)}, null)});
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[getCursorForConstruction] cursor count: ");
        sb.append(mergeCursor != null ? Integer.valueOf(mergeCursor.getCount()) : "null");
        Log.d("RecipAlternates", sb.toString());
        Cursor cursorRemoveDuplicateDestinations = removeDuplicateDestinations(mergeCursor);
        if (mergeCursor != null) {
            mergeCursor.close();
        }
        return cursorRemoveDuplicateDestinations;
    }

    public static RecipientEntry getRecipientEntryByPhoneNumber(Context context, String str) {
        Cursor cursorQuery;
        long j;
        printSensitiveDebugLog("RecipAlternates", "[getRecipientEntryByPhoneNumber] phoneNumber: " + str);
        RecipientEntry recipientEntryConstructTopLevelEntry = null;
        if (str == null || TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArr = {"_id", "contact_id", "data1", "data4", "display_name"};
        String strNormalizeNumber = PhoneNumberUtils.normalizeNumber(str);
        try {
            cursorQuery = context.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, strNormalizeNumber), strArr, null, null, null);
        } catch (IllegalArgumentException e) {
            Log.e("RecipAlternates", "getRecipientEntryByPhoneNumber meet IllegalArgumentException " + e);
            e.printStackTrace();
            cursorQuery = null;
        }
        if (cursorQuery == null) {
            Log.d("RecipAlternates", "[getRecipientEntryByPhoneNumber] cursorNormalize is null");
            return null;
        }
        if (cursorQuery.moveToFirst()) {
            do {
                j = cursorQuery.getLong(1);
                if (Log.isLoggable("RecipAlternates", 3)) {
                    printSensitiveDebugLog("RecipAlternates", "[getRecipientEntryByPhoneNumber] Query ID for " + str + " RESULTS:  NAME : " + cursorQuery.getString(4) + " CONTACT ID : " + cursorQuery.getLong(1) + " ADDRESS :" + cursorQuery.getString(2));
                }
            } while (cursorQuery.moveToNext());
        } else {
            j = -1;
        }
        cursorQuery.close();
        if (j == -1) {
            return null;
        }
        Cursor cursorQuery2 = context.getContentResolver().query(Queries.PHONE.getContentUri(), Queries.PHONE.getProjection(), Queries.PHONE.getProjection()[4] + " IN (" + String.valueOf(j) + ")", null, null);
        if (cursorQuery2 == null) {
            return null;
        }
        if (cursorQuery2.moveToFirst()) {
            while (true) {
                if (Log.isLoggable("RecipAlternates", 3)) {
                    printSensitiveDebugLog("RecipAlternates", "[getRecipientEntryByPhoneNumber] Query detail for " + str + " RESULTS:  NAME : " + cursorQuery2.getString(0) + " CONTACT ID : " + cursorQuery2.getLong(4) + " ADDRESS :" + cursorQuery2.getString(1));
                }
                if (PhoneNumberUtils.compare(PhoneNumberUtils.normalizeNumber(cursorQuery2.getString(1)), strNormalizeNumber)) {
                    recipientEntryConstructTopLevelEntry = RecipientEntry.constructTopLevelEntry(cursorQuery2.getString(0), cursorQuery2.getInt(7), cursorQuery2.getString(1), cursorQuery2.getInt(2), cursorQuery2.getString(3), cursorQuery2.getLong(4), cursorQuery2.getLong(5), cursorQuery2.getString(6), true, false);
                    break;
                }
                if (!cursorQuery2.moveToNext()) {
                    break;
                }
            }
        }
        Log.d("RecipAlternates", "[getRecipientEntryByPhoneNumber] cursor count: " + cursorQuery2.getCount());
        cursorQuery2.close();
        return recipientEntryConstructTopLevelEntry;
    }

    public static List<RecipientEntry> getRecipientEntryByContactID(Context context, long j, boolean z) {
        Cursor cursorForConstruction = getCursorForConstruction(context, j, -1, z);
        ArrayList arrayList = new ArrayList();
        if (cursorForConstruction != null) {
            try {
                if (cursorForConstruction.moveToFirst()) {
                    do {
                        arrayList.add(RecipientEntry.constructTopLevelEntry(cursorForConstruction.getString(0), cursorForConstruction.getInt(7), cursorForConstruction.getString(1), cursorForConstruction.getInt(2), cursorForConstruction.getString(3), cursorForConstruction.getLong(4), cursorForConstruction.getLong(5), cursorForConstruction.getString(6), true, false));
                    } while (cursorForConstruction.moveToNext());
                }
            } catch (Throwable th) {
                if (cursorForConstruction != null) {
                    cursorForConstruction.close();
                }
                throw th;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[getRecipientEntryByPhoneNumber] cursor count: ");
        sb.append(cursorForConstruction != null ? Integer.valueOf(cursorForConstruction.getCount()) : "null");
        Log.d("RecipAlternates", sb.toString());
        if (cursorForConstruction != null) {
            cursorForConstruction.close();
        }
        return arrayList;
    }

    private static void printSensitiveDebugLog(String str, String str2) {
        if (piLoggable) {
            Log.d(str, str2);
        }
    }
}
