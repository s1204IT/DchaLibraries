package com.android.settings.accounts;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.Tile;
import java.util.ArrayList;
import java.util.List;

public class AccountDetailDashboardFragment extends DashboardFragment {
    Account mAccount;
    private String mAccountLabel;
    private AccountSyncPreferenceController mAccountSynController;
    String mAccountType;
    private RemoveAccountPreferenceController mRemoveAccountController;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getPreferenceManager().setPreferenceComparisonCallback(null);
        Bundle arguments = getArguments();
        Activity activity = getActivity();
        UserHandle secureTargetUser = Utils.getSecureTargetUser(activity.getActivityToken(), (UserManager) getSystemService("user"), arguments, activity.getIntent().getExtras());
        if (arguments != null) {
            if (arguments.containsKey("account")) {
                this.mAccount = (Account) arguments.getParcelable("account");
            }
            if (arguments.containsKey("account_label")) {
                this.mAccountLabel = arguments.getString("account_label");
            }
            if (arguments.containsKey("account_type")) {
                this.mAccountType = arguments.getString("account_type");
            }
        }
        this.mAccountSynController.init(this.mAccount, secureTargetUser);
        this.mRemoveAccountController.init(this.mAccount, secureTargetUser);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (this.mAccountLabel != null) {
            getActivity().setTitle(this.mAccountLabel);
        }
        updateUi();
    }

    @Override
    public int getMetricsCategory() {
        return 8;
    }

    @Override
    protected String getLogTag() {
        return "AccountDetailDashboard";
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_account_detail;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.account_type_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mAccountSynController = new AccountSyncPreferenceController(context);
        arrayList.add(this.mAccountSynController);
        this.mRemoveAccountController = new RemoveAccountPreferenceController(context, this);
        arrayList.add(this.mRemoveAccountController);
        arrayList.add(new AccountHeaderPreferenceController(context, getLifecycle(), getActivity(), this, getArguments()));
        return arrayList;
    }

    @Override
    protected boolean displayTile(Tile tile) {
        Bundle bundle;
        if (this.mAccountType == null || (bundle = tile.metaData) == null) {
            return false;
        }
        boolean zEquals = this.mAccountType.equals(bundle.getString("com.android.settings.ia.account"));
        if (zEquals && tile.intent != null) {
            tile.intent.putExtra("extra.accountName", this.mAccount.name);
        }
        return zEquals;
    }

    void updateUi() {
        UserHandle userHandle;
        Context context = getContext();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("user_handle")) {
            userHandle = (UserHandle) arguments.getParcelable("user_handle");
        } else {
            userHandle = null;
        }
        AccountTypePreferenceLoader accountTypePreferenceLoader = new AccountTypePreferenceLoader(this, new AuthenticatorHelper(context, userHandle, null), userHandle);
        PreferenceScreen preferenceScreenAddPreferencesForType = accountTypePreferenceLoader.addPreferencesForType(this.mAccountType, getPreferenceScreen());
        if (preferenceScreenAddPreferencesForType != null) {
            accountTypePreferenceLoader.updatePreferenceIntents(preferenceScreenAddPreferencesForType, this.mAccountType, this.mAccount);
        }
    }
}
