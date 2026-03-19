package com.mediatek.contacts.simservice;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.SubContactsUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimProcessorManager;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SimEditProcessor extends SimProcessorBase {
    private AccountWithDataSet mAccount;
    private String mAccountName;
    private String mAccountType;
    private boolean mAllEditorInvalid;
    private int mContactId;
    private Context mContext;
    private boolean mDoublePhoneNumber;
    private boolean mFixNumberInvalid;
    HashMap<Long, String> mGroupAddList;
    private int mGroupNum;
    private int mIndexInSim;
    private long mIndicate;
    private Intent mIntent;
    private Uri mLookupUri;
    private boolean mNickNameInvalid;
    private boolean mNumberInvalid;
    private boolean mNumberIsNull;
    private String mOldEmail;
    HashMap<Long, String> mOldGroupAddList;
    private String mOldName;
    private String mOldOtherPhone;
    private String mOldPhone;
    private boolean mPhbReady;
    private boolean mQuitEdit;
    private long mRawContactId;
    private int mSaveFailToastStrId;
    private int mSaveMode;
    private ArrayList<RawContactDelta> mSimData;
    private ArrayList<RawContactDelta> mSimOldData;
    private String mSimType;
    private long mSlotId;
    private int mSubId;
    private String mUpdateAdditionalNumber;
    private String mUpdateName;
    private String mUpdatemail;
    private String mUpdatephone;
    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = {"name", "number", "emails", "additionalNumber", "groupIds"};
    private static String sName = null;
    private static String sEmail = null;
    private static String sPhone = null;
    private static String sOtherPhone = null;
    private static String sAfterPhone = null;
    private static String sAfterOtherPhone = null;
    private static List<Listener> sListeners = new ArrayList();
    private static Map<Listener, Handler> sListenerHolder = new HashMap();

    public interface Listener {
        void onSIMEditCompleted(Intent intent);
    }

    public static void registerListener(Listener listener, Handler handler) {
        if (!(listener instanceof Activity)) {
            throw new ClassCastException("Only activities can be registered to receive callback from " + SimProcessorService.class.getName());
        }
        Log.d("SimEditProcessor", "[registerListener]listener added to SIMEditProcessor: " + listener);
        sListeners.add(listener);
        sListenerHolder.put(listener, handler);
        Log.d("SimEditProcessor", "[registerListener] sListenerHolder = " + listener.hashCode() + " mHandler = " + handler.hashCode());
    }

    public static void unregisterListener(Listener listener) {
        Log.d("SimEditProcessor", "[unregisterListener]listener removed from SIMEditProcessor: " + listener);
        Handler handler = sListenerHolder.get(listener);
        if (handler != null) {
            Log.d("SimEditProcessor", "[unregisterListener] handler = " + handler.hashCode() + " listener = " + listener.hashCode());
            sListeners.remove(listener);
            sListenerHolder.remove(listener);
        }
    }

    public static boolean isNeedRegisterHandlerAgain(Handler handler) {
        Log.d("SimEditProcessor", "[isNeedRegisterHandlerAgain] handler: " + handler);
        Iterator<Listener> it = sListeners.iterator();
        while (it.hasNext()) {
            if (handler.equals(sListenerHolder.get(it.next()))) {
                return false;
            }
        }
        return true;
    }

    public SimEditProcessor(Context context, int i, Intent intent, SimProcessorManager.ProcessorCompleteListener processorCompleteListener) {
        super(intent, processorCompleteListener);
        this.mIntent = null;
        this.mLookupUri = null;
        this.mAccount = null;
        this.mGroupAddList = new HashMap<>();
        this.mOldGroupAddList = new HashMap<>();
        this.mSimData = new ArrayList<>();
        this.mSimOldData = new ArrayList<>();
        this.mAccountType = null;
        this.mAccountName = null;
        this.mOldName = null;
        this.mOldPhone = null;
        this.mOldOtherPhone = null;
        this.mOldEmail = null;
        this.mUpdateName = null;
        this.mUpdatephone = null;
        this.mUpdatemail = null;
        this.mUpdateAdditionalNumber = null;
        this.mSimType = "UNKNOWN";
        this.mGroupNum = 0;
        this.mSlotId = SubInfoUtils.getInvalidSlotId();
        this.mSubId = SubInfoUtils.getInvalidSubId();
        this.mSaveMode = 0;
        this.mSaveFailToastStrId = -1;
        this.mContactId = 0;
        this.mIndicate = -1L;
        this.mIndexInSim = -1;
        this.mRawContactId = -1L;
        this.mPhbReady = false;
        this.mDoublePhoneNumber = false;
        this.mNumberInvalid = false;
        this.mQuitEdit = false;
        this.mNumberIsNull = false;
        this.mFixNumberInvalid = false;
        this.mAllEditorInvalid = false;
        this.mNickNameInvalid = false;
        this.mContext = context;
        this.mSubId = i;
        this.mIntent = intent;
        Log.i("SimEditProcessor", "[SIMEditProcessor]new mSubId = " + this.mSubId);
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public void doWork() {
        if (isCancelled()) {
            Log.w("SimEditProcessor", "[dowork]cancel remove work. Thread id = " + Thread.currentThread().getId());
            return;
        }
        if (isPhbReady()) {
            this.mQuitEdit = false;
            this.mSimData = this.mIntent.getParcelableArrayListExtra("simData");
            this.mSimOldData = this.mIntent.getParcelableArrayListExtra("oldsimData");
            this.mAccountType = this.mSimData.get(0).getValues().getAsString("account_type");
            if (this.mAccountType.equals("USIM Account") || this.mAccountType.equals("CSIM Account")) {
                this.mGroupNum = this.mIntent.getIntExtra("groupNum", 0);
                Log.i("SimEditProcessor", "[dowork]groupNum : " + Log.anonymize(Integer.valueOf(this.mGroupNum)));
            }
            this.mAccountName = this.mSimData.get(0).getValues().getAsString("account_name");
            if (this.mAccountType != null && this.mAccountName != null) {
                this.mAccount = new AccountWithDataSet(this.mAccountName, this.mAccountType, null);
                this.mIndicate = this.mIntent.getIntExtra("indicate_phone_or_sim_contact", -1);
                this.mIndexInSim = this.mIntent.getIntExtra("simIndex", -1);
                this.mSaveMode = this.mIntent.getIntExtra("simSaveMode", 0);
                this.mLookupUri = this.mIntent.getData();
                Log.d("SimEditProcessor", "[dowork]the mSubId is =" + this.mSubId + " the mIndicate is =" + this.mIndicate + " the mSaveMode = " + this.mSaveMode + " the accounttype is = " + Log.anonymize(this.mAccountType) + " the uri is  = " + this.mLookupUri + " | mIndexInSim : " + this.mIndexInSim);
                this.mSimType = SimCardUtils.getSimTypeBySubId(this.mSubId);
                if (SimCardUtils.isUsimOrCsimType(this.mSubId)) {
                    this.mSimType = "USIM";
                } else {
                    this.mSimType = "SIM";
                }
                Log.d("SimEditProcessor", "[doWork]mSimType:" + this.mSimType);
                initStaticValues();
                int size = this.mSimData.get(0).getContentValues().size();
                Log.d("SimEditProcessor", "[dowork]kindCount: " + size);
                String[] strArr = new String[2];
                String[] strArr2 = new String[2];
                long[] jArr = new long[this.mGroupNum];
                getRawContactDataFromIntent(size, strArr, strArr2, jArr);
                if (this.mAccountType.equals("USIM Account")) {
                    setGroupFromIntent(jArr);
                }
                if (!TextUtils.isEmpty(strArr[1]) || !TextUtils.isEmpty(strArr2[1])) {
                    this.mDoublePhoneNumber = true;
                    Log.w("SimEditProcessor", "[dowork] double phone number, two mobile phone type, return");
                    setSaveFailToastText();
                    deliverCallbackAndBackToFragment();
                    return;
                }
                sOtherPhone = strArr[0];
                sPhone = strArr2[0];
                Log.d("SimEditProcessor", "[dowork] sName = " + Log.anonymize(sName) + ", sPhone =" + Log.anonymize(sPhone) + ", sOtherPhone = " + Log.anonymize(sOtherPhone) + ", email =" + Log.anonymize(sEmail));
                if (isPhoneNumberInvaild()) {
                    Log.w("SimEditProcessor", "[dowork] Phone Number Invaild, need return");
                    this.mNumberInvalid = true;
                    setSaveFailToastText();
                    deliverCallbackAndBackToFragment();
                    return;
                }
                if (SimServiceUtils.isServiceRunning(this.mContext, this.mSubId)) {
                    Log.i("SimEditProcessor", "[dowork]hasImported,return.");
                    showToastMessage(R.string.msg_loading_sim_contacts_toast, null);
                    deliverCallbackOnError();
                    return;
                }
                saveSimContact(this.mSaveMode);
                Log.d("SimEditProcessor", "[dowork] sName = " + Log.anonymize(sName) + ", sPhone =" + Log.anonymize(sPhone) + ", additionalNumberBuffer[0] = " + Log.anonymize(strArr[0]) + ", sOtherPhone = " + Log.anonymize(sOtherPhone) + ", email =" + Log.anonymize(sEmail));
                return;
            }
            Log.w("SimEditProcessor", "[dowork]accountType :" + Log.anonymize(this.mAccountType) + "accountName:" + Log.anonymize(this.mAccountName));
            this.mQuitEdit = true;
            deliverCallbackOnError();
            return;
        }
        Log.w("SimEditProcessor", "[dowork] phb not ready, need quit eidtor UI");
        this.mQuitEdit = true;
        setSaveFailToastText();
        deliverCallbackOnError();
    }

    private boolean isPhoneNumberInvaild() {
        Log.d("SimEditProcessor", "[isPhoneNumberInvaild]initial phone number:" + Log.anonymize(sPhone));
        sAfterPhone = sPhone;
        if (!TextUtils.isEmpty(sPhone)) {
            if (PhoneNumberUtils.isUriNumber(sPhone)) {
                Log.e("SimEditProcessor", "input phone number is invaild sPhone = " + Log.anonymize(sPhone));
                return true;
            }
            sAfterPhone = PhoneNumberUtils.stripSeparators(sPhone);
            if (!Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(sAfterPhone))) {
                return true;
            }
        }
        Log.d("SimEditProcessor", "[isPhoneNumberInvaild]initial sOtherPhone number:" + Log.anonymize(sOtherPhone));
        sAfterOtherPhone = sOtherPhone;
        if (!TextUtils.isEmpty(sOtherPhone)) {
            if (PhoneNumberUtils.isUriNumber(sOtherPhone)) {
                Log.e("SimEditProcessor", "input other phone number is invaild sOtherPhone = " + Log.anonymize(sOtherPhone));
                return true;
            }
            sAfterOtherPhone = PhoneNumberUtils.stripSeparators(sOtherPhone);
            return !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(sAfterOtherPhone));
        }
        return false;
    }

    private void getRawContactDataFromIntent(int i, String[] strArr, String[] strArr2, long[] jArr) {
        Log.d("SimEditProcessor", "[getRawContactDataFromIntent]...");
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < i; i5++) {
            String asString = this.mSimData.get(0).getContentValues().get(i5).getAsString("mimetype");
            String asString2 = this.mSimData.get(0).getContentValues().get(i5).getAsString("data1");
            Log.d("SimEditProcessor", "[getRawContactDataFromIntent] countIndex:" + i5 + ", mimeType:" + asString + ", data:" + Log.anonymize(asString2));
            if ("vnd.android.cursor.item/name".equals(asString)) {
                sName = asString2;
            } else if ("vnd.android.cursor.item/phone_v2".equals(asString)) {
                if ((String.valueOf(7).equals(this.mSimData.get(0).getContentValues().get(i5).getAsString("data2")) || ((this.mAccountType.equals("USIM Account") || this.mAccountType.equals("CSIM Account")) && GlobalEnv.getSimAasEditor().checkAasEntry(this.mSimData.get(0).getContentValues().get(i5)))) && i2 < strArr.length) {
                    strArr[i2] = asString2;
                    i2++;
                } else if (i3 < strArr2.length) {
                    strArr2[i3] = asString2;
                    i3++;
                }
            } else if ("vnd.android.cursor.item/email_v2".equals(asString)) {
                sEmail = asString2;
            } else if ("vnd.android.cursor.item/group_membership".equals(asString) && jArr.length > i4) {
                jArr[i4] = this.mSimData.get(0).getContentValues().get(i5).getAsLong("data1").longValue();
                i4++;
            }
        }
    }

    private void setGroupFromIntent(long[] jArr) {
        String[] strArr = new String[this.mGroupNum];
        long[] jArr2 = new long[this.mGroupNum];
        String[] stringArrayExtra = this.mIntent.getStringArrayExtra("groupName");
        long[] longArrayExtra = this.mIntent.getLongArrayExtra(ContactSaveService.EXTRA_GROUP_ID);
        Log.d("SimEditProcessor", "[setGroupFromIntent]groupBuffer len :" + jArr.length);
        for (int i = 0; i < jArr.length; i++) {
            for (int i2 = 0; i2 < this.mGroupNum; i2++) {
                if (jArr[i] == longArrayExtra[i2]) {
                    this.mGroupAddList.put(Long.valueOf(jArr[i]), stringArrayExtra[i2]);
                }
            }
        }
    }

    private String replaceCharOnNumber(String str) {
        if (!TextUtils.isEmpty(str)) {
            Log.d("SimEditProcessor", "[replaceCharOnNumber]befor replaceall number : " + Log.anonymize(str));
            String strReplaceAll = str.replaceAll("-", "").replaceAll(" ", "");
            Log.d("SimEditProcessor", "[replaceCharOnNumber]after replaceall number : " + Log.anonymize(strReplaceAll));
            return strReplaceAll;
        }
        return str;
    }

    private void setSaveFailToastText() {
        this.mSaveFailToastStrId = -1;
        if (!this.mPhbReady) {
            this.mSaveFailToastStrId = R.string.icc_phone_book_invalid;
            this.mQuitEdit = true;
        } else if (this.mNumberIsNull) {
            this.mSaveFailToastStrId = R.string.cannot_insert_null_number;
            this.mNumberIsNull = false;
        } else if (this.mNumberInvalid) {
            this.mSaveFailToastStrId = R.string.sim_invalid_number;
            this.mNumberInvalid = false;
        } else if (this.mFixNumberInvalid) {
            this.mSaveFailToastStrId = R.string.sim_invalid_fix_number;
            this.mFixNumberInvalid = false;
        } else if (this.mDoublePhoneNumber) {
            this.mSaveFailToastStrId = R.string.has_double_phone_number;
            this.mDoublePhoneNumber = false;
        } else if (this.mAllEditorInvalid) {
            this.mSaveFailToastStrId = R.string.contactSavedErrorToast;
            this.mAllEditorInvalid = false;
        } else if (this.mNickNameInvalid) {
            this.mSaveFailToastStrId = R.string.nickname_too_long;
            this.mNickNameInvalid = false;
        }
        Log.i("SimEditProcessor", "[setSaveFailToastText]mSaveFailToastStrId is:" + this.mSaveFailToastStrId + ",mPhbReady:" + this.mPhbReady);
        if (this.mSaveFailToastStrId >= 0) {
            if (this.mSaveFailToastStrId == R.string.err_icc_no_phone_book) {
                showToastMessage(-1, this.mContext.getResources().getString(this.mSaveFailToastStrId, this.mSimType));
            } else {
                showToastMessage(this.mSaveFailToastStrId, null);
            }
        }
    }

    private boolean checkIfSaveFail(Uri uri) {
        Log.d("SimEditProcessor", "[checkIfSaveFail] checkUri: " + Log.anonymize(uri));
        if (uri == null) {
            return true;
        }
        String str = uri.getPathSegments().get(0);
        Log.d("SimEditProcessor", "[checkIfSaveFail] msg: " + Log.anonymize(str));
        if ("error".equals(str)) {
            return updateFailToastText(Integer.parseInt(uri.getPathSegments().get(1)));
        }
        return false;
    }

    private boolean updateFailToastText(int i) {
        Log.d("SimEditProcessor", "[updateFailToastText] result: " + i);
        this.mSaveFailToastStrId = -1;
        if (-1 == i) {
            this.mSaveFailToastStrId = R.string.number_too_long;
        } else if (-2 == i) {
            this.mSaveFailToastStrId = R.string.name_too_long;
        } else if (-3 == i) {
            this.mSaveFailToastStrId = R.string.storage_full;
            this.mQuitEdit = true;
        } else if (-6 == i) {
            this.mSaveFailToastStrId = R.string.additional_number_too_long;
        } else if (-10 == i) {
            this.mSaveFailToastStrId = R.string.generic_failure;
            this.mQuitEdit = true;
        } else if (-11 == i) {
            this.mSaveFailToastStrId = R.string.err_icc_no_phone_book;
            this.mQuitEdit = true;
        } else if (-12 == i) {
            this.mSaveFailToastStrId = R.string.error_save_usim_contact_email_lost;
        } else if (-13 == i) {
            this.mSaveFailToastStrId = R.string.email_too_long;
        } else if (i == 0) {
            this.mSaveFailToastStrId = R.string.fail_reason_unknown;
            this.mQuitEdit = true;
        }
        if (this.mSaveFailToastStrId < 0) {
            return false;
        }
        if (this.mSaveFailToastStrId == R.string.err_icc_no_phone_book) {
            showToastMessage(-1, this.mContext.getResources().getString(this.mSaveFailToastStrId, this.mSimType));
        } else {
            showToastMessage(this.mSaveFailToastStrId, null);
        }
        return true;
    }

    private void showResultToastText(int i) {
        String string;
        Log.i("SimEditProcessor", "[showResultToastText]errorType :" + i);
        if (i == -1) {
            string = this.mContext.getString(R.string.contactSavedToast);
        } else {
            string = this.mContext.getString(ContactsGroupUtils.USIMGroupException.getErrorToastId(i));
        }
        if (i == -1 && compareData()) {
            Log.i("SimEditProcessor", "[showResultToastText]saved compareData = true ,return.");
        } else {
            Log.d("SimEditProcessor", "[showResultToastText]showToastMessage default.");
            showToastMessage(-1, string);
        }
    }

    private boolean isContactEditable() {
        Intent intent = this.mIntent;
        if (isRawContactIdInvalid(intent, this.mContext.getContentResolver(), this.mLookupUri)) {
            showToastMessage(R.string.icc_phone_book_invalid, null);
            deliverCallbackOnError();
            Log.d("SimEditProcessor", "[isContactEditable]isRawContactIdInvalid is true,return false.");
            return false;
        }
        if (SubInfoUtils.getSubInfoUsingSubId((int) this.mIndicate) == null) {
            this.mSlotId = SubInfoUtils.getInvalidSlotId();
        } else {
            this.mSlotId = r1.getSimSlotIndex();
        }
        ArrayList<Long> arrayList = new ArrayList<>();
        setOldRawContactData(arrayList);
        Log.i("SimEditProcessor", "the mIndicate: " + this.mIndicate + " | the mSlotId : " + this.mSlotId);
        if (this.mAccountType.equals("USIM Account")) {
            setOldGroupAddList(intent, arrayList);
            return true;
        }
        return true;
    }

    private void setOldGroupAddList(Intent intent, ArrayList<Long> arrayList) {
        String[] strArr = new String[this.mGroupNum];
        long[] jArr = new long[this.mGroupNum];
        Log.i("SimEditProcessor", "[getOldGroupAddList]oldbufferGroup.size() : " + arrayList.size());
        String[] stringArrayExtra = intent.getStringArrayExtra("groupName");
        long[] longArrayExtra = intent.getLongArrayExtra(ContactSaveService.EXTRA_GROUP_ID);
        for (int i = 0; i < arrayList.size(); i++) {
            for (int i2 = 0; i2 < this.mGroupNum; i2++) {
                if (arrayList.get(i).longValue() == longArrayExtra[i2]) {
                    this.mOldGroupAddList.put(Long.valueOf(arrayList.get(i).longValue()), stringArrayExtra[i2]);
                }
            }
        }
    }

    private void setOldRawContactData(ArrayList<Long> arrayList) {
        int size = this.mSimOldData.get(0).getContentValues().size();
        for (int i = 0; i < size; i++) {
            String asString = this.mSimOldData.get(0).getContentValues().get(i).getAsString("mimetype");
            String asString2 = this.mSimOldData.get(0).getContentValues().get(i).getAsString("data1");
            Log.d("SimEditProcessor", "[setOldRawContactData]Data.MIMETYPE: " + asString + ",data:" + Log.anonymize(asString2));
            if ("vnd.android.cursor.item/name".equals(asString)) {
                this.mOldName = this.mSimOldData.get(0).getContentValues().get(i).getAsString("data1");
            } else if ("vnd.android.cursor.item/phone_v2".equals(asString)) {
                if (this.mSimOldData.get(0).getContentValues().get(i).getAsString("data2").equals("7") || GlobalEnv.getSimAasEditor().checkAasEntry(this.mSimOldData.get(0).getContentValues().get(i))) {
                    this.mOldOtherPhone = asString2;
                } else {
                    this.mOldPhone = asString2;
                }
            } else if ("vnd.android.cursor.item/email_v2".equals(asString)) {
                this.mOldEmail = asString2;
            } else if ("vnd.android.cursor.item/group_membership".equals(asString)) {
                Log.d("SimEditProcessor", "[setOldRawContactData] oldIndex = " + i);
                arrayList.add(this.mSimOldData.get(0).getContentValues().get(i).getAsLong("data1"));
            }
        }
        Log.d("SimEditProcessor", "[setOldRawContactData]The mOldName is: " + Log.anonymize(this.mOldName) + " ,mOldOtherPhone: " + Log.anonymize(this.mOldOtherPhone) + " ,mOldPhone: " + Log.anonymize(this.mOldPhone) + " ,mOldEmail: " + Log.anonymize(this.mOldEmail));
    }

    private boolean isRawContactIdInvalid(Intent intent, ContentResolver contentResolver, Uri uri) {
        String authority = uri.getAuthority();
        String strResolveType = intent.resolveType(contentResolver);
        if ("com.android.contacts".equals(authority)) {
            if ("vnd.android.cursor.item/contact".equals(strResolveType)) {
                this.mRawContactId = SubContactsUtils.queryForRawContactId(contentResolver, ContentUris.parseId(uri));
            } else if ("vnd.android.cursor.item/raw_contact".equals(strResolveType)) {
                this.mRawContactId = ContentUris.parseId(uri);
            }
        }
        Log.d("SimEditProcessor", "[isRawContactIdInvalid]authority:" + authority + ",mimeType:" + strResolveType + ",mRawContactId:" + this.mRawContactId);
        if (this.mRawContactId < 1) {
            return true;
        }
        return false;
    }

    private void saveSimContact(int i) {
        Log.d("SimEditProcessor", "[saveSimContact]mode: " + i);
        if (i == 2) {
            if (this.mLookupUri != null) {
                if (!isContactEditable()) {
                    Log.i("SimEditProcessor", "[dowork]isContactEditable is false ,return.");
                    return;
                }
            } else {
                Log.i("SimEditProcessor", "[dowork]mLookupUri is null,return.");
                deliverCallbackOnError();
                return;
            }
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mUpdateName = sName;
        this.mUpdatephone = sAfterPhone;
        this.mUpdatephone = replaceCharOnNumber(this.mUpdatephone);
        ContentValues contentValues = new ContentValues();
        contentValues.put("tag", TextUtils.isEmpty(this.mUpdateName) ? "" : this.mUpdateName);
        contentValues.put("number", TextUtils.isEmpty(this.mUpdatephone) ? "" : this.mUpdatephone);
        if ("USIM".equals(this.mSimType)) {
            updateUSIMSpecValues(contentValues);
        }
        if (i == 1) {
            Log.i("SimEditProcessor", "[saveSimContact]mode is MODE_INSERT");
            insertSIMContact(contentResolver, contentValues);
        } else if (i == 2) {
            Log.i("SimEditProcessor", "[saveSimContact]mode is MODE_EDIT");
            editSIMContact(contentResolver);
        }
    }

    private void insertSIMContact(ContentResolver contentResolver, ContentValues contentValues) {
        if (!isInsertValuesInvalid()) {
            GlobalEnv.getSimAasEditor().updateValues(this.mIntent, this.mSubId, contentValues);
            GlobalEnv.getSimSneEditor().updateValues(this.mIntent, this.mSubId, contentValues);
            Log.i("SimEditProcessor", "[insertSIMContact]insert to SIM card---");
            Uri uriInsert = contentResolver.insert(SubInfoUtils.getIccProviderUri(this.mSubId), contentValues);
            Log.sensitive("SimEditProcessor", "[insertSIMContact]values : " + contentValues + ",checkUri : " + uriInsert);
            if (checkIfSaveFail(uriInsert)) {
                Log.w("SimEditProcessor", "[insertSIMContact]checkIfSaveFail is true,return.");
                deliverCallbackAndBackToFragment();
                return;
            }
            long id = ContentUris.parseId(uriInsert);
            Log.i("SimEditProcessor", "[insertSIMContact]insert to db,mSimType :" + this.mSimType);
            int iInsertGroupToUSIMCard = SimCardUtils.isUsimType(this.mSubId) ? insertGroupToUSIMCard(id, -1) : -1;
            Uri uriInsertToDB = SubContactsUtils.insertToDB(this.mAccount, this.mUpdateName, sPhone, this.mUpdatemail, sOtherPhone, contentResolver, this.mIndicate, this.mSimType, id, this.mGroupAddList.keySet());
            if (uriInsertToDB != null) {
                GlobalEnv.getSimSneEditor().editSimSne(this.mIntent, id, this.mSubId, ContentUris.parseId(uriInsertToDB));
                Uri contactLookupUri = ContactsContract.RawContacts.getContactLookupUri(contentResolver, uriInsertToDB);
                Log.d("SimEditProcessor", "[insertSIMContact]lookupUri: " + contactLookupUri + ", errorType: " + iInsertGroupToUSIMCard);
                showResultToastText(iInsertGroupToUSIMCard);
                if (iInsertGroupToUSIMCard == -1 && contactLookupUri != null) {
                    deliverCallback(contactLookupUri);
                    return;
                } else {
                    deliverCallbackOnError();
                    return;
                }
            }
            Log.e("SimEditProcessor", "[insertSIMContact]insert fail, rawContactUri = null");
            deliverCallbackOnError();
            return;
        }
        Log.w("SimEditProcessor", "[insertSIMContact]isInsertValuesInvalid is true,return.");
        deliverCallbackAndBackToFragment();
    }

    private void editSIMContact(ContentResolver contentResolver) {
        int iUpateGroup;
        ContentValues contentValues = new ContentValues();
        setUpdateValues(contentValues);
        setContactId();
        Log.d("SimEditProcessor", "[editSIMContact] origianl name: " + Log.anonymize(this.mUpdateName));
        if (!TextUtils.isEmpty(this.mUpdateName)) {
            this.mUpdateName = new SubContactsUtils.NamePhoneTypePair(this.mUpdateName).name;
            Log.d("SimEditProcessor", "[editSIMContact] fixed name: " + Log.anonymize(this.mUpdateName));
        }
        Log.d("SimEditProcessor", "[editSIMContact]mSimType:" + this.mSimType);
        if ("SIM".equals(this.mSimType)) {
            if (isSIMUpdateValuesInvalid()) {
                Log.w("SimEditProcessor", "[editSIMContact]isSIMUpdateValuesInvalid is true,return.");
                deliverCallbackAndBackToFragment();
                return;
            }
        } else if ("USIM".equals(this.mSimType) && isUSIMUpdateValuesInvalid()) {
            Log.w("SimEditProcessor", "[editSIMContact]isUSIMUpdateValuesInvalid is true,return.");
            deliverCallbackAndBackToFragment();
            return;
        }
        GlobalEnv.getSimAasEditor().updateValues(this.mIntent, this.mSubId, contentValues);
        GlobalEnv.getSimSneEditor().updateValues(this.mIntent, this.mSubId, contentValues);
        Cursor cursorQuery = contentResolver.query(SubInfoUtils.getIccProviderUri(this.mSubId), ADDRESS_BOOK_COLUMN_NAMES, null, null, null);
        if (cursorQuery != null) {
            try {
                int iUpdate = contentResolver.update(SubInfoUtils.getIccProviderUri(this.mSubId), contentValues, null, null);
                Log.d("SimEditProcessor", "[editSIMContact]result:" + iUpdate);
                if (updateFailToastText(iUpdate)) {
                    Log.i("SimEditProcessor", "[editSIMContact]updateFailToastText,return.");
                    deliverCallbackAndBackToFragment();
                    return;
                }
            } finally {
                cursorQuery.close();
            }
        }
        updateNameToDB(contentResolver);
        updatePhoneNumberToDB(contentResolver);
        if ("USIM".equals(this.mSimType)) {
            updateEmail(contentResolver);
            updateAdditionalNumberToDB(contentResolver);
            iUpateGroup = upateGroup(contentResolver, -1);
            GlobalEnv.getSimSneEditor().editSimSne(this.mIntent, this.mIndexInSim, this.mSubId, this.mRawContactId);
        } else {
            iUpateGroup = -1;
        }
        showResultToastText(iUpateGroup);
        Log.d("SimEditProcessor", "[editSIMContact]errorType :" + iUpateGroup);
        if (iUpateGroup == -1) {
            deliverCallback(ContactsContract.RawContacts.getContactLookupUri(contentResolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, this.mRawContactId)));
        } else {
            deliverCallbackOnError();
        }
    }

    private void setContactId() {
        Cursor cursorQuery = this.mContext.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"contact_id"}, "_id=" + this.mRawContactId, null, null);
        if (cursorQuery != null) {
            if (cursorQuery.moveToFirst()) {
                this.mContactId = cursorQuery.getInt(0);
                Log.d("SimEditProcessor", "[setContactId]contactId:" + this.mContactId);
            }
            cursorQuery.close();
        }
    }

    private void setUpdateValues(ContentValues contentValues) {
        this.mUpdateAdditionalNumber = sAfterOtherPhone;
        if (!TextUtils.isEmpty(this.mUpdateAdditionalNumber)) {
            Log.d("SimEditProcessor", "[setUpdateValues] befor replaceall mUpdateAdditionalNumber : " + Log.anonymize(this.mUpdateAdditionalNumber));
            this.mUpdateAdditionalNumber = this.mUpdateAdditionalNumber.replaceAll("-", "");
            this.mUpdateAdditionalNumber = this.mUpdateAdditionalNumber.replaceAll(" ", "");
            Log.d("SimEditProcessor", "[setUpdateValues] after replaceall mUpdateAdditionalNumber : " + Log.anonymize(this.mUpdateAdditionalNumber));
        }
        contentValues.put("newTag", TextUtils.isEmpty(this.mUpdateName) ? "" : this.mUpdateName);
        contentValues.put("newNumber", TextUtils.isEmpty(this.mUpdatephone) ? "" : this.mUpdatephone);
        contentValues.put("newAnr", TextUtils.isEmpty(this.mUpdateAdditionalNumber) ? "" : this.mUpdateAdditionalNumber);
        contentValues.put("newEmails", TextUtils.isEmpty(this.mUpdatemail) ? "" : this.mUpdatemail);
        contentValues.put("index", Integer.valueOf(this.mIndexInSim));
        Log.sensitive("SimEditProcessor", "[setUpdateValues]updatevalues: " + contentValues);
    }

    private boolean isSIMUpdateValuesInvalid() {
        if (TextUtils.isEmpty(this.mUpdateName) && TextUtils.isEmpty(this.mUpdatephone)) {
            this.mAllEditorInvalid = true;
        } else if (!TextUtils.isEmpty(this.mUpdatephone) && !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(this.mUpdatephone))) {
            this.mNumberInvalid = true;
        }
        Log.i("SimEditProcessor", "[isSIMUpdateValuesInvalid]mNumberInvalid:" + this.mNumberInvalid + ", mAllEditorInvalid:" + this.mAllEditorInvalid);
        setSaveFailToastText();
        if (this.mSaveFailToastStrId >= 0) {
            Log.i("SimEditProcessor", "[isSIMUpdateValuesInvalid]setSaveFailToastText,return true.");
            return true;
        }
        return false;
    }

    private boolean isUSIMUpdateValuesInvalid() {
        String nickName = GlobalEnv.getSimSneEditor().getNickName(this.mIntent, this.mSubId);
        if (TextUtils.isEmpty(this.mUpdatephone) && TextUtils.isEmpty(this.mUpdateName) && TextUtils.isEmpty(this.mUpdatemail) && TextUtils.isEmpty(this.mUpdateAdditionalNumber) && this.mGroupAddList.isEmpty() && TextUtils.isEmpty(nickName)) {
            this.mAllEditorInvalid = true;
        } else if (TextUtils.isEmpty(this.mUpdatephone) && TextUtils.isEmpty(this.mUpdateName) && (!TextUtils.isEmpty(this.mUpdatemail) || !TextUtils.isEmpty(this.mUpdateAdditionalNumber) || !this.mGroupAddList.isEmpty() || !this.mOldGroupAddList.isEmpty() || !TextUtils.isEmpty(nickName))) {
            this.mNumberIsNull = true;
        } else if (!TextUtils.isEmpty(this.mUpdatephone) && !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(this.mUpdatephone))) {
            this.mNumberInvalid = true;
        }
        if (!TextUtils.isEmpty(this.mUpdateAdditionalNumber) && !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(this.mUpdateAdditionalNumber))) {
            this.mFixNumberInvalid = true;
        }
        if (!TextUtils.isEmpty(nickName) && !GlobalEnv.getSimSneEditor().isSneNicknameValid(nickName, this.mSubId)) {
            this.mNickNameInvalid = true;
        }
        Log.i("SimEditProcessor", "[isUSIMUpdateValuesInvalid] mNumberIsNull:" + this.mNumberIsNull + ",mNumberInvalid:" + this.mNumberInvalid + ", mFixNumberInvalid:" + this.mFixNumberInvalid + ", mAllEditorInvalid:" + this.mAllEditorInvalid);
        setSaveFailToastText();
        if (this.mSaveFailToastStrId >= 0) {
            Log.i("SimEditProcessor", "[editSIMContact]setSaveFailToastText,return true.");
            return true;
        }
        return false;
    }

    private void updateNameToDB(ContentResolver contentResolver) {
        ContentValues contentValues = new ContentValues();
        String str = "raw_contact_id = '" + this.mRawContactId + "' AND mimetype='vnd.android.cursor.item/name'";
        Log.d("SimEditProcessor", "[updateNameToDB] mUpdateName:" + Log.anonymize(this.mUpdateName));
        if (!TextUtils.isEmpty(this.mUpdateName)) {
            if (!TextUtils.isEmpty(this.mOldName)) {
                contentValues.put("data1", this.mUpdateName);
                contentValues.putNull("data2");
                contentValues.putNull("data3");
                contentValues.putNull("data4");
                contentValues.putNull("data5");
                contentValues.putNull("data6");
                Log.d("SimEditProcessor", "[updateNameToDB]update ret:" + contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, str, null));
                return;
            }
            contentValues.put("raw_contact_id", Long.valueOf(this.mRawContactId));
            contentValues.put("mimetype", "vnd.android.cursor.item/name");
            contentValues.put("data1", this.mUpdateName);
            Log.d("SimEditProcessor", "[updateNameToDB]uri insert ret:" + Log.anonymize(contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)));
            return;
        }
        Log.d("SimEditProcessor", "[updateNameToDB]delete ret:" + contentResolver.delete(ContactsContract.Data.CONTENT_URI, str, null));
    }

    private void updatePhoneNumberToDB(ContentResolver contentResolver) {
        ContentValues contentValues = new ContentValues();
        String str = "raw_contact_id = '" + this.mRawContactId + "' AND mimetype='vnd.android.cursor.item/phone_v2' AND is_additional_number= 0";
        Log.d("SimEditProcessor", "[updatePhoneNumberToDB] mOldPhone:" + Log.anonymize(this.mOldPhone) + ",mUpdatephone:" + Log.anonymize(sPhone));
        if (!TextUtils.isEmpty(this.mUpdatephone)) {
            if (!TextUtils.isEmpty(this.mOldPhone)) {
                contentValues.put("data1", sPhone);
                Log.i("SimEditProcessor", "[updatePhoneNumberToDB]update ret:" + contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, str, null));
                return;
            }
            contentValues.put("raw_contact_id", Long.valueOf(this.mRawContactId));
            contentValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
            contentValues.put("data1", this.mUpdatephone);
            contentValues.put("is_additional_number", (Integer) 0);
            contentValues.put("data2", (Integer) 2);
            Log.i("SimEditProcessor", "[updatePhoneNumberToDB]Uri insert ret:" + Log.anonymize(contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)));
            return;
        }
        Log.i("SimEditProcessor", "[updatePhoneNumberToDB]delete ret: " + contentResolver.delete(ContactsContract.Data.CONTENT_URI, str, null));
    }

    private void updateAdditionalNumberToDB(ContentResolver contentResolver) {
        if (GlobalEnv.getSimAasEditor().updateAdditionalNumberToDB(this.mIntent, this.mRawContactId)) {
            Log.i("SimEditProcessor", "[updateAdditionalNumberToDB],handle by plugin..");
            return;
        }
        ContentValues contentValues = new ContentValues();
        String str = "raw_contact_id = '" + this.mRawContactId + "' AND mimetype='vnd.android.cursor.item/phone_v2' AND is_additional_number =1";
        Log.sensitive("SimEditProcessor", "[updateAdditionalNumberToDB]whereadditional:" + str);
        if (!TextUtils.isEmpty(this.mUpdateAdditionalNumber)) {
            if (!TextUtils.isEmpty(this.mOldOtherPhone)) {
                contentValues.put("data1", sOtherPhone);
                Log.d("SimEditProcessor", "[updateAdditionalNumberToDB]update ret:" + contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, str, null));
                return;
            }
            contentValues.put("raw_contact_id", Long.valueOf(this.mRawContactId));
            contentValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
            contentValues.put("data1", this.mUpdateAdditionalNumber);
            contentValues.put("is_additional_number", (Integer) 1);
            contentValues.put("data2", (Integer) 7);
            Log.d("SimEditProcessor", "[updateAdditionalNumberToDB]url insert ret:" + Log.anonymize(contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)));
            return;
        }
        Log.d("SimEditProcessor", "[updateAdditionalNumberToDB]delete ret: " + contentResolver.delete(ContactsContract.Data.CONTENT_URI, str, null));
    }

    private void updateEmail(ContentResolver contentResolver) {
        ContentValues contentValues = new ContentValues();
        String str = "raw_contact_id = '" + this.mRawContactId + "' AND mimetype='vnd.android.cursor.item/email_v2'";
        Log.sensitive("SimEditProcessor", "[updateEmail]wheremail:" + str);
        if (!TextUtils.isEmpty(this.mUpdatemail)) {
            if (!TextUtils.isEmpty(this.mOldEmail)) {
                contentValues.put("data1", this.mUpdatemail);
                Log.d("SimEditProcessor", "[updateEmail]update ret:" + contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, str, null));
                return;
            }
            contentValues.put("raw_contact_id", Long.valueOf(this.mRawContactId));
            contentValues.put("mimetype", "vnd.android.cursor.item/email_v2");
            contentValues.put("data1", this.mUpdatemail);
            Log.d("SimEditProcessor", "[updateEmail]Uri insert ret:" + Log.anonymize(contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)));
            return;
        }
        Log.d("SimEditProcessor", "[updateEmail]delete ret:" + contentResolver.delete(ContactsContract.Data.CONTENT_URI, str, null));
    }

    private int upateGroup(ContentResolver contentResolver, int i) {
        int iSyncUSIMGroupNewIfMissing;
        int iHasExistGroup;
        if (this.mOldGroupAddList.size() > 0) {
            for (Map.Entry<Long, String> entry : this.mOldGroupAddList.entrySet()) {
                long jLongValue = entry.getKey().longValue();
                try {
                    iHasExistGroup = ContactsGroupUtils.USIMGroup.hasExistGroup(this.mSubId, entry.getValue());
                } catch (RemoteException e) {
                    iHasExistGroup = -1;
                }
                if (iHasExistGroup > 0) {
                    ContactsGroupUtils.USIMGroup.deleteUSIMGroupMember(this.mSubId, this.mIndexInSim, iHasExistGroup);
                }
                int iDelete = contentResolver.delete(ContactsContract.Data.CONTENT_URI, "mimetype='vnd.android.cursor.item/group_membership' AND raw_contact_id=" + this.mRawContactId + " AND data1=" + jLongValue, null);
                StringBuilder sb = new StringBuilder();
                sb.append("[upateGroup]DB deleteCount:");
                sb.append(iDelete);
                Log.d("SimEditProcessor", sb.toString());
            }
        }
        if (this.mGroupAddList.size() > 0) {
            for (Map.Entry<Long, String> entry2 : this.mGroupAddList.entrySet()) {
                long jLongValue2 = entry2.getKey().longValue();
                try {
                    iSyncUSIMGroupNewIfMissing = ContactsGroupUtils.USIMGroup.syncUSIMGroupNewIfMissing(this.mSubId, entry2.getValue());
                } catch (RemoteException e2) {
                    Log.w("SimEditProcessor", "[upateGroup]RemoteException:" + e2.toString());
                    iSyncUSIMGroupNewIfMissing = -1;
                } catch (ContactsGroupUtils.USIMGroupException e3) {
                    int errorType = e3.getErrorType();
                    Log.w("SimEditProcessor", "[upateGroup]errorType:" + errorType + ",USIMGroupException:" + e3.toString());
                    i = errorType;
                    iSyncUSIMGroupNewIfMissing = -1;
                }
                if (iSyncUSIMGroupNewIfMissing > 0) {
                    Log.d("SimEditProcessor", "[upateGroup]addUSIMGroupMember suFlag:" + ContactsGroupUtils.USIMGroup.addUSIMGroupMember(this.mSubId, this.mIndexInSim, iSyncUSIMGroupNewIfMissing) + ",ugrpId:" + iSyncUSIMGroupNewIfMissing);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("mimetype", "vnd.android.cursor.item/group_membership");
                    contentValues.put("data1", Long.valueOf(jLongValue2));
                    contentValues.put("raw_contact_id", Long.valueOf(this.mRawContactId));
                    contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
                }
            }
        }
        return i;
    }

    private int insertGroupToUSIMCard(long j, int i) throws RemoteException, ContactsGroupUtils.USIMGroupException {
        Iterator<Map.Entry<Long, String>> it = this.mGroupAddList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, String> next = it.next();
            next.getKey().longValue();
            int iSyncUSIMGroupNewIfMissing = -1;
            try {
                iSyncUSIMGroupNewIfMissing = ContactsGroupUtils.USIMGroup.syncUSIMGroupNewIfMissing(this.mSubId, next.getValue());
            } catch (RemoteException e) {
                Log.w("SimEditProcessor", "[insertGroupToUSIMCard]RemoteException: " + e.toString());
            } catch (ContactsGroupUtils.USIMGroupException e2) {
                int errorType = e2.getErrorType();
                Log.w("SimEditProcessor", "[insertGroupToUSIMCard]errorType:" + errorType + ",USIMGroupException: " + e2.toString());
                i = errorType;
            }
            if (iSyncUSIMGroupNewIfMissing > 0) {
                Log.d("SimEditProcessor", "[insertGroupToUSIMCard]addUSIMGroupMember suFlag:" + ContactsGroupUtils.USIMGroup.addUSIMGroupMember(this.mSubId, (int) j, iSyncUSIMGroupNewIfMissing) + ",ugrpId:" + iSyncUSIMGroupNewIfMissing);
            } else {
                it.remove();
            }
        }
        return i;
    }

    private boolean isInsertValuesInvalid() {
        String nickName = GlobalEnv.getSimSneEditor().getNickName(this.mIntent, this.mSubId);
        if ("USIM".equals(this.mSimType)) {
            String str = this.mUpdateName;
            if (!TextUtils.isEmpty(this.mUpdateName)) {
                str = new SubContactsUtils.NamePhoneTypePair(this.mUpdateName).name;
                Log.d("SimEditProcessor", "fix name: " + Log.anonymize(str));
            }
            if (TextUtils.isEmpty(str) && TextUtils.isEmpty(this.mUpdatephone) && TextUtils.isEmpty(this.mUpdatemail) && TextUtils.isEmpty(this.mUpdateAdditionalNumber) && this.mGroupAddList.isEmpty() && TextUtils.isEmpty(nickName)) {
                this.mAllEditorInvalid = true;
            } else if (TextUtils.isEmpty(this.mUpdatephone) && TextUtils.isEmpty(this.mUpdateName) && (!TextUtils.isEmpty(this.mUpdatemail) || !TextUtils.isEmpty(this.mUpdateAdditionalNumber) || !this.mGroupAddList.isEmpty() || !TextUtils.isEmpty(nickName))) {
                this.mNumberIsNull = true;
            } else if (!TextUtils.isEmpty(this.mUpdatephone) && !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(this.mUpdatephone))) {
                this.mNumberInvalid = true;
            }
            if (!TextUtils.isEmpty(this.mUpdateAdditionalNumber) && !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(this.mUpdateAdditionalNumber))) {
                this.mFixNumberInvalid = true;
            }
        } else if (TextUtils.isEmpty(this.mUpdatephone) && TextUtils.isEmpty(this.mUpdateName)) {
            this.mAllEditorInvalid = true;
        } else if (!TextUtils.isEmpty(this.mUpdatephone) && !Pattern.matches("[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*", MtkPhoneNumberUtils.extractCLIRPortion(this.mUpdatephone))) {
            this.mNumberInvalid = true;
        }
        if (!TextUtils.isEmpty(nickName) && !GlobalEnv.getSimSneEditor().isSneNicknameValid(nickName, this.mSubId)) {
            this.mNickNameInvalid = true;
        }
        setSaveFailToastText();
        if (this.mSaveFailToastStrId >= 0) {
            Log.i("SimEditProcessor", "[isInsertValuesInvalid]setSaveFailToastText,return true.");
            return true;
        }
        return false;
    }

    private void updateUSIMSpecValues(ContentValues contentValues) {
        this.mUpdatemail = sEmail;
        this.mUpdateAdditionalNumber = sAfterOtherPhone;
        Log.d("SimEditProcessor", "[updateUSIMSpecValues]before replace, mUpdateAdditionalNumber is:" + Log.anonymize(this.mUpdateAdditionalNumber));
        if (!TextUtils.isEmpty(this.mUpdateAdditionalNumber)) {
            this.mUpdateAdditionalNumber = this.mUpdateAdditionalNumber.replaceAll("-", "");
            this.mUpdateAdditionalNumber = this.mUpdateAdditionalNumber.replaceAll(" ", "");
            Log.i("SimEditProcessor", "[updateUSIMSpecValues]after replace, mUpdateAdditionalNumber is: " + Log.anonymize(this.mUpdateAdditionalNumber));
        }
        contentValues.put("anr", TextUtils.isEmpty(this.mUpdateAdditionalNumber) ? "" : this.mUpdateAdditionalNumber);
        contentValues.put("emails", TextUtils.isEmpty(this.mUpdatemail) ? "" : this.mUpdatemail);
    }

    private boolean compareData() {
        boolean z = TextUtils.isEmpty(sName) || TextUtils.isEmpty(this.mOldName) ? TextUtils.isEmpty(sName) && TextUtils.isEmpty(this.mOldName) : sName.equals(this.mOldName);
        boolean z2 = TextUtils.isEmpty(sPhone) || TextUtils.isEmpty(this.mOldPhone) ? TextUtils.isEmpty(sPhone) && TextUtils.isEmpty(this.mOldPhone) : sPhone.equals(this.mOldPhone);
        boolean z3 = TextUtils.isEmpty(sEmail) || TextUtils.isEmpty(this.mOldEmail) ? TextUtils.isEmpty(sEmail) && TextUtils.isEmpty(this.mOldEmail) : sEmail.equals(this.mOldEmail);
        boolean z4 = TextUtils.isEmpty(sOtherPhone) || TextUtils.isEmpty(this.mOldOtherPhone) ? TextUtils.isEmpty(sOtherPhone) && TextUtils.isEmpty(this.mOldOtherPhone) : sOtherPhone.equals(this.mOldOtherPhone);
        boolean z5 = this.mGroupAddList == null || this.mOldGroupAddList == null ? this.mGroupAddList == null && this.mOldGroupAddList == null : this.mGroupAddList.equals(this.mOldGroupAddList);
        Log.i("SimEditProcessor", "[compareData]compareName : " + z + " | comparePhone : " + z2 + " | compareOther : " + z4 + " | compareEmail: " + z3 + " | compareGroup : " + z5);
        StringBuilder sb = new StringBuilder();
        sb.append("[compareData] mOldName : ");
        sb.append(Log.anonymize(this.mOldName));
        sb.append(" | mOldEmail : ");
        sb.append(Log.anonymize(this.mOldEmail));
        sb.append(" | mOldPhone: ");
        sb.append(Log.anonymize(this.mOldPhone));
        sb.append(" | mOldOtherPhone : ");
        sb.append(Log.anonymize(this.mOldOtherPhone));
        Log.i("SimEditProcessor", sb.toString());
        Log.i("SimEditProcessor", "[compareData] sName : " + Log.anonymize(sName) + " | sEmail : " + Log.anonymize(sEmail) + " | sPhone : " + Log.anonymize(sPhone) + " | sOtherPhone : " + Log.anonymize(sOtherPhone));
        return z && z2 && z4 && z3 && z5;
    }

    private boolean isPhbReady() {
        this.mPhbReady = SimCardUtils.isPhoneBookReady(this.mSubId);
        return this.mPhbReady;
    }

    private void deliverCallback(Uri uri) {
        Log.i("SimEditProcessor", "[deliverCallback]RESULT_OK---");
        Intent intent = (Intent) this.mIntent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
        if (intent != null) {
            intent.putExtra("result", 1);
            intent.setAction("com.mediatek.contacts.simservice.EDIT_SIM");
            intent.setData(uri);
            deliverCallbackOnUiThread(intent);
            return;
        }
        Log.w("SimEditProcessor", "IllegalStateException: callbackIntent == NULL!");
    }

    private void deliverCallbackOnUiThread(final Intent intent) {
        for (final Listener listener : sListeners) {
            Handler handler = sListenerHolder.get(listener);
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSIMEditCompleted(intent);
                    }
                });
            }
        }
    }

    private void deliverCallbackOnError() {
        Log.i("SimEditProcessor", "[deliverCallbackOnError]RESULT_NO_DATA---");
        Intent intent = (Intent) this.mIntent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
        Intent intent2 = new Intent();
        if (intent == null) {
            intent = intent2;
        }
        intent.putExtra("result", 2);
        intent.putExtra("isQuitEdit", this.mQuitEdit);
        intent.setAction("com.mediatek.contacts.simservice.EDIT_SIM");
        deliverCallbackOnUiThread(intent);
    }

    public void deliverCallbackAndBackToFragment() {
        Log.i("SimEditProcessor", "[deliverCallbackAndBackToFragment]RESULT_CANCELED---");
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("simData", this.mSimData);
        intent.putExtra("isQuitEdit", this.mQuitEdit);
        intent.putExtra("result", 0);
        intent.setAction("com.mediatek.contacts.simservice.EDIT_SIM");
        deliverCallbackOnUiThread(intent);
    }

    private void showToastMessage(int i, String str) {
        Iterator<Listener> it = sListeners.iterator();
        while (it.hasNext()) {
            Handler handler = sListenerHolder.get(it.next());
            if (handler != null) {
                Message messageObtainMessage = handler.obtainMessage();
                messageObtainMessage.arg1 = i;
                Bundle bundle = new Bundle();
                bundle.putString("content", str);
                messageObtainMessage.setData(bundle);
                handler.sendMessage(messageObtainMessage);
                Log.i("SimEditProcessor", "[showToastMessage]");
            }
        }
    }

    private void initStaticValues() {
        sName = null;
        sEmail = null;
        sPhone = null;
        sOtherPhone = null;
        sAfterPhone = null;
        sAfterOtherPhone = null;
    }

    public void onAddToServiceFail() {
        showToastMessage(R.string.phone_book_busy, null);
        this.mQuitEdit = false;
        deliverCallbackOnError();
    }
}
