package android.nfc;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.nfc.IAppCallback;
import android.nfc.NfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class NfcActivityManager extends IAppCallback.Stub implements Application.ActivityLifecycleCallbacks {
    static final Boolean DBG = false;
    static final String TAG = "NFC";
    final NfcAdapter mAdapter;
    final List<NfcActivityState> mActivities = new LinkedList();
    final List<NfcApplicationState> mApps = new ArrayList(1);

    class NfcApplicationState {
        final Application app;
        int refCount = 0;

        public NfcApplicationState(Application application) {
            this.app = application;
        }

        public void register() {
            this.refCount++;
            if (this.refCount == 1) {
                this.app.registerActivityLifecycleCallbacks(NfcActivityManager.this);
            }
        }

        public void unregister() {
            this.refCount--;
            if (this.refCount == 0) {
                this.app.unregisterActivityLifecycleCallbacks(NfcActivityManager.this);
            } else if (this.refCount < 0) {
                Log.e(NfcActivityManager.TAG, "-ve refcount for " + this.app);
            }
        }
    }

    NfcApplicationState findAppState(Application application) {
        for (NfcApplicationState nfcApplicationState : this.mApps) {
            if (nfcApplicationState.app == application) {
                return nfcApplicationState;
            }
        }
        return null;
    }

    void registerApplication(Application application) {
        NfcApplicationState nfcApplicationStateFindAppState = findAppState(application);
        if (nfcApplicationStateFindAppState == null) {
            nfcApplicationStateFindAppState = new NfcApplicationState(application);
            this.mApps.add(nfcApplicationStateFindAppState);
        }
        nfcApplicationStateFindAppState.register();
    }

    void unregisterApplication(Application application) {
        NfcApplicationState nfcApplicationStateFindAppState = findAppState(application);
        if (nfcApplicationStateFindAppState == null) {
            Log.e(TAG, "app was not registered " + application);
            return;
        }
        nfcApplicationStateFindAppState.unregister();
    }

    class NfcActivityState {
        Activity activity;
        boolean resumed;
        Binder token;
        NdefMessage ndefMessage = null;
        NfcAdapter.CreateNdefMessageCallback ndefMessageCallback = null;
        NfcAdapter.OnNdefPushCompleteCallback onNdefPushCompleteCallback = null;
        NfcAdapter.CreateBeamUrisCallback uriCallback = null;
        Uri[] uris = null;
        int flags = 0;
        int readerModeFlags = 0;
        NfcAdapter.ReaderCallback readerCallback = null;
        Bundle readerModeExtras = null;

        public NfcActivityState(Activity activity) {
            this.resumed = false;
            if (activity.getWindow().isDestroyed()) {
                throw new IllegalStateException("activity is already destroyed");
            }
            this.resumed = activity.isResumed();
            this.activity = activity;
            this.token = new Binder();
            NfcActivityManager.this.registerApplication(activity.getApplication());
        }

        public void destroy() {
            NfcActivityManager.this.unregisterApplication(this.activity.getApplication());
            this.resumed = false;
            this.activity = null;
            this.ndefMessage = null;
            this.ndefMessageCallback = null;
            this.onNdefPushCompleteCallback = null;
            this.uriCallback = null;
            this.uris = null;
            this.readerModeFlags = 0;
            this.token = null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.ndefMessage);
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.ndefMessageCallback);
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.uriCallback);
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            if (this.uris != null) {
                for (Uri uri : this.uris) {
                    sb.append(this.onNdefPushCompleteCallback);
                    sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    sb.append(uri);
                    sb.append("]");
                }
            }
            return sb.toString();
        }
    }

    synchronized NfcActivityState findActivityState(Activity activity) {
        for (NfcActivityState nfcActivityState : this.mActivities) {
            if (nfcActivityState.activity == activity) {
                return nfcActivityState;
            }
        }
        return null;
    }

    synchronized NfcActivityState getActivityState(Activity activity) {
        NfcActivityState nfcActivityStateFindActivityState;
        nfcActivityStateFindActivityState = findActivityState(activity);
        if (nfcActivityStateFindActivityState == null) {
            nfcActivityStateFindActivityState = new NfcActivityState(activity);
            this.mActivities.add(nfcActivityStateFindActivityState);
        }
        return nfcActivityStateFindActivityState;
    }

    synchronized NfcActivityState findResumedActivityState() {
        for (NfcActivityState nfcActivityState : this.mActivities) {
            if (nfcActivityState.resumed) {
                return nfcActivityState;
            }
        }
        return null;
    }

    synchronized void destroyActivityState(Activity activity) {
        NfcActivityState nfcActivityStateFindActivityState = findActivityState(activity);
        if (nfcActivityStateFindActivityState != null) {
            nfcActivityStateFindActivityState.destroy();
            this.mActivities.remove(nfcActivityStateFindActivityState);
        }
    }

    public NfcActivityManager(NfcAdapter nfcAdapter) {
        this.mAdapter = nfcAdapter;
    }

    public void enableReaderMode(Activity activity, NfcAdapter.ReaderCallback readerCallback, int i, Bundle bundle) {
        Binder binder;
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.readerCallback = readerCallback;
            activityState.readerModeFlags = i;
            activityState.readerModeExtras = bundle;
            binder = activityState.token;
            z = activityState.resumed;
        }
        if (z) {
            setReaderMode(binder, i, bundle);
        }
    }

    public void disableReaderMode(Activity activity) {
        Binder binder;
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.readerCallback = null;
            activityState.readerModeFlags = 0;
            activityState.readerModeExtras = null;
            binder = activityState.token;
            z = activityState.resumed;
        }
        if (z) {
            setReaderMode(binder, 0, null);
        }
    }

    public void setReaderMode(Binder binder, int i, Bundle bundle) {
        if (DBG.booleanValue()) {
            Log.d(TAG, "Setting reader mode");
        }
        try {
            NfcAdapter.sService.setReaderMode(binder, this, i, bundle);
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    public void setNdefPushContentUri(Activity activity, Uri[] uriArr) {
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.uris = uriArr;
            z = activityState.resumed;
        }
        if (z) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushContentUriCallback(Activity activity, NfcAdapter.CreateBeamUrisCallback createBeamUrisCallback) {
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.uriCallback = createBeamUrisCallback;
            z = activityState.resumed;
        }
        if (z) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushMessage(Activity activity, NdefMessage ndefMessage, int i) {
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.ndefMessage = ndefMessage;
            activityState.flags = i;
            z = activityState.resumed;
        }
        if (z) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushMessageCallback(Activity activity, NfcAdapter.CreateNdefMessageCallback createNdefMessageCallback, int i) {
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.ndefMessageCallback = createNdefMessageCallback;
            activityState.flags = i;
            z = activityState.resumed;
        }
        if (z) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setOnNdefPushCompleteCallback(Activity activity, NfcAdapter.OnNdefPushCompleteCallback onNdefPushCompleteCallback) {
        boolean z;
        synchronized (this) {
            NfcActivityState activityState = getActivityState(activity);
            activityState.onNdefPushCompleteCallback = onNdefPushCompleteCallback;
            z = activityState.resumed;
        }
        if (z) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    void requestNfcServiceCallback() {
        try {
            NfcAdapter.sService.setAppCallback(this);
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    void verifyNfcPermission() {
        try {
            NfcAdapter.sService.verifyNfcPermission();
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    @Override
    public BeamShareData createBeamShareData(byte b) {
        NfcEvent nfcEvent = new NfcEvent(this.mAdapter, b);
        synchronized (this) {
            NfcActivityState nfcActivityStateFindResumedActivityState = findResumedActivityState();
            if (nfcActivityStateFindResumedActivityState == null) {
                return null;
            }
            NfcAdapter.CreateNdefMessageCallback createNdefMessageCallback = nfcActivityStateFindResumedActivityState.ndefMessageCallback;
            NfcAdapter.CreateBeamUrisCallback createBeamUrisCallback = nfcActivityStateFindResumedActivityState.uriCallback;
            NdefMessage ndefMessageCreateNdefMessage = nfcActivityStateFindResumedActivityState.ndefMessage;
            Uri[] uriArrCreateBeamUris = nfcActivityStateFindResumedActivityState.uris;
            int i = nfcActivityStateFindResumedActivityState.flags;
            Activity activity = nfcActivityStateFindResumedActivityState.activity;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            if (createNdefMessageCallback != null) {
                try {
                    ndefMessageCreateNdefMessage = createNdefMessageCallback.createNdefMessage(nfcEvent);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            if (createBeamUrisCallback != null && (uriArrCreateBeamUris = createBeamUrisCallback.createBeamUris(nfcEvent)) != null) {
                ArrayList arrayList = new ArrayList();
                for (Uri uri : uriArrCreateBeamUris) {
                    if (uri == null) {
                        Log.e(TAG, "Uri not allowed to be null.");
                    } else {
                        String scheme = uri.getScheme();
                        if (scheme == null || (!scheme.equalsIgnoreCase(ContentResolver.SCHEME_FILE) && !scheme.equalsIgnoreCase("content"))) {
                            Log.e(TAG, "Uri needs to have either scheme file or scheme content");
                        } else {
                            arrayList.add(ContentProvider.maybeAddUserId(uri, activity.getUserId()));
                        }
                    }
                }
                uriArrCreateBeamUris = (Uri[]) arrayList.toArray(new Uri[arrayList.size()]);
            }
            if (uriArrCreateBeamUris != null && uriArrCreateBeamUris.length > 0) {
                for (Uri uri2 : uriArrCreateBeamUris) {
                    activity.grantUriPermission("com.android.nfc", uri2, 1);
                }
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return new BeamShareData(ndefMessageCreateNdefMessage, uriArrCreateBeamUris, activity.getUser(), i);
        }
    }

    @Override
    public void onNdefPushComplete(byte b) {
        synchronized (this) {
            NfcActivityState nfcActivityStateFindResumedActivityState = findResumedActivityState();
            if (nfcActivityStateFindResumedActivityState == null) {
                return;
            }
            NfcAdapter.OnNdefPushCompleteCallback onNdefPushCompleteCallback = nfcActivityStateFindResumedActivityState.onNdefPushCompleteCallback;
            NfcEvent nfcEvent = new NfcEvent(this.mAdapter, b);
            if (onNdefPushCompleteCallback != null) {
                onNdefPushCompleteCallback.onNdefPushComplete(nfcEvent);
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) throws RemoteException {
        synchronized (this) {
            NfcActivityState nfcActivityStateFindResumedActivityState = findResumedActivityState();
            if (nfcActivityStateFindResumedActivityState == null) {
                return;
            }
            NfcAdapter.ReaderCallback readerCallback = nfcActivityStateFindResumedActivityState.readerCallback;
            if (readerCallback != null) {
                readerCallback.onTagDiscovered(tag);
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        synchronized (this) {
            NfcActivityState nfcActivityStateFindActivityState = findActivityState(activity);
            if (DBG.booleanValue()) {
                Log.d(TAG, "onResume() for " + activity + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + nfcActivityStateFindActivityState);
            }
            if (nfcActivityStateFindActivityState == null) {
                return;
            }
            nfcActivityStateFindActivityState.resumed = true;
            Binder binder = nfcActivityStateFindActivityState.token;
            int i = nfcActivityStateFindActivityState.readerModeFlags;
            Bundle bundle = nfcActivityStateFindActivityState.readerModeExtras;
            if (i != 0) {
                setReaderMode(binder, i, bundle);
            }
            requestNfcServiceCallback();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        synchronized (this) {
            NfcActivityState nfcActivityStateFindActivityState = findActivityState(activity);
            if (DBG.booleanValue()) {
                Log.d(TAG, "onPause() for " + activity + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + nfcActivityStateFindActivityState);
            }
            if (nfcActivityStateFindActivityState == null) {
                return;
            }
            nfcActivityStateFindActivityState.resumed = false;
            Binder binder = nfcActivityStateFindActivityState.token;
            boolean z = nfcActivityStateFindActivityState.readerModeFlags != 0;
            if (z) {
                setReaderMode(binder, 0, null);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        synchronized (this) {
            NfcActivityState nfcActivityStateFindActivityState = findActivityState(activity);
            if (DBG.booleanValue()) {
                Log.d(TAG, "onDestroy() for " + activity + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + nfcActivityStateFindActivityState);
            }
            if (nfcActivityStateFindActivityState != null) {
                destroyActivityState(activity);
            }
        }
    }
}
