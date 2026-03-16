package com.android.providers.media;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public final class RingtonePickerActivity extends AlertActivity implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, AlertController.AlertParams.OnPrepareListViewListener, Runnable {
    private static Ringtone sPlayingRingtone;
    private Ringtone mCurrentRingtone;
    private Cursor mCursor;
    private Ringtone mDefaultRingtone;
    private Uri mExistingUri;
    private Handler mHandler;
    private boolean mHasDefaultItem;
    private boolean mHasSilentItem;
    private RingtoneManager mRingtoneManager;
    private int mStaticItemCount;
    private int mType;
    private Uri mUriForDefaultItem;
    private int mSilentPos = -1;
    private int mDefaultRingtonePos = -1;
    private int mClickedPos = -1;
    private int mSampleRingtonePos = -1;
    private DialogInterface.OnClickListener mRingtoneClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            RingtonePickerActivity.this.mClickedPos = which;
            RingtonePickerActivity.this.playRingtone(which, 0);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mHandler = new Handler();
        Intent intent = getIntent();
        this.mHasDefaultItem = intent.getBooleanExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true);
        this.mUriForDefaultItem = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.DEFAULT_URI");
        if (this.mUriForDefaultItem == null) {
            this.mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
        }
        if (savedInstanceState != null) {
            this.mClickedPos = savedInstanceState.getInt("clicked_pos", -1);
        }
        this.mHasSilentItem = intent.getBooleanExtra("android.intent.extra.ringtone.SHOW_SILENT", true);
        this.mRingtoneManager = new RingtoneManager((Activity) this);
        this.mType = intent.getIntExtra("android.intent.extra.ringtone.TYPE", -1);
        if (this.mType != -1) {
            this.mRingtoneManager.setType(this.mType);
        }
        this.mCursor = this.mRingtoneManager.getCursor();
        setVolumeControlStream(this.mRingtoneManager.inferStreamType());
        this.mExistingUri = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.EXISTING_URI");
        AlertController.AlertParams p = this.mAlertParams;
        p.mCursor = this.mCursor;
        p.mOnClickListener = this.mRingtoneClickListener;
        p.mLabelColumn = "title";
        p.mIsSingleChoice = true;
        p.mOnItemSelectedListener = this;
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mPositiveButtonListener = this;
        p.mOnPrepareListViewListener = this;
        p.mTitle = intent.getCharSequenceExtra("android.intent.extra.ringtone.TITLE");
        if (p.mTitle == null) {
            p.mTitle = getString(android.R.string.imProtocolCustom);
        }
        setupAlert();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("clicked_pos", this.mClickedPos);
    }

    public void onPrepareListView(ListView listView) {
        if (this.mHasDefaultItem) {
            this.mDefaultRingtonePos = addDefaultRingtoneItem(listView);
            if (this.mClickedPos == -1 && RingtoneManager.isDefault(this.mExistingUri)) {
                this.mClickedPos = this.mDefaultRingtonePos;
            }
        }
        if (this.mHasSilentItem) {
            this.mSilentPos = addSilentItem(listView);
            if (this.mClickedPos == -1 && this.mExistingUri == null) {
                this.mClickedPos = this.mSilentPos;
            }
        }
        if (this.mClickedPos == -1) {
            this.mClickedPos = getListPosition(this.mRingtoneManager.getRingtonePosition(this.mExistingUri));
        }
        this.mAlertParams.mCheckedItem = this.mClickedPos;
    }

    private int addStaticItem(ListView listView, int textResId) {
        TextView textView = (TextView) getLayoutInflater().inflate(android.R.layout.notification_2025_template_expanded_inbox, (ViewGroup) listView, false);
        textView.setText(textResId);
        listView.addHeaderView(textView);
        this.mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    private int addDefaultRingtoneItem(ListView listView) {
        if (this.mType == 2) {
            return addStaticItem(listView, R.string.notification_sound_default);
        }
        if (this.mType == 4) {
            return addStaticItem(listView, R.string.alarm_sound_default);
        }
        return addStaticItem(listView, R.string.ringtone_default);
    }

    private int addSilentItem(ListView listView) {
        return addStaticItem(listView, android.R.string.imProtocolAim);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Uri uri;
        boolean positiveResult = which == -1;
        this.mRingtoneManager.stopPreviousRingtone();
        if (positiveResult) {
            Intent resultIntent = new Intent();
            if (this.mClickedPos == this.mDefaultRingtonePos) {
                uri = this.mUriForDefaultItem;
            } else if (this.mClickedPos == this.mSilentPos) {
                uri = null;
            } else {
                uri = this.mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(this.mClickedPos));
            }
            resultIntent.putExtra("android.intent.extra.ringtone.PICKED_URI", uri);
            setResult(-1, resultIntent);
        } else {
            setResult(0);
        }
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(this);
        }
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                RingtonePickerActivity.this.mCursor.deactivate();
            }
        });
        finish();
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        playRingtone(position, 300);
    }

    @Override
    public void onNothingSelected(AdapterView parent) {
    }

    private void playRingtone(int position, int delayMs) {
        this.mHandler.removeCallbacks(this);
        this.mSampleRingtonePos = position;
        this.mHandler.postDelayed(this, delayMs);
    }

    @Override
    public void run() {
        Ringtone ringtone;
        if (!isFinishing()) {
            stopAnyPlayingRingtone();
            if (this.mSampleRingtonePos != this.mSilentPos) {
                if (this.mSampleRingtonePos == this.mDefaultRingtonePos) {
                    if (this.mDefaultRingtone == null) {
                        this.mDefaultRingtone = RingtoneManager.getRingtone(this, this.mUriForDefaultItem);
                    }
                    if (this.mDefaultRingtone != null) {
                        this.mDefaultRingtone.setStreamType(this.mRingtoneManager.inferStreamType());
                    }
                    ringtone = this.mDefaultRingtone;
                    this.mCurrentRingtone = null;
                } else {
                    ringtone = this.mRingtoneManager.getRingtone(getRingtoneManagerPosition(this.mSampleRingtonePos));
                    this.mCurrentRingtone = ringtone;
                }
                if (ringtone != null) {
                    ringtone.play();
                }
            }
        }
    }

    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        } else {
            saveAnyPlayingRingtone();
        }
    }

    protected void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        }
    }

    private void saveAnyPlayingRingtone() {
        if (this.mDefaultRingtone != null && this.mDefaultRingtone.isPlaying()) {
            sPlayingRingtone = this.mDefaultRingtone;
        } else if (this.mCurrentRingtone != null && this.mCurrentRingtone.isPlaying()) {
            sPlayingRingtone = this.mCurrentRingtone;
        }
    }

    private void stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone.isPlaying()) {
            sPlayingRingtone.stop();
        }
        sPlayingRingtone = null;
        if (this.mDefaultRingtone != null && this.mDefaultRingtone.isPlaying()) {
            this.mDefaultRingtone.stop();
        }
        if (this.mRingtoneManager != null) {
            this.mRingtoneManager.stopPreviousRingtone();
        }
    }

    private int getRingtoneManagerPosition(int listPos) {
        return listPos - this.mStaticItemCount;
    }

    private int getListPosition(int ringtoneManagerPos) {
        return ringtoneManagerPos < 0 ? ringtoneManagerPos : ringtoneManagerPos + this.mStaticItemCount;
    }
}
