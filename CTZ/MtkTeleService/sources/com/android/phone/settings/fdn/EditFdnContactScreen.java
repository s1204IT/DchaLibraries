package com.android.phone.settings.fdn;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.mediatek.settings.CallSettingUtils;

public class EditFdnContactScreen extends Activity implements PhoneGlobals.SubInfoUpdateListener {
    private boolean mAddContact;
    private Button mButton;
    private boolean mDataBusy;
    private String mName;
    private EditText mNameField;
    private String mNumber;
    private EditText mNumberField;
    private String mPin2;
    private LinearLayout mPinFieldContainer;
    private QueryHandler mQueryHandler;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private static final String[] NUM_PROJECTION = {"display_name", "data1"};
    private static final Intent CONTACT_IMPORT_INTENT = new Intent("android.intent.action.GET_CONTENT");
    private Handler mHandler = new Handler();
    private final View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (EditFdnContactScreen.this.mPinFieldContainer.getVisibility() == 0) {
                if (view == EditFdnContactScreen.this.mNameField) {
                    EditFdnContactScreen.this.mNumberField.requestFocus();
                    return;
                }
                if (view == EditFdnContactScreen.this.mNumberField) {
                    EditFdnContactScreen.this.mButton.requestFocus();
                    return;
                }
                if (view == EditFdnContactScreen.this.mButton) {
                    if (!EditFdnContactScreen.this.isValidNumber(PhoneNumberUtils.stripSeparators(EditFdnContactScreen.this.getNumberFromTextField()))) {
                        EditFdnContactScreen.this.handleResult(false, true);
                    } else if (!EditFdnContactScreen.this.mDataBusy) {
                        if (!EditFdnContactScreen.this.needTipsPIN2Blocked()) {
                            EditFdnContactScreen.this.authenticatePin2();
                        } else {
                            EditFdnContactScreen.this.log("[onClick] PIN2 Locked!!");
                        }
                    }
                }
            }
        }
    };
    private final View.OnFocusChangeListener mOnFocusChangeHandler = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean z) {
            if (z) {
                Selection.selectAll((Spannable) ((TextView) view).getText());
            }
        }
    };
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            EditFdnContactScreen.this.setButtonEnabled();
        }
    };

    static {
        CONTACT_IMPORT_INTENT.setType("vnd.android.cursor.item/phone_v2");
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        resolveIntent();
        getWindow().requestFeature(5);
        setContentView(R.layout.edit_fdn_contact_screen);
        setupView();
        setTitle(this.mAddContact ? R.string.add_fdn_contact : R.string.edit_fdn_contact);
        displayProgress(false);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) throws Throwable {
        Cursor cursorQuery;
        log("onActivityResult request:" + i + " result:" + i2);
        if (i == 100) {
            Bundle extras = intent != null ? intent.getExtras() : null;
            if (extras == null) {
                if (i2 != -1) {
                    log("onActivityResult: cancelled.");
                    finish();
                    return;
                }
                return;
            }
            this.mPin2 = extras.getString("pin2");
            if (this.mAddContact) {
                addContact();
                return;
            } else {
                updateContact();
                return;
            }
        }
        if (i != 200) {
            return;
        }
        if (i2 != -1) {
            log("onActivityResult: cancelled.");
            return;
        }
        try {
            cursorQuery = getContentResolver().query(intent.getData(), NUM_PROJECTION, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        this.mNameField.setText(cursorQuery.getString(0));
                        this.mNumberField.setText(cursorQuery.getString(1));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            return;
                        }
                        return;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            Log.w(PhoneGlobals.LOG_TAG, "onActivityResult: bad contact data, no results found.");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Resources resources = getResources();
        menu.add(0, 1, 0, resources.getString(R.string.importToFDNfromContacts)).setIcon(R.drawable.ic_menu_contact);
        menu.add(0, 2, 0, resources.getString(R.string.menu_delete)).setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean zOnPrepareOptionsMenu = super.onPrepareOptionsMenu(menu);
        if (this.mDataBusy) {
            return false;
        }
        return zOnPrepareOptionsMenu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Log.d(PhoneGlobals.LOG_TAG, "[onOptionsItemSelected]item text = " + ((Object) menuItem.getTitle()));
        int itemId = menuItem.getItemId();
        if (itemId != 16908332) {
            switch (itemId) {
                case 1:
                    startActivityForResult(CONTACT_IMPORT_INTENT, 200);
                    return true;
                case 2:
                    if (!needTipsPIN2Blocked()) {
                        deleteSelected();
                    }
                    return true;
                default:
                    return super.onOptionsItemSelected(menuItem);
            }
        }
        onBackPressed();
        return true;
    }

    private void resolveIntent() {
        Intent intent = getIntent();
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, intent);
        this.mName = intent.getStringExtra("name");
        this.mNumber = intent.getStringExtra("number");
        this.mAddContact = intent.getBooleanExtra("addContact", false);
    }

    private void setupView() {
        this.mNameField = (EditText) findViewById(R.id.fdn_name);
        if (this.mNameField != null) {
            this.mNameField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
            this.mNameField.setOnClickListener(this.mClicked);
            this.mNameField.addTextChangedListener(this.mTextWatcher);
        }
        this.mNumberField = (EditText) findViewById(R.id.fdn_number);
        if (this.mNumberField != null) {
            this.mNumberField.setTextDirection(3);
            this.mNumberField.setKeyListener(DialerKeyListener.getInstance());
            this.mNumberField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
            this.mNumberField.setOnClickListener(this.mClicked);
            this.mNumberField.addTextChangedListener(this.mTextWatcher);
        }
        if (!this.mAddContact) {
            if (this.mNameField != null) {
                this.mNameField.setText(this.mName);
            }
            if (this.mNumberField != null) {
                this.mNumberField.setText(this.mNumber);
            }
        }
        this.mButton = (Button) findViewById(R.id.button);
        if (this.mButton != null) {
            this.mButton.setOnClickListener(this.mClicked);
            setButtonEnabled();
        }
        this.mPinFieldContainer = (LinearLayout) findViewById(R.id.pinc);
    }

    private String getNameFromTextField() {
        return this.mNameField.getText().toString();
    }

    private String getNumberFromTextField() {
        return this.mNumberField.getText().toString();
    }

    private void setButtonEnabled() {
        if (this.mButton != null && this.mNameField != null && this.mNumberField != null) {
            this.mButton.setEnabled(this.mNameField.length() > 0 && this.mNumberField.length() > 0);
        }
    }

    private boolean isValidNumber(String str) {
        return str.length() > 0;
    }

    private void addContact() {
        log("addContact");
        String strStripSeparators = PhoneNumberUtils.stripSeparators(getNumberFromTextField());
        if (!isValidNumber(strStripSeparators)) {
            handleResult(false, true);
            return;
        }
        Uri contentUri = FdnList.getContentUri(this.mSubscriptionInfoHelper);
        ContentValues contentValues = new ContentValues(3);
        contentValues.put("tag", getNameFromTextField());
        contentValues.put("number", strStripSeparators);
        contentValues.put("pin2", this.mPin2);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.mQueryHandler.startInsert(0, null, contentUri, contentValues);
        displayProgress(true);
        showStatus(getResources().getText(R.string.adding_fdn_contact));
    }

    private void updateContact() {
        log("updateContact");
        String nameFromTextField = getNameFromTextField();
        String strStripSeparators = PhoneNumberUtils.stripSeparators(getNumberFromTextField());
        if (!isValidNumber(strStripSeparators)) {
            handleResult(false, true);
            return;
        }
        Uri contentUri = FdnList.getContentUri(this.mSubscriptionInfoHelper);
        ContentValues contentValues = new ContentValues();
        contentValues.put("tag", this.mName);
        contentValues.put("number", this.mNumber);
        contentValues.put("newTag", nameFromTextField);
        contentValues.put("newNumber", strStripSeparators);
        contentValues.put("pin2", this.mPin2);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.mQueryHandler.startUpdate(0, null, contentUri, contentValues, null, null);
        displayProgress(true);
        showStatus(getResources().getText(R.string.updating_fdn_contact));
    }

    private void deleteSelected() {
        if (!this.mAddContact) {
            Intent intent = this.mSubscriptionInfoHelper.getIntent(DeleteFdnContactScreen.class);
            intent.putExtra("name", this.mName);
            intent.putExtra("number", this.mNumber);
            startActivity(intent);
        }
        finish();
    }

    private void authenticatePin2() {
        Intent intent = new Intent();
        intent.setClass(this, GetPin2Screen.class);
        intent.setData(FdnList.getContentUri(this.mSubscriptionInfoHelper));
        intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mSubscriptionInfoHelper.getSubId());
        startActivityForResult(intent, 100);
    }

    private void displayProgress(boolean z) {
        this.mDataBusy = z;
        getWindow().setFeatureInt(5, this.mDataBusy ? -1 : -2);
        this.mButton.setClickable(!this.mDataBusy);
    }

    private void showStatus(CharSequence charSequence) {
        if (charSequence != null) {
            Toast.makeText(this, charSequence, 1).show();
        }
    }

    private void handleResult(boolean z, boolean z2) {
        if (z) {
            log("handleResult: success!");
            showStatus(getResources().getText(this.mAddContact ? R.string.fdn_contact_added : R.string.fdn_contact_updated));
        } else {
            log("handleResult: failed!");
            if (z2) {
                showStatus(getResources().getText(R.string.fdn_contact_number_invalid));
            } else if (this.mSubscriptionInfoHelper.getPhone().getIccCard().getIccPin2Blocked()) {
                showStatus(getResources().getText(R.string.fdn_enable_puk2_requested));
            } else if (this.mSubscriptionInfoHelper.getPhone().getIccCard().getIccPuk2Blocked()) {
                showStatus(getResources().getText(R.string.puk2_blocked));
            } else {
                showStatus(getResources().getText(R.string.pin2_or_fdn_invalid));
            }
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EditFdnContactScreen.this.finish();
            }
        }, 2000L);
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
        }

        @Override
        protected void onInsertComplete(int i, Object obj, Uri uri) {
            EditFdnContactScreen.this.log("onInsertComplete");
            EditFdnContactScreen.this.displayProgress(false);
            EditFdnContactScreen.this.handleResult(EditFdnContactScreen.this.getInsertResult(uri) > 0, false);
        }

        @Override
        protected void onUpdateComplete(int i, Object obj, int i2) {
            EditFdnContactScreen.this.log("onUpdateComplete");
            EditFdnContactScreen.this.displayProgress(false);
            EditFdnContactScreen.this.handleResult(i2 > 0, false);
        }

        @Override
        protected void onDeleteComplete(int i, Object obj, int i2) {
        }
    }

    private void log(String str) {
        Log.d(PhoneGlobals.LOG_TAG, "[EditFdnContact] " + str);
    }

    private int getInsertResult(Uri uri) {
        int iIntValue;
        if (uri == null) {
            log("[getInsertResult]  uri == null.");
            return 0;
        }
        log("[getInsertResult]  uri.toString() = " + uri.toString());
        String string = uri.toString();
        if (string.indexOf("error") == -1) {
            iIntValue = 1;
        } else {
            iIntValue = Integer.valueOf(string.replace("content://icc/error/", "")).intValue();
        }
        log("[getInsertResult] result=" + iIntValue);
        return iIntValue;
    }

    private boolean needTipsPIN2Blocked() {
        if (CallSettingUtils.getPin2RetryNumber(this.mSubscriptionInfoHelper.getSubId()) != 0) {
            return false;
        }
        log("[onClick] retry number is 0, tips...");
        Toast.makeText(this, getString(R.string.fdn_puk_need_tips), 0).show();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
