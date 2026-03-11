package android.support.v4.app;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.ViewModelStore;
import android.arch.lifecycle.ViewModelStoreOwner;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.SparseArrayCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class FragmentActivity extends SupportActivity implements ViewModelStoreOwner {
    boolean mCreated;
    int mNextCandidateRequestIndex;
    SparseArrayCompat<String> mPendingFragmentActivityResults;
    boolean mResumed;
    boolean mRetaining;
    boolean mStartedActivityFromFragment;
    boolean mStartedIntentSenderFromFragment;
    private ViewModelStore mViewModelStore;
    final Handler mHandler = new Handler(this) {
        final FragmentActivity this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (this.this$0.mStopped) {
                        this.this$0.doReallyStop(false);
                    }
                    break;
                case 2:
                    this.this$0.onResumeFragments();
                    this.this$0.mFragments.execPendingActions();
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    };
    final FragmentController mFragments = FragmentController.createController(new HostCallbacks(this));
    boolean mStopped = true;
    boolean mReallyStopped = true;

    class HostCallbacks extends FragmentHostCallback<FragmentActivity> {
        final FragmentActivity this$0;

        public HostCallbacks(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
            this.this$0 = fragmentActivity;
        }

        @Override
        public void onAttachFragment(Fragment fragment) {
            this.this$0.onAttachFragment(fragment);
        }

        @Override
        public void onDump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            this.this$0.dump(str, fileDescriptor, printWriter, strArr);
        }

        @Override
        public View onFindViewById(int i) {
            return this.this$0.findViewById(i);
        }

        @Override
        public LayoutInflater onGetLayoutInflater() {
            return this.this$0.getLayoutInflater().cloneInContext(this.this$0);
        }

        @Override
        public int onGetWindowAnimations() {
            Window window = this.this$0.getWindow();
            if (window == null) {
                return 0;
            }
            return window.getAttributes().windowAnimations;
        }

        @Override
        public boolean onHasView() {
            Window window = this.this$0.getWindow();
            return (window == null || window.peekDecorView() == null) ? false : true;
        }

        @Override
        public boolean onHasWindowAnimations() {
            return this.this$0.getWindow() != null;
        }

        @Override
        public boolean onShouldSaveFragmentState(Fragment fragment) {
            return !this.this$0.isFinishing();
        }

        @Override
        public void onSupportInvalidateOptionsMenu() {
            this.this$0.supportInvalidateOptionsMenu();
        }
    }

    static final class NonConfigurationInstances {
        Object custom;
        FragmentManagerNonConfig fragments;
        ViewModelStore viewModelStore;

        NonConfigurationInstances() {
        }
    }

    static void checkForValidRequestCode(int i) {
        if (((-65536) & i) != 0) {
            throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
        }
    }

    private void markFragmentsCreated() {
        while (markState(getSupportFragmentManager(), Lifecycle.State.CREATED)) {
        }
    }

    private static boolean markState(FragmentManager fragmentManager, Lifecycle.State state) {
        boolean zMarkState = false;
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment != null) {
                if (fragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                    fragment.mLifecycleRegistry.markState(state);
                    zMarkState = true;
                }
                FragmentManager fragmentManagerPeekChildFragmentManager = fragment.peekChildFragmentManager();
                zMarkState = fragmentManagerPeekChildFragmentManager != null ? markState(fragmentManagerPeekChildFragmentManager, state) | zMarkState : zMarkState;
            }
        }
        return zMarkState;
    }

    final View dispatchFragmentsOnCreateView(View view, String str, Context context, AttributeSet attributeSet) {
        return this.mFragments.onCreateView(view, str, context, attributeSet);
    }

    void doReallyStop(boolean z) {
        if (this.mReallyStopped) {
            return;
        }
        this.mReallyStopped = true;
        this.mRetaining = z;
        this.mHandler.removeMessages(1);
        onReallyStop();
    }

    @Override
    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(str, fileDescriptor, printWriter, strArr);
        printWriter.print(str);
        printWriter.print("Local FragmentActivity ");
        printWriter.print(Integer.toHexString(System.identityHashCode(this)));
        printWriter.println(" State:");
        String str2 = str + "  ";
        printWriter.print(str2);
        printWriter.print("mCreated=");
        printWriter.print(this.mCreated);
        printWriter.print(" mResumed=");
        printWriter.print(this.mResumed);
        printWriter.print(" mStopped=");
        printWriter.print(this.mStopped);
        printWriter.print(" mReallyStopped=");
        printWriter.println(this.mReallyStopped);
        LoaderManager.getInstance(this).dump(str2, fileDescriptor, printWriter, strArr);
        this.mFragments.getSupportFragmentManager().dump(str, fileDescriptor, printWriter, strArr);
    }

    @Override
    public Lifecycle getLifecycle() {
        return super.getLifecycle();
    }

    public FragmentManager getSupportFragmentManager() {
        return this.mFragments.getSupportFragmentManager();
    }

    @Override
    public ViewModelStore getViewModelStore() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the Application instance. You can't request ViewModel before onCreate call.");
        }
        if (this.mViewModelStore == null) {
            this.mViewModelStore = new ViewModelStore();
        }
        return this.mViewModelStore;
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        this.mFragments.noteStateNotSaved();
        int i3 = i >> 16;
        if (i3 == 0) {
            ActivityCompat.PermissionCompatDelegate permissionCompatDelegate = ActivityCompat.getPermissionCompatDelegate();
            if (permissionCompatDelegate == null || !permissionCompatDelegate.onActivityResult(this, i, i2, intent)) {
                super.onActivityResult(i, i2, intent);
                return;
            }
            return;
        }
        int i4 = i3 - 1;
        String str = this.mPendingFragmentActivityResults.get(i4);
        this.mPendingFragmentActivityResults.remove(i4);
        if (str == null) {
            Log.w("FragmentActivity", "Activity result delivered for unknown Fragment.");
            return;
        }
        Fragment fragmentFindFragmentByWho = this.mFragments.findFragmentByWho(str);
        if (fragmentFindFragmentByWho != null) {
            fragmentFindFragmentByWho.onActivityResult(65535 & i, i2, intent);
            return;
        }
        Log.w("FragmentActivity", "Activity result no fragment exists for who: " + str);
    }

    public void onAttachFragment(Fragment fragment) {
    }

    @Override
    public void onBackPressed() {
        FragmentManager supportFragmentManager = this.mFragments.getSupportFragmentManager();
        boolean zIsStateSaved = supportFragmentManager.isStateSaved();
        if (!zIsStateSaved || Build.VERSION.SDK_INT > 25) {
            if (zIsStateSaved || !supportFragmentManager.popBackStackImmediate()) {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mFragments.noteStateNotSaved();
        this.mFragments.dispatchConfigurationChanged(configuration);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        this.mFragments.attachHost(null);
        super.onCreate(bundle);
        NonConfigurationInstances nonConfigurationInstances = (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nonConfigurationInstances != null) {
            this.mViewModelStore = nonConfigurationInstances.viewModelStore;
        }
        if (bundle != null) {
            this.mFragments.restoreAllState(bundle.getParcelable("android:support:fragments"), nonConfigurationInstances != null ? nonConfigurationInstances.fragments : null);
            if (bundle.containsKey("android:support:next_request_index")) {
                this.mNextCandidateRequestIndex = bundle.getInt("android:support:next_request_index");
                int[] intArray = bundle.getIntArray("android:support:request_indicies");
                String[] stringArray = bundle.getStringArray("android:support:request_fragment_who");
                if (intArray == null || stringArray == null || intArray.length != stringArray.length) {
                    Log.w("FragmentActivity", "Invalid requestCode mapping in savedInstanceState.");
                } else {
                    this.mPendingFragmentActivityResults = new SparseArrayCompat<>(intArray.length);
                    for (int i = 0; i < intArray.length; i++) {
                        this.mPendingFragmentActivityResults.put(intArray[i], stringArray[i]);
                    }
                }
            }
        }
        if (this.mPendingFragmentActivityResults == null) {
            this.mPendingFragmentActivityResults = new SparseArrayCompat<>();
            this.mNextCandidateRequestIndex = 0;
        }
        this.mFragments.dispatchCreate();
    }

    @Override
    public boolean onCreatePanelMenu(int i, Menu menu) {
        return i == 0 ? super.onCreatePanelMenu(i, menu) | this.mFragments.dispatchCreateOptionsMenu(menu, getMenuInflater()) : super.onCreatePanelMenu(i, menu);
    }

    @Override
    public View onCreateView(View view, String str, Context context, AttributeSet attributeSet) {
        View viewDispatchFragmentsOnCreateView = dispatchFragmentsOnCreateView(view, str, context, attributeSet);
        return viewDispatchFragmentsOnCreateView == null ? super.onCreateView(view, str, context, attributeSet) : viewDispatchFragmentsOnCreateView;
    }

    @Override
    public View onCreateView(String str, Context context, AttributeSet attributeSet) {
        View viewDispatchFragmentsOnCreateView = dispatchFragmentsOnCreateView(null, str, context, attributeSet);
        return viewDispatchFragmentsOnCreateView == null ? super.onCreateView(str, context, attributeSet) : viewDispatchFragmentsOnCreateView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doReallyStop(false);
        if (this.mViewModelStore != null && !this.mRetaining) {
            this.mViewModelStore.clear();
        }
        this.mFragments.dispatchDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        this.mFragments.dispatchLowMemory();
    }

    @Override
    public boolean onMenuItemSelected(int i, MenuItem menuItem) {
        if (super.onMenuItemSelected(i, menuItem)) {
            return true;
        }
        if (i == 0) {
            return this.mFragments.dispatchOptionsItemSelected(menuItem);
        }
        if (i != 6) {
            return false;
        }
        return this.mFragments.dispatchContextItemSelected(menuItem);
    }

    @Override
    public void onMultiWindowModeChanged(boolean z) {
        this.mFragments.dispatchMultiWindowModeChanged(z);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.mFragments.noteStateNotSaved();
    }

    @Override
    public void onPanelClosed(int i, Menu menu) {
        if (i == 0) {
            this.mFragments.dispatchOptionsMenuClosed(menu);
        }
        super.onPanelClosed(i, menu);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mResumed = false;
        if (this.mHandler.hasMessages(2)) {
            this.mHandler.removeMessages(2);
            onResumeFragments();
        }
        this.mFragments.dispatchPause();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean z) {
        this.mFragments.dispatchPictureInPictureModeChanged(z);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        this.mHandler.removeMessages(2);
        onResumeFragments();
        this.mFragments.execPendingActions();
    }

    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        return super.onPreparePanel(0, view, menu);
    }

    @Override
    public boolean onPreparePanel(int i, View view, Menu menu) {
        return (i != 0 || menu == null) ? super.onPreparePanel(i, view, menu) : onPrepareOptionsPanel(view, menu) | this.mFragments.dispatchPrepareOptionsMenu(menu);
    }

    void onReallyStop() {
        this.mFragments.dispatchReallyStop();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        this.mFragments.noteStateNotSaved();
        int i2 = (i >> 16) & 65535;
        if (i2 != 0) {
            int i3 = i2 - 1;
            String str = this.mPendingFragmentActivityResults.get(i3);
            this.mPendingFragmentActivityResults.remove(i3);
            if (str == null) {
                Log.w("FragmentActivity", "Activity result delivered for unknown Fragment.");
                return;
            }
            Fragment fragmentFindFragmentByWho = this.mFragments.findFragmentByWho(str);
            if (fragmentFindFragmentByWho != null) {
                fragmentFindFragmentByWho.onRequestPermissionsResult(65535 & i, strArr, iArr);
                return;
            }
            Log.w("FragmentActivity", "Activity result no fragment exists for who: " + str);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mHandler.sendEmptyMessage(2);
        this.mResumed = true;
        this.mFragments.execPendingActions();
    }

    protected void onResumeFragments() {
        this.mFragments.dispatchResume();
    }

    public Object onRetainCustomNonConfigurationInstance() {
        return null;
    }

    @Override
    public final Object onRetainNonConfigurationInstance() {
        if (this.mStopped) {
            doReallyStop(true);
        }
        Object objOnRetainCustomNonConfigurationInstance = onRetainCustomNonConfigurationInstance();
        FragmentManagerNonConfig fragmentManagerNonConfigRetainNestedNonConfig = this.mFragments.retainNestedNonConfig();
        if (fragmentManagerNonConfigRetainNestedNonConfig == null && this.mViewModelStore == null && objOnRetainCustomNonConfigurationInstance == null) {
            return null;
        }
        NonConfigurationInstances nonConfigurationInstances = new NonConfigurationInstances();
        nonConfigurationInstances.custom = objOnRetainCustomNonConfigurationInstance;
        nonConfigurationInstances.viewModelStore = this.mViewModelStore;
        nonConfigurationInstances.fragments = fragmentManagerNonConfigRetainNestedNonConfig;
        return nonConfigurationInstances;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        markFragmentsCreated();
        Parcelable parcelableSaveAllState = this.mFragments.saveAllState();
        if (parcelableSaveAllState != null) {
            bundle.putParcelable("android:support:fragments", parcelableSaveAllState);
        }
        if (this.mPendingFragmentActivityResults.size() <= 0) {
            return;
        }
        bundle.putInt("android:support:next_request_index", this.mNextCandidateRequestIndex);
        int[] iArr = new int[this.mPendingFragmentActivityResults.size()];
        String[] strArr = new String[this.mPendingFragmentActivityResults.size()];
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.mPendingFragmentActivityResults.size()) {
                bundle.putIntArray("android:support:request_indicies", iArr);
                bundle.putStringArray("android:support:request_fragment_who", strArr);
                return;
            } else {
                iArr[i2] = this.mPendingFragmentActivityResults.keyAt(i2);
                strArr[i2] = this.mPendingFragmentActivityResults.valueAt(i2);
                i = i2 + 1;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mStopped = false;
        this.mReallyStopped = false;
        this.mHandler.removeMessages(1);
        if (!this.mCreated) {
            this.mCreated = true;
            this.mFragments.dispatchActivityCreated();
        }
        this.mFragments.noteStateNotSaved();
        this.mFragments.execPendingActions();
        this.mFragments.dispatchStart();
    }

    @Override
    public void onStateNotSaved() {
        this.mFragments.noteStateNotSaved();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mStopped = true;
        markFragmentsCreated();
        this.mHandler.sendEmptyMessage(1);
        this.mFragments.dispatchStop();
    }

    @Override
    public void startActivityForResult(Intent intent, int i) {
        if (!this.mStartedActivityFromFragment && i != -1) {
            checkForValidRequestCode(i);
        }
        super.startActivityForResult(intent, i);
    }

    @Override
    public void startActivityForResult(Intent intent, int i, Bundle bundle) {
        if (!this.mStartedActivityFromFragment && i != -1) {
            checkForValidRequestCode(i);
        }
        super.startActivityForResult(intent, i, bundle);
    }

    @Override
    public void startIntentSenderForResult(IntentSender intentSender, int i, Intent intent, int i2, int i3, int i4) throws IntentSender.SendIntentException {
        if (!this.mStartedIntentSenderFromFragment && i != -1) {
            checkForValidRequestCode(i);
        }
        super.startIntentSenderForResult(intentSender, i, intent, i2, i3, i4);
    }

    @Override
    public void startIntentSenderForResult(IntentSender intentSender, int i, Intent intent, int i2, int i3, int i4, Bundle bundle) throws IntentSender.SendIntentException {
        if (!this.mStartedIntentSenderFromFragment && i != -1) {
            checkForValidRequestCode(i);
        }
        super.startIntentSenderForResult(intentSender, i, intent, i2, i3, i4, bundle);
    }

    @Deprecated
    public void supportInvalidateOptionsMenu() {
        invalidateOptionsMenu();
    }
}
