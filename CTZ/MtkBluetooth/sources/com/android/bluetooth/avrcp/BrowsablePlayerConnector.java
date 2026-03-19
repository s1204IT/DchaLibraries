package com.android.bluetooth.avrcp;

import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.bluetooth.avrcp.BrowsablePlayerConnector;
import com.android.bluetooth.avrcp.BrowsedPlayerWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrowsablePlayerConnector {
    private static final long CONNECT_TIMEOUT_MS = 10000;
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final int MSG_CONNECT_CB = 1;
    private static final int MSG_GET_FOLDER_ITEMS_CB = 0;
    private static final int MSG_TIMEOUT = 2;
    private static final String TAG = "NewAvrcpBrowsablePlayerConnector";
    private PlayerListCallback mCallback;
    private Context mContext;
    private Handler mHandler;
    private List<BrowsedPlayerWrapper> mResults = new ArrayList();
    private Set<BrowsedPlayerWrapper> mPendingPlayers = new HashSet();

    interface PlayerListCallback {
        void run(List<BrowsedPlayerWrapper> list);
    }

    static BrowsablePlayerConnector connectToPlayers(Context context, Looper looper, List<ResolveInfo> list, PlayerListCallback playerListCallback) {
        if (playerListCallback == null) {
            Log.wtfStack(TAG, "Null callback passed");
            return null;
        }
        final BrowsablePlayerConnector browsablePlayerConnector = new BrowsablePlayerConnector(context, looper, playerListCallback);
        for (final ResolveInfo resolveInfo : list) {
            BrowsedPlayerWrapper browsedPlayerWrapperWrap = BrowsedPlayerWrapper.wrap(context, ((PackageItemInfo) resolveInfo.serviceInfo).packageName, ((PackageItemInfo) resolveInfo.serviceInfo).name);
            browsablePlayerConnector.mPendingPlayers.add(browsedPlayerWrapperWrap);
            browsedPlayerWrapperWrap.connect(new BrowsedPlayerWrapper.ConnectionCallback() {
                @Override
                public final void run(int i, BrowsedPlayerWrapper browsedPlayerWrapper) {
                    BrowsablePlayerConnector.lambda$connectToPlayers$0(resolveInfo, browsablePlayerConnector, i, browsedPlayerWrapper);
                }
            });
        }
        browsablePlayerConnector.mHandler.sendMessageDelayed(browsablePlayerConnector.mHandler.obtainMessage(2), CONNECT_TIMEOUT_MS);
        return browsablePlayerConnector;
    }

    static void lambda$connectToPlayers$0(ResolveInfo resolveInfo, BrowsablePlayerConnector browsablePlayerConnector, int i, BrowsedPlayerWrapper browsedPlayerWrapper) {
        if (DEBUG) {
            Log.d(TAG, "Browse player callback called: package=" + ((PackageItemInfo) resolveInfo.serviceInfo).packageName + " : status=" + i);
        }
        Message messageObtainMessage = browsablePlayerConnector.mHandler.obtainMessage(1);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.obj = browsedPlayerWrapper;
        browsablePlayerConnector.mHandler.sendMessage(messageObtainMessage);
    }

    private BrowsablePlayerConnector(Context context, Looper looper, PlayerListCallback playerListCallback) {
        this.mContext = context;
        this.mCallback = playerListCallback;
        this.mHandler = new AnonymousClass1(looper);
    }

    class AnonymousClass1 extends Handler {
        AnonymousClass1(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (BrowsablePlayerConnector.DEBUG) {
                Log.d(BrowsablePlayerConnector.TAG, "Received a message: msg.what=" + message.what);
            }
            switch (message.what) {
                case 0:
                    BrowsedPlayerWrapper browsedPlayerWrapper = (BrowsedPlayerWrapper) message.obj;
                    if (!BrowsablePlayerConnector.this.mPendingPlayers.remove(browsedPlayerWrapper)) {
                        return;
                    }
                    Log.i(BrowsablePlayerConnector.TAG, "Successfully added package to results: " + browsedPlayerWrapper.getPackageName());
                    BrowsablePlayerConnector.this.mResults.add(browsedPlayerWrapper);
                    break;
                    break;
                case 1:
                    final BrowsedPlayerWrapper browsedPlayerWrapper2 = (BrowsedPlayerWrapper) message.obj;
                    if (message.arg1 == 0) {
                        if (BrowsablePlayerConnector.DEBUG) {
                            Log.i(BrowsablePlayerConnector.TAG, "Checking root contents for " + browsedPlayerWrapper2.getPackageName());
                        }
                        browsedPlayerWrapper2.getFolderItems(browsedPlayerWrapper2.getRootId(), new BrowsedPlayerWrapper.BrowseCallback() {
                            @Override
                            public final void run(int i, String str, List list) {
                                BrowsablePlayerConnector.AnonymousClass1.lambda$handleMessage$0(this.f$0, browsedPlayerWrapper2, i, str, list);
                            }
                        });
                    } else {
                        Log.i(BrowsablePlayerConnector.TAG, browsedPlayerWrapper2.getPackageName() + " is not browsable");
                        BrowsablePlayerConnector.this.mPendingPlayers.remove(browsedPlayerWrapper2);
                        return;
                    }
                    break;
                case 2:
                    Log.v(BrowsablePlayerConnector.TAG, "Timed out waiting for players");
                    for (BrowsedPlayerWrapper browsedPlayerWrapper3 : BrowsablePlayerConnector.this.mPendingPlayers) {
                        if (BrowsablePlayerConnector.DEBUG) {
                            Log.d(BrowsablePlayerConnector.TAG, "Disconnecting " + browsedPlayerWrapper3.getPackageName());
                        }
                        browsedPlayerWrapper3.disconnect();
                    }
                    BrowsablePlayerConnector.this.mPendingPlayers.clear();
                    break;
            }
            if (BrowsablePlayerConnector.this.mPendingPlayers.size() == 0) {
                Log.i(BrowsablePlayerConnector.TAG, "Successfully connected to " + BrowsablePlayerConnector.this.mResults.size() + " browsable players.");
                removeMessages(2);
                BrowsablePlayerConnector.this.mCallback.run(BrowsablePlayerConnector.this.mResults);
            }
        }

        public static void lambda$handleMessage$0(AnonymousClass1 anonymousClass1, BrowsedPlayerWrapper browsedPlayerWrapper, int i, String str, List list) {
            if (i != 0) {
                BrowsablePlayerConnector.this.mPendingPlayers.remove(browsedPlayerWrapper);
            } else {
                if (list.size() == 0) {
                    BrowsablePlayerConnector.this.mPendingPlayers.remove(browsedPlayerWrapper);
                    return;
                }
                Message messageObtainMessage = BrowsablePlayerConnector.this.mHandler.obtainMessage(0);
                messageObtainMessage.obj = browsedPlayerWrapper;
                BrowsablePlayerConnector.this.mHandler.sendMessage(messageObtainMessage);
            }
        }
    }
}
