package com.android.documentsui;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.android.documentsui.Model;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.EventListener;
import com.android.documentsui.base.Events;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Procedure;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.dirlist.DocumentHolder;
import com.android.documentsui.dirlist.DocumentsAdapter;
import com.android.documentsui.dirlist.FocusHandler;
import com.android.documentsui.selection.SelectionHelper;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class FocusManager implements FocusHandler {
    static final boolean $assertionsDisabled = false;
    private final DrawerController mDrawer;
    private final Features mFeatures;
    private boolean mNavDrawerHasFocus;
    private final Procedure mRootsFocuser;
    private final ContentScope mScope = new ContentScope();
    private final TitleSearchHelper mSearchHelper;
    private final SelectionHelper mSelectionMgr;

    private interface FocusCallback {
        void onFocus(View view);
    }

    public FocusManager(Features features, SelectionHelper selectionHelper, DrawerController drawerController, Procedure procedure, int i) {
        this.mFeatures = (Features) Preconditions.checkNotNull(features);
        this.mSelectionMgr = selectionHelper;
        this.mDrawer = drawerController;
        this.mRootsFocuser = procedure;
        this.mSearchHelper = new TitleSearchHelper(i);
    }

    @Override
    public boolean advanceFocusArea() {
        boolean zRun;
        if (this.mNavDrawerHasFocus) {
            this.mDrawer.setOpen(false);
            zRun = focusDirectoryList();
        } else {
            this.mDrawer.setOpen(true);
            zRun = this.mRootsFocuser.run();
        }
        if (!zRun) {
            return false;
        }
        this.mNavDrawerHasFocus = !this.mNavDrawerHasFocus;
        return true;
    }

    @Override
    public boolean handleKey(DocumentHolder documentHolder, int i, KeyEvent keyEvent) {
        if (this.mSearchHelper.handleKey(documentHolder, i, keyEvent)) {
            return true;
        }
        if (Events.isNavigationKeyCode(i)) {
            int iFindTargetPosition = findTargetPosition(documentHolder.itemView, i, keyEvent);
            if (iFindTargetPosition != -1) {
                focusItem(iFindTargetPosition);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (z && view.getParent() == this.mScope.view) {
            this.mScope.lastFocusPosition = this.mScope.view.getChildAdapterPosition(view);
        }
    }

    @Override
    public boolean focusDirectoryList() {
        if (this.mScope.adapter == null || this.mScope.adapter.getItemCount() == 0) {
            if (SharedMinimal.DEBUG) {
                Log.v("FocusManager", "Nothing to focus.");
            }
            return false;
        }
        if (this.mSelectionMgr.hasSelection()) {
            if (SharedMinimal.DEBUG) {
                Log.v("FocusManager", "Existing selection found. No focus will be done.");
            }
            return false;
        }
        int iFindFirstVisibleItemPosition = this.mScope.lastFocusPosition != -1 ? this.mScope.lastFocusPosition : this.mScope.layout.findFirstVisibleItemPosition();
        if (iFindFirstVisibleItemPosition == -1) {
            return false;
        }
        focusItem(iFindFirstVisibleItemPosition);
        return true;
    }

    public void onLayoutCompleted() {
        if (this.mScope.pendingFocusId == null) {
            return;
        }
        int iIndexOf = this.mScope.adapter.getStableIds().indexOf(this.mScope.pendingFocusId);
        if (iIndexOf != -1) {
            focusItem(iIndexOf);
        }
        this.mScope.pendingFocusId = null;
    }

    @Override
    public void clearFocus() {
        this.mScope.view.clearFocus();
    }

    @Override
    public void focusDocument(String str) {
        int adapterPosition = this.mScope.adapter.getAdapterPosition(str);
        if (adapterPosition != -1 && this.mScope.view.findViewHolderForAdapterPosition(adapterPosition) != null) {
            focusItem(adapterPosition);
        } else {
            this.mScope.pendingFocusId = str;
        }
    }

    @Override
    public int getFocusPosition() {
        return this.mScope.lastFocusPosition;
    }

    @Override
    public boolean hasFocusedItem() {
        return this.mScope.lastFocusPosition != -1;
    }

    @Override
    public String getFocusModelId() {
        DocumentHolder documentHolder;
        if (this.mScope.lastFocusPosition != -1 && (documentHolder = (DocumentHolder) this.mScope.view.findViewHolderForAdapterPosition(this.mScope.lastFocusPosition)) != null) {
            return documentHolder.getModelId();
        }
        return null;
    }

    private int findTargetPosition(View view, int i, KeyEvent keyEvent) {
        int i2;
        int i3;
        switch (i) {
            case 92:
            case 93:
                return findPagedTargetPosition(view, i, keyEvent);
            case 122:
                return 0;
            case 123:
                return this.mScope.adapter.getItemCount() - 1;
            default:
                switch (i) {
                    case 19:
                        i2 = 33;
                        break;
                    case 20:
                        i2 = 130;
                        break;
                    default:
                        i2 = -1;
                        break;
                }
                if (inGridMode()) {
                    int childAdapterPosition = this.mScope.view.getChildAdapterPosition(view);
                    switch (i) {
                        case 21:
                            i3 = childAdapterPosition <= 0 ? i2 : 1;
                            break;
                        case 22:
                            i3 = childAdapterPosition >= this.mScope.adapter.getItemCount() - 1 ? i2 : 2;
                            break;
                        default:
                            i3 = i2;
                            break;
                    }
                } else {
                    i3 = i2;
                }
                if (i3 != -1) {
                    this.mScope.view.setFocusable(false);
                    View viewFocusSearch = view.focusSearch(i3);
                    this.mScope.view.setFocusable(true);
                    if (viewFocusSearch != null && viewFocusSearch.getParent() == this.mScope.view) {
                        return this.mScope.view.getChildAdapterPosition(viewFocusSearch);
                    }
                }
                return -1;
        }
    }

    private int findPagedTargetPosition(View view, int i, KeyEvent keyEvent) {
        int iFindFirstVisibleItemPosition = this.mScope.layout.findFirstVisibleItemPosition();
        int iFindLastVisibleItemPosition = this.mScope.layout.findLastVisibleItemPosition();
        int childAdapterPosition = this.mScope.view.getChildAdapterPosition(view);
        int i2 = (iFindLastVisibleItemPosition - iFindFirstVisibleItemPosition) + 1;
        if (i == 92) {
            if (childAdapterPosition > iFindFirstVisibleItemPosition) {
                return iFindFirstVisibleItemPosition;
            }
            int i3 = childAdapterPosition - i2;
            if (i3 < 0) {
                return 0;
            }
            return i3;
        }
        if (i == 93) {
            if (childAdapterPosition < iFindLastVisibleItemPosition) {
                return iFindLastVisibleItemPosition;
            }
            int i4 = childAdapterPosition + i2;
            int itemCount = this.mScope.adapter.getItemCount() - 1;
            return i4 < itemCount ? i4 : itemCount;
        }
        throw new IllegalArgumentException("Unsupported keyCode: " + i);
    }

    private void focusItem(int i) {
        if (i == -1) {
            Log.d("FocusManager", "Skip focus invalid position");
        } else {
            focusItem(i, null);
        }
    }

    private void focusItem(final int i, final FocusCallback focusCallback) {
        if (this.mScope.pendingFocusId != null) {
            Log.v("FocusManager", "clearing pending focus id: " + this.mScope.pendingFocusId);
            this.mScope.pendingFocusId = null;
        }
        RecyclerView recyclerView = this.mScope.view;
        RecyclerView.ViewHolder viewHolderFindViewHolderForAdapterPosition = recyclerView.findViewHolderForAdapterPosition(i);
        if (viewHolderFindViewHolderForAdapterPosition != null) {
            if (viewHolderFindViewHolderForAdapterPosition.itemView.requestFocus() && focusCallback != null) {
                focusCallback.onFocus(viewHolderFindViewHolderForAdapterPosition.itemView);
                return;
            }
            return;
        }
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView2, int i2) {
                if (i2 == 0) {
                    RecyclerView.ViewHolder viewHolderFindViewHolderForAdapterPosition2 = recyclerView2.findViewHolderForAdapterPosition(i);
                    if (viewHolderFindViewHolderForAdapterPosition2 != null) {
                        if (viewHolderFindViewHolderForAdapterPosition2.itemView.requestFocus() && focusCallback != null) {
                            focusCallback.onFocus(viewHolderFindViewHolderForAdapterPosition2.itemView);
                        }
                    } else {
                        Log.w("FocusManager", "Unable to focus position " + i + " after scroll");
                    }
                    recyclerView2.removeOnScrollListener(this);
                }
            }
        });
        recyclerView.smoothScrollToPosition(i);
    }

    private boolean inGridMode() {
        return this.mScope.layout.getSpanCount() > 1;
    }

    private class TitleSearchHelper {
        private boolean mActive;
        private List<String> mIndex;
        private KeyEvent mLastEvent;
        private final BackgroundColorSpan mSpan;
        private Timer mTimer;
        private final KeyListener mTextListener = new TextKeyListener(TextKeyListener.Capitalize.NONE, false);
        private final Editable mSearchString = Editable.Factory.getInstance().newEditable("");
        private final Highlighter mHighlighter = new Highlighter();
        private EventListener<Model.Update> mModelListener = new EventListener<Model.Update>() {
            @Override
            public void accept(Model.Update update) {
                TitleSearchHelper.this.mIndex = null;
            }
        };
        private Handler mUiRunner = new Handler(Looper.getMainLooper());

        public TitleSearchHelper(int i) {
            this.mSpan = new BackgroundColorSpan(i);
        }

        public boolean handleKey(DocumentHolder documentHolder, int i, KeyEvent keyEvent) {
            if (i != 62) {
                if (i == 66 || i == 111) {
                    if (!this.mActive) {
                        return false;
                    }
                    endSearch();
                    return true;
                }
            } else if (!this.mActive) {
                return false;
            }
            if (Events.isNavigationKeyCode(i)) {
                endSearch();
                return false;
            }
            boolean zOnKeyDown = this.mTextListener.onKeyDown(documentHolder.itemView, this.mSearchString, i, keyEvent);
            if (i == 67) {
                zOnKeyDown = true;
            }
            if (zOnKeyDown) {
                this.mLastEvent = keyEvent;
                if (this.mSearchString.length() == 0) {
                    return false;
                }
                search();
            }
            return zOnKeyDown;
        }

        private void search() {
            if (!this.mActive) {
                FocusManager.this.mScope.model.addUpdateListener(this.mModelListener);
                this.mTimer = new Timer();
                this.mActive = true;
            }
            if (this.mIndex == null) {
                buildIndex();
            }
            String lowerCase = this.mSearchString.toString().toLowerCase();
            for (int i = 0; i < this.mIndex.size(); i++) {
                String str = this.mIndex.get(i);
                if (str != null && str.startsWith(lowerCase)) {
                    FocusManager.this.focusItem(i, new FocusCallback() {
                        @Override
                        public void onFocus(View view) {
                            TitleSearchHelper.this.mHighlighter.applyHighlight(view);
                            TitleSearchHelper.this.mTimer.schedule(new TimeoutTask(), 0L, 250L);
                        }
                    });
                    return;
                }
            }
        }

        private void endSearch() {
            if (this.mActive) {
                FocusManager.this.mScope.model.removeUpdateListener(this.mModelListener);
                this.mTimer.cancel();
            }
            this.mHighlighter.removeHighlight();
            this.mIndex = null;
            this.mSearchString.clear();
            this.mActive = false;
        }

        private void buildIndex() {
            int itemCount = FocusManager.this.mScope.adapter.getItemCount();
            ArrayList arrayList = new ArrayList(itemCount);
            for (int i = 0; i < itemCount; i++) {
                String stableId = FocusManager.this.mScope.adapter.getStableId(i);
                Cursor item = FocusManager.this.mScope.model.getItem(stableId);
                if (stableId != null && item != null) {
                    arrayList.add(DocumentInfo.getCursorString(item, "_display_name").toLowerCase());
                } else {
                    arrayList.add("");
                }
            }
            this.mIndex = arrayList;
        }

        private class TimeoutTask extends TimerTask {
            private TimeoutTask() {
            }

            @Override
            public void run() {
                if (SystemClock.uptimeMillis() - TitleSearchHelper.this.mLastEvent.getEventTime() > 500) {
                    TitleSearchHelper.this.mUiRunner.post(new Runnable() {
                        @Override
                        public void run() {
                            TitleSearchHelper.this.endSearch();
                        }
                    });
                }
            }
        }

        private class Highlighter {
            private Spannable mCurrentHighlight;

            private Highlighter() {
            }

            private void applyHighlight(View view) {
                TextView textView = (TextView) view.findViewById(android.R.id.title);
                if (textView == null) {
                    return;
                }
                CharSequence text = textView.getText();
                if (text instanceof Spannable) {
                    if (this.mCurrentHighlight != null) {
                        this.mCurrentHighlight.removeSpan(TitleSearchHelper.this.mSpan);
                    }
                    this.mCurrentHighlight = (Spannable) text;
                    this.mCurrentHighlight.setSpan(TitleSearchHelper.this.mSpan, 0, TitleSearchHelper.this.mSearchString.length(), 33);
                }
            }

            private void removeHighlight() {
                if (this.mCurrentHighlight != null) {
                    this.mCurrentHighlight.removeSpan(TitleSearchHelper.this.mSpan);
                }
            }
        }
    }

    public FocusManager reset(RecyclerView recyclerView, Model model) {
        this.mScope.view = recyclerView;
        this.mScope.adapter = (DocumentsAdapter) recyclerView.getAdapter();
        this.mScope.layout = (GridLayoutManager) recyclerView.getLayoutManager();
        this.mScope.model = model;
        this.mScope.lastFocusPosition = -1;
        this.mScope.pendingFocusId = null;
        return this;
    }

    private static final class ContentScope {
        private DocumentsAdapter adapter;
        private int lastFocusPosition;
        private GridLayoutManager layout;
        private Model model;
        private String pendingFocusId;
        private RecyclerView view;

        private ContentScope() {
            this.lastFocusPosition = -1;
        }
    }
}
