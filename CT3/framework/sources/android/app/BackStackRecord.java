package android.app;

import android.app.FragmentManager;
import android.graphics.Rect;
import android.net.wifi.WifiEnterpriseConfig;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LogWriter;
import android.util.SparseArray;
import android.util.SuperNotCalledException;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

final class BackStackRecord extends FragmentTransaction implements FragmentManager.BackStackEntry, Runnable {
    static final int OP_ADD = 1;
    static final int OP_ATTACH = 7;
    static final int OP_DETACH = 6;
    static final int OP_HIDE = 4;
    static final int OP_NULL = 0;
    static final int OP_REMOVE = 3;
    static final int OP_REPLACE = 2;
    static final int OP_SHOW = 5;
    static final String TAG = "FragmentManager";
    boolean mAddToBackStack;
    int mBreadCrumbShortTitleRes;
    CharSequence mBreadCrumbShortTitleText;
    int mBreadCrumbTitleRes;
    CharSequence mBreadCrumbTitleText;
    boolean mCommitted;
    int mEnterAnim;
    int mExitAnim;
    Op mHead;
    final FragmentManagerImpl mManager;
    String mName;
    int mNumOp;
    int mPopEnterAnim;
    int mPopExitAnim;
    ArrayList<String> mSharedElementSourceNames;
    ArrayList<String> mSharedElementTargetNames;
    Op mTail;
    int mTransition;
    int mTransitionStyle;
    boolean mAllowAddToBackStack = true;
    int mIndex = -1;

    static final class Op {
        int cmd;
        int enterAnim;
        int exitAnim;
        Fragment fragment;
        Op next;
        int popEnterAnim;
        int popExitAnim;
        Op prev;
        ArrayList<Fragment> removed;

        Op() {
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("BackStackEntry{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        if (this.mIndex >= 0) {
            sb.append(" #");
            sb.append(this.mIndex);
        }
        if (this.mName != null) {
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.mName);
        }
        sb.append("}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        dump(prefix, writer, true);
    }

    void dump(String prefix, PrintWriter writer, boolean full) {
        String cmdStr;
        if (full) {
            writer.print(prefix);
            writer.print("mName=");
            writer.print(this.mName);
            writer.print(" mIndex=");
            writer.print(this.mIndex);
            writer.print(" mCommitted=");
            writer.println(this.mCommitted);
            if (this.mTransition != 0) {
                writer.print(prefix);
                writer.print("mTransition=#");
                writer.print(Integer.toHexString(this.mTransition));
                writer.print(" mTransitionStyle=#");
                writer.println(Integer.toHexString(this.mTransitionStyle));
            }
            if (this.mEnterAnim != 0 || this.mExitAnim != 0) {
                writer.print(prefix);
                writer.print("mEnterAnim=#");
                writer.print(Integer.toHexString(this.mEnterAnim));
                writer.print(" mExitAnim=#");
                writer.println(Integer.toHexString(this.mExitAnim));
            }
            if (this.mPopEnterAnim != 0 || this.mPopExitAnim != 0) {
                writer.print(prefix);
                writer.print("mPopEnterAnim=#");
                writer.print(Integer.toHexString(this.mPopEnterAnim));
                writer.print(" mPopExitAnim=#");
                writer.println(Integer.toHexString(this.mPopExitAnim));
            }
            if (this.mBreadCrumbTitleRes != 0 || this.mBreadCrumbTitleText != null) {
                writer.print(prefix);
                writer.print("mBreadCrumbTitleRes=#");
                writer.print(Integer.toHexString(this.mBreadCrumbTitleRes));
                writer.print(" mBreadCrumbTitleText=");
                writer.println(this.mBreadCrumbTitleText);
            }
            if (this.mBreadCrumbShortTitleRes != 0 || this.mBreadCrumbShortTitleText != null) {
                writer.print(prefix);
                writer.print("mBreadCrumbShortTitleRes=#");
                writer.print(Integer.toHexString(this.mBreadCrumbShortTitleRes));
                writer.print(" mBreadCrumbShortTitleText=");
                writer.println(this.mBreadCrumbShortTitleText);
            }
        }
        if (this.mHead == null) {
            return;
        }
        writer.print(prefix);
        writer.println("Operations:");
        String innerPrefix = prefix + "    ";
        Op op = this.mHead;
        int num = 0;
        while (op != null) {
            switch (op.cmd) {
                case 0:
                    cmdStr = WifiEnterpriseConfig.EMPTY_VALUE;
                    break;
                case 1:
                    cmdStr = "ADD";
                    break;
                case 2:
                    cmdStr = "REPLACE";
                    break;
                case 3:
                    cmdStr = "REMOVE";
                    break;
                case 4:
                    cmdStr = "HIDE";
                    break;
                case 5:
                    cmdStr = "SHOW";
                    break;
                case 6:
                    cmdStr = "DETACH";
                    break;
                case 7:
                    cmdStr = "ATTACH";
                    break;
                default:
                    cmdStr = "cmd=" + op.cmd;
                    break;
            }
            writer.print(prefix);
            writer.print("  Op #");
            writer.print(num);
            writer.print(": ");
            writer.print(cmdStr);
            writer.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            writer.println(op.fragment);
            if (full) {
                if (op.enterAnim != 0 || op.exitAnim != 0) {
                    writer.print(innerPrefix);
                    writer.print("enterAnim=#");
                    writer.print(Integer.toHexString(op.enterAnim));
                    writer.print(" exitAnim=#");
                    writer.println(Integer.toHexString(op.exitAnim));
                }
                if (op.popEnterAnim != 0 || op.popExitAnim != 0) {
                    writer.print(innerPrefix);
                    writer.print("popEnterAnim=#");
                    writer.print(Integer.toHexString(op.popEnterAnim));
                    writer.print(" popExitAnim=#");
                    writer.println(Integer.toHexString(op.popExitAnim));
                }
            }
            if (op.removed != null && op.removed.size() > 0) {
                for (int i = 0; i < op.removed.size(); i++) {
                    writer.print(innerPrefix);
                    if (op.removed.size() == 1) {
                        writer.print("Removed: ");
                    } else {
                        if (i == 0) {
                            writer.println("Removed:");
                        }
                        writer.print(innerPrefix);
                        writer.print("  #");
                        writer.print(i);
                        writer.print(": ");
                    }
                    writer.println(op.removed.get(i));
                }
            }
            op = op.next;
            num++;
        }
    }

