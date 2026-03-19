package android.app;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.BackStackRecord;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DebugUtils;
import android.util.Log;
import android.util.LogWriter;
import android.util.SparseArray;
import android.util.SuperNotCalledException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.internal.R;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class FragmentManagerImpl extends FragmentManager implements LayoutInflater.Factory2 {
    static boolean DEBUG = false;
    static final String TAG = "FragmentManager";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    ArrayList<Fragment> mActive;
    ArrayList<Fragment> mAdded;
    ArrayList<Integer> mAvailBackStackIndices;
    ArrayList<Integer> mAvailIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<FragmentManager.OnBackStackChangedListener> mBackStackChangeListeners;
    ArrayList<BackStackRecord> mBackStackIndices;
    FragmentContainer mContainer;
    FragmentController mController;
    ArrayList<Fragment> mCreatedMenus;
    boolean mDestroyed;
    boolean mExecutingActions;
    boolean mHavePendingDeferredStart;
    FragmentHostCallback<?> mHost;
    boolean mNeedMenuInvalidate;
    String mNoTransactionsBecause;
    Fragment mParent;
    ArrayList<Runnable> mPendingActions;
    boolean mStateSaved;
    Runnable[] mTmpActions;
    int mCurState = 0;
    Bundle mStateBundle = null;
    SparseArray<Parcelable> mStateArray = null;
    Runnable mExecCommit = new Runnable() {
        @Override
        public void run() throws SuperNotCalledException {
            FragmentManagerImpl.this.execPendingActions();
        }
    };

    FragmentManagerImpl() {
    }

    static class AnimateOnHWLayerIfNeededListener implements Animator.AnimatorListener {
        private boolean mShouldRunOnHWLayer = false;
        private View mView;

        public AnimateOnHWLayerIfNeededListener(View v) {
            if (v == null) {
                return;
            }
            this.mView = v;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            this.mShouldRunOnHWLayer = FragmentManagerImpl.shouldRunOnHWLayer(this.mView, animation);
            if (!this.mShouldRunOnHWLayer) {
                return;
            }
            this.mView.setLayerType(2, null);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (this.mShouldRunOnHWLayer) {
                this.mView.setLayerType(0, null);
            }
            this.mView = null;
            animation.removeListener(this);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        LogWriter logw = new LogWriter(6, TAG);
        FastPrintWriter fastPrintWriter = new FastPrintWriter(logw, false, 1024);
        if (this.mHost != null) {
            Log.e(TAG, "Activity state:");
            try {
                this.mHost.onDump("  ", null, fastPrintWriter, new String[0]);
            } catch (Exception e) {
                fastPrintWriter.flush();
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            Log.e(TAG, "Fragment manager state:");
            try {
                dump("  ", null, fastPrintWriter, new String[0]);
            } catch (Exception e2) {
                fastPrintWriter.flush();
                Log.e(TAG, "Failed dumping state", e2);
            }
        }
        fastPrintWriter.flush();
        throw ex;
    }

    static boolean modifiesAlpha(Animator anim) {
        if (anim == null) {
            return false;
        }
        if (anim instanceof ValueAnimator) {
            ValueAnimator valueAnim = (ValueAnimator) anim;
            PropertyValuesHolder[] values = valueAnim.getValues();
            for (PropertyValuesHolder propertyValuesHolder : values) {
                if ("alpha".equals(propertyValuesHolder.getPropertyName())) {
                    return true;
                }
            }
        } else if (anim instanceof AnimatorSet) {
            List<Animator> animList = ((AnimatorSet) anim).getChildAnimations();
            for (int i = 0; i < animList.size(); i++) {
                if (modifiesAlpha(animList.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, Animator anim) {
        if (v == null || anim == null || v.getLayerType() != 0 || !v.hasOverlappingRendering()) {
            return false;
        }
        return modifiesAlpha(anim);
    }

    private void setHWLayerAnimListenerIfAlpha(View v, Animator anim) {
        if (v == null || anim == null || !shouldRunOnHWLayer(v, anim)) {
            return;
        }
        anim.addListener(new AnimateOnHWLayerIfNeededListener(v));
    }

    @Override
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    @Override
    public boolean executePendingTransactions() {
        return execPendingActions();
    }

    @Override
    public void popBackStack() {
        enqueueAction(new Runnable() {
            @Override
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mHost.getHandler(), null, -1, 0);
            }
        }, false);
    }

    @Override
    public boolean popBackStackImmediate() {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(this.mHost.getHandler(), null, -1, 0);
    }

    @Override
    public void popBackStack(final String name, final int flags) {
        enqueueAction(new Runnable() {
            @Override
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mHost.getHandler(), name, -1, flags);
            }
        }, false);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(this.mHost.getHandler(), name, -1, flags);
    }

    @Override
    public void popBackStack(final int id, final int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new Runnable() {
            @Override
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mHost.getHandler(), null, id, flags);
            }
        }, false);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        executePendingTransactions();
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackState(this.mHost.getHandler(), null, id, flags);
    }

    @Override
    public int getBackStackEntryCount() {
        if (this.mBackStack != null) {
            return this.mBackStack.size();
        }
        return 0;
    }

    @Override
    public FragmentManager.BackStackEntry getBackStackEntryAt(int index) {
        return this.mBackStack.get(index);
    }

    @Override
    public void addOnBackStackChangedListener(FragmentManager.OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners == null) {
            this.mBackStackChangeListeners = new ArrayList<>();
        }
        this.mBackStackChangeListeners.add(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(FragmentManager.OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners == null) {
            return;
        }
        this.mBackStackChangeListeners.remove(listener);
    }

    @Override
    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager"));
        }
        bundle.putInt(key, fragment.mIndex);
    }

    @Override
    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        if (index >= this.mActive.size()) {
            throwException(new IllegalStateException("Fragment no longer exists for key " + key + ": index " + index));
        }
        Fragment f = this.mActive.get(index);
        if (f == null) {
            throwException(new IllegalStateException("Fragment no longer exists for key " + key + ": index " + index));
        }
        return f;
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(Fragment fragment) {
        Bundle result;
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager"));
        }
        if (fragment.mState <= 0 || (result = saveFragmentBasicState(fragment)) == null) {
            return null;
        }
        return new Fragment.SavedState(result);
    }

    @Override
    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (this.mParent != null) {
            DebugUtils.buildShortClassTag(this.mParent, sb);
        } else {
            DebugUtils.buildShortClassTag(this.mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        int N;
        int N2;
        int N3;
        int N4;
        int N5;
        int N6;
        String innerPrefix = prefix + "    ";
        if (this.mActive != null && (N6 = this.mActive.size()) > 0) {
            writer.print(prefix);
            writer.print("Active Fragments in ");
            writer.print(Integer.toHexString(System.identityHashCode(this)));
            writer.println(":");
            for (int i = 0; i < N6; i++) {
                Fragment f = this.mActive.get(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.print(": ");
                writer.println(f);
                if (f != null) {
                    f.dump(innerPrefix, fd, writer, args);
                }
            }
        }
        if (this.mAdded != null && (N5 = this.mAdded.size()) > 0) {
            writer.print(prefix);
            writer.println("Added Fragments:");
            for (int i2 = 0; i2 < N5; i2++) {
                Fragment f2 = this.mAdded.get(i2);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i2);
                writer.print(": ");
                writer.println(f2.toString());
            }
        }
        if (this.mCreatedMenus != null && (N4 = this.mCreatedMenus.size()) > 0) {
            writer.print(prefix);
            writer.println("Fragments Created Menus:");
            for (int i3 = 0; i3 < N4; i3++) {
                Fragment f3 = this.mCreatedMenus.get(i3);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i3);
                writer.print(": ");
                writer.println(f3.toString());
            }
        }
        if (this.mBackStack != null && (N3 = this.mBackStack.size()) > 0) {
            writer.print(prefix);
            writer.println("Back Stack:");
            for (int i4 = 0; i4 < N3; i4++) {
                BackStackRecord bs = this.mBackStack.get(i4);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i4);
                writer.print(": ");
                writer.println(bs.toString());
                bs.dump(innerPrefix, fd, writer, args);
            }
        }
        synchronized (this) {
            if (this.mBackStackIndices != null && (N2 = this.mBackStackIndices.size()) > 0) {
                writer.print(prefix);
                writer.println("Back Stack Indices:");
                for (int i5 = 0; i5 < N2; i5++) {
                    BackStackRecord bs2 = this.mBackStackIndices.get(i5);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i5);
                    writer.print(": ");
                    writer.println(bs2);
                }
            }
            if (this.mAvailBackStackIndices != null && this.mAvailBackStackIndices.size() > 0) {
                writer.print(prefix);
                writer.print("mAvailBackStackIndices: ");
                writer.println(Arrays.toString(this.mAvailBackStackIndices.toArray()));
            }
        }
        if (this.mPendingActions != null && (N = this.mPendingActions.size()) > 0) {
            writer.print(prefix);
            writer.println("Pending Actions:");
            for (int i6 = 0; i6 < N; i6++) {
                Runnable r = this.mPendingActions.get(i6);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i6);
                writer.print(": ");
                writer.println(r);
            }
        }
        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(this.mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(this.mContainer);
        if (this.mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(this.mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(this.mCurState);
        writer.print(" mStateSaved=");
        writer.print(this.mStateSaved);
        writer.print(" mDestroyed=");
        writer.println(this.mDestroyed);
        if (this.mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(this.mNeedMenuInvalidate);
        }
        if (this.mNoTransactionsBecause != null) {
            writer.print(prefix);
            writer.print("  mNoTransactionsBecause=");
            writer.println(this.mNoTransactionsBecause);
        }
        if (this.mAvailIndices == null || this.mAvailIndices.size() <= 0) {
            return;
        }
        writer.print(prefix);
        writer.print("  mAvailIndices: ");
        writer.println(Arrays.toString(this.mAvailIndices.toArray()));
    }

    Animator loadAnimator(Fragment fragment, int transit, boolean enter, int transitionStyle) {
        int styleIndex;
        Animator anim;
        Animator animObj = fragment.onCreateAnimator(transit, enter, fragment.mNextAnim);
        if (animObj != null) {
            return animObj;
        }
        if (fragment.mNextAnim != 0 && (anim = AnimatorInflater.loadAnimator(this.mHost.getContext(), fragment.mNextAnim)) != null) {
            return anim;
        }
        if (transit == 0 || (styleIndex = transitToStyleIndex(transit, enter)) < 0) {
            return null;
        }
        if (transitionStyle == 0 && this.mHost.onHasWindowAnimations()) {
            transitionStyle = this.mHost.onGetWindowAnimations();
        }
        if (transitionStyle == 0) {
            return null;
        }
        TypedArray attrs = this.mHost.getContext().obtainStyledAttributes(transitionStyle, R.styleable.FragmentAnimation);
        int anim2 = attrs.getResourceId(styleIndex, 0);
        attrs.recycle();
        if (anim2 == 0) {
            return null;
        }
        return AnimatorInflater.loadAnimator(this.mHost.getContext(), anim2);
    }

    public void performPendingDeferredStart(Fragment f) throws SuperNotCalledException {
        if (!f.mDeferStart) {
            return;
        }
        if (this.mExecutingActions) {
            this.mHavePendingDeferredStart = true;
        } else {
            f.mDeferStart = false;
            moveToState(f, this.mCurState, 0, 0, false);
        }
    }

    boolean isStateAtLeast(int state) {
        return this.mCurState >= state;
    }

    void moveToState(final android.app.Fragment r15, int r16, int r17, int r18, boolean r19) throws android.util.SuperNotCalledException {
        throw new UnsupportedOperationException("Method not decompiled: android.app.FragmentManagerImpl.moveToState(android.app.Fragment, int, int, int, boolean):void");
    }

    void moveToState(Fragment f) throws SuperNotCalledException {
        moveToState(f, this.mCurState, 0, 0, false);
    }

    void moveToState(int newState, boolean always) throws SuperNotCalledException {
        moveToState(newState, 0, 0, always);
    }

    void moveToState(int newState, int transit, int transitStyle, boolean always) throws SuperNotCalledException {
        if (this.mHost == null && newState != 0) {
            throw new IllegalStateException("No activity");
        }
        if (!always && this.mCurState == newState) {
            return;
        }
        this.mCurState = newState;
        if (this.mActive == null) {
            return;
        }
        boolean loadersRunning = false;
        for (int i = 0; i < this.mActive.size(); i++) {
            Fragment f = this.mActive.get(i);
            if (f != null) {
                moveToState(f, newState, transit, transitStyle, false);
                if (f.mLoaderManager != null) {
                    loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                }
            }
        }
        if (!loadersRunning) {
            startPendingDeferredFragments();
        }
        if (!this.mNeedMenuInvalidate || this.mHost == null || this.mCurState != 5) {
            return;
        }
        this.mHost.onInvalidateOptionsMenu();
        this.mNeedMenuInvalidate = false;
    }

    void startPendingDeferredFragments() throws SuperNotCalledException {
        if (this.mActive == null) {
            return;
        }
        for (int i = 0; i < this.mActive.size(); i++) {
            Fragment f = this.mActive.get(i);
            if (f != null) {
                performPendingDeferredStart(f);
            }
        }
    }

    void makeActive(Fragment f) {
        if (f.mIndex >= 0) {
            return;
        }
        if (this.mAvailIndices == null || this.mAvailIndices.size() <= 0) {
            if (this.mActive == null) {
                this.mActive = new ArrayList<>();
            }
            f.setIndex(this.mActive.size(), this.mParent);
            this.mActive.add(f);
        } else {
            f.setIndex(this.mAvailIndices.remove(this.mAvailIndices.size() - 1).intValue(), this.mParent);
            this.mActive.set(f.mIndex, f);
        }
        if (DEBUG) {
            Log.v(TAG, "Allocated fragment index " + f);
        }
    }

    void makeInactive(Fragment f) {
        if (f.mIndex < 0) {
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "Freeing fragment index " + f);
        }
        this.mActive.set(f.mIndex, null);
        if (this.mAvailIndices == null) {
            this.mAvailIndices = new ArrayList<>();
        }
        this.mAvailIndices.add(Integer.valueOf(f.mIndex));
        this.mHost.inactivateFragment(f.mWho);
        f.initState();
    }

    public void addFragment(Fragment fragment, boolean moveToStateNow) throws SuperNotCalledException {
        if (this.mAdded == null) {
            this.mAdded = new ArrayList<>();
        }
        if (DEBUG) {
            Log.v(TAG, "add: " + fragment);
        }
        makeActive(fragment);
        if (fragment.mDetached) {
            return;
        }
        if (this.mAdded.contains(fragment)) {
            throw new IllegalStateException("Fragment already added: " + fragment);
        }
        this.mAdded.add(fragment);
        fragment.mAdded = true;
        fragment.mRemoving = false;
        if (fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        if (!moveToStateNow) {
            return;
        }
        moveToState(fragment);
    }

    public void removeFragment(Fragment fragment, int transition, int transitionStyle) throws SuperNotCalledException {
        if (DEBUG) {
            Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        }
        boolean inactive = !fragment.isInBackStack();
        if (fragment.mDetached && !inactive) {
            return;
        }
        if (this.mAdded != null) {
            this.mAdded.remove(fragment);
        }
        if (fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        fragment.mAdded = false;
        fragment.mRemoving = true;
        moveToState(fragment, inactive ? 0 : 1, transition, transitionStyle, false);
    }

    public void hideFragment(final Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "hide: " + fragment);
        }
        if (fragment.mHidden) {
            return;
        }
        fragment.mHidden = true;
        if (fragment.mView != null) {
            Animator anim = loadAnimator(fragment, transition, false, transitionStyle);
            if (anim != null) {
                anim.setTarget(fragment.mView);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (fragment.mView == null) {
                            return;
                        }
                        fragment.mView.setVisibility(8);
                    }
                });
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.start();
            } else {
                fragment.mView.setVisibility(8);
            }
        }
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        fragment.onHiddenChanged(true);
    }

    public void showFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "show: " + fragment);
        }
        if (!fragment.mHidden) {
            return;
        }
        fragment.mHidden = false;
        if (fragment.mView != null) {
            Animator anim = loadAnimator(fragment, transition, true, transitionStyle);
            if (anim != null) {
                anim.setTarget(fragment.mView);
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.start();
            }
            fragment.mView.setVisibility(0);
        }
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        fragment.onHiddenChanged(false);
    }

    public void detachFragment(Fragment fragment, int transition, int transitionStyle) throws SuperNotCalledException {
        if (DEBUG) {
            Log.v(TAG, "detach: " + fragment);
        }
        if (fragment.mDetached) {
            return;
        }
        fragment.mDetached = true;
        if (!fragment.mAdded) {
            return;
        }
        if (this.mAdded != null) {
            if (DEBUG) {
                Log.v(TAG, "remove from detach: " + fragment);
            }
            this.mAdded.remove(fragment);
        }
        if (fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        fragment.mAdded = false;
        moveToState(fragment, 1, transition, transitionStyle, false);
    }

    public void attachFragment(Fragment fragment, int transition, int transitionStyle) throws SuperNotCalledException {
        if (DEBUG) {
            Log.v(TAG, "attach: " + fragment);
        }
        if (!fragment.mDetached) {
            return;
        }
        fragment.mDetached = false;
        if (fragment.mAdded) {
            return;
        }
        if (this.mAdded == null) {
            this.mAdded = new ArrayList<>();
        }
        if (this.mAdded.contains(fragment)) {
            throw new IllegalStateException("Fragment already added: " + fragment);
        }
        if (DEBUG) {
            Log.v(TAG, "add from attach: " + fragment);
        }
        this.mAdded.add(fragment);
        fragment.mAdded = true;
        if (fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        moveToState(fragment, this.mCurState, transition, transitionStyle, false);
    }

    @Override
    public Fragment findFragmentById(int id) {
        if (this.mAdded != null) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        if (this.mActive != null) {
            for (int i2 = this.mActive.size() - 1; i2 >= 0; i2--) {
                Fragment f2 = this.mActive.get(i2);
                if (f2 != null && f2.mFragmentId == id) {
                    return f2;
                }
            }
        }
        return null;
    }

    @Override
    public Fragment findFragmentByTag(String tag) {
        if (this.mAdded != null && tag != null) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = this.mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (this.mActive != null && tag != null) {
            for (int i2 = this.mActive.size() - 1; i2 >= 0; i2--) {
                Fragment f2 = this.mActive.get(i2);
                if (f2 != null && tag.equals(f2.mTag)) {
                    return f2;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByWho(String who) {
        Fragment f;
        if (this.mActive != null && who != null) {
            for (int i = this.mActive.size() - 1; i >= 0; i--) {
                Fragment f2 = this.mActive.get(i);
                if (f2 != null && (f = f2.findFragmentByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (this.mStateSaved) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        }
        if (this.mNoTransactionsBecause == null) {
        } else {
            throw new IllegalStateException("Can not perform this action inside of " + this.mNoTransactionsBecause);
        }
    }

    public void enqueueAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (this.mDestroyed || this.mHost == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (this.mPendingActions == null) {
                this.mPendingActions = new ArrayList<>();
            }
            this.mPendingActions.add(action);
            if (this.mPendingActions.size() == 1) {
                this.mHost.getHandler().removeCallbacks(this.mExecCommit);
                this.mHost.getHandler().post(this.mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (this.mAvailBackStackIndices == null || this.mAvailBackStackIndices.size() <= 0) {
                if (this.mBackStackIndices == null) {
                    this.mBackStackIndices = new ArrayList<>();
                }
                int index = this.mBackStackIndices.size();
                if (DEBUG) {
                    Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                }
                this.mBackStackIndices.add(bse);
                return index;
            }
            int index2 = this.mAvailBackStackIndices.remove(this.mAvailBackStackIndices.size() - 1).intValue();
            if (DEBUG) {
                Log.v(TAG, "Adding back stack index " + index2 + " with " + bse);
            }
            this.mBackStackIndices.set(index2, bse);
            return index2;
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (this.mBackStackIndices == null) {
                this.mBackStackIndices = new ArrayList<>();
            }
            int N = this.mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) {
                    Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                }
                this.mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    this.mBackStackIndices.add(null);
                    if (this.mAvailBackStackIndices == null) {
                        this.mAvailBackStackIndices = new ArrayList<>();
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Adding available back stack index " + N);
                    }
                    this.mAvailBackStackIndices.add(Integer.valueOf(N));
                    N++;
                }
                if (DEBUG) {
                    Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                }
                this.mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            this.mBackStackIndices.set(index, null);
            if (this.mAvailBackStackIndices == null) {
                this.mAvailBackStackIndices = new ArrayList<>();
            }
            if (DEBUG) {
                Log.v(TAG, "Freeing back stack index " + index);
            }
            this.mAvailBackStackIndices.add(Integer.valueOf(index));
        }
    }

    public void execSingleAction(Runnable action, boolean allowStateLoss) throws SuperNotCalledException {
        if (this.mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }
        if (Looper.myLooper() != this.mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }
        if (!allowStateLoss) {
            checkStateLoss();
        }
        this.mExecutingActions = true;
        action.run();
        this.mExecutingActions = false;
        doPendingDeferredStart();
    }

    public boolean execPendingActions() throws SuperNotCalledException {
        int numActions;
        if (this.mExecutingActions) {
            throw new IllegalStateException("Recursive entry to executePendingTransactions");
        }
        if (Looper.myLooper() != this.mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of process");
        }
        boolean didSomething = false;
        while (true) {
            synchronized (this) {
                if (this.mPendingActions == null || this.mPendingActions.size() == 0) {
                    break;
                }
                numActions = this.mPendingActions.size();
                if (this.mTmpActions == null || this.mTmpActions.length < numActions) {
                    this.mTmpActions = new Runnable[numActions];
                }
                this.mPendingActions.toArray(this.mTmpActions);
                this.mPendingActions.clear();
                this.mHost.getHandler().removeCallbacks(this.mExecCommit);
            }
            this.mExecutingActions = true;
            for (int i = 0; i < numActions; i++) {
                this.mTmpActions[i].run();
                this.mTmpActions[i] = null;
            }
            this.mExecutingActions = false;
            didSomething = true;
        }
        doPendingDeferredStart();
        return didSomething;
    }

    void doPendingDeferredStart() throws SuperNotCalledException {
        if (!this.mHavePendingDeferredStart) {
            return;
        }
        boolean loadersRunning = false;
        for (int i = 0; i < this.mActive.size(); i++) {
            Fragment f = this.mActive.get(i);
            if (f != null && f.mLoaderManager != null) {
                loadersRunning |= f.mLoaderManager.hasRunningLoaders();
            }
        }
        if (loadersRunning) {
            return;
        }
        this.mHavePendingDeferredStart = false;
        startPendingDeferredFragments();
    }

    void reportBackStackChanged() {
        if (this.mBackStackChangeListeners == null) {
            return;
        }
        for (int i = 0; i < this.mBackStackChangeListeners.size(); i++) {
            this.mBackStackChangeListeners.get(i).onBackStackChanged();
        }
    }

    void addBackStackState(BackStackRecord state) {
        if (this.mBackStack == null) {
            this.mBackStack = new ArrayList<>();
        }
        this.mBackStack.add(state);
        reportBackStackChanged();
    }

    boolean popBackStackState(Handler handler, String name, int id, int flags) {
        if (this.mBackStack == null) {
            return false;
        }
        if (name == null && id < 0 && (flags & 1) == 0) {
            int last = this.mBackStack.size() - 1;
            if (last < 0) {
                return false;
            }
            BackStackRecord bss = this.mBackStack.remove(last);
            SparseArray<Fragment> firstOutFragments = new SparseArray<>();
            SparseArray<Fragment> lastInFragments = new SparseArray<>();
            if (this.mCurState >= 1) {
                bss.calculateBackFragments(firstOutFragments, lastInFragments);
            }
            bss.popFromBackStack(true, null, firstOutFragments, lastInFragments);
            reportBackStackChanged();
            return true;
        }
        int index = -1;
        if (name != null || id >= 0) {
            index = this.mBackStack.size() - 1;
            while (index >= 0) {
                BackStackRecord bss2 = this.mBackStack.get(index);
                if ((name != null && name.equals(bss2.getName())) || (id >= 0 && id == bss2.mIndex)) {
                    break;
                }
                index--;
            }
            if (index < 0) {
                return false;
            }
            if ((flags & 1) != 0) {
                index--;
                while (index >= 0) {
                    BackStackRecord bss3 = this.mBackStack.get(index);
                    if ((name == null || !name.equals(bss3.getName())) && (id < 0 || id != bss3.mIndex)) {
                        break;
                    }
                    index--;
                }
            }
        }
        if (index == this.mBackStack.size() - 1) {
            return false;
        }
        ArrayList<BackStackRecord> states = new ArrayList<>();
        for (int i = this.mBackStack.size() - 1; i > index; i--) {
            states.add(this.mBackStack.remove(i));
        }
        int LAST = states.size() - 1;
        SparseArray<Fragment> firstOutFragments2 = new SparseArray<>();
        SparseArray<Fragment> lastInFragments2 = new SparseArray<>();
        if (this.mCurState >= 1) {
            for (int i2 = 0; i2 <= LAST; i2++) {
                states.get(i2).calculateBackFragments(firstOutFragments2, lastInFragments2);
            }
        }
        BackStackRecord.TransitionState state = null;
        int i3 = 0;
        while (i3 <= LAST) {
            if (DEBUG) {
                Log.v(TAG, "Popping back stack state: " + states.get(i3));
            }
            state = states.get(i3).popFromBackStack(i3 == LAST, state, firstOutFragments2, lastInFragments2);
            i3++;
        }
        reportBackStackChanged();
        return true;
    }

    FragmentManagerNonConfig retainNonConfig() {
        FragmentManagerNonConfig child;
        ArrayList<Fragment> fragments = null;
        ArrayList<FragmentManagerNonConfig> childFragments = null;
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = this.mActive.get(i);
                if (f != null) {
                    if (f.mRetainInstance) {
                        if (fragments == null) {
                            fragments = new ArrayList<>();
                        }
                        fragments.add(f);
                        f.mRetaining = true;
                        f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                        if (DEBUG) {
                            Log.v(TAG, "retainNonConfig: keeping retained " + f);
                        }
                    }
                    boolean addedChild = false;
                    if (f.mChildFragmentManager != null && (child = f.mChildFragmentManager.retainNonConfig()) != null) {
                        if (childFragments == null) {
                            childFragments = new ArrayList<>();
                            for (int j = 0; j < i; j++) {
                                childFragments.add(null);
                            }
                        }
                        childFragments.add(child);
                        addedChild = true;
                    }
                    if (childFragments != null && !addedChild) {
                        childFragments.add(null);
                    }
                }
            }
        }
        if (fragments == null && childFragments == null) {
            return null;
        }
        return new FragmentManagerNonConfig(fragments, childFragments);
    }

    void saveFragmentViewState(Fragment f) {
        if (f.mView == null) {
            return;
        }
        if (this.mStateArray == null) {
            this.mStateArray = new SparseArray<>();
        } else {
            this.mStateArray.clear();
        }
        f.mView.saveHierarchyState(this.mStateArray);
        if (this.mStateArray.size() <= 0) {
            return;
        }
        f.mSavedViewState = this.mStateArray;
        this.mStateArray = null;
    }

    Bundle saveFragmentBasicState(Fragment f) {
        Bundle result = null;
        if (this.mStateBundle == null) {
            this.mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(this.mStateBundle);
        if (!this.mStateBundle.isEmpty()) {
            result = this.mStateBundle;
            this.mStateBundle = null;
        }
        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            result.putBoolean(USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }
        return result;
    }

    Parcelable saveAllState() throws SuperNotCalledException {
        int N;
        int N2;
        execPendingActions();
        this.mStateSaved = true;
        if (this.mActive == null || this.mActive.size() <= 0) {
            return null;
        }
        int N3 = this.mActive.size();
        FragmentState[] active = new FragmentState[N3];
        boolean haveFragments = false;
        for (int i = 0; i < N3; i++) {
            Fragment f = this.mActive.get(i);
            if (f != null) {
                if (f.mIndex < 0) {
                    throwException(new IllegalStateException("Failure saving state: active " + f + " has cleared index: " + f.mIndex));
                }
                haveFragments = true;
                FragmentState fs = new FragmentState(f);
                active[i] = fs;
                if (f.mState > 0 && fs.mSavedFragmentState == null) {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);
                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            throwException(new IllegalStateException("Failure saving state: " + f + " has target not in fragment manager: " + f.mTarget));
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState, TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(TARGET_REQUEST_CODE_STATE_TAG, f.mTargetRequestCode);
                        }
                    }
                } else {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                }
                if (DEBUG) {
                    Log.v(TAG, "Saved state of " + f + ": " + fs.mSavedFragmentState);
                }
            }
        }
        if (!haveFragments) {
            if (DEBUG) {
                Log.v(TAG, "saveAllState: no fragments!");
            }
            return null;
        }
        int[] added = null;
        BackStackState[] backStack = null;
        if (this.mAdded != null && (N2 = this.mAdded.size()) > 0) {
            added = new int[N2];
            for (int i2 = 0; i2 < N2; i2++) {
                added[i2] = this.mAdded.get(i2).mIndex;
                if (added[i2] < 0) {
                    throwException(new IllegalStateException("Failure saving state: active " + this.mAdded.get(i2) + " has cleared index: " + added[i2]));
                }
                if (DEBUG) {
                    Log.v(TAG, "saveAllState: adding fragment #" + i2 + ": " + this.mAdded.get(i2));
                }
            }
        }
        if (this.mBackStack != null && (N = this.mBackStack.size()) > 0) {
            backStack = new BackStackState[N];
            for (int i3 = 0; i3 < N; i3++) {
                backStack[i3] = new BackStackState(this, this.mBackStack.get(i3));
                if (DEBUG) {
                    Log.v(TAG, "saveAllState: adding back stack #" + i3 + ": " + this.mBackStack.get(i3));
                }
            }
        }
        FragmentManagerState fms = new FragmentManagerState();
        fms.mActive = active;
        fms.mAdded = added;
        fms.mBackStack = backStack;
        return fms;
    }

    void restoreAllState(Parcelable state, FragmentManagerNonConfig nonConfig) {
        if (state == null) {
            return;
        }
        FragmentManagerState fms = (FragmentManagerState) state;
        if (fms.mActive == null) {
            return;
        }
        List<FragmentManagerNonConfig> childNonConfigs = null;
        if (nonConfig != null) {
            List<Fragment> nonConfigFragments = nonConfig.getFragments();
            childNonConfigs = nonConfig.getChildNonConfigs();
            int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
            for (int i = 0; i < count; i++) {
                Fragment f = nonConfigFragments.get(i);
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: re-attaching retained " + f);
                }
                FragmentState fs = fms.mActive[f.mIndex];
                fs.mInstance = f;
                f.mSavedViewState = null;
                f.mBackStackNesting = 0;
                f.mInLayout = false;
                f.mAdded = false;
                f.mTarget = null;
                if (fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState.setClassLoader(this.mHost.getContext().getClassLoader());
                    f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                    f.mSavedFragmentState = fs.mSavedFragmentState;
                }
            }
        }
        this.mActive = new ArrayList<>(fms.mActive.length);
        if (this.mAvailIndices != null) {
            this.mAvailIndices.clear();
        }
        for (int i2 = 0; i2 < fms.mActive.length; i2++) {
            FragmentState fs2 = fms.mActive[i2];
            if (fs2 != null) {
                FragmentManagerNonConfig childNonConfig = null;
                if (childNonConfigs != null && i2 < childNonConfigs.size()) {
                    childNonConfig = childNonConfigs.get(i2);
                }
                Fragment f2 = fs2.instantiate(this.mHost, this.mParent, childNonConfig);
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: active #" + i2 + ": " + f2);
                }
                this.mActive.add(f2);
                fs2.mInstance = null;
            } else {
                this.mActive.add(null);
                if (this.mAvailIndices == null) {
                    this.mAvailIndices = new ArrayList<>();
                }
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: avail #" + i2);
                }
                this.mAvailIndices.add(Integer.valueOf(i2));
            }
        }
        if (nonConfig != null) {
            List<Fragment> nonConfigFragments2 = nonConfig.getFragments();
            int count2 = nonConfigFragments2 != null ? nonConfigFragments2.size() : 0;
            for (int i3 = 0; i3 < count2; i3++) {
                Fragment f3 = nonConfigFragments2.get(i3);
                if (f3.mTargetIndex >= 0) {
                    if (f3.mTargetIndex < this.mActive.size()) {
                        f3.mTarget = this.mActive.get(f3.mTargetIndex);
                    } else {
                        Log.w(TAG, "Re-attaching retained fragment " + f3 + " target no longer exists: " + f3.mTargetIndex);
                        f3.mTarget = null;
                    }
                }
            }
        }
        if (fms.mAdded != null) {
            this.mAdded = new ArrayList<>(fms.mAdded.length);
            for (int i4 = 0; i4 < fms.mAdded.length; i4++) {
                Fragment f4 = this.mActive.get(fms.mAdded[i4]);
                if (f4 == null) {
                    throwException(new IllegalStateException("No instantiated fragment for index #" + fms.mAdded[i4]));
                }
                f4.mAdded = true;
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: added #" + i4 + ": " + f4);
                }
                if (this.mAdded.contains(f4)) {
                    throw new IllegalStateException("Already added!");
                }
                this.mAdded.add(f4);
            }
        } else {
            this.mAdded = null;
        }
        if (fms.mBackStack == null) {
            this.mBackStack = null;
            return;
        }
        this.mBackStack = new ArrayList<>(fms.mBackStack.length);
        for (int i5 = 0; i5 < fms.mBackStack.length; i5++) {
            BackStackRecord bse = fms.mBackStack[i5].instantiate(this);
            if (DEBUG) {
                Log.v(TAG, "restoreAllState: back stack #" + i5 + " (index " + bse.mIndex + "): " + bse);
                LogWriter logw = new LogWriter(2, TAG);
                FastPrintWriter fastPrintWriter = new FastPrintWriter(logw, false, 1024);
                bse.dump("  ", fastPrintWriter, false);
                fastPrintWriter.flush();
            }
            this.mBackStack.add(bse);
            if (bse.mIndex >= 0) {
                setBackStackIndex(bse.mIndex, bse);
            }
        }
    }

    public void attachController(FragmentHostCallback<?> host, FragmentContainer container, Fragment parent) {
        if (this.mHost != null) {
            throw new IllegalStateException("Already attached");
        }
        this.mHost = host;
        this.mContainer = container;
        this.mParent = parent;
    }

    public void noteStateNotSaved() {
        this.mStateSaved = false;
    }

    public void dispatchCreate() throws SuperNotCalledException {
        this.mStateSaved = false;
        moveToState(1, false);
    }

    public void dispatchActivityCreated() throws SuperNotCalledException {
        this.mStateSaved = false;
        moveToState(2, false);
    }

    public void dispatchStart() throws SuperNotCalledException {
        this.mStateSaved = false;
        moveToState(4, false);
    }

    public void dispatchResume() throws SuperNotCalledException {
        this.mStateSaved = false;
        moveToState(5, false);
    }

    public void dispatchPause() throws SuperNotCalledException {
        moveToState(4, false);
    }

    public void dispatchStop() throws SuperNotCalledException {
        moveToState(3, false);
    }

    public void dispatchDestroyView() throws SuperNotCalledException {
        moveToState(1, false);
    }

    public void dispatchDestroy() throws SuperNotCalledException {
        this.mDestroyed = true;
        execPendingActions();
        moveToState(0, false);
        this.mHost = null;
        this.mContainer = null;
        this.mParent = null;
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (this.mAdded == null) {
            return;
        }
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = this.mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (this.mAdded == null) {
            return;
        }
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = this.mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (this.mAdded == null) {
            return;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = this.mAdded.get(i);
            if (f != null) {
                f.performConfigurationChanged(newConfig);
            }
        }
    }

    public void dispatchLowMemory() {
        if (this.mAdded == null) {
            return;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = this.mAdded.get(i);
            if (f != null) {
                f.performLowMemory();
            }
        }
    }

    public void dispatchTrimMemory(int level) {
        if (this.mAdded == null) {
            return;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = this.mAdded.get(i);
            if (f != null) {
                f.performTrimMemory(level);
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        ArrayList<Fragment> newMenus = null;
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performCreateOptionsMenu(menu, inflater)) {
                    show = true;
                    if (newMenus == null) {
                        newMenus = new ArrayList<>();
                    }
                    newMenus.add(f);
                }
            }
        }
        if (this.mCreatedMenus != null) {
            for (int i2 = 0; i2 < this.mCreatedMenus.size(); i2++) {
                Fragment f2 = this.mCreatedMenus.get(i2);
                if (newMenus == null || !newMenus.contains(f2)) {
                    f2.onDestroyOptionsMenu();
                }
            }
        }
        this.mCreatedMenus = newMenus;
        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performPrepareOptionsMenu(menu)) {
                    show = true;
                }
            }
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performOptionsItemSelected(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performContextItemSelected(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (this.mAdded == null) {
            return;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = this.mAdded.get(i);
            if (f != null) {
                f.performOptionsMenuClosed(menu);
            }
        }
    }

    @Override
    public void invalidateOptionsMenu() {
        if (this.mHost != null && this.mCurState == 5) {
            this.mHost.onInvalidateOptionsMenu();
        } else {
            this.mNeedMenuInvalidate = true;
        }
    }

    public static int reverseTransit(int transit) {
        switch (transit) {
            case 4097:
                return 8194;
            case 4099:
                return 4099;
            case 8194:
                return 4097;
            default:
                return 0;
        }
    }

    public static int transitToStyleIndex(int transit, boolean enter) {
        switch (transit) {
            case 4097:
                if (enter) {
                    return 0;
                }
                return 1;
            case 4099:
                if (enter) {
                    return 4;
                }
                return 5;
            case 8194:
                if (enter) {
                    return 2;
                }
                return 3;
            default:
                return -1;
        }
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) throws SuperNotCalledException {
        if (!"fragment".equals(name)) {
            return null;
        }
        String fname = attrs.getAttributeValue(null, "class");
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Fragment);
        if (fname == null) {
            fname = a.getString(0);
        }
        int id = a.getResourceId(1, -1);
        String tag = a.getString(2);
        a.recycle();
        int containerId = parent != null ? parent.getId() : 0;
        if (containerId == -1 && id == -1 && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription() + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
        }
        Fragment fragment = id != -1 ? findFragmentById(id) : null;
        if (fragment == null && tag != null) {
            fragment = findFragmentByTag(tag);
        }
        if (fragment == null && containerId != -1) {
            fragment = findFragmentById(containerId);
        }
        if (DEBUG) {
            Log.v(TAG, "onCreateView: id=0x" + Integer.toHexString(id) + " fname=" + fname + " existing=" + fragment);
        }
        if (fragment == null) {
            fragment = Fragment.instantiate(context, fname);
            fragment.mFromLayout = true;
            fragment.mFragmentId = id != 0 ? id : containerId;
            fragment.mContainerId = containerId;
            fragment.mTag = tag;
            fragment.mInLayout = true;
            fragment.mFragmentManager = this;
            fragment.mHost = this.mHost;
            fragment.onInflate(this.mHost.getContext(), attrs, fragment.mSavedFragmentState);
            addFragment(fragment, true);
        } else {
            if (fragment.mInLayout) {
                throw new IllegalArgumentException(attrs.getPositionDescription() + ": Duplicate id 0x" + Integer.toHexString(id) + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId) + " with another fragment for " + fname);
            }
            fragment.mInLayout = true;
            fragment.mHost = this.mHost;
            if (!fragment.mRetaining) {
                fragment.onInflate(this.mHost.getContext(), attrs, fragment.mSavedFragmentState);
            }
        }
        if (this.mCurState >= 1 || !fragment.mFromLayout) {
            moveToState(fragment);
        } else {
            moveToState(fragment, 1, 0, 0, false);
        }
        if (fragment.mView == null) {
            throw new IllegalStateException("Fragment " + fname + " did not create a view.");
        }
        if (id != 0) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        return fragment.mView;
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    LayoutInflater.Factory2 getLayoutInflaterFactory() {
        return this;
    }
}
