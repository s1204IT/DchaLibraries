package com.android.calendar.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static final String[] CALENDAR_PERMISSION;
    public static boolean CALENDAR_PERMISSION_GRANTED;
    static final String[] EVENT_PROJECTION = {"allDay", "begin", "end", "title", "eventLocation", "event_id", "startDay", "endDay", "displayColor", "selfAttendeeStatus"};
    private static CalendarFactory calendarFactory;
    private static IntentFilter intentFilter;

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[8] = "calendar_color";
        }
        CALENDAR_PERMISSION = new String[]{"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
        CALENDAR_PERMISSION_GRANTED = false;
    }

    private boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPermissions() {
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            return false;
        }
        return true;
    }

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        CALENDAR_PERMISSION_GRANTED = checkPermissions();
        return new CalendarFactory(getApplicationContext(), intent);
    }

    public static class CalendarFactory extends BroadcastReceiver implements Loader.OnLoadCompleteListener<Cursor>, RemoteViewsService.RemoteViewsFactory {
        private static CalendarFactory mLastRegisterListener;
        private static CursorLoader mLoader;
        private static CalendarAppWidgetModel mModel;
        private int mAllDayColor;
        private int mAppWidgetId;
        private Context mContext;
        private int mDeclinedColor;
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
                if (CalendarFactory.mLoader != null) {
                    CalendarFactory.mLoader.forceLoad();
                }
            }
        };

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
            if (CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED) {
                IntentFilter unused = CalendarAppWidgetService.intentFilter = new IntentFilter("CalendarProvider2.intent.action.PROVIDER_CHANGED");
                CalendarFactory unused2 = CalendarAppWidgetService.calendarFactory = new CalendarFactory();
                this.mContext.getApplicationContext().registerReceiver(CalendarAppWidgetService.calendarFactory, CalendarAppWidgetService.intentFilter);
                initLoader(queryForSelection());
            }
        }

        @Override
        public void onDataSetChanged() {
        }

        @Override
        public void onDestroy() {
            if (mLoader != null) {
                mLoader.reset();
                mLoader = null;
            }
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_loading);
        }

        @Override
        public RemoteViews getViewAt(int i) {
            RemoteViews remoteViews;
            if (i < 0 || i >= getCount()) {
                return null;
            }
            if (mModel == null) {
                RemoteViews remoteViews2 = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_loading);
                remoteViews2.setOnClickPendingIntent(R.id.appwidget_loading, PendingIntent.getActivity(this.mContext, 0, CalendarAppWidgetProvider.getLaunchFillInIntent(this.mContext, 0L, 0L, 0L, false), 0));
                return remoteViews2;
            }
            if (!mModel.mEventInfos.isEmpty() && !mModel.mRowInfos.isEmpty()) {
                CalendarAppWidgetModel.RowInfo rowInfo = mModel.mRowInfos.get(i);
                if (rowInfo.mType == 0) {
                    RemoteViews remoteViews3 = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_day);
                    updateTextView(remoteViews3, R.id.date, 0, mModel.mDayInfos.get(rowInfo.mIndex).mDayLabel);
                    return remoteViews3;
                }
                CalendarAppWidgetModel.EventInfo eventInfo = mModel.mEventInfos.get(rowInfo.mIndex);
                if (eventInfo.allDay) {
                    remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.widget_all_day_item);
                } else {
                    remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.widget_item);
                }
                int displayColorFromColor = Utils.getDisplayColorFromColor(eventInfo.color);
                long jCurrentTimeMillis = System.currentTimeMillis();
                if (!eventInfo.allDay && eventInfo.start <= jCurrentTimeMillis && jCurrentTimeMillis <= eventInfo.end) {
                    remoteViews.setInt(R.id.widget_row, "setBackgroundResource", R.drawable.agenda_item_bg_secondary);
                } else {
                    remoteViews.setInt(R.id.widget_row, "setBackgroundResource", R.drawable.agenda_item_bg_primary);
                }
                if (!eventInfo.allDay) {
                    updateTextView(remoteViews, R.id.when, eventInfo.visibWhen, eventInfo.when);
                    updateTextView(remoteViews, R.id.where, eventInfo.visibWhere, eventInfo.where);
                }
                updateTextView(remoteViews, R.id.title, eventInfo.visibTitle, eventInfo.title);
                remoteViews.setViewVisibility(R.id.agenda_item_color, 0);
                int i2 = eventInfo.selfAttendeeStatus;
                if (eventInfo.allDay) {
                    if (i2 == 3) {
                        remoteViews.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_not_responded_bg);
                        remoteViews.setInt(R.id.title, "setTextColor", displayColorFromColor);
                    } else {
                        remoteViews.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_responded_bg);
                        remoteViews.setInt(R.id.title, "setTextColor", this.mAllDayColor);
                    }
                    if (i2 != 2) {
                        remoteViews.setInt(R.id.agenda_item_color, "setColorFilter", displayColorFromColor);
                    } else {
                        remoteViews.setInt(R.id.agenda_item_color, "setColorFilter", Utils.getDeclinedColorFromColor(displayColorFromColor));
                    }
                } else if (i2 == 2) {
                    remoteViews.setInt(R.id.title, "setTextColor", this.mDeclinedColor);
                    remoteViews.setInt(R.id.when, "setTextColor", this.mDeclinedColor);
                    remoteViews.setInt(R.id.where, "setTextColor", this.mDeclinedColor);
                    remoteViews.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_responded_bg);
                    remoteViews.setInt(R.id.agenda_item_color, "setColorFilter", Utils.getDeclinedColorFromColor(displayColorFromColor));
                } else {
                    remoteViews.setInt(R.id.title, "setTextColor", this.mStandardColor);
                    remoteViews.setInt(R.id.when, "setTextColor", this.mStandardColor);
                    remoteViews.setInt(R.id.where, "setTextColor", this.mStandardColor);
                    if (i2 == 3) {
                        remoteViews.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_not_responded_bg);
                    } else {
                        remoteViews.setInt(R.id.agenda_item_color, "setImageResource", R.drawable.widget_chip_responded_bg);
                    }
                    remoteViews.setInt(R.id.agenda_item_color, "setColorFilter", displayColorFromColor);
                }
                long jConvertAlldayLocalToUTC = eventInfo.start;
                long jConvertAlldayLocalToUTC2 = eventInfo.end;
                if (eventInfo.allDay) {
                    String timeZone = Utils.getTimeZone(this.mContext, null);
                    Time time = new Time();
                    jConvertAlldayLocalToUTC = Utils.convertAlldayLocalToUTC(time, jConvertAlldayLocalToUTC, timeZone);
                    jConvertAlldayLocalToUTC2 = Utils.convertAlldayLocalToUTC(time, jConvertAlldayLocalToUTC2, timeZone);
                }
                remoteViews.setOnClickFillInIntent(R.id.widget_row, CalendarAppWidgetProvider.getLaunchFillInIntent(this.mContext, eventInfo.id, jConvertAlldayLocalToUTC, jConvertAlldayLocalToUTC2, eventInfo.allDay));
                return remoteViews;
            }
            RemoteViews remoteViews4 = new RemoteViews(this.mContext.getPackageName(), R.layout.appwidget_no_events);
            remoteViews4.setOnClickPendingIntent(R.id.appwidget_no_events, PendingIntent.getActivity(this.mContext, 0, CalendarAppWidgetProvider.getLaunchFillInIntent(this.mContext, 0L, 0L, 0L, false), 0));
            return remoteViews4;
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
        public long getItemId(int i) {
            if (mModel == null || mModel.mRowInfos.isEmpty() || i >= getCount()) {
                return 0L;
            }
            CalendarAppWidgetModel.RowInfo rowInfo = mModel.mRowInfos.get(i);
            if (rowInfo.mType == 0) {
                return rowInfo.mIndex;
            }
            CalendarAppWidgetModel.EventInfo eventInfo = mModel.mEventInfos.get(rowInfo.mIndex);
            return (31 * (((long) ((int) (eventInfo.id ^ (eventInfo.id >>> 32)))) + 31)) + ((long) ((int) (eventInfo.start ^ (eventInfo.start >>> 32))));
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public void initLoader(String str) {
            Log.d("CalendarWidget", "Querying for widget events...");
            mLoader = new CursorLoader(this.mContext, createLoaderUri(), CalendarAppWidgetService.EVENT_PROJECTION, str, null, "startDay ASC, startMinute ASC, endDay ASC, endMinute ASC LIMIT 100");
            mLoader.setUpdateThrottle(500L);
            synchronized (mLock) {
                int i = mSerialNum + 1;
                mSerialNum = i;
                this.mLastSerialNum = i;
            }
            mLoader.registerListener(this.mAppWidgetId, this);
            mLastRegisterListener = this;
            mLoader.startLoading();
        }

        public void resetLoader(String str, int i) {
            Uri uriCreateLoaderUri = createLoaderUri();
            Log.d("CalendarWidget", "CalendarAppWidgetService restarLoad uri: " + uriCreateLoaderUri);
            synchronized (mLock) {
                int i2 = mSerialNum + 1;
                mSerialNum = i2;
                this.mLastSerialNum = i2;
            }
            if (mLastRegisterListener != null) {
                mLoader.unregisterListener(mLastRegisterListener);
            }
            mLoader.registerListener(this.mAppWidgetId, this);
            mLastRegisterListener = this;
            mLoader.setSelection(str);
            mLoader.setUri(uriCreateLoaderUri);
            if (i < currentVersion.get()) {
                return;
            }
            mLoader.forceLoad();
        }

        private String queryForSelection() {
            return Utils.getHideDeclinedEvents(this.mContext) ? "visible=1 AND selfAttendeeStatus!=2" : "visible=1";
        }

        private Uri createLoaderUri() {
            long jCurrentTimeMillis = System.currentTimeMillis();
            return Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI, Long.toString(jCurrentTimeMillis - 86400000) + "/" + (jCurrentTimeMillis + 604800000 + 86400000));
        }

        protected static CalendarAppWidgetModel buildAppWidgetModel(Context context, Cursor cursor, String str) {
            CalendarAppWidgetModel calendarAppWidgetModel = new CalendarAppWidgetModel(context, str);
            calendarAppWidgetModel.buildFromCursor(cursor, str);
            return calendarAppWidgetModel;
        }

        private long calculateUpdateTime(CalendarAppWidgetModel calendarAppWidgetModel, long j, String str) {
            long nextMidnightTimeMillis = getNextMidnightTimeMillis(str);
            for (CalendarAppWidgetModel.EventInfo eventInfo : calendarAppWidgetModel.mEventInfos) {
                long j2 = eventInfo.start;
                long j3 = eventInfo.end;
                if (j < j2) {
                    nextMidnightTimeMillis = Math.min(nextMidnightTimeMillis, j2);
                } else if (j < j3) {
                    nextMidnightTimeMillis = Math.min(nextMidnightTimeMillis, j3);
                }
            }
            return nextMidnightTimeMillis;
        }

        private static long getNextMidnightTimeMillis(String str) {
            Time time = new Time();
            time.setToNow();
            time.monthDay++;
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            long jNormalize = time.normalize(true);
            time.timezone = str;
            time.setToNow();
            time.monthDay++;
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            return Math.min(jNormalize, time.normalize(true));
        }

        static void updateTextView(RemoteViews remoteViews, int i, int i2, String str) {
            remoteViews.setViewVisibility(i, i2);
            if (i2 == 0) {
                remoteViews.setTextViewText(i, str);
            }
        }

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {
            if (!CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED || cursor == null) {
                return;
            }
            synchronized (mLock) {
                if (cursor.isClosed()) {
                    Log.wtf("CalendarWidget", "Got a closed cursor from onLoadComplete");
                    return;
                }
                if (this.mLastSerialNum != mSerialNum) {
                    return;
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                String timeZone = Utils.getTimeZone(this.mContext, this.mTimezoneChanged);
                MatrixCursor matrixCursorMatrixCursorFromCursor = Utils.matrixCursorFromCursor(cursor);
                try {
                    mModel = buildAppWidgetModel(this.mContext, matrixCursorMatrixCursorFromCursor, timeZone);
                    long jCalculateUpdateTime = calculateUpdateTime(mModel, jCurrentTimeMillis, timeZone);
                    if (jCalculateUpdateTime < jCurrentTimeMillis) {
                        Log.w("CalendarWidget", "Encountered bad trigger time " + CalendarAppWidgetService.formatDebugTime(jCalculateUpdateTime, jCurrentTimeMillis));
                        jCalculateUpdateTime = 21600000 + jCurrentTimeMillis;
                    }
                    AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
                    PendingIntent updateIntent = CalendarAppWidgetProvider.getUpdateIntent(this.mContext);
                    alarmManager.cancel(updateIntent);
                    alarmManager.set(1, jCalculateUpdateTime, updateIntent);
                    Time time = new Time(Utils.getTimeZone(this.mContext, null));
                    time.setToNow();
                    if (time.normalize(true) != sLastUpdateTime) {
                        Time time2 = new Time(Utils.getTimeZone(this.mContext, null));
                        time2.set(sLastUpdateTime);
                        time2.normalize(true);
                        if (time.year != time2.year || time.yearDay != time2.yearDay) {
                            this.mContext.sendBroadcast(new Intent(Utils.getWidgetUpdateAction(this.mContext)));
                        }
                        sLastUpdateTime = time.toMillis(true);
                    }
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
                    if (this.mAppWidgetId == -1) {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(CalendarAppWidgetProvider.getComponentName(this.mContext)), R.id.events_list);
                    } else {
                        appWidgetManager.notifyAppWidgetViewDataChanged(this.mAppWidgetId, R.id.events_list);
                    }
                } finally {
                    if (matrixCursorMatrixCursorFromCursor != null) {
                        matrixCursorMatrixCursorFromCursor.close();
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("CalendarWidget", "AppWidgetService received an intent. It was " + intent.toString());
            if (!CalendarAppWidgetService.CALENDAR_PERMISSION_GRANTED) {
                return;
            }
            this.mContext = context;
            final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    final String strQueryForSelection = CalendarFactory.this.queryForSelection();
                    CalendarFactory.this.mAppWidgetId = -1;
                    CalendarFactory.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (CalendarFactory.mLoader != null) {
                                CalendarFactory.this.resetLoader(strQueryForSelection, CalendarFactory.currentVersion.incrementAndGet());
                            } else {
                                CalendarFactory.this.initLoader(strQueryForSelection);
                            }
                            pendingResultGoAsync.finish();
                        }
                    });
                }
            });
        }
    }

    static String formatDebugTime(long j, long j2) {
        Time time = new Time();
        time.set(j);
        long j3 = j - j2;
        if (j3 > 60000) {
            return String.format("[%d] %s (%+d mins)", Long.valueOf(j), time.format("%H:%M:%S"), Long.valueOf(j3 / 60000));
        }
        return String.format("[%d] %s (%+d secs)", Long.valueOf(j), time.format("%H:%M:%S"), Long.valueOf(j3 / 1000));
    }
}