    public BackStackRecord(FragmentManagerImpl manager) {
        this.mManager = manager;
    }

    @Override
    public int getId() {
        return this.mIndex;
    }

    @Override
    public int getBreadCrumbTitleRes() {
        return this.mBreadCrumbTitleRes;
    }

    @Override
    public int getBreadCrumbShortTitleRes() {
        return this.mBreadCrumbShortTitleRes;
    }

    @Override
    public CharSequence getBreadCrumbTitle() {
        if (this.mBreadCrumbTitleRes != 0) {
            return this.mManager.mHost.getContext().getText(this.mBreadCrumbTitleRes);
        }
        return this.mBreadCrumbTitleText;
    }

    @Override
    public CharSequence getBreadCrumbShortTitle() {
        if (this.mBreadCrumbShortTitleRes != 0) {
            return this.mManager.mHost.getContext().getText(this.mBreadCrumbShortTitleRes);
        }
        return this.mBreadCrumbShortTitleText;
    }

    void addOp(Op op) {
        if (this.mHead == null) {
            this.mTail = op;
            this.mHead = op;
        } else {
            op.prev = this.mTail;
            this.mTail.next = op;
            this.mTail = op;
        }
        op.enterAnim = this.mEnterAnim;
        op.exitAnim = this.mExitAnim;
        op.popEnterAnim = this.mPopEnterAnim;
        op.popExitAnim = this.mPopExitAnim;
        this.mNumOp++;
    }

    @Override
    public FragmentTransaction add(Fragment fragment, String tag) {
        doAddOp(0, fragment, tag, 1);
        return this;
    }

    @Override
    public FragmentTransaction add(int containerViewId, Fragment fragment) {
        doAddOp(containerViewId, fragment, null, 1);
        return this;
    }

    @Override
    public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
        doAddOp(containerViewId, fragment, tag, 1);
        return this;
    }

