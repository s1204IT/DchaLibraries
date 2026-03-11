package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;

public abstract class RestrictedSettingsFragment extends SettingsPreferenceFragment {
    private View mAdminSupportDetails;
    private boolean mChallengeRequested;
    private boolean mChallengeSucceeded;
    private TextView mEmptyTextView;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private boolean mIsAdminUser;
    private final String mRestrictionKey;
    private RestrictionsManager mRestrictionsManager;
    private UserManager mUserManager;
    private boolean mOnlyAvailableForAdmins = false;
    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RestrictedSettingsFragment.this.mChallengeRequested) {
                return;
            }
            RestrictedSettingsFragment.this.mChallengeSucceeded = false;
            RestrictedSettingsFragment.this.mChallengeRequested = false;
        }
    };

    public RestrictedSettingsFragment(String restrictionKey) {
        this.mRestrictionKey = restrictionKey;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mRestrictionsManager = (RestrictionsManager) getSystemService("restrictions");
        this.mUserManager = (UserManager) getSystemService("user");
        this.mIsAdminUser = this.mUserManager.isAdminUser();
        if (icicle != null) {
            this.mChallengeSucceeded = icicle.getBoolean("chsc", false);
            this.mChallengeRequested = icicle.getBoolean("chrq", false);
        }
        IntentFilter offFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        offFilter.addAction("android.intent.action.USER_PRESENT");
        getActivity().registerReceiver(this.mScreenOffReceiver, offFilter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mAdminSupportDetails = initAdminSupportDetailsView();
        this.mEmptyTextView = initEmptyTextView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!getActivity().isChangingConfigurations()) {
            return;
        }
        outState.putBoolean("chrq", this.mChallengeRequested);
        outState.putBoolean("chsc", this.mChallengeSucceeded);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!shouldBeProviderProtected(this.mRestrictionKey)) {
            return;
        }
        ensurePin();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(this.mScreenOffReceiver);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 12309) {
            if (resultCode == -1) {
                this.mChallengeSucceeded = true;
                this.mChallengeRequested = false;
                return;
            } else {
                this.mChallengeSucceeded = false;
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ensurePin() {
        Intent intent;
        if (this.mChallengeSucceeded || this.mChallengeRequested || !this.mRestrictionsManager.hasRestrictionsProvider() || (intent = this.mRestrictionsManager.createLocalApprovalIntent()) == null) {
            return;
        }
        this.mChallengeRequested = true;
        this.mChallengeSucceeded = false;
        PersistableBundle request = new PersistableBundle();
        request.putString("android.request.mesg", getResources().getString(R.string.restr_pin_enter_admin_pin));
        intent.putExtra("android.content.extra.REQUEST_BUNDLE", request);
        startActivityForResult(intent, 12309);
    }

    protected boolean isRestrictedAndNotProviderProtected() {
        return (this.mRestrictionKey == null || "restrict_if_overridable".equals(this.mRestrictionKey) || !this.mUserManager.hasUserRestriction(this.mRestrictionKey) || this.mRestrictionsManager.hasRestrictionsProvider()) ? false : true;
    }

    protected boolean hasChallengeSucceeded() {
        return (this.mChallengeRequested && this.mChallengeSucceeded) || !this.mChallengeRequested;
    }

    protected boolean shouldBeProviderProtected(String restrictionKey) {
        boolean restricted;
        if (restrictionKey == null) {
            return false;
        }
        if ("restrict_if_overridable".equals(restrictionKey)) {
            restricted = true;
        } else {
            restricted = this.mUserManager.hasUserRestriction(this.mRestrictionKey);
        }
        if (restricted) {
            return this.mRestrictionsManager.hasRestrictionsProvider();
        }
        return false;
    }

    private View initAdminSupportDetailsView() {
        return getActivity().findViewById(R.id.admin_support_details);
    }

    protected TextView initEmptyTextView() {
        TextView emptyView = (TextView) getActivity().findViewById(android.R.id.empty);
        return emptyView;
    }

    public RestrictedLockUtils.EnforcedAdmin getRestrictionEnforcedAdmin() {
        this.mEnforcedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), this.mRestrictionKey, UserHandle.myUserId());
        if (this.mEnforcedAdmin != null && this.mEnforcedAdmin.userId == -10000) {
            this.mEnforcedAdmin.userId = UserHandle.myUserId();
        }
        return this.mEnforcedAdmin;
    }

    public TextView getEmptyTextView() {
        return this.mEmptyTextView;
    }

    @Override
    protected void onDataSetChanged() {
        highlightPreferenceIfNeeded();
        if (this.mAdminSupportDetails != null && isUiRestrictedByOnlyAdmin()) {
            RestrictedLockUtils.EnforcedAdmin admin = getRestrictionEnforcedAdmin();
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), this.mAdminSupportDetails, admin, false);
            setEmptyView(this.mAdminSupportDetails);
        } else if (this.mEmptyTextView != null) {
            setEmptyView(this.mEmptyTextView);
        }
        super.onDataSetChanged();
    }

    public void setIfOnlyAvailableForAdmins(boolean onlyForAdmins) {
        this.mOnlyAvailableForAdmins = onlyForAdmins;
    }

    protected boolean isUiRestricted() {
        if (isRestrictedAndNotProviderProtected() || !hasChallengeSucceeded()) {
            return true;
        }
        if (this.mIsAdminUser) {
            return false;
        }
        return this.mOnlyAvailableForAdmins;
    }

    protected boolean isUiRestrictedByOnlyAdmin() {
        if (!isUiRestricted() || this.mUserManager.hasBaseUserRestriction(this.mRestrictionKey, UserHandle.of(UserHandle.myUserId()))) {
            return false;
        }
        return this.mIsAdminUser || !this.mOnlyAvailableForAdmins;
    }
}
