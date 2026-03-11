package com.android.systemui.qs.tiles;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.qs.QSTile;
import java.util.Arrays;
import java.util.Objects;

public class IntentTile extends QSTile<QSTile.State> {
    private int mCurrentUserId;
    private String mIntentPackage;
    private Intent mLastIntent;
    private PendingIntent mOnClick;
    private String mOnClickUri;
    private PendingIntent mOnLongClick;
    private String mOnLongClickUri;
    private final BroadcastReceiver mReceiver;

    private IntentTile(QSTile.Host host, String action) {
        super(host);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                IntentTile.this.refreshState(intent);
            }
        };
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(action));
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public static QSTile<?> create(QSTile.Host host, String spec) {
        if (spec == null || !spec.startsWith("intent(") || !spec.endsWith(")")) {
            throw new IllegalArgumentException("Bad intent tile spec: " + spec);
        }
        String action = spec.substring("intent(".length(), spec.length() - 1);
        if (action.isEmpty()) {
            throw new IllegalArgumentException("Empty intent tile spec action");
        }
        return new IntentTile(host, action);
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        super.handleUserSwitch(newUserId);
        this.mCurrentUserId = newUserId;
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(this.mContext, getMetricsCategory(), this.mIntentPackage);
        sendIntent("click", this.mOnClick, this.mOnClickUri);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleLongClick() {
        sendIntent("long-click", this.mOnLongClick, this.mOnLongClickUri);
    }

    private void sendIntent(String type, PendingIntent pi, String uri) {
        try {
            if (pi != null) {
                if (pi.isActivity()) {
                    getHost().startActivityDismissingKeyguard(pi);
                } else {
                    pi.send();
                }
            } else {
                if (uri == null) {
                    return;
                }
                Intent intent = Intent.parseUri(uri, 1);
                this.mContext.sendBroadcastAsUser(intent, new UserHandle(this.mCurrentUserId));
            }
        } catch (Throwable t) {
            Log.w(this.TAG, "Error sending " + type + " intent", t);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object arg) {
        Intent intent = (Intent) arg;
        if (intent == null) {
            if (this.mLastIntent == null) {
                return;
            } else {
                intent = this.mLastIntent;
            }
        }
        this.mLastIntent = intent;
        state.contentDescription = intent.getStringExtra("contentDescription");
        state.label = intent.getStringExtra("label");
        state.icon = null;
        byte[] iconBitmap = intent.getByteArrayExtra("iconBitmap");
        if (iconBitmap != null) {
            try {
                state.icon = new BytesIcon(iconBitmap);
            } catch (Throwable t) {
                Log.w(this.TAG, "Error loading icon bitmap, length " + iconBitmap.length, t);
            }
        } else {
            int iconId = intent.getIntExtra("iconId", 0);
            if (iconId != 0) {
                String iconPackage = intent.getStringExtra("iconPackage");
                if (!TextUtils.isEmpty(iconPackage)) {
                    state.icon = new PackageDrawableIcon(iconPackage, iconId);
                } else {
                    state.icon = QSTile.ResourceIcon.get(iconId);
                }
            }
        }
        this.mOnClick = (PendingIntent) intent.getParcelableExtra("onClick");
        this.mOnClickUri = intent.getStringExtra("onClickUri");
        this.mOnLongClick = (PendingIntent) intent.getParcelableExtra("onLongClick");
        this.mOnLongClickUri = intent.getStringExtra("onLongClickUri");
        this.mIntentPackage = intent.getStringExtra("package");
        this.mIntentPackage = this.mIntentPackage == null ? "" : this.mIntentPackage;
    }

    @Override
    public int getMetricsCategory() {
        return 121;
    }

    private static class BytesIcon extends QSTile.Icon {
        private final byte[] mBytes;

        public BytesIcon(byte[] bytes) {
            this.mBytes = bytes;
        }

        @Override
        public Drawable getDrawable(Context context) {
            Bitmap b = BitmapFactory.decodeByteArray(this.mBytes, 0, this.mBytes.length);
            return new BitmapDrawable(context.getResources(), b);
        }

        public boolean equals(Object o) {
            if (o instanceof BytesIcon) {
                return Arrays.equals(((BytesIcon) o).mBytes, this.mBytes);
            }
            return false;
        }

        public String toString() {
            return String.format("BytesIcon[len=%s]", Integer.valueOf(this.mBytes.length));
        }
    }

    private class PackageDrawableIcon extends QSTile.Icon {
        private final String mPackage;
        private final int mResId;

        public PackageDrawableIcon(String pkg, int resId) {
            this.mPackage = pkg;
            this.mResId = resId;
        }

        public boolean equals(Object o) {
            if (!(o instanceof PackageDrawableIcon)) {
                return false;
            }
            PackageDrawableIcon other = (PackageDrawableIcon) o;
            return Objects.equals(other.mPackage, this.mPackage) && other.mResId == this.mResId;
        }

        @Override
        public Drawable getDrawable(Context context) {
            try {
                return context.createPackageContext(this.mPackage, 0).getDrawable(this.mResId);
            } catch (Throwable t) {
                Log.w(IntentTile.this.TAG, "Error loading package drawable pkg=" + this.mPackage + " id=" + this.mResId, t);
                return null;
            }
        }

        public String toString() {
            return String.format("PackageDrawableIcon[pkg=%s,id=0x%08x]", this.mPackage, Integer.valueOf(this.mResId));
        }
    }
}
