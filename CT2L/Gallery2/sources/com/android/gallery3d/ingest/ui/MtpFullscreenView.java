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

    public MtpFullscreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPosition = -1;
    }

    public MtpFullscreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
    public void setChecked(boolean checked) {
        this.mCheckbox.setChecked(checked);
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

    public void setPositionAndBroker(int position, CheckBroker b) {
        if (this.mBroker != null) {
            this.mBroker.unregisterOnCheckedChangeListener(this);
        }
        this.mPosition = position;
        this.mBroker = b;
        if (this.mBroker != null) {
            setChecked(this.mBroker.isItemChecked(position));
            this.mBroker.registerOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
        if (this.mBroker != null) {
            this.mBroker.setItemChecked(this.mPosition, isChecked);
        }
    }

    @Override
    public void onCheckedChanged(int position, boolean isChecked) {
        if (position == this.mPosition) {
            setChecked(isChecked);
        }
    }

    @Override
    public void onBulkCheckedChanged() {
        if (this.mBroker != null) {
            setChecked(this.mBroker.isItemChecked(this.mPosition));
        }
    }
}
