package com.android.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsActivity;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

public class CalendarController {
    private static WeakHashMap<Context, WeakReference<CalendarController>> instances = new WeakHashMap<>();
    private static boolean mIsTabletConfig;
    private final Context mContext;
    private int mDetailViewType;
    private Pair<Integer, EventHandler> mFirstEventHandler;
    private Pair<Integer, EventHandler> mToBeAddedFirstEventHandler;
    private final LinkedHashMap<Integer, EventHandler> eventHandlers = new LinkedHashMap<>(5);
    private final LinkedList<Integer> mToBeRemovedEventHandlers = new LinkedList<>();
    private final LinkedHashMap<Integer, EventHandler> mToBeAddedEventHandlers = new LinkedHashMap<>();
    private volatile int mDispatchInProgressCounter = 0;
    private final WeakHashMap<Object, Long> filters = new WeakHashMap<>(1);
    private int mViewType = -1;
    private int mPreviousViewType = -1;
    private long mEventId = -1;
    private Time mTime = new Time();
    private long mDateFlags = 0;
    private final Runnable mUpdateTimezone = new Runnable() {
        @Override
        public void run() {
            CalendarController.this.mTime.switchTimezone(Utils.getTimeZone(CalendarController.this.mContext, this));
        }
    };

    public interface EventHandler {
        long getSupportedEventTypes();

        void handleEvent(EventInfo eventInfo);
    }

    public static class EventInfo {
        public long calendarId;
        public ComponentName componentName;
        public Time endTime;
        public String eventTitle;
        public long eventType;
        public long extraLong;
        public long id;
        public String query;
        public Time selectedTime;
        public Time startTime;
        public int viewType;
        public int x;
        public int y;

        public boolean isAllDay() {
            if (this.eventType == 2) {
                return (this.extraLong & 256) != 0;
            }
            Log.wtf("CalendarController", "illegal call to isAllDay , wrong event type " + this.eventType);
            return false;
        }

        public int getResponse() {
            if (this.eventType != 2) {
                Log.wtf("CalendarController", "illegal call to getResponse , wrong event type " + this.eventType);
                return 0;
            }
            int i = (int) (this.extraLong & 255);
            if (i == 4) {
                return 2;
            }
            if (i == 8) {
                return 4;
            }
            switch (i) {
                case 1:
                    return 0;
                case 2:
                    return 1;
                default:
                    Log.wtf("CalendarController", "Unknown attendee response " + i);
                    return 1;
            }
        }

        public static long buildViewExtraLong(int i, boolean z) {
            long j = z ? 256L : 0L;
            if (i != 4) {
                switch (i) {
                    case 0:
                        return j | 1;
                    case 1:
                        return 2 | j;
                    case 2:
                        return 4 | j;
                    default:
                        Log.wtf("CalendarController", "Unknown attendee response " + i);
                        return j | 1;
                }
            }
            return 8 | j;
        }
    }

    public static CalendarController getInstance(Context context) {
        CalendarController calendarController;
        synchronized (instances) {
            calendarController = null;
            WeakReference<CalendarController> weakReference = instances.get(context);
            if (weakReference != null) {
                calendarController = weakReference.get();
            }
            if (calendarController == null) {
                calendarController = new CalendarController(context);
                instances.put(context, new WeakReference<>(calendarController));
            }
        }
        return calendarController;
    }

    public static void removeInstance(Context context) {
        instances.remove(context);
    }

    private CalendarController(Context context) {
        this.mDetailViewType = -1;
        this.mContext = context;
        this.mUpdateTimezone.run();
        this.mTime.setToNow();
        mIsTabletConfig = Utils.getConfigBool(this.mContext, R.bool.tablet_config);
        this.mDetailViewType = Utils.getSharedPreference(this.mContext, "preferred_detailedView", 2);
    }

    public void sendEventRelatedEvent(Object obj, long j, long j2, long j3, long j4, int i, int i2, long j5) {
        sendEventRelatedEventWithExtra(obj, j, j2, j3, j4, i, i2, EventInfo.buildViewExtraLong(0, false), j5);
    }

    public void sendEventRelatedEventWithExtra(Object obj, long j, long j2, long j3, long j4, int i, int i2, long j5, long j6) {
        sendEventRelatedEventWithExtraWithTitleWithCalendarId(obj, j, j2, j3, j4, i, i2, j5, j6, null, -1L);
    }

