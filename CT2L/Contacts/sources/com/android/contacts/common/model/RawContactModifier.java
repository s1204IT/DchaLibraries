package com.android.contacts.common.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.StructuredNameDataItem;
import com.android.contacts.common.util.CommonDateUtils;
import com.android.contacts.common.util.DateUtils;
import com.android.contacts.common.util.NameConverter;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RawContactModifier {
    private static final String TAG = RawContactModifier.class.getSimpleName();
    private static final Set<String> sGenericMimeTypesWithTypeSupport = new HashSet(Arrays.asList("vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/im", "vnd.android.cursor.item/nickname", "vnd.android.cursor.item/website", "vnd.android.cursor.item/relation", "vnd.android.cursor.item/sip_address"));
    private static final Set<String> sGenericMimeTypesWithoutTypeSupport = new HashSet(Arrays.asList("vnd.android.cursor.item/organization", "vnd.android.cursor.item/note", "vnd.android.cursor.item/photo", "vnd.android.cursor.item/group_membership"));

    public static boolean canInsert(RawContactDelta state, DataKind kind) {
        int visibleCount = state.getMimeEntriesCount(kind.mimeType, true);
        boolean validTypes = hasValidTypes(state, kind);
        boolean validOverall = kind.typeOverallMax == -1 || visibleCount < kind.typeOverallMax;
        return validTypes && validOverall;
    }

    public static boolean hasValidTypes(RawContactDelta state, DataKind kind) {
        return !hasEditTypes(kind) || getValidTypes(state, kind).size() > 0;
    }

    public static ValuesDelta ensureKindExists(RawContactDelta state, AccountType accountType, String mimeType) {
        DataKind kind = accountType.getKindForMimetype(mimeType);
        boolean hasChild = state.getMimeEntriesCount(mimeType, true) > 0;
        if (kind != null) {
            if (hasChild) {
                return state.getMimeEntries(mimeType).get(0);
            }
            ValuesDelta child = insertChild(state, kind);
            if (kind.mimeType.equals("vnd.android.cursor.item/photo")) {
                child.setFromTemplate(true);
            }
            return child;
        }
        return null;
    }

    public static ArrayList<AccountType.EditType> getValidTypes(RawContactDelta state, DataKind kind) {
        return getValidTypes(state, kind, null, true, null);
    }

    public static ArrayList<AccountType.EditType> getValidTypes(RawContactDelta state, DataKind kind, AccountType.EditType forceInclude) {
        return getValidTypes(state, kind, forceInclude, true, null);
    }

    private static ArrayList<AccountType.EditType> getValidTypes(RawContactDelta state, DataKind kind, AccountType.EditType forceInclude, boolean includeSecondary, SparseIntArray typeCount) {
        ArrayList<AccountType.EditType> validTypes = new ArrayList<>();
        if (hasEditTypes(kind)) {
            if (typeCount == null) {
                typeCount = getTypeFrequencies(state, kind);
            }
            int overallCount = typeCount.get(Integer.MIN_VALUE);
            for (AccountType.EditType type : kind.typeList) {
                boolean validOverall = kind.typeOverallMax == -1 || overallCount < kind.typeOverallMax;
                boolean validSpecific = type.specificMax == -1 || typeCount.get(type.rawValue) < type.specificMax;
                boolean validSecondary = includeSecondary || !type.secondary;
                boolean forcedInclude = type.equals(forceInclude);
                if (forcedInclude || (validOverall && validSpecific && validSecondary)) {
                    validTypes.add(type);
                }
            }
        }
        return validTypes;
    }

    public static ArrayList<AccountType.EditType> getValidTypes(RawContactDelta state, DataKind kind, AccountType.EditType forceInclude, boolean isSimKind) {
        return isSimKind ? getValidTypesForSim(state, kind, forceInclude, true, null) : getValidTypes(state, kind, forceInclude, true, null);
    }

    private static ArrayList<AccountType.EditType> getValidTypesForSim(RawContactDelta state, DataKind kind, AccountType.EditType forceInclude, boolean includeSecondary, SparseIntArray typeCount) {
        ArrayList<AccountType.EditType> validTypes = new ArrayList<>();
        if (hasEditTypes(kind)) {
            if (typeCount == null) {
                typeCount = getTypeFrequencies(state, kind);
            }
            int overallCount = typeCount.get(Integer.MIN_VALUE);
            for (AccountType.EditType type : kind.typeList) {
                boolean validOverall = kind.typeOverallMax == -1 || overallCount < kind.typeOverallMax;
                boolean validSpecific = type.specificMax == -1 || typeCount.get(type.rawValue) < type.specificMax;
                boolean validSecondary = includeSecondary || !type.secondary;
                boolean forcedInclude = type.equals(forceInclude);
                if (forcedInclude || (validOverall && validSpecific && validSecondary)) {
                    validTypes.add(type);
                    break;
                }
            }
        }
        return validTypes;
    }

    private static SparseIntArray getTypeFrequencies(RawContactDelta state, DataKind kind) {
        SparseIntArray typeCount = new SparseIntArray();
        List<ValuesDelta> mimeEntries = state.getMimeEntries(kind.mimeType);
        if (mimeEntries != null) {
            int totalCount = 0;
            for (ValuesDelta entry : mimeEntries) {
                if (entry.isVisible()) {
                    totalCount++;
                    AccountType.EditType type = getCurrentType(entry, kind);
                    if (type != null) {
                        int count = typeCount.get(type.rawValue);
                        typeCount.put(type.rawValue, count + 1);
                    }
                }
            }
            typeCount.put(Integer.MIN_VALUE, totalCount);
        }
        return typeCount;
    }

    public static boolean hasEditTypes(DataKind kind) {
        return kind.typeList != null && kind.typeList.size() > 0;
    }

    public static AccountType.EditType getCurrentType(ValuesDelta entry, DataKind kind) {
        Long rawValue = entry.getAsLong(kind.typeColumn);
        if (rawValue == null) {
            return null;
        }
        return getType(kind, rawValue.intValue());
    }

    public static AccountType.EditType getType(DataKind kind, int rawValue) {
        for (AccountType.EditType type : kind.typeList) {
            if (type.rawValue == rawValue) {
                return type;
            }
        }
        return null;
    }

    public static int getTypePrecedence(DataKind kind, int rawValue) {
        for (int i = 0; i < kind.typeList.size(); i++) {
            AccountType.EditType type = kind.typeList.get(i);
            if (type.rawValue == rawValue) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static AccountType.EditType getBestValidType(RawContactDelta state, DataKind kind, boolean includeSecondary, int exactValue) {
        if (kind == null || kind.typeColumn == null) {
            return null;
        }
        SparseIntArray typeCount = getTypeFrequencies(state, kind);
        ArrayList<AccountType.EditType> validTypes = getValidTypes(state, kind, null, includeSecondary, typeCount);
        if (validTypes.size() == 0) {
            return null;
        }
        AccountType.EditType lastType = validTypes.get(validTypes.size() - 1);
        Iterator<AccountType.EditType> iterator = validTypes.iterator();
        while (iterator.hasNext()) {
            AccountType.EditType type = iterator.next();
            int count = typeCount.get(type.rawValue);
            if (exactValue != type.rawValue) {
                if (count > 0) {
                    iterator.remove();
                }
            } else {
                return type;
            }
        }
        return validTypes.size() > 0 ? validTypes.get(0) : lastType;
    }

    public static ValuesDelta insertChild(RawContactDelta state, DataKind kind) {
        if (kind == null) {
            return null;
        }
        AccountType.EditType bestType = getBestValidType(state, kind, false, Integer.MIN_VALUE);
        if (bestType == null) {
            bestType = getBestValidType(state, kind, true, Integer.MIN_VALUE);
        }
        return insertChild(state, kind, bestType);
    }

    public static ValuesDelta insertChild(RawContactDelta state, DataKind kind, AccountType.EditType type) {
        if (kind == null) {
            return null;
        }
        ContentValues after = new ContentValues();
        after.put("mimetype", kind.mimeType);
        if (kind.defaultValues != null) {
            after.putAll(kind.defaultValues);
        }
        if (kind.typeColumn != null && type != null) {
            after.put(kind.typeColumn, Integer.valueOf(type.rawValue));
        }
        ValuesDelta child = ValuesDelta.fromAfter(after);
        state.addEntry(child);
        return child;
    }

    public static void trimEmpty(RawContactDeltaList set, AccountTypeManager accountTypes) {
        for (RawContactDelta state : set) {
            ValuesDelta values = state.getValues();
            String accountType = values.getAsString("account_type");
            String dataSet = values.getAsString("data_set");
            AccountType type = accountTypes.getAccountType(accountType, dataSet);
            trimEmpty(state, type);
        }
    }

    public static boolean hasChanges(RawContactDeltaList set, AccountTypeManager accountTypes) {
        if (set.isMarkedForSplitting() || set.isMarkedForJoining()) {
            return true;
        }
        for (RawContactDelta state : set) {
            ValuesDelta values = state.getValues();
            String accountType = values.getAsString("account_type");
            String dataSet = values.getAsString("data_set");
            AccountType type = accountTypes.getAccountType(accountType, dataSet);
            if (hasChanges(state, type)) {
                return true;
            }
        }
        return false;
    }

    public static void trimEmpty(RawContactDelta state, AccountType accountType) {
        boolean hasValues = false;
        for (DataKind kind : accountType.getSortedDataKinds()) {
            String mimeType = kind.mimeType;
            ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
            if (entries != null) {
                for (ValuesDelta entry : entries) {
                    boolean touched = entry.isInsert() || entry.isUpdate();
                    if (!touched) {
                        hasValues = true;
                    } else {
                        boolean isGoogleAccount = TextUtils.equals("com.google", state.getValues().getAsString("account_type"));
                        boolean isPhoto = TextUtils.equals("vnd.android.cursor.item/photo", kind.mimeType);
                        boolean isGooglePhoto = isPhoto && isGoogleAccount;
                        if (isEmpty(entry, kind) && !isGooglePhoto) {
                            Log.v(TAG, "Trimming: " + entry.toString());
                            entry.markDeleted();
                        } else if (!entry.isFromTemplate()) {
                            hasValues = true;
                        }
                    }
                }
            }
        }
        if (!hasValues) {
            state.markDeleted();
        }
    }

    private static boolean hasChanges(RawContactDelta state, AccountType accountType) {
        for (DataKind kind : accountType.getSortedDataKinds()) {
            String mimeType = kind.mimeType;
            ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
            if (entries != null) {
                for (ValuesDelta entry : entries) {
                    boolean isRealInsert = entry.isInsert() && !isEmpty(entry, kind);
                    if (isRealInsert || entry.isUpdate() || entry.isDelete()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isEmpty(ValuesDelta values, DataKind kind) {
        if ("vnd.android.cursor.item/photo".equals(kind.mimeType)) {
            return values.isInsert() && values.getAsByteArray("data15") == null;
        }
        if (kind.fieldList == null) {
            return true;
        }
        for (AccountType.EditField field : kind.fieldList) {
            String value = values.getAsString(field.column);
            if (ContactsUtils.isGraphic(value)) {
                return false;
            }
        }
        return true;
    }

    protected static boolean areEqual(ValuesDelta values1, ContentValues values2, DataKind kind) {
        if (kind.fieldList == null) {
            return false;
        }
        for (AccountType.EditField field : kind.fieldList) {
            String value1 = values1.getAsString(field.column);
            String value2 = values2.getAsString(field.column);
            if (!TextUtils.equals(value1, value2)) {
                return false;
            }
        }
        return true;
    }

    public static void parseExtras(Context context, AccountType accountType, RawContactDelta state, Bundle extras) {
        if (extras != null && extras.size() != 0) {
            parseStructuredNameExtra(context, accountType, state, extras);
            parseStructuredPostalExtra(accountType, state, extras);
            DataKind kind = accountType.getKindForMimetype("vnd.android.cursor.item/phone_v2");
            parseExtras(state, kind, extras, "phone_type", "phone", "data1");
            parseExtras(state, kind, extras, "secondary_phone_type", "secondary_phone", "data1");
            parseExtras(state, kind, extras, "tertiary_phone_type", "tertiary_phone", "data1");
            DataKind kind2 = accountType.getKindForMimetype("vnd.android.cursor.item/email_v2");
            parseExtras(state, kind2, extras, "email_type", "email", "data1");
            parseExtras(state, kind2, extras, "secondary_email_type", "secondary_email", "data1");
            parseExtras(state, kind2, extras, "tertiary_email_type", "tertiary_email", "data1");
            DataKind kind3 = accountType.getKindForMimetype("vnd.android.cursor.item/im");
            fixupLegacyImType(extras);
            parseExtras(state, kind3, extras, "im_protocol", "im_handle", "data1");
            boolean hasOrg = extras.containsKey("company") || extras.containsKey("job_title");
            DataKind kindOrg = accountType.getKindForMimetype("vnd.android.cursor.item/organization");
            if (hasOrg && canInsert(state, kindOrg)) {
                ValuesDelta child = insertChild(state, kindOrg);
                String company = extras.getString("company");
                if (ContactsUtils.isGraphic(company)) {
                    child.put("data1", company);
                }
                String title = extras.getString("job_title");
                if (ContactsUtils.isGraphic(title)) {
                    child.put("data4", title);
                }
            }
            boolean hasNotes = extras.containsKey("notes");
            DataKind kindNotes = accountType.getKindForMimetype("vnd.android.cursor.item/note");
            if (hasNotes && canInsert(state, kindNotes)) {
                ValuesDelta child2 = insertChild(state, kindNotes);
                String notes = extras.getString("notes");
                if (ContactsUtils.isGraphic(notes)) {
                    child2.put("data1", notes);
                }
            }
            ArrayList<ContentValues> values = extras.getParcelableArrayList("data");
            if (values != null) {
                parseValues(state, accountType, values);
            }
        }
    }

    private static void parseStructuredNameExtra(Context context, AccountType accountType, RawContactDelta state, Bundle extras) {
        ensureKindExists(state, accountType, "vnd.android.cursor.item/name");
        ValuesDelta child = state.getPrimaryEntry("vnd.android.cursor.item/name");
        String name = extras.getString("name");
        if (ContactsUtils.isGraphic(name)) {
            DataKind kind = accountType.getKindForMimetype("vnd.android.cursor.item/name");
            boolean supportsDisplayName = false;
            if (kind.fieldList != null) {
                Iterator<AccountType.EditField> it = kind.fieldList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    AccountType.EditField field = it.next();
                    if ("data1".equals(field.column)) {
                        supportsDisplayName = true;
                        break;
                    }
                }
            }
            if (supportsDisplayName) {
                child.put("data1", name);
            } else {
                Uri uri = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name").appendQueryParameter("data1", name).build();
                Cursor cursor = context.getContentResolver().query(uri, new String[]{"data4", "data2", "data5", "data3", "data6"}, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            child.put("data4", cursor.getString(0));
                            child.put("data2", cursor.getString(1));
                            child.put("data5", cursor.getString(2));
                            child.put("data3", cursor.getString(3));
                            child.put("data6", cursor.getString(4));
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        String phoneticName = extras.getString("phonetic_name");
        if (ContactsUtils.isGraphic(phoneticName)) {
            child.put("data7", phoneticName);
        }
    }

    private static void parseStructuredPostalExtra(AccountType accountType, RawContactDelta state, Bundle extras) {
        DataKind kind = accountType.getKindForMimetype("vnd.android.cursor.item/postal-address_v2");
        ValuesDelta child = parseExtras(state, kind, extras, "postal_type", "postal", "data1");
        String address = child == null ? null : child.getAsString("data1");
        if (!TextUtils.isEmpty(address)) {
            boolean supportsFormatted = false;
            if (kind.fieldList != null) {
                Iterator<AccountType.EditField> it = kind.fieldList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    AccountType.EditField field = it.next();
                    if ("data1".equals(field.column)) {
                        supportsFormatted = true;
                        break;
                    }
                }
            }
            if (!supportsFormatted) {
                child.put("data4", address);
                child.putNull("data1");
            }
        }
    }

    private static void parseValues(RawContactDelta state, AccountType accountType, ArrayList<ContentValues> dataValueList) {
        for (ContentValues values : dataValueList) {
            String mimeType = values.getAsString("mimetype");
            if (TextUtils.isEmpty(mimeType)) {
                Log.e(TAG, "Mimetype is required. Ignoring: " + values);
            } else if (!"vnd.android.cursor.item/name".equals(mimeType)) {
                if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) {
                    values.remove("formattedPhoneNumber");
                    Integer type = values.getAsInteger("data2");
                    if (type != null && type.intValue() == 0 && TextUtils.isEmpty(values.getAsString("data3"))) {
                        values.put("data2", (Integer) 2);
                    }
                }
                DataKind kind = accountType.getKindForMimetype(mimeType);
                if (kind == null) {
                    Log.e(TAG, "Mimetype not supported for account type " + accountType.getAccountTypeAndDataSet() + ". Ignoring: " + values);
                } else {
                    ValuesDelta entry = ValuesDelta.fromAfter(values);
                    if (!isEmpty(entry, kind)) {
                        ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
                        if (kind.typeOverallMax != 1 || "vnd.android.cursor.item/group_membership".equals(mimeType)) {
                            boolean addEntry = true;
                            int count = 0;
                            if (entries != null && entries.size() > 0) {
                                Iterator<ValuesDelta> it = entries.iterator();
                                while (true) {
                                    if (!it.hasNext()) {
                                        break;
                                    }
                                    ValuesDelta delta = it.next();
                                    if (!delta.isDelete()) {
                                        if (areEqual(delta, values, kind)) {
                                            addEntry = false;
                                            break;
                                        }
                                        count++;
                                    }
                                }
                            }
                            if (kind.typeOverallMax != -1 && count >= kind.typeOverallMax) {
                                Log.e(TAG, "Mimetype allows at most " + kind.typeOverallMax + " entries. Ignoring: " + values);
                                addEntry = false;
                            }
                            if (addEntry) {
                                addEntry = adjustType(entry, entries, kind);
                            }
                            if (addEntry) {
                                state.addEntry(entry);
                            }
                        } else {
                            boolean addEntry2 = true;
                            if (entries != null && entries.size() > 0) {
                                Iterator<ValuesDelta> it2 = entries.iterator();
                                while (true) {
                                    if (!it2.hasNext()) {
                                        break;
                                    }
                                    ValuesDelta delta2 = it2.next();
                                    if (!delta2.isDelete() && !isEmpty(delta2, kind)) {
                                        addEntry2 = false;
                                        break;
                                    }
                                }
                                if (addEntry2) {
                                    Iterator<ValuesDelta> it3 = entries.iterator();
                                    while (it3.hasNext()) {
                                        it3.next().markDeleted();
                                    }
                                }
                            }
                            if (addEntry2) {
                                addEntry2 = adjustType(entry, entries, kind);
                            }
                            if (addEntry2) {
                                state.addEntry(entry);
                            } else if ("vnd.android.cursor.item/note".equals(mimeType)) {
                                Iterator<ValuesDelta> it4 = entries.iterator();
                                while (true) {
                                    if (it4.hasNext()) {
                                        ValuesDelta delta3 = it4.next();
                                        if (!isEmpty(delta3, kind)) {
                                            delta3.put("data1", delta3.getAsString("data1") + "\n" + values.getAsString("data1"));
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "Will not override mimetype " + mimeType + ". Ignoring: " + values);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean adjustType(ValuesDelta entry, ArrayList<ValuesDelta> entries, DataKind kind) {
        if (kind.typeColumn == null || kind.typeList == null || kind.typeList.size() == 0) {
            return true;
        }
        Integer typeInteger = entry.getAsInteger(kind.typeColumn);
        int type = typeInteger != null ? typeInteger.intValue() : kind.typeList.get(0).rawValue;
        if (isTypeAllowed(type, entries, kind)) {
            entry.put(kind.typeColumn, type);
            return true;
        }
        int size = kind.typeList.size();
        for (int i = 0; i < size; i++) {
            AccountType.EditType editType = kind.typeList.get(i);
            if (isTypeAllowed(editType.rawValue, entries, kind)) {
                entry.put(kind.typeColumn, editType.rawValue);
                return true;
            }
        }
        return false;
    }

    private static boolean isTypeAllowed(int type, ArrayList<ValuesDelta> entries, DataKind kind) {
        int max = 0;
        int size = kind.typeList.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            AccountType.EditType editType = kind.typeList.get(i);
            if (editType.rawValue != type) {
                i++;
            } else {
                max = editType.specificMax;
                break;
            }
        }
        if (max == 0) {
            return false;
        }
        return max == -1 || getEntryCountByType(entries, kind.typeColumn, type) < max;
    }

    private static int getEntryCountByType(ArrayList<ValuesDelta> entries, String typeColumn, int type) {
        int count = 0;
        if (entries != null) {
            for (ValuesDelta entry : entries) {
                Integer typeInteger = entry.getAsInteger(typeColumn);
                if (typeInteger != null && typeInteger.intValue() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void fixupLegacyImType(Bundle bundle) {
        String encodedString = bundle.getString("im_protocol");
        if (encodedString != null) {
            try {
                Object protocol = Contacts.ContactMethods.decodeImProtocol(encodedString);
                if (protocol instanceof Integer) {
                    bundle.putInt("im_protocol", ((Integer) protocol).intValue());
                } else {
                    bundle.putString("im_protocol", (String) protocol);
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public static ValuesDelta parseExtras(RawContactDelta state, DataKind kind, Bundle extras, String typeExtra, String valueExtra, String valueColumn) {
        CharSequence value = extras.getCharSequence(valueExtra);
        if (kind == null) {
            return null;
        }
        boolean canInsert = canInsert(state, kind);
        boolean validValue = value != null && TextUtils.isGraphic(value);
        if (!validValue || !canInsert) {
            return null;
        }
        boolean hasType = extras.containsKey(typeExtra);
        int typeValue = extras.getInt(typeExtra, hasType ? 0 : Integer.MIN_VALUE);
        AccountType.EditType editType = getBestValidType(state, kind, true, typeValue);
        ValuesDelta child = insertChild(state, kind, editType);
        child.put(valueColumn, value.toString());
        if (editType != null && editType.customColumn != null) {
            String customType = extras.getString(typeExtra);
            child.put(editType.customColumn, customType);
            return child;
        }
        return child;
    }

    public static void migrateStateForNewContact(Context context, RawContactDelta oldState, RawContactDelta newState, AccountType oldAccountType, AccountType newAccountType) {
        if (newAccountType == oldAccountType) {
            for (DataKind kind : newAccountType.getSortedDataKinds()) {
                String mimeType = kind.mimeType;
                if ("vnd.android.cursor.item/name".equals(mimeType)) {
                    migrateStructuredName(context, oldState, newState, kind);
                } else {
                    List<ValuesDelta> entryList = oldState.getMimeEntries(mimeType);
                    if (entryList != null && !entryList.isEmpty()) {
                        for (ValuesDelta entry : entryList) {
                            ContentValues values = entry.getAfter();
                            if (values != null) {
                                newState.addEntry(ValuesDelta.fromAfter(values));
                            }
                        }
                    }
                }
            }
            return;
        }
        for (DataKind kind2 : newAccountType.getSortedDataKinds()) {
            if (kind2.editable) {
                String mimeType2 = kind2.mimeType;
                if (!"#displayName".equals(mimeType2) && !"#phoneticName".equals(mimeType2)) {
                    if ("vnd.android.cursor.item/name".equals(mimeType2)) {
                        migrateStructuredName(context, oldState, newState, kind2);
                    } else if ("vnd.android.cursor.item/postal-address_v2".equals(mimeType2)) {
                        migratePostal(oldState, newState, kind2);
                    } else if ("vnd.android.cursor.item/contact_event".equals(mimeType2)) {
                        migrateEvent(oldState, newState, kind2, null);
                    } else if (sGenericMimeTypesWithoutTypeSupport.contains(mimeType2)) {
                        migrateGenericWithoutTypeColumn(oldState, newState, kind2);
                    } else if (sGenericMimeTypesWithTypeSupport.contains(mimeType2)) {
                        migrateGenericWithTypeColumn(oldState, newState, kind2);
                    } else {
                        throw new IllegalStateException("Unexpected editable mime-type: " + mimeType2);
                    }
                }
            }
        }
    }

    private static ArrayList<ValuesDelta> ensureEntryMaxSize(RawContactDelta newState, DataKind kind, ArrayList<ValuesDelta> mimeEntries) {
        if (mimeEntries == null) {
            return null;
        }
        int typeOverallMax = kind.typeOverallMax;
        if (typeOverallMax >= 0 && mimeEntries.size() > typeOverallMax) {
            ArrayList<ValuesDelta> newMimeEntries = new ArrayList<>(typeOverallMax);
            for (int i = 0; i < typeOverallMax; i++) {
                newMimeEntries.add(mimeEntries.get(i));
            }
            mimeEntries = newMimeEntries;
        }
        return mimeEntries;
    }

    public static void migrateStructuredName(Context context, RawContactDelta oldState, RawContactDelta newState, DataKind newDataKind) {
        ContentValues values = oldState.getPrimaryEntry("vnd.android.cursor.item/name").getAfter();
        if (values != null) {
            boolean supportDisplayName = false;
            boolean supportPhoneticFullName = false;
            boolean supportPhoneticFamilyName = false;
            boolean supportPhoneticMiddleName = false;
            boolean supportPhoneticGivenName = false;
            for (AccountType.EditField editField : newDataKind.fieldList) {
                if ("data1".equals(editField.column)) {
                    supportDisplayName = true;
                }
                if ("#phoneticName".equals(editField.column)) {
                    supportPhoneticFullName = true;
                }
                if ("data9".equals(editField.column)) {
                    supportPhoneticFamilyName = true;
                }
                if ("data8".equals(editField.column)) {
                    supportPhoneticMiddleName = true;
                }
                if ("data7".equals(editField.column)) {
                    supportPhoneticGivenName = true;
                }
            }
            String displayName = values.getAsString("data1");
            if (!TextUtils.isEmpty(displayName)) {
                if (!supportDisplayName) {
                    NameConverter.displayNameToStructuredName(context, displayName, values);
                    values.remove("data1");
                }
            } else if (supportDisplayName) {
                values.put("data1", NameConverter.structuredNameToDisplayName(context, values));
                String[] arr$ = NameConverter.STRUCTURED_NAME_FIELDS;
                for (String field : arr$) {
                    values.remove(field);
                }
            }
            String phoneticFullName = values.getAsString("#phoneticName");
            if (!TextUtils.isEmpty(phoneticFullName)) {
                if (!supportPhoneticFullName) {
                    StructuredNameDataItem tmpItem = NameConverter.parsePhoneticName(phoneticFullName, null);
                    values.remove("#phoneticName");
                    if (supportPhoneticFamilyName) {
                        values.put("data9", tmpItem.getPhoneticFamilyName());
                    } else {
                        values.remove("data9");
                    }
                    if (supportPhoneticMiddleName) {
                        values.put("data8", tmpItem.getPhoneticMiddleName());
                    } else {
                        values.remove("data8");
                    }
                    if (supportPhoneticGivenName) {
                        values.put("data7", tmpItem.getPhoneticGivenName());
                    } else {
                        values.remove("data7");
                    }
                }
            } else {
                if (supportPhoneticFullName) {
                    values.put("#phoneticName", NameConverter.buildPhoneticName(values.getAsString("data9"), values.getAsString("data8"), values.getAsString("data7")));
                }
                if (!supportPhoneticFamilyName) {
                    values.remove("data9");
                }
                if (!supportPhoneticMiddleName) {
                    values.remove("data8");
                }
                if (!supportPhoneticGivenName) {
                    values.remove("data7");
                }
            }
            newState.addEntry(ValuesDelta.fromAfter(values));
        }
    }

    public static void migratePostal(RawContactDelta oldState, RawContactDelta newState, DataKind newDataKind) {
        String[] structuredData;
        int defaultType;
        ArrayList<ValuesDelta> mimeEntries = ensureEntryMaxSize(newState, newDataKind, oldState.getMimeEntries("vnd.android.cursor.item/postal-address_v2"));
        if (mimeEntries != null && !mimeEntries.isEmpty()) {
            boolean supportFormattedAddress = false;
            boolean supportStreet = false;
            String firstColumn = newDataKind.fieldList.get(0).column;
            for (AccountType.EditField editField : newDataKind.fieldList) {
                if ("data1".equals(editField.column)) {
                    supportFormattedAddress = true;
                }
                if ("data4".equals(editField.column)) {
                    supportStreet = true;
                }
            }
            Set<Integer> supportedTypes = new HashSet<>();
            if (newDataKind.typeList != null && !newDataKind.typeList.isEmpty()) {
                for (AccountType.EditType editType : newDataKind.typeList) {
                    supportedTypes.add(Integer.valueOf(editType.rawValue));
                }
            }
            for (ValuesDelta entry : mimeEntries) {
                ContentValues values = entry.getAfter();
                if (values != null) {
                    Integer oldType = values.getAsInteger("data2");
                    if (!supportedTypes.contains(oldType)) {
                        if (newDataKind.defaultValues != null) {
                            defaultType = newDataKind.defaultValues.getAsInteger("data2").intValue();
                        } else {
                            defaultType = newDataKind.typeList.get(0).rawValue;
                        }
                        values.put("data2", Integer.valueOf(defaultType));
                        if (oldType != null && oldType.intValue() == 0) {
                            values.remove("data3");
                        }
                    }
                    String formattedAddress = values.getAsString("data1");
                    if (!TextUtils.isEmpty(formattedAddress)) {
                        if (!supportFormattedAddress) {
                            values.remove("data1");
                            if (supportStreet) {
                                values.put("data4", formattedAddress);
                            } else {
                                values.put(firstColumn, formattedAddress);
                            }
                        }
                    } else if (supportFormattedAddress) {
                        boolean useJapaneseOrder = Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage());
                        if (useJapaneseOrder) {
                            structuredData = new String[]{values.getAsString("data10"), values.getAsString("data9"), values.getAsString("data8"), values.getAsString("data7"), values.getAsString("data6"), values.getAsString("data4"), values.getAsString("data5")};
                        } else {
                            structuredData = new String[]{values.getAsString("data5"), values.getAsString("data4"), values.getAsString("data6"), values.getAsString("data7"), values.getAsString("data8"), values.getAsString("data9"), values.getAsString("data10")};
                        }
                        StringBuilder builder = new StringBuilder();
                        String[] arr$ = structuredData;
                        for (String elem : arr$) {
                            if (!TextUtils.isEmpty(elem)) {
                                builder.append(elem + "\n");
                            }
                        }
                        values.put("data1", builder.toString());
                        values.remove("data5");
                        values.remove("data4");
                        values.remove("data6");
                        values.remove("data7");
                        values.remove("data8");
                        values.remove("data9");
                        values.remove("data10");
                    }
                    newState.addEntry(ValuesDelta.fromAfter(values));
                }
            }
        }
    }

    public static void migrateEvent(RawContactDelta oldState, RawContactDelta newState, DataKind newDataKind, Integer defaultYear) {
        ArrayList<ValuesDelta> mimeEntries = ensureEntryMaxSize(newState, newDataKind, oldState.getMimeEntries("vnd.android.cursor.item/contact_event"));
        if (mimeEntries != null && !mimeEntries.isEmpty()) {
            SparseArray<AccountType.EventEditType> allowedTypes = new SparseArray<>();
            for (AccountType.EditType editType : newDataKind.typeList) {
                allowedTypes.put(editType.rawValue, (AccountType.EventEditType) editType);
            }
            for (ValuesDelta entry : mimeEntries) {
                ContentValues values = entry.getAfter();
                if (values != null) {
                    String dateString = values.getAsString("data1");
                    Integer type = values.getAsInteger("data2");
                    if (type != null && allowedTypes.indexOfKey(type.intValue()) >= 0 && !TextUtils.isEmpty(dateString)) {
                        AccountType.EventEditType suitableType = allowedTypes.get(type.intValue());
                        ParsePosition position = new ParsePosition(0);
                        boolean yearOptional = false;
                        Date date = CommonDateUtils.DATE_AND_TIME_FORMAT.parse(dateString, position);
                        if (date == null) {
                            yearOptional = true;
                            date = CommonDateUtils.NO_YEAR_DATE_FORMAT.parse(dateString, position);
                        }
                        if (date != null && yearOptional && !suitableType.isYearOptional()) {
                            Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
                            if (defaultYear == null) {
                                defaultYear = Integer.valueOf(calendar.get(1));
                            }
                            calendar.setTime(date);
                            int month = calendar.get(2);
                            int day = calendar.get(5);
                            calendar.set(defaultYear.intValue(), month, day, 8, 0, 0);
                            values.put("data1", CommonDateUtils.FULL_DATE_FORMAT.format(calendar.getTime()));
                        }
                        newState.addEntry(ValuesDelta.fromAfter(values));
                    }
                }
            }
        }
    }

    public static void migrateGenericWithoutTypeColumn(RawContactDelta oldState, RawContactDelta newState, DataKind newDataKind) {
        ArrayList<ValuesDelta> mimeEntries = ensureEntryMaxSize(newState, newDataKind, oldState.getMimeEntries(newDataKind.mimeType));
        if (mimeEntries != null && !mimeEntries.isEmpty()) {
            for (ValuesDelta entry : mimeEntries) {
                ContentValues values = entry.getAfter();
                if (values != null) {
                    newState.addEntry(ValuesDelta.fromAfter(values));
                }
            }
        }
    }

    public static void migrateGenericWithTypeColumn(RawContactDelta oldState, RawContactDelta newState, DataKind newDataKind) {
        Integer typeForNewAccount;
        int specificMax;
        ArrayList<ValuesDelta> mimeEntries = oldState.getMimeEntries(newDataKind.mimeType);
        if (mimeEntries != null && !mimeEntries.isEmpty()) {
            Integer defaultType = null;
            if (newDataKind.defaultValues != null) {
                defaultType = newDataKind.defaultValues.getAsInteger("data2");
            }
            Set<Integer> allowedTypes = new HashSet<>();
            SparseIntArray typeSpecificMaxMap = new SparseIntArray();
            if (defaultType != null) {
                allowedTypes.add(defaultType);
                typeSpecificMaxMap.put(defaultType.intValue(), -1);
            }
            if (!"vnd.android.cursor.item/im".equals(newDataKind.mimeType) && newDataKind.typeList != null && !newDataKind.typeList.isEmpty()) {
                for (AccountType.EditType editType : newDataKind.typeList) {
                    allowedTypes.add(Integer.valueOf(editType.rawValue));
                    typeSpecificMaxMap.put(editType.rawValue, editType.specificMax);
                }
                if (defaultType == null) {
                    defaultType = Integer.valueOf(newDataKind.typeList.get(0).rawValue);
                }
            }
            if (defaultType == null) {
                Log.w(TAG, "Default type isn't available for mimetype " + newDataKind.mimeType);
            }
            int typeOverallMax = newDataKind.typeOverallMax;
            SparseIntArray currentEntryCount = new SparseIntArray();
            int totalCount = 0;
            for (ValuesDelta entry : mimeEntries) {
                if (typeOverallMax == -1 || totalCount < typeOverallMax) {
                    ContentValues values = entry.getAfter();
                    if (values != null) {
                        Integer oldType = entry.getAsInteger("data2");
                        if (!allowedTypes.contains(oldType)) {
                            if (defaultType != null) {
                                typeForNewAccount = Integer.valueOf(defaultType.intValue());
                                values.put("data2", Integer.valueOf(defaultType.intValue()));
                                if (oldType != null && oldType.intValue() == 0) {
                                    values.remove("data3");
                                }
                            } else {
                                typeForNewAccount = null;
                                values.remove("data2");
                            }
                        } else {
                            typeForNewAccount = oldType;
                        }
                        if (typeForNewAccount != null && (specificMax = typeSpecificMaxMap.get(typeForNewAccount.intValue(), 0)) >= 0) {
                            int currentCount = currentEntryCount.get(typeForNewAccount.intValue(), 0);
                            if (currentCount < specificMax) {
                                currentEntryCount.put(typeForNewAccount.intValue(), currentCount + 1);
                            }
                        }
                        newState.addEntry(ValuesDelta.fromAfter(values));
                        totalCount++;
                    }
                } else {
                    return;
                }
            }
        }
    }
}
