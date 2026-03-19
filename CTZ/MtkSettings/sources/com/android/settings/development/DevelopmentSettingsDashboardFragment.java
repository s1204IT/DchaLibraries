package com.android.settings.development;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.development.BluetoothA2dpHwOffloadRebootDialog;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.development.SystemPropPoker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DevelopmentSettingsDashboardFragment extends RestrictedDashboardFragment implements AdbClearKeysDialogHost, AdbDialogHost, BluetoothA2dpHwOffloadRebootDialog.OnA2dpHwDialogConfirmedListener, LogPersistDialogHost, OemUnlockDialogHost, SwitchBar.OnSwitchChangeListener {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        protected boolean isPageSearchEnabled(Context context) {
            return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.development_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return DevelopmentSettingsDashboardFragment.buildPreferenceControllers(context, null, null, null, null);
        }
    };
    private BluetoothA2dp mBluetoothA2dp;
    private final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private final BroadcastReceiver mBluetoothA2dpReceiver;
    private final BluetoothProfile.ServiceListener mBluetoothA2dpServiceListener;
    private AlertDialog mDialog;
    private EditText mEditText;
    private final BroadcastReceiver mEnableAdbReceiver;
    private FrameLayout mFrameLayout;
    private boolean mIsAvailable;
    private List<AbstractPreferenceController> mPreferenceControllers;
    private SwitchBar mSwitchBar;
    private DevelopmentSwitchBarController mSwitchBarController;
    private View mView;

    public DevelopmentSettingsDashboardFragment() {
        super("no_debugging_features");
        this.mBluetoothA2dpConfigStore = new BluetoothA2dpConfigStore();
        this.mIsAvailable = true;
        this.mPreferenceControllers = new ArrayList();
        this.mEnableAdbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (Object obj : DevelopmentSettingsDashboardFragment.this.mPreferenceControllers) {
                    if (obj instanceof AdbOnChangeListener) {
                        ((AdbOnChangeListener) obj).onAdbSettingChanged();
                    }
                }
            }
        };
        this.mBluetoothA2dpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("DevSettingsDashboard", "mBluetoothA2dpReceiver.onReceive intent=" + intent);
                if ("android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED".equals(intent.getAction())) {
                    Log.d("DevSettingsDashboard", "Received BluetoothCodecStatus=" + ((BluetoothCodecStatus) intent.getParcelableExtra("android.bluetooth.codec.extra.CODEC_STATUS")));
                    for (Object obj : DevelopmentSettingsDashboardFragment.this.mPreferenceControllers) {
                        if (obj instanceof BluetoothServiceConnectionListener) {
                            ((BluetoothServiceConnectionListener) obj).onBluetoothCodecUpdated();
                        }
                    }
                }
            }
        };
        this.mBluetoothA2dpServiceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                synchronized (DevelopmentSettingsDashboardFragment.this.mBluetoothA2dpConfigStore) {
                    DevelopmentSettingsDashboardFragment.this.mBluetoothA2dp = (BluetoothA2dp) bluetoothProfile;
                }
                for (Object obj : DevelopmentSettingsDashboardFragment.this.mPreferenceControllers) {
                    if (obj instanceof BluetoothServiceConnectionListener) {
                        ((BluetoothServiceConnectionListener) obj).onBluetoothServiceConnected(DevelopmentSettingsDashboardFragment.this.mBluetoothA2dp);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int i) {
                synchronized (DevelopmentSettingsDashboardFragment.this.mBluetoothA2dpConfigStore) {
                    DevelopmentSettingsDashboardFragment.this.mBluetoothA2dp = null;
                }
                for (Object obj : DevelopmentSettingsDashboardFragment.this.mPreferenceControllers) {
                    if (obj instanceof BluetoothServiceConnectionListener) {
                        ((BluetoothServiceConnectionListener) obj).onBluetoothServiceDisconnected();
                    }
                }
            }
        };
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Utils.isMonkeyRunning()) {
            getActivity().finish();
            return;
        }
        if (BenesseExtension.getDchaState() != 3 && BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists() && !BenesseExtension.IGNORE_DCHA_COMPLETED_FILE.exists()) {
            if (this.mDialog != null) {
                this.mDialog.setDismissMessage(null);
                this.mDialog = null;
            }
            if (this.mEditText != null) {
                this.mEditText = null;
            }
            this.mEditText = new EditText(getActivity());
            this.mEditText.setInputType(129);
            this.mDialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.unlock_set_unlock_password_title).setView(this.mEditText).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    DevelopmentSettingsDashboardFragment.lambda$onCreate$0(this.f$0, dialogInterface, i);
                }
            }).setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.getActivity().finish();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public final void onCancel(DialogInterface dialogInterface) {
                    this.f$0.getActivity().finish();
                }
            }).create();
            this.mDialog.show();
        }
    }

    public static void lambda$onCreate$0(DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment, DialogInterface dialogInterface, int i) {
        if (!BenesseExtension.checkPassword(developmentSettingsDashboardFragment.mEditText.getText().toString())) {
            developmentSettingsDashboardFragment.getActivity().finish();
        } else {
            developmentSettingsDashboardFragment.mView.setVisibility(8);
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted() || !Utils.isDeviceProvisioned(getActivity())) {
            this.mIsAvailable = false;
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.development_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBarController = new DevelopmentSwitchBarController(this, this.mSwitchBar, this.mIsAvailable, getLifecycle());
        this.mSwitchBar.show();
        if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            enableDeveloperOptions();
        } else {
            disableDeveloperOptions();
        }
        if (BenesseExtension.getDchaState() != 3 && BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists() && !BenesseExtension.IGNORE_DCHA_COMPLETED_FILE.exists()) {
            this.mFrameLayout.addView(this.mView, 0, new ViewGroup.LayoutParams(-1, -1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        registerReceivers();
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            defaultAdapter.getProfileProxy(getActivity(), this.mBluetoothA2dpServiceListener, 2);
        }
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        this.mFrameLayout = (FrameLayout) viewOnCreateView.findViewById(android.R.id.list_container);
        this.mView = new View(getActivity());
        this.mView.setBackgroundColor(-16777216);
        this.mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        return viewOnCreateView;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mView.bringToFront();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterReceivers();
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            defaultAdapter.closeProfileProxy(2, this.mBluetoothA2dp);
            this.mBluetoothA2dp = null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 39;
    }

    @Override
    public void onSwitchChanged(Switch r2, boolean z) {
        if (r2 == this.mSwitchBar.getSwitch() && z != DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            if (z) {
                EnableDevelopmentSettingWarningDialog.show(this);
            } else {
                disableDeveloperOptions();
            }
        }
    }

    @Override
    public void onOemUnlockDialogConfirmed() {
        ((OemUnlockPreferenceController) getDevelopmentOptionsController(OemUnlockPreferenceController.class)).onOemUnlockConfirmed();
    }

    @Override
    public void onOemUnlockDialogDismissed() {
        ((OemUnlockPreferenceController) getDevelopmentOptionsController(OemUnlockPreferenceController.class)).onOemUnlockDismissed();
    }

    @Override
    public void onEnableAdbDialogConfirmed() {
        ((AdbPreferenceController) getDevelopmentOptionsController(AdbPreferenceController.class)).onAdbDialogConfirmed();
    }

    @Override
    public void onEnableAdbDialogDismissed() {
        ((AdbPreferenceController) getDevelopmentOptionsController(AdbPreferenceController.class)).onAdbDialogDismissed();
    }

    @Override
    public void onAdbClearKeysDialogConfirmed() {
        ((ClearAdbKeysPreferenceController) getDevelopmentOptionsController(ClearAdbKeysPreferenceController.class)).onClearAdbKeysConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogConfirmed() {
        ((LogPersistPreferenceController) getDevelopmentOptionsController(LogPersistPreferenceController.class)).onDisableLogPersistDialogConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogRejected() {
        ((LogPersistPreferenceController) getDevelopmentOptionsController(LogPersistPreferenceController.class)).onDisableLogPersistDialogRejected();
    }

    @Override
    public void onA2dpHwDialogConfirmed() {
        ((BluetoothA2dpHwOffloadPreferenceController) getDevelopmentOptionsController(BluetoothA2dpHwOffloadPreferenceController.class)).onA2dpHwDialogConfirmed();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        boolean zOnActivityResult = false;
        for (Object obj : this.mPreferenceControllers) {
            if (obj instanceof OnActivityResultListener) {
                zOnActivityResult |= ((OnActivityResultListener) obj).onActivityResult(i, i2, intent);
            }
        }
        if (!zOnActivityResult) {
            super.onActivityResult(i, i2, intent);
        }
    }

    @Override
    protected String getLogTag() {
        return "DevSettingsDashboard";
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return Utils.isMonkeyRunning() ? R.xml.placeholder_prefs : R.xml.development_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (Utils.isMonkeyRunning()) {
            this.mPreferenceControllers = new ArrayList();
            return null;
        }
        this.mPreferenceControllers = buildPreferenceControllers(context, getActivity(), getLifecycle(), this, new BluetoothA2dpConfigStore());
        return this.mPreferenceControllers;
    }

    private void registerReceivers() {
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(this.mEnableAdbReceiver, new IntentFilter("com.android.settingslib.development.AbstractEnableAdbController.ENABLE_ADB_STATE_CHANGED"));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED");
        getActivity().registerReceiver(this.mBluetoothA2dpReceiver, intentFilter);
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(this.mEnableAdbReceiver);
        getActivity().unregisterReceiver(this.mBluetoothA2dpReceiver);
    }

    private void enableDeveloperOptions() {
        if (Utils.isMonkeyRunning()) {
            return;
        }
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), true);
        for (AbstractPreferenceController abstractPreferenceController : this.mPreferenceControllers) {
            if (abstractPreferenceController instanceof DeveloperOptionsPreferenceController) {
                ((DeveloperOptionsPreferenceController) abstractPreferenceController).onDeveloperOptionsEnabled();
            }
        }
    }

    private void disableDeveloperOptions() {
        if (Utils.isMonkeyRunning()) {
            return;
        }
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), false);
        SystemPropPoker systemPropPoker = SystemPropPoker.getInstance();
        systemPropPoker.blockPokes();
        for (AbstractPreferenceController abstractPreferenceController : this.mPreferenceControllers) {
            if (abstractPreferenceController instanceof DeveloperOptionsPreferenceController) {
                ((DeveloperOptionsPreferenceController) abstractPreferenceController).onDeveloperOptionsDisabled();
            }
        }
        systemPropPoker.unblockPokes();
        systemPropPoker.poke();
    }

    void onEnableDevelopmentOptionsConfirmed() {
        enableDeveloperOptions();
    }

    void onEnableDevelopmentOptionsRejected() {
        this.mSwitchBar.setChecked(false);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Activity activity, Lifecycle lifecycle, DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new TouchPanelVersionPreferenceController(context));
        if (Build.MODEL.equals("TAB-A05-BD")) {
            arrayList.add(new DigitizerVersionPreferenceController(context));
        }
        arrayList.add(new MemoryUsagePreferenceController(context));
        arrayList.add(new BugReportPreferenceController(context));
        arrayList.add(new LocalBackupPasswordPreferenceController(context));
        arrayList.add(new StayAwakePreferenceController(context, lifecycle));
        arrayList.add(new HdcpCheckingPreferenceController(context));
        arrayList.add(new DarkUIPreferenceController(context));
        arrayList.add(new BluetoothSnoopLogPreferenceController(context));
        arrayList.add(new OemUnlockPreferenceController(context, activity, developmentSettingsDashboardFragment));
        arrayList.add(new FileEncryptionPreferenceController(context));
        arrayList.add(new PictureColorModePreferenceController(context, lifecycle));
        arrayList.add(new WebViewAppPreferenceController(context));
        arrayList.add(new CoolColorTemperaturePreferenceController(context));
        arrayList.add(new DisableAutomaticUpdatesPreferenceController(context));
        arrayList.add(new AdbPreferenceController(context, developmentSettingsDashboardFragment));
        arrayList.add(new ClearAdbKeysPreferenceController(context, developmentSettingsDashboardFragment));
        arrayList.add(new LocalTerminalPreferenceController(context));
        arrayList.add(new BugReportInPowerPreferenceController(context));
        arrayList.add(new MockLocationAppPreferenceController(context, developmentSettingsDashboardFragment));
        arrayList.add(new DebugViewAttributesPreferenceController(context));
        arrayList.add(new SelectDebugAppPreferenceController(context, developmentSettingsDashboardFragment));
        arrayList.add(new WaitForDebuggerPreferenceController(context));
        arrayList.add(new EnableGpuDebugLayersPreferenceController(context));
        arrayList.add(new VerifyAppsOverUsbPreferenceController(context));
        arrayList.add(new AllowScreenShotPreferenceController(context));
        arrayList.add(new LogdSizePreferenceController(context));
        arrayList.add(new LogPersistPreferenceController(context, developmentSettingsDashboardFragment, lifecycle));
        arrayList.add(new CameraLaserSensorPreferenceController(context));
        arrayList.add(new WifiDisplayCertificationPreferenceController(context));
        arrayList.add(new WifiVerboseLoggingPreferenceController(context));
        arrayList.add(new WifiConnectedMacRandomizationPreferenceController(context));
        arrayList.add(new MobileDataAlwaysOnPreferenceController(context));
        arrayList.add(new TetheringHardwareAccelPreferenceController(context));
        arrayList.add(new BluetoothDeviceNoNamePreferenceController(context));
        arrayList.add(new BluetoothAbsoluteVolumePreferenceController(context));
        arrayList.add(new BluetoothAvrcpVersionPreferenceController(context));
        arrayList.add(new BluetoothA2dpHwOffloadPreferenceController(context, developmentSettingsDashboardFragment));
        arrayList.add(new BluetoothAudioCodecPreferenceController(context, lifecycle, bluetoothA2dpConfigStore));
        arrayList.add(new BluetoothAudioSampleRatePreferenceController(context, lifecycle, bluetoothA2dpConfigStore));
        arrayList.add(new BluetoothAudioBitsPerSamplePreferenceController(context, lifecycle, bluetoothA2dpConfigStore));
        arrayList.add(new BluetoothAudioChannelModePreferenceController(context, lifecycle, bluetoothA2dpConfigStore));
        arrayList.add(new BluetoothAudioQualityPreferenceController(context, lifecycle, bluetoothA2dpConfigStore));
        arrayList.add(new BluetoothMaxConnectedAudioDevicesPreferenceController(context));
        arrayList.add(new ShowTapsPreferenceController(context));
        arrayList.add(new PointerLocationPreferenceController(context));
        arrayList.add(new ShowSurfaceUpdatesPreferenceController(context));
        arrayList.add(new ShowLayoutBoundsPreferenceController(context));
        arrayList.add(new RtlLayoutPreferenceController(context));
        arrayList.add(new WindowAnimationScalePreferenceController(context));
        arrayList.add(new EmulateDisplayCutoutPreferenceController(context));
        arrayList.add(new TransitionAnimationScalePreferenceController(context));
        arrayList.add(new AnimatorDurationScalePreferenceController(context));
        arrayList.add(new SecondaryDisplayPreferenceController(context));
        arrayList.add(new ForceGpuRenderingPreferenceController(context));
        arrayList.add(new GpuViewUpdatesPreferenceController(context));
        arrayList.add(new HardwareLayersUpdatesPreferenceController(context));
        arrayList.add(new DebugGpuOverdrawPreferenceController(context));
        arrayList.add(new DebugNonRectClipOperationsPreferenceController(context));
        arrayList.add(new ForceMSAAPreferenceController(context));
        arrayList.add(new HardwareOverlaysPreferenceController(context));
        arrayList.add(new SimulateColorSpacePreferenceController(context));
        arrayList.add(new UsbAudioRoutingPreferenceController(context));
        arrayList.add(new StrictModePreferenceController(context));
        arrayList.add(new ProfileGpuRenderingPreferenceController(context));
        arrayList.add(new KeepActivitiesPreferenceController(context));
        arrayList.add(new BackgroundProcessLimitPreferenceController(context));
        arrayList.add(new ShowFirstCrashDialogPreferenceController(context));
        arrayList.add(new AppsNotRespondingPreferenceController(context));
        arrayList.add(new NotificationChannelWarningsPreferenceController(context));
        arrayList.add(new AllowAppsOnExternalPreferenceController(context));
        arrayList.add(new ResizableActivityPreferenceController(context));
        arrayList.add(new FreeformWindowsPreferenceController(context));
        arrayList.add(new ShortcutManagerThrottlingPreferenceController(context));
        arrayList.add(new EnableGnssRawMeasFullTrackingPreferenceController(context));
        arrayList.add(new DefaultLaunchPreferenceController(context, "running_apps"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "demo_mode"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "quick_settings_tiles"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "feature_flags_dashboard"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "default_usb_configuration"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "density"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "background_check"));
        arrayList.add(new DefaultLaunchPreferenceController(context, "inactive_apps"));
        return arrayList;
    }

    <T extends AbstractPreferenceController> T getDevelopmentOptionsController(Class<T> cls) {
        return (T) use(cls);
    }
}
