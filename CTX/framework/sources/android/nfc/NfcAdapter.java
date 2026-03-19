package android.nfc;

import android.annotation.SystemApi;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.OnActivityPausedListener;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.INfcAdapter;
import android.nfc.INfcUnlockHandler;
import android.nfc.ITagRemovedCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.HashMap;

public final class NfcAdapter {
    public static final String ACTION_ADAPTER_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED";
    public static final String ACTION_HANDOVER_TRANSFER_DONE = "android.nfc.action.HANDOVER_TRANSFER_DONE";
    public static final String ACTION_HANDOVER_TRANSFER_STARTED = "android.nfc.action.HANDOVER_TRANSFER_STARTED";
    public static final String ACTION_NDEF_DISCOVERED = "android.nfc.action.NDEF_DISCOVERED";
    public static final String ACTION_TAG_DISCOVERED = "android.nfc.action.TAG_DISCOVERED";
    public static final String ACTION_TAG_LEFT_FIELD = "android.nfc.action.TAG_LOST";
    public static final String ACTION_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED";
    public static final String ACTION_TRANSACTION_DETECTED = "android.nfc.action.TRANSACTION_DETECTED";
    public static final String EXTRA_ADAPTER_STATE = "android.nfc.extra.ADAPTER_STATE";
    public static final String EXTRA_AID = "android.nfc.extra.AID";
    public static final String EXTRA_DATA = "android.nfc.extra.DATA";
    public static final String EXTRA_HANDOVER_TRANSFER_STATUS = "android.nfc.extra.HANDOVER_TRANSFER_STATUS";
    public static final String EXTRA_HANDOVER_TRANSFER_URI = "android.nfc.extra.HANDOVER_TRANSFER_URI";
    public static final String EXTRA_ID = "android.nfc.extra.ID";
    public static final String EXTRA_NDEF_MESSAGES = "android.nfc.extra.NDEF_MESSAGES";
    public static final String EXTRA_READER_PRESENCE_CHECK_DELAY = "presence";
    public static final String EXTRA_SECURE_ELEMENT_NAME = "android.nfc.extra.SECURE_ELEMENT_NAME";
    public static final String EXTRA_TAG = "android.nfc.extra.TAG";

    @SystemApi
    public static final int FLAG_NDEF_PUSH_NO_CONFIRM = 1;
    public static final int FLAG_READER_NFC_A = 1;
    public static final int FLAG_READER_NFC_B = 2;
    public static final int FLAG_READER_NFC_BARCODE = 16;
    public static final int FLAG_READER_NFC_F = 4;
    public static final int FLAG_READER_NFC_V = 8;
    public static final int FLAG_READER_NO_PLATFORM_SOUNDS = 256;
    public static final int FLAG_READER_SKIP_NDEF_CHECK = 128;
    public static final int HANDOVER_TRANSFER_STATUS_FAILURE = 1;
    public static final int HANDOVER_TRANSFER_STATUS_SUCCESS = 0;
    public static final int STATE_OFF = 1;
    public static final int STATE_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_TURNING_ON = 2;
    static final String TAG = "NFC";
    static INfcCardEmulation sCardEmulationService;
    static boolean sHasNfcFeature;
    static boolean sIsInitialized = false;
    static HashMap<Context, NfcAdapter> sNfcAdapters = new HashMap<>();
    static INfcFCardEmulation sNfcFCardEmulationService;
    static NfcAdapter sNullContextNfcAdapter;
    static INfcAdapter sService;
    static INfcTag sTagService;
    final Context mContext;
    OnActivityPausedListener mForegroundDispatchListener = new OnActivityPausedListener() {
        @Override
        public void onPaused(Activity activity) {
            NfcAdapter.this.disableForegroundDispatchInternal(activity, true);
        }
    };
    final NfcActivityManager mNfcActivityManager = new NfcActivityManager(this);
    final HashMap<NfcUnlockHandler, INfcUnlockHandler> mNfcUnlockHandlers = new HashMap<>();
    ITagRemovedCallback mTagRemovedListener = null;
    final Object mLock = new Object();

    public interface CreateBeamUrisCallback {
        Uri[] createBeamUris(NfcEvent nfcEvent);
    }

