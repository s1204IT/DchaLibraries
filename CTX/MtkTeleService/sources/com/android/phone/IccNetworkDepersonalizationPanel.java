package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.telephony.Phone;

public class IccNetworkDepersonalizationPanel extends IccPanel {
    private static final boolean DBG = false;
    private static final int EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT = 100;
    private static boolean sShowingDialog = false;
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

    public static void showDialog(Phone phone) {
        if (sShowingDialog) {
            Log.i(PhoneGlobals.LOG_TAG, "[IccNetworkDepersonalizationPanel] - showDialog; skipped already shown.");
            return;
        }
        Log.i(PhoneGlobals.LOG_TAG, "[IccNetworkDepersonalizationPanel] - showDialog; showing dialog.");
        sShowingDialog = true;
        new IccNetworkDepersonalizationPanel(PhoneGlobals.getInstance(), phone).show();
    }

    public IccNetworkDepersonalizationPanel(Context context) {
        super(context);
        this.mPinEntryWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (SpecialCharSequenceMgr.handleChars(IccNetworkDepersonalizationPanel.this.getContext(), editable.toString())) {
                    IccNetworkDepersonalizationPanel.this.mPinEntry.getText().clear();
                }
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 100) {
                    if (((AsyncResult) message.obj).exception != null) {
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
            public void onClick(View view) {
                String string = IccNetworkDepersonalizationPanel.this.mPinEntry.getText().toString();
                if (TextUtils.isEmpty(string)) {
                    return;
                }
                IccNetworkDepersonalizationPanel.this.mPhone.getIccCard().supplyNetworkDepersonalization(string, Message.obtain(IccNetworkDepersonalizationPanel.this.mHandler, 100));
                IccNetworkDepersonalizationPanel.this.indicateBusy();
            }
        };
        this.mDismissListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IccNetworkDepersonalizationPanel.this.dismiss();
            }
        };
        this.mPhone = PhoneGlobals.getPhone();
    }

    public IccNetworkDepersonalizationPanel(Context context, Phone phone) {
        super(context);
        this.mPinEntryWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (SpecialCharSequenceMgr.handleChars(IccNetworkDepersonalizationPanel.this.getContext(), editable.toString())) {
                    IccNetworkDepersonalizationPanel.this.mPinEntry.getText().clear();
                }
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 100) {
                    if (((AsyncResult) message.obj).exception != null) {
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
            public void onClick(View view) {
                String string = IccNetworkDepersonalizationPanel.this.mPinEntry.getText().toString();
                if (TextUtils.isEmpty(string)) {
                    return;
                }
                IccNetworkDepersonalizationPanel.this.mPhone.getIccCard().supplyNetworkDepersonalization(string, Message.obtain(IccNetworkDepersonalizationPanel.this.mHandler, 100));
                IccNetworkDepersonalizationPanel.this.indicateBusy();
            }
        };
        this.mDismissListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IccNetworkDepersonalizationPanel.this.dismiss();
            }
        };
        this.mPhone = phone == null ? PhoneGlobals.getPhone() : phone;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.sim_ndp);
        this.mPinEntry = (EditText) findViewById(R.id.pin_entry);
        this.mPinEntry.setKeyListener(DialerKeyListener.getInstance());
        this.mPinEntry.setOnClickListener(this.mUnlockListener);
        Editable text = this.mPinEntry.getText();
        text.setSpan(this.mPinEntryWatcher, 0, text.length(), 18);
        this.mEntryPanel = (LinearLayout) findViewById(R.id.entry_panel);
        this.mUnlockButton = (Button) findViewById(R.id.ndp_unlock);
        this.mUnlockButton.setOnClickListener(this.mUnlockListener);
        this.mDismissButton = (Button) findViewById(R.id.ndp_dismiss);
        if (PhoneGlobals.getInstance().getCarrierConfig().getBoolean("sim_network_unlock_allow_dismiss_bool")) {
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setOnClickListener(this.mDismissListener);
        } else {
            this.mDismissButton.setVisibility(8);
        }
        this.mStatusPanel = (LinearLayout) findViewById(R.id.status_panel);
        this.mStatusText = (TextView) findViewById(R.id.status_text);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(PhoneGlobals.LOG_TAG, "[IccNetworkDepersonalizationPanel] - showDialog; hiding dialog.");
        sShowingDialog = false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
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

    private void log(String str) {
        Log.v(PhoneGlobals.LOG_TAG, "[IccNetworkDepersonalizationPanel] " + str);
    }
}
