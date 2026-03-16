package com.android.phone;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.INetworkQueryService;
import java.util.ArrayList;

public class NetworkQueryService extends Service {
    private Phone mPhone;
    private int mState;
    private final IBinder mLocalBinder = new LocalBinder();
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    NetworkQueryService.log("scan completed, broadcasting results");
                    NetworkQueryService.this.broadcastQueryResults((AsyncResult) msg.obj);
                    break;
            }
        }
    };
    final RemoteCallbackList<INetworkQueryServiceCallback> mCallbacks = new RemoteCallbackList<>();
    private final INetworkQueryService.Stub mBinder = new INetworkQueryService.Stub() {
        @Override
        public void startNetworkQuery(INetworkQueryServiceCallback cb) {
            if (cb != null) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    NetworkQueryService.this.mCallbacks.register(cb);
                    NetworkQueryService.log("registering callback " + cb.getClass().toString());
                    switch (NetworkQueryService.this.mState) {
                        case -2:
                            NetworkQueryService.log("query already in progress");
                            break;
                        case -1:
                            NetworkQueryService.this.mPhone.getAvailableNetworks(NetworkQueryService.this.mHandler.obtainMessage(100));
                            NetworkQueryService.this.mState = -2;
                            NetworkQueryService.log("starting new query");
                            break;
                    }
                }
            }
        }

        @Override
        public void stopNetworkQuery(INetworkQueryServiceCallback cb) {
            unregisterCallback(cb);
        }

        @Override
        public void unregisterCallback(INetworkQueryServiceCallback cb) {
            if (cb != null) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    NetworkQueryService.log("unregistering callback " + cb.getClass().toString());
                    NetworkQueryService.this.mCallbacks.unregister(cb);
                }
            }
        }
    };

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        INetworkQueryService getService() {
            return NetworkQueryService.this.mBinder;
        }
    }

    @Override
    public void onCreate() {
        this.mState = -1;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(intent.getIntExtra("sub_id", SubscriptionManager.getDefaultSubId())));
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("binding service implementation");
        this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(intent.getIntExtra("sub_id", SubscriptionManager.getDefaultSubId())));
        return this.mLocalBinder;
    }

    private void broadcastQueryResults(AsyncResult ar) {
        synchronized (this.mCallbacks) {
            this.mState = -1;
            if (ar == null) {
                log("AsyncResult is null.");
                return;
            }
            int exception = ar.exception == null ? 0 : 1;
            log("AsyncResult has exception " + exception);
            for (int i = this.mCallbacks.beginBroadcast() - 1; i >= 0; i--) {
                INetworkQueryServiceCallback cb = (INetworkQueryServiceCallback) this.mCallbacks.getBroadcastItem(i);
                log("broadcasting results to " + cb.getClass().toString());
                try {
                    cb.onQueryComplete((ArrayList) ar.result, exception);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
        }
    }

    private static void log(String msg) {
        Log.d("NetworkQuery", msg);
    }
}
