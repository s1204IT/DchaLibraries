package com.android.gallery3d.filtershow.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
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
import java.io.File;

public class ExportDialog extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    Rect mCompressedBounds;
    int mCompressedSize;
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

        Watcher(EditText text) {
            this.mEditText = text;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            ExportDialog.this.textChanged(this.mEditText);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mHandler = new Handler(getActivity().getMainLooper());
        View view = inflater.inflate(R.layout.filtershow_export_dialog, container);
        this.mSeekBar = (SeekBar) view.findViewById(R.id.qualitySeekBar);
        this.mSeekVal = (TextView) view.findViewById(R.id.qualityTextView);
        this.mSliderLabel = getString(R.string.quality) + ": ";
        this.mSeekBar.setProgress(this.mQuality);
        this.mSeekVal.setText(this.mSliderLabel + this.mSeekBar.getProgress());
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mWidthText = (EditText) view.findViewById(R.id.editableWidth);
        this.mHeightText = (EditText) view.findViewById(R.id.editableHeight);
        this.mEstimatedSize = (TextView) view.findViewById(R.id.estimadedSize);
        this.mOriginalBounds = MasterImage.getImage().getOriginalBounds();
        ImagePreset preset = MasterImage.getImage().getPreset();
        this.mOriginalBounds = preset.finalGeometryRect(this.mOriginalBounds.width(), this.mOriginalBounds.height());
        this.mRatio = this.mOriginalBounds.width() / this.mOriginalBounds.height();
        this.mWidthText.setText("" + this.mOriginalBounds.width());
        this.mHeightText.setText("" + this.mOriginalBounds.height());
        this.mExportWidth = this.mOriginalBounds.width();
        this.mExportHeight = this.mOriginalBounds.height();
        this.mWidthText.addTextChangedListener(new Watcher(this.mWidthText));
        this.mHeightText.addTextChangedListener(new Watcher(this.mHeightText));
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.done).setOnClickListener(this);
        getDialog().setTitle(R.string.export_flattened);
        updateCompressionFactor();
        updateSize();
        return view;
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        this.mSeekVal.setText(this.mSliderLabel + arg1);
        this.mQuality = this.mSeekBar.getProgress();
        scheduleUpdateCompressionFactor();
    }

    private void scheduleUpdateCompressionFactor() {
        this.mHandler.removeCallbacks(this.mUpdateRunnable);
        this.mHandler.postDelayed(this.mUpdateRunnable, this.mUpdateDelay);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                dismiss();
                break;
            case R.id.done:
                FilterShowActivity activity = (FilterShowActivity) getActivity();
                Uri sourceUri = MasterImage.getImage().getUri();
                File dest = SaveImage.getNewFile(activity, activity.getSelectedImageUri());
                float scaleFactor = this.mExportWidth / this.mOriginalBounds.width();
                Intent processIntent = ProcessingService.getSaveIntent(activity, MasterImage.getImage().getPreset(), dest, activity.getSelectedImageUri(), sourceUri, true, this.mSeekBar.getProgress(), scaleFactor, false);
                activity.startService(processIntent);
                dismiss();
                break;
        }
    }

    public void updateCompressionFactor() {
        Bitmap bitmap = MasterImage.getImage().getFilteredImage();
        if (bitmap != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, this.mQuality, out);
            this.mCompressedSize = out.size();
            this.mCompressedBounds = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
    }

    public void updateSize() {
        if (this.mCompressedBounds != null) {
            float originalArea = this.mCompressedBounds.width() * this.mCompressedBounds.height();
            float newArea = this.mExportWidth * this.mExportHeight;
            float factor = originalArea / this.mCompressedSize;
            float compressedSize = newArea / factor;
            float size = ((compressedSize * this.mExportCompressionMargin) / 1024.0f) / 1024.0f;
            String estimatedSize = "" + (((int) (size * 100.0f)) / 100.0f) + " Mb";
            this.mEstimatedSize.setText(estimatedSize);
        }
    }

    private void textChanged(EditText text) {
        if (!this.mEditing) {
            this.mEditing = true;
            int width = 1;
            int height = 1;
            if (text.getId() == R.id.editableWidth) {
                if (this.mWidthText.getText() != null) {
                    String value = String.valueOf(this.mWidthText.getText());
                    if (value.length() > 0) {
                        width = Integer.parseInt(value);
                        if (width > this.mOriginalBounds.width()) {
                            width = this.mOriginalBounds.width();
                            this.mWidthText.setText("" + width);
                        }
                        if (width <= 0) {
                            width = (int) Math.ceil(this.mRatio);
                            this.mWidthText.setText("" + width);
                        }
                        height = (int) (width / this.mRatio);
                    }
                    this.mHeightText.setText("" + height);
                }
            } else if (text.getId() == R.id.editableHeight && this.mHeightText.getText() != null) {
                String value2 = String.valueOf(this.mHeightText.getText());
                if (value2.length() > 0) {
                    height = Integer.parseInt(value2);
                    if (height > this.mOriginalBounds.height()) {
                        height = this.mOriginalBounds.height();
                        this.mHeightText.setText("" + height);
                    }
                    if (height <= 0) {
                        height = 1;
                        this.mHeightText.setText("1");
                    }
                    width = (int) (height * this.mRatio);
                }
                this.mWidthText.setText("" + width);
            }
            this.mExportWidth = width;
            this.mExportHeight = height;
            updateSize();
            this.mEditing = false;
        }
    }
}
