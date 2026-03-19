package com.android.phone;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import android.widget.Toast;
import java.util.ArrayList;

public class SimContacts extends ADNList {
    private static final String LOG_TAG = "SimContacts";
    private static final int MENU_IMPORT_ALL = 2;
    private static final int MENU_IMPORT_ONE = 1;
    static final ContentValues sEmptyContentValues = new ContentValues();
    private Account mAccount;
    private ProgressDialog mProgressDialog;

    private static class NamePhoneTypePair {
        final String name;
        final int phoneType;

        public NamePhoneTypePair(String str) {
            int length = str.length();
            int i = length - 2;
            if (i >= 0 && str.charAt(i) == '/') {
                char upperCase = Character.toUpperCase(str.charAt(length - 1));
                if (upperCase == 'W') {
                    this.phoneType = 3;
                } else if (upperCase == 'M' || upperCase == 'O') {
                    this.phoneType = 2;
                } else if (upperCase == 'H') {
                    this.phoneType = 1;
                } else {
                    this.phoneType = 7;
                }
                this.name = str.substring(0, i);
                return;
            }
            this.phoneType = 7;
            this.name = str;
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
            ContentResolver contentResolver = SimContacts.this.getContentResolver();
            SimContacts.this.mCursor.moveToPosition(-1);
            while (!this.mCanceled && SimContacts.this.mCursor.moveToNext()) {
                SimContacts.actuallyImportOneSimContact(SimContacts.this.mCursor, contentResolver, SimContacts.this.mAccount);
                SimContacts.this.mProgressDialog.incrementProgressBy(1);
            }
            SimContacts.this.mProgressDialog.dismiss();
            SimContacts.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            this.mCanceled = true;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                this.mCanceled = true;
                SimContacts.this.mProgressDialog.dismiss();
            } else {
                Log.e(SimContacts.LOG_TAG, "Unknown button event has come: " + dialogInterface.toString());
            }
        }
    }

    private static boolean actuallyImportOneSimContact(Cursor cursor, ContentResolver contentResolver, Account account) {
        String[] strArrSplit;
        NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(cursor.getString(1));
        String str = namePhoneTypePair.name;
        int i = namePhoneTypePair.phoneType;
        String string = cursor.getString(2);
        String string2 = cursor.getString(3);
        if (!TextUtils.isEmpty(string2)) {
            strArrSplit = string2.split(",");
        } else {
            strArrSplit = null;
        }
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        if (account != null) {
            builderNewInsert.withValue("account_name", account.name);
            builderNewInsert.withValue("account_type", account.type);
        } else {
            builderNewInsert.withValues(sEmptyContentValues);
        }
        arrayList.add(builderNewInsert.build());
        ContentProviderOperation.Builder builderNewInsert2 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builderNewInsert2.withValueBackReference("raw_contact_id", 0);
        builderNewInsert2.withValue("mimetype", "vnd.android.cursor.item/name");
        builderNewInsert2.withValue("data1", str);
        arrayList.add(builderNewInsert2.build());
        ContentProviderOperation.Builder builderNewInsert3 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builderNewInsert3.withValueBackReference("raw_contact_id", 0);
        builderNewInsert3.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
        builderNewInsert3.withValue("data2", Integer.valueOf(i));
        builderNewInsert3.withValue("data1", string);
        builderNewInsert3.withValue("is_primary", 1);
        arrayList.add(builderNewInsert3.build());
        if (string2 != null) {
            for (String str2 : strArrSplit) {
                ContentProviderOperation.Builder builderNewInsert4 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builderNewInsert4.withValueBackReference("raw_contact_id", 0);
                builderNewInsert4.withValue("mimetype", "vnd.android.cursor.item/email_v2");
                builderNewInsert4.withValue("data2", 4);
                builderNewInsert4.withValue("data1", str2);
                arrayList.add(builderNewInsert4.build());
            }
        }
        try {
            return contentResolver.applyBatch("com.android.contacts", arrayList).length > 0;
        } catch (OperationApplicationException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            return false;
        } catch (RemoteException e2) {
            Log.e(LOG_TAG, String.format("%s: %s", e2.toString(), e2.getMessage()));
            return false;
        }
    }

    private void importOneSimContact(int i) {
        ContentResolver contentResolver = getContentResolver();
        Context applicationContext = getApplicationContext();
        if (this.mCursor.moveToPosition(i)) {
            if (actuallyImportOneSimContact(this.mCursor, contentResolver, this.mAccount)) {
                Toast.makeText(applicationContext, R.string.singleContactImportedMsg, 0).show();
                return;
            } else {
                Toast.makeText(applicationContext, R.string.failedToImportSingleContactMsg, 0).show();
                return;
            }
        }
        Log.e(LOG_TAG, "Failed to move the cursor to the position \"" + i + "\"");
        Toast.makeText(applicationContext, R.string.failedToImportSingleContactMsg, 0).show();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent != null) {
            String stringExtra = intent.getStringExtra("account_name");
            String stringExtra2 = intent.getStringExtra("account_type");
            if (!TextUtils.isEmpty(stringExtra) && !TextUtils.isEmpty(stringExtra2)) {
                this.mAccount = new Account(stringExtra, stringExtra2);
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
        int intExtra;
        Intent intent = getIntent();
        if (intent.hasExtra(PhoneGlobals.EXTRA_SUBSCRIPTION_ID)) {
            intExtra = intent.getIntExtra(PhoneGlobals.EXTRA_SUBSCRIPTION_ID, -1);
        } else {
            intExtra = -1;
        }
        if (intExtra != -1) {
            intent.setData(Uri.parse(ADNList.ICC_ADN_SUBID_URI + intExtra));
        } else {
            intent.setData(Uri.parse(ADNList.ICC_ADN_URI));
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
        MenuItem menuItemFindItem = menu.findItem(2);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(this.mCursor != null && this.mCursor.getCount() > 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 2) {
            String string = getString(R.string.importAllSimEntries);
            String string2 = getString(R.string.importingSimContacts);
            ImportAllSimContactsThread importAllSimContactsThread = new ImportAllSimContactsThread();
            if (this.mCursor == null) {
                Log.e(LOG_TAG, "cursor is null. Ignore silently.");
            } else {
                this.mProgressDialog = new ProgressDialog(this);
                this.mProgressDialog.setTitle(string);
                this.mProgressDialog.setMessage(string2);
                this.mProgressDialog.setProgressStyle(1);
                this.mProgressDialog.setButton(-2, getString(R.string.cancel), importAllSimContactsThread);
                this.mProgressDialog.setProgress(0);
                this.mProgressDialog.setMax(this.mCursor.getCount());
                this.mProgressDialog.show();
                importAllSimContactsThread.start();
                return true;
            }
        } else if (itemId == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            ContextMenu.ContextMenuInfo menuInfo = menuItem.getMenuInfo();
            if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
                importOneSimContact(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
                return true;
            }
        }
        return super.onContextItemSelected(menuItem);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (contextMenuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            TextView textView = (TextView) ((AdapterView.AdapterContextMenuInfo) contextMenuInfo).targetView.findViewById(android.R.id.text1);
            if (textView != null) {
                contextMenu.setHeaderTitle(textView.getText());
            }
            contextMenu.add(0, 1, 0, R.string.importSimEntry);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int i, long j) {
        importOneSimContact(i);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 5 && this.mCursor != null && this.mCursor.moveToPosition(getSelectedItemPosition())) {
            String string = this.mCursor.getString(2);
            if (string == null || !TextUtils.isGraphic(string)) {
                return true;
            }
            Intent intent = new Intent("android.intent.action.CALL_PRIVILEGED", Uri.fromParts("tel", string, null));
            intent.setFlags(276824064);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }
}
