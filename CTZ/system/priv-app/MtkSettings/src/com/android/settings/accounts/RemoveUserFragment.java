package com.android.settings.accounts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserManager;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.users.UserDialogs;

/* loaded from: classes.dex */
public class RemoveUserFragment extends InstrumentedDialogFragment {
    static RemoveUserFragment newInstance(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt("userId", i);
        RemoveUserFragment removeUserFragment = new RemoveUserFragment();
        removeUserFragment.setArguments(bundle);
        return removeUserFragment;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        final int i = getArguments().getInt("userId");
        return UserDialogs.createRemoveDialog(getActivity(), i, new DialogInterface.OnClickListener() { // from class: com.android.settings.accounts.RemoveUserFragment.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                ((UserManager) RemoveUserFragment.this.getActivity().getSystemService("user")).removeUser(i);
            }
        });
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 534;
    }
}
