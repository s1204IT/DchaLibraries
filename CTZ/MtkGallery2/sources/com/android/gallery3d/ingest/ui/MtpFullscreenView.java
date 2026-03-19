package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.adapter.CheckBroker;

public class MtpFullscreenView extends RelativeLayout implements Checkable, CompoundButton.OnCheckedChangeListener, CheckBroker.OnCheckedChangedListener {
    private CheckBroker mBroker;
    private CheckBox mCheckbox;
    private MtpImageView mImageView;
    private int mPosition;

    public MtpFullscreenView(Context context) {
        super(context);
        this.mPosition = -1;
    }

    public MtpFullscreenView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPosition = -1;
    }

    public MtpFullscreenView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPosition = -1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mImageView = (MtpImageView) findViewById(R.id.ingest_fullsize_image);
        this.mCheckbox = (CheckBox) findViewById(R.id.ingest_fullsize_image_checkbox);
        this.mCheckbox.setOnCheckedChangeListener(this);
    }

    @Override
    public boolean isChecked() {
        return this.mCheckbox.isChecked();
    }

    @Override
    public void setChecked(boolean z) {
        this.mCheckbox.setChecked(z);
    }

    @Override
    public void toggle() {
        this.mCheckbox.toggle();
    }

    @Override
    public void onDetachedFromWindow() {
        setPositionAndBroker(-1, null);
        super.onDetachedFromWindow();
    }

    public MtpImageView getImageView() {
        return this.mImageView;
    }

    public void setPositionAndBroker(int i, CheckBroker checkBroker) {
        if (this.mBroker != null) {
            this.mBroker.unregisterOnCheckedChangeListener(this);
        }
        this.mPosition = i;
        this.mBroker = checkBroker;
        if (this.mBroker != null) {
            setChecked(this.mBroker.isItemChecked(i));
            this.mBroker.registerOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (this.mBroker != null) {
            this.mBroker.setItemChecked(this.mPosition, z);
        }
    }

    @Override
    public void onCheckedChanged(int i, boolean z) {
        if (i == this.mPosition) {
            setChecked(z);
        }
    }

    @Override
    public void onBulkCheckedChanged() {
        if (this.mBroker != null) {
            setChecked(this.mBroker.isItemChecked(this.mPosition));
        }
    }
}