    public interface CreateNdefMessageCallback {
        NdefMessage createNdefMessage(NfcEvent nfcEvent);
    }

    @SystemApi
    public interface NfcUnlockHandler {
        boolean onUnlockAttempted(Tag tag);
    }

    public interface OnNdefPushCompleteCallback {
        void onNdefPushComplete(NfcEvent nfcEvent);
    }

    public interface OnTagRemovedListener {
        void onTagRemoved();
    }

    public interface ReaderCallback {
        void onTagDiscovered(Tag tag);
    }

    private static boolean hasNfcFeature() {
        IPackageManager packageManager = ActivityThread.getPackageManager();
        if (packageManager != null) {
            try {
                return packageManager.hasSystemFeature(PackageManager.FEATURE_NFC, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "Package manager query failed, assuming no NFC feature", e);
                return false;
            }
        }
        Log.e(TAG, "Cannot get package manager, assuming no NFC feature");
        return false;
    }

    private static boolean hasNfcHceFeature() {
        IPackageManager packageManager = ActivityThread.getPackageManager();
        if (packageManager != null) {
            try {
                if (!packageManager.hasSystemFeature("android.hardware.nfc.hce", 0)) {
                    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF, 0)) {
                        return false;
                    }
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Package manager query failed, assuming no NFC feature", e);
                return false;
            }
        }
        Log.e(TAG, "Cannot get package manager, assuming no NFC feature");
        return false;
    }

    public static synchronized NfcAdapter getNfcAdapter(Context context) {
        if (!sIsInitialized) {
            sHasNfcFeature = hasNfcFeature();
            boolean zHasNfcHceFeature = hasNfcHceFeature();
            if (!sHasNfcFeature && !zHasNfcHceFeature) {
                Log.v(TAG, "this device does not have NFC support");
                throw new UnsupportedOperationException();
            }
            sService = getServiceInterface();
            if (sService == null) {
                Log.e(TAG, "could not retrieve NFC service");
                throw new UnsupportedOperationException();
            }
            if (sHasNfcFeature) {
                try {
                    sTagService = sService.getNfcTagInterface();
                } catch (RemoteException e) {
                    Log.e(TAG, "could not retrieve NFC Tag service");
                    throw new UnsupportedOperationException();
                }
            }
            if (zHasNfcHceFeature) {
                try {
                    sNfcFCardEmulationService = sService.getNfcFCardEmulationInterface();
                    try {
                        sCardEmulationService = sService.getNfcCardEmulationInterface();
                    } catch (RemoteException e2) {
                        Log.e(TAG, "could not retrieve card emulation service");
                        throw new UnsupportedOperationException();
                    }
                } catch (RemoteException e3) {
                    Log.e(TAG, "could not retrieve NFC-F card emulation service");
                    throw new UnsupportedOperationException();
                }
            }
            sIsInitialized = true;
        }
        if (context == null) {
            if (sNullContextNfcAdapter == null) {
                sNullContextNfcAdapter = new NfcAdapter(null);
            }
            return sNullContextNfcAdapter;
        }
        NfcAdapter nfcAdapter = sNfcAdapters.get(context);
        if (nfcAdapter == null) {
            nfcAdapter = new NfcAdapter(context);
            sNfcAdapters.put(context, nfcAdapter);
        }
        return nfcAdapter;
    }

    private static INfcAdapter getServiceInterface() {
        IBinder service = ServiceManager.getService("nfc");
        if (service == null) {
            return null;
        }
        return INfcAdapter.Stub.asInterface(service);
    }

    public static NfcAdapter getDefaultAdapter(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            throw new IllegalArgumentException("context not associated with any application (using a mock context?)");
        }
        NfcManager nfcManager = (NfcManager) applicationContext.getSystemService("nfc");
        if (nfcManager == null) {
            return null;
        }
        return nfcManager.getDefaultAdapter();
    }

    @Deprecated
    public static NfcAdapter getDefaultAdapter() {
        Log.w(TAG, "WARNING: NfcAdapter.getDefaultAdapter() is deprecated, use NfcAdapter.getDefaultAdapter(Context) instead", new Exception());
        return getNfcAdapter(null);
    }

    NfcAdapter(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return this.mContext;
    }

    public INfcAdapter getService() {
        isEnabled();
        return sService;
    }

    public INfcTag getTagService() {
        isEnabled();
        return sTagService;
    }

    public INfcCardEmulation getCardEmulationService() {
        isEnabled();
        return sCardEmulationService;
    }

    public INfcFCardEmulation getNfcFCardEmulationService() {
        isEnabled();
        return sNfcFCardEmulationService;
    }

    public INfcDta getNfcDtaInterface() {
        if (this.mContext == null) {
            throw new UnsupportedOperationException("You need a context on NfcAdapter to use the  NFC extras APIs");
        }
        try {
            return sService.getNfcDtaInterface(this.mContext.getPackageName());
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return null;
        }
    }

    public void attemptDeadServiceRecovery(Exception exc) {
        Log.e(TAG, "NFC service dead - attempting to recover", exc);
        INfcAdapter serviceInterface = getServiceInterface();
        if (serviceInterface == null) {
            Log.e(TAG, "could not retrieve NFC service during service recovery");
            return;
        }
        sService = serviceInterface;
        try {
            sTagService = serviceInterface.getNfcTagInterface();
            try {
                sCardEmulationService = serviceInterface.getNfcCardEmulationInterface();
            } catch (RemoteException e) {
                Log.e(TAG, "could not retrieve NFC card emulation service during service recovery");
            }
            try {
                sNfcFCardEmulationService = serviceInterface.getNfcFCardEmulationInterface();
            } catch (RemoteException e2) {
                Log.e(TAG, "could not retrieve NFC-F card emulation service during service recovery");
            }
        } catch (RemoteException e3) {
            Log.e(TAG, "could not retrieve NFC tag service during service recovery");
        }
    }

    public boolean isEnabled() {
        try {
            return sService.getState() == 3;
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public int getAdapterState() {
        try {
            return sService.getState();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return 1;
        }
    }

    @SystemApi
    public boolean enable() {
        try {
            return sService.enable();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @SystemApi
    public boolean disable() {
        try {
            return sService.disable(true);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @SystemApi
    public boolean disable(boolean z) {
        try {
            return sService.disable(z);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public void pausePolling(int i) {
        try {
            sService.pausePolling(i);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void resumePolling() {
        try {
            sService.resumePolling();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void setBeamPushUris(Uri[] uriArr, Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null) {
            throw new NullPointerException("activity cannot be null");
        }
        if (uriArr != null) {
            for (Uri uri : uriArr) {
                if (uri == null) {
                    throw new NullPointerException("Uri not allowed to be null");
                }
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equalsIgnoreCase(ContentResolver.SCHEME_FILE) && !scheme.equalsIgnoreCase("content"))) {
                    throw new IllegalArgumentException("URI needs to have either scheme file or scheme content");
                }
            }
        }
        this.mNfcActivityManager.setNdefPushContentUri(activity, uriArr);
    }

    public void setBeamPushUrisCallback(CreateBeamUrisCallback createBeamUrisCallback, Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null) {
            throw new NullPointerException("activity cannot be null");
        }
        this.mNfcActivityManager.setNdefPushContentUriCallback(activity, createBeamUrisCallback);
    }

    public void setNdefPushMessage(NdefMessage ndefMessage, Activity activity, Activity... activityArr) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        int sdkVersion = getSdkVersion();
        try {
            if (activity == null) {
                throw new NullPointerException("activity cannot be null");
            }
            this.mNfcActivityManager.setNdefPushMessage(activity, ndefMessage, 0);
            for (Activity activity2 : activityArr) {
                if (activity2 == null) {
                    throw new NullPointerException("activities cannot contain null");
                }
                this.mNfcActivityManager.setNdefPushMessage(activity2, ndefMessage, 0);
            }
        } catch (IllegalStateException e) {
            if (sdkVersion < 16) {
                Log.e(TAG, "Cannot call API with Activity that has already been destroyed", e);
                return;
            }
            throw e;
        }
    }

    @SystemApi
    public void setNdefPushMessage(NdefMessage ndefMessage, Activity activity, int i) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null) {
            throw new NullPointerException("activity cannot be null");
        }
        this.mNfcActivityManager.setNdefPushMessage(activity, ndefMessage, i);
    }

    public void setNdefPushMessageCallback(CreateNdefMessageCallback createNdefMessageCallback, Activity activity, Activity... activityArr) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        int sdkVersion = getSdkVersion();
        try {
            if (activity == null) {
                throw new NullPointerException("activity cannot be null");
            }
            this.mNfcActivityManager.setNdefPushMessageCallback(activity, createNdefMessageCallback, 0);
            for (Activity activity2 : activityArr) {
                if (activity2 == null) {
                    throw new NullPointerException("activities cannot contain null");
                }
                this.mNfcActivityManager.setNdefPushMessageCallback(activity2, createNdefMessageCallback, 0);
            }
        } catch (IllegalStateException e) {
            if (sdkVersion < 16) {
                Log.e(TAG, "Cannot call API with Activity that has already been destroyed", e);
                return;
            }
            throw e;
        }
    }

    public void setNdefPushMessageCallback(CreateNdefMessageCallback createNdefMessageCallback, Activity activity, int i) {
        if (activity == null) {
            throw new NullPointerException("activity cannot be null");
        }
        this.mNfcActivityManager.setNdefPushMessageCallback(activity, createNdefMessageCallback, i);
    }

    public void setOnNdefPushCompleteCallback(OnNdefPushCompleteCallback onNdefPushCompleteCallback, Activity activity, Activity... activityArr) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        int sdkVersion = getSdkVersion();
        try {
            if (activity == null) {
                throw new NullPointerException("activity cannot be null");
            }
            this.mNfcActivityManager.setOnNdefPushCompleteCallback(activity, onNdefPushCompleteCallback);
            for (Activity activity2 : activityArr) {
                if (activity2 == null) {
                    throw new NullPointerException("activities cannot contain null");
                }
                this.mNfcActivityManager.setOnNdefPushCompleteCallback(activity2, onNdefPushCompleteCallback);
            }
        } catch (IllegalStateException e) {
            if (sdkVersion < 16) {
                Log.e(TAG, "Cannot call API with Activity that has already been destroyed", e);
                return;
            }
            throw e;
        }
    }

    public void enableForegroundDispatch(Activity activity, PendingIntent pendingIntent, IntentFilter[] intentFilterArr, String[][] strArr) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null || pendingIntent == null) {
            throw new NullPointerException();
        }
        if (!activity.isResumed()) {
            throw new IllegalStateException("Foreground dispatch can only be enabled when your activity is resumed");
        }
        TechListParcel techListParcel = null;
        if (strArr != null) {
            try {
                if (strArr.length > 0) {
                    techListParcel = new TechListParcel(strArr);
                }
            } catch (RemoteException e) {
                attemptDeadServiceRecovery(e);
                return;
            }
        }
        ActivityThread.currentActivityThread().registerOnActivityPausedListener(activity, this.mForegroundDispatchListener);
        sService.setForegroundDispatch(pendingIntent, intentFilterArr, techListParcel);
    }

    public void disableForegroundDispatch(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        ActivityThread.currentActivityThread().unregisterOnActivityPausedListener(activity, this.mForegroundDispatchListener);
        disableForegroundDispatchInternal(activity, false);
    }

    void disableForegroundDispatchInternal(Activity activity, boolean z) {
        try {
            sService.setForegroundDispatch(null, null, null);
            if (!z && !activity.isResumed()) {
                throw new IllegalStateException("You must disable foreground dispatching while your activity is still resumed");
            }
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void enableReaderMode(Activity activity, ReaderCallback readerCallback, int i, Bundle bundle) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        this.mNfcActivityManager.enableReaderMode(activity, readerCallback, i, bundle);
    }

    public void disableReaderMode(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        this.mNfcActivityManager.disableReaderMode(activity);
    }

    public boolean invokeBeam(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null) {
            throw new NullPointerException("activity may not be null.");
        }
        enforceResumed(activity);
        try {
            sService.invokeBeam();
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "invokeBeam: NFC process has died.");
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public boolean invokeBeam(BeamShareData beamShareData) {
        try {
            Log.e(TAG, "invokeBeamInternal()");
            sService.invokeBeamInternal(beamShareData);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "invokeBeam: NFC process has died.");
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @Deprecated
    public void enableForegroundNdefPush(Activity activity, NdefMessage ndefMessage) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null || ndefMessage == null) {
            throw new NullPointerException();
        }
        enforceResumed(activity);
        this.mNfcActivityManager.setNdefPushMessage(activity, ndefMessage, 0);
    }

    @Deprecated
    public void disableForegroundNdefPush(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null) {
            throw new NullPointerException();
        }
        enforceResumed(activity);
        this.mNfcActivityManager.setNdefPushMessage(activity, null, 0);
        this.mNfcActivityManager.setNdefPushMessageCallback(activity, null, 0);
        this.mNfcActivityManager.setOnNdefPushCompleteCallback(activity, null);
    }

    @SystemApi
    public boolean enableNdefPush() {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.enableNdefPush();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @SystemApi
    public boolean disableNdefPush() {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        try {
            return sService.disableNdefPush();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public boolean isNdefPushEnabled() {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        try {
            return sService.isNdefPushEnabled();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public boolean ignore(Tag tag, int i, final OnTagRemovedListener onTagRemovedListener, final Handler handler) {
        ITagRemovedCallback.Stub stub;
        if (onTagRemovedListener != null) {
            stub = new ITagRemovedCallback.Stub() {
                @Override
                public void onTagRemoved() throws RemoteException {
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onTagRemovedListener.onTagRemoved();
                            }
                        });
                    } else {
                        onTagRemovedListener.onTagRemoved();
                    }
                    synchronized (NfcAdapter.this.mLock) {
                        NfcAdapter.this.mTagRemovedListener = null;
                    }
                }
            };
        } else {
            stub = null;
        }
        synchronized (this.mLock) {
            this.mTagRemovedListener = stub;
        }
        try {
            return sService.ignore(tag.getServiceHandle(), i, stub);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void dispatch(Tag tag) {
        if (tag == null) {
            throw new NullPointerException("tag cannot be null");
        }
        try {
            sService.dispatch(tag);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void setP2pModes(int i, int i2) {
        try {
            sService.setP2pModes(i, i2);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    @SystemApi
    public boolean addNfcUnlockHandler(final NfcUnlockHandler nfcUnlockHandler, String[] strArr) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (strArr.length == 0) {
            return false;
        }
        try {
            synchronized (this.mLock) {
                if (this.mNfcUnlockHandlers.containsKey(nfcUnlockHandler)) {
                    sService.removeNfcUnlockHandler(this.mNfcUnlockHandlers.get(nfcUnlockHandler));
                    this.mNfcUnlockHandlers.remove(nfcUnlockHandler);
                }
                INfcUnlockHandler.Stub stub = new INfcUnlockHandler.Stub() {
                    @Override
                    public boolean onUnlockAttempted(Tag tag) throws RemoteException {
                        return nfcUnlockHandler.onUnlockAttempted(tag);
                    }
                };
                sService.addNfcUnlockHandler(stub, Tag.getTechCodesFromStrings(strArr));
                this.mNfcUnlockHandlers.put(nfcUnlockHandler, stub);
            }
            return true;
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Unable to register LockscreenDispatch", e2);
            return false;
        }
    }

    @SystemApi
    public boolean removeNfcUnlockHandler(NfcUnlockHandler nfcUnlockHandler) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        try {
            synchronized (this.mLock) {
                if (this.mNfcUnlockHandlers.containsKey(nfcUnlockHandler)) {
                    sService.removeNfcUnlockHandler(this.mNfcUnlockHandlers.remove(nfcUnlockHandler));
                }
            }
            return true;
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public INfcAdapterExtras getNfcAdapterExtrasInterface() {
        if (this.mContext == null) {
            throw new UnsupportedOperationException("You need a context on NfcAdapter to use the  NFC extras APIs");
        }
        try {
            return sService.getNfcAdapterExtrasInterface(this.mContext.getPackageName());
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return null;
        }
    }

    void enforceResumed(Activity activity) {
        if (!activity.isResumed()) {
            throw new IllegalStateException("API cannot be called while activity is paused");
        }
    }

    int getSdkVersion() {
        if (this.mContext == null) {
            return 9;
        }
        return this.mContext.getApplicationInfo().targetSdkVersion;
    }
}
