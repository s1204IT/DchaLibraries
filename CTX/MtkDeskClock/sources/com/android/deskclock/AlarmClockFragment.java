package com.android.deskclock;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.alarms.AlarmUpdateHandler;
import com.android.deskclock.alarms.ScrollHandler;
import com.android.deskclock.alarms.TimePickerDialogFragment;
import com.android.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.android.deskclock.alarms.dataadapter.CollapsedAlarmViewHolder;
import com.android.deskclock.alarms.dataadapter.ExpandedAlarmViewHolder;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.EmptyViewController;
import com.android.deskclock.widget.toast.SnackbarManager;
import com.android.deskclock.widget.toast.ToastManager;
import java.util.ArrayList;
import java.util.List;

public final class AlarmClockFragment extends DeskClockFragment implements LoaderManager.LoaderCallbacks<Cursor>, ScrollHandler, TimePickerDialogFragment.OnTimeSetListener {
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";
    private static final String KEY_EXPANDED_ID = "expandedId";
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";
    private AlarmTimeClickHandler mAlarmTimeClickHandler;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private long mCurrentUpdateToken;
    private Loader mCursorLoader;
    private EmptyViewController mEmptyViewController;
    private long mExpandedAlarmId;
    private ItemAdapter<AlarmItemHolder> mItemAdapter;
    private LinearLayoutManager mLayoutManager;
    private ViewGroup mMainLayout;
    private final Runnable mMidnightUpdater;
    private RecyclerView mRecyclerView;
    private long mScrollToAlarmId;

