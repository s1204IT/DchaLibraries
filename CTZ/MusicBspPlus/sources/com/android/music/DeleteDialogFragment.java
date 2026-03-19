package com.android.music;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DeleteDialogFragment extends DialogFragment {
    private long[] mItemList = {-1};

    public static DeleteDialogFragment newInstance(Boolean bool, long j, int i, String str) {
        DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("single", bool.booleanValue());
        bundle.putLong("track", j);
        bundle.putInt("delete_desc_string_id", i);
        bundle.putString("delete_desc_track_info", str);
        deleteDialogFragment.setArguments(bundle);
        return deleteDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        MusicLogUtils.d("DeleteItems", "<onDELTEDialog>");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        this.mItemList[0] = (int) getArguments().getLong("track");
        MusicLogUtils.d("DeleteItems", "Delete mList item id" + this.mItemList[0] + ", Track id = " + getArguments().getLong("track"));
        builder.setMessage(String.format(getString(getArguments().getInt("delete_desc_string_id")), getArguments().getString("delete_desc_track_info"))).setPositiveButton(getString(R.string.delete_confirm_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (MusicUtils.deleteTracks(DeleteDialogFragment.this.getActivity().getApplicationContext(), DeleteDialogFragment.this.mItemList) != -1) {
                    MusicUtils.showDeleteToast(1, DeleteDialogFragment.this.getActivity().getApplicationContext());
                }
            }
        }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.setTitle(R.string.delete_item);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.setCanceledOnTouchOutside(false);
        return alertDialogCreate;
    }
}
