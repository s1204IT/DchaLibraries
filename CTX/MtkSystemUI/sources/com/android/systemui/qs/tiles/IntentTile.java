package com.android.systemui.qs.tiles;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.util.Arrays;
import java.util.Objects;

public class IntentTile extends QSTileImpl<QSTile.State> {
    private int mCurrentUserId;
    private String mIntentPackage;
    private Intent mLastIntent;
    private PendingIntent mOnClick;
    private String mOnClickUri;
    private PendingIntent mOnLongClick;
    private String mOnLongClickUri;
    private final BroadcastReceiver mReceiver;

    private IntentTile(QSHost qSHost, String str) {
        super(qSHost);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                IntentTile.this.refreshState(intent);
            }
        };
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(str));
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public static IntentTile create(QSHost qSHost, String str) {
        if (str == null || !str.startsWith("intent(") || !str.endsWith(")")) {
            throw new IllegalArgumentException("Bad intent tile spec: " + str);
        }
        String strSubstring = str.substring("intent(".length(), str.length() - 1);
        if (strSubstring.isEmpty()) {
            throw new IllegalArgumentException("Empty intent tile spec action");
        }
        return new IntentTile(qSHost, strSubstring);
    }

    @Override
    public void handleSetListening(boolean z) {
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    protected void handleUserSwitch(int i) {
        super.handleUserSwitch(i);
        this.mCurrentUserId = i;
    }

    @Override
    protected void handleClick() {
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

    private void sendIntent(String str, PendingIntent pendingIntent, String str2) {
        try {
            if (pendingIntent != null) {
                if (pendingIntent.isActivity()) {
                    ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(pendingIntent);
                } else {
                    pendingIntent.send();
                }
            } else if (str2 != null) {
                this.mContext.sendBroadcastAsUser(Intent.parseUri(str2, 1), new UserHandle(this.mCurrentUserId));
            }
        } catch (Throwable th) {
            Log.w(this.TAG, "Error sending " + str + " intent", th);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object obj) {
        Intent intent = (Intent) obj;
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
        byte[] byteArrayExtra = intent.getByteArrayExtra("iconBitmap");
        if (byteArrayExtra != null) {
            try {
                state.icon = new BytesIcon(byteArrayExtra);
            } catch (Throwable th) {
                Log.w(this.TAG, "Error loading icon bitmap, length " + byteArrayExtra.length, th);
            }
        } else {
            int intExtra = intent.getIntExtra("iconId", 0);
            if (intExtra != 0) {
                String stringExtra = intent.getStringExtra("iconPackage");
                if (!TextUtils.isEmpty(stringExtra)) {
                    state.icon = new PackageDrawableIcon(stringExtra, intExtra);
                } else {
                    state.icon = QSTileImpl.ResourceIcon.get(intExtra);
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

        public BytesIcon(byte[] bArr) {
            this.mBytes = bArr;
        }

        @Override
        public Drawable getDrawable(Context context) {
            return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(this.mBytes, 0, this.mBytes.length));
        }

        public boolean equals(Object obj) {
            return (obj instanceof BytesIcon) && Arrays.equals(((BytesIcon) obj).mBytes, this.mBytes);
        }

        public String toString() {
            return String.format("BytesIcon[len=%s]", Integer.valueOf(this.mBytes.length));
        }
    }

    private class PackageDrawableIcon extends QSTile.Icon {
        private final String mPackage;
        private final int mResId;

        public PackageDrawableIcon(String str, int i) {
            this.mPackage = str;
            this.mResId = i;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PackageDrawableIcon)) {
                return false;
            }
            PackageDrawableIcon packageDrawableIcon = (PackageDrawableIcon) obj;
            return Objects.equals(packageDrawableIcon.mPackage, this.mPackage) && packageDrawableIcon.mResId == this.mResId;
        }

        @Override
        public Drawable getDrawable(Context context) {
            try {
                return context.createPackageContext(this.mPackage, 0).getDrawable(this.mResId);
            } catch (Throwable th) {
                Log.w(IntentTile.this.TAG, "Error loading package drawable pkg=" + this.mPackage + " id=" + this.mResId, th);
                return null;
            }
        }

        public String toString() {
            return String.format("PackageDrawableIcon[pkg=%s,id=0x%08x]", this.mPackage, Integer.valueOf(this.mResId));
        }
    }
}
