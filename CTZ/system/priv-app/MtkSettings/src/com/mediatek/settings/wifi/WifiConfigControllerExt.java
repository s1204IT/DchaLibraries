package com.mediatek.settings.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.SystemProperties;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.wifi.WifiConfigController;
import com.android.settings.wifi.WifiConfigUiBase;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiExt;
import java.util.ArrayList;
import java.util.Arrays;

/* loaded from: classes.dex */
public class WifiConfigControllerExt {
    private WifiConfigUiBase mConfigUi;
    private Context mContext;
    private WifiConfigController mController;
    private IWifiExt mExt;
    private Spinner mSimSlot;
    private View mView;
    private Spinner mWapiCert;

    public WifiConfigControllerExt(WifiConfigController wifiConfigController, WifiConfigUiBase wifiConfigUiBase, View view) {
        this.mController = wifiConfigController;
        this.mConfigUi = wifiConfigUiBase;
        this.mContext = this.mConfigUi.getContext();
        this.mView = view;
        this.mExt = UtilsExt.getWifiExt(this.mContext);
    }

    public void addViews(WifiConfigUiBase wifiConfigUiBase, String str) {
        ViewGroup viewGroup = (ViewGroup) this.mView.findViewById(R.id.info);
        View viewInflate = wifiConfigUiBase.getLayoutInflater().inflate(R.layout.wifi_dialog_row, viewGroup, false);
        ((TextView) viewInflate.findViewById(R.id.name)).setText(wifiConfigUiBase.getContext().getString(R.string.wifi_security));
        this.mExt.setSecurityText((TextView) viewInflate.findViewById(R.id.name));
        ((TextView) viewInflate.findViewById(R.id.value)).setText(str);
        viewGroup.addView(viewInflate);
    }

    public void setConfig(WifiConfiguration wifiConfiguration, int i, TextView textView, Spinner spinner) {
        int selectedItemPosition;
        wifiConfiguration.wapiCertSelMode = 0;
        switch (i) {
            case 3:
                wifiConfiguration.enterpriseConfig.setSimNum(addQuote(-1));
                String str = (String) spinner.getSelectedItem();
                Log.d("WifiConfigControllerExt", "selected eap method:" + str);
                if ("AKA".equals(str) || "SIM".equals(str) || "AKA'".equals(str)) {
                    if (this.mSimSlot == null) {
                        this.mSimSlot = (Spinner) this.mView.findViewById(R.id.sim_slot);
                    }
                    if (TelephonyManager.getDefault().getPhoneCount() == 2 && (selectedItemPosition = this.mSimSlot.getSelectedItemPosition() - 1) > -1) {
                        wifiConfiguration.enterpriseConfig.setSimNum(addQuote(selectedItemPosition));
                    }
                    Log.d("WifiConfigControllerExt", "EAP SIM/AKA config: " + wifiConfiguration.toString());
                    break;
                }
                break;
            case 4:
                wifiConfiguration.allowedKeyManagement.set(8);
                wifiConfiguration.allowedProtocols.set(3);
                if (textView.length() != 0) {
                    wifiConfiguration.preSharedKey = '\"' + textView.getText().toString() + '\"';
                    break;
                }
                break;
            case 5:
                wifiConfiguration.allowedKeyManagement.set(9);
                wifiConfiguration.allowedProtocols.set(3);
                if (this.mWapiCert.getSelectedItemPosition() != 0) {
                    wifiConfiguration.wapiCertSel = (String) this.mWapiCert.getSelectedItem();
                    wifiConfiguration.wapiCertSelMode = 1;
                    break;
                }
                break;
        }
    }

    private static String addQuote(int i) {
        return "\"" + i + "\"";
    }

