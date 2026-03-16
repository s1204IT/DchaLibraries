package com.android.printspooler.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.internal.os.SomeArgs;
import com.android.printspooler.R;
import com.android.printspooler.model.MutexFileProvider;
import com.android.printspooler.ui.PageAdapter;
import com.android.printspooler.widget.EmbeddedContentContainer;
import com.android.printspooler.widget.PrintContentView;
import com.android.printspooler.widget.PrintOptionsLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

class PrintPreviewController implements MutexFileProvider.OnReleaseRequestCallback, PageAdapter.PreviewArea, EmbeddedContentContainer.OnSizeChangeListener {
    private final PrintActivity mActivity;
    private final PrintContentView mContentView;
    private int mDocumentPageCount;
    private final EmbeddedContentContainer mEmbeddedContentContainer;
    private final MutexFileProvider mFileProvider;
    private final MyHandler mHandler;
    private final GridLayoutManager mLayoutManger;
    private final PageAdapter mPageAdapter;
    private final PreloadController mPreloadController;
    private final PrintOptionsLayout mPrintOptionsLayout;
    private final RecyclerView mRecyclerView;

    public PrintPreviewController(PrintActivity activity, MutexFileProvider fileProvider) {
        this.mActivity = activity;
        this.mHandler = new MyHandler(activity.getMainLooper());
        this.mFileProvider = fileProvider;
        this.mPrintOptionsLayout = (PrintOptionsLayout) activity.findViewById(R.id.options_container);
        this.mPageAdapter = new PageAdapter(activity, activity, this);
        int columnCount = this.mActivity.getResources().getInteger(R.integer.preview_page_per_row_count);
        this.mLayoutManger = new GridLayoutManager(this.mActivity, columnCount);
        this.mRecyclerView = (RecyclerView) activity.findViewById(R.id.preview_content);
        this.mRecyclerView.setLayoutManager(this.mLayoutManger);
        this.mRecyclerView.setAdapter(this.mPageAdapter);
        this.mRecyclerView.setItemViewCacheSize(0);
        this.mPreloadController = new PreloadController(this.mRecyclerView);
        this.mRecyclerView.setOnScrollListener(this.mPreloadController);
        this.mContentView = (PrintContentView) activity.findViewById(R.id.options_content);
        this.mEmbeddedContentContainer = (EmbeddedContentContainer) activity.findViewById(R.id.embedded_content_container);
        this.mEmbeddedContentContainer.setOnSizeChangeListener(this);
    }

    @Override
    public void onSizeChanged(int width, int height) {
        this.mPageAdapter.onPreviewAreaSizeChanged();
    }

    public boolean isOptionsOpened() {
        return this.mContentView.isOptionsOpened();
    }

    public void closeOptions() {
        this.mContentView.closeOptions();
    }

    public void setUiShown(boolean shown) {
        if (shown) {
            this.mRecyclerView.setVisibility(0);
        } else {
            this.mRecyclerView.setVisibility(8);
        }
    }

    public void onOrientationChanged() {
        int optionColumnCount = this.mActivity.getResources().getInteger(R.integer.print_option_column_count);
        this.mPrintOptionsLayout.setColumnCount(optionColumnCount);
        this.mPageAdapter.onOrientationChanged();
    }

    public int getFilePageCount() {
        return this.mPageAdapter.getFilePageCount();
    }

    public PageRange[] getSelectedPages() {
        return this.mPageAdapter.getSelectedPages();
    }

    public PageRange[] getRequestedPages() {
        return this.mPageAdapter.getRequestedPages();
    }

