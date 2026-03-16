package com.android.contacts.common.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentCallbacks2;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountsListAdapter;

public final class SelectAccountDialogFragment extends DialogFragment {

    public interface Listener {
        void onAccountChosen(AccountWithDataSet accountWithDataSet, Bundle bundle);

        void onAccountSelectorCancelled();
    }

    public static <F extends Fragment & Listener> DialogFragment show(FragmentManager fragmentManager, F targetFragment, int titleResourceId, AccountsListAdapter.AccountListFilter accountListFilter, Bundle extraArgs) {
        Bundle args = new Bundle();
        args.putInt("title_res_id", titleResourceId);
        args.putSerializable("list_filter", accountListFilter);
        if (extraArgs == null) {
            extraArgs = Bundle.EMPTY;
        }
        args.putBundle("extra_args", extraArgs);
        SelectAccountDialogFragment instance = new SelectAccountDialogFragment();
        instance.setArguments(args);
        instance.setTargetFragment(targetFragment, 0);
        instance.show(fragmentManager, (String) null);
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle args = getArguments();
        AccountsListAdapter.AccountListFilter filter = (AccountsListAdapter.AccountListFilter) args.getSerializable("list_filter");
        final AccountsListAdapter accountAdapter = new AccountsListAdapter(builder.getContext(), filter);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SelectAccountDialogFragment.this.onAccountSelected(accountAdapter.getItem(which));
            }
        };
        builder.setTitle(args.getInt("title_res_id"));
        builder.setSingleChoiceItems(accountAdapter, 0, clickListener);
        AlertDialog result = builder.create();
        return result;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        ComponentCallbacks2 targetFragment = getTargetFragment();
        if (targetFragment != null && (targetFragment instanceof Listener)) {
            Listener target = (Listener) targetFragment;
            target.onAccountSelectorCancelled();
        }
    }

    private void onAccountSelected(AccountWithDataSet account) {
        ComponentCallbacks2 targetFragment = getTargetFragment();
        if (targetFragment != null && (targetFragment instanceof Listener)) {
            Listener target = (Listener) targetFragment;
            target.onAccountChosen(account, getArguments().getBundle("extra_args"));
        }
    }
}
