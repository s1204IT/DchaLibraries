package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;
import java.util.List;

public class MasterClear extends OptionsMenuFragment {
    private View mContentView;
    private CheckBox mExternalStorage;
    private View mExternalStorageContainer;
    private Button mInitiateButton;
    private final View.OnClickListener mInitiateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (MasterClear.this.runKeyguardConfirmation(55)) {
                return;
            }
            MasterClear.this.showFinalConfirmation();
        }
    };

    public boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(request, res.getText(R.string.master_clear_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 55) {
            return;
        }
        if (resultCode == -1) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    public void showFinalConfirmation() {
        Bundle args = new Bundle();
        args.putBoolean("erase_sd", this.mExternalStorage.isChecked());
        ((SettingsActivity) getActivity()).startPreferencePanel(MasterClearConfirm.class.getName(), args, R.string.master_clear_confirm_title, null, null, 0);
    }

    private void establishInitialState() {
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_master_clear);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
        this.mExternalStorageContainer = this.mContentView.findViewById(R.id.erase_external_container);
        this.mExternalStorage = (CheckBox) this.mContentView.findViewById(R.id.erase_external);
        boolean isExtStorageEmulated = Environment.isExternalStorageEmulated();
        if (isExtStorageEmulated || (!Environment.isExternalStorageRemovable() && isExtStorageEncrypted())) {
            this.mExternalStorageContainer.setVisibility(8);
            View externalOption = this.mContentView.findViewById(R.id.erase_external_option_text);
            externalOption.setVisibility(8);
            View externalAlsoErased = this.mContentView.findViewById(R.id.also_erases_external);
            externalAlsoErased.setVisibility(0);
            this.mExternalStorage.setChecked(!isExtStorageEmulated);
        } else {
            this.mExternalStorageContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MasterClear.this.mExternalStorage.toggle();
                }
            });
        }
        UserManager um = (UserManager) getActivity().getSystemService("user");
        loadAccountList(um);
        StringBuffer contentDescription = new StringBuffer();
        View masterClearContainer = this.mContentView.findViewById(R.id.master_clear_container);
        getContentDescription(masterClearContainer, contentDescription);
        masterClearContainer.setContentDescription(contentDescription);
    }

    private void getContentDescription(View v, StringBuffer description) {
        if (v.getVisibility() != 0) {
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vGroup = (ViewGroup) v;
            for (int i = 0; i < vGroup.getChildCount(); i++) {
                View nextChild = vGroup.getChildAt(i);
                getContentDescription(nextChild, description);
            }
            return;
        }
        if (!(v instanceof TextView)) {
            return;
        }
        TextView vText = (TextView) v;
        description.append(vText.getText());
        description.append(",");
    }

    private boolean isExtStorageEncrypted() {
        String state = SystemProperties.get("vold.decrypt");
        return !"".equals(state);
    }

    private void loadAccountList(UserManager um) {
        View accountsLabel = this.mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout) this.mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();
        Context context = getActivity();
        List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
        int profilesSize = profiles.size();
        AccountManager mgr = AccountManager.get(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        int accountsCount = 0;
        for (int profileIndex = 0; profileIndex < profilesSize; profileIndex++) {
            UserInfo userInfo = profiles.get(profileIndex);
            int profileId = userInfo.id;
            UserHandle userHandle = new UserHandle(profileId);
            Account[] accounts = mgr.getAccountsAsUser(profileId);
            int N = accounts.length;
            if (N != 0) {
                accountsCount += N;
                AuthenticatorDescription[] descs = AccountManager.get(context).getAuthenticatorTypesAsUser(profileId);
                int M = descs.length;
                View titleView = Utils.inflateCategoryHeader(inflater, contents);
                TextView titleText = (TextView) titleView.findViewById(android.R.id.title);
                titleText.setText(userInfo.isManagedProfile() ? R.string.category_work : R.string.category_personal);
                contents.addView(titleView);
                for (Account account : accounts) {
                    AuthenticatorDescription desc = null;
                    int j = 0;
                    while (true) {
                        if (j >= M) {
                            break;
                        }
                        if (!account.type.equals(descs[j].type)) {
                            j++;
                        } else {
                            desc = descs[j];
                            break;
                        }
                    }
                    if (desc == null) {
                        Log.w("MasterClear", "No descriptor for account name=" + account.name + " type=" + account.type);
                    } else {
                        Drawable icon = null;
                        try {
                            if (desc.iconId != 0) {
                                Context authContext = context.createPackageContextAsUser(desc.packageName, 0, userHandle);
                                icon = context.getPackageManager().getUserBadgedIcon(authContext.getDrawable(desc.iconId), userHandle);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.w("MasterClear", "Bad package name for account type " + desc.type);
                        } catch (Resources.NotFoundException e2) {
                            Log.w("MasterClear", "Invalid icon id for account type " + desc.type, e2);
                        }
                        if (icon == null) {
                            icon = context.getPackageManager().getDefaultActivityIcon();
                        }
                        TextView child = (TextView) inflater.inflate(R.layout.master_clear_account, (ViewGroup) contents, false);
                        child.setText(account.name);
                        child.setCompoundDrawablesWithIntrinsicBounds(icon, (Drawable) null, (Drawable) null, (Drawable) null);
                        contents.addView(child);
                    }
                }
            }
        }
        if (accountsCount > 0) {
            accountsLabel.setVisibility(0);
            contents.setVisibility(0);
        }
        View otherUsers = this.mContentView.findViewById(R.id.other_users_present);
        boolean hasOtherUsers = um.getUserCount() - profilesSize > 0;
        otherUsers.setVisibility(hasOtherUsers ? 0 : 8);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_factory_reset", UserHandle.myUserId());
        UserManager um = UserManager.get(getActivity());
        if (!um.isAdminUser() || RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_factory_reset", UserHandle.myUserId())) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, (ViewGroup) null);
        }
        if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, (ViewGroup) null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(0);
            return view;
        }
        this.mContentView = inflater.inflate(R.layout.master_clear, (ViewGroup) null);
        establishInitialState();
        return this.mContentView;
    }

    @Override
    protected int getMetricsCategory() {
        return 66;
    }
}
