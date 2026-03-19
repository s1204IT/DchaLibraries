package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.UserHandle;
import android.telecom.Log;
import android.telecom.PhoneAccountHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.BluetoothPhoneServiceImpl;
import com.android.server.telecom.CallAudioManager;
import com.android.server.telecom.CallIntentProcessor;
import com.android.server.telecom.ConnectionServiceFocusManager;
import com.android.server.telecom.ContactsAsyncHelper;
import com.android.server.telecom.DefaultDialerCache;
import com.android.server.telecom.InCallTonePlayer;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.TelecomServiceImpl;
import com.android.server.telecom.Timeouts;
import com.android.server.telecom.bluetooth.BluetoothDeviceManager;
import com.android.server.telecom.bluetooth.BluetoothRouteManager;
import com.android.server.telecom.bluetooth.BluetoothStateReceiver;
import com.android.server.telecom.components.UserCallIntentProcessor;
import com.android.server.telecom.components.UserCallIntentProcessorFactory;
import com.android.server.telecom.ui.IncomingCallNotifier;
import com.android.server.telecom.ui.MissedCallNotifierImpl;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class TelecomSystem {
    private static TelecomSystem INSTANCE = null;
    private static final String TELECOM_TESTS_PACKAGE_NAME = "com.android.server.telecom.tests";
    private static boolean sIsInitDoneForTest;
    private final BluetoothPhoneServiceImpl mBluetoothPhoneServiceImpl;
    private final CallIntentProcessor mCallIntentProcessor;
    private final CallsManager mCallsManager;
    private final ContactsAsyncHelper mContactsAsyncHelper;
    private final Context mContext;
    private final DialerCodeReceiver mDialerCodeReceiver;
    private final IncomingCallNotifier mIncomingCallNotifier;
    private final MissedCallNotifier mMissedCallNotifier;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final RespondViaSmsManager mRespondViaSmsManager;
    private final TelecomBroadcastIntentProcessor mTelecomBroadcastIntentProcessor;
    private final TelecomServiceImpl mTelecomServiceImpl;
    private static final IntentFilter USER_SWITCHED_FILTER = new IntentFilter("android.intent.action.USER_SWITCHED");
    private static final IntentFilter USER_STARTING_FILTER = new IntentFilter("android.intent.action.USER_STARTING");
    private static final IntentFilter BOOT_COMPLETE_FILTER = new IntentFilter("android.intent.action.BOOT_COMPLETED");
    private static final IntentFilter DIALER_SECRET_CODE_FILTER = new IntentFilter("android.provider.Telephony.SECRET_CODE");
    private final SyncRoot mLock = new SyncRoot() {
    };
    private boolean mIsBootComplete = false;
    private final BroadcastReceiver mUserSwitchedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.startSession("TSSwR.oR");
            try {
                synchronized (TelecomSystem.this.mLock) {
                    UserHandle userHandle = new UserHandle(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    TelecomSystem.this.mPhoneAccountRegistrar.setCurrentUserHandle(userHandle);
                    TelecomSystem.this.mCallsManager.onUserSwitch(userHandle);
                }
            } finally {
                Log.endSession();
            }
        }
    };
    private final BroadcastReceiver mUserStartingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.startSession("TSStR.oR");
            try {
                synchronized (TelecomSystem.this.mLock) {
                    TelecomSystem.this.mCallsManager.onUserStarting(new UserHandle(intent.getIntExtra("android.intent.extra.user_handle", 0)));
                }
            } finally {
                Log.endSession();
            }
        }
    };
    private final BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.startSession("TSBCR.oR");
            try {
                synchronized (TelecomSystem.this.mLock) {
                    TelecomSystem.this.mIsBootComplete = true;
                    TelecomSystem.this.mCallsManager.onBootCompleted();
                }
            } finally {
                Log.endSession();
            }
        }
    };

    public interface SyncRoot {
    }

    static {
        DIALER_SECRET_CODE_FILTER.addDataScheme("android_secret_code");
        DIALER_SECRET_CODE_FILTER.addDataAuthority("823241", null);
        DIALER_SECRET_CODE_FILTER.addDataAuthority("823240", null);
        DIALER_SECRET_CODE_FILTER.addDataAuthority("826275", null);
        INSTANCE = null;
        sIsInitDoneForTest = false;
    }

    public static TelecomSystem getInstance() {
        return INSTANCE;
    }

    public static void setInstance(TelecomSystem telecomSystem) {
        if (INSTANCE != null) {
            Log.w("TelecomSystem", "Attempt to set TelecomSystem.INSTANCE twice", new Object[0]);
        }
        Log.i(TelecomSystem.class, "TelecomSystem.INSTANCE being set", new Object[0]);
        INSTANCE = telecomSystem;
    }

    public TelecomSystem(Context context, MissedCallNotifierImpl.MissedCallNotifierImplFactory missedCallNotifierImplFactory, CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory, HeadsetMediaButtonFactory headsetMediaButtonFactory, ProximitySensorManagerFactory proximitySensorManagerFactory, InCallWakeLockControllerFactory inCallWakeLockControllerFactory, CallAudioManager.AudioServiceFactory audioServiceFactory, BluetoothPhoneServiceImpl.BluetoothPhoneServiceImplFactory bluetoothPhoneServiceImplFactory, ConnectionServiceFocusManager.ConnectionServiceFocusManagerFactory connectionServiceFocusManagerFactory, Timeouts.Adapter adapter, AsyncRingtonePlayer asyncRingtonePlayer, PhoneNumberUtilsAdapter phoneNumberUtilsAdapter, IncomingCallNotifier incomingCallNotifier, InCallTonePlayer.ToneGeneratorFactory toneGeneratorFactory, ClockProxy clockProxy) {
        this.mContext = context.getApplicationContext();
        if (this.mContext.getPackageName().equals(TELECOM_TESTS_PACKAGE_NAME)) {
            if (!sIsInitDoneForTest) {
                LogUtils.initLogging(this.mContext);
                sIsInitDoneForTest = true;
            }
        } else {
            LogUtils.initLogging(this.mContext);
        }
        DefaultDialerCache defaultDialerCache = new DefaultDialerCache(this.mContext, new DefaultDialerCache.DefaultDialerManagerAdapterImpl(), this.mLock);
        Log.startSession("TS.init");
        this.mPhoneAccountRegistrar = new PhoneAccountRegistrar(this.mContext, defaultDialerCache, new PhoneAccountRegistrar.AppLabelProxy() {
            @Override
            public CharSequence getAppLabel(String str) {
                PackageManager packageManager = TelecomSystem.this.mContext.getPackageManager();
                try {
                    return packageManager.getApplicationLabel(packageManager.getApplicationInfo(str, 0));
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(this, "Could not determine package name.", new Object[0]);
                    return null;
                }
            }
        });
        this.mContactsAsyncHelper = new ContactsAsyncHelper(new ContactsAsyncHelper.ContentResolverAdapter() {
            @Override
            public InputStream openInputStream(Context context2, Uri uri) throws FileNotFoundException {
                return context2.getContentResolver().openInputStream(uri);
            }
        });
        BluetoothDeviceManager bluetoothDeviceManager = new BluetoothDeviceManager(this.mContext, new BluetoothAdapterProxy(), this.mLock);
        BluetoothRouteManager bluetoothRouteManager = new BluetoothRouteManager(this.mContext, this.mLock, bluetoothDeviceManager, new Timeouts.Adapter());
        BluetoothStateReceiver bluetoothStateReceiver = new BluetoothStateReceiver(bluetoothDeviceManager, bluetoothRouteManager);
        this.mContext.registerReceiver(bluetoothStateReceiver, BluetoothStateReceiver.INTENT_FILTER);
        WiredHeadsetManager wiredHeadsetManager = new WiredHeadsetManager(this.mContext);
        SystemStateProvider systemStateProvider = new SystemStateProvider(this.mContext);
        this.mMissedCallNotifier = missedCallNotifierImplFactory.makeMissedCallNotifierImpl(this.mContext, this.mPhoneAccountRegistrar, defaultDialerCache);
        this.mCallsManager = new CallsManager(this.mContext, this.mLock, this.mContactsAsyncHelper, callerInfoAsyncQueryFactory, this.mMissedCallNotifier, this.mPhoneAccountRegistrar, headsetMediaButtonFactory, proximitySensorManagerFactory, inCallWakeLockControllerFactory, connectionServiceFocusManagerFactory, audioServiceFactory, bluetoothRouteManager, wiredHeadsetManager, systemStateProvider, defaultDialerCache, adapter, asyncRingtonePlayer, phoneNumberUtilsAdapter, new EmergencyCallHelper(this.mContext, this.mContext.getResources().getString(R.string.ui_default_package), adapter), toneGeneratorFactory, clockProxy, bluetoothStateReceiver, new InCallControllerFactory() {
            @Override
            public InCallController create(Context context2, SyncRoot syncRoot, CallsManager callsManager, SystemStateProvider systemStateProvider2, DefaultDialerCache defaultDialerCache2, Timeouts.Adapter adapter2, EmergencyCallHelper emergencyCallHelper) {
                return new InCallController(context2, syncRoot, callsManager, systemStateProvider2, defaultDialerCache2, adapter2, emergencyCallHelper);
            }
        });
        this.mIncomingCallNotifier = incomingCallNotifier;
        incomingCallNotifier.setCallsManagerProxy(new IncomingCallNotifier.CallsManagerProxy() {
            @Override
            public boolean hasCallsForOtherPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
                return TelecomSystem.this.mCallsManager.hasCallsForOtherPhoneAccount(phoneAccountHandle);
            }

            @Override
            public int getNumCallsForOtherPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
                return TelecomSystem.this.mCallsManager.getNumCallsForOtherPhoneAccount(phoneAccountHandle);
            }

            @Override
            public Call getActiveCall() {
                return TelecomSystem.this.mCallsManager.getActiveCall();
            }
        });
        this.mCallsManager.setIncomingCallNotifier(this.mIncomingCallNotifier);
        this.mRespondViaSmsManager = new RespondViaSmsManager(this.mCallsManager, this.mLock);
        this.mCallsManager.setRespondViaSmsManager(this.mRespondViaSmsManager);
        this.mContext.registerReceiver(this.mUserSwitchedReceiver, USER_SWITCHED_FILTER);
        this.mContext.registerReceiver(this.mUserStartingReceiver, USER_STARTING_FILTER);
        this.mContext.registerReceiver(this.mBootCompletedReceiver, BOOT_COMPLETE_FILTER);
        this.mBluetoothPhoneServiceImpl = bluetoothPhoneServiceImplFactory.makeBluetoothPhoneServiceImpl(this.mContext, this.mLock, this.mCallsManager, this.mPhoneAccountRegistrar);
        this.mCallIntentProcessor = new CallIntentProcessor(this.mContext, this.mCallsManager);
        this.mTelecomBroadcastIntentProcessor = new TelecomBroadcastIntentProcessor(this.mContext, this.mCallsManager);
        this.mDialerCodeReceiver = new DialerCodeReceiver(this.mCallsManager);
        this.mContext.registerReceiver(this.mDialerCodeReceiver, DIALER_SECRET_CODE_FILTER, "android.permission.CONTROL_INCALL_EXPERIENCE", null);
        this.mTelecomServiceImpl = new TelecomServiceImpl(this.mContext, this.mCallsManager, this.mPhoneAccountRegistrar, new CallIntentProcessor.AdapterImpl(), new UserCallIntentProcessorFactory() {
            @Override
            public UserCallIntentProcessor create(Context context2, UserHandle userHandle) {
                return new UserCallIntentProcessor(context2, userHandle);
            }
        }, defaultDialerCache, new TelecomServiceImpl.SubscriptionManagerAdapterImpl(), this.mLock);
        Log.endSession();
    }

    @VisibleForTesting
    public PhoneAccountRegistrar getPhoneAccountRegistrar() {
        return this.mPhoneAccountRegistrar;
    }

    @VisibleForTesting
    public CallsManager getCallsManager() {
        return this.mCallsManager;
    }

    public BluetoothPhoneServiceImpl getBluetoothPhoneServiceImpl() {
        return this.mBluetoothPhoneServiceImpl;
    }

    public CallIntentProcessor getCallIntentProcessor() {
        return this.mCallIntentProcessor;
    }

    public TelecomBroadcastIntentProcessor getTelecomBroadcastIntentProcessor() {
        return this.mTelecomBroadcastIntentProcessor;
    }

    public TelecomServiceImpl getTelecomServiceImpl() {
        return this.mTelecomServiceImpl;
    }

    public Object getLock() {
        return this.mLock;
    }

    public boolean isBootComplete() {
        return this.mIsBootComplete;
    }
}