    private void doAddOp(int containerViewId, Fragment fragment, String tag, int opcmd) {
        fragment.mFragmentManager = this.mManager;
        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment " + fragment + ": was " + fragment.mTag + " now " + tag);
            }
            fragment.mTag = tag;
        }
        if (containerViewId != 0) {
            if (containerViewId == -1) {
                throw new IllegalArgumentException("Can't add fragment " + fragment + " with tag " + tag + " to container view with no id");
            }
            if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment " + fragment + ": was " + fragment.mFragmentId + " now " + containerViewId);
            }
            fragment.mFragmentId = containerViewId;
            fragment.mContainerId = containerViewId;
        }
        Op op = new Op();
        op.cmd = opcmd;
        op.fragment = fragment;
        addOp(op);
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Fragment fragment) {
        return replace(containerViewId, fragment, null);
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag) {
        if (containerViewId == 0) {
            throw new IllegalArgumentException("Must use non-zero containerViewId");
        }
        doAddOp(containerViewId, fragment, tag, 2);
        return this;
    }

    @Override
    public FragmentTransaction remove(Fragment fragment) {
        Op op = new Op();
        op.cmd = 3;
        op.fragment = fragment;
        addOp(op);
        return this;
    }

    @Override
    public FragmentTransaction hide(Fragment fragment) {
        Op op = new Op();
        op.cmd = 4;
        op.fragment = fragment;
        addOp(op);
        return this;
    }

    @Override
    public FragmentTransaction show(Fragment fragment) {
        Op op = new Op();
        op.cmd = 5;
        op.fragment = fragment;
        addOp(op);
        return this;
    }

    @Override
    public FragmentTransaction detach(Fragment fragment) {
        Op op = new Op();
        op.cmd = 6;
        op.fragment = fragment;
        addOp(op);
        return this;
    }

    @Override
    public FragmentTransaction attach(Fragment fragment) {
        Op op = new Op();
        op.cmd = 7;
        op.fragment = fragment;
        addOp(op);
        return this;
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit) {
        return setCustomAnimations(enter, exit, 0, 0);
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
        this.mEnterAnim = enter;
        this.mExitAnim = exit;
        this.mPopEnterAnim = popEnter;
        this.mPopExitAnim = popExit;
        return this;
    }

    @Override
    public FragmentTransaction setTransition(int transition) {
        this.mTransition = transition;
        return this;
    }

    @Override
    public FragmentTransaction addSharedElement(View sharedElement, String name) {
        String transitionName = sharedElement.getTransitionName();
        if (transitionName == null) {
            throw new IllegalArgumentException("Unique transitionNames are required for all sharedElements");
        }
        if (this.mSharedElementSourceNames == null) {
            this.mSharedElementSourceNames = new ArrayList<>();
            this.mSharedElementTargetNames = new ArrayList<>();
        }
        this.mSharedElementSourceNames.add(transitionName);
        this.mSharedElementTargetNames.add(name);
        return this;
    }

    @Override
    public FragmentTransaction setTransitionStyle(int styleRes) {
        this.mTransitionStyle = styleRes;
        return this;
    }

    @Override
    public FragmentTransaction addToBackStack(String name) {
        if (!this.mAllowAddToBackStack) {
            throw new IllegalStateException("This FragmentTransaction is not allowed to be added to the back stack.");
        }
        this.mAddToBackStack = true;
        this.mName = name;
        return this;
    }

    @Override
    public boolean isAddToBackStackAllowed() {
        return this.mAllowAddToBackStack;
    }

    @Override
    public FragmentTransaction disallowAddToBackStack() {
        if (this.mAddToBackStack) {
            throw new IllegalStateException("This transaction is already being added to the back stack");
        }
        this.mAllowAddToBackStack = false;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(int res) {
        this.mBreadCrumbTitleRes = res;
        this.mBreadCrumbTitleText = null;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(CharSequence text) {
        this.mBreadCrumbTitleRes = 0;
        this.mBreadCrumbTitleText = text;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(int res) {
        this.mBreadCrumbShortTitleRes = res;
        this.mBreadCrumbShortTitleText = null;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(CharSequence text) {
        this.mBreadCrumbShortTitleRes = 0;
        this.mBreadCrumbShortTitleText = text;
        return this;
    }

    void bumpBackStackNesting(int amt) {
        if (this.mAddToBackStack) {
            if (FragmentManagerImpl.DEBUG) {
                Log.v(TAG, "Bump nesting in " + this + " by " + amt);
            }
            for (Op op = this.mHead; op != null; op = op.next) {
                if (op.fragment != null) {
                    op.fragment.mBackStackNesting += amt;
                    if (FragmentManagerImpl.DEBUG) {
                        Log.v(TAG, "Bump nesting of " + op.fragment + " to " + op.fragment.mBackStackNesting);
                    }
                }
                if (op.removed != null) {
                    for (int i = op.removed.size() - 1; i >= 0; i--) {
                        Fragment r = op.removed.get(i);
                        r.mBackStackNesting += amt;
                        if (FragmentManagerImpl.DEBUG) {
                            Log.v(TAG, "Bump nesting of " + r + " to " + r.mBackStackNesting);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int commit() {
        return commitInternal(false);
    }

    @Override
    public int commitAllowingStateLoss() {
        return commitInternal(true);
    }

    @Override
    public void commitNow() throws SuperNotCalledException {
        disallowAddToBackStack();
        this.mManager.execSingleAction(this, false);
    }

    @Override
    public void commitNowAllowingStateLoss() throws SuperNotCalledException {
        disallowAddToBackStack();
        this.mManager.execSingleAction(this, true);
    }

    int commitInternal(boolean allowStateLoss) {
        if (this.mCommitted) {
            throw new IllegalStateException("commit already called");
        }
        if (FragmentManagerImpl.DEBUG) {
            Log.v(TAG, "Commit: " + this);
            LogWriter logw = new LogWriter(2, TAG);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(logw, false, 1024);
            dump("  ", null, fastPrintWriter, null);
            fastPrintWriter.flush();
        }
        this.mCommitted = true;
        if (this.mAddToBackStack) {
            this.mIndex = this.mManager.allocBackStackIndex(this);
        } else {
            this.mIndex = -1;
        }
        this.mManager.enqueueAction(this, allowStateLoss);
        return this.mIndex;
    }

    @Override
    public void run() throws SuperNotCalledException {
        if (FragmentManagerImpl.DEBUG) {
            Log.v(TAG, "Run: " + this);
        }
        if (this.mAddToBackStack && this.mIndex < 0) {
            throw new IllegalStateException("addToBackStack() called after commit()");
        }
        bumpBackStackNesting(1);
        if (this.mManager.mCurState >= 1) {
            SparseArray<Fragment> firstOutFragments = new SparseArray<>();
            SparseArray<Fragment> lastInFragments = new SparseArray<>();
            calculateFragments(firstOutFragments, lastInFragments);
            beginTransition(firstOutFragments, lastInFragments, false);
        }
        for (Op op = this.mHead; op != null; op = op.next) {
            switch (op.cmd) {
                case 1:
                    Fragment f = op.fragment;
                    f.mNextAnim = op.enterAnim;
                    this.mManager.addFragment(f, false);
                    break;
                case 2:
                    Fragment f2 = op.fragment;
                    int containerId = f2.mContainerId;
                    if (this.mManager.mAdded != null) {
                        for (int i = this.mManager.mAdded.size() - 1; i >= 0; i--) {
                            Fragment old = this.mManager.mAdded.get(i);
                            if (FragmentManagerImpl.DEBUG) {
                                Log.v(TAG, "OP_REPLACE: adding=" + f2 + " old=" + old);
                            }
                            if (old.mContainerId == containerId) {
                                if (old == f2) {
                                    f2 = null;
                                    op.fragment = null;
                                } else {
                                    if (op.removed == null) {
                                        op.removed = new ArrayList<>();
                                    }
                                    op.removed.add(old);
                                    old.mNextAnim = op.exitAnim;
                                    if (this.mAddToBackStack) {
                                        old.mBackStackNesting++;
                                        if (FragmentManagerImpl.DEBUG) {
                                            Log.v(TAG, "Bump nesting of " + old + " to " + old.mBackStackNesting);
                                        }
                                    }
                                    this.mManager.removeFragment(old, this.mTransition, this.mTransitionStyle);
                                }
                            }
                        }
                    }
                    if (f2 != null) {
                        f2.mNextAnim = op.enterAnim;
                        this.mManager.addFragment(f2, false);
                    }
                    break;
                case 3:
                    Fragment f3 = op.fragment;
                    f3.mNextAnim = op.exitAnim;
                    this.mManager.removeFragment(f3, this.mTransition, this.mTransitionStyle);
                    break;
                case 4:
                    Fragment f4 = op.fragment;
                    f4.mNextAnim = op.exitAnim;
                    this.mManager.hideFragment(f4, this.mTransition, this.mTransitionStyle);
                    break;
                case 5:
                    Fragment f5 = op.fragment;
                    f5.mNextAnim = op.enterAnim;
                    this.mManager.showFragment(f5, this.mTransition, this.mTransitionStyle);
                    break;
                case 6:
                    Fragment f6 = op.fragment;
                    f6.mNextAnim = op.exitAnim;
                    this.mManager.detachFragment(f6, this.mTransition, this.mTransitionStyle);
                    break;
                case 7:
                    Fragment f7 = op.fragment;
                    f7.mNextAnim = op.enterAnim;
                    this.mManager.attachFragment(f7, this.mTransition, this.mTransitionStyle);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
            }
        }
        this.mManager.moveToState(this.mManager.mCurState, this.mTransition, this.mTransitionStyle, true);
        if (!this.mAddToBackStack) {
            return;
        }
        this.mManager.addBackStackState(this);
    }

    private static void setFirstOut(SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments, Fragment fragment) {
        int containerId;
        if (fragment == null || (containerId = fragment.mContainerId) == 0 || fragment.isHidden()) {
            return;
        }
        if (fragment.isAdded() && fragment.getView() != null && firstOutFragments.get(containerId) == null) {
            firstOutFragments.put(containerId, fragment);
        }
        if (lastInFragments.get(containerId) != fragment) {
            return;
        }
        lastInFragments.remove(containerId);
    }

    private void setLastIn(SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments, Fragment fragment) throws SuperNotCalledException {
        if (fragment == null) {
            return;
        }
        int containerId = fragment.mContainerId;
        if (containerId != 0) {
            if (!fragment.isAdded()) {
                lastInFragments.put(containerId, fragment);
            }
            if (firstOutFragments.get(containerId) == fragment) {
                firstOutFragments.remove(containerId);
            }
        }
        if (fragment.mState >= 1 || this.mManager.mCurState < 1) {
            return;
        }
        this.mManager.makeActive(fragment);
        this.mManager.moveToState(fragment, 1, 0, 0, false);
    }

    private void calculateFragments(SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments) throws SuperNotCalledException {
        if (!this.mManager.mContainer.onHasView()) {
            return;
        }
        for (Op op = this.mHead; op != null; op = op.next) {
            switch (op.cmd) {
                case 1:
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 2:
                    Fragment f = op.fragment;
                    if (this.mManager.mAdded != null) {
                        for (int i = 0; i < this.mManager.mAdded.size(); i++) {
                            Fragment old = this.mManager.mAdded.get(i);
                            if (f == null || old.mContainerId == f.mContainerId) {
                                if (old == f) {
                                    f = null;
                                    lastInFragments.remove(old.mContainerId);
                                } else {
                                    setFirstOut(firstOutFragments, lastInFragments, old);
                                }
                            }
                        }
                    }
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 3:
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 4:
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 5:
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 6:
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 7:
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
            }
        }
    }

    public void calculateBackFragments(SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments) {
        if (!this.mManager.mContainer.onHasView()) {
            return;
        }
        for (Op op = this.mTail; op != null; op = op.prev) {
            switch (op.cmd) {
                case 1:
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 2:
                    if (op.removed != null) {
                        for (int i = op.removed.size() - 1; i >= 0; i--) {
                            setLastIn(firstOutFragments, lastInFragments, op.removed.get(i));
                        }
                    }
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 3:
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 4:
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 5:
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 6:
                    setLastIn(firstOutFragments, lastInFragments, op.fragment);
                    break;
                case 7:
                    setFirstOut(firstOutFragments, lastInFragments, op.fragment);
                    break;
            }
        }
    }

    private TransitionState beginTransition(SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments, boolean isBack) {
        TransitionState state = new TransitionState();
        state.nonExistentView = new View(this.mManager.mHost.getContext());
        for (int i = 0; i < firstOutFragments.size(); i++) {
            configureTransitions(firstOutFragments.keyAt(i), state, isBack, firstOutFragments, lastInFragments);
        }
        for (int i2 = 0; i2 < lastInFragments.size(); i2++) {
            int containerId = lastInFragments.keyAt(i2);
            if (firstOutFragments.get(containerId) == null) {
                configureTransitions(containerId, state, isBack, firstOutFragments, lastInFragments);
            }
        }
        return state;
    }

    private static Transition cloneTransition(Transition transition) {
        if (transition != null) {
            return transition.clone();
        }
        return transition;
    }

    private static Transition getEnterTransition(Fragment inFragment, boolean isBack) {
        if (inFragment == null) {
            return null;
        }
        return cloneTransition(isBack ? inFragment.getReenterTransition() : inFragment.getEnterTransition());
    }

    private static Transition getExitTransition(Fragment outFragment, boolean isBack) {
        if (outFragment == null) {
            return null;
        }
        return cloneTransition(isBack ? outFragment.getReturnTransition() : outFragment.getExitTransition());
    }

    private static TransitionSet getSharedElementTransition(Fragment inFragment, Fragment outFragment, boolean isBack) {
        Transition sharedElementEnterTransition;
        if (inFragment == null || outFragment == null) {
            return null;
        }
        if (isBack) {
            sharedElementEnterTransition = outFragment.getSharedElementReturnTransition();
        } else {
            sharedElementEnterTransition = inFragment.getSharedElementEnterTransition();
        }
        Transition transition = cloneTransition(sharedElementEnterTransition);
        if (transition == null) {
            return null;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(transition);
        return transitionSet;
    }

    private static ArrayList<View> captureExitingViews(Transition exitTransition, Fragment outFragment, ArrayMap<String, View> namedViews, View nonExistentView) {
        ArrayList<View> viewList = null;
        if (exitTransition != null) {
            viewList = new ArrayList<>();
            View root = outFragment.getView();
            root.captureTransitioningViews(viewList);
            if (namedViews != null) {
                viewList.removeAll(namedViews.values());
            }
            if (!viewList.isEmpty()) {
                viewList.add(nonExistentView);
                addTargets(exitTransition, viewList);
            }
        }
        return viewList;
    }

    private ArrayMap<String, View> remapSharedElements(TransitionState state, Fragment outFragment, boolean isBack) {
        ArrayMap<String, View> namedViews = new ArrayMap<>();
        if (this.mSharedElementSourceNames != null) {
            outFragment.getView().findNamedViews(namedViews);
            if (isBack) {
                namedViews.retainAll(this.mSharedElementTargetNames);
            } else {
                namedViews = remapNames(this.mSharedElementSourceNames, this.mSharedElementTargetNames, namedViews);
            }
        }
        if (isBack) {
            outFragment.mEnterTransitionCallback.onMapSharedElements(this.mSharedElementTargetNames, namedViews);
            setBackNameOverrides(state, namedViews, false);
        } else {
            outFragment.mExitTransitionCallback.onMapSharedElements(this.mSharedElementTargetNames, namedViews);
            setNameOverrides(state, namedViews, false);
        }
        return namedViews;
    }

    private ArrayList<View> addTransitionTargets(final TransitionState state, final Transition enterTransition, final TransitionSet sharedElementTransition, final Transition exitTransition, final Transition overallTransition, final View container, final Fragment inFragment, final Fragment outFragment, final ArrayList<View> hiddenFragmentViews, final boolean isBack, final ArrayList<View> sharedElementTargets) {
        if (enterTransition == null && sharedElementTransition == null && overallTransition == null) {
            return null;
        }
        final ArrayList<View> enteringViews = new ArrayList<>();
        container.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                container.getViewTreeObserver().removeOnPreDrawListener(this);
                if (inFragment != null) {
                    BackStackRecord.this.excludeHiddenFragments(hiddenFragmentViews, inFragment.mContainerId, overallTransition);
                }
                ArrayMap<String, View> namedViews = null;
                if (sharedElementTransition != null) {
                    namedViews = BackStackRecord.this.mapSharedElementsIn(state, isBack, inFragment);
                    BackStackRecord.removeTargets(sharedElementTransition, sharedElementTargets);
                    sharedElementTargets.remove(state.nonExistentView);
                    BackStackRecord.excludeViews(exitTransition, sharedElementTransition, sharedElementTargets, false);
                    BackStackRecord.excludeViews(enterTransition, sharedElementTransition, sharedElementTargets, false);
                    BackStackRecord.setSharedElementTargets(sharedElementTransition, state.nonExistentView, namedViews, sharedElementTargets);
                    BackStackRecord.this.setEpicenterIn(namedViews, state);
                    BackStackRecord.this.callSharedElementEnd(state, inFragment, outFragment, isBack, namedViews);
                }
                if (enterTransition != null) {
                    enterTransition.removeTarget(state.nonExistentView);
                    View view = inFragment.getView();
                    if (view != null) {
                        view.captureTransitioningViews(enteringViews);
                        if (namedViews != null) {
                            enteringViews.removeAll(namedViews.values());
                        }
                        enteringViews.add(state.nonExistentView);
                        BackStackRecord.addTargets(enterTransition, enteringViews);
                    }
                    BackStackRecord.this.setSharedElementEpicenter(enterTransition, state);
                }
                BackStackRecord.excludeViews(exitTransition, enterTransition, enteringViews, true);
                BackStackRecord.excludeViews(exitTransition, sharedElementTransition, sharedElementTargets, true);
                BackStackRecord.excludeViews(enterTransition, sharedElementTransition, sharedElementTargets, true);
                return true;
            }
        });
        return enteringViews;
    }

    private void callSharedElementEnd(TransitionState state, Fragment inFragment, Fragment outFragment, boolean isBack, ArrayMap<String, View> namedViews) {
        SharedElementCallback sharedElementCallback;
        if (isBack) {
            sharedElementCallback = outFragment.mEnterTransitionCallback;
        } else {
            sharedElementCallback = inFragment.mEnterTransitionCallback;
        }
        ArrayList<String> names = new ArrayList<>(namedViews.keySet());
        ArrayList<View> views = new ArrayList<>(namedViews.values());
        sharedElementCallback.onSharedElementEnd(names, views, null);
    }

    private void setEpicenterIn(ArrayMap<String, View> namedViews, TransitionState state) {
        View epicenter;
        if (this.mSharedElementTargetNames == null || namedViews.isEmpty() || (epicenter = namedViews.get(this.mSharedElementTargetNames.get(0))) == null) {
            return;
        }
        state.enteringEpicenterView = epicenter;
    }

    private ArrayMap<String, View> mapSharedElementsIn(TransitionState state, boolean isBack, Fragment inFragment) {
        ArrayMap<String, View> namedViews = mapEnteringSharedElements(state, inFragment, isBack);
        if (isBack) {
            inFragment.mExitTransitionCallback.onMapSharedElements(this.mSharedElementTargetNames, namedViews);
            setBackNameOverrides(state, namedViews, true);
        } else {
            inFragment.mEnterTransitionCallback.onMapSharedElements(this.mSharedElementTargetNames, namedViews);
            setNameOverrides(state, namedViews, true);
        }
        return namedViews;
    }

    private static Transition mergeTransitions(Transition enterTransition, Transition exitTransition, Transition sharedElementTransition, Fragment inFragment, boolean isBack) {
        boolean overlap = true;
        if (enterTransition != null && exitTransition != null && inFragment != null) {
            overlap = isBack ? inFragment.getAllowReturnTransitionOverlap() : inFragment.getAllowEnterTransitionOverlap();
        }
        if (overlap) {
            TransitionSet transitionSet = new TransitionSet();
            if (enterTransition != null) {
                transitionSet.addTransition(enterTransition);
            }
            if (exitTransition != null) {
                transitionSet.addTransition(exitTransition);
            }
            if (sharedElementTransition != null) {
                transitionSet.addTransition(sharedElementTransition);
            }
            return transitionSet;
        }
        Transition staggered = null;
        if (exitTransition != null && enterTransition != null) {
            staggered = new TransitionSet().addTransition(exitTransition).addTransition(enterTransition).setOrdering(1);
        } else if (exitTransition != null) {
            staggered = exitTransition;
        } else if (enterTransition != null) {
            staggered = enterTransition;
        }
        if (sharedElementTransition != null) {
            TransitionSet together = new TransitionSet();
            if (staggered != null) {
                together.addTransition(staggered);
            }
            together.addTransition(sharedElementTransition);
            return together;
        }
        Transition transition = staggered;
        return transition;
    }

    private void configureTransitions(int containerId, TransitionState state, boolean isBack, SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments) {
        SharedElementCallback callback;
        ViewGroup sceneRoot = (ViewGroup) this.mManager.mContainer.onFindViewById(containerId);
        if (sceneRoot == null) {
            return;
        }
        Fragment inFragment = lastInFragments.get(containerId);
        Fragment outFragment = firstOutFragments.get(containerId);
        Transition enterTransition = getEnterTransition(inFragment, isBack);
        TransitionSet sharedElementTransition = getSharedElementTransition(inFragment, outFragment, isBack);
        Transition exitTransition = getExitTransition(outFragment, isBack);
        if (enterTransition == null && sharedElementTransition == null && exitTransition == null) {
            return;
        }
        if (enterTransition != null) {
            enterTransition.addTarget(state.nonExistentView);
        }
        ArrayMap<String, View> namedViews = null;
        ArrayList<View> sharedElementTargets = new ArrayList<>();
        if (sharedElementTransition != null) {
            namedViews = remapSharedElements(state, outFragment, isBack);
            setSharedElementTargets(sharedElementTransition, state.nonExistentView, namedViews, sharedElementTargets);
            if (isBack) {
                callback = outFragment.mEnterTransitionCallback;
            } else {
                callback = inFragment.mEnterTransitionCallback;
            }
            ArrayList<String> names = new ArrayList<>(namedViews.keySet());
            ArrayList<View> views = new ArrayList<>(namedViews.values());
            callback.onSharedElementStart(names, views, null);
        }
        ArrayList<View> exitingViews = captureExitingViews(exitTransition, outFragment, namedViews, state.nonExistentView);
        if (exitingViews == null || exitingViews.isEmpty()) {
            exitTransition = null;
        }
        excludeViews(enterTransition, exitTransition, exitingViews, true);
        excludeViews(enterTransition, sharedElementTransition, sharedElementTargets, true);
        excludeViews(exitTransition, sharedElementTransition, sharedElementTargets, true);
        if (this.mSharedElementTargetNames != null && namedViews != null) {
            View epicenterView = namedViews.get(this.mSharedElementTargetNames.get(0));
            if (epicenterView != null) {
                if (exitTransition != null) {
                    setEpicenter(exitTransition, epicenterView);
                }
                if (sharedElementTransition != null) {
                    setEpicenter(sharedElementTransition, epicenterView);
                }
            }
        }
        Transition transition = mergeTransitions(enterTransition, exitTransition, sharedElementTransition, inFragment, isBack);
        if (transition == null) {
            return;
        }
        ArrayList<View> hiddenFragments = new ArrayList<>();
        ArrayList<View> enteringViews = addTransitionTargets(state, enterTransition, sharedElementTransition, exitTransition, transition, sceneRoot, inFragment, outFragment, hiddenFragments, isBack, sharedElementTargets);
        transition.setNameOverrides(state.nameOverrides);
        transition.excludeTarget(state.nonExistentView, true);
        excludeHiddenFragments(hiddenFragments, containerId, transition);
        TransitionManager.beginDelayedTransition(sceneRoot, transition);
        removeTargetedViewsFromTransitions(sceneRoot, state.nonExistentView, enterTransition, enteringViews, exitTransition, exitingViews, sharedElementTransition, sharedElementTargets, transition, hiddenFragments);
    }

    private static void setSharedElementTargets(TransitionSet transition, View nonExistentView, ArrayMap<String, View> namedViews, ArrayList<View> sharedElementTargets) {
        sharedElementTargets.clear();
        sharedElementTargets.addAll(namedViews.values());
        List<View> views = transition.getTargets();
        views.clear();
        int count = sharedElementTargets.size();
        for (int i = 0; i < count; i++) {
            View view = sharedElementTargets.get(i);
            bfsAddViewChildren(views, view);
        }
        sharedElementTargets.add(nonExistentView);
        addTargets(transition, sharedElementTargets);
    }

    private static void bfsAddViewChildren(List<View> views, View startView) {
        int startIndex = views.size();
        if (containedBeforeIndex(views, startView, startIndex)) {
            return;
        }
        views.add(startView);
        for (int index = startIndex; index < views.size(); index++) {
            View view = views.get(index);
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                for (int childIndex = 0; childIndex < childCount; childIndex++) {
                    View child = viewGroup.getChildAt(childIndex);
                    if (!containedBeforeIndex(views, child, startIndex)) {
                        views.add(child);
                    }
                }
            }
        }
    }

    private static boolean containedBeforeIndex(List<View> views, View view, int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            if (views.get(i) == view) {
                return true;
            }
        }
        return false;
    }

    private static void excludeViews(Transition transition, Transition fromTransition, ArrayList<View> views, boolean exclude) {
        if (transition == null) {
            return;
        }
        int viewCount = fromTransition == null ? 0 : views.size();
        for (int i = 0; i < viewCount; i++) {
            transition.excludeTarget(views.get(i), exclude);
        }
    }

    private void removeTargetedViewsFromTransitions(final ViewGroup sceneRoot, final View nonExistingView, final Transition enterTransition, final ArrayList<View> enteringViews, final Transition exitTransition, final ArrayList<View> exitingViews, final Transition sharedElementTransition, final ArrayList<View> sharedElementTargets, final Transition overallTransition, final ArrayList<View> hiddenViews) {
        if (overallTransition == null) {
            return;
        }
        sceneRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                sceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                if (enterTransition != null) {
                    BackStackRecord.removeTargets(enterTransition, enteringViews);
                    BackStackRecord.excludeViews(enterTransition, exitTransition, exitingViews, false);
                    BackStackRecord.excludeViews(enterTransition, sharedElementTransition, sharedElementTargets, false);
                }
                if (exitTransition != null) {
                    BackStackRecord.removeTargets(exitTransition, exitingViews);
                    BackStackRecord.excludeViews(exitTransition, enterTransition, enteringViews, false);
                    BackStackRecord.excludeViews(exitTransition, sharedElementTransition, sharedElementTargets, false);
                }
                if (sharedElementTransition != null) {
                    BackStackRecord.removeTargets(sharedElementTransition, sharedElementTargets);
                }
                int numViews = hiddenViews.size();
                for (int i = 0; i < numViews; i++) {
                    overallTransition.excludeTarget((View) hiddenViews.get(i), false);
                }
                overallTransition.excludeTarget(nonExistingView, false);
                return true;
            }
        });
    }

    public static void removeTargets(Transition transition, ArrayList<View> views) {
        List<View> targets;
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                removeTargets(child, views);
            }
            return;
        }
        if (hasSimpleTarget(transition) || (targets = transition.getTargets()) == null || targets.size() != views.size() || !targets.containsAll(views)) {
            return;
        }
        for (int i2 = views.size() - 1; i2 >= 0; i2--) {
            transition.removeTarget(views.get(i2));
        }
    }

    public static void addTargets(Transition transition, ArrayList<View> views) {
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                addTargets(child, views);
            }
            return;
        }
        if (hasSimpleTarget(transition)) {
            return;
        }
        List<View> targets = transition.getTargets();
        if (!isNullOrEmpty(targets)) {
            return;
        }
        int numViews = views.size();
        for (int i2 = 0; i2 < numViews; i2++) {
            transition.addTarget(views.get(i2));
        }
    }

    private static boolean hasSimpleTarget(Transition transition) {
        return (isNullOrEmpty(transition.getTargetIds()) && isNullOrEmpty(transition.getTargetNames()) && isNullOrEmpty(transition.getTargetTypes())) ? false : true;
    }

    private static boolean isNullOrEmpty(List list) {
        if (list != null) {
            return list.isEmpty();
        }
        return true;
    }

    private static ArrayMap<String, View> remapNames(ArrayList<String> inMap, ArrayList<String> toGoInMap, ArrayMap<String, View> namedViews) {
        ArrayMap<String, View> remappedViews = new ArrayMap<>();
        if (!namedViews.isEmpty()) {
            int numKeys = inMap.size();
            for (int i = 0; i < numKeys; i++) {
                View view = namedViews.get(inMap.get(i));
                if (view != null) {
                    remappedViews.put(toGoInMap.get(i), view);
                }
            }
        }
        return remappedViews;
    }

    private ArrayMap<String, View> mapEnteringSharedElements(TransitionState state, Fragment inFragment, boolean isBack) {
        ArrayMap<String, View> namedViews = new ArrayMap<>();
        View root = inFragment.getView();
        if (root != null && this.mSharedElementSourceNames != null) {
            root.findNamedViews(namedViews);
            if (isBack) {
                return remapNames(this.mSharedElementSourceNames, this.mSharedElementTargetNames, namedViews);
            }
            namedViews.retainAll(this.mSharedElementTargetNames);
            return namedViews;
        }
        return namedViews;
    }

    private void excludeHiddenFragments(ArrayList<View> hiddenFragmentViews, int containerId, Transition transition) {
        if (this.mManager.mAdded == null) {
            return;
        }
        for (int i = 0; i < this.mManager.mAdded.size(); i++) {
            Fragment fragment = this.mManager.mAdded.get(i);
            if (fragment.mView != null && fragment.mContainer != null && fragment.mContainerId == containerId) {
                if (fragment.mHidden) {
                    if (!hiddenFragmentViews.contains(fragment.mView)) {
                        transition.excludeTarget(fragment.mView, true);
                        hiddenFragmentViews.add(fragment.mView);
                    }
                } else {
                    transition.excludeTarget(fragment.mView, false);
                    hiddenFragmentViews.remove(fragment.mView);
                }
            }
        }
    }

    private static void setEpicenter(Transition transition, View view) {
        final Rect epicenter = new Rect();
        view.getBoundsOnScreen(epicenter);
        transition.setEpicenterCallback(new Transition.EpicenterCallback() {
            @Override
            public Rect onGetEpicenter(Transition transition2) {
                return epicenter;
            }
        });
    }

    private void setSharedElementEpicenter(Transition transition, final TransitionState state) {
        transition.setEpicenterCallback(new Transition.EpicenterCallback() {
            private Rect mEpicenter;

            @Override
            public Rect onGetEpicenter(Transition transition2) {
                if (this.mEpicenter == null && state.enteringEpicenterView != null) {
                    this.mEpicenter = new Rect();
                    state.enteringEpicenterView.getBoundsOnScreen(this.mEpicenter);
                }
                return this.mEpicenter;
            }
        });
    }

    public TransitionState popFromBackStack(boolean doStateMove, TransitionState state, SparseArray<Fragment> firstOutFragments, SparseArray<Fragment> lastInFragments) {
        if (FragmentManagerImpl.DEBUG) {
            Log.v(TAG, "popFromBackStack: " + this);
            LogWriter logw = new LogWriter(2, TAG);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(logw, false, 1024);
            dump("  ", null, fastPrintWriter, null);
            fastPrintWriter.flush();
        }
        if (this.mManager.mCurState >= 1) {
            if (state == null) {
                if (firstOutFragments.size() != 0 || lastInFragments.size() != 0) {
                    state = beginTransition(firstOutFragments, lastInFragments, true);
                }
            } else if (!doStateMove) {
                setNameOverrides(state, this.mSharedElementTargetNames, this.mSharedElementSourceNames);
            }
        }
        bumpBackStackNesting(-1);
        for (Op op = this.mTail; op != null; op = op.prev) {
            switch (op.cmd) {
                case 1:
                    Fragment f = op.fragment;
                    f.mNextAnim = op.popExitAnim;
                    this.mManager.removeFragment(f, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle);
                    break;
                case 2:
                    Fragment f2 = op.fragment;
                    if (f2 != null) {
                        f2.mNextAnim = op.popExitAnim;
                        this.mManager.removeFragment(f2, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle);
                    }
                    if (op.removed != null) {
                        for (int i = 0; i < op.removed.size(); i++) {
                            Fragment old = op.removed.get(i);
                            old.mNextAnim = op.popEnterAnim;
                            this.mManager.addFragment(old, false);
                        }
                    }
                    break;
                case 3:
                    Fragment f3 = op.fragment;
                    f3.mNextAnim = op.popEnterAnim;
                    this.mManager.addFragment(f3, false);
                    break;
                case 4:
                    Fragment f4 = op.fragment;
                    f4.mNextAnim = op.popEnterAnim;
                    this.mManager.showFragment(f4, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle);
                    break;
                case 5:
                    Fragment f5 = op.fragment;
                    f5.mNextAnim = op.popExitAnim;
                    this.mManager.hideFragment(f5, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle);
                    break;
                case 6:
                    Fragment f6 = op.fragment;
                    f6.mNextAnim = op.popEnterAnim;
                    this.mManager.attachFragment(f6, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle);
                    break;
                case 7:
                    Fragment f7 = op.fragment;
                    f7.mNextAnim = op.popExitAnim;
                    this.mManager.detachFragment(f7, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
            }
        }
        if (doStateMove) {
            this.mManager.moveToState(this.mManager.mCurState, FragmentManagerImpl.reverseTransit(this.mTransition), this.mTransitionStyle, true);
            state = null;
        }
        if (this.mIndex >= 0) {
            this.mManager.freeBackStackIndex(this.mIndex);
            this.mIndex = -1;
        }
        return state;
    }

    private static void setNameOverride(ArrayMap<String, String> overrides, String source, String target) {
        if (source == null || target == null || source.equals(target)) {
            return;
        }
        for (int index = 0; index < overrides.size(); index++) {
            if (source.equals(overrides.valueAt(index))) {
                overrides.setValueAt(index, target);
                return;
            }
        }
        overrides.put(source, target);
    }

    private static void setNameOverrides(TransitionState state, ArrayList<String> sourceNames, ArrayList<String> targetNames) {
        if (sourceNames == null || targetNames == null) {
            return;
        }
        for (int i = 0; i < sourceNames.size(); i++) {
            String source = sourceNames.get(i);
            String target = targetNames.get(i);
            setNameOverride(state.nameOverrides, source, target);
        }
    }

    private void setBackNameOverrides(TransitionState state, ArrayMap<String, View> namedViews, boolean isEnd) {
        int targetCount = this.mSharedElementTargetNames == null ? 0 : this.mSharedElementTargetNames.size();
        int sourceCount = this.mSharedElementSourceNames == null ? 0 : this.mSharedElementSourceNames.size();
        int count = Math.min(targetCount, sourceCount);
        for (int i = 0; i < count; i++) {
            String source = this.mSharedElementSourceNames.get(i);
            String originalTarget = this.mSharedElementTargetNames.get(i);
            View view = namedViews.get(originalTarget);
            if (view != null) {
                String target = view.getTransitionName();
                if (isEnd) {
                    setNameOverride(state.nameOverrides, source, target);
                } else {
                    setNameOverride(state.nameOverrides, target, source);
                }
            }
        }
    }

    private void setNameOverrides(TransitionState state, ArrayMap<String, View> namedViews, boolean isEnd) {
        int count = namedViews == null ? 0 : namedViews.size();
        for (int i = 0; i < count; i++) {
            String source = namedViews.keyAt(i);
            String target = namedViews.valueAt(i).getTransitionName();
            if (isEnd) {
                setNameOverride(state.nameOverrides, source, target);
            } else {
                setNameOverride(state.nameOverrides, target, source);
            }
        }
    }

    @Override
    public String getName() {
        return this.mName;
    }

    public int getTransition() {
        return this.mTransition;
    }

    public int getTransitionStyle() {
        return this.mTransitionStyle;
    }

    @Override
    public boolean isEmpty() {
        return this.mNumOp == 0;
    }

    public class TransitionState {
        public View enteringEpicenterView;
        public ArrayMap<String, String> nameOverrides = new ArrayMap<>();
        public View nonExistentView;

        public TransitionState() {
        }
    }
}
