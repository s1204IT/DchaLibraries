package com.android.phone;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;

public class GsmUmtsAdditionalCallOptions extends TimeConsumingPreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private static final String BUTTON_CLIR_KEY = "button_clir_key";
    private static final String BUTTON_CW_KEY = "button_cw_key";
    private static final String KEY_STATE = "state";
    private static final String KEY_TOGGLE = "toggle";
    private static final String LOG_TAG = "GsmUmtsAdditionalCallOptions";
    private CLIRListPreference mCLIRButton;
    private CallWaitingSwitchPreference mCWButton;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private final boolean DBG = true;
    private final ArrayList<Preference> mPreferences = new ArrayList<>();
    private int mInitIndex = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.gsm_umts_additional_options);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.additional_gsm_call_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        if (this.mPhone == null) {
            Log.d(LOG_TAG, "onCreate: mPhone is null, finish!!!");
            finish();
            return;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mCLIRButton = (CLIRListPreference) preferenceScreen.findPreference(BUTTON_CLIR_KEY);
        this.mCWButton = (CallWaitingSwitchPreference) preferenceScreen.findPreference(BUTTON_CW_KEY);
        this.mPreferences.add(this.mCLIRButton);
        this.mPreferences.add(this.mCWButton);
        this.mIsForeground = true;
        ExtensionManager.getCallFeaturesSettingExt().customizeAdditionalSettings(this, this.mPhone);
        if (bundle == null) {
            Log.d(LOG_TAG, "start to init ");
            this.mCLIRButton.init(this, false, this.mPhone);
        } else {
            Log.d(LOG_TAG, "restore stored states");
            this.mInitIndex = this.mPreferences.size();
            this.mCLIRButton.init(this, true, this.mPhone);
            this.mCWButton.init(this, true, this.mPhone);
            int[] intArray = bundle.getIntArray(this.mCLIRButton.getKey());
            if (intArray != null) {
                Log.d(LOG_TAG, "onCreate:  clirArray[0]=" + intArray[0] + ", clirArray[1]=" + intArray[1]);
                this.mCLIRButton.handleGetCLIRResult(intArray);
            } else {
                this.mCLIRButton.init(this, false, this.mPhone);
            }
            Bundle bundle2 = (Bundle) bundle.getParcelable(this.mCWButton.getKey());
            if (bundle2 != null) {
                this.mCWButton.setChecked(bundle2.getBoolean(KEY_TOGGLE));
                this.mCWButton.setEnabled(bundle2.getBoolean(KEY_STATE));
            }
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mCLIRButton != null && this.mCLIRButton.clirArray != null) {
            bundle.putIntArray(this.mCLIRButton.getKey(), this.mCLIRButton.clirArray);
        }
        if (this.mCWButton != null) {
            Bundle bundle2 = new Bundle();
            bundle2.putBoolean(KEY_TOGGLE, this.mCWButton.isChecked());
            bundle2.putBoolean(KEY_STATE, this.mCWButton.isEnabled());
            bundle.putBundle(this.mCWButton.getKey(), bundle2);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean z) {
        if (this.mInitIndex < this.mPreferences.size() - 1 && !isFinishing()) {
            this.mInitIndex++;
            Preference preference2 = this.mPreferences.get(this.mInitIndex);
            if (preference2 instanceof CallWaitingSwitchPreference) {
                ((CallWaitingSwitchPreference) preference2).init(this, false, this.mPhone);
            }
        }
        super.onFinished(preference, z);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            CallFeaturesSetting.goUpToTopLevelSetting(this, this.mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
