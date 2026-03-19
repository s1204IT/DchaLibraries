package com.android.music;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;
import java.util.ArrayList;
import java.util.HashMap;

public class MusicBrowserActivity extends TabActivity implements ServiceConnection, ViewPager.OnPageChangeListener, TabHost.OnTabChangeListener {
    private LocalActivityManager mActivityManager;
    private int mCurrentTab;
    private int mOrientaiton;
    private View mOverflowMenuButton;
    private int mOverflowMenuButtonId;
    Bundle mSavedInstanceState;
    ImageButton mSearchButton;
    MenuItem mSearchItem;
    private int mTabCount;
    private TabHost mTabHost;
    private MusicUtils.ServiceToken mToken;
    private ViewPager mViewPager;
    private static boolean mPermissionReqProcessed = false;
    private static final HashMap<String, Integer> TAB_MAP = new HashMap<>(5);
    private ArrayList<View> mPagers = new ArrayList<>(4);
    private IMediaPlaybackService mService = null;
    private PopupMenu mPopupMenu = null;
    private boolean mPopupMenuShowing = false;
    private boolean mHasMenukey = true;
    private boolean mIsSdcardMounted = true;
    private boolean mSearchViewShowing = false;
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String str) {
            Intent intent = new Intent();
            intent.setClass(MusicBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra("query", str);
            MusicBrowserActivity.this.startActivity(intent);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String str) {
            return false;
        }
    };
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicLogUtils.v("MusicBrowser", "mTrackListListener");
            if (MusicBrowserActivity.this.mService != null) {
                MusicUtils.updateNowPlaying(MusicBrowserActivity.this, MusicBrowserActivity.this.mOrientaiton);
                MusicBrowserActivity.this.updatePlaybackTab();
            }
        }
    };
    private BroadcastReceiver mSdcardstatustListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicBrowserActivity.this.mIsSdcardMounted = intent.getBooleanExtra("onoff", false);
            if (MusicBrowserActivity.this.mIsSdcardMounted) {
                MusicLogUtils.v("MusicBrowser", "Sdcard normal");
                View viewFindViewById = MusicBrowserActivity.this.findViewById(R.id.normal_view);
                if (viewFindViewById != null) {
                    viewFindViewById.setVisibility(0);
                }
                View viewFindViewById2 = MusicBrowserActivity.this.findViewById(R.id.sd_message);
                if (viewFindViewById2 != null) {
                    viewFindViewById2.setVisibility(8);
                }
                View viewFindViewById3 = MusicBrowserActivity.this.findViewById(R.id.sd_icon);
                if (viewFindViewById3 != null) {
                    viewFindViewById3.setVisibility(8);
                }
                View viewFindViewById4 = MusicBrowserActivity.this.findViewById(R.id.sd_error);
                if (viewFindViewById4 != null) {
                    viewFindViewById4.setVisibility(8);
                }
                if (MusicBrowserActivity.this.mService != null) {
                    MusicUtils.updateNowPlaying(MusicBrowserActivity.this, MusicBrowserActivity.this.mOrientaiton);
                    return;
                }
                return;
            }
            MusicLogUtils.v("MusicBrowser", "Sdcard error");
            View viewFindViewById5 = MusicBrowserActivity.this.findViewById(R.id.normal_view);
            if (viewFindViewById5 != null) {
                viewFindViewById5.setVisibility(8);
            }
            View viewFindViewById6 = MusicBrowserActivity.this.findViewById(R.id.sd_icon);
            if (viewFindViewById6 != null) {
                viewFindViewById6.setVisibility(0);
            }
            TextView textView = (TextView) MusicBrowserActivity.this.findViewById(R.id.sd_message);
            if (textView != null) {
                textView.setVisibility(0);
                textView.setText(intent.getIntExtra("message", R.string.sdcard_error_message));
            }
            View viewFindViewById7 = MusicBrowserActivity.this.findViewById(R.id.sd_error);
            if (viewFindViewById7 != null) {
                viewFindViewById7.setVisibility(0);
            }
        }
    };

    static {
        TAB_MAP.put("Artist", 0);
        TAB_MAP.put("Album", 1);
        TAB_MAP.put("Song", 2);
        TAB_MAP.put("Playlist", 3);
        TAB_MAP.put("Playback", 4);
    }

    @Override
    public void onCreate(Bundle bundle) {
        PDebug.Start("MusicBrowserActivity.onCreate");
        this.mSavedInstanceState = bundle;
        super.onCreate(bundle);
        setContentView(R.layout.main);
        if (getApplicationContext().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            requestMusicPermissions();
            mPermissionReqProcessed = false;
        } else {
            mPermissionReqProcessed = true;
            onCreateContinue(this.mSavedInstanceState);
        }
        ((NotificationManager) getSystemService("notification")).createNotificationChannel(new NotificationChannel("music_notification_channel", "MUSIC", 2));
    }

    public void onCreateContinue(Bundle bundle) {
        MusicLogUtils.v("MusicBrowser", "onCreate");
        getActionBar().hide();
        setVolumeControlStream(3);
        PDebug.Start("MusicBrowserActivity.bindToService()");
        this.mToken = MusicUtils.bindToService(this, this);
        PDebug.End("MusicBrowserActivity.bindToService()");
        this.mHasMenukey = ViewConfiguration.get(this).hasPermanentMenuKey();
        PDebug.Start("MusicBrowserActivity.dispatchCreate()");
        this.mActivityManager = new LocalActivityManager(this, false);
        this.mActivityManager.dispatchCreate(bundle);
        PDebug.End("MusicBrowserActivity.dispatchCreate()");
        this.mTabHost = getTabHost();
        PDebug.Start("MusicBrowserActivity.initTab()");
        initTab();
        PDebug.End("MusicBrowserActivity.initTab()");
        PDebug.Start("MusicBrowserActivity.setCurrentTab()");
        this.mCurrentTab = MusicUtils.getIntPref(this, "activetab", 0);
        MusicLogUtils.v("MusicBrowser", "onCreate mCurrentTab: " + this.mCurrentTab);
        if (this.mCurrentTab < 0 || this.mCurrentTab >= this.mTabCount) {
            this.mCurrentTab = 0;
        }
        if (this.mCurrentTab == 0) {
            this.mTabHost.setCurrentTab(1);
        }
        this.mTabHost.setOnTabChangedListener(this);
        PDebug.End("MusicBrowserActivity.setCurrentTab()");
        PDebug.Start("MusicBrowserActivity.initPager()");
        initPager();
        PDebug.End("MusicBrowserActivity.initPager()");
        PDebug.Start("MusicBrowserActivity.setAdapter()");
        this.mViewPager = (ViewPager) findViewById(R.id.viewpage);
        this.mViewPager.setAdapter(new MusicPagerAdapter());
        this.mViewPager.setOnPageChangeListener(this);
        PDebug.End("MusicBrowserActivity.setAdapter()");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.sdcardstatusupdate");
        registerReceiver(this.mSdcardstatustListener, intentFilter);
        createFakeMenu();
        initSearchButton();
        PDebug.End("MusicBrowserActivity.onCreate");
    }

    private void requestMusicPermissions() {
        requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1) {
            if (iArr.length > 0 && iArr[0] == 0) {
                mPermissionReqProcessed = true;
                onCreateContinue(this.mSavedInstanceState);
                onResumeContinue();
            } else {
                finish();
                Toast.makeText(this, R.string.music_storage_permission_deny, 0).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPermissionReqProcessed) {
            onResumeContinue();
        }
    }

    public void onResumeContinue() {
        PDebug.Start("MusicBrowserActivity.onResume");
        MusicLogUtils.v("MusicBrowser", "onResume>>>");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.metachanged");
        registerReceiver(this.mTrackListListener, intentFilter);
        PDebug.Start("MusicBrowserActivity.setCurrentTab()");
        this.mTabHost.setCurrentTab(this.mCurrentTab);
        PDebug.End("MusicBrowserActivity.setCurrentTab()");
        PDebug.Start("MusicBrowserActivity.dispatchResume()");
        this.mActivityManager.dispatchResume();
        PDebug.End("MusicBrowserActivity.dispatchResume()");
        MusicLogUtils.v("MusicBrowser", "onResume<<<");
        PDebug.End("MusicBrowserActivity.onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPermissionReqProcessed) {
            MusicLogUtils.v("MusicBrowser", "onPause");
            unregisterReceiver(this.mTrackListListener);
            this.mActivityManager.dispatchPause(false);
            MusicUtils.setIntPref(this, "activetab", this.mCurrentTab);
        }
    }

    @Override
    public void onStop() {
        if (mPermissionReqProcessed) {
            if (this.mPopupMenu != null) {
                this.mPopupMenu.dismiss();
                this.mPopupMenuShowing = false;
            }
            this.mActivityManager.dispatchStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mPermissionReqProcessed) {
            MusicLogUtils.v("MusicBrowser", "onDestroy");
            if (this.mToken != null) {
                MusicUtils.unbindFromService(this.mToken);
                this.mService = null;
            }
            unregisterReceiver(this.mSdcardstatustListener);
            this.mActivityManager.dispatchDestroy(false);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (mPermissionReqProcessed) {
            int intExtra = this.mCurrentTab;
            if (intent != null) {
                intExtra = intent.getIntExtra("start_activity_tab_id", this.mCurrentTab);
            }
            MusicLogUtils.v("MusicBrowser", "onActivityResult: startActivityTab = " + intExtra);
            Activity activity = this.mActivityManager.getActivity(getStringId(intExtra));
            if (activity == null) {
            }
            switch (intExtra) {
                case 0:
                    ((ArtistAlbumBrowserActivity) activity).onActivityResult(i, i2, intent);
                    break;
                case 1:
                    ((AlbumBrowserActivity) activity).onActivityResult(i, i2, intent);
                    break;
                case 2:
                    ((TrackBrowserActivity) activity).onActivityResult(i, i2, intent);
                    break;
                case 3:
                    ((PlaylistBrowserActivity) activity).onActivityResult(i, i2, intent);
                    break;
                default:
                    MusicLogUtils.v("MusicBrowser", "default");
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        int i;
        super.onConfigurationChanged(configuration);
        if (mPermissionReqProcessed) {
            MusicLogUtils.v("MusicBrowser", "onConfigurationChanged>>");
            TabWidget tabWidget = this.mTabHost.getTabWidget();
            this.mOrientaiton = configuration.orientation;
            if (this.mOrientaiton == 2) {
                MusicLogUtils.v("MusicBrowser", "onConfigurationChanged--LandScape");
                i = 0;
            } else {
                i = 8;
            }
            for (int i2 = 4; i2 < this.mTabCount; i2++) {
                View childTabViewAt = tabWidget.getChildTabViewAt(i2);
                if (childTabViewAt != null) {
                    childTabViewAt.setVisibility(i);
                }
            }
            for (int i3 = 0; i3 < 4; i3++) {
                Activity activity = this.mActivityManager.getActivity(getStringId(i3));
                if (activity != null) {
                    activity.onConfigurationChanged(configuration);
                }
            }
            if (!this.mHasMenukey) {
                boolean z = this.mPopupMenuShowing;
                if (z && this.mPopupMenu != null) {
                    this.mPopupMenu.dismiss();
                    MusicLogUtils.v("MusicBrowser", "changeFakeMenu:mPopupMenu.dismiss()");
                }
                MusicLogUtils.v("MusicBrowser", "changeFakeMenu:popupMenuShowing=" + z);
                createFakeMenu();
                if (!this.mSearchViewShowing && this.mOverflowMenuButton != null) {
                    this.mOverflowMenuButton.setEnabled(true);
                }
                if (z && this.mOverflowMenuButton != null) {
                    this.mOverflowMenuButton.setSoundEffectsEnabled(false);
                    this.mOverflowMenuButton.performClick();
                    this.mOverflowMenuButton.setSoundEffectsEnabled(true);
                    MusicLogUtils.v("MusicBrowser", "changeFakeMenu:performClick()");
                }
            }
            if (this.mService != null) {
                MusicLogUtils.v("MusicBrowser", "mSearchViewShowing:" + this.mSearchViewShowing);
                if (this.mSearchViewShowing) {
                    this.mSearchButton.setVisibility(8);
                } else {
                    this.mSearchButton.setVisibility(0);
                }
                MusicUtils.updateNowPlaying(this, this.mOrientaiton);
                updatePlaybackTab();
            }
            MusicLogUtils.v("MusicBrowser", "onConfigurationChanged--mCurrentTab = " + this.mCurrentTab);
            this.mTabHost.setCurrentTab(this.mCurrentTab);
            this.mViewPager.setAdapter(new MusicPagerAdapter());
            onTabChanged(getStringId(this.mCurrentTab));
            MusicLogUtils.v("MusicBrowser", "onConfigurationChanged<<");
        }
    }

    private void createFakeMenu() {
        if (mPermissionReqProcessed) {
            if (this.mHasMenukey) {
                MusicLogUtils.v("MusicBrowser", "createFakeMenu Quit when there has Menu Key");
                return;
            }
            if (this.mOrientaiton == 2) {
                this.mOverflowMenuButtonId = R.id.overflow_menu;
                this.mOverflowMenuButton = findViewById(R.id.overflow_menu);
            } else {
                this.mOverflowMenuButtonId = R.id.overflow_menu_nowplaying;
                this.mOverflowMenuButton = findViewById(R.id.overflow_menu_nowplaying);
                View view = (View) this.mOverflowMenuButton.getParent();
                if (view != null) {
                    view.setVisibility(0);
                }
            }
            if (this.mOverflowMenuButton != null) {
                this.mOverflowMenuButton.setVisibility(0);
                this.mOverflowMenuButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        MusicLogUtils.v("MusicBrowser", "createFakeMenu:onClick()");
                        if (view2.getId() == MusicBrowserActivity.this.mOverflowMenuButtonId) {
                            PopupMenu popupMenu = new PopupMenu(MusicBrowserActivity.this, MusicBrowserActivity.this.mOverflowMenuButton);
                            MusicBrowserActivity.this.mPopupMenu = popupMenu;
                            Menu menu = popupMenu.getMenu();
                            MusicBrowserActivity.this.onCreateOptionsMenu(menu);
                            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    return MusicBrowserActivity.this.onOptionsItemSelected(menuItem);
                                }
                            });
                            popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                                @Override
                                public void onDismiss(PopupMenu popupMenu2) {
                                    MusicBrowserActivity.this.mPopupMenuShowing = false;
                                    MusicLogUtils.v("MusicBrowser", "createFakeMenu:onDismiss() called");
                                }
                            });
                            MusicBrowserActivity.this.onPrepareOptionsMenu(menu);
                            MusicBrowserActivity.this.mPopupMenuShowing = true;
                            MusicLogUtils.v("MusicBrowser", "createFakeMenu:popupMenu.show()");
                            popupMenu.show();
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mPermissionReqProcessed) {
            menu.add(0, 19, 0, R.string.play_all);
            menu.add(0, 8, 0, R.string.party_shuffle);
            menu.add(0, 9, 0, R.string.shuffle_all);
            menu.add(0, 13, 0, R.string.effects_list_title);
            this.mSearchItem = MusicUtils.addSearchView(this, menu, this.mQueryTextListener, null);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
        super.onPrepareOptionsMenu(menu);
        if (mPermissionReqProcessed) {
            if (!this.mIsSdcardMounted) {
                MusicLogUtils.v("MusicBrowser", "Sdcard is not mounted, don't show option menu!");
                return false;
            }
            menu.findItem(19).setVisible(this.mCurrentTab == 2);
            menu.findItem(9).setVisible(this.mCurrentTab != 3);
            MusicUtils.setEffectPanelMenu(getApplicationContext(), menu);
            this.mSearchItem.setVisible(this.mOrientaiton == 2);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 13) {
            return MusicUtils.startEffectPanel(this);
        }
        if (itemId == 19) {
            Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, "title_key");
            if (cursorQuery != null) {
                MusicUtils.playAll(this, cursorQuery);
                cursorQuery.close();
            }
            return true;
        }
        if (itemId != R.id.search) {
            switch (itemId) {
                case 8:
                    MusicUtils.togglePartyShuffle();
                    return true;
                case 9:
                    Cursor cursorQuery2 = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, "title_key");
                    if (cursorQuery2 != null) {
                        MusicUtils.shuffleAll(this, cursorQuery2);
                        cursorQuery2.close();
                    }
                    return true;
                default:
                    return super.onOptionsItemSelected(menuItem);
            }
        }
        onSearchRequested();
        this.mSearchViewShowing = true;
        return true;
    }

    private String getStringId(int i) {
        switch (i) {
            case 1:
                return "Album";
            case 2:
                return "Song";
            case 3:
                return "Playlist";
            case 4:
                return "Playback";
            default:
                MusicLogUtils.v("MusicBrowser", "ARTIST_INDEX or default");
                return "Artist";
        }
    }

    private void initTab() {
        if (mPermissionReqProcessed) {
            MusicLogUtils.v("MusicBrowser", "initTab>>");
            TabWidget tabWidget = (TabWidget) getLayoutInflater().inflate(R.layout.buttonbar, (ViewGroup) null);
            this.mOrientaiton = getResources().getConfiguration().orientation;
            this.mTabCount = tabWidget.getChildCount();
            if (this.mHasMenukey) {
                this.mTabCount--;
            }
            for (int i = 0; i < this.mTabCount; i++) {
                View childAt = tabWidget.getChildAt(0);
                if (childAt != null) {
                    tabWidget.removeView(childAt);
                }
                MusicLogUtils.v("MusicBrowser", "addTab:" + i);
                this.mTabHost.addTab(this.mTabHost.newTabSpec(getStringId(i)).setIndicator(childAt).setContent(android.R.id.tabcontent));
            }
            if (this.mOrientaiton == 1) {
                TabWidget tabWidget2 = this.mTabHost.getTabWidget();
                for (int i2 = 4; i2 < this.mTabCount; i2++) {
                    View childTabViewAt = tabWidget2.getChildTabViewAt(i2);
                    if (childTabViewAt != null) {
                        childTabViewAt.setVisibility(8);
                    }
                    MusicLogUtils.v("MusicBrowser", "set tab gone:" + i2);
                }
            }
            MusicLogUtils.v("MusicBrowser", "initTab<<");
        }
    }

    private View getView(int i) {
        MusicLogUtils.v("MusicBrowser", "getView>>>index = " + i);
        Intent intent = new Intent("android.intent.action.PICK");
        switch (i) {
            case 0:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/artistalbum");
                break;
            case 1:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                break;
            case 2:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                break;
            case 3:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/playlist");
                break;
            default:
                MusicLogUtils.v("MusicBrowser", "default");
                return null;
        }
        intent.putExtra("withtabs", true);
        intent.addFlags(67108864);
        View decorView = this.mActivityManager.startActivity(getStringId(i), intent).getDecorView();
        MusicLogUtils.v("MusicBrowser", "getView<<<");
        return decorView;
    }

    private void initPager() {
        this.mPagers.clear();
        int i = 0;
        while (i <= 3) {
            this.mPagers.add(i == this.mCurrentTab ? getView(i) : null);
            i++;
        }
    }

    private void updatePlaybackTab() {
        long audioId;
        int i;
        boolean z;
        TextView textView = (TextView) this.mTabHost.getTabWidget().getChildTabViewAt(4);
        if (textView == null) {
            return;
        }
        try {
            if (this.mService != null) {
                audioId = this.mService.getAudioId();
            } else {
                audioId = -1;
            }
        } catch (RemoteException e) {
            MusicLogUtils.v("MusicBrowser", "updatePlaybackTab getAudioId remote excption:" + e);
            audioId = -1L;
        }
        if (audioId == -1) {
            z = false;
            i = 128;
        } else {
            i = 255;
            z = true;
        }
        textView.setEnabled(z);
        Drawable drawable = textView.getCompoundDrawables()[1];
        if (drawable != null) {
            drawable.setAlpha(i);
        }
        MusicLogUtils.v("MusicBrowser", "updatePlaybackTab:" + z);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (mPermissionReqProcessed) {
            this.mService = IMediaPlaybackService.Stub.asInterface(iBinder);
            String stringExtra = getIntent().getStringExtra("autoshuffle");
            if (this.mService != null) {
                if (Boolean.valueOf(stringExtra).booleanValue()) {
                    try {
                        this.mService.setShuffleMode(2);
                    } catch (RemoteException e) {
                        MusicLogUtils.v("MusicBrowser", "onServiceConnected setShuffleMode remote excption:" + e);
                    }
                }
                MusicUtils.updateNowPlaying(this, this.mOrientaiton);
                updatePlaybackTab();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (mPermissionReqProcessed) {
            this.mService = null;
            finish();
        }
    }

    @Override
    public void onTabChanged(String str) {
        int iIntValue = TAB_MAP.get(str).intValue();
        MusicLogUtils.v("MusicBrowser", "onTabChanged-tabId:" + str);
        if (iIntValue >= 0 && iIntValue <= 3) {
            this.mViewPager.setCurrentItem(iIntValue);
            this.mCurrentTab = iIntValue;
        } else if (iIntValue == 4) {
            startActivity(new Intent(this, (Class<?>) MediaPlaybackActivity.class));
        }
    }

    @Override
    public void onPageSelected(int i) {
        MusicLogUtils.v("MusicBrowser", "onPageSelected-position:" + i);
        this.mTabHost.setCurrentTab(i);
    }

    @Override
    public void onPageScrolled(int i, float f, int i2) {
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    private class MusicPagerAdapter extends PagerAdapter {
        private MusicPagerAdapter() {
        }

        @Override
        public void destroyItem(View view, int i, Object obj) {
            ((ViewPager) view).removeView((View) MusicBrowserActivity.this.mPagers.get(i));
        }

        @Override
        public Object instantiateItem(View view, int i) {
            ViewPager viewPager = (ViewPager) view;
            View view2 = (View) MusicBrowserActivity.this.mPagers.get(i);
            MusicLogUtils.v("MusicBrowser", "instantiateItem-position:" + i);
            if (view2 == null) {
                view2 = MusicBrowserActivity.this.getView(i);
                MusicBrowserActivity.this.mPagers.remove(i);
                MusicBrowserActivity.this.mPagers.add(i, view2);
                MusicBrowserActivity.this.mActivityManager.dispatchResume();
            }
            viewPager.addView(view2);
            return MusicBrowserActivity.this.mPagers.get(i);
        }

        @Override
        public int getCount() {
            return MusicBrowserActivity.this.mPagers.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            if (view == null) {
                return false;
            }
            return view.equals(obj);
        }
    }

    private void initSearchButton() {
        this.mSearchButton = (ImageButton) findViewById(R.id.search_menu_nowplaying);
        final View viewFindViewById = findViewById(R.id.blank_between_search_and_overflow);
        final View viewFindViewById2 = findViewById(R.id.nowplaying);
        if (this.mSearchButton != null) {
            this.mSearchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MusicBrowserActivity.this.mOverflowMenuButton != null) {
                        MusicBrowserActivity.this.mOverflowMenuButton.setEnabled(false);
                    }
                    MusicBrowserActivity.this.mSearchButton.setVisibility(8);
                    MusicBrowserActivity.this.onSearchRequested();
                    if (viewFindViewById.getVisibility() == 0) {
                        viewFindViewById.setVisibility(8);
                    }
                    MusicBrowserActivity.this.mSearchViewShowing = true;
                }
            });
            ((SearchManager) getSystemService("search")).setOnDismissListener(new SearchManager.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (MusicBrowserActivity.this.mOverflowMenuButton != null) {
                        MusicBrowserActivity.this.mOverflowMenuButton.setEnabled(true);
                    }
                    MusicBrowserActivity.this.mSearchButton.setVisibility(0);
                    if (viewFindViewById2.getVisibility() != 0 && !MusicBrowserActivity.this.mHasMenukey) {
                        viewFindViewById.setVisibility(0);
                    }
                    MusicBrowserActivity.this.mSearchViewShowing = false;
                    InputMethodManager inputMethodManager = (InputMethodManager) MusicBrowserActivity.this.getApplicationContext().getSystemService("input_method");
                    if (inputMethodManager != null) {
                        MusicLogUtils.v("MusicBrowser", "IIME getService failed");
                    }
                    MusicLogUtils.v("MusicBrowser", "IME getService success");
                    if (inputMethodManager != null) {
                        MusicLogUtils.v("MusicBrowser", "Search Dialog hiding the IME");
                        inputMethodManager.hideSoftInputFromWindow(MusicBrowserActivity.this.mSearchButton.getWindowToken(), 0);
                    }
                    MusicLogUtils.v("MusicBrowser", "Search dialog on dismiss, enalbe search button");
                }
            });
        }
    }
}
