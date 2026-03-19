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
    private Drawable mErrorState;
    private boolean mIsFailed;
    private PrintAttributes.MediaSize mMediaSize;
    private PrintAttributes.Margins mMinMargins;
    private PageContentRepository.PageContentProvider mProvider;

    public PageContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mContentRequested = false;
        requestPageContentIfNeeded();
    }

    @Override
    public void onPageContentAvailable(BitmapDrawable bitmapDrawable) {
        this.mIsFailed = bitmapDrawable == null;
        if (this.mIsFailed) {
            setBackground(this.mErrorState);
        } else {
            setBackground(bitmapDrawable);
        }
    }

    public PageContentRepository.PageContentProvider getPageContentProvider() {
        return this.mProvider;
    }

    public void init(PageContentRepository.PageContentProvider pageContentProvider, Drawable drawable, Drawable drawable2, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins margins) {
        boolean z = true;
        boolean z2 = this.mProvider != null ? !this.mProvider.equals(pageContentProvider) : pageContentProvider != null;
        boolean z3 = this.mEmptyState != null ? !this.mEmptyState.equals(drawable) : drawable != null;
        boolean z4 = this.mMediaSize != null ? !this.mMediaSize.equals(mediaSize) : mediaSize != null;
        if (this.mMinMargins != null ? this.mMinMargins.equals(margins) : margins == null) {
            z = false;
        }
        if (!z2 && !z4 && !z && !z3) {
            return;
        }
        this.mIsFailed = false;
        this.mProvider = pageContentProvider;
        this.mMediaSize = mediaSize;
        this.mMinMargins = margins;
        this.mEmptyState = drawable;
        this.mErrorState = drawable2;
        this.mContentRequested = false;
        if (this.mProvider == null) {
            setBackground(this.mEmptyState);
        } else if (this.mIsFailed) {
            setBackground(this.mErrorState);
        }
        requestPageContentIfNeeded();
    }

    private void requestPageContentIfNeeded() {
        if (getWidth() > 0 && getHeight() > 0 && !this.mContentRequested && this.mProvider != null) {
            this.mContentRequested = true;
            this.mProvider.getPageContent(new PageContentRepository.RenderSpec(getWidth(), getHeight(), this.mMediaSize, this.mMinMargins), this);
        }
    }
}
