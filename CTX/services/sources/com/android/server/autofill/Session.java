package com.android.server.autofill;

import android.R;
import android.app.ActivityManager;
import android.app.IAssistDataReceiver;
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.metrics.LogMaker;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.autofill.AutofillFieldClassificationService;
import android.service.autofill.Dataset;
import android.service.autofill.FieldClassification;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.InternalSanitizer;
import android.service.autofill.InternalValidator;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.service.autofill.UserData;
import android.service.autofill.ValueFinder;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.KeyEvent;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import android.view.autofill.IAutofillWindowPresenter;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.autofill.RemoteFillService;
import com.android.server.autofill.ViewState;
import com.android.server.autofill.ui.AutoFillUI;
import com.android.server.autofill.ui.PendingUi;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class Session implements RemoteFillService.FillServiceCallbacks, ViewState.Listener, AutoFillUI.AutoFillUiCallback, ValueFinder {
    private static final String EXTRA_REQUEST_ID = "android.service.autofill.extra.REQUEST_ID";
    private static final String TAG = "AutofillSession";
    private static AtomicInteger sIdCounter = new AtomicInteger();
    public final int id;

    @GuardedBy("mLock")
    private IBinder mActivityToken;

    @GuardedBy("mLock")
    private IAutoFillManagerClient mClient;

    @GuardedBy("mLock")
    private Bundle mClientState;

    @GuardedBy("mLock")
    private IBinder.DeathRecipient mClientVulture;
    private final boolean mCompatMode;
    private final ComponentName mComponentName;

    @GuardedBy("mLock")
    private ArrayList<FillContext> mContexts;

    @GuardedBy("mLock")
    private AutofillId mCurrentViewId;

    @GuardedBy("mLock")
    private boolean mDestroyed;
    public final int mFlags;
    private final Handler mHandler;
    private boolean mHasCallback;

    @GuardedBy("mLock")
    private boolean mIsSaving;
    private final Object mLock;

    @GuardedBy("mLock")
    private PendingUi mPendingSaveUi;
    private final RemoteFillService mRemoteFillService;

    @GuardedBy("mLock")
    private SparseArray<FillResponse> mResponses;

    @GuardedBy("mLock")
    private boolean mSaveOnAllViewsInvisible;

    @GuardedBy("mLock")
    private ArrayList<String> mSelectedDatasetIds;
    private final AutofillManagerServiceImpl mService;
    private final AutoFillUI mUi;

    @GuardedBy("mLock")
    private final LocalLog mUiLatencyHistory;

    @GuardedBy("mLock")
    private long mUiShownTime;

    @GuardedBy("mLock")
    private AssistStructure.ViewNode mUrlBar;

    @GuardedBy("mLock")
    private final LocalLog mWtfHistory;
    public final int uid;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();

    @GuardedBy("mLock")
    private final ArrayMap<AutofillId, ViewState> mViewStates = new ArrayMap<>();

    @GuardedBy("mLock")
    private final SparseArray<LogMaker> mRequestLogs = new SparseArray<>(1);
    private final IAssistDataReceiver mAssistReceiver = new IAssistDataReceiver.Stub() {
        public void onHandleAssistData(Bundle bundle) throws RemoteException {
            FillRequest fillRequest;
            AssistStructure assistStructure = (AssistStructure) bundle.getParcelable("structure");
            if (assistStructure == null) {
                Slog.e(Session.TAG, "No assist structure - app might have crashed providing it");
                return;
            }
            Bundle bundle2 = bundle.getBundle("receiverExtras");
            if (bundle2 == null) {
                Slog.e(Session.TAG, "No receiver extras - app might have crashed providing it");
                return;
            }
            int i = bundle2.getInt(Session.EXTRA_REQUEST_ID);
            if (Helper.sVerbose) {
                Slog.v(Session.TAG, "New structure for requestId " + i + ": " + assistStructure);
            }
            synchronized (Session.this.mLock) {
                try {
                    assistStructure.ensureDataForAutofill();
                    ComponentName activityComponent = assistStructure.getActivityComponent();
                    if (activityComponent == null || !Session.this.mComponentName.getPackageName().equals(activityComponent.getPackageName())) {
                        Slog.w(Session.TAG, "Activity " + Session.this.mComponentName + " forged different component on AssistStructure: " + activityComponent);
                        assistStructure.setActivityComponent(Session.this.mComponentName);
                        Session.this.mMetricsLogger.write(Session.this.newLogMaker(948).addTaggedData(949, activityComponent == null ? "null" : activityComponent.flattenToShortString()));
                    }
                    if (Session.this.mCompatMode) {
                        String[] urlBarResourceIdsForCompatMode = Session.this.mService.getUrlBarResourceIdsForCompatMode(Session.this.mComponentName.getPackageName());
                        if (Helper.sDebug) {
                            Slog.d(Session.TAG, "url_bars in compat mode: " + Arrays.toString(urlBarResourceIdsForCompatMode));
                        }
                        if (urlBarResourceIdsForCompatMode != null) {
                            Session.this.mUrlBar = Helper.sanitizeUrlBar(assistStructure, urlBarResourceIdsForCompatMode);
                            if (Session.this.mUrlBar != null) {
                                AutofillId autofillId = Session.this.mUrlBar.getAutofillId();
                                if (Helper.sDebug) {
                                    Slog.d(Session.TAG, "Setting urlBar as id=" + autofillId + " and domain " + Session.this.mUrlBar.getWebDomain());
                                }
                                Session.this.mViewStates.put(autofillId, new ViewState(Session.this, autofillId, Session.this, 512));
                            }
                        }
                    }
                    assistStructure.sanitizeForParceling(true);
                    int flags = assistStructure.getFlags();
                    if (Session.this.mContexts == null) {
                        Session.this.mContexts = new ArrayList(1);
                    }
                    Session.this.mContexts.add(new FillContext(i, assistStructure));
                    Session.this.cancelCurrentRequestLocked();
                    int size = Session.this.mContexts.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        Session.this.fillContextWithAllowedValuesLocked((FillContext) Session.this.mContexts.get(i2), flags);
                    }
                    fillRequest = new FillRequest(i, new ArrayList(Session.this.mContexts), Session.this.mClientState, flags);
                } catch (RuntimeException e) {
                    Session.this.wtf(e, "Exception lazy loading assist structure for %s: %s", assistStructure.getActivityComponent(), e);
                    return;
                }
            }
            Session.this.mRemoteFillService.onFillRequest(fillRequest);
        }

        public void onHandleAssistScreenshot(Bitmap bitmap) {
        }
    };
    private final long mStartTime = SystemClock.elapsedRealtime();

    @GuardedBy("mLock")
    private AutofillId[] getIdsOfAllViewStatesLocked() {
        int size = this.mViewStates.size();
        AutofillId[] autofillIdArr = new AutofillId[size];
        for (int i = 0; i < size; i++) {
            autofillIdArr[i] = this.mViewStates.valueAt(i).id;
        }
        return autofillIdArr;
    }

    public String findByAutofillId(AutofillId autofillId) {
        synchronized (this.mLock) {
            AutofillValue autofillValueFindValueLocked = findValueLocked(autofillId);
            if (autofillValueFindValueLocked != null) {
                if (autofillValueFindValueLocked.isText()) {
                    return autofillValueFindValueLocked.getTextValue().toString();
                }
                if (autofillValueFindValueLocked.isList()) {
                    CharSequence[] autofillOptionsFromContextsLocked = getAutofillOptionsFromContextsLocked(autofillId);
                    if (autofillOptionsFromContextsLocked != null) {
                        CharSequence charSequence = autofillOptionsFromContextsLocked[autofillValueFindValueLocked.getListValue()];
                        return charSequence != null ? charSequence.toString() : null;
                    }
                    Slog.w(TAG, "findByAutofillId(): no autofill options for id " + autofillId);
                }
            }
            return null;
        }
    }

    public AutofillValue findRawValueByAutofillId(AutofillId autofillId) {
        AutofillValue autofillValueFindValueLocked;
        synchronized (this.mLock) {
            autofillValueFindValueLocked = findValueLocked(autofillId);
        }
        return autofillValueFindValueLocked;
    }

    @GuardedBy("mLock")
    private AutofillValue findValueLocked(AutofillId autofillId) {
        ViewState viewState = this.mViewStates.get(autofillId);
        if (viewState == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "findValueLocked(): no view state for " + autofillId);
                return null;
            }
            return null;
        }
        AutofillValue currentValue = viewState.getCurrentValue();
        if (currentValue == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "findValueLocked(): no current value for " + autofillId);
            }
            return getValueFromContextsLocked(autofillId);
        }
        return currentValue;
    }

    @GuardedBy("mLock")
    private void fillContextWithAllowedValuesLocked(FillContext fillContext, int i) {
        AssistStructure.ViewNode[] viewNodeArrFindViewNodesByAutofillIds = fillContext.findViewNodesByAutofillIds(getIdsOfAllViewStatesLocked());
        int size = this.mViewStates.size();
        for (int i2 = 0; i2 < size; i2++) {
            ViewState viewStateValueAt = this.mViewStates.valueAt(i2);
            AssistStructure.ViewNode viewNode = viewNodeArrFindViewNodesByAutofillIds[i2];
            if (viewNode == null) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "fillContextWithAllowedValuesLocked(): no node for " + viewStateValueAt.id);
                }
            } else {
                AutofillValue currentValue = viewStateValueAt.getCurrentValue();
                AutofillValue autofilledValue = viewStateValueAt.getAutofilledValue();
                AssistStructure.AutofillOverlay autofillOverlay = new AssistStructure.AutofillOverlay();
                if (autofilledValue != null && autofilledValue.equals(currentValue)) {
                    autofillOverlay.value = currentValue;
                }
                if (this.mCurrentViewId != null) {
                    autofillOverlay.focused = this.mCurrentViewId.equals(viewStateValueAt.id);
                    if (autofillOverlay.focused && (i & 1) != 0) {
                        autofillOverlay.value = currentValue;
                    }
                }
                viewNode.setAutofillOverlay(autofillOverlay);
            }
        }
    }

    @GuardedBy("mLock")
    private void cancelCurrentRequestLocked() {
        int iCancelCurrentRequest = this.mRemoteFillService.cancelCurrentRequest();
        if (iCancelCurrentRequest != Integer.MIN_VALUE && this.mContexts != null) {
            for (int size = this.mContexts.size() - 1; size >= 0; size--) {
                if (this.mContexts.get(size).getRequestId() == iCancelCurrentRequest) {
                    if (Helper.sDebug) {
                        Slog.d(TAG, "cancelCurrentRequest(): id = " + iCancelCurrentRequest);
                    }
                    this.mContexts.remove(size);
                    return;
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void requestNewFillResponseLocked(int i) {
        int andIncrement;
        do {
            andIncrement = sIdCounter.getAndIncrement();
        } while (andIncrement == Integer.MIN_VALUE);
        int size = this.mRequestLogs.size() + 1;
        LogMaker logMakerAddTaggedData = newLogMaker(907).addTaggedData(1454, Integer.valueOf(size));
        if (i != 0) {
            logMakerAddTaggedData.addTaggedData(1452, Integer.valueOf(i));
        }
        this.mRequestLogs.put(andIncrement, logMakerAddTaggedData);
        if (Helper.sVerbose) {
            Slog.v(TAG, "Requesting structure for request #" + size + " ,requestId=" + andIncrement + ", flags=" + i);
        }
        cancelCurrentRequestLocked();
        try {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_REQUEST_ID, andIncrement);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!ActivityManager.getService().requestAutofillData(this.mAssistReceiver, bundle, this.mActivityToken, i)) {
                    Slog.w(TAG, "failed to request autofill data for " + this.mActivityToken);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (RemoteException e) {
        }
    }

    Session(AutofillManagerServiceImpl autofillManagerServiceImpl, AutoFillUI autoFillUI, Context context, Handler handler, int i, Object obj, int i2, int i3, IBinder iBinder, IBinder iBinder2, boolean z, LocalLog localLog, LocalLog localLog2, ComponentName componentName, ComponentName componentName2, boolean z2, boolean z3, int i4) {
        this.id = i2;
        this.mFlags = i4;
        this.uid = i3;
        this.mService = autofillManagerServiceImpl;
        this.mLock = obj;
        this.mUi = autoFillUI;
        this.mHandler = handler;
        this.mRemoteFillService = new RemoteFillService(context, componentName, i, this, z3);
        this.mActivityToken = iBinder;
        this.mHasCallback = z;
        this.mUiLatencyHistory = localLog;
        this.mWtfHistory = localLog2;
        this.mComponentName = componentName2;
        this.mCompatMode = z2;
        setClientLocked(iBinder2);
        this.mMetricsLogger.write(newLogMaker(906).addTaggedData(1452, Integer.valueOf(i4)));
    }

    @GuardedBy("mLock")
    IBinder getActivityTokenLocked() {
        return this.mActivityToken;
    }

    void switchActivity(IBinder iBinder, IBinder iBinder2) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#switchActivity() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mActivityToken = iBinder;
            setClientLocked(iBinder2);
            updateTrackedIdsLocked();
        }
    }

    @GuardedBy("mLock")
    private void setClientLocked(IBinder iBinder) {
        unlinkClientVultureLocked();
        this.mClient = IAutoFillManagerClient.Stub.asInterface(iBinder);
        this.mClientVulture = new IBinder.DeathRecipient() {
            @Override
            public final void binderDied() {
                Session.lambda$setClientLocked$0(this.f$0);
            }
        };
        try {
            this.mClient.asBinder().linkToDeath(this.mClientVulture, 0);
        } catch (RemoteException e) {
            Slog.w(TAG, "could not set binder death listener on autofill client: " + e);
        }
    }

    public static void lambda$setClientLocked$0(Session session) {
        Slog.d(TAG, "handling death of " + session.mActivityToken + " when saving=" + session.mIsSaving);
        synchronized (session.mLock) {
            if (session.mIsSaving) {
                session.mUi.hideFillUi(session);
            } else {
                session.mUi.destroyAll(session.mPendingSaveUi, session, false);
            }
        }
    }

    @GuardedBy("mLock")
    private void unlinkClientVultureLocked() {
        if (this.mClient != null && this.mClientVulture != null && !this.mClient.asBinder().unlinkToDeath(this.mClientVulture, 0)) {
            Slog.w(TAG, "unlinking vulture from death failed for " + this.mActivityToken);
        }
    }

    @Override
    public void onFillRequestSuccess(int i, FillResponse fillResponse, String str, int i2) {
        int i3;
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onFillRequestSuccess() rejected - session: " + this.id + " destroyed");
                return;
            }
            LogMaker logMaker = this.mRequestLogs.get(i);
            if (logMaker != null) {
                logMaker.setType(10);
            } else {
                Slog.w(TAG, "onFillRequestSuccess(): no request log for id " + i);
            }
            if (fillResponse == null) {
                if (logMaker != null) {
                    logMaker.addTaggedData(909, -1);
                }
                processNullResponseLocked(i2);
                return;
            }
            AutofillId[] fieldClassificationIds = fillResponse.getFieldClassificationIds();
            if (fieldClassificationIds != null && !this.mService.isFieldClassificationEnabledLocked()) {
                Slog.w(TAG, "Ignoring " + fillResponse + " because field detection is disabled");
                processNullResponseLocked(i2);
                return;
            }
            this.mService.setLastResponse(this.id, fillResponse);
            long disableDuration = fillResponse.getDisableDuration();
            if (disableDuration > 0) {
                int flags = fillResponse.getFlags();
                if (Helper.sDebug) {
                    StringBuilder sb = new StringBuilder("Service disabled autofill for ");
                    sb.append(this.mComponentName);
                    sb.append(": flags=");
                    sb.append(flags);
                    sb.append(", duration=");
                    TimeUtils.formatDuration(disableDuration, sb);
                    Slog.d(TAG, sb.toString());
                }
                if ((flags & 2) != 0) {
                    this.mService.disableAutofillForActivity(this.mComponentName, disableDuration, this.id, this.mCompatMode);
                } else {
                    this.mService.disableAutofillForApp(this.mComponentName.getPackageName(), disableDuration, this.id, this.mCompatMode);
                }
                i3 = 4;
            } else {
                i3 = 0;
            }
            if (((fillResponse.getDatasets() == null || fillResponse.getDatasets().isEmpty()) && fillResponse.getAuthentication() == null) || disableDuration > 0) {
                notifyUnavailableToClient(i3);
            }
            if (logMaker != null) {
                logMaker.addTaggedData(909, Integer.valueOf(fillResponse.getDatasets() != null ? fillResponse.getDatasets().size() : 0));
                if (fieldClassificationIds != null) {
                    logMaker.addTaggedData(1271, Integer.valueOf(fieldClassificationIds.length));
                }
            }
            synchronized (this.mLock) {
                processResponseLocked(fillResponse, null, i2);
            }
        }
    }

    @Override
    public void onFillRequestFailure(int i, CharSequence charSequence, String str) {
        onFillRequestFailureOrTimeout(i, false, charSequence, str);
    }

    @Override
    public void onFillRequestTimeout(int i, String str) {
        onFillRequestFailureOrTimeout(i, true, null, str);
    }

    private void onFillRequestFailureOrTimeout(int i, boolean z, CharSequence charSequence, String str) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onFillRequestFailureOrTimeout(req=" + i + ") rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mService.resetLastResponse();
            LogMaker logMaker = this.mRequestLogs.get(i);
            if (logMaker == null) {
                Slog.w(TAG, "onFillRequestFailureOrTimeout(): no log for id " + i);
            } else {
                logMaker.setType(z ? 2 : 11);
            }
            if (charSequence != null) {
                getUiForShowing().showError(charSequence, this);
            }
            removeSelf();
        }
    }

    @Override
    public void onSaveRequestSuccess(String str, IntentSender intentSender) {
        synchronized (this.mLock) {
            this.mIsSaving = false;
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onSaveRequestSuccess() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mMetricsLogger.write(newLogMaker(918, str).setType(intentSender == null ? 10 : 1));
            if (intentSender != null) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "Starting intent sender on save()");
                }
                startIntentSender(intentSender);
            }
            removeSelf();
        }
    }

    @Override
    public void onSaveRequestFailure(CharSequence charSequence, String str) {
        synchronized (this.mLock) {
            this.mIsSaving = false;
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onSaveRequestFailure() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mMetricsLogger.write(newLogMaker(918, str).setType(11));
            getUiForShowing().showError(charSequence, this);
            removeSelf();
        }
    }

    @GuardedBy("mLock")
    private FillContext getFillContextByRequestIdLocked(int i) {
        if (this.mContexts == null) {
            return null;
        }
        int size = this.mContexts.size();
        for (int i2 = 0; i2 < size; i2++) {
            FillContext fillContext = this.mContexts.get(i2);
            if (fillContext.getRequestId() == i) {
                return fillContext;
            }
        }
        return null;
    }

    @Override
    public void authenticate(int i, int i2, IntentSender intentSender, Bundle bundle) {
        if (Helper.sDebug) {
            Slog.d(TAG, "authenticate(): requestId=" + i + "; datasetIdx=" + i2 + "; intentSender=" + intentSender);
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#authenticate() rejected - session: " + this.id + " destroyed");
                return;
            }
            Intent intentCreateAuthFillInIntentLocked = createAuthFillInIntentLocked(i, bundle);
            if (intentCreateAuthFillInIntentLocked == null) {
                forceRemoveSelfLocked();
                return;
            }
            this.mService.setAuthenticationSelected(this.id, this.mClientState);
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() {
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                    ((Session) obj).startAuthentication(((Integer) obj2).intValue(), (IntentSender) obj3, (Intent) obj4);
                }
            }, this, Integer.valueOf(AutofillManager.makeAuthenticationId(i, i2)), intentSender, intentCreateAuthFillInIntentLocked));
        }
    }

    @Override
    public void onServiceDied(RemoteFillService remoteFillService) {
    }

    @Override
    public void fill(int i, int i2, Dataset dataset) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#fill() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() {
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((Session) obj).autoFill(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (Dataset) obj4, ((Boolean) obj5).booleanValue());
                }
            }, this, Integer.valueOf(i), Integer.valueOf(i2), dataset, true));
        }
    }

    @Override
    public void save() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#save() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((AutofillManagerServiceImpl) obj).handleSessionSave((Session) obj2);
                }
            }, this.mService, this));
        }
    }

    @Override
    public void cancelSave() {
        synchronized (this.mLock) {
            this.mIsSaving = false;
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#cancelSave() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((Session) obj).removeSelf();
                }
            }, this));
        }
    }

    @Override
    public void requestShowFillUi(AutofillId autofillId, int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#requestShowFillUi() rejected - session: " + autofillId + " destroyed");
                return;
            }
            if (autofillId.equals(this.mCurrentViewId)) {
                try {
                    this.mClient.requestShowFillUi(this.id, autofillId, i, i2, this.mViewStates.get(autofillId).getVirtualBounds(), iAutofillWindowPresenter);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error requesting to show fill UI", e);
                }
            } else if (Helper.sDebug) {
                Slog.d(TAG, "Do not show full UI on " + autofillId + " as it is not the current view (" + this.mCurrentViewId + ") anymore");
            }
        }
    }

    @Override
    public void dispatchUnhandledKey(AutofillId autofillId, KeyEvent keyEvent) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#dispatchUnhandledKey() rejected - session: " + autofillId + " destroyed");
                return;
            }
            if (autofillId.equals(this.mCurrentViewId)) {
                try {
                    this.mClient.dispatchUnhandledKey(this.id, autofillId, keyEvent);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error requesting to dispatch unhandled key", e);
                }
            } else {
                Slog.w(TAG, "Do not dispatch unhandled key on " + autofillId + " as it is not the current view (" + this.mCurrentViewId + ") anymore");
            }
        }
    }

    @Override
    public void requestHideFillUi(AutofillId autofillId) {
        synchronized (this.mLock) {
            try {
                this.mClient.requestHideFillUi(this.id, autofillId);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error requesting to hide fill UI", e);
            }
        }
    }

    @Override
    public void startIntentSender(IntentSender intentSender) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#startIntentSender() rejected - session: " + this.id + " destroyed");
                return;
            }
            removeSelfLocked();
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((Session) obj).doStartIntentSender((IntentSender) obj2);
                }
            }, this, intentSender));
        }
    }

    private void doStartIntentSender(IntentSender intentSender) {
        try {
            synchronized (this.mLock) {
                this.mClient.startIntentSender(intentSender, (Intent) null);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error launching auth intent", e);
        }
    }

    @GuardedBy("mLock")
    void setAuthenticationResultLocked(Bundle bundle, int i) {
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#setAuthenticationResultLocked() rejected - session: " + this.id + " destroyed");
            return;
        }
        if (this.mResponses == null) {
            Slog.w(TAG, "setAuthenticationResultLocked(" + i + "): no responses");
            removeSelf();
            return;
        }
        int requestIdFromAuthenticationId = AutofillManager.getRequestIdFromAuthenticationId(i);
        FillResponse fillResponse = this.mResponses.get(requestIdFromAuthenticationId);
        if (fillResponse == null || bundle == null) {
            removeSelf();
            return;
        }
        int datasetIdFromAuthenticationId = AutofillManager.getDatasetIdFromAuthenticationId(i);
        if (datasetIdFromAuthenticationId != 65535 && ((Dataset) fillResponse.getDatasets().get(datasetIdFromAuthenticationId)) == null) {
            removeSelf();
            return;
        }
        Parcelable parcelable = bundle.getParcelable("android.view.autofill.extra.AUTHENTICATION_RESULT");
        Bundle bundle2 = bundle.getBundle("android.view.autofill.extra.CLIENT_STATE");
        if (Helper.sDebug) {
            Slog.d(TAG, "setAuthenticationResultLocked(): result=" + parcelable + ", clientState=" + bundle2);
        }
        if (parcelable instanceof FillResponse) {
            logAuthenticationStatusLocked(requestIdFromAuthenticationId, 912);
            replaceResponseLocked(fillResponse, (FillResponse) parcelable, bundle2);
            return;
        }
        if (parcelable instanceof Dataset) {
            if (datasetIdFromAuthenticationId != 65535) {
                logAuthenticationStatusLocked(requestIdFromAuthenticationId, 1126);
                if (bundle2 != null) {
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Updating client state from auth dataset");
                    }
                    this.mClientState = bundle2;
                }
                Dataset dataset = (Dataset) parcelable;
                fillResponse.getDatasets().set(datasetIdFromAuthenticationId, dataset);
                autoFill(requestIdFromAuthenticationId, datasetIdFromAuthenticationId, dataset, false);
                return;
            }
            logAuthenticationStatusLocked(requestIdFromAuthenticationId, 1127);
            return;
        }
        if (parcelable != null) {
            Slog.w(TAG, "service returned invalid auth type: " + parcelable);
        }
        logAuthenticationStatusLocked(requestIdFromAuthenticationId, 1128);
        processNullResponseLocked(0);
    }

    @GuardedBy("mLock")
    void setHasCallbackLocked(boolean z) {
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#setHasCallbackLocked() rejected - session: " + this.id + " destroyed");
            return;
        }
        this.mHasCallback = z;
    }

    @GuardedBy("mLock")
    private FillResponse getLastResponseLocked(String str) {
        if (this.mContexts == null) {
            if (Helper.sDebug && str != null) {
                Slog.d(TAG, str + ": no contexts");
            }
            return null;
        }
        if (this.mResponses == null) {
            if (Helper.sVerbose && str != null) {
                Slog.v(TAG, str + ": no responses on session");
            }
            return null;
        }
        int lastResponseIndexLocked = getLastResponseIndexLocked();
        if (lastResponseIndexLocked < 0) {
            if (str != null) {
                Slog.w(TAG, str + ": did not get last response. mResponses=" + this.mResponses + ", mViewStates=" + this.mViewStates);
            }
            return null;
        }
        FillResponse fillResponseValueAt = this.mResponses.valueAt(lastResponseIndexLocked);
        if (Helper.sVerbose && str != null) {
            Slog.v(TAG, str + ": mResponses=" + this.mResponses + ", mContexts=" + this.mContexts + ", mViewStates=" + this.mViewStates);
        }
        return fillResponseValueAt;
    }

    @GuardedBy("mLock")
    private SaveInfo getSaveInfoLocked() {
        FillResponse lastResponseLocked = getLastResponseLocked(null);
        if (lastResponseLocked == null) {
            return null;
        }
        return lastResponseLocked.getSaveInfo();
    }

    public void logContextCommitted() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Session) obj).doLogContextCommitted();
            }
        }, this));
    }

    private void doLogContextCommitted() {
        synchronized (this.mLock) {
            logContextCommittedLocked();
        }
    }

    @GuardedBy("mLock")
    private void logContextCommittedLocked() {
        ArrayList<AutofillId> arrayList;
        ArrayList<ArrayList<String>> arrayList2;
        int i;
        boolean z;
        int i2;
        boolean z2;
        int i3;
        boolean z3;
        ArrayList arrayList3;
        boolean z4;
        ArrayList<String> arrayList4;
        FillResponse lastResponseLocked = getLastResponseLocked("logContextCommited()");
        if (lastResponseLocked == null) {
            return;
        }
        int flags = lastResponseLocked.getFlags();
        if ((flags & 1) == 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "logContextCommittedLocked(): ignored by flags " + flags);
                return;
            }
            return;
        }
        int size = this.mResponses.size();
        boolean z5 = false;
        ArraySet<String> arraySet = null;
        for (int i4 = 0; i4 < size; i4++) {
            List datasets = this.mResponses.valueAt(i4).getDatasets();
            if (datasets == null || datasets.isEmpty()) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "logContextCommitted() no datasets at " + i4);
                }
            } else {
                ArraySet<String> arraySet2 = arraySet;
                boolean z6 = z5;
                for (int i5 = 0; i5 < datasets.size(); i5++) {
                    Dataset dataset = (Dataset) datasets.get(i5);
                    String id = dataset.getId();
                    if (id == null) {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "logContextCommitted() skipping idless dataset " + dataset);
                        }
                    } else {
                        if (this.mSelectedDatasetIds == null || !this.mSelectedDatasetIds.contains(id)) {
                            if (Helper.sVerbose) {
                                Slog.v(TAG, "adding ignored dataset " + id);
                            }
                            if (arraySet2 == null) {
                                arraySet2 = new ArraySet<>();
                            }
                            arraySet2.add(id);
                        }
                        z6 = true;
                    }
                }
                z5 = z6;
                arraySet = arraySet2;
            }
        }
        AutofillId[] fieldClassificationIds = lastResponseLocked.getFieldClassificationIds();
        if (!z5 && fieldClassificationIds == null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "logContextCommittedLocked(): skipped (no datasets nor fields classification ids)");
                return;
            }
            return;
        }
        UserData userData = this.mService.getUserData();
        ArraySet<String> arraySet3 = arraySet;
        int i6 = 0;
        ArrayMap arrayMap = null;
        ArrayList<AutofillId> arrayList5 = null;
        ArrayList<String> arrayList6 = null;
        while (i6 < this.mViewStates.size()) {
            ViewState viewStateValueAt = this.mViewStates.valueAt(i6);
            int state = viewStateValueAt.getState();
            if ((state & 8) == 0) {
                i = size;
                z = z5;
            } else {
                if ((state & 4) != 0) {
                    String datasetId = viewStateValueAt.getDatasetId();
                    if (datasetId == null) {
                        Slog.w(TAG, "logContextCommitted(): no dataset id on " + viewStateValueAt);
                    } else {
                        AutofillValue autofilledValue = viewStateValueAt.getAutofilledValue();
                        AutofillValue currentValue = viewStateValueAt.getCurrentValue();
                        if (autofilledValue != null && autofilledValue.equals(currentValue)) {
                            if (Helper.sDebug) {
                                Slog.d(TAG, "logContextCommitted(): ignoring changed " + viewStateValueAt + " because it has same value that was autofilled");
                            }
                        } else {
                            if (Helper.sDebug) {
                                Slog.d(TAG, "logContextCommitted() found changed state: " + viewStateValueAt);
                            }
                            if (arrayList5 == null) {
                                arrayList5 = new ArrayList<>();
                                arrayList4 = new ArrayList<>();
                            } else {
                                arrayList4 = arrayList6;
                            }
                            arrayList5.add(viewStateValueAt.id);
                            arrayList4.add(datasetId);
                            i = size;
                            z = z5;
                            arrayList6 = arrayList4;
                        }
                    }
                } else {
                    AutofillValue currentValue2 = viewStateValueAt.getCurrentValue();
                    if (currentValue2 == null) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "logContextCommitted(): skipping view without current value ( " + viewStateValueAt + ")");
                        }
                    } else if (z5) {
                        ArrayMap arrayMap2 = arrayMap;
                        int i7 = 0;
                        while (i7 < size) {
                            List datasets2 = this.mResponses.valueAt(i7).getDatasets();
                            if (datasets2 == null || datasets2.isEmpty()) {
                                i2 = size;
                                z2 = z5;
                                if (Helper.sVerbose) {
                                    Slog.v(TAG, "logContextCommitted() no datasets at " + i7);
                                }
                            } else {
                                ArrayMap arrayMap3 = arrayMap2;
                                int i8 = 0;
                                while (i8 < datasets2.size()) {
                                    Dataset dataset2 = (Dataset) datasets2.get(i8);
                                    String id2 = dataset2.getId();
                                    if (id2 == null) {
                                        if (!Helper.sVerbose) {
                                            i3 = size;
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            i3 = size;
                                            sb.append("logContextCommitted() skipping idless dataset ");
                                            sb.append(dataset2);
                                            Slog.v(TAG, sb.toString());
                                        }
                                        z3 = z5;
                                    } else {
                                        i3 = size;
                                        ArrayList fieldValues = dataset2.getFieldValues();
                                        int i9 = 0;
                                        while (i9 < fieldValues.size()) {
                                            if (!currentValue2.equals((AutofillValue) fieldValues.get(i9))) {
                                                arrayList3 = fieldValues;
                                                z4 = z5;
                                            } else {
                                                if (Helper.sDebug) {
                                                    arrayList3 = fieldValues;
                                                    StringBuilder sb2 = new StringBuilder();
                                                    z4 = z5;
                                                    sb2.append("field ");
                                                    sb2.append(viewStateValueAt.id);
                                                    sb2.append(" was manually filled with value set by dataset ");
                                                    sb2.append(id2);
                                                    Slog.d(TAG, sb2.toString());
                                                } else {
                                                    arrayList3 = fieldValues;
                                                    z4 = z5;
                                                }
                                                if (arrayMap3 == null) {
                                                    arrayMap3 = new ArrayMap();
                                                }
                                                ArraySet arraySet4 = (ArraySet) arrayMap3.get(viewStateValueAt.id);
                                                if (arraySet4 == null) {
                                                    arraySet4 = new ArraySet(1);
                                                    arrayMap3.put(viewStateValueAt.id, arraySet4);
                                                }
                                                arraySet4.add(id2);
                                            }
                                            i9++;
                                            fieldValues = arrayList3;
                                            z5 = z4;
                                        }
                                        z3 = z5;
                                        if (this.mSelectedDatasetIds == null || !this.mSelectedDatasetIds.contains(id2)) {
                                            if (Helper.sVerbose) {
                                                Slog.v(TAG, "adding ignored dataset " + id2);
                                            }
                                            if (arraySet3 == null) {
                                                arraySet3 = new ArraySet<>();
                                            }
                                            arraySet3.add(id2);
                                        }
                                    }
                                    i8++;
                                    size = i3;
                                    z5 = z3;
                                }
                                i2 = size;
                                z2 = z5;
                                arrayMap2 = arrayMap3;
                            }
                            i7++;
                            size = i2;
                            z5 = z2;
                        }
                        i = size;
                        z = z5;
                        arrayMap = arrayMap2;
                    }
                }
                i = size;
                z = z5;
            }
            i6++;
            size = i;
            z5 = z;
        }
        if (arrayMap != null) {
            int size2 = arrayMap.size();
            ArrayList<AutofillId> arrayList7 = new ArrayList<>(size2);
            ArrayList<ArrayList<String>> arrayList8 = new ArrayList<>(size2);
            for (int i10 = 0; i10 < size2; i10++) {
                AutofillId autofillId = (AutofillId) arrayMap.keyAt(i10);
                ArraySet arraySet5 = (ArraySet) arrayMap.valueAt(i10);
                arrayList7.add(autofillId);
                arrayList8.add(new ArrayList<>(arraySet5));
            }
            arrayList = arrayList7;
            arrayList2 = arrayList8;
        } else {
            arrayList = null;
            arrayList2 = null;
        }
        FieldClassificationStrategy fieldClassificationStrategy = this.mService.getFieldClassificationStrategy();
        if (userData == null || fieldClassificationStrategy == null) {
            this.mService.logContextCommittedLocked(this.id, this.mClientState, this.mSelectedDatasetIds, arraySet3, arrayList5, arrayList6, arrayList, arrayList2, this.mComponentName, this.mCompatMode);
        } else {
            logFieldClassificationScoreLocked(fieldClassificationStrategy, arraySet3, arrayList5, arrayList6, arrayList, arrayList2, userData, this.mViewStates.values());
        }
    }

    private void logFieldClassificationScoreLocked(FieldClassificationStrategy fieldClassificationStrategy, final ArraySet<String> arraySet, final ArrayList<AutofillId> arrayList, final ArrayList<String> arrayList2, final ArrayList<AutofillId> arrayList3, final ArrayList<ArrayList<String>> arrayList4, UserData userData, Collection<ViewState> collection) {
        int length;
        final String[] values = userData.getValues();
        final String[] categoryIds = userData.getCategoryIds();
        if (values == null || categoryIds == null || values.length != categoryIds.length) {
            if (values != null) {
                length = values.length;
            } else {
                length = -1;
            }
            Slog.w(TAG, "setScores(): user data mismatch: values.length = " + length + ", ids.length = " + (categoryIds != null ? categoryIds.length : -1));
            return;
        }
        int maxFieldClassificationIdsSize = UserData.getMaxFieldClassificationIdsSize();
        final ArrayList arrayList5 = new ArrayList(maxFieldClassificationIdsSize);
        final ArrayList arrayList6 = new ArrayList(maxFieldClassificationIdsSize);
        String fieldClassificationAlgorithm = userData.getFieldClassificationAlgorithm();
        Bundle algorithmArgs = userData.getAlgorithmArgs();
        final int size = collection.size();
        final AutofillId[] autofillIdArr = new AutofillId[size];
        ArrayList arrayList7 = new ArrayList(size);
        int i = 0;
        for (ViewState viewState : collection) {
            arrayList7.add(viewState.getCurrentValue());
            autofillIdArr[i] = viewState.id;
            i++;
        }
        fieldClassificationStrategy.getScores(new RemoteCallback(new RemoteCallback.OnResultListener() {
            public final void onResult(Bundle bundle) {
                Session.lambda$logFieldClassificationScoreLocked$1(this.f$0, arraySet, arrayList, arrayList2, arrayList3, arrayList4, size, autofillIdArr, values, categoryIds, arrayList5, arrayList6, bundle);
            }
        }), fieldClassificationAlgorithm, algorithmArgs, arrayList7, values);
    }

    public static void lambda$logFieldClassificationScoreLocked$1(Session session, ArraySet arraySet, ArrayList arrayList, ArrayList arrayList2, ArrayList arrayList3, ArrayList arrayList4, int i, AutofillId[] autofillIdArr, String[] strArr, String[] strArr2, ArrayList arrayList5, ArrayList arrayList6, Bundle bundle) {
        if (bundle != null) {
            AutofillFieldClassificationService.Scores parcelable = bundle.getParcelable("scores");
            if (parcelable == null) {
                Slog.w(TAG, "No field classification score on " + bundle);
                return;
            }
            int i2 = 0;
            for (int i3 = 0; i3 < i; i3++) {
                try {
                    AutofillId autofillId = autofillIdArr[i3];
                    ArrayMap arrayMap = null;
                    i2 = 0;
                    while (i2 < strArr.length) {
                        String str = strArr2[i2];
                        float f = parcelable.scores[i3][i2];
                        if (f > 0.0f) {
                            if (arrayMap == null) {
                                arrayMap = new ArrayMap(strArr.length);
                            }
                            Float f2 = (Float) arrayMap.get(str);
                            if (f2 != null && f2.floatValue() > f) {
                                if (Helper.sVerbose) {
                                    Slog.v(TAG, "skipping score " + f + " because it's less than " + f2);
                                }
                            } else {
                                if (Helper.sVerbose) {
                                    Slog.v(TAG, "adding score " + f + " at index " + i2 + " and id " + autofillId);
                                }
                                arrayMap.put(str, Float.valueOf(f));
                            }
                        } else if (Helper.sVerbose) {
                            Slog.v(TAG, "skipping score 0 at index " + i2 + " and id " + autofillId);
                        }
                        i2++;
                    }
                    if (arrayMap == null) {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "no score for autofillId=" + autofillId);
                        }
                    } else {
                        ArrayList arrayList7 = new ArrayList(arrayMap.size());
                        i2 = 0;
                        while (i2 < arrayMap.size()) {
                            arrayList7.add(new FieldClassification.Match((String) arrayMap.keyAt(i2), ((Float) arrayMap.valueAt(i2)).floatValue()));
                            i2++;
                        }
                        arrayList5.add(autofillId);
                        arrayList6.add(new FieldClassification(arrayList7));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    session.wtf(e, "Error accessing FC score at [%d, %d] (%s): %s", Integer.valueOf(i3), Integer.valueOf(i2), parcelable, e);
                    return;
                }
            }
            session.mService.logContextCommittedLocked(session.id, session.mClientState, session.mSelectedDatasetIds, arraySet, arrayList, arrayList2, arrayList3, arrayList4, arrayList5, arrayList6, session.mComponentName, session.mCompatMode);
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "setFieldClassificationScore(): no results");
        }
        session.mService.logContextCommittedLocked(session.id, session.mClientState, session.mSelectedDatasetIds, arraySet, arrayList, arrayList2, arrayList3, arrayList4, session.mComponentName, session.mCompatMode);
    }

    @GuardedBy("mLock")
    public boolean showSaveLocked() {
        boolean z;
        int i;
        AutofillValue sanitizedValue;
        boolean z2;
        AutofillValue valueFromContextsLocked;
        boolean z3 = false;
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#showSaveLocked() rejected - session: " + this.id + " destroyed");
            return false;
        }
        FillResponse lastResponseLocked = getLastResponseLocked("showSaveLocked()");
        SaveInfo saveInfo = lastResponseLocked == null ? null : lastResponseLocked.getSaveInfo();
        if (saveInfo == null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "showSaveLocked(): no saveInfo from service");
            }
            return true;
        }
        ArrayMap<AutofillId, InternalSanitizer> arrayMapCreateSanitizers = createSanitizers(saveInfo);
        ArrayMap arrayMap = new ArrayMap();
        ArraySet arraySet = new ArraySet();
        AutofillId[] requiredIds = saveInfo.getRequiredIds();
        if (requiredIds == null) {
            z3 = true;
            z = false;
        } else {
            int i2 = 0;
            z = false;
            while (true) {
                if (i2 >= requiredIds.length) {
                    z3 = true;
                    break;
                }
                AutofillId autofillId = requiredIds[i2];
                if (autofillId == null) {
                    Slog.w(TAG, "null autofill id on " + Arrays.toString(requiredIds));
                } else {
                    arraySet.add(autofillId);
                    ViewState viewState = this.mViewStates.get(autofillId);
                    if (viewState == null) {
                        Slog.w(TAG, "showSaveLocked(): no ViewState for required " + autofillId);
                        break;
                    }
                    AutofillValue currentValue = viewState.getCurrentValue();
                    if (currentValue == null || currentValue.isEmpty()) {
                        currentValue = getValueFromContextsLocked(autofillId);
                        if (currentValue != null) {
                            if (Helper.sDebug) {
                                Slog.d(TAG, "Value of required field " + autofillId + " didn't change; using initial value (" + currentValue + ") instead");
                            }
                            sanitizedValue = getSanitizedValue(arrayMapCreateSanitizers, autofillId, currentValue);
                            if (sanitizedValue != null) {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "value of required field " + autofillId + " failed sanitization");
                                }
                            } else {
                                viewState.setSanitizedValue(sanitizedValue);
                                arrayMap.put(autofillId, sanitizedValue);
                                AutofillValue autofilledValue = viewState.getAutofilledValue();
                                if (!sanitizedValue.equals(autofilledValue)) {
                                    if (autofilledValue != null || (valueFromContextsLocked = getValueFromContextsLocked(autofillId)) == null || !valueFromContextsLocked.equals(sanitizedValue)) {
                                        z2 = true;
                                    } else {
                                        if (Helper.sDebug) {
                                            Slog.d(TAG, "id " + autofillId + " is part of dataset but initial value didn't change: " + sanitizedValue);
                                        }
                                        z2 = false;
                                    }
                                    if (z2) {
                                        if (Helper.sDebug) {
                                            Slog.d(TAG, "found a change on required " + autofillId + ": " + autofilledValue + " => " + sanitizedValue);
                                        }
                                        z = true;
                                    }
                                }
                            }
                        } else if (Helper.sDebug) {
                            Slog.d(TAG, "empty value for required " + autofillId);
                        }
                    } else {
                        sanitizedValue = getSanitizedValue(arrayMapCreateSanitizers, autofillId, currentValue);
                        if (sanitizedValue != null) {
                        }
                    }
                }
                i2++;
                z3 = false;
            }
            z3 = false;
        }
        AutofillId[] optionalIds = saveInfo.getOptionalIds();
        if (z3) {
            if (!z && optionalIds != null) {
                int i3 = 0;
                while (true) {
                    if (i3 >= optionalIds.length) {
                        break;
                    }
                    AutofillId autofillId2 = optionalIds[i3];
                    arraySet.add(autofillId2);
                    ViewState viewState2 = this.mViewStates.get(autofillId2);
                    if (viewState2 == null) {
                        Slog.w(TAG, "no ViewState for optional " + autofillId2);
                    } else if ((viewState2.getState() & 8) != 0) {
                        AutofillValue currentValue2 = viewState2.getCurrentValue();
                        arrayMap.put(autofillId2, currentValue2);
                        AutofillValue autofilledValue2 = viewState2.getAutofilledValue();
                        if (currentValue2 != null && !currentValue2.equals(autofilledValue2)) {
                            if (Helper.sDebug) {
                                Slog.d(TAG, "found a change on optional " + autofillId2 + ": " + autofilledValue2 + " => " + currentValue2);
                            }
                            z = true;
                        }
                    } else {
                        AutofillValue valueFromContextsLocked2 = getValueFromContextsLocked(autofillId2);
                        if (Helper.sDebug) {
                            Slog.d(TAG, "no current value for " + autofillId2 + "; initial value is " + valueFromContextsLocked2);
                        }
                        if (valueFromContextsLocked2 != null) {
                            arrayMap.put(autofillId2, valueFromContextsLocked2);
                        }
                    }
                    i3++;
                }
            }
            if (z) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "at least one field changed, validate fields for save UI");
                }
                InternalValidator validator = saveInfo.getValidator();
                if (validator != null) {
                    LogMaker logMakerNewLogMaker = newLogMaker(1133);
                    try {
                        boolean zIsValid = validator.isValid(this);
                        if (Helper.sDebug) {
                            Slog.d(TAG, validator + " returned " + zIsValid);
                        }
                        if (zIsValid) {
                            i = 10;
                        } else {
                            i = 5;
                        }
                        logMakerNewLogMaker.setType(i);
                        this.mMetricsLogger.write(logMakerNewLogMaker);
                        if (!zIsValid) {
                            Slog.i(TAG, "not showing save UI because fields failed validation");
                            return true;
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Not showing save UI because validation failed:", e);
                        logMakerNewLogMaker.setType(11);
                        this.mMetricsLogger.write(logMakerNewLogMaker);
                        return true;
                    }
                }
                List datasets = lastResponseLocked.getDatasets();
                if (datasets != null) {
                    for (int i4 = 0; i4 < datasets.size(); i4++) {
                        Dataset dataset = (Dataset) datasets.get(i4);
                        ArrayMap<AutofillId, AutofillValue> fields = Helper.getFields(dataset);
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "Checking if saved fields match contents of dataset #" + i4 + ": " + dataset + "; allIds=" + arraySet);
                        }
                        for (int i5 = 0; i5 < arraySet.size(); i5++) {
                            AutofillId autofillId3 = (AutofillId) arraySet.valueAt(i5);
                            AutofillValue autofillValue = (AutofillValue) arrayMap.get(autofillId3);
                            if (autofillValue == null) {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "dataset has value for field that is null: " + autofillId3);
                                }
                            } else {
                                AutofillValue autofillValue2 = fields.get(autofillId3);
                                if (!autofillValue.equals(autofillValue2)) {
                                    if (Helper.sDebug) {
                                        Slog.d(TAG, "found a dataset change on id " + autofillId3 + ": from " + autofillValue2 + " to " + autofillValue);
                                    }
                                } else {
                                    if (Helper.sVerbose) {
                                        Slog.v(TAG, "no dataset changes for id " + autofillId3);
                                    }
                                }
                            }
                        }
                        if (Helper.sDebug) {
                            Slog.d(TAG, "ignoring Save UI because all fields match contents of dataset #" + i4 + ": " + dataset);
                        }
                        return true;
                    }
                }
                if (Helper.sDebug) {
                    Slog.d(TAG, "Good news, everyone! All checks passed, show save UI for " + this.id + "!");
                }
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((Session) obj).logSaveShown();
                    }
                }, this));
                IAutoFillManagerClient client = getClient();
                this.mPendingSaveUi = new PendingUi(this.mActivityToken, this.id, client);
                getUiForShowing().showSaveUi(this.mService.getServiceLabel(), this.mService.getServiceIcon(), this.mService.getServicePackageName(), saveInfo, this, this.mComponentName, this, this.mPendingSaveUi, this.mCompatMode);
                if (client != null) {
                    try {
                        client.setSaveUiState(this.id, true);
                    } catch (RemoteException e2) {
                        Slog.e(TAG, "Error notifying client to set save UI state to shown: " + e2);
                    }
                }
                this.mIsSaving = true;
                return false;
            }
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "showSaveLocked(" + this.id + "): with no changes, comes no responsibilities.allRequiredAreNotNull=" + z3 + ", atLeastOneChanged=" + z);
        }
        return true;
    }

    private void logSaveShown() {
        this.mService.logSaveShown(this.id, this.mClientState);
    }

    private ArrayMap<AutofillId, InternalSanitizer> createSanitizers(SaveInfo saveInfo) {
        InternalSanitizer[] sanitizerKeys;
        if (saveInfo == null || (sanitizerKeys = saveInfo.getSanitizerKeys()) == null) {
            return null;
        }
        int length = sanitizerKeys.length;
        ArrayMap<AutofillId, InternalSanitizer> arrayMap = new ArrayMap<>(length);
        if (Helper.sDebug) {
            Slog.d(TAG, "Service provided " + length + " sanitizers");
        }
        AutofillId[][] sanitizerValues = saveInfo.getSanitizerValues();
        for (int i = 0; i < length; i++) {
            InternalSanitizer internalSanitizer = sanitizerKeys[i];
            AutofillId[] autofillIdArr = sanitizerValues[i];
            if (Helper.sDebug) {
                Slog.d(TAG, "sanitizer #" + i + " (" + internalSanitizer + ") for ids " + Arrays.toString(autofillIdArr));
            }
            for (AutofillId autofillId : autofillIdArr) {
                arrayMap.put(autofillId, internalSanitizer);
            }
        }
        return arrayMap;
    }

    private AutofillValue getSanitizedValue(ArrayMap<AutofillId, InternalSanitizer> arrayMap, AutofillId autofillId, AutofillValue autofillValue) {
        InternalSanitizer internalSanitizer;
        if (arrayMap == null || (internalSanitizer = arrayMap.get(autofillId)) == null) {
            return autofillValue;
        }
        AutofillValue autofillValueSanitize = internalSanitizer.sanitize(autofillValue);
        if (Helper.sDebug) {
            Slog.d(TAG, "Value for " + autofillId + "(" + autofillValue + ") sanitized to " + autofillValueSanitize);
        }
        return autofillValueSanitize;
    }

    @GuardedBy("mLock")
    boolean isSavingLocked() {
        return this.mIsSaving;
    }

    @GuardedBy("mLock")
    private AutofillValue getValueFromContextsLocked(AutofillId autofillId) {
        for (int size = this.mContexts.size() - 1; size >= 0; size--) {
            AssistStructure.ViewNode viewNodeFindViewNodeByAutofillId = Helper.findViewNodeByAutofillId(this.mContexts.get(size).getStructure(), autofillId);
            if (viewNodeFindViewNodeByAutofillId != null) {
                AutofillValue autofillValue = viewNodeFindViewNodeByAutofillId.getAutofillValue();
                if (Helper.sDebug) {
                    Slog.d(TAG, "getValueFromContexts(" + autofillId + ") at " + size + ": " + autofillValue);
                }
                if (autofillValue != null && !autofillValue.isEmpty()) {
                    return autofillValue;
                }
            }
        }
        return null;
    }

    @GuardedBy("mLock")
    private CharSequence[] getAutofillOptionsFromContextsLocked(AutofillId autofillId) {
        for (int size = this.mContexts.size() - 1; size >= 0; size--) {
            AssistStructure.ViewNode viewNodeFindViewNodeByAutofillId = Helper.findViewNodeByAutofillId(this.mContexts.get(size).getStructure(), autofillId);
            if (viewNodeFindViewNodeByAutofillId != null && viewNodeFindViewNodeByAutofillId.getAutofillOptions() != null) {
                return viewNodeFindViewNodeByAutofillId.getAutofillOptions();
            }
        }
        return null;
    }

    @GuardedBy("mLock")
    void callSaveLocked() {
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#callSaveLocked() rejected - session: " + this.id + " destroyed");
            return;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "callSaveLocked(): mViewStates=" + this.mViewStates);
        }
        if (this.mContexts == null) {
            Slog.w(TAG, "callSaveLocked(): no contexts");
            return;
        }
        ArrayMap<AutofillId, InternalSanitizer> arrayMapCreateSanitizers = createSanitizers(getSaveInfoLocked());
        int size = this.mContexts.size();
        for (int i = 0; i < size; i++) {
            FillContext fillContext = this.mContexts.get(i);
            AssistStructure.ViewNode[] viewNodeArrFindViewNodesByAutofillIds = fillContext.findViewNodesByAutofillIds(getIdsOfAllViewStatesLocked());
            if (Helper.sVerbose) {
                Slog.v(TAG, "callSaveLocked(): updating " + fillContext);
            }
            for (int i2 = 0; i2 < this.mViewStates.size(); i2++) {
                ViewState viewStateValueAt = this.mViewStates.valueAt(i2);
                AutofillId autofillId = viewStateValueAt.id;
                AutofillValue currentValue = viewStateValueAt.getCurrentValue();
                if (currentValue == null) {
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "callSaveLocked(): skipping " + autofillId);
                    }
                } else {
                    AssistStructure.ViewNode viewNode = viewNodeArrFindViewNodesByAutofillIds[i2];
                    if (viewNode == null) {
                        Slog.w(TAG, "callSaveLocked(): did not find node with id " + autofillId);
                    } else {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "callSaveLocked(): updating " + autofillId + " to " + currentValue);
                        }
                        AutofillValue sanitizedValue = viewStateValueAt.getSanitizedValue();
                        if (sanitizedValue == null) {
                            sanitizedValue = getSanitizedValue(arrayMapCreateSanitizers, autofillId, currentValue);
                        }
                        if (sanitizedValue != null) {
                            viewNode.updateAutofillValue(sanitizedValue);
                        } else if (Helper.sDebug) {
                            Slog.d(TAG, "Not updating field " + autofillId + " because it failed sanitization");
                        }
                    }
                }
            }
            fillContext.getStructure().sanitizeForParceling(false);
            if (Helper.sVerbose) {
                Slog.v(TAG, "Dumping structure of " + fillContext + " before calling service.save()");
                fillContext.getStructure().dump(false);
            }
        }
        cancelCurrentRequestLocked();
        this.mRemoteFillService.onSaveRequest(new SaveRequest(new ArrayList(this.mContexts), this.mClientState, this.mSelectedDatasetIds));
    }

    @GuardedBy("mLock")
    private void requestNewFillResponseOnViewEnteredIfNecessaryLocked(AutofillId autofillId, ViewState viewState, int i) {
        if ((i & 1) != 0) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Re-starting session on view " + autofillId + " and flags " + i);
            }
            viewState.setState(256);
            requestNewFillResponseLocked(i);
            return;
        }
        if (shouldStartNewPartitionLocked(autofillId)) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Starting partition for view id " + autofillId + ": " + viewState.getStateAsString());
            }
            viewState.setState(32);
            requestNewFillResponseLocked(i);
            return;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "Not starting new partition for view " + autofillId + ": " + viewState.getStateAsString());
        }
    }

    @GuardedBy("mLock")
    private boolean shouldStartNewPartitionLocked(AutofillId autofillId) {
        if (this.mResponses == null) {
            return true;
        }
        int size = this.mResponses.size();
        if (size >= Helper.sPartitionMaxCount) {
            Slog.e(TAG, "Not starting a new partition on " + autofillId + " because session " + this.id + " reached maximum of " + Helper.sPartitionMaxCount);
            return false;
        }
        for (int i = 0; i < size; i++) {
            FillResponse fillResponseValueAt = this.mResponses.valueAt(i);
            if (ArrayUtils.contains(fillResponseValueAt.getIgnoredIds(), autofillId)) {
                return false;
            }
            SaveInfo saveInfo = fillResponseValueAt.getSaveInfo();
            if (saveInfo != null && (ArrayUtils.contains(saveInfo.getOptionalIds(), autofillId) || ArrayUtils.contains(saveInfo.getRequiredIds(), autofillId))) {
                return false;
            }
            List datasets = fillResponseValueAt.getDatasets();
            if (datasets != null) {
                int size2 = datasets.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    ArrayList fieldIds = ((Dataset) datasets.get(i2)).getFieldIds();
                    if (fieldIds != null && fieldIds.contains(autofillId)) {
                        return false;
                    }
                }
            }
            if (ArrayUtils.contains(fillResponseValueAt.getAuthenticationIds(), autofillId)) {
                return false;
            }
        }
        return true;
    }

    @GuardedBy("mLock")
    void updateLocked(AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i, int i2) {
        CharSequence textValue;
        String strTrim;
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#updateLocked() rejected - session: " + autofillId + " destroyed");
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "updateLocked(): id=" + autofillId + ", action=" + actionAsString(i) + ", flags=" + i2);
        }
        ViewState viewState = this.mViewStates.get(autofillId);
        if (viewState == null) {
            if (i == 1 || i == 4 || i == 2) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Creating viewState for " + autofillId);
                }
                boolean zIsIgnoredLocked = isIgnoredLocked(autofillId);
                ViewState viewState2 = new ViewState(this, autofillId, this, zIsIgnoredLocked ? 128 : 1);
                this.mViewStates.put(autofillId, viewState2);
                if (zIsIgnoredLocked) {
                    if (Helper.sDebug) {
                        Slog.d(TAG, "updateLocked(): ignoring view " + viewState2);
                        return;
                    }
                    return;
                }
                viewState = viewState2;
            } else {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Ignoring specific action when viewState=null");
                    return;
                }
                return;
            }
        }
        String string = null;
        switch (i) {
            case 1:
                this.mCurrentViewId = viewState.id;
                viewState.update(autofillValue, rect, i2);
                viewState.setState(16);
                requestNewFillResponseLocked(i2);
                break;
            case 2:
                if (Helper.sVerbose && rect != null) {
                    Slog.v(TAG, "entered on virtual child " + autofillId + ": " + rect);
                }
                if (this.mCompatMode && (viewState.getState() & 512) != 0) {
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Ignoring VIEW_ENTERED on URL BAR (id=" + autofillId + ")");
                    }
                } else {
                    requestNewFillResponseOnViewEnteredIfNecessaryLocked(autofillId, viewState, i2);
                    if (this.mCurrentViewId != viewState.id) {
                        this.mUi.hideFillUi(this);
                        this.mCurrentViewId = viewState.id;
                    }
                    viewState.update(autofillValue, rect, i2);
                }
                break;
            case 3:
                if (this.mCurrentViewId == viewState.id) {
                    if (Helper.sVerbose) {
                        Slog.d(TAG, "Exiting view " + autofillId);
                    }
                    this.mUi.hideFillUi(this);
                    this.mCurrentViewId = null;
                }
                break;
            case 4:
                if (this.mCompatMode && (viewState.getState() & 512) != 0) {
                    if (this.mUrlBar != null) {
                        strTrim = this.mUrlBar.getText().toString().trim();
                    } else {
                        strTrim = null;
                    }
                    if (strTrim == null) {
                        wtf(null, "URL bar value changed, but current value is null", new Object[0]);
                    } else if (autofillValue == null || !autofillValue.isText()) {
                        wtf(null, "URL bar value changed to null or non-text: %s", autofillValue);
                    } else if (autofillValue.getTextValue().toString().equals(strTrim)) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Ignoring change on URL bar as it's the same");
                        }
                    } else if (this.mSaveOnAllViewsInvisible) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Ignoring change on URL because session will finish when views are gone");
                        }
                    } else {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Finishing session because URL bar changed");
                        }
                        forceRemoveSelfLocked(5);
                    }
                } else if (!Objects.equals(autofillValue, viewState.getCurrentValue())) {
                    if ((autofillValue == null || autofillValue.isEmpty()) && viewState.getCurrentValue() != null && viewState.getCurrentValue().isText() && viewState.getCurrentValue().getTextValue() != null && getSaveInfoLocked() != null) {
                        int length = viewState.getCurrentValue().getTextValue().length();
                        if (Helper.sDebug) {
                            Slog.d(TAG, "updateLocked(" + autofillId + "): resetting value that was " + length + " chars long");
                        }
                        this.mMetricsLogger.write(newLogMaker(1124).addTaggedData(1125, Integer.valueOf(length)));
                    }
                    viewState.setCurrentValue(autofillValue);
                    AutofillValue autofilledValue = viewState.getAutofilledValue();
                    if (autofilledValue != null && autofilledValue.equals(autofillValue)) {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "ignoring autofilled change on id " + autofillId);
                        }
                    } else {
                        viewState.setState(8);
                        if (autofillValue != null && autofillValue.isText() && (textValue = autofillValue.getTextValue()) != null) {
                            string = textValue.toString();
                        }
                        getUiForShowing().filterFillUi(string, this);
                    }
                }
                break;
            default:
                Slog.w(TAG, "updateLocked(): unknown action: " + i);
                break;
        }
    }

    @GuardedBy("mLock")
    private boolean isIgnoredLocked(AutofillId autofillId) {
        FillResponse lastResponseLocked = getLastResponseLocked(null);
        if (lastResponseLocked == null) {
            return false;
        }
        return ArrayUtils.contains(lastResponseLocked.getIgnoredIds(), autofillId);
    }

    @Override
    public void onFillReady(FillResponse fillResponse, AutofillId autofillId, AutofillValue autofillValue) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onFillReady() rejected - session: " + this.id + " destroyed");
                return;
            }
            String string = null;
            if (autofillValue != null && autofillValue.isText()) {
                string = autofillValue.getTextValue().toString();
            }
            getUiForShowing().showFillUi(autofillId, fillResponse, string, this.mService.getServicePackageName(), this.mComponentName, this.mService.getServiceLabel(), this.mService.getServiceIcon(), this, this.id, this.mCompatMode);
            synchronized (this.mLock) {
                if (this.mUiShownTime == 0) {
                    this.mUiShownTime = SystemClock.elapsedRealtime();
                    long j = this.mUiShownTime - this.mStartTime;
                    if (Helper.sDebug) {
                        StringBuilder sb = new StringBuilder("1st UI for ");
                        sb.append(this.mActivityToken);
                        sb.append(" shown in ");
                        TimeUtils.formatDuration(j, sb);
                        Slog.d(TAG, sb.toString());
                    }
                    StringBuilder sb2 = new StringBuilder("id=");
                    sb2.append(this.id);
                    sb2.append(" app=");
                    sb2.append(this.mActivityToken);
                    sb2.append(" svc=");
                    sb2.append(this.mService.getServicePackageName());
                    sb2.append(" latency=");
                    TimeUtils.formatDuration(j, sb2);
                    this.mUiLatencyHistory.log(sb2.toString());
                    addTaggedDataToRequestLogLocked(fillResponse.getRequestId(), 1145, Long.valueOf(j));
                }
            }
        }
    }

    boolean isDestroyed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDestroyed;
        }
        return z;
    }

    IAutoFillManagerClient getClient() {
        IAutoFillManagerClient iAutoFillManagerClient;
        synchronized (this.mLock) {
            iAutoFillManagerClient = this.mClient;
        }
        return iAutoFillManagerClient;
    }

    private void notifyUnavailableToClient(int i) {
        synchronized (this.mLock) {
            if (this.mCurrentViewId == null) {
                return;
            }
            try {
                if (this.mHasCallback) {
                    this.mClient.notifyNoFillUi(this.id, this.mCurrentViewId, i);
                } else if (i != 0) {
                    this.mClient.setSessionFinished(i);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client no fill UI: id=" + this.mCurrentViewId, e);
            }
        }
    }

    @GuardedBy("mLock")
    private void updateTrackedIdsLocked() {
        ArraySet arraySet;
        AutofillId autofillId;
        boolean z;
        boolean z2;
        ArraySet arraySet2 = null;
        FillResponse lastResponseLocked = getLastResponseLocked(null);
        if (lastResponseLocked == null) {
            return;
        }
        this.mSaveOnAllViewsInvisible = false;
        SaveInfo saveInfo = lastResponseLocked.getSaveInfo();
        if (saveInfo != null) {
            AutofillId triggerId = saveInfo.getTriggerId();
            if (triggerId != null) {
                writeLog(1228);
            }
            if ((saveInfo.getFlags() & 1) == 0) {
                z2 = false;
            } else {
                z2 = true;
            }
            this.mSaveOnAllViewsInvisible = z2;
            if (this.mSaveOnAllViewsInvisible) {
                arraySet = new ArraySet();
                if (saveInfo.getRequiredIds() != null) {
                    Collections.addAll(arraySet, saveInfo.getRequiredIds());
                }
                if (saveInfo.getOptionalIds() != null) {
                    Collections.addAll(arraySet, saveInfo.getOptionalIds());
                }
            } else {
                arraySet = null;
            }
            if ((saveInfo.getFlags() & 2) == 0) {
                z = true;
            } else {
                z = false;
            }
            autofillId = triggerId;
        } else {
            arraySet = null;
            autofillId = null;
            z = true;
        }
        List datasets = lastResponseLocked.getDatasets();
        if (datasets != null) {
            ArraySet arraySet3 = null;
            for (int i = 0; i < datasets.size(); i++) {
                ArrayList fieldIds = ((Dataset) datasets.get(i)).getFieldIds();
                if (fieldIds != null) {
                    ArraySet arraySetAdd = arraySet3;
                    for (int i2 = 0; i2 < fieldIds.size(); i2++) {
                        AutofillId autofillId2 = (AutofillId) fieldIds.get(i2);
                        if (arraySet == null || !arraySet.contains(autofillId2)) {
                            arraySetAdd = ArrayUtils.add(arraySetAdd, autofillId2);
                        }
                    }
                    arraySet3 = arraySetAdd;
                }
            }
            arraySet2 = arraySet3;
        }
        try {
            if (Helper.sVerbose) {
                Slog.v(TAG, "updateTrackedIdsLocked(): " + arraySet + " => " + arraySet2 + " triggerId: " + autofillId + " saveOnFinish:" + z);
            }
            this.mClient.setTrackedViews(this.id, Helper.toArray(arraySet), this.mSaveOnAllViewsInvisible, z, Helper.toArray(arraySet2), autofillId);
        } catch (RemoteException e) {
            Slog.w(TAG, "Cannot set tracked ids", e);
        }
    }

    @GuardedBy("mLock")
    void setAutofillFailureLocked(List<AutofillId> list) {
        for (int i = 0; i < list.size(); i++) {
            AutofillId autofillId = list.get(i);
            ViewState viewState = this.mViewStates.get(autofillId);
            if (viewState == null) {
                Slog.w(TAG, "setAutofillFailure(): no view for id " + autofillId);
            } else {
                viewState.resetState(4);
                viewState.setState(viewState.getState() | 1024);
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Changed state of " + autofillId + " to " + viewState.getStateAsString());
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void replaceResponseLocked(FillResponse fillResponse, FillResponse fillResponse2, Bundle bundle) {
        setViewStatesLocked(fillResponse, 1, true);
        fillResponse2.setRequestId(fillResponse.getRequestId());
        this.mResponses.put(fillResponse2.getRequestId(), fillResponse2);
        processResponseLocked(fillResponse2, bundle, 0);
    }

    private void processNullResponseLocked(int i) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "canceling session " + this.id + " when server returned null");
        }
        if ((i & 1) != 0) {
            getUiForShowing().showError(R.string.PERSOSUBSTATE_SIM_NETWORK_PUK_SUCCESS, this);
        }
        this.mService.resetLastResponse();
        notifyUnavailableToClient(2);
        removeSelf();
    }

    @GuardedBy("mLock")
    private void processResponseLocked(FillResponse fillResponse, Bundle bundle, int i) {
        this.mUi.hideAll(this);
        int requestId = fillResponse.getRequestId();
        if (Helper.sVerbose) {
            Slog.v(TAG, "processResponseLocked(): mCurrentViewId=" + this.mCurrentViewId + ",flags=" + i + ", reqId=" + requestId + ", resp=" + fillResponse + ",newClientState=" + bundle);
        }
        if (this.mResponses == null) {
            this.mResponses = new SparseArray<>(2);
        }
        this.mResponses.put(requestId, fillResponse);
        if (bundle == null) {
            bundle = fillResponse.getClientState();
        }
        this.mClientState = bundle;
        setViewStatesLocked(fillResponse, 2, false);
        updateTrackedIdsLocked();
        if (this.mCurrentViewId == null) {
            return;
        }
        this.mViewStates.get(this.mCurrentViewId).maybeCallOnFillReady(i);
    }

    @GuardedBy("mLock")
    private void setViewStatesLocked(FillResponse fillResponse, int i, boolean z) {
        List datasets = fillResponse.getDatasets();
        if (datasets != null) {
            for (int i2 = 0; i2 < datasets.size(); i2++) {
                Dataset dataset = (Dataset) datasets.get(i2);
                if (dataset == null) {
                    Slog.w(TAG, "Ignoring null dataset on " + datasets);
                } else {
                    setViewStatesLocked(fillResponse, dataset, i, z);
                }
            }
        } else if (fillResponse.getAuthentication() != null) {
            for (AutofillId autofillId : fillResponse.getAuthenticationIds()) {
                ViewState viewStateCreateOrUpdateViewStateLocked = createOrUpdateViewStateLocked(autofillId, i, null);
                if (!z) {
                    viewStateCreateOrUpdateViewStateLocked.setResponse(fillResponse);
                } else {
                    viewStateCreateOrUpdateViewStateLocked.setResponse(null);
                }
            }
        }
        SaveInfo saveInfo = fillResponse.getSaveInfo();
        if (saveInfo != null) {
            AutofillId[] requiredIds = saveInfo.getRequiredIds();
            if (requiredIds != null) {
                for (AutofillId autofillId2 : requiredIds) {
                    createOrUpdateViewStateLocked(autofillId2, i, null);
                }
            }
            AutofillId[] optionalIds = saveInfo.getOptionalIds();
            if (optionalIds != null) {
                for (AutofillId autofillId3 : optionalIds) {
                    createOrUpdateViewStateLocked(autofillId3, i, null);
                }
            }
        }
        AutofillId[] authenticationIds = fillResponse.getAuthenticationIds();
        if (authenticationIds != null) {
            for (AutofillId autofillId4 : authenticationIds) {
                createOrUpdateViewStateLocked(autofillId4, i, null);
            }
        }
    }

    @GuardedBy("mLock")
    private void setViewStatesLocked(FillResponse fillResponse, Dataset dataset, int i, boolean z) {
        ArrayList fieldIds = dataset.getFieldIds();
        ArrayList fieldValues = dataset.getFieldValues();
        for (int i2 = 0; i2 < fieldIds.size(); i2++) {
            ViewState viewStateCreateOrUpdateViewStateLocked = createOrUpdateViewStateLocked((AutofillId) fieldIds.get(i2), i, (AutofillValue) fieldValues.get(i2));
            String id = dataset.getId();
            if (id != null) {
                viewStateCreateOrUpdateViewStateLocked.setDatasetId(id);
            }
            if (fillResponse != null) {
                viewStateCreateOrUpdateViewStateLocked.setResponse(fillResponse);
            } else if (z) {
                viewStateCreateOrUpdateViewStateLocked.setResponse(null);
            }
        }
    }

    @GuardedBy("mLock")
    private ViewState createOrUpdateViewStateLocked(AutofillId autofillId, int i, AutofillValue autofillValue) {
        ViewState viewState = this.mViewStates.get(autofillId);
        if (viewState != null) {
            viewState.setState(i);
        } else {
            viewState = new ViewState(this, autofillId, this, i);
            if (Helper.sVerbose) {
                Slog.v(TAG, "Adding autofillable view with id " + autofillId + " and state " + i);
            }
            this.mViewStates.put(autofillId, viewState);
        }
        if ((i & 4) != 0) {
            viewState.setAutofilledValue(autofillValue);
        }
        return viewState;
    }

    void autoFill(int i, int i2, Dataset dataset, boolean z) {
        if (Helper.sDebug) {
            Slog.d(TAG, "autoFill(): requestId=" + i + "; datasetIdx=" + i2 + "; dataset=" + dataset);
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#autoFill() rejected - session: " + this.id + " destroyed");
                return;
            }
            if (dataset.getAuthentication() == null) {
                if (z) {
                    this.mService.logDatasetSelected(dataset.getId(), this.id, this.mClientState);
                }
                autoFillApp(dataset);
                return;
            }
            this.mService.logDatasetAuthenticationSelected(dataset.getId(), this.id, this.mClientState);
            setViewStatesLocked(null, dataset, 64, false);
            Intent intentCreateAuthFillInIntentLocked = createAuthFillInIntentLocked(i, this.mClientState);
            if (intentCreateAuthFillInIntentLocked == null) {
                forceRemoveSelfLocked();
            } else {
                startAuthentication(AutofillManager.makeAuthenticationId(i, i2), dataset.getAuthentication(), intentCreateAuthFillInIntentLocked);
            }
        }
    }

    CharSequence getServiceName() {
        CharSequence serviceName;
        synchronized (this.mLock) {
            serviceName = this.mService.getServiceName();
        }
        return serviceName;
    }

    @GuardedBy("mLock")
    private Intent createAuthFillInIntentLocked(int i, Bundle bundle) {
        Intent intent = new Intent();
        FillContext fillContextByRequestIdLocked = getFillContextByRequestIdLocked(i);
        if (fillContextByRequestIdLocked == null) {
            wtf(null, "createAuthFillInIntentLocked(): no FillContext. requestId=%d; mContexts=%s", Integer.valueOf(i), this.mContexts);
            return null;
        }
        intent.putExtra("android.view.autofill.extra.ASSIST_STRUCTURE", fillContextByRequestIdLocked.getStructure());
        intent.putExtra("android.view.autofill.extra.CLIENT_STATE", bundle);
        return intent;
    }

    private void startAuthentication(int i, IntentSender intentSender, Intent intent) {
        try {
            synchronized (this.mLock) {
                this.mClient.authenticate(this.id, i, intentSender, intent);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error launching auth intent", e);
        }
    }

    public String toString() {
        return "Session: [id=" + this.id + ", component=" + this.mComponentName + "]";
    }

    @GuardedBy("mLock")
    void dumpLocked(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("id: ");
        printWriter.println(this.id);
        printWriter.print(str);
        printWriter.print("uid: ");
        printWriter.println(this.uid);
        printWriter.print(str);
        printWriter.print("flags: ");
        printWriter.println(this.mFlags);
        printWriter.print(str);
        printWriter.print("mComponentName: ");
        printWriter.println(this.mComponentName);
        printWriter.print(str);
        printWriter.print("mActivityToken: ");
        printWriter.println(this.mActivityToken);
        printWriter.print(str);
        printWriter.print("mStartTime: ");
        printWriter.println(this.mStartTime);
        printWriter.print(str);
        printWriter.print("Time to show UI: ");
        if (this.mUiShownTime == 0) {
            printWriter.println("N/A");
        } else {
            TimeUtils.formatDuration(this.mUiShownTime - this.mStartTime, printWriter);
            printWriter.println();
        }
        int size = this.mRequestLogs.size();
        printWriter.print(str);
        printWriter.print("mSessionLogs: ");
        printWriter.println(size);
        for (int i = 0; i < size; i++) {
            int iKeyAt = this.mRequestLogs.keyAt(i);
            LogMaker logMakerValueAt = this.mRequestLogs.valueAt(i);
            printWriter.print(str2);
            printWriter.print('#');
            printWriter.print(i);
            printWriter.print(": req=");
            printWriter.print(iKeyAt);
            printWriter.print(", log=");
            dumpRequestLog(printWriter, logMakerValueAt);
            printWriter.println();
        }
        printWriter.print(str);
        printWriter.print("mResponses: ");
        if (this.mResponses == null) {
            printWriter.println("null");
        } else {
            printWriter.println(this.mResponses.size());
            for (int i2 = 0; i2 < this.mResponses.size(); i2++) {
                printWriter.print(str2);
                printWriter.print('#');
                printWriter.print(i2);
                printWriter.print(' ');
                printWriter.println(this.mResponses.valueAt(i2));
            }
        }
        printWriter.print(str);
        printWriter.print("mCurrentViewId: ");
        printWriter.println(this.mCurrentViewId);
        printWriter.print(str);
        printWriter.print("mDestroyed: ");
        printWriter.println(this.mDestroyed);
        printWriter.print(str);
        printWriter.print("mIsSaving: ");
        printWriter.println(this.mIsSaving);
        printWriter.print(str);
        printWriter.print("mPendingSaveUi: ");
        printWriter.println(this.mPendingSaveUi);
        int size2 = this.mViewStates.size();
        printWriter.print(str);
        printWriter.print("mViewStates size: ");
        printWriter.println(this.mViewStates.size());
        for (int i3 = 0; i3 < size2; i3++) {
            printWriter.print(str);
            printWriter.print("ViewState at #");
            printWriter.println(i3);
            this.mViewStates.valueAt(i3).dump(str2, printWriter);
        }
        printWriter.print(str);
        printWriter.print("mContexts: ");
        if (this.mContexts != null) {
            int size3 = this.mContexts.size();
            for (int i4 = 0; i4 < size3; i4++) {
                FillContext fillContext = this.mContexts.get(i4);
                printWriter.print(str2);
                printWriter.print(fillContext);
                if (Helper.sVerbose) {
                    printWriter.println("AssistStructure dumped at logcat)");
                    fillContext.getStructure().dump(false);
                }
            }
        } else {
            printWriter.println("null");
        }
        printWriter.print(str);
        printWriter.print("mHasCallback: ");
        printWriter.println(this.mHasCallback);
        if (this.mClientState != null) {
            printWriter.print(str);
            printWriter.print("mClientState: ");
            printWriter.print(this.mClientState.getSize());
            printWriter.println(" bytes");
        }
        printWriter.print(str);
        printWriter.print("mCompatMode: ");
        printWriter.println(this.mCompatMode);
        printWriter.print(str);
        printWriter.print("mUrlBar: ");
        if (this.mUrlBar == null) {
            printWriter.println("N/A");
        } else {
            printWriter.print("id=");
            printWriter.print(this.mUrlBar.getAutofillId());
            printWriter.print(" domain=");
            printWriter.print(this.mUrlBar.getWebDomain());
            printWriter.print(" text=");
            Helper.printlnRedactedText(printWriter, this.mUrlBar.getText());
        }
        printWriter.print(str);
        printWriter.print("mSaveOnAllViewsInvisible: ");
        printWriter.println(this.mSaveOnAllViewsInvisible);
        printWriter.print(str);
        printWriter.print("mSelectedDatasetIds: ");
        printWriter.println(this.mSelectedDatasetIds);
        this.mRemoteFillService.dump(str, printWriter);
    }

    private static void dumpRequestLog(PrintWriter printWriter, LogMaker logMaker) {
        printWriter.print("CAT=");
        printWriter.print(logMaker.getCategory());
        printWriter.print(", TYPE=");
        int type = logMaker.getType();
        if (type != 2) {
            switch (type) {
                case 10:
                    printWriter.print("SUCCESS");
                    break;
                case 11:
                    printWriter.print("FAILURE");
                    break;
                default:
                    printWriter.print("UNSUPPORTED");
                    break;
            }
        } else {
            printWriter.print("CLOSE");
        }
        printWriter.print('(');
        printWriter.print(type);
        printWriter.print(')');
        printWriter.print(", PKG=");
        printWriter.print(logMaker.getPackageName());
        printWriter.print(", SERVICE=");
        printWriter.print(logMaker.getTaggedData(908));
        printWriter.print(", ORDINAL=");
        printWriter.print(logMaker.getTaggedData(1454));
        dumpNumericValue(printWriter, logMaker, "FLAGS", 1452);
        dumpNumericValue(printWriter, logMaker, "NUM_DATASETS", 909);
        dumpNumericValue(printWriter, logMaker, "UI_LATENCY", 1145);
        int numericValue = Helper.getNumericValue(logMaker, 1453);
        if (numericValue != 0) {
            printWriter.print(", AUTH_STATUS=");
            if (numericValue == 912) {
                printWriter.print("AUTHENTICATED");
            } else {
                switch (numericValue) {
                    case 1126:
                        printWriter.print("DATASET_AUTHENTICATED");
                        break;
                    case 1127:
                        printWriter.print("INVALID_DATASET_AUTHENTICATION");
                        break;
                    case 1128:
                        printWriter.print("INVALID_AUTHENTICATION");
                        break;
                    default:
                        printWriter.print("UNSUPPORTED");
                        break;
                }
            }
            printWriter.print('(');
            printWriter.print(numericValue);
            printWriter.print(')');
        }
        dumpNumericValue(printWriter, logMaker, "FC_IDS", 1271);
        dumpNumericValue(printWriter, logMaker, "COMPAT_MODE", 1414);
    }

    private static void dumpNumericValue(PrintWriter printWriter, LogMaker logMaker, String str, int i) {
        int numericValue = Helper.getNumericValue(logMaker, i);
        if (numericValue != 0) {
            printWriter.print(", ");
            printWriter.print(str);
            printWriter.print('=');
            printWriter.print(numericValue);
        }
    }

    void autoFillApp(Dataset dataset) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#autoFillApp() rejected - session: " + this.id + " destroyed");
                return;
            }
            try {
                int size = dataset.getFieldIds().size();
                ArrayList arrayList = new ArrayList(size);
                ArrayList arrayList2 = new ArrayList(size);
                boolean z = false;
                for (int i = 0; i < size; i++) {
                    if (dataset.getFieldValues().get(i) != null) {
                        AutofillId autofillId = (AutofillId) dataset.getFieldIds().get(i);
                        arrayList.add(autofillId);
                        arrayList2.add((AutofillValue) dataset.getFieldValues().get(i));
                        ViewState viewState = this.mViewStates.get(autofillId);
                        if (viewState != null && (viewState.getState() & 64) != 0) {
                            if (Helper.sVerbose) {
                                Slog.v(TAG, "autofillApp(): view " + autofillId + " waiting auth");
                            }
                            viewState.resetState(64);
                            z = true;
                        }
                    }
                }
                if (!arrayList.isEmpty()) {
                    if (z) {
                        this.mUi.hideFillUi(this);
                    }
                    if (Helper.sDebug) {
                        Slog.d(TAG, "autoFillApp(): the buck is on the app: " + dataset);
                    }
                    this.mClient.autofill(this.id, arrayList, arrayList2);
                    if (dataset.getId() != null) {
                        if (this.mSelectedDatasetIds == null) {
                            this.mSelectedDatasetIds = new ArrayList<>();
                        }
                        this.mSelectedDatasetIds.add(dataset.getId());
                    }
                    setViewStatesLocked(null, dataset, 4, false);
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Error autofilling activity: " + e);
            }
        }
    }

    private AutoFillUI getUiForShowing() {
        AutoFillUI autoFillUI;
        synchronized (this.mLock) {
            this.mUi.setCallback(this);
            autoFillUI = this.mUi;
        }
        return autoFillUI;
    }

    @GuardedBy("mLock")
    RemoteFillService destroyLocked() {
        if (this.mDestroyed) {
            return null;
        }
        unlinkClientVultureLocked();
        this.mUi.destroyAll(this.mPendingSaveUi, this, true);
        this.mUi.clearCallback(this);
        this.mDestroyed = true;
        int size = this.mRequestLogs.size();
        if (size > 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "destroyLocked(): logging " + size + " requests");
            }
            for (int i = 0; i < size; i++) {
                this.mMetricsLogger.write(this.mRequestLogs.valueAt(i));
            }
        }
        this.mMetricsLogger.write(newLogMaker(919).addTaggedData(1455, Integer.valueOf(size)));
        return this.mRemoteFillService;
    }

    @GuardedBy("mLock")
    void forceRemoveSelfLocked() {
        forceRemoveSelfLocked(0);
    }

    @GuardedBy("mLock")
    void forceRemoveSelfLocked(int i) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "forceRemoveSelfLocked(): " + this.mPendingSaveUi);
        }
        boolean zIsSaveUiPendingLocked = isSaveUiPendingLocked();
        this.mPendingSaveUi = null;
        removeSelfLocked();
        this.mUi.destroyAll(this.mPendingSaveUi, this, false);
        if (!zIsSaveUiPendingLocked) {
            try {
                this.mClient.setSessionFinished(i);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client to finish session", e);
            }
        }
    }

    private void removeSelf() {
        synchronized (this.mLock) {
            removeSelfLocked();
        }
    }

    @GuardedBy("mLock")
    void removeSelfLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "removeSelfLocked(): " + this.mPendingSaveUi);
        }
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#removeSelfLocked() rejected - session: " + this.id + " destroyed");
            return;
        }
        if (isSaveUiPendingLocked()) {
            Slog.i(TAG, "removeSelfLocked() ignored, waiting for pending save ui");
            return;
        }
        RemoteFillService remoteFillServiceDestroyLocked = destroyLocked();
        this.mService.removeSessionLocked(this.id);
        if (remoteFillServiceDestroyLocked != null) {
            remoteFillServiceDestroyLocked.destroy();
        }
    }

    void onPendingSaveUi(int i, IBinder iBinder) {
        getUiForShowing().onPendingSaveUi(i, iBinder);
    }

    @GuardedBy("mLock")
    boolean isSaveUiPendingForTokenLocked(IBinder iBinder) {
        return isSaveUiPendingLocked() && iBinder.equals(this.mPendingSaveUi.getToken());
    }

    @GuardedBy("mLock")
    private boolean isSaveUiPendingLocked() {
        return this.mPendingSaveUi != null && this.mPendingSaveUi.getState() == 2;
    }

    @GuardedBy("mLock")
    private int getLastResponseIndexLocked() {
        if (this.mResponses == null) {
            return -1;
        }
        int size = this.mResponses.size();
        int i = -1;
        for (int i2 = 0; i2 < size; i2++) {
            if (this.mResponses.keyAt(i2) > -1) {
                i = i2;
            }
        }
        return i;
    }

    private LogMaker newLogMaker(int i) {
        return newLogMaker(i, this.mService.getServicePackageName());
    }

    private LogMaker newLogMaker(int i, String str) {
        return Helper.newLogMaker(i, this.mComponentName, str, this.id, this.mCompatMode);
    }

    private void writeLog(int i) {
        this.mMetricsLogger.write(newLogMaker(i));
    }

    private void logAuthenticationStatusLocked(int i, int i2) {
        addTaggedDataToRequestLogLocked(i, 1453, Integer.valueOf(i2));
    }

    private void addTaggedDataToRequestLogLocked(int i, int i2, Object obj) {
        LogMaker logMaker = this.mRequestLogs.get(i);
        if (logMaker == null) {
            Slog.w(TAG, "addTaggedDataToRequestLogLocked(tag=" + i2 + "): no log for id " + i);
            return;
        }
        logMaker.addTaggedData(i2, obj);
    }

    private static String requestLogToString(LogMaker logMaker) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        dumpRequestLog(printWriter, logMaker);
        printWriter.flush();
        return stringWriter.toString();
    }

    private void wtf(Exception exc, String str, Object... objArr) {
        String str2 = String.format(str, objArr);
        this.mWtfHistory.log(str2);
        if (exc != null) {
            Slog.wtf(TAG, str2, exc);
        } else {
            Slog.wtf(TAG, str2);
        }
    }

    private static String actionAsString(int i) {
        switch (i) {
            case 1:
                return "START_SESSION";
            case 2:
                return "VIEW_ENTERED";
            case 3:
                return "VIEW_EXITED";
            case 4:
                return "VALUE_CHANGED";
            default:
                return "UNKNOWN_" + i;
        }
    }
}
