package com.mediatek.contacts.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.activities.RequestImportVCardPermissionsActivity;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.model.account.FallbackAccountType;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.AccountSelectionUtil;
import com.mediatek.contacts.eventhandler.BaseEventHandlerActivity;
import com.mediatek.contacts.list.ContactListMultiChoiceActivity;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.widget.ImportExportItem;
import com.mediatek.storage.StorageManagerEx;
import java.util.ArrayList;
import java.util.List;

public class ContactImportExportActivity extends BaseEventHandlerActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AccountsLoader.AccountsListener {
    private ListView mListView = null;
    private List<AccountWithDataSetEx> mAccounts = null;
    private int mShowingStep = 0;
    private int mCheckedPosition = 0;
    private boolean mIsFirstEntry = true;
    private AccountWithDataSetEx mCheckedAccount1 = null;
    private AccountWithDataSetEx mCheckedAccount2 = null;
    private List<ListViewItemObject> mListItemObjectList = new ArrayList();
    private AccountListAdapter mAdapter = null;
    private String mCallingActivityName = null;
    private String mType = null;
    private boolean mIsFinished = false;

    private boolean isImport() {
        return "Import".equals(this.mType);
    }

    private boolean isExport() {
        return "Export".equals(this.mType);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("ContactImportExportActivity", "[onCreate]");
        if (RequestImportVCardPermissionsActivity.startPermissionActivity(this, isCallerSelf(this))) {
            Log.i("ContactImportExportActivity", "[onCreate]startPermissionActivity,return.");
            return;
        }
        if (MultiChoiceService.isProcessing(2)) {
            Log.i("ContactImportExportActivity", "[onCreate] MultiChoiceService isProcessing delete contacts,stop Create and return");
            setResult(0);
            finish();
            return;
        }
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e("ContactImportExportActivity", "[onCreate] callingActivity has no putExtra");
            finish();
            return;
        }
        this.mCallingActivityName = extras.getString("CALLING_ACTIVITY", null);
        if (this.mCallingActivityName == null) {
            Log.e("ContactImportExportActivity", "[onCreate] callingActivity = null and return");
            finish();
            return;
        }
        this.mType = extras.getString("CALLING_TYPE");
        Log.d("ContactImportExportActivity", "[onCreate]mCallingActivityName=" + this.mCallingActivityName + ", mType=" + this.mType);
        setContentView(R.layout.mtk_import_export_bridge_layout);
        ((Button) findViewById(R.id.btn_action)).setOnClickListener(this);
        ((Button) findViewById(R.id.btn_back)).setOnClickListener(this);
        ((LinearLayout) findViewById(R.id.buttonbar_layout)).setVisibility(8);
        this.mListView = (ListView) findViewById(R.id.list_view);
        this.mListView.setOnItemClickListener(this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(12, 14);
            if (isImport()) {
                actionBar.setTitle(R.string.import_title);
            } else if (isExport()) {
                actionBar.setTitle(R.string.export_title);
            } else {
                actionBar.setTitle(R.string.imexport_title);
            }
        }
        this.mAdapter = new AccountListAdapter(this);
        AccountsLoader.loadAccounts(this, 0, AccountTypeManager.writableFilter());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        setCheckedPosition(i);
        setCheckedAccount(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if (this.mShowingStep > 1) {
            onBackAction();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_action:
            case R.id.btn_back:
                if (view.getId() == R.id.btn_action) {
                    onNextAction();
                } else {
                    onBackAction();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        Log.i("ContactImportExportActivity", "[onActivityResult]requestCode:" + i + ",resultCode:" + i2);
        super.onActivityResult(i, i2, intent);
        if (i == 11111 && i2 == 11112) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        this.mIsFinished = true;
        super.onDestroy();
        Log.i("ContactImportExportActivity", "[onDestroy]");
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> list) {
        List<AccountWithDataSetEx> listExtractAccountsEx = extractAccountsEx(list);
        if (isActivityFinished()) {
            Log.w("ContactImportExportActivity", "[onLoadFinished]isActivityFinished is true,return.");
            return;
        }
        if (listExtractAccountsEx == null) {
            Log.e("ContactImportExportActivity", "[onLoadFinished]data is null,return.");
            return;
        }
        Log.d("ContactImportExportActivity", "[onLoadFinished]data = " + listExtractAccountsEx);
        if (this.mAccounts == null) {
            this.mAccounts = listExtractAccountsEx;
            this.mAccounts.addAll(getStorageAccounts());
            if (this.mAccounts.size() <= 1) {
                Log.i("ContactImportExportActivity", "[onLoadFinished]mAccounts.size = " + this.mAccounts.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ContactImportExportActivity.this.getApplicationContext(), R.string.xport_error_one_account, 0).show();
                    }
                });
                finish();
            }
            Log.i("ContactImportExportActivity", "[onLoadFinished]mAccounts.size() = " + this.mAccounts.size() + ",mAccounts:" + this.mAccounts + ",mShowingStep =" + this.mShowingStep);
            if (this.mShowingStep == 0) {
                setShowingStep(1);
            } else {
                setShowingStep(this.mShowingStep);
            }
            setCheckedAccount(this.mCheckedPosition);
            updateUi();
        }
    }

    private List<AccountWithDataSetEx> extractAccountsEx(List<AccountInfo> list) {
        List<AccountWithDataSet> listExtractAccounts = AccountInfo.extractAccounts(list);
        ArrayList arrayList = new ArrayList();
        for (AccountWithDataSet accountWithDataSet : listExtractAccounts) {
            AccountType accountType = getAccountType(accountWithDataSet.type, accountWithDataSet.dataSet, false);
            Log.d("ContactImportExportActivity", "[loadAccountFilters]account.type = " + accountWithDataSet.type + ",account.name =" + Log.anonymize(accountWithDataSet.name));
            if (accountType.isExtension() && !accountWithDataSet.hasData(this)) {
                Log.d("ContactImportExportActivity", "[loadAccountFilters]continue.");
            } else {
                int invalidSubId = SubInfoUtils.getInvalidSubId();
                if (accountWithDataSet instanceof AccountWithDataSetEx) {
                    invalidSubId = ((AccountWithDataSetEx) accountWithDataSet).getSubId();
                }
                Log.d("ContactImportExportActivity", "[loadAccountFilters]subId = " + invalidSubId);
                arrayList.add(new AccountWithDataSetEx(accountWithDataSet.name, accountWithDataSet.type, invalidSubId));
            }
        }
        return arrayList;
    }

    public void doImportExport() {
        Log.i("ContactImportExportActivity", "[doImportExport]...");
        if (AccountTypeUtils.isAccountTypeIccCard(this.mCheckedAccount1.type)) {
            if (!SimCardUtils.isPhoneBookReady(this.mCheckedAccount1.getSubId())) {
                Toast.makeText(this, R.string.icc_phone_book_invalid, 1).show();
                finish();
                Log.i("ContactImportExportActivity", "[doImportExport] phb is not ready.");
                return;
            }
            handleImportExportAction();
            return;
        }
        handleImportExportAction();
    }

    public List<AccountWithDataSetEx> getStorageAccounts() {
        ArrayList arrayList = new ArrayList();
        StorageManager storageManager = (StorageManager) getApplicationContext().getSystemService("storage");
        if (storageManager == null) {
            Log.w("ContactImportExportActivity", "[getStorageAccounts]storageManager is null!");
            return arrayList;
        }
        if (ContactsPortableUtils.MTK_STORAGE_SUPPORT) {
            try {
                if (!storageManager.getVolumeState(StorageManagerEx.getDefaultPath()).equals("mounted")) {
                    Log.w("ContactImportExportActivity", "[getStorageAccounts]State is  not MEDIA_MOUNTED!");
                    return arrayList;
                }
            } catch (Exception e) {
                Log.e("ContactImportExportActivity", "StorageManagerEx.getDefaultPath native exception!");
                e.printStackTrace();
            }
        }
        StorageVolume[] volumeList = StorageManager.getVolumeList(UserHandle.myUserId(), 256);
        if (volumeList != null) {
            Log.d("ContactImportExportActivity", "[getStorageAccounts]volumes are: " + volumeList);
            for (StorageVolume storageVolume : volumeList) {
                String path = storageVolume.getPath();
                String externalStorageState = Environment.getExternalStorageState(storageVolume.getPathFile());
                Log.d("ContactImportExportActivity", "[getStorageAccounts]path:" + Log.anonymize(path) + ", state=" + externalStorageState);
                if (externalStorageState.equals("mounted")) {
                    arrayList.add(new AccountWithDataSetEx(storageVolume.getDescription(this), "_STORAGE_ACCOUNT", path));
                }
            }
        }
        return arrayList;
    }

    private class ListViewItemObject {
        public AccountWithDataSetEx mAccount;
        public ImportExportItem mView;

        public ListViewItemObject(AccountWithDataSetEx accountWithDataSetEx) {
            this.mAccount = accountWithDataSetEx;
        }

        public String getName() {
            if (this.mAccount == null) {
                Log.w("ContactImportExportActivity", "[getName]mAccount is null!");
                return "null";
            }
            String accountDisplayNameByAccount = AccountFilterUtil.getAccountDisplayNameByAccount(this.mAccount.type, this.mAccount.name);
            Log.d("ContactImportExportActivity", "[getName]type : " + this.mAccount.type + ",name:" + Log.anonymize(this.mAccount.name) + ",displayName:" + Log.anonymize(accountDisplayNameByAccount));
            if (TextUtils.isEmpty(accountDisplayNameByAccount)) {
                if (AccountWithDataSetEx.isLocalPhone(this.mAccount.type)) {
                    return ContactImportExportActivity.this.getString(R.string.account_phone_only);
                }
                return this.mAccount.name;
            }
            return accountDisplayNameByAccount;
        }
    }

    private class AccountListAdapter extends BaseAdapter {
        private Context mContext;
        private final LayoutInflater mLayoutInflater;

        public AccountListAdapter(Context context) {
            this.mContext = context;
            this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        @Override
        public int getCount() {
            return ContactImportExportActivity.this.mListItemObjectList.size();
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public AccountWithDataSetEx getItem(int i) {
            return null;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ImportExportItem importExportItem;
            if (view == null) {
                importExportItem = (ImportExportItem) this.mLayoutInflater.inflate(R.layout.mtk_contact_import_export_item, viewGroup, false);
            } else {
                importExportItem = (ImportExportItem) view;
            }
            ListViewItemObject listViewItemObject = (ListViewItemObject) ContactImportExportActivity.this.mListItemObjectList.get(i);
            listViewItemObject.mView = importExportItem;
            AccountWithDataSetEx accountWithDataSetEx = listViewItemObject.mAccount;
            AccountType accountType = ContactImportExportActivity.this.getAccountType(accountWithDataSetEx.type, accountWithDataSetEx.dataSet, true);
            Drawable displayIcon = null;
            String name = listViewItemObject.getName();
            int subId = listViewItemObject.mAccount.getSubId();
            Log.d("ContactImportExportActivity", "[getView] accounttype: " + accountType);
            if (accountType != null && accountType.isIccCardAccount()) {
                displayIcon = accountType.getDisplayIconBySubId(this.mContext, subId);
                name = (String) accountType.getDisplayLabel(this.mContext);
            } else if (accountType != null) {
                displayIcon = accountType.getDisplayIcon(this.mContext);
            }
            importExportItem.bindView(displayIcon, name, accountWithDataSetEx.dataSet);
            importExportItem.setActivated(ContactImportExportActivity.this.mCheckedPosition == i);
            return importExportItem;
        }
    }

    private void onBackAction() {
        setShowingStep(1);
        int checkedAccountPosition = getCheckedAccountPosition(this.mCheckedAccount1);
        Log.d("ContactImportExportActivity", "[onBackAction] mCheckedAccount1 =" + this.mCheckedAccount1 + ",pos = " + checkedAccountPosition);
        this.mCheckedPosition = checkedAccountPosition;
        setCheckedAccount(this.mCheckedPosition);
        updateUi();
    }

    private void onNextAction() {
        int checkedAccountPosition;
        Log.d("ContactImportExportActivity", "[onNextAction] mShowingStep = " + this.mShowingStep);
        if (this.mShowingStep >= 2) {
            doImportExport();
            return;
        }
        setShowingStep(2);
        if (!this.mIsFirstEntry && (this.mCheckedAccount1 != null || this.mCheckedAccount2 != null)) {
            checkedAccountPosition = getCheckedAccountPosition(this.mCheckedAccount2);
        } else {
            checkedAccountPosition = 0;
        }
        this.mIsFirstEntry = false;
        this.mCheckedPosition = checkedAccountPosition;
        setCheckedAccount(this.mCheckedPosition);
        updateUi();
    }

    private void updateUi() {
        setButtonState(true);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
    }

    private int getCheckedAccountPosition(AccountWithDataSetEx accountWithDataSetEx) {
        for (int i = 0; i < this.mListItemObjectList.size(); i++) {
            if (this.mListItemObjectList.get(i).mAccount.equals(accountWithDataSetEx)) {
                return i;
            }
        }
        return 0;
    }

    private void handleImportExportAction() {
        Log.d("ContactImportExportActivity", "[handleImportExportAction]...");
        if ((isStorageAccount(this.mCheckedAccount1) && !checkSDCardAvaliable(this.mCheckedAccount1.dataSet)) || (isStorageAccount(this.mCheckedAccount2) && !checkSDCardAvaliable(this.mCheckedAccount2.dataSet))) {
            new AlertDialog.Builder(this).setMessage(R.string.no_sdcard_message).setTitle(R.string.no_sdcard_title).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ContactImportExportActivity.this.finish();
                }
            }).show();
            return;
        }
        if (isStorageAccount(this.mCheckedAccount1)) {
            if (this.mCheckedAccount2 != null) {
                AccountSelectionUtil.doImportFromSdCard(this, this.mCheckedAccount1.dataSet, this.mCheckedAccount2);
            }
        } else {
            if (isStorageAccount(this.mCheckedAccount2)) {
                if (isSDCardFull(this.mCheckedAccount2.dataSet)) {
                    Log.i("ContactImportExportActivity", "[handleImportExportAction] isSDCardFull");
                    new AlertDialog.Builder(this).setMessage(R.string.storage_full).setTitle(R.string.storage_full).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ContactImportExportActivity.this.finish();
                        }
                    }).show();
                    return;
                } else {
                    startActivityForResult(new Intent(this, (Class<?>) ContactListMultiChoiceActivity.class).setAction("mediatek.intent.action.contacts.list.PICKMULTICONTACTS").putExtra("request_type", 1).putExtra("fromaccount", this.mCheckedAccount1).putExtra("toaccount", this.mCheckedAccount2).putExtra("CALLING_ACTIVITY", this.mCallingActivityName), 11111);
                    return;
                }
            }
            startActivityForResult(new Intent(this, (Class<?>) ContactListMultiChoiceActivity.class).setAction("mediatek.intent.action.contacts.list.PICKMULTICONTACTS").putExtra("request_type", 1).putExtra("fromaccount", this.mCheckedAccount1).putExtra("toaccount", this.mCheckedAccount2).putExtra("CALLING_ACTIVITY", this.mCallingActivityName), 11111);
        }
    }

    private boolean checkSDCardAvaliable(String str) {
        if (TextUtils.isEmpty(str)) {
            Log.w("ContactImportExportActivity", "[checkSDCardAvaliable]path is null!");
            return false;
        }
        StorageManager storageManager = (StorageManager) getSystemService("storage");
        if (storageManager == null) {
            Log.i("ContactImportExportActivity", "[checkSDCardAvaliable] story manager is null");
            return false;
        }
        String volumeState = storageManager.getVolumeState(str);
        Log.d("ContactImportExportActivity", "[checkSDCardAvaliable]path = " + Log.anonymize(str) + ",storageState = " + volumeState);
        return volumeState.equals("mounted");
    }

    private boolean isSDCardFull(String str) {
        if (TextUtils.isEmpty(str)) {
            Log.w("ContactImportExportActivity", "[isSDCardFull]path is null!");
            return false;
        }
        Log.d("ContactImportExportActivity", "[isSDCardFull] storage path is " + Log.anonymize(str));
        if (!checkSDCardAvaliable(str)) {
            return true;
        }
        try {
            return ((long) new StatFs(str).getAvailableBlocks()) <= 0;
        } catch (IllegalArgumentException e) {
            Log.e("ContactImportExportActivity", "[isSDCardFull]catch exception:");
            e.printStackTrace();
            return false;
        }
    }

    private void setButtonState(boolean z) {
        int i;
        View viewFindViewById = findViewById(R.id.btn_back);
        boolean z2 = false;
        if (z && this.mShowingStep > 1) {
            i = 0;
        } else {
            i = 8;
        }
        viewFindViewById.setVisibility(i);
        View viewFindViewById2 = findViewById(R.id.btn_action);
        if (z && this.mShowingStep > 0) {
            z2 = true;
        }
        viewFindViewById2.setEnabled(z2);
    }

    private void setShowingStep(int i) {
        this.mShowingStep = i;
        this.mListItemObjectList.clear();
        int i2 = 0;
        ((LinearLayout) findViewById(R.id.buttonbar_layout)).setVisibility(0);
        Log.d("ContactImportExportActivity", "[setShowingStep]mShowingStep = " + this.mShowingStep);
        if (this.mShowingStep == 1) {
            ((TextView) findViewById(R.id.tips)).setText(R.string.tips_source);
            for (AccountWithDataSetEx accountWithDataSetEx : this.mAccounts) {
                if (isImport()) {
                    if (!isStorageAccount(accountWithDataSetEx)) {
                        i2++;
                    }
                } else if (isExport()) {
                    AccountType accountType = getAccountType(accountWithDataSetEx.type, accountWithDataSetEx.dataSet, true);
                    if (isStorageAccount(accountWithDataSetEx) || accountType.isIccCardAccount()) {
                    }
                }
                this.mListItemObjectList.add(new ListViewItemObject(accountWithDataSetEx));
            }
            if (isImport() && 1 == i2) {
                for (ListViewItemObject listViewItemObject : this.mListItemObjectList) {
                    if (!isStorageAccount(listViewItemObject.mAccount)) {
                        this.mListItemObjectList.remove(listViewItemObject);
                        return;
                    }
                }
                return;
            }
            return;
        }
        if (this.mShowingStep == 2) {
            ((TextView) findViewById(R.id.tips)).setText(R.string.tips_target);
            for (AccountWithDataSetEx accountWithDataSetEx2 : this.mAccounts) {
                if (!this.mCheckedAccount1.equals(accountWithDataSetEx2)) {
                    AccountType accountType2 = getAccountType(accountWithDataSetEx2.type, accountWithDataSetEx2.dataSet, true);
                    AccountType accountType3 = getAccountType(this.mCheckedAccount1.type, this.mCheckedAccount1.dataSet, true);
                    Log.d("ContactImportExportActivity", "[setShowingStep]accountType: " + accountType2 + ", checkedAccountType: " + accountType3);
                    if (!isStorageAccount(this.mCheckedAccount1) || !accountType2.isIccCardAccount()) {
                        if (!accountType3.isIccCardAccount() || !isStorageAccount(accountWithDataSetEx2)) {
                            if (!isStorageAccount(this.mCheckedAccount1) || !isStorageAccount(accountWithDataSetEx2)) {
                                if (isImport()) {
                                    if (!isStorageAccount(accountWithDataSetEx2)) {
                                        this.mListItemObjectList.add(new ListViewItemObject(accountWithDataSetEx2));
                                    }
                                } else if (!isExport() || isStorageAccount(accountWithDataSetEx2)) {
                                    this.mListItemObjectList.add(new ListViewItemObject(accountWithDataSetEx2));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private AccountType getAccountType(String str, String str2, boolean z) {
        AccountType accountType = AccountTypeManager.getInstance(this).getAccountType(str, str2);
        if (accountType == null && z && "_STORAGE_ACCOUNT".equalsIgnoreCase(str)) {
            return new FallbackAccountType(this);
        }
        return accountType;
    }

    private static boolean isStorageAccount(AccountWithDataSetEx accountWithDataSetEx) {
        if (accountWithDataSetEx != null) {
            return "_STORAGE_ACCOUNT".equalsIgnoreCase(accountWithDataSetEx.type);
        }
        return false;
    }

    private void setCheckedPosition(int i) {
        if (this.mCheckedPosition != i) {
            setListViewItemChecked(this.mCheckedPosition, false);
            this.mCheckedPosition = i;
            setListViewItemChecked(this.mCheckedPosition, true);
        }
    }

    private void setCheckedAccount(int i) {
        if (this.mListItemObjectList.size() == 0) {
            Log.e("ContactImportExportActivity", "[setCheckedAccount]mListItemObjectList.size() == 0");
            finish();
            return;
        }
        if (this.mShowingStep == 1) {
            this.mCheckedAccount1 = this.mListItemObjectList.get(i).mAccount;
        } else if (this.mShowingStep == 2) {
            this.mCheckedAccount2 = this.mListItemObjectList.get(i).mAccount;
        }
        Log.d("ContactImportExportActivity", "[setCheckedAccount]mCheckedAccount1 = " + this.mCheckedAccount1 + ",mCheckedAccount2 =" + this.mCheckedAccount2 + ",pos = " + i);
    }

    private void setListViewItemChecked(int i, boolean z) {
        if (i > -1) {
            ListViewItemObject listViewItemObject = this.mListItemObjectList.get(i);
            if (listViewItemObject.mView != null) {
                listViewItemObject.mView.setActivated(z);
            }
        }
    }

    private boolean isActivityFinished() {
        return this.mIsFinished;
    }

    private static boolean isCallerSelf(Activity activity) {
        String packageName;
        ComponentName callingActivity = activity.getCallingActivity();
        if (callingActivity == null || (packageName = callingActivity.getPackageName()) == null) {
            return false;
        }
        return packageName.equals(activity.getApplicationContext().getPackageName());
    }
}
