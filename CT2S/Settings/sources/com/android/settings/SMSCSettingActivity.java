package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class SMSCSettingActivity extends Activity implements View.OnClickListener {
    private Button mCancelButton;
    private String mDefault = "";
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        SMSCSettingActivity.this.mSmscEditText.setText((String) ar.result);
                        SMSCSettingActivity.this.mDefault = (String) ar.result;
                    } else {
                        Toast.makeText(SMSCSettingActivity.this.getApplicationContext(), R.string.no_valid_smsc, 0).show();
                        SMSCSettingActivity.this.mSmscEditText.setText(SMSCSettingActivity.this.mDefault);
                    }
                    break;
                case 1002:
                    if (((AsyncResult) msg.obj).exception != null) {
                        SMSCSettingActivity.this.mSmscEditText.setText(SMSCSettingActivity.this.mDefault);
                        Toast.makeText(SMSCSettingActivity.this.getApplicationContext(), R.string.update_error, 0).show();
                    } else {
                        SMSCSettingActivity.this.mDefault = SMSCSettingActivity.this.mSmscEditText.getText().toString();
                        Toast.makeText(SMSCSettingActivity.this.getApplicationContext(), R.string.update_success, 1).show();
                        SMSCSettingActivity.this.finish();
                    }
                    break;
            }
        }
    };
    private Button mOkButton;
    private Phone mPhone;
    private EditText mSmscEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smsc_setting);
        Intent intent = getIntent();
        int phoneId = intent.getIntExtra("phone", Dsds.defaultSimId().ordinal());
        this.mPhone = PhoneFactory.getPhone(phoneId);
        this.mOkButton = (Button) findViewById(R.id.smsc_ok);
        this.mCancelButton = (Button) findViewById(R.id.smsc_cancel);
        this.mSmscEditText = (EditText) findViewById(R.id.smsc);
        this.mOkButton.setOnClickListener(this);
        this.mCancelButton.setOnClickListener(this);
        setSmscEditText();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.smsc_ok:
                this.mPhone.setSmscAddress(this.mSmscEditText.getText().toString(), this.mHandler.obtainMessage(1002));
                break;
            case R.id.smsc_cancel:
                finish();
                break;
        }
    }

    public void setSmscEditText() {
        this.mPhone.getSmscAddress(this.mHandler.obtainMessage(1001));
    }
}
