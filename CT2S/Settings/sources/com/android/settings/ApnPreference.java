package com.android.settings;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.provider.Telephony;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

public class ApnPreference extends Preference implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private boolean mProtectFromCheckedChange;
    private boolean mSelectable;
    private static String mSelectedKey = null;
    private static CompoundButton mCurrentChecked = null;

    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mProtectFromCheckedChange = false;
        this.mSelectable = true;
    }

    public ApnPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.apnPreferenceStyle);
    }

    public ApnPreference(Context context) {
        this(context, null);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        View widget = view.findViewById(R.id.apn_radiobutton);
        if (widget != null && (widget instanceof RadioButton)) {
            RadioButton rb = (RadioButton) widget;
            if (this.mSelectable) {
                rb.setOnCheckedChangeListener(this);
                boolean isChecked = getKey().equals(mSelectedKey);
                if (isChecked) {
                    mCurrentChecked = rb;
                    mSelectedKey = getKey();
                }
                this.mProtectFromCheckedChange = true;
                rb.setChecked(isChecked);
                this.mProtectFromCheckedChange = false;
                rb.setVisibility(0);
            } else {
                rb.setVisibility(8);
            }
        }
        View textLayout = view.findViewById(R.id.text_layout);
        if (textLayout != null && (textLayout instanceof RelativeLayout)) {
            textLayout.setOnClickListener(this);
        }
        return view;
    }

    public void setChecked() {
        mSelectedKey = getKey();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i("ApnPreference", "ID: " + getKey() + " :" + isChecked);
        if (!this.mProtectFromCheckedChange) {
            if (isChecked) {
                if (mCurrentChecked != null) {
                    mCurrentChecked.setChecked(false);
                }
                mCurrentChecked = buttonView;
                mSelectedKey = getKey();
                callChangeListener(mSelectedKey);
                return;
            }
            mCurrentChecked = null;
            mSelectedKey = null;
        }
    }

    @Override
    public void onClick(View v) {
        Context context;
        if (v != null && R.id.text_layout == v.getId() && (context = getContext()) != null) {
            int pos = Integer.parseInt(getKey());
            Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
            context.startActivity(new Intent("android.intent.action.EDIT", url));
        }
    }

    @Override
    public void setSelectable(boolean selectable) {
        this.mSelectable = selectable;
    }
}
