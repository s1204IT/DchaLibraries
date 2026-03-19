package com.android.contacts.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public abstract class AppCompatTransactionSafeActivity extends AppCompatActivity {
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
