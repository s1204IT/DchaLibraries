package android.view;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.ViewOverlay;
import android.widget.FrameLayout;
import java.util.ArrayList;

public class GhostView extends View {
    private boolean mBeingMoved;
    private int mReferences;
    private final View mView;

    private GhostView(View view) {
        super(view.getContext());
        this.mView = view;
        this.mView.mGhostView = this;
        ViewGroup parent = (ViewGroup) this.mView.getParent();
        setGhostedVisibility(4);
        parent.mRecreateDisplayList = true;
        parent.getDisplayList();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas instanceof HardwareCanvas) {
            HardwareCanvas hwCanvas = (HardwareCanvas) canvas;
            this.mView.mRecreateDisplayList = true;
            RenderNode renderNode = this.mView.getDisplayList();
            if (renderNode.isValid()) {
                hwCanvas.insertReorderBarrier();
                hwCanvas.drawRenderNode(renderNode);
                hwCanvas.insertInorderBarrier();
            }
        }
    }

    public void setMatrix(Matrix matrix) {
        this.mRenderNode.setAnimationMatrix(matrix);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mView.mGhostView == this) {
            int inverseVisibility = visibility == 0 ? 4 : 0;
            setGhostedVisibility(inverseVisibility);
        }
    }

    private void setGhostedVisibility(int visibility) {
        this.mView.mViewFlags = (this.mView.mViewFlags & (-13)) | visibility;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!this.mBeingMoved) {
            setGhostedVisibility(0);
            this.mView.mGhostView = null;
            ViewGroup parent = (ViewGroup) this.mView.getParent();
            if (parent != null) {
                parent.mRecreateDisplayList = true;
                parent.getDisplayList();
            }
        }
    }

    public static void calculateMatrix(View view, ViewGroup host, Matrix matrix) {
        ViewGroup parent = (ViewGroup) view.getParent();
        matrix.reset();
        parent.transformMatrixToGlobal(matrix);
        matrix.preTranslate(-parent.getScrollX(), -parent.getScrollY());
        host.transformMatrixToLocal(matrix);
    }

    public static GhostView addGhost(View view, ViewGroup viewGroup, Matrix matrix) {
        if (!(view.getParent() instanceof ViewGroup)) {
            throw new IllegalArgumentException("Ghosted views must be parented by a ViewGroup");
        }
        ViewGroupOverlay overlay = viewGroup.getOverlay();
        ViewOverlay.OverlayViewGroup overlayViewGroup = overlay.mOverlayViewGroup;
        GhostView ghostView = view.mGhostView;
        int previousRefCount = 0;
        if (ghostView != null) {
            View oldParent = (View) ghostView.getParent();
            ViewGroup oldGrandParent = (ViewGroup) oldParent.getParent();
            if (oldGrandParent != overlayViewGroup) {
                previousRefCount = ghostView.mReferences;
                oldGrandParent.removeView(oldParent);
                ghostView = null;
            }
        }
        if (ghostView == null) {
            if (matrix == null) {
                matrix = new Matrix();
                calculateMatrix(view, viewGroup, matrix);
            }
            ghostView = new GhostView(view);
            ghostView.setMatrix(matrix);
            FrameLayout parent = new FrameLayout(view.getContext());
            parent.setClipChildren(false);
            copySize(viewGroup, parent);
            copySize(viewGroup, ghostView);
            parent.addView(ghostView);
            ArrayList<View> tempViews = new ArrayList<>();
            int firstGhost = moveGhostViewsToTop(overlay.mOverlayViewGroup, tempViews);
            insertIntoOverlay(overlay.mOverlayViewGroup, parent, ghostView, tempViews, firstGhost);
            ghostView.mReferences = previousRefCount;
        } else if (matrix != null) {
            ghostView.setMatrix(matrix);
        }
        ghostView.mReferences++;
        return ghostView;
    }

    public static GhostView addGhost(View view, ViewGroup viewGroup) {
        return addGhost(view, viewGroup, null);
    }

    public static void removeGhost(View view) {
        GhostView ghostView = view.mGhostView;
        if (ghostView != null) {
            ghostView.mReferences--;
            if (ghostView.mReferences == 0) {
                ViewGroup parent = (ViewGroup) ghostView.getParent();
                ViewGroup grandParent = (ViewGroup) parent.getParent();
                grandParent.removeView(parent);
            }
        }
    }

    public static GhostView getGhost(View view) {
        return view.mGhostView;
    }

    private static void copySize(View from, View to) {
        to.setLeft(0);
        to.setTop(0);
        to.setRight(from.getWidth());
        to.setBottom(from.getHeight());
    }

    private static int moveGhostViewsToTop(ViewGroup viewGroup, ArrayList<View> tempViews) {
        int numChildren = viewGroup.getChildCount();
        if (numChildren == 0) {
            return -1;
        }
        if (isGhostWrapper(viewGroup.getChildAt(numChildren - 1))) {
            int firstGhost = numChildren - 1;
            for (int i = numChildren - 2; i >= 0 && isGhostWrapper(viewGroup.getChildAt(i)); i--) {
                firstGhost = i;
            }
            return firstGhost;
        }
        for (int i2 = numChildren - 2; i2 >= 0; i2--) {
            View child = viewGroup.getChildAt(i2);
            if (isGhostWrapper(child)) {
                tempViews.add(child);
                GhostView ghostView = (GhostView) ((ViewGroup) child).getChildAt(0);
                ghostView.mBeingMoved = true;
                viewGroup.removeViewAt(i2);
                ghostView.mBeingMoved = false;
            }
        }
        if (tempViews.isEmpty()) {
            return -1;
        }
        int firstGhost2 = viewGroup.getChildCount();
        for (int i3 = tempViews.size() - 1; i3 >= 0; i3--) {
            viewGroup.addView(tempViews.get(i3));
        }
        tempViews.clear();
        return firstGhost2;
    }

    private static void insertIntoOverlay(ViewGroup viewGroup, ViewGroup wrapper, GhostView ghostView, ArrayList<View> tempParents, int firstGhost) {
        if (firstGhost == -1) {
            viewGroup.addView(wrapper);
            return;
        }
        ArrayList<View> viewParents = new ArrayList<>();
        getParents(ghostView.mView, viewParents);
        int index = getInsertIndex(viewGroup, viewParents, tempParents, firstGhost);
        if (index < 0 || index >= viewGroup.getChildCount()) {
            viewGroup.addView(wrapper);
        } else {
            viewGroup.addView(wrapper, index);
        }
    }

    private static int getInsertIndex(ViewGroup overlayViewGroup, ArrayList<View> viewParents, ArrayList<View> tempParents, int firstGhost) {
        int low = firstGhost;
        int high = overlayViewGroup.getChildCount() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            ViewGroup wrapper = (ViewGroup) overlayViewGroup.getChildAt(mid);
            GhostView midView = (GhostView) wrapper.getChildAt(0);
            getParents(midView.mView, tempParents);
            if (isOnTop(viewParents, tempParents)) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
            tempParents.clear();
        }
        return low;
    }

    private static boolean isGhostWrapper(View view) {
        if (!(view instanceof FrameLayout)) {
            return false;
        }
        FrameLayout frameLayout = (FrameLayout) view;
        if (frameLayout.getChildCount() != 1) {
            return false;
        }
        View child = frameLayout.getChildAt(0);
        return child instanceof GhostView;
    }

    private static boolean isOnTop(ArrayList<View> viewParents, ArrayList<View> comparedWith) {
        if (viewParents.isEmpty() || comparedWith.isEmpty() || viewParents.get(0) != comparedWith.get(0)) {
            return true;
        }
        int depth = Math.min(viewParents.size(), comparedWith.size());
        for (int i = 1; i < depth; i++) {
            View viewParent = viewParents.get(i);
            View comparedWithParent = comparedWith.get(i);
            if (viewParent != comparedWithParent) {
                return isOnTop(viewParent, comparedWithParent);
            }
        }
        return comparedWith.size() == depth;
    }

    private static void getParents(View view, ArrayList<View> parents) {
        Object parent = view.getParent();
        if (parent != null && (parent instanceof ViewGroup)) {
            getParents((View) parent, parents);
        }
        parents.add(view);
    }

    private static boolean isOnTop(View view, View comparedWith) {
        ViewGroup parent = (ViewGroup) view.getParent();
        int childrenCount = parent.getChildCount();
        ArrayList<View> preorderedList = parent.buildOrderedChildList();
        boolean customOrder = preorderedList == null && parent.isChildrenDrawingOrderEnabled();
        boolean isOnTop = true;
        int i = 0;
        while (true) {
            if (i >= childrenCount) {
                break;
            }
            int childIndex = customOrder ? parent.getChildDrawingOrder(childrenCount, i) : i;
            View child = preorderedList == null ? parent.getChildAt(childIndex) : preorderedList.get(childIndex);
            if (child == view) {
                isOnTop = false;
                break;
            }
            if (child != comparedWith) {
                i++;
            } else {
                isOnTop = true;
                break;
            }
        }
        if (preorderedList != null) {
            preorderedList.clear();
        }
        return isOnTop;
    }
}
