package com.android.contacts.common.activity;

import android.app.Activity;
import android.os.Bundle;

public abstract class TransactionSafeActivity extends Activity {
    private boolean mIsSafeToCommitTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mIsSafeToCommitTransactions = false;
    }
}
