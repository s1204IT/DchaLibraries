package com.android.phone;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import com.android.internal.telephony.Phone;
import java.util.ArrayList;

public class GsmUmtsAdditionalCallOptions extends TimeConsumingPreferenceActivity {
    private CLIRListPreference mCLIRButton;
    private CallWaitingCheckBoxPreference mCWButton;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private final boolean DBG = true;
    private final ArrayList<Preference> mPreferences = new ArrayList<>();
    private int mInitIndex = 0;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gsm_umts_additional_options);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.additional_gsm_call_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        PreferenceScreen prefSet = getPreferenceScreen();
        this.mCLIRButton = (CLIRListPreference) prefSet.findPreference("button_clir_key");
        this.mCWButton = (CallWaitingCheckBoxPreference) prefSet.findPreference("button_cw_key");
        this.mPreferences.add(this.mCLIRButton);
        this.mPreferences.add(this.mCWButton);
        if (icicle == null) {
            Log.d("GsmUmtsAdditionalCallOptions", "start to init ");
            this.mCLIRButton.init(this, false, this.mPhone);
        } else {
            Log.d("GsmUmtsAdditionalCallOptions", "restore stored states");
            this.mInitIndex = this.mPreferences.size();
            this.mCLIRButton.init(this, true, this.mPhone);
            this.mCWButton.init(this, true, this.mPhone);
            int[] clirArray = icicle.getIntArray(this.mCLIRButton.getKey());
            if (clirArray != null) {
                Log.d("GsmUmtsAdditionalCallOptions", "onCreate:  clirArray[0]=" + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                this.mCLIRButton.handleGetCLIRResult(clirArray);
            } else {
                this.mCLIRButton.init(this, false, this.mPhone);
            }
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mCLIRButton.clirArray != null) {
            outState.putIntArray(this.mCLIRButton.getKey(), this.mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (this.mInitIndex < this.mPreferences.size() - 1 && !isFinishing()) {
            this.mInitIndex++;
            Preference pref = this.mPreferences.get(this.mInitIndex);
            if (pref instanceof CallWaitingCheckBoxPreference) {
                ((CallWaitingCheckBoxPreference) pref).init(this, false, this.mPhone);
            }
        }
        super.onFinished(preference, reading);
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
