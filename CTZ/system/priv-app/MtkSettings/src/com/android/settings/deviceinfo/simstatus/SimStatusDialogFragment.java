package com.android.settings.deviceinfo.simstatus;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.mediatek.settings.sim.SimHotSwapHandler;

/* loaded from: classes.dex */
public class SimStatusDialogFragment extends InstrumentedDialogFragment {
    private SimStatusDialogController mController;
    private View mRootView;
    private SimHotSwapHandler mSimHotSwapHandler = null;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1246;
    }

    public static void show(Fragment fragment, int i, String str) {
        FragmentManager childFragmentManager = fragment.getChildFragmentManager();
        if (childFragmentManager.findFragmentByTag("SimStatusDialog") == null) {
            Bundle bundle = new Bundle();
            bundle.putInt("arg_key_sim_slot", i);
            bundle.putString("arg_key_dialog_title", str);
            SimStatusDialogFragment simStatusDialogFragment = new SimStatusDialogFragment();
            simStatusDialogFragment.setArguments(bundle);
            simStatusDialogFragment.show(childFragmentManager, "SimStatusDialog");
        }
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) throws Resources.NotFoundException, PackageManager.NameNotFoundException {
        Bundle arguments = getArguments();
        int i = arguments.getInt("arg_key_sim_slot");
        String string = arguments.getString("arg_key_dialog_title");
        this.mController = new SimStatusDialogController(this, this.mLifecycle, i);
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(getActivity()).setTitle(string).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
        this.mRootView = LayoutInflater.from(positiveButton.getContext()).inflate(com.android.settings.R.layout.dialog_sim_status, (ViewGroup) null);
        this.mController.initialize();
        return positiveButton.setView(this.mRootView).create();
    }

    public void removeSettingFromScreen(int i) {
        View viewFindViewById = this.mRootView.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(8);
        }
    }

    public void setText(int i, CharSequence charSequence) {
        TextView textView = (TextView) this.mRootView.findViewById(i);
        if (TextUtils.isEmpty(charSequence)) {
            charSequence = getResources().getString(com.android.settings.R.string.device_info_default);
        }
        if (textView != null) {
            textView.setText(charSequence);
        }
    }

    public boolean isSettingOnScreen(int i) {
        View viewFindViewById = this.mRootView.findViewById(i);
        return viewFindViewById != null && viewFindViewById.getVisibility() == 0;
    }

    public void addSettingToScreen(int i) {
        View viewFindViewById = this.mRootView.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(0);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (this.mSimHotSwapHandler == null) {
            this.mSimHotSwapHandler = new SimHotSwapHandler(getContext());
        }
        if (this.mSimHotSwapHandler != null) {
            this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() { // from class: com.android.settings.deviceinfo.simstatus.SimStatusDialogFragment.1
                @Override // com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener
                public void onSimHotSwap() {
                    Log.d("SimStatusDialogFragment", "onSimHotSwap, dismiss dialog.");
                    SimStatusDialogFragment.this.dismissAllowingStateLoss();
                }
            });
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onDestroy() {
        if (this.mSimHotSwapHandler != null) {
            this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
        super.onDestroy();
    }
}
