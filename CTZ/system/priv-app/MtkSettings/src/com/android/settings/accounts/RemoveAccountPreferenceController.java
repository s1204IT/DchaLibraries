package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import java.io.IOException;

/* loaded from: classes.dex */
public class RemoveAccountPreferenceController extends AbstractPreferenceController implements View.OnClickListener, PreferenceControllerMixin {
    private Account mAccount;
    private Fragment mParentFragment;
    private UserHandle mUserHandle;

    public RemoveAccountPreferenceController(Context context, Fragment fragment) {
        super(context);
        this.mParentFragment = fragment;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        ((Button) ((LayoutPreference) preferenceScreen.findPreference("remove_account")).findViewById(R.id.button)).setOnClickListener(this);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "remove_account";
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced;
        if (this.mUserHandle != null && (enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_modify_accounts", this.mUserHandle.getIdentifier())) != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, enforcedAdminCheckIfRestrictionEnforced);
        } else {
            ConfirmRemoveAccountDialog.show(this.mParentFragment, this.mAccount, this.mUserHandle);
        }
    }

    public void init(Account account, UserHandle userHandle) {
        this.mAccount = account;
        this.mUserHandle = userHandle;
    }

    public static class ConfirmRemoveAccountDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        private Account mAccount;
        private UserHandle mUserHandle;

        public static ConfirmRemoveAccountDialog show(Fragment fragment, Account account, UserHandle userHandle) {
            if (!fragment.isAdded()) {
                return null;
            }
            ConfirmRemoveAccountDialog confirmRemoveAccountDialog = new ConfirmRemoveAccountDialog();
            Bundle bundle = new Bundle();
            bundle.putParcelable("account", account);
            bundle.putParcelable("android.intent.extra.USER", userHandle);
            confirmRemoveAccountDialog.setArguments(bundle);
            confirmRemoveAccountDialog.setTargetFragment(fragment, 0);
            confirmRemoveAccountDialog.show(fragment.getFragmentManager(), "confirmRemoveAccount");
            return confirmRemoveAccountDialog;
        }

        @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.DialogFragment, android.app.Fragment
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            Bundle arguments = getArguments();
            this.mAccount = (Account) arguments.getParcelable("account");
            this.mUserHandle = (UserHandle) arguments.getParcelable("android.intent.extra.USER");
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.really_remove_account_title).setMessage(R.string.really_remove_account_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.remove_account_label, this).create();
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 585;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            Activity activity = getTargetFragment().getActivity();
            AccountManager.get(activity).removeAccountAsUser(this.mAccount, activity, new AccountManagerCallback<Bundle>() { // from class: com.android.settings.accounts.RemoveAccountPreferenceController.ConfirmRemoveAccountDialog.1
                @Override // android.accounts.AccountManagerCallback
                public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                    if (!ConfirmRemoveAccountDialog.this.getTargetFragment().isResumed()) {
                        return;
                    }
                    boolean z = true;
                    try {
                        if (accountManagerFuture.getResult().getBoolean("booleanResult")) {
                            z = false;
                        }
                    } catch (AuthenticatorException e) {
                    } catch (OperationCanceledException e2) {
                    } catch (IOException e3) {
                    }
                    Activity activity2 = ConfirmRemoveAccountDialog.this.getTargetFragment().getActivity();
                    if (z && activity2 != null && !activity2.isFinishing()) {
                        RemoveAccountFailureDialog.show(ConfirmRemoveAccountDialog.this.getTargetFragment());
                    } else {
                        activity2.finish();
                    }
                }
            }, null, this.mUserHandle);
        }
    }

    public static class RemoveAccountFailureDialog extends InstrumentedDialogFragment {
        public static void show(Fragment fragment) {
            if (!fragment.isAdded()) {
                return;
            }
            RemoveAccountFailureDialog removeAccountFailureDialog = new RemoveAccountFailureDialog();
            removeAccountFailureDialog.setTargetFragment(fragment, 0);
            removeAccountFailureDialog.show(fragment.getFragmentManager(), "removeAccountFailed");
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.really_remove_account_title).setMessage(R.string.remove_account_failed).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 586;
        }
    }
}
