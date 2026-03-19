package com.mediatek.contacts.editor;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.contacts.ContactSaveService;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.editor.LabeledEditorView;
import com.android.contacts.editor.TextFieldsEditorView;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.dataitem.StructuredNameDataItem;
import com.android.contacts.preference.ContactsPreferences;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.aassne.SimAasSneUtils;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContactEditorUtilsEx {
    private static String TAG = "ContactEditorUtilsEx";
    private static final Map<Integer, Integer> FIELD_VIEW_MAX_LENGTH_MAP = new HashMap();

    static {
        FIELD_VIEW_MAX_LENGTH_MAP.put(Integer.valueOf(BaseAccountType.getTypeNote()), 1024);
        FIELD_VIEW_MAX_LENGTH_MAP.put(Integer.valueOf(BaseAccountType.getTypeWebSite()), 1024);
    }

    public static int getFieldEditorLengthLimit(int i) {
        if (FIELD_VIEW_MAX_LENGTH_MAP.containsKey(Integer.valueOf(i))) {
            return FIELD_VIEW_MAX_LENGTH_MAP.get(Integer.valueOf(i)).intValue();
        }
        return 128;
    }

    public static StructuredNameDataItem restoreStructuredNameDataItem(ContentValues contentValues) {
        DataItem dataItemCreateFrom = DataItem.createFrom(contentValues);
        if (dataItemCreateFrom instanceof StructuredNameDataItem) {
            return (StructuredNameDataItem) dataItemCreateFrom;
        }
        Log.w(TAG, "[restoreStructuredNameDataItem] The dataItem is not an instance of StructuredNameDataItem!!! mimeType: " + contentValues.getAsString("mimetype"));
        return null;
    }

    public static AccountWithDataSet getOnlyOrDefaultAccountEx(ContactsPreferences contactsPreferences, List<AccountWithDataSet> list) {
        boolean z = false;
        if (list.size() == 1) {
            return list.get(0);
        }
        AccountWithDataSet defaultAccount = contactsPreferences.getDefaultAccount();
        Iterator<AccountWithDataSet> it = list.iterator();
        while (true) {
            if (it.hasNext()) {
                AccountWithDataSet next = it.next();
                if (next.equals(defaultAccount)) {
                    defaultAccount = next;
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        if (z) {
            Log.d(TAG, "[getDefaultAccountEx] Default account is not exist, reset it to local phone account");
            for (AccountWithDataSet accountWithDataSet : list) {
                if ("Local Phone Account".equals(accountWithDataSet.type)) {
                    contactsPreferences.setDefaultAccount(accountWithDataSet);
                    return accountWithDataSet;
                }
            }
            return defaultAccount;
        }
        return defaultAccount;
    }

    public static void processGroupMetadataToSim(RawContactDeltaList rawContactDeltaList, Intent intent, Cursor cursor) {
        if (cursor != null) {
            int count = cursor.getCount();
            String asString = rawContactDeltaList.get(0).getValues().getAsString("account_type");
            if ((asString.equals("USIM Account") || "CSIM Account".equals(asString)) && count > 0) {
                String[] strArr = new String[count];
                long[] jArr = new long[count];
                cursor.moveToPosition(-1);
                int i = 0;
                while (cursor.moveToNext()) {
                    Log.sensitive(TAG, "[processGroupMetadataToSim] ACCOUNT_NAME: " + cursor.getString(0) + ",DATA_SET: " + cursor.getString(2) + ",GROUP_ID: " + cursor.getLong(3) + ", TITLE: " + cursor.getString(4));
                    strArr[i] = cursor.getString(4);
                    jArr[i] = cursor.getLong(3);
                    i++;
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("[processGroupMetadataToSim] I : ");
                    sb.append(i);
                    Log.d(str, sb.toString());
                }
                intent.putExtra("groupName", strArr);
                intent.putExtra("groupNum", count);
                intent.putExtra(ContactSaveService.EXTRA_GROUP_ID, jArr);
                Log.d(TAG, "[processGroupMetadataToSim] groupNum : " + Log.anonymize(Integer.valueOf(count)));
            }
        }
    }

    public static void setInputMethodVisible(boolean z, Context context) {
        ((ContactEditorActivity) context).getWindow().setSoftInputMode((z ? 5 : 3) | 16);
    }

    public static void updateAasView(Context context, RawContactDeltaList rawContactDeltaList, LinearLayout linearLayout) {
        int size = rawContactDeltaList.size();
        for (int i = 0; i < size; i++) {
            RawContactDelta rawContactDelta = rawContactDeltaList.get(i);
            String accountType = rawContactDelta.getAccountType();
            if (AccountTypeUtils.isUsimOrCsim(accountType)) {
                ValuesDelta values = rawContactDelta.getValues();
                if (values.isVisible()) {
                    DataKind kindForMimetype = AccountTypeManager.getInstance(context).getAccountType(accountType, values.getAsString("data_set")).getKindForMimetype("vnd.android.cursor.item/phone_v2");
                    GlobalEnv.getSimAasEditor().updatePhoneType(SimAasSneUtils.getCurSubId(), kindForMimetype);
                    updateEditorViewsLabel(linearLayout);
                }
            }
        }
    }

    private static void updateEditorViewsLabel(ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt instanceof TextFieldsEditorView) {
                ((LabeledEditorView) childAt).updateValues();
            } else if (childAt instanceof ViewGroup) {
                updateEditorViewsLabel((ViewGroup) childAt);
            }
        }
    }

    public static void setSimDataKindCountMax(AccountType accountType, int i) {
        for (DataKind dataKind : accountType.getSortedDataKinds()) {
            if ("USIM Account".equals(accountType.accountType) || "CSIM Account".equals(accountType.accountType)) {
                if (dataKind.mimeType.equals("vnd.android.cursor.item/phone_v2") && dataKind.typeOverallMax <= 0) {
                    dataKind.typeOverallMax = PhbInfoUtils.getUsimAnrCount(i) + 1;
                    Log.d(TAG, "[setSimDataKindCountMax] Usim max number = ANR + 1 = " + dataKind.typeOverallMax);
                }
            }
        }
    }

    public static void showLogContactState(RawContactDeltaList rawContactDeltaList) {
        if (rawContactDeltaList != null) {
            Log.i(TAG, "[showLogContactState] state size = " + rawContactDeltaList.size());
        }
    }

    public static void ensureDataKindsForSim(RawContactDeltaList rawContactDeltaList, int i, Context context) {
        GlobalEnv.getSimAasEditor().ensurePhoneKindForCompactEditor(rawContactDeltaList, i, context);
        GlobalEnv.getSimSneEditor().onEditorBindForCompactEditor(rawContactDeltaList, i, context);
        ensureEmailKindForSim(rawContactDeltaList, i, context);
    }

    public static void updateDataKindsForSim(AccountType accountType, int i) {
        GlobalEnv.getSimAasEditor().updatePhoneKind(accountType, i);
        GlobalEnv.getSimSneEditor().updateNickNameKind(accountType, i);
        updateEmailKind(accountType, i);
    }

    private static void ensureEmailKindForSim(RawContactDeltaList rawContactDeltaList, int i, Context context) {
        int size = rawContactDeltaList.size();
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(context);
        for (int i2 = 0; i2 < size; i2++) {
            RawContactDelta rawContactDelta = rawContactDeltaList.get(i2);
            AccountType accountType = rawContactDelta.getAccountType(accountTypeManager);
            Log.d(TAG, "[ensureEmailKindForSim] loop " + i2 + " subid=" + i);
            if (accountType != null && AccountTypeUtils.isUsimOrCsim(accountType.accountType)) {
                if (PhbInfoUtils.getUsimEmailCount(i) > 0) {
                    AccountTypeUtils.addDataKindEmail(accountType);
                    Log.d(TAG, "[ensureEmailKindForSim] ensure email kind exists");
                    RawContactModifier.ensureKindExists(rawContactDelta, accountType, "vnd.android.cursor.item/email_v2");
                } else {
                    AccountTypeUtils.removeDataKind(accountType, "vnd.android.cursor.item/email_v2");
                }
            }
        }
    }

    private static void updateEmailKind(AccountType accountType, int i) {
        if (accountType != null && AccountTypeUtils.isUsimOrCsim(accountType.accountType)) {
            if (PhbInfoUtils.getUsimEmailCount(i) > 0) {
                AccountTypeUtils.addDataKindEmail(accountType);
            } else {
                AccountTypeUtils.removeDataKind(accountType, "vnd.android.cursor.item/email_v2");
            }
        }
    }

    public static AccountWithDataSet getAccountWithDataSet(Context context, Account account, String str, Bundle bundle) {
        if (account != null) {
            if (AccountTypeUtils.isAccountTypeIccCard(account.type)) {
                return new AccountWithDataSetEx(account.name, account.type, str, AccountTypeUtils.getSubIdBySimAccountName(context, account.name));
            }
            return new AccountWithDataSet(account.name, account.type, str);
        }
        return (AccountWithDataSet) bundle.getParcelable("com.android.contacts.ACCOUNT_WITH_DATA_SET");
    }
}
