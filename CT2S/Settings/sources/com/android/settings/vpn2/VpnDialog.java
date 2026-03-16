package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;
import java.net.InetAddress;

class VpnDialog extends AlertDialog implements TextWatcher, View.OnClickListener, AdapterView.OnItemSelectedListener {
    private TextView mDnsServers;
    private boolean mEditing;
    private Spinner mIpsecCaCert;
    private TextView mIpsecIdentifier;
    private TextView mIpsecSecret;
    private Spinner mIpsecServerCert;
    private Spinner mIpsecUserCert;
    private final KeyStore mKeyStore;
    private TextView mL2tpSecret;
    private final DialogInterface.OnClickListener mListener;
    private CheckBox mMppe;
    private TextView mName;
    private TextView mPassword;
    private final VpnProfile mProfile;
    private TextView mRoutes;
    private CheckBox mSaveLogin;
    private TextView mSearchDomains;
    private TextView mServer;
    private Spinner mType;
    private TextView mUsername;
    private View mView;

    VpnDialog(Context context, DialogInterface.OnClickListener listener, VpnProfile profile, boolean editing) {
        super(context);
        this.mKeyStore = KeyStore.getInstance();
        this.mListener = listener;
        this.mProfile = profile;
        this.mEditing = editing;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        this.mView = getLayoutInflater().inflate(R.layout.vpn_dialog, (ViewGroup) null);
        setView(this.mView);
        setInverseBackgroundForced(true);
        Context context = getContext();
        this.mName = (TextView) this.mView.findViewById(R.id.name);
        this.mType = (Spinner) this.mView.findViewById(R.id.type);
        this.mServer = (TextView) this.mView.findViewById(R.id.server);
        this.mUsername = (TextView) this.mView.findViewById(R.id.username);
        this.mPassword = (TextView) this.mView.findViewById(R.id.password);
        this.mSearchDomains = (TextView) this.mView.findViewById(R.id.search_domains);
        this.mDnsServers = (TextView) this.mView.findViewById(R.id.dns_servers);
        this.mRoutes = (TextView) this.mView.findViewById(R.id.routes);
        this.mMppe = (CheckBox) this.mView.findViewById(R.id.mppe);
        this.mL2tpSecret = (TextView) this.mView.findViewById(R.id.l2tp_secret);
        this.mIpsecIdentifier = (TextView) this.mView.findViewById(R.id.ipsec_identifier);
        this.mIpsecSecret = (TextView) this.mView.findViewById(R.id.ipsec_secret);
        this.mIpsecUserCert = (Spinner) this.mView.findViewById(R.id.ipsec_user_cert);
        this.mIpsecCaCert = (Spinner) this.mView.findViewById(R.id.ipsec_ca_cert);
        this.mIpsecServerCert = (Spinner) this.mView.findViewById(R.id.ipsec_server_cert);
        this.mSaveLogin = (CheckBox) this.mView.findViewById(R.id.save_login);
        this.mName.setText(this.mProfile.name);
        this.mType.setSelection(this.mProfile.type);
        this.mServer.setText(this.mProfile.server);
        if (this.mProfile.saveLogin) {
            this.mUsername.setText(this.mProfile.username);
            this.mPassword.setText(this.mProfile.password);
        }
        this.mSearchDomains.setText(this.mProfile.searchDomains);
        this.mDnsServers.setText(this.mProfile.dnsServers);
        this.mRoutes.setText(this.mProfile.routes);
        this.mMppe.setChecked(this.mProfile.mppe);
        this.mL2tpSecret.setText(this.mProfile.l2tpSecret);
        this.mIpsecIdentifier.setText(this.mProfile.ipsecIdentifier);
        this.mIpsecSecret.setText(this.mProfile.ipsecSecret);
        loadCertificates(this.mIpsecUserCert, "USRPKEY_", 0, this.mProfile.ipsecUserCert);
        loadCertificates(this.mIpsecCaCert, "CACERT_", R.string.vpn_no_ca_cert, this.mProfile.ipsecCaCert);
        loadCertificates(this.mIpsecServerCert, "USRCERT_", R.string.vpn_no_server_cert, this.mProfile.ipsecServerCert);
        this.mSaveLogin.setChecked(this.mProfile.saveLogin);
        this.mName.addTextChangedListener(this);
        this.mType.setOnItemSelectedListener(this);
        this.mServer.addTextChangedListener(this);
        this.mUsername.addTextChangedListener(this);
        this.mPassword.addTextChangedListener(this);
        this.mDnsServers.addTextChangedListener(this);
        this.mRoutes.addTextChangedListener(this);
        this.mIpsecSecret.addTextChangedListener(this);
        this.mIpsecUserCert.setOnItemSelectedListener(this);
        boolean valid = validate(true);
        this.mEditing = this.mEditing || !valid;
        if (this.mEditing) {
            setTitle(R.string.vpn_edit);
            this.mView.findViewById(R.id.editor).setVisibility(0);
            changeType(this.mProfile.type);
            View showOptions = this.mView.findViewById(R.id.show_options);
            if (this.mProfile.searchDomains.isEmpty() && this.mProfile.dnsServers.isEmpty() && this.mProfile.routes.isEmpty()) {
                showOptions.setOnClickListener(this);
            } else {
                onClick(showOptions);
            }
            setButton(-1, context.getString(R.string.vpn_save), this.mListener);
        } else {
            setTitle(context.getString(R.string.vpn_connect_to, this.mProfile.name));
            this.mView.findViewById(R.id.login).setVisibility(0);
            setButton(-1, context.getString(R.string.vpn_connect), this.mListener);
        }
        setButton(-2, context.getString(R.string.vpn_cancel), this.mListener);
        super.onCreate(null);
        Button button = getButton(-1);
        if (!this.mEditing) {
            valid = validate(false);
        }
        button.setEnabled(valid);
        getWindow().setSoftInputMode(20);
    }

