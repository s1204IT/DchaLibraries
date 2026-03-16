package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class MasterClear extends Fragment {
    private View mContentView;
    private CheckBox mExternalStorage;
    private View mExternalStorageContainer;
    private Button mInitiateButton;
    private final View.OnClickListener mInitiateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!MasterClear.this.runKeyguardConfirmation(55)) {
                MasterClear.this.showFinalConfirmation();
            }
        }
    };

    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(request, null, res.getText(R.string.master_clear_gesture_explanation));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 55) {
            if (resultCode == -1) {
                showFinalConfirmation();
            } else {
                establishInitialState();
            }
        }
    }

    private void showFinalConfirmation() {
        Preference preference = new Preference(getActivity());
        preference.setFragment(MasterClearConfirm.class.getName());
        preference.setTitle(R.string.master_clear_confirm_title);
        preference.getExtras().putBoolean("erase_sd", this.mExternalStorage.isChecked());
        ((SettingsActivity) getActivity()).onPreferenceStartFragment(null, preference);
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
    }

    private boolean isExtStorageEncrypted() {
        String state = SystemProperties.get("vold.decrypt");
        return !"".equals(state);
    }

    private void loadAccountList(UserManager um) {
        View accountsLabel = this.mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout) this.mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();
        Activity context = getActivity();
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
                View titleView = newTitleView(contents, inflater);
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
                            Log.w("MasterClear", "No icon for account type " + desc.type);
                        }
                        TextView child = (TextView) inflater.inflate(R.layout.master_clear_account, (ViewGroup) contents, false);
                        child.setText(account.name);
                        if (icon != null) {
                            child.setCompoundDrawablesWithIntrinsicBounds(icon, (Drawable) null, (Drawable) null, (Drawable) null);
                        }
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
        if (!Process.myUserHandle().isOwner() || UserManager.get(getActivity()).hasUserRestriction("no_factory_reset")) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, (ViewGroup) null);
        }
        this.mContentView = inflater.inflate(R.layout.master_clear, (ViewGroup) null);
        establishInitialState();
        return this.mContentView;
    }

    private View newTitleView(ViewGroup parent, LayoutInflater inflater) {
        TypedArray a = inflater.getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
        int resId = a.getResourceId(3, 0);
        return inflater.inflate(resId, parent, false);
    }
}
