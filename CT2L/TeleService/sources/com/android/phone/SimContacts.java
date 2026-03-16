package com.android.phone;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class SimContacts extends ADNList {
    static final ContentValues sEmptyContentValues = new ContentValues();
    private Account mAccount;
    private ProgressDialog mProgressDialog;

    private static class NamePhoneTypePair {
        final String name;
        final int phoneType;

        public NamePhoneTypePair(String nameWithPhoneType) {
            int nameLen = nameWithPhoneType.length();
            if (nameLen - 2 >= 0 && nameWithPhoneType.charAt(nameLen - 2) == '/') {
                char c = Character.toUpperCase(nameWithPhoneType.charAt(nameLen - 1));
                if (c == 'W') {
                    this.phoneType = 3;
                } else if (c == 'M' || c == 'O') {
                    this.phoneType = 2;
                } else if (c == 'H') {
                    this.phoneType = 1;
                } else {
                    this.phoneType = 7;
                }
                this.name = nameWithPhoneType.substring(0, nameLen - 2);
                return;
            }
            this.phoneType = 7;
            this.name = nameWithPhoneType;
        }
    }

    private class ImportAllSimContactsThread extends Thread implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        boolean mCanceled;

        public ImportAllSimContactsThread() {
            super("ImportAllSimContactsThread");
            this.mCanceled = false;
        }

        @Override
        public void run() {
            new ContentValues();
            ContentResolver resolver = SimContacts.this.getContentResolver();
            SimContacts.this.mCursor.moveToPosition(-1);
            while (!this.mCanceled && SimContacts.this.mCursor.moveToNext()) {
                SimContacts.actuallyImportOneSimContact(SimContacts.this.mCursor, resolver, SimContacts.this.mAccount);
                SimContacts.this.mProgressDialog.incrementProgressBy(1);
            }
            SimContacts.this.mProgressDialog.dismiss();
            SimContacts.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            this.mCanceled = true;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                this.mCanceled = true;
                SimContacts.this.mProgressDialog.dismiss();
            } else {
                Log.e("SimContacts", "Unknown button event has come: " + dialog.toString());
            }
        }
    }

    private static void actuallyImportOneSimContact(Cursor cursor, ContentResolver resolver, Account account) {
        String[] emailAddressArray;
        NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(cursor.getString(0));
        String name = namePhoneTypePair.name;
        int phoneType = namePhoneTypePair.phoneType;
        String phoneNumber = cursor.getString(1);
        String emailAddresses = cursor.getString(2);
        if (!TextUtils.isEmpty(emailAddresses)) {
            emailAddressArray = emailAddresses.split(",");
        } else {
            emailAddressArray = null;
        }
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        if (account != null) {
            builder.withValue("account_name", account.name);
            builder.withValue("account_type", account.type);
        } else {
            builder.withValues(sEmptyContentValues);
        }
        operationList.add(builder.build());
        ContentProviderOperation.Builder builder2 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder2.withValueBackReference("raw_contact_id", 0);
        builder2.withValue("mimetype", "vnd.android.cursor.item/name");
        builder2.withValue("data1", name);
        operationList.add(builder2.build());
        ContentProviderOperation.Builder builder3 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder3.withValueBackReference("raw_contact_id", 0);
        builder3.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
        builder3.withValue("data2", Integer.valueOf(phoneType));
        builder3.withValue("data1", phoneNumber);
        builder3.withValue("is_primary", 1);
        operationList.add(builder3.build());
        if (emailAddressArray != null) {
            String[] arr$ = emailAddressArray;
            for (String emailAddress : arr$) {
                ContentProviderOperation.Builder builder4 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builder4.withValueBackReference("raw_contact_id", 0);
                builder4.withValue("mimetype", "vnd.android.cursor.item/email_v2");
                builder4.withValue("data2", 4);
                builder4.withValue("data1", emailAddress);
                operationList.add(builder4.build());
            }
        }
        if (0 != 0) {
            ContentProviderOperation.Builder builder5 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder5.withValueBackReference("raw_contact_id", 0);
            builder5.withValue("mimetype", "vnd.android.cursor.item/group_membership");
            builder5.withValue("group_sourceid", null);
            operationList.add(builder5.build());
        }
        try {
            resolver.applyBatch("com.android.contacts", operationList);
        } catch (OperationApplicationException e) {
            Log.e("SimContacts", String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (RemoteException e2) {
            Log.e("SimContacts", String.format("%s: %s", e2.toString(), e2.getMessage()));
        }
    }

    private void importOneSimContact(int position) {
        ContentResolver resolver = getContentResolver();
        if (this.mCursor.moveToPosition(position)) {
            actuallyImportOneSimContact(this.mCursor, resolver, this.mAccount);
        } else {
            Log.e("SimContacts", "Failed to move the cursor to the position \"" + position + "\"");
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        if (intent != null) {
            String accountName = intent.getStringExtra("account_name");
            String accountType = intent.getStringExtra("account_type");
            if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                this.mAccount = new Account(accountName, accountType);
            }
        }
        registerForContextMenu(getListView());
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected CursorAdapter newAdapter() {
        return new SimpleCursorAdapter(this, R.layout.sim_import_list_entry, this.mCursor, new String[]{"name"}, new int[]{android.R.id.text1});
    }

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
        int subId = -1;
        if (intent.hasExtra("subscription_id")) {
            subId = intent.getIntExtra("subscription_id", -1);
        }
        if (subId != -1) {
            intent.setData(Uri.parse("content://icc/adn/subId/" + subId));
        } else {
            intent.setData(Uri.parse("content://icc/adn"));
        }
        if ("android.intent.action.PICK".equals(intent.getAction())) {
            this.mInitialSelection = intent.getIntExtra("index", 0) - 1;
        }
        return intent.getData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 2, 0, R.string.importAllSimEntries);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(2);
        if (item != null) {
            item.setVisible(this.mCursor != null && this.mCursor.getCount() > 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                CharSequence title = getString(R.string.importAllSimEntries);
                CharSequence message = getString(R.string.importingSimContacts);
                ImportAllSimContactsThread thread = new ImportAllSimContactsThread();
                if (this.mCursor == null) {
                    Log.e("SimContacts", "cursor is null. Ignore silently.");
                } else {
                    this.mProgressDialog = new ProgressDialog(this);
                    this.mProgressDialog.setTitle(title);
                    this.mProgressDialog.setMessage(message);
                    this.mProgressDialog.setProgressStyle(1);
                    this.mProgressDialog.setButton(-2, getString(R.string.cancel), thread);
                    this.mProgressDialog.setProgress(0);
                    this.mProgressDialog.setMax(this.mCursor.getCount());
                    this.mProgressDialog.show();
                    thread.start();
                    return true;
                }
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
                if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
                    int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
                    importOneSimContact(position);
                    return true;
                }
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            TextView textView = (TextView) itemInfo.targetView.findViewById(android.R.id.text1);
            if (textView != null) {
                menu.setHeaderTitle(textView.getText());
            }
            menu.add(0, 1, 0, R.string.importSimEntry);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        importOneSimContact(position);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 5:
                if (this.mCursor != null && this.mCursor.moveToPosition(getSelectedItemPosition())) {
                    String phoneNumber = this.mCursor.getString(1);
                    if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
                        return true;
                    }
                    Intent intent = new Intent("android.intent.action.CALL_PRIVILEGED", Uri.fromParts("tel", phoneNumber, null));
                    intent.setFlags(276824064);
                    startActivity(intent);
                    finish();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
