package com.android.deskclock.stopwatch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.media.subtitle.Cea708CCParser;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.FabContainer;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.StopwatchTextController;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.data.StopwatchListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;

public final class StopwatchFragment extends DeskClockFragment {
    private static final int REDRAW_PERIOD_PAUSED = 500;
    private static final int REDRAW_PERIOD_RUNNING = 25;
    private GradientItemDecoration mGradientItemDecoration;
    private TextView mHundredthsTimeText;
    private LapsAdapter mLapsAdapter;
    private LinearLayoutManager mLapsLayoutManager;
    private RecyclerView mLapsList;
    private TextView mMainTimeText;
    private StopwatchTextController mStopwatchTextController;
    private final StopwatchListener mStopwatchWatcher;
    private View mStopwatchWrapper;
    private final TabListener mTabWatcher;
    private StopwatchCircleView mTime;
    private final Runnable mTimeUpdateRunnable;

    public StopwatchFragment() {
        super(UiDataModel.Tab.STOPWATCH);
        this.mTabWatcher = new TabWatcher();
        this.mTimeUpdateRunnable = new TimeUpdateRunnable();
        this.mStopwatchWatcher = new StopwatchWatcher();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mLapsAdapter = new LapsAdapter(getActivity());
        this.mLapsLayoutManager = new LinearLayoutManager(getActivity());
        this.mGradientItemDecoration = new GradientItemDecoration(getActivity());
        View viewInflate = layoutInflater.inflate(R.layout.stopwatch_fragment, viewGroup, false);
        this.mTime = (StopwatchCircleView) viewInflate.findViewById(R.id.stopwatch_circle);
        this.mLapsList = (RecyclerView) viewInflate.findViewById(R.id.laps_list);
        ((SimpleItemAnimator) this.mLapsList.getItemAnimator()).setSupportsChangeAnimations(false);
        this.mLapsList.setLayoutManager(this.mLapsLayoutManager);
        this.mLapsList.addItemDecoration(this.mGradientItemDecoration);
        if (Utils.isLandscape(getActivity())) {
            ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
            this.mLapsList.addOnLayoutChangeListener(scrollPositionWatcher);
            this.mLapsList.addOnScrollListener(scrollPositionWatcher);
        } else {
            setTabScrolledToTop(true);
        }
        this.mLapsList.setAdapter(this.mLapsAdapter);
        this.mMainTimeText = (TextView) viewInflate.findViewById(R.id.stopwatch_time_text);
        this.mHundredthsTimeText = (TextView) viewInflate.findViewById(R.id.stopwatch_hundredths_text);
        this.mStopwatchTextController = new StopwatchTextController(this.mMainTimeText, this.mHundredthsTimeText);
        this.mStopwatchWrapper = viewInflate.findViewById(R.id.stopwatch_time_wrapper);
        DataModel.getDataModel().addStopwatchListener(this.mStopwatchWatcher);
        this.mStopwatchWrapper.setOnClickListener(new TimeClickListener());
        if (this.mTime != null) {
            this.mStopwatchWrapper.setOnTouchListener(new CircleTouchListener());
        }
        Context context = this.mMainTimeText.getContext();
        ColorStateList colorStateList = new ColorStateList(new int[][]{new int[]{-16843518, -16842919}, new int[0]}, new int[]{ThemeUtils.resolveColor(context, android.R.attr.textColorPrimary), ThemeUtils.resolveColor(context, R.attr.colorAccent)});
        this.mMainTimeText.setTextColor(colorStateList);
        this.mHundredthsTimeText.setTextColor(colorStateList);
        return viewInflate;
    }

