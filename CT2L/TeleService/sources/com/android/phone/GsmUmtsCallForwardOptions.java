package com.android.phone;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;
import java.util.ArrayList;

public class GsmUmtsCallForwardOptions extends TimeConsumingPreferenceActivity {
    private static final String[] NUM_PROJECTION = {"data1"};
    private CallForwardEditPreference mButtonCFB;
    private CallForwardEditPreference mButtonCFNRc;
    private CallForwardEditPreference mButtonCFNRy;
    private CallForwardEditPreference mButtonCFU;
    private boolean mFirstResume;
    private Bundle mIcicle;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private final boolean DBG = true;
    private final ArrayList<CallForwardEditPreference> mPreferences = new ArrayList<>();
    private int mInitIndex = 0;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.callforward_options);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_forwarding_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        PreferenceScreen prefSet = getPreferenceScreen();
        this.mButtonCFU = (CallForwardEditPreference) prefSet.findPreference("button_cfu_key");
        this.mButtonCFB = (CallForwardEditPreference) prefSet.findPreference("button_cfb_key");
        this.mButtonCFNRy = (CallForwardEditPreference) prefSet.findPreference("button_cfnry_key");
        this.mButtonCFNRc = (CallForwardEditPreference) prefSet.findPreference("button_cfnrc_key");
        this.mButtonCFU.setParentActivity(this, this.mButtonCFU.reason);
        this.mButtonCFB.setParentActivity(this, this.mButtonCFB.reason);
        this.mButtonCFNRy.setParentActivity(this, this.mButtonCFNRy.reason);
        this.mButtonCFNRc.setParentActivity(this, this.mButtonCFNRc.reason);
        this.mPreferences.add(this.mButtonCFU);
        this.mPreferences.add(this.mButtonCFB);
        this.mPreferences.add(this.mButtonCFNRy);
        this.mPreferences.add(this.mButtonCFNRc);
        this.mFirstResume = true;
        this.mIcicle = icicle;
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mFirstResume) {
            if (this.mIcicle == null) {
                Log.d("GsmUmtsCallForwardOptions", "start to init ");
                this.mPreferences.get(this.mInitIndex).init(this, false, this.mPhone);
            } else {
                this.mInitIndex = this.mPreferences.size();
                for (CallForwardEditPreference pref : this.mPreferences) {
                    Bundle bundle = (Bundle) this.mIcicle.getParcelable(pref.getKey());
                    pref.setToggled(bundle.getBoolean("toggle"));
                    CallForwardInfo cf = new CallForwardInfo();
                    cf.number = bundle.getString("number");
                    cf.status = bundle.getInt("status");
                    pref.handleCallForwardResult(cf);
                    pref.init(this, true, this.mPhone);
                }
            }
            this.mFirstResume = false;
            this.mIcicle = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        for (CallForwardEditPreference pref : this.mPreferences) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("toggle", pref.isToggled());
            if (pref.callForwardInfo != null) {
                bundle.putString("number", pref.callForwardInfo.number);
                bundle.putInt("status", pref.callForwardInfo.status);
            }
            outState.putParcelable(pref.getKey(), bundle);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (this.mInitIndex < this.mPreferences.size() - 1 && !isFinishing()) {
            this.mInitIndex++;
            this.mPreferences.get(this.mInitIndex).init(this, false, this.mPhone);
        }
        super.onFinished(preference, reading);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("GsmUmtsCallForwardOptions", "onActivityResult: done");
        if (resultCode != -1) {
            Log.d("GsmUmtsCallForwardOptions", "onActivityResult: contact picker result not OK.");
            return;
        }
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(data.getData(), NUM_PROJECTION, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.d("GsmUmtsCallForwardOptions", "onActivityResult: bad contact data, no results found.");
                if (cursor != null) {
                    return;
                } else {
                    return;
                }
            }
            switch (requestCode) {
                case 0:
                    this.mButtonCFU.onPickActivityResult(cursor.getString(0));
                    break;
                case 1:
                    this.mButtonCFB.onPickActivityResult(cursor.getString(0));
                    break;
                case 2:
                    this.mButtonCFNRy.onPickActivityResult(cursor.getString(0));
                    break;
                case 3:
                    this.mButtonCFNRc.onPickActivityResult(cursor.getString(0));
                    break;
            }
            if (cursor != null) {
                cursor.close();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        CallFeaturesSetting.goUpToTopLevelSetting(this, this.mSubscriptionInfoHelper);
        return true;
    }
}
