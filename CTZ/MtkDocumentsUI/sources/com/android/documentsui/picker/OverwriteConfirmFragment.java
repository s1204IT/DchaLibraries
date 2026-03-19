package com.android.documentsui.picker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;

public class OverwriteConfirmFragment extends DialogFragment {
    private ActionHandler<PickActivity> mActions;
    private DocumentInfo mOverwriteTarget;

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mActions = (ActionHandler) ((PickActivity) getActivity()).getInjector().actions;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        if (getArguments() != null) {
            bundle = getArguments();
        }
        this.mOverwriteTarget = (DocumentInfo) bundle.getParcelable("document");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.overwrite_file_confirmation_message), this.mOverwriteTarget.displayName));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                OverwriteConfirmFragment overwriteConfirmFragment = this.f$0;
                overwriteConfirmFragment.mActions.finishPicking(overwriteConfirmFragment.mOverwriteTarget.derivedUri);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("document", this.mOverwriteTarget);
    }

    public static void show(FragmentManager fragmentManager, DocumentInfo documentInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("document", documentInfo);
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        OverwriteConfirmFragment overwriteConfirmFragment = new OverwriteConfirmFragment();
        overwriteConfirmFragment.setArguments(bundle);
        fragmentTransactionBeginTransaction.add(overwriteConfirmFragment, "OverwriteConfirmFragment");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }
}
