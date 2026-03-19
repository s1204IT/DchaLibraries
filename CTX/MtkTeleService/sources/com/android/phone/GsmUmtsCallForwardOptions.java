package com.android.phone;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.mediatek.settings.vtss.GsmUmtsVTUtils;
import java.util.ArrayList;

public class GsmUmtsCallForwardOptions extends TimeConsumingPreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private static final String BUTTON_CFB_KEY = "button_cfb_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFU_KEY = "button_cfu_key";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_STATE = "state";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TOGGLE = "toggle";
    private static final String LOG_TAG = "GsmUmtsCallForwardOptions";
    private static final String[] NUM_PROJECTION = {"data1"};
    private CallForwardEditPreference mButtonCFB;
    private CallForwardEditPreference mButtonCFNRc;
    private CallForwardEditPreference mButtonCFNRy;
    private CallForwardEditPreference mButtonCFU;
    private boolean mFirstResume;
    private Bundle mIcicle;
    private IntentFilter mIntentFilter;
    private Phone mPhone;
    private boolean mReplaceInvalidCFNumbers;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private final ArrayList<CallForwardEditPreference> mPreferences = new ArrayList<>();
    private int mInitIndex = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction()) && intent.getBooleanExtra(GsmUmtsCallForwardOptions.KEY_STATE, false)) {
                GsmUmtsCallForwardOptions.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.callforward_options);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_forwarding_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        registerCallBacks();
        if (this.mPhone == null) {
            Log.d(LOG_TAG, "onCreate: mPhone is null, finish!!!");
            finish();
            return;
        }
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        if (carrierConfigForSubId != null) {
            this.mReplaceInvalidCFNumbers = carrierConfigForSubId.getBoolean("call_forwarding_map_non_number_to_voicemail_bool");
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mButtonCFU = (CallForwardEditPreference) preferenceScreen.findPreference(BUTTON_CFU_KEY);
        this.mButtonCFB = (CallForwardEditPreference) preferenceScreen.findPreference(BUTTON_CFB_KEY);
        this.mButtonCFNRy = (CallForwardEditPreference) preferenceScreen.findPreference(BUTTON_CFNRY_KEY);
        this.mButtonCFNRc = (CallForwardEditPreference) preferenceScreen.findPreference(BUTTON_CFNRC_KEY);
        if (carrierConfigForSubId.getBoolean("mtk_support_vt_ss_bool")) {
            int intExtra = getIntent().getIntExtra("service_class", 1);
            GsmUmtsVTUtils.setCFServiceClass(preferenceScreen, intExtra);
            this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), GsmUmtsVTUtils.getActionBarResId(intExtra, 0));
        }
        this.mButtonCFU.setParentActivity(this, this.mButtonCFU.reason);
        this.mButtonCFB.setParentActivity(this, this.mButtonCFB.reason);
        this.mButtonCFNRy.setParentActivity(this, this.mButtonCFNRy.reason);
        this.mButtonCFNRc.setParentActivity(this, this.mButtonCFNRc.reason);
        this.mPreferences.add(this.mButtonCFU);
        this.mPreferences.add(this.mButtonCFB);
        this.mPreferences.add(this.mButtonCFNRy);
        this.mPreferences.add(this.mButtonCFNRc);
        this.mFirstResume = true;
        this.mIcicle = bundle;
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
                Log.d(LOG_TAG, "start to init ");
                this.mPreferences.get(this.mInitIndex).init(this, false, this.mPhone, this.mReplaceInvalidCFNumbers);
            } else {
                this.mInitIndex = this.mPreferences.size();
                for (CallForwardEditPreference callForwardEditPreference : this.mPreferences) {
                    Bundle bundle = (Bundle) this.mIcicle.getParcelable(callForwardEditPreference.getKey());
                    callForwardEditPreference.setToggled(bundle.getBoolean(KEY_TOGGLE));
                    callForwardEditPreference.setEnabled(bundle.getBoolean(KEY_STATE));
                    CallForwardInfo callForwardInfo = new CallForwardInfo();
                    callForwardInfo.number = bundle.getString(KEY_NUMBER);
                    callForwardInfo.status = bundle.getInt(KEY_STATUS);
                    callForwardEditPreference.handleCallForwardResult(callForwardInfo);
                    callForwardEditPreference.init(this, true, this.mPhone, this.mReplaceInvalidCFNumbers);
                }
            }
            this.mFirstResume = false;
            this.mIcicle = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (CallForwardEditPreference callForwardEditPreference : this.mPreferences) {
            Bundle bundle2 = new Bundle();
            bundle2.putBoolean(KEY_TOGGLE, callForwardEditPreference.isToggled());
            bundle2.putBoolean(KEY_STATE, callForwardEditPreference.isEnabled());
            if (callForwardEditPreference.callForwardInfo != null) {
                bundle2.putString(KEY_NUMBER, callForwardEditPreference.callForwardInfo.number);
                bundle2.putInt(KEY_STATUS, callForwardEditPreference.callForwardInfo.status);
            }
            bundle.putParcelable(callForwardEditPreference.getKey(), bundle2);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean z) {
        boolean zHasUtError;
        if (preference instanceof CallForwardEditPreference) {
            zHasUtError = ((CallForwardEditPreference) preference).hasUtError();
        } else {
            zHasUtError = false;
        }
        if (this.mInitIndex < this.mPreferences.size() - 1 && !isFinishing() && !zHasUtError) {
            this.mInitIndex++;
            this.mPreferences.get(this.mInitIndex).init(this, false, this.mPhone, this.mReplaceInvalidCFNumbers);
        }
        super.onFinished(preference, z);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) throws Throwable {
        Cursor cursorQuery;
        Log.d(LOG_TAG, "onActivityResult: done");
        if (i2 != -1) {
            Log.d(LOG_TAG, "onActivityResult: contact picker result not OK.");
            return;
        }
        try {
            cursorQuery = getContentResolver().query(intent.getData(), NUM_PROJECTION, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        switch (i) {
                            case 0:
                                this.mButtonCFU.onPickActivityResult(cursorQuery.getString(0));
                                break;
                            case 1:
                                this.mButtonCFB.onPickActivityResult(cursorQuery.getString(0));
                                break;
                            case 2:
                                this.mButtonCFNRy.onPickActivityResult(cursorQuery.getString(0));
                                break;
                            case 3:
                                this.mButtonCFNRc.onPickActivityResult(cursorQuery.getString(0));
                                break;
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            return;
                        }
                        return;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            Log.d(LOG_TAG, "onActivityResult: bad contact data, no results found.");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            CallFeaturesSetting.goUpToTopLevelSetting(this, this.mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void registerCallBacks() {
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mReceiver);
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
