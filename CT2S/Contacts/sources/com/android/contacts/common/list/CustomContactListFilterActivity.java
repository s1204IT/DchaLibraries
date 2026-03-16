package com.android.contacts.common.list;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.AsyncTaskLoader;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.EmptyService;
import com.android.contacts.common.util.LocalizedNameResolver;
import com.android.contacts.common.util.WeakAsyncTask;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class CustomContactListFilterActivity extends Activity implements LoaderManager.LoaderCallbacks<AccountSet>, View.OnClickListener, ExpandableListView.OnChildClickListener {
    private static Comparator<GroupDelta> sIdComparator = new Comparator<GroupDelta>() {
        @Override
        public int compare(GroupDelta object1, GroupDelta object2) {
            Long id1 = object1.getId();
            Long id2 = object2.getId();
            if (id1 == null && id2 == null) {
                return 0;
            }
            if (id1 == null) {
                return -1;
            }
            if (id2 == null) {
                return 1;
            }
            if (id1.longValue() < id2.longValue()) {
                return -1;
            }
            return id1.longValue() > id2.longValue() ? 1 : 0;
        }
    };
    private DisplayAdapter mAdapter;
    private ExpandableListView mList;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.contact_list_filter_custom);
        this.mList = (ExpandableListView) findViewById(android.R.id.list);
        this.mList.setOnChildClickListener(this);
        this.mList.setHeaderDividersEnabled(true);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.mAdapter = new DisplayAdapter(this);
        getLayoutInflater();
        findViewById(R.id.btn_done).setOnClickListener(this);
        findViewById(R.id.btn_discard).setOnClickListener(this);
        this.mList.setOnCreateContextMenuListener(this);
        this.mList.setAdapter(this.mAdapter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class CustomFilterConfigurationLoader extends AsyncTaskLoader<AccountSet> {
        private AccountSet mAccountSet;

        public CustomFilterConfigurationLoader(Context context) {
            super(context);
        }

        @Override
        public AccountSet loadInBackground() {
            Context context = getContext();
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
            ContentResolver resolver = context.getContentResolver();
            AccountSet accounts = new AccountSet();
            for (AccountWithDataSet account : accountTypes.getAccounts(false)) {
                AccountType accountType = accountTypes.getAccountTypeForAccount(account);
                if (!accountType.isExtension() || account.hasData(context)) {
                    AccountDisplay accountDisplay = new AccountDisplay(resolver, account.name, account.type, account.dataSet);
                    Uri.Builder groupsUri = ContactsContract.Groups.CONTENT_URI.buildUpon().appendQueryParameter("account_name", account.name).appendQueryParameter("account_type", account.type);
                    if (account.dataSet != null) {
                        groupsUri.appendQueryParameter("data_set", account.dataSet).build();
                    }
                    Cursor cursor = resolver.query(groupsUri.build(), null, null, null, null);
                    if (cursor != null) {
                        EntityIterator iterator = ContactsContract.Groups.newEntityIterator(cursor);
                        boolean hasGroups = false;
                        while (iterator.hasNext()) {
                            try {
                                ContentValues values = ((Entity) iterator.next()).getEntityValues();
                                GroupDelta group = GroupDelta.fromBefore(values);
                                accountDisplay.addGroup(group);
                                hasGroups = true;
                            } catch (Throwable th) {
                                iterator.close();
                                throw th;
                            }
                        }
                        accountDisplay.mUngrouped = GroupDelta.fromSettings(resolver, account.name, account.type, account.dataSet, hasGroups);
                        accountDisplay.addGroup(accountDisplay.mUngrouped);
                        iterator.close();
                        accounts.add(accountDisplay);
                    } else {
                        continue;
                    }
                }
            }
            return accounts;
        }

        @Override
        public void deliverResult(AccountSet cursor) {
            if (!isReset()) {
                this.mAccountSet = cursor;
                if (isStarted()) {
                    super.deliverResult(cursor);
                }
            }
        }

        @Override
        protected void onStartLoading() {
            if (this.mAccountSet != null) {
                deliverResult(this.mAccountSet);
            }
            if (takeContentChanged() || this.mAccountSet == null) {
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
            this.mAccountSet = null;
        }
    }

    @Override
    protected void onStart() {
        getLoaderManager().initLoader(1, null, this);
        super.onStart();
    }

    @Override
    public Loader<AccountSet> onCreateLoader(int id, Bundle args) {
        return new CustomFilterConfigurationLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<AccountSet> loader, AccountSet data) {
        this.mAdapter.setAccounts(data);
    }

    @Override
    public void onLoaderReset(Loader<AccountSet> loader) {
        this.mAdapter.setAccounts(null);
    }

    protected static class GroupDelta extends ValuesDelta {
        private boolean mAccountHasGroups;
        private boolean mUngrouped = false;

        private GroupDelta() {
        }

        public static GroupDelta fromSettings(ContentResolver resolver, String accountName, String accountType, String dataSet, boolean accountHasGroups) {
            GroupDelta ungrouped;
            Uri.Builder settingsUri = ContactsContract.Settings.CONTENT_URI.buildUpon().appendQueryParameter("account_name", accountName).appendQueryParameter("account_type", accountType);
            if (dataSet != null) {
                settingsUri.appendQueryParameter("data_set", dataSet);
            }
            Cursor cursor = resolver.query(settingsUri.build(), new String[]{"should_sync", "ungrouped_visible"}, null, null, null);
            try {
                ContentValues values = new ContentValues();
                values.put("account_name", accountName);
                values.put("account_type", accountType);
                values.put("data_set", dataSet);
                if (cursor != null && cursor.moveToFirst()) {
                    values.put("should_sync", Integer.valueOf(cursor.getInt(0)));
                    values.put("ungrouped_visible", Integer.valueOf(cursor.getInt(1)));
                    ungrouped = fromBefore(values).setUngrouped(accountHasGroups);
                } else {
                    values.put("should_sync", (Integer) 1);
                    values.put("ungrouped_visible", (Integer) 0);
                    ungrouped = fromAfter(values).setUngrouped(accountHasGroups);
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return ungrouped;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        public static GroupDelta fromBefore(ContentValues before) {
            GroupDelta entry = new GroupDelta();
            entry.mBefore = before;
            entry.mAfter = new ContentValues();
            return entry;
        }

        public static GroupDelta fromAfter(ContentValues after) {
            GroupDelta entry = new GroupDelta();
            entry.mBefore = null;
            entry.mAfter = after;
            return entry;
        }

        protected GroupDelta setUngrouped(boolean accountHasGroups) {
            this.mUngrouped = true;
            this.mAccountHasGroups = accountHasGroups;
            return this;
        }

        @Override
        public boolean beforeExists() {
            return this.mBefore != null;
        }

        public boolean getShouldSync() {
            return getAsInteger(this.mUngrouped ? "should_sync" : "should_sync", 1).intValue() != 0;
        }

        public boolean getVisible() {
            return getAsInteger(this.mUngrouped ? "ungrouped_visible" : "group_visible", 0).intValue() != 0;
        }

        public void putShouldSync(boolean shouldSync) {
            put(this.mUngrouped ? "should_sync" : "should_sync", shouldSync ? 1 : 0);
        }

        public void putVisible(boolean visible) {
            put(this.mUngrouped ? "ungrouped_visible" : "group_visible", visible ? 1 : 0);
        }

        private String getAccountType() {
            return (this.mBefore == null ? this.mAfter : this.mBefore).getAsString("account_type");
        }

        public CharSequence getTitle(Context context) {
            if (this.mUngrouped) {
                String customAllContactsName = LocalizedNameResolver.getAllContactsName(context, getAccountType());
                if (customAllContactsName == null) {
                    if (this.mAccountHasGroups) {
                        return context.getText(R.string.display_ungrouped);
                    }
                    return context.getText(R.string.display_all_contacts);
                }
                return customAllContactsName;
            }
            Integer titleRes = getAsInteger("title_res");
            if (titleRes != null) {
                String packageName = getAsString("res_package");
                return context.getPackageManager().getText(packageName, titleRes.intValue(), null);
            }
            return getAsString("title");
        }

        public ContentProviderOperation buildDiff() {
            String[] selectionArgs;
            if (isInsert()) {
                if (this.mUngrouped) {
                    this.mAfter.remove(this.mIdColumn);
                    return ContentProviderOperation.newInsert(ContactsContract.Settings.CONTENT_URI).withValues(this.mAfter).build();
                }
                throw new IllegalStateException("Unexpected diff");
            }
            if (!isUpdate()) {
                return null;
            }
            if (!this.mUngrouped) {
                return ContentProviderOperation.newUpdate(CustomContactListFilterActivity.addCallerIsSyncAdapterParameter(ContactsContract.Groups.CONTENT_URI)).withSelection("_id=" + getId(), null).withValues(this.mAfter).build();
            }
            String accountName = getAsString("account_name");
            String accountType = getAsString("account_type");
            String dataSet = getAsString("data_set");
            StringBuilder selection = new StringBuilder("account_name=? AND account_type=?");
            if (dataSet == null) {
                selection.append(" AND data_set IS NULL");
                selectionArgs = new String[]{accountName, accountType};
            } else {
                selection.append(" AND data_set=?");
                selectionArgs = new String[]{accountName, accountType, dataSet};
            }
            return ContentProviderOperation.newUpdate(ContactsContract.Settings.CONTENT_URI).withSelection(selection.toString(), selectionArgs).withValues(this.mAfter).build();
        }
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter("caller_is_syncadapter", "true").build();
    }

    protected static class AccountSet extends ArrayList<AccountDisplay> {
        protected AccountSet() {
        }

        public ArrayList<ContentProviderOperation> buildDiff() {
            ArrayList<ContentProviderOperation> diff = Lists.newArrayList();
            for (AccountDisplay account : this) {
                account.buildDiff(diff);
            }
            return diff;
        }
    }

    protected static class AccountDisplay {
        public final String mDataSet;
        public final String mName;
        public final String mType;
        public GroupDelta mUngrouped;
        public ArrayList<GroupDelta> mSyncedGroups = Lists.newArrayList();
        public ArrayList<GroupDelta> mUnsyncedGroups = Lists.newArrayList();

        public AccountDisplay(ContentResolver resolver, String accountName, String accountType, String dataSet) {
            this.mName = accountName;
            this.mType = accountType;
            this.mDataSet = dataSet;
        }

        private void addGroup(GroupDelta group) {
            if (group.getShouldSync()) {
                this.mSyncedGroups.add(group);
            } else {
                this.mUnsyncedGroups.add(group);
            }
        }

        public void setShouldSync(boolean shouldSync) {
            Iterator<GroupDelta> oppositeChildren = shouldSync ? this.mUnsyncedGroups.iterator() : this.mSyncedGroups.iterator();
            while (oppositeChildren.hasNext()) {
                GroupDelta child = oppositeChildren.next();
                setShouldSync(child, shouldSync, false);
                oppositeChildren.remove();
            }
        }

        public void setShouldSync(GroupDelta child, boolean shouldSync) {
            setShouldSync(child, shouldSync, true);
        }

        public void setShouldSync(GroupDelta child, boolean shouldSync, boolean attemptRemove) {
            child.putShouldSync(shouldSync);
            if (shouldSync) {
                if (attemptRemove) {
                    this.mUnsyncedGroups.remove(child);
                }
                this.mSyncedGroups.add(child);
                Collections.sort(this.mSyncedGroups, CustomContactListFilterActivity.sIdComparator);
                return;
            }
            if (attemptRemove) {
                this.mSyncedGroups.remove(child);
            }
            this.mUnsyncedGroups.add(child);
        }

        public void buildDiff(ArrayList<ContentProviderOperation> diff) {
            for (GroupDelta group : this.mSyncedGroups) {
                ContentProviderOperation oper = group.buildDiff();
                if (oper != null) {
                    diff.add(oper);
                }
            }
            for (GroupDelta group2 : this.mUnsyncedGroups) {
                ContentProviderOperation oper2 = group2.buildDiff();
                if (oper2 != null) {
                    diff.add(oper2);
                }
            }
        }
    }

    protected static class DisplayAdapter extends BaseExpandableListAdapter {
        private AccountTypeManager mAccountTypes;
        private AccountSet mAccounts;
        private boolean mChildWithPhones = false;
        private Context mContext;
        private LayoutInflater mInflater;

        public DisplayAdapter(Context context) {
            this.mContext = context;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mAccountTypes = AccountTypeManager.getInstance(context);
        }

        public void setAccounts(AccountSet accounts) {
            this.mAccounts = accounts;
            notifyDataSetChanged();
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.custom_contact_list_filter_account, parent, false);
            }
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
            AccountDisplay account = (AccountDisplay) getGroup(groupPosition);
            AccountType accountType = this.mAccountTypes.getAccountType(account.mType, account.mDataSet);
            text1.setText(account.mName);
            text1.setVisibility(account.mName == null ? 8 : 0);
            text2.setText(accountType.getDisplayLabel(this.mContext));
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.custom_contact_list_filter_group, parent, false);
            }
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
            CheckBox checkbox = (CheckBox) convertView.findViewById(android.R.id.checkbox);
            this.mAccounts.get(groupPosition);
            GroupDelta child = (GroupDelta) getChild(groupPosition, childPosition);
            if (child != null) {
                boolean groupVisible = child.getVisible();
                checkbox.setVisibility(0);
                checkbox.setChecked(groupVisible);
                CharSequence groupTitle = child.getTitle(this.mContext);
                text1.setText(groupTitle);
                text2.setVisibility(8);
            } else {
                checkbox.setVisibility(8);
                text1.setText(R.string.display_more_groups);
                text2.setVisibility(8);
            }
            return convertView;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            AccountDisplay account = this.mAccounts.get(groupPosition);
            boolean validChild = childPosition >= 0 && childPosition < account.mSyncedGroups.size();
            if (validChild) {
                return account.mSyncedGroups.get(childPosition);
            }
            return null;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            Long childId;
            GroupDelta child = (GroupDelta) getChild(groupPosition, childPosition);
            if (child == null || (childId = child.getId()) == null) {
                return Long.MIN_VALUE;
            }
            return childId.longValue();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            AccountDisplay account = this.mAccounts.get(groupPosition);
            boolean anyHidden = account.mUnsyncedGroups.size() > 0;
            return (anyHidden ? 1 : 0) + account.mSyncedGroups.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return this.mAccounts.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            if (this.mAccounts == null) {
                return 0;
            }
            return this.mAccounts.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_done:
                doSaveAction();
                break;
            case R.id.btn_discard:
                finish();
                break;
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        CheckBox checkbox = (CheckBox) view.findViewById(android.R.id.checkbox);
        GroupDelta child = (GroupDelta) this.mAdapter.getChild(groupPosition, childPosition);
        if (child != null) {
            checkbox.toggle();
            child.putVisible(checkbox.isChecked());
            return true;
        }
        openContextMenu(view);
        return true;
    }

    protected int getSyncMode(AccountDisplay account) {
        return ("com.google".equals(account.mType) && account.mDataSet == null) ? 2 : 0;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (menuInfo instanceof ExpandableListView.ExpandableListContextMenuInfo) {
            ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
            int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
            if (childPosition != -1) {
                AccountDisplay account = (AccountDisplay) this.mAdapter.getGroup(groupPosition);
                GroupDelta child = (GroupDelta) this.mAdapter.getChild(groupPosition, childPosition);
                int syncMode = getSyncMode(account);
                if (syncMode != 0) {
                    if (child != null) {
                        showRemoveSync(menu, account, child, syncMode);
                    } else {
                        showAddSync(menu, account, syncMode);
                    }
                }
            }
        }
    }

    protected void showRemoveSync(ContextMenu menu, final AccountDisplay account, final GroupDelta child, final int syncMode) {
        final CharSequence title = child.getTitle(this);
        menu.setHeaderTitle(title);
        menu.add(R.string.menu_sync_remove).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                CustomContactListFilterActivity.this.handleRemoveSync(account, child, syncMode, title);
                return true;
            }
        });
    }

    protected void handleRemoveSync(final AccountDisplay account, final GroupDelta child, int syncMode, CharSequence title) {
        boolean shouldSyncUngrouped = account.mUngrouped.getShouldSync();
        if (syncMode == 2 && shouldSyncUngrouped && !child.equals(account.mUngrouped)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            CharSequence removeMessage = getString(R.string.display_warn_remove_ungrouped, new Object[]{title});
            builder.setTitle(R.string.menu_sync_remove);
            builder.setMessage(removeMessage);
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    account.setShouldSync(account.mUngrouped, false);
                    account.setShouldSync(child, false);
                    CustomContactListFilterActivity.this.mAdapter.notifyDataSetChanged();
                }
            });
            builder.show();
            return;
        }
        account.setShouldSync(child, false);
        this.mAdapter.notifyDataSetChanged();
    }

    protected void showAddSync(ContextMenu menu, final AccountDisplay account, final int syncMode) {
        menu.setHeaderTitle(R.string.dialog_sync_add);
        for (final GroupDelta child : account.mUnsyncedGroups) {
            if (!child.getShouldSync()) {
                CharSequence title = child.getTitle(this);
                menu.add(title).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (child.mUngrouped && syncMode == 2) {
                            account.setShouldSync(true);
                        } else {
                            account.setShouldSync(child, true);
                        }
                        CustomContactListFilterActivity.this.mAdapter.notifyDataSetChanged();
                        return true;
                    }
                });
            }
        }
    }

    private void doSaveAction() {
        if (this.mAdapter == null || this.mAdapter.mAccounts == null) {
            finish();
            return;
        }
        setResult(-1);
        ArrayList<ContentProviderOperation> diff = this.mAdapter.mAccounts.buildDiff();
        if (diff.isEmpty()) {
            finish();
        } else {
            new UpdateTask(this).execute(new ArrayList[]{diff});
        }
    }

    public static class UpdateTask extends WeakAsyncTask<ArrayList<ContentProviderOperation>, Void, Void, Activity> {
        private ProgressDialog mProgress;

        public UpdateTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(Activity target) {
            this.mProgress = ProgressDialog.show(target, null, target.getText(R.string.savingDisplayGroups));
            target.startService(new Intent(target, (Class<?>) EmptyService.class));
        }

        @Override
        protected Void doInBackground(Activity target, ArrayList<ContentProviderOperation>... params) {
            new ContentValues();
            ContentResolver resolver = target.getContentResolver();
            try {
                ArrayList<ContentProviderOperation> diff = params[0];
                resolver.applyBatch("com.android.contacts", diff);
                return null;
            } catch (OperationApplicationException e) {
                Log.e("CustomContactListFilterActivity", "Problem saving display groups", e);
                return null;
            } catch (RemoteException e2) {
                Log.e("CustomContactListFilterActivity", "Problem saving display groups", e2);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Activity target, Void result) {
            try {
                this.mProgress.dismiss();
            } catch (Exception e) {
                Log.e("CustomContactListFilterActivity", "Error dismissing progress dialog", e);
            }
            target.finish();
            target.stopService(new Intent(target, (Class<?>) EmptyService.class));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(0);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
