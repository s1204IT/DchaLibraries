package com.android.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.phone.ext.ExtensionManager;

public class NetworkOperators extends PreferenceCategory implements Preference.OnPreferenceChangeListener {
    private static final int ALREADY_IN_AUTO_SELECTION = 1;
    public static final String BUTTON_AUTO_SELECT_KEY = "button_auto_select_key";
    public static final String BUTTON_CHOOSE_NETWORK_KEY = "button_choose_network_key";
    public static final String BUTTON_NETWORK_SELECT_KEY = "button_network_select_key";
    private static final String BUTTTON_MANUAL_FEMTOCELL = "button_manual_femtocell";
    public static final String CATEGORY_NETWORK_OPERATORS_KEY = "network_operators_category_key";
    private static final boolean DBG = true;
    private static final int EVENT_AUTO_SELECT_DONE = 100;
    private static final int EVENT_GET_NETWORK_SELECTION_MODE_DONE = 200;
    private static final String LOG_TAG = "NetworkOperators";
    private TwoStatePreference mAutoSelect;
    private Preference mChooseNetwork;
    boolean mEnableNewManualSelectNetworkUI;
    private final Handler mHandler;
    private Preference mManuSelectFemtocell;
    private NetworkSelectListPreference mNetworkSelect;
    int mPhoneId;
    private ProgressDialog mProgressDialog;
    private int mSubId;

