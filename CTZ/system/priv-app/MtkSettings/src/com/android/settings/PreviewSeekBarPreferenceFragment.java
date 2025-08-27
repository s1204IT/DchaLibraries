package com.android.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.widget.DotsPageIndicator;
import com.android.settings.widget.LabeledSeekBar;

/* loaded from: classes.dex */
public abstract class PreviewSeekBarPreferenceFragment extends SettingsPreferenceFragment {
    protected int mActivityLayoutResId;
    protected int mCurrentIndex;
    protected String[] mEntries;
    protected int mInitialIndex;
    private TextView mLabel;
    private View mLarger;
    private DotsPageIndicator mPageIndicator;
    private ViewPager mPreviewPager;
    private PreviewPagerAdapter mPreviewPagerAdapter;
    protected int[] mPreviewSampleResIds;
    private LabeledSeekBar mSeekBar;
    private View mSmaller;
    private ViewPager.OnPageChangeListener mPreviewPageChangeListener = new ViewPager.OnPageChangeListener() { // from class: com.android.settings.PreviewSeekBarPreferenceFragment.3
        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int i) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageSelected(int i) {
            PreviewSeekBarPreferenceFragment.this.mPreviewPager.sendAccessibilityEvent(16384);
        }
    };
    private ViewPager.OnPageChangeListener mPageIndicatorPageChangeListener = new ViewPager.OnPageChangeListener() { // from class: com.android.settings.PreviewSeekBarPreferenceFragment.4
        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int i) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageSelected(int i) {
            PreviewSeekBarPreferenceFragment.this.setPagerIndicatorContentDescription(i);
        }
    };

    protected abstract void commit();

    protected abstract Configuration createConfig(Configuration configuration, int i);

    private class onPreviewSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private boolean mSeekByTouch;

        private onPreviewSeekBarChangeListener() {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            PreviewSeekBarPreferenceFragment.this.setPreviewLayer(i, false);
            if (!this.mSeekByTouch) {
                PreviewSeekBarPreferenceFragment.this.commit();
            }
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
            this.mSeekByTouch = true;
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (PreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.isAnimating()) {
                PreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.setAnimationEndAction(new Runnable() { // from class: com.android.settings.PreviewSeekBarPreferenceFragment.onPreviewSeekBarChangeListener.1
                    @Override // java.lang.Runnable
                    public void run() {
                        PreviewSeekBarPreferenceFragment.this.commit();
                    }
                });
            } else {
                PreviewSeekBarPreferenceFragment.this.commit();
            }
            this.mSeekByTouch = false;
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) throws Resources.NotFoundException {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        ViewGroup viewGroup2 = (ViewGroup) viewOnCreateView.findViewById(android.R.id.list_container);
        viewGroup2.removeAllViews();
        View viewInflate = layoutInflater.inflate(this.mActivityLayoutResId, viewGroup2, false);
        viewGroup2.addView(viewInflate);
        this.mLabel = (TextView) viewInflate.findViewById(R.id.current_label);
        int iMax = Math.max(1, this.mEntries.length - 1);
        this.mSeekBar = (LabeledSeekBar) viewInflate.findViewById(R.id.seek_bar);
        this.mSeekBar.setLabels(this.mEntries);
        this.mSeekBar.setMax(iMax);
        this.mSmaller = viewInflate.findViewById(R.id.smaller);
        this.mSmaller.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.PreviewSeekBarPreferenceFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                int progress = PreviewSeekBarPreferenceFragment.this.mSeekBar.getProgress();
                if (progress > 0) {
                    PreviewSeekBarPreferenceFragment.this.mSeekBar.setProgress(progress - 1, true);
                }
            }
        });
        this.mLarger = viewInflate.findViewById(R.id.larger);
        this.mLarger.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.PreviewSeekBarPreferenceFragment.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                int progress = PreviewSeekBarPreferenceFragment.this.mSeekBar.getProgress();
                if (progress < PreviewSeekBarPreferenceFragment.this.mSeekBar.getMax()) {
                    PreviewSeekBarPreferenceFragment.this.mSeekBar.setProgress(progress + 1, true);
                }
            }
        });
        if (this.mEntries.length == 1) {
            this.mSeekBar.setEnabled(false);
        }
        Context context = getContext();
        Configuration configuration = context.getResources().getConfiguration();
        boolean z = configuration.getLayoutDirection() == 1;
        Configuration[] configurationArr = new Configuration[this.mEntries.length];
        for (int i = 0; i < this.mEntries.length; i++) {
            configurationArr[i] = createConfig(configuration, i);
        }
        this.mPreviewPager = (ViewPager) viewInflate.findViewById(R.id.preview_pager);
        this.mPreviewPagerAdapter = new PreviewPagerAdapter(context, z, this.mPreviewSampleResIds, configurationArr);
        this.mPreviewPager.setAdapter(this.mPreviewPagerAdapter);
        this.mPreviewPager.setCurrentItem(z ? this.mPreviewSampleResIds.length - 1 : 0);
        this.mPreviewPager.addOnPageChangeListener(this.mPreviewPageChangeListener);
        this.mPageIndicator = (DotsPageIndicator) viewInflate.findViewById(R.id.page_indicator);
        if (this.mPreviewSampleResIds.length > 1) {
            this.mPageIndicator.setViewPager(this.mPreviewPager);
            this.mPageIndicator.setVisibility(0);
            this.mPageIndicator.setOnPageChangeListener(this.mPageIndicatorPageChangeListener);
        } else {
            this.mPageIndicator.setVisibility(8);
        }
        setPreviewLayer(this.mInitialIndex, false);
        return viewOnCreateView;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
        this.mSeekBar.setProgress(this.mCurrentIndex);
        this.mSeekBar.setOnSeekBarChangeListener(new onPreviewSeekBarChangeListener());
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        super.onStop();
        this.mSeekBar.setOnSeekBarChangeListener(null);
    }

    private void setPreviewLayer(int i, boolean z) {
        this.mLabel.setText(this.mEntries[i]);
        this.mSmaller.setEnabled(i > 0);
        this.mLarger.setEnabled(i < this.mEntries.length - 1);
        setPagerIndicatorContentDescription(this.mPreviewPager.getCurrentItem());
        this.mPreviewPagerAdapter.setPreviewLayer(i, this.mCurrentIndex, this.mPreviewPager.getCurrentItem(), z);
        this.mCurrentIndex = i;
    }

    private void setPagerIndicatorContentDescription(int i) {
        this.mPageIndicator.setContentDescription(getPrefContext().getString(R.string.preview_page_indicator_content_description, Integer.valueOf(i + 1), Integer.valueOf(this.mPreviewSampleResIds.length)));
    }
}
