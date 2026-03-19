package com.android.contacts.interactions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;

public class GroupDeletionDialogFragment extends DialogFragment {
    private int mSubId = SubInfoUtils.getInvalidSubId();

    public int getSubId() {
        return this.mSubId;
    }

    public static void show(FragmentManager fragmentManager, long j, String str, int i) {
        GroupDeletionDialogFragment groupDeletionDialogFragment = new GroupDeletionDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ContactSaveService.EXTRA_GROUP_ID, j);
        bundle.putString("label", str);
        bundle.putInt("subId", i);
        groupDeletionDialogFragment.setArguments(bundle);
        groupDeletionDialogFragment.show(fragmentManager, "deleteGroup");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        String string = getActivity().getString(R.string.delete_group_dialog_message, new Object[]{getArguments().getString("label")});
        this.mSubId = getArguments().getInt("subId");
        return new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(string).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                GroupDeletionDialogFragment.this.deleteGroup();
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    protected void deleteGroup() {
        long j = getArguments().getLong(ContactSaveService.EXTRA_GROUP_ID);
        PeopleActivity peopleActivity = (PeopleActivity) getActivity();
        int i = getArguments().getInt("subId");
        if (i > 0) {
            peopleActivity.startService(SimGroupUtils.createGroupDeletionIntentForIcc(getActivity(), j, i, getArguments().getString("label")));
        } else {
            peopleActivity.startService(ContactSaveService.createGroupDeletionIntent(getActivity(), j));
        }
    }
}
