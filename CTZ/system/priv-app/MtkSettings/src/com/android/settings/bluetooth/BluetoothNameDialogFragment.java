package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
abstract class BluetoothNameDialogFragment extends InstrumentedDialogFragment implements TextWatcher {
    private AlertDialog mAlertDialog;
    private boolean mDeviceNameEdited;
    private boolean mDeviceNameUpdated;
    EditText mDeviceNameView;
    private Button mOkButton;

    protected abstract String getDeviceName();

    protected abstract int getDialogTitle();

    protected abstract void setDeviceName(String str);

    BluetoothNameDialogFragment() {
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        Log.d("BluetoothNameDialogFragment", "onCreateDialog, getActivity() is " + getActivity());
        String deviceName = getDeviceName();
        if (bundle != null) {
            deviceName = bundle.getString("device_name", deviceName);
            this.mDeviceNameEdited = bundle.getBoolean("device_name_edited", false);
        }
        this.mAlertDialog = new AlertDialog.Builder(getActivity()).setTitle(getDialogTitle()).setView(createDialogView(deviceName)).setPositiveButton(R.string.bluetooth_rename_button, new DialogInterface.OnClickListener() { // from class: com.android.settings.bluetooth.-$$Lambda$BluetoothNameDialogFragment$pGuotXbZkr5ej_7pdbB840goZcw
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                BluetoothNameDialogFragment bluetoothNameDialogFragment = this.f$0;
                bluetoothNameDialogFragment.setDeviceName(bluetoothNameDialogFragment.mDeviceNameView.getText().toString());
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        this.mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.android.settings.bluetooth.-$$Lambda$BluetoothNameDialogFragment$UwiP0mVIJKgl9XIciCSL5BjtkO4
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                BluetoothNameDialogFragment.lambda$onCreateDialog$1(this.f$0, dialogInterface);
            }
        });
        return this.mAlertDialog;
    }

    public static /* synthetic */ void lambda$onCreateDialog$1(BluetoothNameDialogFragment bluetoothNameDialogFragment, DialogInterface dialogInterface) {
        InputMethodManager inputMethodManager;
        if (bluetoothNameDialogFragment.mDeviceNameView != null && bluetoothNameDialogFragment.mDeviceNameView.requestFocus() && (inputMethodManager = (InputMethodManager) bluetoothNameDialogFragment.getContext().getSystemService("input_method")) != null) {
            inputMethodManager.showSoftInput(bluetoothNameDialogFragment.mDeviceNameView, 1);
        }
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("device_name", this.mDeviceNameView.getText().toString());
        bundle.putBoolean("device_name_edited", this.mDeviceNameEdited);
    }

    private View createDialogView(String str) {
        View viewInflate = ((LayoutInflater) getActivity().getSystemService("layout_inflater")).inflate(R.layout.dialog_edittext, (ViewGroup) null);
        this.mDeviceNameView = (EditText) viewInflate.findViewById(R.id.edittext);
        this.mDeviceNameView.setFilters(new InputFilter[]{new BluetoothLengthDeviceNameFilter()});
        this.mDeviceNameView.setText(str);
        if (!TextUtils.isEmpty(str)) {
            this.mDeviceNameView.setSelection(str.length());
        }
        this.mDeviceNameView.addTextChangedListener(this);
        com.android.settings.Utils.setEditTextCursorPosition(this.mDeviceNameView);
        this.mDeviceNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.android.settings.bluetooth.BluetoothNameDialogFragment.1
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6) {
                    BluetoothNameDialogFragment.this.setDeviceName(textView.getText().toString());
                    BluetoothNameDialogFragment.this.mAlertDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        return viewInflate;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.mAlertDialog = null;
        this.mDeviceNameView = null;
        this.mOkButton = null;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mOkButton == null) {
            this.mOkButton = this.mAlertDialog.getButton(-1);
            this.mOkButton.setEnabled(this.mDeviceNameEdited);
        }
    }

    void updateDeviceName() {
        String deviceName = getDeviceName();
        if (deviceName != null) {
            this.mDeviceNameUpdated = true;
            this.mDeviceNameEdited = false;
            this.mDeviceNameView.setText(deviceName);
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        if (this.mDeviceNameUpdated) {
            this.mDeviceNameUpdated = false;
            this.mOkButton.setEnabled(false);
        } else {
            this.mDeviceNameEdited = true;
            if (this.mOkButton != null) {
                this.mOkButton.setEnabled(editable.toString().trim().length() != 0);
            }
        }
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }
}
