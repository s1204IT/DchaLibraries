package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.util.DialogManager;
import java.util.ArrayList;

public class ContactEditorActivity extends ContactsActivity implements DialogManager.DialogShowingViewActivity {
    private boolean mFinishActivityOnSaveCompleted;
    private ContactEditorFragment mFragment;
    private DialogManager mDialogManager = new DialogManager(this);
    private final ContactEditorFragment.Listener mFragmentListener = new ContactEditorFragment.Listener() {
        @Override
        public void onDeleteRequested(Uri contactUri) {
            ContactDeletionInteraction.start(ContactEditorActivity.this, contactUri, true);
        }

        @Override
        public void onReverted() {
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onSaveFinished(Intent resultIntent) {
            if (ContactEditorActivity.this.mFinishActivityOnSaveCompleted) {
                ContactEditorActivity.this.setResult(resultIntent == null ? 0 : -1, resultIntent);
            } else if (resultIntent != null) {
                ContactEditorActivity.this.startActivity(resultIntent);
            }
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onContactSplit(Uri newLookupUri) {
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onContactNotFound() {
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onEditOtherContactRequested(Uri contactLookupUri, ArrayList<ContentValues> values) {
            Intent intent = new Intent("android.intent.action.EDIT", contactLookupUri);
            intent.setFlags(41943040);
            intent.putExtra("addToDefaultDirectory", "");
            if (values != null && values.size() != 0) {
                intent.putParcelableArrayListExtra("data", values);
            }
            ContactEditorActivity.this.startActivity(intent);
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onCustomCreateContactActivityRequested(AccountWithDataSet account, Bundle intentExtras) {
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(ContactEditorActivity.this);
            AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            Intent intent = new Intent();
            intent.setClassName(accountType.syncAdapterPackageName, accountType.getCreateContactActivityClassName());
            intent.setAction("android.intent.action.INSERT");
            intent.setType("vnd.android.cursor.item/contact");
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }
            intent.putExtra("account_name", account.name);
            intent.putExtra("account_type", account.type);
            intent.putExtra("data_set", account.dataSet);
            intent.setFlags(41943040);
            ContactEditorActivity.this.startActivity(intent);
            ContactEditorActivity.this.finish();
        }

        @Override
        public void onCustomEditContactActivityRequested(AccountWithDataSet account, Uri rawContactUri, Bundle intentExtras, boolean redirect) {
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(ContactEditorActivity.this);
            AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            Intent intent = new Intent();
            intent.setClassName(accountType.syncAdapterPackageName, accountType.getEditContactActivityClassName());
            intent.setAction("android.intent.action.EDIT");
            intent.setData(rawContactUri);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }
            if (redirect) {
                intent.setFlags(41943040);
                ContactEditorActivity.this.startActivity(intent);
                ContactEditorActivity.this.finish();
                return;
            }
            ContactEditorActivity.this.startActivity(intent);
        }
    };

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Intent intent = getIntent();
        String action = intent.getAction();
        this.mFinishActivityOnSaveCompleted = intent.getBooleanExtra("finishActivityOnSaveCompleted", false);
        if ("joinCompleted".equals(action)) {
            finish();
            return;
        }
        if ("saveCompleted".equals(action)) {
            finish();
            return;
        }
        setContentView(R.layout.contact_editor_activity);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if ("android.intent.action.EDIT".equals(action)) {
                actionBar.setTitle(getResources().getString(R.string.contact_editor_title_existing_contact));
            } else {
                actionBar.setTitle(getResources().getString(R.string.contact_editor_title_new_contact));
            }
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mFragment = (ContactEditorFragment) getFragmentManager().findFragmentById(R.id.contact_editor_fragment);
        this.mFragment.setListener(this.mFragmentListener);
        Uri uri = "android.intent.action.EDIT".equals(action) ? getIntent().getData() : null;
        this.mFragment.load(action, uri, getIntent().getExtras());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (this.mFragment != null) {
            String action = intent.getAction();
            if ("android.intent.action.EDIT".equals(action)) {
                this.mFragment.setIntentExtras(intent.getExtras());
            } else if ("saveCompleted".equals(action)) {
                this.mFragment.onSaveCompleted(true, intent.getIntExtra("saveMode", 0), intent.getBooleanExtra("saveSucceeded", false), intent.getData());
            } else if ("joinCompleted".equals(action)) {
                this.mFragment.onJoinCompleted(intent.getData());
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) {
            return this.mDialogManager.onCreateDialog(id, args);
        }
        Log.w("ContactEditorActivity", "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    @Override
    public void onBackPressed() {
        this.mFragment.save(0);
    }

    @Override
    public DialogManager getDialogManager() {
        return this.mDialogManager;
    }
}
