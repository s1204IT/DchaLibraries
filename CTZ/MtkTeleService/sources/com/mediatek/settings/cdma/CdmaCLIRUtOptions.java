package com.mediatek.settings.cdma;

import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import com.android.internal.telephony.Phone;
import com.android.phone.CLIRListPreference;
import com.android.phone.CallFeaturesSetting;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.TimeConsumingPreferenceActivity;

public class CdmaCLIRUtOptions extends TimeConsumingPreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private CLIRListPreference mCLIRPreference;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.mtk_cdma_clir_options);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.mtk_caller_id_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        if (this.mPhone == null) {
            Log.d("CdmaCLIRUtOptions", "onCreate: mPhone is null, finish!!!");
            finish();
            return;
        }
        this.mCLIRPreference = (CLIRListPreference) getPreferenceScreen().findPreference("button_clir_key");
        this.mIsForeground = true;
        if (bundle == null) {
            Log.d("CdmaCLIRUtOptions", "start to init ");
            this.mCLIRPreference.init(this, false, this.mPhone);
        } else {
            Log.d("CdmaCLIRUtOptions", "restore stored states");
            this.mCLIRPreference.init(this, true, this.mPhone);
            int[] intArray = bundle.getIntArray(this.mCLIRPreference.getKey());
            if (intArray != null) {
                Log.d("CdmaCLIRUtOptions", "onCreate:  clirArray[0]=" + intArray[0] + ", clirArray[1]=" + intArray[1]);
                this.mCLIRPreference.handleGetCLIRResult(intArray);
            } else {
                this.mCLIRPreference.init(this, false, this.mPhone);
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
        if (this.mCLIRPreference != null && this.mCLIRPreference.clirArray != null) {
            bundle.putIntArray(this.mCLIRPreference.getKey(), this.mCLIRPreference.clirArray);
        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            CallFeaturesSetting.goUpToTopLevelSetting(this, this.mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
