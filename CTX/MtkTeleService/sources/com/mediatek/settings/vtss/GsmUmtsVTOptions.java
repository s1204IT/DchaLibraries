package com.mediatek.settings.vtss;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.android.phone.PhoneGlobals;
import com.android.phone.SubscriptionInfoHelper;

public abstract class GsmUmtsVTOptions extends PreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    protected abstract void init(SubscriptionInfoHelper subscriptionInfoHelper);

    protected abstract void setActionBar(SubscriptionInfoHelper subscriptionInfoHelper);

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SubscriptionInfoHelper subscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        setActionBar(subscriptionInfoHelper);
        init(subscriptionInfoHelper);
        if (subscriptionInfoHelper.getPhone().getPhoneType() != 1) {
            getPreferenceScreen().setEnabled(false);
        }
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
