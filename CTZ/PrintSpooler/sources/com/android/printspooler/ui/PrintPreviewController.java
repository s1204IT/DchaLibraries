package com.android.printspooler.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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

    public PrintPreviewController(PrintActivity printActivity, MutexFileProvider mutexFileProvider) {
        this.mActivity = printActivity;
        this.mHandler = new MyHandler(printActivity.getMainLooper());
        this.mFileProvider = mutexFileProvider;
        this.mPrintOptionsLayout = (PrintOptionsLayout) printActivity.findViewById(R.id.options_container);
        this.mPageAdapter = new PageAdapter(printActivity, printActivity, this);
        this.mLayoutManger = new GridLayoutManager(this.mActivity, this.mActivity.getResources().getInteger(R.integer.preview_page_per_row_count));
        this.mRecyclerView = (RecyclerView) printActivity.findViewById(R.id.preview_content);
        this.mRecyclerView.setLayoutManager(this.mLayoutManger);
        this.mRecyclerView.setAdapter(this.mPageAdapter);
        this.mRecyclerView.setItemViewCacheSize(0);
        this.mPreloadController = new PreloadController();
        this.mRecyclerView.addOnScrollListener(this.mPreloadController);
        this.mContentView = (PrintContentView) printActivity.findViewById(R.id.options_content);
        this.mEmbeddedContentContainer = (EmbeddedContentContainer) printActivity.findViewById(R.id.embedded_content_container);
        this.mEmbeddedContentContainer.setOnSizeChangeListener(this);
    }

    @Override
    public void onSizeChanged(int i, int i2) {
        this.mPageAdapter.onPreviewAreaSizeChanged();
    }

    public boolean isOptionsOpened() {
        return this.mContentView.isOptionsOpened();
    }

    public void closeOptions() {
        this.mContentView.closeOptions();
    }

    public void setUiShown(boolean z) {
        if (z) {
            this.mRecyclerView.setVisibility(0);
        } else {
            this.mRecyclerView.setVisibility(8);
        }
    }

    public void onOrientationChanged() {
        this.mPrintOptionsLayout.setColumnCount(this.mActivity.getResources().getInteger(R.integer.print_option_column_count));
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

    public void onContentUpdated(boolean z, int i, PageRange[] pageRangeArr, PageRange[] pageRangeArr2, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins margins) {
        boolean z2 = z;
        if (i != this.mDocumentPageCount) {
            this.mDocumentPageCount = i;
            z2 = true;
        }
        if (z2 && this.mPageAdapter.isOpened()) {
            this.mHandler.enqueueOperation(this.mHandler.obtainMessage(2));
        }
        if ((z2 || !this.mPageAdapter.isOpened()) && pageRangeArr != null) {
            this.mHandler.enqueueOperation(this.mHandler.obtainMessage(1));
        }
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = pageRangeArr;
        someArgsObtain.arg2 = pageRangeArr2;
        someArgsObtain.arg3 = mediaSize;
        someArgsObtain.arg4 = margins;
        someArgsObtain.argi1 = i;
        this.mHandler.enqueueOperation(this.mHandler.obtainMessage(4, someArgsObtain));
        if (z2 && pageRangeArr != null) {
            this.mHandler.enqueueOperation(this.mHandler.obtainMessage(5));
        }
    }

    @Override
    public void onReleaseRequested(File file) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (PrintPreviewController.this.mPageAdapter.isOpened()) {
                    PrintPreviewController.this.mHandler.enqueueOperation(PrintPreviewController.this.mHandler.obtainMessage(2));
                }
            }
        });
    }

    public void destroy(Runnable runnable) {
        this.mHandler.cancelQueuedOperations();
        this.mRecyclerView.setAdapter(null);
        this.mPageAdapter.destroy(runnable);
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
    public void setColumnCount(int i) {
        this.mLayoutManger.setSpanCount(i);
    }

    @Override
    public void setPadding(int i, int i2, int i3, int i4) {
        this.mRecyclerView.setPadding(i, i2, i3, i4);
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
                handleMessage(this.mPendingOperations.remove(0));
            }
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    try {
                        ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(PrintPreviewController.this.mFileProvider.acquireFile(PrintPreviewController.this), 268435456);
                        this.mAsyncOperationInProgress = true;
                        PrintPreviewController.this.mPageAdapter.open(parcelFileDescriptorOpen, new Runnable() {
                            @Override
                            public void run() {
                                if (PrintPreviewController.this.mDocumentPageCount == -1) {
                                    PrintPreviewController.this.mDocumentPageCount = PrintPreviewController.this.mPageAdapter.getFilePageCount();
                                    PrintPreviewController.this.mActivity.updateOptionsUi();
                                }
                                MyHandler.this.mOnAsyncOperationDoneCallback.run();
                            }
                        });
                        break;
                    } catch (FileNotFoundException e) {
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
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    PageRange[] pageRangeArr = (PageRange[]) someArgs.arg1;
                    PageRange[] pageRangeArr2 = (PageRange[]) someArgs.arg2;
                    PrintAttributes.MediaSize mediaSize = (PrintAttributes.MediaSize) someArgs.arg3;
                    PrintAttributes.Margins margins = (PrintAttributes.Margins) someArgs.arg4;
                    int i = someArgs.argi1;
                    someArgs.recycle();
                    PrintPreviewController.this.mPageAdapter.update(pageRangeArr, pageRangeArr2, i, mediaSize, margins);
                    break;
                case 5:
                    PrintPreviewController.this.mPreloadController.startPreloadContent();
                    break;
            }
        }
    }

    private final class PreloadController extends RecyclerView.OnScrollListener {
        private int mOldScrollState;

        public PreloadController() {
            this.mOldScrollState = PrintPreviewController.this.mRecyclerView.getScrollState();
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int i) {
            switch (this.mOldScrollState) {
                case 0:
                case 1:
                    if (i == 2) {
                        stopPreloadContent();
                    }
                    break;
                case 2:
                    if (i == 0 || i == 1) {
                        startPreloadContent();
                    }
                    break;
            }
            this.mOldScrollState = i;
        }

        public void startPreloadContent() {
            PageRange pageRangeComputeShownPages;
            PageAdapter pageAdapter = (PageAdapter) PrintPreviewController.this.mRecyclerView.getAdapter();
            if (pageAdapter != null && pageAdapter.isOpened() && (pageRangeComputeShownPages = computeShownPages()) != null) {
                pageAdapter.startPreloadContent(pageRangeComputeShownPages);
            }
        }

        public void stopPreloadContent() {
            PageAdapter pageAdapter = (PageAdapter) PrintPreviewController.this.mRecyclerView.getAdapter();
            if (pageAdapter != null && pageAdapter.isOpened()) {
                pageAdapter.stopPreloadContent();
            }
        }

        private PageRange computeShownPages() {
            if (PrintPreviewController.this.mRecyclerView.getChildCount() > 0) {
                RecyclerView.LayoutManager layoutManager = PrintPreviewController.this.mRecyclerView.getLayoutManager();
                return new PageRange(PrintPreviewController.this.mRecyclerView.getChildViewHolder(layoutManager.getChildAt(0)).getLayoutPosition(), PrintPreviewController.this.mRecyclerView.getChildViewHolder(layoutManager.getChildAt(layoutManager.getChildCount() - 1)).getLayoutPosition());
            }
            return null;
        }
    }
}
