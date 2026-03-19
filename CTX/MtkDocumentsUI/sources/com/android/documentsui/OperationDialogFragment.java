package com.android.documentsui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.ui.MessageBuilder;
import java.util.ArrayList;

public class OperationDialogFragment extends DialogFragment {
    public static void show(FragmentManager fragmentManager, int i, ArrayList<DocumentInfo> arrayList, ArrayList<Uri> arrayList2, DocumentStack documentStack, int i2) {
        Bundle bundle = new Bundle();
        bundle.putInt("com.android.documentsui.DIALOG_TYPE", i);
        bundle.putInt("com.android.documentsui.OPERATION_TYPE", i2);
        bundle.putParcelableArrayList("com.android.documentsui.FAILED_DOCS", arrayList);
        bundle.putParcelableArrayList("com.android.documentsui.FAILED_URIS", arrayList2);
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        OperationDialogFragment operationDialogFragment = new OperationDialogFragment();
        operationDialogFragment.setArguments(bundle);
        fragmentTransactionBeginTransaction.add(operationDialogFragment, "OperationDialogFragment");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        super.onCreate(bundle);
        int i = getArguments().getInt("com.android.documentsui.DIALOG_TYPE");
        int i2 = getArguments().getInt("com.android.documentsui.OPERATION_TYPE");
        ArrayList parcelableArrayList = getArguments().getParcelableArrayList("com.android.documentsui.FAILED_URIS");
        ArrayList parcelableArrayList2 = getArguments().getParcelableArrayList("com.android.documentsui.FAILED_DOCS");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(Html.fromHtml(new MessageBuilder(getContext()).generateListMessage(i, i2, parcelableArrayList2, parcelableArrayList)));
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                dialogInterface.dismiss();
            }
        });
        return builder.create();
    }
}
