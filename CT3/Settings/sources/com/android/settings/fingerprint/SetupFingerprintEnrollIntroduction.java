package com.android.settings.fingerprint;

import android.content.Intent;
import android.content.res.Resources;
import android.os.UserHandle;
import android.widget.Button;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupChooseLockGeneric;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.SetupWizardRecyclerLayout;
import com.android.setupwizardlib.items.Item;
import com.android.setupwizardlib.items.RecyclerItemAdapter;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupFingerprintEnrollIntroduction extends FingerprintEnrollIntroduction implements NavigationBar.NavigationBarListener {
    @Override
    protected Intent getChooseLockIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupChooseLockGeneric.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected Intent getFindSensorIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupFingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        int resid2 = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid2, first);
    }

    @Override
    protected void initViews() {
        SetupWizardRecyclerLayout layout = (SetupWizardRecyclerLayout) findViewById(R.id.setup_wizard_layout);
        RecyclerItemAdapter adapter = (RecyclerItemAdapter) layout.getAdapter();
        Item nextItem = (Item) adapter.findItemById(R.id.next_button);
        nextItem.setTitle(getText(R.string.security_settings_fingerprint_enroll_introduction_continue_setup));
        Item cancelItem = (Item) adapter.findItemById(R.id.cancel_button);
        cancelItem.setTitle(getText(R.string.security_settings_fingerprint_enroll_introduction_cancel_setup));
        SetupWizardUtils.setImmersiveMode(this);
        getNavigationBar().setNavigationBarListener(this);
        Button nextButton = getNavigationBar().getNextButton();
        nextButton.setText((CharSequence) null);
        nextButton.setEnabled(false);
        layout.setDividerInset(getResources().getDimensionPixelSize(R.dimen.suw_items_icon_divider_inset));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if (data == null) {
                data = new Intent();
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
            data.putExtra(":settings:password_quality", lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCancelButtonClick() {
        SetupSkipDialog dialog = SetupSkipDialog.newInstance(getIntent().getBooleanExtra(":settings:frp_supported", false));
        dialog.show(getFragmentManager());
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
    }

    @Override
    protected int getMetricsCategory() {
        return 249;
    }
}
