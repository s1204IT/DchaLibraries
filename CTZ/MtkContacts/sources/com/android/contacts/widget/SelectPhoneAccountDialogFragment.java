package com.android.contacts.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneAccountCompat;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import java.util.List;

public class SelectPhoneAccountDialogFragment extends DialogFragment {
    private List<PhoneAccountHandle> mAccountHandles;
    private boolean mCanSetDefault;
    private boolean mIsDefaultChecked;
    private boolean mIsSelected;
    private SelectPhoneAccountListener mListener;
    private TelecomManager mTelecomManager;
    private int mTitleResId;

    public static class SelectPhoneAccountListener extends ResultReceiver {
        public SelectPhoneAccountListener() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (i == 1) {
                onPhoneAccountSelected((PhoneAccountHandle) bundle.getParcelable("extra_selected_account_handle"), bundle.getBoolean("extra_set_default"));
            } else if (i == 2) {
                onDialogDismissed();
            }
        }

        public void onPhoneAccountSelected(PhoneAccountHandle phoneAccountHandle, boolean z) {
        }

        public void onDialogDismissed() {
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("is_default_checked", this.mIsDefaultChecked);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        this.mTitleResId = getArguments().getInt("title_res_id");
        this.mCanSetDefault = getArguments().getBoolean("can_set_default");
        this.mAccountHandles = getArguments().getParcelableArrayList("account_handles");
        this.mListener = (SelectPhoneAccountListener) getArguments().getParcelable("listener");
        if (bundle != null) {
            this.mIsDefaultChecked = bundle.getBoolean("is_default_checked");
        }
        this.mIsSelected = false;
        this.mTelecomManager = (TelecomManager) getActivity().getSystemService("telecom");
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SelectPhoneAccountDialogFragment.this.mIsSelected = true;
                PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) SelectPhoneAccountDialogFragment.this.mAccountHandles.get(i);
                Bundle bundle2 = new Bundle();
                bundle2.putParcelable("extra_selected_account_handle", phoneAccountHandle);
                bundle2.putBoolean("extra_set_default", SelectPhoneAccountDialogFragment.this.mIsDefaultChecked);
                if (SelectPhoneAccountDialogFragment.this.mListener != null) {
                    SelectPhoneAccountDialogFragment.this.mListener.onReceiveResult(1, bundle2);
                }
            }
        };
        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                SelectPhoneAccountDialogFragment.this.mIsDefaultChecked = z;
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDialogCreate = builder.setTitle(this.mTitleResId).setAdapter(new SelectAccountListAdapter(builder.getContext(), R.layout.select_account_list_item, this.mAccountHandles), onClickListener).create();
        if (this.mCanSetDefault) {
            LinearLayout linearLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.default_account_checkbox, (ViewGroup) null);
            CheckBox checkBox = (CheckBox) linearLayout.findViewById(R.id.default_account_checkbox_view);
            checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            checkBox.setChecked(this.mIsDefaultChecked);
            alertDialogCreate.getListView().addFooterView(linearLayout);
        }
        return alertDialogCreate;
    }

    private class SelectAccountListAdapter extends ArrayAdapter<PhoneAccountHandle> {
        private int mResId;

        public SelectAccountListAdapter(Context context, int i, List<PhoneAccountHandle> list) {
            super(context, i, list);
            this.mResId = i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
            if (view == null) {
                view = layoutInflater.inflate(this.mResId, (ViewGroup) null);
                viewHolder = new ViewHolder();
                viewHolder.labelTextView = (TextView) view.findViewById(R.id.label);
                viewHolder.numberTextView = (TextView) view.findViewById(R.id.number);
                viewHolder.imageView = (ImageView) view.findViewById(R.id.icon);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            PhoneAccount phoneAccount = SelectPhoneAccountDialogFragment.this.mTelecomManager.getPhoneAccount(getItem(i));
            if (phoneAccount == null) {
                return view;
            }
            viewHolder.labelTextView.setText(phoneAccount.getLabel());
            if (phoneAccount.getAddress() == null || TextUtils.isEmpty(phoneAccount.getAddress().getSchemeSpecificPart())) {
                viewHolder.numberTextView.setVisibility(8);
            } else {
                viewHolder.numberTextView.setVisibility(0);
                viewHolder.numberTextView.setText(PhoneNumberUtilsCompat.createTtsSpannable(phoneAccount.getAddress().getSchemeSpecificPart()));
            }
            viewHolder.imageView.setImageDrawable(PhoneAccountCompat.createIconDrawable(phoneAccount, getContext()));
            return view;
        }

        private class ViewHolder {
            ImageView imageView;
            TextView labelTextView;
            TextView numberTextView;

            private ViewHolder() {
            }
        }
    }

    @Override
    public void onStop() {
        if (!this.mIsSelected && this.mListener != null) {
            this.mListener.onReceiveResult(2, null);
        }
        super.onStop();
    }
}
