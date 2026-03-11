package com.android.settings;

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
import com.android.settings.accessibility.CustomToggleFontSizePreferenceFragment;
import com.android.settings.widget.DotsPageIndicator;
import com.android.settings.widget.LabeledSeekBar;

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
    private View mSmaller;
    private int mOldProgress = 2;
    private ViewPager.OnPageChangeListener mPreviewPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            CustomPreviewSeekBarPreferenceFragment.this.mPreviewPager.sendAccessibilityEvent(16384);
        }
    };
    private ViewPager.OnPageChangeListener mPageIndicatorPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            CustomPreviewSeekBarPreferenceFragment.this.setPagerIndicatorContentDescription(position);
        }
    };

    protected abstract void commit();

    protected abstract Configuration createConfig(Configuration configuration, int i);

    private class onPreviewSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private Context context;
        private boolean mSeekByTouch;

        onPreviewSeekBarChangeListener(CustomPreviewSeekBarPreferenceFragment this$0, onPreviewSeekBarChangeListener onpreviewseekbarchangelistener) {
            this();
        }

        private onPreviewSeekBarChangeListener() {
            this.context = CustomPreviewSeekBarPreferenceFragment.this.getPrefContext();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            SharedPreferences settings = this.context.getSharedPreferences("check_box_pref", 0);
            String skipMessage = settings.getString("skipMessage", "NOT checked");
            if (skipMessage.equals("NOT checked") && (progress == 0 || progress == 4)) {
                Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onProgressChanged progress: " + progress + "fromUser" + fromUser);
                Activity activity = CustomPreviewSeekBarPreferenceFragment.this.getActivity();
                showDialog(activity, seekBar, progress);
            } else {
                Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onProgressChanged progress: " + progress + "fromUser" + fromUser);
                CustomPreviewSeekBarPreferenceFragment.this.setPreviewLayer(progress, true);
                if (this.mSeekByTouch) {
                    return;
                }
                CustomPreviewSeekBarPreferenceFragment.this.commit();
            }
        }

        public String[] getFontEntryValues() {
            return this.context.getResources().getStringArray(R.array.custom_entryvalues_font_size);
        }

        private void showDialog(final Activity activity, final SeekBar seekBar, final int value) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_ShowDialog");
            SharedPreferences settings = this.context.getSharedPreferences("check_box_pref", 0);
            String skipMessage = settings.getString("skipMessage", "NOT checked");
            Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_ShowDialog skip checkbox value from SharedPref is " + skipMessage);
            CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain = new CheckBox(this.context);
            CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.setText(this.context.getString(R.string.do_not_show));
            float dpi = this.context.getResources().getDisplayMetrics().density;
            if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain == null) {
                Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_mDontShowAgain is null");
            }
            builder.setView(CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain, (int) (19.0f * dpi), (int) (5.0f * dpi), (int) (14.0f * dpi), (int) (5.0f * dpi));
            if (value == 4) {
                builder.setMessage(this.context.getString(R.string.large_font_warning));
            } else {
                builder.setMessage(this.context.getString(R.string.small_font_warning));
            }
            builder.setTitle(this.context.getString(R.string.warning_dialog_title));
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_showDialogonCancel");
                    CustomPreviewSeekBarPreferenceFragment.this.mResponse = false;
                    String checkBoxResult = "NOT checked";
                    if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.isChecked()) {
                        checkBoxResult = "checked";
                    }
                    SharedPreferences settings2 = onPreviewSeekBarChangeListener.this.context.getSharedPreferences("check_box_pref", 0);
                    SharedPreferences.Editor editor = settings2.edit();
                    editor.putString("skipMessage", checkBoxResult);
                    editor.commit();
                    float currentScale = Settings.System.getFloat(onPreviewSeekBarChangeListener.this.context.getContentResolver(), "font_scale", 1.0f);
                    CustomPreviewSeekBarPreferenceFragment.this.mOldProgress = CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale, onPreviewSeekBarChangeListener.this.getFontEntryValues());
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onCancel mOldProgress: " + CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    seekBar.setProgress(CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    Intent intent = activity.getIntent();
                    activity.finish();
                    activity.startActivity(intent);
                }
            });
            builder.setPositiveButton(this.context.getString(R.string.positive_button_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_PositiveButtonClick");
                    CustomPreviewSeekBarPreferenceFragment.this.mResponse = true;
                    String checkBoxResult = "NOT checked";
                    if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.isChecked()) {
                        checkBoxResult = "checked";
                    }
                    SharedPreferences settings2 = onPreviewSeekBarChangeListener.this.context.getSharedPreferences("check_box_pref", 0);
                    SharedPreferences.Editor editor = settings2.edit();
                    editor.putString("skipMessage", checkBoxResult);
                    editor.commit();
                    CustomPreviewSeekBarPreferenceFragment.this.commit();
                    float currentScale = Settings.System.getFloat(onPreviewSeekBarChangeListener.this.context.getContentResolver(), "font_scale", 1.0f);
                    CustomPreviewSeekBarPreferenceFragment.this.mOldProgress = CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale, onPreviewSeekBarChangeListener.this.getFontEntryValues());
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onPositiveClick mOldProgress: " + CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    CustomPreviewSeekBarPreferenceFragment.this.setPreviewLayer(value, true);
                    if (onPreviewSeekBarChangeListener.this.mSeekByTouch) {
                        return;
                    }
                    CustomPreviewSeekBarPreferenceFragment.this.commit();
                }
            });
            builder.setNegativeButton(this.context.getString(R.string.negative_button_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_NegativeButtonClick");
                    CustomPreviewSeekBarPreferenceFragment.this.mResponse = false;
                    String checkBoxResult = "NOT checked";
                    if (CustomPreviewSeekBarPreferenceFragment.this.mDontShowAgain.isChecked()) {
                        checkBoxResult = "checked";
                    }
                    SharedPreferences settings2 = onPreviewSeekBarChangeListener.this.context.getSharedPreferences("check_box_pref", 0);
                    SharedPreferences.Editor editor = settings2.edit();
                    editor.putString("skipMessage", checkBoxResult);
                    editor.commit();
                    float currentScale = Settings.System.getFloat(onPreviewSeekBarChangeListener.this.context.getContentResolver(), "font_scale", 1.0f);
                    CustomPreviewSeekBarPreferenceFragment.this.mOldProgress = CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale, onPreviewSeekBarChangeListener.this.getFontEntryValues());
                    Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onNegativeClick mOldProgress: " + CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    seekBar.setProgress(CustomPreviewSeekBarPreferenceFragment.this.mOldProgress);
                    Intent intent = activity.getIntent();
                    activity.finish();
                    activity.startActivity(intent);
                }
            });
            if (!skipMessage.equals("checked") && CustomPreviewSeekBarPreferenceFragment.this.mOldProgress != value) {
                CustomPreviewSeekBarPreferenceFragment.this.mDialog = builder.show();
            } else {
                CustomPreviewSeekBarPreferenceFragment.this.mResponse = true;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            this.mSeekByTouch = true;
            Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onStartTrackingTouch");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (CustomPreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.isAnimating()) {
                Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onStopTrackingTouch isAnimating: " + CustomPreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.isAnimating());
                CustomPreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.setAnimationEndAction(new Runnable() {
                    @Override
                    public void run() {
                        CustomPreviewSeekBarPreferenceFragment.this.commit();
                    }
                });
            } else {
                Log.d("CustomPreviewSeekBarPreferenceFragment", "@M_onStopTrackingTouch isAnimating: " + CustomPreviewSeekBarPreferenceFragment.this.mPreviewPagerAdapter.isAnimating());
                CustomPreviewSeekBarPreferenceFragment.this.commit();
            }
            this.mSeekByTouch = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        ViewGroup listContainer = (ViewGroup) root.findViewById(android.R.id.list_container);
        listContainer.removeAllViews();
        View content = inflater.inflate(this.mActivityLayoutResId, listContainer, false);
        listContainer.addView(content);
        this.mLabel = (TextView) content.findViewById(R.id.current_label);
        int max = Math.max(1, this.mEntries.length - 1);
        final LabeledSeekBar seekBar = (LabeledSeekBar) content.findViewById(R.id.seek_bar);
        seekBar.setLabels(this.mEntries);
        seekBar.setMax(max);
        seekBar.setProgress(this.mInitialIndex);
        seekBar.setOnSeekBarChangeListener(new onPreviewSeekBarChangeListener(this, null));
        this.mSmaller = content.findViewById(R.id.smaller);
        this.mSmaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = seekBar.getProgress();
                if (progress <= 0) {
                    return;
                }
                seekBar.setProgress(progress - 1, true);
            }
        });
        this.mLarger = content.findViewById(R.id.larger);
        this.mLarger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = seekBar.getProgress();
                if (progress >= seekBar.getMax()) {
                    return;
                }
                seekBar.setProgress(progress + 1, true);
            }
        });
        if (this.mEntries.length == 1) {
            seekBar.setEnabled(false);
        }
        Context context = getPrefContext();
        Configuration origConfig = context.getResources().getConfiguration();
        boolean isLayoutRtl = origConfig.getLayoutDirection() == 1;
        Configuration[] configurations = new Configuration[this.mEntries.length];
        for (int i = 0; i < this.mEntries.length; i++) {
            configurations[i] = createConfig(origConfig, i);
        }
        this.mPreviewPager = (ViewPager) content.findViewById(R.id.preview_pager);
        this.mPreviewPagerAdapter = new PreviewPagerAdapter(context, isLayoutRtl, this.mPreviewSampleResIds, configurations);
        this.mPreviewPager.setAdapter(this.mPreviewPagerAdapter);
        this.mPreviewPager.setCurrentItem(isLayoutRtl ? this.mPreviewSampleResIds.length - 1 : 0);
        this.mPreviewPager.addOnPageChangeListener(this.mPreviewPageChangeListener);
        this.mPageIndicator = (DotsPageIndicator) content.findViewById(R.id.page_indicator);
        if (this.mPreviewSampleResIds.length > 1) {
            this.mPageIndicator.setViewPager(this.mPreviewPager);
            this.mPageIndicator.setVisibility(0);
            this.mPageIndicator.setOnPageChangeListener(this.mPageIndicatorPageChangeListener);
        } else {
            this.mPageIndicator.setVisibility(8);
        }
        setPreviewLayer(this.mInitialIndex, false);
        return root;
    }

    public void setPreviewLayer(int index, boolean animate) {
        Log.d("CustomPreviewSeekBarPreferenceFragment", "setPreviewLayer mCurrentIndex: " + this.mCurrentIndex + "newIndex" + index);
        this.mLabel.setText(this.mEntries[index]);
        this.mSmaller.setEnabled(index > 0);
        this.mLarger.setEnabled(index < this.mEntries.length + (-1));
        setPagerIndicatorContentDescription(this.mPreviewPager.getCurrentItem());
        this.mPreviewPagerAdapter.setPreviewLayer(index, this.mCurrentIndex, this.mPreviewPager.getCurrentItem(), animate);
        this.mCurrentIndex = index;
    }

    public void setPagerIndicatorContentDescription(int position) {
        this.mPageIndicator.setContentDescription(getPrefContext().getString(R.string.preview_page_indicator_content_description, Integer.valueOf(position + 1), Integer.valueOf(this.mPreviewSampleResIds.length)));
    }
}
