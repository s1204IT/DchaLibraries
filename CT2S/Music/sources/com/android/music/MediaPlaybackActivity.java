package com.android.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;
import com.android.music.RepeatingImageButton;

public class MediaPlaybackActivity extends Activity implements View.OnLongClickListener, View.OnTouchListener {
    private int lastX;
    private int lastY;
    private ImageView mAlbum;
    private AlbumArtHandler mAlbumArtHandler;
    private Worker mAlbumArtWorker;
    private TextView mAlbumName;
    private TextView mArtistName;
    private TextView mCurrentTime;
    private boolean mDeviceHasDpad;
    private long mDuration;
    private long mLastSeekEventTime;
    private RepeatingImageButton mNextButton;
    private ImageButton mPauseButton;
    private RepeatingImageButton mPrevButton;
    private ProgressBar mProgress;
    private ImageButton mQueueButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private Toast mToast;
    private MusicUtils.ServiceToken mToken;
    private TextView mTotalTime;
    private int mTouchSlop;
    private TextView mTrackName;
    private boolean paused;
    private int seekmethod;
    private boolean mSeeking = false;
    private long mStartSeekPos = 0;
    private int mRepeatCount = 0;
    private long eventstarttime = 0;
    private long eventstoptime = 0;
    private IMediaPlaybackService mService = null;
    int mInitialX = -1;
    int mLastX = -1;
    int mTextWidth = 0;
    int mViewWidth = 0;
    boolean mDraggingLabel = false;
    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = (tv.getScrollX() * 3) / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                MediaPlaybackActivity.this.mLabelScroller.sendMessageDelayed(newmsg, 15L);
            }
        }
    };
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            MediaPlaybackActivity.this.mLastSeekEventTime = 0L;
            MediaPlaybackActivity.this.mFromTouch = true;
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (fromuser && MediaPlaybackActivity.this.mService != null) {
                long now = SystemClock.elapsedRealtime();
                if (now - MediaPlaybackActivity.this.mLastSeekEventTime > 250) {
                    MediaPlaybackActivity.this.mLastSeekEventTime = now;
                    MediaPlaybackActivity.this.mPosOverride = (MediaPlaybackActivity.this.mDuration * ((long) progress)) / 1000;
                    try {
                        MediaPlaybackActivity.this.mService.seek(MediaPlaybackActivity.this.mPosOverride);
                    } catch (RemoteException e) {
                    }
                    if (!MediaPlaybackActivity.this.mFromTouch) {
                        MediaPlaybackActivity.this.refreshNow();
                        MediaPlaybackActivity.this.mPosOverride = -1L;
                    }
                }
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            MediaPlaybackActivity.this.mPosOverride = -1L;
            MediaPlaybackActivity.this.mFromTouch = false;
        }
    };
    private View.OnClickListener mQueueListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaPlaybackActivity.this.startActivity(new Intent("android.intent.action.EDIT").setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track").putExtra("playlist", "nowplaying"));
        }
    };
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaPlaybackActivity.this.toggleShuffle();
        }
    };
    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaPlaybackActivity.this.cycleRepeat();
        }
    };
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaPlaybackActivity.this.doPauseResume();
        }
    };
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (MediaPlaybackActivity.this.mService == null) {
                return;
            }
            try {
                if (MediaPlaybackActivity.this.mService.position() < 2000) {
                    MediaPlaybackActivity.this.mService.prev();
                } else {
                    MediaPlaybackActivity.this.mService.seek(0L);
                    MediaPlaybackActivity.this.mService.play();
                }
            } catch (RemoteException e) {
            }
        }
    };
    private View.OnClickListener mNextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (MediaPlaybackActivity.this.mService != null) {
                try {
                    MediaPlaybackActivity.this.mService.next();
                } catch (RemoteException e) {
                }
            }
        }
    };
    private RepeatingImageButton.RepeatListener mRewListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(View v, long howlong, int repcnt) {
            MediaPlaybackActivity.this.scanBackward(repcnt, howlong);
        }
    };
    private RepeatingImageButton.RepeatListener mFfwdListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(View v, long howlong, int repcnt) {
            MediaPlaybackActivity.this.scanForward(repcnt, howlong);
        }
    };
    private final int[][] keyboard = {new int[]{45, 51, 33, 46, 48, 53, 49, 37, 43, 44}, new int[]{29, 47, 32, 34, 35, 36, 38, 39, 40, 67}, new int[]{54, 52, 31, 50, 30, 42, 41, 55, 56, 66}};
    private ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            MediaPlaybackActivity.this.mService = IMediaPlaybackService.Stub.asInterface(obj);
            MediaPlaybackActivity.this.startPlayback();
            try {
                if (MediaPlaybackActivity.this.mService.getAudioId() >= 0 || MediaPlaybackActivity.this.mService.isPlaying() || MediaPlaybackActivity.this.mService.getPath() != null) {
                    MediaPlaybackActivity.this.mRepeatButton.setVisibility(0);
                    MediaPlaybackActivity.this.mShuffleButton.setVisibility(0);
                    MediaPlaybackActivity.this.mQueueButton.setVisibility(0);
                    MediaPlaybackActivity.this.setRepeatButtonImage();
                    MediaPlaybackActivity.this.setShuffleButtonImage();
                    MediaPlaybackActivity.this.setPauseButtonImage();
                    return;
                }
            } catch (RemoteException e) {
            }
            if (MediaPlaybackActivity.this.getIntent().getData() == null) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setFlags(268435456);
                intent.setClass(MediaPlaybackActivity.this, MusicBrowserActivity.class);
                MediaPlaybackActivity.this.startActivity(intent);
            }
            MediaPlaybackActivity.this.finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            MediaPlaybackActivity.this.mService = null;
        }
    };
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private boolean mScreenOn = true;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    long next = MediaPlaybackActivity.this.refreshNow();
                    MediaPlaybackActivity.this.queueNextRefresh(next);
                    break;
                case 2:
                    new AlertDialog.Builder(MediaPlaybackActivity.this).setTitle(R.string.service_start_error_title).setMessage(R.string.service_start_error_msg).setPositiveButton(R.string.service_start_error_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            MediaPlaybackActivity.this.finish();
                        }
                    }).setCancelable(false).show();
                    break;
                case 4:
                    MediaPlaybackActivity.this.mAlbum.setImageBitmap((Bitmap) msg.obj);
                    MediaPlaybackActivity.this.mAlbum.getDrawable().setDither(true);
                    break;
            }
        }
    };
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.android.music.metachanged")) {
                MediaPlaybackActivity.this.updateTrackInfo();
                MediaPlaybackActivity.this.setPauseButtonImage();
                MediaPlaybackActivity.this.queueNextRefresh(1L);
            } else {
                if (action.equals("com.android.music.playstatechanged")) {
                    MediaPlaybackActivity.this.setPauseButtonImage();
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    MediaPlaybackActivity.this.mScreenOn = true;
                    MediaPlaybackActivity.this.queueNextRefresh(1L);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    MediaPlaybackActivity.this.mScreenOn = false;
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(3);
        this.mAlbumArtWorker = new Worker("album art worker");
        this.mAlbumArtHandler = new AlbumArtHandler(this.mAlbumArtWorker.getLooper());
        requestWindowFeature(1);
        setContentView(R.layout.audio_player);
        this.mCurrentTime = (TextView) findViewById(R.id.currenttime);
        this.mTotalTime = (TextView) findViewById(R.id.totaltime);
        this.mProgress = (ProgressBar) findViewById(android.R.id.progress);
        this.mAlbum = (ImageView) findViewById(R.id.album);
        this.mArtistName = (TextView) findViewById(R.id.artistname);
        this.mAlbumName = (TextView) findViewById(R.id.albumname);
        this.mTrackName = (TextView) findViewById(R.id.trackname);
        View v = (View) this.mArtistName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
        View v2 = (View) this.mAlbumName.getParent();
        v2.setOnTouchListener(this);
        v2.setOnLongClickListener(this);
        View v3 = (View) this.mTrackName.getParent();
        v3.setOnTouchListener(this);
        v3.setOnLongClickListener(this);
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
        this.mQueueButton = (ImageButton) findViewById(R.id.curplaylist);
        this.mQueueButton.setOnClickListener(this.mQueueListener);
        this.mShuffleButton = (ImageButton) findViewById(R.id.shuffle);
        this.mShuffleButton.setOnClickListener(this.mShuffleListener);
        this.mRepeatButton = (ImageButton) findViewById(R.id.repeat);
        this.mRepeatButton.setOnClickListener(this.mRepeatListener);
        if (this.mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) this.mProgress;
            seeker.setOnSeekBarChangeListener(this.mSeekListener);
        }
        this.mProgress.setMax(1000);
        this.mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.artistname);
        if (vv != null) {
            return (TextView) vv;
        }
        View vv2 = v.findViewById(R.id.albumname);
        if (vv2 != null) {
            return (TextView) vv2;
        }
        View vv3 = v.findViewById(R.id.trackname);
        if (vv3 != null) {
            return (TextView) vv3;
        }
        return null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == 0) {
            v.setBackgroundColor(-10461088);
            int x = (int) event.getX();
            this.mLastX = x;
            this.mInitialX = x;
            this.mDraggingLabel = false;
            return false;
        }
        if (action == 1 || action == 3) {
            v.setBackgroundColor(0);
            if (!this.mDraggingLabel) {
                return false;
            }
            Message msg = this.mLabelScroller.obtainMessage(0, tv);
            this.mLabelScroller.sendMessageDelayed(msg, 1000L);
            return false;
        }
        if (action != 2) {
            return false;
        }
        if (this.mDraggingLabel) {
            int scrollx = tv.getScrollX();
            int x2 = (int) event.getX();
            int delta = this.mLastX - x2;
            if (delta != 0) {
                this.mLastX = x2;
                int scrollx2 = scrollx + delta;
                if (scrollx2 > this.mTextWidth) {
                    scrollx2 = (scrollx2 - this.mTextWidth) - this.mViewWidth;
                }
                if (scrollx2 < (-this.mViewWidth)) {
                    scrollx2 = scrollx2 + this.mViewWidth + this.mTextWidth;
                }
                tv.scrollTo(scrollx2, 0);
            }
            return true;
        }
        if (Math.abs(this.mInitialX - ((int) event.getX())) <= this.mTouchSlop) {
            return false;
        }
        this.mLabelScroller.removeMessages(0, tv);
        if (tv.getEllipsize() != null) {
            tv.setEllipsize(null);
        }
        Layout ll = tv.getLayout();
        if (ll == null) {
            return false;
        }
        this.mTextWidth = (int) tv.getLayout().getLineWidth(0);
        this.mViewWidth = tv.getWidth();
        if (this.mViewWidth > this.mTextWidth) {
            tv.setEllipsize(TextUtils.TruncateAt.END);
            v.cancelLongPress();
            return false;
        }
        this.mDraggingLabel = true;
        tv.setHorizontalFadingEdgeEnabled(true);
        v.cancelLongPress();
        return true;
    }

    @Override
    public boolean onLongClick(View view) {
        Object title;
        String query;
        String mime;
        if (BenesseExtension.getDchaState() != 0) {
            return false;
        }
        try {
            String artist = this.mService.getArtistName();
            String album = this.mService.getAlbumName();
            String song = this.mService.getTrackName();
            long audioid = this.mService.getAudioId();
            if (("<unknown>".equals(album) && "<unknown>".equals(artist) && song != null && song.startsWith("recording")) || audioid < 0) {
                return false;
            }
            Cursor c = MusicUtils.query(this, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioid), new String[]{"is_music"}, null, null, null);
            boolean ismusic = true;
            if (c != null) {
                if (c.moveToFirst()) {
                    ismusic = c.getInt(0) != 0;
                }
                c.close();
            }
            if (!ismusic) {
                return false;
            }
            boolean knownartist = (artist == null || "<unknown>".equals(artist)) ? false : true;
            boolean knownalbum = (album == null || "<unknown>".equals(album)) ? false : true;
            if (knownartist && view.equals(this.mArtistName.getParent())) {
                title = artist;
                query = artist;
                mime = "vnd.android.cursor.item/artist";
            } else if (knownalbum && view.equals(this.mAlbumName.getParent())) {
                title = album;
                if (knownartist) {
                    query = artist + " " + album;
                } else {
                    query = album;
                }
                mime = "vnd.android.cursor.item/album";
            } else if (view.equals(this.mTrackName.getParent()) || !knownartist || !knownalbum) {
                if (song == null || "<unknown>".equals(song)) {
                    return true;
                }
                title = song;
                if (knownartist) {
                    query = artist + " " + song;
                } else {
                    query = song;
                }
                mime = "audio/*";
            } else {
                throw new RuntimeException("shouldn't be here");
            }
            String title2 = getString(R.string.mediasearch, new Object[]{title});
            Intent i = new Intent();
            i.setFlags(268435456);
            i.setAction("android.intent.action.MEDIA_SEARCH");
            i.putExtra("query", query);
            if (knownartist) {
                i.putExtra("android.intent.extra.artist", artist);
            }
            if (knownalbum) {
                i.putExtra("android.intent.extra.album", album);
            }
            i.putExtra("android.intent.extra.title", song);
            i.putExtra("android.intent.extra.focus", mime);
            startActivity(Intent.createChooser(i, title2));
            return true;
        } catch (RemoteException e) {
            return true;
        } catch (NullPointerException e2) {
            return true;
        }
    }

    @Override
    public void onStop() {
        this.paused = true;
        this.mHandler.removeMessages(1);
        unregisterReceiver(this.mStatusListener);
        MusicUtils.unbindFromService(this.mToken);
        this.mService = null;
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.paused = false;
        this.mToken = MusicUtils.bindToService(this, this.osc);
        if (this.mToken == null) {
            this.mHandler.sendEmptyMessage(2);
        }
        IntentFilter f = new IntentFilter();
        f.addAction("com.android.music.playstatechanged");
        f.addAction("com.android.music.metachanged");
        f.addAction("android.intent.action.SCREEN_ON");
        f.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(this.mStatusListener, new IntentFilter(f));
        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTrackInfo();
        setPauseButtonImage();
    }

    @Override
    public void onDestroy() {
        this.mAlbumArtWorker.quit();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (MusicUtils.getCurrentAudioId() < 0) {
            return false;
        }
        menu.add(0, 6, 0, R.string.goto_start).setIcon(R.drawable.ic_menu_music_library);
        menu.add(0, 8, 0, R.string.party_shuffle);
        menu.addSubMenu(0, 1, 0, R.string.add_to_playlist).setIcon(android.R.drawable.ic_menu_add);
        menu.add(1, 14, 0, R.string.ringtone_menu_short).setIcon(R.drawable.ic_menu_set_as_ringtone);
        menu.add(1, 10, 0, R.string.delete_item).setIcon(R.drawable.ic_menu_delete);
        Intent i = new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL");
        if (getPackageManager().resolveActivity(i, 0) == null) {
            return true;
        }
        menu.add(0, 13, 0, R.string.effectspanel).setIcon(R.drawable.ic_menu_eq);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mService == null) {
            return false;
        }
        MenuItem item = menu.findItem(8);
        if (item != null) {
            int shuffle = MusicUtils.getCurrentShuffleMode();
            if (shuffle == 2) {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle_off);
            } else {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle);
            }
        }
        MenuItem item2 = menu.findItem(1);
        if (item2 != null) {
            SubMenu sub = item2.getSubMenu();
            MusicUtils.makePlaylistMenu(this, sub);
        }
        KeyguardManager km = (KeyguardManager) getSystemService("keyguard");
        menu.setGroupVisible(1, km.inKeyguardRestrictedInputMode() ? false : true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String f;
        try {
            switch (item.getItemId()) {
                case 3:
                    long[] list = {MusicUtils.getCurrentAudioId()};
                    long playlist = item.getIntent().getLongExtra("playlist", 0L);
                    MusicUtils.addToPlaylist(this, list, playlist);
                    return true;
                case 4:
                    Intent intent = new Intent();
                    intent.setClass(this, CreatePlaylist.class);
                    startActivityForResult(intent, 4);
                    return true;
                case 6:
                    Intent intent2 = new Intent();
                    intent2.setClass(this, MusicBrowserActivity.class);
                    intent2.setFlags(335544320);
                    startActivity(intent2);
                    finish();
                    break;
                case 8:
                    MusicUtils.togglePartyShuffle();
                    setShuffleButtonImage();
                    break;
                case 10:
                    if (this.mService == null) {
                        return true;
                    }
                    long[] list2 = {MusicUtils.getCurrentAudioId()};
                    Bundle b = new Bundle();
                    if (Environment.isExternalStorageRemovable()) {
                        f = getString(R.string.delete_song_desc, new Object[]{this.mService.getTrackName()});
                    } else {
                        f = getString(R.string.delete_song_desc_nosdcard, new Object[]{this.mService.getTrackName()});
                    }
                    b.putString("description", f);
                    b.putLongArray("items", list2);
                    Intent intent3 = new Intent();
                    intent3.setClass(this, DeleteItems.class);
                    intent3.putExtras(b);
                    startActivityForResult(intent3, -1);
                    return true;
                case 13:
                    Intent i = new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL");
                    i.putExtra("android.media.extra.AUDIO_SESSION", this.mService.getAudioSessionId());
                    startActivityForResult(i, 13);
                    return true;
                case 14:
                    if (this.mService == null) {
                        return true;
                    }
                    MusicUtils.setRingtone(this, this.mService.getAudioId());
                    return true;
            }
        } catch (RemoteException e) {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == -1) {
            switch (requestCode) {
                case 4:
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long[] list = {MusicUtils.getCurrentAudioId()};
                        int playlist = Integer.parseInt(uri.getLastPathSegment());
                        MusicUtils.addToPlaylist(this, list, playlist);
                    }
                    break;
            }
        }
    }

    private boolean seekMethod1(int keyCode) {
        if (this.mService == null) {
            return false;
        }
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 3; y++) {
                if (this.keyboard[y][x] == keyCode) {
                    int dir = 0;
                    if (x == this.lastX && y == this.lastY) {
                        dir = 0;
                    } else if (y == 0 && this.lastY == 0 && x > this.lastX) {
                        dir = 1;
                    } else if (y == 0 && this.lastY == 0 && x < this.lastX) {
                        dir = -1;
                    } else if (y == 2 && this.lastY == 2 && x > this.lastX) {
                        dir = -1;
                    } else if (y == 2 && this.lastY == 2 && x < this.lastX) {
                        dir = 1;
                    } else if (y < this.lastY && x <= 4) {
                        dir = 1;
                    } else if (y < this.lastY && x >= 5) {
                        dir = -1;
                    } else if (y > this.lastY && x <= 4) {
                        dir = -1;
                    } else if (y > this.lastY && x >= 5) {
                        dir = 1;
                    }
                    this.lastX = x;
                    this.lastY = y;
                    try {
                        this.mService.seek(this.mService.position() + ((long) (dir * 5)));
                    } catch (RemoteException e) {
                    }
                    refreshNow();
                    return true;
                }
            }
        }
        this.lastX = -1;
        this.lastY = -1;
        return false;
    }

    private boolean seekMethod2(int keyCode) {
        if (this.mService == null) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (this.keyboard[0][i] == keyCode) {
                int seekpercentage = (i * 100) / 10;
                try {
                    this.mService.seek((this.mService.duration() * ((long) seekpercentage)) / 100);
                } catch (RemoteException e) {
                }
                refreshNow();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            switch (keyCode) {
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
                                scanBackward(-1, event.getEventTime() - event.getDownTime());
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
                                scanForward(-1, event.getEventTime() - event.getDownTime());
                                this.mPauseButton.requestFocus();
                                this.mStartSeekPos = -1L;
                            }
                        }
                        this.mSeeking = false;
                        this.mPosOverride = -1L;
                        return true;
                    }
                    break;
                case 89:
                    this.mRepeatCount = -1;
                    this.eventstoptime = event.getEventTime();
                    Log.d("MediaPlaybackActivity", "eventstoptime = " + this.eventstoptime);
                    return true;
                case 90:
                    this.mRepeatCount = -1;
                    this.eventstoptime = event.getEventTime();
                    Log.d("MediaPlaybackActivity", "eventstoptime = " + this.eventstoptime);
                    return true;
            }
        } catch (RemoteException e) {
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean useDpadMusicControl() {
        return this.mDeviceHasDpad && (this.mPrevButton.isFocused() || this.mNextButton.isFocused() || this.mPauseButton.isFocused());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int repcnt = event.getRepeatCount();
        if (this.seekmethod != 0 ? seekMethod2(keyCode) : seekMethod1(keyCode)) {
            return true;
        }
        switch (keyCode) {
            case 21:
                if (useDpadMusicControl()) {
                    if (!this.mPrevButton.hasFocus()) {
                        this.mPrevButton.requestFocus();
                    }
                    scanBackward(repcnt, event.getEventTime() - event.getDownTime());
                    return true;
                }
                break;
            case 22:
                if (useDpadMusicControl()) {
                    if (!this.mNextButton.hasFocus()) {
                        this.mNextButton.requestFocus();
                    }
                    scanForward(repcnt, event.getEventTime() - event.getDownTime());
                    return true;
                }
                break;
            case 23:
            case 62:
                doPauseResume();
                return true;
            case 47:
                toggleShuffle();
                return true;
            case 76:
                this.seekmethod = 1 - this.seekmethod;
                return true;
            case 89:
                if (this.mRepeatCount == 0) {
                    this.eventstarttime = event.getEventTime();
                }
                long eventtimerw = event.getEventTime();
                if (eventtimerw - this.eventstoptime > 100) {
                    Log.e("MediaPlaybackActivity", "re-get eventstarttime");
                    this.eventstarttime = event.getEventTime();
                    eventtimerw = event.getEventTime();
                    this.mRepeatCount = 0;
                }
                Log.d("MediaPlaybackActivity", "event time = " + eventtimerw + "event start time = " + this.eventstarttime + "event stop time = " + this.eventstoptime);
                int i = this.mRepeatCount;
                this.mRepeatCount = i + 1;
                scanBackward(i, eventtimerw - this.eventstarttime);
                return true;
            case 90:
                if (this.mRepeatCount == 0) {
                    this.eventstarttime = event.getEventTime();
                }
                long eventtimeff = event.getEventTime();
                if (eventtimeff - this.eventstoptime > 100) {
                    this.eventstarttime = event.getEventTime();
                    eventtimeff = event.getEventTime();
                    this.mRepeatCount = 0;
                }
                Log.d("MediaPlaybackActivity", "event time = " + eventtimeff + "event start time = " + this.eventstarttime + "event stop time = " + this.eventstoptime);
                int i2 = this.mRepeatCount;
                this.mRepeatCount = i2 + 1;
                scanForward(i2, eventtimeff - this.eventstarttime);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void scanBackward(int repcnt, long delta) {
        long delta2;
        if (this.mService != null) {
            try {
                if (repcnt == 0) {
                    this.mStartSeekPos = this.mService.position();
                    this.mLastSeekEventTime = 0L;
                    this.mSeeking = false;
                    return;
                }
                this.mSeeking = true;
                if (delta < 5000) {
                    delta2 = delta * 10;
                } else {
                    delta2 = 50000 + ((delta - 5000) * 40);
                }
                long newpos = this.mStartSeekPos - delta2;
                if (newpos < 0) {
                    this.mService.prev();
                    long duration = this.mService.duration();
                    this.mStartSeekPos += duration;
                    newpos += duration;
                }
                if (delta2 - this.mLastSeekEventTime > 250 || repcnt < 0) {
                    this.mService.seek(newpos);
                    this.mLastSeekEventTime = delta2;
                }
                if (repcnt >= 0) {
                    this.mPosOverride = newpos;
                } else {
                    this.mPosOverride = -1L;
                }
                refreshNow();
            } catch (RemoteException e) {
            }
        }
    }

    private void scanForward(int repcnt, long delta) {
        long delta2;
        if (this.mService != null) {
            try {
                if (repcnt == 0) {
                    this.mStartSeekPos = this.mService.position();
                    this.mLastSeekEventTime = 0L;
                    this.mSeeking = false;
                    return;
                }
                this.mSeeking = true;
                if (delta < 5000) {
                    delta2 = delta * 10;
                } else {
                    delta2 = 50000 + ((delta - 5000) * 40);
                }
                long newpos = this.mStartSeekPos + delta2;
                long duration = this.mService.duration();
                if (newpos >= duration) {
                    this.mService.next();
                    this.mStartSeekPos -= duration;
                    newpos -= duration;
                }
                if (delta2 - this.mLastSeekEventTime > 250 || repcnt < 0) {
                    this.mService.seek(newpos);
                    this.mLastSeekEventTime = delta2;
                }
                if (repcnt >= 0) {
                    this.mPosOverride = newpos;
                } else {
                    this.mPosOverride = -1L;
                }
                refreshNow();
            } catch (RemoteException e) {
            }
        }
    }

    private void doPauseResume() {
        try {
            if (this.mService != null) {
                if (this.mService.isPlaying()) {
                    this.mService.pause();
                } else {
                    this.mService.play();
                }
                setPauseButtonImage();
            }
        } catch (RemoteException e) {
        }
    }

    private void toggleShuffle() {
        if (this.mService != null) {
            try {
                int shuffle = this.mService.getShuffleMode();
                if (shuffle == 0) {
                    this.mService.setShuffleMode(1);
                    if (this.mService.getRepeatMode() == 1) {
                        this.mService.setRepeatMode(2);
                        setRepeatButtonImage();
                    }
                    showToast(R.string.shuffle_on_notif);
                } else if (shuffle == 1 || shuffle == 2) {
                    this.mService.setShuffleMode(0);
                    showToast(R.string.shuffle_off_notif);
                } else {
                    Log.e("MediaPlaybackActivity", "Invalid shuffle mode: " + shuffle);
                }
                setShuffleButtonImage();
            } catch (RemoteException e) {
            }
        }
    }

    private void cycleRepeat() {
        if (this.mService != null) {
            try {
                int mode = this.mService.getRepeatMode();
                if (mode == 0) {
                    this.mService.setRepeatMode(2);
                    showToast(R.string.repeat_all_notif);
                } else if (mode == 2) {
                    this.mService.setRepeatMode(1);
                    if (this.mService.getShuffleMode() != 0) {
                        this.mService.setShuffleMode(0);
                        setShuffleButtonImage();
                    }
                    showToast(R.string.repeat_current_notif);
                } else {
                    this.mService.setRepeatMode(0);
                    showToast(R.string.repeat_off_notif);
                }
                setRepeatButtonImage();
            } catch (RemoteException e) {
            }
        }
    }

    private void showToast(int resid) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this, "", 0);
        }
        this.mToast.setText(resid);
        this.mToast.show();
    }

    private void startPlayback() {
        String filename;
        if (this.mService != null) {
            Intent intent = getIntent();
            Uri uri = intent.getData();
            if (uri != null && uri.toString().length() > 0) {
                String scheme = uri.getScheme();
                if ("file".equals(scheme)) {
                    filename = uri.getPath();
                } else {
                    filename = uri.toString();
                }
                try {
                    this.mService.stop();
                    this.mService.openFile(filename);
                    this.mService.play();
                    setIntent(new Intent());
                } catch (Exception ex) {
                    Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
                }
            }
            updateTrackInfo();
            long next = refreshNow();
            queueNextRefresh(next);
        }
    }

    private void setRepeatButtonImage() {
        if (this.mService != null) {
            try {
                switch (this.mService.getRepeatMode()) {
                    case 1:
                        this.mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_once_btn);
                        break;
                    case 2:
                        this.mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_all_btn);
                        break;
                    default:
                        this.mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
                        break;
                }
            } catch (RemoteException e) {
            }
        }
    }

    private void setShuffleButtonImage() {
        if (this.mService != null) {
            try {
                switch (this.mService.getShuffleMode()) {
                    case 0:
                        this.mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
                        break;
                    case 1:
                    default:
                        this.mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
                        break;
                    case 2:
                        this.mShuffleButton.setImageResource(R.drawable.ic_mp_partyshuffle_on_btn);
                        break;
                }
            } catch (RemoteException e) {
            }
        }
    }

    private void setPauseButtonImage() {
        try {
            if (this.mService != null && this.mService.isPlaying()) {
                this.mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                this.mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        } catch (RemoteException e) {
        }
    }

    private void queueNextRefresh(long delay) {
        if (!this.paused && this.mScreenOn) {
            Message msg = this.mHandler.obtainMessage(1);
            this.mHandler.removeMessages(1);
            this.mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        if (this.mService == null) {
            return 500L;
        }
        try {
            long pos = this.mPosOverride < 0 ? this.mService.position() : this.mPosOverride;
            if (pos >= 0 && this.mDuration > 0) {
                this.mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
                int progress = (int) ((1000 * pos) / this.mDuration);
                this.mProgress.setProgress(progress);
                if (this.mService.isPlaying()) {
                    this.mCurrentTime.setVisibility(0);
                } else {
                    int vis = this.mCurrentTime.getVisibility();
                    this.mCurrentTime.setVisibility(vis == 4 ? 0 : 4);
                    return 500L;
                }
            } else {
                this.mCurrentTime.setText("--:--");
                this.mProgress.setProgress(1000);
            }
            long remaining = 1000 - (pos % 1000);
            int width = this.mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            long smoothrefreshtime = this.mDuration / ((long) width);
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20L;
            }
            return smoothrefreshtime;
        } catch (RemoteException e) {
            return 500L;
        }
    }

    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;

        AlbumSongIdWrapper(long aid, long sid) {
            this.albumid = aid;
            this.songid = sid;
        }
    }

    private void updateTrackInfo() {
        if (this.mService != null) {
            try {
                String path = this.mService.getPath();
                if (path == null) {
                    finish();
                    return;
                }
                long songid = this.mService.getAudioId();
                if (songid < 0 && path.toLowerCase().startsWith("http://")) {
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
                    long albumid = this.mService.getAlbumId();
                    if ("<unknown>".equals(albumName)) {
                        albumName = getString(R.string.unknown_album_name);
                        albumid = -1;
                    }
                    this.mAlbumName.setText(albumName);
                    this.mTrackName.setText(this.mService.getTrackName());
                    this.mAlbumArtHandler.removeMessages(3);
                    this.mAlbumArtHandler.obtainMessage(3, new AlbumSongIdWrapper(albumid, songid)).sendToTarget();
                    this.mAlbum.setVisibility(0);
                }
                this.mDuration = this.mService.duration();
                this.mTotalTime.setText(MusicUtils.makeTimeString(this, this.mDuration / 1000));
            } catch (RemoteException e) {
                finish();
            }
        }
    }

    public class AlbumArtHandler extends Handler {
        private long mAlbumId;

        public AlbumArtHandler(Looper looper) {
            super(looper);
            this.mAlbumId = -1L;
        }

        @Override
        public void handleMessage(Message msg) {
            long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
            long songid = ((AlbumSongIdWrapper) msg.obj).songid;
            if (msg.what == 3) {
                if (this.mAlbumId != albumid || albumid < 0) {
                    Message numsg = MediaPlaybackActivity.this.mHandler.obtainMessage(4, null);
                    MediaPlaybackActivity.this.mHandler.removeMessages(4);
                    MediaPlaybackActivity.this.mHandler.sendMessageDelayed(numsg, 300L);
                    Bitmap bm = MusicUtils.getArtwork(MediaPlaybackActivity.this, songid, albumid, false);
                    if (bm == null) {
                        bm = MusicUtils.getArtwork(MediaPlaybackActivity.this, songid, -1L);
                        albumid = -1;
                    }
                    if (bm != null) {
                        Message numsg2 = MediaPlaybackActivity.this.mHandler.obtainMessage(4, bm);
                        MediaPlaybackActivity.this.mHandler.removeMessages(4);
                        MediaPlaybackActivity.this.mHandler.sendMessage(numsg2);
                    }
                    this.mAlbumId = albumid;
                }
            }
        }
    }

    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(1);
            t.start();
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
}
