package android.animation;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.app.ActivityThread;
import android.app.Application;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Looper;
import android.provider.Telephony;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public final class AnimatorSet extends Animator implements AnimationHandler.AnimationFrameCallback {
    private static final String TAG = "AnimatorSet";
    private final boolean mEndCanBeCalled;
    private final boolean mShouldIgnoreEndWithoutStart;
    private final boolean mShouldResetValuesAtStart;
    private ArrayList<Node> mPlayingSet = new ArrayList<>();
    private ArrayMap<Animator, Node> mNodeMap = new ArrayMap<>();
    private ArrayList<AnimationEvent> mEvents = new ArrayList<>();
    private ArrayList<Node> mNodes = new ArrayList<>();
    private boolean mDependencyDirty = false;
    private boolean mStarted = false;
    private long mStartDelay = 0;
    private ValueAnimator mDelayAnim = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(0L);
    private Node mRootNode = new Node(this.mDelayAnim);
    private long mDuration = -1;
    private TimeInterpolator mInterpolator = null;
    private long mTotalDuration = 0;
    private long mLastFrameTime = -1;
    private long mFirstFrame = -1;
    private int mLastEventId = -1;
    private boolean mReversing = false;
    private boolean mSelfPulse = true;
    private SeekState mSeekState = new SeekState();
    private boolean mChildrenInitialized = false;
    private long mPauseTime = -1;
    private AnimatorListenerAdapter mDummyListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            if (AnimatorSet.this.mNodeMap.get(animator) != null) {
                ((Node) AnimatorSet.this.mNodeMap.get(animator)).mEnded = true;
                return;
            }
            throw new AndroidRuntimeException("Error: animation ended is not in the node map");
        }
    };

    public AnimatorSet() {
        boolean z = false;
        this.mNodeMap.put(this.mDelayAnim, this.mRootNode);
        this.mNodes.add(this.mRootNode);
        Application applicationCurrentApplication = ActivityThread.currentApplication();
        if (applicationCurrentApplication == null || applicationCurrentApplication.getApplicationInfo() == null) {
            this.mShouldIgnoreEndWithoutStart = true;
        } else {
            if (applicationCurrentApplication.getApplicationInfo().targetSdkVersion < 24) {
                this.mShouldIgnoreEndWithoutStart = true;
            } else {
                this.mShouldIgnoreEndWithoutStart = false;
            }
            if (applicationCurrentApplication.getApplicationInfo().targetSdkVersion < 26) {
            }
            this.mShouldResetValuesAtStart = !z;
            this.mEndCanBeCalled = !z;
        }
        z = true;
        this.mShouldResetValuesAtStart = !z;
        this.mEndCanBeCalled = !z;
    }

    public void playTogether(Animator... animatorArr) {
        if (animatorArr != null) {
            Builder builderPlay = play(animatorArr[0]);
            for (int i = 1; i < animatorArr.length; i++) {
                builderPlay.with(animatorArr[i]);
            }
        }
    }

    public void playTogether(Collection<Animator> collection) {
        if (collection != null && collection.size() > 0) {
            Builder builderPlay = null;
            for (Animator animator : collection) {
                if (builderPlay == null) {
                    builderPlay = play(animator);
                } else {
                    builderPlay.with(animator);
                }
            }
        }
    }

    public void playSequentially(Animator... animatorArr) {
        if (animatorArr != null) {
            int i = 0;
            if (animatorArr.length == 1) {
                play(animatorArr[0]);
                return;
            }
            while (i < animatorArr.length - 1) {
                Builder builderPlay = play(animatorArr[i]);
                i++;
                builderPlay.before(animatorArr[i]);
            }
        }
    }

    public void playSequentially(List<Animator> list) {
        if (list != null && list.size() > 0) {
            int i = 0;
            if (list.size() == 1) {
                play(list.get(0));
                return;
            }
            while (i < list.size() - 1) {
                Builder builderPlay = play(list.get(i));
                i++;
                builderPlay.before(list.get(i));
            }
        }
    }

    public ArrayList<Animator> getChildAnimations() {
        ArrayList<Animator> arrayList = new ArrayList<>();
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            if (node != this.mRootNode) {
                arrayList.add(node.mAnimation);
            }
        }
        return arrayList;
    }

    @Override
    public void setTarget(Object obj) {
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Animator animator = this.mNodes.get(i).mAnimation;
            if (animator instanceof AnimatorSet) {
                ((AnimatorSet) animator).setTarget(obj);
            } else if (animator instanceof ObjectAnimator) {
                ((ObjectAnimator) animator).setTarget(obj);
            }
        }
    }

    @Override
    public int getChangingConfigurations() {
        int changingConfigurations = super.getChangingConfigurations();
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            changingConfigurations |= this.mNodes.get(i).mAnimation.getChangingConfigurations();
        }
        return changingConfigurations;
    }

    @Override
    public void setInterpolator(TimeInterpolator timeInterpolator) {
        this.mInterpolator = timeInterpolator;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    public Builder play(Animator animator) {
        if (animator != null) {
            return new Builder(animator);
        }
        return null;
    }

    @Override
    public void cancel() {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        if (isStarted()) {
            if (this.mListeners != null) {
                ArrayList arrayList = (ArrayList) this.mListeners.clone();
                int size = arrayList.size();
                for (int i = 0; i < size; i++) {
                    ((Animator.AnimatorListener) arrayList.get(i)).onAnimationCancel(this);
                }
            }
            ArrayList arrayList2 = new ArrayList(this.mPlayingSet);
            int size2 = arrayList2.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ((Node) arrayList2.get(i2)).mAnimation.cancel();
            }
            this.mPlayingSet.clear();
            endAnimation();
        }
    }

    private void forceToEnd() {
        if (this.mEndCanBeCalled) {
            end();
            return;
        }
        if (this.mReversing) {
            handleAnimationEvents(this.mLastEventId, 0, getTotalDuration());
        } else {
            long totalDuration = getTotalDuration();
            if (totalDuration == -1) {
                totalDuration = 2147483647L;
            }
            handleAnimationEvents(this.mLastEventId, this.mEvents.size() - 1, totalDuration);
        }
        this.mPlayingSet.clear();
        endAnimation();
    }

    @Override
    public void end() {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        if (this.mShouldIgnoreEndWithoutStart && !isStarted()) {
            return;
        }
        if (isStarted()) {
            if (this.mReversing) {
                this.mLastEventId = this.mLastEventId == -1 ? this.mEvents.size() : this.mLastEventId;
                while (this.mLastEventId > 0) {
                    this.mLastEventId--;
                    AnimationEvent animationEvent = this.mEvents.get(this.mLastEventId);
                    Animator animator = animationEvent.mNode.mAnimation;
                    if (!this.mNodeMap.get(animator).mEnded) {
                        if (animationEvent.mEvent == 2) {
                            animator.reverse();
                        } else if (animationEvent.mEvent == 1 && animator.isStarted()) {
                            animator.end();
                        }
                    }
                }
            } else {
                while (this.mLastEventId < this.mEvents.size() - 1) {
                    this.mLastEventId++;
                    AnimationEvent animationEvent2 = this.mEvents.get(this.mLastEventId);
                    Animator animator2 = animationEvent2.mNode.mAnimation;
                    if (!this.mNodeMap.get(animator2).mEnded) {
                        if (animationEvent2.mEvent == 0) {
                            animator2.start();
                        } else if (animationEvent2.mEvent == 2 && animator2.isStarted()) {
                            animator2.end();
                        }
                    }
                }
            }
            this.mPlayingSet.clear();
        }
        endAnimation();
    }

    @Override
    public boolean isRunning() {
        if (this.mStartDelay == 0) {
            return this.mStarted;
        }
        return this.mLastFrameTime > 0;
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
    public void setStartDelay(long j) {
        if (j < 0) {
            Log.w(TAG, "Start delay should always be non-negative");
            j = 0;
        }
        long j2 = j - this.mStartDelay;
        if (j2 == 0) {
            return;
        }
        this.mStartDelay = j;
        if (!this.mDependencyDirty) {
            int size = this.mNodes.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                Node node = this.mNodes.get(i);
                if (node == this.mRootNode) {
                    node.mEndTime = this.mStartDelay;
                } else {
                    node.mStartTime = node.mStartTime == -1 ? -1L : node.mStartTime + j2;
                    node.mEndTime = node.mEndTime != -1 ? node.mEndTime + j2 : -1L;
                }
                i++;
            }
            if (this.mTotalDuration != -1) {
                this.mTotalDuration += j2;
            }
        }
    }

    @Override
    public long getDuration() {
        return this.mDuration;
    }

    @Override
    public AnimatorSet setDuration(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("duration must be a value of zero or greater");
        }
        this.mDependencyDirty = true;
        this.mDuration = j;
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
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        boolean z = this.mPaused;
        super.pause();
        if (!z && this.mPaused) {
            this.mPauseTime = -1L;
        }
    }

    @Override
    public void resume() {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        boolean z = this.mPaused;
        super.resume();
        if (z && !this.mPaused && this.mPauseTime >= 0) {
            addAnimationCallback(0L);
        }
    }

    @Override
    public void start() {
        start(false, true);
    }

    @Override
    void startWithoutPulsing(boolean z) {
        start(z, false);
    }

    private void initAnimation() {
        if (this.mInterpolator != null) {
            for (int i = 0; i < this.mNodes.size(); i++) {
                this.mNodes.get(i).mAnimation.setInterpolator(this.mInterpolator);
            }
        }
        updateAnimatorsDuration();
        createDependencyGraph();
    }

    private void start(boolean z, boolean z2) {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        this.mStarted = true;
        this.mSelfPulse = z2;
        this.mPaused = false;
        this.mPauseTime = -1L;
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = this.mNodes.get(i);
            node.mEnded = false;
            node.mAnimation.setAllowRunningAsynchronously(false);
        }
        initAnimation();
        if (z && !canReverse()) {
            throw new UnsupportedOperationException("Cannot reverse infinite AnimatorSet");
        }
        this.mReversing = z;
        boolean zIsEmptySet = isEmptySet(this);
        if (!zIsEmptySet) {
            startAnimation();
        }
        if (this.mListeners != null) {
            ArrayList arrayList = (ArrayList) this.mListeners.clone();
            int size2 = arrayList.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ((Animator.AnimatorListener) arrayList.get(i2)).onAnimationStart(this, z);
            }
        }
        if (zIsEmptySet) {
            end();
        }
    }

    private static boolean isEmptySet(AnimatorSet animatorSet) {
        if (animatorSet.getStartDelay() > 0) {
            return false;
        }
        for (int i = 0; i < animatorSet.getChildAnimations().size(); i++) {
            Animator animator = animatorSet.getChildAnimations().get(i);
            if (!(animator instanceof AnimatorSet) || !isEmptySet((AnimatorSet) animator)) {
                return false;
            }
        }
        return true;
    }

    private void updateAnimatorsDuration() {
        if (this.mDuration >= 0) {
            int size = this.mNodes.size();
            for (int i = 0; i < size; i++) {
                this.mNodes.get(i).mAnimation.setDuration(this.mDuration);
            }
        }
        this.mDelayAnim.setDuration(this.mStartDelay);
    }

    @Override
    void skipToEndValue(boolean z) {
        if (!isInitialized()) {
            throw new UnsupportedOperationException("Children must be initialized.");
        }
        initAnimation();
        if (z) {
            for (int size = this.mEvents.size() - 1; size >= 0; size--) {
                if (this.mEvents.get(size).mEvent == 1) {
                    this.mEvents.get(size).mNode.mAnimation.skipToEndValue(true);
                }
            }
            return;
        }
        for (int i = 0; i < this.mEvents.size(); i++) {
            if (this.mEvents.get(i).mEvent == 2) {
                this.mEvents.get(i).mNode.mAnimation.skipToEndValue(false);
            }
        }
    }

    @Override
    void animateBasedOnPlayTime(long j, long j2, boolean z) {
        if (j < 0 || j2 < 0) {
            throw new UnsupportedOperationException("Error: Play time should never be negative.");
        }
        if (z) {
            if (getTotalDuration() == -1) {
                throw new UnsupportedOperationException("Cannot reverse AnimatorSet with infinite duration");
            }
            long totalDuration = getTotalDuration() - this.mStartDelay;
            j = totalDuration - Math.min(j, totalDuration);
            j2 = totalDuration - j2;
            z = false;
        }
        skipToStartValue(false);
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mEvents.size(); i++) {
            AnimationEvent animationEvent = this.mEvents.get(i);
            if (animationEvent.getTime() > j || animationEvent.getTime() == -1) {
                break;
            }
            if (animationEvent.mEvent == 1 && (animationEvent.mNode.mEndTime == -1 || animationEvent.mNode.mEndTime > j)) {
                arrayList.add(animationEvent.mNode);
            }
            if (animationEvent.mEvent == 2) {
                animationEvent.mNode.mAnimation.skipToEndValue(false);
            }
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            Node node = (Node) arrayList.get(i2);
            long playTimeForNode = getPlayTimeForNode(j, node, z);
            if (!z) {
                playTimeForNode -= node.mAnimation.getStartDelay();
            }
            node.mAnimation.animateBasedOnPlayTime(playTimeForNode, j2, z);
        }
    }

    @Override
    boolean isInitialized() {
        if (this.mChildrenInitialized) {
            return true;
        }
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < this.mNodes.size()) {
                if (!this.mNodes.get(i).mAnimation.isInitialized()) {
                    break;
                }
                i++;
            } else {
                z = true;
                break;
            }
        }
        this.mChildrenInitialized = z;
        return this.mChildrenInitialized;
    }

    private void skipToStartValue(boolean z) {
        skipToEndValue(!z);
    }

    public void setCurrentPlayTime(long j) {
        if (this.mReversing && getTotalDuration() == -1) {
            throw new UnsupportedOperationException("Error: Cannot seek in reverse in an infinite AnimatorSet");
        }
        if ((getTotalDuration() != -1 && j > getTotalDuration() - this.mStartDelay) || j < 0) {
            throw new UnsupportedOperationException("Error: Play time should always be in between0 and duration.");
        }
        initAnimation();
        if (!isStarted()) {
            if (this.mReversing) {
                throw new UnsupportedOperationException("Error: Something went wrong. mReversing should not be set when AnimatorSet is not started.");
            }
            if (!this.mSeekState.isActive()) {
                findLatestEventIdForTime(0L);
                initChildren();
                skipToStartValue(this.mReversing);
                this.mSeekState.setPlayTime(0L, this.mReversing);
            }
            animateBasedOnPlayTime(j, 0L, this.mReversing);
            this.mSeekState.setPlayTime(j, this.mReversing);
            return;
        }
        this.mSeekState.setPlayTime(j, this.mReversing);
    }

    public long getCurrentPlayTime() {
        if (this.mSeekState.isActive()) {
            return this.mSeekState.getPlayTime();
        }
        if (this.mLastFrameTime == -1) {
            return 0L;
        }
        float durationScale = ValueAnimator.getDurationScale();
        if (durationScale == 0.0f) {
            durationScale = 1.0f;
        }
        if (this.mReversing) {
            return (long) ((this.mLastFrameTime - this.mFirstFrame) / durationScale);
        }
        return (long) (((this.mLastFrameTime - this.mFirstFrame) - this.mStartDelay) / durationScale);
    }

    private void initChildren() {
        if (!isInitialized()) {
            this.mChildrenInitialized = true;
            skipToEndValue(false);
        }
    }

    @Override
    public boolean doAnimationFrame(long j) {
        float durationScale = ValueAnimator.getDurationScale();
        if (durationScale == 0.0f) {
            forceToEnd();
            return true;
        }
        if (this.mFirstFrame < 0) {
            this.mFirstFrame = j;
        }
        if (this.mPaused) {
            this.mPauseTime = j;
            removeAnimationCallback();
            return false;
        }
        if (this.mPauseTime > 0) {
            this.mFirstFrame += j - this.mPauseTime;
            this.mPauseTime = -1L;
        }
        if (this.mSeekState.isActive()) {
            this.mSeekState.updateSeekDirection(this.mReversing);
            if (this.mReversing) {
                this.mFirstFrame = (long) (j - (this.mSeekState.getPlayTime() * durationScale));
            } else {
                this.mFirstFrame = (long) (j - ((this.mSeekState.getPlayTime() + this.mStartDelay) * durationScale));
            }
            this.mSeekState.reset();
        }
        if (!this.mReversing && j < this.mFirstFrame + (this.mStartDelay * durationScale)) {
            return false;
        }
        long j2 = (long) ((j - this.mFirstFrame) / durationScale);
        this.mLastFrameTime = j;
        int iFindLatestEventIdForTime = findLatestEventIdForTime(j2);
        handleAnimationEvents(this.mLastEventId, iFindLatestEventIdForTime, j2);
        this.mLastEventId = iFindLatestEventIdForTime;
        for (int i = 0; i < this.mPlayingSet.size(); i++) {
            Node node = this.mPlayingSet.get(i);
            if (!node.mEnded) {
                pulseFrame(node, getPlayTimeForNode(j2, node));
            }
        }
        for (int size = this.mPlayingSet.size() - 1; size >= 0; size--) {
            if (this.mPlayingSet.get(size).mEnded) {
                this.mPlayingSet.remove(size);
            }
        }
        if (!(!this.mReversing ? !(this.mPlayingSet.isEmpty() && this.mLastEventId == this.mEvents.size() - 1) : !(this.mPlayingSet.size() == 1 && this.mPlayingSet.get(0) == this.mRootNode) && (!this.mPlayingSet.isEmpty() || this.mLastEventId >= 3))) {
            return false;
        }
        endAnimation();
        return true;
    }

    @Override
    public void commitAnimationFrame(long j) {
    }

    @Override
    boolean pulseAnimationFrame(long j) {
        return doAnimationFrame(j);
    }

    private void handleAnimationEvents(int i, int i2, long j) {
        if (this.mReversing) {
            if (i == -1) {
                i = this.mEvents.size();
            }
            for (int i3 = i - 1; i3 >= i2; i3--) {
                AnimationEvent animationEvent = this.mEvents.get(i3);
                Node node = animationEvent.mNode;
                if (animationEvent.mEvent == 2) {
                    if (node.mAnimation.isStarted()) {
                        node.mAnimation.cancel();
                    }
                    node.mEnded = false;
                    this.mPlayingSet.add(animationEvent.mNode);
                    node.mAnimation.startWithoutPulsing(true);
                    pulseFrame(node, 0L);
                } else if (animationEvent.mEvent == 1 && !node.mEnded) {
                    pulseFrame(node, getPlayTimeForNode(j, node));
                }
            }
            return;
        }
        for (int i4 = i + 1; i4 <= i2; i4++) {
            AnimationEvent animationEvent2 = this.mEvents.get(i4);
            Node node2 = animationEvent2.mNode;
            if (animationEvent2.mEvent == 0) {
                this.mPlayingSet.add(animationEvent2.mNode);
                if (node2.mAnimation.isStarted()) {
                    node2.mAnimation.cancel();
                }
                node2.mEnded = false;
                node2.mAnimation.startWithoutPulsing(false);
                pulseFrame(node2, 0L);
            } else if (animationEvent2.mEvent == 2 && !node2.mEnded) {
                pulseFrame(node2, getPlayTimeForNode(j, node2));
            }
        }
    }

    private void pulseFrame(Node node, long j) {
        if (!node.mEnded) {
            float durationScale = ValueAnimator.getDurationScale();
            if (durationScale == 0.0f) {
                durationScale = 1.0f;
            }
            node.mEnded = node.mAnimation.pulseAnimationFrame((long) (j * durationScale));
        }
    }

    private long getPlayTimeForNode(long j, Node node) {
        return getPlayTimeForNode(j, node, this.mReversing);
    }

    private long getPlayTimeForNode(long j, Node node, boolean z) {
        if (z) {
            return node.mEndTime - (getTotalDuration() - j);
        }
        return j - node.mStartTime;
    }

    private void startAnimation() {
        addDummyListener();
        long playTime = 0;
        addAnimationCallback(0L);
        if (this.mSeekState.getPlayTimeNormalized() == 0 && this.mReversing) {
            this.mSeekState.reset();
        }
        if (this.mShouldResetValuesAtStart) {
            if (isInitialized()) {
                skipToEndValue(!this.mReversing);
            } else if (this.mReversing) {
                initChildren();
                skipToEndValue(!this.mReversing);
            } else {
                for (int size = this.mEvents.size() - 1; size >= 0; size--) {
                    if (this.mEvents.get(size).mEvent == 1) {
                        Animator animator = this.mEvents.get(size).mNode.mAnimation;
                        if (animator.isInitialized()) {
                            animator.skipToEndValue(true);
                        }
                    }
                }
            }
        }
        if (this.mReversing || this.mStartDelay == 0 || this.mSeekState.isActive()) {
            if (this.mSeekState.isActive()) {
                this.mSeekState.updateSeekDirection(this.mReversing);
                playTime = this.mSeekState.getPlayTime();
            }
            int iFindLatestEventIdForTime = findLatestEventIdForTime(playTime);
            handleAnimationEvents(-1, iFindLatestEventIdForTime, playTime);
            for (int size2 = this.mPlayingSet.size() - 1; size2 >= 0; size2--) {
                if (this.mPlayingSet.get(size2).mEnded) {
                    this.mPlayingSet.remove(size2);
                }
            }
            this.mLastEventId = iFindLatestEventIdForTime;
        }
    }

    private void addDummyListener() {
        for (int i = 1; i < this.mNodes.size(); i++) {
            this.mNodes.get(i).mAnimation.addListener(this.mDummyListener);
        }
    }

    private void removeDummyListener() {
        for (int i = 1; i < this.mNodes.size(); i++) {
            this.mNodes.get(i).mAnimation.removeListener(this.mDummyListener);
        }
    }

    private int findLatestEventIdForTime(long j) {
        int size = this.mEvents.size();
        int i = this.mLastEventId;
        if (this.mReversing) {
            long totalDuration = getTotalDuration() - j;
            if (this.mLastEventId != -1) {
                size = this.mLastEventId;
            }
            this.mLastEventId = size;
            for (int i2 = this.mLastEventId - 1; i2 >= 0; i2--) {
                if (this.mEvents.get(i2).getTime() >= totalDuration) {
                    i = i2;
                }
            }
        } else {
            int i3 = this.mLastEventId;
            while (true) {
                i3++;
                if (i3 >= size) {
                    break;
                }
                AnimationEvent animationEvent = this.mEvents.get(i3);
                if (animationEvent.getTime() != -1 && animationEvent.getTime() <= j) {
                    i = i3;
                }
            }
        }
        return i;
    }

    private void endAnimation() {
        this.mStarted = false;
        this.mLastFrameTime = -1L;
        this.mFirstFrame = -1L;
        this.mLastEventId = -1;
        this.mPaused = false;
        this.mPauseTime = -1L;
        this.mSeekState.reset();
        this.mPlayingSet.clear();
        removeAnimationCallback();
        if (this.mListeners != null) {
            ArrayList arrayList = (ArrayList) this.mListeners.clone();
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((Animator.AnimatorListener) arrayList.get(i)).onAnimationEnd(this, this.mReversing);
            }
        }
        removeDummyListener();
        this.mSelfPulse = true;
        this.mReversing = false;
    }

    private void removeAnimationCallback() {
        if (!this.mSelfPulse) {
            return;
        }
        AnimationHandler.getInstance().removeCallback(this);
    }

    private void addAnimationCallback(long j) {
        if (!this.mSelfPulse) {
            return;
        }
        AnimationHandler.getInstance().addAnimationFrameCallback(this, j);
    }

    @Override
    public AnimatorSet mo0clone() {
        int size;
        int size2;
        int size3;
        final AnimatorSet animatorSet = (AnimatorSet) super.mo0clone();
        int size4 = this.mNodes.size();
        animatorSet.mStarted = false;
        animatorSet.mLastFrameTime = -1L;
        animatorSet.mFirstFrame = -1L;
        animatorSet.mLastEventId = -1;
        animatorSet.mPaused = false;
        animatorSet.mPauseTime = -1L;
        animatorSet.mSeekState = new SeekState();
        animatorSet.mSelfPulse = true;
        animatorSet.mPlayingSet = new ArrayList<>();
        animatorSet.mNodeMap = new ArrayMap<>();
        animatorSet.mNodes = new ArrayList<>(size4);
        animatorSet.mEvents = new ArrayList<>();
        animatorSet.mDummyListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (animatorSet.mNodeMap.get(animator) != null) {
                    ((Node) animatorSet.mNodeMap.get(animator)).mEnded = true;
                    return;
                }
                throw new AndroidRuntimeException("Error: animation ended is not in the node map");
            }
        };
        animatorSet.mReversing = false;
        animatorSet.mDependencyDirty = true;
        HashMap map = new HashMap(size4);
        for (int i = 0; i < size4; i++) {
            Node node = this.mNodes.get(i);
            Node nodeM1clone = node.m1clone();
            nodeM1clone.mAnimation.removeListener(this.mDummyListener);
            map.put(node, nodeM1clone);
            animatorSet.mNodes.add(nodeM1clone);
            animatorSet.mNodeMap.put(nodeM1clone.mAnimation, nodeM1clone);
        }
        animatorSet.mRootNode = (Node) map.get(this.mRootNode);
        animatorSet.mDelayAnim = (ValueAnimator) animatorSet.mRootNode.mAnimation;
        for (int i2 = 0; i2 < size4; i2++) {
            Node node2 = this.mNodes.get(i2);
            Node node3 = (Node) map.get(node2);
            node3.mLatestParent = node2.mLatestParent == null ? null : (Node) map.get(node2.mLatestParent);
            if (node2.mChildNodes != null) {
                size = node2.mChildNodes.size();
            } else {
                size = 0;
            }
            for (int i3 = 0; i3 < size; i3++) {
                node3.mChildNodes.set(i3, (Node) map.get(node2.mChildNodes.get(i3)));
            }
            if (node2.mSiblings != null) {
                size2 = node2.mSiblings.size();
            } else {
                size2 = 0;
            }
            for (int i4 = 0; i4 < size2; i4++) {
                node3.mSiblings.set(i4, (Node) map.get(node2.mSiblings.get(i4)));
            }
            if (node2.mParents != null) {
                size3 = node2.mParents.size();
            } else {
                size3 = 0;
            }
            for (int i5 = 0; i5 < size3; i5++) {
                node3.mParents.set(i5, (Node) map.get(node2.mParents.get(i5)));
            }
        }
        return animatorSet;
    }

    @Override
    public boolean canReverse() {
        return getTotalDuration() != -1;
    }

    @Override
    public void reverse() {
        start(true, true);
    }

    public String toString() {
        String str = "AnimatorSet@" + Integer.toHexString(hashCode()) + "{";
        int size = this.mNodes.size();
        for (int i = 0; i < size; i++) {
            str = str + "\n    " + this.mNodes.get(i).mAnimation.toString();
        }
        return str + "\n}";
    }

    private void printChildCount() {
        int i;
        ArrayList arrayList = new ArrayList(this.mNodes.size());
        arrayList.add(this.mRootNode);
        Log.d(TAG, "Current tree: ");
        int i2 = 0;
        while (i2 < arrayList.size()) {
            int size = arrayList.size();
            StringBuilder sb = new StringBuilder();
            while (i2 < size) {
                Node node = (Node) arrayList.get(i2);
                if (node.mChildNodes != null) {
                    i = 0;
                    for (int i3 = 0; i3 < node.mChildNodes.size(); i3++) {
                        Node node2 = node.mChildNodes.get(i3);
                        if (node2.mLatestParent == node) {
                            i++;
                            arrayList.add(node2);
                        }
                    }
                } else {
                    i = 0;
                }
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(i);
                i2++;
            }
            Log.d(TAG, sb.toString());
        }
    }

    private void createDependencyGraph() {
        boolean z;
        if (!this.mDependencyDirty) {
            int i = 0;
            while (true) {
                if (i >= this.mNodes.size()) {
                    z = false;
                    break;
                }
                if (this.mNodes.get(i).mTotalDuration == this.mNodes.get(i).mAnimation.getTotalDuration()) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
            if (!z) {
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
                    int size2 = node.mSiblings.size();
                    for (int i4 = 0; i4 < size2; i4++) {
                        node.addParents(node.mSiblings.get(i4).mParents);
                    }
                    for (int i5 = 0; i5 < size2; i5++) {
                        Node node2 = node.mSiblings.get(i5);
                        node2.addParents(node.mParents);
                        node2.mParentsAdded = true;
                    }
                }
            }
        }
        for (int i6 = 0; i6 < size; i6++) {
            Node node3 = this.mNodes.get(i6);
            if (node3 != this.mRootNode && node3.mParents == null) {
                node3.addParent(this.mRootNode);
            }
        }
        ArrayList<Node> arrayList = new ArrayList<>(this.mNodes.size());
        this.mRootNode.mStartTime = 0L;
        this.mRootNode.mEndTime = this.mDelayAnim.getDuration();
        updatePlayTime(this.mRootNode, arrayList);
        sortAnimationEvents();
        this.mTotalDuration = this.mEvents.get(this.mEvents.size() - 1).getTime();
    }

    private void sortAnimationEvents() {
        boolean z;
        this.mEvents.clear();
        for (int i = 1; i < this.mNodes.size(); i++) {
            Node node = this.mNodes.get(i);
            this.mEvents.add(new AnimationEvent(node, 0));
            this.mEvents.add(new AnimationEvent(node, 1));
            this.mEvents.add(new AnimationEvent(node, 2));
        }
        this.mEvents.sort(new Comparator<AnimationEvent>() {
            @Override
            public int compare(AnimationEvent animationEvent, AnimationEvent animationEvent2) {
                long time = animationEvent.getTime();
                long time2 = animationEvent2.getTime();
                if (time == time2) {
                    if (animationEvent2.mEvent + animationEvent.mEvent == 1) {
                        return animationEvent.mEvent - animationEvent2.mEvent;
                    }
                    return animationEvent2.mEvent - animationEvent.mEvent;
                }
                if (time2 == -1) {
                    return -1;
                }
                if (time == -1) {
                    return 1;
                }
                return (int) (time - time2);
            }
        });
        int size = this.mEvents.size();
        int i2 = 0;
        while (i2 < size) {
            AnimationEvent animationEvent = this.mEvents.get(i2);
            if (animationEvent.mEvent == 2) {
                if (animationEvent.mNode.mStartTime == animationEvent.mNode.mEndTime) {
                    z = true;
                } else if (animationEvent.mNode.mEndTime != animationEvent.mNode.mStartTime + animationEvent.mNode.mAnimation.getStartDelay()) {
                    i2++;
                } else {
                    z = false;
                }
                int i3 = i2 + 1;
                int i4 = size;
                int i5 = i4;
                for (int i6 = i3; i6 < size && (i4 >= size || i5 >= size); i6++) {
                    if (this.mEvents.get(i6).mNode == animationEvent.mNode) {
                        if (this.mEvents.get(i6).mEvent != 0) {
                            if (this.mEvents.get(i6).mEvent == 1) {
                                i5 = i6;
                            }
                        } else {
                            i4 = i6;
                        }
                    }
                }
                if (z && i4 == this.mEvents.size()) {
                    throw new UnsupportedOperationException("Something went wrong, no start isfound after stop for an animation that has the same start and endtime.");
                }
                if (i5 == this.mEvents.size()) {
                    throw new UnsupportedOperationException("Something went wrong, no startdelay end is found after stop for an animation");
                }
                if (z) {
                    this.mEvents.add(i2, this.mEvents.remove(i4));
                    i2 = i3;
                }
                this.mEvents.add(i2, this.mEvents.remove(i5));
                i2 += 2;
            } else {
                i2++;
            }
        }
        if (!this.mEvents.isEmpty() && this.mEvents.get(0).mEvent != 0) {
            throw new UnsupportedOperationException("Sorting went bad, the start event should always be at index 0");
        }
        this.mEvents.add(0, new AnimationEvent(this.mRootNode, 0));
        this.mEvents.add(1, new AnimationEvent(this.mRootNode, 1));
        this.mEvents.add(2, new AnimationEvent(this.mRootNode, 2));
        if (this.mEvents.get(this.mEvents.size() - 1).mEvent == 0 || this.mEvents.get(this.mEvents.size() - 1).mEvent == 1) {
            throw new UnsupportedOperationException("Something went wrong, the last event is not an end event");
        }
    }

    private void updatePlayTime(Node node, ArrayList<Node> arrayList) {
        int i = 0;
        if (node.mChildNodes == null) {
            if (node == this.mRootNode) {
                while (i < this.mNodes.size()) {
                    Node node2 = this.mNodes.get(i);
                    if (node2 != this.mRootNode) {
                        node2.mStartTime = -1L;
                        node2.mEndTime = -1L;
                    }
                    i++;
                }
                return;
            }
            return;
        }
        arrayList.add(node);
        int size = node.mChildNodes.size();
        while (i < size) {
            Node node3 = node.mChildNodes.get(i);
            node3.mTotalDuration = node3.mAnimation.getTotalDuration();
            int iIndexOf = arrayList.indexOf(node3);
            if (iIndexOf >= 0) {
                while (iIndexOf < arrayList.size()) {
                    arrayList.get(iIndexOf).mLatestParent = null;
                    arrayList.get(iIndexOf).mStartTime = -1L;
                    arrayList.get(iIndexOf).mEndTime = -1L;
                    iIndexOf++;
                }
                node3.mStartTime = -1L;
                node3.mEndTime = -1L;
                node3.mLatestParent = null;
                Log.w(TAG, "Cycle found in AnimatorSet: " + this);
            } else {
                if (node3.mStartTime != -1) {
                    if (node.mEndTime == -1) {
                        node3.mLatestParent = node;
                        node3.mStartTime = -1L;
                        node3.mEndTime = -1L;
                    } else {
                        if (node.mEndTime >= node3.mStartTime) {
                            node3.mLatestParent = node;
                            node3.mStartTime = node.mEndTime;
                        }
                        node3.mEndTime = node3.mTotalDuration == -1 ? -1L : node3.mStartTime + node3.mTotalDuration;
                    }
                }
                updatePlayTime(node3, arrayList);
            }
            i++;
        }
        arrayList.remove(node);
    }

    private void findSiblings(Node node, ArrayList<Node> arrayList) {
        if (!arrayList.contains(node)) {
            arrayList.add(node);
            if (node.mSiblings == null) {
                return;
            }
            for (int i = 0; i < node.mSiblings.size(); i++) {
                findSiblings(node.mSiblings.get(i), arrayList);
            }
        }
    }

    public boolean shouldPlayTogether() {
        updateAnimatorsDuration();
        createDependencyGraph();
        return this.mRootNode.mChildNodes == null || this.mRootNode.mChildNodes.size() == this.mNodes.size() - 1;
    }

    @Override
    public long getTotalDuration() {
        updateAnimatorsDuration();
        createDependencyGraph();
        return this.mTotalDuration;
    }

    private Node getNodeForAnimation(Animator animator) {
        Node node = this.mNodeMap.get(animator);
        if (node == null) {
            Node node2 = new Node(animator);
            this.mNodeMap.put(animator, node2);
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
        boolean mEnded = false;
        Node mLatestParent = null;
        boolean mParentsAdded = false;
        long mStartTime = 0;
        long mEndTime = 0;
        long mTotalDuration = 0;

        public Node(Animator animator) {
            this.mAnimation = animator;
        }

        public Node m1clone() {
            try {
                Node node = (Node) super.clone();
                node.mAnimation = this.mAnimation.mo0clone();
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
            if (!this.mChildNodes.contains(node)) {
                this.mChildNodes.add(node);
                node.addParent(this);
            }
        }

        public void addSibling(Node node) {
            if (this.mSiblings == null) {
                this.mSiblings = new ArrayList<>();
            }
            if (!this.mSiblings.contains(node)) {
                this.mSiblings.add(node);
                node.addSibling(this);
            }
        }

        public void addParent(Node node) {
            if (this.mParents == null) {
                this.mParents = new ArrayList<>();
            }
            if (!this.mParents.contains(node)) {
                this.mParents.add(node);
                node.addChild(this);
            }
        }

        public void addParents(ArrayList<Node> arrayList) {
            if (arrayList == null) {
                return;
            }
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                addParent(arrayList.get(i));
            }
        }
    }

    private static class AnimationEvent {
        static final int ANIMATION_DELAY_ENDED = 1;
        static final int ANIMATION_END = 2;
        static final int ANIMATION_START = 0;
        final int mEvent;
        final Node mNode;

        AnimationEvent(Node node, int i) {
            this.mNode = node;
            this.mEvent = i;
        }

        long getTime() {
            if (this.mEvent == 0) {
                return this.mNode.mStartTime;
            }
            if (this.mEvent != 1) {
                return this.mNode.mEndTime;
            }
            if (this.mNode.mStartTime == -1) {
                return -1L;
            }
            return this.mNode.mAnimation.getStartDelay() + this.mNode.mStartTime;
        }

        public String toString() {
            String str;
            if (this.mEvent == 0) {
                str = Telephony.BaseMmsColumns.START;
            } else {
                str = this.mEvent == 1 ? "delay ended" : "end";
            }
            return str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.mNode.mAnimation.toString();
        }
    }

    private class SeekState {
        private long mPlayTime;
        private boolean mSeekingInReverse;

        private SeekState() {
            this.mPlayTime = -1L;
            this.mSeekingInReverse = false;
        }

        void reset() {
            this.mPlayTime = -1L;
            this.mSeekingInReverse = false;
        }

        void setPlayTime(long j, boolean z) {
            if (AnimatorSet.this.getTotalDuration() != -1) {
                this.mPlayTime = Math.min(j, AnimatorSet.this.getTotalDuration() - AnimatorSet.this.mStartDelay);
            }
            this.mPlayTime = Math.max(0L, this.mPlayTime);
            this.mSeekingInReverse = z;
        }

        void updateSeekDirection(boolean z) {
            if (z && AnimatorSet.this.getTotalDuration() == -1) {
                throw new UnsupportedOperationException("Error: Cannot reverse infinite animator set");
            }
            if (this.mPlayTime >= 0 && z != this.mSeekingInReverse) {
                this.mPlayTime = (AnimatorSet.this.getTotalDuration() - AnimatorSet.this.mStartDelay) - this.mPlayTime;
                this.mSeekingInReverse = z;
            }
        }

        long getPlayTime() {
            return this.mPlayTime;
        }

        long getPlayTimeNormalized() {
            if (AnimatorSet.this.mReversing) {
                return (AnimatorSet.this.getTotalDuration() - AnimatorSet.this.mStartDelay) - this.mPlayTime;
            }
            return this.mPlayTime;
        }

        boolean isActive() {
            return this.mPlayTime != -1;
        }
    }

    public class Builder {
        private Node mCurrentNode;

        Builder(Animator animator) {
            AnimatorSet.this.mDependencyDirty = true;
            this.mCurrentNode = AnimatorSet.this.getNodeForAnimation(animator);
        }

        public Builder with(Animator animator) {
            this.mCurrentNode.addSibling(AnimatorSet.this.getNodeForAnimation(animator));
            return this;
        }

        public Builder before(Animator animator) {
            this.mCurrentNode.addChild(AnimatorSet.this.getNodeForAnimation(animator));
            return this;
        }

        public Builder after(Animator animator) {
            this.mCurrentNode.addParent(AnimatorSet.this.getNodeForAnimation(animator));
            return this;
        }

        public Builder after(long j) {
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimatorOfFloat.setDuration(j);
            after(valueAnimatorOfFloat);
            return this;
        }
    }
}
