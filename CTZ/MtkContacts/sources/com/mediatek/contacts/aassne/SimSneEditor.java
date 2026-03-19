package com.mediatek.contacts.aassne;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.google.android.collect.Lists;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.model.account.UsimAccountType;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class SimSneEditor {
    private Context mContext;
    ArrayList<String> mNickNameArray = null;

    public SimSneEditor(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    private boolean updateDataToDb(int i, String str, ContentResolver contentResolver, String str2, String str3, long j) {
        if (!PhbInfoUtils.usimHasSne(i)) {
            Log.e("SimSneEditor", "[updateDataToDb]DB_UPDATE_NICKNAME-error subId");
            return false;
        }
        return updateNicknameToDB(str, contentResolver, str2, str3, j);
    }

    private boolean updateNicknameToDB(String str, ContentResolver contentResolver, String str2, String str3, long j) {
        if (AccountTypeUtils.isUsimOrCsim(str)) {
            ContentValues contentValues = new ContentValues();
            String str4 = "raw_contact_id = '" + j + "' AND mimetype='vnd.android.cursor.item/nickname'";
            if (TextUtils.isEmpty(str2)) {
                str2 = "";
            }
            Log.sensitive("SimSneEditor", "[updateNicknameToDB]whereNickname is:" + str4 + ",updateNickname:" + str2);
            if (!TextUtils.isEmpty(str2) && !TextUtils.isEmpty(str3)) {
                contentValues.put("data1", str2);
                Log.d("SimSneEditor", "[updateNickname] upNickname is " + Log.anonymize(Integer.valueOf(contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, str4, null))));
                return true;
            }
            if (TextUtils.isEmpty(str2) || !TextUtils.isEmpty(str3)) {
                if (TextUtils.isEmpty(str2)) {
                    Log.d("SimSneEditor", "[updateNickname] deleteNickname is " + Log.anonymize(Integer.valueOf(contentResolver.delete(ContactsContract.Data.CONTENT_URI, str4, null))));
                    return true;
                }
                return true;
            }
            contentValues.put("raw_contact_id", Long.valueOf(j));
            contentValues.put("mimetype", "vnd.android.cursor.item/nickname");
            contentValues.put("data1", str2);
            Log.d("SimSneEditor", "[updateNickname] upNicknameUri is " + Log.anonymize(contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)));
            return true;
        }
        return false;
    }

    private boolean buildSneOperation(String str, ArrayList<ContentProviderOperation> arrayList, String str2, int i) {
        Log.d("SimSneEditor", "[buildSneOperation] entry");
        if (AccountTypeUtils.isUsimOrCsim(str)) {
            Log.d("SimSneEditor", "[buildSneOperation] isUSIM true");
            if (!TextUtils.isEmpty(str2)) {
                Log.d("SimSneEditor", "[buildSneOperation] nickname is not empty");
                ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                Log.d("SimSneEditor", "[buildSneOperation] nickname:" + Log.anonymize(str2));
                builderNewInsert.withValueBackReference("raw_contact_id", i);
                builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/nickname");
                builderNewInsert.withValue("data1", str2);
                arrayList.add(builderNewInsert.build());
                Log.sensitive("SimSneEditor", "[buildSneOperation] operationList:" + arrayList);
                return true;
            }
        }
        return true;
    }

    private boolean isNickname(String str) {
        Log.d("SimSneEditor", "[isNickname] plugin");
        return "vnd.android.cursor.item/nickname".equals(str);
    }

    public String buildSimNickname(String str, ArrayList<String> arrayList, int i, String str2) {
        Log.d("SimSneEditor", "[buildSimNickname] Entry");
        if (AccountTypeUtils.isUsimOrCsim(str)) {
            Log.d("SimSneEditor", "[buildSimNickname] USIM is true");
            boolean zUsimHasSne = PhbInfoUtils.usimHasSne(i);
            if (arrayList.isEmpty() || !zUsimHasSne) {
                return null;
            }
            Log.d("SimSneEditor", "[buildSimNickname] nickname array is not empty");
            Log.d("SimSneEditor", "[buildSimNickname] hasSne is:" + zUsimHasSne);
            String strRemove = arrayList.remove(0);
            if (TextUtils.isEmpty(strRemove)) {
                strRemove = "";
            }
            int usimSneMaxNameLength = PhbInfoUtils.getUsimSneMaxNameLength(i);
            Log.d("SimSneEditor", "[buildSimNickname]before encode simNickname=" + Log.anonymize(strRemove));
            try {
                GsmAlphabet.stringToGsm7BitPacked(strRemove);
                if (strRemove.length() > usimSneMaxNameLength) {
                    strRemove = "";
                }
            } catch (EncodeException e) {
                Log.e("SimSneEditor", "[buildSimNickname] Error at GsmAlphabet.stringToGsm7BitPacked()!");
                if (strRemove.length() > ((usimSneMaxNameLength - 1) >> 1)) {
                    strRemove = "";
                }
            }
            String str3 = strRemove;
            Log.d("SimSneEditor", "[buildSimNickname]after encode simNickname=" + Log.anonymize(str3));
            return str3;
        }
        return str2;
    }

    public void onEditorBindEditors(RawContactDelta rawContactDelta, AccountType accountType, int i) {
        Log.d("SimSneEditor", "[onEditorBindEditors] Entry");
        boolean zUsimHasSne = PhbInfoUtils.usimHasSne(i);
        if (AccountTypeUtils.isUsim(accountType.accountType)) {
            Log.d("SimSneEditor", "[onEditorBindEditors] isUSIM");
            SimAasSneUtils.setCurrentSubId(i);
            if (zUsimHasSne) {
                addDataKindNickname(accountType);
                Log.d("SimSneEditor", "[onEditorBindEditors] ensurekindexists");
                RawContactModifier.ensureKindExists(rawContactDelta, accountType, "vnd.android.cursor.item/nickname");
            } else {
                Log.d("SimSneEditor", "[onEditorBindEditors] removeDataKindNickname");
                removeDataKindNickname(accountType);
            }
            DataKind kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/nickname");
            if (kindForMimetype != null) {
                Log.d("SimSneEditor", "[onEditorBindEditors] datakind not null");
                updateNickname(kindForMimetype, zUsimHasSne);
            }
        }
    }

    public void updateNickNameKind(AccountType accountType, int i) {
        if (accountType != null && AccountTypeUtils.isUsim(accountType.accountType)) {
            boolean zUsimHasSne = PhbInfoUtils.usimHasSne(i);
            if (zUsimHasSne) {
                addDataKindNickname(accountType);
            } else {
                removeDataKindNickname(accountType);
            }
            DataKind kindForMimetype = accountType.getKindForMimetype("vnd.android.cursor.item/nickname");
            if (kindForMimetype != null) {
                updateNickname(kindForMimetype, zUsimHasSne);
            }
        }
    }

    public void onEditorBindForCompactEditor(RawContactDeltaList rawContactDeltaList, int i, Context context) {
        int size = rawContactDeltaList.size();
        Log.d("SimSneEditor", "[onEditorBindForCompactEditor] Entry numRawContacts= " + size);
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(this.mContext);
        for (int i2 = 0; i2 < size; i2++) {
            RawContactDelta rawContactDelta = rawContactDeltaList.get(i2);
            AccountType accountType = rawContactDelta.getAccountType(accountTypeManager);
            Log.d("SimSneEditor", "[onEditorBindForCompactEditor] loop subid=" + i);
            onEditorBindEditors(rawContactDelta, accountType, i);
        }
    }

    private void updateNickname(DataKind dataKind, boolean z) {
        Log.d("SimSneEditor", "[updateNickname]for USIM,hasSne:" + z);
        if (z) {
            if (dataKind.fieldList == null) {
                dataKind.fieldList = Lists.newArrayList();
            } else {
                dataKind.fieldList.clear();
            }
            dataKind.fieldList.add(new AccountType.EditField("data1", R.string.nicknameLabelsGroup, 8289));
            return;
        }
        dataKind.fieldList = Lists.newArrayList();
    }

    private void addDataKindNickname(AccountType accountType) {
        Log.d("SimSneEditor", "[addDataKindNickname]Entry");
        if (accountType instanceof UsimAccountType) {
            Log.d("SimSneEditor", "[addDataKindNickname]account type is instance of USIM");
            try {
                DataKind dataKindAddKind = ((UsimAccountType) accountType).addKind(new DataKind("vnd.android.cursor.item/nickname", R.string.nicknameLabelsGroup, 115, true));
                dataKindAddKind.typeOverallMax = 1;
                dataKindAddKind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nicknameLabelsGroup);
                dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
                dataKindAddKind.defaultValues = new ContentValues();
                dataKindAddKind.defaultValues.put("data2", (Integer) 1);
                Log.d("SimSneEditor", "[addDataKindNickname]adding kind");
            } catch (Exception e) {
                Log.d("SimSneEditor", "[addDataKindNickname]addkind Exception & return");
            }
        }
    }

    private void removeDataKindNickname(AccountType accountType) {
        Log.d("SimSneEditor", "[removeDataKindNickname]Entry");
        if (accountType instanceof UsimAccountType) {
            Log.d("SimSneEditor", "[removeDataKindNickname]account type is instance of USIM");
            try {
                ((UsimAccountType) accountType).removeKind("vnd.android.cursor.item/nickname");
                Log.d("SimSneEditor", "[removeDataKindNickname] done");
            } catch (Exception e) {
                Log.d("SimSneEditor", "[removeDataKindNickname]removekind Exception & return");
            }
        }
    }

    private void fillNickNameArray(Uri uri) {
        Log.d("SimSneEditor", "[fillNickNameArray] Entry");
        this.mNickNameArray = new ArrayList<>();
        Cursor cursorQuery = this.mContext.getContentResolver().query(uri, new String[]{"mimetype", "data1"}, null, null, null);
        if (cursorQuery != null && cursorQuery.moveToFirst()) {
            do {
                if (isNickname(cursorQuery.getString(0))) {
                    String string = cursorQuery.getString(1);
                    Log.d("SimSneEditor", "[fillNickNameArray] nickname is:" + Log.anonymize(string));
                    this.mNickNameArray.add(string);
                }
            } while (cursorQuery.moveToNext());
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
    }

    public void copySimSneToAccount(ArrayList<ContentProviderOperation> arrayList, AccountWithDataSet accountWithDataSet, Uri uri, int i) {
        Log.d("SimSneEditor", "[copySimSneToAccount] Entry and sourceUri: " + uri);
        fillNickNameArray(uri);
        Log.d("SimSneEditor", "[copySimSneToAccount] after fillNickNameArray backRef " + i);
        if (!(accountWithDataSet instanceof AccountWithDataSetEx)) {
            throw new IllegalArgumentException("targetAccount is not AccountWithDataSetEx!");
        }
        int subId = ((AccountWithDataSetEx) accountWithDataSet).getSubId();
        Log.d("SimSneEditor", "[copySimSneToAccount] subId " + subId);
        String strBuildSimNickname = buildSimNickname(accountWithDataSet.type, this.mNickNameArray, subId, null);
        Log.d("SimSneEditor", "[copySimSneToAccount] after buildSimNickname simNickname is:" + Log.anonymize(strBuildSimNickname));
        buildSneOperation(accountWithDataSet.type, arrayList, strBuildSimNickname, i);
        Log.d("SimSneEditor", "[copySimSneToAccount] after buildSneOperation");
    }

    public void editSimSne(Intent intent, long j, int i, long j2) {
        String str;
        new ArrayList();
        new ArrayList();
        Log.d("SimSneEditor", "[editSimSne] Entry");
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("simData");
        ArrayList parcelableArrayListExtra2 = intent.getParcelableArrayListExtra("oldsimData");
        String asString = ((RawContactDelta) parcelableArrayListExtra.get(0)).getValues().getAsString("account_type");
        Log.sensitive("SimSneEditor", "[editSimSne] Accountype from newSimData:" + asString);
        if (!PhbInfoUtils.usimHasSne(i) || !AccountTypeUtils.isUsimOrCsim(asString)) {
            Log.d("SimSneEditor", "[editSimSne] do nothing & return ");
            return;
        }
        int size = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().size();
        String str2 = null;
        if (AccountTypeUtils.isUsimOrCsim(asString)) {
            String str3 = null;
            for (int i2 = 0; i2 < size; i2++) {
                String asString2 = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i2).getAsString("mimetype");
                String asString3 = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i2).getAsString("data1");
                Log.d("SimSneEditor", "[editSimSne]countIndex:" + i2 + ",mimeType:" + asString2 + "data:" + Log.anonymize(asString3));
                if ("vnd.android.cursor.item/nickname".equals(asString2)) {
                    if (TextUtils.isEmpty(asString3)) {
                        asString3 = "";
                    }
                    Log.d("SimSneEditor", "[editSimSne] updated nickname is" + Log.anonymize(asString3));
                    str3 = asString3;
                }
            }
            str = str3;
        } else {
            str = null;
        }
        if (parcelableArrayListExtra2 != null) {
            int size2 = ((RawContactDelta) parcelableArrayListExtra2.get(0)).getContentValues().size();
            for (int i3 = 0; i3 < size2; i3++) {
                String asString4 = ((RawContactDelta) parcelableArrayListExtra2.get(0)).getContentValues().get(i3).getAsString("mimetype");
                String asString5 = ((RawContactDelta) parcelableArrayListExtra2.get(0)).getContentValues().get(i3).getAsString("data1");
                Log.d("SimSneEditor", "[getOldRawContactData]Data.MIMETYPE: " + asString4 + ",data:" + Log.anonymize(asString5));
                if ("vnd.android.cursor.item/nickname".equals(asString4)) {
                    Log.d("SimSneEditor", "[editSimSne]sOldNickname=" + Log.anonymize(asString5));
                    str2 = asString5;
                }
            }
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Log.d("SimSneEditor", "[editSimSne] RawcontactId" + j2);
        updateDataToDb(i, asString, contentResolver, str, str2, j2);
    }

    public boolean isSneNicknameValid(String str, int i) {
        if (!PhbInfoUtils.usimHasSne(i)) {
            Log.i("SimSneEditor", "[isSneNicknameValid] return false, sim not support sne");
            return false;
        }
        if (TextUtils.isEmpty(str)) {
            Log.d("SimSneEditor", "[isSneNicknameValid] nickname valid true");
            return true;
        }
        int usimSneMaxNameLength = PhbInfoUtils.getUsimSneMaxNameLength(i);
        Log.d("SimSneEditor", "[isSneNicknameValid] max sne length" + usimSneMaxNameLength);
        try {
            GsmAlphabet.stringToGsm7BitPacked(str);
            Log.d("SimSneEditor", "[isSneNicknameValid] given sne length" + str.length());
            if (str.length() > usimSneMaxNameLength) {
                Log.d("SimSneEditor", "[isSneNicknameValid] length exceeds & false");
                return false;
            }
        } catch (EncodeException e) {
            if (str.length() > ((usimSneMaxNameLength - 1) >> 1)) {
                Log.d("SimSneEditor", "[isSneNicknameValid] exception & false");
                return false;
            }
        }
        return true;
    }

    public String getNickName(Intent intent, int i) {
        Log.d("SimSneEditor", "[getNickName] getNickName subId value:" + i);
        String str = null;
        if (!PhbInfoUtils.usimHasSne(i)) {
            Log.i("SimSneEditor", "[checkNickName] return null, sim not support sne");
            return null;
        }
        new ArrayList();
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("simData");
        String asString = ((RawContactDelta) parcelableArrayListExtra.get(0)).getValues().getAsString("account_type");
        int size = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().size();
        if (AccountTypeUtils.isUsimOrCsim(asString)) {
            int i2 = 0;
            while (true) {
                if (i2 >= size) {
                    break;
                }
                String asString2 = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i2).getAsString("mimetype");
                String asString3 = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i2).getAsString("data1");
                Log.d("SimSneEditor", "[checkNickName]countIndex:" + i2 + ",mimeType:" + asString2 + "data:" + Log.anonymize(asString3));
                if (!"vnd.android.cursor.item/nickname".equals(asString2)) {
                    i2++;
                } else {
                    str = TextUtils.isEmpty(asString3) ? "" : asString3;
                    Log.d("SimSneEditor", "[checkNickName] updated nickname is" + Log.anonymize(str));
                }
            }
        }
        return str;
    }

    public void updateValues(Intent intent, int i, ContentValues contentValues) {
        if (!PhbInfoUtils.usimHasSne(i)) {
            contentValues.remove("sne");
            Log.i("SimSneEditor", "[updateValues] hasSne false & return");
            return;
        }
        new ArrayList();
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("simData");
        String asString = ((RawContactDelta) parcelableArrayListExtra.get(0)).getValues().getAsString("account_type");
        SimAasSneUtils.setCurrentSubId(i);
        int size = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().size();
        String str = null;
        if (AccountTypeUtils.isUsimOrCsim(asString)) {
            for (int i2 = 0; i2 < size; i2++) {
                String asString2 = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i2).getAsString("mimetype");
                String asString3 = ((RawContactDelta) parcelableArrayListExtra.get(0)).getContentValues().get(i2).getAsString("data1");
                Log.d("SimSneEditor", "[updateValues]countIndex:" + i2 + ",mimeType:" + asString2 + "data:" + Log.anonymize(asString3));
                if ("vnd.android.cursor.item/nickname".equals(asString2)) {
                    str = TextUtils.isEmpty(asString3) ? "" : asString3;
                    Log.d("SimSneEditor", "[updateValues] updated nickname is" + Log.anonymize(str));
                }
            }
        }
        Log.d("SimSneEditor", "[updateValues] hasSne and sne is:" + Log.anonymize(str));
        if (TextUtils.isEmpty(str)) {
            str = "";
        }
        contentValues.put("sne", str);
    }

    public void updateValuesforCopy(Uri uri, int i, String str, ContentValues contentValues) {
        Log.d("SimSneEditor", "[updateValuesforCopy] Entry sourceUri is :" + Log.anonymize(uri) + " Entry subId is : " + i);
        ArrayList<String> arrayList = new ArrayList<>();
        String[] strArr = {"mimetype", "data1"};
        if (!PhbInfoUtils.usimHasSne(i)) {
            Log.d("SimSneEditor", "[updateValuesforCopy] No sne field in SIM");
            return;
        }
        Cursor cursorQuery = this.mContext.getContentResolver().query(uri, strArr, null, null, null);
        if (cursorQuery != null && cursorQuery.moveToFirst()) {
            do {
                if (isNickname(cursorQuery.getString(0))) {
                    String string = cursorQuery.getString(1);
                    Log.d("SimSneEditor", "[updateValuesforCopy] nickname is:" + string);
                    arrayList.add(string);
                }
            } while (cursorQuery.moveToNext());
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        String strBuildSimNickname = buildSimNickname(str, arrayList, i, null);
        Log.d("SimSneEditor", "[updateValuesforCopy] put values nickname is:" + strBuildSimNickname);
        contentValues.put("sne", strBuildSimNickname);
    }
}
