package com.android.phone;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.android.internal.telephony.test.SimulatedRadioControl;

public class FakePhoneActivity extends Activity {
    private static final String TAG = "FakePhoneActivity";
    private EditText mPhoneNumber;
    private Button mPlaceCall;
    SimulatedRadioControl mRadioControl;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.fake_phone_activity);
        this.mPlaceCall = (Button) findViewById(R.id.placeCall);
        this.mPlaceCall.setOnClickListener(new ButtonListener());
        this.mPhoneNumber = (EditText) findViewById(R.id.phoneNumber);
        this.mPhoneNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FakePhoneActivity.this.mPlaceCall.requestFocus();
            }
        });
        this.mRadioControl = PhoneGlobals.getPhone().getSimulatedRadioControl();
        Log.i(TAG, "- PhoneApp.getInstance(): " + PhoneGlobals.getInstance());
        Log.i(TAG, "- PhoneApp.getPhone(): " + PhoneGlobals.getPhone());
        Log.i(TAG, "- mRadioControl: " + this.mRadioControl);
    }

    private class ButtonListener implements View.OnClickListener {
        private ButtonListener() {
        }

        @Override
        public void onClick(View view) {
            if (FakePhoneActivity.this.mRadioControl != null) {
                FakePhoneActivity.this.mRadioControl.triggerRing(FakePhoneActivity.this.mPhoneNumber.getText().toString());
                return;
            }
            Log.e("Phone", "SimulatedRadioControl not available, abort!");
            Toast.makeText(FakePhoneActivity.this, "null mRadioControl!", 0).show();
        }
    }
}