    @Override
    public void afterTextChanged(Editable field) {
        getButton(-1).setEnabled(validate(this.mEditing));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onClick(View showOptions) {
        showOptions.setVisibility(8);
        this.mView.findViewById(R.id.options).setVisibility(0);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == this.mType) {
            changeType(position);
        }
        getButton(-1).setEnabled(validate(this.mEditing));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void changeType(int type) {
        this.mMppe.setVisibility(8);
        this.mView.findViewById(R.id.l2tp).setVisibility(8);
        this.mView.findViewById(R.id.ipsec_psk).setVisibility(8);
        this.mView.findViewById(R.id.ipsec_user).setVisibility(8);
        this.mView.findViewById(R.id.ipsec_peer).setVisibility(8);
        switch (type) {
            case 0:
                this.mMppe.setVisibility(0);
                break;
            case 1:
                this.mView.findViewById(R.id.l2tp).setVisibility(0);
                this.mView.findViewById(R.id.ipsec_psk).setVisibility(0);
                break;
            case 2:
                this.mView.findViewById(R.id.l2tp).setVisibility(0);
                this.mView.findViewById(R.id.ipsec_user).setVisibility(0);
                this.mView.findViewById(R.id.ipsec_peer).setVisibility(0);
                break;
            case 3:
                this.mView.findViewById(R.id.ipsec_psk).setVisibility(0);
                break;
            case 4:
                this.mView.findViewById(R.id.ipsec_user).setVisibility(0);
                this.mView.findViewById(R.id.ipsec_peer).setVisibility(0);
                break;
            case 5:
                this.mView.findViewById(R.id.ipsec_peer).setVisibility(0);
                break;
        }
    }

    private boolean validate(boolean editing) {
        if (!editing) {
            return (this.mUsername.getText().length() == 0 || this.mPassword.getText().length() == 0) ? false : true;
        }
        if (this.mName.getText().length() == 0 || this.mServer.getText().length() == 0 || !validateAddresses(this.mDnsServers.getText().toString(), false) || !validateAddresses(this.mRoutes.getText().toString(), true)) {
            return false;
        }
        switch (this.mType.getSelectedItemPosition()) {
            case 0:
            case 5:
                return true;
            case 1:
            case 3:
                return this.mIpsecSecret.getText().length() != 0;
            case 2:
            case 4:
                return this.mIpsecUserCert.getSelectedItemPosition() != 0;
            default:
                return false;
        }
    }

    private boolean validateAddresses(String addresses, boolean cidr) {
        try {
            String[] arr$ = addresses.split(" ");
            for (String address : arr$) {
                if (!address.isEmpty()) {
                    int prefixLength = 32;
                    if (cidr) {
                        String[] parts = address.split("/", 2);
                        address = parts[0];
                        prefixLength = Integer.parseInt(parts[1]);
                    }
                    byte[] bytes = InetAddress.parseNumericAddress(address).getAddress();
                    int integer = (bytes[3] & 255) | ((bytes[2] & 255) << 8) | ((bytes[1] & 255) << 16) | ((bytes[0] & 255) << 24);
                    if (bytes.length != 4 || prefixLength < 0 || prefixLength > 32 || (prefixLength < 32 && (integer << prefixLength) != 0)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadCertificates(Spinner spinner, String prefix, int firstId, String selected) {
        String[] certificates;
        Context context = getContext();
        String first = firstId == 0 ? "" : context.getString(firstId);
        String[] certificates2 = this.mKeyStore.saw(prefix);
        if (certificates2 == null || certificates2.length == 0) {
            certificates = new String[]{first};
        } else {
            String[] array = new String[certificates2.length + 1];
            array[0] = first;
            System.arraycopy(certificates2, 0, array, 1, certificates2.length);
            certificates = array;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        for (int i = 1; i < certificates.length; i++) {
            if (certificates[i].equals(selected)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    boolean isEditing() {
        return this.mEditing;
    }

    VpnProfile getProfile() {
        VpnProfile profile = new VpnProfile(this.mProfile.key);
        profile.name = this.mName.getText().toString();
        profile.type = this.mType.getSelectedItemPosition();
        profile.server = this.mServer.getText().toString().trim();
        profile.username = this.mUsername.getText().toString();
        profile.password = this.mPassword.getText().toString();
        profile.searchDomains = this.mSearchDomains.getText().toString().trim();
        profile.dnsServers = this.mDnsServers.getText().toString().trim();
        profile.routes = this.mRoutes.getText().toString().trim();
        switch (profile.type) {
            case 0:
                profile.mppe = this.mMppe.isChecked();
                break;
            case 1:
                profile.l2tpSecret = this.mL2tpSecret.getText().toString();
                profile.ipsecIdentifier = this.mIpsecIdentifier.getText().toString();
                profile.ipsecSecret = this.mIpsecSecret.getText().toString();
                break;
            case 2:
                profile.l2tpSecret = this.mL2tpSecret.getText().toString();
                if (this.mIpsecUserCert.getSelectedItemPosition() != 0) {
                    profile.ipsecUserCert = (String) this.mIpsecUserCert.getSelectedItem();
                }
                if (this.mIpsecCaCert.getSelectedItemPosition() != 0) {
                    profile.ipsecCaCert = (String) this.mIpsecCaCert.getSelectedItem();
                }
                if (this.mIpsecServerCert.getSelectedItemPosition() != 0) {
                    profile.ipsecServerCert = (String) this.mIpsecServerCert.getSelectedItem();
                }
                break;
            case 3:
                profile.ipsecIdentifier = this.mIpsecIdentifier.getText().toString();
                profile.ipsecSecret = this.mIpsecSecret.getText().toString();
                break;
            case 4:
                if (this.mIpsecUserCert.getSelectedItemPosition() != 0) {
                }
                if (this.mIpsecCaCert.getSelectedItemPosition() != 0) {
                }
                if (this.mIpsecServerCert.getSelectedItemPosition() != 0) {
                }
                break;
            case 5:
                if (this.mIpsecCaCert.getSelectedItemPosition() != 0) {
                }
                if (this.mIpsecServerCert.getSelectedItemPosition() != 0) {
                }
                break;
        }
        profile.saveLogin = this.mSaveLogin.isChecked();
        return profile;
    }
}
