package com.android.contacts.interactions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;

public class GroupDeletionDialogFragment extends DialogFragment {
    public static void show(FragmentManager fragmentManager, long groupId, String label, boolean endActivity) {
        GroupDeletionDialogFragment dialog = new GroupDeletionDialogFragment();
        Bundle args = new Bundle();
        args.putLong("groupId", groupId);
        args.putString("label", label);
        args.putBoolean("endActivity", endActivity);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "deleteGroup");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String label = getArguments().getString("label");
        String message = getActivity().getString(R.string.delete_group_dialog_message, new Object[]{label});
        return new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                GroupDeletionDialogFragment.this.deleteGroup();
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    protected void deleteGroup() {
        Bundle arguments = getArguments();
        long groupId = arguments.getLong("groupId");
        getActivity().startService(ContactSaveService.createGroupDeletionIntent(getActivity(), groupId));
        if (shouldEndActivity()) {
            getActivity().finish();
        }
    }

    private boolean shouldEndActivity() {
        return getArguments().getBoolean("endActivity");
    }
}
