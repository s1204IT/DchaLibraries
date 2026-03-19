package com.android.contacts.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentCallbacks2;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.util.AccountsListAdapter;
import com.google.common.base.Preconditions;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.util.AccountsListAdapterUtils;
import com.mediatek.contacts.util.Log;
import java.util.List;

public final class SelectAccountDialogFragment extends DialogFragment implements AccountsLoader.AccountsListener {
    private AccountsListAdapter mAccountsAdapter;
    private AccountTypeManager.AccountFilter mFilter;

    public interface Listener {
        void onAccountChosen(AccountWithDataSet accountWithDataSet, Bundle bundle);

        void onAccountSelectorCancelled();
    }

    public static void show(FragmentManager fragmentManager, int i, AccountTypeManager.AccountFilter accountFilter, Bundle bundle, String str) {
        Bundle bundle2 = new Bundle();
        bundle2.putInt("title_res_id", i);
        if (bundle == null) {
            bundle = Bundle.EMPTY;
        }
        bundle2.putBundle("extra_args", bundle);
        bundle2.putSerializable("list_filter", accountFilter);
        SelectAccountDialogFragment selectAccountDialogFragment = new SelectAccountDialogFragment();
        selectAccountDialogFragment.setArguments(bundle2);
        selectAccountDialogFragment.show(fragmentManager, str);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFilter = (AccountTypeManager.AccountFilter) getArguments().getSerializable("list_filter");
        if (this.mFilter == null) {
            this.mFilter = AccountTypeManager.AccountFilter.ALL;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        this.mAccountsAdapter = new AccountsListAdapter(builder.getContext());
        this.mAccountsAdapter.setCustomLayout(R.layout.account_selector_list_item_condensed);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                AccountWithDataSet item = SelectAccountDialogFragment.this.mAccountsAdapter.getItem(i);
                if (!(item instanceof AccountWithDataSetEx) || SimCardUtils.isPhoneBookReady(SelectAccountDialogFragment.this.getActivity(), ((AccountWithDataSetEx) item).getSubId())) {
                    SelectAccountDialogFragment.this.onAccountSelected(item);
                } else {
                    Log.w("SelectAccountDialog", "[onClick]PhoneBook is not ready for use");
                }
            }
        };
        TextView textView = (TextView) View.inflate(getActivity(), R.layout.dialog_title, null);
        textView.setText(arguments.getInt("title_res_id"));
        builder.setCustomTitle(textView);
        builder.setSingleChoiceItems(this.mAccountsAdapter, 0, onClickListener);
        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        AccountsLoader.loadAccounts(this, 0, this.mFilter);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        super.onCancel(dialogInterface);
        Listener listener = getListener();
        if (listener != null) {
            listener.onAccountSelectorCancelled();
        }
    }

    private void onAccountSelected(AccountWithDataSet accountWithDataSet) {
        Listener listener = getListener();
        if (listener != null) {
            listener.onAccountChosen(accountWithDataSet, getArguments().getBundle("extra_args"));
        }
    }

    private Listener getListener() {
        ComponentCallbacks2 activity = getActivity();
        if (activity != null && (activity instanceof Listener)) {
            return (Listener) activity;
        }
        return null;
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> list) {
        Preconditions.checkNotNull(this.mAccountsAdapter, "Accounts adapter should have been initialized");
        if (this.mFilter == AccountTypeManager.AccountFilter.GROUPS_WRITABLE) {
            list = AccountsListAdapterUtils.getGroupAccount(list);
        }
        this.mAccountsAdapter.setAccounts(list, null);
    }
}
