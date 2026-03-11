package com.android.settings.bluetooth;

import android.content.Context;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.location.ScanningSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.FooterPreference;

public class BluetoothSwitchPreferenceController implements View.OnClickListener, SwitchWidgetController.OnSwitchChangeListener, LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    LocalBluetoothAdapter mBluetoothAdapter;
    private BluetoothEnabler mBluetoothEnabler;
    private LocalBluetoothManager mBluetoothManager;
    private Context mContext;
    private FooterPreference mFooterPreference;
    private RestrictionUtils mRestrictionUtils;
    private SwitchWidgetController mSwitch;

    public BluetoothSwitchPreferenceController(Context context, SwitchWidgetController switchWidgetController, FooterPreference footerPreference) {
        this(context, Utils.getLocalBtManager(context), new RestrictionUtils(), switchWidgetController, footerPreference);
    }

    @VisibleForTesting
    public BluetoothSwitchPreferenceController(Context context, LocalBluetoothManager localBluetoothManager, RestrictionUtils restrictionUtils, SwitchWidgetController switchWidgetController, FooterPreference footerPreference) {
        this.mBluetoothManager = localBluetoothManager;
        this.mRestrictionUtils = restrictionUtils;
        this.mSwitch = switchWidgetController;
        this.mContext = context;
        this.mFooterPreference = footerPreference;
        this.mSwitch.setupView();
        updateText(this.mSwitch.isChecked());
        if (this.mBluetoothManager != null) {
            this.mBluetoothAdapter = this.mBluetoothManager.getBluetoothAdapter();
        }
        this.mBluetoothEnabler = new BluetoothEnabler(context, switchWidgetController, FeatureFactory.getFactory(context).getMetricsFeatureProvider(), this.mBluetoothManager, 870, this.mRestrictionUtils);
        this.mBluetoothEnabler.setToggleCallback(this);
    }

    @Override
    public void onStart() {
        this.mBluetoothEnabler.resume(this.mContext);
        if (this.mSwitch != null) {
            updateText(this.mSwitch.isChecked());
        }
    }

    @Override
    public void onStop() {
        this.mBluetoothEnabler.pause();
    }

    @Override
    public boolean onSwitchToggled(boolean z) {
        updateText(z);
        return true;
    }

    @Override
    public void onClick(View view) {
        new SubSettingLauncher(this.mContext).setDestination(ScanningSettings.class.getName()).setSourceMetricsCategory(1390).launch();
    }

    @VisibleForTesting
    void updateText(boolean z) {
        if (!z && Utils.isBluetoothScanningEnabled(this.mContext)) {
            this.mFooterPreference.setTitle(AnnotationSpan.linkify(this.mContext.getText(R.string.bluetooth_scanning_on_info_message), new AnnotationSpan.LinkInfo("link", this)));
        } else {
            this.mFooterPreference.setTitle(R.string.bluetooth_empty_list_bluetooth_off);
        }
    }
}
