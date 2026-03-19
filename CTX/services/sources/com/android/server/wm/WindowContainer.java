package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Pools;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.MagnificationSpec;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.WindowContainer;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

class WindowContainer<E extends WindowContainer> extends ConfigurationContainer<E> implements Comparable<WindowContainer>, SurfaceAnimator.Animatable {
    static final int ANIMATION_LAYER_BOOSTED = 1;
    static final int ANIMATION_LAYER_HOME = 2;
    static final int ANIMATION_LAYER_STANDARD = 0;
    static final int POSITION_BOTTOM = Integer.MIN_VALUE;
    static final int POSITION_TOP = Integer.MAX_VALUE;
    private static final String TAG = "WindowManager";
    private boolean mCommittedReparentToAnimationLeash;
    WindowContainerController mController;
    protected final SurfaceControl.Transaction mPendingTransaction;
    protected final WindowManagerService mService;
    protected final SurfaceAnimator mSurfaceAnimator;
    protected SurfaceControl mSurfaceControl;
    private WindowContainer<WindowContainer> mParent = null;
    protected final WindowList<E> mChildren = new WindowList<>();
    protected int mOrientation = -1;
    private final Pools.SynchronizedPool<WindowContainer<E>.ForAllWindowsConsumerWrapper> mConsumerWrapperPool = new Pools.SynchronizedPool<>(3);
    private int mLastLayer = 0;
    private SurfaceControl mLastRelativeToLayer = null;
    private final Point mTmpPos = new Point();
    protected final Point mLastSurfacePosition = new Point();
    private int mTreeWeight = 1;
    private final LinkedList<WindowContainer> mTmpChain1 = new LinkedList<>();
    private final LinkedList<WindowContainer> mTmpChain2 = new LinkedList<>();

    @interface AnimationLayer {
    }

