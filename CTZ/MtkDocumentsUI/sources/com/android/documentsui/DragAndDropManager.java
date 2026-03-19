package com.android.documentsui;

import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.KeyEvent;
import android.view.View;
import com.android.documentsui.MenuManager;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.clipping.DocumentClipper;
import com.android.documentsui.dirlist.IconHelper;
import com.android.documentsui.services.FileOperations;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public interface DragAndDropManager {
    boolean canSpringOpen(RootInfo rootInfo, DocumentInfo documentInfo);

    void dragEnded();

    boolean drop(ClipData clipData, Object obj, DocumentStack documentStack, FileOperations.Callback callback);

    boolean drop(ClipData clipData, Object obj, RootInfo rootInfo, ActionHandler actionHandler, FileOperations.Callback callback);

    void onKeyEvent(KeyEvent keyEvent);

    void resetState(View view);

    void startDrag(View view, List<DocumentInfo> list, RootInfo rootInfo, List<Uri> list2, MenuManager.SelectionDetails selectionDetails, IconHelper iconHelper, DocumentInfo documentInfo);

    int updateState(View view, RootInfo rootInfo, DocumentInfo documentInfo);

    void updateStateToNotAllowed(View view);

    static DragAndDropManager create(Context context, DocumentClipper documentClipper) {
        return new RuntimeDragAndDropManager(context, documentClipper);
    }

    public static class RuntimeDragAndDropManager implements DragAndDropManager {
        static final boolean $assertionsDisabled = false;
        private ClipData mClipData;
        private final DocumentClipper mClipper;
        private final Context mContext;
        private final Drawable mDefaultShadowIcon;
        private DocumentInfo mDestDoc;
        private RootInfo mDestRoot;
        private List<Uri> mInvalidDest;
        private boolean mIsCtrlPressed;
        private boolean mMustBeCopied;
        private final DragShadowBuilder mShadowBuilder;
        private int mState;
        private View mView;

        private RuntimeDragAndDropManager(Context context, DocumentClipper documentClipper) {
            this(context.getApplicationContext(), documentClipper, new DragShadowBuilder(context), context.getDrawable(R.drawable.ic_doc_generic));
        }

        RuntimeDragAndDropManager(Context context, DocumentClipper documentClipper, DragShadowBuilder dragShadowBuilder, Drawable drawable) {
            this.mState = 0;
            this.mContext = context;
            this.mClipper = documentClipper;
            this.mShadowBuilder = dragShadowBuilder;
            this.mDefaultShadowIcon = drawable;
        }

        @Override
        public void onKeyEvent(KeyEvent keyEvent) {
            switch (keyEvent.getKeyCode()) {
                case 113:
                case 114:
                    adjustCtrlKeyCount(keyEvent);
                    break;
            }
        }

        private void adjustCtrlKeyCount(KeyEvent keyEvent) {
            this.mIsCtrlPressed = keyEvent.isCtrlPressed();
            if (this.mView != null) {
                if (this.mState == 3 || this.mState == 2) {
                    updateState(this.mView, this.mDestRoot, this.mDestDoc);
                }
            }
        }

        @Override
        public void startDrag(View view, List<DocumentInfo> list, RootInfo rootInfo, List<Uri> list2, MenuManager.SelectionDetails selectionDetails, IconHelper iconHelper, DocumentInfo documentInfo) {
            this.mView = view;
            this.mInvalidDest = list2;
            this.mMustBeCopied = !selectionDetails.canDelete();
            ArrayList arrayList = new ArrayList(list.size());
            Iterator<DocumentInfo> it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().derivedUri);
            }
            this.mClipData = documentInfo == null ? this.mClipper.getClipDataForDocuments(arrayList, -1) : this.mClipper.getClipDataForDocuments(arrayList, -1, documentInfo);
            this.mClipData.getDescription().getExtras().putString("dragAndDropMgr:srcRoot", rootInfo.getUri().toString());
            updateShadow(list, iconHelper);
            int i = 768;
            if (!selectionDetails.containsFilesInArchive()) {
                i = 771;
            }
            startDragAndDrop(view, this.mClipData, this.mShadowBuilder, this, i);
        }

        private void updateShadow(List<DocumentInfo> list, IconHelper iconHelper) {
            String quantityString;
            Drawable documentIcon;
            int size = list.size();
            if (size != 1) {
                quantityString = this.mContext.getResources().getQuantityString(R.plurals.elements_dragged, size, Integer.valueOf(size));
                documentIcon = this.mDefaultShadowIcon;
            } else {
                DocumentInfo documentInfo = list.get(0);
                quantityString = documentInfo.displayName;
                documentIcon = iconHelper.getDocumentIcon(this.mContext, documentInfo);
            }
            this.mShadowBuilder.updateTitle(quantityString);
            this.mShadowBuilder.updateIcon(documentIcon);
            this.mShadowBuilder.onStateUpdated(0);
        }

        void startDragAndDrop(View view, ClipData clipData, DragShadowBuilder dragShadowBuilder, Object obj, int i) {
            view.startDragAndDrop(clipData, dragShadowBuilder, obj, i);
        }

        @Override
        public boolean canSpringOpen(RootInfo rootInfo, DocumentInfo documentInfo) {
            return isValidDestination(rootInfo, documentInfo.derivedUri);
        }

        @Override
        public void updateStateToNotAllowed(View view) {
            this.mView = view;
            updateState(1);
        }

        @Override
        public int updateState(View view, RootInfo rootInfo, DocumentInfo documentInfo) {
            int i;
            this.mView = view;
            this.mDestRoot = rootInfo;
            this.mDestDoc = documentInfo;
            if (!rootInfo.supportsCreate()) {
                updateState(1);
                return 1;
            }
            if (documentInfo == null) {
                updateState(0);
                return 0;
            }
            if (!documentInfo.isCreateSupported() || this.mInvalidDest.contains(documentInfo.derivedUri)) {
                updateState(1);
                return 1;
            }
            int iCalculateOpType = calculateOpType(this.mClipData, rootInfo);
            if (iCalculateOpType == 1) {
                i = 3;
            } else if (iCalculateOpType == 4) {
                i = 2;
            } else {
                throw new IllegalStateException("Unknown opType: " + iCalculateOpType);
            }
            updateState(i);
            return i;
        }

        @Override
        public void resetState(View view) {
            this.mView = view;
            updateState(0);
        }

        private void updateState(int i) {
            this.mState = i;
            this.mShadowBuilder.onStateUpdated(i);
            updateDragShadow(this.mView);
        }

        void updateDragShadow(View view) {
            view.updateDragShadow(this.mShadowBuilder);
        }

        @Override
        public boolean drop(final ClipData clipData, final Object obj, final RootInfo rootInfo, ActionHandler actionHandler, final FileOperations.Callback callback) {
            if (!isValidDestination(rootInfo, DocumentsContract.buildDocumentUri(rootInfo.authority, rootInfo.documentId))) {
                return false;
            }
            final int iCalculateOpType = calculateOpType(clipData, rootInfo);
            actionHandler.getRootDocument(rootInfo, -1, new Consumer() {
                @Override
                public final void accept(Object obj2) {
                    this.f$0.dropOnRootDocument(clipData, obj, rootInfo, (DocumentInfo) obj2, iCalculateOpType, callback);
                }
            });
            return true;
        }

        private void dropOnRootDocument(ClipData clipData, Object obj, RootInfo rootInfo, DocumentInfo documentInfo, int i, FileOperations.Callback callback) {
            if (documentInfo == null) {
                callback.onOperationResult(2, i, 0);
            } else {
                dropChecked(clipData, obj, new DocumentStack(rootInfo, documentInfo), i, callback);
            }
        }

        @Override
        public boolean drop(ClipData clipData, Object obj, DocumentStack documentStack, FileOperations.Callback callback) {
            if (!canCopyTo(documentStack)) {
                return false;
            }
            dropChecked(clipData, obj, documentStack, calculateOpType(clipData, documentStack.getRoot()), callback);
            return true;
        }

        private void dropChecked(ClipData clipData, Object obj, DocumentStack documentStack, int i, FileOperations.Callback callback) {
            Metrics.logUserAction(this.mContext, obj == null ? 25 : 24);
            this.mClipper.copyFromClipData(documentStack, clipData, i, callback);
        }

        @Override
        public void dragEnded() {
            this.mView = null;
            this.mInvalidDest = null;
            this.mClipData = null;
            this.mDestDoc = null;
            this.mDestRoot = null;
            this.mMustBeCopied = false;
        }

        private int calculateOpType(ClipData clipData, RootInfo rootInfo) {
            if (this.mMustBeCopied) {
                return 1;
            }
            return clipData.getDescription().getExtras().getString("dragAndDropMgr:srcRoot").equals(rootInfo.getUri().toString()) ? this.mIsCtrlPressed ? 1 : 4 : this.mIsCtrlPressed ? 4 : 1;
        }

        private boolean canCopyTo(DocumentStack documentStack) {
            return isValidDestination(documentStack.getRoot(), documentStack.peek().derivedUri);
        }

        private boolean isValidDestination(RootInfo rootInfo, Uri uri) {
            return rootInfo.supportsCreate() && !this.mInvalidDest.contains(uri);
        }
    }
}
