package com.android.contacts.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.contacts.ContactsUtils;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.GoogleAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.dataitem.PhoneDataItem;
import com.android.contacts.model.dataitem.StructuredNameDataItem;
import com.android.contacts.util.CommonDateUtils;
import com.android.contacts.util.DateUtils;
import com.android.contacts.util.NameConverter;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.aassne.SimAasEditor;
import com.mediatek.contacts.util.Log;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class RawContactModifier {
    private static final String COLUMN_FOR_LABEL = "data3";
    private static final String COLUMN_FOR_TYPE = "data2";
    private static final boolean DEBUG = false;
    private static final int FREQUENCY_TOTAL = Integer.MIN_VALUE;
    private static final int TYPE_CUSTOM = 0;
    private static final String TAG = RawContactModifier.class.getSimpleName();
    private static final Set<String> sGenericMimeTypesWithTypeSupport = new HashSet(Arrays.asList("vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/im", "vnd.android.cursor.item/nickname", "vnd.android.cursor.item/website", "vnd.android.cursor.item/relation", "vnd.android.cursor.item/sip_address"));
    private static final Set<String> sGenericMimeTypesWithoutTypeSupport = new HashSet(Arrays.asList("vnd.android.cursor.item/organization", "vnd.android.cursor.item/note", "vnd.android.cursor.item/photo", "vnd.android.cursor.item/group_membership"));

    public static boolean canInsert(RawContactDelta rawContactDelta, DataKind dataKind) {
        return hasValidTypes(rawContactDelta, dataKind) && (dataKind.typeOverallMax == -1 || rawContactDelta.getMimeEntriesCount(dataKind.mimeType, true) < dataKind.typeOverallMax);
    }

    public static boolean hasValidTypes(RawContactDelta rawContactDelta, DataKind dataKind) {
        return !hasEditTypes(dataKind) || getValidTypes(rawContactDelta, dataKind, null, true, null, true).size() > 0;
    }

    public static ValuesDelta ensureKindExists(RawContactDelta rawContactDelta, AccountType accountType, String str) {
        DataKind kindForMimetype = accountType.getKindForMimetype(str);
        boolean z = rawContactDelta.getMimeEntriesCount(str, true) > 0;
        if (kindForMimetype != null) {
            if (z) {
                return rawContactDelta.getMimeEntries(str).get(0);
            }
            ValuesDelta valuesDeltaInsertChild = insertChild(rawContactDelta, kindForMimetype);
            if (kindForMimetype.mimeType.equals("vnd.android.cursor.item/photo")) {
                valuesDeltaInsertChild.setFromTemplate(true);
            }
            return valuesDeltaInsertChild;
        }
        return null;
    }

    public static ArrayList<AccountType.EditType> getValidTypes(RawContactDelta rawContactDelta, DataKind dataKind, AccountType.EditType editType, boolean z, SparseIntArray sparseIntArray, boolean z2) {
        boolean z3;
        ArrayList<AccountType.EditType> arrayList = new ArrayList<>();
        if (!hasEditTypes(dataKind)) {
            return arrayList;
        }
        if (sparseIntArray == null) {
            sparseIntArray = getTypeFrequencies(rawContactDelta, dataKind);
        }
        if (z2) {
            z3 = dataKind.typeOverallMax == -1 || sparseIntArray.get(FREQUENCY_TOTAL) <= dataKind.typeOverallMax;
        }
        for (AccountType.EditType editType2 : dataKind.typeList) {
            boolean z4 = editType2.specificMax == -1 || sparseIntArray.get(editType2.rawValue) < editType2.specificMax;
            boolean z5 = z || !editType2.secondary;
            if (editType2.equals(editType) || (z3 && z4 && z5)) {
                arrayList.add(editType2);
            }
        }
        return arrayList;
    }

    private static SparseIntArray getTypeFrequencies(RawContactDelta rawContactDelta, DataKind dataKind) {
        SparseIntArray sparseIntArray = new SparseIntArray();
        ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(dataKind.mimeType);
        if (mimeEntries == null) {
            return sparseIntArray;
        }
        int i = 0;
        for (ValuesDelta valuesDelta : mimeEntries) {
            if (valuesDelta.isVisible()) {
                i++;
                AccountType.EditType currentType = getCurrentType(valuesDelta, dataKind);
                if (currentType != null) {
                    sparseIntArray.put(currentType.rawValue, sparseIntArray.get(currentType.rawValue) + 1);
                }
            }
        }
        sparseIntArray.put(FREQUENCY_TOTAL, i);
        return sparseIntArray;
    }

    public static boolean hasEditTypes(DataKind dataKind) {
        return (dataKind == null || dataKind.typeList == null || dataKind.typeList.size() <= 0) ? false : true;
    }

    public static AccountType.EditType getCurrentType(ValuesDelta valuesDelta, DataKind dataKind) {
        Long asLong = valuesDelta.getAsLong(dataKind.typeColumn);
        if (asLong == null) {
            return null;
        }
        return GlobalEnv.getSimAasEditor().getCurrentType(valuesDelta, dataKind, asLong.intValue());
    }

    public static AccountType.EditType getCurrentType(ContentValues contentValues, DataKind dataKind) {
        Integer asInteger;
        if (dataKind.typeColumn == null || (asInteger = contentValues.getAsInteger(dataKind.typeColumn)) == null) {
            return null;
        }
        return getType(dataKind, asInteger.intValue());
    }

    public static AccountType.EditType getCurrentType(Cursor cursor, DataKind dataKind) {
        int columnIndex;
        if (dataKind.typeColumn == null || (columnIndex = cursor.getColumnIndex(dataKind.typeColumn)) == -1) {
            return null;
        }
        return getType(dataKind, cursor.getInt(columnIndex));
    }

    public static AccountType.EditType getType(DataKind dataKind, int i) {
        for (AccountType.EditType editType : dataKind.typeList) {
            if (editType.rawValue == i) {
                return editType;
            }
        }
        return null;
    }

    public static int getTypePrecedence(DataKind dataKind, int i) {
        for (int i2 = 0; i2 < dataKind.typeList.size(); i2++) {
            if (dataKind.typeList.get(i2).rawValue == i) {
                return i2;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static AccountType.EditType getBestValidType(RawContactDelta rawContactDelta, DataKind dataKind, boolean z, int i) {
        if (dataKind == null || dataKind.typeColumn == null) {
            return null;
        }
        SparseIntArray typeFrequencies = getTypeFrequencies(rawContactDelta, dataKind);
        ArrayList<AccountType.EditType> validTypes = getValidTypes(rawContactDelta, dataKind, null, z, typeFrequencies, true);
        if (validTypes.size() == 0) {
            return null;
        }
        AccountType.EditType editType = validTypes.get(validTypes.size() - 1);
        Iterator<AccountType.EditType> it = validTypes.iterator();
        while (it.hasNext()) {
            AccountType.EditType next = it.next();
            int i2 = typeFrequencies.get(next.rawValue);
            if (i == next.rawValue) {
                return next;
            }
            if (i2 > 0) {
                it.remove();
            }
        }
        if (validTypes.size() > 0) {
            return validTypes.get(0);
        }
        return editType;
    }

    public static ValuesDelta insertChild(RawContactDelta rawContactDelta, DataKind dataKind) {
        if (dataKind == null) {
            return null;
        }
        AccountType.EditType bestValidType = getBestValidType(rawContactDelta, dataKind, false, FREQUENCY_TOTAL);
        if (bestValidType == null) {
            bestValidType = getBestValidType(rawContactDelta, dataKind, true, FREQUENCY_TOTAL);
        }
        return insertChild(rawContactDelta, dataKind, bestValidType);
    }

    public static ValuesDelta insertChild(RawContactDelta rawContactDelta, DataKind dataKind, AccountType.EditType editType) {
        if (dataKind == null) {
            return null;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("mimetype", dataKind.mimeType);
        if (dataKind.defaultValues != null) {
            contentValues.putAll(dataKind.defaultValues);
        }
        if (dataKind.typeColumn != null && editType != null) {
            contentValues.put(dataKind.typeColumn, Integer.valueOf(editType.rawValue));
        }
        ValuesDelta valuesDeltaFromAfter = ValuesDelta.fromAfter(contentValues);
        rawContactDelta.addEntry(valuesDeltaFromAfter);
        return valuesDeltaFromAfter;
    }

    public static void trimEmpty(RawContactDeltaList rawContactDeltaList, AccountTypeManager accountTypeManager) {
        for (RawContactDelta rawContactDelta : rawContactDeltaList) {
            ValuesDelta values = rawContactDelta.getValues();
            trimEmpty(rawContactDelta, accountTypeManager.getAccountType(values.getAsString("account_type"), values.getAsString("data_set")));
        }
    }

    public static boolean hasChanges(RawContactDeltaList rawContactDeltaList, AccountTypeManager accountTypeManager) {
        return hasChanges(rawContactDeltaList, accountTypeManager, (Set<String>) null);
    }

    public static boolean hasChanges(RawContactDeltaList rawContactDeltaList, AccountTypeManager accountTypeManager, Set<String> set) {
        if (rawContactDeltaList.isMarkedForSplitting() || rawContactDeltaList.isMarkedForJoining()) {
            return true;
        }
        for (RawContactDelta rawContactDelta : rawContactDeltaList) {
            ValuesDelta values = rawContactDelta.getValues();
            if (hasChanges(rawContactDelta, accountTypeManager.getAccountType(values.getAsString("account_type"), values.getAsString("data_set")), set)) {
                return true;
            }
        }
        return false;
    }

    public static void trimEmpty(RawContactDelta rawContactDelta, AccountType accountType) {
        boolean z = false;
        for (DataKind dataKind : accountType.getSortedDataKinds()) {
            ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(dataKind.mimeType);
            if (mimeEntries != null) {
                for (ValuesDelta valuesDelta : mimeEntries) {
                    if (!(valuesDelta.isInsert() || valuesDelta.isUpdate())) {
                        z = true;
                    } else {
                        boolean z2 = TextUtils.equals("vnd.android.cursor.item/photo", dataKind.mimeType) && TextUtils.equals(GoogleAccountType.ACCOUNT_TYPE, rawContactDelta.getValues().getAsString("account_type"));
                        if (isEmpty(valuesDelta, dataKind) && !z2) {
                            valuesDelta.markDeleted();
                        } else if (!valuesDelta.isFromTemplate()) {
                            z = true;
                        }
                    }
                }
            }
        }
        if (!z) {
            rawContactDelta.markDeleted();
        }
    }

    private static boolean hasChanges(RawContactDelta rawContactDelta, AccountType accountType, Set<String> set) {
        for (DataKind dataKind : accountType.getSortedDataKinds()) {
            String str = dataKind.mimeType;
            if (set == null || !set.contains(str)) {
                ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(str);
                if (mimeEntries == null) {
                    continue;
                } else {
                    for (ValuesDelta valuesDelta : mimeEntries) {
                        if ((valuesDelta.isInsert() && !isEmpty(valuesDelta, dataKind)) || valuesDelta.isUpdate() || valuesDelta.isDelete()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isEmpty(ValuesDelta valuesDelta, DataKind dataKind) {
        if ("vnd.android.cursor.item/photo".equals(dataKind.mimeType)) {
            return valuesDelta.isInsert() && valuesDelta.getAsByteArray("data15") == null;
        }
        if (dataKind.fieldList == null) {
            return true;
        }
        Iterator<AccountType.EditField> it = dataKind.fieldList.iterator();
        while (it.hasNext()) {
            if (ContactsUtils.isGraphic(valuesDelta.getAsString(it.next().column))) {
                return false;
            }
        }
        return true;
    }

    protected static boolean areEqual(ValuesDelta valuesDelta, ContentValues contentValues, DataKind dataKind) {
        if (dataKind.fieldList == null) {
            return false;
        }
        for (AccountType.EditField editField : dataKind.fieldList) {
            if (!TextUtils.equals(valuesDelta.getAsString(editField.column), contentValues.getAsString(editField.column))) {
                return false;
            }
        }
        return true;
    }

    public static void parseExtras(Context context, AccountType accountType, RawContactDelta rawContactDelta, Bundle bundle) {
        if (bundle == null || bundle.size() == 0) {
            return;
        }
        parseStructuredNameExtra(context, accountType, rawContactDelta, bundle);
        parseStructuredPostalExtra(accountType, rawContactDelta, bundle);
        DataKind kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/phone_v2");
        parseExtras(rawContactDelta, kindForMimetype, bundle, "phone_type", "phone", "data1");
        parseExtras(rawContactDelta, kindForMimetype, bundle, "secondary_phone_type", "secondary_phone", "data1");
        parseExtras(rawContactDelta, kindForMimetype, bundle, "tertiary_phone_type", "tertiary_phone", "data1");
        DataKind kindForMimetype2 = accountType.getKindForMimetype("vnd.android.cursor.item/email_v2");
        parseExtras(rawContactDelta, kindForMimetype2, bundle, "email_type", "email", "data1");
        parseExtras(rawContactDelta, kindForMimetype2, bundle, "secondary_email_type", "secondary_email", "data1");
        parseExtras(rawContactDelta, kindForMimetype2, bundle, "tertiary_email_type", "tertiary_email", "data1");
        DataKind kindForMimetype3 = accountType.getKindForMimetype("vnd.android.cursor.item/im");
        fixupLegacyImType(bundle);
        parseExtras(rawContactDelta, kindForMimetype3, bundle, "im_protocol", "im_handle", "data1");
        boolean z = bundle.containsKey("company") || bundle.containsKey("job_title");
        DataKind kindForMimetype4 = accountType.getKindForMimetype("vnd.android.cursor.item/organization");
        if (z && canInsert(rawContactDelta, kindForMimetype4)) {
            ValuesDelta valuesDeltaInsertChild = insertChild(rawContactDelta, kindForMimetype4);
            String string = bundle.getString("company");
            if (ContactsUtils.isGraphic(string)) {
                valuesDeltaInsertChild.put("data1", string);
            }
            String string2 = bundle.getString("job_title");
            if (ContactsUtils.isGraphic(string2)) {
                valuesDeltaInsertChild.put("data4", string2);
            }
        }
        boolean zContainsKey = bundle.containsKey("notes");
        DataKind kindForMimetype5 = accountType.getKindForMimetype("vnd.android.cursor.item/note");
        if (zContainsKey && canInsert(rawContactDelta, kindForMimetype5)) {
            ValuesDelta valuesDeltaInsertChild2 = insertChild(rawContactDelta, kindForMimetype5);
            String string3 = bundle.getString("notes");
            if (ContactsUtils.isGraphic(string3)) {
                valuesDeltaInsertChild2.put("data1", string3);
            }
        }
        ArrayList parcelableArrayList = bundle.getParcelableArrayList("data");
        if (parcelableArrayList != null) {
            Log.d(TAG, "[parseValues]");
            parseValues(rawContactDelta, accountType, parcelableArrayList);
        }
    }

    private static void parseStructuredNameExtra(Context context, AccountType accountType, RawContactDelta rawContactDelta, Bundle bundle) {
        boolean z;
        ensureKindExists(rawContactDelta, accountType, "vnd.android.cursor.item/name");
        ValuesDelta primaryEntry = rawContactDelta.getPrimaryEntry("vnd.android.cursor.item/name");
        String string = bundle.getString("name");
        if (ContactsUtils.isGraphic(string)) {
            DataKind kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/name");
            if (kindForMimetype.fieldList != null) {
                Iterator<AccountType.EditField> it = kindForMimetype.fieldList.iterator();
                while (it.hasNext()) {
                    if ("data1".equals(it.next().column)) {
                        z = true;
                        break;
                    }
                }
                z = false;
                if (!z) {
                    primaryEntry.put("data1", string);
                } else {
                    Cursor cursorQuery = context.getContentResolver().query(ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name").appendQueryParameter("data1", string).build(), new String[]{"data4", COLUMN_FOR_TYPE, "data5", COLUMN_FOR_LABEL, "data6"}, null, null, null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                primaryEntry.put("data4", cursorQuery.getString(0));
                                primaryEntry.put(COLUMN_FOR_TYPE, cursorQuery.getString(1));
                                primaryEntry.put("data5", cursorQuery.getString(2));
                                primaryEntry.put(COLUMN_FOR_LABEL, cursorQuery.getString(3));
                                primaryEntry.put("data6", cursorQuery.getString(4));
                            }
                        } finally {
                            cursorQuery.close();
                        }
                    }
                }
            } else {
                z = false;
                if (!z) {
                }
            }
        }
        String string2 = bundle.getString("phonetic_name");
        if (ContactsUtils.isGraphic(string2)) {
            StructuredNameDataItem phoneticName = NameConverter.parsePhoneticName(string2, null);
            primaryEntry.put("data9", phoneticName.getPhoneticFamilyName());
            primaryEntry.put("data8", phoneticName.getPhoneticMiddleName());
            primaryEntry.put("data7", phoneticName.getPhoneticGivenName());
        }
    }

    private static void parseStructuredPostalExtra(AccountType accountType, RawContactDelta rawContactDelta, Bundle bundle) {
        DataKind kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/postal-address_v2");
        ValuesDelta extras = parseExtras(rawContactDelta, kindForMimetype, bundle, "postal_type", "postal", "data1");
        String asString = extras == null ? null : extras.getAsString("data1");
        if (!TextUtils.isEmpty(asString)) {
            boolean z = false;
            if (kindForMimetype.fieldList != null) {
                Iterator<AccountType.EditField> it = kindForMimetype.fieldList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    } else if ("data1".equals(it.next().column)) {
                        z = true;
                        break;
                    }
                }
            }
            if (!z) {
                extras.put("data4", asString);
                extras.putNull("data1");
            }
        }
    }

    private static void parseValues(RawContactDelta rawContactDelta, AccountType accountType, ArrayList<ContentValues> arrayList) {
        int i;
        Log.sensitive(TAG, "[parseValues] state : " + rawContactDelta);
        Log.sensitive(TAG, "[parseValues] dataValueList : " + arrayList);
        for (ContentValues contentValues : arrayList) {
            String asString = contentValues.getAsString("mimetype");
            if (TextUtils.isEmpty(asString)) {
                Log.e(TAG, "Mimetype is required. Ignoring: " + contentValues);
            } else if (!"vnd.android.cursor.item/name".equals(asString)) {
                if ("vnd.android.cursor.item/phone_v2".equals(asString)) {
                    contentValues.remove(PhoneDataItem.KEY_FORMATTED_PHONE_NUMBER);
                    Integer asInteger = contentValues.getAsInteger(COLUMN_FOR_TYPE);
                    if (asInteger != null && asInteger.intValue() == 0 && TextUtils.isEmpty(contentValues.getAsString(COLUMN_FOR_LABEL))) {
                        contentValues.put(COLUMN_FOR_TYPE, (Integer) 2);
                    }
                }
                DataKind kindForMimetype = accountType.getKindForMimetype(asString);
                if (kindForMimetype == null) {
                    Log.e(TAG, "Mimetype not supported for account type " + accountType.getAccountTypeAndDataSet() + ". Ignoring: " + contentValues);
                } else {
                    ValuesDelta valuesDeltaFromAfter = ValuesDelta.fromAfter(contentValues);
                    if (!isEmpty(valuesDeltaFromAfter, kindForMimetype)) {
                        ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(asString);
                        Log.sensitive(TAG, "[parseValues] entries : " + mimeEntries);
                        boolean zAdjustType = false;
                        boolean zAdjustType2 = true;
                        if (kindForMimetype.typeOverallMax != 1 || "vnd.android.cursor.item/group_membership".equals(asString)) {
                            Log.d(TAG, "[parseValues] kind.typeOverallMax != 1");
                            if (mimeEntries != null && mimeEntries.size() > 0) {
                                Log.d(TAG, "[parseValues] entries.size() : " + mimeEntries.size());
                                Iterator<ValuesDelta> it = mimeEntries.iterator();
                                i = 0;
                                while (true) {
                                    if (!it.hasNext()) {
                                        break;
                                    }
                                    ValuesDelta next = it.next();
                                    if (!next.isDelete()) {
                                        Log.d(TAG, "[parseValues]!delta.isDelete() ");
                                        if (!areEqual(next, contentValues, kindForMimetype)) {
                                            Log.d(TAG, "[parseValues] entries.size() : " + mimeEntries.size());
                                            if (!"vnd.android.cursor.item/group_membership".equals(asString)) {
                                                i++;
                                            }
                                        } else {
                                            zAdjustType2 = false;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                i = 0;
                            }
                            Log.d(TAG, "[parseValues] count : " + i + " | kind.typeOverallMax: " + kindForMimetype.typeOverallMax);
                            if (kindForMimetype.typeOverallMax != -1 && i >= kindForMimetype.typeOverallMax) {
                                Log.e(TAG, "Mimetype allows at most " + kindForMimetype.typeOverallMax + " entries. Ignoring: " + contentValues);
                            } else {
                                zAdjustType = zAdjustType2;
                            }
                            if (zAdjustType) {
                                Log.sensitive(TAG, "[parseValues] entry : " + valuesDeltaFromAfter + " | entries : " + mimeEntries + " | kind : " + kindForMimetype);
                                zAdjustType = adjustType(valuesDeltaFromAfter, mimeEntries, kindForMimetype);
                                String str = TAG;
                                StringBuilder sb = new StringBuilder();
                                sb.append("[parseValues] addEntry : ");
                                sb.append(zAdjustType);
                                Log.d(str, sb.toString());
                            }
                            if (zAdjustType) {
                                rawContactDelta.addEntry(valuesDeltaFromAfter);
                                Log.sensitive(TAG, "[parseValues] state : " + rawContactDelta);
                            }
                            Log.d(TAG, "[parseValues] count : " + i);
                        } else {
                            Log.d(TAG, "[parseValues] kind.typeOverallMax == 1");
                            if (mimeEntries != null && mimeEntries.size() > 0) {
                                Log.sensitive(TAG, "[parseValues] entries != null entries: " + mimeEntries);
                                Iterator<ValuesDelta> it2 = mimeEntries.iterator();
                                while (true) {
                                    if (!it2.hasNext()) {
                                        break;
                                    }
                                    ValuesDelta next2 = it2.next();
                                    if (!next2.isDelete() && !isEmpty(next2, kindForMimetype)) {
                                        zAdjustType2 = false;
                                        break;
                                    }
                                }
                                if (zAdjustType2) {
                                    Iterator<ValuesDelta> it3 = mimeEntries.iterator();
                                    while (it3.hasNext()) {
                                        it3.next().markDeleted();
                                    }
                                }
                            }
                            if (zAdjustType2) {
                                zAdjustType2 = adjustType(valuesDeltaFromAfter, mimeEntries, kindForMimetype);
                                Log.d(TAG, "[parseValues] addEntry1 : " + zAdjustType2);
                            }
                            if (zAdjustType2) {
                                rawContactDelta.addEntry(valuesDeltaFromAfter);
                                Log.sensitive(TAG, "[parseValues] state1 : " + rawContactDelta);
                            } else if ("vnd.android.cursor.item/note".equals(asString)) {
                                Iterator<ValuesDelta> it4 = mimeEntries.iterator();
                                while (true) {
                                    if (it4.hasNext()) {
                                        ValuesDelta next3 = it4.next();
                                        if (!isEmpty(next3, kindForMimetype)) {
                                            next3.put("data1", next3.getAsString("data1") + "\n" + contentValues.getAsString("data1"));
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "Will not override mimetype " + asString + ". Ignoring: " + contentValues);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean adjustType(ValuesDelta valuesDelta, ArrayList<ValuesDelta> arrayList, DataKind dataKind) {
        if (dataKind.typeColumn == null || dataKind.typeList == null || dataKind.typeList.size() == 0) {
            Log.d(TAG, "[adjustType] true");
            return true;
        }
        Integer asInteger = valuesDelta.getAsInteger(dataKind.typeColumn);
        int iIntValue = asInteger != null ? asInteger.intValue() : dataKind.typeList.get(0).rawValue;
        Log.d(TAG, "[adjustType] typeInteger : " + asInteger + " | type : " + iIntValue);
        if (isTypeAllowed(iIntValue, arrayList, dataKind)) {
            valuesDelta.put(dataKind.typeColumn, iIntValue);
            Log.sensitive(TAG, "[adjustType] entry1 : " + valuesDelta);
            return true;
        }
        int size = dataKind.typeList.size();
        for (int i = 0; i < size; i++) {
            AccountType.EditType editType = dataKind.typeList.get(i);
            if (isTypeAllowed(editType.rawValue, arrayList, dataKind)) {
                valuesDelta.put(dataKind.typeColumn, editType.rawValue);
                Log.sensitive(TAG, "[adjustType] entry1 : " + valuesDelta + " | size: " + size);
                return true;
            }
        }
        Log.d(TAG, "[adjustType] false");
        return false;
    }

    private static boolean isTypeAllowed(int i, ArrayList<ValuesDelta> arrayList, DataKind dataKind) {
        int i2;
        int size = dataKind.typeList.size();
        int i3 = 0;
        while (true) {
            if (i3 < size) {
                AccountType.EditType editType = dataKind.typeList.get(i3);
                if (editType.rawValue != i) {
                    i3++;
                } else {
                    i2 = editType.specificMax;
                    break;
                }
            } else {
                i2 = 0;
                break;
            }
        }
        if (i2 == 0) {
            return false;
        }
        return i2 == -1 || getEntryCountByType(arrayList, dataKind.typeColumn, i) < i2;
    }

    private static int getEntryCountByType(ArrayList<ValuesDelta> arrayList, String str, int i) {
        int i2 = 0;
        if (arrayList != null) {
            Iterator<ValuesDelta> it = arrayList.iterator();
            while (it.hasNext()) {
                Integer asInteger = it.next().getAsInteger(str);
                if (asInteger != null && asInteger.intValue() == i) {
                    i2++;
                }
            }
        }
        return i2;
    }

    private static void fixupLegacyImType(Bundle bundle) {
        String string = bundle.getString("im_protocol");
        if (string == null) {
            return;
        }
        try {
            Object objDecodeImProtocol = Contacts.ContactMethods.decodeImProtocol(string);
            if (objDecodeImProtocol instanceof Integer) {
                bundle.putInt("im_protocol", ((Integer) objDecodeImProtocol).intValue());
            } else {
                bundle.putString("im_protocol", (String) objDecodeImProtocol);
            }
        } catch (IllegalArgumentException e) {
        }
    }

    public static ValuesDelta parseExtras(RawContactDelta rawContactDelta, DataKind dataKind, Bundle bundle, String str, String str2, String str3) {
        CharSequence charSequence = bundle.getCharSequence(str2);
        if (dataKind == null) {
            return null;
        }
        boolean zCanInsert = canInsert(rawContactDelta, dataKind);
        int i = 0;
        if (!(charSequence != null && TextUtils.isGraphic(charSequence)) || !zCanInsert) {
            return null;
        }
        if (!bundle.containsKey(str)) {
            i = FREQUENCY_TOTAL;
        }
        AccountType.EditType bestValidType = getBestValidType(rawContactDelta, dataKind, true, bundle.getInt(str, i));
        ValuesDelta valuesDeltaInsertChild = insertChild(rawContactDelta, dataKind, bestValidType);
        valuesDeltaInsertChild.put(str3, charSequence.toString());
        if (bestValidType != null && bestValidType.customColumn != null) {
            valuesDeltaInsertChild.put(bestValidType.customColumn, bundle.getString(str));
        }
        return valuesDeltaInsertChild;
    }

    public static void migrateStateForNewContact(Context context, RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2, AccountType accountType, AccountType accountType2) {
        Log.sensitive(TAG, "[migrateStateForNewContact] beg, oldState: " + rawContactDelta + ",newState: " + rawContactDelta2 + ",oldAccountType: " + accountType + ",newAccountType: " + accountType2);
        if (accountType2 == accountType) {
            for (DataKind dataKind : accountType2.getSortedDataKinds()) {
                String str = dataKind.mimeType;
                if ("vnd.android.cursor.item/name".equals(str)) {
                    migrateStructuredName(context, rawContactDelta, rawContactDelta2, dataKind);
                } else {
                    ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(str);
                    if (mimeEntries != null && !mimeEntries.isEmpty()) {
                        Iterator<ValuesDelta> it = mimeEntries.iterator();
                        while (it.hasNext()) {
                            ContentValues after = it.next().getAfter();
                            if (after != null) {
                                rawContactDelta2.addEntry(ValuesDelta.fromAfter(after));
                            }
                        }
                    }
                }
            }
        } else {
            for (DataKind dataKind2 : accountType2.getSortedDataKinds()) {
                if (dataKind2.editable) {
                    String str2 = dataKind2.mimeType;
                    if (!DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(str2) && !DataKind.PSEUDO_MIME_TYPE_NAME.equals(str2)) {
                        if ("vnd.android.cursor.item/name".equals(str2)) {
                            migrateStructuredName(context, rawContactDelta, rawContactDelta2, dataKind2);
                        } else if ("vnd.android.cursor.item/postal-address_v2".equals(str2)) {
                            migratePostal(rawContactDelta, rawContactDelta2, dataKind2);
                        } else if ("vnd.android.cursor.item/contact_event".equals(str2)) {
                            migrateEvent(rawContactDelta, rawContactDelta2, dataKind2, null);
                        } else if (sGenericMimeTypesWithoutTypeSupport.contains(str2)) {
                            migrateGenericWithoutTypeColumn(rawContactDelta, rawContactDelta2, dataKind2);
                        } else if (sGenericMimeTypesWithTypeSupport.contains(str2)) {
                            migrateGenericWithTypeColumn(rawContactDelta, rawContactDelta2, dataKind2);
                        } else {
                            throw new IllegalStateException("Unexpected editable mime-type: " + str2);
                        }
                        if ("vnd.android.cursor.item/phone_v2".equals(str2)) {
                            SimAasEditor.removeRedundantAnrFieldForNonSimAccount(accountType, accountType2, rawContactDelta2, str2);
                        }
                    }
                }
            }
            rawContactDelta2.removeEntry(null);
        }
        Log.sensitive(TAG, "[migrateStateForNewContact] end, newState: " + rawContactDelta2);
    }

    private static ArrayList<ValuesDelta> ensureEntryMaxSize(RawContactDelta rawContactDelta, DataKind dataKind, ArrayList<ValuesDelta> arrayList) {
        if (arrayList == null) {
            return null;
        }
        int i = dataKind.typeOverallMax;
        if (i >= 0 && arrayList.size() > i) {
            ArrayList<ValuesDelta> arrayList2 = new ArrayList<>(i);
            for (int i2 = 0; i2 < i; i2++) {
                arrayList2.add(arrayList.get(i2));
            }
            return arrayList2;
        }
        return arrayList;
    }

    public static void migrateStructuredName(Context context, RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2, DataKind dataKind) {
        ContentValues after = rawContactDelta.getPrimaryEntry("vnd.android.cursor.item/name").getAfter();
        if (after == null) {
            return;
        }
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        for (AccountType.EditField editField : dataKind.fieldList) {
            if ("data1".equals(editField.column)) {
                z = true;
            }
            if ("data9".equals(editField.column)) {
                z2 = true;
            }
            if ("data8".equals(editField.column)) {
                z3 = true;
            }
            if ("data7".equals(editField.column)) {
                z4 = true;
            }
        }
        String asString = after.getAsString("data1");
        if (!TextUtils.isEmpty(asString)) {
            if (!z) {
                NameConverter.displayNameToStructuredName(context, asString, after);
                after.remove("data1");
            }
        } else if (z) {
            after.put("data1", NameConverter.structuredNameToDisplayName(context, after));
            for (String str : NameConverter.STRUCTURED_NAME_FIELDS) {
                after.remove(str);
            }
        }
        if (!z2) {
            after.remove("data9");
        }
        if (!z3) {
            after.remove("data8");
        }
        if (!z4) {
            after.remove("data7");
        }
        rawContactDelta2.addEntry(ValuesDelta.fromAfter(after));
    }

    public static void migratePostal(RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2, DataKind dataKind) {
        String[] strArr;
        int iIntValue;
        ArrayList<ValuesDelta> arrayListEnsureEntryMaxSize = ensureEntryMaxSize(rawContactDelta2, dataKind, rawContactDelta.getMimeEntries("vnd.android.cursor.item/postal-address_v2"));
        if (arrayListEnsureEntryMaxSize == null || arrayListEnsureEntryMaxSize.isEmpty()) {
            return;
        }
        String str = dataKind.fieldList.get(0).column;
        boolean z = false;
        boolean z2 = false;
        for (AccountType.EditField editField : dataKind.fieldList) {
            if ("data1".equals(editField.column)) {
                z = true;
            }
            if ("data4".equals(editField.column)) {
                z2 = true;
            }
        }
        HashSet hashSet = new HashSet();
        if (dataKind.typeList != null && !dataKind.typeList.isEmpty()) {
            Iterator<AccountType.EditType> it = dataKind.typeList.iterator();
            while (it.hasNext()) {
                hashSet.add(Integer.valueOf(it.next().rawValue));
            }
        }
        Iterator<ValuesDelta> it2 = arrayListEnsureEntryMaxSize.iterator();
        while (it2.hasNext()) {
            ContentValues after = it2.next().getAfter();
            if (after != null) {
                Integer asInteger = after.getAsInteger(COLUMN_FOR_TYPE);
                if (!hashSet.contains(asInteger)) {
                    if (dataKind.defaultValues != null) {
                        iIntValue = dataKind.defaultValues.getAsInteger(COLUMN_FOR_TYPE).intValue();
                    } else {
                        iIntValue = dataKind.typeList.get(0).rawValue;
                    }
                    after.put(COLUMN_FOR_TYPE, Integer.valueOf(iIntValue));
                    if (asInteger != null && asInteger.intValue() == 0) {
                        after.remove(COLUMN_FOR_LABEL);
                    }
                }
                String asString = after.getAsString("data1");
                if (!TextUtils.isEmpty(asString)) {
                    if (!z) {
                        after.remove("data1");
                        if (z2) {
                            after.put("data4", asString);
                        } else {
                            after.put(str, asString);
                        }
                    }
                } else if (z) {
                    if (Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage())) {
                        strArr = new String[]{after.getAsString("data10"), after.getAsString("data9"), after.getAsString("data8"), after.getAsString("data7"), after.getAsString("data6"), after.getAsString("data4"), after.getAsString("data5")};
                    } else {
                        strArr = new String[]{after.getAsString("data5"), after.getAsString("data4"), after.getAsString("data6"), after.getAsString("data7"), after.getAsString("data8"), after.getAsString("data9"), after.getAsString("data10")};
                    }
                    StringBuilder sb = new StringBuilder();
                    for (String str2 : strArr) {
                        if (!TextUtils.isEmpty(str2)) {
                            sb.append(str2 + "\n");
                        }
                    }
                    after.put("data1", sb.toString());
                    after.remove("data5");
                    after.remove("data4");
                    after.remove("data6");
                    after.remove("data7");
                    after.remove("data8");
                    after.remove("data9");
                    after.remove("data10");
                }
                rawContactDelta2.addEntry(ValuesDelta.fromAfter(after));
            }
        }
    }

    public static void migrateEvent(RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2, DataKind dataKind, Integer num) {
        ArrayList<ValuesDelta> arrayListEnsureEntryMaxSize = ensureEntryMaxSize(rawContactDelta2, dataKind, rawContactDelta.getMimeEntries("vnd.android.cursor.item/contact_event"));
        if (arrayListEnsureEntryMaxSize == null || arrayListEnsureEntryMaxSize.isEmpty()) {
            return;
        }
        SparseArray sparseArray = new SparseArray();
        for (AccountType.EditType editType : dataKind.typeList) {
            sparseArray.put(editType.rawValue, (AccountType.EventEditType) editType);
        }
        Iterator<ValuesDelta> it = arrayListEnsureEntryMaxSize.iterator();
        Integer numValueOf = num;
        while (it.hasNext()) {
            ContentValues after = it.next().getAfter();
            if (after != null) {
                String asString = after.getAsString("data1");
                Integer asInteger = after.getAsInteger(COLUMN_FOR_TYPE);
                if (asInteger != null && sparseArray.indexOfKey(asInteger.intValue()) >= 0 && !TextUtils.isEmpty(asString)) {
                    AccountType.EventEditType eventEditType = (AccountType.EventEditType) sparseArray.get(asInteger.intValue());
                    boolean z = false;
                    ParsePosition parsePosition = new ParsePosition(0);
                    Date date = CommonDateUtils.DATE_AND_TIME_FORMAT.parse(asString, parsePosition);
                    if (date == null) {
                        date = CommonDateUtils.NO_YEAR_DATE_FORMAT.parse(asString, parsePosition);
                        z = true;
                    }
                    if (date != null && z && !eventEditType.isYearOptional()) {
                        Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
                        if (numValueOf == null) {
                            numValueOf = Integer.valueOf(calendar.get(1));
                        }
                        calendar.setTime(date);
                        calendar.set(numValueOf.intValue(), calendar.get(2), calendar.get(5), 8, 0, 0);
                        after.put("data1", CommonDateUtils.FULL_DATE_FORMAT.format(calendar.getTime()));
                    }
                    rawContactDelta2.addEntry(ValuesDelta.fromAfter(after));
                }
            }
        }
    }

    public static void migrateGenericWithoutTypeColumn(RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2, DataKind dataKind) {
        ArrayList<ValuesDelta> arrayListEnsureEntryMaxSize = ensureEntryMaxSize(rawContactDelta2, dataKind, rawContactDelta.getMimeEntries(dataKind.mimeType));
        if (arrayListEnsureEntryMaxSize == null || arrayListEnsureEntryMaxSize.isEmpty()) {
            return;
        }
        Iterator<ValuesDelta> it = arrayListEnsureEntryMaxSize.iterator();
        while (it.hasNext()) {
            ContentValues after = it.next().getAfter();
            if (after != null) {
                rawContactDelta2.addEntry(ValuesDelta.fromAfter(after));
            }
        }
    }

    public static void migrateGenericWithTypeColumn(RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2, DataKind dataKind) {
        Integer numValueOf;
        int i;
        ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(dataKind.mimeType);
        if (mimeEntries == null || mimeEntries.isEmpty()) {
            return;
        }
        if (dataKind.defaultValues != null) {
            numValueOf = dataKind.defaultValues.getAsInteger(COLUMN_FOR_TYPE);
        } else {
            numValueOf = null;
        }
        HashSet hashSet = new HashSet();
        SparseIntArray sparseIntArray = new SparseIntArray();
        if (numValueOf != null) {
            hashSet.add(numValueOf);
            sparseIntArray.put(numValueOf.intValue(), -1);
        }
        if (!"vnd.android.cursor.item/im".equals(dataKind.mimeType) && dataKind.typeList != null && !dataKind.typeList.isEmpty()) {
            for (AccountType.EditType editType : dataKind.typeList) {
                hashSet.add(Integer.valueOf(editType.rawValue));
                sparseIntArray.put(editType.rawValue, editType.specificMax);
            }
            if (numValueOf == null) {
                numValueOf = Integer.valueOf(dataKind.typeList.get(0).rawValue);
            }
        }
        if (numValueOf == null) {
            Log.w(TAG, "Default type isn't available for mimetype " + dataKind.mimeType);
        }
        int i2 = dataKind.typeOverallMax;
        SparseIntArray sparseIntArray2 = new SparseIntArray();
        int i3 = 0;
        for (ValuesDelta valuesDelta : mimeEntries) {
            if (i2 == -1 || i3 < i2) {
                ContentValues after = valuesDelta.getAfter();
                if (after != null) {
                    Integer asInteger = valuesDelta.getAsInteger(COLUMN_FOR_TYPE);
                    if (!hashSet.contains(asInteger)) {
                        if (numValueOf != null) {
                            Integer numValueOf2 = Integer.valueOf(numValueOf.intValue());
                            after.put(COLUMN_FOR_TYPE, Integer.valueOf(numValueOf.intValue()));
                            if (asInteger != null && asInteger.intValue() == 0) {
                                after.remove(COLUMN_FOR_LABEL);
                            }
                            asInteger = numValueOf2;
                        } else {
                            after.remove(COLUMN_FOR_TYPE);
                            asInteger = null;
                        }
                    }
                    if (asInteger != null && (i = sparseIntArray.get(asInteger.intValue(), 0)) >= 0) {
                        int i4 = sparseIntArray2.get(asInteger.intValue(), 0);
                        if (i4 < i) {
                            sparseIntArray2.put(asInteger.intValue(), i4 + 1);
                        }
                    }
                    rawContactDelta2.addEntry(ValuesDelta.fromAfter(after));
                    i3++;
                }
            } else {
                return;
            }
        }
    }
}
