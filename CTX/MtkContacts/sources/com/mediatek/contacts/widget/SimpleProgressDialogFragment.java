package com.mediatek.contacts.widget;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;

public class SimpleProgressDialogFragment extends DialogFragment {
    private static String TAG = "SimpleProgressDialogFragment";

    private static SimpleProgressDialogFragment getInstance(FragmentManager fragmentManager) {
        SimpleProgressDialogFragment existDialogFragment = getExistDialogFragment(fragmentManager);
        Log.i(TAG, "[getInstance]dialog:" + existDialogFragment);
        if (existDialogFragment == null) {
            SimpleProgressDialogFragment simpleProgressDialogFragment = new SimpleProgressDialogFragment();
            Log.i(TAG, "[getInstance]create new dialog " + simpleProgressDialogFragment + " in " + fragmentManager);
            return simpleProgressDialogFragment;
        }
        return existDialogFragment;
    }

    private static SimpleProgressDialogFragment getExistDialogFragment(FragmentManager fragmentManager) {
        return (SimpleProgressDialogFragment) fragmentManager.findFragmentByTag("progress_dialog");
    }

    public static void show(FragmentManager fragmentManager) {
        Log.d(TAG, "[show]show dialog for " + fragmentManager);
        SimpleProgressDialogFragment simpleProgressDialogFragment = getInstance(fragmentManager);
        if (simpleProgressDialogFragment.isAdded()) {
            Log.d(TAG, "[show]dialog is already shown: " + simpleProgressDialogFragment);
            return;
        }
        Log.d(TAG, "[show]dialog created and shown: " + simpleProgressDialogFragment);
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        fragmentTransactionBeginTransaction.add(simpleProgressDialogFragment, "progress_dialog");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        simpleProgressDialogFragment.setCancelable(false);
    }

    public static void dismiss(FragmentManager fragmentManager) {
        Log.d(TAG, "[dismiss]dismiss dialog for " + fragmentManager);
        if (fragmentManager == null) {
            return;
        }
        SimpleProgressDialogFragment existDialogFragment = getExistDialogFragment(fragmentManager);
        if (existDialogFragment == null) {
            Log.w(TAG, "[dismiss]dialog never shown before, no need dismiss");
            return;
        }
        if (existDialogFragment.isAdded()) {
            Log.d(TAG, "[dismiss]force dismiss dialog: " + existDialogFragment);
            existDialogFragment.dismissAllowingStateLoss();
            return;
        }
        Log.d(TAG, "[dismiss]dialog not added, dismiss failed: " + existDialogFragment);
    }

    @Override
    public String toString() {
        return String.valueOf(hashCode());
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Log.d(TAG, "[onCreateDialog]");
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.please_wait));
        return progressDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "[onResume]");
        getDialog().getWindow().clearFlags(131072);
    }
}