    public void sendEventRelatedEventWithExtraWithTitleWithCalendarId(Object obj, long j, long j2, long j3, long j4, int i, int i2, long j5, long j6, String str, long j7) {
        EventInfo eventInfo = new EventInfo();
        eventInfo.eventType = j;
        if (j == 8 || j == 4) {
            eventInfo.viewType = 0;
        }
        eventInfo.id = j2;
        eventInfo.startTime = new Time(Utils.getTimeZone(this.mContext, this.mUpdateTimezone));
        eventInfo.startTime.set(j3);
        if (j6 != -1) {
            eventInfo.selectedTime = new Time(Utils.getTimeZone(this.mContext, this.mUpdateTimezone));
            eventInfo.selectedTime.set(j6);
        } else {
            eventInfo.selectedTime = eventInfo.startTime;
        }
        eventInfo.endTime = new Time(Utils.getTimeZone(this.mContext, this.mUpdateTimezone));
        eventInfo.endTime.set(j4);
        eventInfo.x = i;
        eventInfo.y = i2;
        eventInfo.extraLong = j5;
        eventInfo.eventTitle = str;
        eventInfo.calendarId = j7;
        sendEvent(obj, eventInfo);
    }

    public void sendEvent(Object obj, long j, Time time, Time time2, long j2, int i) {
        sendEvent(obj, j, time, time2, time, j2, i, 2L, null, null);
    }

    public void sendEvent(Object obj, long j, Time time, Time time2, long j2, int i, long j3, String str, ComponentName componentName) {
        sendEvent(obj, j, time, time2, time, j2, i, j3, str, componentName);
    }

    public void sendEvent(Object obj, long j, Time time, Time time2, Time time3, long j2, int i, long j3, String str, ComponentName componentName) {
        EventInfo eventInfo = new EventInfo();
        eventInfo.eventType = j;
        eventInfo.startTime = time;
        eventInfo.selectedTime = time3;
        eventInfo.endTime = time2;
        eventInfo.id = j2;
        eventInfo.viewType = i;
        eventInfo.query = str;
        eventInfo.componentName = componentName;
        eventInfo.extraLong = j3;
        sendEvent(obj, eventInfo);
    }

