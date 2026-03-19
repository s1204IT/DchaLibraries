package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.service.chooser.IChooserTargetResult;
import android.service.chooser.IChooserTargetService;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Space;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.ResolverActivity;
import com.android.internal.logging.MetricsLogger;
import com.google.android.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChooserActivity extends ResolverActivity {
    private static final float CALLER_TARGET_SCORE_BOOST = 900.0f;
    private static final int CHOOSER_TARGET_SERVICE_RESULT = 1;
    private static final int CHOOSER_TARGET_SERVICE_WATCHDOG_TIMEOUT = 2;
    private static final boolean DEBUG = false;
    public static final String EXTRA_PRIVATE_RETAIN_IN_ON_STOP = "com.android.internal.app.ChooserActivity.EXTRA_PRIVATE_RETAIN_IN_ON_STOP";
    private static final String PINNED_SHARED_PREFS_NAME = "chooser_pin_settings";
    private static final float PINNED_TARGET_SCORE_BOOST = 1000.0f;
    private static final int QUERY_TARGET_SERVICE_LIMIT = 5;
    private static final String TAG = "ChooserActivity";
    private static final String TARGET_DETAILS_FRAGMENT_TAG = "targetDetailsFragment";
    private static final int WATCHDOG_TIMEOUT_MILLIS = 2000;
    private ChooserTarget[] mCallerChooserTargets;
    private ChooserListAdapter mChooserListAdapter;
    private ChooserRowAdapter mChooserRowAdapter;
    private long mChooserShownTime;
    private IntentSender mChosenComponentSender;
    private ComponentName[] mFilteredComponentNames;
    protected boolean mIsSuccessfullySelected;
    private SharedPreferences mPinnedSharedPrefs;
    private Intent mReferrerFillInIntent;
    private IntentSender mRefinementIntentSender;
    private RefinementResultReceiver mRefinementResultReceiver;
    private Bundle mReplacementExtras;
    private final List<ChooserTargetServiceConnection> mServiceConnections = new ArrayList();
    private final Handler mChooserHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (!ChooserActivity.this.isDestroyed()) {
                        ServiceResultInfo serviceResultInfo = (ServiceResultInfo) message.obj;
                        if (!ChooserActivity.this.mServiceConnections.contains(serviceResultInfo.connection)) {
                            Log.w(ChooserActivity.TAG, "ChooserTargetServiceConnection " + serviceResultInfo.connection + " returned after being removed from active connections. Have you considered returning results faster?");
                        } else {
                            if (serviceResultInfo.resultTargets != null) {
                                ChooserActivity.this.mChooserListAdapter.addServiceResults(serviceResultInfo.originalTarget, serviceResultInfo.resultTargets);
                            }
                            ChooserActivity.this.unbindService(serviceResultInfo.connection);
                            serviceResultInfo.connection.destroy();
                            ChooserActivity.this.mServiceConnections.remove(serviceResultInfo.connection);
                            if (ChooserActivity.this.mServiceConnections.isEmpty()) {
                                ChooserActivity.this.mChooserHandler.removeMessages(2);
                                ChooserActivity.this.sendVoiceChoicesIfNeeded();
                                ChooserActivity.this.mChooserListAdapter.setShowServiceTargets(true);
                            }
                        }
                        break;
                    }
                    break;
                case 2:
                    ChooserActivity.this.unbindRemainingServices();
                    ChooserActivity.this.sendVoiceChoicesIfNeeded();
                    ChooserActivity.this.mChooserListAdapter.setShowServiceTargets(true);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        int i;
        Intent[] intentArr;
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mIsSuccessfullySelected = false;
        Intent intent = getIntent();
        Parcelable parcelableExtra = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (!(parcelableExtra instanceof Intent)) {
            Log.w(TAG, "Target is not an intent: " + parcelableExtra);
            finish();
            super.onCreate(null);
            return;
        }
        Intent intent2 = (Intent) parcelableExtra;
        if (intent2 != null) {
            modifyTargetIntent(intent2);
        }
        Parcelable[] parcelableArrayExtra = intent.getParcelableArrayExtra(Intent.EXTRA_ALTERNATE_INTENTS);
        if (parcelableArrayExtra != null) {
            boolean z = intent2 == null;
            Intent[] intentArr2 = new Intent[z ? parcelableArrayExtra.length - 1 : parcelableArrayExtra.length];
            Intent intent3 = intent2;
            for (int i2 = 0; i2 < parcelableArrayExtra.length; i2++) {
                if (!(parcelableArrayExtra[i2] instanceof Intent)) {
                    Log.w(TAG, "EXTRA_ALTERNATE_INTENTS array entry #" + i2 + " is not an Intent: " + parcelableArrayExtra[i2]);
                    finish();
                    super.onCreate(null);
                    return;
                }
                Intent intent4 = (Intent) parcelableArrayExtra[i2];
                if (i2 == 0 && intent3 == null) {
                    modifyTargetIntent(intent4);
                    intent3 = intent4;
                } else {
                    intentArr2[z ? i2 - 1 : i2] = intent4;
                    modifyTargetIntent(intent4);
                }
            }
            setAdditionalTargets(intentArr2);
            intent2 = intent3;
        }
        this.mReplacementExtras = intent.getBundleExtra(Intent.EXTRA_REPLACEMENT_EXTRAS);
        CharSequence charSequenceExtra = intent.getCharSequenceExtra(Intent.EXTRA_TITLE);
        if (charSequenceExtra == null) {
            i = R.string.chooseActivity;
        } else {
            i = 0;
        }
        Parcelable[] parcelableArrayExtra2 = intent.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        if (parcelableArrayExtra2 != null) {
            intentArr = new Intent[parcelableArrayExtra2.length];
            for (int i3 = 0; i3 < parcelableArrayExtra2.length; i3++) {
                if (!(parcelableArrayExtra2[i3] instanceof Intent)) {
                    Log.w(TAG, "Initial intent #" + i3 + " not an Intent: " + parcelableArrayExtra2[i3]);
                    finish();
                    super.onCreate(null);
                    return;
                }
                Intent intent5 = (Intent) parcelableArrayExtra2[i3];
                modifyTargetIntent(intent5);
                intentArr[i3] = intent5;
            }
        } else {
            intentArr = null;
        }
        this.mReferrerFillInIntent = new Intent().putExtra(Intent.EXTRA_REFERRER, getReferrer());
        this.mChosenComponentSender = (IntentSender) intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER);
        this.mRefinementIntentSender = (IntentSender) intent.getParcelableExtra(Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER);
        setSafeForwardingMode(true);
        Parcelable[] parcelableArrayExtra3 = intent.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS);
        if (parcelableArrayExtra3 != null) {
            ComponentName[] componentNameArr = new ComponentName[parcelableArrayExtra3.length];
            int i4 = 0;
            while (true) {
                if (i4 >= parcelableArrayExtra3.length) {
                    break;
                }
                if (!(parcelableArrayExtra3[i4] instanceof ComponentName)) {
                    Log.w(TAG, "Filtered component #" + i4 + " not a ComponentName: " + parcelableArrayExtra3[i4]);
                    componentNameArr = null;
                    break;
                }
                componentNameArr[i4] = (ComponentName) parcelableArrayExtra3[i4];
                i4++;
            }
            this.mFilteredComponentNames = componentNameArr;
        }
        Parcelable[] parcelableArrayExtra4 = intent.getParcelableArrayExtra(Intent.EXTRA_CHOOSER_TARGETS);
        if (parcelableArrayExtra4 != null) {
            ChooserTarget[] chooserTargetArr = new ChooserTarget[parcelableArrayExtra4.length];
            int i5 = 0;
            while (true) {
                if (i5 >= parcelableArrayExtra4.length) {
                    break;
                }
                if (!(parcelableArrayExtra4[i5] instanceof ChooserTarget)) {
                    Log.w(TAG, "Chooser target #" + i5 + " not a ChooserTarget: " + parcelableArrayExtra4[i5]);
                    chooserTargetArr = null;
                    break;
                }
                chooserTargetArr[i5] = (ChooserTarget) parcelableArrayExtra4[i5];
                i5++;
            }
            this.mCallerChooserTargets = chooserTargetArr;
        }
        this.mPinnedSharedPrefs = getPinnedSharedPrefs(this);
        setRetainInOnStop(intent.getBooleanExtra(EXTRA_PRIVATE_RETAIN_IN_ON_STOP, false));
        super.onCreate(bundle, intent2, charSequenceExtra, i, intentArr, null, false);
        MetricsLogger.action(this, 214);
        this.mChooserShownTime = System.currentTimeMillis();
        MetricsLogger.histogram(null, "system_cost_for_smart_sharing", (int) (this.mChooserShownTime - jCurrentTimeMillis));
    }

    static SharedPreferences getPinnedSharedPrefs(Context context) {
        return context.getSharedPreferences(new File(new File(Environment.getDataUserCePackageDirectory(StorageManager.UUID_PRIVATE_INTERNAL, context.getUserId(), context.getPackageName()), "shared_prefs"), "chooser_pin_settings.xml"), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mRefinementResultReceiver != null) {
            this.mRefinementResultReceiver.destroy();
            this.mRefinementResultReceiver = null;
        }
        unbindRemainingServices();
        this.mChooserHandler.removeMessages(1);
    }

    @Override
    public Intent getReplacementIntent(ActivityInfo activityInfo, Intent intent) {
        Bundle bundle;
        if (this.mReplacementExtras != null && (bundle = this.mReplacementExtras.getBundle(activityInfo.packageName)) != null) {
            Intent intent2 = new Intent(intent);
            intent2.putExtras(bundle);
            intent = intent2;
        }
        if (activityInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_PARENT) || activityInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            Intent intentCreateChooser = Intent.createChooser(intent, getIntent().getCharSequenceExtra(Intent.EXTRA_TITLE));
            intentCreateChooser.putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
            return intentCreateChooser;
        }
        return intent;
    }

    @Override
    public void onActivityStarted(ResolverActivity.TargetInfo targetInfo) {
        ComponentName resolvedComponentName;
        if (this.mChosenComponentSender != null && (resolvedComponentName = targetInfo.getResolvedComponentName()) != null) {
            try {
                this.mChosenComponentSender.sendIntent(this, -1, new Intent().putExtra(Intent.EXTRA_CHOSEN_COMPONENT, resolvedComponentName), null, null);
            } catch (IntentSender.SendIntentException e) {
                Slog.e(TAG, "Unable to launch supplied IntentSender to report the chosen component: " + e);
            }
        }
    }

    @Override
    public void onPrepareAdapterView(AbsListView absListView, ResolverActivity.ResolveListAdapter resolveListAdapter) {
        ListView listView = absListView instanceof ListView ? (ListView) absListView : null;
        this.mChooserListAdapter = (ChooserListAdapter) resolveListAdapter;
        if (this.mCallerChooserTargets != null && this.mCallerChooserTargets.length > 0) {
            this.mChooserListAdapter.addServiceResults(null, Lists.newArrayList(this.mCallerChooserTargets));
        }
        this.mChooserRowAdapter = new ChooserRowAdapter(this.mChooserListAdapter);
        this.mChooserRowAdapter.registerDataSetObserver(new OffsetDataSetObserver(absListView));
        absListView.setAdapter((ListAdapter) this.mChooserRowAdapter);
        if (listView != null) {
            listView.setItemsCanFocus(true);
        }
    }

    @Override
    public int getLayoutResource() {
        return R.layout.chooser_grid;
    }

    @Override
    public boolean shouldGetActivityMetadata() {
        return true;
    }

    @Override
    public boolean shouldAutoLaunchSingleChoice(ResolverActivity.TargetInfo targetInfo) {
        return getIntent().getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, super.shouldAutoLaunchSingleChoice(targetInfo));
    }

    @Override
    public void showTargetDetails(ResolveInfo resolveInfo) {
        if (resolveInfo == null) {
            return;
        }
        ComponentName componentName = resolveInfo.activityInfo.getComponentName();
        new ResolverTargetActionsDialogFragment(resolveInfo.loadLabel(getPackageManager()), componentName, this.mPinnedSharedPrefs.getBoolean(componentName.flattenToString(), false)).show(getFragmentManager(), TARGET_DETAILS_FRAGMENT_TAG);
    }

    private void modifyTargetIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            intent.addFlags(134742016);
        }
    }

    @Override
    protected boolean onTargetSelected(ResolverActivity.TargetInfo targetInfo, boolean z) {
        if (this.mRefinementIntentSender != null) {
            Intent intent = new Intent();
            List<Intent> allSourceIntents = targetInfo.getAllSourceIntents();
            if (!allSourceIntents.isEmpty()) {
                intent.putExtra(Intent.EXTRA_INTENT, allSourceIntents.get(0));
                if (allSourceIntents.size() > 1) {
                    Intent[] intentArr = new Intent[allSourceIntents.size() - 1];
                    int size = allSourceIntents.size();
                    for (int i = 1; i < size; i++) {
                        intentArr[i - 1] = allSourceIntents.get(i);
                    }
                    intent.putExtra(Intent.EXTRA_ALTERNATE_INTENTS, intentArr);
                }
                if (this.mRefinementResultReceiver != null) {
                    this.mRefinementResultReceiver.destroy();
                }
                this.mRefinementResultReceiver = new RefinementResultReceiver(this, targetInfo, null);
                intent.putExtra(Intent.EXTRA_RESULT_RECEIVER, this.mRefinementResultReceiver);
                try {
                    this.mRefinementIntentSender.sendIntent(this, 0, intent, null, null);
                    return false;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Refinement IntentSender failed to send", e);
                }
            }
        }
        updateModelAndChooserCounts(targetInfo);
        return super.onTargetSelected(targetInfo, z);
    }

    @Override
    public void startSelected(int i, boolean z, boolean z2) {
        long jCurrentTimeMillis = System.currentTimeMillis() - this.mChooserShownTime;
        super.startSelected(i, z, z2);
        if (this.mChooserListAdapter != null) {
            int i2 = 0;
            switch (this.mChooserListAdapter.getPositionTargetType(i)) {
                case 0:
                    i2 = 215;
                    break;
                case 1:
                    i2 = 216;
                    i -= this.mChooserListAdapter.getCallerTargetCount();
                    break;
                case 2:
                    i2 = 217;
                    i -= this.mChooserListAdapter.getCallerTargetCount() + this.mChooserListAdapter.getServiceTargetCount();
                    break;
            }
            if (i2 != 0) {
                MetricsLogger.action(this, i2, i);
            }
            if (this.mIsSuccessfullySelected) {
                MetricsLogger.histogram(null, "user_selection_cost_for_smart_sharing", (int) jCurrentTimeMillis);
                MetricsLogger.histogram(null, "app_position_for_smart_sharing", i);
            }
        }
    }

    void queryTargetServices(ChooserListAdapter chooserListAdapter) {
        String strConvertServiceName;
        PackageManager packageManager = getPackageManager();
        int displayResolveInfoCount = chooserListAdapter.getDisplayResolveInfoCount();
        int i = 0;
        for (int i2 = 0; i2 < displayResolveInfoCount; i2++) {
            ResolverActivity.DisplayResolveInfo displayResolveInfo = chooserListAdapter.getDisplayResolveInfo(i2);
            if (chooserListAdapter.getScore(displayResolveInfo) != 0.0f) {
                ActivityInfo activityInfo = displayResolveInfo.getResolveInfo().activityInfo;
                Bundle bundle = activityInfo.metaData;
                if (bundle != null) {
                    strConvertServiceName = convertServiceName(activityInfo.packageName, bundle.getString(ChooserTargetService.META_DATA_NAME));
                } else {
                    strConvertServiceName = null;
                }
                if (strConvertServiceName != null) {
                    ComponentName componentName = new ComponentName(activityInfo.packageName, strConvertServiceName);
                    Intent component = new Intent(ChooserTargetService.SERVICE_INTERFACE).setComponent(componentName);
                    try {
                        if (!"android.permission.BIND_CHOOSER_TARGET_SERVICE".equals(packageManager.getServiceInfo(componentName, 0).permission)) {
                            Log.w(TAG, "ChooserTargetService " + componentName + " does not require permission android.permission.BIND_CHOOSER_TARGET_SERVICE - this service will not be queried for ChooserTargets. add android:permission=\"android.permission.BIND_CHOOSER_TARGET_SERVICE\" to the <service> tag for " + componentName + " in the manifest.");
                        } else {
                            ChooserTargetServiceConnection chooserTargetServiceConnection = new ChooserTargetServiceConnection(this, displayResolveInfo);
                            if (bindServiceAsUser(component, chooserTargetServiceConnection, 5, Process.myUserHandle())) {
                                this.mServiceConnections.add(chooserTargetServiceConnection);
                                i++;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Could not look up service " + componentName + "; component name not found");
                    }
                }
                if (i >= 5) {
                    break;
                }
            }
        }
        if (!this.mServiceConnections.isEmpty()) {
            this.mChooserHandler.sendEmptyMessageDelayed(2, 2000L);
        } else {
            sendVoiceChoicesIfNeeded();
        }
    }

    private String convertServiceName(String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return null;
        }
        if (str2.startsWith(".")) {
            return str + str2;
        }
        if (str2.indexOf(46) >= 0) {
            return str2;
        }
        return null;
    }

    void unbindRemainingServices() {
        int size = this.mServiceConnections.size();
        for (int i = 0; i < size; i++) {
            ChooserTargetServiceConnection chooserTargetServiceConnection = this.mServiceConnections.get(i);
            unbindService(chooserTargetServiceConnection);
            chooserTargetServiceConnection.destroy();
        }
        this.mServiceConnections.clear();
        this.mChooserHandler.removeMessages(2);
    }

    @Override
    public void onSetupVoiceInteraction() {
    }

    void updateModelAndChooserCounts(ResolverActivity.TargetInfo targetInfo) {
        if (targetInfo != null) {
            ResolveInfo resolveInfo = targetInfo.getResolveInfo();
            Intent targetIntent = getTargetIntent();
            if (resolveInfo != null && resolveInfo.activityInfo != null && targetIntent != null && this.mAdapter != null) {
                this.mAdapter.updateModel(targetInfo.getResolvedComponentName());
                this.mAdapter.updateChooserCounts(resolveInfo.activityInfo.packageName, getUserId(), targetIntent.getAction());
            }
        }
        this.mIsSuccessfullySelected = true;
    }

    void onRefinementResult(ResolverActivity.TargetInfo targetInfo, Intent intent) {
        if (this.mRefinementResultReceiver != null) {
            this.mRefinementResultReceiver.destroy();
            this.mRefinementResultReceiver = null;
        }
        if (targetInfo == null) {
            Log.e(TAG, "Refinement result intent did not match any known targets; canceling");
        } else if (!checkTargetSourceIntent(targetInfo, intent)) {
            Log.e(TAG, "onRefinementResult: Selected target " + targetInfo + " cannot match refined source intent " + intent);
        } else {
            ResolverActivity.TargetInfo targetInfoCloneFilledIn = targetInfo.cloneFilledIn(intent, 0);
            if (super.onTargetSelected(targetInfoCloneFilledIn, false)) {
                updateModelAndChooserCounts(targetInfoCloneFilledIn);
                finish();
                return;
            }
        }
        onRefinementCanceled();
    }

    void onRefinementCanceled() {
        if (this.mRefinementResultReceiver != null) {
            this.mRefinementResultReceiver.destroy();
            this.mRefinementResultReceiver = null;
        }
        finish();
    }

    boolean checkTargetSourceIntent(ResolverActivity.TargetInfo targetInfo, Intent intent) {
        List<Intent> allSourceIntents = targetInfo.getAllSourceIntents();
        int size = allSourceIntents.size();
        for (int i = 0; i < size; i++) {
            if (allSourceIntents.get(i).filterEquals(intent)) {
                return true;
            }
        }
        return false;
    }

    void filterServiceTargets(String str, List<ChooserTarget> list) {
        if (list == null) {
            return;
        }
        PackageManager packageManager = getPackageManager();
        for (int size = list.size() - 1; size >= 0; size--) {
            ChooserTarget chooserTarget = list.get(size);
            ComponentName componentName = chooserTarget.getComponentName();
            if (str == null || !str.equals(componentName.getPackageName())) {
                boolean z = false;
                try {
                    ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
                    if (!activityInfo.exported || activityInfo.permission != null) {
                        z = true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Target " + chooserTarget + " returned by " + str + " component not found");
                    z = true;
                }
                if (z) {
                    list.remove(size);
                }
            }
        }
    }

    public class ChooserListController extends ResolverListController {
        public ChooserListController(Context context, PackageManager packageManager, Intent intent, String str, int i) {
            super(context, packageManager, intent, str, i);
        }

        @Override
        boolean isComponentPinned(ComponentName componentName) {
            return ChooserActivity.this.mPinnedSharedPrefs.getBoolean(componentName.flattenToString(), false);
        }

        @Override
        boolean isComponentFiltered(ComponentName componentName) {
            if (ChooserActivity.this.mFilteredComponentNames == null) {
                return false;
            }
            for (ComponentName componentName2 : ChooserActivity.this.mFilteredComponentNames) {
                if (componentName.equals(componentName2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public float getScore(ResolverActivity.DisplayResolveInfo displayResolveInfo) {
            if (displayResolveInfo == null) {
                return ChooserActivity.CALLER_TARGET_SCORE_BOOST;
            }
            float score = super.getScore(displayResolveInfo);
            if (displayResolveInfo.isPinned()) {
                return score + ChooserActivity.PINNED_TARGET_SCORE_BOOST;
            }
            return score;
        }
    }

    @Override
    public ResolverActivity.ResolveListAdapter createAdapter(Context context, List<Intent> list, Intent[] intentArr, List<ResolveInfo> list2, int i, boolean z) {
        return new ChooserListAdapter(context, list, intentArr, list2, i, z, createListController());
    }

    @Override
    @VisibleForTesting
    protected ResolverListController createListController() {
        return new ChooserListController(this, this.mPm, getTargetIntent(), getReferrerPackageName(), this.mLaunchedFromUid);
    }

    final class ChooserTargetInfo implements ResolverActivity.TargetInfo {
        private final ResolveInfo mBackupResolveInfo;
        private CharSequence mBadgeContentDescription;
        private Drawable mBadgeIcon;
        private final ChooserTarget mChooserTarget;
        private Drawable mDisplayIcon;
        private final int mFillInFlags;
        private final Intent mFillInIntent;
        private final float mModifiedScore;
        private final ResolverActivity.DisplayResolveInfo mSourceInfo;

        public ChooserTargetInfo(ResolverActivity.DisplayResolveInfo displayResolveInfo, ChooserTarget chooserTarget, float f) {
            ResolveInfo resolveInfo;
            ActivityInfo activityInfo;
            this.mBadgeIcon = null;
            this.mSourceInfo = displayResolveInfo;
            this.mChooserTarget = chooserTarget;
            this.mModifiedScore = f;
            if (displayResolveInfo != null && (resolveInfo = displayResolveInfo.getResolveInfo()) != null && (activityInfo = resolveInfo.activityInfo) != null && activityInfo.applicationInfo != null) {
                PackageManager packageManager = ChooserActivity.this.getPackageManager();
                this.mBadgeIcon = packageManager.getApplicationIcon(activityInfo.applicationInfo);
                this.mBadgeContentDescription = packageManager.getApplicationLabel(activityInfo.applicationInfo);
            }
            Icon icon = chooserTarget.getIcon();
            this.mDisplayIcon = icon != null ? icon.loadDrawable(ChooserActivity.this) : null;
            if (displayResolveInfo == null) {
                this.mBackupResolveInfo = ChooserActivity.this.getPackageManager().resolveActivity(getResolvedIntent(), 0);
            } else {
                this.mBackupResolveInfo = null;
            }
            this.mFillInIntent = null;
            this.mFillInFlags = 0;
        }

        private ChooserTargetInfo(ChooserTargetInfo chooserTargetInfo, Intent intent, int i) {
            this.mBadgeIcon = null;
            this.mSourceInfo = chooserTargetInfo.mSourceInfo;
            this.mBackupResolveInfo = chooserTargetInfo.mBackupResolveInfo;
            this.mChooserTarget = chooserTargetInfo.mChooserTarget;
            this.mBadgeIcon = chooserTargetInfo.mBadgeIcon;
            this.mBadgeContentDescription = chooserTargetInfo.mBadgeContentDescription;
            this.mDisplayIcon = chooserTargetInfo.mDisplayIcon;
            this.mFillInIntent = intent;
            this.mFillInFlags = i;
            this.mModifiedScore = chooserTargetInfo.mModifiedScore;
        }

        public float getModifiedScore() {
            return this.mModifiedScore;
        }

        @Override
        public Intent getResolvedIntent() {
            if (this.mSourceInfo != null) {
                return this.mSourceInfo.getResolvedIntent();
            }
            Intent intent = new Intent(ChooserActivity.this.getTargetIntent());
            intent.setComponent(this.mChooserTarget.getComponentName());
            intent.putExtras(this.mChooserTarget.getIntentExtras());
            return intent;
        }

        @Override
        public ComponentName getResolvedComponentName() {
            if (this.mSourceInfo != null) {
                return this.mSourceInfo.getResolvedComponentName();
            }
            if (this.mBackupResolveInfo != null) {
                return new ComponentName(this.mBackupResolveInfo.activityInfo.packageName, this.mBackupResolveInfo.activityInfo.name);
            }
            return null;
        }

        private Intent getBaseIntentToSend() {
            Intent resolvedIntent = getResolvedIntent();
            if (resolvedIntent == null) {
                Log.e(ChooserActivity.TAG, "ChooserTargetInfo: no base intent available to send");
                return resolvedIntent;
            }
            Intent intent = new Intent(resolvedIntent);
            if (this.mFillInIntent != null) {
                intent.fillIn(this.mFillInIntent, this.mFillInFlags);
            }
            intent.fillIn(ChooserActivity.this.mReferrerFillInIntent, 0);
            return intent;
        }

        @Override
        public boolean start(Activity activity, Bundle bundle) {
            throw new RuntimeException("ChooserTargets should be started as caller.");
        }

        @Override
        public boolean startAsCaller(Activity activity, Bundle bundle, int i) {
            Intent baseIntentToSend = getBaseIntentToSend();
            boolean z = false;
            if (baseIntentToSend == null) {
                return false;
            }
            baseIntentToSend.setComponent(this.mChooserTarget.getComponentName());
            baseIntentToSend.putExtras(this.mChooserTarget.getIntentExtras());
            if (this.mSourceInfo != null && this.mSourceInfo.getResolvedComponentName().getPackageName().equals(this.mChooserTarget.getComponentName().getPackageName())) {
                z = true;
            }
            activity.startActivityAsCaller(baseIntentToSend, bundle, z, i);
            return true;
        }

        @Override
        public boolean startAsUser(Activity activity, Bundle bundle, UserHandle userHandle) {
            throw new RuntimeException("ChooserTargets should be started as caller.");
        }

        @Override
        public ResolveInfo getResolveInfo() {
            return this.mSourceInfo != null ? this.mSourceInfo.getResolveInfo() : this.mBackupResolveInfo;
        }

        @Override
        public CharSequence getDisplayLabel() {
            return this.mChooserTarget.getTitle();
        }

        @Override
        public CharSequence getExtendedInfo() {
            return null;
        }

        @Override
        public Drawable getDisplayIcon() {
            return this.mDisplayIcon;
        }

        @Override
        public Drawable getBadgeIcon() {
            return this.mBadgeIcon;
        }

        @Override
        public CharSequence getBadgeContentDescription() {
            return this.mBadgeContentDescription;
        }

        @Override
        public ResolverActivity.TargetInfo cloneFilledIn(Intent intent, int i) {
            return ChooserActivity.this.new ChooserTargetInfo(this, intent, i);
        }

        @Override
        public List<Intent> getAllSourceIntents() {
            ArrayList arrayList = new ArrayList();
            if (this.mSourceInfo != null) {
                arrayList.add(this.mSourceInfo.getAllSourceIntents().get(0));
            }
            return arrayList;
        }

        @Override
        public boolean isPinned() {
            if (this.mSourceInfo != null) {
                return this.mSourceInfo.isPinned();
            }
            return false;
        }
    }

    public class ChooserListAdapter extends ResolverActivity.ResolveListAdapter {
        private static final int MAX_SERVICE_TARGETS = 4;
        private static final int MAX_TARGETS_PER_SERVICE = 2;
        public static final int TARGET_BAD = -1;
        public static final int TARGET_CALLER = 0;
        public static final int TARGET_SERVICE = 1;
        public static final int TARGET_STANDARD = 2;
        private final BaseChooserTargetComparator mBaseTargetComparator;
        private final List<ResolverActivity.TargetInfo> mCallerTargets;
        private float mLateFee;
        private final List<ChooserTargetInfo> mServiceTargets;
        private boolean mShowServiceTargets;
        private boolean mTargetsNeedPruning;

        public ChooserListAdapter(Context context, List<Intent> list, Intent[] intentArr, List<ResolveInfo> list2, int i, boolean z, ResolverListController resolverListController) {
            ActivityInfo activityInfo;
            ResolveInfo resolveInfo;
            ResolveInfo resolveInfo2;
            super(context, list, null, list2, i, z, resolverListController);
            this.mServiceTargets = new ArrayList();
            this.mCallerTargets = new ArrayList();
            this.mLateFee = 1.0f;
            this.mTargetsNeedPruning = false;
            this.mBaseTargetComparator = new BaseChooserTargetComparator();
            if (intentArr != null) {
                PackageManager packageManager = ChooserActivity.this.getPackageManager();
                for (Intent intent : intentArr) {
                    if (intent != null) {
                        if (intent.getComponent() != null) {
                            try {
                                activityInfo = packageManager.getActivityInfo(intent.getComponent(), 0);
                                try {
                                    resolveInfo = new ResolveInfo();
                                    try {
                                        resolveInfo.activityInfo = activityInfo;
                                    } catch (PackageManager.NameNotFoundException e) {
                                    }
                                } catch (PackageManager.NameNotFoundException e2) {
                                    resolveInfo = null;
                                }
                            } catch (PackageManager.NameNotFoundException e3) {
                                activityInfo = null;
                                resolveInfo = null;
                            }
                        } else {
                            activityInfo = null;
                            resolveInfo = null;
                        }
                        if (activityInfo == null) {
                            ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(intent, 65536);
                            activityInfo = resolveInfoResolveActivity != null ? resolveInfoResolveActivity.activityInfo : null;
                            resolveInfo2 = resolveInfoResolveActivity;
                        } else {
                            resolveInfo2 = resolveInfo;
                        }
                        if (activityInfo == null) {
                            Log.w(ChooserActivity.TAG, "No activity found for " + intent);
                        } else {
                            UserManager userManager = (UserManager) ChooserActivity.this.getSystemService("user");
                            if (intent instanceof LabeledIntent) {
                                LabeledIntent labeledIntent = (LabeledIntent) intent;
                                resolveInfo2.resolvePackageName = labeledIntent.getSourcePackage();
                                resolveInfo2.labelRes = labeledIntent.getLabelResource();
                                resolveInfo2.nonLocalizedLabel = labeledIntent.getNonLocalizedLabel();
                                resolveInfo2.icon = labeledIntent.getIconResource();
                                resolveInfo2.iconResourceId = resolveInfo2.icon;
                            }
                            if (userManager.isManagedProfile()) {
                                resolveInfo2.noResourceId = true;
                                resolveInfo2.icon = 0;
                            }
                            this.mCallerTargets.add(new ResolverActivity.DisplayResolveInfo(intent, resolveInfo2, resolveInfo2.loadLabel(packageManager), null, intent));
                        }
                    }
                }
            }
        }

        @Override
        public boolean showsExtendedInfo(ResolverActivity.TargetInfo targetInfo) {
            return false;
        }

        @Override
        public boolean isComponentPinned(ComponentName componentName) {
            return ChooserActivity.this.mPinnedSharedPrefs.getBoolean(componentName.flattenToString(), false);
        }

        @Override
        public View onCreateView(ViewGroup viewGroup) {
            return this.mInflater.inflate(R.layout.resolve_grid_item, viewGroup, false);
        }

        @Override
        public void onListRebuilt() {
            if (ActivityManager.isLowRamDeviceStatic()) {
                return;
            }
            if (this.mServiceTargets != null && getDisplayInfoCount() == 0) {
                this.mTargetsNeedPruning = true;
            }
            ChooserActivity.this.queryTargetServices(this);
        }

        @Override
        public boolean shouldGetResolvedFilter() {
            return true;
        }

        @Override
        public int getCount() {
            return super.getCount() + getServiceTargetCount() + getCallerTargetCount();
        }

        @Override
        public int getUnfilteredCount() {
            return super.getUnfilteredCount() + getServiceTargetCount() + getCallerTargetCount();
        }

        public int getCallerTargetCount() {
            return this.mCallerTargets.size();
        }

        public int getServiceTargetCount() {
            if (!this.mShowServiceTargets) {
                return 0;
            }
            return Math.min(this.mServiceTargets.size(), 4);
        }

        public int getStandardTargetCount() {
            return super.getCount();
        }

        public int getPositionTargetType(int i) {
            int callerTargetCount = getCallerTargetCount();
            if (i < callerTargetCount) {
                return 0;
            }
            int i2 = 0 + callerTargetCount;
            int serviceTargetCount = getServiceTargetCount();
            if (i - i2 < serviceTargetCount) {
                return 1;
            }
            if (i - (i2 + serviceTargetCount) < super.getCount()) {
                return 2;
            }
            return -1;
        }

        @Override
        public ResolverActivity.TargetInfo getItem(int i) {
            return targetInfoForPosition(i, true);
        }

        @Override
        public ResolverActivity.TargetInfo targetInfoForPosition(int i, boolean z) {
            int callerTargetCount = getCallerTargetCount();
            if (i < callerTargetCount) {
                return this.mCallerTargets.get(i);
            }
            int i2 = 0 + callerTargetCount;
            int serviceTargetCount = getServiceTargetCount();
            int i3 = i - i2;
            if (i3 < serviceTargetCount) {
                return this.mServiceTargets.get(i3);
            }
            int i4 = i2 + serviceTargetCount;
            return z ? super.getItem(i - i4) : getDisplayInfoAt(i - i4);
        }

        public void addServiceResults(ResolverActivity.DisplayResolveInfo displayResolveInfo, List<ChooserTarget> list) {
            if (this.mTargetsNeedPruning && list.size() > 0) {
                this.mServiceTargets.clear();
                this.mTargetsNeedPruning = false;
            }
            float score = getScore(displayResolveInfo);
            Collections.sort(list, this.mBaseTargetComparator);
            float f = 0.0f;
            int iMin = Math.min(list.size(), 2);
            for (int i = 0; i < iMin; i++) {
                ChooserTarget chooserTarget = list.get(i);
                float score2 = chooserTarget.getScore() * score * this.mLateFee;
                if (i > 0 && score2 >= f) {
                    f *= 0.95f;
                } else {
                    f = score2;
                }
                insertServiceTarget(ChooserActivity.this.new ChooserTargetInfo(displayResolveInfo, chooserTarget, f));
            }
            this.mLateFee *= 0.95f;
            notifyDataSetChanged();
        }

        public void setShowServiceTargets(boolean z) {
            if (z != this.mShowServiceTargets) {
                this.mShowServiceTargets = z;
                notifyDataSetChanged();
            }
        }

        private void insertServiceTarget(ChooserTargetInfo chooserTargetInfo) {
            float modifiedScore = chooserTargetInfo.getModifiedScore();
            int size = this.mServiceTargets.size();
            for (int i = 0; i < size; i++) {
                if (modifiedScore > this.mServiceTargets.get(i).getModifiedScore()) {
                    this.mServiceTargets.add(i, chooserTargetInfo);
                    return;
                }
            }
            this.mServiceTargets.add(chooserTargetInfo);
        }
    }

    static class BaseChooserTargetComparator implements Comparator<ChooserTarget> {
        BaseChooserTargetComparator() {
        }

        @Override
        public int compare(ChooserTarget chooserTarget, ChooserTarget chooserTarget2) {
            return (int) Math.signum(chooserTarget2.getScore() - chooserTarget.getScore());
        }
    }

    class ChooserRowAdapter extends BaseAdapter {
        private ChooserListAdapter mChooserListAdapter;
        private final LayoutInflater mLayoutInflater;
        private final int mColumnCount = 4;
        private int mAnimationCount = 0;

        public ChooserRowAdapter(ChooserListAdapter chooserListAdapter) {
            this.mChooserListAdapter = chooserListAdapter;
            this.mLayoutInflater = LayoutInflater.from(ChooserActivity.this);
            chooserListAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    ChooserRowAdapter.this.notifyDataSetChanged();
                }

                @Override
                public void onInvalidated() {
                    super.onInvalidated();
                    ChooserRowAdapter.this.notifyDataSetInvalidated();
                }
            });
        }

        @Override
        public int getCount() {
            return (int) (((double) (getCallerTargetRowCount() + getServiceTargetRowCount())) + Math.ceil(this.mChooserListAdapter.getStandardTargetCount() / 4.0f));
        }

        public int getCallerTargetRowCount() {
            return (int) Math.ceil(this.mChooserListAdapter.getCallerTargetCount() / 4.0f);
        }

        public int getServiceTargetRowCount() {
            return this.mChooserListAdapter.getServiceTargetCount() == 0 ? 0 : 1;
        }

        @Override
        public Object getItem(int i) {
            return Integer.valueOf(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            RowViewHolder rowViewHolderCreateViewHolder;
            if (view == null) {
                rowViewHolderCreateViewHolder = createViewHolder(viewGroup);
            } else {
                rowViewHolderCreateViewHolder = (RowViewHolder) view.getTag();
            }
            bindViewHolder(i, rowViewHolderCreateViewHolder);
            return rowViewHolderCreateViewHolder.row;
        }

        RowViewHolder createViewHolder(ViewGroup viewGroup) {
            ViewGroup viewGroup2 = (ViewGroup) this.mLayoutInflater.inflate(R.layout.chooser_row, viewGroup, false);
            final RowViewHolder rowViewHolder = new RowViewHolder(viewGroup2, 4);
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            for (final int i = 0; i < 4; i++) {
                View viewCreateView = this.mChooserListAdapter.createView(viewGroup2);
                viewCreateView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ChooserActivity.this.startSelected(rowViewHolder.itemIndices[i], false, true);
                    }
                });
                viewCreateView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        ChooserActivity.this.showTargetDetails(ChooserRowAdapter.this.mChooserListAdapter.resolveInfoForPosition(rowViewHolder.itemIndices[i], true));
                        return true;
                    }
                });
                viewGroup2.addView(viewCreateView);
                rowViewHolder.cells[i] = viewCreateView;
                ViewGroup.LayoutParams layoutParams = viewCreateView.getLayoutParams();
                viewCreateView.measure(iMakeMeasureSpec, iMakeMeasureSpec);
                if (layoutParams == null) {
                    viewGroup2.setLayoutParams(new ViewGroup.LayoutParams(-1, viewCreateView.getMeasuredHeight()));
                } else {
                    layoutParams.height = viewCreateView.getMeasuredHeight();
                }
                if (i != 3) {
                    viewGroup2.addView(new Space(ChooserActivity.this), new LinearLayout.LayoutParams(0, 0, 1.0f));
                }
            }
            rowViewHolder.measure();
            ViewGroup.LayoutParams layoutParams2 = viewGroup2.getLayoutParams();
            if (layoutParams2 == null) {
                viewGroup2.setLayoutParams(new ViewGroup.LayoutParams(-1, rowViewHolder.measuredRowHeight));
            } else {
                layoutParams2.height = rowViewHolder.measuredRowHeight;
            }
            viewGroup2.setTag(rowViewHolder);
            return rowViewHolder;
        }

        void bindViewHolder(int i, RowViewHolder rowViewHolder) {
            int firstRowPosition = getFirstRowPosition(i);
            int positionTargetType = this.mChooserListAdapter.getPositionTargetType(firstRowPosition);
            int i2 = (firstRowPosition + 4) - 1;
            while (this.mChooserListAdapter.getPositionTargetType(i2) != positionTargetType && i2 >= firstRowPosition) {
                i2--;
            }
            if (positionTargetType == 1) {
                rowViewHolder.row.setBackgroundColor(ChooserActivity.this.getColor(R.color.chooser_service_row_background_color));
                int positionTargetType2 = this.mChooserListAdapter.getPositionTargetType(getFirstRowPosition(i + 1));
                int dimensionPixelSize = rowViewHolder.row.getContext().getResources().getDimensionPixelSize(R.dimen.chooser_service_spacing);
                if (i == 0 && positionTargetType2 != 1) {
                    setVertPadding(rowViewHolder, 0, 0);
                } else {
                    int i3 = i == 0 ? dimensionPixelSize : 0;
                    if (positionTargetType2 != 1) {
                        setVertPadding(rowViewHolder, i3, dimensionPixelSize);
                    } else {
                        setVertPadding(rowViewHolder, i3, 0);
                    }
                }
            } else {
                rowViewHolder.row.setBackgroundColor(0);
                if (this.mChooserListAdapter.getPositionTargetType(getFirstRowPosition(i - 1)) == 1 || i == 0) {
                    setVertPadding(rowViewHolder, rowViewHolder.row.getContext().getResources().getDimensionPixelSize(R.dimen.chooser_service_spacing), 0);
                } else {
                    setVertPadding(rowViewHolder, 0, 0);
                }
            }
            int i4 = rowViewHolder.row.getLayoutParams().height;
            rowViewHolder.row.getLayoutParams().height = Math.max(1, rowViewHolder.measuredRowHeight);
            if (rowViewHolder.row.getLayoutParams().height != i4) {
                rowViewHolder.row.requestLayout();
            }
            for (int i5 = 0; i5 < 4; i5++) {
                View view = rowViewHolder.cells[i5];
                int i6 = firstRowPosition + i5;
                if (i6 <= i2) {
                    view.setVisibility(0);
                    rowViewHolder.itemIndices[i5] = i6;
                    this.mChooserListAdapter.bindView(rowViewHolder.itemIndices[i5], view);
                } else {
                    view.setVisibility(4);
                }
            }
        }

        private void setVertPadding(RowViewHolder rowViewHolder, int i, int i2) {
            rowViewHolder.row.setPadding(rowViewHolder.row.getPaddingLeft(), i, rowViewHolder.row.getPaddingRight(), i2);
        }

        int getFirstRowPosition(int i) {
            int callerTargetCount = this.mChooserListAdapter.getCallerTargetCount();
            int iCeil = (int) Math.ceil(callerTargetCount / 4.0f);
            if (i < iCeil) {
                return i * 4;
            }
            int serviceTargetCount = this.mChooserListAdapter.getServiceTargetCount();
            int iCeil2 = (int) Math.ceil(serviceTargetCount / 4.0f);
            if (i < iCeil + iCeil2) {
                return callerTargetCount + ((i - iCeil) * 4);
            }
            return callerTargetCount + serviceTargetCount + (((i - iCeil) - iCeil2) * 4);
        }
    }

    static class RowViewHolder {
        final View[] cells;
        int[] itemIndices;
        int measuredRowHeight;
        final ViewGroup row;

        public RowViewHolder(ViewGroup viewGroup, int i) {
            this.row = viewGroup;
            this.cells = new View[i];
            this.itemIndices = new int[i];
        }

        public void measure() {
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            this.row.measure(iMakeMeasureSpec, iMakeMeasureSpec);
            this.measuredRowHeight = this.row.getMeasuredHeight();
        }
    }

    static class ChooserTargetServiceConnection implements ServiceConnection {
        private ChooserActivity mChooserActivity;
        private ComponentName mConnectedComponent;
        private ResolverActivity.DisplayResolveInfo mOriginalTarget;
        private final Object mLock = new Object();
        private final IChooserTargetResult mChooserTargetResult = new IChooserTargetResult.Stub() {
            @Override
            public void sendResult(List<ChooserTarget> list) throws RemoteException {
                synchronized (ChooserTargetServiceConnection.this.mLock) {
                    if (ChooserTargetServiceConnection.this.mChooserActivity == null) {
                        Log.e(ChooserActivity.TAG, "destroyed ChooserTargetServiceConnection received result from " + ChooserTargetServiceConnection.this.mConnectedComponent + "; ignoring...");
                        return;
                    }
                    ChooserTargetServiceConnection.this.mChooserActivity.filterServiceTargets(ChooserTargetServiceConnection.this.mOriginalTarget.getResolveInfo().activityInfo.packageName, list);
                    Message messageObtain = Message.obtain();
                    messageObtain.what = 1;
                    messageObtain.obj = new ServiceResultInfo(ChooserTargetServiceConnection.this.mOriginalTarget, list, ChooserTargetServiceConnection.this);
                    ChooserTargetServiceConnection.this.mChooserActivity.mChooserHandler.sendMessage(messageObtain);
                }
            }
        };

        public ChooserTargetServiceConnection(ChooserActivity chooserActivity, ResolverActivity.DisplayResolveInfo displayResolveInfo) {
            this.mChooserActivity = chooserActivity;
            this.mOriginalTarget = displayResolveInfo;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (this.mLock) {
                if (this.mChooserActivity == null) {
                    Log.e(ChooserActivity.TAG, "destroyed ChooserTargetServiceConnection got onServiceConnected");
                    return;
                }
                try {
                    IChooserTargetService.Stub.asInterface(iBinder).getChooserTargets(this.mOriginalTarget.getResolvedComponentName(), this.mOriginalTarget.getResolveInfo().filter, this.mChooserTargetResult);
                } catch (RemoteException e) {
                    Log.e(ChooserActivity.TAG, "Querying ChooserTargetService " + componentName + " failed.", e);
                    this.mChooserActivity.unbindService(this);
                    this.mChooserActivity.mServiceConnections.remove(this);
                    destroy();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (this.mLock) {
                if (this.mChooserActivity == null) {
                    Log.e(ChooserActivity.TAG, "destroyed ChooserTargetServiceConnection got onServiceDisconnected");
                    return;
                }
                this.mChooserActivity.unbindService(this);
                this.mChooserActivity.mServiceConnections.remove(this);
                if (this.mChooserActivity.mServiceConnections.isEmpty()) {
                    this.mChooserActivity.mChooserHandler.removeMessages(2);
                    this.mChooserActivity.sendVoiceChoicesIfNeeded();
                }
                this.mConnectedComponent = null;
                destroy();
            }
        }

        public void destroy() {
            synchronized (this.mLock) {
                this.mChooserActivity = null;
                this.mOriginalTarget = null;
            }
        }

        public String toString() {
            String string;
            StringBuilder sb = new StringBuilder();
            sb.append("ChooserTargetServiceConnection{service=");
            sb.append(this.mConnectedComponent);
            sb.append(", activity=");
            if (this.mOriginalTarget != null) {
                string = this.mOriginalTarget.getResolveInfo().activityInfo.toString();
            } else {
                string = "<connection destroyed>";
            }
            sb.append(string);
            sb.append("}");
            return sb.toString();
        }
    }

    static class ServiceResultInfo {
        public final ChooserTargetServiceConnection connection;
        public final ResolverActivity.DisplayResolveInfo originalTarget;
        public final List<ChooserTarget> resultTargets;

        public ServiceResultInfo(ResolverActivity.DisplayResolveInfo displayResolveInfo, List<ChooserTarget> list, ChooserTargetServiceConnection chooserTargetServiceConnection) {
            this.originalTarget = displayResolveInfo;
            this.resultTargets = list;
            this.connection = chooserTargetServiceConnection;
        }
    }

    static class RefinementResultReceiver extends ResultReceiver {
        private ChooserActivity mChooserActivity;
        private ResolverActivity.TargetInfo mSelectedTarget;

        public RefinementResultReceiver(ChooserActivity chooserActivity, ResolverActivity.TargetInfo targetInfo, Handler handler) {
            super(handler);
            this.mChooserActivity = chooserActivity;
            this.mSelectedTarget = targetInfo;
        }

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (this.mChooserActivity == null) {
                Log.e(ChooserActivity.TAG, "Destroyed RefinementResultReceiver received a result");
            }
            if (bundle == null) {
                Log.e(ChooserActivity.TAG, "RefinementResultReceiver received null resultData");
                return;
            }
            switch (i) {
                case -1:
                    Parcelable parcelable = bundle.getParcelable(Intent.EXTRA_INTENT);
                    if (parcelable instanceof Intent) {
                        this.mChooserActivity.onRefinementResult(this.mSelectedTarget, (Intent) parcelable);
                    } else {
                        Log.e(ChooserActivity.TAG, "RefinementResultReceiver received RESULT_OK but no Intent in resultData with key Intent.EXTRA_INTENT");
                    }
                    break;
                case 0:
                    this.mChooserActivity.onRefinementCanceled();
                    break;
                default:
                    Log.w(ChooserActivity.TAG, "Unknown result code " + i + " sent to RefinementResultReceiver");
                    break;
            }
        }

        public void destroy() {
            this.mChooserActivity = null;
            this.mSelectedTarget = null;
        }
    }

    class OffsetDataSetObserver extends DataSetObserver {
        private View mCachedView;
        private int mCachedViewType = -1;
        private final AbsListView mListView;

        public OffsetDataSetObserver(AbsListView absListView) {
            this.mListView = absListView;
        }

        @Override
        public void onChanged() {
            if (ChooserActivity.this.mResolverDrawerLayout != null) {
                int serviceTargetRowCount = ChooserActivity.this.mChooserRowAdapter.getServiceTargetRowCount();
                int i = 0;
                for (int i2 = 0; i2 < serviceTargetRowCount; i2++) {
                    int callerTargetRowCount = ChooserActivity.this.mChooserRowAdapter.getCallerTargetRowCount() + i2;
                    int itemViewType = ChooserActivity.this.mChooserRowAdapter.getItemViewType(callerTargetRowCount);
                    if (itemViewType != this.mCachedViewType) {
                        this.mCachedView = null;
                    }
                    View view = ChooserActivity.this.mChooserRowAdapter.getView(callerTargetRowCount, this.mCachedView, this.mListView);
                    i += ((RowViewHolder) view.getTag()).measuredRowHeight;
                    if (itemViewType >= 0) {
                        this.mCachedViewType = itemViewType;
                        this.mCachedView = view;
                    } else {
                        this.mCachedViewType = -1;
                    }
                }
                ChooserActivity.this.mResolverDrawerLayout.setCollapsibleHeightReserved(i);
            }
        }
    }
}
