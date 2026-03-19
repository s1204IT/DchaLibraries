package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.mediatek.galleryportable.Log;
import com.mediatek.galleryportable.StorageManagerUtils;
import java.io.File;

public class ClearMotionTool extends Activity implements SeekBar.OnSeekBarChangeListener {
    public static final String ACTION_ClearMotionTool = "com.android.camera.action.ClearMotionTool";
    private static final int DEFAULTVALUE = 125;
    private static final int DEFAULTVALUEOFDEMOMODE = 0;
    private static final short MAX_VALUE = 256;
    private static final String TAG = "VP/ClearMotionTool";
    private static final int sDemooffParameter = 0;
    private static final int sHorizontalParameter = 2;
    private static final int sVerticalParameter = 1;
    private RadioGroup mGroup;
    private int mRange;
    private SeekBar mSeekBarSkinSat;
    private int mSkinSatRange;
    private TextView mTextViewSkinSat;
    private TextView mTextViewSkinSatProgress;
    private TextView mTextViewSkinSatRange;
    private static String[] sExtPath = null;
    private static final String sDemooff = Integer.toString(0);
    private static final String sVertical = Integer.toString(1);
    private static final String sHorizontal = Integer.toString(2);
    private final String BDR = "persist.clearMotion.fblevel.bdr";
    private final String BDR_NAME = "Fluency fine tune";
    private final String DEMOMODE = "persist.clearMotion.demoMode";
    private String fblevel_nrm = null;
    private int[] mClearMotionParameters = new int[2];
    private int[] mOldClearMotionParameters = new int[2];
    private String mStoragePath = null;
    private Context mContext = null;
    private String mBDRProgress = null;
    private String mDemoMode = null;
    private String mOldBDRProgress = null;
    private String mOldDemoMode = null;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(8);
        requestWindowFeature(9);
        this.mContext = this;
        sExtPath = StorageManagerUtils.getVolumnPaths((StorageManager) getSystemService("storage"));
        setContentView(R.layout.m_clear_motion_tool);
        getViewById();
        this.mRange = ClearMotionQualityJni.nativeGetFallbackRange();
        if (sExtPath != null) {
            int length = sExtPath.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    if (sExtPath[i] == null || !new File(sExtPath[i], "SUPPORT_CLEARMOTION").exists()) {
                        i++;
                    } else {
                        this.mStoragePath = sExtPath[i];
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        setValue();
    }

    private void setValue() {
        this.mSeekBarSkinSat.setMax(this.mRange);
        this.mTextViewSkinSatRange.setText(this.mRange + "");
        this.mSeekBarSkinSat.setOnSeekBarChangeListener(this);
        read();
        this.mOldClearMotionParameters[0] = this.mClearMotionParameters[0];
        this.mOldClearMotionParameters[1] = this.mClearMotionParameters[1];
        this.mOldBDRProgress = Integer.toString(this.mClearMotionParameters[0]);
        this.mOldDemoMode = Integer.toString(this.mClearMotionParameters[1]);
        Log.d(TAG, " <setValue> mOldBDRProgress==" + this.mOldBDRProgress + " mOldDemoMode=" + this.mOldDemoMode);
        try {
            if (this.mOldBDRProgress != null && this.mOldDemoMode != null) {
                this.mBDRProgress = this.mOldBDRProgress;
                this.mSeekBarSkinSat.setProgress(Integer.parseInt(this.mOldBDRProgress));
                this.mDemoMode = this.mOldDemoMode;
                if (this.mOldDemoMode.equals(sDemooff)) {
                    ((RadioButton) findViewById(R.id.demooff)).setChecked(true);
                } else if (this.mOldDemoMode.equals(sVertical)) {
                    ((RadioButton) findViewById(R.id.vertical)).setChecked(true);
                } else if (this.mOldDemoMode.equals(sHorizontal)) {
                    ((RadioButton) findViewById(R.id.horizontal)).setChecked(true);
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        this.mTextViewSkinSatProgress.setText("Fluency fine tune :" + this.mOldBDRProgress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_clearmotion_actionbar, menu);
        return true;
    }

    private void recoverIndex() {
        if (this.mOldBDRProgress != null) {
            write(this.mOldClearMotionParameters);
            Log.d(TAG, "<recoverIndex>  mOldBDRProgress=" + this.mOldClearMotionParameters[0] + "  mOldDemoMode = " + this.mOldClearMotionParameters[1]);
        }
    }

    private void onSaveClicked() {
        if (this.mBDRProgress != null) {
            write(this.mClearMotionParameters);
            Log.d(TAG, "<onSaveClicked>  mBDRProgress=" + this.mClearMotionParameters[0] + " mDemoMode = " + this.mClearMotionParameters[1]);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            finish();
            return true;
        }
        if (itemId == R.id.cancel) {
            recoverIndex();
            finish();
            return true;
        }
        if (itemId == R.id.save) {
            onSaveClicked();
            finish();
            return true;
        }
        return true;
    }

    private void setVisible(View view, int i) {
        if (view != null) {
            view.setVisibility(i);
        }
    }

    private void getViewById() {
        this.mGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        this.mGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (R.id.demooff == i) {
                    ClearMotionTool.this.mDemoMode = ClearMotionTool.sDemooff;
                    ClearMotionTool.this.mClearMotionParameters[1] = 0;
                } else if (R.id.vertical == i) {
                    ClearMotionTool.this.mDemoMode = ClearMotionTool.sVertical;
                    ClearMotionTool.this.mClearMotionParameters[1] = 1;
                } else if (R.id.horizontal == i) {
                    ClearMotionTool.this.mDemoMode = ClearMotionTool.sHorizontal;
                    ClearMotionTool.this.mClearMotionParameters[1] = 2;
                }
                Log.d(ClearMotionTool.TAG, "<getViewById>SystemProperties.set = " + ClearMotionTool.this.mClearMotionParameters[1]);
                ClearMotionTool.this.write(ClearMotionTool.this.mClearMotionParameters);
            }
        });
        this.mTextViewSkinSat = (TextView) findViewById(R.id.textView1_skinSat);
        this.mTextViewSkinSatRange = (TextView) findViewById(R.id.textView_skinSat);
        this.mTextViewSkinSatProgress = (TextView) findViewById(R.id.textView_skinSat_progress);
        this.mSeekBarSkinSat = (SeekBar) findViewById(R.id.seekBar_skinSat);
        this.mSeekBarSkinSat.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (this.mSeekBarSkinSat == seekBar) {
            this.mTextViewSkinSatProgress.setText("Fluency fine tune: " + i);
            this.mClearMotionParameters[0] = i;
        }
        Log.d(TAG, "<onProgressChanged>progress===" + i + "  onProgressChanged  mClearMotionParameters:" + this.mClearMotionParameters[0] + "  " + this.mClearMotionParameters[1]);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "<onProgressChanged>  mClearMotionParameters:" + this.mClearMotionParameters[0] + "  " + this.mClearMotionParameters[1]);
        write(this.mClearMotionParameters);
    }

    private void read() {
        this.mClearMotionParameters[0] = ClearMotionQualityJni.nativeGetFallbackIndex();
        this.mClearMotionParameters[1] = ClearMotionQualityJni.nativeGetDemoMode();
        Log.d(TAG, "<read> mClearMotionParameters[0]=" + this.mClearMotionParameters[0] + " mClearMotionParameters[1]=" + this.mClearMotionParameters[1]);
    }

    private void write(int[] iArr) {
        ClearMotionQualityJni.nativeSetFallbackIndex(iArr[0]);
        ClearMotionQualityJni.nativeSetDemoMode(iArr[1]);
    }
}
