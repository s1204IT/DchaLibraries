package com.android.settings.network;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import com.android.settings.R;

public class ApnPreference extends Preference implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private boolean mEditable;
    private boolean mProtectFromCheckedChange;
    private boolean mSelectable;
    private int mSubId;
    private static String mSelectedKey = null;
    private static CompoundButton mCurrentChecked = null;

    public ApnPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mSubId = -1;
        this.mProtectFromCheckedChange = false;
        this.mSelectable = true;
    }

    public ApnPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.apnPreferenceStyle);
    }

    public ApnPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.apn_radiobutton);
        if (viewFindViewById != null && (viewFindViewById instanceof RadioButton)) {
            RadioButton radioButton = (RadioButton) viewFindViewById;
            if (this.mSelectable) {
                radioButton.setOnCheckedChangeListener(this);
                boolean zEquals = getKey().equals(mSelectedKey);
                if (zEquals) {
                    mCurrentChecked = radioButton;
                    mSelectedKey = getKey();
                }
                this.mProtectFromCheckedChange = true;
                radioButton.setChecked(zEquals);
                this.mProtectFromCheckedChange = false;
                radioButton.setVisibility(0);
            } else {
                radioButton.setVisibility(8);
            }
        }
        View viewFindViewById2 = preferenceViewHolder.findViewById(R.id.text_layout);
        if (viewFindViewById2 != null && (viewFindViewById2 instanceof RelativeLayout)) {
            viewFindViewById2.setOnClickListener(this);
        }
    }

    public void setChecked() {
        mSelectedKey = getKey();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        Log.i("ApnPreference", "ID: " + getKey() + " :" + z);
        if (this.mProtectFromCheckedChange) {
            return;
        }
        if (z) {
            if (mCurrentChecked != null) {
                mCurrentChecked.setChecked(false);
            }
            mCurrentChecked = compoundButton;
            mSelectedKey = getKey();
            callChangeListener(mSelectedKey);
            return;
        }
        mCurrentChecked = null;
        mSelectedKey = null;
    }

    @Override
    public void onClick(View view) {
        Context context;
        if (view != null && R.id.text_layout == view.getId() && (context = getContext()) != null) {
            Intent intent = new Intent("android.intent.action.EDIT", ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, Integer.parseInt(getKey())));
            intent.putExtra("readOnly", !this.mEditable);
            intent.putExtra("sub_id", this.mSubId);
            context.startActivity(intent);
        }
    }

    @Override
    public void setSelectable(boolean z) {
        this.mSelectable = z;
    }

    public void setSubId(int i) {
        this.mSubId = i;
    }

    public void setApnEditable(boolean z) {
        Log.d("ApnPreference", "setApnEditable " + z);
        this.mEditable = z;
    }
}
