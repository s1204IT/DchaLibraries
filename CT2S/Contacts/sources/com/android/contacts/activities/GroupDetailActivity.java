package com.android.contacts.activities;

import android.app.ActionBar;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.group.GroupDetailDisplayUtils;
import com.android.contacts.group.GroupDetailFragment;

public class GroupDetailActivity extends ContactsActivity {
    private String mAccountTypeString;
    private String mDataSet;
    private GroupDetailFragment mFragment;
    private final GroupDetailFragment.Listener mFragmentListener = new GroupDetailFragment.Listener() {
        @Override
        public void onGroupSizeUpdated(String size) {
            GroupDetailActivity.this.getActionBar().setSubtitle(size);
        }

        @Override
        public void onGroupTitleUpdated(String title) {
            GroupDetailActivity.this.getActionBar().setTitle(title);
        }

        @Override
        public void onAccountTypeUpdated(String accountTypeString, String dataSet) {
            GroupDetailActivity.this.mAccountTypeString = accountTypeString;
            GroupDetailActivity.this.mDataSet = dataSet;
            GroupDetailActivity.this.invalidateOptionsMenu();
        }

        @Override
        public void onEditRequested(Uri groupUri) {
            Intent intent = new Intent(GroupDetailActivity.this, (Class<?>) GroupEditorActivity.class);
            intent.setData(groupUri);
            intent.setAction("android.intent.action.EDIT");
            GroupDetailActivity.this.startActivity(intent);
        }

        @Override
        public void onContactSelected(Uri contactUri) {
            Intent intent = new Intent("android.intent.action.VIEW", contactUri);
            GroupDetailActivity.this.startActivity(intent);
        }
    };
    private boolean mShowGroupSourceInActionBar;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.group_detail_activity);
        this.mShowGroupSourceInActionBar = getResources().getBoolean(R.bool.config_show_group_action_in_action_bar);
        this.mFragment = (GroupDetailFragment) getFragmentManager().findFragmentById(R.id.group_detail_fragment);
        this.mFragment.setListener(this.mFragmentListener);
        this.mFragment.setShowGroupSourceInActionBar(this.mShowGroupSourceInActionBar);
        this.mFragment.loadGroup(getIntent().getData());
        this.mFragment.closeActivityAfterDelete(true);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(12, 14);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (this.mShowGroupSourceInActionBar) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.group_source, menu);
            return true;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem groupSourceMenuItem;
        if (!this.mShowGroupSourceInActionBar || (groupSourceMenuItem = menu.findItem(R.id.menu_group_source)) == null) {
            return false;
        }
        AccountTypeManager manager = AccountTypeManager.getInstance(this);
        final AccountType accountType = manager.getAccountType(this.mAccountTypeString, this.mDataSet);
        if (TextUtils.isEmpty(this.mAccountTypeString) || TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            groupSourceMenuItem.setVisible(false);
            return false;
        }
        View groupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(this);
        GroupDetailDisplayUtils.bindGroupSourceView(this, groupSourceView, this.mAccountTypeString, this.mDataSet);
        groupSourceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, GroupDetailActivity.this.mFragment.getGroupId());
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                intent.setClassName(accountType.syncAdapterPackageName, accountType.getViewGroupActivity());
                GroupDetailActivity.this.startActivity(intent);
            }
        });
        groupSourceMenuItem.setActionView(groupSourceView);
        groupSourceMenuItem.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, (Class<?>) PeopleActivity.class);
                intent.addFlags(67108864);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
