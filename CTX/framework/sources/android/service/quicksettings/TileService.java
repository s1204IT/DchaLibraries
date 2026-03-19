package android.service.quicksettings;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.service.quicksettings.IQSService;
import android.service.quicksettings.IQSTileService;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.R;

public class TileService extends Service {
    public static final String ACTION_QS_TILE = "android.service.quicksettings.action.QS_TILE";
    public static final String ACTION_QS_TILE_PREFERENCES = "android.service.quicksettings.action.QS_TILE_PREFERENCES";
    public static final String ACTION_REQUEST_LISTENING = "android.service.quicksettings.action.REQUEST_LISTENING";
    public static final String EXTRA_SERVICE = "service";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_TOKEN = "token";
    public static final String META_DATA_ACTIVE_TILE = "android.service.quicksettings.ACTIVE_TILE";
    private final H mHandler = new H(Looper.getMainLooper());
    private boolean mListening = false;
    private IQSService mService;
    private Tile mTile;
    private IBinder mTileToken;
    private IBinder mToken;
    private Runnable mUnlockRunnable;

    @Override
    public void onDestroy() {
        if (this.mListening) {
            onStopListening();
            this.mListening = false;
        }
        super.onDestroy();
    }

    public void onTileAdded() {
    }

    public void onTileRemoved() {
    }

    public void onStartListening() {
    }

    public void onStopListening() {
    }

    public void onClick() {
    }

    @SystemApi
    public final void setStatusIcon(Icon icon, String str) {
        if (this.mService != null) {
            try {
                this.mService.updateStatusIcon(this.mTileToken, icon, str);
            } catch (RemoteException e) {
            }
        }
    }

    public final void showDialog(Dialog dialog) {
        dialog.getWindow().getAttributes().token = this.mToken;
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_QS_DIALOG);
        dialog.getWindow().getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                try {
                    TileService.this.mService.onDialogHidden(TileService.this.mTileToken);
                } catch (RemoteException e) {
                }
            }
        });
        dialog.show();
        try {
            this.mService.onShowDialog(this.mTileToken);
        } catch (RemoteException e) {
        }
    }

    public final void unlockAndRun(Runnable runnable) {
        this.mUnlockRunnable = runnable;
        try {
            this.mService.startUnlockAndRun(this.mTileToken);
        } catch (RemoteException e) {
        }
    }

    public final boolean isSecure() {
        try {
            return this.mService.isSecure();
        } catch (RemoteException e) {
            return true;
        }
    }

    public final boolean isLocked() {
        try {
            return this.mService.isLocked();
        } catch (RemoteException e) {
            return true;
        }
    }

    public final void startActivityAndCollapse(Intent intent) {
        startActivity(intent);
        try {
            this.mService.onStartActivity(this.mTileToken);
        } catch (RemoteException e) {
        }
    }

    public final Tile getQsTile() {
        return this.mTile;
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.mService = IQSService.Stub.asInterface(intent.getIBinderExtra("service"));
        this.mTileToken = intent.getIBinderExtra(EXTRA_TOKEN);
        try {
            this.mTile = this.mService.getTile(this.mTileToken);
            if (this.mTile != null) {
                this.mTile.setService(this.mService, this.mTileToken);
                this.mHandler.sendEmptyMessage(7);
            }
            return new IQSTileService.Stub() {
                @Override
                public void onTileRemoved() throws RemoteException {
                    TileService.this.mHandler.sendEmptyMessage(4);
                }

                @Override
                public void onTileAdded() throws RemoteException {
                    TileService.this.mHandler.sendEmptyMessage(3);
                }

                @Override
                public void onStopListening() throws RemoteException {
                    TileService.this.mHandler.sendEmptyMessage(2);
                }

                @Override
                public void onStartListening() throws RemoteException {
                    TileService.this.mHandler.sendEmptyMessage(1);
                }

                @Override
                public void onClick(IBinder iBinder) throws RemoteException {
                    TileService.this.mHandler.obtainMessage(5, iBinder).sendToTarget();
                }

                @Override
                public void onUnlockComplete() throws RemoteException {
                    TileService.this.mHandler.sendEmptyMessage(6);
                }
            };
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to reach IQSService", e);
        }
    }

    private class H extends Handler {
        private static final int MSG_START_LISTENING = 1;
        private static final int MSG_START_SUCCESS = 7;
        private static final int MSG_STOP_LISTENING = 2;
        private static final int MSG_TILE_ADDED = 3;
        private static final int MSG_TILE_CLICKED = 5;
        private static final int MSG_TILE_REMOVED = 4;
        private static final int MSG_UNLOCK_COMPLETE = 6;

        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (!TileService.this.mListening) {
                        TileService.this.mListening = true;
                        TileService.this.onStartListening();
                    }
                    break;
                case 2:
                    if (TileService.this.mListening) {
                        TileService.this.mListening = false;
                        TileService.this.onStopListening();
                    }
                    break;
                case 3:
                    TileService.this.onTileAdded();
                    break;
                case 4:
                    if (TileService.this.mListening) {
                        TileService.this.mListening = false;
                        TileService.this.onStopListening();
                    }
                    TileService.this.onTileRemoved();
                    break;
                case 5:
                    TileService.this.mToken = (IBinder) message.obj;
                    TileService.this.onClick();
                    break;
                case 6:
                    if (TileService.this.mUnlockRunnable != null) {
                        TileService.this.mUnlockRunnable.run();
                    }
                    break;
                case 7:
                    try {
                        TileService.this.mService.onStartSuccessful(TileService.this.mTileToken);
                    } catch (RemoteException e) {
                        return;
                    }
                    break;
            }
        }
    }

    public static boolean isQuickSettingsSupported() {
        return Resources.getSystem().getBoolean(R.bool.config_quickSettingsSupported);
    }

    public static final void requestListeningState(Context context, ComponentName componentName) {
        Intent intent = new Intent(ACTION_REQUEST_LISTENING);
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, componentName);
        intent.setPackage("com.android.systemui");
        context.sendBroadcast(intent, Manifest.permission.BIND_QUICK_SETTINGS_TILE);
    }
}
