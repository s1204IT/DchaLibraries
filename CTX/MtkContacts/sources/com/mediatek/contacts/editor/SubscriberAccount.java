package com.mediatek.contacts.editor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.model.Contact;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.account.AccountWithDataSet;
import com.google.common.collect.ImmutableList;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.ProgressHandler;

public class SubscriberAccount {
    private static String TAG = "SubscriberAccount";
    private RawContactDeltaList mOldState;
    private int mIndicatePhoneOrSimContact = -1;
    private int mSaveModeForSim = 0;
    private int mSubId = SubInfoUtils.getInvalidSubId();
    private int mSimIndex = -1;
    ProgressHandler mProgressHandler = new ProgressHandler();

    public void setSimInfo(AccountWithDataSet accountWithDataSet) {
        if (!(accountWithDataSet instanceof AccountWithDataSetEx)) {
            Log.sensitive(TAG, "[setSimInfo] not sim account, account=" + accountWithDataSet);
            return;
        }
        this.mSubId = ((AccountWithDataSetEx) accountWithDataSet).getSubId();
        GlobalEnv.getSimAasEditor().setCurrentSubId(this.mSubId);
        Log.d(TAG, "[setSimInfo] subId=" + this.mSubId);
    }

    public void clearSimInfo() {
        this.mSubId = -1;
        GlobalEnv.getSimAasEditor().setCurrentSubId(this.mSubId);
    }

    public void insertRawDataToSim(ImmutableList<RawContact> immutableList) {
        RawContactDeltaList rawContactDeltaListFromIterator = RawContactDeltaList.fromIterator(immutableList.iterator());
        if (!isAccountTypeIccCard(rawContactDeltaListFromIterator)) {
            return;
        }
        Log.d(TAG, "[insertRawDataToSim] keep mOldState for sim contact");
        setSimSaveMode(2);
        this.mOldState = rawContactDeltaListFromIterator;
        GlobalEnv.getSimAasEditor().setOldAasIndicatorAndNames(this.mOldState);
        ContactEditorUtilsEx.showLogContactState(this.mOldState);
    }

    public void initIccCard(Contact contact) {
        this.mIndicatePhoneOrSimContact = contact.getIndicate();
        this.mSubId = contact.getIndicate();
        Log.i(TAG, "[initIccCard]mSubId = " + this.mSubId + ", mIndicatePhoneOrSimContact = " + this.mIndicatePhoneOrSimContact);
    }

    public void restoreSimAndSubId(Bundle bundle) {
        Log.d(TAG, "[restoreSimAndSubId]");
        this.mSubId = bundle.getInt("key_subid");
        this.mSaveModeForSim = bundle.getInt("key_savemode_for_sim");
        this.mIndicatePhoneOrSimContact = bundle.getInt("key_indicate_phone_or_sim");
        this.mOldState = (RawContactDeltaList) bundle.getParcelable("key_oldstate");
        this.mSimIndex = bundle.getInt("key_sim_index");
        Log.i(TAG, "[restoreSimAndSubId] mSubId : " + this.mSubId + " | mSaveModeForSim : " + this.mSaveModeForSim);
        ContactEditorUtilsEx.showLogContactState(this.mOldState);
    }

    public void setSimSaveMode(int i) {
        this.mSaveModeForSim = i;
    }

    public void onSaveInstanceStateSim(Bundle bundle) {
        bundle.putInt("key_subid", this.mSubId);
        bundle.putInt("key_savemode_for_sim", this.mSaveModeForSim);
        bundle.putInt("key_indicate_phone_or_sim", this.mIndicatePhoneOrSimContact);
        bundle.putInt("key_sim_index", this.mSimIndex);
        if (this.mOldState != null && this.mOldState.size() > 0) {
            bundle.putParcelable("key_oldstate", this.mOldState);
        }
        Log.sensitive(TAG, "[onSaveInstanceStateSim] mSubId : " + this.mSubId + ", mSaveModeForSim : " + this.mSaveModeForSim + ", mIndicatePhoneOrSimContact : " + this.mIndicatePhoneOrSimContact + ", mSimIndex : " + this.mSimIndex + ", mOldState : " + this.mOldState);
    }

