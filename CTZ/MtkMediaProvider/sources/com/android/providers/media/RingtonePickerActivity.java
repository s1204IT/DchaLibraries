package com.android.providers.media;

import android.content.ContentProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import java.io.IOException;
import java.util.regex.Pattern;

public final class RingtonePickerActivity extends AlertActivity implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, AlertController.AlertParams.OnPrepareListViewListener, Runnable {
    private static Ringtone sPlayingRingtone;
    private BadgedRingtoneAdapter mAdapter;
    private int mAttributesFlags;
    private Ringtone mCurrentRingtone;
    private Cursor mCursor;
    private Ringtone mDefaultRingtone;
    private Uri mExistingUri;
    private Handler mHandler;
    private boolean mHasDefaultItem;
    private boolean mHasSilentItem;
    private int mPickerUserId;
    private RingtoneManager mRingtoneManager;
    private boolean mShowOkCancelButtons;
    private int mStaticItemCount;
    private Context mTargetContext;
    private int mType;
    private Uri mUriForDefaultItem;
    private int mSilentPos = -1;
    private int mDefaultRingtonePos = -1;
    private int mSampleRingtonePos = -1;
    private long mCheckedItemId = -1;
    private DialogInterface.OnClickListener mRingtoneClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i != RingtonePickerActivity.this.mCursor.getCount() + RingtonePickerActivity.this.mStaticItemCount) {
                RingtonePickerActivity.this.setCheckedItem(i);
                if (!RingtonePickerActivity.this.mShowOkCancelButtons) {
                    RingtonePickerActivity.this.setSuccessResultWithRingtone(RingtonePickerActivity.this.getCurrentlySelectedRingtoneUri());
                }
                RingtonePickerActivity.this.playRingtone(i, 0);
                return;
            }
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("audio/*");
            intent.putExtra("android.intent.extra.MIME_TYPES", new String[]{"audio/*", "application/ogg"});
            intent.putExtra("android.intent.extra.drm_level", 1);
            RingtonePickerActivity.this.startActivityForResult(intent, 300);
        }
    };

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mHandler = new Handler();
        Intent intent = getIntent();
        this.mPickerUserId = UserHandle.myUserId();
        this.mTargetContext = this;
        this.mType = intent.getIntExtra("android.intent.extra.ringtone.TYPE", -1);
        initRingtoneManager();
        this.mHasDefaultItem = intent.getBooleanExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true);
        this.mUriForDefaultItem = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.DEFAULT_URI");
        if (this.mUriForDefaultItem == null) {
            if (this.mType == 2) {
                this.mUriForDefaultItem = Settings.System.DEFAULT_NOTIFICATION_URI;
            } else if (this.mType == 4) {
                this.mUriForDefaultItem = Settings.System.DEFAULT_ALARM_ALERT_URI;
            } else if (this.mType == 1) {
                this.mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
            } else {
                this.mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
            }
        }
        this.mHasSilentItem = intent.getBooleanExtra("android.intent.extra.ringtone.SHOW_SILENT", true);
        this.mAttributesFlags |= intent.getIntExtra("android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS", 0);
        this.mShowOkCancelButtons = getResources().getBoolean(R.bool.config_showOkCancelButtons);
        setVolumeControlStream(this.mRingtoneManager.inferStreamType());
        this.mExistingUri = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.EXISTING_URI");
        this.mAdapter = new BadgedRingtoneAdapter(this, this.mCursor, UserManager.get(this).isManagedProfile(this.mPickerUserId));
        if (bundle != null) {
            setCheckedItem(bundle.getInt("clicked_pos", -1));
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mAdapter = this.mAdapter;
        alertParams.mOnClickListener = this.mRingtoneClickListener;
        alertParams.mLabelColumn = "title";
        alertParams.mIsSingleChoice = true;
        alertParams.mOnItemSelectedListener = this;
        if (this.mShowOkCancelButtons) {
            alertParams.mPositiveButtonText = getString(android.R.string.ok);
            alertParams.mPositiveButtonListener = this;
            alertParams.mNegativeButtonText = getString(android.R.string.cancel);
            alertParams.mPositiveButtonListener = this;
        }
        alertParams.mOnPrepareListViewListener = this;
        alertParams.mTitle = intent.getCharSequenceExtra("android.intent.extra.ringtone.TITLE");
        if (alertParams.mTitle == null) {
            if (this.mType == 4) {
                alertParams.mTitle = getString(android.R.string.launch_warning_original);
            } else if (this.mType == 2) {
                alertParams.mTitle = getString(android.R.string.launch_warning_replace);
            } else {
                alertParams.mTitle = getString(android.R.string.launchBrowserDefault);
            }
        }
        Log.d("RingtonePickerActivity", "onCreate: mHasDefaultItem = " + this.mHasDefaultItem + ", mUriForDefaultItem = " + this.mUriForDefaultItem + ", mHasSilentItem = " + this.mHasSilentItem + ", mType = " + this.mType + ", mExistingUri = " + this.mExistingUri);
        setupAlert();
    }

    public void onSaveInstanceState(Bundle bundle) {
        int checkedItem = getCheckedItem();
        super.onSaveInstanceState(bundle);
        bundle.putInt("clicked_pos", checkedItem);
    }

    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 300 && i2 == -1) {
            new AsyncTask<Uri, Void, Uri>() {
                @Override
                protected Uri doInBackground(Uri... uriArr) {
                    try {
                        return RingtonePickerActivity.this.mRingtoneManager.addCustomExternalRingtone(uriArr[0], RingtonePickerActivity.this.mType);
                    } catch (IOException | IllegalArgumentException e) {
                        Log.e("RingtonePickerActivity", "Unable to add new ringtone", e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Uri uri) {
                    if (uri != null) {
                        RingtonePickerActivity.this.requeryForAdapter();
                        Log.v("RingtonePickerActivity", "onActivityResult: RESULT_OK,ringtoneUri so set to be ringtone! " + uri);
                        return;
                    }
                    Toast.makeText((Context) RingtonePickerActivity.this, R.string.unable_to_add_ringtone, 0).show();
                }
            }.execute(intent.getData());
        }
    }

    public void onPrepareListView(ListView listView) {
        this.mStaticItemCount = 0;
        Log.d("RingtonePickerActivity", "onPrepareListView>>>: mClickedPos = " + getCheckedItem());
        if (this.mHasDefaultItem) {
            this.mDefaultRingtonePos = addDefaultRingtoneItem(listView);
            if (getCheckedItem() == -1 && RingtoneManager.isDefault(this.mExistingUri)) {
                setCheckedItem(this.mDefaultRingtonePos);
            }
        }
        if (this.mHasSilentItem) {
            this.mSilentPos = addSilentItem(listView);
            if (getCheckedItem() == -1 && this.mExistingUri == null) {
                setCheckedItem(this.mSilentPos);
            }
        }
        if (getCheckedItem() == -1) {
            setCheckedItem(getListPosition(this.mRingtoneManager.getRingtonePosition(this.mExistingUri)));
        }
        Log.d("RingtonePickerActivity", "onPrepareListView<<<: mClickedPos = " + getCheckedItem() + ", mExistingUri = " + this.mExistingUri);
        if (!this.mShowOkCancelButtons) {
            setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
        }
        if (Environment.getExternalStorageState().equals("mounted")) {
            addNewRingtoneItem(listView);
        }
        registerForContextMenu(listView);
    }

    private void requeryForAdapter() {
        int listPosition;
        initRingtoneManager();
        this.mAdapter.changeCursor(this.mCursor);
        int i = 0;
        while (true) {
            if (i >= this.mAdapter.getCount()) {
                listPosition = -1;
                break;
            } else if (this.mAdapter.getItemId(i) != this.mCheckedItemId) {
                i++;
            } else {
                listPosition = getListPosition(i);
                break;
            }
        }
        if (this.mHasSilentItem && listPosition == -1) {
            listPosition = this.mSilentPos;
        }
        setCheckedItem(listPosition);
        setupAlert();
    }

    private int addStaticItem(ListView listView, int i) {
        TextView textView = (TextView) getLayoutInflater().inflate(android.R.layout.notification_template_material_messaging_compact_heads_up, (ViewGroup) listView, false);
        textView.setText(i);
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
        return addStaticItem(listView, android.R.string.launch_warning_title);
    }

    private void addNewRingtoneItem(ListView listView) {
        listView.addFooterView(getLayoutInflater().inflate(R.layout.add_ringtone_item, (ViewGroup) listView, false));
    }

    private void initRingtoneManager() {
        this.mRingtoneManager = new RingtoneManager(this, true);
        if (this.mType != -1) {
            this.mRingtoneManager.setType(this.mType);
        }
        this.mCursor = new LocalizedCursor(this.mRingtoneManager.getCursor(), getResources(), "title");
    }

    private int getCheckedItem() {
        return this.mAlertParams.mCheckedItem;
    }

    private void setCheckedItem(int i) {
        this.mAlertParams.mCheckedItem = i;
        this.mCheckedItemId = this.mAdapter.getItemId(getRingtoneManagerPosition(i));
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mCursor == null) {
            return;
        }
        boolean z = i == -1;
        this.mRingtoneManager.stopPreviousRingtone();
        if (z) {
            setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
        } else {
            setResult(0);
        }
        finish();
    }

    @Override
    public void onItemSelected(AdapterView adapterView, View view, int i, long j) {
        playRingtone(i, 300);
        if (!this.mShowOkCancelButtons) {
            setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
        }
    }

    @Override
    public void onNothingSelected(AdapterView adapterView) {
    }

    private void playRingtone(int i, int i2) {
        this.mHandler.removeCallbacks(this);
        this.mSampleRingtonePos = i;
        this.mHandler.postDelayed(this, i2);
    }

    @Override
    public void run() {
        Ringtone ringtone;
        stopAnyPlayingRingtone();
        if (this.mSampleRingtonePos == this.mSilentPos) {
            return;
        }
        if (this.mDefaultRingtone != null && this.mDefaultRingtone.isPlaying()) {
            this.mDefaultRingtone.stop();
            this.mDefaultRingtone = null;
        }
        if (this.mSampleRingtonePos == this.mDefaultRingtonePos) {
            if (this.mUriForDefaultItem.toString().isEmpty()) {
                return;
            }
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
            if (this.mAttributesFlags != 0) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder(ringtone.getAudioAttributes()).setFlags(this.mAttributesFlags).build());
            }
            ringtone.play();
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

    private void setSuccessResultWithRingtone(Uri uri) {
        setResult(-1, new Intent().putExtra("android.intent.extra.ringtone.PICKED_URI", uri));
    }

    private Uri getCurrentlySelectedRingtoneUri() {
        if (getCheckedItem() == this.mDefaultRingtonePos) {
            Uri uri = this.mUriForDefaultItem;
            if (this.mDefaultRingtonePos == -1) {
                Log.w("RingtonePickerActivity", "onClick with no list item, set uri to be null! mDefaultRingtonePos = " + this.mDefaultRingtonePos);
                return null;
            }
            return uri;
        }
        if (getCheckedItem() == this.mSilentPos) {
            Log.w("RingtonePickerActivity", "onClick ok,  mSilentPos = " + ((Object) null));
            return null;
        }
        Uri ringtoneUri = this.mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(getCheckedItem()));
        Log.w("RingtonePickerActivity", "onClick ok,  checkeduri = " + ringtoneUri);
        return ringtoneUri;
    }

    private void saveAnyPlayingRingtone() {
        if (this.mDefaultRingtone != null && this.mDefaultRingtone.isPlaying()) {
            Log.d("RingtonePickerActivity", "saveAnyPlayingRingtone>>>:  mDefaultRingtone " + this.mDefaultRingtone);
            sPlayingRingtone = this.mDefaultRingtone;
            return;
        }
        if (this.mCurrentRingtone != null && this.mCurrentRingtone.isPlaying()) {
            Log.d("RingtonePickerActivity", "saveAnyPlayingRingtone>>>:   mCurrentRingtone " + this.mCurrentRingtone);
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

    private int getRingtoneManagerPosition(int i) {
        return i - this.mStaticItemCount;
    }

    private int getListPosition(int i) {
        return i < 0 ? i : i + this.mStaticItemCount;
    }

    protected void onRestoreInstanceState(Bundle bundle) {
        int checkedItem = getCheckedItem();
        super.onRestoreInstanceState(bundle);
        setCheckedItem(bundle.getInt("clicked_pos", checkedItem));
    }

    protected void onDestroy() {
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mCursor != null && !this.mCursor.isClosed()) {
            this.mCursor.close();
            this.mCursor = null;
        }
        super.onDestroy();
    }

    private static class LocalizedCursor extends CursorWrapper {
        String mNamePrefix;
        final Resources mResources;
        final Pattern mSanitizePattern;
        final int mTitleIndex;

        LocalizedCursor(Cursor cursor, Resources resources, String str) {
            super(cursor);
            this.mTitleIndex = this.mCursor.getColumnIndex(str);
            this.mResources = resources;
            this.mSanitizePattern = Pattern.compile("[^a-zA-Z0-9]");
            if (this.mTitleIndex == -1) {
                Log.e("RingtonePickerActivity", "No index for column " + str);
                this.mNamePrefix = null;
                return;
            }
            try {
                this.mNamePrefix = String.format("%s:%s/%s", this.mResources.getResourcePackageName(R.string.notification_sound_default), this.mResources.getResourceTypeName(R.string.notification_sound_default), "sound_name_");
            } catch (Resources.NotFoundException e) {
                this.mNamePrefix = null;
            }
        }

        private String sanitize(String str) {
            if (str == null) {
                return "";
            }
            return this.mSanitizePattern.matcher(str).replaceAll("_").toLowerCase();
        }

        @Override
        public String getString(int i) {
            String string = this.mCursor.getString(i);
            if (i != this.mTitleIndex || this.mNamePrefix == null) {
                return string;
            }
            TypedValue typedValue = new TypedValue();
            try {
                this.mResources.getValue(this.mNamePrefix + sanitize(string), typedValue, false);
                if (typedValue.type == 3) {
                    Log.d("RingtonePickerActivity", String.format("Replacing name %s with %s", string, typedValue.string.toString()));
                    return typedValue.string.toString();
                }
                Log.e("RingtonePickerActivity", "Invalid value when looking up localized name, using " + string);
                return string;
            } catch (Resources.NotFoundException e) {
                return string;
            }
        }
    }

    private class BadgedRingtoneAdapter extends CursorAdapter {
        private final boolean mIsManagedProfile;

        public BadgedRingtoneAdapter(Context context, Cursor cursor, boolean z) {
            super(context, cursor);
            this.mIsManagedProfile = z;
        }

        @Override
        public long getItemId(int i) {
            if (i < 0) {
                return i;
            }
            return super.getItemId(i);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.radio_with_work_badge, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            boolean z = true;
            ((TextView) view.findViewById(R.id.checked_text_view)).setText(cursor.getString(1));
            if (this.mIsManagedProfile) {
                Uri ringtoneUri = RingtonePickerActivity.this.mRingtoneManager.getRingtoneUri(cursor.getPosition());
                int userIdFromUri = ContentProvider.getUserIdFromUri(ringtoneUri, RingtonePickerActivity.this.mPickerUserId);
                Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(ringtoneUri);
                if (userIdFromUri != RingtonePickerActivity.this.mPickerUserId || !uriWithoutUserId.toString().startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    z = false;
                }
            }
            ImageView imageView = (ImageView) view.findViewById(R.id.work_icon);
            if (z) {
                imageView.setImageDrawable(RingtonePickerActivity.this.getPackageManager().getUserBadgeForDensityNoBackground(UserHandle.of(RingtonePickerActivity.this.mPickerUserId), -1));
                imageView.setVisibility(0);
            } else {
                imageView.setVisibility(8);
            }
        }
    }
}