    public NetworkOperators(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPhoneId = -1;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                int i = message.what;
                if (i == 100) {
                    NetworkOperators.this.mAutoSelect.setEnabled(NetworkOperators.DBG);
                    NetworkOperators.this.dismissProgressBar();
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        NetworkOperators.this.logd("automatic network selection: failed!");
                        NetworkOperators.this.displayNetworkSelectionFailed(asyncResult.exception);
                        return;
                    } else {
                        NetworkOperators.this.logd("automatic network selection: succeeded!");
                        NetworkOperators.this.displayNetworkSelectionSucceeded(message.arg1);
                        return;
                    }
                }
                if (i == NetworkOperators.EVENT_GET_NETWORK_SELECTION_MODE_DONE) {
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2.exception != null) {
                        NetworkOperators.this.logd("get network selection mode: failed!");
                        return;
                    }
                    if (asyncResult2.result != null) {
                        try {
                            boolean z = false;
                            if (((int[]) asyncResult2.result)[0] == 0) {
                                z = true;
                            }
                            NetworkOperators networkOperators = NetworkOperators.this;
                            StringBuilder sb = new StringBuilder();
                            sb.append("get network selection mode: ");
                            sb.append(z ? "auto" : "manual");
                            sb.append(" selection");
                            networkOperators.logd(sb.toString());
                            if (NetworkOperators.this.mAutoSelect != null) {
                                NetworkOperators.this.mAutoSelect.setChecked(z);
                            }
                            if (NetworkOperators.this.mEnableNewManualSelectNetworkUI) {
                                if (NetworkOperators.this.mChooseNetwork != null) {
                                    NetworkOperators.this.mChooseNetwork.setEnabled(z ^ NetworkOperators.DBG);
                                }
                            } else if (NetworkOperators.this.mNetworkSelect != null) {
                                NetworkOperators.this.mNetworkSelect.setEnabled(z ^ NetworkOperators.DBG);
                            }
                        } catch (Exception e) {
                            NetworkOperators.this.loge("get network selection mode: unable to parse result.");
                        }
                    }
                }
            }
        };
    }

    public NetworkOperators(Context context) {
        super(context);
        this.mPhoneId = -1;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                int i = message.what;
                if (i == 100) {
                    NetworkOperators.this.mAutoSelect.setEnabled(NetworkOperators.DBG);
                    NetworkOperators.this.dismissProgressBar();
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        NetworkOperators.this.logd("automatic network selection: failed!");
                        NetworkOperators.this.displayNetworkSelectionFailed(asyncResult.exception);
                        return;
                    } else {
                        NetworkOperators.this.logd("automatic network selection: succeeded!");
                        NetworkOperators.this.displayNetworkSelectionSucceeded(message.arg1);
                        return;
                    }
                }
                if (i == NetworkOperators.EVENT_GET_NETWORK_SELECTION_MODE_DONE) {
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2.exception != null) {
                        NetworkOperators.this.logd("get network selection mode: failed!");
                        return;
                    }
                    if (asyncResult2.result != null) {
                        try {
                            boolean z = false;
                            if (((int[]) asyncResult2.result)[0] == 0) {
                                z = true;
                            }
                            NetworkOperators networkOperators = NetworkOperators.this;
                            StringBuilder sb = new StringBuilder();
                            sb.append("get network selection mode: ");
                            sb.append(z ? "auto" : "manual");
                            sb.append(" selection");
                            networkOperators.logd(sb.toString());
                            if (NetworkOperators.this.mAutoSelect != null) {
                                NetworkOperators.this.mAutoSelect.setChecked(z);
                            }
                            if (NetworkOperators.this.mEnableNewManualSelectNetworkUI) {
                                if (NetworkOperators.this.mChooseNetwork != null) {
                                    NetworkOperators.this.mChooseNetwork.setEnabled(z ^ NetworkOperators.DBG);
                                }
                            } else if (NetworkOperators.this.mNetworkSelect != null) {
                                NetworkOperators.this.mNetworkSelect.setEnabled(z ^ NetworkOperators.DBG);
                            }
                        } catch (Exception e) {
                            NetworkOperators.this.loge("get network selection mode: unable to parse result.");
                        }
                    }
                }
            }
        };
    }

    public void initialize() {
        this.mEnableNewManualSelectNetworkUI = getContext().getResources().getBoolean(android.R.^attr-private.hasRoundedCorners);
        this.mAutoSelect = (TwoStatePreference) findPreference(BUTTON_AUTO_SELECT_KEY);
        this.mChooseNetwork = findPreference(BUTTON_CHOOSE_NETWORK_KEY);
        this.mNetworkSelect = (NetworkSelectListPreference) findPreference(BUTTON_NETWORK_SELECT_KEY);
        if (this.mEnableNewManualSelectNetworkUI) {
            removePreference(this.mNetworkSelect);
        } else {
            removePreference(this.mChooseNetwork);
        }
        this.mProgressDialog = new ProgressDialog(getContext());
        newManuSelectFemetocellPreference(this);
        ExtensionManager.getNetworkSettingExt().initOtherNetworkSetting(this);
    }

    protected void update(int i, INetworkQueryService iNetworkQueryService) {
        this.mSubId = i;
        this.mPhoneId = SubscriptionManager.getPhoneId(this.mSubId);
        if (this.mAutoSelect != null) {
            this.mAutoSelect.setOnPreferenceChangeListener(this);
        }
        if (this.mEnableNewManualSelectNetworkUI) {
            if (this.mChooseNetwork != null) {
                TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService("phone");
                logd("data connection status " + telephonyManager.getDataState());
                if (telephonyManager.getDataState() == 2) {
                    this.mChooseNetwork.setSummary(telephonyManager.getNetworkOperatorName());
                } else {
                    this.mChooseNetwork.setSummary(R.string.network_disconnected);
                }
            }
        } else if (this.mNetworkSelect != null) {
            this.mNetworkSelect.initialize(this.mSubId, iNetworkQueryService, this, this.mProgressDialog);
        }
        getNetworkSelectionMode();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mAutoSelect) {
            boolean zBooleanValue = ((Boolean) obj).booleanValue();
            logd("onPreferenceChange autoSelect: " + String.valueOf(zBooleanValue));
            selectNetworkAutomatic(zBooleanValue);
            MetricsLogger.action(getContext(), 1209, zBooleanValue);
            return DBG;
        }
        if (preference == this.mManuSelectFemtocell) {
            selectFemtocellManually();
            return DBG;
        }
        return false;
    }

    protected void displayNetworkSelectionFailed(Throwable th) {
        String string;
        ServiceState serviceStateForSubscriber;
        if (th != null && (th instanceof CommandException) && ((CommandException) th).getCommandError() == CommandException.Error.ILLEGAL_SIM_OR_ME) {
            string = getContext().getResources().getString(R.string.not_allowed);
        } else {
            string = getContext().getResources().getString(R.string.connect_later);
        }
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        phoneGlobals.notificationMgr.postTransientNotification(2, string);
        TelephonyManager telephonyManager = (TelephonyManager) phoneGlobals.getSystemService("phone");
        Phone phone = PhoneFactory.getPhone(this.mPhoneId);
        if (phone != null && (serviceStateForSubscriber = telephonyManager.getServiceStateForSubscriber(phone.getSubId())) != null) {
            phoneGlobals.notificationMgr.updateNetworkSelection(serviceStateForSubscriber.getState(), phone.getSubId());
        }
    }

    protected void displayNetworkSelectionSucceeded(int i) {
        String string;
        if (i == 1) {
            string = getContext().getResources().getString(R.string.already_auto);
        } else {
            string = getContext().getResources().getString(R.string.registration_done);
        }
        PhoneGlobals.getInstance().notificationMgr.postTransientNotification(2, string);
    }

    private void selectNetworkAutomatic(boolean z) {
        logd("selectNetworkAutomatic: " + String.valueOf(z));
        if (z) {
            if (this.mEnableNewManualSelectNetworkUI) {
                if (this.mChooseNetwork != null) {
                    this.mChooseNetwork.setEnabled(z ^ DBG);
                }
            } else if (this.mNetworkSelect != null) {
                this.mNetworkSelect.setEnabled(z ^ DBG);
            }
            logd("select network automatically...");
            showAutoSelectProgressBar();
            this.mAutoSelect.setEnabled(false);
            Message messageObtainMessage = this.mHandler.obtainMessage(100);
            Phone phone = PhoneFactory.getPhone(this.mPhoneId);
            if (phone != null) {
                phone.setNetworkSelectionModeAutomatic(messageObtainMessage);
                return;
            }
            return;
        }
        if (this.mEnableNewManualSelectNetworkUI) {
            if (this.mChooseNetwork != null) {
                openChooseNetworkPage();
            }
        } else if (this.mNetworkSelect != null) {
            this.mNetworkSelect.onClick();
        }
    }

    protected void getNetworkSelectionMode() {
        logd("getting network selection mode...");
        Message messageObtainMessage = this.mHandler.obtainMessage(EVENT_GET_NETWORK_SELECTION_MODE_DONE);
        Phone phone = PhoneFactory.getPhone(this.mPhoneId);
        if (phone != null) {
            phone.getNetworkSelectionMode(messageObtainMessage);
        }
    }

    private void dismissProgressBar() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            try {
                this.mProgressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                logd("dismiss error.");
            }
        }
    }

    private void showAutoSelectProgressBar() {
        this.mProgressDialog.setMessage(getContext().getResources().getString(R.string.register_automatically));
        this.mProgressDialog.setCanceledOnTouchOutside(false);
        this.mProgressDialog.setCancelable(false);
        this.mProgressDialog.setIndeterminate(DBG);
        this.mProgressDialog.show();
    }

    public void openChooseNetworkPage() {
        getContext().startActivity(NetworkSelectSettingActivity.getIntent(getContext(), this.mPhoneId));
    }

    protected boolean preferenceTreeClick(Preference preference) {
        if (this.mEnableNewManualSelectNetworkUI) {
            logd("enable New AutoSelectNetwork UI");
            if (preference == this.mChooseNetwork) {
                openChooseNetworkPage();
            }
            if (preference == this.mAutoSelect || preference == this.mChooseNetwork) {
                return DBG;
            }
            return false;
        }
        if (preference == this.mAutoSelect || preference == this.mNetworkSelect) {
            return DBG;
        }
        return false;
    }

    private void logd(String str) {
        Log.d(LOG_TAG, "[NetworksList] " + str);
    }

    private void loge(String str) {
        Log.e(LOG_TAG, "[NetworksList] " + str);
    }

    private void newManuSelectFemetocellPreference(PreferenceCategory preferenceCategory) {
        if (PhoneFeatureConstants.FeatureOption.isMtkFemtoCellSupport() && !isNetworkModeSetGsmOnly() && this.mManuSelectFemtocell == null) {
            this.mManuSelectFemtocell = new Preference(getContext());
            this.mManuSelectFemtocell.setKey(BUTTTON_MANUAL_FEMTOCELL);
            this.mManuSelectFemtocell.setTitle(R.string.sum_search_femtocell_networks);
            this.mManuSelectFemtocell.setPersistent(false);
            preferenceCategory.addPreference(this.mManuSelectFemtocell);
        }
    }

    private boolean isNetworkModeSetGsmOnly() {
        if (1 == Settings.Global.getInt(getContext().getContentResolver(), "preferred_network_mode", Phone.PREFERRED_NT_MODE)) {
            return DBG;
        }
        return false;
    }

    private void selectFemtocellManually() {
        logd("selectFemtocellManually()");
        Intent intent = new Intent();
        intent.setClassName("com.android.phone", "com.mediatek.settings.FemtoPointList");
        intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mSubId);
        getContext().startActivity(intent);
    }
}
