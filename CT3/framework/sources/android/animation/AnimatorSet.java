package android.animation;

import android.animation.Animator;
import android.app.ActivityThread;
import android.app.Application;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AnimatorSet extends Animator {
    private static final String TAG = "AnimatorSet";
    private final boolean mShouldIgnoreEndWithoutStart;
    private ArrayList<Animator> mPlayingSet = new ArrayList<>();
    private ArrayMap<Animator, Node> mNodeMap = new ArrayMap<>();
    private ArrayList<Node> mNodes = new ArrayList<>();
    private AnimatorSetListener mSetListener = new AnimatorSetListener(this);
    private boolean mTerminated = false;
    private boolean mDependencyDirty = false;
    private boolean mStarted = false;
    private long mStartDelay = 0;
    private ValueAnimator mDelayAnim = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(0L);
    private Node mRootNode = new Node(this.mDelayAnim);
    private long mDuration = -1;
    private TimeInterpolator mInterpolator = null;
    private boolean mReversible = true;
    private long mTotalDuration = 0;

    public AnimatorSet() {
        this.mNodeMap.put(this.mDelayAnim, this.mRootNode);
        this.mNodes.add(this.mRootNode);
        Application app = ActivityThread.currentApplication();
        if (app == null || app.getApplicationInfo() == null) {
            this.mShouldIgnoreEndWithoutStart = true;
        } else if (app.getApplicationInfo().targetSdkVersion < 24) {
            this.mShouldIgnoreEndWithoutStart = true;
        } else {
            this.mShouldIgnoreEndWithoutStart = false;
        }
    }

    public void playTogether(Animator... items) {
        if (items == null) {
            return;
        }
        Builder builder = play(items[0]);
        for (int i = 1; i < items.length; i++) {
            builder.with(items[i]);
        }
    }

    public void playTogether(Collection<Animator> items) {
        if (items == null || items.size() <= 0) {
            return;
        }
        Builder builder = null;
        for (Animator anim : items) {
            if (builder == null) {
                builder = play(anim);
            } else {
                builder.with(anim);
            }
        }
    }

    public void playSequentially(Animator... items) {
        if (items == null) {
            return;
        }
        if (items.length == 1) {
            play(items[0]);
            return;
        }
        this.mReversible = false;
        for (int i = 0; i < items.length - 1; i++) {
            play(items[i]).before(items[i + 1]);
        }
    }

    public void playSequentially(List<Animator> items) {
        if (items == null || items.size() <= 0) {
            return;
        }
        if (items.size() == 1) {
            play(items.get(0));
            return;
        }
        this.mReversible = false;
        for (int i = 0; i < items.size() - 1; i++) {
            play(items.get(i)).before(items.get(i + 1));
        }
    }

    public ArrayList<Animator> getChildAnimations() {
        ArrayList<Animator> childList = new ArrayList<>();
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode) {
                childList.add(node.mAnimation);
            }
        }
        return childList;
    }

    @Override
    public void setTarget(Object target) {
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            Animator animation = node.mAnimation;
            if (animation instanceof AnimatorSet) {
                ((AnimatorSet) animation).setTarget(target);
            } else if (animation instanceof ObjectAnimator) {
                ((ObjectAnimator) animation).setTarget(target);
            }
        }
    }

    @Override
    public int getChangingConfigurations() {
        int conf = super.getChangingConfigurations();
        int nodeCount = this.mNodes.size();
        for (int i = 0; i < nodeCount; i++) {
            conf |= this.mNodes.get(i).mAnimation.getChangingConfigurations();
        }
        return conf;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    public Builder play(Animator anim) {
        if (anim != null) {
            return new Builder(anim);
        }
        return null;
    }

    @Override
    public void cancel() {
        this.mTerminated = true;
        if (!isStarted()) {
            return;
        }
        ArrayList<Animator.AnimatorListener> tmpListeners = null;
        if (this.mListeners != null) {
            tmpListeners = (ArrayList) this.mListeners.clone();
            int size = tmpListeners.size();
            for (int i = 0; i < size; i++) {
                tmpListeners.get(i).onAnimationCancel(this);
            }
        }
        ArrayList<Animator> playingSet = new ArrayList<>(this.mPlayingSet);
        int setSize = playingSet.size();
        for (int i2 = 0; i2 < setSize; i2++) {
            playingSet.get(i2).cancel();
        }
        if (tmpListeners != null) {
            int size2 = tmpListeners.size();
            for (int i3 = 0; i3 < size2; i3++) {
                tmpListeners.get(i3).onAnimationEnd(this);
            }
        }
        this.mStarted = false;
    }

    @Override
    public void end() {
        if (this.mShouldIgnoreEndWithoutStart && !isStarted()) {
            return;
        }
        this.mTerminated = true;
        if (isStarted()) {
            endRemainingAnimations();
        }
        if (this.mListeners != null) {
            ArrayList<Animator.AnimatorListener> tmpListeners = (ArrayList) this.mListeners.clone();
            for (int i = 0; i < tmpListeners.size(); i++) {
                tmpListeners.get(i).onAnimationEnd(this);
            }
        }
        this.mStarted = false;
    }

    private void endRemainingAnimations() {
        ArrayList<Animator> remainingList = new ArrayList<>(this.mNodes.size());
        remainingList.addAll(this.mPlayingSet);
        int index = 0;
        while (index < remainingList.size()) {
            Animator anim = remainingList.get(index);
            anim.end();
            index++;
            Node node = this.mNodeMap.get(anim);
            if (node.mChildNodes != null) {
                int childSize = node.mChildNodes.size();
                for (int i = 0; i < childSize; i++) {
                    Node child = node.mChildNodes.get(i);
                    if (child.mLatestParent == node) {
                        remainingList.add(child.mAnimation);
                    }
                }
            }
        }
    }

    @Override
    public boolean isRunning() {
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode && node.mAnimation.isStarted()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isStarted() {
        return this.mStarted;
    }

    @Override
    public long getStartDelay() {
        return this.mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        if (startDelay < 0) {
            Log.w(TAG, "Start delay should always be non-negative");
            startDelay = 0;
        }
        long delta = startDelay - this.mStartDelay;
        if (delta == 0) {
            return;
        }
        this.mStartDelay = startDelay;
        if (this.mStartDelay > 0) {
            this.mReversible = false;
        }
        if (this.mDependencyDirty) {
            return;
        }
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node == this.mRootNode) {
                node.mEndTime = this.mStartDelay;
            } else {
                node.mStartTime = node.mStartTime == -1 ? -1L : node.mStartTime + delta;
                node.mEndTime = node.mEndTime == -1 ? -1L : node.mEndTime + delta;
            }
        }
        if (this.mTotalDuration == -1) {
            return;
        }
        this.mTotalDuration += delta;
    }

    @Override
    public long getDuration() {
        return this.mDuration;
    }

    @Override
    public AnimatorSet setDuration(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("duration must be a value of zero or greater");
        }
        this.mDependencyDirty = true;
        this.mDuration = duration;
        return this;
    }

    @Override
    public void setupStartValues() {
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode) {
                node.mAnimation.setupStartValues();
            }
        }
    }

    @Override
    public void setupEndValues() {
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode) {
                node.mAnimation.setupEndValues();
            }
        }
    }

    @Override
    public void pause() {
        boolean previouslyPaused = this.mPaused;
        super.pause();
        if (previouslyPaused || !this.mPaused) {
            return;
        }
        if (this.mDelayAnim.isStarted()) {
            this.mDelayAnim.pause();
            return;
        }
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode) {
                node.mAnimation.pause();
            }
        }
    }

    @Override
    public void resume() {
        boolean previouslyPaused = this.mPaused;
        super.resume();
        if (!previouslyPaused || this.mPaused) {
            return;
        }
        if (this.mDelayAnim.isStarted()) {
            this.mDelayAnim.resume();
            return;
        }
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode) {
                node.mAnimation.resume();
            }
        }
    }

    @Override
    public void start() {
        this.mTerminated = false;
        this.mStarted = true;
        this.mPaused = false;
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            node.mEnded = false;
            node.mAnimation.setAllowRunningAsynchronously(false);
        }
        if (this.mInterpolator != null) {
            for (int i2 = 0; i2 < size; i2++) {
                this.mNodes.get(i2).mAnimation.setInterpolator(this.mInterpolator);
            }
        }
        updateAnimatorsDuration();
        createDependencyGraph();
        boolean setIsEmpty = false;
        if (this.mStartDelay > 0) {
            start(this.mRootNode);
        } else if (this.mNodes.size() > 1) {
            onChildAnimatorEnded(this.mDelayAnim);
        } else {
            setIsEmpty = true;
        }
        if (this.mListeners != null) {
            ArrayList<Animator.AnimatorListener> tmpListeners = (ArrayList) this.mListeners.clone();
            int numListeners = tmpListeners.size();
            for (int i3 = 0; i3 < numListeners; i3++) {
                tmpListeners.get(i3).onAnimationStart(this);
            }
        }
        if (!setIsEmpty) {
            return;
        }
        onChildAnimatorEnded(this.mDelayAnim);
    }

    private void updateAnimatorsDuration() {
        if (this.mDuration >= 0) {
            int size = this.mNodes.size();
            for (int i = 0; i < size; i++) {
                Node node = this.mNodes.get(i);
                node.mAnimation.setDuration(this.mDuration);
            }
        }
        this.mDelayAnim.setDuration(this.mStartDelay);
    }

    void start(Node node) {
        Animator anim = node.mAnimation;
        this.mPlayingSet.add(anim);
        anim.addListener(this.mSetListener);
        anim.start();
    }

    @Override
    public AnimatorSet m35clone() {
        AnimatorSet anim = (AnimatorSet) super.m35clone();
        int nodeCount = this.mNodes.size();
        anim.mTerminated = false;
        anim.mStarted = false;
        anim.mPlayingSet = new ArrayList<>();
        anim.mNodeMap = new ArrayMap<>();
        anim.mNodes = new ArrayList<>(nodeCount);
        anim.mReversible = this.mReversible;
        anim.mSetListener = new AnimatorSetListener(anim);
        for (int n = 0; n < nodeCount; n++) {
            Node node = this.mNodes.get(n);
            Node nodeClone = node.m44clone();
            node.mTmpClone = nodeClone;
            anim.mNodes.add(nodeClone);
            anim.mNodeMap.put(nodeClone.mAnimation, nodeClone);
            ArrayList<Animator.AnimatorListener> cloneListeners = nodeClone.mAnimation.getListeners();
            if (cloneListeners != null) {
                for (int i = cloneListeners.size() - 1; i >= 0; i--) {
                    Animator.AnimatorListener listener = cloneListeners.get(i);
                    if (listener instanceof AnimatorSetListener) {
                        cloneListeners.remove(i);
                    }
                }
            }
        }
        anim.mRootNode = this.mRootNode.mTmpClone;
        anim.mDelayAnim = (ValueAnimator) anim.mRootNode.mAnimation;
        for (int i2 = 0; i2 < nodeCount; i2++) {
            Node node2 = this.mNodes.get(i2);
            node2.mTmpClone.mLatestParent = node2.mLatestParent == null ? null : node2.mLatestParent.mTmpClone;
            int size = node2.mChildNodes == null ? 0 : node2.mChildNodes.size();
            for (int j = 0; j < size; j++) {
                node2.mTmpClone.mChildNodes.set(j, node2.mChildNodes.get(j).mTmpClone);
            }
            int size2 = node2.mSiblings == null ? 0 : node2.mSiblings.size();
            for (int j2 = 0; j2 < size2; j2++) {
                node2.mTmpClone.mSiblings.set(j2, node2.mSiblings.get(j2).mTmpClone);
            }
            int size3 = node2.mParents == null ? 0 : node2.mParents.size();
            for (int j3 = 0; j3 < size3; j3++) {
                node2.mTmpClone.mParents.set(j3, node2.mParents.get(j3).mTmpClone);
            }
        }
        for (int n2 = 0; n2 < nodeCount; n2++) {
            this.mNodes.get(n2).mTmpClone = null;
        }
        return anim;
    }

    private static class AnimatorSetListener implements Animator.AnimatorListener {
        private AnimatorSet mAnimatorSet;

        AnimatorSetListener(AnimatorSet animatorSet) {
            this.mAnimatorSet = animatorSet;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            ArrayList<Animator.AnimatorListener> listeners;
            if (this.mAnimatorSet.mTerminated || this.mAnimatorSet.mPlayingSet.size() != 0 || (listeners = this.mAnimatorSet.mListeners) == null) {
                return;
            }
            int numListeners = listeners.size();
            for (int i = 0; i < numListeners; i++) {
                listeners.get(i).onAnimationCancel(this.mAnimatorSet);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            animation.removeListener(this);
            this.mAnimatorSet.mPlayingSet.remove(animation);
            this.mAnimatorSet.onChildAnimatorEnded(animation);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }
    }

    private void onChildAnimatorEnded(Animator animation) {
        Node animNode = this.mNodeMap.get(animation);
        animNode.mEnded = true;
        if (this.mTerminated) {
            return;
        }
        List<Node> children = animNode.mChildNodes;
        int childrenSize = children == null ? 0 : children.size();
        for (int i = 0; i < childrenSize; i++) {
            if (children.get(i).mLatestParent == animNode) {
                start(children.get(i));
            }
        }
        boolean allDone = true;
        int size = this.mNodes.size();
        int i2 = 0;
        while (true) {
            if (i2 >= size) {
                break;
            }
            if (this.mNodes.get(i2).mEnded) {
                i2++;
            } else {
                allDone = false;
                break;
            }
        }
        if (!allDone) {
            return;
        }
        if (this.mListeners != null) {
            ArrayList<Animator.AnimatorListener> tmpListeners = (ArrayList) this.mListeners.clone();
            int numListeners = tmpListeners.size();
            for (int i3 = 0; i3 < numListeners; i3++) {
                tmpListeners.get(i3).onAnimationEnd(this);
            }
        }
        this.mStarted = false;
        this.mPaused = false;
    }

    @Override
    public boolean canReverse() {
        if (!this.mReversible) {
            return false;
        }
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (!node.mAnimation.canReverse() || node.mAnimation.getStartDelay() > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reverse() {
        if (!canReverse()) {
            return;
        }
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            node.mAnimation.reverse();
        }
    }

    public String toString() {
        String returnVal = "AnimatorSet@" + Integer.toHexString(hashCode()) + "{";
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            returnVal = returnVal + "\n    " + node.mAnimation.toString();
        }
        return returnVal + "\n}";
    }

    private void printChildCount() {
        ArrayList<Node> list = new ArrayList<>(this.mNodes.size());
        list.add(this.mRootNode);
        Log.d(TAG, "Current tree: ");
        int index = 0;
        while (index < list.size()) {
            int listSize = list.size();
            StringBuilder builder = new StringBuilder();
            while (index < listSize) {
                Node node = list.get(index);
                int num = 0;
                if (node.mChildNodes != null) {
                    for (int i = 0; i < node.mChildNodes.size(); i++) {
                        Node child = node.mChildNodes.get(i);
                        if (child.mLatestParent == node) {
                            num++;
                            list.add(child);
                        }
                    }
                }
                builder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                builder.append(num);
                index++;
            }
            Log.d(TAG, builder.toString());
        }
    }

    private void createDependencyGraph() {
        if (!this.mDependencyDirty) {
            boolean durationChanged = false;
            int i = 0;
            while (true) {
                if (i >= this.mNodes.size()) {
                    break;
                }
                Animator anim = this.mNodes.get(i).mAnimation;
                if (this.mNodes.get(i).mTotalDuration == anim.getTotalDuration()) {
                    i++;
                } else {
                    durationChanged = true;
                    break;
                }
            }
            if (!durationChanged) {
                return;
            }
        }
        this.mDependencyDirty = false;
        int size = this.mNodes.size();
        for (int i2 = 0; i2 < size; i2++) {
            this.mNodes.get(i2).mParentsAdded = false;
        }
        for (int i3 = 0; i3 < size; i3++) {
            Node node = this.mNodes.get(i3);
            if (!node.mParentsAdded) {
                node.mParentsAdded = true;
                if (node.mSiblings != null) {
                    findSiblings(node, node.mSiblings);
                    node.mSiblings.remove(node);
                    int siblingSize = node.mSiblings.size();
                    for (int j = 0; j < siblingSize; j++) {
                        node.addParents(node.mSiblings.get(j).mParents);
                    }
                    for (int j2 = 0; j2 < siblingSize; j2++) {
                        Node sibling = node.mSiblings.get(j2);
                        sibling.addParents(node.mParents);
                        sibling.mParentsAdded = true;
                    }
                }
            }
        }
        for (int i4 = 0; i4 < size; i4++) {
            Node node2 = this.mNodes.get(i4);
            if (node2 != this.mRootNode && node2.mParents == null) {
                node2.addParent(this.mRootNode);
            }
        }
        ArrayList<Node> visited = new ArrayList<>(this.mNodes.size());
        this.mRootNode.mStartTime = 0L;
        this.mRootNode.mEndTime = this.mDelayAnim.getDuration();
        updatePlayTime(this.mRootNode, visited);
        long maxEndTime = 0;
        int i5 = 0;
        while (true) {
            if (i5 >= size) {
                break;
            }
            Node node3 = this.mNodes.get(i5);
            node3.mTotalDuration = node3.mAnimation.getTotalDuration();
            if (node3.mEndTime != -1) {
                if (node3.mEndTime > maxEndTime) {
                    maxEndTime = node3.mEndTime;
                }
                i5++;
            } else {
                maxEndTime = -1;
                break;
            }
        }
        this.mTotalDuration = maxEndTime;
    }

    private void updatePlayTime(Node parent, ArrayList<Node> visited) {
        if (parent.mChildNodes == null) {
            if (parent == this.mRootNode) {
                for (int i = 0; i < this.mNodes.size(); i++) {
                    Node node = this.mNodes.get(i);
                    if (node != this.mRootNode) {
                        node.mStartTime = -1L;
                        node.mEndTime = -1L;
                    }
                }
                return;
            }
            return;
        }
        visited.add(parent);
        int childrenSize = parent.mChildNodes.size();
        for (int i2 = 0; i2 < childrenSize; i2++) {
            Node child = parent.mChildNodes.get(i2);
            int index = visited.indexOf(child);
            if (index >= 0) {
                for (int j = index; j < visited.size(); j++) {
                    visited.get(j).mLatestParent = null;
                    visited.get(j).mStartTime = -1L;
                    visited.get(j).mEndTime = -1L;
                }
                child.mStartTime = -1L;
                child.mEndTime = -1L;
                child.mLatestParent = null;
                Log.w(TAG, "Cycle found in AnimatorSet: " + this);
            } else {
                if (child.mStartTime != -1) {
                    if (parent.mEndTime == -1) {
                        child.mLatestParent = parent;
                        child.mStartTime = -1L;
                        child.mEndTime = -1L;
                    } else {
                        if (parent.mEndTime >= child.mStartTime) {
                            child.mLatestParent = parent;
                            child.mStartTime = parent.mEndTime;
                        }
                        long duration = child.mAnimation.getTotalDuration();
                        child.mEndTime = duration == -1 ? -1L : child.mStartTime + duration;
                    }
                }
                updatePlayTime(child, visited);
            }
        }
        visited.remove(parent);
    }

    private void findSiblings(Node node, ArrayList<Node> siblings) {
        if (siblings.contains(node)) {
            return;
        }
        siblings.add(node);
        if (node.mSiblings == null) {
            return;
        }
        for (int i = 0; i < node.mSiblings.size(); i++) {
            findSiblings(node.mSiblings.get(i), siblings);
        }
    }

    public boolean shouldPlayTogether() {
        updateAnimatorsDuration();
        createDependencyGraph();
        return this.mRootNode.mChildNodes.size() == this.mNodes.size() + (-1);
    }

    @Override
    public long getTotalDuration() {
        updateAnimatorsDuration();
        createDependencyGraph();
        return this.mTotalDuration;
    }

    private Node getNodeForAnimation(Animator anim) {
        Node node = this.mNodeMap.get(anim);
        if (node == null) {
            Node node2 = new Node(anim);
            this.mNodeMap.put(anim, node2);
            this.mNodes.add(node2);
            return node2;
        }
        return node;
    }

    private static class Node implements Cloneable {
        Animator mAnimation;
        ArrayList<Node> mParents;
        ArrayList<Node> mSiblings;
        ArrayList<Node> mChildNodes = null;
        private Node mTmpClone = null;
        boolean mEnded = false;
        Node mLatestParent = null;
        boolean mParentsAdded = false;
        long mStartTime = 0;
        long mEndTime = 0;
        long mTotalDuration = 0;

        public Node(Animator animation) {
            this.mAnimation = animation;
        }

        public Node m44clone() {
            try {
                Node node = (Node) super.clone();
                node.mAnimation = this.mAnimation.m35clone();
                if (this.mChildNodes != null) {
                    node.mChildNodes = new ArrayList<>(this.mChildNodes);
                }
                if (this.mSiblings != null) {
                    node.mSiblings = new ArrayList<>(this.mSiblings);
                }
                if (this.mParents != null) {
                    node.mParents = new ArrayList<>(this.mParents);
                }
                node.mEnded = false;
                return node;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        void addChild(Node node) {
            if (this.mChildNodes == null) {
                this.mChildNodes = new ArrayList<>();
            }
            if (this.mChildNodes.contains(node)) {
                return;
            }
            this.mChildNodes.add(node);
            node.addParent(this);
        }

        public void addSibling(Node node) {
            if (this.mSiblings == null) {
                this.mSiblings = new ArrayList<>();
            }
            if (this.mSiblings.contains(node)) {
                return;
            }
            this.mSiblings.add(node);
            node.addSibling(this);
        }

        public void addParent(Node node) {
            if (this.mParents == null) {
                this.mParents = new ArrayList<>();
            }
            if (this.mParents.contains(node)) {
                return;
            }
            this.mParents.add(node);
            node.addChild(this);
        }

        public void addParents(ArrayList<Node> parents) {
            if (parents == null) {
                return;
            }
            int size = parents.size();
            for (int i = 0; i < size; i++) {
                addParent(parents.get(i));
            }
        }
    }

    public class Builder {
        private Node mCurrentNode;

        Builder(Animator anim) {
            AnimatorSet.this.mDependencyDirty = true;
            this.mCurrentNode = AnimatorSet.this.getNodeForAnimation(anim);
        }

        public Builder with(Animator anim) {
            Node node = AnimatorSet.this.getNodeForAnimation(anim);
            this.mCurrentNode.addSibling(node);
            return this;
        }

        public Builder before(Animator anim) {
            AnimatorSet.this.mReversible = false;
            Node node = AnimatorSet.this.getNodeForAnimation(anim);
            this.mCurrentNode.addChild(node);
            return this;
        }

        public Builder after(Animator anim) {
            AnimatorSet.this.mReversible = false;
            Node node = AnimatorSet.this.getNodeForAnimation(anim);
            this.mCurrentNode.addParent(node);
            return this;
        }

        public Builder after(long delay) {
            ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
            anim.setDuration(delay);
            after(anim);
            return this;
        }
    }
}
