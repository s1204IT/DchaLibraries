package com.android.deskclock;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.timer.TimerObj;

public class LabelDialogFragment extends DialogFragment {
    private EditText mLabelBox;

    interface AlarmLabelDialogHandler {
    }

    interface TimerLabelDialogHandler {
    }

    public static LabelDialogFragment newInstance(Alarm alarm, String label, String tag) {
        LabelDialogFragment frag = new LabelDialogFragment();
        Bundle args = new Bundle();
        args.putString("label", label);
        args.putParcelable("alarm", alarm);
        args.putString("tag", tag);
        frag.setArguments(args);
        return frag;
    }

    public static LabelDialogFragment newInstance(TimerObj timer, String label, String tag) {
        LabelDialogFragment frag = new LabelDialogFragment();
        Bundle args = new Bundle();
        args.putString("label", label);
        args.putParcelable("timer", timer);
        args.putString("tag", tag);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(1, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String label = bundle.getString("label");
        final Alarm alarm = (Alarm) bundle.getParcelable("alarm");
        final TimerObj timer = (TimerObj) bundle.getParcelable("timer");
        final String tag = bundle.getString("tag");
        View view = inflater.inflate(R.layout.label_dialog, container, false);
        this.mLabelBox = (EditText) view.findViewById(R.id.labelBox);
        this.mLabelBox.setText(label);
        this.mLabelBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != 6) {
                    return false;
                }
                LabelDialogFragment.this.set(alarm, timer, tag);
                return true;
            }
        });
        this.mLabelBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                LabelDialogFragment.this.setLabelBoxBackground(s == null || TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        setLabelBoxBackground(TextUtils.isEmpty(label));
        Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                LabelDialogFragment.this.dismiss();
            }
        });
        Button setButton = (Button) view.findViewById(R.id.setButton);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                LabelDialogFragment.this.set(alarm, timer, tag);
            }
        });
        getDialog().getWindow().setSoftInputMode(4);
        return view;
    }

    private void set(Alarm alarm, TimerObj timer, String tag) {
        String label = this.mLabelBox.getText().toString();
        if (label.trim().length() == 0) {
            label = "";
        }
        if (alarm != null) {
            set(alarm, tag, label);
        } else if (timer != null) {
            set(timer, tag, label);
        } else {
            LogUtils.e("No alarm or timer available.", new Object[0]);
        }
    }

    private void set(Alarm alarm, String tag, String label) {
        Activity activity = getActivity();
        if (activity instanceof AlarmLabelDialogHandler) {
            ((DeskClock) getActivity()).onDialogLabelSet(alarm, label, tag);
        } else {
            LogUtils.e("Error! Activities that use LabelDialogFragment must implement AlarmLabelDialogHandler", new Object[0]);
        }
        dismiss();
    }

    private void set(TimerObj timer, String tag, String label) {
        Activity activity = getActivity();
        if (activity instanceof TimerLabelDialogHandler) {
            ((DeskClock) getActivity()).onDialogLabelSet(timer, label, tag);
        } else {
            LogUtils.e("Error! Activities that use LabelDialogFragment must implement AlarmLabelDialogHandler or TimerLabelDialogHandler", new Object[0]);
        }
        dismiss();
    }

    private void setLabelBoxBackground(boolean emptyText) {
        this.mLabelBox.setBackgroundResource(emptyText ? R.drawable.bg_edittext_default : R.drawable.bg_edittext_activated);
    }
}
