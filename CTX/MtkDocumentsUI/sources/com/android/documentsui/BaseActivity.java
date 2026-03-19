package com.android.documentsui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.MessageQueue;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.NavigationViewManager;
import com.android.documentsui.base.DebugHelper;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.dirlist.DirectoryFragment;
import com.android.documentsui.prefs.LocalPreferences;
import com.android.documentsui.prefs.PreferencesMonitor;
import com.android.documentsui.queries.CommandInterceptor;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.roots.ProvidersCache;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.sidebar.RootsFragment;
import com.android.documentsui.sorting.SortController;
import com.android.documentsui.sorting.SortModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class BaseActivity extends Activity implements AbstractActionHandler.CommonAddons, NavigationViewManager.Environment {
    static final boolean $assertionsDisabled = false;
    protected DocumentsAccess mDocs;
    protected DrawerController mDrawer;
    private final List<EventListener> mEventListeners = new ArrayList();
    protected Injector<?> mInjector;
    private int mLayoutId;
    protected NavigationViewManager mNavigator;
    private PreferencesMonitor mPreferencesMonitor;
    protected ProvidersCache mProviders;
    protected RetainedState mRetainedState;
    private RootsMonitor<BaseActivity> mRootsMonitor;
    protected SearchViewManager mSearchManager;
    protected SortController mSortController;
    private long mStartTime;
    protected State mState;
    private final String mTag;
    private Toast mToast;

    protected interface EventListener {
        void onDirectoryLoaded(Uri uri);

        void onDirectoryNavigated(Uri uri);
    }

    public abstract Injector<?> getInjector();

    protected abstract void includeState(State state);

    protected abstract void onDirectoryCreated(DocumentInfo documentInfo);

    protected abstract void refreshDirectory(int i);

    public BaseActivity(int i, String str) {
        this.mLayoutId = i;
        this.mTag = str;
    }

    public void showToast(int i) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this, i, 0);
        }
        this.mToast.setText(i);
        this.mToast.show();
    }

    @Override
    public void onCreate(Bundle bundle) {
        this.mStartTime = new Date().getTime();
        super.onCreate(bundle);
        Intent intent = getIntent();
        addListenerForLaunchCompletion();
        setContentView(this.mLayoutId);
        this.mInjector = getInjector();
        this.mState = getState(bundle);
        this.mDrawer = DrawerController.create(this, this.mInjector.config);
        Metrics.logActivityLaunch(this, this.mState, intent);
        this.mRetainedState = (RetainedState) getLastNonConfigurationInstance();
        this.mProviders = DocumentsApplication.getProvidersCache(this);
        this.mDocs = DocumentsAccess.create(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        this.mNavigator = new NavigationViewManager(this.mDrawer, toolbar, this.mState, this, (NavigationViewManager.Breadcrumb) Shared.findView(this, R.id.dropdown_breadcrumb, R.id.horizontal_breadcrumb));
        SearchViewManager.SearchManagerListener searchManagerListener = new SearchViewManager.SearchManagerListener() {
            @Override
            public void onSearchChanged(String str) {
                if (str != null) {
                    Metrics.logUserAction(BaseActivity.this, 7);
                }
                BaseActivity.this.mInjector.actions.loadDocumentsForCurrentStack();
            }

            @Override
            public void onSearchFinished() {
                BaseActivity.this.invalidateOptionsMenu();
            }

            @Override
            public void onSearchViewChanged(boolean z) {
                BaseActivity.this.mNavigator.update();
            }
        };
        CommandInterceptor commandInterceptor = new CommandInterceptor(this.mInjector.features);
        commandInterceptor.add(new CommandInterceptor.DumpRootsCacheHandler(this));
        Features features = this.mInjector.features;
        final DebugHelper debugHelper = this.mInjector.debugHelper;
        Objects.requireNonNull(debugHelper);
        this.mSearchManager = new SearchViewManager(searchManagerListener, CommandInterceptor.createDebugModeFlipper(features, new Runnable() {
            @Override
            public final void run() {
                debugHelper.toggleDebugMode();
            }
        }, commandInterceptor), bundle);
        this.mSortController = SortController.create(this, this.mState.derivedMode, this.mState.sortModel);
        this.mPreferencesMonitor = new PreferencesMonitor(getApplicationContext().getPackageName(), PreferenceManager.getDefaultSharedPreferences(this), new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onPreferenceChanged((String) obj);
            }
        });
        this.mPreferencesMonitor.start();
        setResult(0);
    }

    public void onPreferenceChanged(String str) {
        byte b = -1;
        if (str.hashCode() == 1923512096 && str.equals("includeDeviceRoot")) {
            b = 0;
        }
        if (b == 0) {
            updateDisplayAdvancedDevices(this.mInjector.prefs.getShowDeviceRoot());
        }
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        T t = this.mInjector.actions;
        ProvidersCache providersCache = this.mProviders;
        DocumentsAccess documentsAccess = this.mDocs;
        State state = this.mState;
        SearchViewManager searchViewManager = this.mSearchManager;
        final ActionModeController actionModeController = this.mInjector.actionModeController;
        Objects.requireNonNull(actionModeController);
        this.mRootsMonitor = new RootsMonitor<>(this, t, providersCache, documentsAccess, state, searchViewManager, new Runnable() {
            @Override
            public final void run() {
                actionModeController.finishActionMode();
            }
        });
        this.mRootsMonitor.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean zOnCreateOptionsMenu = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity, menu);
        this.mNavigator.update();
        this.mSearchManager.install(menu, getResources().getBoolean(R.bool.full_bar_search_view));
        return zOnCreateOptionsMenu;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.mSearchManager.showMenu(this.mState.stack);
        return true;
    }

    @Override
    protected void onDestroy() {
        this.mRootsMonitor.stop();
        this.mPreferencesMonitor.stop();
        this.mSortController.destroy();
        super.onDestroy();
    }

    private State getState(Bundle bundle) {
        if (bundle != null) {
            State state = (State) bundle.getParcelable("state");
            if (SharedMinimal.DEBUG) {
                Log.d(this.mTag, "Recovered existing state object: " + state);
            }
            return state;
        }
        State state2 = new State();
        Intent intent = getIntent();
        state2.sortModel = SortModel.createModel();
        state2.localOnly = intent.getBooleanExtra("android.intent.extra.LOCAL_ONLY", false);
        state2.excludedAuthorities = getExcludedAuthorities();
        includeState(state2);
        state2.showAdvanced = Shared.mustShowDeviceRoot(intent) || this.mInjector.prefs.getShowDeviceRoot();
        state2.showDeviceStorageOption = !Shared.mustShowDeviceRoot(intent);
        if (SharedMinimal.DEBUG) {
            Log.d(this.mTag, "Created new state object: " + state2);
        }
        return state2;
    }

    @Override
    public void setRootsDrawerOpen(boolean z) {
        this.mNavigator.revealRootsDrawer(z);
    }

    @Override
    public void onRootPicked(RootInfo rootInfo) {
        this.mSearchManager.cancelSearch();
        if (rootInfo.equals(getCurrentRoot()) && this.mState.stack.size() == 1) {
            return;
        }
        this.mInjector.actionModeController.finishActionMode();
        this.mSortController.onViewModeChanged(this.mState.derivedMode);
        this.mState.sortModel.setDimensionVisibility(android.R.id.summary, (rootInfo.isRecents() || rootInfo.isDownloads()) ? 0 : 4);
        this.mState.stack.changeRoot(rootInfo);
        if (this.mProviders.isRecentsRoot(rootInfo)) {
            refreshCurrentRootAndDirectory(1);
        } else {
            this.mInjector.actions.getRootDocument(rootInfo, -1, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.mInjector.actions.openRootDocument((DocumentInfo) obj);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            onBackPressed();
            return true;
        }
        if (itemId == R.id.option_menu_list) {
            setViewMode(1);
            return true;
        }
        switch (itemId) {
            case R.id.option_menu_advanced:
                onDisplayAdvancedDevices();
                return true;
            case R.id.option_menu_create_dir:
                getInjector().actions.showCreateDirectoryDialog();
                return true;
            case R.id.option_menu_debug:
                getInjector().actions.showDebugMessage();
                return true;
            case R.id.option_menu_grid:
                setViewMode(2);
                return true;
            default:
                switch (itemId) {
                    case R.id.option_menu_search:
                        return false;
                    case R.id.option_menu_select_all:
                        getInjector().actions.selectAllFiles();
                        return true;
                    default:
                        return super.onOptionsItemSelected(menuItem);
                }
        }
    }

    protected final DirectoryFragment getDirectoryFragment() {
        return DirectoryFragment.get(getFragmentManager());
    }

    protected boolean canCreateDirectory() {
        RootInfo currentRoot = getCurrentRoot();
        DocumentInfo currentDirectory = getCurrentDirectory();
        return (currentDirectory == null || !currentDirectory.isCreateSupported() || this.mSearchManager.isSearching() || currentRoot.isRecents()) ? false : true;
    }

    @Override
    public final void updateNavigator() {
        this.mNavigator.update();
    }

    @Override
    public void restoreRootAndDirectory() {
        if (DirectoryFragment.get(getFragmentManager()) == null) {
            refreshCurrentRootAndDirectory(1);
        }
    }

    @Override
    public final void refreshCurrentRootAndDirectory(int i) {
        this.mSearchManager.cancelSearch();
        this.mState.derivedMode = LocalPreferences.getViewMode(this, this.mState.stack.getRoot(), 2);
        refreshDirectory(i);
        RootsFragment rootsFragment = RootsFragment.get(getFragmentManager());
        if (rootsFragment != null) {
            rootsFragment.onCurrentRootChanged();
        }
        this.mNavigator.update();
        setTitle(this.mState.stack.getTitle());
        invalidateOptionsMenu();
    }

    private final List<String> getExcludedAuthorities() {
        ArrayList arrayList = new ArrayList();
        if (getIntent().getBooleanExtra("android.provider.extra.EXCLUDE_SELF", false)) {
            String callingPackageName = Shared.getCallingPackageName(this);
            try {
                for (ProviderInfo providerInfo : getPackageManager().getPackageInfo(callingPackageName, 8).providers) {
                    arrayList.add(providerInfo.authority);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(this.mTag, "Calling package name does not resolve: " + callingPackageName);
            }
        }
        return arrayList;
    }

    public static BaseActivity get(Fragment fragment) {
        return (BaseActivity) fragment.getActivity();
    }

    public State getDisplayState() {
        return this.mState;
    }

    private void onDisplayAdvancedDevices() {
        boolean z = !this.mState.showAdvanced;
        Metrics.logUserAction(this, z ? 19 : 20);
        this.mInjector.prefs.setShowDeviceRoot(z);
        updateDisplayAdvancedDevices(z);
    }

    private void updateDisplayAdvancedDevices(boolean z) {
        this.mState.showAdvanced = z;
        RootsFragment rootsFragment = RootsFragment.get(getFragmentManager());
        if (rootsFragment != null) {
            rootsFragment.onDisplayStateChanged();
        }
        invalidateOptionsMenu();
    }

    void setViewMode(int i) {
        if (i == 2) {
            Metrics.logUserAction(this, 2);
        } else if (i == 1) {
            Metrics.logUserAction(this, 3);
        }
        LocalPreferences.setViewMode(this, getCurrentRoot(), i);
        this.mState.derivedMode = i;
        invalidateOptionsMenu();
        DirectoryFragment directoryFragment = getDirectoryFragment();
        if (directoryFragment != null) {
            directoryFragment.onViewModeChanged();
        }
        this.mSortController.onViewModeChanged(i);
    }

    public void setPending(boolean z) {
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("state", this.mState);
        this.mSearchManager.onSaveInstanceState(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }

    @Override
    public RetainedState onRetainNonConfigurationInstance() {
        RetainedState retainedState = new RetainedState();
        DirectoryFragment directoryFragment = DirectoryFragment.get(getFragmentManager());
        if (directoryFragment != null) {
            directoryFragment.retainState(retainedState);
        }
        return retainedState;
    }

    public RetainedState getRetainedState() {
        return this.mRetainedState;
    }

    @Override
    public boolean isSearchExpanded() {
        return this.mSearchManager.isExpanded();
    }

    @Override
    public RootInfo getCurrentRoot() {
        RootInfo root = this.mState.stack.getRoot();
        if (root != null) {
            return root;
        }
        return this.mProviders.getRecentsRoot();
    }

    public DocumentInfo getCurrentDirectory() {
        return this.mState.stack.peek();
    }

    public void addEventListener(EventListener eventListener) {
        this.mEventListeners.add(eventListener);
    }

    public void removeEventListener(EventListener eventListener) {
        this.mEventListeners.remove(eventListener);
    }

    public void notifyDirectoryLoaded(Uri uri) {
        Iterator<EventListener> it = this.mEventListeners.iterator();
        while (it.hasNext()) {
            it.next().onDirectoryLoaded(uri);
        }
    }

    @Override
    public void notifyDirectoryNavigated(Uri uri) {
        Iterator<EventListener> it = this.mEventListeners.iterator();
        while (it.hasNext()) {
            it.next().onDirectoryNavigated(uri);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 0) {
            this.mInjector.debugHelper.debugCheck(keyEvent.getDownTime(), keyEvent.getKeyCode());
        }
        DocumentsApplication.getDragAndDropManager(this).onKeyEvent(keyEvent);
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        this.mInjector.actions.onActivityResult(i, i2, intent);
    }

    protected boolean popDir() {
        if (this.mState.stack.size() > 1) {
            this.mState.stack.pop();
            refreshCurrentRootAndDirectory(3);
            return true;
        }
        return false;
    }

    protected boolean focusSidebar() {
        return RootsFragment.get(getFragmentManager()).requestFocus();
    }

    private void addListenerForLaunchCompletion() {
        addEventListener(new EventListener() {
            @Override
            public void onDirectoryNavigated(Uri uri) {
            }

            @Override
            public void onDirectoryLoaded(Uri uri) {
                BaseActivity.this.removeEventListener(this);
                BaseActivity.this.getMainLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                    @Override
                    public boolean queueIdle() {
                        if (BaseActivity.this.getIntent().getBooleanExtra("com.android.documentsui.benchmark", false) && "com.android.documentsui.appperftests".equals(BaseActivity.this.getCallingPackage())) {
                            BaseActivity.this.setResult(-1);
                            BaseActivity.this.finish();
                        }
                        Metrics.logStartupMs(BaseActivity.this, (int) (new Date().getTime() - BaseActivity.this.mStartTime));
                        return false;
                    }
                });
            }
        });
    }

    public static final class RetainedState {
        public Selection selection;

        public boolean hasSelection() {
            return this.selection != null;
        }
    }
}
