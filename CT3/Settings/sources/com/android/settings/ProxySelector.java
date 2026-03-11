package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Proxy;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class ProxySelector extends InstrumentedFragment implements DialogCreatable {
    Button mClearButton;
    Button mDefaultButton;
    private SettingsPreferenceFragment.SettingsDialogFragment mDialogFragment;
    EditText mExclusionListField;
    EditText mHostnameField;
    Button mOKButton;
    EditText mPortField;
    private View mView;
    View.OnClickListener mOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!ProxySelector.this.saveToDb()) {
                return;
            }
            ProxySelector.this.getActivity().onBackPressed();
        }
    };
    View.OnClickListener mClearHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ProxySelector.this.mHostnameField.setText("");
            ProxySelector.this.mPortField.setText("");
            ProxySelector.this.mExclusionListField.setText("");
        }
    };
    View.OnClickListener mDefaultHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ProxySelector.this.populateFields();
        }
    };
    View.OnFocusChangeListener mOnFocusChangeHandler = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                return;
            }
            TextView textView = (TextView) v;
            Selection.selectAll((Spannable) textView.getText());
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mView = inflater.inflate(R.layout.proxy, container, false);
        initView(this.mView);
        populateFields();
        return this.mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        boolean userSetGlobalProxy = dpm.getGlobalProxyAdmin() == null;
        this.mHostnameField.setEnabled(userSetGlobalProxy);
        this.mPortField.setEnabled(userSetGlobalProxy);
        this.mExclusionListField.setEnabled(userSetGlobalProxy);
        this.mOKButton.setEnabled(userSetGlobalProxy);
        this.mClearButton.setEnabled(userSetGlobalProxy);
        this.mDefaultButton.setEnabled(userSetGlobalProxy);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id != 0) {
            return null;
        }
        String hostname = this.mHostnameField.getText().toString().trim();
        String portStr = this.mPortField.getText().toString().trim();
        String exclList = this.mExclusionListField.getText().toString().trim();
        String msg = getActivity().getString(validate(hostname, portStr, exclList));
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.proxy_error).setPositiveButton(R.string.proxy_error_dismiss, (DialogInterface.OnClickListener) null).setMessage(msg).create();
    }

    private void showDialog(int dialogId) {
        if (this.mDialogFragment != null) {
            Log.e("ProxySelector", "Old dialog fragment not null!");
        }
        this.mDialogFragment = new SettingsPreferenceFragment.SettingsDialogFragment(this, dialogId);
        this.mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    private void initView(View view) {
        this.mHostnameField = (EditText) view.findViewById(R.id.hostname);
        this.mHostnameField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
        this.mPortField = (EditText) view.findViewById(R.id.port);
        this.mPortField.setOnClickListener(this.mOKHandler);
        this.mPortField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
        this.mExclusionListField = (EditText) view.findViewById(R.id.exclusionlist);
        this.mExclusionListField.setOnFocusChangeListener(this.mOnFocusChangeHandler);
        this.mOKButton = (Button) view.findViewById(R.id.action);
        this.mOKButton.setOnClickListener(this.mOKHandler);
        this.mClearButton = (Button) view.findViewById(R.id.clear);
        this.mClearButton.setOnClickListener(this.mClearHandler);
        this.mDefaultButton = (Button) view.findViewById(R.id.defaultView);
        this.mDefaultButton.setOnClickListener(this.mDefaultHandler);
    }

    void populateFields() {
        Activity activity = getActivity();
        String hostname = "";
        int port = -1;
        String exclList = "";
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService("connectivity");
        ProxyInfo proxy = cm.getGlobalProxy();
        if (proxy != null) {
            hostname = proxy.getHost();
            port = proxy.getPort();
            exclList = proxy.getExclusionListAsString();
        }
        if (hostname == null) {
            hostname = "";
        }
        this.mHostnameField.setText(hostname);
        String portStr = port == -1 ? "" : Integer.toString(port);
        this.mPortField.setText(portStr);
        this.mExclusionListField.setText(exclList);
        Intent intent = activity.getIntent();
        String buttonLabel = intent.getStringExtra("button-label");
        if (!TextUtils.isEmpty(buttonLabel)) {
            this.mOKButton.setText(buttonLabel);
        }
        String title = intent.getStringExtra("title");
        if (TextUtils.isEmpty(title)) {
            return;
        }
        activity.setTitle(title);
    }

    public static int validate(String hostname, String port, String exclList) {
        switch (Proxy.validate(hostname, port, exclList)) {
            case DefaultWfcSettingsExt.RESUME:
                return 0;
            case DefaultWfcSettingsExt.PAUSE:
                return R.string.proxy_error_empty_host_set_port;
            case DefaultWfcSettingsExt.CREATE:
                return R.string.proxy_error_invalid_host;
            case DefaultWfcSettingsExt.DESTROY:
                return R.string.proxy_error_empty_port;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                return R.string.proxy_error_invalid_port;
            case 5:
                return R.string.proxy_error_invalid_exclusion_list;
            default:
                Log.e("ProxySelector", "Unknown proxy settings error");
                return -1;
        }
    }

    boolean saveToDb() {
        String hostname = this.mHostnameField.getText().toString().trim();
        String portStr = this.mPortField.getText().toString().trim();
        String exclList = this.mExclusionListField.getText().toString().trim();
        int port = 0;
        int result = validate(hostname, portStr, exclList);
        if (result != 0) {
            showDialog(0);
            return false;
        }
        if (portStr.length() > 0) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        ProxyInfo p = new ProxyInfo(hostname, port, exclList);
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService("connectivity");
        cm.setGlobalProxy(p);
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return 82;
    }
}
