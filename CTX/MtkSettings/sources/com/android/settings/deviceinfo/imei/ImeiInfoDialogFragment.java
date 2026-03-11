package com.android.settings.deviceinfo.imei;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ImeiInfoDialogFragment extends InstrumentedDialogFragment {
    static final String TAG = "ImeiInfoDialog";
    private View mRootView;

    public static void show(Fragment fragment, int i, String str) {
        FragmentManager childFragmentManager = fragment.getChildFragmentManager();
        if (childFragmentManager.findFragmentByTag(TAG) == null) {
            Bundle bundle = new Bundle();
            bundle.putInt("arg_key_slot_id", i);
            bundle.putString("arg_key_dialog_title", str);
            ImeiInfoDialogFragment imeiInfoDialogFragment = new ImeiInfoDialogFragment();
            imeiInfoDialogFragment.setArguments(bundle);
            imeiInfoDialogFragment.show(childFragmentManager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1240;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        int i = arguments.getInt("arg_key_slot_id");
        String string = arguments.getString("arg_key_dialog_title");
        ImeiInfoDialogController imeiInfoDialogController = new ImeiInfoDialogController(this, i);
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(getActivity()).setTitle(string).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
        this.mRootView = LayoutInflater.from(positiveButton.getContext()).inflate(com.android.settings.R.layout.dialog_imei_info, (ViewGroup) null);
        imeiInfoDialogController.populateImeiInfo();
        return positiveButton.setView(this.mRootView).create();
    }

    public void removeViewFromScreen(int i) {
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
}
