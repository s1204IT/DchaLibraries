package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.content.PackageMonitor;
import com.android.internal.widget.ResolverDrawerLayout;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResolverActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "ResolverActivity";
    private static final long USAGE_STATS_PERIOD = 1209600000;
    private ResolveListAdapter mAdapter;
    private Button mAlwaysButton;
    private boolean mAlwaysUseOption;
    private int mIconDpi;
    private int mIconSize;
    private Intent mIntent;
    private int mLaunchedFromUid;
    private ListView mListView;
    private int mMaxColumns;
    private Button mOnceButton;
    private PackageManager mPm;
    private View mProfileView;
    private boolean mRegistered;
    private boolean mSafeForwardingMode;
    private boolean mShowExtended;
    private Map<String, UsageStats> mStats;
    private UsageStatsManager mUsm;
    private int mLastSelected = -1;
    private boolean mResolvingHome = false;
    private int mProfileSwitchMessageId = -1;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onSomePackagesChanged() {
            ResolverActivity.this.mAdapter.handlePackagesChanged();
            if (ResolverActivity.this.mProfileView != null) {
                ResolverActivity.this.bindProfileView();
            }
        }
    };

    private enum ActionTitle {
        VIEW("android.intent.action.VIEW", R.string.whichViewApplication, R.string.whichViewApplicationNamed),
        EDIT(Intent.ACTION_EDIT, R.string.whichEditApplication, R.string.whichEditApplicationNamed),
        SEND(Intent.ACTION_SEND, R.string.whichSendApplication, R.string.whichSendApplicationNamed),
        SENDTO(Intent.ACTION_SENDTO, R.string.whichSendApplication, R.string.whichSendApplicationNamed),
        SEND_MULTIPLE(Intent.ACTION_SEND_MULTIPLE, R.string.whichSendApplication, R.string.whichSendApplicationNamed),
        DEFAULT(null, R.string.whichApplication, R.string.whichApplicationNamed),
        HOME(Intent.ACTION_MAIN, R.string.whichHomeApplication, R.string.whichHomeApplicationNamed);

        public final String action;
        public final int namedTitleRes;
        public final int titleRes;

        ActionTitle(String action, int titleRes, int namedTitleRes) {
            this.action = action;
            this.titleRes = titleRes;
            this.namedTitleRes = namedTitleRes;
        }

        public static ActionTitle forAction(String action) {
            ActionTitle[] arr$ = values();
            for (ActionTitle title : arr$) {
                if (title != HOME && action != null && action.equals(title.action)) {
                    return title;
                }
            }
            return DEFAULT;
        }
    }

    private Intent makeMyIntent() {
        Intent intent = new Intent(getIntent());
        intent.setComponent(null);
        intent.setFlags(intent.getFlags() & (-8388609));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = makeMyIntent();
        Set<String> categories = intent.getCategories();
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && categories != null && categories.size() == 1 && categories.contains(Intent.CATEGORY_HOME)) {
            this.mResolvingHome = true;
        }
        setSafeForwardingMode(true);
        onCreate(savedInstanceState, intent, null, 0, null, null, true);
    }

    protected void onCreate(Bundle savedInstanceState, Intent intent, CharSequence title, Intent[] initialIntents, List<ResolveInfo> rList, boolean alwaysUseOption) {
        onCreate(savedInstanceState, intent, title, 0, initialIntents, rList, alwaysUseOption);
    }

    protected void onCreate(Bundle savedInstanceState, Intent intent, CharSequence title, int defaultTitleRes, Intent[] initialIntents, List<ResolveInfo> rList, boolean alwaysUseOption) {
        boolean useHeader;
        int layoutId;
        setTheme(R.style.Theme_DeviceDefault_Resolver);
        super.onCreate(savedInstanceState);
        setProfileSwitchMessageId(intent.getContentUserHint());
        try {
            this.mLaunchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
        } catch (RemoteException e) {
            this.mLaunchedFromUid = -1;
        }
        this.mPm = getPackageManager();
        this.mUsm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long sinceTime = System.currentTimeMillis() - USAGE_STATS_PERIOD;
        this.mStats = this.mUsm.queryAndAggregateUsageStats(sinceTime, System.currentTimeMillis());
        this.mMaxColumns = getResources().getInteger(R.integer.config_maxResolverActivityColumns);
        this.mPackageMonitor.register(this, getMainLooper(), false);
        this.mRegistered = true;
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        this.mIconDpi = am.getLauncherLargeIconDensity();
        this.mIconSize = am.getLauncherLargeIconSize();
        this.mIntent = new Intent(intent);
        this.mAdapter = new ResolveListAdapter(this, initialIntents, rList, this.mLaunchedFromUid, alwaysUseOption);
        if (this.mAdapter.hasFilteredItem()) {
            layoutId = R.layout.resolver_list_with_default;
            alwaysUseOption = false;
            useHeader = true;
        } else {
            useHeader = false;
            layoutId = R.layout.resolver_list;
        }
        this.mAlwaysUseOption = alwaysUseOption;
        if (this.mLaunchedFromUid < 0 || UserHandle.isIsolated(this.mLaunchedFromUid)) {
            finish();
            return;
        }
        int count = this.mAdapter.mList.size();
        if (count > 1 || (count == 1 && this.mAdapter.getOtherProfile() != null)) {
            setContentView(layoutId);
            this.mListView = (ListView) findViewById(R.id.resolver_list);
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
            this.mListView.setOnItemClickListener(this);
            this.mListView.setOnItemLongClickListener(new ItemLongClickListener());
            if (alwaysUseOption) {
                this.mListView.setChoiceMode(1);
            }
            if (useHeader) {
                this.mListView.addHeaderView(LayoutInflater.from(this).inflate(R.layout.resolver_different_item_header, (ViewGroup) this.mListView, false));
            }
        } else {
            if (count == 1) {
                safelyStartActivity(this.mAdapter.intentForPosition(0, false));
                this.mPackageMonitor.unregister();
                this.mRegistered = false;
                finish();
                return;
            }
            setContentView(R.layout.resolver_list);
            TextView empty = (TextView) findViewById(16908292);
            empty.setVisibility(0);
            this.mListView = (ListView) findViewById(R.id.resolver_list);
            this.mListView.setVisibility(8);
        }
        getWindow().clearFlags(65792);
        ResolverDrawerLayout rdl = (ResolverDrawerLayout) findViewById(R.id.contentPanel);
        if (rdl != null) {
            rdl.setOnDismissedListener(new ResolverDrawerLayout.OnDismissedListener() {
                @Override
                public void onDismissed() {
                    ResolverActivity.this.finish();
                }
            });
        }
        if (title == null) {
            title = getTitleForAction(intent.getAction(), defaultTitleRes);
        }
        if (!TextUtils.isEmpty(title)) {
            TextView titleView = (TextView) findViewById(16908310);
            if (titleView != null) {
                titleView.setText(title);
            }
            setTitle(title);
        }
        ImageView iconView = (ImageView) findViewById(16908294);
        DisplayResolveInfo iconInfo = this.mAdapter.getFilteredItem();
        if (iconView != null && iconInfo != null) {
            new LoadIconIntoViewTask(iconView).execute(iconInfo);
        }
        if (alwaysUseOption || this.mAdapter.hasFilteredItem()) {
            ViewGroup buttonLayout = (ViewGroup) findViewById(R.id.button_bar);
            if (buttonLayout != null) {
                buttonLayout.setVisibility(0);
                this.mAlwaysButton = (Button) buttonLayout.findViewById(R.id.button_always);
                this.mOnceButton = (Button) buttonLayout.findViewById(R.id.button_once);
            } else {
                this.mAlwaysUseOption = false;
            }
        }
        if (this.mAdapter.hasFilteredItem()) {
            setAlwaysButtonEnabled(true, this.mAdapter.getFilteredPosition(), false);
            this.mOnceButton.setEnabled(true);
        }
        this.mProfileView = findViewById(R.id.profile_button);
        if (this.mProfileView != null) {
            this.mProfileView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DisplayResolveInfo dri = ResolverActivity.this.mAdapter.getOtherProfile();
                    if (dri != null) {
                        Intent intent2 = ResolverActivity.this.intentForDisplayResolveInfo(dri);
                        ResolverActivity.this.onIntentSelected(dri.ri, intent2, false);
                        ResolverActivity.this.finish();
                    }
                }
            });
            bindProfileView();
        }
    }

    void bindProfileView() {
        DisplayResolveInfo dri = this.mAdapter.getOtherProfile();
        if (dri != null) {
            this.mProfileView.setVisibility(0);
            ImageView icon = (ImageView) this.mProfileView.findViewById(16908294);
            TextView text = (TextView) this.mProfileView.findViewById(16908308);
            if (dri.displayIcon == null) {
                new LoadIconTask().execute(dri);
            }
            icon.setImageDrawable(dri.displayIcon);
            text.setText(dri.displayLabel);
            return;
        }
        this.mProfileView.setVisibility(8);
    }

    private void setProfileSwitchMessageId(int contentUserHint) {
        if (contentUserHint != -2 && contentUserHint != UserHandle.myUserId()) {
            UserManager userManager = (UserManager) getSystemService("user");
            UserInfo originUserInfo = userManager.getUserInfo(contentUserHint);
            boolean originIsManaged = originUserInfo != null ? originUserInfo.isManagedProfile() : false;
            boolean targetIsManaged = userManager.isManagedProfile();
            if (originIsManaged && !targetIsManaged) {
                this.mProfileSwitchMessageId = R.string.forward_intent_to_owner;
            } else if (!originIsManaged && targetIsManaged) {
                this.mProfileSwitchMessageId = R.string.forward_intent_to_work;
            }
        }
    }

    public void setSafeForwardingMode(boolean safeForwarding) {
        this.mSafeForwardingMode = safeForwarding;
    }

    protected CharSequence getTitleForAction(String action, int defaultTitleRes) {
        ActionTitle title = this.mResolvingHome ? ActionTitle.HOME : ActionTitle.forAction(action);
        boolean named = this.mAdapter.hasFilteredItem();
        if (title != ActionTitle.DEFAULT || defaultTitleRes == 0) {
            return named ? getString(title.namedTitleRes, this.mAdapter.getFilteredItem().displayLabel) : getString(title.titleRes);
        }
        return getString(defaultTitleRes);
    }

    void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }

    Drawable getIcon(Resources res, int resId) {
        try {
            Drawable result = res.getDrawableForDensity(resId, this.mIconDpi);
            return result;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    Drawable loadIconForResolveInfo(ResolveInfo ri) {
        Drawable dr;
        try {
            if (ri.resolvePackageName == null || ri.icon == 0 || (dr = getIcon(this.mPm.getResourcesForApplication(ri.resolvePackageName), ri.icon)) == null) {
                int iconRes = ri.getIconResource();
                if (iconRes != 0) {
                    Drawable dr2 = getIcon(this.mPm.getResourcesForApplication(ri.activityInfo.packageName), iconRes);
                    if (dr2 != null) {
                        return dr2;
                    }
                }
            } else {
                return dr;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Couldn't find resources for package", e);
        }
        return ri.loadIcon(this.mPm);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!this.mRegistered) {
            this.mPackageMonitor.register(this, getMainLooper(), false);
            this.mRegistered = true;
        }
        this.mAdapter.handlePackagesChanged();
        if (this.mProfileView != null) {
            bindProfileView();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mRegistered) {
            this.mPackageMonitor.unregister();
            this.mRegistered = false;
        }
        if ((getIntent().getFlags() & 268435456) != 0 && !isChangingConfigurations()) {
            finish();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (this.mAlwaysUseOption) {
            int checkedPos = this.mListView.getCheckedItemPosition();
            boolean hasValidSelection = checkedPos != -1;
            this.mLastSelected = checkedPos;
            setAlwaysButtonEnabled(hasValidSelection, checkedPos, true);
            this.mOnceButton.setEnabled(hasValidSelection);
            if (hasValidSelection) {
                this.mListView.setSelection(checkedPos);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int position2 = position - this.mListView.getHeaderViewsCount();
        if (position2 >= 0) {
            ResolveInfo resolveInfo = this.mAdapter.resolveInfoForPosition(position2, true);
            if (!this.mResolvingHome || !hasManagedProfile() || supportsManagedProfiles(resolveInfo)) {
                int checkedPos = this.mListView.getCheckedItemPosition();
                boolean hasValidSelection = checkedPos != -1;
                if (this.mAlwaysUseOption && (!hasValidSelection || this.mLastSelected != checkedPos)) {
                    setAlwaysButtonEnabled(hasValidSelection, checkedPos, true);
                    this.mOnceButton.setEnabled(hasValidSelection);
                    if (hasValidSelection) {
                        this.mListView.smoothScrollToPosition(checkedPos);
                    }
                    this.mLastSelected = checkedPos;
                    return;
                }
                startSelected(position2, false, true);
                return;
            }
            Toast.makeText(this, String.format(getResources().getString(R.string.activity_resolver_work_profiles_support), resolveInfo.activityInfo.loadLabel(getPackageManager()).toString()), 1).show();
        }
    }

    private boolean hasManagedProfile() {
        UserManager userManager = (UserManager) getSystemService("user");
        if (userManager == null) {
            return false;
        }
        try {
            List<UserInfo> profiles = userManager.getProfiles(getUserId());
            for (UserInfo userInfo : profiles) {
                if (userInfo != null && userInfo.isManagedProfile()) {
                    return true;
                }
            }
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }

    private boolean supportsManagedProfiles(ResolveInfo resolveInfo) {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(resolveInfo.activityInfo.packageName, 0);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= 21;
    }

    private void setAlwaysButtonEnabled(boolean hasValidSelection, int checkedPos, boolean filtered) {
        boolean enabled = false;
        if (hasValidSelection) {
            ResolveInfo ri = this.mAdapter.resolveInfoForPosition(checkedPos, filtered);
            if (ri.targetUserId == -2) {
                enabled = true;
            }
        }
        this.mAlwaysButton.setEnabled(enabled);
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        startSelected(this.mAlwaysUseOption ? this.mListView.getCheckedItemPosition() : this.mAdapter.getFilteredPosition(), id == 16909163, this.mAlwaysUseOption);
        dismiss();
    }

    void startSelected(int which, boolean always, boolean filtered) {
        if (!isFinishing()) {
            ResolveInfo ri = this.mAdapter.resolveInfoForPosition(which, filtered);
            Intent intent = this.mAdapter.intentForPosition(which, filtered);
            onIntentSelected(ri, intent, always);
            finish();
        }
    }

    public Intent getReplacementIntent(ActivityInfo aInfo, Intent defIntent) {
        return defIntent;
    }

    protected void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
        String mimeType;
        if ((this.mAlwaysUseOption || this.mAdapter.hasFilteredItem()) && this.mAdapter.mOrigResolveList != null) {
            IntentFilter filter = new IntentFilter();
            if (intent.getAction() != null) {
                filter.addAction(intent.getAction());
            }
            Set<String> categories = intent.getCategories();
            if (categories != null) {
                Iterator<String> it = categories.iterator();
                while (it.hasNext()) {
                    filter.addCategory(it.next());
                }
            }
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            int cat = ri.match & IntentFilter.MATCH_CATEGORY_MASK;
            Uri data = intent.getData();
            if (cat == 6291456 && (mimeType = intent.resolveType(this)) != null) {
                try {
                    filter.addDataType(mimeType);
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    Log.w(TAG, e);
                    filter = null;
                }
            }
            if (data != null && data.getScheme() != null && (cat != 6291456 || (!ContentResolver.SCHEME_FILE.equals(data.getScheme()) && !"content".equals(data.getScheme())))) {
                filter.addDataScheme(data.getScheme());
                Iterator<PatternMatcher> pIt = ri.filter.schemeSpecificPartsIterator();
                if (pIt != null) {
                    String ssp = data.getSchemeSpecificPart();
                    while (true) {
                        if (ssp == null || !pIt.hasNext()) {
                            break;
                        }
                        PatternMatcher p = pIt.next();
                        if (p.match(ssp)) {
                            filter.addDataSchemeSpecificPart(p.getPath(), p.getType());
                            break;
                        }
                    }
                }
                Iterator<IntentFilter.AuthorityEntry> aIt = ri.filter.authoritiesIterator();
                if (aIt != null) {
                    while (true) {
                        if (!aIt.hasNext()) {
                            break;
                        }
                        IntentFilter.AuthorityEntry a = aIt.next();
                        if (a.match(data) >= 0) {
                            int port = a.getPort();
                            filter.addDataAuthority(a.getHost(), port >= 0 ? Integer.toString(port) : null);
                        }
                    }
                }
                Iterator<PatternMatcher> pIt2 = ri.filter.pathsIterator();
                if (pIt2 != null) {
                    String path = data.getPath();
                    while (true) {
                        if (path == null || !pIt2.hasNext()) {
                            break;
                        }
                        PatternMatcher p2 = pIt2.next();
                        if (p2.match(path)) {
                            filter.addDataPath(p2.getPath(), p2.getType());
                            break;
                        }
                    }
                }
            }
            if (filter != null) {
                int N = this.mAdapter.mOrigResolveList.size();
                ComponentName[] set = new ComponentName[N];
                int bestMatch = 0;
                for (int i = 0; i < N; i++) {
                    ResolveInfo r = this.mAdapter.mOrigResolveList.get(i);
                    set[i] = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
                    if (r.match > bestMatch) {
                        bestMatch = r.match;
                    }
                }
                if (alwaysCheck) {
                    getPackageManager().addPreferredActivity(filter, bestMatch, set, intent.getComponent());
                } else {
                    try {
                        AppGlobals.getPackageManager().setLastChosenActivity(intent, intent.resolveTypeIfNeeded(getContentResolver()), 65536, filter, bestMatch, intent.getComponent());
                    } catch (RemoteException re) {
                        Log.d(TAG, "Error calling setLastChosenActivity\n" + re);
                    }
                }
            }
        }
        if (intent != null) {
            safelyStartActivity(intent);
        }
    }

    public void safelyStartActivity(Intent intent) {
        String launchedFromPackage;
        if (this.mProfileSwitchMessageId != -1) {
            Toast.makeText(this, getString(this.mProfileSwitchMessageId), 1).show();
        }
        if (!this.mSafeForwardingMode) {
            startActivity(intent);
            onActivityStarted(intent);
            return;
        }
        try {
            startActivityAsCaller(intent, null, -10000);
            onActivityStarted(intent);
        } catch (RuntimeException e) {
            try {
                launchedFromPackage = ActivityManagerNative.getDefault().getLaunchedFromPackage(getActivityToken());
            } catch (RemoteException e2) {
                launchedFromPackage = "??";
            }
            Slog.wtf(TAG, "Unable to launch as uid " + this.mLaunchedFromUid + " package " + launchedFromPackage + ", while running in " + ActivityThread.currentProcessName(), e);
        }
    }

    public void onActivityStarted(Intent intent) {
    }

    void showAppDetails(ResolveInfo ri) {
        Intent in = new Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", ri.activityInfo.packageName, null)).addFlags(524288);
        startActivity(in);
    }

    Intent intentForDisplayResolveInfo(DisplayResolveInfo dri) {
        Intent intent = new Intent(dri.origIntent != null ? dri.origIntent : getReplacementIntent(dri.ri.activityInfo, this.mIntent));
        intent.addFlags(View.SCROLLBARS_OUTSIDE_INSET);
        ActivityInfo ai = dri.ri.activityInfo;
        intent.setComponent(new ComponentName(ai.applicationInfo.packageName, ai.name));
        return intent;
    }

    private final class DisplayResolveInfo {
        Drawable displayIcon;
        CharSequence displayLabel;
        CharSequence extendedInfo;
        Intent origIntent;
        ResolveInfo ri;

        DisplayResolveInfo(ResolveInfo pri, CharSequence pLabel, CharSequence pInfo, Intent pOrigIntent) {
            this.ri = pri;
            this.displayLabel = pLabel;
            this.extendedInfo = pInfo;
            this.origIntent = pOrigIntent;
        }
    }

    private final class ResolveListAdapter extends BaseAdapter {
        private final List<ResolveInfo> mBaseResolveList;
        private boolean mFilterLastUsed;
        private final LayoutInflater mInflater;
        private final Intent[] mInitialIntents;
        private ResolveInfo mLastChosen;
        private final int mLaunchedFromUid;
        List<ResolveInfo> mOrigResolveList;
        private DisplayResolveInfo mOtherProfile;
        private int mLastChosenPosition = -1;
        List<DisplayResolveInfo> mList = new ArrayList();

        public ResolveListAdapter(Context context, Intent[] initialIntents, List<ResolveInfo> rList, int launchedFromUid, boolean filterLastUsed) {
            this.mInitialIntents = initialIntents;
            this.mBaseResolveList = rList;
            this.mLaunchedFromUid = launchedFromUid;
            this.mInflater = LayoutInflater.from(context);
            this.mFilterLastUsed = filterLastUsed;
            rebuildList();
        }

        public void handlePackagesChanged() {
            rebuildList();
            notifyDataSetChanged();
            if (getCount() == 0) {
                ResolverActivity.this.finish();
            }
        }

        public DisplayResolveInfo getFilteredItem() {
            if (!this.mFilterLastUsed || this.mLastChosenPosition < 0) {
                return null;
            }
            return this.mList.get(this.mLastChosenPosition);
        }

        public DisplayResolveInfo getOtherProfile() {
            return this.mOtherProfile;
        }

        public int getFilteredPosition() {
            if (!this.mFilterLastUsed || this.mLastChosenPosition < 0) {
                return -1;
            }
            return this.mLastChosenPosition;
        }

        public boolean hasFilteredItem() {
            return this.mFilterLastUsed && this.mLastChosenPosition >= 0;
        }

        private void rebuildList() {
            List<ResolveInfo> currentResolveList;
            int N;
            try {
                this.mLastChosen = AppGlobals.getPackageManager().getLastChosenActivity(ResolverActivity.this.mIntent, ResolverActivity.this.mIntent.resolveTypeIfNeeded(ResolverActivity.this.getContentResolver()), 65536);
            } catch (RemoteException re) {
                Log.d(ResolverActivity.TAG, "Error calling setLastChosenActivity\n" + re);
            }
            this.mList.clear();
            if (this.mBaseResolveList == null) {
                currentResolveList = ResolverActivity.this.mPm.queryIntentActivities(ResolverActivity.this.mIntent, (this.mFilterLastUsed ? 64 : 0) | 65536);
                this.mOrigResolveList = currentResolveList;
                if (currentResolveList != null) {
                    for (int i = currentResolveList.size() - 1; i >= 0; i--) {
                        ActivityInfo ai = currentResolveList.get(i).activityInfo;
                        int granted = ActivityManager.checkComponentPermission(ai.permission, this.mLaunchedFromUid, ai.applicationInfo.uid, ai.exported);
                        if (granted != 0) {
                            if (this.mOrigResolveList == currentResolveList) {
                                this.mOrigResolveList = new ArrayList(this.mOrigResolveList);
                            }
                            currentResolveList.remove(i);
                        }
                    }
                }
            } else {
                currentResolveList = this.mBaseResolveList;
                this.mOrigResolveList = currentResolveList;
            }
            if (currentResolveList != null && (N = currentResolveList.size()) > 0) {
                ResolveInfo r0 = currentResolveList.get(0);
                for (int i2 = 1; i2 < N; i2++) {
                    ResolveInfo ri = currentResolveList.get(i2);
                    if (r0.priority != ri.priority || r0.isDefault != ri.isDefault) {
                        while (i2 < N) {
                            if (this.mOrigResolveList == currentResolveList) {
                                this.mOrigResolveList = new ArrayList(this.mOrigResolveList);
                            }
                            currentResolveList.remove(i2);
                            N--;
                        }
                    }
                }
                if (N > 1) {
                    Comparator<ResolveInfo> rComparator = ResolverActivity.this.new ResolverComparator(ResolverActivity.this, ResolverActivity.this.mIntent);
                    Collections.sort(currentResolveList, rComparator);
                }
                if (this.mInitialIntents != null) {
                    for (int i3 = 0; i3 < this.mInitialIntents.length; i3++) {
                        Intent ii = this.mInitialIntents[i3];
                        if (ii != null) {
                            ActivityInfo ai2 = ii.resolveActivityInfo(ResolverActivity.this.getPackageManager(), 0);
                            if (ai2 == null) {
                                Log.w(ResolverActivity.TAG, "No activity found for " + ii);
                            } else {
                                ResolveInfo ri2 = new ResolveInfo();
                                ri2.activityInfo = ai2;
                                UserManager userManager = (UserManager) ResolverActivity.this.getSystemService("user");
                                if (userManager.isManagedProfile()) {
                                    ri2.noResourceId = true;
                                }
                                if (ii instanceof LabeledIntent) {
                                    LabeledIntent li = (LabeledIntent) ii;
                                    ri2.resolvePackageName = li.getSourcePackage();
                                    ri2.labelRes = li.getLabelResource();
                                    ri2.nonLocalizedLabel = li.getNonLocalizedLabel();
                                    ri2.icon = li.getIconResource();
                                }
                                addResolveInfo(ResolverActivity.this.new DisplayResolveInfo(ri2, ri2.loadLabel(ResolverActivity.this.getPackageManager()), null, ii));
                            }
                        }
                    }
                }
                ResolveInfo r02 = currentResolveList.get(0);
                int start = 0;
                CharSequence r0Label = r02.loadLabel(ResolverActivity.this.mPm);
                ResolverActivity.this.mShowExtended = false;
                for (int i4 = 1; i4 < N; i4++) {
                    if (r0Label == null) {
                        r0Label = r02.activityInfo.packageName;
                    }
                    ResolveInfo ri3 = currentResolveList.get(i4);
                    CharSequence riLabel = ri3.loadLabel(ResolverActivity.this.mPm);
                    if (riLabel == null) {
                        riLabel = ri3.activityInfo.packageName;
                    }
                    if (!riLabel.equals(r0Label)) {
                        processGroup(currentResolveList, start, i4 - 1, r02, r0Label);
                        r02 = ri3;
                        r0Label = riLabel;
                        start = i4;
                    }
                }
                processGroup(currentResolveList, start, N - 1, r02, r0Label);
            }
            if (this.mOtherProfile != null && this.mLastChosenPosition >= 0) {
                this.mLastChosenPosition = -1;
                this.mFilterLastUsed = false;
            }
        }

        private void processGroup(List<ResolveInfo> rList, int start, int end, ResolveInfo ro, CharSequence roLabel) {
            int num = (end - start) + 1;
            if (num != 1) {
                ResolverActivity.this.mShowExtended = true;
                boolean usePkg = false;
                CharSequence startApp = ro.activityInfo.applicationInfo.loadLabel(ResolverActivity.this.mPm);
                if (startApp == null) {
                    usePkg = true;
                }
                if (!usePkg) {
                    HashSet<CharSequence> duplicates = new HashSet<>();
                    duplicates.add(startApp);
                    for (int j = start + 1; j <= end; j++) {
                        ResolveInfo jRi = rList.get(j);
                        CharSequence jApp = jRi.activityInfo.applicationInfo.loadLabel(ResolverActivity.this.mPm);
                        if (jApp == null || duplicates.contains(jApp)) {
                            usePkg = true;
                            break;
                        }
                        duplicates.add(jApp);
                    }
                    duplicates.clear();
                }
                for (int k = start; k <= end; k++) {
                    ResolveInfo add = rList.get(k);
                    if (usePkg) {
                        addResolveInfo(ResolverActivity.this.new DisplayResolveInfo(add, roLabel, add.activityInfo.packageName, null));
                    } else {
                        addResolveInfo(ResolverActivity.this.new DisplayResolveInfo(add, roLabel, add.activityInfo.applicationInfo.loadLabel(ResolverActivity.this.mPm), null));
                    }
                    updateLastChosenPosition(add);
                }
                return;
            }
            addResolveInfo(ResolverActivity.this.new DisplayResolveInfo(ro, roLabel, null, null));
            updateLastChosenPosition(ro);
        }

        private void updateLastChosenPosition(ResolveInfo info) {
            if (this.mLastChosen != null && this.mLastChosen.activityInfo.packageName.equals(info.activityInfo.packageName) && this.mLastChosen.activityInfo.name.equals(info.activityInfo.name)) {
                this.mLastChosenPosition = this.mList.size() - 1;
            }
        }

        private void addResolveInfo(DisplayResolveInfo dri) {
            if (dri.ri.targetUserId != -2 && this.mOtherProfile == null) {
                this.mOtherProfile = dri;
            } else {
                this.mList.add(dri);
            }
        }

        public ResolveInfo resolveInfoForPosition(int position, boolean filtered) {
            return (filtered ? getItem(position) : this.mList.get(position)).ri;
        }

        public Intent intentForPosition(int position, boolean filtered) {
            DisplayResolveInfo dri = filtered ? getItem(position) : this.mList.get(position);
            return ResolverActivity.this.intentForDisplayResolveInfo(dri);
        }

        @Override
        public int getCount() {
            int result = this.mList.size();
            if (this.mFilterLastUsed && this.mLastChosenPosition >= 0) {
                return result - 1;
            }
            return result;
        }

        @Override
        public DisplayResolveInfo getItem(int position) {
            if (this.mFilterLastUsed && this.mLastChosenPosition >= 0 && position >= this.mLastChosenPosition) {
                position++;
            }
            return this.mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = this.mInflater.inflate(R.layout.resolve_list_item, parent, false);
                ViewHolder holder = new ViewHolder(view);
                view.setTag(holder);
            }
            bindView(view, getItem(position));
            return view;
        }

        private final void bindView(View view, DisplayResolveInfo info) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.text.setText(info.displayLabel);
            if (ResolverActivity.this.mShowExtended) {
                holder.text2.setVisibility(0);
                holder.text2.setText(info.extendedInfo);
            } else {
                holder.text2.setVisibility(8);
            }
            if (info.displayIcon == null) {
                ResolverActivity.this.new LoadIconTask().execute(info);
            }
            holder.icon.setImageDrawable(info.displayIcon);
        }
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView text;
        public TextView text2;

        public ViewHolder(View view) {
            this.text = (TextView) view.findViewById(16908308);
            this.text2 = (TextView) view.findViewById(16908309);
            this.icon = (ImageView) view.findViewById(16908294);
        }
    }

    class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
        ItemLongClickListener() {
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (BenesseExtension.getDchaState() != 0) {
                return true;
            }
            int position2 = position - ResolverActivity.this.mListView.getHeaderViewsCount();
            if (position2 >= 0) {
                ResolveInfo ri = ResolverActivity.this.mAdapter.resolveInfoForPosition(position2, true);
                ResolverActivity.this.showAppDetails(ri);
                return true;
            }
            return false;
        }
    }

    class LoadIconTask extends AsyncTask<DisplayResolveInfo, Void, DisplayResolveInfo> {
        LoadIconTask() {
        }

        @Override
        protected DisplayResolveInfo doInBackground(DisplayResolveInfo... params) {
            DisplayResolveInfo info = params[0];
            if (info.displayIcon == null) {
                info.displayIcon = ResolverActivity.this.loadIconForResolveInfo(info.ri);
            }
            return info;
        }

        @Override
        protected void onPostExecute(DisplayResolveInfo info) {
            if (ResolverActivity.this.mProfileView != null && ResolverActivity.this.mAdapter.getOtherProfile() == info) {
                ResolverActivity.this.bindProfileView();
            }
            ResolverActivity.this.mAdapter.notifyDataSetChanged();
        }
    }

    class LoadIconIntoViewTask extends AsyncTask<DisplayResolveInfo, Void, DisplayResolveInfo> {
        final ImageView mTargetView;

        public LoadIconIntoViewTask(ImageView target) {
            this.mTargetView = target;
        }

        @Override
        protected DisplayResolveInfo doInBackground(DisplayResolveInfo... params) {
            DisplayResolveInfo info = params[0];
            if (info.displayIcon == null) {
                info.displayIcon = ResolverActivity.this.loadIconForResolveInfo(info.ri);
            }
            return info;
        }

        @Override
        protected void onPostExecute(DisplayResolveInfo info) {
            this.mTargetView.setImageDrawable(info.displayIcon);
        }
    }

    static final boolean isSpecificUriMatch(int match) {
        int match2 = match & IntentFilter.MATCH_CATEGORY_MASK;
        return match2 >= 3145728 && match2 <= 5242880;
    }

    class ResolverComparator implements Comparator<ResolveInfo> {
        private final Collator mCollator;
        private final boolean mHttp;

        public ResolverComparator(Context context, Intent intent) {
            this.mCollator = Collator.getInstance(context.getResources().getConfiguration().locale);
            String scheme = intent.getScheme();
            this.mHttp = "http".equals(scheme) || "https".equals(scheme);
        }

        @Override
        public int compare(ResolveInfo lhs, ResolveInfo rhs) {
            if (lhs.targetUserId != -2) {
                return 1;
            }
            if (this.mHttp) {
                boolean lhsSpecific = ResolverActivity.isSpecificUriMatch(lhs.match);
                boolean rhsSpecific = ResolverActivity.isSpecificUriMatch(rhs.match);
                if (lhsSpecific != rhsSpecific) {
                    return lhsSpecific ? -1 : 1;
                }
            }
            if (ResolverActivity.this.mStats != null) {
                long timeDiff = getPackageTimeSpent(rhs.activityInfo.packageName) - getPackageTimeSpent(lhs.activityInfo.packageName);
                if (timeDiff != 0) {
                    return timeDiff > 0 ? 1 : -1;
                }
            }
            CharSequence sa = lhs.loadLabel(ResolverActivity.this.mPm);
            if (sa == null) {
                sa = lhs.activityInfo.name;
            }
            CharSequence sb = rhs.loadLabel(ResolverActivity.this.mPm);
            if (sb == null) {
                sb = rhs.activityInfo.name;
            }
            return this.mCollator.compare(sa.toString(), sb.toString());
        }

        private long getPackageTimeSpent(String packageName) {
            UsageStats stats;
            if (ResolverActivity.this.mStats == null || (stats = (UsageStats) ResolverActivity.this.mStats.get(packageName)) == null) {
                return 0L;
            }
            return stats.getTotalTimeInForeground();
        }
    }
}
