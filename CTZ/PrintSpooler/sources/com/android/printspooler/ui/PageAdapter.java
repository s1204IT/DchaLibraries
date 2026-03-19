package com.android.printspooler.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.printspooler.R;
import com.android.printspooler.model.OpenDocumentCallback;
import com.android.printspooler.model.PageContentRepository;
import com.android.printspooler.util.PageRangeUtils;
import com.android.printspooler.widget.PageContentView;
import com.android.printspooler.widget.PreviewPageFrame;
import dalvik.system.CloseGuard;
import java.util.ArrayList;
import java.util.Arrays;

public final class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final PageRange[] ALL_PAGES_ARRAY = {PageRange.ALL_PAGES};
    private final ContentCallbacks mCallbacks;
    private int mColumnCount;
    private final Context mContext;
    private BitmapDrawable mEmptyState;
    private BitmapDrawable mErrorState;
    private int mFooterHeight;
    private final LayoutInflater mLayoutInflater;
    private PrintAttributes.MediaSize mMediaSize;
    private PrintAttributes.Margins mMinMargins;
    private int mPageContentHeight;
    private final PageContentRepository mPageContentRepository;
    private int mPageContentWidth;
    private final PreviewArea mPreviewArea;
    private int mPreviewListPadding;
    private int mPreviewPageMargin;
    private int mPreviewPageMinWidth;
    private PageRange[] mRequestedPages;
    private int mSelectedPageCount;
    private PageRange[] mSelectedPages;
    private int mState;
    private PageRange[] mWrittenPages;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final SparseArray<Void> mBoundPagesInAdapter = new SparseArray<>();
    private final SparseArray<Void> mConfirmedPagesInDocument = new SparseArray<>();
    private final PageClickListener mPageClickListener = new PageClickListener();
    private int mDocumentPageCount = -1;

    public interface ContentCallbacks {
        void onMalformedPdfFile();

        void onRequestContentUpdate();

        void onSecurePdfFile();
    }

    public interface PreviewArea {
        int getHeight();

        int getWidth();

        void setColumnCount(int i);

        void setPadding(int i, int i2, int i3, int i4);
    }

    public PageAdapter(Context context, ContentCallbacks contentCallbacks, PreviewArea previewArea) {
        this.mContext = context;
        this.mCallbacks = contentCallbacks;
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mPageContentRepository = new PageContentRepository(context);
        this.mPreviewPageMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.preview_page_margin);
        this.mPreviewPageMinWidth = this.mContext.getResources().getDimensionPixelSize(R.dimen.preview_page_min_width);
        this.mPreviewListPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.preview_list_padding);
        this.mColumnCount = this.mContext.getResources().getInteger(R.integer.preview_page_per_row_count);
        this.mFooterHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.preview_page_footer_height);
        this.mPreviewArea = previewArea;
        this.mCloseGuard.open("destroy");
        setHasStableIds(true);
        this.mState = 0;
    }

    public void onOrientationChanged() {
        this.mColumnCount = this.mContext.getResources().getInteger(R.integer.preview_page_per_row_count);
        notifyDataSetChanged();
    }

    public boolean isOpened() {
        return this.mState == 1;
    }

    public int getFilePageCount() {
        return this.mPageContentRepository.getFilePageCount();
    }

    public void open(ParcelFileDescriptor parcelFileDescriptor, final Runnable runnable) {
        throwIfNotClosed();
        this.mState = 1;
        this.mPageContentRepository.open(parcelFileDescriptor, new OpenDocumentCallback() {
            @Override
            public void onSuccess() {
                PageAdapter.this.notifyDataSetChanged();
                runnable.run();
            }

            @Override
            public void onFailure(int i) {
                switch (i) {
                    case -2:
                        PageAdapter.this.mCallbacks.onSecurePdfFile();
                        break;
                    case -1:
                        PageAdapter.this.mCallbacks.onMalformedPdfFile();
                        break;
                }
            }
        });
    }

    public void update(PageRange[] pageRangeArr, PageRange[] pageRangeArr2, int i, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins margins) {
        boolean z;
        if (i == -1) {
            if (pageRangeArr == null) {
                if (!Arrays.equals(ALL_PAGES_ARRAY, this.mRequestedPages)) {
                    this.mRequestedPages = ALL_PAGES_ARRAY;
                    this.mCallbacks.onRequestContentUpdate();
                    return;
                }
                return;
            }
            i = this.mPageContentRepository.getFilePageCount();
            if (i <= 0) {
                return;
            }
        }
        boolean z2 = false;
        boolean z3 = true;
        if (this.mDocumentPageCount != i) {
            this.mDocumentPageCount = i;
            z = true;
        } else {
            z = false;
        }
        boolean z4 = z;
        if (this.mMediaSize == null || !this.mMediaSize.equals(mediaSize)) {
            this.mMediaSize = mediaSize;
            z = true;
            z2 = true;
            z4 = true;
        }
        if (this.mMinMargins == null || !this.mMinMargins.equals(margins)) {
            this.mMinMargins = margins;
            z = true;
            z2 = true;
            z4 = true;
        }
        if (z) {
            this.mSelectedPages = PageRange.ALL_PAGES_ARRAY;
            this.mSelectedPageCount = i;
            setConfirmedPages(this.mSelectedPages, i);
        } else {
            if (!Arrays.equals(this.mSelectedPages, pageRangeArr2)) {
                this.mSelectedPages = pageRangeArr2;
                this.mSelectedPageCount = PageRangeUtils.getNormalizedPageCount(this.mSelectedPages, i);
                setConfirmedPages(this.mSelectedPages, i);
            }
            if (pageRangeArr != null) {
                if (PageRangeUtils.isAllPages(pageRangeArr)) {
                    pageRangeArr = this.mRequestedPages;
                }
                if (!Arrays.equals(this.mWrittenPages, pageRangeArr)) {
                    this.mWrittenPages = pageRangeArr;
                }
            } else {
                z3 = z4;
            }
            if (z2) {
                updatePreviewAreaPageSizeAndEmptyState();
            }
            if (!z3) {
                notifyDataSetChanged();
                return;
            }
            return;
        }
        z2 = true;
        z4 = true;
        if (pageRangeArr != null) {
        }
        if (z2) {
        }
        if (!z3) {
        }
    }

    public void close(Runnable runnable) {
        throwIfNotOpened();
        this.mState = 0;
        this.mPageContentRepository.close(runnable);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new MyViewHolder(i == 0 ? this.mLayoutInflater.inflate(R.layout.preview_page_selected, viewGroup, false) : this.mLayoutInflater.inflate(R.layout.preview_page, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        PageContentRepository.PageContentProvider pageContentProvider;
        PreviewPageFrame previewPageFrame = (PreviewPageFrame) viewHolder.itemView;
        previewPageFrame.setOnClickListener(this.mPageClickListener);
        previewPageFrame.setTag(viewHolder);
        ((MyViewHolder) viewHolder).mPageInAdapter = i;
        int iComputePageIndexInDocument = computePageIndexInDocument(i);
        int iComputePageIndexInFile = computePageIndexInFile(iComputePageIndexInDocument);
        PageContentView pageContentView = (PageContentView) previewPageFrame.findViewById(R.id.page_content);
        ViewGroup.LayoutParams layoutParams = pageContentView.getLayoutParams();
        layoutParams.width = this.mPageContentWidth;
        layoutParams.height = this.mPageContentHeight;
        PageContentRepository.PageContentProvider pageContentProvider2 = pageContentView.getPageContentProvider();
        if (iComputePageIndexInFile != -1) {
            PageContentRepository.PageContentProvider pageContentProviderAcquirePageContentProvider = this.mPageContentRepository.acquirePageContentProvider(iComputePageIndexInFile, pageContentView);
            this.mBoundPagesInAdapter.put(i, null);
            pageContentProvider = pageContentProviderAcquirePageContentProvider;
        } else {
            onSelectedPageNotInFile(iComputePageIndexInDocument);
            pageContentProvider = pageContentProvider2;
        }
        pageContentView.init(pageContentProvider, this.mEmptyState, this.mErrorState, this.mMediaSize, this.mMinMargins);
        if (this.mConfirmedPagesInDocument.indexOfKey(iComputePageIndexInDocument) >= 0) {
            previewPageFrame.setSelected(true);
        } else {
            previewPageFrame.setSelected(false);
        }
        int i2 = iComputePageIndexInDocument + 1;
        previewPageFrame.setContentDescription(this.mContext.getString(R.string.page_description_template, Integer.valueOf(i2), Integer.valueOf(this.mDocumentPageCount)));
        ((TextView) previewPageFrame.findViewById(R.id.page_number)).setText(this.mContext.getString(R.string.current_page_template, Integer.valueOf(i2), Integer.valueOf(this.mDocumentPageCount)));
    }

    @Override
    public int getItemCount() {
        return this.mSelectedPageCount;
    }

    @Override
    public int getItemViewType(int i) {
        if (this.mConfirmedPagesInDocument.indexOfKey(computePageIndexInDocument(i)) >= 0) {
            return 0;
        }
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return computePageIndexInDocument(i);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        MyViewHolder myViewHolder = (MyViewHolder) viewHolder;
        recyclePageView((PageContentView) viewHolder.itemView.findViewById(R.id.page_content), myViewHolder.mPageInAdapter);
        myViewHolder.mPageInAdapter = -1;
    }

    public PageRange[] getRequestedPages() {
        return this.mRequestedPages;
    }

    public PageRange[] getSelectedPages() {
        PageRange[] pageRangeArrComputeSelectedPages = computeSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, pageRangeArrComputeSelectedPages)) {
            this.mSelectedPages = pageRangeArrComputeSelectedPages;
            this.mSelectedPageCount = PageRangeUtils.getNormalizedPageCount(this.mSelectedPages, this.mDocumentPageCount);
            updatePreviewAreaPageSizeAndEmptyState();
            notifyDataSetChanged();
        }
        return this.mSelectedPages;
    }

    public void onPreviewAreaSizeChanged() {
        if (this.mMediaSize != null) {
            updatePreviewAreaPageSizeAndEmptyState();
            notifyDataSetChanged();
        }
    }

    private void updatePreviewAreaPageSizeAndEmptyState() {
        if (this.mMediaSize == null) {
            return;
        }
        int width = this.mPreviewArea.getWidth();
        int height = this.mPreviewArea.getHeight();
        float widthMils = this.mMediaSize.getWidthMils() / this.mMediaSize.getHeightMils();
        int iMin = Math.min(this.mSelectedPageCount, this.mColumnCount);
        this.mPreviewArea.setColumnCount(iMin);
        int i = 2 * iMin * this.mPreviewPageMargin;
        this.mPageContentHeight = Math.min((int) ((((int) (((width - ((this.mPreviewListPadding * 2) + i)) / iMin) + 0.5f)) / widthMils) + 0.5f), Math.max((int) ((this.mPreviewPageMinWidth / widthMils) + 0.5f), (height - ((this.mPreviewListPadding + this.mPreviewPageMargin) * 2)) - this.mFooterHeight));
        this.mPageContentWidth = (int) ((this.mPageContentHeight * widthMils) + 0.5f);
        int i2 = (width - ((this.mPageContentWidth * iMin) + i)) / 2;
        int iMax = ((this.mPageContentHeight + this.mFooterHeight) + this.mPreviewListPadding) + (this.mPreviewPageMargin * 2) > height ? Math.max(0, (((height - this.mPageContentHeight) - this.mFooterHeight) / 2) - this.mPreviewPageMargin) : Math.max(this.mPreviewListPadding, (height - (((this.mSelectedPageCount / iMin) + (this.mSelectedPageCount % iMin > 0 ? 1 : 0)) * ((this.mPageContentHeight + this.mFooterHeight) + (this.mPreviewPageMargin * 2)))) / 2);
        this.mPreviewArea.setPadding(i2, iMax, i2, iMax);
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(this.mContext);
        View viewInflate = layoutInflaterFrom.inflate(R.layout.preview_page_loading, (ViewGroup) null, false);
        viewInflate.measure(View.MeasureSpec.makeMeasureSpec(this.mPageContentWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mPageContentHeight, 1073741824));
        viewInflate.layout(0, 0, viewInflate.getMeasuredWidth(), viewInflate.getMeasuredHeight());
        if (this.mPageContentHeight <= 0 || this.mPageContentWidth <= 0) {
            Log.w("PageAdapter", "Unable to create bitmap, height or width smaller than 0!");
            return;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mPageContentWidth, this.mPageContentHeight, Bitmap.Config.ARGB_8888);
        viewInflate.draw(new Canvas(bitmapCreateBitmap));
        this.mEmptyState = new BitmapDrawable(this.mContext.getResources(), bitmapCreateBitmap);
        View viewInflate2 = layoutInflaterFrom.inflate(R.layout.preview_page_error, (ViewGroup) null, false);
        viewInflate2.measure(View.MeasureSpec.makeMeasureSpec(this.mPageContentWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mPageContentHeight, 1073741824));
        viewInflate2.layout(0, 0, viewInflate2.getMeasuredWidth(), viewInflate2.getMeasuredHeight());
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(this.mPageContentWidth, this.mPageContentHeight, Bitmap.Config.ARGB_8888);
        viewInflate2.draw(new Canvas(bitmapCreateBitmap2));
        this.mErrorState = new BitmapDrawable(this.mContext.getResources(), bitmapCreateBitmap2);
    }

    private PageRange[] computeSelectedPages() {
        ArrayList arrayList = new ArrayList();
        int size = this.mConfirmedPagesInDocument.size();
        int i = 0;
        int i2 = -1;
        int i3 = -1;
        while (i < size) {
            int iKeyAt = this.mConfirmedPagesInDocument.keyAt(i);
            if (i2 == -1) {
                i2 = iKeyAt;
                i3 = i2;
            }
            if (i3 + 1 < iKeyAt) {
                arrayList.add(new PageRange(i2, i3));
                i2 = iKeyAt;
            }
            i++;
            i3 = iKeyAt;
        }
        if (i2 != -1 && i3 != -1) {
            arrayList.add(new PageRange(i2, i3));
        }
        PageRange[] pageRangeArr = new PageRange[arrayList.size()];
        arrayList.toArray(pageRangeArr);
        return pageRangeArr;
    }

    public void destroy(Runnable runnable) {
        this.mCloseGuard.close();
        this.mState = 2;
        this.mPageContentRepository.destroy(runnable);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            if (this.mState != 2) {
                destroy(null);
            }
        } finally {
            super.finalize();
        }
    }

    private int computePageIndexInDocument(int i) {
        int length = this.mSelectedPages.length;
        int size = 0;
        for (int i2 = 0; i2 < length; i2++) {
            PageRange pageRangeAsAbsoluteRange = PageRangeUtils.asAbsoluteRange(this.mSelectedPages[i2], this.mDocumentPageCount);
            size += pageRangeAsAbsoluteRange.getSize();
            if (size > i) {
                return pageRangeAsAbsoluteRange.getEnd() - ((size - i) - 1);
            }
        }
        return -1;
    }

    private int computePageIndexInFile(int i) {
        if (!PageRangeUtils.contains(this.mSelectedPages, i) || this.mWrittenPages == null) {
            return -1;
        }
        int length = this.mWrittenPages.length;
        int size = -1;
        for (int i2 = 0; i2 < length; i2++) {
            PageRange pageRange = this.mWrittenPages[i2];
            if (!pageRange.contains(i)) {
                size += pageRange.getSize();
            } else {
                return size + (i - pageRange.getStart()) + 1;
            }
        }
        return -1;
    }

    private void setConfirmedPages(PageRange[] pageRangeArr, int i) {
        this.mConfirmedPagesInDocument.clear();
        for (PageRange pageRange : pageRangeArr) {
            PageRange pageRangeAsAbsoluteRange = PageRangeUtils.asAbsoluteRange(pageRange, i);
            for (int start = pageRangeAsAbsoluteRange.getStart(); start <= pageRangeAsAbsoluteRange.getEnd(); start++) {
                this.mConfirmedPagesInDocument.put(start, null);
            }
        }
    }

    private void onSelectedPageNotInFile(int i) {
        PageRange[] pageRangeArrComputeRequestedPages = computeRequestedPages(i);
        if (!Arrays.equals(this.mRequestedPages, pageRangeArrComputeRequestedPages)) {
            this.mRequestedPages = pageRangeArrComputeRequestedPages;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    PageAdapter.this.mCallbacks.onRequestContentUpdate();
                }
            });
        }
    }

    private PageRange[] computeRequestedPages(int i) {
        int iMin;
        PageRange pageRange;
        int iMax;
        PageRange pageRange2;
        if (this.mRequestedPages != null && PageRangeUtils.contains(this.mRequestedPages, i)) {
            return this.mRequestedPages;
        }
        ArrayList arrayList = new ArrayList();
        int length = this.mSelectedPages.length;
        PageRange[] pageRangeArrComputeBoundPagesInDocument = computeBoundPagesInDocument();
        for (PageRange pageRange3 : pageRangeArrComputeBoundPagesInDocument) {
            arrayList.add(pageRange3);
        }
        int normalizedPageCount = 50 - PageRangeUtils.getNormalizedPageCount(pageRangeArrComputeBoundPagesInDocument, this.mDocumentPageCount);
        if (!(this.mRequestedPages == null || i > this.mRequestedPages[this.mRequestedPages.length - 1].getEnd())) {
            for (int i2 = length - 1; i2 >= 0 && normalizedPageCount > 0; i2--) {
                PageRange pageRangeAsAbsoluteRange = PageRangeUtils.asAbsoluteRange(this.mSelectedPages[i2], this.mDocumentPageCount);
                if (i >= pageRangeAsAbsoluteRange.getStart()) {
                    if (pageRangeAsAbsoluteRange.contains(i)) {
                        int iMin2 = Math.min((i - pageRangeAsAbsoluteRange.getStart()) + 1, normalizedPageCount);
                        int iMax2 = Math.max((i - iMin2) - 1, 0);
                        iMax = Math.max(iMin2, 0);
                        pageRange2 = new PageRange(iMax2, i);
                    } else {
                        int iMax3 = Math.max(Math.min(pageRangeAsAbsoluteRange.getSize(), normalizedPageCount), 0);
                        PageRange pageRange4 = new PageRange(Math.max((pageRangeAsAbsoluteRange.getEnd() - iMax3) - 1, 0), pageRangeAsAbsoluteRange.getEnd());
                        iMax = iMax3;
                        pageRange2 = pageRange4;
                    }
                    arrayList.add(pageRange2);
                    normalizedPageCount -= iMax;
                }
            }
        } else {
            for (int i3 = 0; i3 < length && normalizedPageCount > 0; i3++) {
                PageRange pageRangeAsAbsoluteRange2 = PageRangeUtils.asAbsoluteRange(this.mSelectedPages[i3], this.mDocumentPageCount);
                if (i <= pageRangeAsAbsoluteRange2.getEnd()) {
                    if (pageRangeAsAbsoluteRange2.contains(i)) {
                        iMin = Math.min((pageRangeAsAbsoluteRange2.getEnd() - i) + 1, normalizedPageCount);
                        pageRange = new PageRange(i, Math.min((i + iMin) - 1, this.mDocumentPageCount - 1));
                    } else {
                        int iMin3 = Math.min(pageRangeAsAbsoluteRange2.getSize(), normalizedPageCount);
                        PageRange pageRange5 = new PageRange(pageRangeAsAbsoluteRange2.getStart(), Math.min((pageRangeAsAbsoluteRange2.getStart() + iMin3) - 1, this.mDocumentPageCount - 1));
                        iMin = iMin3;
                        pageRange = pageRange5;
                    }
                    arrayList.add(pageRange);
                    normalizedPageCount -= iMin;
                }
            }
        }
        PageRange[] pageRangeArr = new PageRange[arrayList.size()];
        arrayList.toArray(pageRangeArr);
        return PageRangeUtils.normalize(pageRangeArr);
    }

    private PageRange[] computeBoundPagesInDocument() {
        ArrayList arrayList = new ArrayList();
        int size = this.mBoundPagesInAdapter.size();
        int i = 0;
        int i2 = -1;
        int i3 = -1;
        while (i < size) {
            int iComputePageIndexInDocument = computePageIndexInDocument(this.mBoundPagesInAdapter.keyAt(i));
            if (i2 == -1) {
                i2 = iComputePageIndexInDocument;
            }
            if (i3 == -1) {
                i3 = iComputePageIndexInDocument;
            }
            if (iComputePageIndexInDocument > i3 + 1) {
                arrayList.add(new PageRange(i2, i3));
                i2 = iComputePageIndexInDocument;
            }
            i++;
            i3 = iComputePageIndexInDocument;
        }
        if (i2 != -1 && i3 != -1) {
            arrayList.add(new PageRange(i2, i3));
        }
        PageRange[] pageRangeArr = new PageRange[arrayList.size()];
        arrayList.toArray(pageRangeArr);
        return pageRangeArr;
    }

    private void recyclePageView(PageContentView pageContentView, int i) {
        PageContentRepository.PageContentProvider pageContentProvider = pageContentView.getPageContentProvider();
        if (pageContentProvider != null) {
            pageContentView.init(null, this.mEmptyState, this.mErrorState, this.mMediaSize, this.mMinMargins);
            this.mPageContentRepository.releasePageContentProvider(pageContentProvider);
        }
        this.mBoundPagesInAdapter.remove(i);
        pageContentView.setTag(null);
    }

    void startPreloadContent(PageRange pageRange) {
        int iComputePageIndexInDocument = computePageIndexInDocument(pageRange.getStart());
        int iComputePageIndexInDocument2 = computePageIndexInDocument(pageRange.getEnd());
        if (iComputePageIndexInDocument == -1 || iComputePageIndexInDocument2 == -1) {
            return;
        }
        this.mPageContentRepository.startPreload(new PageRange(iComputePageIndexInDocument, iComputePageIndexInDocument2), this.mSelectedPages, this.mWrittenPages);
    }

    public void stopPreloadContent() {
        this.mPageContentRepository.stopPreload();
    }

    private void throwIfNotOpened() {
        if (this.mState != 1) {
            throw new IllegalStateException("Not opened");
        }
    }

    private void throwIfNotClosed() {
        if (this.mState != 0) {
            throw new IllegalStateException("Not closed");
        }
    }

    private final class MyViewHolder extends RecyclerView.ViewHolder {
        int mPageInAdapter;

        private MyViewHolder(View view) {
            super(view);
        }
    }

    private final class PageClickListener implements View.OnClickListener {
        private PageClickListener() {
        }

        @Override
        public void onClick(View view) {
            int i = ((MyViewHolder) ((PreviewPageFrame) view).getTag()).mPageInAdapter;
            int iComputePageIndexInDocument = PageAdapter.this.computePageIndexInDocument(i);
            if (PageAdapter.this.mConfirmedPagesInDocument.indexOfKey(iComputePageIndexInDocument) < 0) {
                PageAdapter.this.mConfirmedPagesInDocument.put(iComputePageIndexInDocument, null);
            } else if (PageAdapter.this.mConfirmedPagesInDocument.size() > 1) {
                PageAdapter.this.mConfirmedPagesInDocument.remove(iComputePageIndexInDocument);
            } else {
                return;
            }
            PageAdapter.this.notifyItemChanged(i);
        }
    }
}