    @Override
    public void onStart() {
        super.onStart();
        UiDataModel.getUiDataModel().addTabListener(this.mTabWatcher);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (StopwatchService.ACTION_START_STOPWATCH.equals(action)) {
                DataModel.getDataModel().startStopwatch();
                intent.setAction(null);
            } else if (StopwatchService.ACTION_PAUSE_STOPWATCH.equals(action)) {
                DataModel.getDataModel().pauseStopwatch();
                intent.setAction(null);
            }
        }
        this.mLapsAdapter.notifyDataSetChanged();
        updateUI(9);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdatingTime();
        UiDataModel.getUiDataModel().removeTabListener(this.mTabWatcher);
        releaseWakeLock();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DataModel.getDataModel().removeStopwatchListener(this.mStopwatchWatcher);
    }

    @Override
    public void onFabClick(@NonNull ImageView imageView) {
        toggleStopwatchState();
    }

    @Override
    public void onLeftButtonClick(@NonNull Button button) {
        doReset();
    }

    @Override
    public void onRightButtonClick(@NonNull Button button) {
        switch (getStopwatch().getState()) {
            case RUNNING:
                doAddLap();
                break;
            case PAUSED:
                doShare();
                break;
        }
    }

    private void updateFab(@NonNull ImageView imageView, boolean z) {
        if (getStopwatch().isRunning()) {
            if (z) {
                imageView.setImageResource(R.drawable.ic_play_pause_animation);
            } else {
                imageView.setImageResource(R.drawable.ic_play_pause);
            }
            imageView.setContentDescription(imageView.getResources().getString(R.string.sw_pause_button));
        } else {
            if (z) {
                imageView.setImageResource(R.drawable.ic_pause_play_animation);
            } else {
                imageView.setImageResource(R.drawable.ic_pause_play);
            }
            imageView.setContentDescription(imageView.getResources().getString(R.string.sw_start_button));
        }
        imageView.setVisibility(0);
    }

    @Override
    public void onUpdateFab(@NonNull ImageView imageView) {
        updateFab(imageView, false);
    }

    @Override
    public void onMorphFab(@NonNull ImageView imageView) {
        updateFab(imageView, Utils.isNOrLater());
        AnimatorUtils.startDrawableAnimation(imageView);
    }

    @Override
    public void onUpdateFabButtons(@NonNull Button button, @NonNull Button button2) {
        if (isAdded()) {
            Resources resources = getResources();
            button.setClickable(true);
            button.setText(R.string.sw_reset_button);
            button.setContentDescription(resources.getString(R.string.sw_reset_button));
            switch (getStopwatch().getState()) {
                case RUNNING:
                    button.setVisibility(0);
                    boolean zCanRecordMoreLaps = canRecordMoreLaps();
                    button2.setText(R.string.sw_lap_button);
                    button2.setContentDescription(resources.getString(R.string.sw_lap_button));
                    button2.setClickable(zCanRecordMoreLaps);
                    button2.setVisibility(zCanRecordMoreLaps ? 0 : 4);
                    break;
                case PAUSED:
                    button.setVisibility(0);
                    button2.setClickable(true);
                    button2.setVisibility(0);
                    button2.setText(R.string.sw_share_button);
                    button2.setContentDescription(resources.getString(R.string.sw_share_button));
                    break;
                case RESET:
                    button.setVisibility(4);
                    button2.setClickable(true);
                    button2.setVisibility(4);
                    break;
            }
        }
    }

    @Override
    protected void onAppColorChanged(@ColorInt int i) {
        if (this.mGradientItemDecoration != null) {
            this.mGradientItemDecoration.updateGradientColors(i);
        }
        if (this.mLapsList != null) {
            this.mLapsList.invalidateItemDecorations();
        }
    }

    private void doStart() {
        Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);
        DataModel.getDataModel().startStopwatch();
    }

    private void doPause() {
        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_deskclock);
        DataModel.getDataModel().pauseStopwatch();
    }

    private void doReset() {
        Stopwatch.State state = getStopwatch().getState();
        Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);
        DataModel.getDataModel().resetStopwatch();
        this.mMainTimeText.setAlpha(1.0f);
        this.mHundredthsTimeText.setAlpha(1.0f);
        if (state == Stopwatch.State.RUNNING) {
            updateFab(3);
        }
    }

    private void doShare() {
        updateFab(32);
        String[] stringArray = getResources().getStringArray(R.array.sw_share_strings);
        String str = stringArray[(int) (Math.random() * ((double) stringArray.length))];
        String shareText = this.mLapsAdapter.getShareText();
        Intent intent = new Intent("android.intent.action.SEND");
        if (Utils.isLOrLater()) {
        }
        Intent type = intent.addFlags(524288).putExtra("android.intent.extra.SUBJECT", str).putExtra("android.intent.extra.TEXT", shareText).setType("text/plain");
        Activity activity = getActivity();
        try {
            activity.startActivity(Intent.createChooser(type, activity.getString(R.string.sw_share_button)));
        } catch (ActivityNotFoundException e) {
            LogUtils.e("Cannot share lap data because no suitable receiving Activity exists", new Object[0]);
            updateFab(8);
        }
    }

    private void doAddLap() {
        Events.sendStopwatchEvent(R.string.action_lap, R.string.label_deskclock);
        Lap lapAddLap = this.mLapsAdapter.addLap();
        if (lapAddLap == null) {
            return;
        }
        updateFab(8);
        if (lapAddLap.getLapNumber() == 1) {
            this.mLapsList.removeAllViewsInLayout();
            if (this.mTime != null) {
                this.mTime.update();
            }
            showOrHideLaps(false);
        }
        this.mLapsList.scrollToPosition(0);
    }

    private void showOrHideLaps(boolean z) {
        ViewGroup viewGroup = (ViewGroup) getView();
        if (viewGroup == null) {
            return;
        }
        TransitionManager.beginDelayedTransition(viewGroup);
        if (z) {
            this.mLapsAdapter.clearLaps();
        }
        boolean z2 = this.mLapsAdapter.getItemCount() > 0;
        this.mLapsList.setVisibility(z2 ? 0 : 8);
        if (Utils.isPortrait(getActivity())) {
            viewGroup.setPadding(viewGroup.getPaddingLeft(), viewGroup.getPaddingTop(), viewGroup.getPaddingRight(), z2 ? 0 : getResources().getDimensionPixelSize(R.dimen.fab_height));
        }
    }

    private void adjustWakeLock() {
        boolean zIsApplicationInForeground = DataModel.getDataModel().isApplicationInForeground();
        if (getStopwatch().isRunning() && isTabSelected() && zIsApplicationInForeground) {
            getActivity().getWindow().addFlags(128);
        } else {
            releaseWakeLock();
        }
    }

    private void releaseWakeLock() {
        getActivity().getWindow().clearFlags(128);
    }

    private void toggleStopwatchState() {
        if (getStopwatch().isRunning()) {
            doPause();
        } else {
            doStart();
        }
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private boolean canRecordMoreLaps() {
        return DataModel.getDataModel().canAddMoreLaps();
    }

    private void startUpdatingTime() {
        stopUpdatingTime();
        this.mMainTimeText.post(this.mTimeUpdateRunnable);
    }

    private void stopUpdatingTime() {
        this.mMainTimeText.removeCallbacks(this.mTimeUpdateRunnable);
    }

    private void updateTime() {
        Stopwatch stopwatch = getStopwatch();
        long totalTime = stopwatch.getTotalTime();
        this.mStopwatchTextController.setTimeString(totalTime);
        boolean z = this.mLapsLayoutManager.findFirstVisibleItemPosition() == 0;
        if (!stopwatch.isReset() && z) {
            this.mLapsAdapter.updateCurrentLap(this.mLapsList, totalTime);
        }
    }

    private void updateUI(@FabContainer.UpdateFabFlag int i) {
        adjustWakeLock();
        updateTime();
        if (this.mTime != null) {
            this.mTime.update();
        }
        Stopwatch stopwatch = getStopwatch();
        if (!stopwatch.isReset()) {
            startUpdatingTime();
        }
        showOrHideLaps(stopwatch.isReset());
        updateFab(i);
    }

    private final class TimeUpdateRunnable implements Runnable {
        private TimeUpdateRunnable() {
        }

        @Override
        public void run() {
            long jNow = Utils.now();
            StopwatchFragment.this.updateTime();
            View view = StopwatchFragment.this.mTime != null ? StopwatchFragment.this.mTime : StopwatchFragment.this.mStopwatchWrapper;
            Stopwatch stopwatch = StopwatchFragment.this.getStopwatch();
            if (stopwatch.isPaused() && jNow % 1000 < 500 && !view.isPressed()) {
                StopwatchFragment.this.mMainTimeText.setAlpha(0.0f);
                StopwatchFragment.this.mHundredthsTimeText.setAlpha(0.0f);
            } else {
                StopwatchFragment.this.mMainTimeText.setAlpha(1.0f);
                StopwatchFragment.this.mHundredthsTimeText.setAlpha(1.0f);
            }
            if (!stopwatch.isReset()) {
                StopwatchFragment.this.mMainTimeText.postDelayed(this, Math.max(0L, (jNow + (stopwatch.isPaused() ? 500L : 25L)) - Utils.now()));
            }
        }
    }

    private final class TabWatcher implements TabListener {
        private TabWatcher() {
        }

        @Override
        public void selectedTabChanged(UiDataModel.Tab tab, UiDataModel.Tab tab2) {
            StopwatchFragment.this.adjustWakeLock();
        }
    }

    private class StopwatchWatcher implements StopwatchListener {
        private StopwatchWatcher() {
        }

        @Override
        public void stopwatchUpdated(Stopwatch stopwatch, Stopwatch stopwatch2) {
            if (stopwatch2.isReset()) {
                StopwatchFragment.this.setTabScrolledToTop(true);
                if (DataModel.getDataModel().isApplicationInForeground()) {
                    StopwatchFragment.this.updateUI(8);
                    return;
                }
                return;
            }
            if (DataModel.getDataModel().isApplicationInForeground()) {
                StopwatchFragment.this.updateUI(11);
            }
        }

        @Override
        public void lapAdded(Lap lap) {
        }
    }

    private final class TimeClickListener implements View.OnClickListener {
        private TimeClickListener() {
        }

        @Override
        public void onClick(View view) {
            if (StopwatchFragment.this.getStopwatch().isRunning()) {
                DataModel.getDataModel().pauseStopwatch();
            } else {
                DataModel.getDataModel().startStopwatch();
            }
        }
    }

    private final class CircleTouchListener implements View.OnTouchListener {
        private CircleTouchListener() {
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            boolean z = false;
            if (motionEvent.getActionMasked() != 0) {
                return false;
            }
            float fMin = Math.min(view.getWidth() / 2.0f, (view.getHeight() - view.getPaddingBottom()) / 2.0f);
            if (Math.pow((motionEvent.getX() - r0) / fMin, 2.0d) + Math.pow((motionEvent.getY() - r8) / fMin, 2.0d) <= 1.0d) {
                z = true;
            }
            return !z;
        }
    }

    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener implements View.OnLayoutChangeListener {
        private ScrollPositionWatcher() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            StopwatchFragment.this.setTabScrolledToTop(Utils.isScrolledToTop(StopwatchFragment.this.mLapsList));
        }

        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            StopwatchFragment.this.setTabScrolledToTop(Utils.isScrolledToTop(StopwatchFragment.this.mLapsList));
        }
    }

    private static final class GradientItemDecoration extends RecyclerView.ItemDecoration {
        private static final int[] ALPHAS = {0, 26, 51, 77, 102, 128, Cea708CCParser.Const.CODE_C1_DSW, 147, Cea708CCParser.Const.CODE_C1_DF5, 167, 177, 186, 196, 206, 216, 226, 235, 245, 255, 255, 255};
        private final int mGradientHeight;
        private final int[] mGradientColors = new int[ALPHAS.length];
        private final GradientDrawable mGradient = new GradientDrawable();

        GradientItemDecoration(Context context) {
            this.mGradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            updateGradientColors(ThemeUtils.resolveColor(context, android.R.attr.windowBackground));
            this.mGradientHeight = Math.round(context.getResources().getDimensionPixelSize(R.dimen.fab_height) * 1.2f);
        }

        @Override
        public void onDrawOver(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
            super.onDrawOver(canvas, recyclerView, state);
            int width = recyclerView.getWidth();
            int height = recyclerView.getHeight();
            this.mGradient.setBounds(0, height - this.mGradientHeight, width, height);
            this.mGradient.draw(canvas);
        }

        void updateGradientColors(@ColorInt int i) {
            for (int i2 = 0; i2 < this.mGradientColors.length; i2++) {
                this.mGradientColors[i2] = ColorUtils.setAlphaComponent(i, ALPHAS[i2]);
            }
            this.mGradient.setColors(this.mGradientColors);
        }
    }
}