    public void onContentUpdated(boolean documentChanged, int documentPageCount, PageRange[] writtenPages, PageRange[] selectedPages, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins minMargins) {
        boolean contentChanged = false;
        if (documentChanged) {
            contentChanged = true;
        }
        if (documentPageCount != this.mDocumentPageCount) {
            this.mDocumentPageCount = documentPageCount;
            contentChanged = true;
        }
        if (contentChanged && this.mPageAdapter.isOpened()) {
            Message operation = this.mHandler.obtainMessage(2);
            this.mHandler.enqueueOperation(operation);
        }
        if ((contentChanged || !this.mPageAdapter.isOpened()) && writtenPages != null) {
            Message operation2 = this.mHandler.obtainMessage(1);
            this.mHandler.enqueueOperation(operation2);
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = writtenPages;
        args.arg2 = selectedPages;
        args.arg3 = mediaSize;
        args.arg4 = minMargins;
        args.argi1 = documentPageCount;
        Message operation3 = this.mHandler.obtainMessage(4, args);
        this.mHandler.enqueueOperation(operation3);
        if (contentChanged && writtenPages != null) {
            Message operation4 = this.mHandler.obtainMessage(5);
            this.mHandler.enqueueOperation(operation4);
        }
    }

    @Override
    public void onReleaseRequested(File file) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (PrintPreviewController.this.mPageAdapter.isOpened()) {
                    Message operation = PrintPreviewController.this.mHandler.obtainMessage(2);
                    PrintPreviewController.this.mHandler.enqueueOperation(operation);
                }
            }
        });
    }

    public void destroy(Runnable callback) {
        this.mHandler.cancelQueuedOperations();
        this.mRecyclerView.setAdapter(null);
        this.mPageAdapter.destroy(callback);
    }

    @Override
    public int getWidth() {
        return this.mEmbeddedContentContainer.getWidth();
    }

    @Override
    public int getHeight() {
        return this.mEmbeddedContentContainer.getHeight();
    }

    @Override
    public void setColumnCount(int columnCount) {
        this.mLayoutManger.setSpanCount(columnCount);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        this.mRecyclerView.setPadding(left, top, right, bottom);
    }

    private final class MyHandler extends Handler {
        private boolean mAsyncOperationInProgress;
        private final Runnable mOnAsyncOperationDoneCallback;
        private final List<Message> mPendingOperations;

        public MyHandler(Looper looper) {
            super(looper, null, false);
            this.mOnAsyncOperationDoneCallback = new Runnable() {
                @Override
                public void run() {
                    MyHandler.this.mAsyncOperationInProgress = false;
                    MyHandler.this.handleNextOperation();
                }
            };
            this.mPendingOperations = new ArrayList();
        }

        public void cancelQueuedOperations() {
            this.mPendingOperations.clear();
        }

        public void enqueueOperation(Message message) {
            this.mPendingOperations.add(message);
            handleNextOperation();
        }

        public void handleNextOperation() {
            while (!this.mPendingOperations.isEmpty() && !this.mAsyncOperationInProgress) {
                Message operation = this.mPendingOperations.remove(0);
                handleMessage(operation);
            }
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    try {
                        File file = PrintPreviewController.this.mFileProvider.acquireFile(PrintPreviewController.this);
                        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, 268435456);
                        this.mAsyncOperationInProgress = true;
                        PrintPreviewController.this.mPageAdapter.open(pfd, new Runnable() {
                            @Override
                            public void run() {
                                if (PrintPreviewController.this.mDocumentPageCount == -1) {
                                    PrintPreviewController.this.mDocumentPageCount = PrintPreviewController.this.mPageAdapter.getFilePageCount();
                                    PrintPreviewController.this.mActivity.updateOptionsUi();
                                }
                                MyHandler.this.mOnAsyncOperationDoneCallback.run();
                            }
                        });
                    } catch (FileNotFoundException e) {
                        return;
                    }
                    break;
                case 2:
                    this.mAsyncOperationInProgress = true;
                    PrintPreviewController.this.mPageAdapter.close(new Runnable() {
                        @Override
                        public void run() {
                            PrintPreviewController.this.mFileProvider.releaseFile();
                            MyHandler.this.mOnAsyncOperationDoneCallback.run();
                        }
                    });
                    break;
                case 4:
                    SomeArgs args = (SomeArgs) message.obj;
                    PageRange[] writtenPages = (PageRange[]) args.arg1;
                    PageRange[] selectedPages = (PageRange[]) args.arg2;
                    PrintAttributes.MediaSize mediaSize = (PrintAttributes.MediaSize) args.arg3;
                    PrintAttributes.Margins margins = (PrintAttributes.Margins) args.arg4;
                    int pageCount = args.argi1;
                    args.recycle();
                    PrintPreviewController.this.mPageAdapter.update(writtenPages, selectedPages, pageCount, mediaSize, margins);
                    break;
                case 5:
                    PrintPreviewController.this.mPreloadController.startPreloadContent();
                    break;
            }
        }
    }

    private final class PreloadController extends RecyclerView.OnScrollListener {
        private int mOldScrollState;
        private final RecyclerView mRecyclerView;

        public PreloadController(RecyclerView recyclerView) {
            this.mRecyclerView = recyclerView;
            this.mOldScrollState = this.mRecyclerView.getScrollState();
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int state) {
            switch (this.mOldScrollState) {
                case 0:
                case 1:
                    if (state == 2) {
                        stopPreloadContent();
                    }
                    break;
                case 2:
                    if (state == 0 || state == 1) {
                        startPreloadContent();
                    }
                    break;
            }
            this.mOldScrollState = state;
        }

        public void startPreloadContent() {
            PageRange shownPages;
            PageAdapter pageAdapter = (PageAdapter) this.mRecyclerView.getAdapter();
            if (pageAdapter != null && pageAdapter.isOpened() && (shownPages = computeShownPages()) != null) {
                pageAdapter.startPreloadContent(shownPages);
            }
        }

        public void stopPreloadContent() {
            PageAdapter pageAdapter = (PageAdapter) this.mRecyclerView.getAdapter();
            if (pageAdapter != null && pageAdapter.isOpened()) {
                pageAdapter.stopPreloadContent();
            }
        }

        private PageRange computeShownPages() {
            int childCount = this.mRecyclerView.getChildCount();
            if (childCount <= 0) {
                return null;
            }
            RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
            View firstChild = layoutManager.getChildAt(0);
            RecyclerView.ViewHolder firstHolder = this.mRecyclerView.getChildViewHolder(firstChild);
            View lastChild = layoutManager.getChildAt(layoutManager.getChildCount() - 1);
            RecyclerView.ViewHolder lastHolder = this.mRecyclerView.getChildViewHolder(lastChild);
            return new PageRange(firstHolder.getPosition(), lastHolder.getPosition());
        }
    }
}
