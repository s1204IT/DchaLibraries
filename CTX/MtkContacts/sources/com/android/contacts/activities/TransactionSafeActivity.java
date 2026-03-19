package com.android.contacts.activities;

import android.app.Activity;
import android.os.Bundle;

public abstract class TransactionSafeActivity extends Activity {
    private boolean mIsSafeToCommitTransactions;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mIsSafeToCommitTransactions = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mIsSafeToCommitTransactions = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mIsSafeToCommitTransactions = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mIsSafeToCommitTransactions = false;
    }

    public boolean isSafeToCommitTransactions() {
        return this.mIsSafeToCommitTransactions;
    }
}
