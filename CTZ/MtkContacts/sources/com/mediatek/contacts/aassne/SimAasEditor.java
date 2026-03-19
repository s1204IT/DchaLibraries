package com.mediatek.contacts.aassne;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.dataitem.DataKind;
import com.google.android.collect.Lists;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.phb.AlphaTag;
import com.mediatek.provider.MtkContactsContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SimAasEditor {
    private Context mContext;
    private ArrayList<Anr> mOldAnrsList = new ArrayList<>();
    private ArrayList<Anr> mAnrsList = new ArrayList<>();
    private ArrayList<Anr> mCopyAnrList = null;
    private ArrayList<AasIndicatorNamePair> mOldAasIndNameList = null;
    private Uri mCopyUri = null;
    private int mInsertFlag = 0;
    private int mCopyCount = 0;
    private ArrayList<Anr> additionalArray = new ArrayList<>();

    public SimAasEditor(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    public void setCurrentSubId(int i) {
        Log.d("SimAasEditor", "[setCurrentSubId] subId: = " + i);
        SimAasSneUtils.setCurrentSubId(i);
    }

    public void updatePhoneType(int i, DataKind dataKind) {
        if (dataKind.typeList == null) {
            dataKind.typeList = Lists.newArrayList();
        } else {
            dataKind.typeList.clear();
        }
        if (PhbInfoUtils.getUsimAasCount(i) > 0) {
            List<AlphaTag> aas = SimAasSneUtils.getAAS(i);
            dataKind.typeList.add(new AccountType.EditType(101, R.string.aas_phone_type_none).setSpecificMax(-1));
            for (AlphaTag alphaTag : aas) {
                int recordIndex = alphaTag.getRecordIndex();
                Log.d("SimAasEditor", "[updatePhoneType] label=" + Log.anonymize(alphaTag.getAlphaTag()));
                dataKind.typeList.add(new AccountType.EditType(101, ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(101)).setSpecificMax(-1).setCustomColumn(MtkContactsContract.Aas.buildIndicator(i, recordIndex)));
            }
            dataKind.typeList.add(new AccountType.EditType(0, ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(0)).setSpecificMax(-1));
            Log.d("SimAasEditor", "[updatePhoneType] subId = " + i + " specificMax=-1");
        }
        dataKind.fieldList = Lists.newArrayList();
        dataKind.fieldList.add(new AccountType.EditField("data1", this.mContext.getResources().getIdentifier("local_phone_account", "string", "com.android.contacts"), 3));
    }

    private void ensureKindExists(RawContactDelta rawContactDelta, String str, DataKind dataKind, int i) {
        int i2;
        if (rawContactDelta == null) {
            Log.w("SimAasEditor", "[ensureKindExists] state is null,return!");
            return;
        }
        if (dataKind != null) {
            ArrayList<ValuesDelta> mimeEntries = rawContactDelta.getMimeEntries(str);
            int usimAnrCount = PhbInfoUtils.getUsimAnrCount(i);
            removeAnrFieldIfNotSupportAnr(rawContactDelta, mimeEntries, usimAnrCount);
            int i3 = 0;
            if (mimeEntries != null && mimeEntries.size() == usimAnrCount + 1) {
                Iterator<ValuesDelta> it = mimeEntries.iterator();
                while (it.hasNext()) {
                    Integer asInteger = it.next().getAsInteger("is_additional_number");
                    if (asInteger != null && asInteger.intValue() == 1) {
                        i3++;
                    }
                }
                Log.d("SimAasEditor", "[ensureKindExists] size=" + mimeEntries.size() + ",slotAnrSize=" + usimAnrCount + ",anrSize=" + i3);
                if (i3 < usimAnrCount && mimeEntries.size() > 1) {
                    for (int i4 = 1; i4 < mimeEntries.size(); i4++) {
                        mimeEntries.get(i4).put("is_additional_number", 1);
                    }
                    return;
                }
                return;
            }
            if (mimeEntries == null || mimeEntries.isEmpty()) {
                Log.d("SimAasEditor", "[ensureKindExists] Empty, insert primary: and anr:" + usimAnrCount);
                ValuesDelta valuesDeltaInsertChild = RawContactModifier.insertChild(rawContactDelta, dataKind);
                if (dataKind.mimeType.equals("vnd.android.cursor.item/phone_v2")) {
                    valuesDeltaInsertChild.setFromTemplate(true);
                }
                while (i3 < usimAnrCount) {
                    RawContactModifier.insertChild(rawContactDelta, dataKind).put("is_additional_number", 1);
                    i3++;
                }
                return;
            }
            if (mimeEntries != null) {
                Iterator<ValuesDelta> it2 = mimeEntries.iterator();
                i2 = 0;
                while (it2.hasNext()) {
                    Integer asInteger2 = it2.next().getAsInteger("is_additional_number");
                    if (asInteger2 != null && asInteger2.intValue() == 1) {
                        i2++;
                    } else {
                        i3++;
                    }
                }
            } else {
                i2 = 0;
            }
            Log.d("SimAasEditor", "[ensureKindExists] pnrSize=" + i3 + ", anrSize=" + i2 + ",slotAnrSize: " + usimAnrCount);
            if (i3 < 1) {
                RawContactModifier.insertChild(rawContactDelta, dataKind);
            }
            while (i2 < usimAnrCount) {
                RawContactModifier.insertChild(rawContactDelta, dataKind).put("is_additional_number", 1);
                i2++;
            }
        }
    }

    public boolean ensurePhoneKindForEditor(AccountType accountType, int i, RawContactDelta rawContactDelta) {
        SimAasSneUtils.setCurrentSubId(i);
        if (AccountTypeUtils.isUsimOrCsim(rawContactDelta.getAccountType())) {
            DataKind kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/phone_v2");
            if (kindForMimetype != null) {
                updatePhoneType(i, kindForMimetype);
            }
            ensureKindExists(rawContactDelta, "vnd.android.cursor.item/phone_v2", kindForMimetype, i);
            return true;
        }
        return true;
    }

    public void updatePhoneKind(AccountType accountType, int i) {
        DataKind kindForMimetype;
        if (accountType != null && AccountTypeUtils.isUsimOrCsim(accountType.accountType) && (kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/phone_v2")) != null) {
            updatePhoneType(i, kindForMimetype);
        }
    }

    public boolean handleLabel(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta rawContactDelta) {
        String accountType = rawContactDelta.getAccountType();
        if (AccountTypeUtils.isSimOrRuim(accountType) && AccountTypeUtils.isPhoneNumType(dataKind.mimeType)) {
            Log.d("SimAasEditor", "[handleLabel] hide label for sim card or Ruim");
            return true;
        }
        if (isPrimaryNumber(dataKind, valuesDelta, rawContactDelta)) {
            Log.d("SimAasEditor", "[handleLabel] hide label for primary number.");
            return true;
        }
        if (AccountTypeUtils.isUsimOrCsim(accountType) && "vnd.android.cursor.item/email_v2".equals(dataKind.mimeType)) {
            Log.d("SimAasEditor", "[handleLabel] hide label for email");
            return true;
        }
        return false;
    }

    public boolean isPrimaryNumber(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta rawContactDelta) {
        return AccountTypeUtils.isUsimOrCsim(rawContactDelta.getAccountType()) && AccountTypeUtils.isPhoneNumType(dataKind.mimeType) && !"1".equals(valuesDelta.getAsString("is_additional_number"));
    }

    public boolean updateView(RawContactDelta rawContactDelta, View view, ValuesDelta valuesDelta, int i) {
        int i2 = (valuesDelta != null && isAdditionalNumber(valuesDelta)) ? 1 : 0;
        String accountType = rawContactDelta.getAccountType();
        Log.sensitive("SimAasEditor", "[updateView] type=" + i2 + ",action=" + i + ",accountType=" + accountType);
        switch (i) {
            case 1:
                if (AccountTypeUtils.isUsimOrCsim(accountType)) {
                    if (view instanceof TextView) {
                        if (i2 == 0) {
                            ((TextView) view).setHint(R.string.aas_phone_primary);
                        } else if (i2 == 1) {
                            ((TextView) view).setHint(R.string.aas_phone_additional);
                        }
                    } else {
                        Log.e("SimAasEditor", "[updateView]  VIEW_UPDATE_HINT but view is not a TextView");
                    }
                }
            case 2:
                if (AccountTypeUtils.isUsimOrCsim(accountType)) {
                    view.setVisibility(8);
                }
                return false;
            case 3:
                return AccountTypeUtils.isUsimOrCsim(accountType);
        }
    }

    public int getMaxEmptyEditors(RawContactDelta rawContactDelta, String str) {
        String accountType = rawContactDelta.getAccountType();
        Log.sensitive("SimAasEditor", "[getMaxEmptyEditors] accountType=" + accountType + ",mimeType=" + str);
        if (AccountTypeUtils.isUsimOrCsim(accountType) && AccountTypeUtils.isPhoneNumType(str)) {
            int usimAnrCount = PhbInfoUtils.getUsimAnrCount(SimAasSneUtils.getCurSubId()) + 1;
            Log.d("SimAasEditor", "[getMaxEmptyEditors] max=" + usimAnrCount);
            return usimAnrCount;
        }
        Log.d("SimAasEditor", "[getMaxEmptyEditors] max= 1");
        return 1;
    }

    public String getCustomTypeLabel(int i, String str) {
        if (AccountTypeUtils.isUsimOrCsim(SimAasSneUtils.getCurAccount()) && AccountTypeUtils.isAasPhoneType(i) && !TextUtils.isEmpty(str)) {
            CharSequence typeLabel = MtkContactsContract.CommonDataKinds.Phone.getTypeLabel(this.mContext, i, str);
            Log.d("SimAasEditor", "[getCustomTypeLabel] index" + str + " tag=" + Log.anonymize(typeLabel));
            return typeLabel.toString();
        }
        return null;
    }

    public boolean rebuildLabelSelection(RawContactDelta rawContactDelta, Spinner spinner, ArrayAdapter<AccountType.EditType> arrayAdapter, AccountType.EditType editType, DataKind dataKind) {
        if (editType == null || dataKind == null) {
            spinner.setSelection(arrayAdapter.getPosition(editType));
            return false;
        }
        if (AccountTypeUtils.isUsimOrCsim(rawContactDelta.getAccountType()) && AccountTypeUtils.isPhoneNumType(dataKind.mimeType) && AccountTypeUtils.isAasPhoneType(editType.rawValue)) {
            for (int i = 0; i < arrayAdapter.getCount(); i++) {
                AccountType.EditType item = arrayAdapter.getItem(i);
                if (item.customColumn != null && item.customColumn.equals(editType.customColumn)) {
                    spinner.setSelection(i);
                    Log.d("SimAasEditor", "[rebuildLabelSelection] position=" + i);
                    return true;
                }
            }
        }
        spinner.setSelection(arrayAdapter.getPosition(editType));
        return false;
    }

    public boolean onTypeSelectionChange(RawContactDelta rawContactDelta, ValuesDelta valuesDelta, DataKind dataKind, ArrayAdapter<AccountType.EditType> arrayAdapter, AccountType.EditType editType, AccountType.EditType editType2, Context context) {
        String accountType = rawContactDelta.getAccountType();
        Log.sensitive("SimAasEditor", "[onTypeSelectionChange] Entry: accountType= " + accountType);
        if (!AccountTypeUtils.isUsimOrCsim(accountType) || !AccountTypeUtils.isPhoneNumType(dataKind.mimeType)) {
            return false;
        }
        if (editType2 == editType) {
            Log.i("SimAasEditor", "[onTypeSelectionChange] same select");
            return true;
        }
        if (editType.rawValue == 0) {
            Log.i("SimAasEditor", "[onTypeSelectionChange] Custom Selected");
            onTypeSelectionChange(context, editType.rawValue);
        } else {
            Log.i("SimAasEditor", "[onTypeSelectionChange] different Selected");
            valuesDelta.put(dataKind.typeColumn, Integer.toString(editType.rawValue));
            updatemEntryValue(valuesDelta, editType);
        }
        return true;
    }

    private void onTypeSelectionChange(Context context, int i) {
        Log.d("SimAasEditor", "[onTypeSelectionChange] private");
        if (AccountTypeUtils.isUsimOrCsim(SimAasSneUtils.getCurAccount())) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setAction("com.mediatek.contacts.action.EDIT_AAS");
            int curSubId = SimAasSneUtils.getCurSubId();
            Log.d("SimAasEditor", "[onTypeSelectionChange] internal: subId to fill in slot_key= " + curSubId);
            intent.putExtra("subId", curSubId);
            Log.d("SimAasEditor", "[onTypeSelectionChange] call for startActivity");
            if (context != null) {
                context.startActivity(intent);
            } else {
                this.mContext.startActivity(intent);
            }
        }
    }

    public AccountType.EditType getCurrentType(ValuesDelta valuesDelta, DataKind dataKind, int i) {
        if (AccountTypeUtils.isAasPhoneType(i)) {
            return getAasEditType(valuesDelta, dataKind, i);
        }
        return RawContactModifier.getType(dataKind, i);
    }

    public static boolean updatemEntryValue(ValuesDelta valuesDelta, AccountType.EditType editType) {
        if (AccountTypeUtils.isAasPhoneType(editType.rawValue)) {
            valuesDelta.put("data3", editType.customColumn);
            return true;
        }
        return false;
    }

    private boolean buildAnrOperation(String str, ArrayList<ContentProviderOperation> arrayList, ArrayList arrayList2, int i) {
        if (AccountTypeUtils.isUsimOrCsim(str)) {
            Iterator it = arrayList2.iterator();
            while (it.hasNext()) {
                Anr anr = (Anr) it.next();
                if (!TextUtils.isEmpty(anr.additionalNumber)) {
                    Log.d("SimAasEditor", "[buildAnrOperation] additionalNumber=" + Log.anonymize(anr.additionalNumber) + " aas=" + anr.aasIndex);
                    ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                    builderNewInsert.withValueBackReference("raw_contact_id", i);
                    builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
                    builderNewInsert.withValue("data2", 101);
                    builderNewInsert.withValue("data1", anr.additionalNumber);
                    builderNewInsert.withValue("data3", SimAasSneUtils.buildAASIndicator(anr.aasIndex, SimAasSneUtils.getCurSubId()));
                    builderNewInsert.withValue("is_additional_number", 1);
                    arrayList.add(builderNewInsert.build());
                }
            }
            return true;
        }
        return false;
    }

    public void updateValuesforCopy(Uri uri, int i, String str, ContentValues contentValues) {
        Log.d("SimAasEditor", "[updateValuesforCopy] Entry");
        SimAasSneUtils.setCurrentSubId(i);
        if (!AccountTypeUtils.isUsimOrCsim(str)) {
            Log.d("SimAasEditor", "[updateValuesforCopy] return account is not USIM");
            return;
        }
        this.mInsertFlag = 3;
        if (this.mCopyCount == 0) {
            ArrayList arrayList = new ArrayList();
            Cursor cursorQuery = this.mContext.getContentResolver().query(uri, new String[]{"_id", "mimetype", "data1", "is_additional_number", "data2", "data3"}, null, null, null);
            if (cursorQuery != null && cursorQuery.moveToFirst()) {
                do {
                    if ("vnd.android.cursor.item/phone_v2".equals(cursorQuery.getString(1))) {
                        cursorQuery.getString(2);
                        Anr anr = new Anr();
                        anr.additionalNumber = cursorQuery.getString(2);
                        Log.d("SimAasEditor", "[updateValuesforCopy] simAnrNum:" + Log.anonymize(anr.additionalNumber));
                        anr.aasIndex = this.mContext.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(cursorQuery.getInt(4)));
                        Log.d("SimAasEditor", "[updateValuesforCopy] aasIndex:" + Log.anonymize(anr.aasIndex));
                        if (cursorQuery.getInt(3) == 1) {
                            this.additionalArray.add(anr);
                        } else {
                            arrayList.add(anr);
                        }
                    }
                } while (cursorQuery.moveToNext());
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            if (arrayList.size() > 0) {
                arrayList.remove(0);
            }
            this.additionalArray.addAll(arrayList);
            this.mCopyCount = this.additionalArray.size();
        } else {
            this.additionalArray.remove(0);
            this.mCopyCount--;
        }
        int usimAnrCount = PhbInfoUtils.getUsimAnrCount(i);
        if (this.additionalArray.size() <= usimAnrCount) {
            usimAnrCount = this.additionalArray.size();
        }
        this.mCopyAnrList = new ArrayList<>();
        for (int i2 = 0; i2 < usimAnrCount; i2++) {
            Anr anrRemove = this.additionalArray.remove(0);
            int aasIndexByName = SimAasSneUtils.getAasIndexByName(anrRemove.aasIndex, i);
            Log.d("SimAasEditor", "[updateValuesforCopy] additionalNumber:" + Log.anonymize(anrRemove.additionalNumber));
            anrRemove.additionalNumber = TextUtils.isEmpty(anrRemove.additionalNumber) ? "" : anrRemove.additionalNumber.replace("-", "");
            Log.d("SimAasEditor", "[updateValuesforCopy] aasIndex:" + aasIndexByName);
            contentValues.put("anr" + SimAasSneUtils.getSuffix(i2), PhoneNumberUtils.stripSeparators(anrRemove.additionalNumber));
            contentValues.put("aas" + SimAasSneUtils.getSuffix(i2), Integer.valueOf(aasIndexByName));
            this.mCopyAnrList.add(anrRemove);
            this.mCopyCount = this.mCopyCount - 1;
        }
    }

    public boolean cursorColumnToBuilder(Cursor cursor, ContentProviderOperation.Builder builder, String str, String str2, int i, int i2) {
        return generateDataBuilder(null, cursor, builder, cursor.getColumnNames(), str, str2, i, i2);
    }

    public boolean generateDataBuilder(Context context, Cursor cursor, ContentProviderOperation.Builder builder, String[] strArr, String str, String str2, int i, int i2) {
        if (AccountTypeUtils.isAccountTypeIccCard(str) && AccountTypeUtils.isPhoneNumType(str2)) {
            String string = cursor.getString(cursor.getColumnIndex("is_additional_number"));
            if ("data2".equals(strArr[i2])) {
                Log.d("SimAasEditor", "[generateDataBuilder] isAnr:" + string);
                if ("1".equals(string)) {
                    builder.withValue("data2", 7);
                    Log.d("SimAasEditor", "[generateDataBuilder] DATA2 to be TYPE_OTHER ");
                } else {
                    builder.withValue("data2", 2);
                    Log.d("SimAasEditor", "[generateDataBuilder] DATA2 to be TYPE_MOBILE ");
                }
                return true;
            }
            if ("data3".equals(strArr[i2])) {
                Log.d("SimAasEditor", "[generateDataBuilder] DATA3 to be null");
                builder.withValue("data3", null);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean checkAasEntry(ContentValues contentValues) {
        Log.sensitive("SimAasEditor", "[checkAasEntry] para = " + contentValues);
        if (isAdditionalNumber(contentValues)) {
            return true;
        }
        return false;
    }

    public String getSubheaderString(int i, int i2) {
        Log.d("SimAasEditor", "[getSubheaderString] subId = " + i);
        if (i == -1) {
            Log.d("SimAasEditor", "[getSubheaderString] Phone contact");
            return null;
        }
        if (AccountTypeUtils.isUsimOrCsim(AccountTypeUtils.getAccountTypeBySub(i))) {
            if (AccountTypeUtils.isAasPhoneType(i2)) {
                Log.d("SimAasEditor", "[getSubheaderString] USIM additional number");
                return this.mContext.getResources().getString(R.string.aas_phone_additional);
            }
            Log.d("SimAasEditor", "[getSubheaderString] USIM primary number ");
            return this.mContext.getResources().getString(R.string.aas_phone_primary);
        }
        Log.d("SimAasEditor", "[getSubheaderString] Account is SIM ");
        return null;
    }

    public boolean updateValues(Intent intent, int i, ContentValues contentValues) {
        Log.d("SimAasEditor", "[updateValues] Entry.");
        new ArrayList();
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("simData");
        new ArrayList();
        ArrayList parcelableArrayListExtra2 = intent.getParcelableArrayListExtra("oldsimData");
        String asString = ((RawContactDelta) parcelableArrayListExtra.get(0)).getValues().getAsString("account_type");
        SimAasSneUtils.setCurrentSubId(i);
        if (!AccountTypeUtils.isUsimOrCsim(asString)) {
            Log.d("SimAasEditor", "[updateValues] Account type is not USIM.");
            return false;
        }
        if (parcelableArrayListExtra2 == null) {
            Log.d("SimAasEditor", "[updateValues] for new contact.");
            this.mInsertFlag = 1;
            prepareNewAnrList(intent);
            Log.d("SimAasEditor", "[updateValues] for new contact Newanrlist filled");
            return buildAnrInsertValues(asString, contentValues, this.mAnrsList);
        }
        Log.d("SimAasEditor", "[updateValues] for Edit contact.");
        this.mInsertFlag = 2;
        prepareNewAnrList(intent);
        Log.d("SimAasEditor", "[updateValues] for New anrlist filled");
        prepareOldAnrList(intent);
        Log.d("SimAasEditor", "[updateValues] for Old anrlist filled");
        return buildAnrUpdateValues(asString, contentValues, this.mAnrsList);
    }

    private void prepareNewAnrList(Intent intent) {
        new ArrayList();
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("simData");
        this.mAnrsList.clear();
        if (parcelableArrayListExtra == null) {
            return;
        }
        int size = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().size();
        for (int i = 0; i < size; i++) {
            if ("vnd.android.cursor.item/phone_v2".equals(((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i).getAsString("mimetype"))) {
                ContentValues contentValues = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i);
                if (isAdditionalNumber(contentValues)) {
                    Anr anr = new Anr();
                    anr.additionalNumber = replaceCharOnNumber(contentValues.getAsString("data1"));
                    anr.aasIndex = contentValues.getAsString("data3");
                    Log.d("SimAasEditor", "[prepareNewAnrList] additionalNumber:" + Log.anonymize(anr.additionalNumber) + ",aasIndex:" + anr.aasIndex);
                    this.mAnrsList.add(anr);
                }
            }
        }
    }

    private void prepareOldAnrList(Intent intent) {
        new ArrayList();
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("oldsimData");
        this.mOldAnrsList.clear();
        if (parcelableArrayListExtra == null) {
            return;
        }
        int size = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().size();
        for (int i = 0; i < size; i++) {
            if ("vnd.android.cursor.item/phone_v2".equals(((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i).getAsString("mimetype"))) {
                ContentValues contentValues = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i);
                if (isAdditionalNumber(contentValues)) {
                    Anr anr = new Anr();
                    anr.additionalNumber = replaceCharOnNumber(contentValues.getAsString("data1"));
                    anr.aasIndex = contentValues.getAsString("data3");
                    Log.d("SimAasEditor", "[prepareOldAnrList] additionalNumber:" + Log.anonymize(anr.additionalNumber) + ",aasIndex: " + anr.aasIndex);
                    anr.id = (long) contentValues.getAsInteger("_id").intValue();
                    this.mOldAnrsList.add(anr);
                }
            }
        }
    }

    private boolean buildAnrInsertValues(String str, ContentValues contentValues, ArrayList arrayList) {
        String strStripSeparators;
        int i = 0;
        if (!AccountTypeUtils.isUsimOrCsim(str)) {
            return false;
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            Anr anr = (Anr) it.next();
            String str2 = TextUtils.isEmpty(anr.additionalNumber) ? "" : anr.additionalNumber;
            if (!TextUtils.isEmpty(str2)) {
                strStripSeparators = PhoneNumberUtils.stripSeparators(str2);
                if (!Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(strStripSeparators))) {
                    Log.d("SimAasEditor", "[buildAnrInsertValues] additionalNumber Invalid ");
                }
                Log.d("SimAasEditor", "[buildAnrInsertValues] additionalNumber updated : " + Log.anonymize(strStripSeparators));
            } else {
                strStripSeparators = str2;
            }
            contentValues.put("anr" + SimAasSneUtils.getSuffix(i), strStripSeparators);
            contentValues.put("aas" + SimAasSneUtils.getSuffix(i), Integer.valueOf(SimAasSneUtils.getAasIndexFromIndicator(anr.aasIndex)));
            i++;
            Log.d("SimAasEditor", "[buildAnrInsertValues] aasIndex=" + anr.aasIndex + ", additionalNumber=" + Log.anonymize(str2));
        }
        return true;
    }

    private boolean buildAnrUpdateValues(String str, ContentValues contentValues, ArrayList<Anr> arrayList) {
        int i = 0;
        if (!AccountTypeUtils.isUsimOrCsim(str)) {
            return false;
        }
        for (Anr anr : arrayList) {
            Log.d("SimAasEditor", "[buildAnrUpdateValues] additionalNumber : " + Log.anonymize(anr.additionalNumber));
            if (!TextUtils.isEmpty(anr.additionalNumber)) {
                String strStripSeparators = PhoneNumberUtils.stripSeparators(anr.additionalNumber);
                if (!Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(strStripSeparators))) {
                    Log.d("SimAasEditor", "[buildAnrUpdateValues] additionalNumber Invalid");
                }
                Log.d("SimAasEditor", "[buildAnrUpdateValues] additionalNumber updated: " + Log.anonymize(strStripSeparators));
                contentValues.put("newAnr" + SimAasSneUtils.getSuffix(i), strStripSeparators);
                contentValues.put("aas" + SimAasSneUtils.getSuffix(i), Integer.valueOf(SimAasSneUtils.getAasIndexFromIndicator(anr.aasIndex)));
            }
            i++;
        }
        return true;
    }

    public boolean updateAdditionalNumberToDB(Intent intent, long j) {
        Log.d("SimAasEditor", "[updateAdditionalNumberToDB] Entry");
        String asString = ((RawContactDelta) intent.getParcelableArrayListExtra("simData").get(0)).getValues().getAsString("account_type");
        if (!AccountTypeUtils.isUsimOrCsim(asString)) {
            Log.d("SimAasEditor", "[updateAdditionalNumberToDB] return false, account is not USIM");
            return false;
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Log.sensitive("SimAasEditor", "[updateAdditionalNumberToDB] mAnrlist:" + this.mAnrsList + ",mOldAnrsList: " + this.mOldAnrsList);
        return updateAnrToDb(asString, contentResolver, this.mAnrsList, this.mOldAnrsList, j);
    }

    private boolean updateAnrToDb(String str, ContentResolver contentResolver, ArrayList arrayList, ArrayList arrayList2, long j) {
        if (AccountTypeUtils.isUsimOrCsim(str)) {
            String str2 = "raw_contact_id='" + j + "' AND mimetype='vnd.android.cursor.item/phone_v2' AND is_additional_number=1 AND _id=?";
            Log.sensitive("SimAasEditor", "[updateAnrInfoToDb] whereClause:" + str2);
            int size = arrayList.size();
            int size2 = arrayList2.size();
            int iMin = Math.min(size, size2);
            ContentValues contentValues = new ContentValues();
            int i = 0;
            while (i < iMin) {
                Anr anr = (Anr) arrayList.get(i);
                Anr anr2 = (Anr) arrayList2.get(i);
                contentValues.clear();
                if (!TextUtils.isEmpty(anr.additionalNumber) && !TextUtils.isEmpty(anr2.additionalNumber)) {
                    contentValues.put("data1", anr.additionalNumber);
                    contentValues.put("data2", (Integer) 101);
                    contentValues.put("data3", anr.aasIndex);
                    Log.d("SimAasEditor", "[updateAnrInfoToDb] updatedCount: " + contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, str2, new String[]{Long.toString(anr2.id)}));
                } else if (!TextUtils.isEmpty(anr.additionalNumber) && TextUtils.isEmpty(anr2.additionalNumber)) {
                    contentValues.put("raw_contact_id", Long.valueOf(j));
                    contentValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
                    contentValues.put("data1", anr.additionalNumber);
                    contentValues.put("data2", (Integer) 101);
                    contentValues.put("data3", anr.aasIndex);
                    contentValues.put("is_additional_number", (Integer) 1);
                    Log.d("SimAasEditor", "[updateAnrInfoToDb] upAdditionalUri: " + contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues));
                } else if (TextUtils.isEmpty(anr.additionalNumber)) {
                    Log.d("SimAasEditor", "[updateAnrInfoToDb] deletedCount:" + contentResolver.delete(ContactsContract.Data.CONTENT_URI, str2, new String[]{Long.toString(anr2.id)}));
                }
                i++;
            }
            while (i < size2) {
                Log.d("SimAasEditor", "[updateAnrInfoToDb] deleteAdditional:" + contentResolver.delete(ContactsContract.Data.CONTENT_URI, str2, new String[]{Long.toString(((Anr) arrayList2.get(i)).id)}));
                i++;
            }
            while (i < size) {
                Anr anr3 = (Anr) arrayList.get(i);
                contentValues.clear();
                if (!TextUtils.isEmpty(anr3.additionalNumber)) {
                    contentValues.put("raw_contact_id", Long.valueOf(j));
                    contentValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
                    contentValues.put("data1", anr3.additionalNumber);
                    contentValues.put("data2", (Integer) 101);
                    contentValues.put("data3", anr3.aasIndex);
                    contentValues.put("is_additional_number", (Integer) 1);
                    Log.d("SimAasEditor", "[updateAnrInfoToDb] insertedUri: " + contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues));
                }
                i++;
            }
            return true;
        }
        return false;
    }

    public boolean updateOperationList(AccountWithDataSet accountWithDataSet, ArrayList<ContentProviderOperation> arrayList, int i) {
        if (!AccountTypeUtils.isUsimOrCsim(accountWithDataSet.type)) {
            Log.d("SimAasEditor", "[updateOperationList] Account is not USIM so return false");
            return false;
        }
        if (this.mInsertFlag == 3) {
            if (this.mCopyAnrList != null && this.mCopyAnrList.size() > 0) {
                Log.d("SimAasEditor", "[updateOperationList] for copy ");
                boolean zBuildAnrOperation = buildAnrOperation(accountWithDataSet.type, arrayList, this.mCopyAnrList, i);
                Log.d("SimAasEditor", "[updateOperationList] result : " + zBuildAnrOperation);
                this.mCopyAnrList.clear();
                this.mCopyAnrList = null;
                return zBuildAnrOperation;
            }
            Log.d("SimAasEditor", "[updateOperationList] result false");
            return false;
        }
        if (AccountTypeUtils.isUsimOrCsim(accountWithDataSet.type)) {
            Log.d("SimAasEditor", "[updateOperationList] for Insert contact ");
            for (Anr anr : this.mAnrsList) {
                if (!TextUtils.isEmpty(anr.additionalNumber)) {
                    Log.d("SimAasEditor", "[updateOperationList] additionalNumber=" + Log.anonymize(anr.additionalNumber) + ",aas=" + anr.aasIndex);
                    ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                    builderNewInsert.withValueBackReference("raw_contact_id", i);
                    builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
                    builderNewInsert.withValue("data2", 101);
                    builderNewInsert.withValue("data1", anr.additionalNumber);
                    builderNewInsert.withValue("data3", anr.aasIndex);
                    builderNewInsert.withValue("is_additional_number", 1);
                    arrayList.add(builderNewInsert.build());
                }
            }
            Log.d("SimAasEditor", "[updateOperationList] result true");
            return true;
        }
        Log.d("SimAasEditor", "[updateOperationList] result false");
        return false;
    }

    public CharSequence getLabelForBindData(Resources resources, int i, String str, String str2, Cursor cursor, CharSequence charSequence) {
        Log.d("SimAasEditor", "[getLabelForBindData] Entry mimetype:" + str2);
        int columnIndex = cursor.getColumnIndex("indicate_phone_or_sim_contact");
        int i2 = -1;
        if (columnIndex != -1) {
            i2 = cursor.getInt(columnIndex);
        }
        if (AccountTypeUtils.isUsimOrCsim(AccountTypeUtils.getAccountTypeBySub(i2)) && str2.equals("vnd.android.cursor.item/email_v2")) {
            return "";
        }
        return getTypeLabel(i, str, (String) charSequence, i2);
    }

    private String replaceCharOnNumber(String str) {
        if (!TextUtils.isEmpty(str)) {
            Log.d("SimAasEditor", "[replaceCharOnNumber]befor replaceall number : " + Log.anonymize(str));
            String strReplaceAll = str.replaceAll("-", "").replaceAll(" ", "");
            Log.d("SimAasEditor", "[replaceCharOnNumber]after replaceall number : " + Log.anonymize(strReplaceAll));
            return strReplaceAll;
        }
        return str;
    }

    public CharSequence getTypeLabel(int i, CharSequence charSequence, String str, int i2) {
        String accountTypeBySub = AccountTypeUtils.getAccountTypeBySub(i2);
        Log.sensitive("SimAasEditor", "[getTypeLabel] subId=" + i2 + " accountType=" + accountTypeBySub);
        if (AccountTypeUtils.isSim(accountTypeBySub) || AccountTypeUtils.isRuim(accountTypeBySub)) {
            Log.d("SimAasEditor", "[getTypeLabel] SIM Account no Label.");
            return "";
        }
        if (AccountTypeUtils.isUsimOrCsim(accountTypeBySub) && AccountTypeUtils.isAasPhoneType(i)) {
            if (TextUtils.isEmpty(charSequence)) {
                Log.d("SimAasEditor", "[getTypeLabel] Empty");
                return "";
            }
            try {
                CharSequence typeLabel = MtkContactsContract.CommonDataKinds.Phone.getTypeLabel(this.mContext, i, charSequence);
                Log.d("SimAasEditor", "[getTypeLabel] label" + Log.anonymize(charSequence) + " tag=" + Log.anonymize(typeLabel));
                return typeLabel;
            } catch (NumberFormatException e) {
                Log.e("SimAasEditor", "[getTypeLabel] return label=" + ((Object) charSequence));
            }
        }
        if (AccountTypeUtils.isUsimOrCsim(accountTypeBySub) && !AccountTypeUtils.isAasPhoneType(i)) {
            Log.d("SimAasEditor", "[getTypeLabel] account is USIM but type is not additional");
            return "";
        }
        return str;
    }

    private boolean isAdditionalNumber(ValuesDelta valuesDelta) {
        Integer asInteger = valuesDelta.getAsInteger("is_additional_number");
        return asInteger != null && 1 == asInteger.intValue();
    }

    private boolean isAdditionalNumber(ContentValues contentValues) {
        Integer asInteger;
        if (contentValues != null && contentValues.containsKey("is_additional_number")) {
            asInteger = contentValues.getAsInteger("is_additional_number");
        } else {
            asInteger = null;
        }
        return asInteger != null && 1 == asInteger.intValue();
    }

    private AccountType.EditType getAasEditType(ValuesDelta valuesDelta, DataKind dataKind, int i) {
        if (i == 101) {
            String asString = valuesDelta.getAsString("data3");
            Log.d("SimAasEditor", "[getAasEditType] customColumn=" + asString);
            if (asString != null) {
                for (AccountType.EditType editType : dataKind.typeList) {
                    if (editType.rawValue == 101 && asString.equals(editType.customColumn)) {
                        return editType;
                    }
                }
            }
            return null;
        }
        Log.e("SimAasEditor", "[getAasEditType] error Not Anr.TYPE_AAS, type=" + i);
        return null;
    }

    public void ensurePhoneKindForCompactEditor(RawContactDeltaList rawContactDeltaList, int i, Context context) {
        int size = rawContactDeltaList.size();
        Log.d("SimAasEditor", "[ensurePhoneKindForCompactEditor] Entry numRawContacts= " + size);
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(this.mContext);
        for (int i2 = 0; i2 < size; i2++) {
            RawContactDelta rawContactDelta = rawContactDeltaList.get(i2);
            AccountType accountType = rawContactDelta.getAccountType(accountTypeManager);
            Log.d("SimAasEditor", "[ensurePhoneKindForCompactEditor] loop subid=" + i);
            ensurePhoneKindForEditor(accountType, i, rawContactDelta);
        }
    }

    private void removeAnrFieldIfNotSupportAnr(RawContactDelta rawContactDelta, ArrayList<ValuesDelta> arrayList, int i) {
        boolean z;
        Log.sensitive("SimAasEditor", "[removeAnrFieldIfNotSupportAnr] state:" + rawContactDelta + ",values:" + arrayList + ",slotAnrSize:" + i);
        if (rawContactDelta == null || arrayList == null) {
            Log.w("SimAasEditor", "[removeAnrFieldIfNotSupportAnr] state or value is null,return");
            return;
        }
        if (i <= 0) {
            z = false;
        } else {
            z = true;
        }
        if (!z) {
            Iterator<ValuesDelta> it = arrayList.iterator();
            while (it.hasNext()) {
                ValuesDelta next = it.next();
                Integer asInteger = next.getAsInteger("is_additional_number");
                if (asInteger != null && asInteger.intValue() == 1) {
                    Log.sensitive("SimAasEditor", "[removeAnrFieldIfNotSupportAnr] remove vaule: " + next);
                    it.remove();
                }
            }
            Log.sensitive("SimAasEditor", "[removeAnrFieldIfNotSupportAnr] after state:" + rawContactDelta);
        }
    }

    public static void removeRedundantAnrFieldForNonSimAccount(AccountType accountType, AccountType accountType2, RawContactDelta rawContactDelta, String str) {
        Log.sensitive("SimAasEditor", "[removeRedundantAnrFieldForNonSimAccount] oldAccountType:" + accountType + ",newAccountType: " + accountType2 + ",newState: " + rawContactDelta + ",mimeType:" + str);
        if (accountType2 != null && !AccountTypeUtils.isAccountTypeIccCard(accountType2.accountType)) {
            removeAnrValueDirectly(rawContactDelta.getMimeEntries(str));
        }
        Log.sensitive("SimAasEditor", "[removeRedundantAnrFieldForNonSimAccount] result newState:" + rawContactDelta);
    }

    private static void removeAnrValueDirectly(ArrayList<ValuesDelta> arrayList) {
        Log.d("SimAasEditor", "[removeAnrValueDirectly]");
        if (arrayList == null) {
            Log.w("SimAasEditor", "[removeAnrValueDirectly] null values,return!");
            return;
        }
        for (ValuesDelta valuesDelta : arrayList) {
            boolean zIsEmpty = TextUtils.isEmpty(valuesDelta.getAsString("data1"));
            Integer asInteger = valuesDelta.getAsInteger("is_additional_number");
            boolean z = asInteger != null && asInteger.intValue() == 1;
            if (zIsEmpty && z) {
                Log.sensitive("SimAasEditor", "[removeAnrFieldDirectly] remove Anr value:" + valuesDelta);
                arrayList.remove(valuesDelta);
            }
        }
    }

    private static class AasIndicatorNamePair {
        public String indicator;
        public String name;

        public AasIndicatorNamePair(String str, String str2) {
            this.indicator = str;
            this.name = str2;
        }

        public String toString() {
            return "{indicator = " + this.indicator + ", name = " + this.name + "}";
        }
    }

    public void setOldAasIndicatorAndNames(RawContactDeltaList rawContactDeltaList) {
        if (this.mOldAasIndNameList != null) {
            this.mOldAasIndNameList.clear();
            this.mOldAasIndNameList = null;
        }
        this.mOldAasIndNameList = getAasIndicatorAndNames(rawContactDeltaList);
    }

    private ArrayList<AasIndicatorNamePair> getAasIndicatorAndNames(RawContactDeltaList rawContactDeltaList) {
        ArrayList<AasIndicatorNamePair> arrayList = null;
        if (rawContactDeltaList == null) {
            return null;
        }
        ArrayList<ContentValues> contentValues = rawContactDeltaList.get(0).getContentValues();
        int size = contentValues.size();
        for (int i = 0; i < size; i++) {
            if ("vnd.android.cursor.item/phone_v2".equals(contentValues.get(i).getAsString("mimetype"))) {
                ContentValues contentValues2 = contentValues.get(i);
                if (isAdditionalNumber(contentValues2)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    String asString = contentValues2.getAsString("data3");
                    arrayList.add(new AasIndicatorNamePair(asString, SimAasSneUtils.getAASByIndicator(asString)));
                }
            }
        }
        return arrayList;
    }

    public boolean isAasNameChangedOnly(RawContactDeltaList rawContactDeltaList) {
        if (rawContactDeltaList == null || rawContactDeltaList.isEmpty() || !AccountTypeUtils.isUsimOrCsim(rawContactDeltaList.get(0).getAccountType())) {
            return false;
        }
        ArrayList<AasIndicatorNamePair> aasIndicatorAndNames = getAasIndicatorAndNames(rawContactDeltaList);
        Log.sensitive("SimAasEditor", "[isAasNameChangedOnly] mOldAasIndNameList = " + this.mOldAasIndNameList + ", curAasIndNameList = " + aasIndicatorAndNames);
        if (this.mOldAasIndNameList == null || aasIndicatorAndNames == null || this.mOldAasIndNameList.size() != aasIndicatorAndNames.size()) {
            return false;
        }
        int size = this.mOldAasIndNameList.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            String str = this.mOldAasIndNameList.get(i).indicator;
            String str2 = aasIndicatorAndNames.get(i).indicator;
            if (str != null || str2 != null) {
                if (str == null || str2 == null || !str.equals(str2)) {
                    return false;
                }
                if (!this.mOldAasIndNameList.get(i).name.equals(aasIndicatorAndNames.get(i).name)) {
                    Log.d("SimAasEditor", "[isAasNameChangedOnly] i = " + i);
                    z = true;
                }
            }
        }
        Log.d("SimAasEditor", "[isAasNameChangedOnly] return " + z);
        return z;
    }
}
