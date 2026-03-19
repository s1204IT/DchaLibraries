package com.android.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;
import com.android.music.RepeatingImageButton;
import com.mediatek.omadrm.OmaDrmUtils;

public class MediaPlaybackActivity extends Activity implements NfcAdapter.CreateBeamUrisCallback, View.OnLongClickListener, View.OnTouchListener {
    private int lastX;
    private int lastY;
    private SubMenu mAddToPlaylistSubmenu;
    private ImageView mAlbum;
    private AlbumArtHandler mAlbumArtHandler;
    private Worker mAlbumArtWorker;
    private TextView mAlbumName;
    private TextView mArtistName;
    private TextView mCurrentTime;
    private boolean mDeviceHasDpad;
    private long mDuration;
    private boolean mIsLandscape;
    private long mLastSeekEventTime;
    private RepeatingImageButton mNextButton;
    NfcAdapter mNfcAdapter;
    private ImageButton mPauseButton;
    private RepeatingImageButton mPrevButton;
    private ProgressBar mProgress;
    private CharSequence mQueryText;
    private ImageButton mQueueButton;
    private MenuItem mQueueMenuItem;
    private ImageButton mRepeatButton;
    private MenuItem mRepeatMenuItem;
    MenuItem mSearchItem;
    private ImageButton mShuffleButton;
    private MenuItem mShuffleMenuItem;
    private Toast mToast;
    private MusicUtils.ServiceToken mToken;
    private TextView mTotalTime;
    private int mTouchSlop;
    private TextView mTrackName;
    private boolean paused;
    private int seekmethod;
    private boolean mSeeking = false;
    private long mStartSeekPos = 0;
    private IMediaPlaybackService mService = null;
    private boolean mIsShowAlbumArt = false;
    private Bitmap mArtBitmap = null;
    private long mArtSongId = -1;
    private String mPerformanceTestString = null;
    private int mRepeatCount = -1;
    private boolean mNeedUpdateDuration = true;
    private boolean mIsInBackgroud = false;
    private boolean mIsCallOnStop = false;
    private boolean mIsHotnotClicked = false;
    private boolean mIsConfigurationChanged = false;
    private HotKnotHelper mHotKnotHelper = null;
    private boolean mLongClickBlocked = false;
    private boolean mDurationHasChanged = false;
    int mInitialX = -1;
    int mLastX = -1;
    int mTextWidth = 0;
    int mViewWidth = 0;
    boolean mDraggingLabel = false;
    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message message) {
            TextView textView = (TextView) message.obj;
            int scrollX = (textView.getScrollX() * 3) / 4;
            textView.scrollTo(scrollX, 0);
            if (scrollX == 0) {
                textView.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                MediaPlaybackActivity.this.mLabelScroller.sendMessageDelayed(obtainMessage(0, textView), 15L);
            }
        }
    };
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            MediaPlaybackActivity.this.mFromTouch = true;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            if (z && MediaPlaybackActivity.this.mService != null && !MediaPlaybackActivity.this.mFromTouch) {
                MediaPlaybackActivity.this.mPosOverride = (MediaPlaybackActivity.this.mDuration * ((long) i)) / 1000;
                try {
                    MediaPlaybackActivity.this.mService.seek(MediaPlaybackActivity.this.mPosOverride);
                } catch (RemoteException e) {
                    MusicLogUtils.v("MediaPlayback", "Error:" + e);
                }
                MediaPlaybackActivity.this.refreshNow();
                MediaPlaybackActivity.this.mPosOverride = -1L;
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (MediaPlaybackActivity.this.mService != null) {
                try {
                    MediaPlaybackActivity.this.mPosOverride = (((long) seekBar.getProgress()) * MediaPlaybackActivity.this.mDuration) / 1000;
                    MediaPlaybackActivity.this.mService.seek(MediaPlaybackActivity.this.mPosOverride);
                    MediaPlaybackActivity.this.refreshNow();
                } catch (RemoteException e) {
                    MusicLogUtils.v("MediaPlayback", "Error:" + e);
                }
            }
            MediaPlaybackActivity.this.mPosOverride = -1L;
            MediaPlaybackActivity.this.mFromTouch = false;
        }
    };
    private View.OnClickListener mQueueListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MediaPlaybackActivity.this.startActivity(new Intent("android.intent.action.EDIT").setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track").putExtra("playlist", "nowplaying"));
        }
    };
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MediaPlaybackActivity.this.toggleShuffle();
        }
    };
    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MediaPlaybackActivity.this.cycleRepeat();
        }
    };
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MediaPlaybackActivity.this.doPauseResume();
        }
    };
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MusicLogUtils.v("MusicPerformanceTest", "[Performance test][Music] prev song start [" + System.currentTimeMillis() + "]");
            MediaPlaybackActivity.this.mPerformanceTestString = "prev song";
            MusicLogUtils.v("MediaPlayback", "Prev Button onClick,Send Msg");
            Message messageObtainMessage = MediaPlaybackActivity.this.mHandler.obtainMessage(7, null);
            MediaPlaybackActivity.this.mHandler.removeMessages(7);
            MediaPlaybackActivity.this.mHandler.sendMessage(messageObtainMessage);
        }
    };
    private View.OnClickListener mNextListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MusicLogUtils.v("MusicPerformanceTest", "[Performance test][Music] next song start [" + System.currentTimeMillis() + "]");
            MediaPlaybackActivity.this.mPerformanceTestString = "next song";
            MusicLogUtils.v("MediaPlayback", "Next Button onClick,Send Msg");
            Message messageObtainMessage = MediaPlaybackActivity.this.mHandler.obtainMessage(6, null);
            MediaPlaybackActivity.this.mHandler.removeMessages(6);
            MediaPlaybackActivity.this.mHandler.sendMessage(messageObtainMessage);
        }
    };
    private RepeatingImageButton.RepeatListener mRewListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(View view, long j, int i) {
            MusicLogUtils.v("MediaPlayback", "music backward");
            MediaPlaybackActivity.this.mRepeatCount = i;
            if (i != -1) {
                MediaPlaybackActivity.this.scanBackward(i, j);
                return;
            }
            Intent intent = new Intent(MediaPlaybackActivity.this.getApplicationContext(), (Class<?>) MediaPlaybackService.class);
            intent.setAction("com.android.music.musicservicecommand");
            intent.putExtra("command", "endscan");
            MediaPlaybackActivity.this.startService(intent);
        }
    };
    private RepeatingImageButton.RepeatListener mFfwdListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(View view, long j, int i) {
            MusicLogUtils.v("MediaPlayback", "music forward");
            MediaPlaybackActivity.this.mRepeatCount = i;
            if (i != -1) {
                MediaPlaybackActivity.this.scanForward(i, j);
                return;
            }
            Intent intent = new Intent(MediaPlaybackActivity.this.getApplicationContext(), (Class<?>) MediaPlaybackService.class);
            intent.setAction("com.android.music.musicservicecommand");
            intent.putExtra("command", "endscan");
            MediaPlaybackActivity.this.startService(intent);
        }
    };
    private final int[][] keyboard = {new int[]{45, 51, 33, 46, 48, 53, 49, 37, 43, 44}, new int[]{29, 47, 32, 34, 35, 36, 38, 39, 40, 67}, new int[]{54, 52, 31, 50, 30, 42, 41, 55, 56, 66}};
    private ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) throws Throwable {
            MediaPlaybackActivity.this.mService = IMediaPlaybackService.Stub.asInterface(iBinder);
            MediaPlaybackActivity.this.invalidateOptionsMenu();
            MediaPlaybackActivity.this.startPlayback();
            if (MediaPlaybackActivity.this.mService.getAudioId() < 0 && !MediaPlaybackActivity.this.mService.isPlaying() && MediaPlaybackActivity.this.mService.getPath() == null) {
                MediaPlaybackActivity.this.finish();
                return;
            }
            if (!MediaPlaybackActivity.this.mIsLandscape) {
                MediaPlaybackActivity.this.mRepeatButton.setVisibility(0);
                MediaPlaybackActivity.this.mShuffleButton.setVisibility(0);
                MediaPlaybackActivity.this.mQueueButton.setVisibility(0);
            }
            MediaPlaybackActivity.this.setRepeatButtonImage();
            MediaPlaybackActivity.this.setShuffleButtonImage();
            MediaPlaybackActivity.this.setPauseButtonImage();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            MediaPlaybackActivity.this.mService = null;
            MediaPlaybackActivity.this.finish();
        }
    };
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    MediaPlaybackActivity.this.queueNextRefresh(MediaPlaybackActivity.this.refreshNow());
                    break;
                case 2:
                    new AlertDialog.Builder(MediaPlaybackActivity.this).setTitle(R.string.service_start_error_title).setMessage(R.string.service_start_error_msg).setPositiveButton(R.string.service_start_error_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MediaPlaybackActivity.this.finish();
                        }
                    }).setCancelable(false).show();
                    break;
                case 4:
                    MediaPlaybackActivity.this.mAlbum.setImageBitmap((Bitmap) message.obj);
                    MediaPlaybackActivity.this.mAlbum.getDrawable().setDither(true);
                    break;
                case 6:
                    MusicLogUtils.v("MediaPlayback", "Next Handle");
                    if (MediaPlaybackActivity.this.mService != null) {
                        MediaPlaybackActivity.this.mNextButton.setEnabled(false);
                        MediaPlaybackActivity.this.mNextButton.setFocusable(false);
                        try {
                            MediaPlaybackActivity.this.mService.next();
                            MediaPlaybackActivity.this.mPosOverride = -1L;
                        } catch (RemoteException e) {
                            MusicLogUtils.v("MediaPlayback", "Error:" + e);
                        }
                        MediaPlaybackActivity.this.mNextButton.setEnabled(true);
                        MediaPlaybackActivity.this.mNextButton.setFocusable(true);
                        break;
                    }
                    break;
                case 7:
                    MusicLogUtils.v("MediaPlayback", "Prev Handle");
                    if (MediaPlaybackActivity.this.mService != null) {
                        MediaPlaybackActivity.this.mPrevButton.setEnabled(false);
                        MediaPlaybackActivity.this.mPrevButton.setFocusable(false);
                        try {
                            MediaPlaybackActivity.this.mPosOverride = -1L;
                            MediaPlaybackActivity.this.mService.prev();
                        } catch (RemoteException e2) {
                            MusicLogUtils.v("MediaPlayback", "Error:" + e2);
                        }
                        MediaPlaybackActivity.this.mPrevButton.setEnabled(true);
                        MediaPlaybackActivity.this.mPrevButton.setFocusable(true);
                        break;
                    }
                    break;
            }
        }
    };
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Throwable {
            String action = intent.getAction();
            if (action == null) {
                MusicLogUtils.v("MediaPlayback", "onReceive: action = null");
                return;
            }
            MusicLogUtils.v("MediaPlayback", "mStatusListener: " + action);
            if (action.equals("com.android.music.metachanged")) {
                MediaPlaybackActivity.this.invalidateOptionsMenu();
                MediaPlaybackActivity.this.updateTrackInfo();
                MediaPlaybackActivity.this.setPauseButtonImage();
                MusicLogUtils.v("MusicPerformanceTest", "[Performance test][Music] " + MediaPlaybackActivity.this.mPerformanceTestString + " end [" + System.currentTimeMillis() + "]");
                MusicLogUtils.v("MusicPerformanceTest", "[CMCC Performance test][Music] " + MediaPlaybackActivity.this.mPerformanceTestString + " end [" + System.currentTimeMillis() + "]");
                MediaPlaybackActivity.this.queueNextRefresh(1L);
                return;
            }
            if (action.equals("com.android.music.playstatechanged")) {
                MediaPlaybackActivity.this.setPauseButtonImage();
                return;
            }
            if (action.equals("com.android.music.quitplayback")) {
                MediaPlaybackActivity.this.mHandler.removeMessages(1);
                MediaPlaybackActivity.this.finish();
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                MusicLogUtils.v("MediaPlayback", "onReceive, stop refreshing ...");
                MediaPlaybackActivity.this.mHandler.removeMessages(1);
            } else if (action.equals("android.intent.action.SCREEN_ON")) {
                MusicLogUtils.v("MediaPlayback", "onReceive, restore refreshing ...");
                MediaPlaybackActivity.this.queueNextRefresh(MediaPlaybackActivity.this.refreshNow());
            }
        }
    };
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String str) {
            Intent intent = new Intent();
            intent.setClass(MediaPlaybackActivity.this, QueryBrowserActivity.class);
            intent.putExtra("query", str);
            MediaPlaybackActivity.this.startActivity(intent);
            MediaPlaybackActivity.this.mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String str) {
            return false;
        }
    };
    SearchView.OnSuggestionListener mOnSuggestionListener = new SearchView.OnSuggestionListener() {
        @Override
        public boolean onSuggestionClick(int i) {
            MusicLogUtils.v("MediaPlayback", "onSuggestionClick()");
            MediaPlaybackActivity.this.mSearchItem.collapseActionView();
            return false;
        }

        @Override
        public boolean onSuggestionSelect(int i) {
            MusicLogUtils.v("MediaPlayback", "onSuggestionSelect()");
            return false;
        }
    };

    @Override
    public void onCreate(Bundle bundle) throws Throwable {
        super.onCreate(bundle);
        setVolumeControlStream(3);
        this.mHotKnotHelper = new HotKnotHelper(this);
        this.mAlbumArtWorker = new Worker("album art worker");
        this.mAlbumArtHandler = new AlbumArtHandler(this.mAlbumArtWorker.getLooper());
        this.mIsLandscape = getResources().getConfiguration().orientation == 2;
        updateUI();
        this.mToken = MusicUtils.bindToService(this, this.osc);
        if (this.mToken == null) {
            this.mHandler.sendEmptyMessage(2);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.playstatechanged");
        intentFilter.addAction("com.android.music.metachanged");
        intentFilter.addAction("com.android.music.quitplayback");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(this.mStatusListener, new IntentFilter(intentFilter));
        updateTrackInfo();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (this.mNfcAdapter == null) {
            MusicLogUtils.v("MediaPlayback", "NFC not available!");
        }
    }

    TextView textViewForContainer(View view) {
        View viewFindViewById = view.findViewById(R.id.artistname);
        if (viewFindViewById != null) {
            return (TextView) viewFindViewById;
        }
        View viewFindViewById2 = view.findViewById(R.id.albumname);
        if (viewFindViewById2 != null) {
            return (TextView) viewFindViewById2;
        }
        View viewFindViewById3 = view.findViewById(R.id.trackname);
        if (viewFindViewById3 != null) {
            return (TextView) viewFindViewById3;
        }
        return null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        TextView textViewTextViewForContainer = textViewForContainer(view);
        if (textViewTextViewForContainer == null) {
            return false;
        }
        if (action == 0) {
            view.setBackgroundColor(getBackgroundColor());
            int x = (int) motionEvent.getX();
            this.mLastX = x;
            this.mInitialX = x;
            this.mDraggingLabel = false;
            this.mTextWidth = (int) textViewTextViewForContainer.getPaint().measureText(textViewTextViewForContainer.getText().toString());
            this.mViewWidth = textViewTextViewForContainer.getWidth();
            if (this.mTextWidth > this.mViewWidth) {
                textViewTextViewForContainer.setEllipsize(null);
            }
        } else if (action == 1 || action == 3) {
            view.setBackgroundColor(0);
            if (this.mDraggingLabel) {
                this.mLabelScroller.sendMessageDelayed(this.mLabelScroller.obtainMessage(0, textViewTextViewForContainer), 1000L);
            }
            textViewTextViewForContainer.setEllipsize(TextUtils.TruncateAt.END);
        } else if (action == 2) {
            if (this.mDraggingLabel) {
                int scrollX = textViewTextViewForContainer.getScrollX();
                int x2 = (int) motionEvent.getX();
                int i = this.mLastX - x2;
                if (i != 0) {
                    this.mLastX = x2;
                    int i2 = scrollX + i;
                    if (i2 > this.mTextWidth) {
                        i2 = (i2 - this.mTextWidth) - this.mViewWidth;
                    }
                    if (i2 < (-this.mViewWidth)) {
                        i2 = i2 + this.mViewWidth + this.mTextWidth;
                    }
                    textViewTextViewForContainer.scrollTo(i2, 0);
                }
                return true;
            }
            if (Math.abs(this.mInitialX - ((int) motionEvent.getX())) > this.mTouchSlop) {
                this.mLabelScroller.removeMessages(0, textViewTextViewForContainer);
                if (this.mViewWidth > this.mTextWidth) {
                    view.cancelLongPress();
                    return false;
                }
                this.mDraggingLabel = true;
                view.cancelLongPress();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onLongClick(View view) {
        boolean z;
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        if (BenesseExtension.getDchaState() != 0 || this.mLongClickBlocked) {
            return true;
        }
        try {
            String artistName = this.mService.getArtistName();
            String albumName = this.mService.getAlbumName();
            String trackName = this.mService.getTrackName();
            long audioId = this.mService.getAudioId();
            if (("<unknown>".equals(albumName) && "<unknown>".equals(artistName) && trackName != null && trackName.startsWith("recording")) || audioId < 0) {
                return false;
            }
            Cursor cursorQuery = MusicUtils.query(this, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId), new String[]{"is_music"}, null, null, null);
            if (cursorQuery != null) {
                z = (cursorQuery.moveToFirst() && cursorQuery.getInt(0) == 0) ? false : true;
                cursorQuery.close();
            } else {
                z = true;
            }
            if (!z) {
                return false;
            }
            boolean z2 = (artistName == null || "<unknown>".equals(artistName)) ? false : true;
            boolean z3 = (albumName == null || "<unknown>".equals(albumName)) ? false : true;
            if (z2 && view.equals(this.mArtistName.getParent())) {
                str2 = "vnd.android.cursor.item/artist";
                str4 = artistName;
                str3 = str4;
            } else if (z3 && view.equals(this.mAlbumName.getParent())) {
                if (z2) {
                    str5 = artistName + " " + albumName;
                } else {
                    str5 = albumName;
                }
                str2 = "vnd.android.cursor.item/album";
                str3 = str5;
                str4 = albumName;
            } else if (view.equals(this.mTrackName.getParent()) || !z2 || !z3) {
                if (trackName == null || "<unknown>".equals(trackName)) {
                    return true;
                }
                if (z2) {
                    str = artistName + " " + trackName;
                } else {
                    str = trackName;
                }
                str2 = "audio/*";
                str3 = str;
                str4 = trackName;
            } else {
                throw new RuntimeException("shouldn't be here");
            }
            String string = getString(R.string.mediasearch, new Object[]{str4});
            Intent intent = new Intent();
            intent.setFlags(268435456);
            intent.setAction("android.intent.action.MEDIA_SEARCH");
            intent.putExtra("query", str3);
            if (z2) {
                intent.putExtra("android.intent.extra.artist", artistName);
            }
            if (z3) {
                intent.putExtra("android.intent.extra.album", albumName);
            }
            intent.putExtra("android.intent.extra.title", trackName);
            intent.putExtra("android.intent.extra.focus", str2);
            this.mLongClickBlocked = true;
            startActivityForResult(Intent.createChooser(intent, string), 15);
            return true;
        } catch (RemoteException e) {
            return true;
        } catch (NullPointerException e2) {
            return true;
        }
    }

    @Override
    public void onStop() {
        if (this.mSearchItem != null && this.mIsHotnotClicked) {
            this.mSearchItem.collapseActionView();
            this.mIsHotnotClicked = false;
        }
        this.paused = true;
        MusicLogUtils.v("MediaPlayback", "onStop()");
        this.mIsCallOnStop = true;
        this.mHandler.removeMessages(1);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.paused = false;
        queueNextRefresh(refreshNow());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onResume() throws Throwable {
        super.onResume();
        boolean booleanExtra = getIntent().getBooleanExtra("collapse_statusbar", false);
        MusicLogUtils.v("MediaPlayback", "onResume: collapseStatusBar=" + booleanExtra);
        if (booleanExtra) {
            ((StatusBarManager) getSystemService("statusbar")).collapsePanels();
        }
        updateTrackInfo();
        if (!this.mIsCallOnStop) {
            setPauseButtonImage();
        }
        this.mIsCallOnStop = false;
        this.mPosOverride = -1L;
        invalidateOptionsMenu();
        this.mPerformanceTestString = "play song";
        this.mIsInBackgroud = false;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mStatusListener);
        MusicUtils.unbindFromService(this.mToken);
        this.mService = null;
        this.mAlbumArtWorker.quit();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (MusicUtils.getCurrentAudioId() < 0) {
            return false;
        }
        menu.add(0, 8, 0, R.string.party_shuffle);
        this.mAddToPlaylistSubmenu = menu.addSubMenu(0, 1, 0, R.string.add_to_playlist).setIcon(android.R.drawable.ic_menu_add);
        if (UserHandle.myUserId() == 0 && MusicUtils.isVoiceCapable(this)) {
            menu.add(0, 16, 0, R.string.ringtone_menu_short).setIcon(R.drawable.ic_menu_set_as_ringtone);
        }
        menu.add(0, 10, 0, R.string.delete_item).setIcon(R.drawable.ic_menu_delete);
        menu.add(0, 13, 0, R.string.effects_list_title).setIcon(R.drawable.ic_menu_eq);
        if (MusicFeatureOption.IS_SUPPORT_FM_TX) {
            menu.add(0, 14, 0, R.string.music_fm_transmiter).setIcon(R.drawable.ic_menu_fmtransmitter);
        } else {
            menu.add(0, 6, 0, R.string.goto_start).setIcon(R.drawable.ic_menu_music_library);
        }
        MenuInflater menuInflater = getMenuInflater();
        if (menuInflater != null) {
            menuInflater.inflate(R.menu.music_playback_action_bar, menu);
        }
        this.mQueueMenuItem = menu.findItem(R.id.current_playlist_menu_item);
        this.mShuffleMenuItem = menu.findItem(R.id.shuffle_menu_item);
        this.mRepeatMenuItem = menu.findItem(R.id.repeat_menu_item);
        this.mSearchItem = MusicUtils.addSearchView(this, menu, this.mQueryTextListener, this.mOnSuggestionListener);
        if (this.mHotKnotHelper != null) {
            this.mHotKnotHelper.createHotKnotMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mService == null) {
            return false;
        }
        MenuItem menuItemFindItem = menu.findItem(8);
        if (menuItemFindItem != null) {
            if (MusicUtils.getCurrentShuffleMode() == 2) {
                menuItemFindItem.setIcon(R.drawable.ic_menu_party_shuffle);
                menuItemFindItem.setTitle(R.string.party_shuffle_off);
            } else {
                menuItemFindItem.setIcon(R.drawable.ic_menu_party_shuffle);
                menuItemFindItem.setTitle(R.string.party_shuffle);
            }
        }
        if (UserHandle.myUserId() == 0) {
            if (OmaDrmUtils.isOmaDrmEnabled() && MusicUtils.isVoiceCapable(this)) {
                try {
                    menu.findItem(16).setVisible(this.mService.canUseAsRingtone());
                } catch (RemoteException e) {
                    MusicLogUtils.v("MediaPlayback", "onPrepareOptionsMenu with RemoteException " + e);
                }
            }
        } else {
            MenuItem menuItemFindItem2 = menu.findItem(16);
            if (menuItemFindItem2 != null) {
                menuItemFindItem2.setVisible(false);
            }
        }
        MusicUtils.setEffectPanelMenu(getApplicationContext(), menu);
        if (MusicFeatureOption.IS_SUPPORT_FM_TX) {
            Intent intent = new Intent("com.mediatek.FMTransmitter.FMTransmitterActivity");
            intent.setClassName("com.mediatek.FMTransmitter", "com.mediatek.FMTransmitter.FMTransmitterActivity");
            menu.findItem(14).setVisible(getPackageManager().resolveActivity(intent, 0) != null);
        }
        MusicUtils.makePlaylistMenu(this, this.mAddToPlaylistSubmenu);
        this.mAddToPlaylistSubmenu.removeItem(12);
        menu.setGroupVisible(1, !((KeyguardManager) getSystemService("keyguard")).inKeyguardRestrictedInputMode());
        this.mQueueMenuItem.setVisible(this.mIsLandscape);
        this.mShuffleMenuItem.setVisible(this.mIsLandscape);
        this.mRepeatMenuItem.setVisible(this.mIsLandscape);
        setRepeatButtonImage();
        setShuffleButtonImage();
        MusicLogUtils.v("MediaPlayback", "mIsCallOnStop:" + this.mIsCallOnStop + ",mIsInBackgroud:" + this.mIsInBackgroud + ",mIsConfigurationChanged:" + this.mIsConfigurationChanged);
        if (this.mSearchItem != null && ((this.mIsCallOnStop || this.mIsInBackgroud) && !this.mIsConfigurationChanged)) {
            this.mSearchItem.collapseActionView();
            this.mIsCallOnStop = false;
            this.mQueryText = null;
        }
        if (this.mSearchItem != null) {
            MusicLogUtils.v("MediaPlayback", "isActionViewExpanded:" + this.mSearchItem.isActionViewExpanded());
        }
        if (this.mQueryText != null && !this.mQueryText.toString().equals("")) {
            MusicLogUtils.v("MediaPlayback", "setQueryText:" + ((Object) this.mQueryText));
            ((SearchView) this.mSearchItem.getActionView()).setQuery(this.mQueryText, false);
            this.mQueryText = null;
        }
        this.mIsConfigurationChanged = false;
        if (this.mHotKnotHelper != null) {
            this.mHotKnotHelper.updateHotKnotMenu(this.mHotKnotHelper.mSendable);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        try {
        } catch (RemoteException e) {
            MusicLogUtils.v("MediaPlayback", "onOptionsItemSelected with RemoteException " + e);
        }
        switch (menuItem.getItemId()) {
            case 3:
                MusicUtils.addToPlaylist(this, new long[]{MusicUtils.getCurrentAudioId()}, menuItem.getIntent().getLongExtra("playlist", 0L));
                break;
            case 4:
                showCreateDialog(String.valueOf(MusicUtils.getCurrentAudioId()), -1, "new_playlist");
                break;
            case 6:
                Intent intent = new Intent();
                intent.setClass(this, MusicBrowserActivity.class);
                intent.setFlags(335544320);
                startActivity(intent);
                finish();
                break;
            case 8:
                MusicUtils.togglePartyShuffle();
                setShuffleButtonImage();
                setRepeatButtonImage();
                break;
            case 10:
                if (this.mService != null) {
                    long[] jArr = {MusicUtils.getCurrentAudioId()};
                    Bundle bundle = new Bundle();
                    bundle.putInt("delete_desc_string_id", R.string.delete_song_desc);
                    bundle.putString("delete_desc_track_info", this.mService.getTrackName());
                    bundle.putLongArray("items", jArr);
                    Intent intent2 = new Intent();
                    intent2.setClass(this, DeleteItems.class);
                    intent2.putExtras(bundle);
                    showDeleteDialog(Long.valueOf(MusicUtils.getCurrentAudioId()), R.string.delete_song_desc, this.mService.getTrackName());
                }
                break;
            case 13:
                break;
            case 14:
                Intent intent3 = new Intent("com.mediatek.FMTransmitter.FMTransmitterActivity");
                intent3.setClassName("com.mediatek.FMTransmitter", "com.mediatek.FMTransmitter.FMTransmitterActivity");
                try {
                    startActivity(intent3);
                } catch (ActivityNotFoundException e2) {
                    MusicLogUtils.v("MediaPlayback", "FMTx activity isn't found!!");
                }
                break;
            case 16:
                if (this.mService != null) {
                    MusicUtils.setRingtone(this, this.mService.getAudioId());
                }
                break;
            case 26:
                if (this.mHotKnotHelper != null) {
                    this.mIsHotnotClicked = true;
                    this.mHotKnotHelper.shareViaHotKnot();
                }
                break;
            case android.R.id.home:
                if (!this.mIsInBackgroud) {
                    Intent intent4 = new Intent(this, (Class<?>) MusicBrowserActivity.class);
                    intent4.setFlags(335544320);
                    finish();
                    startActivity(intent4);
                }
                break;
            case R.id.current_playlist_menu_item:
                this.mQueueListener.onClick(null);
                break;
            case R.id.shuffle_menu_item:
                toggleShuffle();
                break;
            case R.id.repeat_menu_item:
                cycleRepeat();
                break;
            case R.id.search:
                onSearchRequested();
                break;
        }
        return true;
        return true;
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        Uri data;
        if (i == 15) {
            this.mLongClickBlocked = false;
        }
        if (i2 != -1) {
            return;
        }
        MusicLogUtils.v("MediaPlayback", "onActivityResult " + i + " result" + i2);
        if (i == 4 && (data = intent.getData()) != null) {
            MusicUtils.addToPlaylist(this, new long[]{MusicUtils.getCurrentAudioId()}, Integer.parseInt(data.getLastPathSegment()));
        }
    }

    private void showCreateDialog(String str, int i, String str2) {
        MusicLogUtils.v("MediaPlayback", "showEditDialog>>");
        removeOldFragmentByTag("Create");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("MediaPlayback", "<showDeleteDialog> fragmentManager = " + fragmentManager);
        CreatePlaylist.newInstance(str, i, str2, null).show(fragmentManager, "Create");
        fragmentManager.executePendingTransactions();
    }

    private void showDeleteDialog(Long l, int i, String str) {
        MusicLogUtils.v("MediaPlayback", "showEditDialog>>");
        removeOldFragmentByTag("Delete");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("MediaPlayback", "<showDeleteDialog> fragmentManager = " + fragmentManager);
        DeleteDialogFragment.newInstance(true, l.longValue(), i, str).show(fragmentManager, "Delete");
        fragmentManager.executePendingTransactions();
    }

    private void removeOldFragmentByTag(String str) {
        MusicLogUtils.v("MediaPlayback", "<removeOldFragmentByTag> tag = " + str);
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("MediaPlayback", "<removeOldFragmentByTag> fragmentManager = " + fragmentManager);
        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(str);
        MusicLogUtils.v("MediaPlayback", "<removeOldFragmentByTag> oldFragment = " + dialogFragment);
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
    }

    private boolean seekMethod1(int i) {
        if (this.mService == null) {
            return false;
        }
        int i2 = 0;
        while (true) {
            int i3 = -1;
            if (i2 < 10) {
                int i4 = 0;
                while (i4 < 3) {
                    if (this.keyboard[i4][i2] != i) {
                        i4++;
                    } else {
                        if (i2 != this.lastX || i4 != this.lastY) {
                            if (i4 != 0 || this.lastY != 0 || i2 <= this.lastX) {
                                if ((i4 != 0 || this.lastY != 0 || i2 >= this.lastX) && (i4 != 2 || this.lastY != 2 || i2 <= this.lastX)) {
                                    if ((i4 != 2 || this.lastY != 2 || i2 >= this.lastX) && (i4 >= this.lastY || i2 > 4)) {
                                        if ((i4 >= this.lastY || i2 < 5) && (i4 <= this.lastY || i2 > 4)) {
                                            i3 = (i4 <= this.lastY || i2 < 5) ? 0 : 1;
                                        }
                                    }
                                }
                            }
                        }
                        this.lastX = i2;
                        this.lastY = i4;
                        try {
                            this.mService.seek(this.mService.position() + ((long) (i3 * 5)));
                        } catch (RemoteException e) {
                        }
                        refreshNow();
                        return true;
                    }
                }
                i2++;
            } else {
                this.lastX = -1;
                this.lastY = -1;
                return false;
            }
        }
    }

    private boolean seekMethod2(int i) {
        if (this.mService == null) {
            return false;
        }
        for (int i2 = 0; i2 < 10; i2++) {
            if (this.keyboard[0][i2] == i) {
                try {
                    this.mService.seek((this.mService.duration() * ((long) ((100 * i2) / 10))) / 100);
                } catch (RemoteException e) {
                }
                refreshNow();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        try {
            switch (i) {
                case 21:
                    if (useDpadMusicControl()) {
                        if (this.mService != null) {
                            if (!this.mSeeking && this.mStartSeekPos >= 0) {
                                this.mPauseButton.requestFocus();
                                if (this.mStartSeekPos < 1000) {
                                    this.mService.prev();
                                } else {
                                    this.mService.seek(0L);
                                }
                            } else {
                                scanBackward(-1, keyEvent.getEventTime() - keyEvent.getDownTime());
                                this.mPauseButton.requestFocus();
                                this.mStartSeekPos = -1L;
                            }
                        }
                        this.mSeeking = false;
                        this.mPosOverride = -1L;
                        return true;
                    }
                    break;
                case 22:
                    if (useDpadMusicControl()) {
                        if (this.mService != null) {
                            if (!this.mSeeking && this.mStartSeekPos >= 0) {
                                this.mPauseButton.requestFocus();
                                this.mService.next();
                            } else {
                                scanForward(-1, keyEvent.getEventTime() - keyEvent.getDownTime());
                                this.mPauseButton.requestFocus();
                                this.mStartSeekPos = -1L;
                            }
                        }
                        this.mSeeking = false;
                        this.mPosOverride = -1L;
                        return true;
                    }
                    break;
                case 23:
                    View currentFocus = getCurrentFocus();
                    if ((currentFocus != null && R.id.pause == currentFocus.getId()) || currentFocus == null) {
                        doPauseResume();
                    }
                    return true;
            }
        } catch (RemoteException e) {
        }
        return super.onKeyUp(i, keyEvent);
    }

    private boolean useDpadMusicControl() {
        if (this.mDeviceHasDpad) {
            if (this.mPrevButton.isFocused() || this.mNextButton.isFocused() || this.mPauseButton.isFocused()) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        int repeatCount = keyEvent.getRepeatCount();
        if (this.seekmethod != 0 ? seekMethod2(i) : seekMethod1(i)) {
            return true;
        }
        if (i == 47) {
            toggleShuffle();
            return true;
        }
        if (i == 62 || i == 66) {
            doPauseResume();
            return true;
        }
        if (i == 76) {
            this.seekmethod = 1 - this.seekmethod;
            return true;
        }
        if (i == 82) {
            return this.mSearchItem != null && this.mSearchItem.isActionViewExpanded();
        }
        switch (i) {
            case 21:
                if (useDpadMusicControl()) {
                    if (!this.mPrevButton.hasFocus()) {
                        this.mPrevButton.requestFocus();
                    }
                    scanBackward(repeatCount, keyEvent.getEventTime() - keyEvent.getDownTime());
                    return true;
                }
                break;
            case 22:
                if (useDpadMusicControl()) {
                    if (!this.mNextButton.hasFocus()) {
                        this.mNextButton.requestFocus();
                    }
                    scanForward(repeatCount, keyEvent.getEventTime() - keyEvent.getDownTime());
                    return true;
                }
                break;
            case 23:
                return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    private void scanBackward(int i, long j) {
        long j2;
        if (this.mService == null) {
            return;
        }
        try {
            if (i == 0) {
                this.mStartSeekPos = this.mService.position();
                this.mLastSeekEventTime = 0L;
                this.mSeeking = false;
            } else {
                this.mSeeking = true;
                if (j < 5000) {
                    j2 = j * 10;
                } else {
                    j2 = ((j - 5000) * 40) + 50000;
                }
                long j3 = this.mStartSeekPos - j2;
                if (j3 < 0) {
                    this.mService.prev();
                    long jDuration = this.mService.duration();
                    this.mStartSeekPos += jDuration;
                    j3 += jDuration;
                }
                if (j2 - this.mLastSeekEventTime > 250 || i < 0) {
                    this.mService.seek(j3);
                    this.mLastSeekEventTime = j2;
                }
                if (i >= 0) {
                    this.mPosOverride = j3;
                } else {
                    this.mPosOverride = -1L;
                }
                refreshNow();
            }
        } catch (RemoteException e) {
        }
        this.mPosOverride = -1L;
    }

    private void scanForward(int i, long j) {
        long j2;
        if (this.mService == null) {
            return;
        }
        try {
            if (i == 0) {
                this.mStartSeekPos = this.mService.position();
                this.mLastSeekEventTime = 0L;
                this.mSeeking = false;
            } else {
                this.mSeeking = true;
                if (j < 5000) {
                    j2 = j * 10;
                } else {
                    j2 = ((j - 5000) * 40) + 50000;
                }
                long j3 = this.mStartSeekPos + j2;
                long jDuration = this.mService.duration();
                if (j3 >= jDuration) {
                    this.mService.next();
                    this.mStartSeekPos -= jDuration;
                    j3 -= jDuration;
                }
                if (j2 - this.mLastSeekEventTime > 250 || i < 0) {
                    this.mService.seek(j3);
                    this.mLastSeekEventTime = j2;
                }
                if (i >= 0) {
                    this.mPosOverride = j3;
                } else {
                    this.mPosOverride = -1L;
                }
                refreshNow();
            }
        } catch (RemoteException e) {
        }
        this.mPosOverride = -1L;
    }

    private void doPauseResume() {
        try {
            if (this.mService != null) {
                Boolean boolValueOf = Boolean.valueOf(this.mService.isPlaying());
                MusicLogUtils.v("MediaPlayback", "doPauseResume: isPlaying=" + boolValueOf);
                this.mPosOverride = -1L;
                if (boolValueOf.booleanValue()) {
                    this.mService.pause();
                } else {
                    this.mService.play();
                }
                refreshNow();
                setPauseButtonImage();
            }
        } catch (RemoteException e) {
        }
    }

    private void toggleShuffle() {
        if (this.mService == null) {
            return;
        }
        try {
            int shuffleMode = this.mService.getShuffleMode();
            if (shuffleMode == 0) {
                this.mService.setShuffleMode(1);
                if (this.mService.getRepeatMode() == 1) {
                    this.mService.setRepeatMode(2);
                }
                setRepeatButtonImage();
                showToast(R.string.shuffle_on_notif);
            } else if (shuffleMode == 1 || shuffleMode == 2) {
                this.mService.setShuffleMode(0);
                showToast(R.string.shuffle_off_notif);
            } else {
                MusicLogUtils.v("MediaPlayback", "Invalid shuffle mode: " + shuffleMode);
            }
            setShuffleButtonImage();
        } catch (RemoteException e) {
        }
    }

    private void cycleRepeat() {
        if (this.mService == null) {
            return;
        }
        try {
            int repeatMode = this.mService.getRepeatMode();
            if (repeatMode == 0) {
                this.mService.setRepeatMode(2);
                showToast(R.string.repeat_all_notif);
            } else if (repeatMode != 2) {
                this.mService.setRepeatMode(0);
                showToast(R.string.repeat_off_notif);
            } else {
                this.mService.setRepeatMode(1);
                if (this.mService.getShuffleMode() != 0) {
                    this.mService.setShuffleMode(0);
                    setShuffleButtonImage();
                }
                showToast(R.string.repeat_current_notif);
            }
            setRepeatButtonImage();
        } catch (RemoteException e) {
        }
    }

    private void showToast(int i) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this, "", 0);
        }
        this.mToast.setText(i);
        this.mToast.show();
    }

    private void startPlayback() throws Throwable {
        String string;
        if (this.mService == null) {
            return;
        }
        Uri data = getIntent().getData();
        if (data != null && data.toString().length() > 0) {
            if ("file".equals(data.getScheme())) {
                string = data.getPath();
            } else {
                string = data.toString();
            }
            try {
                this.mService.stop();
                this.mService.openFile(string);
                this.mService.play();
                setIntent(new Intent());
            } catch (Exception e) {
                MusicLogUtils.v("MediaPlayback", "couldn't start playback: " + e);
            }
        }
        updateTrackInfo();
        queueNextRefresh(refreshNow());
    }

    private void setRepeatButtonImage() {
        int i;
        if (this.mService == null) {
            return;
        }
        try {
            switch (this.mService.getRepeatMode()) {
                case 1:
                    i = R.drawable.ic_mp_repeat_once_btn;
                    break;
                case 2:
                    i = R.drawable.ic_mp_repeat_all_btn;
                    break;
                default:
                    i = R.drawable.ic_mp_repeat_off_btn;
                    break;
            }
            if (this.mIsLandscape) {
                if (this.mRepeatMenuItem != null) {
                    this.mRepeatMenuItem.setIcon(i);
                }
            } else {
                this.mRepeatButton.setImageResource(i);
            }
        } catch (RemoteException e) {
        }
    }

    private void setShuffleButtonImage() {
        int i;
        if (this.mService == null) {
            return;
        }
        try {
            int shuffleMode = this.mService.getShuffleMode();
            if (shuffleMode == 0) {
                i = R.drawable.ic_mp_shuffle_off_btn;
            } else if (shuffleMode == 2) {
                i = R.drawable.ic_mp_partyshuffle_on_btn;
            } else {
                i = R.drawable.ic_mp_shuffle_on_btn;
            }
            if (this.mIsLandscape) {
                if (this.mShuffleMenuItem != null) {
                    this.mShuffleMenuItem.setIcon(i);
                }
            } else {
                this.mShuffleButton.setImageResource(i);
            }
        } catch (RemoteException e) {
        }
    }

    private void setPauseButtonImage() {
        try {
            if (this.mService != null && this.mService.isPlaying()) {
                this.mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                if (!this.mSeeking) {
                    this.mPosOverride = -1L;
                }
            } else {
                this.mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        } catch (RemoteException e) {
        }
    }

    private void queueNextRefresh(long j) {
        if (!this.paused) {
            Message messageObtainMessage = this.mHandler.obtainMessage(1);
            this.mHandler.removeMessages(1);
            this.mHandler.sendMessageDelayed(messageObtainMessage, j);
        }
    }

    private long refreshNow() {
        if (this.mService == null) {
            return 500L;
        }
        try {
            MusicLogUtils.v("MediaPlayback", "refreshNow()-mPosOverride = " + this.mPosOverride);
            if (this.mDuration != this.mService.duration()) {
                this.mDurationHasChanged = true;
            }
            this.mDuration = this.mService.duration();
            long jPosition = this.mService.position();
            MusicLogUtils.v("MediaPlayback", "refreshNow()-position = " + jPosition);
            if (this.mPosOverride >= 0) {
                jPosition = this.mPosOverride;
            }
            if (300 + jPosition > this.mDuration) {
                MusicLogUtils.v("MediaPlayback", "refreshNow()-do a workaround for position");
                jPosition = this.mDuration;
            }
            updateDuration(jPosition);
            if (jPosition >= 0 && this.mDuration > 0) {
                MusicLogUtils.v("MediaPlayback", "refreshNow()-pos = " + jPosition + "Duration: " + this.mDuration);
                String strMakeTimeString = MusicUtils.makeTimeString(this, jPosition / 1000);
                StringBuilder sb = new StringBuilder();
                sb.append("refreshNow()-time = ");
                sb.append(strMakeTimeString);
                MusicLogUtils.v("MediaPlayback", sb.toString());
                this.mCurrentTime.setText(strMakeTimeString);
                if (!this.mFromTouch) {
                    this.mProgress.setProgress((int) ((1000 * jPosition) / this.mDuration));
                }
                if (!this.mService.isPlaying() && this.mRepeatCount <= -1) {
                    this.mCurrentTime.setVisibility(this.mCurrentTime.getVisibility() == 4 ? 0 : 4);
                    return 500L;
                }
                this.mCurrentTime.setVisibility(0);
            } else {
                this.mCurrentTime.setVisibility(0);
                this.mCurrentTime.setText(MusicUtils.makeTimeString(this, 0L));
                this.mTotalTime.setText("--:--");
                if (!this.mFromTouch) {
                    this.mProgress.setProgress(0);
                }
            }
            long j = 1000 - (jPosition % 1000);
            int width = this.mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            long j2 = this.mDuration / ((long) width);
            if (j2 > j) {
                return j;
            }
            if (j2 < 20) {
                return 20L;
            }
            return j2;
        } catch (RemoteException e) {
            return 500L;
        }
    }

    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;

        AlbumSongIdWrapper(long j, long j2) {
            this.albumid = j;
            this.songid = j2;
        }
    }

    private void updateTrackInfo() throws Throwable {
        if (this.mService == null) {
            return;
        }
        try {
            String path = this.mService.getPath();
            if (path == null) {
                finish();
                return;
            }
            if (this.mHotKnotHelper != null) {
                this.mHotKnotHelper.setHotKnotUri(path);
            }
            long audioId = this.mService.getAudioId();
            long j = -1;
            if (audioId < 0 && path.toLowerCase().startsWith("http://")) {
                ((View) this.mArtistName.getParent()).setVisibility(4);
                ((View) this.mAlbumName.getParent()).setVisibility(4);
                this.mAlbum.setVisibility(8);
                this.mTrackName.setText(path);
                this.mAlbumArtHandler.removeMessages(3);
                this.mAlbumArtHandler.obtainMessage(3, new AlbumSongIdWrapper(-1L, -1L)).sendToTarget();
            } else {
                ((View) this.mArtistName.getParent()).setVisibility(0);
                ((View) this.mAlbumName.getParent()).setVisibility(0);
                String artistName = this.mService.getArtistName();
                if ("<unknown>".equals(artistName)) {
                    artistName = getString(R.string.unknown_artist_name);
                }
                this.mArtistName.setText(artistName);
                String albumName = this.mService.getAlbumName();
                long albumId = this.mService.getAlbumId();
                if ("<unknown>".equals(albumName)) {
                    albumName = getString(R.string.unknown_album_name);
                } else {
                    j = albumId;
                }
                this.mAlbumName.setText(albumName);
                this.mTrackName.setText(this.mService.getTrackName());
                this.mAlbumArtHandler.removeMessages(3);
                this.mAlbumArtHandler.obtainMessage(3, new AlbumSongIdWrapper(j, audioId)).sendToTarget();
                this.mAlbum.setVisibility(0);
            }
            this.mDuration = this.mService.duration();
            this.mTotalTime.setText(MusicUtils.makeTimeString(this, this.mDuration / 1000));
            recordDurationUpdateStatus();
        } catch (RemoteException e) {
            finish();
        }
    }

    public class AlbumArtHandler extends Handler {
        private long mAlbumId;

        public AlbumArtHandler(Looper looper) {
            super(looper);
            this.mAlbumId = -1L;
        }

        @Override
        public void handleMessage(Message message) {
            long j = ((AlbumSongIdWrapper) message.obj).albumid;
            long j2 = ((AlbumSongIdWrapper) message.obj).songid;
            if (message.what == 3) {
                if (this.mAlbumId != j || j < 0 || MediaPlaybackActivity.this.mIsShowAlbumArt) {
                    if (MediaPlaybackActivity.this.mArtBitmap == null || MediaPlaybackActivity.this.mArtSongId != j2) {
                        Message messageObtainMessage = MediaPlaybackActivity.this.mHandler.obtainMessage(4, null);
                        MediaPlaybackActivity.this.mHandler.removeMessages(4);
                        MediaPlaybackActivity.this.mHandler.sendMessageDelayed(messageObtainMessage, 300L);
                        MediaPlaybackActivity.this.mArtBitmap = MusicUtils.getArtwork(MediaPlaybackActivity.this, j2, j, false);
                        MusicLogUtils.v("MediaPlayback", "get art. mArtSongId = " + MediaPlaybackActivity.this.mArtSongId + " ,songid = " + j2 + " ");
                        MediaPlaybackActivity.this.mArtSongId = j2;
                    }
                    if (MediaPlaybackActivity.this.mArtBitmap == null) {
                        MediaPlaybackActivity.this.mArtBitmap = MusicUtils.getDefaultArtwork(MediaPlaybackActivity.this);
                        j = -1;
                    }
                    if (MediaPlaybackActivity.this.mArtBitmap != null) {
                        Message messageObtainMessage2 = MediaPlaybackActivity.this.mHandler.obtainMessage(4, MediaPlaybackActivity.this.mArtBitmap);
                        MediaPlaybackActivity.this.mHandler.removeMessages(4);
                        MediaPlaybackActivity.this.mHandler.sendMessage(messageObtainMessage2);
                    }
                    this.mAlbumId = j;
                    MediaPlaybackActivity.this.mIsShowAlbumArt = false;
                }
            }
        }
    }

    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        Worker(String str) {
            Thread thread = new Thread(null, this, str);
            thread.setPriority(1);
            thread.start();
            synchronized (this.mLock) {
                while (this.mLooper == null) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public Looper getLooper() {
            return this.mLooper;
        }

        @Override
        public void run() {
            synchronized (this.mLock) {
                Looper.prepare();
                this.mLooper = Looper.myLooper();
                this.mLock.notifyAll();
            }
            Looper.loop();
        }

        public void quit() {
            this.mLooper.quit();
        }
    }

    private void updateUI() {
        setContentView(R.layout.audio_player);
        this.mCurrentTime = (TextView) findViewById(R.id.currenttime);
        this.mTotalTime = (TextView) findViewById(R.id.totaltime);
        this.mProgress = (ProgressBar) findViewById(android.R.id.progress);
        this.mAlbum = (ImageView) findViewById(R.id.album);
        this.mArtistName = (TextView) findViewById(R.id.artistname);
        this.mAlbumName = (TextView) findViewById(R.id.albumname);
        this.mTrackName = (TextView) findViewById(R.id.trackname);
        View view = (View) this.mArtistName.getParent();
        view.setOnTouchListener(this);
        view.setOnLongClickListener(this);
        View view2 = (View) this.mAlbumName.getParent();
        view2.setOnTouchListener(this);
        view2.setOnLongClickListener(this);
        View view3 = (View) this.mTrackName.getParent();
        view3.setOnTouchListener(this);
        view3.setOnLongClickListener(this);
        this.mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
        this.mPrevButton.setOnClickListener(this.mPrevListener);
        this.mPrevButton.setRepeatListener(this.mRewListener, 260L);
        this.mPauseButton = (ImageButton) findViewById(R.id.pause);
        this.mPauseButton.requestFocus();
        this.mPauseButton.setOnClickListener(this.mPauseListener);
        this.mNextButton = (RepeatingImageButton) findViewById(R.id.next);
        this.mNextButton.setOnClickListener(this.mNextListener);
        this.mNextButton.setRepeatListener(this.mFfwdListener, 260L);
        this.seekmethod = 1;
        this.mDeviceHasDpad = getResources().getConfiguration().navigation == 2;
        if (!this.mIsLandscape) {
            this.mQueueButton = (ImageButton) findViewById(R.id.curplaylist);
            this.mQueueButton.setOnClickListener(this.mQueueListener);
            this.mShuffleButton = (ImageButton) findViewById(R.id.shuffle);
            this.mShuffleButton.setOnClickListener(this.mShuffleListener);
            this.mRepeatButton = (ImageButton) findViewById(R.id.repeat);
            this.mRepeatButton.setOnClickListener(this.mRepeatListener);
        }
        if (this.mProgress instanceof SeekBar) {
            ((SeekBar) this.mProgress).setOnSeekBarChangeListener(this.mSeekListener);
        }
        this.mProgress.setMax(1000);
        this.mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    @Override
    protected void onPause() {
        this.mIsInBackgroud = true;
        if (this.mSearchItem != null) {
            this.mQueryText = ((SearchView) this.mSearchItem.getActionView()).getQuery();
            MusicLogUtils.v("MediaPlayback", "searchText:" + ((Object) this.mQueryText));
        }
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) throws Throwable {
        super.onConfigurationChanged(configuration);
        this.mIsConfigurationChanged = true;
        this.mIsLandscape = getResources().getConfiguration().orientation == 2;
        this.mIsShowAlbumArt = true;
        updateUI();
        updateTrackInfo();
        queueNextRefresh(refreshNow());
        setRepeatButtonImage();
        setPauseButtonImage();
        setShuffleButtonImage();
        this.mPosOverride = -1L;
        if (this.mSearchItem != null) {
            this.mQueryText = ((SearchView) this.mSearchItem.getActionView()).getQuery();
            MusicLogUtils.v("MediaPlayback", "searchText:" + ((Object) this.mQueryText));
        }
        invalidateOptionsMenu();
    }

    private int getBackgroundColor() {
        return -872375860;
    }

    private void updateDuration(long j) {
        try {
            if (!this.mNeedUpdateDuration || !this.mService.isPlaying()) {
                if (j < 0 || j >= this.mDuration) {
                    this.mNeedUpdateDuration = false;
                    return;
                }
                return;
            }
            long jDuration = this.mService.duration();
            if (jDuration > 0 && (jDuration != this.mDuration || this.mDurationHasChanged)) {
                this.mDurationHasChanged = false;
                this.mDuration = jDuration;
                this.mNeedUpdateDuration = false;
                this.mTotalTime.setText(MusicUtils.makeTimeString(this, this.mDuration / 1000));
                MusicLogUtils.v("MediaPlayback", "new duration updated!!");
            }
        } catch (RemoteException e) {
            MusicLogUtils.v("MediaPlayback", "Error:" + e);
        }
    }

    private void recordDurationUpdateStatus() {
        String mIMEType;
        this.mNeedUpdateDuration = false;
        try {
            mIMEType = this.mService.getMIMEType();
        } catch (RemoteException e) {
            MusicLogUtils.v("MediaPlayback", "Error:" + e);
            mIMEType = null;
        }
        if (mIMEType != null) {
            MusicLogUtils.v("MediaPlayback", "mimeType=" + mIMEType);
            if (mIMEType.equals("audio/mpeg") || mIMEType.equals("audio/amr") || mIMEType.equals("audio/amr-wb") || mIMEType.equals("audio/aac") || mIMEType.equals("audio/flac")) {
                this.mNeedUpdateDuration = true;
            }
        }
    }

    @Override
    public Uri[] createBeamUris(NfcEvent nfcEvent) {
        Uri uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicUtils.getCurrentAudioId());
        MusicLogUtils.d("MediaPlayback", "NFC call for uri " + uriWithAppendedId);
        return new Uri[]{uriWithAppendedId};
    }

    @Override
    public boolean onSearchRequested() {
        if (this.mSearchItem != null) {
            this.mSearchItem.expandActionView();
            return true;
        }
        return true;
    }

    class HotKnotHelper {
        private Context mPlaybackContext;
        private MenuItem mHotknotItem = null;
        private String mHotKnotUri = null;
        private boolean mSendable = true;
        private boolean mIsDrmSd = false;

        public HotKnotHelper(Context context) {
            this.mPlaybackContext = null;
            if (checkHotKnotEnabled()) {
                this.mPlaybackContext = context;
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (-1 == i) {
                            if (BenesseExtension.getDchaState() == 0) {
                                MusicLogUtils.v("MediaPlayback", "hotKnot start setting");
                                HotKnotHelper.this.mPlaybackContext.startActivity(new Intent("mediatek.settings.HOTKNOT_SETTINGS"));
                            }
                            dialogInterface.cancel();
                            return;
                        }
                        MusicLogUtils.v("MediaPlayback", "onClick cancel dialog");
                        dialogInterface.cancel();
                    }
                };
            }
        }

        public void createHotKnotMenu(Menu menu) {
            if (!checkHotKnotEnabled()) {
                return;
            }
            this.mHotknotItem = menu.add(0, 26, 0, R.string.hotknot).setIcon(R.drawable.ic_hotknot);
            this.mHotknotItem.setShowAsActionFlags(2);
        }

        public void updateHotKnotMenu(boolean z) {
            if (this.mHotknotItem == null) {
                return;
            }
            if (z) {
                this.mHotknotItem.setIcon(R.drawable.ic_hotknot);
            } else {
                this.mHotknotItem.setIcon(R.drawable.ic_hotknot_disable);
            }
            this.mHotknotItem.setEnabled(z);
        }

        public void shareViaHotKnot() {
            Uri[] uriArr;
            if (!checkHotKnotEnabled() && this.mHotKnotUri != null) {
                return;
            }
            if (this.mIsDrmSd) {
                uriArr = new Uri[]{Uri.parse(this.mHotKnotUri + "?isMimeType=no")};
            } else {
                uriArr = new Uri[]{Uri.parse(this.mHotKnotUri)};
            }
            Intent intent = new Intent();
            intent.setAction("com.mediatek.hotknot.action.SHARE");
            intent.putExtra("com.mediatek.hotknot.extra.SHARE_URIS", uriArr);
            intent.addFlags(134742016);
            MediaPlaybackActivity.this.startActivity(intent);
        }

        private boolean checkHotKnotEnabled() {
            return false;
        }

        public void setHotKnotUri(String str) throws Throwable {
            if (MediaPlaybackActivity.this.mSearchItem != null) {
                MediaPlaybackActivity.this.mQueryText = ((SearchView) MediaPlaybackActivity.this.mSearchItem.getActionView()).getQuery();
                MusicLogUtils.v("MediaPlayback", "setHotKnotUri,searchText:" + ((Object) MediaPlaybackActivity.this.mQueryText));
            }
            Cursor cursor = null;
            if (!MediaPlaybackActivity.this.mHotKnotHelper.checkHotKnotEnabled() || str == null || str.startsWith("file")) {
                this.mHotKnotUri = null;
                MediaPlaybackActivity.this.invalidateOptionsMenu();
                return;
            }
            this.mHotKnotUri = str;
            Uri uri = Uri.parse(this.mHotKnotUri);
            this.mIsDrmSd = false;
            this.mSendable = true;
            try {
                try {
                    Cursor cursorQuery = MediaPlaybackActivity.this.getContentResolver().query(uri, null, null, null, null);
                    if (cursorQuery != null) {
                        try {
                            cursorQuery.moveToFirst();
                        } catch (IllegalStateException e) {
                            cursor = cursorQuery;
                            MusicLogUtils.v("MediaPlayback", "setHotKnotUri()-IllegalStateException");
                            if (cursor != null) {
                                cursor.close();
                            }
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery;
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (IllegalStateException e2) {
                }
                MusicLogUtils.v("MediaPlayback", "setHotKnotUri(),mSendable=" + this.mSendable);
                MediaPlaybackActivity.this.invalidateOptionsMenu();
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }
}
