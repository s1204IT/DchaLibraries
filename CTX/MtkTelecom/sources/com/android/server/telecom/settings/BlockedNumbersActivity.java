package com.android.server.telecom.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.server.telecom.R;
import com.android.server.telecom.settings.BlockNumberTaskFragment;

public class BlockedNumbersActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, TextWatcher, View.OnClickListener, BlockNumberTaskFragment.Listener {
    private static final String[] PROJECTION = {"_id", "original_number"};
    private BlockedNumbersAdapter mAdapter;
    private TextView mAddButton;
    private Button mBlockButton;
    private BlockNumberTaskFragment mBlockNumberTaskFragment;
    private BroadcastReceiver mBlockingStatusReceiver;
    private RelativeLayout mButterBar;
    private ProgressBar mProgressBar;
    private TextView mReEnableButton;

    public static Intent getIntentForStartingActivity() {
        Intent intent = new Intent("android.telecom.action.MANAGE_BLOCKED_NUMBERS");
        intent.setPackage("com.android.server.telecom");
        return intent;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.xml.activity_blocked_numbers);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (!BlockedNumberContract.canCurrentUserBlockNumbers(this)) {
            ((TextView) findViewById(R.id.non_primary_user)).setVisibility(0);
            ((LinearLayout) findViewById(R.id.manage_blocked_ui)).setVisibility(8);
            return;
        }
        FragmentManager fragmentManager = getFragmentManager();
        this.mBlockNumberTaskFragment = (BlockNumberTaskFragment) fragmentManager.findFragmentByTag("block_number_task_fragment");
        if (this.mBlockNumberTaskFragment == null) {
            this.mBlockNumberTaskFragment = new BlockNumberTaskFragment();
            fragmentManager.beginTransaction().add(this.mBlockNumberTaskFragment, "block_number_task_fragment").commit();
        }
        this.mAddButton = (TextView) findViewById(R.id.add_blocked);
        this.mAddButton.setOnClickListener(this);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        this.mAdapter = new BlockedNumbersAdapter(this, R.xml.layout_blocked_number, null, new String[]{"original_number"}, new int[]{R.id.blocked_number}, 0);
        ListView listView = getListView();
        listView.setAdapter((ListAdapter) this.mAdapter);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        this.mButterBar = (RelativeLayout) findViewById(R.id.butter_bar);
        this.mReEnableButton = (TextView) this.mButterBar.findViewById(R.id.reenable_button);
        this.mReEnableButton.setOnClickListener(this);
        updateButterBar();
        updateEnhancedCallBlockingFragment(BlockedNumbersUtil.isEnhancedCallBlockingEnabledByPlatform(this));
        this.mBlockingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BlockedNumbersActivity.this.updateButterBar();
            }
        };
        registerReceiver(this.mBlockingStatusReceiver, new IntentFilter("android.provider.action.BLOCK_SUPPRESSION_STATE_CHANGED"));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onDestroy() {
        if (this.mBlockingStatusReceiver != null) {
            unregisterReceiver(this.mBlockingStatusReceiver);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void updateButterBar() {
        if (BlockedNumberContract.SystemContract.getBlockSuppressionStatus(this).isSuppressed) {
            this.mButterBar.setVisibility(0);
        } else {
            this.mButterBar.setVisibility(8);
        }
    }

    private void updateEnhancedCallBlockingFragment(boolean z) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragmentFindFragmentById = fragmentManager.findFragmentById(R.id.enhanced_call_blocking_container);
        if (!z && fragmentFindFragmentById == null) {
            return;
        }
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        if (z) {
            if (fragmentFindFragmentById == null) {
                fragmentTransactionBeginTransaction.add(R.id.enhanced_call_blocking_container, new EnhancedCallBlockingFragment());
            } else {
                fragmentTransactionBeginTransaction.show(fragmentFindFragmentById);
            }
        } else {
            fragmentTransactionBeginTransaction.hide(fragmentFindFragmentById);
        }
        fragmentTransactionBeginTransaction.commit();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, BlockedNumberContract.BlockedNumbers.CONTENT_URI, PROJECTION, "((original_number NOTNULL) AND (original_number != '' ))", null, "_id DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mAdapter.swapCursor(cursor);
        this.mProgressBar.setVisibility(8);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        this.mAdapter.swapCursor(null);
        this.mProgressBar.setVisibility(0);
    }

    @Override
    public void onClick(View view) {
        if (view == this.mAddButton) {
            showAddBlockedNumberDialog();
        } else if (view == this.mReEnableButton) {
            BlockedNumberContract.SystemContract.endBlockSuppression(this);
            this.mButterBar.setVisibility(8);
        }
    }

    private void showAddBlockedNumberDialog() {
        View viewInflate = getLayoutInflater().inflate(R.xml.add_blocked_number_dialog, (ViewGroup) null);
        final EditText editText = (EditText) viewInflate.findViewById(R.id.add_blocked_number);
        editText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        editText.addTextChangedListener(this);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(viewInflate).setPositiveButton(R.string.block_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BlockedNumbersActivity.this.addBlockedNumber(PhoneNumberUtils.stripSeparators(editText.getText().toString()));
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                BlockedNumbersActivity.this.mBlockButton = ((AlertDialog) dialogInterface).getButton(-1);
                BlockedNumbersActivity.this.mBlockButton.setEnabled(false);
                ((InputMethodManager) BlockedNumbersActivity.this.getSystemService("input_method")).showSoftInput(editText, 1);
            }
        });
        alertDialogCreate.show();
    }

    private void addBlockedNumber(String str) {
        if (PhoneNumberUtils.isEmergencyNumber(str)) {
            Toast.makeText(this, getString(R.string.blocked_numbers_block_emergency_number_message), 0).show();
        } else {
            this.mAddButton.setEnabled(false);
            this.mBlockNumberTaskFragment.blockIfNotAlreadyBlocked(str, this);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (this.mBlockButton != null) {
            this.mBlockButton.setEnabled(!TextUtils.isEmpty(PhoneNumberUtils.stripSeparators(charSequence.toString())));
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    public void onBlocked(String str, boolean z) {
        if (z) {
            BlockedNumbersUtil.showToastWithFormattedNumber(this, R.string.blocked_numbers_number_already_blocked_message, str);
        } else {
            BlockedNumbersUtil.showToastWithFormattedNumber(this, R.string.blocked_numbers_number_blocked_message, str);
        }
        this.mAddButton.setEnabled(true);
    }
}
