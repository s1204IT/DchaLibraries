package com.android.settings.accounts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserManager;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.users.UserDialogs;

public class RemoveUserFragment extends InstrumentedDialogFragment {
    static RemoveUserFragment newInstance(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt("userId", i);
        RemoveUserFragment removeUserFragment = new RemoveUserFragment();
        removeUserFragment.setArguments(bundle);
        return removeUserFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final int i = getArguments().getInt("userId");
        return UserDialogs.createRemoveDialog(getActivity(), i, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                ((UserManager) RemoveUserFragment.this.getActivity().getSystemService("user")).removeUser(i);
            }
        });
    }

    @Override
    public int getMetricsCategory() {
        return 534;
    }
}
