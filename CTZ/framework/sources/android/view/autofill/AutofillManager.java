package android.view.autofill;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Rect;
import android.metrics.LogMaker;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.SettingsStringUtil;
import android.service.autofill.FillEventHistory;
import android.service.autofill.UserData;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.autofill.AutofillManager;
import android.view.autofill.IAutoFillManagerClient;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
import sun.misc.Cleaner;

public final class AutofillManager {
    public static final int ACTION_START_SESSION = 1;
    public static final int ACTION_VALUE_CHANGED = 4;
    public static final int ACTION_VIEW_ENTERED = 2;
    public static final int ACTION_VIEW_EXITED = 3;
    private static final int AUTHENTICATION_ID_DATASET_ID_MASK = 65535;
    private static final int AUTHENTICATION_ID_DATASET_ID_SHIFT = 16;
    public static final int AUTHENTICATION_ID_DATASET_ID_UNDEFINED = 65535;
    public static final String EXTRA_ASSIST_STRUCTURE = "android.view.autofill.extra.ASSIST_STRUCTURE";
    public static final String EXTRA_AUTHENTICATION_RESULT = "android.view.autofill.extra.AUTHENTICATION_RESULT";
    public static final String EXTRA_CLIENT_STATE = "android.view.autofill.extra.CLIENT_STATE";
    public static final String EXTRA_RESTORE_SESSION_TOKEN = "android.view.autofill.extra.RESTORE_SESSION_TOKEN";
    public static final int FC_SERVICE_TIMEOUT = 5000;
    public static final int FLAG_ADD_CLIENT_DEBUG = 2;
    public static final int FLAG_ADD_CLIENT_ENABLED = 1;
    public static final int FLAG_ADD_CLIENT_VERBOSE = 4;
    private static final String LAST_AUTOFILLED_DATA_TAG = "android:lastAutoFilledData";
    public static final int NO_SESSION = Integer.MIN_VALUE;
    public static final int PENDING_UI_OPERATION_CANCEL = 1;
    public static final int PENDING_UI_OPERATION_RESTORE = 2;
    private static final String SESSION_ID_TAG = "android:sessionId";
    public static final int SET_STATE_FLAG_DEBUG = 8;
    public static final int SET_STATE_FLAG_ENABLED = 1;
    public static final int SET_STATE_FLAG_RESET_CLIENT = 4;
    public static final int SET_STATE_FLAG_RESET_SESSION = 2;
    public static final int SET_STATE_FLAG_VERBOSE = 16;
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_DISABLED_BY_SERVICE = 4;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_SHOWING_SAVE_UI = 3;
    private static final String STATE_TAG = "android:state";
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_UNKNOWN_COMPAT_MODE = 5;
    private static final String TAG = "AutofillManager";

    @GuardedBy("mLock")
    private AutofillCallback mCallback;

    @GuardedBy("mLock")
    private CompatibilityBridge mCompatibilityBridge;
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mEnabled;

    @GuardedBy("mLock")
    private ArraySet<AutofillId> mEnteredIds;

    @GuardedBy("mLock")
    private ArraySet<AutofillId> mFillableIds;
    private AutofillId mIdShownFillUi;

    @GuardedBy("mLock")
    private ParcelableMap mLastAutofilledData;

    @GuardedBy("mLock")
    private boolean mOnInvisibleCalled;

    @GuardedBy("mLock")
    private boolean mSaveOnFinish;

    @GuardedBy("mLock")
    private AutofillId mSaveTriggerId;
    private final IAutoFillManager mService;

    @GuardedBy("mLock")
    private IAutoFillManagerClient mServiceClient;

    @GuardedBy("mLock")
    private Cleaner mServiceClientCleaner;

    @GuardedBy("mLock")
    private TrackedViews mTrackedViews;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mSessionId = Integer.MIN_VALUE;

    @GuardedBy("mLock")
    private int mState = 0;

    public interface AutofillClient {
        void autofillClientAuthenticate(int i, IntentSender intentSender, Intent intent);

        void autofillClientDispatchUnhandledKey(View view, KeyEvent keyEvent);

        View autofillClientFindViewByAccessibilityIdTraversal(int i, int i2);

        View autofillClientFindViewByAutofillIdTraversal(AutofillId autofillId);

        View[] autofillClientFindViewsByAutofillIdTraversal(AutofillId[] autofillIdArr);

        IBinder autofillClientGetActivityToken();

        ComponentName autofillClientGetComponentName();

        AutofillId autofillClientGetNextAutofillId();

        boolean[] autofillClientGetViewVisibility(AutofillId[] autofillIdArr);

        boolean autofillClientIsCompatibilityModeEnabled();

        boolean autofillClientIsFillUiShowing();

        boolean autofillClientIsVisibleForAutofill();

        boolean autofillClientRequestHideFillUi();

        boolean autofillClientRequestShowFillUi(View view, int i, int i2, Rect rect, IAutofillWindowPresenter iAutofillWindowPresenter);

        void autofillClientResetableStateAvailable();

        void autofillClientRunOnUiThread(Runnable runnable);

        boolean isDisablingEnterExitEventForAutofill();
    }

    public static int makeAuthenticationId(int i, int i2) {
        return (i << 16) | (i2 & 65535);
    }

    public static int getRequestIdFromAuthenticationId(int i) {
        return i >> 16;
    }

    public static int getDatasetIdFromAuthenticationId(int i) {
        return i & 65535;
    }

