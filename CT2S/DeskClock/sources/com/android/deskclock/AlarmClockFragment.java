package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.ActionableToastBar;
import com.android.deskclock.widget.TextTime;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;

public class AlarmClockFragment extends DeskClockFragment implements LoaderManager.LoaderCallbacks<Cursor>, TimePickerDialog.OnTimeSetListener, View.OnTouchListener {
    private static final DeskClockExtensions sDeskClockExtensions = ExtensionsFactory.getDeskClockExtensions();
    private AlarmItemAdapter mAdapter;
    private Transition mAddRemoveTransition;
    private Alarm mAddedAlarm;
    private ListView mAlarmsList;
    private Interpolator mCollapseInterpolator;
    private Alarm mDeletedAlarm;
    private View mEmptyView;
    private Transition mEmptyViewTransition;
    private Interpolator mExpandInterpolator;
    private View mFooterView;
    private FrameLayout mMainLayout;
    private Transition mRepeatTransition;
    private Bundle mRingtoneTitleCache;
    private Alarm mSelectedAlarm;
    private ActionableToastBar mUndoBar;
    private View mUndoFrame;
    private boolean mUndoShowing;
    private long mScrollToAlarmId = -1;
    private Loader mCursorLoader = null;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        this.mCursorLoader = getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View v = inflater.inflate(R.layout.alarm_clock, container, false);
        long expandedId = -1;
        long[] repeatCheckedIds = null;
        long[] selectedAlarms = null;
        Bundle previousDayMap = null;
        if (savedState != null) {
            expandedId = savedState.getLong("expandedId");
            repeatCheckedIds = savedState.getLongArray("repeatCheckedIds");
            this.mRingtoneTitleCache = savedState.getBundle("ringtoneTitleCache");
            this.mDeletedAlarm = (Alarm) savedState.getParcelable("deletedAlarm");
            this.mUndoShowing = savedState.getBoolean("undoShowing");
            selectedAlarms = savedState.getLongArray("selectedAlarms");
            previousDayMap = savedState.getBundle("previousDayMap");
            this.mSelectedAlarm = (Alarm) savedState.getParcelable("selectedAlarm");
        }
        this.mExpandInterpolator = new DecelerateInterpolator(1.0f);
        this.mCollapseInterpolator = new DecelerateInterpolator(0.7f);
        this.mAddRemoveTransition = new AutoTransition();
        this.mAddRemoveTransition.setDuration(300L);
        this.mRepeatTransition = new AutoTransition();
        this.mRepeatTransition.setDuration(150L);
        this.mRepeatTransition.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mEmptyViewTransition = new TransitionSet().setOrdering(1).addTransition(new Fade(2)).addTransition(new Fade(1)).setDuration(300L);
        boolean isLandscape = getResources().getConfiguration().orientation == 2;
        View menuButton = v.findViewById(R.id.menu_button);
        if (menuButton != null) {
            if (isLandscape) {
                menuButton.setVisibility(8);
            } else {
                menuButton.setVisibility(0);
                setupFakeOverflowMenuButton(menuButton);
            }
        }
        this.mEmptyView = v.findViewById(R.id.alarms_empty_view);
        this.mMainLayout = (FrameLayout) v.findViewById(R.id.main);
        this.mAlarmsList = (ListView) v.findViewById(R.id.alarms_list);
        this.mUndoBar = (ActionableToastBar) v.findViewById(R.id.undo_bar);
        this.mUndoFrame = v.findViewById(R.id.undo_frame);
        this.mUndoFrame.setOnTouchListener(this);
        this.mFooterView = v.findViewById(R.id.alarms_footer_view);
        this.mFooterView.setOnTouchListener(this);
        this.mAdapter = new AlarmItemAdapter(getActivity(), expandedId, repeatCheckedIds, selectedAlarms, previousDayMap, this.mAlarmsList);
        this.mAdapter.registerDataSetObserver(new DataSetObserver() {
            private int prevAdapterCount = -1;

            @Override
            public void onChanged() {
                int count = AlarmClockFragment.this.mAdapter.getCount();
                if (AlarmClockFragment.this.mDeletedAlarm != null && this.prevAdapterCount > count) {
                    AlarmClockFragment.this.showUndoBar();
                }
                if ((count == 0 && this.prevAdapterCount > 0) || (count > 0 && this.prevAdapterCount == 0)) {
                    TransitionManager.beginDelayedTransition(AlarmClockFragment.this.mMainLayout, AlarmClockFragment.this.mEmptyViewTransition);
                }
                AlarmClockFragment.this.mEmptyView.setVisibility(count == 0 ? 0 : 8);
                this.prevAdapterCount = count;
                super.onChanged();
            }
        });
        if (this.mRingtoneTitleCache == null) {
            this.mRingtoneTitleCache = new Bundle();
        }
        this.mAlarmsList.setAdapter((ListAdapter) this.mAdapter);
        this.mAlarmsList.setVerticalScrollBarEnabled(true);
        this.mAlarmsList.setOnCreateContextMenuListener(this);
        if (this.mUndoShowing) {
            showUndoBar();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == 0) {
            setFabAppearance();
            setLeftRightButtonAppearance();
        }
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
        Intent intent = getActivity().getIntent();
        if (intent.hasExtra("deskclock.create.new")) {
            if (intent.getBooleanExtra("deskclock.create.new", false)) {
                startCreatingAlarm();
            }
            intent.removeExtra("deskclock.create.new");
        } else if (intent.hasExtra("deskclock.scroll.to.alarm")) {
            long alarmId = intent.getLongExtra("deskclock.scroll.to.alarm", -1L);
            if (alarmId != -1) {
                this.mScrollToAlarmId = alarmId;
                if (this.mCursorLoader != null && this.mCursorLoader.isStarted()) {
                    this.mCursorLoader.forceLoad();
                }
            }
            intent.removeExtra("deskclock.scroll.to.alarm");
        }
    }

    private void hideUndoBar(boolean animate, MotionEvent event) {
        if (this.mUndoBar != null) {
            this.mUndoFrame.setVisibility(8);
            if (event == null || !this.mUndoBar.isEventInToastBar(event)) {
                this.mUndoBar.hide(animate);
            } else {
                return;
            }
        }
        this.mDeletedAlarm = null;
        this.mUndoShowing = false;
    }

    private void showUndoBar() {
        final Alarm deletedAlarm = this.mDeletedAlarm;
        this.mUndoFrame.setVisibility(0);
        this.mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
            @Override
            public void onActionClicked() {
                AlarmClockFragment.this.mAddedAlarm = deletedAlarm;
                AlarmClockFragment.this.mDeletedAlarm = null;
                AlarmClockFragment.this.mUndoShowing = false;
                AlarmClockFragment.this.asyncAddAlarm(deletedAlarm);
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("expandedId", this.mAdapter.getExpandedId());
        outState.putLongArray("repeatCheckedIds", this.mAdapter.getRepeatArray());
        outState.putLongArray("selectedAlarms", this.mAdapter.getSelectedAlarmsArray());
        outState.putBundle("ringtoneTitleCache", this.mRingtoneTitleCache);
        outState.putParcelable("deletedAlarm", this.mDeletedAlarm);
        outState.putBoolean("undoShowing", this.mUndoShowing);
        outState.putBundle("previousDayMap", this.mAdapter.getPreviousDaysOfWeekMap());
        outState.putParcelable("selectedAlarm", this.mSelectedAlarm);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideUndoBar(false, null);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        if (this.mSelectedAlarm == null) {
            Alarm a = new Alarm();
            a.alert = RingtoneManager.getActualDefaultRingtoneUri(getActivity(), 4);
            if (a.alert == null) {
                a.alert = Uri.parse("content://settings/system/alarm_alert");
            }
            a.hour = hourOfDay;
            a.minutes = minute;
            a.enabled = true;
            this.mAddedAlarm = a;
            asyncAddAlarm(a);
            return;
        }
        this.mSelectedAlarm.hour = hourOfDay;
        this.mSelectedAlarm.minutes = minute;
        this.mSelectedAlarm.enabled = true;
        this.mScrollToAlarmId = this.mSelectedAlarm.id;
        asyncUpdateAlarm(this.mSelectedAlarm, true);
        this.mSelectedAlarm = null;
    }

    private void showLabelDialog(Alarm alarm) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        LabelDialogFragment newFragment = LabelDialogFragment.newInstance(alarm, alarm.label, getTag());
        newFragment.show(ft, "label_dialog");
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        asyncUpdateAlarm(alarm, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        this.mAdapter.swapCursor(data);
        if (this.mScrollToAlarmId != -1) {
            scrollToAlarm(this.mScrollToAlarmId);
            this.mScrollToAlarmId = -1L;
        }
    }

    private void scrollToAlarm(long alarmId) {
        int alarmPosition = -1;
        int i = 0;
        while (true) {
            if (i >= this.mAdapter.getCount()) {
                break;
            }
            long id = this.mAdapter.getItemId(i);
            if (id != alarmId) {
                i++;
            } else {
                alarmPosition = i;
                break;
            }
        }
        if (alarmPosition >= 0) {
            this.mAdapter.setNewAlarm(alarmId);
            this.mAlarmsList.smoothScrollToPositionFromTop(alarmPosition, 0);
        } else {
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context, R.string.missed_alarm_has_been_deleted, 1);
            ToastMaster.setToast(toast);
            toast.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        this.mAdapter.swapCursor(null);
    }

    private void launchRingTonePicker(Alarm alarm) {
        this.mSelectedAlarm = alarm;
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null : alarm.alert;
        Intent intent = new Intent("android.intent.action.RINGTONE_PICKER");
        intent.putExtra("android.intent.extra.ringtone.EXISTING_URI", oldRingtone);
        intent.putExtra("android.intent.extra.ringtone.TYPE", 4);
        intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", false);
        startActivityForResult(intent, 1);
    }

    private void saveRingtoneUri(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI");
        if (uri == null) {
            uri = Alarm.NO_RINGTONE_URI;
        }
        this.mSelectedAlarm.alert = uri;
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            RingtoneManager.setActualDefaultRingtoneUri(getActivity(), 4, uri);
        }
        asyncUpdateAlarm(this.mSelectedAlarm, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1) {
            switch (requestCode) {
                case 1:
                    saveRingtoneUri(data);
                    break;
                default:
                    LogUtils.w("Unhandled request code in onActivityResult: " + requestCode, new Object[0]);
                    break;
            }
        }
    }

    public class AlarmItemAdapter extends CursorAdapter {
        private final int[] DAY_ORDER;
        private final int mCollapseExpandHeight;
        private final int mColorDim;
        private final int mColorLit;
        private final Context mContext;
        private long mExpandedId;
        private ItemHolder mExpandedItemHolder;
        private final LayoutInflater mFactory;
        private final boolean mHasVibrator;
        private final ListView mList;
        private final String[] mLongWeekDayStrings;
        private Bundle mPreviousDaysOfWeekMap;
        private final HashSet<Long> mRepeatChecked;
        private final Typeface mRobotoNormal;
        private long mScrollAlarmId;
        private final Runnable mScrollRunnable;
        private final HashSet<Long> mSelectedAlarms;
        private final String[] mShortWeekDayStrings;

        public class ItemHolder {
            Alarm alarm;
            LinearLayout alarmItem;
            View arrow;
            TextView clickableLabel;
            TextTime clock;
            View collapseExpandArea;
            Button[] dayButtons = new Button[7];
            TextView daysOfWeek;
            ImageButton delete;
            View expandArea;
            View hairLine;
            TextView label;
            Switch onoff;
            CheckBox repeat;
            LinearLayout repeatDays;
            TextView ringtone;
            View summary;
            TextView tomorrowLabel;
            CheckBox vibrate;

            public ItemHolder() {
            }
        }

        public AlarmItemAdapter(Context context, long expandedId, long[] repeatCheckedIds, long[] selectedAlarms, Bundle previousDaysOfWeekMap, ListView list) {
            super(context, (Cursor) null, 0);
            this.mRepeatChecked = new HashSet<>();
            this.mSelectedAlarms = new HashSet<>();
            this.mPreviousDaysOfWeekMap = new Bundle();
            this.DAY_ORDER = new int[]{1, 2, 3, 4, 5, 6, 7};
            this.mScrollAlarmId = -1L;
            this.mScrollRunnable = new Runnable() {
                @Override
                public void run() {
                    if (AlarmItemAdapter.this.mScrollAlarmId != -1) {
                        View v = AlarmItemAdapter.this.getViewById(AlarmItemAdapter.this.mScrollAlarmId);
                        if (v != null) {
                            Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                            AlarmItemAdapter.this.mList.requestChildRectangleOnScreen(v, rect, false);
                        }
                        AlarmItemAdapter.this.mScrollAlarmId = -1L;
                    }
                }
            };
            this.mContext = context;
            this.mFactory = LayoutInflater.from(context);
            this.mList = list;
            DateFormatSymbols dfs = new DateFormatSymbols();
            this.mShortWeekDayStrings = Utils.getShortWeekdays();
            this.mLongWeekDayStrings = dfs.getWeekdays();
            Resources res = this.mContext.getResources();
            this.mColorLit = res.getColor(R.color.clock_white);
            this.mColorDim = res.getColor(R.color.clock_gray);
            this.mRobotoNormal = Typeface.create("sans-serif", 0);
            this.mExpandedId = expandedId;
            if (repeatCheckedIds != null) {
                buildHashSetFromArray(repeatCheckedIds, this.mRepeatChecked);
            }
            if (previousDaysOfWeekMap != null) {
                this.mPreviousDaysOfWeekMap = previousDaysOfWeekMap;
            }
            if (selectedAlarms != null) {
                buildHashSetFromArray(selectedAlarms, this.mSelectedAlarms);
            }
            this.mHasVibrator = ((Vibrator) context.getSystemService("vibrator")).hasVibrator();
            this.mCollapseExpandHeight = (int) res.getDimension(R.dimen.collapse_expand_height);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (!getCursor().moveToPosition(position)) {
                LogUtils.v("couldn't move cursor to position " + position, new Object[0]);
                return null;
            }
            if (convertView == null) {
                v = newView(this.mContext, getCursor(), parent);
            } else {
                v = convertView;
            }
            bindView(v, this.mContext, getCursor());
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = this.mFactory.inflate(R.layout.alarm_time, parent, false);
            setNewHolder(view);
            return view;
        }

        @Override
        public synchronized Cursor swapCursor(Cursor cursor) {
            Cursor c;
            if (AlarmClockFragment.this.mAddedAlarm != null || AlarmClockFragment.this.mDeletedAlarm != null) {
                TransitionManager.beginDelayedTransition(AlarmClockFragment.this.mAlarmsList, AlarmClockFragment.this.mAddRemoveTransition);
            }
            c = super.swapCursor(cursor);
            AlarmClockFragment.this.mAddedAlarm = null;
            AlarmClockFragment.this.mDeletedAlarm = null;
            return c;
        }

        private void setNewHolder(View view) {
            ItemHolder holder = new ItemHolder();
            holder.alarmItem = (LinearLayout) view.findViewById(R.id.alarm_item);
            holder.tomorrowLabel = (TextView) view.findViewById(R.id.tomorrowLabel);
            holder.clock = (TextTime) view.findViewById(R.id.digital_clock);
            holder.onoff = (Switch) view.findViewById(R.id.onoff);
            holder.onoff.setTypeface(this.mRobotoNormal);
            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
            holder.label = (TextView) view.findViewById(R.id.label);
            holder.delete = (ImageButton) view.findViewById(R.id.delete);
            holder.summary = view.findViewById(R.id.summary);
            holder.expandArea = view.findViewById(R.id.expand_area);
            holder.hairLine = view.findViewById(R.id.hairline);
            holder.arrow = view.findViewById(R.id.arrow);
            holder.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
            holder.clickableLabel = (TextView) view.findViewById(R.id.edit_label);
            holder.repeatDays = (LinearLayout) view.findViewById(R.id.repeat_days);
            holder.collapseExpandArea = view.findViewById(R.id.collapse_expand);
            for (int i = 0; i < 7; i++) {
                Button dayButton = (Button) this.mFactory.inflate(R.layout.day_button, (ViewGroup) holder.repeatDays, false);
                dayButton.setText(this.mShortWeekDayStrings[i]);
                dayButton.setContentDescription(this.mLongWeekDayStrings[this.DAY_ORDER[i]]);
                holder.repeatDays.addView(dayButton);
                holder.dayButtons[i] = dayButton;
            }
            holder.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
            holder.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);
            view.setTag(holder);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);
            Object tag = view.getTag();
            if (tag == null) {
                setNewHolder(view);
            }
            final ItemHolder itemHolder = (ItemHolder) tag;
            itemHolder.alarm = alarm;
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(alarm.enabled);
            if (this.mSelectedAlarms.contains(Long.valueOf(itemHolder.alarm.id))) {
                setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, true);
                setDigitalTimeAlpha(itemHolder, true);
                itemHolder.onoff.setEnabled(false);
            } else {
                itemHolder.onoff.setEnabled(true);
                setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, false);
                setDigitalTimeAlpha(itemHolder, itemHolder.onoff.isChecked());
            }
            itemHolder.clock.setFormat((int) this.mContext.getResources().getDimension(R.dimen.alarm_label_size));
            itemHolder.clock.setTime(alarm.hour, alarm.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    AlarmClockFragment.this.mSelectedAlarm = itemHolder.alarm;
                    AlarmUtils.showTimeEditDialog(AlarmClockFragment.this, alarm);
                    AlarmItemAdapter.this.expandAlarm(itemHolder, true);
                    itemHolder.alarmItem.post(AlarmItemAdapter.this.mScrollRunnable);
                }
            });
            CompoundButton.OnCheckedChangeListener onOffListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked != alarm.enabled) {
                        if (!AlarmItemAdapter.this.isAlarmExpanded(alarm)) {
                            AlarmItemAdapter.this.setDigitalTimeAlpha(itemHolder, checked);
                        }
                        alarm.enabled = checked;
                        AlarmClockFragment.this.asyncUpdateAlarm(alarm, alarm.enabled);
                    }
                }
            };
            if (this.mRepeatChecked.contains(Long.valueOf(alarm.id)) || itemHolder.alarm.daysOfWeek.isRepeating()) {
                itemHolder.tomorrowLabel.setVisibility(8);
            } else {
                itemHolder.tomorrowLabel.setVisibility(0);
                Resources resources = AlarmClockFragment.this.getResources();
                String labelText = isTomorrow(alarm) ? resources.getString(R.string.alarm_tomorrow) : resources.getString(R.string.alarm_today);
                itemHolder.tomorrowLabel.setText(labelText);
            }
            itemHolder.onoff.setOnCheckedChangeListener(onOffListener);
            boolean expanded = isAlarmExpanded(alarm);
            if (expanded) {
                this.mExpandedItemHolder = itemHolder;
            }
            itemHolder.expandArea.setVisibility(expanded ? 0 : 8);
            itemHolder.delete.setVisibility(expanded ? 0 : 8);
            itemHolder.summary.setVisibility(expanded ? 8 : 0);
            itemHolder.hairLine.setVisibility(expanded ? 8 : 0);
            itemHolder.arrow.setRotation(expanded ? 180.0f : 0.0f);
            itemHolder.arrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    if (AlarmItemAdapter.this.isAlarmExpanded(alarm)) {
                        AlarmItemAdapter.this.collapseAlarm(itemHolder, true);
                    } else {
                        AlarmItemAdapter.this.expandAlarm(itemHolder, true);
                    }
                }
            });
            String daysOfWeekStr = alarm.daysOfWeek.toString(AlarmClockFragment.this.getActivity(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(AlarmClockFragment.this.getActivity()));
                itemHolder.daysOfWeek.setVisibility(0);
                itemHolder.daysOfWeek.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        AlarmItemAdapter.this.expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(AlarmItemAdapter.this.mScrollRunnable);
                    }
                });
            } else {
                itemHolder.daysOfWeek.setVisibility(8);
            }
            if (alarm.label != null && alarm.label.length() != 0) {
                itemHolder.label.setText(alarm.label + "  ");
                itemHolder.label.setVisibility(0);
                itemHolder.label.setContentDescription(this.mContext.getResources().getString(R.string.label_description) + " " + alarm.label);
                itemHolder.label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        AlarmItemAdapter.this.expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(AlarmItemAdapter.this.mScrollRunnable);
                    }
                });
            } else {
                itemHolder.label.setVisibility(8);
            }
            itemHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlarmClockFragment.this.mDeletedAlarm = alarm;
                    AlarmItemAdapter.this.mRepeatChecked.remove(Long.valueOf(alarm.id));
                    AlarmClockFragment.this.asyncDeleteAlarm(alarm);
                }
            });
            if (expanded) {
                expandAlarm(itemHolder, false);
            }
            itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    if (AlarmItemAdapter.this.isAlarmExpanded(alarm)) {
                        AlarmItemAdapter.this.collapseAlarm(itemHolder, true);
                    } else {
                        AlarmItemAdapter.this.expandAlarm(itemHolder, true);
                    }
                }
            });
        }

        private void setAlarmItemBackgroundAndElevation(LinearLayout layout, boolean expanded) {
            if (expanded) {
                layout.setBackgroundColor(getTintedBackgroundColor());
                layout.setElevation(8.0f);
            } else {
                layout.setBackgroundResource(R.drawable.alarm_background_normal);
                layout.setElevation(0.0f);
            }
        }

        private int getTintedBackgroundColor() {
            int c = Utils.getCurrentHourColor();
            int red = Color.red(c) + ((int) ((255 - Color.red(c)) * 0.09f));
            int green = Color.green(c) + ((int) ((255 - Color.green(c)) * 0.09f));
            int blue = Color.blue(c) + ((int) ((255 - Color.blue(c)) * 0.09f));
            return Color.rgb(red, green, blue);
        }

        private boolean isTomorrow(Alarm alarm) {
            Calendar now = Calendar.getInstance();
            int alarmHour = alarm.hour;
            int currHour = now.get(11);
            return alarmHour < currHour || (alarmHour == currHour && alarm.minutes <= now.get(12));
        }

        private void bindExpandArea(final ItemHolder itemHolder, final Alarm alarm) {
            String ringtone;
            if (alarm.label != null && alarm.label.length() > 0) {
                itemHolder.clickableLabel.setText(alarm.label);
            } else {
                itemHolder.clickableLabel.setText(R.string.label);
            }
            itemHolder.clickableLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlarmClockFragment.this.showLabelDialog(alarm);
                }
            });
            if (this.mRepeatChecked.contains(Long.valueOf(alarm.id)) || itemHolder.alarm.daysOfWeek.isRepeating()) {
                itemHolder.repeat.setChecked(true);
                itemHolder.repeatDays.setVisibility(0);
            } else {
                itemHolder.repeat.setChecked(false);
                itemHolder.repeatDays.setVisibility(8);
            }
            itemHolder.repeat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TransitionManager.beginDelayedTransition(AlarmItemAdapter.this.mList, AlarmClockFragment.this.mRepeatTransition);
                    boolean checked = ((CheckBox) view).isChecked();
                    if (checked) {
                        itemHolder.repeatDays.setVisibility(0);
                        AlarmItemAdapter.this.mRepeatChecked.add(Long.valueOf(alarm.id));
                        int bitSet = AlarmItemAdapter.this.mPreviousDaysOfWeekMap.getInt("" + alarm.id);
                        alarm.daysOfWeek.setBitSet(bitSet);
                        if (!alarm.daysOfWeek.isRepeating()) {
                            alarm.daysOfWeek.setDaysOfWeek(true, AlarmItemAdapter.this.DAY_ORDER);
                        }
                        AlarmItemAdapter.this.updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
                    } else {
                        itemHolder.repeatDays.setVisibility(8);
                        AlarmItemAdapter.this.mRepeatChecked.remove(Long.valueOf(alarm.id));
                        int bitSet2 = alarm.daysOfWeek.getBitSet();
                        AlarmItemAdapter.this.mPreviousDaysOfWeekMap.putInt("" + alarm.id, bitSet2);
                        alarm.daysOfWeek.clearAllDays();
                    }
                    AlarmClockFragment.this.asyncUpdateAlarm(alarm, false);
                }
            });
            updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
            for (int i = 0; i < 7; i++) {
                final int buttonIndex = i;
                itemHolder.dayButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean isActivated = itemHolder.dayButtons[buttonIndex].isActivated();
                        alarm.daysOfWeek.setDaysOfWeek(!isActivated, AlarmItemAdapter.this.DAY_ORDER[buttonIndex]);
                        if (!isActivated) {
                            AlarmItemAdapter.this.turnOnDayOfWeek(itemHolder, buttonIndex);
                        } else {
                            AlarmItemAdapter.this.turnOffDayOfWeek(itemHolder, buttonIndex);
                            if (!alarm.daysOfWeek.isRepeating()) {
                                TransitionManager.beginDelayedTransition(AlarmItemAdapter.this.mList, AlarmClockFragment.this.mRepeatTransition);
                                itemHolder.repeat.setChecked(false);
                                itemHolder.repeatDays.setVisibility(8);
                                AlarmItemAdapter.this.mRepeatChecked.remove(Long.valueOf(alarm.id));
                                AlarmItemAdapter.this.mPreviousDaysOfWeekMap.putInt("" + alarm.id, 0);
                            }
                        }
                        AlarmClockFragment.this.asyncUpdateAlarm(alarm, false);
                    }
                });
            }
            if (!this.mHasVibrator) {
                itemHolder.vibrate.setVisibility(4);
            } else {
                itemHolder.vibrate.setVisibility(0);
                if (!alarm.vibrate) {
                    itemHolder.vibrate.setChecked(false);
                } else {
                    itemHolder.vibrate.setChecked(true);
                }
            }
            itemHolder.vibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();
                    alarm.vibrate = checked;
                    AlarmClockFragment.this.asyncUpdateAlarm(alarm, false);
                }
            });
            if (Alarm.NO_RINGTONE_URI.equals(alarm.alert)) {
                ringtone = this.mContext.getResources().getString(R.string.silent_alarm_summary);
            } else {
                ringtone = getRingToneTitle(alarm.alert);
            }
            itemHolder.ringtone.setText(ringtone);
            itemHolder.ringtone.setContentDescription(this.mContext.getResources().getString(R.string.ringtone_description) + " " + ringtone);
            itemHolder.ringtone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlarmClockFragment.this.launchRingTonePicker(alarm);
                }
            });
        }

        private void setDigitalTimeAlpha(ItemHolder holder, boolean enabled) {
            float alpha = enabled ? 1.0f : 0.69f;
            holder.clock.setAlpha(alpha);
        }

        private void updateDaysOfWeekButtons(ItemHolder holder, DaysOfWeek daysOfWeek) {
            HashSet<Integer> setDays = daysOfWeek.getSetDays();
            for (int i = 0; i < 7; i++) {
                if (setDays.contains(Integer.valueOf(this.DAY_ORDER[i]))) {
                    turnOnDayOfWeek(holder, i);
                } else {
                    turnOffDayOfWeek(holder, i);
                }
            }
        }

        private void turnOffDayOfWeek(ItemHolder holder, int dayIndex) {
            Button dayButton = holder.dayButtons[dayIndex];
            dayButton.setActivated(false);
            dayButton.setTextColor(AlarmClockFragment.this.getResources().getColor(R.color.clock_white));
        }

        private void turnOnDayOfWeek(ItemHolder holder, int dayIndex) {
            Button dayButton = holder.dayButtons[dayIndex];
            dayButton.setActivated(true);
            dayButton.setTextColor(Utils.getCurrentHourColor());
        }

        private String getRingToneTitle(Uri uri) {
            String title = AlarmClockFragment.this.mRingtoneTitleCache.getString(uri.toString());
            if (title == null) {
                Ringtone ringTone = RingtoneManager.getRingtone(this.mContext, uri);
                title = ringTone.getTitle(this.mContext);
                if (title != null) {
                    AlarmClockFragment.this.mRingtoneTitleCache.putString(uri.toString(), title);
                }
            }
            return title;
        }

        public void setNewAlarm(long alarmId) {
            this.mExpandedId = alarmId;
        }

        private void expandAlarm(final ItemHolder itemHolder, boolean animate) {
            boolean animate2 = animate & (this.mExpandedId != itemHolder.alarm.id);
            if (this.mExpandedItemHolder != null && this.mExpandedItemHolder != itemHolder && this.mExpandedId != itemHolder.alarm.id) {
                collapseAlarm(this.mExpandedItemHolder, animate2);
            }
            bindExpandArea(itemHolder, itemHolder.alarm);
            this.mExpandedId = itemHolder.alarm.id;
            this.mExpandedItemHolder = itemHolder;
            this.mScrollAlarmId = itemHolder.alarm.id;
            final int startingHeight = itemHolder.alarmItem.getHeight();
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, true);
            itemHolder.expandArea.setVisibility(0);
            itemHolder.delete.setVisibility(0);
            setDigitalTimeAlpha(itemHolder, true);
            itemHolder.arrow.setContentDescription(AlarmClockFragment.this.getString(R.string.collapse_alarm));
            if (animate2) {
                final ViewTreeObserver observer = AlarmClockFragment.this.mAlarmsList.getViewTreeObserver();
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (observer.isAlive()) {
                            observer.removeOnPreDrawListener(this);
                        }
                        int endingHeight = itemHolder.alarmItem.getHeight();
                        final int distance = endingHeight - startingHeight;
                        final int collapseHeight = itemHolder.collapseExpandArea.getHeight();
                        itemHolder.alarmItem.getLayoutParams().height = startingHeight;
                        FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams) itemHolder.expandArea.getLayoutParams();
                        expandParams.setMargins(0, -distance, 0, collapseHeight);
                        itemHolder.alarmItem.requestLayout();
                        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(300L);
                        animator.setInterpolator(AlarmClockFragment.this.mExpandInterpolator);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator2) {
                                Float value = (Float) animator2.getAnimatedValue();
                                itemHolder.alarmItem.getLayoutParams().height = (int) ((value.floatValue() * distance) + startingHeight);
                                FrameLayout.LayoutParams expandParams2 = (FrameLayout.LayoutParams) itemHolder.expandArea.getLayoutParams();
                                expandParams2.setMargins(0, (int) (-((1.0f - value.floatValue()) * distance)), 0, collapseHeight);
                                itemHolder.arrow.setRotation(180.0f * value.floatValue());
                                itemHolder.summary.setAlpha(1.0f - value.floatValue());
                                itemHolder.hairLine.setAlpha(1.0f - value.floatValue());
                                itemHolder.alarmItem.requestLayout();
                            }
                        });
                        animator.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                itemHolder.alarmItem.getLayoutParams().height = -2;
                                itemHolder.arrow.setRotation(180.0f);
                                itemHolder.summary.setVisibility(8);
                                itemHolder.hairLine.setVisibility(8);
                                itemHolder.delete.setVisibility(0);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                            }
                        });
                        animator.start();
                        return false;
                    }
                });
            } else {
                itemHolder.arrow.setRotation(180.0f);
            }
        }

        private boolean isAlarmExpanded(Alarm alarm) {
            return this.mExpandedId == alarm.id;
        }

        private void collapseAlarm(final ItemHolder itemHolder, boolean animate) {
            this.mExpandedId = -1L;
            this.mExpandedItemHolder = null;
            final int startingHeight = itemHolder.alarmItem.getHeight();
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, false);
            itemHolder.expandArea.setVisibility(8);
            setDigitalTimeAlpha(itemHolder, itemHolder.onoff.isChecked());
            itemHolder.arrow.setContentDescription(AlarmClockFragment.this.getString(R.string.expand_alarm));
            if (animate) {
                final ViewTreeObserver observer = AlarmClockFragment.this.mAlarmsList.getViewTreeObserver();
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (observer.isAlive()) {
                            observer.removeOnPreDrawListener(this);
                        }
                        int endingHeight = itemHolder.alarmItem.getHeight();
                        final int distance = endingHeight - startingHeight;
                        itemHolder.expandArea.setVisibility(0);
                        itemHolder.delete.setVisibility(8);
                        itemHolder.summary.setVisibility(0);
                        itemHolder.hairLine.setVisibility(0);
                        itemHolder.summary.setAlpha(1.0f);
                        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(250L);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator2) {
                                Float value = (Float) animator2.getAnimatedValue();
                                itemHolder.alarmItem.getLayoutParams().height = (int) ((value.floatValue() * distance) + startingHeight);
                                FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams) itemHolder.expandArea.getLayoutParams();
                                expandParams.setMargins(0, (int) (value.floatValue() * distance), 0, AlarmItemAdapter.this.mCollapseExpandHeight);
                                itemHolder.arrow.setRotation(180.0f * (1.0f - value.floatValue()));
                                itemHolder.delete.setAlpha(value.floatValue());
                                itemHolder.summary.setAlpha(value.floatValue());
                                itemHolder.hairLine.setAlpha(value.floatValue());
                                itemHolder.alarmItem.requestLayout();
                            }
                        });
                        animator.setInterpolator(AlarmClockFragment.this.mCollapseInterpolator);
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                itemHolder.alarmItem.getLayoutParams().height = -2;
                                FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams) itemHolder.expandArea.getLayoutParams();
                                expandParams.setMargins(0, 0, 0, AlarmItemAdapter.this.mCollapseExpandHeight);
                                itemHolder.expandArea.setVisibility(8);
                                itemHolder.arrow.setRotation(0.0f);
                            }
                        });
                        animator.start();
                        return false;
                    }
                });
            } else {
                itemHolder.arrow.setRotation(0.0f);
                itemHolder.hairLine.setTranslationY(0.0f);
            }
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        private View getViewById(long id) {
            ItemHolder h;
            for (int i = 0; i < this.mList.getCount(); i++) {
                View v = this.mList.getChildAt(i);
                if (v != null && (h = (ItemHolder) v.getTag()) != null && h.alarm.id == id) {
                    return v;
                }
            }
            return null;
        }

        public long getExpandedId() {
            return this.mExpandedId;
        }

        public long[] getSelectedAlarmsArray() {
            int index = 0;
            long[] ids = new long[this.mSelectedAlarms.size()];
            Iterator<Long> it = this.mSelectedAlarms.iterator();
            while (it.hasNext()) {
                long id = it.next().longValue();
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public long[] getRepeatArray() {
            int index = 0;
            long[] ids = new long[this.mRepeatChecked.size()];
            Iterator<Long> it = this.mRepeatChecked.iterator();
            while (it.hasNext()) {
                long id = it.next().longValue();
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public Bundle getPreviousDaysOfWeekMap() {
            return this.mPreviousDaysOfWeekMap;
        }

        private void buildHashSetFromArray(long[] ids, HashSet<Long> set) {
            for (long id : ids) {
                set.add(Long.valueOf(id));
            }
        }
    }

    private void startCreatingAlarm() {
        this.mSelectedAlarm = null;
        AlarmUtils.showTimeEditDialog(this, null);
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = AlarmInstance.addInstance(cr, alarm.createInstanceAfter(Calendar.getInstance()));
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private void asyncDeleteAlarm(final Alarm alarm) {
        final Context context = getActivity().getApplicationContext();
        AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();
                    AlarmStateManager.deleteAllInstances(context, alarm.id);
                    Alarm.deleteAlarm(cr, alarm.id);
                    AlarmClockFragment.sDeskClockExtensions.deleteAlarm(AlarmClockFragment.this.getActivity().getApplicationContext(), alarm.id);
                    return null;
                }
                return null;
            }
        };
        this.mUndoShowing = true;
        deleteTask.execute(new Void[0]);
    }

    private void asyncAddAlarm(final Alarm alarm) {
        final Context context = getActivity().getApplicationContext();
        AsyncTask<Void, Void, AlarmInstance> updateTask = new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void... parameters) {
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();
                    Alarm newAlarm = Alarm.addAlarm(cr, alarm);
                    AlarmClockFragment.this.mScrollToAlarmId = newAlarm.id;
                    if (newAlarm.enabled) {
                        AlarmClockFragment.sDeskClockExtensions.addAlarm(AlarmClockFragment.this.getActivity().getApplicationContext(), newAlarm);
                        return AlarmClockFragment.setupAlarmInstance(context, newAlarm);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute(new Void[0]);
    }

    private void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final Context context = getActivity().getApplicationContext();
        AsyncTask<Void, Void, AlarmInstance> updateTask = new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void... parameters) {
                ContentResolver cr = context.getContentResolver();
                AlarmStateManager.deleteAllInstances(context, alarm.id);
                Alarm.updateAlarm(cr, alarm);
                if (alarm.enabled) {
                    return AlarmClockFragment.setupAlarmInstance(context, alarm);
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (popToast && instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute(new Void[0]);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideUndoBar(true, event);
        return false;
    }

    @Override
    public void onFabClick(View view) {
        hideUndoBar(true, null);
        startCreatingAlarm();
    }

    @Override
    public void setFabAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mFab != null && activity.getSelectedTab() == 0) {
            this.mFab.setVisibility(0);
            this.mFab.setImageResource(R.drawable.ic_fab_plus);
            this.mFab.setContentDescription(getString(R.string.button_alarms));
        }
    }

    @Override
    public void setLeftRightButtonAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mLeftButton != null && this.mRightButton != null && activity.getSelectedTab() == 0) {
            this.mLeftButton.setVisibility(4);
            this.mRightButton.setVisibility(4);
        }
    }
}
