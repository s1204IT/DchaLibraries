package com.mediatek.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.PreviewPagerAdapter;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.DotsPageIndicator;
import com.android.settings.widget.LabeledSeekBar;
import com.mediatek.settings.accessibility.CustomToggleFontSizePreferenceFragment;

public abstract class CustomPreviewSeekBarPreferenceFragment extends SettingsPreferenceFragment {
    protected int mActivityLayoutResId;
    protected int mCurrentIndex;
    private AlertDialog mDialog;
    private CheckBox mDontShowAgain;
    protected String[] mEntries;
    protected int mInitialIndex;
    private TextView mLabel;
    private View mLarger;
    private DotsPageIndicator mPageIndicator;
    private ViewPager mPreviewPager;
    private PreviewPagerAdapter mPreviewPagerAdapter;
    protected int[] mPreviewSampleResIds;
    private boolean mResponse;
    private LabeledSeekBar mSeekBar;
    private View mSmaller;
    private int mOldProgress = 2;
    private ViewPager.OnPageChangeListener mPreviewPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int i) {
        }

        @Override
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override
        public void onPageSelected(int i) {
            CustomPreviewSeekBarPreferenceFragment.this.mPreviewPager.sendAccessibilityEvent(16384);
        }
    };
    private ViewPager.OnPageChangeListener mPageIndicatorPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int i) {
        }

        @Override
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override
        public void onPageSelected(int i) {
            CustomPreviewSeekBarPreferenceFragment.this.setPagerIndicatorContentDescription(i);
        }
    };

    protected abstract void commit();

    protected abstract Configuration createConfig(Configuration configuration, int i);

    private class onPreviewSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private Context context;
        private boolean mSeekByTouch;

        private onPreviewSeekBarChangeListener() {
            this.context = CustomPreviewSeekBarPreferenceFragment.this.getPrefContext();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            String string = this.context.getSharedPreferences("check_box_pref", 0).getString("skipMessage", "NOT checked");
            Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onProgressChanged progress: " + i + "fromUser" + z);
            if (!string.equals("NOT checked") || (i != 0 && i != 4)) {
                CustomPreviewSeekBarPreferenceFragment.this.setPreviewLayer(i, true);
                if (!this.mSeekByTouch) {
                    CustomPreviewSeekBarPreferenceFragment.this.commit();
                    return;
                }
                return;
            }
            showDialog(CustomPreviewSeekBarPreferenceFragment.this.getActivity(), seekBar, i);
        }

        public String[] getFontEntryValues() {
            return this.context.getResources().getStringArray(R.array.custom_entryvalues_font_size);
        }

        private void showDialog(final Activity activity, final SeekBar seekBar, final int i) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            String string = this.context.getSharedPreferences("check_box_pref", 0).getString("skipMessage", "NOT checked");
            Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_ShowDialog skip checkbox value from SharedPref is " + string);
            CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain = new CheckBox(this.context);
            CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.setText(this.context.getString(R.string.do_not_show));
            float f = this.context.getResources().getDisplayMetrics().density;
            int i2 = (int) (5.0f * f);
            builder.setView(CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain, (int) (19.0f * f), i2, (int) (14.0f * f), i2);
            if (i == 4) {
                builder.setMessage(this.context.getString(R.string.large_font_warning));
            } else {
                builder.setMessage(this.context.getString(R.string.small_font_warning));
            }
            builder.setTitle(this.context.getString(R.string.warning_dialog_title));
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    CustomPreviewSeekBarPreferenceFragment.this.mResponse = false;
                    String str = "NOT checked";
                    if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.isChecked()) {
                        str = "checked";
                    }
                    SharedPreferences.Editor editorEdit = onPreviewSeekBarChangeListener.this.context.getSharedPreferences("check_box_pref", 0).edit();
                    editorEdit.putString("skipMessage", str);
                    editorEdit.commit();
                    CustomPreviewSeekBarPreferenceFragment.this.mOldProgress = CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(Settings.System.getFloat(onPreviewSeekBarChangeListener.this.context.getContentResolver(), "font_scale", 1.0f), onPreviewSeekBarChangeListener.this.getFontEntryValues());
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onCancel mOldProgress: " + CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    seekBar.setProgress(CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    Intent intent = activity.getIntent();
                    activity.finish();
                    activity.startActivity(intent);
                }
            });
            builder.setPositiveButton(this.context.getString(R.string.positive_button_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i3) {
                    CustomPreviewSeekBarPreferenceFragment.this.mResponse = true;
                    String str = "NOT checked";
                    if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.isChecked()) {
                        str = "checked";
                    }
                    SharedPreferences.Editor editorEdit = onPreviewSeekBarChangeListener.this.context.getSharedPreferences("check_box_pref", 0).edit();
                    editorEdit.putString("skipMessage", str);
                    editorEdit.commit();
                    CustomPreviewSeekBarPreferenceFragment.this.commit();
                    CustomPreviewSeekBarPreferenceFragment.this.mOldProgress = CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(Settings.System.getFloat(onPreviewSeekBarChangeListener.this.context.getContentResolver(), "font_scale", 1.0f), onPreviewSeekBarChangeListener.this.getFontEntryValues());
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onPositiveClick mOldProgress: " + CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    CustomPreviewSeekBarPreferenceFragment.this.setPreviewLayer(i, true);
                    if (!onPreviewSeekBarChangeListener.this.mSeekByTouch) {
                        CustomPreviewSeekBarPreferenceFragment.this.commit();
                    }
                }
            });
            builder.setNegativeButton(this.context.getString(R.string.negative_button_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i3) {
                    CustomPreviewSeekBarPreferenceFragment.this.mResponse = false;
                    String str = "NOT checked";
                    if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.isChecked()) {
                        str = "checked";
                    }
                    SharedPreferences.Editor editorEdit = onPreviewSeekBarChangeListener.this.context.getSharedPreferences("check_box_pref", 0).edit();
                    editorEdit.putString("skipMessage", str);
                    editorEdit.commit();
                    CustomPreviewSeekBarPreferenceFragment.this.mOldProgress = CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(Settings.System.getFloat(onPreviewSeekBarChangeListener.this.context.getContentResolver(), "font_scale", 1.0f), onPreviewSeekBarChangeListener.this.getFontEntryValues());
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onNegativeClick mOldProgress: " + CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    seekBar.setProgress(CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    Intent intent = activity.getIntent();
                    activity.finish();
                    activity.startActivity(intent);
                }
            });
            if (string.equals("checked") || CustomPreviewSeekBarPreferenceFragment.this.mOldProgress == i) {
                CustomPreviewSeekBarPreferenceFragment.this.mResponse = true;
            } else {
                CustomPreviewSeekBarPreferenceFragment.this.mDialog = builder.show();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            this.mSeekByTouch = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (CustomPreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.isAnimating()) {
                CustomPreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.setAnimationEndAction(new Runnable() {
                    @Override
                    public void run() {
                        CustomPreviewSeekBarPreferenceFragment.this.commit();
                    }
                });
            } else {
                CustomPreviewSeekBarPreferenceFragment.this.commit();
            }
            this.mSeekByTouch = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
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
        this.mSmaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int progress = CustomPreviewSeekBarPreferenceFragment.this.mSeekBar.getProgress();
                if (progress > 0) {
                    CustomPreviewSeekBarPreferenceFragment.this.mSeekBar.setProgress(progress - 1, true);
                }
            }
        });
        this.mLarger = viewInflate.findViewById(R.id.larger);
        this.mLarger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int progress = CustomPreviewSeekBarPreferenceFragment.this.mSeekBar.getProgress();
                if (progress < CustomPreviewSeekBarPreferenceFragment.this.mSeekBar.getMax()) {
                    CustomPreviewSeekBarPreferenceFragment.this.mSeekBar.setProgress(progress + 1, true);
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

    @Override
    public void onStart() {
        super.onStart();
        this.mSeekBar.setProgress(this.mCurrentIndex);
        this.mSeekBar.setOnSeekBarChangeListener(new onPreviewSeekBarChangeListener());
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mSeekBar.setOnSeekBarChangeListener(null);
    }

    private void setPreviewLayer(int i, boolean z) {
        Log.d("CustomPreviewSeekBarPreferenceFragment", "setPreviewLayer mCurrentIndex: " + this.mCurrentIndex + "newIndex" + i);
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
