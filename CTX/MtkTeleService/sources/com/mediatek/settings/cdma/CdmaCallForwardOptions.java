package com.mediatek.settings.cdma;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.settings.CallSettingUtils;
import java.util.ArrayList;

public class CdmaCallForwardOptions extends PreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private Bundle mBundle;
    private Preference mButtonCFB;
    private Preference mButtonCFC;
    private Preference mButtonCFNRc;
    private Preference mButtonCFNRy;
    private Preference mButtonCFU;
    private String mCarrierName;
    private IntentFilter mIntentFilter;
    private static final String[] NUM_PROJECTION = {"data1"};
    private static final String[] CF_HEADERS = {"*72", "*720", "*90", "*900", "*92", "*920", "*68", "*680", "*730"};
    private ArrayList<Preference> mPreferences = null;
    private EditText mEditNumber = null;
    private int mSubId = -1;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction()) && intent.getBooleanExtra("state", false)) {
                CdmaCallForwardOptions.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        String string;
        super.onCreate(bundle);
        this.mBundle = bundle;
        addPreferencesFromResource(R.xml.mtk_cdma_callforward_options);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mButtonCFU = preferenceScreen.findPreference("button_cfu_key");
        this.mButtonCFB = preferenceScreen.findPreference("button_cfb_key");
        this.mButtonCFNRy = preferenceScreen.findPreference("button_cfnry_key");
        this.mButtonCFNRc = preferenceScreen.findPreference("button_cfnrc_key");
        this.mButtonCFC = preferenceScreen.findPreference("button_cfc_key");
        if (PhoneFeatureConstants.FeatureOption.isMtkCtaSet() && "CN".equals(getResources().getConfiguration().locale.getCountry()) && (string = getResources().getString(R.string.cdma_labelCFNRc_cta)) != null) {
            this.mButtonCFNRc.setTitle(string);
        }
        this.mPreferences = new ArrayList<>();
        this.mPreferences.add(this.mButtonCFU);
        this.mPreferences.add(this.mButtonCFB);
        this.mPreferences.add(this.mButtonCFNRy);
        this.mPreferences.add(this.mButtonCFNRc);
        this.mPreferences.add(this.mButtonCFC);
        SubscriptionInfoHelper subscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubId = subscriptionInfoHelper.getPhone() != null ? subscriptionInfoHelper.getPhone().getSubId() : -1;
        Log.d("Settings/CdmaCallForwardOptions", "onCreate: " + this.mSubId);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        registerCallBacks();
        if (!PhoneUtils.isValidSubId(this.mSubId)) {
            finish();
            Log.d("Settings/CdmaCallForwardOptions", "onCreate, mSubId is invalid = " + this.mSubId);
            return;
        }
        PersistableBundle configForSubId = ((CarrierConfigManager) getSystemService("carrier_config")).getConfigForSubId(this.mSubId);
        if (configForSubId != null) {
            this.mCarrierName = configForSubId.getString("carrier_name_string");
        }
        if (this.mCarrierName != null && this.mCarrierName.equalsIgnoreCase("Sprint")) {
            Log.d("Settings/CdmaCallForwardOptions", "Removing CallForward unReachable and CallForward Cancel");
            preferenceScreen.removePreference(this.mButtonCFNRc);
            preferenceScreen.removePreference(this.mButtonCFC);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mReceiver);
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    private void registerCallBacks() {
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    @Override
    public void handleSubInfoUpdate() {
        Log.d("Settings/CdmaCallForwardOptions", "handleSubInfoUpdate");
        finish();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mButtonCFU) {
            showDialog(0);
        } else if (preference == this.mButtonCFB) {
            showDialog(1);
        } else if (preference == this.mButtonCFNRy) {
            showDialog(2);
        } else if (preference == this.mButtonCFNRc) {
            showDialog(3);
        } else if (preference == this.mButtonCFC) {
            String str = CF_HEADERS[8];
            if (CallSettingUtils.isOperator(this.mSubId, CallSettingUtils.OPID.OP12)) {
                Log.d("Settings/CdmaCallForwardOptions", "cancel all call forwarding, change to special feature code...");
                str = "*73";
            }
            setCallForward(str);
        }
        return true;
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
    protected Dialog onCreateDialog(final int i) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.mtk_cdma_cf_dialog);
        dialog.setTitle(this.mPreferences.get(i).getTitle());
        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.group);
        ImageButton imageButton = (ImageButton) dialog.findViewById(R.id.select_contact);
        if (imageButton != null) {
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CdmaCallForwardOptions.this.startContacts();
                }
            });
        }
        Button button = (Button) dialog.findViewById(R.id.save);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String str;
                    if (radioGroup.getCheckedRadioButtonId() == -1) {
                        return;
                    }
                    if (radioGroup.getCheckedRadioButtonId() == R.id.enable) {
                        str = CdmaCallForwardOptions.CF_HEADERS[i * 2] + ((Object) CdmaCallForwardOptions.this.mEditNumber.getText());
                        if (CdmaCallForwardOptions.this.mCarrierName != null && CdmaCallForwardOptions.this.mCarrierName.equalsIgnoreCase("Sprint")) {
                            if (i == 1) {
                                str = "*74" + ((Object) CdmaCallForwardOptions.this.mEditNumber.getText());
                            } else if (i == 2) {
                                str = "*73" + ((Object) CdmaCallForwardOptions.this.mEditNumber.getText());
                            }
                        }
                    } else {
                        str = CdmaCallForwardOptions.CF_HEADERS[(i * 2) + 1];
                        if (CdmaCallForwardOptions.this.mCarrierName != null && CdmaCallForwardOptions.this.mCarrierName.equalsIgnoreCase("Sprint")) {
                            if (i == 1) {
                                str = "*740";
                            } else if (i == 2) {
                                str = "*730";
                            }
                        }
                    }
                    CallSettingUtils.sensitiveLog("Settings/CdmaCallForwardOptions", "CDMA CallForward Prefix: ", str);
                    dialog.dismiss();
                    CdmaCallForwardOptions.this.setCallForward(str);
                }
            });
        }
        Button button2 = (Button) dialog.findViewById(R.id.cancel);
        if (button2 != null) {
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }
        return dialog;
    }

    @Override
    public void onPrepareDialog(int i, Dialog dialog) {
        super.onPrepareDialog(i, dialog);
        this.mEditNumber = (EditText) dialog.findViewById(R.id.EditNumber);
    }

    private void setCallForward(String str) {
        if (this.mSubId == -1 || str == null || str.isEmpty()) {
            Log.d("Settings/CdmaCallForwardOptions", "setCallForward null return");
            return;
        }
        Intent intent = new Intent("android.intent.action.CALL");
        intent.setData(Uri.parse("tel:" + str));
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", PhoneUtils.makePstnPhoneAccountHandle(SubscriptionManager.getPhoneId(this.mSubId)));
        startActivity(intent);
    }

    private void startContacts() {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setType("vnd.android.cursor.dir/phone_v2");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) throws Throwable {
        Cursor cursorQuery;
        Throwable th;
        if (i2 != -1 || i != 100 || intent == null) {
            return;
        }
        try {
            cursorQuery = getContentResolver().query(intent.getData(), NUM_PROJECTION, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst() && this.mEditNumber != null) {
                        this.mEditNumber.setText(cursorQuery.getString(0));
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th3) {
            cursorQuery = null;
            th = th3;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        SubscriptionInfoHelper subscriptionInfoHelper = new SubscriptionInfoHelper(this, intent);
        this.mSubId = subscriptionInfoHelper.getPhone() != null ? subscriptionInfoHelper.getPhone().getSubId() : -1;
        Log.d("Settings/CdmaCallForwardOptions", "onNewIntent: " + this.mSubId);
        if (!PhoneUtils.isValidSubId(this.mSubId)) {
            finish();
            Log.d("Settings/CdmaCallForwardOptions", "onNewIntent, mSubId is invalid = " + this.mSubId);
        }
    }
}
