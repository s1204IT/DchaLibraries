package com.android.printspooler.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.print.PrintAttributes;
import android.util.AttributeSet;
import android.view.View;
import com.android.printspooler.model.PageContentRepository;

public class PageContentView extends View implements PageContentRepository.OnPageContentAvailableCallback {
    private boolean mContentRequested;
    private Drawable mEmptyState;
    private PrintAttributes.MediaSize mMediaSize;
    private PrintAttributes.Margins mMinMargins;
    private PageContentRepository.PageContentProvider mProvider;

    public PageContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mContentRequested = false;
        requestPageContentIfNeeded();
    }

    @Override
    public void onPageContentAvailable(BitmapDrawable content) {
        setBackground(content);
    }

    public PageContentRepository.PageContentProvider getPageContentProvider() {
        return this.mProvider;
    }

    public void init(PageContentRepository.PageContentProvider provider, Drawable emptyState, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins minMargins) {
        boolean providerChanged = this.mProvider == null ? provider != null : !this.mProvider.equals(provider);
        boolean loadingDrawableChanged = this.mEmptyState == null ? emptyState != null : !this.mEmptyState.equals(emptyState);
        boolean mediaSizeChanged = this.mMediaSize == null ? mediaSize != null : !this.mMediaSize.equals(mediaSize);
        boolean marginsChanged = this.mMinMargins == null ? minMargins != null : !this.mMinMargins.equals(minMargins);
        if (providerChanged || mediaSizeChanged || marginsChanged || loadingDrawableChanged) {
            this.mProvider = provider;
            this.mMediaSize = mediaSize;
            this.mMinMargins = minMargins;
            this.mEmptyState = emptyState;
            this.mContentRequested = false;
            if (this.mProvider == null && getBackground() != this.mEmptyState) {
                setBackground(this.mEmptyState);
            }
            requestPageContentIfNeeded();
        }
    }

    private void requestPageContentIfNeeded() {
        if (getWidth() > 0 && getHeight() > 0 && !this.mContentRequested && this.mProvider != null) {
            this.mContentRequested = true;
            this.mProvider.getPageContent(new PageContentRepository.RenderSpec(getWidth(), getHeight(), this.mMediaSize, this.mMinMargins), this);
        }
    }
}
