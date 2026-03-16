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
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
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
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;

public class EditFdnContactScreen extends Activity {
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
        public void onClick(View v) {
            if (EditFdnContactScreen.this.mPinFieldContainer.getVisibility() == 0) {
                if (v == EditFdnContactScreen.this.mNameField) {
                    EditFdnContactScreen.this.mNumberField.requestFocus();
                    return;
                }
                if (v == EditFdnContactScreen.this.mNumberField) {
                    EditFdnContactScreen.this.mButton.requestFocus();
                } else if (v == EditFdnContactScreen.this.mButton && !EditFdnContactScreen.this.mDataBusy) {
                    EditFdnContactScreen.this.authenticatePin2();
                }
            }
        }
    };
    private final View.OnFocusChangeListener mOnFocusChangeHandler = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                TextView textView = (TextView) v;
                Selection.selectAll((Spannable) textView.getText());
            }
        }
    };

    static {
        CONTACT_IMPORT_INTENT.setType("vnd.android.cursor.item/phone_v2");
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        resolveIntent();
        getWindow().requestFeature(5);
        setContentView(R.layout.edit_fdn_contact_screen);
        setupView();
        setTitle(this.mAddContact ? R.string.add_fdn_contact : R.string.edit_fdn_contact);
        displayProgress(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case 100:
                Bundle extras = intent != null ? intent.getExtras() : null;
                if (extras != null) {
                    this.mPin2 = extras.getString("pin2");
                    if (this.mAddContact) {
                        addContact();
                        return;
                    } else {
                        updateContact();
                        return;
                    }
                }
                if (resultCode != -1) {
                    finish();
                    return;
                }
                return;
            case 200:
                if (resultCode == -1) {
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(intent.getData(), NUM_PROJECTION, null, null, null);
                        if (cursor == null || !cursor.moveToFirst()) {
                            Log.w("PhoneApp", "onActivityResult: bad contact data, no results found.");
                        } else {
                            this.mNameField.setText(cursor.getString(0));
                            this.mNumberField.setText(cursor.getString(1));
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                        return;
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
                return;
            default:
                return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Resources r = getResources();
        menu.add(0, 1, 0, r.getString(R.string.importToFDNfromContacts)).setIcon(R.drawable.ic_menu_contact);
        menu.add(0, 2, 0, r.getString(R.string.menu_delete)).setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        if (this.mDataBusy) {
            return false;
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                startActivityForResult(CONTACT_IMPORT_INTENT, 200);
                return true;
            case 2:
                deleteSelected();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void resolveIntent() {
        Intent intent = getIntent();
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, intent);
        this.mName = intent.getStringExtra("name");
        this.mNumber = intent.getStringExtra("number");
        this.mAddContact = TextUtils.isEmpty(this.mNumber);
    }

    private void setupView() {
        this.mNameField = (EditText) findViewById(R.id.fdn_name);
        if (this.mNameField != null) {
            this.mNameField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
            this.mNameField.setOnClickListener(this.mClicked);
        }
        this.mNumberField = (EditText) findViewById(R.id.fdn_number);
        if (this.mNumberField != null) {
            this.mNumberField.setKeyListener(DialerKeyListener.getInstance());
            this.mNumberField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
            this.mNumberField.setOnClickListener(this.mClicked);
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
        }
        this.mPinFieldContainer = (LinearLayout) findViewById(R.id.pinc);
    }

    private String getNameFromTextField() {
        return this.mNameField.getText().toString();
    }

    private String getNumberFromTextField() {
        return this.mNumberField.getText().toString();
    }

    private boolean isValidNumber(String number) {
        return number.length() <= 40;
    }

    private void addContact() {
        String number = PhoneNumberUtils.convertAndStrip(getNumberFromTextField());
        if (!isValidNumber(number)) {
            handleResult(false, true);
            return;
        }
        Uri uri = FdnList.getContentUri(this.mSubscriptionInfoHelper);
        ContentValues bundle = new ContentValues(3);
        bundle.put("tag", getNameFromTextField());
        bundle.put("number", number);
        bundle.put("pin2", this.mPin2);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.mQueryHandler.startInsert(0, null, uri, bundle);
        displayProgress(true);
        showStatus(getResources().getText(R.string.adding_fdn_contact));
    }

    private void updateContact() {
        String name = getNameFromTextField();
        String number = PhoneNumberUtils.convertAndStrip(getNumberFromTextField());
        if (!isValidNumber(number)) {
            handleResult(false, true);
            return;
        }
        Uri uri = FdnList.getContentUri(this.mSubscriptionInfoHelper);
        ContentValues bundle = new ContentValues();
        bundle.put("tag", this.mName);
        bundle.put("number", this.mNumber);
        bundle.put("newTag", name);
        bundle.put("newNumber", number);
        bundle.put("pin2", this.mPin2);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.mQueryHandler.startUpdate(0, null, uri, bundle, null, null);
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
        startActivityForResult(intent, 100);
    }

    private void displayProgress(boolean flag) {
        this.mDataBusy = flag;
        getWindow().setFeatureInt(5, this.mDataBusy ? -1 : -2);
        this.mButton.setClickable(!this.mDataBusy);
    }

    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            Toast.makeText(this, statusMsg, 1).show();
        }
    }

    private void handleResult(boolean success, boolean invalidNumber) {
        if (success) {
            showStatus(getResources().getText(this.mAddContact ? R.string.fdn_contact_added : R.string.fdn_contact_updated));
        } else if (invalidNumber) {
            showStatus(getResources().getText(R.string.fdn_invalid_number));
        } else if (PhoneFactory.getDefaultPhone().getIccCard().getIccPin2Blocked()) {
            showStatus(getResources().getText(R.string.fdn_enable_puk2_requested));
        } else if (PhoneFactory.getDefaultPhone().getIccCard().getIccPuk2Blocked()) {
            showStatus(getResources().getText(R.string.puk2_blocked));
        } else {
            showStatus(getResources().getText(R.string.pin2_or_fdn_invalid));
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EditFdnContactScreen.this.finish();
            }
        }, 2000L);
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            EditFdnContactScreen.this.displayProgress(false);
            EditFdnContactScreen.this.handleResult(uri != null, false);
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            EditFdnContactScreen.this.displayProgress(false);
            EditFdnContactScreen.this.handleResult(result > 0, false);
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
        }
    }
}