    WindowContainer(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
        this.mPendingTransaction = windowManagerService.mTransactionFactory.make();
        this.mSurfaceAnimator = new SurfaceAnimator(this, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onAnimationFinished();
            }
        }, windowManagerService);
    }

    @Override
    protected final WindowContainer getParent() {
        return this.mParent;
    }

    @Override
    protected int getChildCount() {
        return this.mChildren.size();
    }

    @Override
    protected E getChildAt(int i) {
        return this.mChildren.get(i);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateSurfacePosition();
        scheduleAnimation();
    }

    protected final void setParent(WindowContainer<WindowContainer> windowContainer) {
        this.mParent = windowContainer;
        if (this.mParent != null) {
            onConfigurationChanged(this.mParent.getConfiguration());
            onMergedOverrideConfigurationChanged();
        }
        onParentSet();
    }

    void onParentSet() {
        if (this.mParent == null) {
            return;
        }
        if (this.mSurfaceControl == null) {
            this.mSurfaceControl = makeSurface().build();
            getPendingTransaction().show(this.mSurfaceControl);
            updateSurfacePosition();
        } else {
            reparentSurfaceControl(getPendingTransaction(), this.mParent.mSurfaceControl);
        }
        this.mParent.assignChildLayers();
        scheduleAnimation();
    }

    protected void addChild(E e, Comparator<E> comparator) {
        int i;
        if (e.getParent() != null) {
            throw new IllegalArgumentException("addChild: container=" + e.getName() + " is already a child of container=" + e.getParent().getName() + " can't add to container=" + getName());
        }
        if (comparator != null) {
            int size = this.mChildren.size();
            i = 0;
            while (i < size) {
                if (comparator.compare(e, this.mChildren.get(i)) < 0) {
                    break;
                } else {
                    i++;
                }
            }
            i = -1;
        } else {
            i = -1;
        }
        if (i == -1) {
            this.mChildren.add(e);
        } else {
            this.mChildren.add(i, e);
        }
        onChildAdded(e);
        e.setParent(this);
    }

    void addChild(E e, int i) {
        if (e.getParent() != null) {
            throw new IllegalArgumentException("addChild: container=" + e.getName() + " is already a child of container=" + e.getParent().getName() + " can't add to container=" + getName());
        }
        this.mChildren.add(i, e);
        onChildAdded(e);
        e.setParent(this);
    }

    private void onChildAdded(WindowContainer windowContainer) {
        this.mTreeWeight += windowContainer.mTreeWeight;
        for (WindowContainer parent = getParent(); parent != null; parent = parent.getParent()) {
            parent.mTreeWeight += windowContainer.mTreeWeight;
        }
    }

    void removeChild(E e) {
        if (this.mChildren.remove(e)) {
            onChildRemoved(e);
            e.setParent(null);
            return;
        }
        throw new IllegalArgumentException("removeChild: container=" + e.getName() + " is not a child of container=" + getName());
    }

    private void onChildRemoved(WindowContainer windowContainer) {
        this.mTreeWeight -= windowContainer.mTreeWeight;
        for (WindowContainer parent = getParent(); parent != null; parent = parent.getParent()) {
            parent.mTreeWeight -= windowContainer.mTreeWeight;
        }
    }

    void removeImmediately() {
        while (!this.mChildren.isEmpty()) {
            E ePeekLast = this.mChildren.peekLast();
            ePeekLast.removeImmediately();
            if (this.mChildren.remove(ePeekLast)) {
                onChildRemoved(ePeekLast);
            }
        }
        if (this.mSurfaceControl != null) {
            this.mPendingTransaction.destroy(this.mSurfaceControl);
            if (this.mParent != null) {
                this.mParent.getPendingTransaction().merge(this.mPendingTransaction);
            }
            this.mSurfaceControl = null;
            scheduleAnimation();
        }
        if (this.mParent != null) {
            this.mParent.removeChild(this);
        }
        if (this.mController != null) {
            setController(null);
        }
    }

    int getPrefixOrderIndex() {
        if (this.mParent == null) {
            return 0;
        }
        return this.mParent.getPrefixOrderIndex(this);
    }

    private int getPrefixOrderIndex(WindowContainer windowContainer) {
        E e;
        int prefixOrderIndex = 0;
        for (int i = 0; i < this.mChildren.size() && windowContainer != (e = this.mChildren.get(i)); i++) {
            prefixOrderIndex += e.mTreeWeight;
        }
        if (this.mParent != null) {
            prefixOrderIndex += this.mParent.getPrefixOrderIndex(this);
        }
        return prefixOrderIndex + 1;
    }

    void removeIfPossible() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).removeIfPossible();
        }
    }

    boolean hasChild(E e) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            E e2 = this.mChildren.get(size);
            if (e2 == e || e2.hasChild(e)) {
                return true;
            }
        }
        return false;
    }

    void positionChildAt(int i, E e, boolean z) {
        if (e.getParent() != this) {
            throw new IllegalArgumentException("removeChild: container=" + e.getName() + " is not a child of container=" + getName() + " current parent=" + e.getParent());
        }
        if ((i < 0 && i != Integer.MIN_VALUE) || (i > this.mChildren.size() && i != POSITION_TOP)) {
            throw new IllegalArgumentException("positionAt: invalid position=" + i + ", children number=" + this.mChildren.size());
        }
        if (i < this.mChildren.size() - 1) {
            if (i == 0) {
                i = Integer.MIN_VALUE;
            }
        } else {
            i = POSITION_TOP;
        }
        if (i == Integer.MIN_VALUE) {
            if (this.mChildren.peekFirst() != e) {
                this.mChildren.remove(e);
                this.mChildren.addFirst(e);
            }
            if (z && getParent() != null) {
                getParent().positionChildAt(Integer.MIN_VALUE, this, true);
                return;
            }
            return;
        }
        if (i == POSITION_TOP) {
            if (this.mChildren.peekLast() != e) {
                this.mChildren.remove(e);
                this.mChildren.add(e);
            }
            if (z && getParent() != null) {
                getParent().positionChildAt(POSITION_TOP, this, true);
                return;
            }
            return;
        }
        this.mChildren.remove(e);
        this.mChildren.add(i, e);
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        int iDiffOverrideBounds = diffOverrideBounds(configuration.windowConfiguration.getBounds());
        super.onOverrideConfigurationChanged(configuration);
        if (this.mParent != null) {
            this.mParent.onDescendantOverrideConfigurationChanged();
        }
        if (iDiffOverrideBounds == 0) {
            return;
        }
        if ((iDiffOverrideBounds & 2) == 2) {
            onResize();
        } else {
            onMovedByResize();
        }
    }

    void onDescendantOverrideConfigurationChanged() {
        if (this.mParent != null) {
            this.mParent.onDescendantOverrideConfigurationChanged();
        }
    }

    void onDisplayChanged(DisplayContent displayContent) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).onDisplayChanged(displayContent);
        }
    }

    void setWaitingForDrawnIfResizingChanged() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).setWaitingForDrawnIfResizingChanged();
        }
    }

    void onResize() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).onParentResize();
        }
    }

    void onParentResize() {
        if (hasOverrideBounds()) {
            return;
        }
        onResize();
    }

    void onMovedByResize() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).onMovedByResize();
        }
    }

    void resetDragResizingChangeReported() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).resetDragResizingChangeReported();
        }
    }

    void forceWindowsScaleableInTransaction(boolean z) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).forceWindowsScaleableInTransaction(z);
        }
    }

    boolean isSelfOrChildAnimating() {
        if (isSelfAnimating()) {
            return true;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (this.mChildren.get(size).isSelfOrChildAnimating()) {
                return true;
            }
        }
        return false;
    }

    boolean isAnimating() {
        return isSelfAnimating() || (this.mParent != null && this.mParent.isAnimating());
    }

    boolean isAppAnimating() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (this.mChildren.get(size).isAppAnimating()) {
                return true;
            }
        }
        return false;
    }

    boolean isSelfAnimating() {
        return this.mSurfaceAnimator.isAnimating();
    }

    void sendAppVisibilityToClients() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).sendAppVisibilityToClients();
        }
    }

    boolean hasContentToDisplay() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (this.mChildren.get(size).hasContentToDisplay()) {
                return true;
            }
        }
        return false;
    }

    boolean isVisible() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (this.mChildren.get(size).isVisible()) {
                return true;
            }
        }
        return false;
    }

    boolean isOnTop() {
        return getParent().getTopChild() == this && getParent().isOnTop();
    }

    E getTopChild() {
        return this.mChildren.peekLast();
    }

    boolean checkCompleteDeferredRemoval() {
        boolean zCheckCompleteDeferredRemoval = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            zCheckCompleteDeferredRemoval |= this.mChildren.get(size).checkCompleteDeferredRemoval();
        }
        return zCheckCompleteDeferredRemoval;
    }

    void checkAppWindowsReadyToShow() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).checkAppWindowsReadyToShow();
        }
    }

    void onAppTransitionDone() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).onAppTransitionDone();
        }
    }

    void setOrientation(int i) {
        this.mOrientation = i;
    }

    int getOrientation() {
        return getOrientation(this.mOrientation);
    }

    int getOrientation(int i) {
        if (!fillsParent()) {
            return -2;
        }
        if (this.mOrientation != -2 && this.mOrientation != -1) {
            return this.mOrientation;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            E e = this.mChildren.get(size);
            int orientation = e.getOrientation(i == 3 ? 3 : -2);
            if (orientation == 3) {
                i = orientation;
            } else if (orientation != -2 && (e.fillsParent() || orientation != -1)) {
                return orientation;
            }
        }
        return i;
    }

    boolean fillsParent() {
        return false;
    }

    void switchUser() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).switchUser();
        }
    }

    boolean forAllWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        if (z) {
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                if (this.mChildren.get(size).forAllWindows(toBooleanFunction, z)) {
                    return true;
                }
            }
        } else {
            int size2 = this.mChildren.size();
            for (int i = 0; i < size2; i++) {
                if (this.mChildren.get(i).forAllWindows(toBooleanFunction, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    void forAllWindows(Consumer<WindowState> consumer, boolean z) {
        WindowContainer<E>.ForAllWindowsConsumerWrapper forAllWindowsConsumerWrapperObtainConsumerWrapper = obtainConsumerWrapper(consumer);
        forAllWindows(forAllWindowsConsumerWrapperObtainConsumerWrapper, z);
        forAllWindowsConsumerWrapperObtainConsumerWrapper.release();
    }

    void forAllTasks(Consumer<Task> consumer) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            this.mChildren.get(size).forAllTasks(consumer);
        }
    }

    WindowState getWindow(Predicate<WindowState> predicate) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState window = this.mChildren.get(size).getWindow(predicate);
            if (window != null) {
                return window;
            }
        }
        return null;
    }

    @Override
    public int compareTo(WindowContainer windowContainer) {
        if (this == windowContainer) {
            return 0;
        }
        if (this.mParent != null && this.mParent == windowContainer.mParent) {
            WindowList<E> windowList = this.mParent.mChildren;
            return windowList.indexOf(this) > windowList.indexOf(windowContainer) ? 1 : -1;
        }
        LinkedList<WindowContainer> linkedList = this.mTmpChain1;
        LinkedList<WindowContainer> linkedList2 = this.mTmpChain2;
        try {
            getParents(linkedList);
            windowContainer.getParents(linkedList2);
            WindowContainer windowContainerRemoveLast = null;
            WindowContainer windowContainerPeekLast = linkedList.peekLast();
            for (WindowContainer windowContainerPeekLast2 = linkedList2.peekLast(); windowContainerPeekLast != null && windowContainerPeekLast2 != null && windowContainerPeekLast == windowContainerPeekLast2; windowContainerPeekLast2 = linkedList2.peekLast()) {
                windowContainerRemoveLast = linkedList.removeLast();
                linkedList2.removeLast();
                windowContainerPeekLast = linkedList.peekLast();
            }
            if (windowContainerRemoveLast != null) {
                if (windowContainerRemoveLast == this) {
                    return -1;
                }
                if (windowContainerRemoveLast == windowContainer) {
                    return 1;
                }
                WindowList<E> windowList2 = windowContainerRemoveLast.mChildren;
                return windowList2.indexOf(linkedList.peekLast()) > windowList2.indexOf(linkedList2.peekLast()) ? 1 : -1;
            }
            throw new IllegalArgumentException("No in the same hierarchy this=" + linkedList + " other=" + linkedList2);
        } finally {
            this.mTmpChain1.clear();
            this.mTmpChain2.clear();
        }
    }

    private void getParents(LinkedList<WindowContainer> linkedList) {
        linkedList.clear();
        WindowContainer<WindowContainer> windowContainer = this;
        do {
            linkedList.addLast(windowContainer);
            windowContainer = windowContainer.mParent;
        } while (windowContainer != null);
    }

    WindowContainerController getController() {
        return this.mController;
    }

    void setController(WindowContainerController windowContainerController) {
        if (this.mController != null && windowContainerController != null) {
            throw new IllegalArgumentException("Can't set controller=" + this.mController + " for container=" + this + " Already set to=" + this.mController);
        }
        if (windowContainerController != null) {
            windowContainerController.setContainer(this);
        } else if (this.mController != null) {
            this.mController.setContainer(null);
        }
        this.mController = windowContainerController;
    }

    SurfaceControl.Builder makeSurface() {
        return getParent().makeChildSurface(this);
    }

    SurfaceControl.Builder makeChildSurface(WindowContainer windowContainer) {
        return getParent().makeChildSurface(windowContainer).setParent(this.mSurfaceControl);
    }

    public SurfaceControl getParentSurfaceControl() {
        WindowContainer parent = getParent();
        if (parent == null) {
            return null;
        }
        return parent.getSurfaceControl();
    }

    boolean shouldMagnify() {
        if (this.mSurfaceControl == null) {
            return false;
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (!this.mChildren.get(i).shouldMagnify()) {
                return false;
            }
        }
        return true;
    }

    SurfaceSession getSession() {
        if (getParent() != null) {
            return getParent().getSession();
        }
        return null;
    }

    void assignLayer(SurfaceControl.Transaction transaction, int i) {
        boolean z = (i == this.mLastLayer && this.mLastRelativeToLayer == null) ? false : true;
        if (this.mSurfaceControl != null && z) {
            setLayer(transaction, i);
            this.mLastLayer = i;
            this.mLastRelativeToLayer = null;
        }
    }

    void assignRelativeLayer(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, int i) {
        boolean z = (i == this.mLastLayer && this.mLastRelativeToLayer == surfaceControl) ? false : true;
        if (this.mSurfaceControl != null && z) {
            setRelativeLayer(transaction, surfaceControl, i);
            this.mLastLayer = i;
            this.mLastRelativeToLayer = surfaceControl;
        }
    }

    protected void setLayer(SurfaceControl.Transaction transaction, int i) {
        this.mSurfaceAnimator.setLayer(transaction, i);
    }

    protected void setRelativeLayer(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, int i) {
        this.mSurfaceAnimator.setRelativeLayer(transaction, surfaceControl, i);
    }

    protected void reparentSurfaceControl(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        this.mSurfaceAnimator.reparent(transaction, surfaceControl);
    }

    void assignChildLayers(SurfaceControl.Transaction transaction) {
        int i = 0;
        for (int i2 = 0; i2 < this.mChildren.size(); i2++) {
            E e = this.mChildren.get(i2);
            e.assignChildLayers(transaction);
            if (!e.needsZBoost()) {
                e.assignLayer(transaction, i);
                i++;
            }
        }
        for (int i3 = 0; i3 < this.mChildren.size(); i3++) {
            E e2 = this.mChildren.get(i3);
            if (e2.needsZBoost()) {
                e2.assignLayer(transaction, i);
                i++;
            }
        }
    }

    void assignChildLayers() {
        assignChildLayers(getPendingTransaction());
        scheduleAnimation();
    }

    boolean needsZBoost() {
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (this.mChildren.get(i).needsZBoost()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        protoOutputStream.write(1120986464258L, this.mOrientation);
        protoOutputStream.write(1133871366147L, isVisible());
        this.mSurfaceAnimator.writeToProto(protoOutputStream, 1146756268036L);
        protoOutputStream.end(jStart);
    }

    private WindowContainer<E>.ForAllWindowsConsumerWrapper obtainConsumerWrapper(Consumer<WindowState> consumer) {
        WindowContainer<E>.ForAllWindowsConsumerWrapper forAllWindowsConsumerWrapper = (ForAllWindowsConsumerWrapper) this.mConsumerWrapperPool.acquire();
        if (forAllWindowsConsumerWrapper == null) {
            forAllWindowsConsumerWrapper = new ForAllWindowsConsumerWrapper();
        }
        forAllWindowsConsumerWrapper.setConsumer(consumer);
        return forAllWindowsConsumerWrapper;
    }

    private final class ForAllWindowsConsumerWrapper implements ToBooleanFunction<WindowState> {
        private Consumer<WindowState> mConsumer;

        private ForAllWindowsConsumerWrapper() {
        }

        void setConsumer(Consumer<WindowState> consumer) {
            this.mConsumer = consumer;
        }

        public boolean apply(WindowState windowState) {
            this.mConsumer.accept(windowState);
            return false;
        }

        void release() {
            this.mConsumer = null;
            WindowContainer.this.mConsumerWrapperPool.release(this);
        }
    }

    void applyMagnificationSpec(SurfaceControl.Transaction transaction, MagnificationSpec magnificationSpec) {
        if (shouldMagnify()) {
            transaction.setMatrix(this.mSurfaceControl, magnificationSpec.scale, 0.0f, 0.0f, magnificationSpec.scale).setPosition(this.mSurfaceControl, magnificationSpec.offsetX, magnificationSpec.offsetY);
            return;
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            this.mChildren.get(i).applyMagnificationSpec(transaction, magnificationSpec);
        }
    }

    void prepareSurfaces() {
        SurfaceControl.mergeToGlobalTransaction(getPendingTransaction());
        this.mCommittedReparentToAnimationLeash = this.mSurfaceAnimator.hasLeash();
        for (int i = 0; i < this.mChildren.size(); i++) {
            this.mChildren.get(i).prepareSurfaces();
        }
    }

    boolean hasCommittedReparentToAnimationLeash() {
        return this.mCommittedReparentToAnimationLeash;
    }

    void scheduleAnimation() {
        if (this.mParent != null) {
            this.mParent.scheduleAnimation();
        }
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public SurfaceControl.Transaction getPendingTransaction() {
        return this.mPendingTransaction;
    }

    void startAnimation(SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "Starting animation on " + this + ": " + animationAdapter);
        }
        this.mSurfaceAnimator.startAnimation(transaction, animationAdapter, z);
    }

    void transferAnimation(WindowContainer windowContainer) {
        this.mSurfaceAnimator.transferAnimation(windowContainer.mSurfaceAnimator);
    }

    void cancelAnimation() {
        this.mSurfaceAnimator.cancelAnimation();
    }

    public SurfaceControl.Builder makeAnimationLeash() {
        return makeSurface();
    }

    public SurfaceControl getAnimationLeashParent() {
        return getParentSurfaceControl();
    }

    SurfaceControl getAppAnimationLayer(@AnimationLayer int i) {
        WindowContainer parent = getParent();
        if (parent != null) {
            return parent.getAppAnimationLayer(i);
        }
        return null;
    }

    public void commitPendingTransaction() {
        scheduleAnimation();
    }

    void reassignLayer(SurfaceControl.Transaction transaction) {
        WindowContainer parent = getParent();
        if (parent != null) {
            parent.assignChildLayers(transaction);
        }
    }

    public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        this.mLastLayer = -1;
        reassignLayer(transaction);
    }

    public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        this.mLastLayer = -1;
        reassignLayer(transaction);
    }

    protected void onAnimationFinished() {
    }

    AnimationAdapter getAnimation() {
        return this.mSurfaceAnimator.getAnimation();
    }

    void startDelayingAnimationStart() {
        this.mSurfaceAnimator.startDelayingAnimationStart();
    }

    void endDelayingAnimationStart() {
        this.mSurfaceAnimator.endDelayingAnimationStart();
    }

    public int getSurfaceWidth() {
        return this.mSurfaceControl.getWidth();
    }

    public int getSurfaceHeight() {
        return this.mSurfaceControl.getHeight();
    }

    void dump(PrintWriter printWriter, String str, boolean z) {
        if (this.mSurfaceAnimator.isAnimating()) {
            printWriter.print(str);
            printWriter.println("ContainerAnimator:");
            this.mSurfaceAnimator.dump(printWriter, str + "  ");
        }
    }

    void updateSurfacePosition() {
        if (this.mSurfaceControl == null) {
            return;
        }
        getRelativePosition(this.mTmpPos);
        if (this.mTmpPos.equals(this.mLastSurfacePosition)) {
            return;
        }
        getPendingTransaction().setPosition(this.mSurfaceControl, this.mTmpPos.x, this.mTmpPos.y);
        this.mLastSurfacePosition.set(this.mTmpPos.x, this.mTmpPos.y);
    }

    void getRelativePosition(Point point) {
        Rect bounds = getBounds();
        point.set(bounds.left, bounds.top);
        WindowContainer parent = getParent();
        if (parent != null) {
            Rect bounds2 = parent.getBounds();
            point.offset(-bounds2.left, -bounds2.top);
        }
    }

    Dimmer getDimmer() {
        if (this.mParent == null) {
            return null;
        }
        return this.mParent.getDimmer();
    }
}
