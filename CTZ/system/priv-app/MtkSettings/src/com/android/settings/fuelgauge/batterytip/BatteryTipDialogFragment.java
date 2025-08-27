package com.android.settings.fuelgauge.batterytip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.actions.BatteryTipAction;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import java.util.List;

/* loaded from: classes.dex */
public class BatteryTipDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    BatteryTip mBatteryTip;
    int mMetricsKey;

    public static BatteryTipDialogFragment newInstance(BatteryTip batteryTip, int i) {
        BatteryTipDialogFragment batteryTipDialogFragment = new BatteryTipDialogFragment();
        Bundle bundle = new Bundle(1);
        bundle.putParcelable("battery_tip", batteryTip);
        bundle.putInt("metrics_key", i);
        batteryTipDialogFragment.setArguments(bundle);
        return batteryTipDialogFragment;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        Context context = getContext();
        this.mBatteryTip = (BatteryTip) arguments.getParcelable("battery_tip");
        this.mMetricsKey = arguments.getInt("metrics_key");
        switch (this.mBatteryTip.getType()) {
            case 1:
                RestrictAppTip restrictAppTip = (RestrictAppTip) this.mBatteryTip;
                List<AppInfo> restrictAppList = restrictAppTip.getRestrictAppList();
                int size = restrictAppList.size();
                CharSequence applicationLabel = Utils.getApplicationLabel(context, restrictAppList.get(0).packageName);
                AlertDialog.Builder negativeButton = new AlertDialog.Builder(context).setTitle(context.getResources().getQuantityString(R.plurals.battery_tip_restrict_app_dialog_title, size, Integer.valueOf(size))).setPositiveButton(R.string.battery_tip_restrict_app_dialog_ok, this).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
                if (size == 1) {
                    negativeButton.setMessage(getString(R.string.battery_tip_restrict_app_dialog_message, new Object[]{applicationLabel}));
                } else if (size <= 5) {
                    negativeButton.setMessage(getString(R.string.battery_tip_restrict_apps_less_than_5_dialog_message));
                    RecyclerView recyclerView = (RecyclerView) LayoutInflater.from(context).inflate(R.layout.recycler_view, (ViewGroup) null);
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.setAdapter(new HighUsageAdapter(context, restrictAppList));
                    negativeButton.setView(recyclerView);
                } else {
                    negativeButton.setMessage(context.getString(R.string.battery_tip_restrict_apps_more_than_5_dialog_message, restrictAppTip.getRestrictAppsString(context)));
                }
                return negativeButton.create();
            case 2:
                HighUsageTip highUsageTip = (HighUsageTip) this.mBatteryTip;
                RecyclerView recyclerView2 = (RecyclerView) LayoutInflater.from(context).inflate(R.layout.recycler_view, (ViewGroup) null);
                recyclerView2.setLayoutManager(new LinearLayoutManager(context));
                recyclerView2.setAdapter(new HighUsageAdapter(context, highUsageTip.getHighUsageAppList()));
                return new AlertDialog.Builder(context).setMessage(getString(R.string.battery_tip_dialog_message, new Object[]{Integer.valueOf(highUsageTip.getHighUsageAppList().size())})).setView(recyclerView2).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
            case 3:
            case 4:
            case 5:
            default:
                throw new IllegalArgumentException("unknown type " + this.mBatteryTip.getType());
            case 6:
                return new AlertDialog.Builder(context).setMessage(R.string.battery_tip_dialog_summary_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
            case 7:
                Utils.getApplicationLabel(context, ((UnrestrictAppTip) this.mBatteryTip).getPackageName());
                return new AlertDialog.Builder(context).setTitle(getString(R.string.battery_tip_unrestrict_app_dialog_title)).setMessage(R.string.battery_tip_unrestrict_app_dialog_message).setPositiveButton(R.string.battery_tip_unrestrict_app_dialog_ok, this).setNegativeButton(R.string.battery_tip_unrestrict_app_dialog_cancel, (DialogInterface.OnClickListener) null).create();
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1323;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        BatteryTipPreferenceController.BatteryTipListener batteryTipListener = (BatteryTipPreferenceController.BatteryTipListener) getTargetFragment();
        if (batteryTipListener == null) {
            return;
        }
        BatteryTipAction actionForBatteryTip = BatteryTipUtils.getActionForBatteryTip(this.mBatteryTip, (SettingsActivity) getActivity(), (InstrumentedPreferenceFragment) getTargetFragment());
        if (actionForBatteryTip != null) {
            actionForBatteryTip.handlePositiveAction(this.mMetricsKey);
        }
        batteryTipListener.onBatteryTipHandled(this.mBatteryTip);
    }
}