    public void setEapmethodSpinnerAdapter() {
        Context context = this.mConfigUi.getContext();
        ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, new ArrayList(Arrays.asList(context.getResources().getStringArray(R.array.wifi_eap_method))));
        if (this.mController.getAccessPoint() != null) {
            this.mExt.setEapMethodArray(arrayAdapter, getAccessPointSsid(), getAccessPointSecurity());
        }
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner) this.mView.findViewById(R.id.method)).setAdapter((SpinnerAdapter) arrayAdapter);
    }

    public void setEapMethodFields(boolean z) {
        int selectedItemPosition = ((Spinner) this.mView.findViewById(R.id.method)).getSelectedItemPosition();
        if (this.mController.getAccessPoint() != null) {
            selectedItemPosition = this.mExt.getEapMethodbySpinnerPos(selectedItemPosition, getAccessPointSsid(), getAccessPointSecurity());
        }
        Log.d("WifiConfigControllerExt", "showSecurityFields modify method = " + selectedItemPosition);
        this.mExt.hideWifiConfigInfo(new IWifiExt.Builder().setAccessPoint(this.mController.getAccessPoint()).setEdit(z).setViews(this.mView), this.mConfigUi.getContext());
    }

    public void showEapSimSlotByMethod(int i) {
        WifiConfiguration accessPointConfig;
        if (this.mController.getAccessPoint() != null) {
            i = this.mExt.getEapMethodbySpinnerPos(i, getAccessPointSsid(), getAccessPointSecurity());
        }
        if (i == 4 || i == 5 || i == 6) {
            if (TelephonyManager.getDefault().getPhoneCount() == 2) {
                this.mView.findViewById(R.id.sim_slot_fields).setVisibility(0);
                this.mSimSlot = (Spinner) this.mView.findViewById(R.id.sim_slot);
                Context context = this.mConfigUi.getContext();
                String[] stringArray = context.getResources().getStringArray(R.array.sim_slot);
                int simCount = ((TelephonyManager) this.mContext.getSystemService("phone")).getSimCount();
                Log.d("WifiConfigControllerExt", "the num of sim slot is :" + simCount);
                int i2 = simCount + 1;
                String[] strArr = new String[i2];
                for (int i3 = 0; i3 < i2; i3++) {
                    if (i3 < stringArray.length) {
                        strArr[i3] = stringArray[i3];
                    } else {
                        strArr[i3] = stringArray[1].replaceAll("1", "" + i3);
                    }
                }
                ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, strArr);
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                this.mSimSlot.setAdapter((SpinnerAdapter) arrayAdapter);
                if (this.mController.getAccessPoint() != null && this.mController.getAccessPoint().isSaved() && (accessPointConfig = getAccessPointConfig()) != null && accessPointConfig.enterpriseConfig != null && accessPointConfig.enterpriseConfig.getSimNum() != null) {
                    String strReplace = accessPointConfig.enterpriseConfig.getSimNum().replace("\"", "");
                    if (!strReplace.isEmpty()) {
                        this.mSimSlot.setSelection(Integer.parseInt(strReplace) + 1);
                        return;
                    }
                    return;
                }
                return;
            }
            return;
        }
        if (TelephonyManager.getDefault().getPhoneCount() == 2) {
            this.mView.findViewById(R.id.sim_slot_fields).setVisibility(8);
        }
    }

    public boolean showSecurityFields(int i, boolean z) {
        Log.d("WifiConfigControllerExt", "showSecurityFields, accessPointSecurity = " + i);
        Log.d("WifiConfigControllerExt", "showSecurityFields, edit = " + z);
        if (i == 5) {
            this.mView.findViewById(R.id.security_fields).setVisibility(8);
            this.mView.findViewById(R.id.wapi_cert_fields).setVisibility(0);
            this.mWapiCert = (Spinner) this.mView.findViewById(R.id.wapi_cert);
            this.mWapiCert.setOnItemSelectedListener(this.mController);
            loadCertificates(this.mWapiCert);
            if (this.mController.getAccessPoint() != null && this.mController.getAccessPoint().isSaved()) {
                setCertificate(this.mWapiCert, getAccessPointConfig().wapiCertSel);
                return true;
            }
            return true;
        }
        this.mView.findViewById(R.id.wapi_cert_fields).setVisibility(8);
        this.mExt.hideWifiConfigInfo(new IWifiExt.Builder().setAccessPoint(this.mController.getAccessPoint()).setEdit(z).setViews(this.mView), this.mConfigUi.getContext());
        return false;
    }

    private void setCertificate(Spinner spinner, String str) {
        Log.d("WifiConfigControllerExt", "setSelection, cert = " + str);
        if (str != null) {
            ArrayAdapter arrayAdapter = (ArrayAdapter) spinner.getAdapter();
            for (int count = arrayAdapter.getCount() - 1; count >= 0; count--) {
                if (str.equals(arrayAdapter.getItem(count))) {
                    spinner.setSelection(count);
                    return;
                }
            }
        }
    }

    public boolean enableSubmitIfAppropriate(TextView textView, int i, boolean z) {
        if (textView == null) {
            return z;
        }
        if ((i != 1 || isWepKeyValid(textView.getText().toString())) && (i != 2 || textView.length() >= 8)) {
            if (i != 4) {
                return z;
            }
            if (textView.length() >= 8 && 64 >= textView.length()) {
                return z;
            }
        }
        return true;
    }

    public int getEapMethod(int i) {
        Log.d("WifiConfigControllerExt", "getEapMethod, eapMethod = " + i);
        if (this.mController.getAccessPoint() != null) {
            i = this.mExt.getEapMethodbySpinnerPos(i, getAccessPointSsid(), getAccessPointSecurity());
        }
        Log.d("WifiConfigControllerExt", "getEapMethod, result = " + i);
        return i;
    }

    public void setEapMethodSelection(Spinner spinner, int i) {
        int posByEapMethod;
        if (this.mController.getAccessPoint() != null) {
            posByEapMethod = this.mExt.getPosByEapMethod(i, getAccessPointSsid(), getAccessPointSecurity());
        } else {
            posByEapMethod = i;
        }
        spinner.setSelection(posByEapMethod);
        Log.d("WifiConfigControllerExt", "[skyfyx]showSecurityFields modify pos = " + posByEapMethod);
        Log.d("WifiConfigControllerExt", "[skyfyx]showSecurityFields modify method = " + i);
    }

    public void setProxyText(View view) {
        this.mExt.setProxyText((TextView) view.findViewById(R.id.proxy_exclusionlist_text));
    }

    public void addWifiConfigView(boolean z) {
        this.mExt.setSecurityText((TextView) this.mView.findViewById(R.id.security_text));
        if (this.mController.getAccessPoint() == null) {
            Spinner spinner = (Spinner) this.mView.findViewById(R.id.security);
            boolean z2 = FeatureOption.MTK_WAPI_SUPPORT;
            int i = R.array.wifi_security_no_eap;
            if (z2) {
                String str = SystemProperties.get("persist.vendor.sys.wlan", "wifi-wapi");
                Log.d("WifiConfigControllerExt", "addWifiConfigView, type = " + str);
                if (str.equals("wifi-wapi")) {
                    i = R.array.wapi_security;
                } else if (!str.equals("wifi") && str.equals("wapi")) {
                    i = R.array.wapi_only_security;
                }
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(this.mContext, android.R.layout.simple_spinner_item, this.mContext.getResources().getStringArray(i));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter((SpinnerAdapter) arrayAdapter);
        } else {
            WifiConfiguration accessPointConfig = getAccessPointConfig();
            Log.d("WifiConfigControllerExt", "addWifiConfigView, config = " + accessPointConfig);
            if (this.mController.getAccessPoint().isSaved() && accessPointConfig != null) {
                Log.d("WifiConfigControllerExt", "priority=" + accessPointConfig.priority);
            }
        }
        this.mExt.hideWifiConfigInfo(new IWifiExt.Builder().setAccessPoint(this.mController.getAccessPoint()).setEdit(z).setViews(this.mView), this.mConfigUi.getContext());
    }

    private boolean isWepKeyValid(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        int length = str.length();
        if (((length != 10 && length != 26) || !str.matches("[0-9A-Fa-f]*")) && length != 5 && length != 13) {
            return false;
        }
        return true;
    }

    private void loadCertificates(Spinner spinner) {
        String[] strArr;
        Context context = this.mConfigUi.getContext();
        String string = context.getString(R.string.wapi_auto_sel_cert);
        String[] list = KeyStore.getInstance().list("WAPI_CACERT_", 1010);
        if (list == null || list.length == 0) {
            strArr = new String[]{string};
        } else {
            strArr = new String[list.length + 1];
            strArr[0] = string;
            System.arraycopy(list, 0, strArr, 1, list.length);
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, strArr);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
    }

    public int getSecurity(int i) {
        Log.d("WifiConfigControllerExt", "getSecurity, accessPointSecurity = " + i);
        if (FeatureOption.MTK_WAPI_SUPPORT && SystemProperties.get("persist.vendor.sys.wlan", "wifi-wapi").equals("wapi") && i > 0) {
            i += 3;
        }
        Log.d("WifiConfigControllerExt", "getSecurity, accessPointSecurity = " + i);
        return i;
    }

    private WifiConfiguration getAccessPointConfig() {
        if (this.mController.getAccessPoint() != null) {
            return this.mController.getAccessPoint().getConfig();
        }
        return null;
    }

    private String getAccessPointSsid() {
        if (this.mController.getAccessPoint() != null) {
            return this.mController.getAccessPoint().getSsidStr();
        }
        return null;
    }

    private int getAccessPointSecurity() {
        if (this.mController.getAccessPoint() != null) {
            return this.mController.getAccessPoint().getSecurity();
        }
        return 0;
    }
}
