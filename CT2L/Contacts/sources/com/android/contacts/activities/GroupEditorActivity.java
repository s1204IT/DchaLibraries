package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.group.GroupEditorFragment;
import com.android.contacts.util.DialogManager;

public class GroupEditorActivity extends ContactsActivity implements DialogManager.DialogShowingViewActivity {
    private GroupEditorFragment mFragment;
    private DialogManager mDialogManager = new DialogManager(this);
    private final GroupEditorFragment.Listener mFragmentListener = new GroupEditorFragment.Listener() {
        @Override
        public void onGroupNotFound() {
            GroupEditorActivity.this.finish();
        }

        @Override
        public void onReverted() {
            GroupEditorActivity.this.finish();
        }

        @Override
        public void onAccountsNotFound() {
            GroupEditorActivity.this.finish();
        }

        @Override
        public void onSaveFinished(int resultCode, Intent resultIntent) {
            if (resultIntent != null) {
                Intent intent = new Intent(GroupEditorActivity.this, (Class<?>) GroupDetailActivity.class);
                intent.setData(resultIntent.getData());
                intent.setFlags(67108864);
                GroupEditorActivity.this.startActivity(intent);
                GroupEditorActivity.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        String action = getIntent().getAction();
        if ("saveCompleted".equals(action)) {
            finish();
            return;
        }
        setContentView(R.layout.group_editor_activity);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
            View customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, (ViewGroup) null);
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GroupEditorActivity.this.mFragment.onDoneClicked();
                }
            });
            actionBar.setDisplayOptions(16, 26);
            actionBar.setCustomView(customActionBarView);
        }
        this.mFragment = (GroupEditorFragment) getFragmentManager().findFragmentById(R.id.group_editor_fragment);
        this.mFragment.setListener(this.mFragmentListener);
        this.mFragment.setContentResolver(getContentResolver());
        if (savedState == null) {
            Uri uri = "android.intent.action.EDIT".equals(action) ? getIntent().getData() : null;
            this.mFragment.load(action, uri, getIntent().getExtras());
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) {
            return this.mDialogManager.onCreateDialog(id, args);
        }
        Log.w("GroupEditorActivity", "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    @Override
    public void onBackPressed() {
        if (!this.mFragment.save()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (this.mFragment != null) {
            String action = intent.getAction();
            if ("saveCompleted".equals(action)) {
                this.mFragment.onSaveCompleted(true, intent.getData());
            }
        }
    }

    @Override
    public DialogManager getDialogManager() {
        return this.mDialogManager;
    }
}
