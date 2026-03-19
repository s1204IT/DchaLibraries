package com.android.documentsui.dirlist;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.documentsui.DragAndDropManager;
import com.android.documentsui.MenuManager;
import com.android.documentsui.Model;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Events;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MutableSelection;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.selection.SelectionHelper;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

interface DragStartListener {
    public static final DragStartListener DUMMY = new DragStartListener() {
        @Override
        public boolean onMouseDragEvent(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onTouchDragEvent(MotionEvent motionEvent) {
            return false;
        }
    };

    @FunctionalInterface
    public interface ViewFinder {
        View findView(float f, float f2);
    }

    boolean onMouseDragEvent(MotionEvent motionEvent);

    boolean onTouchDragEvent(MotionEvent motionEvent);

    public static class RuntimeDragStartListener implements DragStartListener {
        private static String TAG = "DragStartListener";
        private final ItemDetailsLookup mDetailsLookup;
        private final Function<Selection, List<DocumentInfo>> mDocsConverter;
        private final DragAndDropManager mDragAndDropManager;
        private final IconHelper mIconHelper;
        private final Function<View, String> mIdFinder;
        private final MenuManager.SelectionDetails mSelectionDetails;
        private final SelectionHelper mSelectionMgr;
        private final State mState;
        private final ViewFinder mViewFinder;

        public RuntimeDragStartListener(IconHelper iconHelper, State state, ItemDetailsLookup itemDetailsLookup, SelectionHelper selectionHelper, MenuManager.SelectionDetails selectionDetails, ViewFinder viewFinder, Function<View, String> function, Function<Selection, List<DocumentInfo>> function2, DragAndDropManager dragAndDropManager) {
            this.mIconHelper = iconHelper;
            this.mState = state;
            this.mDetailsLookup = itemDetailsLookup;
            this.mSelectionMgr = selectionHelper;
            this.mSelectionDetails = selectionDetails;
            this.mViewFinder = viewFinder;
            this.mIdFinder = function;
            this.mDocsConverter = function2;
            this.mDragAndDropManager = dragAndDropManager;
        }

        @Override
        public final boolean onMouseDragEvent(MotionEvent motionEvent) {
            Preconditions.checkArgument(Events.isMouseDragEvent(motionEvent));
            Preconditions.checkArgument(this.mDetailsLookup.inItemDragRegion(motionEvent));
            return startDrag(this.mViewFinder.findView(motionEvent.getX(), motionEvent.getY()), motionEvent);
        }

        @Override
        public final boolean onTouchDragEvent(MotionEvent motionEvent) {
            return startDrag(this.mViewFinder.findView(motionEvent.getX(), motionEvent.getY()), motionEvent);
        }

        private boolean startDrag(View view, MotionEvent motionEvent) {
            if (view == null) {
                if (SharedMinimal.DEBUG) {
                    Log.d(TAG, "Ignoring drag event, null view.");
                }
                return false;
            }
            String strApply = this.mIdFinder.apply(view);
            if (strApply == null) {
                if (SharedMinimal.DEBUG) {
                    Log.d(TAG, "Ignoring drag on view not represented in model.");
                }
                return false;
            }
            List<DocumentInfo> listApply = this.mDocsConverter.apply(getSelectionToBeCopied(strApply, motionEvent));
            ArrayList arrayList = new ArrayList(listApply.size() + 1);
            Iterator<DocumentInfo> it = listApply.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().derivedUri);
            }
            DocumentInfo documentInfoPeek = this.mState.stack.peek();
            if (documentInfoPeek != null) {
                arrayList.add(documentInfoPeek.derivedUri);
            }
            this.mDragAndDropManager.startDrag(view, listApply, this.mState.stack.getRoot(), arrayList, this.mSelectionDetails, this.mIconHelper, documentInfoPeek);
            return true;
        }

        MutableSelection getSelectionToBeCopied(String str, MotionEvent motionEvent) {
            MutableSelection mutableSelection = new MutableSelection();
            if (Events.isCtrlKeyPressed(motionEvent) && this.mSelectionMgr.hasSelection() && !this.mSelectionMgr.isSelected(str)) {
                this.mSelectionMgr.select(str);
            }
            if (this.mSelectionMgr.isSelected(str)) {
                this.mSelectionMgr.copySelection(mutableSelection);
            } else {
                mutableSelection.add(str);
                this.mSelectionMgr.clearSelection();
            }
            return mutableSelection;
        }
    }

    static DragStartListener create(IconHelper iconHelper, final Model model, SelectionHelper selectionHelper, MenuManager.SelectionDetails selectionDetails, State state, ItemDetailsLookup itemDetailsLookup, Function<View, String> function, ViewFinder viewFinder, DragAndDropManager dragAndDropManager) {
        Objects.requireNonNull(model);
        return new RuntimeDragStartListener(iconHelper, state, itemDetailsLookup, selectionHelper, selectionDetails, viewFinder, function, new Function() {
            @Override
            public final Object apply(Object obj) {
                return model.getDocuments((Selection) obj);
            }
        }, dragAndDropManager);
    }
}
