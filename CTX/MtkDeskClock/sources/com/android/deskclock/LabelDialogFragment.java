package com.android.deskclock;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.provider.Alarm;

public class LabelDialogFragment extends DialogFragment {
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_LABEL = "arg_label";
    private static final String ARG_TAG = "arg_tag";
    private static final String ARG_TIMER_ID = "arg_timer_id";
    private static final String TAG = "label_dialog";
    private Alarm mAlarm;
    private AppCompatEditText mLabelBox;
    private String mTag;
    private int mTimerId;

    public interface AlarmLabelDialogHandler {
        void onDialogLabelSet(Alarm alarm, String str, String str2);
    }

    public static LabelDialogFragment newInstance(Alarm alarm, String str, String str2) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_LABEL, str);
        bundle.putParcelable(ARG_ALARM, alarm);
        bundle.putString(ARG_TAG, str2);
        LabelDialogFragment labelDialogFragment = new LabelDialogFragment();
        labelDialogFragment.setArguments(bundle);
        return labelDialogFragment;
    }

    public static LabelDialogFragment newInstance(Timer timer) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_LABEL, timer.getLabel());
        bundle.putInt(ARG_TIMER_ID, timer.getId());
        LabelDialogFragment labelDialogFragment = new LabelDialogFragment();
        labelDialogFragment.setArguments(bundle);
        return labelDialogFragment;
    }

    public static void show(FragmentManager fragmentManager, LabelDialogFragment labelDialogFragment) {
        if (fragmentManager == null || fragmentManager.isDestroyed()) {
            return;
        }
        fragmentManager.executePendingTransactions();
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        Fragment fragmentFindFragmentByTag = fragmentManager.findFragmentByTag(TAG);
        if (fragmentFindFragmentByTag != null) {
            fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
        }
        fragmentTransactionBeginTransaction.addToBackStack(null);
        labelDialogFragment.show(fragmentTransactionBeginTransaction, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mLabelBox != null) {
            bundle.putString(ARG_LABEL, this.mLabelBox.getText().toString());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments() == null ? Bundle.EMPTY : getArguments();
        this.mAlarm = (Alarm) arguments.getParcelable(ARG_ALARM);
        this.mTimerId = arguments.getInt(ARG_TIMER_ID, -1);
        this.mTag = arguments.getString(ARG_TAG);
        String string = arguments.getString(ARG_LABEL);
        if (bundle != null) {
            string = bundle.getString(ARG_LABEL, string);
        }
        AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setPositiveButton(android.R.string.ok, new OkListener()).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setMessage(R.string.label).create();
        Context context = alertDialogCreate.getContext();
        int iResolveColor = ThemeUtils.resolveColor(context, R.attr.colorControlActivated);
        int iResolveColor2 = ThemeUtils.resolveColor(context, R.attr.colorControlNormal);
        this.mLabelBox = new AppCompatEditText(context);
        this.mLabelBox.setSupportBackgroundTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_activated}, new int[0]}, new int[]{iResolveColor, iResolveColor2}));
        this.mLabelBox.setOnEditorActionListener(new ImeDoneListener());
        this.mLabelBox.addTextChangedListener(new TextChangeListener());
        this.mLabelBox.setSingleLine();
        this.mLabelBox.setInputType(16385);
        this.mLabelBox.setText(string);
        this.mLabelBox.selectAll();
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.label_edittext_padding);
        alertDialogCreate.setView(this.mLabelBox, dimensionPixelSize, 0, dimensionPixelSize, 0);
        Window window = alertDialogCreate.getWindow();
        if (window != null) {
            window.setSoftInputMode(4);
        }
        return alertDialogCreate;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mLabelBox.setOnEditorActionListener(null);
    }

    private void setLabel() {
        Timer timer;
        String string = this.mLabelBox.getText().toString();
        if (string.trim().isEmpty()) {
            string = com.google.android.flexbox.BuildConfig.FLAVOR;
        }
        if (this.mAlarm != null) {
            ((AlarmLabelDialogHandler) getActivity()).onDialogLabelSet(this.mAlarm, string, this.mTag);
        } else if (this.mTimerId >= 0 && (timer = DataModel.getDataModel().getTimer(this.mTimerId)) != null) {
            DataModel.getDataModel().setTimerLabel(timer, string);
        }
    }

    private class TextChangeListener implements TextWatcher {
        private TextChangeListener() {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            LabelDialogFragment.this.mLabelBox.setActivated(!TextUtils.isEmpty(charSequence));
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    private class ImeDoneListener implements TextView.OnEditorActionListener {
        private ImeDoneListener() {
        }

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (i == 6) {
                LabelDialogFragment.this.setLabel();
                LabelDialogFragment.this.dismissAllowingStateLoss();
                return true;
            }
            return false;
        }
    }

    private class OkListener implements DialogInterface.OnClickListener {
        private OkListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            LabelDialogFragment.this.setLabel();
            LabelDialogFragment.this.dismiss();
        }
    }
}
