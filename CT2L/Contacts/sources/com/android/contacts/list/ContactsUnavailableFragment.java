package com.android.contacts.list;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.list.ProviderStatusWatcher;

public class ContactsUnavailableFragment extends Fragment implements View.OnClickListener {
    private Button mAddAccountButton;
    private Button mCreateContactButton;
    private Button mImportContactsButton;
    private OnContactsUnavailableActionListener mListener;
    private TextView mMessageView;
    private ProgressBar mProgress;
    private ProviderStatusWatcher.Status mProviderStatus;
    private Button mRetryUpgradeButton;
    private TextView mSecondaryMessageView;
    private Button mUninstallAppsButton;
    private View mView;
    private int mNoContactsMsgResId = -1;
    private int mNSecNoContactsMsgResId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mView = inflater.inflate(R.layout.contacts_unavailable_fragment, (ViewGroup) null);
        this.mMessageView = (TextView) this.mView.findViewById(R.id.message);
        this.mSecondaryMessageView = (TextView) this.mView.findViewById(R.id.secondary_message);
        this.mCreateContactButton = (Button) this.mView.findViewById(R.id.create_contact_button);
        this.mCreateContactButton.setOnClickListener(this);
        this.mAddAccountButton = (Button) this.mView.findViewById(R.id.add_account_button);
        this.mAddAccountButton.setOnClickListener(this);
        this.mImportContactsButton = (Button) this.mView.findViewById(R.id.import_contacts_button);
        this.mImportContactsButton.setOnClickListener(this);
        this.mUninstallAppsButton = (Button) this.mView.findViewById(R.id.import_failure_uninstall_button);
        this.mUninstallAppsButton.setOnClickListener(this);
        this.mRetryUpgradeButton = (Button) this.mView.findViewById(R.id.import_failure_retry_button);
        this.mRetryUpgradeButton.setOnClickListener(this);
        this.mProgress = (ProgressBar) this.mView.findViewById(R.id.progress);
        if (this.mProviderStatus != null) {
            updateStatus(this.mProviderStatus);
        }
        return this.mView;
    }

    public void setOnContactsUnavailableActionListener(OnContactsUnavailableActionListener listener) {
        this.mListener = listener;
    }

    public void updateStatus(ProviderStatusWatcher.Status providerStatus) {
        this.mProviderStatus = providerStatus;
        if (this.mView != null) {
            switch (providerStatus.status) {
                case 1:
                    this.mMessageView.setText(R.string.upgrade_in_progress);
                    this.mMessageView.setGravity(1);
                    this.mMessageView.setVisibility(0);
                    this.mCreateContactButton.setVisibility(8);
                    this.mAddAccountButton.setVisibility(8);
                    this.mImportContactsButton.setVisibility(8);
                    this.mUninstallAppsButton.setVisibility(8);
                    this.mRetryUpgradeButton.setVisibility(8);
                    this.mProgress.setVisibility(0);
                    break;
                case 2:
                    String message = getResources().getString(R.string.upgrade_out_of_memory, providerStatus.data);
                    this.mMessageView.setText(message);
                    this.mMessageView.setGravity(8388611);
                    this.mMessageView.setVisibility(0);
                    this.mCreateContactButton.setVisibility(8);
                    this.mAddAccountButton.setVisibility(8);
                    this.mImportContactsButton.setVisibility(8);
                    this.mUninstallAppsButton.setVisibility(0);
                    this.mRetryUpgradeButton.setVisibility(0);
                    this.mProgress.setVisibility(8);
                    break;
                case 3:
                    this.mMessageView.setText(R.string.locale_change_in_progress);
                    this.mMessageView.setGravity(1);
                    this.mMessageView.setVisibility(0);
                    this.mCreateContactButton.setVisibility(8);
                    this.mAddAccountButton.setVisibility(8);
                    this.mImportContactsButton.setVisibility(8);
                    this.mUninstallAppsButton.setVisibility(8);
                    this.mRetryUpgradeButton.setVisibility(8);
                    this.mProgress.setVisibility(0);
                    break;
                case 4:
                    setMessageText(this.mNoContactsMsgResId, this.mNSecNoContactsMsgResId);
                    this.mCreateContactButton.setVisibility(0);
                    this.mAddAccountButton.setVisibility(0);
                    this.mImportContactsButton.setVisibility(0);
                    this.mUninstallAppsButton.setVisibility(8);
                    this.mRetryUpgradeButton.setVisibility(8);
                    this.mProgress.setVisibility(8);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (this.mListener != null) {
            switch (v.getId()) {
                case R.id.add_account_button:
                    this.mListener.onAddAccountAction();
                    break;
                case R.id.create_contact_button:
                    this.mListener.onCreateNewContactAction();
                    break;
                case R.id.import_contacts_button:
                    this.mListener.onImportContactsFromFileAction();
                    break;
                case R.id.import_failure_uninstall_button:
                    this.mListener.onFreeInternalStorageAction();
                    break;
                case R.id.import_failure_retry_button:
                    Context context = getActivity();
                    if (context != null) {
                        ProviderStatusWatcher.retryUpgrade(context);
                    }
                    break;
            }
        }
    }

    public void setMessageText(int resId, int secResId) {
        this.mNoContactsMsgResId = resId;
        this.mNSecNoContactsMsgResId = secResId;
        if (this.mMessageView != null && this.mProviderStatus != null && this.mProviderStatus.status == 4) {
            if (resId != -1) {
                this.mMessageView.setText(this.mNoContactsMsgResId);
                this.mMessageView.setGravity(1);
                this.mMessageView.setVisibility(0);
                if (secResId != -1) {
                    this.mSecondaryMessageView.setText(this.mNSecNoContactsMsgResId);
                    this.mSecondaryMessageView.setGravity(1);
                    this.mSecondaryMessageView.setVisibility(0);
                    return;
                }
                this.mSecondaryMessageView.setVisibility(4);
                return;
            }
            this.mSecondaryMessageView.setVisibility(8);
            this.mMessageView.setVisibility(8);
        }
    }
}
