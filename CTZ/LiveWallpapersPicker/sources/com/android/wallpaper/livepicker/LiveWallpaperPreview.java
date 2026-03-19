package com.android.wallpaper.livepicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.support.design.widget.BottomSheetBehavior;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;
import com.android.wallpaper.livepicker.LiveWallpaperPreview;
import java.io.IOException;

public class LiveWallpaperPreview extends Activity {
    private Button mAttributionExploreButton;
    private TextView mAttributionSubtitle1;
    private TextView mAttributionSubtitle2;
    private TextView mAttributionTitle;
    private View mBottomSheet;
    private View mLoading;
    private String mPackageName;
    private ImageButton mPreviewPaneArrow;
    private Intent mSettingsIntent;
    private View mSpacer;
    private WallpaperConnection mWallpaperConnection;
    private Intent mWallpaperIntent;
    private WallpaperManager mWallpaperManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        init();
    }

    protected void init() {
        WallpaperInfo wallpaperInfo = (WallpaperInfo) getIntent().getExtras().getParcelable("android.live_wallpaper.info");
        if (wallpaperInfo == null) {
            setResult(0);
            finish();
        }
        initUI(wallpaperInfo);
    }

    protected void initUI(WallpaperInfo wallpaperInfo) {
        getWindow().getDecorView().setSystemUiVisibility(1792);
        setContentView(R.layout.live_wallpaper_preview);
        this.mAttributionTitle = (TextView) findViewById(R.id.preview_attribution_pane_title);
        this.mAttributionSubtitle1 = (TextView) findViewById(R.id.preview_attribution_pane_subtitle1);
        this.mAttributionSubtitle2 = (TextView) findViewById(R.id.preview_attribution_pane_subtitle2);
        this.mAttributionExploreButton = (Button) findViewById(R.id.preview_attribution_pane_explore_button);
        this.mPreviewPaneArrow = (ImageButton) findViewById(R.id.preview_attribution_pane_arrow);
        this.mBottomSheet = findViewById(R.id.bottom_sheet);
        this.mSpacer = findViewById(R.id.spacer);
        this.mLoading = findViewById(R.id.loading);
        this.mPackageName = wallpaperInfo.getPackageName();
        this.mWallpaperIntent = new Intent("android.service.wallpaper.WallpaperService").setClassName(wallpaperInfo.getPackageName(), wallpaperInfo.getServiceName());
        String settingsActivity = wallpaperInfo.getSettingsActivity();
        if (settingsActivity != null) {
            this.mSettingsIntent = new Intent();
            this.mSettingsIntent.setComponent(new ComponentName(this.mPackageName, settingsActivity));
            this.mSettingsIntent.putExtra("android.service.wallpaper.PREVIEW_MODE", true);
            if (this.mSettingsIntent.resolveActivityInfo(getPackageManager(), 0) == null) {
                Log.e("LiveWallpaperPreview", "Couldn't find settings activity: " + settingsActivity);
                this.mSettingsIntent = null;
            }
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowTitleEnabled(false);
        Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        drawable.setAutoMirrored(true);
        toolbar.setNavigationIcon(drawable);
        this.mWallpaperManager = WallpaperManager.getInstance(this);
        this.mWallpaperConnection = new WallpaperConnection(this.mWallpaperIntent);
        populateAttributionPane(wallpaperInfo);
    }

    private void populateAttributionPane(WallpaperInfo wallpaperInfo) {
        CharSequence charSequenceLoadDescription;
        CharSequence charSequenceLoadAuthor;
        if (!wallpaperInfo.getShowMetadataInPreview()) {
            this.mBottomSheet.setVisibility(8);
            return;
        }
        final BottomSheetBehavior bottomSheetBehaviorFrom = BottomSheetBehavior.from(this.mBottomSheet);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehaviorFrom.getState() == 4) {
                    bottomSheetBehaviorFrom.setState(3);
                } else if (bottomSheetBehaviorFrom.getState() == 3) {
                    bottomSheetBehaviorFrom.setState(4);
                }
            }
        };
        this.mAttributionTitle.setOnClickListener(onClickListener);
        this.mPreviewPaneArrow.setOnClickListener(onClickListener);
        bottomSheetBehaviorFrom.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View view, int i) {
                if (i == 4) {
                    LiveWallpaperPreview.this.mPreviewPaneArrow.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
                    LiveWallpaperPreview.this.mPreviewPaneArrow.setContentDescription(LiveWallpaperPreview.this.getResources().getString(R.string.expand_attribution_panel));
                } else if (i == 3) {
                    LiveWallpaperPreview.this.mPreviewPaneArrow.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
                    LiveWallpaperPreview.this.mPreviewPaneArrow.setContentDescription(LiveWallpaperPreview.this.getResources().getString(R.string.collapse_attribution_panel));
                }
            }

            @Override
            public void onSlide(View view, float f) {
                if (f >= 0.0f) {
                }
                LiveWallpaperPreview.this.mAttributionTitle.setAlpha(f);
                LiveWallpaperPreview.this.mAttributionSubtitle1.setAlpha(f);
                LiveWallpaperPreview.this.mAttributionSubtitle2.setAlpha(f);
                LiveWallpaperPreview.this.mAttributionExploreButton.setAlpha(f);
            }
        });
        bottomSheetBehaviorFrom.setState(3);
        this.mPreviewPaneArrow.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
        PackageManager packageManager = getPackageManager();
        CharSequence charSequenceLoadLabel = wallpaperInfo.loadLabel(packageManager);
        if (!TextUtils.isEmpty(charSequenceLoadLabel)) {
            this.mAttributionTitle.setText(charSequenceLoadLabel);
        } else {
            this.mAttributionTitle.setVisibility(8);
        }
        try {
            charSequenceLoadAuthor = wallpaperInfo.loadAuthor(packageManager);
        } catch (Resources.NotFoundException e) {
            this.mAttributionSubtitle1.setVisibility(8);
        }
        if (TextUtils.isEmpty(charSequenceLoadAuthor)) {
            throw new Resources.NotFoundException();
        }
        this.mAttributionSubtitle1.setText(charSequenceLoadAuthor);
        try {
            charSequenceLoadDescription = wallpaperInfo.loadDescription(packageManager);
        } catch (Resources.NotFoundException e2) {
            this.mAttributionSubtitle2.setVisibility(8);
        }
        if (TextUtils.isEmpty(charSequenceLoadDescription)) {
            throw new Resources.NotFoundException();
        }
        this.mAttributionSubtitle2.setText(charSequenceLoadDescription);
        try {
            final Uri uriLoadContextUri = wallpaperInfo.loadContextUri(packageManager);
            CharSequence charSequenceLoadContextDescription = wallpaperInfo.loadContextDescription(packageManager);
            if (uriLoadContextUri == null) {
                throw new Resources.NotFoundException();
            }
            this.mAttributionExploreButton.setText(charSequenceLoadContextDescription);
            this.mAttributionExploreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    LiveWallpaperPreview.lambda$populateAttributionPane$0(this.f$0, uriLoadContextUri, view);
                }
            });
        } catch (Resources.NotFoundException e3) {
            this.mAttributionExploreButton.setVisibility(8);
            this.mSpacer.setVisibility(0);
        }
    }

    public static void lambda$populateAttributionPane$0(LiveWallpaperPreview liveWallpaperPreview, Uri uri, View view) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW", uri);
        intent.setFlags(268435456);
        try {
            liveWallpaperPreview.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("LiveWallpaperPreview", "Couldn't find activity for context link.", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_preview, menu);
        menu.findItem(R.id.configure).setVisible(this.mSettingsIntent != null);
        menu.findItem(R.id.set_wallpaper).getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.setLiveWallpaper(view);
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    public void setLiveWallpaper(final View view) {
        if (this.mWallpaperManager.getWallpaperInfo() != null && this.mWallpaperManager.getWallpaperId(2) < 0) {
            try {
                setLiveWallpaper(view.getRootView().getWindowToken());
                setResult(-1);
            } catch (RuntimeException e) {
                Log.w("LiveWallpaperPreview", "Failure setting wallpaper", e);
            }
            finish();
            return;
        }
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, android.R.style.Theme.DeviceDefault.Settings);
        new AlertDialog.Builder(contextThemeWrapper).setTitle(R.string.set_live_wallpaper).setAdapter(new WallpaperTargetAdapter(contextThemeWrapper), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    LiveWallpaperPreview.this.setLiveWallpaper(view.getRootView().getWindowToken());
                    if (i == 1) {
                        LiveWallpaperPreview.this.mWallpaperManager.clear(2);
                    }
                    LiveWallpaperPreview.this.setResult(-1);
                } catch (IOException | RuntimeException e2) {
                    Log.w("LiveWallpaperPreview", "Failure setting wallpaper", e2);
                }
                LiveWallpaperPreview.this.finish();
            }
        }).show();
    }

    private void setLiveWallpaper(IBinder iBinder) {
        this.mWallpaperManager.setWallpaperComponent(this.mWallpaperIntent.getComponent());
        this.mWallpaperManager.setWallpaperOffsetSteps(0.5f, 0.0f);
        this.mWallpaperManager.setWallpaperOffsets(iBinder, 0.5f, 0.0f);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.configure) {
            startActivity(this.mSettingsIntent);
            return true;
        }
        if (itemId == R.id.set_wallpaper) {
            setLiveWallpaper(getWindow().getDecorView());
            return true;
        }
        if (itemId == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mWallpaperConnection != null && this.mWallpaperConnection.mEngine != null) {
            try {
                this.mWallpaperConnection.mEngine.setVisibility(true);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mWallpaperConnection != null && this.mWallpaperConnection.mEngine != null) {
            try {
                this.mWallpaperConnection.mEngine.setVisibility(false);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                if (!LiveWallpaperPreview.this.mWallpaperConnection.connect()) {
                    LiveWallpaperPreview.this.mWallpaperConnection = null;
                }
            }
        });
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mWallpaperConnection != null) {
            this.mWallpaperConnection.disconnect();
        }
        this.mWallpaperConnection = null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.mWallpaperConnection != null && this.mWallpaperConnection.mEngine != null) {
            try {
                this.mWallpaperConnection.mEngine.dispatchPointer(MotionEvent.obtainNoHistory(motionEvent));
            } catch (RemoteException e) {
            }
        }
        if (motionEvent.getAction() == 0) {
            onUserInteraction();
        }
        boolean zSuperDispatchTouchEvent = getWindow().superDispatchTouchEvent(motionEvent);
        if (!zSuperDispatchTouchEvent) {
            zSuperDispatchTouchEvent = onTouchEvent(motionEvent);
        }
        if (!zSuperDispatchTouchEvent && this.mWallpaperConnection != null && this.mWallpaperConnection.mEngine != null) {
            int actionMasked = motionEvent.getActionMasked();
            try {
                if (actionMasked == 1) {
                    this.mWallpaperConnection.mEngine.dispatchWallpaperCommand("android.wallpaper.tap", (int) motionEvent.getX(), (int) motionEvent.getY(), 0, (Bundle) null);
                } else if (actionMasked == 6) {
                    int actionIndex = motionEvent.getActionIndex();
                    this.mWallpaperConnection.mEngine.dispatchWallpaperCommand("android.wallpaper.secondaryTap", (int) motionEvent.getX(actionIndex), (int) motionEvent.getY(actionIndex), 0, (Bundle) null);
                }
            } catch (RemoteException e2) {
            }
        }
        return zSuperDispatchTouchEvent;
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        boolean mConnected;
        IWallpaperEngine mEngine;
        final Intent mIntent;
        IWallpaperService mService;

        WallpaperConnection(Intent intent) {
            this.mIntent = intent;
        }

        public boolean connect() {
            synchronized (this) {
                if (!LiveWallpaperPreview.this.bindService(this.mIntent, this, 1)) {
                    return false;
                }
                this.mConnected = true;
                return true;
            }
        }

        public void disconnect() {
            synchronized (this) {
                this.mConnected = false;
                if (this.mEngine != null) {
                    try {
                        this.mEngine.destroy();
                    } catch (RemoteException e) {
                    }
                    this.mEngine = null;
                }
                try {
                    LiveWallpaperPreview.this.unbindService(this);
                } catch (IllegalArgumentException e2) {
                    Log.w("LiveWallpaperPreview", "Can't unbind wallpaper service. It might have crashed, just ignoring.", e2);
                }
                this.mService = null;
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (LiveWallpaperPreview.this.mWallpaperConnection == this) {
                this.mService = IWallpaperService.Stub.asInterface(iBinder);
                try {
                    View decorView = LiveWallpaperPreview.this.getWindow().getDecorView();
                    this.mService.attach(this, decorView.getWindowToken(), 1001, true, decorView.getWidth(), decorView.getHeight(), new Rect(0, 0, 0, 0));
                } catch (RemoteException e) {
                    Log.w("LiveWallpaperPreview", "Failed attaching wallpaper; clearing", e);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            this.mService = null;
            this.mEngine = null;
            if (LiveWallpaperPreview.this.mWallpaperConnection == this) {
                Log.w("LiveWallpaperPreview", "Wallpaper service gone: " + componentName);
            }
        }

        public void attachEngine(IWallpaperEngine iWallpaperEngine) {
            synchronized (this) {
                if (this.mConnected) {
                    this.mEngine = iWallpaperEngine;
                    try {
                        iWallpaperEngine.setVisibility(true);
                    } catch (RemoteException e) {
                    }
                } else {
                    try {
                        iWallpaperEngine.destroy();
                    } catch (RemoteException e2) {
                    }
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String str) {
            return null;
        }

        public void onWallpaperColorsChanged(WallpaperColors wallpaperColors) throws RemoteException {
        }

        public void engineShown(IWallpaperEngine iWallpaperEngine) throws RemoteException {
            LiveWallpaperPreview.this.mLoading.post(new Runnable() {
                @Override
                public final void run() {
                    LiveWallpaperPreview.WallpaperConnection wallpaperConnection = this.f$0;
                    LiveWallpaperPreview.this.mLoading.animate().alpha(0.0f).setDuration(220L).setInterpolator(AnimationUtils.loadInterpolator(LiveWallpaperPreview.this, android.R.interpolator.fast_out_linear_in)).withEndAction(new Runnable() {
                        @Override
                        public final void run() {
                            LiveWallpaperPreview.this.mLoading.setVisibility(4);
                        }
                    });
                }
            });
        }
    }

    private static class WallpaperTargetAdapter extends ArrayAdapter<CharSequence> {
        public WallpaperTargetAdapter(Context context) {
            super(context, R.layout.wallpaper_target_dialog_item, context.getResources().getTextArray(R.array.which_wallpaper_options));
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView textView = (TextView) super.getView(i, view, viewGroup);
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(i == 0 ? R.drawable.ic_home : R.drawable.ic_device, 0, 0, 0);
            return textView;
        }
    }
}
