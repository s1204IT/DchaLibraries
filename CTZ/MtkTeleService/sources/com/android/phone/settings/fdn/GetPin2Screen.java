package com.android.phone.settings.fdn;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.mediatek.settings.CallSettingUtils;

public class GetPin2Screen extends Activity implements TextView.OnEditorActionListener, PhoneGlobals.SubInfoUpdateListener {
    private final View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (CallSettingUtils.getPin2RetryNumber(GetPin2Screen.this.mSubId) == 0) {
                GetPin2Screen.this.showStatus(GetPin2Screen.this.getString(R.string.fdn_puk_need_tips));
            } else if (!GetPin2Screen.this.validatePin(GetPin2Screen.this.mPin2Field.getText())) {
                GetPin2Screen.this.showStatus(GetPin2Screen.this.getString(R.string.invalidPin2));
            } else if (!TextUtils.isEmpty(GetPin2Screen.this.mPin2Field.getText())) {
                GetPin2Screen.this.returnResult();
            }
        }
    };
    private TextView mEnterPin2TitleTips;
    private Button mOkButton;
    private EditText mPin2Field;
    private int mSubId;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.get_pin2_screen);
        this.mPin2Field = (EditText) findViewById(R.id.pin);
        this.mPin2Field.setKeyListener(DigitsKeyListener.getInstance());
        this.mPin2Field.setOnEditorActionListener(this);
        this.mPin2Field.setInputType(18);
        this.mOkButton = (Button) findViewById(R.id.ok);
        this.mOkButton.setOnClickListener(this.mClicked);
        onCreateMtk();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private String getPin2() {
        return this.mPin2Field.getText().toString();
    }

    private void returnResult() {
        Bundle bundle = new Bundle();
        bundle.putString("pin2", getPin2());
        Uri data = getIntent().getData();
        Intent intent = new Intent();
        if (data != null) {
            intent.setAction(data.toString());
        }
        setResult(-1, intent.putExtras(bundle));
        finish();
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == 6) {
            this.mOkButton.performClick();
            return true;
        }
        return false;
    }

    private void log(String str) {
        Log.d(PhoneGlobals.LOG_TAG, "[GetPin2] " + str);
    }

    private void onCreateMtk() {
        this.mSubId = getIntent().getIntExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, -1);
        log("onCreateMtk mSubId: " + this.mSubId);
        this.mEnterPin2TitleTips = (TextView) findViewById(R.id.get_pin2_title);
        if (CallSettingUtils.getPin2RetryNumber(this.mSubId) == 0) {
            this.mEnterPin2TitleTips.setText(R.string.fdn_puk_need_tips);
        } else {
            this.mEnterPin2TitleTips.append("\n" + CallSettingUtils.getPinPuk2RetryLeftNumTips(this, this.mSubId, true));
        }
        disableActionBar();
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    private void disableActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private boolean validatePin(CharSequence charSequence) {
        return charSequence.length() >= 4 && charSequence.length() <= 8;
    }

    private void showStatus(CharSequence charSequence) {
        if (charSequence != null) {
            Toast.makeText(this, charSequence, 0).show();
        }
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