    public void setAccountChangedSim(Intent intent, Context context) {
        ContactEditorUtilsEx.setInputMethodVisible(true, (ContactEditorActivity) context);
        this.mSubId = intent.getIntExtra("mSubId", -1);
        Log.i(TAG, "[setAccountChangedSim]msubId: " + this.mSubId);
    }

    public void processSaveToSim(Intent intent, Uri uri) {
        Log.i(TAG, "[processSaveToSim]mSaveModeForSim = " + this.mSaveModeForSim + ", mIndicatePhoneOrSimContact = " + this.mIndicatePhoneOrSimContact + ", mSimIndex = " + this.mSimIndex);
        if (this.mSaveModeForSim == 1) {
            intent.putExtra("indicate_phone_or_sim_contact", this.mSubId);
        } else if (this.mSaveModeForSim == 2) {
            intent.putExtra("indicate_phone_or_sim_contact", this.mIndicatePhoneOrSimContact);
            intent.putExtra("simIndex", this.mSimIndex);
        }
        intent.putExtra("simSaveMode", this.mSaveModeForSim);
        intent.setData(uri);
    }

    public boolean setAccountSimInfo(RawContactDelta rawContactDelta, AccountWithDataSet accountWithDataSet, Context context) {
        ContactEditorActivity contactEditorActivity = (ContactEditorActivity) context;
        if (accountWithDataSet instanceof AccountWithDataSetEx) {
            AccountWithDataSetEx accountWithDataSetEx = (AccountWithDataSetEx) accountWithDataSet;
            if (!SimCardUtils.checkPHBStateAndSimStorage(contactEditorActivity, accountWithDataSetEx.getSubId())) {
                return true;
            }
            if (rawContactDelta != null && rawContactDelta.hasMimeEntries("vnd.android.cursor.item/photo")) {
                rawContactDelta.removeEntry("vnd.android.cursor.item/photo");
                Log.d(TAG, "[setAccountSimInfo]remove photo in currentState as switch to sim account");
            }
            setSimInfo(accountWithDataSetEx);
        } else {
            clearSimInfo();
        }
        if (rawContactDelta != null && rawContactDelta.hasMimeEntries("vnd.android.cursor.item/group_membership")) {
            rawContactDelta.removeEntry("vnd.android.cursor.item/group_membership");
            return false;
        }
        return false;
    }

    public void setAndCheckSimInfo(Context context, AccountWithDataSet accountWithDataSet) {
        if (!(accountWithDataSet instanceof AccountWithDataSetEx)) {
            return;
        }
        setSimInfo(accountWithDataSet);
        SimCardUtils.ShowSimCardStorageInfoTask.showSimCardStorageInfo(context, true, ((AccountWithDataSetEx) accountWithDataSet).getSubId());
    }

    public boolean isAccountTypeIccCard(RawContactDeltaList rawContactDeltaList) {
        if (!rawContactDeltaList.isEmpty()) {
            String asString = rawContactDeltaList.get(0).getValues().getAsString("account_type");
            ContactEditorUtilsEx.showLogContactState(rawContactDeltaList);
            if (AccountTypeUtils.isAccountTypeIccCard(asString)) {
                return true;
            }
        }
        return false;
    }

    public void setIndicatePhoneOrSimContact(int i) {
        this.mIndicatePhoneOrSimContact = i;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public RawContactDeltaList getOldState() {
        return this.mOldState;
    }

    public void setSimIndex(int i) {
        this.mSimIndex = i;
    }

    public ProgressHandler getProgressHandler() {
        return this.mProgressHandler;
    }
}
