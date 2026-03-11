package com.android.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public final class ActiveNetworkScorerDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private String mNewPackageName;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mNewPackageName = intent.getStringExtra("packageName");
        if (!buildDialog()) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -1:
                NetworkScoreManager nsm = (NetworkScoreManager) getSystemService("network_score");
                if (nsm.setActiveScorer(this.mNewPackageName)) {
                    setResult(-1);
                }
                break;
        }
    }

    private boolean buildDialog() {
        if (UserHandle.myUserId() != 0) {
            Log.i("ActiveNetScorerDlg", "Can only set scorer for owner user.");
            return false;
        }
        NetworkScorerAppManager.NetworkScorerAppData newScorer = NetworkScorerAppManager.getScorer(this, this.mNewPackageName);
        if (newScorer == null) {
            Log.e("ActiveNetScorerDlg", "New package " + this.mNewPackageName + " is not a valid scorer.");
            return false;
        }
        NetworkScorerAppManager.NetworkScorerAppData oldScorer = NetworkScorerAppManager.getActiveScorer(this);
        if (oldScorer != null && TextUtils.equals(oldScorer.mPackageName, this.mNewPackageName)) {
            Log.i("ActiveNetScorerDlg", "New package " + this.mNewPackageName + " is already the active scorer.");
            setResult(-1);
            return false;
        }
        CharSequence newName = newScorer.mScorerName;
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.network_scorer_change_active_dialog_title);
        if (oldScorer != null) {
            p.mMessage = getString(R.string.network_scorer_change_active_dialog_text, new Object[]{newName, oldScorer.mScorerName});
        } else {
            p.mMessage = getString(R.string.network_scorer_change_active_no_previous_dialog_text, new Object[]{newName});
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();
        return true;
    }
}
