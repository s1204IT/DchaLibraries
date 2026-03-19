package com.android.server.telecom.components;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.IAudioService;
import android.media.ToneGenerator;
import android.os.IBinder;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telecom.Log;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.server.telecom.AsyncRingtonePlayer;
import com.android.server.telecom.BluetoothAdapterProxy;
import com.android.server.telecom.BluetoothPhoneServiceImpl;
import com.android.server.telecom.CallAudioManager;
import com.android.server.telecom.CallerInfoAsyncQueryFactory;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.ClockProxy;
import com.android.server.telecom.ConnectionServiceFocusManager;
import com.android.server.telecom.DefaultDialerCache;
import com.android.server.telecom.HeadsetMediaButton;
import com.android.server.telecom.HeadsetMediaButtonFactory;
import com.android.server.telecom.InCallTonePlayer;
import com.android.server.telecom.InCallWakeLockController;
import com.android.server.telecom.InCallWakeLockControllerFactory;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.PhoneNumberUtilsAdapterImpl;
import com.android.server.telecom.ProximitySensorManager;
import com.android.server.telecom.ProximitySensorManagerFactory;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.TelecomWakeLock;
import com.android.server.telecom.Timeouts;
import com.android.server.telecom.ui.IncomingCallNotifier;
import com.android.server.telecom.ui.MissedCallNotifierImpl;
import com.android.server.telecom.ui.NotificationChannelManager;
import com.mediatek.internal.telephony.MtkCallerInfoAsyncQuery;
import com.mediatek.server.telecom.MtkTelecomGlobals;
import com.mediatek.server.telecom.MtkTelecomServiceImpl;

public class TelecomService extends Service {
    public static ToneGenerator lambda$zKQgBFeCPgPWMWVGOqypdYQ6XRA(int i, int i2) {
        return new ToneGenerator(i, i2);
    }

    @Override
    public IBinder onBind(Intent intent) {
        ITelecomService.Stub binder;
        Log.d(this, "onBind", new Object[0]);
        MtkTelecomGlobals.createInstance(getApplicationContext());
        initializeTelecomSystem(this);
        new MtkTelecomServiceImpl(getApplicationContext(), getTelecomSystem().getCallsManager(), getTelecomSystem().getPhoneAccountRegistrar(), (TelecomSystem.SyncRoot) getTelecomSystem().getLock());
        synchronized (getTelecomSystem().getLock()) {
            binder = getTelecomSystem().getTelecomServiceImpl().getBinder();
        }
        return binder;
    }

    static void initializeTelecomSystem(Context context) {
        if (TelecomSystem.getInstance() == null) {
            new NotificationChannelManager().createChannels(context);
            TelecomSystem.setInstance(new TelecomSystem(context, new MissedCallNotifierImpl.MissedCallNotifierImplFactory() {
                @Override
                public MissedCallNotifierImpl makeMissedCallNotifierImpl(Context context2, PhoneAccountRegistrar phoneAccountRegistrar, DefaultDialerCache defaultDialerCache) {
                    return new MissedCallNotifierImpl(context2, phoneAccountRegistrar, defaultDialerCache);
                }
            }, new CallerInfoAsyncQueryFactory() {
                @Override
                public CallerInfoAsyncQuery startQuery(int i, Context context2, String str, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj, int i2) {
                    Log.i(TelecomSystem.getInstance(), "CallerInfoAsyncQuery.startQuery number=%s cookie=%s subId=%s", new Object[]{Log.pii(str), obj, Integer.valueOf(i2)});
                    return MtkCallerInfoAsyncQuery.startQuery(i, context2, str, onQueryCompleteListener, obj, i2);
                }
            }, new HeadsetMediaButtonFactory() {
                @Override
                public HeadsetMediaButton create(Context context2, CallsManager callsManager, TelecomSystem.SyncRoot syncRoot) {
                    return new HeadsetMediaButton(context2, callsManager, syncRoot);
                }
            }, new ProximitySensorManagerFactory() {
                @Override
                public ProximitySensorManager create(Context context2, CallsManager callsManager) {
                    return new ProximitySensorManager(new TelecomWakeLock(context2, 32, ProximitySensorManager.class.getSimpleName()), callsManager);
                }
            }, new InCallWakeLockControllerFactory() {
                @Override
                public InCallWakeLockController create(Context context2, CallsManager callsManager) {
                    return new InCallWakeLockController(new TelecomWakeLock(context2, 26, InCallWakeLockController.class.getSimpleName()), callsManager);
                }
            }, new CallAudioManager.AudioServiceFactory() {
                @Override
                public IAudioService getAudioService() {
                    return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
                }
            }, new BluetoothPhoneServiceImpl.BluetoothPhoneServiceImplFactory() {
                @Override
                public BluetoothPhoneServiceImpl makeBluetoothPhoneServiceImpl(Context context2, TelecomSystem.SyncRoot syncRoot, CallsManager callsManager, PhoneAccountRegistrar phoneAccountRegistrar) {
                    return new BluetoothPhoneServiceImpl(context2, syncRoot, callsManager, new BluetoothAdapterProxy(), phoneAccountRegistrar);
                }
            }, new ConnectionServiceFocusManager.ConnectionServiceFocusManagerFactory() {
                @Override
                public ConnectionServiceFocusManager create(ConnectionServiceFocusManager.CallsManagerRequester callsManagerRequester, Looper looper) {
                    return new ConnectionServiceFocusManager(callsManagerRequester, looper);
                }
            }, new Timeouts.Adapter(), new AsyncRingtonePlayer(), new PhoneNumberUtilsAdapterImpl(), new IncomingCallNotifier(context), new InCallTonePlayer.ToneGeneratorFactory() {
                @Override
                public final ToneGenerator get(int i, int i2) {
                    return TelecomService.lambda$zKQgBFeCPgPWMWVGOqypdYQ6XRA(i, i2);
                }
            }, new ClockProxy() {
                @Override
                public long currentTimeMillis() {
                    return System.currentTimeMillis();
                }

                @Override
                public long elapsedRealtime() {
                    return SystemClock.elapsedRealtime();
                }
            }));
        }
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            context.startService(new Intent(context, (Class<?>) BluetoothPhoneService.class));
        }
    }

    public TelecomSystem getTelecomSystem() {
        return TelecomSystem.getInstance();
    }
}
