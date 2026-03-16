package com.android.printspooler.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.support.v7.widget.RecyclerView;
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
import java.util.List;

public final class PageAdapter extends RecyclerView.Adapter {
    private static final PageRange[] ALL_PAGES_ARRAY = {PageRange.ALL_PAGES};
    private final ContentCallbacks mCallbacks;
    private int mColumnCount;
    private final Context mContext;
    private BitmapDrawable mEmptyState;
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

    public PageAdapter(Context context, ContentCallbacks callbacks, PreviewArea previewArea) {
        this.mContext = context;
        this.mCallbacks = callbacks;
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

    public void open(ParcelFileDescriptor source, final Runnable callback) {
        throwIfNotClosed();
        this.mState = 1;
        this.mPageContentRepository.open(source, new OpenDocumentCallback() {
            @Override
            public void onSuccess() {
                PageAdapter.this.notifyDataSetChanged();
                callback.run();
            }

            @Override
            public void onFailure(int error) {
                switch (error) {
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

    public void update(PageRange[] writtenPages, PageRange[] selectedPages, int documentPageCount, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins minMargins) {
        boolean documentChanged = false;
        boolean updatePreviewAreaAndPageSize = false;
        if (documentPageCount == -1) {
            if (writtenPages == null) {
                if (!Arrays.equals(ALL_PAGES_ARRAY, this.mRequestedPages)) {
                    this.mRequestedPages = ALL_PAGES_ARRAY;
                    this.mCallbacks.onRequestContentUpdate();
                    return;
                }
                return;
            }
            documentPageCount = this.mPageContentRepository.getFilePageCount();
            if (documentPageCount <= 0) {
                return;
            }
        }
        if (!Arrays.equals(this.mSelectedPages, selectedPages)) {
            this.mSelectedPages = selectedPages;
            this.mSelectedPageCount = PageRangeUtils.getNormalizedPageCount(this.mSelectedPages, documentPageCount);
            setConfirmedPages(this.mSelectedPages, documentPageCount);
            updatePreviewAreaAndPageSize = true;
            documentChanged = true;
        }
        if (this.mDocumentPageCount != documentPageCount) {
            this.mDocumentPageCount = documentPageCount;
            documentChanged = true;
        }
        if (this.mMediaSize == null || !this.mMediaSize.equals(mediaSize)) {
            this.mMediaSize = mediaSize;
            updatePreviewAreaAndPageSize = true;
            documentChanged = true;
        }
        if (this.mMinMargins == null || !this.mMinMargins.equals(minMargins)) {
            this.mMinMargins = minMargins;
            updatePreviewAreaAndPageSize = true;
            documentChanged = true;
        }
        if (writtenPages != null) {
            if (PageRangeUtils.isAllPages(writtenPages)) {
                writtenPages = this.mRequestedPages;
            }
            if (!Arrays.equals(this.mWrittenPages, writtenPages)) {
                this.mWrittenPages = writtenPages;
                documentChanged = true;
            }
        }
        if (updatePreviewAreaAndPageSize) {
            updatePreviewAreaPageSizeAndEmptyState();
        }
        if (documentChanged) {
            notifyDataSetChanged();
        }
    }

    public void close(Runnable callback) {
        throwIfNotOpened();
        this.mState = 0;
        this.mPageContentRepository.close(callback);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View page = this.mLayoutInflater.inflate(R.layout.preview_page, parent, false);
        return new MyViewHolder(page);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder) holder;
        PreviewPageFrame page = (PreviewPageFrame) holder.itemView;
        page.setOnClickListener(this.mPageClickListener);
        page.setTag(holder);
        myHolder.mPageInAdapter = position;
        int pageInDocument = computePageIndexInDocument(position);
        int pageIndexInFile = computePageIndexInFile(pageInDocument);
        PageContentView content = (PageContentView) page.findViewById(R.id.page_content);
        ViewGroup.LayoutParams params = content.getLayoutParams();
        params.width = this.mPageContentWidth;
        params.height = this.mPageContentHeight;
        PageContentRepository.PageContentProvider provider = content.getPageContentProvider();
        if (pageIndexInFile != -1) {
            provider = this.mPageContentRepository.acquirePageContentProvider(pageIndexInFile, content);
            this.mBoundPagesInAdapter.put(position, null);
        } else {
            onSelectedPageNotInFile(pageInDocument);
        }
        content.init(provider, this.mEmptyState, this.mMediaSize, this.mMinMargins);
        if (this.mConfirmedPagesInDocument.indexOfKey(pageInDocument) >= 0) {
            page.setSelected(true, false);
        } else {
            page.setSelected(false, false);
        }
        page.setContentDescription(this.mContext.getString(R.string.page_description_template, Integer.valueOf(pageInDocument + 1), Integer.valueOf(this.mDocumentPageCount)));
        TextView pageNumberView = (TextView) page.findViewById(R.id.page_number);
        String text = this.mContext.getString(R.string.current_page_template, Integer.valueOf(pageInDocument + 1), Integer.valueOf(this.mDocumentPageCount));
        pageNumberView.setText(text);
    }

    @Override
    public int getItemCount() {
        return this.mSelectedPageCount;
    }

    @Override
    public long getItemId(int position) {
        return computePageIndexInDocument(position);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        MyViewHolder myHolder = (MyViewHolder) holder;
        PageContentView content = (PageContentView) holder.itemView.findViewById(R.id.page_content);
        recyclePageView(content, myHolder.mPageInAdapter);
        myHolder.mPageInAdapter = -1;
    }

    public PageRange[] getRequestedPages() {
        return this.mRequestedPages;
    }

    public PageRange[] getSelectedPages() {
        PageRange[] selectedPages = computeSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, selectedPages)) {
            this.mSelectedPages = selectedPages;
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
        int verticalPadding;
        if (this.mMediaSize != null) {
            int availableWidth = this.mPreviewArea.getWidth();
            int availableHeight = this.mPreviewArea.getHeight();
            float pageAspectRatio = this.mMediaSize.getWidthMils() / this.mMediaSize.getHeightMils();
            int columnCount = Math.min(this.mSelectedPageCount, this.mColumnCount);
            this.mPreviewArea.setColumnCount(columnCount);
            int horizontalMargins = columnCount * 2 * this.mPreviewPageMargin;
            int horizontalPaddingAndMargins = horizontalMargins + (this.mPreviewListPadding * 2);
            int pageContentDesiredWidth = (int) (((availableWidth - horizontalPaddingAndMargins) / columnCount) + 0.5f);
            int pageContentDesiredHeight = (int) ((pageContentDesiredWidth / pageAspectRatio) + 0.5f);
            int pageContentMinHeight = (int) ((this.mPreviewPageMinWidth / pageAspectRatio) + 0.5f);
            int pageContentMaxHeight = Math.max(pageContentMinHeight, (availableHeight - ((this.mPreviewListPadding + this.mPreviewPageMargin) * 2)) - this.mFooterHeight);
            this.mPageContentHeight = Math.min(pageContentDesiredHeight, pageContentMaxHeight);
            this.mPageContentWidth = (int) ((this.mPageContentHeight * pageAspectRatio) + 0.5f);
            int totalContentWidth = (this.mPageContentWidth * columnCount) + horizontalMargins;
            int horizontalPadding = (availableWidth - totalContentWidth) / 2;
            int rowCount = (this.mSelectedPageCount / columnCount) + (this.mSelectedPageCount % columnCount > 0 ? 1 : 0);
            int totalContentHeight = rowCount * (this.mPageContentHeight + this.mFooterHeight + (this.mPreviewPageMargin * 2));
            if (this.mPageContentHeight + this.mFooterHeight + this.mPreviewListPadding + (this.mPreviewPageMargin * 2) > availableHeight) {
                verticalPadding = Math.max(0, (((availableHeight - this.mPageContentHeight) - this.mFooterHeight) / 2) - this.mPreviewPageMargin);
            } else {
                verticalPadding = Math.max(this.mPreviewListPadding, (availableHeight - totalContentHeight) / 2);
            }
            this.mPreviewArea.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            View content = inflater.inflate(R.layout.preview_page_loading, (ViewGroup) null, false);
            content.measure(View.MeasureSpec.makeMeasureSpec(this.mPageContentWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mPageContentHeight, 1073741824));
            content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());
            Bitmap bitmap = Bitmap.createBitmap(this.mPageContentWidth, this.mPageContentHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            content.draw(canvas);
            this.mEmptyState = new BitmapDrawable(this.mContext.getResources(), bitmap);
        }
    }

    private PageRange[] computeSelectedPages() {
        ArrayList<PageRange> selectedPagesList = new ArrayList<>();
        int startPageIndex = -1;
        int endPageIndex = -1;
        int pageCount = this.mConfirmedPagesInDocument.size();
        for (int i = 0; i < pageCount; i++) {
            int pageIndex = this.mConfirmedPagesInDocument.keyAt(i);
            if (startPageIndex == -1) {
                endPageIndex = pageIndex;
                startPageIndex = pageIndex;
            }
            if (endPageIndex + 1 < pageIndex) {
                PageRange pageRange = new PageRange(startPageIndex, endPageIndex);
                selectedPagesList.add(pageRange);
                startPageIndex = pageIndex;
            }
            endPageIndex = pageIndex;
        }
        if (startPageIndex != -1 && endPageIndex != -1) {
            PageRange pageRange2 = new PageRange(startPageIndex, endPageIndex);
            selectedPagesList.add(pageRange2);
        }
        PageRange[] selectedPages = new PageRange[selectedPagesList.size()];
        selectedPagesList.toArray(selectedPages);
        return selectedPages;
    }

    public void destroy(Runnable callback) {
        this.mCloseGuard.close();
        this.mState = 2;
        this.mPageContentRepository.destroy(callback);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mState != 2) {
                this.mCloseGuard.warnIfOpen();
                destroy(null);
            }
        } finally {
            super.finalize();
        }
    }

    private int computePageIndexInDocument(int indexInAdapter) {
        int skippedAdapterPages = 0;
        int selectedPagesCount = this.mSelectedPages.length;
        for (int i = 0; i < selectedPagesCount; i++) {
            PageRange pageRange = PageRangeUtils.asAbsoluteRange(this.mSelectedPages[i], this.mDocumentPageCount);
            skippedAdapterPages += pageRange.getSize();
            if (skippedAdapterPages > indexInAdapter) {
                int overshoot = (skippedAdapterPages - indexInAdapter) - 1;
                return pageRange.getEnd() - overshoot;
            }
        }
        return -1;
    }

    private int computePageIndexInFile(int pageIndexInDocument) {
        if (PageRangeUtils.contains(this.mSelectedPages, pageIndexInDocument) && this.mWrittenPages != null) {
            int indexInFile = -1;
            int rangeCount = this.mWrittenPages.length;
            for (int i = 0; i < rangeCount; i++) {
                PageRange pageRange = this.mWrittenPages[i];
                if (!pageRange.contains(pageIndexInDocument)) {
                    indexInFile += pageRange.getSize();
                } else {
                    return indexInFile + (pageIndexInDocument - pageRange.getStart()) + 1;
                }
            }
            return -1;
        }
        return -1;
    }

    private void setConfirmedPages(PageRange[] pagesInDocument, int documentPageCount) {
        this.mConfirmedPagesInDocument.clear();
        for (PageRange pageRange : pagesInDocument) {
            PageRange pageRange2 = PageRangeUtils.asAbsoluteRange(pageRange, documentPageCount);
            for (int j = pageRange2.getStart(); j <= pageRange2.getEnd(); j++) {
                this.mConfirmedPagesInDocument.put(j, null);
            }
        }
    }

    private void onSelectedPageNotInFile(int pageInDocument) {
        PageRange[] requestedPages = computeRequestedPages(pageInDocument);
        if (!Arrays.equals(this.mRequestedPages, requestedPages)) {
            this.mRequestedPages = requestedPages;
            this.mCallbacks.onRequestContentUpdate();
        }
    }

    private PageRange[] computeRequestedPages(int pageInDocument) {
        int rangeSpan;
        PageRange pagesInRange;
        int rangeSpan2;
        PageRange pagesInRange2;
        if (this.mRequestedPages != null && PageRangeUtils.contains(this.mRequestedPages, pageInDocument)) {
            return this.mRequestedPages;
        }
        List<PageRange> pageRangesList = new ArrayList<>();
        int selectedPagesCount = this.mSelectedPages.length;
        PageRange[] boundPagesInDocument = computeBoundPagesInDocument();
        for (PageRange boundRange : boundPagesInDocument) {
            pageRangesList.add(boundRange);
        }
        int remainingPagesToRequest = 50 - PageRangeUtils.getNormalizedPageCount(boundPagesInDocument, this.mDocumentPageCount);
        boolean requestFromStart = this.mRequestedPages == null || pageInDocument > this.mRequestedPages[this.mRequestedPages.length + (-1)].getEnd();
        if (!requestFromStart) {
            for (int i = selectedPagesCount - 1; i >= 0 && remainingPagesToRequest > 0; i--) {
                PageRange selectedRange = PageRangeUtils.asAbsoluteRange(this.mSelectedPages[i], this.mDocumentPageCount);
                if (pageInDocument >= selectedRange.getStart()) {
                    if (selectedRange.contains(pageInDocument)) {
                        int rangeSpan3 = Math.min((pageInDocument - selectedRange.getStart()) + 1, remainingPagesToRequest);
                        int fromPage = Math.max((pageInDocument - rangeSpan3) - 1, 0);
                        rangeSpan2 = Math.max(rangeSpan3, 0);
                        pagesInRange2 = new PageRange(fromPage, pageInDocument);
                    } else {
                        rangeSpan2 = Math.max(Math.min(selectedRange.getSize(), remainingPagesToRequest), 0);
                        int fromPage2 = Math.max((selectedRange.getEnd() - rangeSpan2) - 1, 0);
                        int toPage = selectedRange.getEnd();
                        pagesInRange2 = new PageRange(fromPage2, toPage);
                    }
                    pageRangesList.add(pagesInRange2);
                    remainingPagesToRequest -= rangeSpan2;
                }
            }
        } else {
            for (int i2 = 0; i2 < selectedPagesCount && remainingPagesToRequest > 0; i2++) {
                PageRange selectedRange2 = PageRangeUtils.asAbsoluteRange(this.mSelectedPages[i2], this.mDocumentPageCount);
                if (pageInDocument <= selectedRange2.getEnd()) {
                    if (selectedRange2.contains(pageInDocument)) {
                        rangeSpan = Math.min((selectedRange2.getEnd() - pageInDocument) + 1, remainingPagesToRequest);
                        int toPage2 = Math.min((pageInDocument + rangeSpan) - 1, this.mDocumentPageCount - 1);
                        pagesInRange = new PageRange(pageInDocument, toPage2);
                    } else {
                        rangeSpan = Math.min(selectedRange2.getSize(), remainingPagesToRequest);
                        int fromPage3 = selectedRange2.getStart();
                        int toPage3 = Math.min((selectedRange2.getStart() + rangeSpan) - 1, this.mDocumentPageCount - 1);
                        pagesInRange = new PageRange(fromPage3, toPage3);
                    }
                    pageRangesList.add(pagesInRange);
                    remainingPagesToRequest -= rangeSpan;
                }
            }
        }
        PageRange[] pageRanges = new PageRange[pageRangesList.size()];
        pageRangesList.toArray(pageRanges);
        return PageRangeUtils.normalize(pageRanges);
    }

    private PageRange[] computeBoundPagesInDocument() {
        List<PageRange> pagesInDocumentList = new ArrayList<>();
        int fromPage = -1;
        int toPage = -1;
        int boundPageCount = this.mBoundPagesInAdapter.size();
        for (int i = 0; i < boundPageCount; i++) {
            int boundPageInAdapter = this.mBoundPagesInAdapter.keyAt(i);
            int boundPageInDocument = computePageIndexInDocument(boundPageInAdapter);
            if (fromPage == -1) {
                fromPage = boundPageInDocument;
            }
            if (toPage == -1) {
                toPage = boundPageInDocument;
            }
            if (boundPageInDocument > toPage + 1) {
                PageRange pageRange = new PageRange(fromPage, toPage);
                pagesInDocumentList.add(pageRange);
                toPage = boundPageInDocument;
                fromPage = boundPageInDocument;
            } else {
                toPage = boundPageInDocument;
            }
        }
        if (fromPage != -1 && toPage != -1) {
            PageRange pageRange2 = new PageRange(fromPage, toPage);
            pagesInDocumentList.add(pageRange2);
        }
        PageRange[] pageInDocument = new PageRange[pagesInDocumentList.size()];
        pagesInDocumentList.toArray(pageInDocument);
        return pageInDocument;
    }

    private void recyclePageView(PageContentView page, int pageIndexInAdapter) {
        PageContentRepository.PageContentProvider provider = page.getPageContentProvider();
        if (provider != null) {
            page.init(null, this.mEmptyState, this.mMediaSize, this.mMinMargins);
            this.mPageContentRepository.releasePageContentProvider(provider);
        }
        this.mBoundPagesInAdapter.remove(pageIndexInAdapter);
        page.setTag(null);
    }

    public void startPreloadContent(PageRange pageRangeInAdapter) {
        int startPageInDocument = computePageIndexInDocument(pageRangeInAdapter.getStart());
        int startPageInFile = computePageIndexInFile(startPageInDocument);
        int endPageInDocument = computePageIndexInDocument(pageRangeInAdapter.getEnd());
        int endPageInFile = computePageIndexInFile(endPageInDocument);
        if (startPageInDocument != -1 && endPageInDocument != -1) {
            this.mPageContentRepository.startPreload(startPageInFile, endPageInFile);
        }
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

        private MyViewHolder(View itemView) {
            super(itemView);
        }
    }

    private final class PageClickListener implements View.OnClickListener {
        private PageClickListener() {
        }

        @Override
        public void onClick(View view) {
            PreviewPageFrame page = (PreviewPageFrame) view;
            MyViewHolder holder = (MyViewHolder) page.getTag();
            int pageInAdapter = holder.mPageInAdapter;
            int pageInDocument = PageAdapter.this.computePageIndexInDocument(pageInAdapter);
            if (PageAdapter.this.mConfirmedPagesInDocument.indexOfKey(pageInDocument) < 0) {
                PageAdapter.this.mConfirmedPagesInDocument.put(pageInDocument, null);
                page.setSelected(true, true);
            } else if (PageAdapter.this.mConfirmedPagesInDocument.size() > 1) {
                PageAdapter.this.mConfirmedPagesInDocument.remove(pageInDocument);
                page.setSelected(false, true);
            }
        }
    }
}