    public void sendEvent(Object obj, EventInfo eventInfo) {
        boolean z;
        long millis;
        EventHandler value;
        EventHandler eventHandler;
        Long l = this.filters.get(obj);
        if (l != null && (l.longValue() & eventInfo.eventType) != 0) {
            return;
        }
        this.mPreviousViewType = this.mViewType;
        if (eventInfo.viewType == -1) {
            eventInfo.viewType = this.mDetailViewType;
            this.mViewType = this.mDetailViewType;
        } else if (eventInfo.viewType == 0) {
            eventInfo.viewType = this.mViewType;
        } else if (eventInfo.viewType != 5) {
            this.mViewType = eventInfo.viewType;
            if (eventInfo.viewType == 1 || eventInfo.viewType == 2 || (Utils.getAllowWeekForDetailView() && eventInfo.viewType == 3)) {
                this.mDetailViewType = this.mViewType;
            }
        }
        long millis2 = Utils.getFirstDisplayTimeInCalendar(this.mContext).toMillis(false);
        long millis3 = millis2 - 1;
        if (eventInfo.startTime != null) {
            millis3 = eventInfo.startTime.toMillis(false);
        }
        if (eventInfo.selectedTime == null || eventInfo.selectedTime.toMillis(false) < millis2) {
            if (millis3 >= millis2) {
                long millis4 = this.mTime.toMillis(false);
                if (eventInfo.startTime != null && (millis4 < millis3 || (eventInfo.endTime != null && millis4 > eventInfo.endTime.toMillis(false)))) {
                    this.mTime.set(eventInfo.startTime);
                }
            }
            eventInfo.selectedTime = this.mTime;
        } else {
            this.mTime.set(eventInfo.selectedTime);
        }
        if (eventInfo.eventType == 1024) {
            this.mDateFlags = eventInfo.extraLong;
        }
        if (millis3 < millis2) {
            eventInfo.startTime = this.mTime;
            Log.v("CalendarController", "Event's start time(0) be changed.");
        }
        if ((eventInfo.eventType & 13) != 0) {
            if (eventInfo.id > 0) {
                this.mEventId = eventInfo.id;
            } else {
                this.mEventId = -1L;
            }
        }
        eventInfo.startTime = Utils.getValidTimeInCalendar(this.mContext, eventInfo.startTime);
        eventInfo.endTime = Utils.getValidTimeInCalendar(this.mContext, eventInfo.endTime);
        eventInfo.selectedTime = Utils.getValidTimeInCalendar(this.mContext, eventInfo.selectedTime);
        this.mTime = Utils.getValidTimeInCalendar(this.mContext, this.mTime);
        synchronized (this) {
            this.mDispatchInProgressCounter++;
            if (this.mFirstEventHandler == null || (eventHandler = (EventHandler) this.mFirstEventHandler.second) == null || (eventHandler.getSupportedEventTypes() & eventInfo.eventType) == 0 || this.mToBeRemovedEventHandlers.contains(this.mFirstEventHandler.first)) {
                z = false;
            } else {
                eventHandler.handleEvent(eventInfo);
                z = true;
            }
            for (Map.Entry<Integer, EventHandler> entry : this.eventHandlers.entrySet()) {
                int iIntValue = entry.getKey().intValue();
                if ((this.mFirstEventHandler == null || iIntValue != ((Integer) this.mFirstEventHandler.first).intValue()) && (value = entry.getValue()) != null) {
                    if ((eventInfo.eventType & value.getSupportedEventTypes()) != 0 && !this.mToBeRemovedEventHandlers.contains(Integer.valueOf(iIntValue))) {
                        value.handleEvent(eventInfo);
                        z = true;
                    }
                }
            }
            this.mDispatchInProgressCounter--;
            if (this.mDispatchInProgressCounter == 0) {
                if (this.mToBeRemovedEventHandlers.size() > 0) {
                    for (Integer num : this.mToBeRemovedEventHandlers) {
                        this.eventHandlers.remove(num);
                        if (this.mFirstEventHandler != null && num.equals(this.mFirstEventHandler.first)) {
                            this.mFirstEventHandler = null;
                        }
                    }
                    this.mToBeRemovedEventHandlers.clear();
                }
                if (this.mToBeAddedFirstEventHandler != null) {
                    this.mFirstEventHandler = this.mToBeAddedFirstEventHandler;
                    this.mToBeAddedFirstEventHandler = null;
                }
                if (this.mToBeAddedEventHandlers.size() > 0) {
                    for (Map.Entry<Integer, EventHandler> entry2 : this.mToBeAddedEventHandlers.entrySet()) {
                        this.eventHandlers.put(entry2.getKey(), entry2.getValue());
                    }
                    this.mToBeAddedEventHandlers.clear();
                }
            }
        }
        if (z) {
            return;
        }
        if (eventInfo.eventType == 64) {
            launchSettings();
            return;
        }
        if (eventInfo.eventType == 2048) {
            launchSelectVisibleCalendars();
            return;
        }
        if (eventInfo.endTime != null) {
            millis = eventInfo.endTime.toMillis(false);
        } else {
            millis = -1;
        }
        if (eventInfo.eventType == 1) {
            launchCreateEvent(eventInfo.startTime.toMillis(false), millis, eventInfo.extraLong == 16, eventInfo.eventTitle, eventInfo.calendarId);
            return;
        }
        if (eventInfo.eventType == 2) {
            launchViewEvent(eventInfo.id, eventInfo.startTime.toMillis(false), millis, eventInfo.getResponse());
            return;
        }
        if (eventInfo.eventType == 8) {
            launchEditEvent(eventInfo.id, eventInfo.startTime.toMillis(false), millis, true);
            return;
        }
        if (eventInfo.eventType == 4) {
            launchEditEvent(eventInfo.id, eventInfo.startTime.toMillis(false), millis, false);
        } else if (eventInfo.eventType == 16) {
            launchDeleteEvent(eventInfo.id, eventInfo.startTime.toMillis(false), millis);
        } else if (eventInfo.eventType == 256) {
            launchSearch(eventInfo.id, eventInfo.query, eventInfo.componentName);
        }
    }

