package com.android.calendar.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.widget.CalendarAppWidgetModel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CalendarAppWidgetService extends RemoteViewsService {
    static final String[] EVENT_PROJECTION = {"allDay", "begin", "end", "title", "eventLocation", "event_id", "startDay", "endDay", "displayColor", "selfAttendeeStatus"};

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[8] = "calendar_color";
        }
    }

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CalendarFactory(getApplicationContext(), intent);
    }

    public static class CalendarFactory extends BroadcastReceiver implements Loader.OnLoadCompleteListener<Cursor>, RemoteViewsService.RemoteViewsFactory {
        private static CalendarAppWidgetModel mModel;
        private int mAllDayColor;
        private int mAppWidgetId;
        private Context mContext;
        private int mDeclinedColor;
        private CursorLoader mLoader;
        private Resources mResources;
        private int mStandardColor;
        private static long sLastUpdateTime = 21600000;
        private static Object mLock = new Object();
        private static volatile int mSerialNum = 0;
        private static final AtomicInteger currentVersion = new AtomicInteger(0);
        private int mLastSerialNum = -1;
        private final Handler mHandler = new Handler();
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Runnable mTimezoneChanged = new Runnable() {
            @Override
            public void run() {
                if (CalendarFactory.this.mLoader != null) {
                    CalendarFactory.this.mLoader.forceLoad();
                }
            }
        };

        static int access$504() {
            int i = mSerialNum + 1;
            mSerialNum = i;
            return i;
        }

        private Runnable createUpdateLoaderRunnable(final String selection, final BroadcastReceiver.PendingResult result, final int version) {
            return new Runnable() {
                @Override
                public void run() {
                    if (CalendarFactory.this.mLoader != null && version >= CalendarFactory.currentVersion.get()) {
                        Uri uri = CalendarFactory.this.createLoaderUri();
                        CalendarFactory.this.mLoader.setUri(uri);
                        CalendarFactory.this.mLoader.setSelection(selection);
                        synchronized (CalendarFactory.mLock) {
                            CalendarFactory.this.mLastSerialNum = CalendarFactory.access$504();
                        }
                        CalendarFactory.this.mLoader.forceLoad();
                    }
                    result.finish();
                }
            };
        }

        protected CalendarFactory(Context context, Intent intent) {
            this.mContext = context;
            this.mResources = context.getResources();
            this.mAppWidgetId = intent.getIntExtra("appWidgetId", 0);
            this.mDeclinedColor = this.mResources.getColor(R.color.appwidget_item_declined_color);
            this.mStandardColor = this.mResources.getColor(R.color.appwidget_item_standard_color);
            this.mAllDayColor = this.mResources.getColor(R.color.appwidget_item_allday_color);
        }

        public CalendarFactory() {
        }

        @Override
        public void onCreate() {
            String selection = queryForSelection();
            initLoader(selection);
        }

        @Override
        public void onDataSetChanged() {
        }

        @Override
        public void onDestroy() {
            if (this.mLoader != null) {
                this.mLoader.reset();
            }
        }

        @Override
        public RemoteViews getLoadingView() {
            RemoteViews views = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_loading);
            return views;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews views;
            if (position < 0 || position >= getCount()) {
                return null;
            }
            if (mModel == null) {
                RemoteViews views2 = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_loading);
                Intent intent = CalendarAppWidgetProvider.getLaunchFillInIntent(this.mContext, 0L, 0L, 0L, false);
                views2.setOnClickFillInIntent(R.id.appwidget_loading, intent);
                return views2;
            }
            if (mModel.mEventInfos.isEmpty() || mModel.mRowInfos.isEmpty()) {
                RemoteViews views3 = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_no_events);
                Intent intent2 = CalendarAppWidgetProvider.getLaunchFillInIntent(this.mContext, 0L, 0L, 0L, false);
                views3.setOnClickFillInIntent(R.id.appwidget_no_events, intent2);
                return views3;
            }
            CalendarAppWidgetModel.RowInfo rowInfo = mModel.mRowInfos.get(position);
            if (rowInfo.mType == 0) {
                RemoteViews views4 = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_day);
                CalendarAppWidgetModel.DayInfo dayInfo = mModel.mDayInfos.get(rowInfo.mIndex);
                updateTextView(views4, R.id.date, 0, dayInfo.mDayLabel);
                return views4;
            }
            CalendarAppWidgetModel.EventInfo eventInfo = mModel.mEventInfos.get(rowInfo.mIndex);
            if (eventInfo.allDay) {
                views = new RemoteViews(this.mContext.getPackageName(), R.layout.widget_all_day_item);
            } else {
                views = new RemoteViews(this.mContext.getPackageName(), R.layout.widget_item);
            }
            int displayColor = Utils.getDisplayColorFromColor(eventInfo.color);
            long now = System.currentTimeMillis();
            if (!eventInfo.allDay && eventInfo.start <= now && now <= eventInfo.end) {
                views.setInt(R.id.widget_row, "setBackgroundResource", R.drawable.agenda_item_bg_secondary);
            } else {
                views.setInt(R.id.widget_row, "setBackgroundResource", R.drawable.agenda_item_bg_primary);
            }
            if (!eventInfo.allDay) {
                updateTextView(views, R.id.when, eventInfo.visibWhen, eventInfo.when);
                updateTextView(views, R.id.where, eventInfo.visibWhere, eventInfo.where);
            }
            updateTextView(views, R.id.title, eventInfo.visibTitle, eventInfo.title);
            views.setViewVisibility(R.id.agenda_item_color, 0);
            int selfAttendeeStatus = eventInfo.selfAttendeeStatus;
            if (eventInfo.allDay) {
                if (selfAttendeeStatus == 3) {
                    views.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_not_responded_bg);
                    views.setInt(R.id.title, "setTextColor", displayColor);
                } else {
                    views.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_responded_bg);
                    views.setInt(R.id.title, "setTextColor", this.mAllDayColor);
                }
                if (selfAttendeeStatus == 2) {
                    views.setInt(R.id.agenda_item_color, "setColorFilter", Utils.getDeclinedColorFromColor(displayColor));
                } else {
                    views.setInt(R.id.agenda_item_color, "setColorFilter", displayColor);
                }
            } else if (selfAttendeeStatus == 2) {
                views.setInt(R.id.title, "setTextColor", this.mDeclinedColor);
                views.setInt(R.id.when, "setTextColor", this.mDeclinedColor);
                views.setInt(R.id.where, "setTextColor", this.mDeclinedColor);
                views.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_responded_bg);
                views.setInt(R.id.agenda_item_color, "setColorFilter", Utils.getDeclinedColorFromColor(displayColor));
            } else {
                views.setInt(R.id.title, "setTextColor", this.mStandardColor);
                views.setInt(R.id.when, "setTextColor", this.mStandardColor);
                views.setInt(R.id.where, "setTextColor", this.mStandardColor);
                if (selfAttendeeStatus == 3) {
                    views.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_not_responded_bg);
                } else {
                    views.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_responded_bg);
                }
                views.setInt(R.id.agenda_item_color, "setColorFilter", displayColor);
            }
            long start = eventInfo.start;
            long end = eventInfo.end;
            if (eventInfo.allDay) {
                String tz = Utils.getTimeZone(this.mContext, null);
                Time recycle = new Time();
                start = Utils.convertAlldayLocalToUTC(recycle, start, tz);
                end = Utils.convertAlldayLocalToUTC(recycle, end, tz);
            }
            Intent fillInIntent = CalendarAppWidgetProvider.getLaunchFillInIntent(this.mContext, eventInfo.id, start, end, eventInfo.allDay);
            views.setOnClickFillInIntent(R.id.widget_row, fillInIntent);
            return views;
        }

        @Override
        public int getViewTypeCount() {
            return 5;
        }

        @Override
        public int getCount() {
            if (mModel == null) {
                return 1;
            }
            return Math.max(1, mModel.mRowInfos.size());
        }

        @Override
        public long getItemId(int position) {
            if (mModel == null || mModel.mRowInfos.isEmpty() || position >= getCount()) {
                return 0L;
            }
            CalendarAppWidgetModel.RowInfo rowInfo = mModel.mRowInfos.get(position);
            if (rowInfo.mType == 0) {
                return rowInfo.mIndex;
            }
            CalendarAppWidgetModel.EventInfo eventInfo = mModel.mEventInfos.get(rowInfo.mIndex);
            long result = (31 * 1) + ((long) ((int) (eventInfo.id ^ (eventInfo.id >>> 32))));
            return (31 * result) + ((long) ((int) (eventInfo.start ^ (eventInfo.start >>> 32))));
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public void initLoader(String selection) {
            Uri uri = createLoaderUri();
            this.mLoader = new CursorLoader(this.mContext, uri, CalendarAppWidgetService.EVENT_PROJECTION, selection, null, "startDay ASC, startMinute ASC, endDay ASC, endMinute ASC LIMIT 100");
            this.mLoader.setUpdateThrottle(500L);
            synchronized (mLock) {
                int i = mSerialNum + 1;
                mSerialNum = i;
                this.mLastSerialNum = i;
            }
            this.mLoader.registerListener(this.mAppWidgetId, this);
            this.mLoader.startLoading();
        }

        private String queryForSelection() {
            return Utils.getHideDeclinedEvents(this.mContext) ? "visible=1 AND selfAttendeeStatus!=2" : "visible=1";
        }

        private Uri createLoaderUri() {
            long now = System.currentTimeMillis();
            long begin = now - 86400000;
            long end = 604800000 + now + 86400000;
            Uri uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI, Long.toString(begin) + "/" + end);
            return uri;
        }

        protected static CalendarAppWidgetModel buildAppWidgetModel(Context context, Cursor cursor, String timeZone) {
            CalendarAppWidgetModel model = new CalendarAppWidgetModel(context, timeZone);
            model.buildFromCursor(cursor, timeZone);
            return model;
        }

        private long calculateUpdateTime(CalendarAppWidgetModel model, long now, String timeZone) {
            long minUpdateTime = getNextMidnightTimeMillis(timeZone);
            for (CalendarAppWidgetModel.EventInfo event : model.mEventInfos) {
                long start = event.start;
                long end = event.end;
                if (now < start) {
                    minUpdateTime = Math.min(minUpdateTime, start);
                } else if (now < end) {
                    minUpdateTime = Math.min(minUpdateTime, end);
                }
            }
            return minUpdateTime;
        }

        private static long getNextMidnightTimeMillis(String timezone) {
            Time time = new Time();
            time.setToNow();
            time.monthDay++;
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            long midnightDeviceTz = time.normalize(true);
            time.timezone = timezone;
            time.setToNow();
            time.monthDay++;
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            long midnightHomeTz = time.normalize(true);
            return Math.min(midnightDeviceTz, midnightHomeTz);
        }

        static void updateTextView(RemoteViews views, int id, int visibility, String string) {
            views.setViewVisibility(id, visibility);
            if (visibility == 0) {
                views.setTextViewText(id, string);
            }
        }

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                synchronized (mLock) {
                    if (cursor.isClosed()) {
                        Log.wtf("CalendarWidget", "Got a closed cursor from onLoadComplete");
                        return;
                    }
                    if (this.mLastSerialNum == mSerialNum) {
                        long now = System.currentTimeMillis();
                        String tz = Utils.getTimeZone(this.mContext, this.mTimezoneChanged);
                        MatrixCursor matrixCursor = Utils.matrixCursorFromCursor(cursor);
                        try {
                            mModel = buildAppWidgetModel(this.mContext, matrixCursor, tz);
                            long triggerTime = calculateUpdateTime(mModel, now, tz);
                            if (triggerTime < now) {
                                Log.w("CalendarWidget", "Encountered bad trigger time " + CalendarAppWidgetService.formatDebugTime(triggerTime, now));
                                triggerTime = now + 21600000;
                            }
                            AlarmManager alertManager = (AlarmManager) this.mContext.getSystemService("alarm");
                            PendingIntent pendingUpdate = CalendarAppWidgetProvider.getUpdateIntent(this.mContext);
                            alertManager.cancel(pendingUpdate);
                            alertManager.set(1, triggerTime, pendingUpdate);
                            Time time = new Time(Utils.getTimeZone(this.mContext, null));
                            time.setToNow();
                            if (time.normalize(true) != sLastUpdateTime) {
                                Time time2 = new Time(Utils.getTimeZone(this.mContext, null));
                                time2.set(sLastUpdateTime);
                                time2.normalize(true);
                                if (time.year != time2.year || time.yearDay != time2.yearDay) {
                                    Intent updateIntent = new Intent(Utils.getWidgetUpdateAction(this.mContext));
                                    this.mContext.sendBroadcast(updateIntent);
                                }
                                sLastUpdateTime = time.toMillis(true);
                            }
                            AppWidgetManager widgetManager = AppWidgetManager.getInstance(this.mContext);
                            if (this.mAppWidgetId == -1) {
                                int[] ids = widgetManager.getAppWidgetIds(CalendarAppWidgetProvider.getComponentName(this.mContext));
                                widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.events_list);
                            } else {
                                widgetManager.notifyAppWidgetViewDataChanged(this.mAppWidgetId, R.id.events_list);
                            }
                        } finally {
                            if (matrixCursor != null) {
                                matrixCursor.close();
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mContext = context;
            final BroadcastReceiver.PendingResult result = goAsync();
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    final String selection = CalendarFactory.this.queryForSelection();
                    if (CalendarFactory.this.mLoader == null) {
                        CalendarFactory.this.mAppWidgetId = -1;
                        CalendarFactory.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                CalendarFactory.this.initLoader(selection);
                                result.finish();
                            }
                        });
                    } else {
                        CalendarFactory.this.mHandler.post(CalendarFactory.this.createUpdateLoaderRunnable(selection, result, CalendarFactory.currentVersion.incrementAndGet()));
                    }
                }
            });
        }
    }

    static String formatDebugTime(long unixTime, long now) {
        Time time = new Time();
        time.set(unixTime);
        long delta = unixTime - now;
        if (delta > 60000) {
            return String.format("[%d] %s (%+d mins)", Long.valueOf(unixTime), time.format("%H:%M:%S"), Long.valueOf(delta / 60000));
        }
        return String.format("[%d] %s (%+d secs)", Long.valueOf(unixTime), time.format("%H:%M:%S"), Long.valueOf(delta / 1000));
    }
}
