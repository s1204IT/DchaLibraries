package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.telephony.Phone;

public class IccNetworkDepersonalizationPanel extends IccPanel {
    private Button mDismissButton;
    View.OnClickListener mDismissListener;
    private LinearLayout mEntryPanel;
    private Handler mHandler;
    private Phone mPhone;
    private EditText mPinEntry;
    private TextWatcher mPinEntryWatcher;
    private LinearLayout mStatusPanel;
    private TextView mStatusText;
    private Button mUnlockButton;
    View.OnClickListener mUnlockListener;

    public IccNetworkDepersonalizationPanel(Context context) {
        super(context);
        this.mPinEntryWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence buffer, int start, int olen, int nlen) {
            }

            @Override
            public void onTextChanged(CharSequence buffer, int start, int olen, int nlen) {
            }

            @Override
            public void afterTextChanged(Editable buffer) {
                if (SpecialCharSequenceMgr.handleChars(IccNetworkDepersonalizationPanel.this.getContext(), buffer.toString())) {
                    IccNetworkDepersonalizationPanel.this.mPinEntry.getText().clear();
                }
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AsyncResult res = (AsyncResult) msg.obj;
                    if (res.exception != null) {
                        IccNetworkDepersonalizationPanel.this.indicateError();
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                IccNetworkDepersonalizationPanel.this.hideAlert();
                                IccNetworkDepersonalizationPanel.this.mPinEntry.getText().clear();
                                IccNetworkDepersonalizationPanel.this.mPinEntry.requestFocus();
                            }
                        }, 3000L);
                    } else {
                        IccNetworkDepersonalizationPanel.this.indicateSuccess();
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                IccNetworkDepersonalizationPanel.this.dismiss();
                            }
                        }, 3000L);
                    }
                }
            }
        };
        this.mUnlockListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin = IccNetworkDepersonalizationPanel.this.mPinEntry.getText().toString();
                if (!TextUtils.isEmpty(pin)) {
                    IccNetworkDepersonalizationPanel.this.mPhone.getIccCard().supplyNetworkDepersonalization(pin, Message.obtain(IccNetworkDepersonalizationPanel.this.mHandler, 100));
                    IccNetworkDepersonalizationPanel.this.indicateBusy();
                }
            }
        };
        this.mDismissListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IccNetworkDepersonalizationPanel.this.dismiss();
            }
        };
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sim_ndp);
        this.mPinEntry = (EditText) findViewById(R.id.pin_entry);
        this.mPinEntry.setKeyListener(DialerKeyListener.getInstance());
        this.mPinEntry.setOnClickListener(this.mUnlockListener);
        Spannable text = this.mPinEntry.getText();
        Spannable span = text;
        span.setSpan(this.mPinEntryWatcher, 0, text.length(), 18);
        this.mEntryPanel = (LinearLayout) findViewById(R.id.entry_panel);
        this.mUnlockButton = (Button) findViewById(R.id.ndp_unlock);
        this.mUnlockButton.setOnClickListener(this.mUnlockListener);
        this.mDismissButton = (Button) findViewById(R.id.ndp_dismiss);
        if (getContext().getResources().getBoolean(R.bool.sim_network_unlock_allow_dismiss)) {
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setOnClickListener(this.mDismissListener);
        } else {
            this.mDismissButton.setVisibility(8);
        }
        this.mStatusPanel = (LinearLayout) findViewById(R.id.status_panel);
        this.mStatusText = (TextView) findViewById(R.id.status_text);
        this.mPhone = PhoneGlobals.getPhone();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void indicateBusy() {
        this.mStatusText.setText(R.string.requesting_unlock);
        this.mEntryPanel.setVisibility(8);
        this.mStatusPanel.setVisibility(0);
    }

    private void indicateError() {
        this.mStatusText.setText(R.string.unlock_failed);
        this.mEntryPanel.setVisibility(8);
        this.mStatusPanel.setVisibility(0);
    }

    private void indicateSuccess() {
        this.mStatusText.setText(R.string.unlock_success);
        this.mEntryPanel.setVisibility(8);
        this.mStatusPanel.setVisibility(0);
    }

    private void hideAlert() {
        this.mEntryPanel.setVisibility(0);
        this.mStatusPanel.setVisibility(8);
    }
}
