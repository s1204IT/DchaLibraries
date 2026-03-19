package com.android.calendar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.DeleteEventHelper;
import com.android.calendar.alerts.QuickResponseActivity;
import com.android.calendar.event.AttendeesView;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.event.EditEventHelper;
import com.android.calendar.event.EventColorPickerDialog;
import com.android.calendar.event.EventViewUtils;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.colorpicker.ColorPickerSwatch;
import com.android.colorpicker.HsvColorComparator;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKUtils;
import com.mediatek.calendar.ext.OpCalendarCustomizationFactoryBase;
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.IOptionsMenuExt;
import com.mediatek.calendar.nfc.NfcHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EventInfoFragment extends DialogFragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, CalendarController.EventHandler, DeleteEventHelper.DeleteNotifyListener, ColorPickerSwatch.OnColorSelectedListener {
    static final String[] CALENDARS_PROJECTION;
    static final String[] COLORS_PROJECTION;
    private static int DIALOG_TOP_MARGIN;
    private static final String[] REMINDERS_PROJECTION;
    private static int mCustomAppIconSize;
    private static int mDialogHeight;
    private static int mDialogWidth;
    private static float mScale;
    private Button emailAttendeesButton;
    ArrayList<CalendarEventModel.Attendee> mAcceptedAttendees;
    private Activity mActivity;
    private Button mAddReminderBtn;
    private boolean mAllDay;
    private ObjectAnimator mAnimateAlpha;
    private int mAttendeeResponseFromIntent;
    private Cursor mAttendeesCursor;
    private String mCalendarAllowedReminders;
    private int mCalendarColor;
    private boolean mCalendarColorInitialized;
    private String mCalendarOwnerAccount;
    private long mCalendarOwnerAttendeeId;
    private Cursor mCalendarsCursor;
    private boolean mCanModifyCalendar;
    private boolean mCanModifyEvent;
    private EventColorPickerDialog mColorPickerDialog;
    private int[] mColors;
    private Context mContext;
    private CalendarController mController;
    private int mCurrentColor;
    private boolean mCurrentColorInitialized;
    private int mCurrentColorKey;
    private int mCurrentQuery;
    ArrayList<CalendarEventModel.Attendee> mDeclinedAttendees;
    private int mDefaultReminderMinutes;
    private boolean mDeleteDialogVisible;
    private DeleteEventHelper mDeleteHelper;
    private ExpandableTextView mDesc;
    private boolean mDismissOnResume;
    private SparseIntArray mDisplayColorKeyMap;
    private EditResponseHelper mEditResponseHelper;
    private long mEndMillis;
    private View mErrorMsgView;
    private Cursor mEventCursor;
    private boolean mEventDeletionStarted;
    private long mEventId;
    private String mEventOrganizerDisplayName;
    private String mEventOrganizerEmail;
    private QueryHandler mHandler;
    private boolean mHasAlarm;
    private boolean mHasAttendeeData;
    private View mHeadlines;
    private boolean mInitRemindersFinished;
    private boolean mIsBusyFreeCalendar;
    private boolean mIsDialog;
    private boolean mIsOrganizer;
    private boolean mIsPaused;
    private boolean mIsRepeating;
    private boolean mIsTabletConfig;
    private final Runnable mLoadingMsgAlphaUpdater;
    private long mLoadingMsgStartTime;
    private View mLoadingMsgView;
    private AttendeesView mLongAttendees;
    private int mMaxReminders;
    private Menu mMenu;
    private int mMinTop;
    private boolean mNoCrossFade;
    ArrayList<CalendarEventModel.Attendee> mNoResponseAttendees;
    private int mNumOfAttendees;
    IOptionsMenuExt mOptionsMenuExt;
    private int mOriginalAttendeeResponse;
    private int mOriginalColor;
    private boolean mOriginalColorInitialized;
    public ArrayList<CalendarEventModel.ReminderEntry> mOriginalReminders;
    private boolean mOwnerCanRespond;
    private AdapterView.OnItemSelectedListener mReminderChangeListener;
    private ArrayList<String> mReminderMethodLabels;
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMinuteLabels;
    private ArrayList<Integer> mReminderMinuteValues;
    private final ArrayList<LinearLayout> mReminderViews;
    public ArrayList<CalendarEventModel.ReminderEntry> mReminders;
    private Cursor mRemindersCursor;
    private RadioGroup mResponseRadioGroup;
    private ScrollView mScrollView;
    private long mStartMillis;
    private String mSyncAccountName;
    private final Runnable mTZUpdater;
    ArrayList<CalendarEventModel.Attendee> mTentativeAttendees;
    private int mTentativeUserSetResponse;
    private TextView mTitle;
    public ArrayList<CalendarEventModel.ReminderEntry> mUnsupportedReminders;
    private Uri mUri;
    private boolean mUserModifiedReminders;
    private int mUserSetResponse;
    private View mView;
    private TextView mWhenDateTime;
    private TextView mWhere;
    private int mWhichDelete;
    private int mWhichEvents;
    private int mWindowStyle;
    private int mX;
    private int mY;
    private final Runnable onDeleteRunnable;
    private static final String[] EVENT_PROJECTION = {"_id", "title", "rrule", "allDay", "calendar_id", "dtstart", "_sync_id", "eventTimezone", "description", "eventLocation", "calendar_access_level", "calendar_color", "eventColor", "hasAttendeeData", "organizer", "hasAlarm", "maxReminders", "allowedReminders", "customAppPackage", "customAppUri", "dtend", "duration", "original_sync_id", "account_type"};
    private static final String[] ATTENDEES_PROJECTION = {"_id", "attendeeName", "attendeeEmail", "attendeeRelationship", "attendeeStatus", "attendeeIdentity", "attendeeIdNamespace"};

    static int access$3676(EventInfoFragment eventInfoFragment, int i) {
        int i2 = i | eventInfoFragment.mCurrentQuery;
        eventInfoFragment.mCurrentQuery = i2;
        return i2;
    }

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[18] = "_id";
            EVENT_PROJECTION[19] = "_id";
            ATTENDEES_PROJECTION[5] = "_id";
            ATTENDEES_PROJECTION[6] = "_id";
        }
        REMINDERS_PROJECTION = new String[]{"_id", "minutes", "method"};
        CALENDARS_PROJECTION = new String[]{"_id", "calendar_displayName", "ownerAccount", "canOrganizerRespond", "account_name", "account_type"};
        COLORS_PROJECTION = new String[]{"_id", "color", "color_index"};
        mScale = 0.0f;
        mCustomAppIconSize = 32;
        mDialogWidth = 500;
        mDialogHeight = 600;
        DIALOG_TOP_MARGIN = 8;
    }

    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            View viewFindViewById;
            int displayColorFromColor;
            Activity activity = EventInfoFragment.this.getActivity();
            if (activity == null || activity.isFinishing()) {
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            ArrayList arrayList = new ArrayList();
            if (i == 4) {
                EventInfoFragment.this.addCursorToArray(arrayList, EventInfoFragment.this.mAttendeesCursor);
                EventInfoFragment.this.mAttendeesCursor = Utils.matrixCursorFromCursor(cursor);
                EventInfoFragment.this.initAttendeesCursor(EventInfoFragment.this.mView);
                EventInfoFragment.this.updateResponse(EventInfoFragment.this.mView);
            } else if (i == 8) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                String string = EventInfoFragment.this.mCalendarsCursor.getString(1);
                spannableStringBuilder.append((CharSequence) string);
                String string2 = EventInfoFragment.this.mCalendarsCursor.getString(2);
                if (cursor.getCount() > 1 && !string.equalsIgnoreCase(string2) && Utils.isValidEmail(string2)) {
                    spannableStringBuilder.append((CharSequence) " (").append((CharSequence) string2).append((CharSequence) ")");
                }
                EventInfoFragment.this.setVisibilityCommon(EventInfoFragment.this.mView, R.id.calendar_container, 0);
                EventInfoFragment.this.setTextCommon(EventInfoFragment.this.mView, R.id.calendar_name, spannableStringBuilder);
            } else if (i == 16) {
                EventInfoFragment.this.addCursorToArray(arrayList, EventInfoFragment.this.mRemindersCursor);
                EventInfoFragment.this.mRemindersCursor = Utils.matrixCursorFromCursor(cursor);
                if (EventInfoFragment.this.mRemindersCursor != null) {
                    EventInfoFragment.this.initReminders(EventInfoFragment.this.mView, EventInfoFragment.this.mRemindersCursor);
                } else {
                    Log.d("EventInfoFragment", "mRemindersCursor is null");
                }
            } else if (i != 32) {
                if (i != 64) {
                    switch (i) {
                        case 1:
                            EventInfoFragment.this.addCursorToArray(arrayList, EventInfoFragment.this.mEventCursor);
                            EventInfoFragment.this.mEventCursor = Utils.matrixCursorFromCursor(cursor, true);
                            if (!EventInfoFragment.this.initEventCursor()) {
                                EventInfoFragment.this.addCursorToArray(arrayList, EventInfoFragment.this.mEventCursor);
                                EventInfoFragment.this.addCursorToArray(arrayList, cursor);
                                EventInfoFragment.this.closeCursors(arrayList);
                                EventInfoFragment.this.displayEventNotFound();
                                return;
                            }
                            if (!EventInfoFragment.this.mCalendarColorInitialized) {
                                EventInfoFragment.this.mCalendarColor = Utils.getDisplayColorFromColor(EventInfoFragment.this.mEventCursor.getInt(11));
                                EventInfoFragment.this.mCalendarColorInitialized = true;
                            }
                            if (!EventInfoFragment.this.mOriginalColorInitialized) {
                                EventInfoFragment eventInfoFragment = EventInfoFragment.this;
                                if (EventInfoFragment.this.mEventCursor.isNull(12)) {
                                    displayColorFromColor = EventInfoFragment.this.mCalendarColor;
                                } else {
                                    displayColorFromColor = Utils.getDisplayColorFromColor(EventInfoFragment.this.mEventCursor.getInt(12));
                                }
                                eventInfoFragment.mOriginalColor = displayColorFromColor;
                                EventInfoFragment.this.mOriginalColorInitialized = true;
                            }
                            if (!EventInfoFragment.this.mCurrentColorInitialized) {
                                EventInfoFragment.this.mCurrentColor = EventInfoFragment.this.mOriginalColor;
                                EventInfoFragment.this.mCurrentColorInitialized = true;
                            }
                            EventInfoFragment.this.updateEvent(EventInfoFragment.this.mView);
                            EventInfoFragment.this.prepareReminders();
                            startQuery(2, null, CalendarContract.Calendars.CONTENT_URI, EventInfoFragment.CALENDARS_PROJECTION, "_id=?", new String[]{Long.toString(EventInfoFragment.this.mEventCursor.getLong(4))}, null);
                            break;
                            break;
                        case 2:
                            EventInfoFragment.this.addCursorToArray(arrayList, EventInfoFragment.this.mCalendarsCursor);
                            EventInfoFragment.this.mCalendarsCursor = Utils.matrixCursorFromCursor(cursor, true);
                            if (EventInfoFragment.this.mCalendarsCursor != null && EventInfoFragment.this.mCalendarsCursor.getCount() != 0) {
                                EventInfoFragment.this.updateCalendar(EventInfoFragment.this.mView);
                                EventInfoFragment.this.updateTitle();
                                startQuery(64, null, CalendarContract.Colors.CONTENT_URI, EventInfoFragment.COLORS_PROJECTION, "account_name=? AND account_type=? AND color_type=1", new String[]{EventInfoFragment.this.mCalendarsCursor.getString(4), EventInfoFragment.this.mCalendarsCursor.getString(5)}, null);
                                if (EventInfoFragment.this.mIsBusyFreeCalendar) {
                                    EventInfoFragment.this.sendAccessibilityEventIfQueryDone(4);
                                } else {
                                    startQuery(4, null, CalendarContract.Attendees.CONTENT_URI, EventInfoFragment.ATTENDEES_PROJECTION, "event_id=?", new String[]{Long.toString(EventInfoFragment.this.mEventId)}, "attendeeName ASC, attendeeEmail ASC");
                                }
                                if (!EventInfoFragment.this.mHasAlarm) {
                                    EventInfoFragment.this.sendAccessibilityEventIfQueryDone(16);
                                } else {
                                    startQuery(16, null, CalendarContract.Reminders.CONTENT_URI, EventInfoFragment.REMINDERS_PROJECTION, "event_id=?", new String[]{Long.toString(EventInfoFragment.this.mEventId)}, null);
                                }
                            } else {
                                Log.e("EventInfoFragment", "the event's calendar data lost, can not found its calendar account.");
                                EventInfoFragment.this.sendAccessibilityEventIfQueryDone(127);
                            }
                            break;
                        default:
                            Log.w("EventInfoFragment", "Warning, the token can not be recognized!! token=" + i);
                            break;
                    }
                } else {
                    ArrayList arrayList2 = new ArrayList();
                    if (cursor.moveToFirst()) {
                        do {
                            int i2 = cursor.getInt(2);
                            int displayColorFromColor2 = Utils.getDisplayColorFromColor(cursor.getInt(1));
                            EventInfoFragment.this.mDisplayColorKeyMap.put(displayColorFromColor2, i2);
                            arrayList2.add(Integer.valueOf(displayColorFromColor2));
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                    Integer[] numArr = new Integer[arrayList2.size()];
                    Arrays.sort((Integer[]) arrayList2.toArray(numArr), new HsvColorComparator());
                    EventInfoFragment.this.mColors = new int[numArr.length];
                    for (int i3 = 0; i3 < numArr.length; i3++) {
                        EventInfoFragment.this.mColors[i3] = numArr[i3].intValue();
                        Color.colorToHSV(EventInfoFragment.this.mColors[i3], new float[3]);
                    }
                    if (EventInfoFragment.this.mCanModifyCalendar && (viewFindViewById = EventInfoFragment.this.mView.findViewById(R.id.change_color)) != null && EventInfoFragment.this.mColors.length > 0) {
                        viewFindViewById.setEnabled(true);
                        viewFindViewById.setVisibility(0);
                    }
                    EventInfoFragment.this.updateMenu();
                }
            } else if (cursor == null || cursor.getCount() <= 1) {
                EventInfoFragment.this.setVisibilityCommon(EventInfoFragment.this.mView, R.id.calendar_container, 8);
                EventInfoFragment.access$3676(EventInfoFragment.this, 8);
            } else {
                EventInfoFragment.this.mHandler.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EventInfoFragment.CALENDARS_PROJECTION, "calendar_displayName=?", new String[]{EventInfoFragment.this.mCalendarsCursor.getString(1)}, null);
            }
            EventInfoFragment.this.addCursorToArray(arrayList, cursor);
            EventInfoFragment.this.closeCursors(arrayList);
            EventInfoFragment.this.sendAccessibilityEventIfQueryDone(i);
            if (EventInfoFragment.this.mCurrentQuery == 127) {
                if (EventInfoFragment.this.mLoadingMsgView.getAlpha() == 1.0f) {
                    long jCurrentTimeMillis = 600 - (System.currentTimeMillis() - EventInfoFragment.this.mLoadingMsgStartTime);
                    if (jCurrentTimeMillis > 0) {
                        EventInfoFragment.this.mAnimateAlpha.setStartDelay(jCurrentTimeMillis);
                    }
                }
                if (EventInfoFragment.this.mAnimateAlpha.isRunning() || EventInfoFragment.this.mAnimateAlpha.isStarted() || EventInfoFragment.this.mNoCrossFade) {
                    EventInfoFragment.this.mScrollView.setAlpha(1.0f);
                    EventInfoFragment.this.mLoadingMsgView.setVisibility(8);
                } else {
                    EventInfoFragment.this.mAnimateAlpha.start();
                }
            }
        }
    }

    private void sendAccessibilityEventIfQueryDone(int i) {
        this.mCurrentQuery = i | this.mCurrentQuery;
        if (this.mCurrentQuery == 127) {
            sendAccessibilityEvent();
        }
    }

    public EventInfoFragment(Context context, Uri uri, long j, long j2, int i, boolean z, int i2, ArrayList<CalendarEventModel.ReminderEntry> arrayList) {
        this.mWindowStyle = 1;
        this.mCurrentQuery = 0;
        this.mEventOrganizerDisplayName = "";
        this.mCalendarOwnerAttendeeId = -1L;
        this.mDeleteDialogVisible = false;
        this.mAttendeeResponseFromIntent = 0;
        this.mUserSetResponse = 0;
        this.mWhichEvents = -1;
        this.mTentativeUserSetResponse = 0;
        this.mEventDeletionStarted = false;
        this.mMenu = null;
        this.mDisplayColorKeyMap = new SparseIntArray();
        this.mOriginalColor = -1;
        this.mOriginalColorInitialized = false;
        this.mCalendarColor = -1;
        this.mCalendarColorInitialized = false;
        this.mCurrentColor = -1;
        this.mCurrentColorInitialized = false;
        this.mCurrentColorKey = -1;
        this.mNoCrossFade = false;
        this.mAcceptedAttendees = new ArrayList<>();
        this.mDeclinedAttendees = new ArrayList<>();
        this.mTentativeAttendees = new ArrayList<>();
        this.mNoResponseAttendees = new ArrayList<>();
        this.mReminderViews = new ArrayList<>(0);
        this.mOriginalReminders = new ArrayList<>();
        this.mUnsupportedReminders = new ArrayList<>();
        this.mUserModifiedReminders = false;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                EventInfoFragment.this.updateEvent(EventInfoFragment.this.mView);
            }
        };
        this.mLoadingMsgAlphaUpdater = new Runnable() {
            @Override
            public void run() {
                if (!EventInfoFragment.this.mAnimateAlpha.isRunning() && EventInfoFragment.this.mScrollView.getAlpha() == 0.0f) {
                    EventInfoFragment.this.mLoadingMsgStartTime = System.currentTimeMillis();
                    EventInfoFragment.this.mLoadingMsgView.setAlpha(1.0f);
                }
            }
        };
        this.mIsDialog = false;
        this.mIsPaused = true;
        this.mDismissOnResume = false;
        this.mX = -1;
        this.mY = -1;
        this.mInitRemindersFinished = false;
        this.onDeleteRunnable = new Runnable() {
            @Override
            public void run() {
                if (EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mDismissOnResume = true;
                } else if (EventInfoFragment.this.isVisible()) {
                    EventInfoFragment.this.dismiss();
                }
            }
        };
        this.mAddReminderBtn = null;
        this.mWhichDelete = -1;
        Resources resources = context.getResources();
        if (mScale == 0.0f) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                mCustomAppIconSize = (int) (mCustomAppIconSize * mScale);
                if (z) {
                    DIALOG_TOP_MARGIN = (int) (DIALOG_TOP_MARGIN * mScale);
                }
            }
        }
        if (z) {
            setDialogSize(resources);
        }
        this.mIsDialog = z;
        setStyle(1, 0);
        this.mUri = uri;
        this.mStartMillis = j;
        this.mEndMillis = j2;
        this.mAttendeeResponseFromIntent = i;
        this.mWindowStyle = i2;
        this.mReminders = arrayList;
    }

    public EventInfoFragment() {
        this.mWindowStyle = 1;
        this.mCurrentQuery = 0;
        this.mEventOrganizerDisplayName = "";
        this.mCalendarOwnerAttendeeId = -1L;
        this.mDeleteDialogVisible = false;
        this.mAttendeeResponseFromIntent = 0;
        this.mUserSetResponse = 0;
        this.mWhichEvents = -1;
        this.mTentativeUserSetResponse = 0;
        this.mEventDeletionStarted = false;
        this.mMenu = null;
        this.mDisplayColorKeyMap = new SparseIntArray();
        this.mOriginalColor = -1;
        this.mOriginalColorInitialized = false;
        this.mCalendarColor = -1;
        this.mCalendarColorInitialized = false;
        this.mCurrentColor = -1;
        this.mCurrentColorInitialized = false;
        this.mCurrentColorKey = -1;
        this.mNoCrossFade = false;
        this.mAcceptedAttendees = new ArrayList<>();
        this.mDeclinedAttendees = new ArrayList<>();
        this.mTentativeAttendees = new ArrayList<>();
        this.mNoResponseAttendees = new ArrayList<>();
        this.mReminderViews = new ArrayList<>(0);
        this.mOriginalReminders = new ArrayList<>();
        this.mUnsupportedReminders = new ArrayList<>();
        this.mUserModifiedReminders = false;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                EventInfoFragment.this.updateEvent(EventInfoFragment.this.mView);
            }
        };
        this.mLoadingMsgAlphaUpdater = new Runnable() {
            @Override
            public void run() {
                if (!EventInfoFragment.this.mAnimateAlpha.isRunning() && EventInfoFragment.this.mScrollView.getAlpha() == 0.0f) {
                    EventInfoFragment.this.mLoadingMsgStartTime = System.currentTimeMillis();
                    EventInfoFragment.this.mLoadingMsgView.setAlpha(1.0f);
                }
            }
        };
        this.mIsDialog = false;
        this.mIsPaused = true;
        this.mDismissOnResume = false;
        this.mX = -1;
        this.mY = -1;
        this.mInitRemindersFinished = false;
        this.onDeleteRunnable = new Runnable() {
            @Override
            public void run() {
                if (EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mDismissOnResume = true;
                } else if (EventInfoFragment.this.isVisible()) {
                    EventInfoFragment.this.dismiss();
                }
            }
        };
        this.mAddReminderBtn = null;
        this.mWhichDelete = -1;
    }

    public EventInfoFragment(Context context, long j, long j2, long j3, int i, boolean z, int i2, ArrayList<CalendarEventModel.ReminderEntry> arrayList) {
        this(context, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j), j2, j3, i, z, i2, arrayList);
        this.mEventId = j;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mReminderChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                Integer num = (Integer) adapterView.getTag();
                if (num == null || num.intValue() != i) {
                    adapterView.setTag(Integer.valueOf(i));
                    EventInfoFragment.this.mUserModifiedReminders = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        if (bundle != null) {
            this.mIsDialog = bundle.getBoolean("key_fragment_is_dialog", false);
            this.mWindowStyle = bundle.getInt("key_window_style", 1);
        }
        if (this.mIsDialog) {
            applyDialogParams();
        }
        Activity activity = getActivity();
        this.mContext = activity;
        this.mColorPickerDialog = (EventColorPickerDialog) activity.getFragmentManager().findFragmentByTag("EventColorPickerDialog");
        if (this.mColorPickerDialog != null) {
            this.mColorPickerDialog.setOnColorSelectedListener(this);
        }
        Log.d("EventInfoFragment", "nfc register in eventinfofragment");
        NfcHandler.register(getActivity(), this);
    }

    private void applyDialogParams() {
        Dialog dialog = getDialog();
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        window.addFlags(2);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.4f;
        ((ViewGroup.LayoutParams) attributes).width = mDialogWidth;
        ((ViewGroup.LayoutParams) attributes).height = mDialogHeight;
        if (this.mX != -1 || this.mY != -1) {
            attributes.x = this.mX - (mDialogWidth / 2);
            attributes.y = this.mY - (mDialogHeight / 2);
            if (attributes.y < this.mMinTop) {
                attributes.y = this.mMinTop + DIALOG_TOP_MARGIN;
            }
            attributes.gravity = 51;
        }
        window.setAttributes(attributes);
    }

    public void setDialogParams(int i, int i2, int i3) {
        this.mX = i;
        this.mY = i2;
        this.mMinTop = i3;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if (this.mTentativeUserSetResponse != 0) {
            return;
        }
        int responseFromButtonId = getResponseFromButtonId(i);
        if (!this.mIsRepeating) {
            this.mUserSetResponse = responseFromButtonId;
        } else if (i == findButtonIdForResponse(this.mOriginalAttendeeResponse)) {
            this.mUserSetResponse = responseFromButtonId;
        } else {
            this.mTentativeUserSetResponse = responseFromButtonId;
            this.mEditResponseHelper.showDialog(this.mWhichEvents);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        saveReminders();
        this.mController.deregisterEventHandler(Integer.valueOf(R.layout.event_info));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        this.mIsTabletConfig = Utils.getConfigBool(this.mActivity, R.bool.tablet_config);
        this.mController = CalendarController.getInstance(this.mActivity);
        this.mController.registerEventHandler(R.layout.event_info, this);
        this.mEditResponseHelper = new EditResponseHelper(activity);
        this.mEditResponseHelper.setDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (EventInfoFragment.this.mEditResponseHelper.getWhichEvents() != -1) {
                    EventInfoFragment.this.mUserSetResponse = EventInfoFragment.this.mTentativeUserSetResponse;
                    EventInfoFragment.this.mWhichEvents = EventInfoFragment.this.mEditResponseHelper.getWhichEvents();
                } else {
                    int iFindButtonIdForResponse = EventInfoFragment.findButtonIdForResponse(EventInfoFragment.this.mUserSetResponse != 0 ? EventInfoFragment.this.mUserSetResponse : EventInfoFragment.this.mOriginalAttendeeResponse);
                    if (EventInfoFragment.this.mResponseRadioGroup != null) {
                        EventInfoFragment.this.mResponseRadioGroup.check(iFindButtonIdForResponse);
                    }
                    if (iFindButtonIdForResponse == -1) {
                        EventInfoFragment.this.mEditResponseHelper.setWhichEvents(-1);
                    }
                }
                if (!EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mTentativeUserSetResponse = 0;
                }
            }
        });
        if (this.mAttendeeResponseFromIntent != 0) {
            this.mEditResponseHelper.setWhichEvents(1);
            this.mWhichEvents = this.mEditResponseHelper.getWhichEvents();
        }
        this.mHandler = new QueryHandler(activity);
        if (!this.mIsDialog) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (bundle != null) {
            this.mIsDialog = bundle.getBoolean("key_fragment_is_dialog", false);
            this.mWindowStyle = bundle.getInt("key_window_style", 1);
            this.mDeleteDialogVisible = bundle.getBoolean("key_delete_dialog_visible", false);
            this.mCalendarColor = bundle.getInt("key_calendar_color");
            this.mCalendarColorInitialized = bundle.getBoolean("key_calendar_color_init");
            this.mOriginalColor = bundle.getInt("key_original_color");
            this.mOriginalColorInitialized = bundle.getBoolean("key_original_color_init");
            this.mCurrentColor = bundle.getInt("key_current_color");
            this.mCurrentColorInitialized = bundle.getBoolean("key_current_color_init");
            this.mCurrentColorKey = bundle.getInt("key_current_color_key");
            this.mTentativeUserSetResponse = bundle.getInt("key_tentative_user_response", 0);
            if (this.mTentativeUserSetResponse != 0 && this.mEditResponseHelper != null) {
                this.mEditResponseHelper.setWhichEvents(bundle.getInt("key_response_which_events", -1));
            }
            this.mUserSetResponse = bundle.getInt("key_user_set_attendee_response", 0);
            if (this.mUserSetResponse != 0) {
                this.mWhichEvents = bundle.getInt("key_response_which_events", -1);
            }
            this.mReminders = Utils.readRemindersFromBundle(bundle);
            this.mWhichDelete = bundle.getInt("key_which_delete", -1);
        }
        if (this.mWindowStyle == 1) {
            this.mView = layoutInflater.inflate(R.layout.event_info_dialog, viewGroup, false);
        } else {
            this.mView = layoutInflater.inflate(R.layout.event_info, viewGroup, false);
        }
        this.mScrollView = (ScrollView) this.mView.findViewById(R.id.event_info_scroll_view);
        this.mLoadingMsgView = this.mView.findViewById(R.id.event_info_loading_msg);
        this.mErrorMsgView = this.mView.findViewById(R.id.event_info_error_msg);
        this.mTitle = (TextView) this.mView.findViewById(R.id.title);
        this.mWhenDateTime = (TextView) this.mView.findViewById(R.id.when_datetime);
        this.mWhere = (TextView) this.mView.findViewById(R.id.where);
        this.mDesc = (ExpandableTextView) this.mView.findViewById(R.id.description);
        this.mHeadlines = this.mView.findViewById(R.id.event_info_headline);
        this.mLongAttendees = (AttendeesView) this.mView.findViewById(R.id.long_attendee_list);
        this.mResponseRadioGroup = (RadioGroup) this.mView.findViewById(R.id.response_value);
        if (this.mUri == null && bundle != null) {
            this.mEventId = bundle.getLong("key_event_id");
            this.mUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
            this.mStartMillis = bundle.getLong("key_start_millis");
            this.mEndMillis = bundle.getLong("key_end_millis");
        }
        this.mAnimateAlpha = ObjectAnimator.ofFloat(this.mScrollView, "Alpha", 0.0f, 1.0f);
        this.mAnimateAlpha.setDuration(300L);
        this.mAnimateAlpha.addListener(new AnimatorListenerAdapter() {
            int defLayerType;

            @Override
            public void onAnimationStart(Animator animator) {
                this.defLayerType = EventInfoFragment.this.mScrollView.getLayerType();
                EventInfoFragment.this.mScrollView.setLayerType(2, null);
                EventInfoFragment.this.mLoadingMsgView.removeCallbacks(EventInfoFragment.this.mLoadingMsgAlphaUpdater);
                EventInfoFragment.this.mLoadingMsgView.setVisibility(8);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                EventInfoFragment.this.mScrollView.setLayerType(this.defLayerType, null);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                EventInfoFragment.this.mScrollView.setLayerType(this.defLayerType, null);
                EventInfoFragment.this.mNoCrossFade = true;
            }
        });
        this.mLoadingMsgView.setAlpha(0.0f);
        this.mScrollView.setAlpha(0.0f);
        this.mErrorMsgView.setVisibility(4);
        this.mLoadingMsgView.postDelayed(this.mLoadingMsgAlphaUpdater, 600L);
        this.mHandler.startQuery(1, null, this.mUri, EVENT_PROJECTION, "deleted!=1", null, null);
        this.mView.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (EventInfoFragment.this.mCanModifyCalendar) {
                    EventInfoFragment.this.mDeleteHelper = new DeleteEventHelper(EventInfoFragment.this.mContext, EventInfoFragment.this.mActivity, (EventInfoFragment.this.mIsDialog || EventInfoFragment.this.mIsTabletConfig) ? false : true);
                    EventInfoFragment.this.mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                    EventInfoFragment.this.mDeleteHelper.setOnDismissListener(EventInfoFragment.this.createDeleteOnDismissListener());
                    EventInfoFragment.this.mDeleteDialogVisible = true;
                    EventInfoFragment.this.mDeleteHelper.delete(EventInfoFragment.this.mStartMillis, EventInfoFragment.this.mEndMillis, EventInfoFragment.this.mEventId, -1, EventInfoFragment.this.onDeleteRunnable);
                }
            }
        });
        this.mView.findViewById(R.id.change_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (EventInfoFragment.this.mCanModifyCalendar) {
                    EventInfoFragment.this.showEventColorPickerDialog();
                }
            }
        });
        if (this.mIsTabletConfig) {
            this.mView.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MTKUtils.sendShareEvent(EventInfoFragment.this.mContext, EventInfoFragment.this.mEventId);
                }
            });
        }
        if ((!this.mIsDialog && !this.mIsTabletConfig) || this.mWindowStyle == 0) {
            this.mView.findViewById(R.id.event_info_buttons_container).setVisibility(8);
        }
        this.emailAttendeesButton = (Button) this.mView.findViewById(R.id.email_attendees_button);
        if (this.emailAttendeesButton != null) {
            this.emailAttendeesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EventInfoFragment.this.emailAttendees();
                }
            });
        }
        this.mAddReminderBtn = (Button) this.mView.findViewById(R.id.reminder_add);
        this.mAddReminderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventInfoFragment.this.addReminder();
                EventInfoFragment.this.mUserModifiedReminders = true;
            }
        });
        this.mDefaultReminderMinutes = Integer.parseInt(GeneralPreferences.getSharedPreferences(this.mActivity).getString("preferences_default_reminder", "-1"));
        prepareReminders();
        return this.mView;
    }

    private void updateTitle() {
        Resources resources = getActivity().getResources();
        if (this.mCanModifyCalendar && !this.mIsOrganizer) {
            getActivity().setTitle(resources.getString(R.string.event_info_title_invite));
        } else {
            getActivity().setTitle(resources.getString(R.string.event_info_title));
        }
    }

    private synchronized boolean initEventCursor() {
        boolean z = false;
        if (this.mEventCursor != null && this.mEventCursor.getCount() != 0) {
            this.mEventCursor.moveToFirst();
            this.mEventId = this.mEventCursor.getInt(0);
            this.mIsRepeating = !TextUtils.isEmpty(this.mEventCursor.getString(2));
            if (this.mEventCursor.getInt(15) == 1 || (this.mReminders != null && this.mReminders.size() > 0)) {
                z = true;
            }
            this.mHasAlarm = z;
            this.mMaxReminders = this.mEventCursor.getInt(16);
            this.mCalendarAllowedReminders = this.mEventCursor.getString(17);
            return true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("initEventCursor, mEventCursor=");
        sb.append(this.mEventCursor);
        sb.append(";mEventCursor.getCount()");
        sb.append(this.mEventCursor == null ? 0 : this.mEventCursor.getCount());
        LogUtil.w("EventInfoFragment", sb.toString());
        return false;
    }

    private void initAttendeesCursor(View view) {
        String string;
        String string2;
        this.mOriginalAttendeeResponse = 0;
        this.mCalendarOwnerAttendeeId = -1L;
        this.mNumOfAttendees = 0;
        if (this.mAttendeesCursor != null) {
            this.mNumOfAttendees = this.mAttendeesCursor.getCount();
            if (this.mAttendeesCursor.moveToFirst()) {
                this.mAcceptedAttendees.clear();
                this.mDeclinedAttendees.clear();
                this.mTentativeAttendees.clear();
                this.mNoResponseAttendees.clear();
                do {
                    int i = this.mAttendeesCursor.getInt(4);
                    String string3 = this.mAttendeesCursor.getString(1);
                    String string4 = this.mAttendeesCursor.getString(2);
                    if (this.mAttendeesCursor.getInt(3) == 2 && !TextUtils.isEmpty(string3)) {
                        this.mEventOrganizerDisplayName = string3;
                        if (!this.mIsOrganizer) {
                            setVisibilityCommon(view, R.id.organizer_container, 0);
                            setTextCommon(view, R.id.organizer, this.mEventOrganizerDisplayName);
                        }
                    }
                    if (this.mCalendarOwnerAttendeeId == -1 && this.mCalendarOwnerAccount.equalsIgnoreCase(string4)) {
                        this.mCalendarOwnerAttendeeId = this.mAttendeesCursor.getInt(0);
                        this.mOriginalAttendeeResponse = this.mAttendeesCursor.getInt(4);
                    } else {
                        if (Utils.isJellybeanOrLater()) {
                            string = this.mAttendeesCursor.getString(5);
                            string2 = this.mAttendeesCursor.getString(6);
                        } else {
                            string = null;
                            string2 = null;
                        }
                        if (i != 4) {
                            switch (i) {
                                case 1:
                                    this.mAcceptedAttendees.add(new CalendarEventModel.Attendee(string3, string4, 1, string, string2));
                                    break;
                                case 2:
                                    this.mDeclinedAttendees.add(new CalendarEventModel.Attendee(string3, string4, 2, string, string2));
                                    break;
                                default:
                                    this.mNoResponseAttendees.add(new CalendarEventModel.Attendee(string3, string4, 0, string, string2));
                                    break;
                            }
                        } else {
                            this.mTentativeAttendees.add(new CalendarEventModel.Attendee(string3, string4, 4, string, string2));
                        }
                    }
                } while (this.mAttendeesCursor.moveToNext());
                this.mAttendeesCursor.moveToFirst();
                updateAttendees(view);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        int i;
        bundle.putLong("key_event_id", this.mEventId);
        bundle.putLong("key_start_millis", this.mStartMillis);
        bundle.putLong("key_end_millis", this.mEndMillis);
        bundle.putBoolean("key_fragment_is_dialog", this.mIsDialog);
        bundle.putInt("key_window_style", this.mWindowStyle);
        bundle.putBoolean("key_delete_dialog_visible", this.mDeleteDialogVisible);
        bundle.putInt("key_calendar_color", this.mCalendarColor);
        bundle.putBoolean("key_calendar_color_init", this.mCalendarColorInitialized);
        bundle.putInt("key_original_color", this.mOriginalColor);
        bundle.putBoolean("key_original_color_init", this.mOriginalColorInitialized);
        bundle.putInt("key_current_color", this.mCurrentColor);
        bundle.putBoolean("key_current_color_init", this.mCurrentColorInitialized);
        bundle.putInt("key_current_color_key", this.mCurrentColorKey);
        bundle.putInt("key_tentative_user_response", this.mTentativeUserSetResponse);
        if (this.mTentativeUserSetResponse != 0 && this.mEditResponseHelper != null) {
            bundle.putInt("key_response_which_events", this.mEditResponseHelper.getWhichEvents());
        }
        if (this.mAttendeeResponseFromIntent != 0) {
            i = this.mAttendeeResponseFromIntent;
        } else {
            i = this.mOriginalAttendeeResponse;
        }
        bundle.putInt("key_attendee_response", i);
        if (this.mUserSetResponse != 0) {
            bundle.putInt("key_user_set_attendee_response", this.mUserSetResponse);
            bundle.putInt("key_response_which_events", this.mWhichEvents);
        }
        if (this.mInitRemindersFinished) {
            this.mReminders = EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues);
            Log.d("EventInfoFragment", "onSaveInstanceState collect from UI " + this.mReminders);
            int size = this.mReminders.size();
            ArrayList<Integer> arrayList = new ArrayList<>(size);
            ArrayList<Integer> arrayList2 = new ArrayList<>(size);
            for (CalendarEventModel.ReminderEntry reminderEntry : this.mReminders) {
                arrayList.add(Integer.valueOf(reminderEntry.getMinutes()));
                arrayList2.add(Integer.valueOf(reminderEntry.getMethod()));
            }
            bundle.putIntegerArrayList("key_reminder_minutes", arrayList);
            bundle.putIntegerArrayList("key_reminder_methods", arrayList2);
        }
        bundle.putInt("key_which_delete", this.mWhichDelete);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        if ((!this.mIsDialog && !this.mIsTabletConfig) || this.mWindowStyle == 0) {
            menuInflater.inflate(R.menu.event_info_title_bar, menu);
            this.mMenu = menu;
            updateMenu();
            this.mOptionsMenuExt = ExtensionFactory.getEventInfoOptionsMenuExt(getActivity(), this.mEventId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mIsDialog) {
            return false;
        }
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            Utils.returnToCalendarHome(this.mContext);
            this.mActivity.finish();
            return true;
        }
        if (itemId == R.id.info_action_edit) {
            if (saveReminders()) {
                Toast.makeText(getActivity(), R.string.saving_event, 0).show();
            }
            doEdit();
            this.mActivity.finish();
        } else if (itemId == R.id.info_action_delete) {
            this.mDeleteHelper = new DeleteEventHelper(this.mActivity, this.mActivity, true);
            this.mDeleteHelper.setDeleteNotificationListener(this);
            this.mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            this.mDeleteDialogVisible = true;
            this.mDeleteHelper.delete(this.mStartMillis, this.mEndMillis, this.mEventId, -1, this.onDeleteRunnable);
        } else if (itemId == R.id.info_action_change_color) {
            showEventColorPickerDialog();
        } else if (this.mOptionsMenuExt != null) {
            this.mOptionsMenuExt.onOptionsItemSelected(menuItem.getItemId());
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void showEventColorPickerDialog() {
        if (this.mColorPickerDialog == null) {
            this.mColorPickerDialog = EventColorPickerDialog.newInstance(this.mColors, this.mCurrentColor, this.mCalendarColor, this.mIsTabletConfig);
            this.mColorPickerDialog.setOnColorSelectedListener(this);
        }
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.executePendingTransactions();
        if (!this.mColorPickerDialog.isAdded()) {
            this.mColorPickerDialog.show(fragmentManager, "EventColorPickerDialog");
        }
    }

    private boolean saveEventColor() {
        if (this.mCurrentColor == this.mOriginalColor) {
            return false;
        }
        ContentValues contentValues = new ContentValues();
        if (this.mCurrentColor != this.mCalendarColor) {
            contentValues.put("eventColor_index", Integer.valueOf(this.mCurrentColorKey));
        } else {
            contentValues.put("eventColor_index", "");
        }
        this.mHandler.startUpdate(this.mHandler.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId), contentValues, null, null, 0L);
        return true;
    }

    @Override
    public void onStop() {
        boolean zSaveReminders;
        Activity activity = getActivity();
        if (!this.mEventDeletionStarted && activity != null && !activity.isChangingConfigurations()) {
            boolean zSaveResponse = saveResponse();
            boolean zSaveEventColor = saveEventColor();
            if (!zSaveResponse || !this.mIsRepeating || this.mEditResponseHelper.getWhichEvents() != 0) {
                zSaveReminders = saveReminders();
            } else {
                zSaveReminders = false;
            }
            if (zSaveReminders || zSaveResponse || zSaveEventColor) {
                Toast.makeText(getActivity(), R.string.saving_event, 0).show();
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (this.mEventCursor != null) {
            this.mEventCursor.close();
        }
        if (this.mCalendarsCursor != null) {
            this.mCalendarsCursor.close();
        }
        if (this.mAttendeesCursor != null) {
            this.mAttendeesCursor.close();
        }
        super.onDestroy();
    }

    private boolean saveResponse() {
        int responseFromButtonId;
        if (this.mAttendeesCursor == null || this.mEventCursor == null || (responseFromButtonId = getResponseFromButtonId(this.mResponseRadioGroup.getCheckedRadioButtonId())) == 0 || responseFromButtonId == this.mOriginalAttendeeResponse || this.mCalendarOwnerAttendeeId == -1) {
            return false;
        }
        if (!this.mIsRepeating) {
            updateResponse(this.mEventId, this.mCalendarOwnerAttendeeId, responseFromButtonId);
            this.mOriginalAttendeeResponse = responseFromButtonId;
            return true;
        }
        switch (this.mWhichEvents) {
            case -1:
                break;
            case 0:
                createExceptionResponse(this.mEventId, responseFromButtonId);
                this.mOriginalAttendeeResponse = responseFromButtonId;
                break;
            case 1:
                updateResponse(this.mEventId, this.mCalendarOwnerAttendeeId, responseFromButtonId);
                this.mOriginalAttendeeResponse = responseFromButtonId;
                break;
            default:
                Log.e("EventInfoFragment", "Unexpected choice for updating invitation response");
                break;
        }
        return false;
    }

    private void updateResponse(long j, long j2, int i) {
        ContentValues contentValues = new ContentValues();
        if (!TextUtils.isEmpty(this.mCalendarOwnerAccount)) {
            contentValues.put("attendeeEmail", this.mCalendarOwnerAccount);
        }
        contentValues.put("attendeeStatus", Integer.valueOf(i));
        contentValues.put("event_id", Long.valueOf(j));
        this.mHandler.startUpdate(this.mHandler.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Attendees.CONTENT_URI, j2), contentValues, null, null, 0L);
    }

    private void createExceptionResponse(long j, int i) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("originalInstanceTime", Long.valueOf(this.mStartMillis));
        contentValues.put("selfAttendeeStatus", Integer.valueOf(i));
        contentValues.put("eventStatus", (Integer) 1);
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        arrayList.add(ContentProviderOperation.newInsert(Uri.withAppendedPath(CalendarContract.Events.CONTENT_EXCEPTION_URI, String.valueOf(j))).withValues(contentValues).build());
        saveRemindersToOps(arrayList, 0);
        this.mHandler.startBatch(this.mHandler.getNextToken(), null, "com.android.calendar", arrayList, 0L);
    }

    public static int getResponseFromButtonId(int i) {
        if (i == R.id.response_yes) {
            return 1;
        }
        if (i == R.id.response_maybe) {
            return 4;
        }
        if (i == R.id.response_no) {
            return 2;
        }
        return 0;
    }

    public static int findButtonIdForResponse(int i) {
        if (i != 4) {
            switch (i) {
                case 1:
                    return R.id.response_yes;
                case 2:
                    return R.id.response_no;
                default:
                    return -1;
            }
        }
        return R.id.response_maybe;
    }

    private void doEdit() {
        if (getActivity() != null) {
            Intent intent = new Intent("android.intent.action.EDIT", ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId));
            intent.setClass(this.mActivity, EditEventActivity.class);
            intent.putExtra("beginTime", this.mStartMillis);
            intent.putExtra("endTime", this.mEndMillis);
            intent.putExtra("allDay", this.mAllDay);
            intent.putExtra("event_color", this.mCurrentColor);
            intent.putExtra("reminders", EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues));
            intent.putExtra("editMode", true);
            startActivity(intent);
        }
    }

    private void displayEventNotFound() {
        this.mErrorMsgView.setVisibility(0);
        this.mScrollView.setVisibility(8);
        this.mLoadingMsgView.setVisibility(8);
    }

    private void updateEvent(View view) {
        Context context;
        String str;
        String displayedTimezone;
        if (this.mEventCursor == null || view == null || (context = view.getContext()) == null) {
            return;
        }
        String string = this.mEventCursor.getString(1);
        if ((string == null || string.length() == 0) && getActivity() != null) {
            string = getActivity().getString(R.string.no_title_label);
        }
        String str2 = string;
        if (this.mStartMillis == 0 && this.mEndMillis == 0) {
            this.mStartMillis = this.mEventCursor.getLong(5);
            this.mEndMillis = this.mEventCursor.getLong(20);
            if (this.mEndMillis == 0) {
                String string2 = this.mEventCursor.getString(21);
                if (!TextUtils.isEmpty(string2)) {
                    try {
                        Duration duration = new Duration();
                        duration.parse(string2);
                        long millis = this.mStartMillis + duration.getMillis();
                        if (millis >= this.mStartMillis) {
                            this.mEndMillis = millis;
                        } else {
                            Log.d("EventInfoFragment", "Invalid duration string: " + string2);
                        }
                    } catch (DateException e) {
                        Log.d("EventInfoFragment", "Error parsing duration string " + string2, e);
                    }
                }
                if (this.mEndMillis == 0) {
                    this.mEndMillis = this.mStartMillis;
                }
            }
        }
        this.mAllDay = this.mEventCursor.getInt(3) != 0;
        String string3 = this.mEventCursor.getString(9);
        String string4 = this.mEventCursor.getString(8);
        String string5 = this.mEventCursor.getString(2);
        String string6 = this.mEventCursor.getString(7);
        this.mHeadlines.setBackgroundColor(this.mCurrentColor);
        if (str2 != null) {
            setTextCommon(view, R.id.title, str2);
        }
        String timeZone = Utils.getTimeZone(this.mActivity, this.mTZUpdater);
        Resources resources = context.getResources();
        String str3 = Utils.getDisplayedDatetime(this.mStartMillis, this.mEndMillis, System.currentTimeMillis(), timeZone, this.mAllDay, context) + OpCalendarCustomizationFactoryBase.getOpFactory(context).makeLunarCalendar(context).getLunarDisplayedDate(timeZone, this.mStartMillis, this.mEndMillis, this.mAllDay);
        CharSequence repeatString = null;
        if (!this.mAllDay) {
            str = timeZone;
            displayedTimezone = Utils.getDisplayedTimezone(this.mStartMillis, str, string6);
        } else {
            str = timeZone;
            displayedTimezone = null;
        }
        if (displayedTimezone == null) {
            setTextCommon(view, R.id.when_datetime, str3);
        } else {
            int length = str3.length();
            String str4 = str3 + "  " + displayedTimezone;
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str4);
            spannableStringBuilder.setSpan(new ForegroundColorSpan(resources.getColor(R.color.event_info_headline_transparent_color)), length, str4.length(), 18);
            setTextCommon(view, R.id.when_datetime, spannableStringBuilder);
        }
        if (!TextUtils.isEmpty(string5)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(string5);
            Time time = new Time(str);
            time.set(this.mStartMillis);
            if (this.mAllDay) {
                time.timezone = "UTC";
            }
            eventRecurrence.setStartDate(time);
            repeatString = EventRecurrenceFormatter.getRepeatString(this.mContext, resources, eventRecurrence, true);
        }
        if (repeatString == null) {
            view.findViewById(R.id.when_repeat).setVisibility(8);
        } else {
            setTextCommon(view, R.id.when_repeat, repeatString);
        }
        if (string3 == null || string3.trim().length() == 0) {
            setVisibilityCommon(view, R.id.where, 8);
        } else {
            TextView textView = this.mWhere;
            if (textView != null) {
                textView.setAutoLinkMask(0);
                textView.setText(string3.trim());
                try {
                    textView.setText(Utils.extendedLinkify(textView.getText().toString(), true));
                    MovementMethod movementMethod = textView.getMovementMethod();
                    if ((movementMethod == null || !(movementMethod instanceof LinkMovementMethod)) && textView.getLinksClickable()) {
                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                } catch (Exception e2) {
                    Log.e("EventInfoFragment", "Linkification failed", e2);
                }
                textView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view2, MotionEvent motionEvent) {
                        try {
                            return view2.onTouchEvent(motionEvent);
                        } catch (ActivityNotFoundException e3) {
                            return true;
                        }
                    }
                });
            }
        }
        if (string4 != null && string4.length() != 0) {
            this.mDesc.setText(string4);
            MovementMethod movementMethod2 = this.mDesc.mTv.getMovementMethod();
            if ((movementMethod2 == null || !(movementMethod2 instanceof LinkMovementMethod)) && this.mDesc.mTv.getLinksClickable()) {
                this.mDesc.mTv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        if (Utils.isJellybeanOrLater()) {
            updateCustomAppButton();
        }
    }

    private void updateCustomAppButton() {
        PackageManager packageManager;
        Button button = (Button) this.mView.findViewById(R.id.launch_custom_app_button);
        if (button != null) {
            String string = this.mEventCursor.getString(18);
            String string2 = this.mEventCursor.getString(19);
            if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2) && (packageManager = this.mContext.getPackageManager()) != null) {
                try {
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(string, 0);
                    if (applicationInfo != null) {
                        final Intent intent = new Intent("android.provider.calendar.action.HANDLE_CUSTOM_EVENT", ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId));
                        intent.setPackage(string);
                        intent.putExtra("customAppUri", string2);
                        intent.putExtra("beginTime", this.mStartMillis);
                        if (packageManager.resolveActivity(intent, 0) != null) {
                            Drawable applicationIcon = packageManager.getApplicationIcon(applicationInfo);
                            if (applicationIcon != null) {
                                Drawable[] compoundDrawables = button.getCompoundDrawables();
                                applicationIcon.setBounds(0, 0, mCustomAppIconSize, mCustomAppIconSize);
                                button.setCompoundDrawables(applicationIcon, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
                            }
                            CharSequence applicationLabel = packageManager.getApplicationLabel(applicationInfo);
                            if (applicationLabel != null && applicationLabel.length() != 0) {
                                button.setText(applicationLabel);
                            }
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    try {
                                        EventInfoFragment.this.startActivityForResult(intent, 0);
                                    } catch (ActivityNotFoundException e) {
                                        EventInfoFragment.this.setVisibilityCommon(EventInfoFragment.this.mView, R.id.launch_custom_app_container, 8);
                                    }
                                }
                            });
                            setVisibilityCommon(this.mView, R.id.launch_custom_app_container, 0);
                            return;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        setVisibilityCommon(this.mView, R.id.launch_custom_app_container, 8);
    }

    private void sendAccessibilityEvent() {
        int checkedRadioButtonId;
        AccessibilityManager accessibilityManager = (AccessibilityManager) getActivity().getSystemService("accessibility");
        if (!accessibilityManager.isEnabled()) {
            return;
        }
        AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(8);
        accessibilityEventObtain.setClassName(EventInfoFragment.class.getName());
        accessibilityEventObtain.setPackageName(getActivity().getPackageName());
        List<CharSequence> text = accessibilityEventObtain.getText();
        addFieldToAccessibilityEvent(text, this.mTitle, null);
        addFieldToAccessibilityEvent(text, this.mWhenDateTime, null);
        addFieldToAccessibilityEvent(text, this.mWhere, null);
        addFieldToAccessibilityEvent(text, null, this.mDesc);
        if (this.mResponseRadioGroup.getVisibility() == 0 && (checkedRadioButtonId = this.mResponseRadioGroup.getCheckedRadioButtonId()) != -1) {
            text.add(((TextView) getView().findViewById(R.id.response_label)).getText());
            text.add(((Object) ((RadioButton) this.mResponseRadioGroup.findViewById(checkedRadioButtonId)).getText()) + ". ");
        }
        accessibilityManager.sendAccessibilityEvent(accessibilityEventObtain);
    }

    private void addFieldToAccessibilityEvent(List<CharSequence> list, TextView textView, ExpandableTextView expandableTextView) {
        CharSequence text;
        if (textView != null) {
            text = textView.getText();
        } else if (expandableTextView != null) {
            text = expandableTextView.getText();
        } else {
            return;
        }
        if (!TextUtils.isEmpty(text)) {
            String strTrim = text.toString().trim();
            if (strTrim.length() > 0) {
                list.add(strTrim);
                list.add(". ");
            }
        }
    }

    private void updateCalendar(View view) {
        View viewFindViewById;
        View viewFindViewById2;
        this.mCalendarOwnerAccount = "";
        if (this.mCalendarsCursor != null && this.mEventCursor != null) {
            this.mCalendarsCursor.moveToFirst();
            this.mEventCursor.moveToFirst();
            String string = this.mCalendarsCursor.getString(2);
            if (string == null) {
                string = "";
            }
            this.mCalendarOwnerAccount = string;
            this.mOwnerCanRespond = this.mCalendarsCursor.getInt(3) != 0;
            this.mSyncAccountName = this.mCalendarsCursor.getString(4);
            this.mHandler.startQuery(32, null, CalendarContract.Calendars.CONTENT_URI, CALENDARS_PROJECTION, "visible=?", new String[]{"1"}, null);
            this.mEventOrganizerEmail = this.mEventCursor.getString(14);
            this.mIsOrganizer = this.mCalendarOwnerAccount.equalsIgnoreCase(this.mEventOrganizerEmail);
            if (!TextUtils.isEmpty(this.mEventOrganizerEmail) && !this.mEventOrganizerEmail.endsWith("calendar.google.com")) {
                this.mEventOrganizerDisplayName = this.mEventOrganizerEmail;
            }
            if (!this.mIsOrganizer && !TextUtils.isEmpty(this.mEventOrganizerDisplayName)) {
                setTextCommon(view, R.id.organizer, this.mEventOrganizerDisplayName);
                setVisibilityCommon(view, R.id.organizer_container, 0);
            } else {
                setVisibilityCommon(view, R.id.organizer_container, 8);
            }
            this.mHasAttendeeData = this.mEventCursor.getInt(13) != 0;
            this.mCanModifyCalendar = this.mEventCursor.getInt(10) >= 500;
            this.mCanModifyEvent = this.mCanModifyCalendar && this.mIsOrganizer;
            this.mIsBusyFreeCalendar = this.mEventCursor.getInt(10) == 100;
            if (!this.mIsBusyFreeCalendar) {
                View viewFindViewById3 = this.mView.findViewById(R.id.edit);
                viewFindViewById3.setEnabled(true);
                viewFindViewById3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        EventInfoFragment.this.doEdit();
                        if (!EventInfoFragment.this.mIsDialog) {
                            if (!EventInfoFragment.this.mIsTabletConfig) {
                                EventInfoFragment.this.getActivity().finish();
                                return;
                            }
                            return;
                        }
                        EventInfoFragment.this.dismiss();
                    }
                });
            }
            if (this.mCanModifyCalendar && (viewFindViewById2 = this.mView.findViewById(R.id.delete)) != null) {
                viewFindViewById2.setEnabled(true);
                viewFindViewById2.setVisibility(0);
            }
            if (this.mCanModifyEvent && (viewFindViewById = this.mView.findViewById(R.id.edit)) != null) {
                viewFindViewById.setEnabled(true);
                viewFindViewById.setVisibility(0);
            }
            if (((!this.mIsDialog && !this.mIsTabletConfig) || this.mWindowStyle == 0) && this.mMenu != null) {
                this.mActivity.invalidateOptionsMenu();
                return;
            }
            return;
        }
        setVisibilityCommon(view, R.id.calendar, 8);
        sendAccessibilityEventIfQueryDone(8);
    }

    private void updateMenu() {
        if (this.mMenu == null) {
            return;
        }
        MenuItem menuItemFindItem = this.mMenu.findItem(R.id.info_action_delete);
        MenuItem menuItemFindItem2 = this.mMenu.findItem(R.id.info_action_edit);
        MenuItem menuItemFindItem3 = this.mMenu.findItem(R.id.info_action_change_color);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(this.mCanModifyCalendar);
            menuItemFindItem.setEnabled(this.mCanModifyCalendar);
        }
        if (menuItemFindItem2 != null) {
            menuItemFindItem2.setVisible(this.mCanModifyEvent);
            menuItemFindItem2.setEnabled(this.mCanModifyEvent);
        }
        if (menuItemFindItem3 != null && this.mColors != null && this.mColors.length > 0) {
            menuItemFindItem3.setVisible(this.mCanModifyCalendar);
            menuItemFindItem3.setEnabled(this.mCanModifyCalendar);
        }
        updateShareMenu();
    }

    private void updateAttendees(View view) {
        if (this.mAcceptedAttendees.size() + this.mDeclinedAttendees.size() + this.mTentativeAttendees.size() + this.mNoResponseAttendees.size() > 0) {
            this.mLongAttendees.clearAttendees();
            this.mLongAttendees.addAttendees(this.mAcceptedAttendees);
            this.mLongAttendees.addAttendees(this.mDeclinedAttendees);
            this.mLongAttendees.addAttendees(this.mTentativeAttendees);
            this.mLongAttendees.addAttendees(this.mNoResponseAttendees);
            this.mLongAttendees.setEnabled(false);
            this.mLongAttendees.setVisibility(0);
        } else {
            this.mLongAttendees.setVisibility(8);
        }
        if (hasEmailableAttendees()) {
            setVisibilityCommon(this.mView, R.id.email_attendees_container, 0);
            if (this.emailAttendeesButton != null) {
                this.emailAttendeesButton.setText(R.string.email_guests_label);
                return;
            }
            return;
        }
        if (hasEmailableOrganizer()) {
            setVisibilityCommon(this.mView, R.id.email_attendees_container, 0);
            if (this.emailAttendeesButton != null) {
                this.emailAttendeesButton.setText(R.string.email_organizer_label);
                return;
            }
            return;
        }
        setVisibilityCommon(this.mView, R.id.email_attendees_container, 8);
    }

    private boolean hasEmailableAttendees() {
        Iterator<CalendarEventModel.Attendee> it = this.mAcceptedAttendees.iterator();
        while (it.hasNext()) {
            if (Utils.isEmailableFrom(it.next().mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        Iterator<CalendarEventModel.Attendee> it2 = this.mTentativeAttendees.iterator();
        while (it2.hasNext()) {
            if (Utils.isEmailableFrom(it2.next().mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        Iterator<CalendarEventModel.Attendee> it3 = this.mNoResponseAttendees.iterator();
        while (it3.hasNext()) {
            if (Utils.isEmailableFrom(it3.next().mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        Iterator<CalendarEventModel.Attendee> it4 = this.mDeclinedAttendees.iterator();
        while (it4.hasNext()) {
            if (Utils.isEmailableFrom(it4.next().mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEmailableOrganizer() {
        return this.mEventOrganizerEmail != null && Utils.isEmailableFrom(this.mEventOrganizerEmail, this.mSyncAccountName);
    }

    public void initReminders(View view, Cursor cursor) {
        ArrayList<CalendarEventModel.ReminderEntry> arrayList;
        this.mOriginalReminders.clear();
        this.mUnsupportedReminders.clear();
        while (cursor.moveToNext()) {
            int i = cursor.getInt(1);
            int i2 = cursor.getInt(2);
            if (i2 != 0 && !this.mReminderMethodValues.contains(Integer.valueOf(i2))) {
                this.mUnsupportedReminders.add(CalendarEventModel.ReminderEntry.valueOf(i, i2));
            } else {
                this.mOriginalReminders.add(CalendarEventModel.ReminderEntry.valueOf(i, i2));
            }
        }
        Collections.sort(this.mOriginalReminders);
        if (this.mUserModifiedReminders && !this.mIsTabletConfig) {
            return;
        }
        LinearLayout linearLayout = (LinearLayout) this.mScrollView.findViewById(R.id.reminder_items_container);
        if (linearLayout != null) {
            linearLayout.removeAllViews();
        }
        if (this.mReminderViews != null) {
            this.mReminderViews.clear();
        }
        if (this.mHasAlarm) {
            if (this.mReminders != null && !this.mIsTabletConfig) {
                arrayList = this.mReminders;
            } else {
                arrayList = this.mOriginalReminders;
            }
            Iterator<CalendarEventModel.ReminderEntry> it = arrayList.iterator();
            while (it.hasNext()) {
                EventViewUtils.addMinutesToList(this.mActivity, this.mReminderMinuteValues, this.mReminderMinuteLabels, it.next().getMinutes());
            }
            Iterator<CalendarEventModel.ReminderEntry> it2 = arrayList.iterator();
            while (it2.hasNext()) {
                EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderViews, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, it2.next(), Integer.MAX_VALUE, this.mReminderChangeListener);
            }
            EventViewUtils.updateAddReminderButton(this.mView, this.mReminderViews, this.mMaxReminders);
        }
        Log.d("EventInfoFragment", "initReminders finished ");
        this.mInitRemindersFinished = true;
    }

    void updateResponse(View view) {
        int i;
        if (!this.mEventCursor.moveToFirst()) {
            Log.w("EventInfoFragment", "can not move mEventCursor to first pos.");
            return;
        }
        boolean zEquals = this.mEventCursor.getString(23).equals("LOCAL");
        if (!this.mCanModifyCalendar || ((this.mHasAttendeeData && this.mIsOrganizer && this.mNumOfAttendees <= 1) || ((this.mIsOrganizer && !this.mOwnerCanRespond) || zEquals || this.mCalendarOwnerAttendeeId == -1))) {
            setVisibilityCommon(view, R.id.response_container, 8);
            return;
        }
        setVisibilityCommon(view, R.id.response_container, 0);
        if (this.mTentativeUserSetResponse != 0) {
            i = this.mTentativeUserSetResponse;
        } else if (this.mUserSetResponse != 0) {
            i = this.mUserSetResponse;
        } else if (this.mAttendeeResponseFromIntent != 0) {
            i = this.mAttendeeResponseFromIntent;
        } else {
            i = this.mOriginalAttendeeResponse;
        }
        this.mResponseRadioGroup.check(findButtonIdForResponse(i));
        this.mResponseRadioGroup.setOnCheckedChangeListener(this);
    }

    private void setTextCommon(View view, int i, CharSequence charSequence) {
        TextView textView = (TextView) view.findViewById(i);
        if (textView == null) {
            return;
        }
        textView.setText(charSequence);
    }

    private void setVisibilityCommon(View view, int i, int i2) {
        View viewFindViewById = view.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(i2);
        }
    }

    @Override
    public void onPause() {
        this.mIsPaused = true;
        this.mHandler.removeCallbacks(this.onDeleteRunnable);
        super.onPause();
        if (this.mDeleteDialogVisible && this.mDeleteHelper != null) {
            this.mWhichDelete = this.mDeleteHelper.getWhichDelete();
            this.mDeleteHelper.dismissAlertDialog();
            this.mDeleteHelper = null;
        }
        if (this.mTentativeUserSetResponse != 0 && this.mEditResponseHelper != null) {
            this.mEditResponseHelper.dismissAlertDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mIsDialog) {
            setDialogSize(getActivity().getResources());
            applyDialogParams();
        }
        if (this.mCurrentQuery == 127 && !this.mIsBusyFreeCalendar) {
            initAttendeesCursor(this.mView);
        }
        boolean z = false;
        this.mIsPaused = false;
        if (this.mDismissOnResume) {
            this.mHandler.post(this.onDeleteRunnable);
        }
        if (this.mDeleteDialogVisible) {
            Context context = this.mContext;
            Activity activity = this.mActivity;
            if (!this.mIsDialog && !this.mIsTabletConfig) {
                z = true;
            }
            this.mDeleteHelper = new DeleteEventHelper(context, activity, z);
            this.mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            this.mDeleteHelper.delete(this.mStartMillis, this.mEndMillis, this.mEventId, this.mWhichDelete, this.onDeleteRunnable);
            return;
        }
        if (this.mTentativeUserSetResponse != 0) {
            this.mResponseRadioGroup.check(findButtonIdForResponse(this.mTentativeUserSetResponse));
            this.mEditResponseHelper.showDialog(this.mEditResponseHelper.getWhichEvents());
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return 128L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        reloadEvents();
    }

    public void reloadEvents() {
        if (this.mHandler != null) {
            this.mHandler.startQuery(1, null, this.mUri, EVENT_PROJECTION, null, null, null);
        }
    }

    @Override
    public void onClick(View view) {
        LinearLayout linearLayout = (LinearLayout) view.getParent();
        ((LinearLayout) linearLayout.getParent()).removeView(linearLayout);
        this.mReminderViews.remove(linearLayout);
        this.mUserModifiedReminders = true;
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderViews, this.mMaxReminders);
    }

    private void addReminder() {
        if (this.mDefaultReminderMinutes == -1) {
            EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderViews, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, CalendarEventModel.ReminderEntry.valueOf(10), this.mMaxReminders, this.mReminderChangeListener);
        } else {
            EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderViews, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, CalendarEventModel.ReminderEntry.valueOf(this.mDefaultReminderMinutes), this.mMaxReminders, this.mReminderChangeListener);
        }
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderViews, this.mMaxReminders);
    }

    private synchronized void prepareReminders() {
        if (this.mReminderMinuteValues == null || this.mReminderMinuteLabels == null || this.mReminderMethodValues == null || this.mReminderMethodLabels == null || this.mCalendarAllowedReminders != null) {
            Resources resources = this.mActivity.getResources();
            this.mReminderMinuteValues = loadIntegerArray(resources, R.array.reminder_minutes_values);
            this.mReminderMinuteLabels = loadStringArray(resources, R.array.reminder_minutes_labels);
            this.mReminderMethodValues = loadIntegerArray(resources, R.array.reminder_methods_values);
            this.mReminderMethodLabels = loadStringArray(resources, R.array.reminder_methods_labels);
            if (this.mCalendarAllowedReminders != null) {
                EventViewUtils.reduceMethodList(this.mReminderMethodValues, this.mReminderMethodLabels, this.mCalendarAllowedReminders);
            }
            if (this.mView != null) {
                this.mView.invalidate();
            }
            updateAddReminderBtnVisibility();
        }
    }

    private boolean saveReminders() {
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>(3);
        this.mReminders = EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues);
        ArrayList arrayList2 = new ArrayList();
        arrayList2.addAll(this.mReminders);
        removeDuplicate(this.mOriginalReminders);
        removeDuplicate(this.mReminders);
        this.mOriginalReminders.addAll(this.mUnsupportedReminders);
        Collections.sort(this.mOriginalReminders);
        this.mReminders.addAll(this.mUnsupportedReminders);
        Collections.sort(this.mReminders);
        int i = 0;
        if (!EditEventHelper.saveReminders(arrayList, this.mEventId, this.mReminders, this.mOriginalReminders, false)) {
            return false;
        }
        AsyncQueryService asyncQueryService = new AsyncQueryService(getActivity());
        asyncQueryService.startBatch(0, null, CalendarContract.Calendars.CONTENT_URI.getAuthority(), arrayList, 0L);
        this.mOriginalReminders = this.mReminders;
        Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
        if (this.mReminders.size() > 0) {
            i = 1;
        }
        if (i != this.mHasAlarm) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("hasAlarm", Integer.valueOf(i));
            asyncQueryService.startUpdate(0, null, uriWithAppendedId, contentValues, null, null, 0L);
        }
        this.mOriginalReminders.clear();
        this.mOriginalReminders.addAll(arrayList2);
        arrayList2.clear();
        return true;
    }

    private void removeDuplicate(ArrayList<CalendarEventModel.ReminderEntry> arrayList) {
        if (arrayList.size() < 2) {
            return;
        }
        Collections.sort(arrayList);
        CalendarEventModel.ReminderEntry reminderEntry = arrayList.get(arrayList.size() - 1);
        int size = arrayList.size() - 2;
        while (size >= 0) {
            CalendarEventModel.ReminderEntry reminderEntry2 = arrayList.get(size);
            if (reminderEntry.equals(reminderEntry2)) {
                arrayList.remove(size + 1);
            }
            size--;
            reminderEntry = reminderEntry2;
        }
    }

    private void emailAttendees() {
        Intent intent = new Intent(getActivity(), (Class<?>) QuickResponseActivity.class);
        intent.putExtra("eventId", this.mEventId);
        intent.addFlags(268435456);
        startActivity(intent);
    }

    private static ArrayList<Integer> loadIntegerArray(Resources resources, int i) {
        int[] intArray = resources.getIntArray(i);
        ArrayList<Integer> arrayList = new ArrayList<>(intArray.length);
        for (int i2 : intArray) {
            arrayList.add(Integer.valueOf(i2));
        }
        return arrayList;
    }

    private static ArrayList<String> loadStringArray(Resources resources, int i) {
        return new ArrayList<>(Arrays.asList(resources.getStringArray(i)));
    }

    @Override
    public void onDeleteStarted() {
        this.mEventDeletionStarted = true;
    }

    private DialogInterface.OnDismissListener createDeleteOnDismissListener() {
        return new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (!EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mDeleteDialogVisible = false;
                }
            }
        };
    }

    public long getEventId() {
        return this.mEventId;
    }

    public long getStartMillis() {
        return this.mStartMillis;
    }

    public long getEndMillis() {
        return this.mEndMillis;
    }

    private void setDialogSize(Resources resources) {
        mDialogWidth = (int) resources.getDimension(R.dimen.event_info_dialog_width);
        mDialogHeight = (int) resources.getDimension(R.dimen.event_info_dialog_height);
    }

    @Override
    public void onColorSelected(int i) {
        this.mCurrentColor = i;
        this.mCurrentColorKey = this.mDisplayColorKeyMap.get(i);
        this.mHeadlines.setBackgroundColor(i);
    }

    private void updateAddReminderBtnVisibility() {
        if (this.mReminderViews.size() >= this.mMaxReminders) {
            this.mAddReminderBtn.setVisibility(8);
        } else {
            this.mAddReminderBtn.setVisibility(0);
        }
    }

    public Uri getUri() {
        return this.mUri;
    }

    private void saveRemindersToOps(ArrayList<ContentProviderOperation> arrayList, int i) {
        this.mReminders = EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues);
        removeDuplicate(this.mOriginalReminders);
        removeDuplicate(this.mReminders);
        this.mOriginalReminders.addAll(this.mUnsupportedReminders);
        Collections.sort(this.mOriginalReminders);
        this.mReminders.addAll(this.mUnsupportedReminders);
        Collections.sort(this.mReminders);
        EditEventHelper.saveRemindersWithBackRef(arrayList, i, this.mReminders, this.mOriginalReminders, false);
        this.mOriginalReminders = this.mReminders;
        int i2 = this.mReminders.size() > 0 ? 1 : 0;
        if (i2 != this.mHasAlarm) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("hasAlarm", Integer.valueOf(i2));
            ContentProviderOperation.Builder builderWithValues = ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI).withValues(contentValues);
            builderWithValues.withSelection("_id=?", new String[1]);
            builderWithValues.withSelectionBackReference(0, i);
            arrayList.add(builderWithValues.build());
        }
    }

    private void updateShareMenu() {
        boolean z = (this.mEventCursor == null || this.mEventCursor.getCount() == 0) ? false : true;
        boolean configBool = Utils.getConfigBool(this.mContext, R.bool.tablet_config);
        MenuItem menuItemFindItem = this.mMenu.findItem(R.id.info_action_share);
        if (!configBool && menuItemFindItem != null) {
            menuItemFindItem.setVisible(z);
            menuItemFindItem.setEnabled(z);
        }
    }

    private void addCursorToArray(ArrayList<Cursor> arrayList, Cursor cursor) {
        if (cursor != null && !arrayList.contains(cursor)) {
            arrayList.add(cursor);
        }
    }

    private void closeCursors(ArrayList<Cursor> arrayList) {
        for (Cursor cursor : arrayList) {
            if (cursor != null) {
                cursor.close();
            }
        }
        arrayList.clear();
    }
}
