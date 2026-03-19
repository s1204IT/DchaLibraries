package com.android.internal.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.app.AlertController;

public class ConfirmUserCreationActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "CreateUser";
    private String mAccountName;
    private PersistableBundle mAccountOptions;
    private String mAccountType;
    private boolean mCanProceed;
    private UserManager mUserManager;
    private String mUserName;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        this.mUserName = intent.getStringExtra(UserManager.EXTRA_USER_NAME);
        this.mAccountName = intent.getStringExtra(UserManager.EXTRA_USER_ACCOUNT_NAME);
        this.mAccountType = intent.getStringExtra(UserManager.EXTRA_USER_ACCOUNT_TYPE);
        this.mAccountOptions = (PersistableBundle) intent.getParcelableExtra(UserManager.EXTRA_USER_ACCOUNT_OPTIONS);
        this.mUserManager = (UserManager) getSystemService(UserManager.class);
        String strCheckUserCreationRequirements = checkUserCreationRequirements();
        if (strCheckUserCreationRequirements == null) {
            finish();
            return;
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mMessage = strCheckUserCreationRequirements;
        alertParams.mPositiveButtonText = getString(17039370);
        alertParams.mPositiveButtonListener = this;
        if (this.mCanProceed) {
            alertParams.mNegativeButtonText = getString(17039360);
            alertParams.mNegativeButtonListener = this;
        }
        setupAlert();
    }

    private String checkUserCreationRequirements() {
        String callingPackage = getCallingPackage();
        if (callingPackage == null) {
            throw new SecurityException("User Creation intent must be launched with startActivityForResult");
        }
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(callingPackage, 0);
            boolean z = this.mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER) || !this.mUserManager.isAdminUser();
            boolean z2 = !this.mUserManager.canAddMoreUsers();
            boolean z3 = (this.mAccountName == null || this.mAccountType == null || (!AccountManager.get(this).someUserHasAccount(new Account(this.mAccountName, this.mAccountType)) && !this.mUserManager.someUserHasSeedAccount(this.mAccountName, this.mAccountType))) ? false : true;
            this.mCanProceed = true;
            String string = applicationInfo.loadLabel(getPackageManager()).toString();
            if (z) {
                setResult(1);
                return null;
            }
            if (z2) {
                setResult(2);
                return null;
            }
            if (z3) {
                return getString(R.string.user_creation_account_exists, string, this.mAccountName);
            }
            return getString(R.string.user_creation_adding, string, this.mAccountName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Cannot find the calling package");
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        setResult(0);
        if (i == -1 && this.mCanProceed) {
            Log.i(TAG, "Ok, creating user");
            UserInfo userInfoCreateUser = this.mUserManager.createUser(this.mUserName, 0);
            if (userInfoCreateUser == null) {
                Log.e(TAG, "Couldn't create user");
                finish();
                return;
            } else {
                this.mUserManager.setSeedAccountData(userInfoCreateUser.id, this.mAccountName, this.mAccountType, this.mAccountOptions);
                setResult(-1);
            }
        }
        finish();
    }
}
