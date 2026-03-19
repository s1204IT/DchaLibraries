package com.android.gallery3d.filtershow.ui;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.ProcessingService;
import com.android.gallery3d.filtershow.tools.SaveImage;
import java.io.ByteArrayOutputStream;

public class ExportDialog extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    Rect mCompressedBounds;
    int mCompressedSize;
    Rect mDefaultBounds;
    TextView mEstimatedSize;
    Handler mHandler;
    EditText mHeightText;
    Rect mOriginalBounds;
    float mRatio;
    SeekBar mSeekBar;
    TextView mSeekVal;
    String mSliderLabel;
    EditText mWidthText;
    int mQuality = 95;
    int mExportWidth = 0;
    int mExportHeight = 0;
    float mExportCompressionMargin = 1.1f;
    boolean mEditing = false;
    int mUpdateDelay = 1000;
    Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            ExportDialog.this.updateCompressionFactor();
            ExportDialog.this.updateSize();
        }
    };

    private class Watcher implements TextWatcher {
        private EditText mEditText;

        Watcher(EditText editText) {
            this.mEditText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            ExportDialog.this.textChanged(this.mEditText);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (MasterImage.getImage().getFilteredImage() == null) {
            return null;
        }
        this.mHandler = new Handler(getActivity().getMainLooper());
        View viewInflate = layoutInflater.inflate(R.layout.filtershow_export_dialog, viewGroup);
        this.mSeekBar = (SeekBar) viewInflate.findViewById(R.id.qualitySeekBar);
        this.mSeekVal = (TextView) viewInflate.findViewById(R.id.qualityTextView);
        this.mSliderLabel = getString(R.string.quality) + ": ";
        this.mSeekBar.setProgress(this.mQuality);
        this.mSeekVal.setText(this.mSliderLabel + this.mSeekBar.getProgress());
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mWidthText = (EditText) viewInflate.findViewById(R.id.editableWidth);
        this.mHeightText = (EditText) viewInflate.findViewById(R.id.editableHeight);
        this.mEstimatedSize = (TextView) viewInflate.findViewById(R.id.estimadedSize);
        this.mOriginalBounds = MasterImage.getImage().getOriginalBounds();
        if (this.mOriginalBounds == null) {
            this.mOriginalBounds = this.mDefaultBounds;
        }
        ImagePreset preset = MasterImage.getImage().getPreset();
        if (preset != null) {
            this.mOriginalBounds = preset.finalGeometryRect(this.mOriginalBounds.width(), this.mOriginalBounds.height());
        }
        this.mRatio = this.mOriginalBounds.width() / this.mOriginalBounds.height();
        this.mWidthText.setText("" + this.mOriginalBounds.width());
        this.mHeightText.setText("" + this.mOriginalBounds.height());
        this.mExportWidth = this.mOriginalBounds.width();
        this.mExportHeight = this.mOriginalBounds.height();
        this.mWidthText.addTextChangedListener(new Watcher(this.mWidthText));
        this.mHeightText.addTextChangedListener(new Watcher(this.mHeightText));
        viewInflate.findViewById(R.id.cancel).setOnClickListener(this);
        viewInflate.findViewById(R.id.done).setOnClickListener(this);
        getDialog().setTitle(R.string.export_flattened);
        updateCompressionFactor();
        updateSize();
        return viewInflate;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        this.mSeekVal.setText(this.mSliderLabel + i);
        this.mQuality = this.mSeekBar.getProgress();
        scheduleUpdateCompressionFactor();
    }

    private void scheduleUpdateCompressionFactor() {
        this.mHandler.removeCallbacks(this.mUpdateRunnable);
        this.mHandler.postDelayed(this.mUpdateRunnable, this.mUpdateDelay);
    }

    @Override
    public void onClick(View view) throws Throwable {
        int id = view.getId();
        if (id == R.id.cancel) {
            dismiss();
            return;
        }
        if (id == R.id.done) {
            FilterShowActivity filterShowActivity = (FilterShowActivity) getActivity();
            filterShowActivity.startService(ProcessingService.getSaveIntent(filterShowActivity, MasterImage.getImage().getPreset(), SaveImage.getNewFile(filterShowActivity, filterShowActivity.getSelectedImageUri()), filterShowActivity.getSelectedImageUri(), MasterImage.getImage().getUri(), true, this.mSeekBar.getProgress(), this.mExportWidth / this.mOriginalBounds.width(), false));
            dismiss();
        }
    }

    public void updateCompressionFactor() {
        Bitmap filteredImage = MasterImage.getImage().getFilteredImage();
        if (filteredImage == null) {
            return;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        filteredImage.compress(Bitmap.CompressFormat.JPEG, this.mQuality, byteArrayOutputStream);
        this.mCompressedSize = byteArrayOutputStream.size();
        this.mCompressedBounds = new Rect(0, 0, filteredImage.getWidth(), filteredImage.getHeight());
    }

    public void updateSize() {
        if (this.mCompressedBounds == null) {
            return;
        }
        this.mEstimatedSize.setText("" + (((int) ((((((this.mExportWidth * this.mExportHeight) / ((this.mCompressedBounds.width() * this.mCompressedBounds.height()) / this.mCompressedSize)) * this.mExportCompressionMargin) / 1024.0f) / 1024.0f) * 100.0f)) / 100.0f) + " Mb");
    }

    private void textChanged(EditText editText) {
        int iHeight;
        int iCeil;
        if (this.mEditing) {
            return;
        }
        int i = 1;
        this.mEditing = true;
        if (editText.getId() == R.id.editableWidth) {
            if (this.mWidthText.getText() != null) {
                String strValueOf = String.valueOf(this.mWidthText.getText());
                String str = "" + this.mOriginalBounds.width();
                if (strValueOf.length() > str.length()) {
                    this.mWidthText.setText(str);
                    strValueOf = str;
                }
                if (strValueOf.length() > 0) {
                    iCeil = Integer.parseInt(strValueOf);
                    if (iCeil > this.mOriginalBounds.width()) {
                        iCeil = this.mOriginalBounds.width();
                        this.mWidthText.setText("" + iCeil);
                    }
                    if (iCeil <= 0) {
                        iCeil = (int) Math.ceil(this.mRatio);
                        this.mWidthText.setText("" + iCeil);
                    }
                    i = (int) (iCeil / this.mRatio);
                } else {
                    iCeil = 1;
                }
                this.mHeightText.setText("" + i);
                int i2 = i;
                i = iCeil;
                iHeight = i2;
            } else {
                iHeight = 1;
            }
        } else if (editText.getId() == R.id.editableHeight && this.mHeightText.getText() != null) {
            String strValueOf2 = String.valueOf(this.mHeightText.getText());
            String str2 = "" + this.mOriginalBounds.height();
            if (strValueOf2.length() > str2.length()) {
                this.mHeightText.setText(str2);
                strValueOf2 = str2;
            }
            if (strValueOf2.length() > 0) {
                iHeight = Integer.parseInt(strValueOf2);
                if (iHeight > this.mOriginalBounds.height()) {
                    iHeight = this.mOriginalBounds.height();
                    this.mHeightText.setText("" + iHeight);
                }
                if (iHeight <= 0) {
                    this.mHeightText.setText("1");
                    iHeight = 1;
                }
                i = (int) (iHeight * this.mRatio);
            } else {
                iHeight = 1;
            }
            this.mWidthText.setText("" + i);
        }
        this.mExportWidth = i;
        this.mExportHeight = iHeight;
        updateSize();
        this.mEditing = false;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putIntArray("original_rect", new int[]{this.mOriginalBounds.left, this.mOriginalBounds.top, this.mOriginalBounds.right, this.mOriginalBounds.bottom});
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            int[] intArray = bundle.getIntArray("original_rect");
            this.mDefaultBounds = new Rect(intArray[0], intArray[1], intArray[2], intArray[3]);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getView() == null) {
            dismissAllowingStateLoss();
        }
    }
}
