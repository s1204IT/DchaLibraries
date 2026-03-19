package com.android.managedprovisioning.common;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemProperties;
import com.android.managedprovisioning.R;
import com.android.setupwizardlib.GlifLayout;
import com.android.setupwizardlib.util.WizardManagerHelper;

public abstract class SetupGlifLayoutActivity extends SetupLayoutActivity {
    public SetupGlifLayoutActivity() {
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setDefaultTheme();
    }

    protected SetupGlifLayoutActivity(Utils utils) {
        super(utils);
    }

    protected void initializeLayoutParams(int i, Integer num, int i2, int i3) {
        setContentView(i);
        GlifLayout glifLayout = (GlifLayout) findViewById(R.id.setup_wizard_layout);
        setStatusBarColor(i3);
        glifLayout.setPrimaryColor(ColorStateList.valueOf(i2));
        if (num != null) {
            glifLayout.setHeaderText(num.intValue());
        }
        glifLayout.setIcon(LogoUtils.getOrganisationLogo(this, Integer.valueOf(i2)));
    }

    private void setDefaultTheme() {
        setTheme(WizardManagerHelper.getThemeRes(SystemProperties.get("setupwizard.theme"), R.style.SuwThemeGlif_Light));
    }
}
