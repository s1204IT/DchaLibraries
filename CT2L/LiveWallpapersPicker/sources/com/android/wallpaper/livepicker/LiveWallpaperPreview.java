package com.android.wallpaper.livepicker;

import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class LiveWallpaperPreview extends Activity {
    private Dialog mDialog;
    private String mPackageName;
    private String mSettings;
    private View mView;
    private WallpaperConnection mWallpaperConnection;
    private Intent mWallpaperIntent;
    private WallpaperManager mWallpaperManager;

    static void showPreview(Activity activity, int code, Intent intent, WallpaperInfo info) {
        if (info == null) {
            Log.w("LiveWallpaperPreview", "Failure showing preview", new Throwable());
            return;
        }
        Intent preview = new Intent(activity, (Class<?>) LiveWallpaperPreview.class);
        preview.putExtra("android.live_wallpaper.intent", intent);
        preview.putExtra("android.live_wallpaper.settings", info.getSettingsActivity());
        preview.putExtra("android.live_wallpaper.package", info.getPackageName());
        activity.startActivityForResult(preview, code);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        this.mWallpaperIntent = (Intent) extras.get("android.live_wallpaper.intent");
        if (this.mWallpaperIntent == null) {
            setResult(0);
            finish();
        }
        setContentView(R.layout.live_wallpaper_preview);
        this.mView = findViewById(R.id.configure);
        this.mSettings = extras.getString("android.live_wallpaper.settings");
        this.mPackageName = extras.getString("android.live_wallpaper.package");
        if (this.mSettings == null) {
            this.mView.setVisibility(8);
        }
        this.mWallpaperManager = WallpaperManager.getInstance(this);
        this.mWallpaperConnection = new WallpaperConnection(this.mWallpaperIntent);
    }

    public void setLiveWallpaper(View v) {
        try {
            this.mWallpaperManager.getIWallpaperManager().setWallpaperComponent(this.mWallpaperIntent.getComponent());
            this.mWallpaperManager.setWallpaperOffsetSteps(0.5f, 0.0f);
            this.mWallpaperManager.setWallpaperOffsets(v.getRootView().getWindowToken(), 0.5f, 0.0f);
            setResult(-1);
        } catch (RemoteException e) {
        } catch (RuntimeException e2) {
            Log.w("LiveWallpaperPreview", "Failure setting wallpaper", e2);
        }
        finish();
    }

    public void configureLiveWallpaper(View v) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this.mPackageName, this.mSettings));
        intent.putExtra("android.service.wallpaper.PREVIEW_MODE", true);
        startActivity(intent);
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
        showLoading();
        this.mView.post(new Runnable() {
            @Override
            public void run() {
                if (!LiveWallpaperPreview.this.mWallpaperConnection.connect()) {
                    LiveWallpaperPreview.this.mWallpaperConnection = null;
                }
            }
        });
    }

    private void showLoading() {
        LayoutInflater inflater = LayoutInflater.from(this);
        TextView content = (TextView) inflater.inflate(R.layout.live_wallpaper_loading, (ViewGroup) null);
        this.mDialog = new Dialog(this, android.R.style.Theme.Black);
        Window window = this.mDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = -1;
        lp.height = -1;
        window.setType(1001);
        this.mDialog.setContentView(content, new ViewGroup.LayoutParams(-1, -1));
        this.mDialog.show();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        if (this.mWallpaperConnection != null) {
            try {
                this.mWallpaperConnection.disconnect();
                this.mWallpaperConnection = null;
            } catch (IllegalArgumentException e) {
                Log.e("LiveWallpaperPreview", "called before connect, failed");
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mWallpaperConnection != null && this.mWallpaperConnection.mEngine != null) {
            MotionEvent dup = MotionEvent.obtainNoHistory(ev);
            try {
                this.mWallpaperConnection.mEngine.dispatchPointer(dup);
            } catch (RemoteException e) {
            }
        }
        if (ev.getAction() == 0) {
            onUserInteraction();
        }
        boolean handled = getWindow().superDispatchTouchEvent(ev);
        if (!handled) {
            handled = onTouchEvent(ev);
        }
        if (!handled && this.mWallpaperConnection != null && this.mWallpaperConnection.mEngine != null) {
            int action = ev.getActionMasked();
            try {
                if (action == 1) {
                    this.mWallpaperConnection.mEngine.dispatchWallpaperCommand("android.wallpaper.tap", (int) ev.getX(), (int) ev.getY(), 0, (Bundle) null);
                } else if (action == 6) {
                    int pointerIndex = ev.getActionIndex();
                    this.mWallpaperConnection.mEngine.dispatchWallpaperCommand("android.wallpaper.secondaryTap", (int) ev.getX(pointerIndex), (int) ev.getY(pointerIndex), 0, (Bundle) null);
                }
            } catch (RemoteException e2) {
            }
        }
        return handled;
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
            boolean z = true;
            synchronized (this) {
                if (!LiveWallpaperPreview.this.bindService(this.mIntent, this, 1)) {
                    z = false;
                } else {
                    this.mConnected = true;
                }
            }
            return z;
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
                LiveWallpaperPreview.this.unbindService(this);
                this.mService = null;
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (LiveWallpaperPreview.this.mWallpaperConnection == this) {
                this.mService = IWallpaperService.Stub.asInterface(service);
                try {
                    View view = LiveWallpaperPreview.this.mView;
                    View root = view.getRootView();
                    this.mService.attach(this, view.getWindowToken(), 1004, true, root.getWidth(), root.getHeight(), new Rect(0, 0, 0, 0));
                } catch (RemoteException e) {
                    Log.w("LiveWallpaperPreview", "Failed attaching wallpaper; clearing", e);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.mService = null;
            this.mEngine = null;
            if (LiveWallpaperPreview.this.mWallpaperConnection == this) {
                Log.w("LiveWallpaperPreview", "Wallpaper service gone: " + name);
            }
        }

        public void attachEngine(IWallpaperEngine engine) {
            synchronized (this) {
                if (this.mConnected) {
                    this.mEngine = engine;
                    try {
                        engine.setVisibility(true);
                    } catch (RemoteException e) {
                    }
                } else {
                    try {
                        engine.destroy();
                    } catch (RemoteException e2) {
                    }
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String name) {
            return null;
        }

        public void engineShown(IWallpaperEngine engine) throws RemoteException {
        }
    }
}