    public void registerEventHandler(int i, EventHandler eventHandler) {
        synchronized (this) {
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeAddedEventHandlers.put(Integer.valueOf(i), eventHandler);
            } else {
                this.eventHandlers.put(Integer.valueOf(i), eventHandler);
            }
        }
    }

    public void registerFirstEventHandler(int i, EventHandler eventHandler) {
        synchronized (this) {
            registerEventHandler(i, eventHandler);
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeAddedFirstEventHandler = new Pair<>(Integer.valueOf(i), eventHandler);
            } else {
                this.mFirstEventHandler = new Pair<>(Integer.valueOf(i), eventHandler);
            }
        }
    }

    public void deregisterEventHandler(Integer num) {
        synchronized (this) {
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeRemovedEventHandlers.add(num);
            } else {
                this.eventHandlers.remove(num);
                if (this.mFirstEventHandler != null && this.mFirstEventHandler.first == num) {
                    this.mFirstEventHandler = null;
                }
            }
        }
    }

    public void deregisterAllEventHandlers() {
        synchronized (this) {
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeRemovedEventHandlers.addAll(this.eventHandlers.keySet());
            } else {
                this.eventHandlers.clear();
                this.mFirstEventHandler = null;
            }
        }
    }

    public long getTime() {
        return this.mTime.toMillis(false);
    }

    public long getDateFlags() {
        return this.mDateFlags;
    }

    public void setTime(long j) {
        this.mTime.set(j);
    }

    public long getEventId() {
        return this.mEventId;
    }

    public int getViewType() {
        return this.mViewType;
    }

    public int getPreviousViewType() {
        return this.mPreviousViewType;
    }

    private void launchSelectVisibleCalendars() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, SelectVisibleCalendarsActivity.class);
        intent.setFlags(537001984);
        this.mContext.startActivity(intent);
    }

    private void launchSettings() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, CalendarSettingsActivity.class);
        intent.setFlags(537001984);
        this.mContext.startActivity(intent);
    }

    private void launchCreateEvent(long j, long j2, boolean z, String str, long j3) {
        Intent intentGenerateCreateEventIntent = generateCreateEventIntent(j, j2, z, str, j3);
        this.mEventId = -1L;
        this.mContext.startActivity(intentGenerateCreateEventIntent);
    }

    public Intent generateCreateEventIntent(long j, long j2, boolean z, String str, long j3) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, EditEventActivity.class);
        intent.putExtra("beginTime", j);
        intent.putExtra("endTime", j2);
        intent.putExtra("allDay", z);
        intent.putExtra("calendar_id", j3);
        intent.putExtra("title", str);
        return intent;
    }

    public void launchViewEvent(long j, long j2, long j3, int i) {
        if (mIsTabletConfig) {
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j));
        intent.setClass(this.mContext, AllInOneActivity.class);
        intent.putExtra("beginTime", j2);
        intent.putExtra("endTime", j3);
        intent.putExtra("attendeeStatus", i);
        intent.setFlags(67108864);
        this.mContext.startActivity(intent);
    }

    private void launchEditEvent(long j, long j2, long j3, boolean z) {
        Intent intent = new Intent("android.intent.action.EDIT", ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j));
        intent.putExtra("beginTime", j2);
        intent.putExtra("endTime", j3);
        intent.setClass(this.mContext, EditEventActivity.class);
        intent.putExtra("editMode", z);
        this.mEventId = j;
        this.mContext.startActivity(intent);
    }

    private void launchDeleteEvent(long j, long j2, long j3) {
        launchDeleteEventAndFinish(null, j, j2, j3, -1);
    }

    private void launchDeleteEventAndFinish(Activity activity, long j, long j2, long j3, int i) {
        new DeleteEventHelper(this.mContext, activity, activity != null).delete(j2, j3, j, i);
    }

    private void launchSearch(long j, String str, ComponentName componentName) {
        SearchableInfo searchableInfo = ((SearchManager) this.mContext.getSystemService("search")).getSearchableInfo(componentName);
        Intent intent = new Intent("android.intent.action.SEARCH");
        intent.putExtra("query", str);
        if (searchableInfo != null) {
            intent.setComponent(searchableInfo.getSearchActivity());
        }
        intent.addFlags(536870912);
        this.mContext.startActivity(intent);
    }

    public void refreshCalendars() {
        Account[] accounts = AccountManager.get(this.mContext).getAccounts();
        Log.d("CalendarController", "Refreshing " + accounts.length + " accounts");
        String authority = CalendarContract.Calendars.CONTENT_URI.getAuthority();
        for (int i = 0; i < accounts.length; i++) {
            if (Log.isLoggable("CalendarController", 3)) {
                Log.d("CalendarController", "Refreshing calendars for: " + accounts[i]);
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean("force", true);
            ContentResolver.requestSync(accounts[i], authority, bundle);
        }
    }

    public void setViewType(int i) {
        this.mViewType = i;
    }

    public void setEventId(long j) {
        this.mEventId = j;
    }
}