    public AlarmClockFragment() {
        super(UiDataModel.Tab.ALARMS);
        this.mMidnightUpdater = new MidnightRunnable();
        this.mScrollToAlarmId = -1L;
        this.mExpandedAlarmId = -1L;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mCursorLoader = getLoaderManager().initLoader(0, null, this);
        if (bundle != null) {
            this.mExpandedAlarmId = bundle.getLong(KEY_EXPANDED_ID, -1L);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.alarm_clock, viewGroup, false);
        Activity activity = getActivity();
        this.mRecyclerView = (RecyclerView) viewInflate.findViewById(R.id.alarms_recycler_view);
        this.mLayoutManager = new LinearLayoutManager(activity) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                int extraLayoutSpace = super.getExtraLayoutSpace(state);
                if (state.willRunPredictiveAnimations()) {
                    return Math.max(getHeight(), extraLayoutSpace);
                }
                return extraLayoutSpace;
            }
        };
        this.mRecyclerView.setLayoutManager(this.mLayoutManager);
        this.mMainLayout = (ViewGroup) viewInflate.findViewById(R.id.main);
        this.mAlarmUpdateHandler = new AlarmUpdateHandler(activity, this, this.mMainLayout);
        TextView textView = (TextView) viewInflate.findViewById(R.id.alarms_empty_view);
        textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, Utils.getVectorDrawable(activity, R.drawable.ic_noalarms), (Drawable) null, (Drawable) null);
        this.mEmptyViewController = new EmptyViewController(this.mMainLayout, this.mRecyclerView, textView);
        this.mAlarmTimeClickHandler = new AlarmTimeClickHandler(this, bundle, this.mAlarmUpdateHandler, this);
        this.mItemAdapter = new ItemAdapter<>();
        this.mItemAdapter.setHasStableIds();
        this.mItemAdapter.withViewTypes(new CollapsedAlarmViewHolder.Factory(layoutInflater), null, R.layout.alarm_time_collapsed);
        this.mItemAdapter.withViewTypes(new ExpandedAlarmViewHolder.Factory(activity), null, R.layout.alarm_time_expanded);
        this.mItemAdapter.setOnItemChangedListener(new ItemAdapter.OnItemChangedListener() {
            @Override
            public void onItemChanged(ItemAdapter.ItemHolder<?> itemHolder) {
                if (!((AlarmItemHolder) itemHolder).isExpanded()) {
                    if (AlarmClockFragment.this.mExpandedAlarmId == itemHolder.itemId) {
                        AlarmClockFragment.this.mExpandedAlarmId = -1L;
                    }
                } else if (AlarmClockFragment.this.mExpandedAlarmId != itemHolder.itemId) {
                    AlarmItemHolder alarmItemHolder = (AlarmItemHolder) AlarmClockFragment.this.mItemAdapter.findItemById(AlarmClockFragment.this.mExpandedAlarmId);
                    if (alarmItemHolder != null) {
                        alarmItemHolder.collapse();
                    }
                    AlarmClockFragment.this.mExpandedAlarmId = itemHolder.itemId;
                    RecyclerView.ViewHolder viewHolderFindViewHolderForItemId = AlarmClockFragment.this.mRecyclerView.findViewHolderForItemId(AlarmClockFragment.this.mExpandedAlarmId);
                    if (viewHolderFindViewHolderForItemId != null) {
                        AlarmClockFragment.this.smoothScrollTo(viewHolderFindViewHolderForItemId.getAdapterPosition());
                    }
                }
            }

            @Override
            public void onItemChanged(ItemAdapter.ItemHolder<?> itemHolder, Object obj) {
            }
        });
        ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
        this.mRecyclerView.addOnLayoutChangeListener(scrollPositionWatcher);
        this.mRecyclerView.addOnScrollListener(scrollPositionWatcher);
        this.mRecyclerView.setAdapter(this.mItemAdapter);
        ItemAnimator itemAnimator = new ItemAnimator();
        itemAnimator.setChangeDuration(300L);
        itemAnimator.setMoveDuration(300L);
        this.mRecyclerView.setItemAnimator(itemAnimator);
        return viewInflate;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isTabSelected()) {
            TimePickerDialogFragment.removeTimeEditDialog(getFragmentManager());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        UiDataModel.getUiDataModel().addMidnightCallback(this.mMidnightUpdater, 100L);
        Intent intent = getActivity().getIntent();
        if (intent == null) {
            return;
        }
        if (intent.hasExtra(ALARM_CREATE_NEW_INTENT_EXTRA)) {
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.ALARMS);
            if (intent.getBooleanExtra(ALARM_CREATE_NEW_INTENT_EXTRA, false)) {
                startCreatingAlarm();
            }
            intent.removeExtra(ALARM_CREATE_NEW_INTENT_EXTRA);
            return;
        }
        if (intent.hasExtra(SCROLL_TO_ALARM_INTENT_EXTRA)) {
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.ALARMS);
            long longExtra = intent.getLongExtra(SCROLL_TO_ALARM_INTENT_EXTRA, -1L);
            if (longExtra != -1) {
                setSmoothScrollStableId(longExtra);
                if (this.mCursorLoader != null && this.mCursorLoader.isStarted()) {
                    this.mCursorLoader.forceLoad();
                }
            }
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        UiDataModel.getUiDataModel().removePeriodicCallback(this.mMidnightUpdater);
        this.mAlarmUpdateHandler.hideUndoBar();
    }

    @Override
    public void smoothScrollTo(int i) {
        this.mLayoutManager.scrollToPositionWithOffset(i, 0);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mAlarmTimeClickHandler.saveInstance(bundle);
        bundle.putLong(KEY_EXPANDED_ID, this.mExpandedAlarmId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastManager.cancelToast();
    }

    public void setLabel(Alarm alarm, String str) {
        alarm.label = str;
        this.mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ArrayList arrayList = new ArrayList(cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Alarm alarm = new Alarm(cursor);
            arrayList.add(new AlarmItemHolder(alarm, alarm.canPreemptivelyDismiss() ? new AlarmInstance(cursor, true) : null, this.mAlarmTimeClickHandler));
            cursor.moveToNext();
        }
        setAdapterItems(arrayList, SystemClock.elapsedRealtime());
    }

    private void setAdapterItems(final List<AlarmItemHolder> list, final long j) {
        if (j < this.mCurrentUpdateToken) {
            LogUtils.v("Ignoring adapter update: %d < %d", Long.valueOf(j), Long.valueOf(this.mCurrentUpdateToken));
            return;
        }
        if (this.mRecyclerView.getItemAnimator().isRunning()) {
            this.mRecyclerView.getItemAnimator().isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                @Override
                public void onAnimationsFinished() {
                    AlarmClockFragment.this.setAdapterItems(list, j);
                }
            });
            return;
        }
        if (this.mRecyclerView.isComputingLayout()) {
            this.mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    AlarmClockFragment.this.setAdapterItems(list, j);
                }
            });
            return;
        }
        this.mCurrentUpdateToken = j;
        this.mItemAdapter.setItems(list);
        boolean zIsEmpty = list.isEmpty();
        this.mEmptyViewController.setEmpty(zIsEmpty);
        if (zIsEmpty) {
            setTabScrolledToTop(true);
        }
        if (this.mExpandedAlarmId != -1) {
            AlarmItemHolder alarmItemHolder = (AlarmItemHolder) this.mItemAdapter.findItemById(this.mExpandedAlarmId);
            if (alarmItemHolder != null) {
                this.mAlarmTimeClickHandler.setSelectedAlarm((Alarm) alarmItemHolder.item);
                alarmItemHolder.expand();
            } else {
                this.mAlarmTimeClickHandler.setSelectedAlarm(null);
                this.mExpandedAlarmId = -1L;
            }
        }
        if (this.mScrollToAlarmId != -1) {
            scrollToAlarm(this.mScrollToAlarmId);
            setSmoothScrollStableId(-1L);
        }
    }

    private void scrollToAlarm(long j) {
        int itemCount = this.mItemAdapter.getItemCount();
        int i = 0;
        while (true) {
            if (i >= itemCount) {
                i = -1;
                break;
            } else if (this.mItemAdapter.getItemId(i) == j) {
                break;
            } else {
                i++;
            }
        }
        if (i < 0) {
            SnackbarManager.show(Snackbar.make(this.mMainLayout, R.string.missed_alarm_has_been_deleted, 0));
        } else {
            ((AlarmItemHolder) this.mItemAdapter.findItemById(j)).expand();
            smoothScrollTo(i);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void setSmoothScrollStableId(long j) {
        this.mScrollToAlarmId = j;
    }

    @Override
    public void onFabClick(@NonNull ImageView imageView) {
        this.mAlarmUpdateHandler.hideUndoBar();
        startCreatingAlarm();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView imageView) {
        imageView.setVisibility(0);
        imageView.setImageResource(R.drawable.ic_add_white_24dp);
        imageView.setContentDescription(imageView.getResources().getString(R.string.button_alarms));
    }

    @Override
    public void onUpdateFabButtons(@NonNull Button button, @NonNull Button button2) {
        button.setVisibility(4);
        button2.setVisibility(4);
    }

    private void startCreatingAlarm() {
        this.mAlarmTimeClickHandler.setSelectedAlarm(null);
        TimePickerDialogFragment.show(this);
    }

    @Override
    public void onTimeSet(TimePickerDialogFragment timePickerDialogFragment, int i, int i2) {
        this.mAlarmTimeClickHandler.onTimeSet(i, i2);
    }

    public void removeItem(AlarmItemHolder alarmItemHolder) {
        this.mItemAdapter.removeItem(alarmItemHolder);
    }

    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener implements View.OnLayoutChangeListener {
        private ScrollPositionWatcher() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            AlarmClockFragment.this.setTabScrolledToTop(Utils.isScrolledToTop(AlarmClockFragment.this.mRecyclerView));
        }

        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            AlarmClockFragment.this.setTabScrolledToTop(Utils.isScrolledToTop(AlarmClockFragment.this.mRecyclerView));
        }
    }

    private final class MidnightRunnable implements Runnable {
        private MidnightRunnable() {
        }

        @Override
        public void run() {
            AlarmClockFragment.this.mItemAdapter.notifyDataSetChanged();
        }
    }
}
