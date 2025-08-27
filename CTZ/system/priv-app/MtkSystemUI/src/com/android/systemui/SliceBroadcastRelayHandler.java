package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Iterator;

/* loaded from: classes.dex */
public class SliceBroadcastRelayHandler extends SystemUI {
    private final ArrayMap<Uri, BroadcastRelay> mRelays = new ArrayMap<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.systemui.SliceBroadcastRelayHandler.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            SliceBroadcastRelayHandler.this.handleIntent(intent);
        }
    };

    @Override // com.android.systemui.SystemUI
    public void start() {
        IntentFilter intentFilter = new IntentFilter("com.android.settingslib.action.REGISTER_SLICE_RECEIVER");
        intentFilter.addAction("com.android.settingslib.action.UNREGISTER_SLICE_RECEIVER");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    @VisibleForTesting
    void handleIntent(Intent intent) {
        if (!"com.android.settingslib.action.REGISTER_SLICE_RECEIVER".equals(intent.getAction())) {
            if ("com.android.settingslib.action.UNREGISTER_SLICE_RECEIVER".equals(intent.getAction())) {
                getAndRemoveRelay((Uri) intent.getParcelableExtra("uri")).unregister(this.mContext);
            }
        } else {
            Uri uri = (Uri) intent.getParcelableExtra("uri");
            getOrCreateRelay(uri).register(this.mContext, (ComponentName) intent.getParcelableExtra("receiver"), (IntentFilter) intent.getParcelableExtra("filter"));
        }
    }

    private BroadcastRelay getOrCreateRelay(Uri uri) {
        BroadcastRelay broadcastRelay = this.mRelays.get(uri);
        if (broadcastRelay == null) {
            BroadcastRelay broadcastRelay2 = new BroadcastRelay(uri);
            this.mRelays.put(uri, broadcastRelay2);
            return broadcastRelay2;
        }
        return broadcastRelay;
    }

    private BroadcastRelay getAndRemoveRelay(Uri uri) {
        return this.mRelays.remove(uri);
    }

    private static class BroadcastRelay extends BroadcastReceiver {
        private final ArraySet<ComponentName> mReceivers = new ArraySet<>();
        private final Uri mUri;
        private final UserHandle mUserId;

        public BroadcastRelay(Uri uri) {
            this.mUserId = new UserHandle(ContentProvider.getUserIdFromUri(uri));
            this.mUri = uri;
        }

        public void register(Context context, ComponentName componentName, IntentFilter intentFilter) {
            this.mReceivers.add(componentName);
            context.registerReceiver(this, intentFilter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            intent.addFlags(268435456);
            Iterator<ComponentName> it = this.mReceivers.iterator();
            while (it.hasNext()) {
                intent.setComponent(it.next());
                intent.putExtra("uri", this.mUri.toString());
                context.sendBroadcastAsUser(intent, this.mUserId);
            }
        }
    }
}
