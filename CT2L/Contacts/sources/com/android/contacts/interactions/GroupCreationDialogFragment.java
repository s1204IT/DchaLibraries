package com.android.contacts.interactions;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.widget.EditText;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.common.model.account.AccountWithDataSet;

public class GroupCreationDialogFragment extends GroupNameDialogFragment {
    private final OnGroupCreatedListener mListener;

    public interface OnGroupCreatedListener {
        void onGroupCreated();
    }

    public static void show(FragmentManager fragmentManager, String accountType, String accountName, String dataSet, OnGroupCreatedListener listener) {
        GroupCreationDialogFragment dialog = new GroupCreationDialogFragment(listener);
        Bundle args = new Bundle();
        args.putString("accountType", accountType);
        args.putString("accountName", accountName);
        args.putString("dataSet", dataSet);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "createGroupDialog");
    }

    public GroupCreationDialogFragment() {
        this.mListener = null;
    }

    private GroupCreationDialogFragment(OnGroupCreatedListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void initializeGroupLabelEditText(EditText editText) {
    }

    @Override
    protected int getTitleResourceId() {
        return R.string.create_group_dialog_title;
    }

    @Override
    protected void onCompleted(String groupLabel) {
        Bundle arguments = getArguments();
        String accountType = arguments.getString("accountType");
        String accountName = arguments.getString("accountName");
        String dataSet = arguments.getString("dataSet");
        if (this.mListener != null) {
            this.mListener.onGroupCreated();
        }
        Activity activity = getActivity();
        activity.startService(ContactSaveService.createNewGroupIntent(activity, new AccountWithDataSet(accountName, accountType, dataSet), groupLabel, null, activity.getClass(), "android.intent.action.EDIT"));
    }
}