    public AutofillManager(Context context, IAutoFillManager iAutoFillManager) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "context cannot be null");
        this.mService = iAutoFillManager;
    }

    public void enableCompatibilityMode() {
        synchronized (this.mLock) {
            this.mCompatibilityBridge = new CompatibilityBridge();
        }
    }

    public void onCreate(Bundle bundle) {
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            this.mLastAutofilledData = (ParcelableMap) bundle.getParcelable(LAST_AUTOFILLED_DATA_TAG);
            if (isActiveLocked()) {
                Log.w(TAG, "New session was started before onCreate()");
                return;
            }
            this.mSessionId = bundle.getInt(SESSION_ID_TAG, Integer.MIN_VALUE);
            this.mState = bundle.getInt(STATE_TAG, 0);
            if (this.mSessionId != Integer.MIN_VALUE) {
                ensureServiceClientAddedIfNeededLocked();
                AutofillClient client = getClient();
                if (client != null) {
                    try {
                        if (!this.mService.restoreSession(this.mSessionId, client.autofillClientGetActivityToken(), this.mServiceClient.asBinder())) {
                            Log.w(TAG, "Session " + this.mSessionId + " could not be restored");
                            this.mSessionId = Integer.MIN_VALUE;
                            this.mState = 0;
                        } else {
                            if (Helper.sDebug) {
                                Log.d(TAG, "session " + this.mSessionId + " was restored");
                            }
                            client.autofillClientResetableStateAvailable();
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not figure out if there was an autofill session", e);
                    }
                }
            }
        }
    }

    public void onVisibleForAutofill() {
        Choreographer.getInstance().postCallback(3, new Runnable() {
            @Override
            public final void run() {
                AutofillManager.lambda$onVisibleForAutofill$0(this.f$0);
            }
        }, null);
    }

    public static void lambda$onVisibleForAutofill$0(AutofillManager autofillManager) {
        synchronized (autofillManager.mLock) {
            if (autofillManager.mEnabled && autofillManager.isActiveLocked() && autofillManager.mTrackedViews != null) {
                autofillManager.mTrackedViews.onVisibleForAutofillChangedLocked();
            }
        }
    }

    public void onInvisibleForAutofill() {
        synchronized (this.mLock) {
            this.mOnInvisibleCalled = true;
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mSessionId != Integer.MIN_VALUE) {
                bundle.putInt(SESSION_ID_TAG, this.mSessionId);
            }
            if (this.mState != 0) {
                bundle.putInt(STATE_TAG, this.mState);
            }
            if (this.mLastAutofilledData != null) {
                bundle.putParcelable(LAST_AUTOFILLED_DATA_TAG, this.mLastAutofilledData);
            }
        }
    }

    @GuardedBy("mLock")
    public boolean isCompatibilityModeEnabledLocked() {
        return this.mCompatibilityBridge != null;
    }

    public boolean isEnabled() {
        if (!hasAutofillFeature()) {
            return false;
        }
        synchronized (this.mLock) {
            if (isDisabledByServiceLocked()) {
                return false;
            }
            ensureServiceClientAddedIfNeededLocked();
            return this.mEnabled;
        }
    }

    public FillEventHistory getFillEventHistory() {
        try {
            return this.mService.getFillEventHistory();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public void requestAutofill(View view) {
        notifyViewEntered(view, 1);
    }

    public void requestAutofill(View view, int i, Rect rect) {
        notifyViewEntered(view, i, rect, 1);
    }

    public void notifyViewEntered(View view) {
        notifyViewEntered(view, 0);
    }

    @GuardedBy("mLock")
    private boolean shouldIgnoreViewEnteredLocked(AutofillId autofillId, int i) {
        if (isDisabledByServiceLocked()) {
            if (Helper.sVerbose) {
                Log.v(TAG, "ignoring notifyViewEntered(flags=" + i + ", view=" + autofillId + ") on state " + getStateAsStringLocked() + " because disabled by svc");
            }
            return true;
        }
        if (isFinishedLocked() && (i & 1) == 0 && this.mEnteredIds != null && this.mEnteredIds.contains(autofillId)) {
            if (Helper.sVerbose) {
                Log.v(TAG, "ignoring notifyViewEntered(flags=" + i + ", view=" + autofillId + ") on state " + getStateAsStringLocked() + " because view was already entered: " + this.mEnteredIds);
            }
            return true;
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "not ignoring notifyViewEntered(flags=" + i + ", view=" + autofillId + ", state " + getStateAsStringLocked() + ", enteredIds=" + this.mEnteredIds);
            return false;
        }
        return false;
    }

    private boolean isClientVisibleForAutofillLocked() {
        AutofillClient client = getClient();
        return client != null && client.autofillClientIsVisibleForAutofill();
    }

    private boolean isClientDisablingEnterExitEvent() {
        AutofillClient client = getClient();
        return client != null && client.isDisablingEnterExitEventForAutofill();
    }

    private void notifyViewEntered(View view, int i) {
        AutofillCallback autofillCallbackNotifyViewEnteredLocked;
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            autofillCallbackNotifyViewEnteredLocked = notifyViewEnteredLocked(view, i);
        }
        if (autofillCallbackNotifyViewEnteredLocked != null) {
            this.mCallback.onAutofillEvent(view, 3);
        }
    }

    @GuardedBy("mLock")
    private AutofillCallback notifyViewEnteredLocked(View view, int i) {
        AutofillId autofillId = view.getAutofillId();
        if (shouldIgnoreViewEnteredLocked(autofillId, i)) {
            return null;
        }
        ensureServiceClientAddedIfNeededLocked();
        if (!this.mEnabled) {
            if (this.mCallback != null) {
                return this.mCallback;
            }
            return null;
        }
        if (isClientDisablingEnterExitEvent()) {
            return null;
        }
        AutofillValue autofillValue = view.getAutofillValue();
        if (!isActiveLocked()) {
            startSessionLocked(autofillId, null, autofillValue, i);
        } else {
            updateSessionLocked(autofillId, null, autofillValue, 2, i);
        }
        addEnteredIdLocked(autofillId);
        return null;
    }

    public void notifyViewExited(View view) {
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            notifyViewExitedLocked(view);
        }
    }

    @GuardedBy("mLock")
    void notifyViewExitedLocked(View view) {
        ensureServiceClientAddedIfNeededLocked();
        if (this.mEnabled && isActiveLocked() && !isClientDisablingEnterExitEvent()) {
            updateSessionLocked(view.getAutofillId(), null, null, 3, 0);
        }
    }

    public void notifyViewVisibilityChanged(View view, boolean z) {
        notifyViewVisibilityChangedInternal(view, 0, z, false);
    }

    public void notifyViewVisibilityChanged(View view, int i, boolean z) {
        notifyViewVisibilityChangedInternal(view, i, z, true);
    }

    private void notifyViewVisibilityChangedInternal(View view, int i, boolean z, boolean z2) {
        synchronized (this.mLock) {
            if (this.mEnabled && isActiveLocked()) {
                AutofillId autofillId = z2 ? getAutofillId(view, i) : view.getAutofillId();
                if (Helper.sVerbose) {
                    Log.v(TAG, "visibility changed for " + autofillId + ": " + z);
                }
                if (!z && this.mFillableIds != null && this.mFillableIds.contains(autofillId)) {
                    if (Helper.sDebug) {
                        Log.d(TAG, "Hidding UI when view " + autofillId + " became invisible");
                    }
                    requestHideFillUi(autofillId, view);
                }
                if (this.mTrackedViews != null) {
                    this.mTrackedViews.notifyViewVisibilityChangedLocked(autofillId, z);
                } else if (Helper.sVerbose) {
                    Log.v(TAG, "Ignoring visibility change on " + autofillId + ": no tracked views");
                }
            }
        }
    }

    public void notifyViewEntered(View view, int i, Rect rect) {
        notifyViewEntered(view, i, rect, 0);
    }

    private void notifyViewEntered(View view, int i, Rect rect, int i2) {
        AutofillCallback autofillCallbackNotifyViewEnteredLocked;
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            autofillCallbackNotifyViewEnteredLocked = notifyViewEnteredLocked(view, i, rect, i2);
        }
        if (autofillCallbackNotifyViewEnteredLocked != null) {
            autofillCallbackNotifyViewEnteredLocked.onAutofillEvent(view, i, 3);
        }
    }

    @GuardedBy("mLock")
    private AutofillCallback notifyViewEnteredLocked(View view, int i, Rect rect, int i2) {
        AutofillId autofillId = getAutofillId(view, i);
        if (shouldIgnoreViewEnteredLocked(autofillId, i2)) {
            return null;
        }
        ensureServiceClientAddedIfNeededLocked();
        if (!this.mEnabled) {
            if (this.mCallback != null) {
                return this.mCallback;
            }
            return null;
        }
        if (isClientDisablingEnterExitEvent()) {
            return null;
        }
        if (!isActiveLocked()) {
            startSessionLocked(autofillId, rect, null, i2);
        } else {
            updateSessionLocked(autofillId, rect, null, 2, i2);
        }
        addEnteredIdLocked(autofillId);
        return null;
    }

    @GuardedBy("mLock")
    private void addEnteredIdLocked(AutofillId autofillId) {
        if (this.mEnteredIds == null) {
            this.mEnteredIds = new ArraySet<>(1);
        }
        this.mEnteredIds.add(autofillId);
    }

    public void notifyViewExited(View view, int i) {
        if (Helper.sVerbose) {
            Log.v(TAG, "notifyViewExited(" + view.getAutofillId() + ", " + i);
        }
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            notifyViewExitedLocked(view, i);
        }
    }

    @GuardedBy("mLock")
    private void notifyViewExitedLocked(View view, int i) {
        ensureServiceClientAddedIfNeededLocked();
        if (this.mEnabled && isActiveLocked() && !isClientDisablingEnterExitEvent()) {
            updateSessionLocked(getAutofillId(view, i), null, null, 3, 0);
        }
    }

    public void notifyValueChanged(View view) {
        AutofillId autofillId;
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            AutofillValue autofillValue = null;
            boolean z = false;
            if (this.mLastAutofilledData == null) {
                view.setAutofilled(false);
                autofillId = null;
            } else {
                autofillId = view.getAutofillId();
                if (this.mLastAutofilledData.containsKey(autofillId)) {
                    autofillValue = view.getAutofillValue();
                    if (Objects.equals(this.mLastAutofilledData.get(autofillId), autofillValue)) {
                        view.setAutofilled(true);
                    } else {
                        view.setAutofilled(false);
                        this.mLastAutofilledData.remove(autofillId);
                    }
                    z = true;
                } else {
                    view.setAutofilled(false);
                }
            }
            if (this.mEnabled && isActiveLocked()) {
                if (autofillId == null) {
                    autofillId = view.getAutofillId();
                }
                AutofillId autofillId2 = autofillId;
                if (!z) {
                    autofillValue = view.getAutofillValue();
                }
                updateSessionLocked(autofillId2, null, autofillValue, 4, 0);
                return;
            }
            if (Helper.sVerbose) {
                Log.v(TAG, "notifyValueChanged(" + view.getAutofillId() + "): ignoring on state " + getStateAsStringLocked());
            }
        }
    }

    public void notifyValueChanged(View view, int i, AutofillValue autofillValue) {
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mEnabled && isActiveLocked()) {
                updateSessionLocked(getAutofillId(view, i), null, autofillValue, 4, 0);
                return;
            }
            if (Helper.sVerbose) {
                Log.v(TAG, "notifyValueChanged(" + view.getAutofillId() + SettingsStringUtil.DELIMITER + i + "): ignoring on state " + getStateAsStringLocked());
            }
        }
    }

    public void notifyViewClicked(View view) {
        notifyViewClicked(view.getAutofillId());
    }

    public void notifyViewClicked(View view, int i) {
        notifyViewClicked(getAutofillId(view, i));
    }

    private void notifyViewClicked(AutofillId autofillId) {
        if (!hasAutofillFeature()) {
            return;
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "notifyViewClicked(): id=" + autofillId + ", trigger=" + this.mSaveTriggerId);
        }
        synchronized (this.mLock) {
            if (this.mEnabled && isActiveLocked()) {
                if (this.mSaveTriggerId != null && this.mSaveTriggerId.equals(autofillId)) {
                    if (Helper.sDebug) {
                        Log.d(TAG, "triggering commit by click of " + autofillId);
                    }
                    commitLocked();
                    this.mMetricsLogger.write(newLog(MetricsProto.MetricsEvent.AUTOFILL_SAVE_EXPLICITLY_TRIGGERED));
                }
            }
        }
    }

    public void onActivityFinishing() {
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mSaveOnFinish) {
                if (Helper.sDebug) {
                    Log.d(TAG, "onActivityFinishing(): calling commitLocked()");
                }
                commitLocked();
            } else {
                if (Helper.sDebug) {
                    Log.d(TAG, "onActivityFinishing(): calling cancelLocked()");
                }
                cancelLocked();
            }
        }
    }

    public void commit() {
        if (!hasAutofillFeature()) {
            return;
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "commit() called by app");
        }
        synchronized (this.mLock) {
            commitLocked();
        }
    }

    @GuardedBy("mLock")
    private void commitLocked() {
        if (!this.mEnabled && !isActiveLocked()) {
            return;
        }
        finishSessionLocked();
    }

    public void cancel() {
        if (Helper.sVerbose) {
            Log.v(TAG, "cancel() called by app");
        }
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            cancelLocked();
        }
    }

    @GuardedBy("mLock")
    private void cancelLocked() {
        if (!this.mEnabled && !isActiveLocked()) {
            return;
        }
        cancelSessionLocked();
    }

    public void disableOwnedAutofillServices() {
        disableAutofillServices();
    }

    public void disableAutofillServices() {
        if (!hasAutofillFeature()) {
            return;
        }
        try {
            this.mService.disableOwnedAutofillServices(this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasEnabledAutofillServices() {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.isServiceEnabled(this.mContext.getUserId(), this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ComponentName getAutofillServiceComponentName() {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getAutofillServiceComponentName();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getUserDataId() {
        try {
            return this.mService.getUserDataId();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public UserData getUserData() {
        try {
            return this.mService.getUserData();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public void setUserData(UserData userData) {
        try {
            this.mService.setUserData(userData);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public boolean isFieldClassificationEnabled() {
        try {
            return this.mService.isFieldClassificationEnabled();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public String getDefaultFieldClassificationAlgorithm() {
        try {
            return this.mService.getDefaultFieldClassificationAlgorithm();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public List<String> getAvailableFieldClassificationAlgorithms() {
        try {
            String[] availableFieldClassificationAlgorithms = this.mService.getAvailableFieldClassificationAlgorithms();
            return availableFieldClassificationAlgorithms != null ? Arrays.asList(availableFieldClassificationAlgorithms) : Collections.emptyList();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public boolean isAutofillSupported() {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.isServiceSupported(this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private AutofillClient getClient() {
        AutofillClient autofillClient = this.mContext.getAutofillClient();
        if (autofillClient == null && Helper.sDebug) {
            Log.d(TAG, "No AutofillClient for " + this.mContext.getPackageName() + " on context " + this.mContext);
        }
        return autofillClient;
    }

    public boolean isAutofillUiShowing() {
        AutofillClient autofillClient = this.mContext.getAutofillClient();
        return autofillClient != null && autofillClient.autofillClientIsFillUiShowing();
    }

    public void onAuthenticationResult(int i, Intent intent, View view) {
        if (!hasAutofillFeature()) {
            return;
        }
        if (Helper.sDebug) {
            Log.d(TAG, "onAuthenticationResult(): d=" + intent);
        }
        synchronized (this.mLock) {
            if (isActiveLocked()) {
                if (!this.mOnInvisibleCalled && view != null && view.canNotifyAutofillEnterExitEvent()) {
                    notifyViewExitedLocked(view);
                    notifyViewEnteredLocked(view, 0);
                }
                if (intent == null) {
                    return;
                }
                Parcelable parcelableExtra = intent.getParcelableExtra(EXTRA_AUTHENTICATION_RESULT);
                Bundle bundle = new Bundle();
                bundle.putParcelable(EXTRA_AUTHENTICATION_RESULT, parcelableExtra);
                Bundle bundleExtra = intent.getBundleExtra(EXTRA_CLIENT_STATE);
                if (bundleExtra != null) {
                    bundle.putBundle(EXTRA_CLIENT_STATE, bundleExtra);
                }
                try {
                    this.mService.setAuthenticationResult(bundle, this.mSessionId, i, this.mContext.getUserId());
                } catch (RemoteException e) {
                    Log.e(TAG, "Error delivering authentication result", e);
                }
            }
        }
    }

    public AutofillId getNextAutofillId() {
        AutofillClient client = getClient();
        if (client == null) {
            return null;
        }
        AutofillId autofillIdAutofillClientGetNextAutofillId = client.autofillClientGetNextAutofillId();
        if (autofillIdAutofillClientGetNextAutofillId == null && Helper.sDebug) {
            Log.d(TAG, "getNextAutofillId(): client " + client + " returned null");
        }
        return autofillIdAutofillClientGetNextAutofillId;
    }

    private static AutofillId getAutofillId(View view, int i) {
        return new AutofillId(view.getAutofillViewId(), i);
    }

    @GuardedBy("mLock")
    private void startSessionLocked(AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i) {
        Rect rect2;
        AutofillValue autofillValue2;
        boolean z;
        if (Helper.sVerbose) {
            StringBuilder sb = new StringBuilder();
            sb.append("startSessionLocked(): id=");
            sb.append(autofillId);
            sb.append(", bounds=");
            rect2 = rect;
            sb.append(rect2);
            sb.append(", value=");
            autofillValue2 = autofillValue;
            sb.append(autofillValue2);
            sb.append(", flags=");
            sb.append(i);
            sb.append(", state=");
            sb.append(getStateAsStringLocked());
            sb.append(", compatMode=");
            sb.append(isCompatibilityModeEnabledLocked());
            sb.append(", enteredIds=");
            sb.append(this.mEnteredIds);
            Log.v(TAG, sb.toString());
        } else {
            rect2 = rect;
            autofillValue2 = autofillValue;
        }
        if (this.mState != 0 && !isFinishedLocked() && (i & 1) == 0) {
            if (Helper.sVerbose) {
                Log.v(TAG, "not automatically starting session for " + autofillId + " on state " + getStateAsStringLocked() + " and flags " + i);
                return;
            }
            return;
        }
        try {
            AutofillClient client = getClient();
            if (client == null) {
                return;
            }
            IAutoFillManager iAutoFillManager = this.mService;
            IBinder iBinderAutofillClientGetActivityToken = client.autofillClientGetActivityToken();
            IBinder iBinderAsBinder = this.mServiceClient.asBinder();
            int userId = this.mContext.getUserId();
            if (this.mCallback == null) {
                z = false;
            } else {
                z = true;
            }
            this.mSessionId = iAutoFillManager.startSession(iBinderAutofillClientGetActivityToken, iBinderAsBinder, autofillId, rect2, autofillValue2, userId, z, i, client.autofillClientGetComponentName(), isCompatibilityModeEnabledLocked());
            if (this.mSessionId != Integer.MIN_VALUE) {
                this.mState = 1;
            }
            client.autofillClientResetableStateAvailable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @GuardedBy("mLock")
    private void finishSessionLocked() {
        if (Helper.sVerbose) {
            Log.v(TAG, "finishSessionLocked(): " + getStateAsStringLocked());
        }
        if (isActiveLocked()) {
            try {
                this.mService.finishSession(this.mSessionId, this.mContext.getUserId());
                resetSessionLocked(true);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @GuardedBy("mLock")
    private void cancelSessionLocked() {
        if (Helper.sVerbose) {
            Log.v(TAG, "cancelSessionLocked(): " + getStateAsStringLocked());
        }
        if (isActiveLocked()) {
            try {
                this.mService.cancelSession(this.mSessionId, this.mContext.getUserId());
                resetSessionLocked(true);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @GuardedBy("mLock")
    private void resetSessionLocked(boolean z) {
        this.mSessionId = Integer.MIN_VALUE;
        this.mState = 0;
        this.mTrackedViews = null;
        this.mFillableIds = null;
        this.mSaveTriggerId = null;
        this.mIdShownFillUi = null;
        if (z) {
            this.mEnteredIds = null;
        }
    }

    @GuardedBy("mLock")
    private void updateSessionLocked(AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i, int i2) {
        AutofillId autofillId2;
        Rect rect2;
        AutofillValue autofillValue2;
        int i3;
        if (Helper.sVerbose) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateSessionLocked(): id=");
            autofillId2 = autofillId;
            sb.append(autofillId2);
            sb.append(", bounds=");
            rect2 = rect;
            sb.append(rect2);
            sb.append(", value=");
            autofillValue2 = autofillValue;
            sb.append(autofillValue2);
            sb.append(", action=");
            i3 = i;
            sb.append(i3);
            sb.append(", flags=");
            sb.append(i2);
            Log.v(TAG, sb.toString());
        } else {
            autofillId2 = autofillId;
            rect2 = rect;
            autofillValue2 = autofillValue;
            i3 = i;
        }
        try {
            if ((i2 & 1) != 0) {
                AutofillClient client = getClient();
                if (client == null) {
                    return;
                }
                int iUpdateOrRestartSession = this.mService.updateOrRestartSession(client.autofillClientGetActivityToken(), this.mServiceClient.asBinder(), autofillId2, rect2, autofillValue2, this.mContext.getUserId(), this.mCallback != null, i2, client.autofillClientGetComponentName(), this.mSessionId, i3, isCompatibilityModeEnabledLocked());
                if (iUpdateOrRestartSession != this.mSessionId) {
                    if (Helper.sDebug) {
                        Log.d(TAG, "Session restarted: " + this.mSessionId + "=>" + iUpdateOrRestartSession);
                    }
                    this.mSessionId = iUpdateOrRestartSession;
                    this.mState = this.mSessionId == Integer.MIN_VALUE ? 0 : 1;
                    client.autofillClientResetableStateAvailable();
                }
                return;
            }
            this.mService.updateSession(this.mSessionId, autofillId2, rect2, autofillValue2, i3, i2, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @GuardedBy("mLock")
    private void ensureServiceClientAddedIfNeededLocked() {
        if (getClient() != null && this.mServiceClient == null) {
            this.mServiceClient = new AutofillManagerClient(this);
            try {
                final int userId = this.mContext.getUserId();
                int iAddClient = this.mService.addClient(this.mServiceClient, userId);
                this.mEnabled = (iAddClient & 1) != 0;
                Helper.sDebug = (iAddClient & 2) != 0;
                Helper.sVerbose = (iAddClient & 4) != 0;
                final IAutoFillManager iAutoFillManager = this.mService;
                final IAutoFillManagerClient iAutoFillManagerClient = this.mServiceClient;
                this.mServiceClientCleaner = Cleaner.create(this, new Runnable() {
                    @Override
                    public final void run() throws RemoteException {
                        iAutoFillManager.removeClient(iAutoFillManagerClient, userId);
                    }
                });
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void registerCallback(AutofillCallback autofillCallback) {
        boolean z;
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            try {
                if (autofillCallback == null) {
                    return;
                }
                if (this.mCallback == null) {
                    z = false;
                } else {
                    z = true;
                }
                this.mCallback = autofillCallback;
                if (!z) {
                    try {
                        this.mService.setHasCallback(this.mSessionId, this.mContext.getUserId(), true);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void unregisterCallback(AutofillCallback autofillCallback) {
        if (!hasAutofillFeature()) {
            return;
        }
        synchronized (this.mLock) {
            if (autofillCallback != null) {
                try {
                    if (this.mCallback != null && autofillCallback == this.mCallback) {
                        this.mCallback = null;
                        try {
                            this.mService.setHasCallback(this.mSessionId, this.mContext.getUserId(), false);
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                } finally {
                }
            }
        }
    }

    private void requestShowFillUi(int i, AutofillId autofillId, int i2, int i3, Rect rect, IAutofillWindowPresenter iAutofillWindowPresenter) {
        AutofillClient client;
        View viewFindView = findView(autofillId);
        if (viewFindView == null) {
            return;
        }
        AutofillCallback autofillCallback = null;
        synchronized (this.mLock) {
            if (this.mSessionId == i && (client = getClient()) != null && client.autofillClientRequestShowFillUi(viewFindView, i2, i3, rect, iAutofillWindowPresenter)) {
                autofillCallback = this.mCallback;
                this.mIdShownFillUi = autofillId;
            }
        }
        if (autofillCallback != null) {
            if (autofillId.isVirtual()) {
                autofillCallback.onAutofillEvent(viewFindView, autofillId.getVirtualChildId(), 1);
            } else {
                autofillCallback.onAutofillEvent(viewFindView, 1);
            }
        }
    }

    private void authenticate(int i, int i2, IntentSender intentSender, Intent intent) {
        AutofillClient client;
        synchronized (this.mLock) {
            if (i == this.mSessionId && (client = getClient()) != null) {
                this.mOnInvisibleCalled = false;
                client.autofillClientAuthenticate(i2, intentSender, intent);
            }
        }
    }

    private void dispatchUnhandledKey(int i, AutofillId autofillId, KeyEvent keyEvent) {
        AutofillClient client;
        View viewFindView = findView(autofillId);
        if (viewFindView == null) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mSessionId == i && (client = getClient()) != null) {
                client.autofillClientDispatchUnhandledKey(viewFindView, keyEvent);
            }
        }
    }

    private void setState(int i) {
        if (Helper.sVerbose) {
            Log.v(TAG, "setState(" + i + ")");
        }
        synchronized (this.mLock) {
            this.mEnabled = (i & 1) != 0;
            if (!this.mEnabled || (i & 2) != 0) {
                resetSessionLocked(true);
            }
            if ((i & 4) != 0) {
                this.mServiceClient = null;
                if (this.mServiceClientCleaner != null) {
                    this.mServiceClientCleaner.clean();
                    this.mServiceClientCleaner = null;
                }
            }
        }
        Helper.sDebug = (i & 8) != 0;
        Helper.sVerbose = (i & 16) != 0;
    }

    private void setAutofilledIfValuesIs(View view, AutofillValue autofillValue) {
        if (Objects.equals(view.getAutofillValue(), autofillValue)) {
            synchronized (this.mLock) {
                if (this.mLastAutofilledData == null) {
                    this.mLastAutofilledData = new ParcelableMap(1);
                }
                this.mLastAutofilledData.put(view.getAutofillId(), autofillValue);
            }
            view.setAutofilled(true);
        }
    }

    private void autofill(int i, List<AutofillId> list, List<AutofillValue> list2) {
        synchronized (this.mLock) {
            if (i != this.mSessionId) {
                return;
            }
            AutofillClient client = getClient();
            if (client == null) {
                return;
            }
            int size = list.size();
            View[] viewArrAutofillClientFindViewsByAutofillIdTraversal = client.autofillClientFindViewsByAutofillIdTraversal(Helper.toArray(list));
            ArrayList arrayList = null;
            ArrayMap arrayMap = null;
            int size2 = 0;
            for (int i2 = 0; i2 < size; i2++) {
                AutofillId autofillId = list.get(i2);
                AutofillValue autofillValue = list2.get(i2);
                autofillId.getViewId();
                View view = viewArrAutofillClientFindViewsByAutofillIdTraversal[i2];
                if (view == null) {
                    Log.d(TAG, "autofill(): no View with id " + autofillId);
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(autofillId);
                } else if (autofillId.isVirtual()) {
                    if (arrayMap == null) {
                        arrayMap = new ArrayMap(1);
                    }
                    SparseArray sparseArray = (SparseArray) arrayMap.get(view);
                    if (sparseArray == null) {
                        sparseArray = new SparseArray(5);
                        arrayMap.put(view, sparseArray);
                    }
                    sparseArray.put(autofillId.getVirtualChildId(), autofillValue);
                } else {
                    if (this.mLastAutofilledData == null) {
                        this.mLastAutofilledData = new ParcelableMap(size - i2);
                    }
                    this.mLastAutofilledData.put(autofillId, autofillValue);
                    view.autofill(autofillValue);
                    setAutofilledIfValuesIs(view, autofillValue);
                    size2++;
                }
            }
            if (arrayList != null) {
                if (Helper.sVerbose) {
                    Log.v(TAG, "autofill(): total failed views: " + arrayList);
                }
                try {
                    this.mService.setAutofillFailure(this.mSessionId, arrayList, this.mContext.getUserId());
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
            if (arrayMap != null) {
                for (int i3 = 0; i3 < arrayMap.size(); i3++) {
                    View view2 = (View) arrayMap.keyAt(i3);
                    SparseArray<AutofillValue> sparseArray2 = (SparseArray) arrayMap.valueAt(i3);
                    view2.autofill(sparseArray2);
                    size2 += sparseArray2.size();
                }
            }
            this.mMetricsLogger.write(newLog(MetricsProto.MetricsEvent.AUTOFILL_DATASET_APPLIED).addTaggedData(MetricsProto.MetricsEvent.FIELD_AUTOFILL_NUM_VALUES, Integer.valueOf(size)).addTaggedData(MetricsProto.MetricsEvent.FIELD_AUTOFILL_NUM_VIEWS_FILLED, Integer.valueOf(size2)));
        }
    }

    private LogMaker newLog(int i) {
        LogMaker logMakerAddTaggedData = new LogMaker(i).addTaggedData(MetricsProto.MetricsEvent.FIELD_AUTOFILL_SESSION_ID, Integer.valueOf(this.mSessionId));
        if (isCompatibilityModeEnabledLocked()) {
            logMakerAddTaggedData.addTaggedData(MetricsProto.MetricsEvent.FIELD_AUTOFILL_COMPAT_MODE, 1);
        }
        AutofillClient client = getClient();
        if (client == null) {
            logMakerAddTaggedData.setPackageName(this.mContext.getPackageName());
        } else {
            logMakerAddTaggedData.setComponentName(client.autofillClientGetComponentName());
        }
        return logMakerAddTaggedData;
    }

    private void setTrackedViews(int i, AutofillId[] autofillIdArr, boolean z, boolean z2, AutofillId[] autofillIdArr2, AutofillId autofillId) {
        synchronized (this.mLock) {
            if (this.mEnabled && this.mSessionId == i) {
                if (z) {
                    this.mTrackedViews = new TrackedViews(autofillIdArr);
                } else {
                    this.mTrackedViews = null;
                }
                this.mSaveOnFinish = z2;
                if (autofillIdArr2 != null) {
                    if (this.mFillableIds == null) {
                        this.mFillableIds = new ArraySet<>(autofillIdArr2.length);
                    }
                    for (AutofillId autofillId2 : autofillIdArr2) {
                        this.mFillableIds.add(autofillId2);
                    }
                    if (Helper.sVerbose) {
                        Log.v(TAG, "setTrackedViews(): fillableIds=" + autofillIdArr2 + ", mFillableIds" + this.mFillableIds);
                    }
                }
                if (this.mSaveTriggerId != null && !this.mSaveTriggerId.equals(autofillId)) {
                    setNotifyOnClickLocked(this.mSaveTriggerId, false);
                }
                if (autofillId != null && !autofillId.equals(this.mSaveTriggerId)) {
                    this.mSaveTriggerId = autofillId;
                    setNotifyOnClickLocked(this.mSaveTriggerId, true);
                }
            }
        }
    }

    private void setNotifyOnClickLocked(AutofillId autofillId, boolean z) {
        View viewFindView = findView(autofillId);
        if (viewFindView == null) {
            Log.w(TAG, "setNotifyOnClick(): invalid id: " + autofillId);
            return;
        }
        viewFindView.setNotifyAutofillManagerOnClick(z);
    }

    private void setSaveUiState(int i, boolean z) {
        if (Helper.sDebug) {
            Log.d(TAG, "setSaveUiState(" + i + "): " + z);
        }
        synchronized (this.mLock) {
            if (this.mSessionId != Integer.MIN_VALUE) {
                Log.w(TAG, "setSaveUiState(" + i + ", " + z + ") called on existing session " + this.mSessionId + "; cancelling it");
                cancelSessionLocked();
            }
            if (z) {
                this.mSessionId = i;
                this.mState = 3;
            } else {
                this.mSessionId = Integer.MIN_VALUE;
                this.mState = 0;
            }
        }
    }

    private void setSessionFinished(int i) {
        synchronized (this.mLock) {
            if (Helper.sVerbose) {
                Log.v(TAG, "setSessionFinished(): from " + getStateAsStringLocked() + " to " + getStateAsString(i));
            }
            if (i == 5) {
                resetSessionLocked(true);
                this.mState = 0;
            } else {
                resetSessionLocked(false);
                this.mState = i;
            }
        }
    }

    public void requestHideFillUi() {
        requestHideFillUi(this.mIdShownFillUi, true);
    }

    private void requestHideFillUi(AutofillId autofillId, boolean z) {
        AutofillClient client;
        View viewFindView = autofillId == null ? null : findView(autofillId);
        if (Helper.sVerbose) {
            Log.v(TAG, "requestHideFillUi(" + autofillId + "): anchor = " + viewFindView);
        }
        if (viewFindView == null) {
            if (z && (client = getClient()) != null) {
                client.autofillClientRequestHideFillUi();
                return;
            }
            return;
        }
        requestHideFillUi(autofillId, viewFindView);
    }

    private void requestHideFillUi(AutofillId autofillId, View view) {
        AutofillCallback autofillCallback;
        synchronized (this.mLock) {
            AutofillClient client = getClient();
            autofillCallback = null;
            if (client != null && client.autofillClientRequestHideFillUi()) {
                this.mIdShownFillUi = null;
                autofillCallback = this.mCallback;
            }
        }
        if (autofillCallback != null) {
            if (autofillId.isVirtual()) {
                autofillCallback.onAutofillEvent(view, autofillId.getVirtualChildId(), 2);
            } else {
                autofillCallback.onAutofillEvent(view, 2);
            }
        }
    }

    private void notifyNoFillUi(int i, AutofillId autofillId, int i2) {
        if (Helper.sVerbose) {
            Log.v(TAG, "notifyNoFillUi(): sessionId=" + i + ", autofillId=" + autofillId + ", sessionFinishedState=" + i2);
        }
        View viewFindView = findView(autofillId);
        if (viewFindView == null) {
            return;
        }
        AutofillCallback autofillCallback = null;
        synchronized (this.mLock) {
            if (this.mSessionId == i && getClient() != null) {
                autofillCallback = this.mCallback;
            }
        }
        if (autofillCallback != null) {
            if (autofillId.isVirtual()) {
                autofillCallback.onAutofillEvent(viewFindView, autofillId.getVirtualChildId(), 3);
            } else {
                autofillCallback.onAutofillEvent(viewFindView, 3);
            }
        }
        if (i2 != 0) {
            setSessionFinished(i2);
        }
    }

    private View findView(AutofillId autofillId) {
        AutofillClient client = getClient();
        if (client != null) {
            return client.autofillClientFindViewByAutofillIdTraversal(autofillId);
        }
        return null;
    }

    public boolean hasAutofillFeature() {
        return this.mService != null;
    }

    public void onPendingSaveUi(int i, IBinder iBinder) {
        if (Helper.sVerbose) {
            Log.v(TAG, "onPendingSaveUi(" + i + "): " + iBinder);
        }
        synchronized (this.mLock) {
            try {
                this.mService.onPendingSaveUi(i, iBinder);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.println("AutofillManager:");
        String str2 = str + "  ";
        printWriter.print(str2);
        printWriter.print("sessionId: ");
        printWriter.println(this.mSessionId);
        printWriter.print(str2);
        printWriter.print("state: ");
        printWriter.println(getStateAsStringLocked());
        printWriter.print(str2);
        printWriter.print("context: ");
        printWriter.println(this.mContext);
        printWriter.print(str2);
        printWriter.print("client: ");
        printWriter.println(getClient());
        printWriter.print(str2);
        printWriter.print("enabled: ");
        printWriter.println(this.mEnabled);
        printWriter.print(str2);
        printWriter.print("hasService: ");
        printWriter.println(this.mService != null);
        printWriter.print(str2);
        printWriter.print("hasCallback: ");
        printWriter.println(this.mCallback != null);
        printWriter.print(str2);
        printWriter.print("onInvisibleCalled ");
        printWriter.println(this.mOnInvisibleCalled);
        printWriter.print(str2);
        printWriter.print("last autofilled data: ");
        printWriter.println(this.mLastAutofilledData);
        printWriter.print(str2);
        printWriter.print("tracked views: ");
        if (this.mTrackedViews == null) {
            printWriter.println("null");
        } else {
            String str3 = str2 + "  ";
            printWriter.println();
            printWriter.print(str3);
            printWriter.print("visible:");
            printWriter.println(this.mTrackedViews.mVisibleTrackedIds);
            printWriter.print(str3);
            printWriter.print("invisible:");
            printWriter.println(this.mTrackedViews.mInvisibleTrackedIds);
        }
        printWriter.print(str2);
        printWriter.print("fillable ids: ");
        printWriter.println(this.mFillableIds);
        printWriter.print(str2);
        printWriter.print("entered ids: ");
        printWriter.println(this.mEnteredIds);
        printWriter.print(str2);
        printWriter.print("save trigger id: ");
        printWriter.println(this.mSaveTriggerId);
        printWriter.print(str2);
        printWriter.print("save on finish(): ");
        printWriter.println(this.mSaveOnFinish);
        printWriter.print(str2);
        printWriter.print("compat mode enabled: ");
        printWriter.println(isCompatibilityModeEnabledLocked());
        printWriter.print(str2);
        printWriter.print("debug: ");
        printWriter.print(Helper.sDebug);
        printWriter.print(" verbose: ");
        printWriter.println(Helper.sVerbose);
    }

    @GuardedBy("mLock")
    private String getStateAsStringLocked() {
        return getStateAsString(this.mState);
    }

    private static String getStateAsString(int i) {
        switch (i) {
            case 0:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            case 1:
                return "ACTIVE";
            case 2:
                return "FINISHED";
            case 3:
                return "SHOWING_SAVE_UI";
            case 4:
                return "DISABLED_BY_SERVICE";
            case 5:
                return "UNKNOWN_COMPAT_MODE";
            default:
                return "INVALID:" + i;
        }
    }

    @GuardedBy("mLock")
    private boolean isActiveLocked() {
        return this.mState == 1;
    }

    @GuardedBy("mLock")
    private boolean isDisabledByServiceLocked() {
        return this.mState == 4;
    }

    @GuardedBy("mLock")
    private boolean isFinishedLocked() {
        return this.mState == 2;
    }

    private void post(Runnable runnable) {
        AutofillClient client = getClient();
        if (client == null) {
            if (Helper.sVerbose) {
                Log.v(TAG, "ignoring post() because client is null");
                return;
            }
            return;
        }
        client.autofillClientRunOnUiThread(runnable);
    }

    private final class CompatibilityBridge implements AccessibilityManager.AccessibilityPolicy {

        @GuardedBy("mLock")
        AccessibilityServiceInfo mCompatServiceInfo;

        @GuardedBy("mLock")
        private final Rect mFocusedBounds = new Rect();

        @GuardedBy("mLock")
        private final Rect mTempBounds = new Rect();

        @GuardedBy("mLock")
        private int mFocusedWindowId = -1;

        @GuardedBy("mLock")
        private long mFocusedNodeId = AccessibilityNodeInfo.UNDEFINED_NODE_ID;

        CompatibilityBridge() {
            AccessibilityManager.getInstance(AutofillManager.this.mContext).setAccessibilityPolicy(this);
        }

        private AccessibilityServiceInfo getCompatServiceInfo() {
            synchronized (AutofillManager.this.mLock) {
                if (this.mCompatServiceInfo != null) {
                    return this.mCompatServiceInfo;
                }
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(ZenModeConfig.SYSTEM_AUTHORITY, "com.android.server.autofill.AutofillCompatAccessibilityService"));
                try {
                    this.mCompatServiceInfo = new AccessibilityServiceInfo(AutofillManager.this.mContext.getPackageManager().resolveService(intent, 1048704), AutofillManager.this.mContext);
                    return this.mCompatServiceInfo;
                } catch (IOException | XmlPullParserException e) {
                    Log.e(AutofillManager.TAG, "Cannot find compat autofill service:" + intent);
                    throw new IllegalStateException("Cannot find compat autofill service");
                }
            }
        }

        @Override
        public boolean isEnabled(boolean z) {
            return true;
        }

        @Override
        public int getRelevantEventTypes(int i) {
            return i | 8 | 16 | 1 | 2048;
        }

        @Override
        public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(List<AccessibilityServiceInfo> list) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(getCompatServiceInfo());
            return list;
        }

        @Override
        public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int i, List<AccessibilityServiceInfo> list) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(getCompatServiceInfo());
            return list;
        }

        @Override
        public AccessibilityEvent onAccessibilityEvent(AccessibilityEvent accessibilityEvent, boolean z, int i) {
            AutofillClient client;
            int eventType = accessibilityEvent.getEventType();
            if (eventType == 1) {
                synchronized (AutofillManager.this.mLock) {
                    notifyViewClicked(accessibilityEvent.getWindowId(), accessibilityEvent.getSourceNodeId());
                }
            } else if (eventType == 8) {
                synchronized (AutofillManager.this.mLock) {
                    if (this.mFocusedWindowId == accessibilityEvent.getWindowId() && this.mFocusedNodeId == accessibilityEvent.getSourceNodeId()) {
                        return accessibilityEvent;
                    }
                    if (this.mFocusedWindowId != -1 && this.mFocusedNodeId != AccessibilityNodeInfo.UNDEFINED_NODE_ID) {
                        notifyViewExited(this.mFocusedWindowId, this.mFocusedNodeId);
                        this.mFocusedWindowId = -1;
                        this.mFocusedNodeId = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
                        this.mFocusedBounds.set(0, 0, 0, 0);
                    }
                    int windowId = accessibilityEvent.getWindowId();
                    long sourceNodeId = accessibilityEvent.getSourceNodeId();
                    if (notifyViewEntered(windowId, sourceNodeId, this.mFocusedBounds)) {
                        this.mFocusedWindowId = windowId;
                        this.mFocusedNodeId = sourceNodeId;
                    }
                }
            } else if (eventType == 16) {
                synchronized (AutofillManager.this.mLock) {
                    if (this.mFocusedWindowId == accessibilityEvent.getWindowId() && this.mFocusedNodeId == accessibilityEvent.getSourceNodeId()) {
                        notifyValueChanged(accessibilityEvent.getWindowId(), accessibilityEvent.getSourceNodeId());
                    }
                }
            } else if (eventType == 2048 && (client = AutofillManager.this.getClient()) != null) {
                synchronized (AutofillManager.this.mLock) {
                    if (client.autofillClientIsFillUiShowing()) {
                        notifyViewEntered(this.mFocusedWindowId, this.mFocusedNodeId, this.mFocusedBounds);
                    }
                    updateTrackedViewsLocked();
                }
            }
            if (z) {
                return accessibilityEvent;
            }
            return null;
        }

        private boolean notifyViewEntered(int i, long j, Rect rect) {
            View viewFindViewByAccessibilityId;
            AccessibilityNodeInfo accessibilityNodeInfoFindVirtualNodeByAccessibilityId;
            int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(j);
            if (!isVirtualNode(virtualDescendantId) || (viewFindViewByAccessibilityId = findViewByAccessibilityId(i, j)) == null || (accessibilityNodeInfoFindVirtualNodeByAccessibilityId = findVirtualNodeByAccessibilityId(viewFindViewByAccessibilityId, virtualDescendantId)) == null || !accessibilityNodeInfoFindVirtualNodeByAccessibilityId.isEditable()) {
                return false;
            }
            Rect rect2 = this.mTempBounds;
            accessibilityNodeInfoFindVirtualNodeByAccessibilityId.getBoundsInScreen(rect2);
            if (rect2.equals(rect)) {
                return false;
            }
            rect.set(rect2);
            AutofillManager.this.notifyViewEntered(viewFindViewByAccessibilityId, virtualDescendantId, rect2);
            return true;
        }

        private void notifyViewExited(int i, long j) {
            View viewFindViewByAccessibilityId;
            int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(j);
            if (!isVirtualNode(virtualDescendantId) || (viewFindViewByAccessibilityId = findViewByAccessibilityId(i, j)) == null) {
                return;
            }
            AutofillManager.this.notifyViewExited(viewFindViewByAccessibilityId, virtualDescendantId);
        }

        private void notifyValueChanged(int i, long j) {
            View viewFindViewByAccessibilityId;
            AccessibilityNodeInfo accessibilityNodeInfoFindVirtualNodeByAccessibilityId;
            int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(j);
            if (!isVirtualNode(virtualDescendantId) || (viewFindViewByAccessibilityId = findViewByAccessibilityId(i, j)) == null || (accessibilityNodeInfoFindVirtualNodeByAccessibilityId = findVirtualNodeByAccessibilityId(viewFindViewByAccessibilityId, virtualDescendantId)) == null) {
                return;
            }
            AutofillManager.this.notifyValueChanged(viewFindViewByAccessibilityId, virtualDescendantId, AutofillValue.forText(accessibilityNodeInfoFindVirtualNodeByAccessibilityId.getText()));
        }

        private void notifyViewClicked(int i, long j) {
            View viewFindViewByAccessibilityId;
            int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(j);
            if (!isVirtualNode(virtualDescendantId) || (viewFindViewByAccessibilityId = findViewByAccessibilityId(i, j)) == null || findVirtualNodeByAccessibilityId(viewFindViewByAccessibilityId, virtualDescendantId) == null) {
                return;
            }
            AutofillManager.this.notifyViewClicked(viewFindViewByAccessibilityId, virtualDescendantId);
        }

        @GuardedBy("mLock")
        private void updateTrackedViewsLocked() {
            if (AutofillManager.this.mTrackedViews != null) {
                AutofillManager.this.mTrackedViews.onVisibleForAutofillChangedLocked();
            }
        }

        private View findViewByAccessibilityId(int i, long j) {
            AutofillClient client = AutofillManager.this.getClient();
            if (client == null) {
                return null;
            }
            return client.autofillClientFindViewByAccessibilityIdTraversal(AccessibilityNodeInfo.getAccessibilityViewId(j), i);
        }

        private AccessibilityNodeInfo findVirtualNodeByAccessibilityId(View view, int i) {
            AccessibilityNodeProvider accessibilityNodeProvider = view.getAccessibilityNodeProvider();
            if (accessibilityNodeProvider == null) {
                return null;
            }
            return accessibilityNodeProvider.createAccessibilityNodeInfo(i);
        }

        private boolean isVirtualNode(int i) {
            return (i == -1 || i == Integer.MAX_VALUE) ? false : true;
        }
    }

    private class TrackedViews {
        private ArraySet<AutofillId> mInvisibleTrackedIds;
        private ArraySet<AutofillId> mVisibleTrackedIds;

        private <T> boolean isInSet(ArraySet<T> arraySet, T t) {
            return arraySet != null && arraySet.contains(t);
        }

        private <T> ArraySet<T> addToSet(ArraySet<T> arraySet, T t) {
            if (arraySet == null) {
                arraySet = new ArraySet<>(1);
            }
            arraySet.add(t);
            return arraySet;
        }

        private <T> ArraySet<T> removeFromSet(ArraySet<T> arraySet, T t) {
            if (arraySet == null) {
                return null;
            }
            arraySet.remove(t);
            if (arraySet.isEmpty()) {
                return null;
            }
            return arraySet;
        }

        TrackedViews(AutofillId[] autofillIdArr) {
            boolean[] zArrAutofillClientGetViewVisibility;
            AutofillClient client = AutofillManager.this.getClient();
            if (!ArrayUtils.isEmpty(autofillIdArr) && client != null) {
                if (client.autofillClientIsVisibleForAutofill()) {
                    if (Helper.sVerbose) {
                        Log.v(AutofillManager.TAG, "client is visible, check tracked ids");
                    }
                    zArrAutofillClientGetViewVisibility = client.autofillClientGetViewVisibility(autofillIdArr);
                } else {
                    zArrAutofillClientGetViewVisibility = new boolean[autofillIdArr.length];
                }
                int length = autofillIdArr.length;
                for (int i = 0; i < length; i++) {
                    AutofillId autofillId = autofillIdArr[i];
                    if (zArrAutofillClientGetViewVisibility[i]) {
                        this.mVisibleTrackedIds = addToSet(this.mVisibleTrackedIds, autofillId);
                    } else {
                        this.mInvisibleTrackedIds = addToSet(this.mInvisibleTrackedIds, autofillId);
                    }
                }
            }
            if (Helper.sVerbose) {
                Log.v(AutofillManager.TAG, "TrackedViews(trackedIds=" + Arrays.toString(autofillIdArr) + "):  mVisibleTrackedIds=" + this.mVisibleTrackedIds + " mInvisibleTrackedIds=" + this.mInvisibleTrackedIds);
            }
            if (this.mVisibleTrackedIds == null) {
                AutofillManager.this.finishSessionLocked();
            }
        }

        @GuardedBy("mLock")
        void notifyViewVisibilityChangedLocked(AutofillId autofillId, boolean z) {
            if (Helper.sDebug) {
                Log.d(AutofillManager.TAG, "notifyViewVisibilityChangedLocked(): id=" + autofillId + " isVisible=" + z);
            }
            if (AutofillManager.this.isClientVisibleForAutofillLocked()) {
                if (z) {
                    if (isInSet(this.mInvisibleTrackedIds, autofillId)) {
                        this.mInvisibleTrackedIds = removeFromSet(this.mInvisibleTrackedIds, autofillId);
                        this.mVisibleTrackedIds = addToSet(this.mVisibleTrackedIds, autofillId);
                    }
                } else if (isInSet(this.mVisibleTrackedIds, autofillId)) {
                    this.mVisibleTrackedIds = removeFromSet(this.mVisibleTrackedIds, autofillId);
                    this.mInvisibleTrackedIds = addToSet(this.mInvisibleTrackedIds, autofillId);
                }
            }
            if (this.mVisibleTrackedIds == null) {
                if (Helper.sVerbose) {
                    Log.v(AutofillManager.TAG, "No more visible ids. Invisibile = " + this.mInvisibleTrackedIds);
                }
                AutofillManager.this.finishSessionLocked();
            }
        }

        @GuardedBy("mLock")
        void onVisibleForAutofillChangedLocked() {
            ArraySet<AutofillId> arraySetAddToSet;
            AutofillClient client = AutofillManager.this.getClient();
            if (client != null) {
                if (Helper.sVerbose) {
                    Log.v(AutofillManager.TAG, "onVisibleForAutofillChangedLocked(): inv= " + this.mInvisibleTrackedIds + " vis=" + this.mVisibleTrackedIds);
                }
                ArraySet<AutofillId> arraySetAddToSet2 = null;
                if (this.mInvisibleTrackedIds != null) {
                    ArrayList arrayList = new ArrayList(this.mInvisibleTrackedIds);
                    boolean[] zArrAutofillClientGetViewVisibility = client.autofillClientGetViewVisibility(Helper.toArray(arrayList));
                    int size = arrayList.size();
                    ArraySet<AutofillId> arraySetAddToSet3 = null;
                    arraySetAddToSet = null;
                    for (int i = 0; i < size; i++) {
                        AutofillId autofillId = (AutofillId) arrayList.get(i);
                        if (zArrAutofillClientGetViewVisibility[i]) {
                            arraySetAddToSet3 = addToSet(arraySetAddToSet3, autofillId);
                            if (Helper.sDebug) {
                                Log.d(AutofillManager.TAG, "onVisibleForAutofill() " + autofillId + " became visible");
                            }
                        } else {
                            arraySetAddToSet = addToSet(arraySetAddToSet, autofillId);
                        }
                    }
                    arraySetAddToSet2 = arraySetAddToSet3;
                } else {
                    arraySetAddToSet = null;
                }
                if (this.mVisibleTrackedIds != null) {
                    ArrayList arrayList2 = new ArrayList(this.mVisibleTrackedIds);
                    boolean[] zArrAutofillClientGetViewVisibility2 = client.autofillClientGetViewVisibility(Helper.toArray(arrayList2));
                    int size2 = arrayList2.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        AutofillId autofillId2 = (AutofillId) arrayList2.get(i2);
                        if (zArrAutofillClientGetViewVisibility2[i2]) {
                            arraySetAddToSet2 = addToSet(arraySetAddToSet2, autofillId2);
                        } else {
                            arraySetAddToSet = addToSet(arraySetAddToSet, autofillId2);
                            if (Helper.sDebug) {
                                Log.d(AutofillManager.TAG, "onVisibleForAutofill() " + autofillId2 + " became invisible");
                            }
                        }
                    }
                }
                this.mInvisibleTrackedIds = arraySetAddToSet;
                this.mVisibleTrackedIds = arraySetAddToSet2;
            }
            if (this.mVisibleTrackedIds == null) {
                if (Helper.sVerbose) {
                    Log.v(AutofillManager.TAG, "onVisibleForAutofillChangedLocked(): no more visible ids");
                }
                AutofillManager.this.finishSessionLocked();
            }
        }
    }

    public static abstract class AutofillCallback {
        public static final int EVENT_INPUT_HIDDEN = 2;
        public static final int EVENT_INPUT_SHOWN = 1;
        public static final int EVENT_INPUT_UNAVAILABLE = 3;

        @Retention(RetentionPolicy.SOURCE)
        public @interface AutofillEventType {
        }

        public void onAutofillEvent(View view, int i) {
        }

        public void onAutofillEvent(View view, int i, int i2) {
        }
    }

    private static final class AutofillManagerClient extends IAutoFillManagerClient.Stub {
        private final WeakReference<AutofillManager> mAfm;

        AutofillManagerClient(AutofillManager autofillManager) {
            this.mAfm = new WeakReference<>(autofillManager);
        }

        @Override
        public void setState(final int i) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.setState(i);
                    }
                });
            }
        }

        @Override
        public void autofill(final int i, final List<AutofillId> list, final List<AutofillValue> list2) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.autofill(i, list, list2);
                    }
                });
            }
        }

        @Override
        public void authenticate(final int i, final int i2, final IntentSender intentSender, final Intent intent) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.authenticate(i, i2, intentSender, intent);
                    }
                });
            }
        }

        @Override
        public void requestShowFillUi(final int i, final AutofillId autofillId, final int i2, final int i3, final Rect rect, final IAutofillWindowPresenter iAutofillWindowPresenter) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.requestShowFillUi(i, autofillId, i2, i3, rect, iAutofillWindowPresenter);
                    }
                });
            }
        }

        @Override
        public void requestHideFillUi(int i, final AutofillId autofillId) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.requestHideFillUi(autofillId, false);
                    }
                });
            }
        }

        @Override
        public void notifyNoFillUi(final int i, final AutofillId autofillId, final int i2) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.notifyNoFillUi(i, autofillId, i2);
                    }
                });
            }
        }

        @Override
        public void dispatchUnhandledKey(final int i, final AutofillId autofillId, final KeyEvent keyEvent) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.dispatchUnhandledKey(i, autofillId, keyEvent);
                    }
                });
            }
        }

        @Override
        public void startIntentSender(final IntentSender intentSender, final Intent intent) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        AutofillManager.AutofillManagerClient.lambda$startIntentSender$7(autofillManager, intentSender, intent);
                    }
                });
            }
        }

        static void lambda$startIntentSender$7(AutofillManager autofillManager, IntentSender intentSender, Intent intent) {
            try {
                autofillManager.mContext.startIntentSender(intentSender, intent, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(AutofillManager.TAG, "startIntentSender() failed for intent:" + intentSender, e);
            }
        }

        @Override
        public void setTrackedViews(final int i, final AutofillId[] autofillIdArr, final boolean z, final boolean z2, final AutofillId[] autofillIdArr2, final AutofillId autofillId) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.setTrackedViews(i, autofillIdArr, z, z2, autofillIdArr2, autofillId);
                    }
                });
            }
        }

        @Override
        public void setSaveUiState(final int i, final boolean z) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.setSaveUiState(i, z);
                    }
                });
            }
        }

        @Override
        public void setSessionFinished(final int i) {
            final AutofillManager autofillManager = this.mAfm.get();
            if (autofillManager != null) {
                autofillManager.post(new Runnable() {
                    @Override
                    public final void run() {
                        autofillManager.setSessionFinished(i);
                    }
                });
            }
        }
    }
}
