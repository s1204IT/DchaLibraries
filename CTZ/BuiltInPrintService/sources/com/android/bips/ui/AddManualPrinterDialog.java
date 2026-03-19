package com.android.bips.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.bips.R;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.discovery.ManualDiscovery;
import java.util.regex.Pattern;

class AddManualPrinterDialog extends AlertDialog implements TextWatcher, View.OnKeyListener, TextView.OnEditorActionListener, ManualDiscovery.PrinterAddCallback {
    private final Activity mActivity;
    private Button mAddButton;
    private final ManualDiscovery mDiscovery;
    private TextView mHostnameView;
    private ProgressBar mProgressBar;
    private static final String TAG = AddManualPrinterDialog.class.getSimpleName();
    private static final Pattern FULL_URI_PATTERN = Pattern.compile("^(ipp[s]?://)?[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*(:[0-9]+)?(/.*)?$");

    AddManualPrinterDialog(Activity activity, ManualDiscovery manualDiscovery) {
        super(activity);
        this.mDiscovery = manualDiscovery;
        this.mActivity = activity;
    }

    @Override
    @SuppressLint({"InflateParams"})
    protected void onCreate(Bundle bundle) {
        setView(getLayoutInflater().inflate(R.layout.manual_printer_add, (ViewGroup) null));
        setTitle(R.string.add_printer_by_ip);
        setButton(-2, getContext().getString(android.R.string.cancel), (DialogInterface.OnClickListener) null);
        setButton(-1, getContext().getString(R.string.add), (DialogInterface.OnClickListener) null);
        super.onCreate(bundle);
        this.mAddButton = getButton(-1);
        this.mHostnameView = (TextView) findViewById(R.id.hostname);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progress);
        this.mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.addPrinter();
            }
        });
        this.mHostnameView.addTextChangedListener(this);
        this.mHostnameView.setOnEditorActionListener(this);
        this.mHostnameView.setOnKeyListener(this);
        openKeyboard(this.mHostnameView);
        updateButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mDiscovery.cancelAddManualPrinter(this);
    }

    private void openKeyboard(TextView textView) {
        Window window = getWindow();
        if (window != null) {
            window.setSoftInputMode(4);
        }
        textView.requestFocus();
        ((InputMethodManager) getContext().getSystemService("input_method")).showSoftInput(textView, 1);
    }

    private void updateButtonState() {
        this.mAddButton.setEnabled(FULL_URI_PATTERN.matcher(this.mHostnameView.getText().toString()).matches());
    }

    private void addPrinter() {
        Uri uri;
        this.mAddButton.setEnabled(false);
        this.mHostnameView.setEnabled(false);
        this.mProgressBar.setVisibility(0);
        String string = this.mHostnameView.getText().toString();
        if (string.contains("://")) {
            uri = Uri.parse(string);
        } else {
            uri = Uri.parse("://" + string);
        }
        this.mDiscovery.addManualPrinter(uri, this);
    }

    @Override
    public void onFound(DiscoveredPrinter discoveredPrinter, boolean z) {
        if (z) {
            dismiss();
            this.mActivity.finish();
        } else {
            error(getContext().getString(R.string.printer_not_supported));
        }
    }

    @Override
    public void onNotFound() {
        error(getContext().getString(R.string.no_printer_found));
    }

    private void error(String str) {
        this.mProgressBar.setVisibility(8);
        this.mHostnameView.setError(str);
        this.mHostnameView.setEnabled(true);
        openKeyboard(this.mHostnameView);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        updateButtonState();
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == 6 && this.mAddButton.isEnabled()) {
            addPrinter();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (i == 66 && this.mAddButton.isEnabled()) {
            addPrinter();
            return true;
        }
        return false;
    }
}
