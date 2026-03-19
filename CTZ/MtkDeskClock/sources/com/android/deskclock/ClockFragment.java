package com.android.deskclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.data.City;
import com.android.deskclock.data.CityListener;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.worldclock.CitySelectionActivity;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class ClockFragment extends DeskClockFragment {
    private BroadcastReceiver mAlarmChangeReceiver;
    private ContentObserver mAlarmObserver;
    private AnalogClock mAnalogClock;
    private SelectedCitiesAdapter mCityAdapter;
    private RecyclerView mCityList;
    private View mClockFrame;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private TextClock mDigitalClock;
    private final Runnable mQuarterHourUpdater;

    public ClockFragment() {
        super(UiDataModel.Tab.CLOCKS);
        this.mQuarterHourUpdater = new QuarterHourRunnable();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAlarmObserver = Utils.isPreL() ? new AlarmObserverPreL() : null;
        this.mAlarmChangeReceiver = Utils.isLOrLater() ? new AlarmChangedBroadcastReceiver() : null;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        View viewInflate = layoutInflater.inflate(R.layout.clock_fragment, viewGroup, false);
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        this.mCityAdapter = new SelectedCitiesAdapter(getActivity(), this.mDateFormat, this.mDateFormatForAccessibility);
        this.mCityList = (RecyclerView) viewInflate.findViewById(R.id.cities);
        this.mCityList.setLayoutManager(new LinearLayoutManager(getActivity()));
        this.mCityList.setAdapter(this.mCityAdapter);
        this.mCityList.setItemAnimator(null);
        DataModel.getDataModel().addCityListener(this.mCityAdapter);
        this.mCityList.addOnScrollListener(new ScrollPositionWatcher());
        this.mCityList.setOnTouchListener(new CityListOnLongClickListener(viewGroup.getContext()));
        viewInflate.setOnLongClickListener(new StartScreenSaverListener());
        this.mClockFrame = viewInflate.findViewById(R.id.main_clock_left_pane);
        if (this.mClockFrame != null) {
            this.mDigitalClock = (TextClock) this.mClockFrame.findViewById(R.id.digital_clock);
            this.mAnalogClock = (AnalogClock) this.mClockFrame.findViewById(R.id.analog_clock);
            Utils.setClockIconTypeface(this.mClockFrame);
            Utils.updateDate(this.mDateFormat, this.mDateFormatForAccessibility, this.mClockFrame);
            Utils.setClockStyle(this.mDigitalClock, this.mAnalogClock);
            Utils.setClockSecondsEnabled(this.mDigitalClock, this.mAnalogClock);
        }
        UiDataModel.getUiDataModel().addQuarterHourCallback(this.mQuarterHourUpdater, 100L);
        return viewInflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        if (this.mAlarmChangeReceiver != null) {
            activity.registerReceiver(this.mAlarmChangeReceiver, new IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED"));
        }
        if (this.mDigitalClock != null && this.mAnalogClock != null) {
            Utils.setClockStyle(this.mDigitalClock, this.mAnalogClock);
            Utils.setClockSecondsEnabled(this.mDigitalClock, this.mAnalogClock);
        }
        View view = getView();
        if (view != null && view.findViewById(R.id.main_clock_left_pane) != null) {
            this.mCityList.setVisibility(this.mCityAdapter.getItemCount() == 0 ? 8 : 0);
        }
        refreshAlarm();
        if (this.mAlarmObserver != null) {
            activity.getContentResolver().registerContentObserver(Settings.System.getUriFor("next_alarm_formatted"), false, this.mAlarmObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity activity = getActivity();
        if (this.mAlarmChangeReceiver != null) {
            activity.unregisterReceiver(this.mAlarmChangeReceiver);
        }
        if (this.mAlarmObserver != null) {
            activity.getContentResolver().unregisterContentObserver(this.mAlarmObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UiDataModel.getUiDataModel().removePeriodicCallback(this.mQuarterHourUpdater);
        DataModel.getDataModel().removeCityListener(this.mCityAdapter);
    }

    @Override
    public void onFabClick(@NonNull ImageView imageView) {
        startActivity(new Intent(getActivity(), (Class<?>) CitySelectionActivity.class));
    }

    @Override
    public void onUpdateFab(@NonNull ImageView imageView) {
        imageView.setVisibility(0);
        imageView.setImageResource(R.drawable.ic_public);
        imageView.setContentDescription(imageView.getResources().getString(R.string.button_cities));
    }

    @Override
    public void onUpdateFabButtons(@NonNull Button button, @NonNull Button button2) {
        button.setVisibility(4);
        button2.setVisibility(4);
    }

    private void refreshAlarm() {
        if (this.mClockFrame != null) {
            Utils.refreshAlarm(getActivity(), this.mClockFrame);
        } else {
            this.mCityAdapter.refreshAlarm();
        }
    }

    private final class StartScreenSaverListener implements View.OnLongClickListener {
        private StartScreenSaverListener() {
        }

        @Override
        public boolean onLongClick(View view) {
            ClockFragment.this.startActivity(new Intent(ClockFragment.this.getActivity(), (Class<?>) ScreensaverActivity.class).setFlags(268435456).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
            return true;
        }
    }

    private final class CityListOnLongClickListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
        private final GestureDetector mGestureDetector;

        private CityListOnLongClickListener(Context context) {
            this.mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            View view = ClockFragment.this.getView();
            if (view != null) {
                view.performLongClick();
            }
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return this.mGestureDetector.onTouchEvent(motionEvent);
        }
    }

    private final class QuarterHourRunnable implements Runnable {
        private QuarterHourRunnable() {
        }

        @Override
        public void run() {
            ClockFragment.this.mCityAdapter.notifyDataSetChanged();
        }
    }

    private final class AlarmObserverPreL extends ContentObserver {
        private AlarmObserverPreL() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            ClockFragment.this.refreshAlarm();
        }
    }

    private final class AlarmChangedBroadcastReceiver extends BroadcastReceiver {
        private AlarmChangedBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ClockFragment.this.refreshAlarm();
        }
    }

    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener implements View.OnLayoutChangeListener {
        private ScrollPositionWatcher() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            ClockFragment.this.setTabScrolledToTop(Utils.isScrolledToTop(ClockFragment.this.mCityList));
        }

        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            ClockFragment.this.setTabScrolledToTop(Utils.isScrolledToTop(ClockFragment.this.mCityList));
        }
    }

    private static final class SelectedCitiesAdapter extends RecyclerView.Adapter implements CityListener {
        private static final int MAIN_CLOCK = 2131558466;
        private static final int WORLD_CLOCK = 2131558521;
        private final Context mContext;
        private final String mDateFormat;
        private final String mDateFormatForAccessibility;
        private final LayoutInflater mInflater;
        private final boolean mIsPortrait;
        private final boolean mShowHomeClock;

        private SelectedCitiesAdapter(Context context, String str, String str2) {
            this.mContext = context;
            this.mDateFormat = str;
            this.mDateFormatForAccessibility = str2;
            this.mInflater = LayoutInflater.from(context);
            this.mIsPortrait = Utils.isPortrait(context);
            this.mShowHomeClock = DataModel.getDataModel().getShowHomeClock();
        }

        @Override
        public int getItemViewType(int i) {
            if (i == 0 && this.mIsPortrait) {
                return R.layout.main_clock_frame;
            }
            return R.layout.world_clock_item;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View viewInflate = this.mInflater.inflate(i, viewGroup, false);
            if (i == R.layout.main_clock_frame) {
                return new MainClockViewHolder(viewInflate);
            }
            if (i == R.layout.world_clock_item) {
                return new CityViewHolder(viewInflate);
            }
            throw new IllegalArgumentException("View type not recognized");
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            City homeCity;
            int itemViewType = getItemViewType(i);
            if (itemViewType == R.layout.main_clock_frame) {
                ((MainClockViewHolder) viewHolder).bind(this.mContext, this.mDateFormat, this.mDateFormatForAccessibility, getItemCount() > 1);
                return;
            }
            if (itemViewType == R.layout.world_clock_item) {
                if (this.mShowHomeClock && i == this.mIsPortrait) {
                    homeCity = getHomeCity();
                } else {
                    homeCity = getCities().get(i - ((this.mIsPortrait ? 1 : 0) + (this.mShowHomeClock ? 1 : 0)));
                }
                ((CityViewHolder) viewHolder).bind(this.mContext, homeCity, i, this.mIsPortrait);
                return;
            }
            throw new IllegalArgumentException("Unexpected view type: " + itemViewType);
        }

        @Override
        public int getItemCount() {
            boolean z = this.mIsPortrait;
            boolean z2 = this.mShowHomeClock;
            return (z ? 1 : 0) + (z2 ? 1 : 0) + getCities().size();
        }

        private City getHomeCity() {
            return DataModel.getDataModel().getHomeCity();
        }

        private List<City> getCities() {
            return DataModel.getDataModel().getSelectedCities();
        }

        private void refreshAlarm() {
            if (this.mIsPortrait && getItemCount() > 0) {
                notifyItemChanged(0);
            }
        }

        @Override
        public void citiesChanged(List<City> list, List<City> list2) {
            notifyDataSetChanged();
        }

        private static final class CityViewHolder extends RecyclerView.ViewHolder {
            private final AnalogClock mAnalogClock;
            private final TextClock mDigitalClock;
            private final TextView mHoursAhead;
            private final TextView mName;

            private CityViewHolder(View view) {
                super(view);
                this.mName = (TextView) view.findViewById(R.id.city_name);
                this.mDigitalClock = (TextClock) view.findViewById(R.id.digital_clock);
                this.mAnalogClock = (AnalogClock) view.findViewById(R.id.analog_clock);
                this.mHoursAhead = (TextView) view.findViewById(R.id.hours_ahead);
            }

            private void bind(Context context, City city, int i, boolean z) {
                String id = city.getTimeZone().getID();
                if (DataModel.getDataModel().getClockStyle() == DataModel.ClockStyle.ANALOG) {
                    this.mDigitalClock.setVisibility(8);
                    this.mAnalogClock.setVisibility(0);
                    this.mAnalogClock.setTimeZone(id);
                    this.mAnalogClock.enableSeconds(false);
                } else {
                    this.mAnalogClock.setVisibility(8);
                    this.mDigitalClock.setVisibility(0);
                    this.mDigitalClock.setTimeZone(id);
                    this.mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(0.3f, false));
                    this.mDigitalClock.setFormat24Hour(Utils.get24ModeFormat(false));
                }
                int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.medium_space_top);
                if (i == 0 && !z) {
                    dimensionPixelSize = 0;
                }
                this.itemView.setPadding(this.itemView.getPaddingLeft(), dimensionPixelSize, this.itemView.getPaddingRight(), this.itemView.getPaddingBottom());
                this.mName.setText(city.getName());
                boolean z2 = Calendar.getInstance(TimeZone.getDefault()).get(7) != Calendar.getInstance(city.getTimeZone()).get(7);
                TimeZone timeZone = TimeZone.getDefault();
                TimeZone timeZone2 = TimeZone.getTimeZone(id);
                long jCurrentTimeMillis = System.currentTimeMillis();
                long offset = ((long) timeZone2.getOffset(jCurrentTimeMillis)) - ((long) timeZone.getOffset(jCurrentTimeMillis));
                int i2 = (int) (offset / 3600000);
                int i3 = ((int) (offset / 60000)) % 60;
                boolean z3 = offset % 3600000 != 0;
                boolean z4 = i2 > 0 || (i2 == 0 && i3 > 0);
                if (!Utils.isLandscape(context)) {
                    this.mHoursAhead.setVisibility(i2 != 0 || z3 ? 0 : 8);
                    String strCreateHoursDifferentString = Utils.createHoursDifferentString(context, z3, z4, i2, i3);
                    TextView textView = this.mHoursAhead;
                    if (z2) {
                        strCreateHoursDifferentString = context.getString(z4 ? R.string.world_hours_tomorrow : R.string.world_hours_yesterday, strCreateHoursDifferentString);
                    }
                    textView.setText(strCreateHoursDifferentString);
                    return;
                }
                this.mHoursAhead.setVisibility(z2 ? 0 : 8);
                if (z2) {
                    this.mHoursAhead.setText(context.getString(z4 ? R.string.world_tomorrow : R.string.world_yesterday));
                }
            }
        }

        private static final class MainClockViewHolder extends RecyclerView.ViewHolder {
            private final AnalogClock mAnalogClock;
            private final TextClock mDigitalClock;
            private final View mHairline;

            private MainClockViewHolder(View view) {
                super(view);
                this.mHairline = view.findViewById(R.id.hairline);
                this.mDigitalClock = (TextClock) view.findViewById(R.id.digital_clock);
                this.mAnalogClock = (AnalogClock) view.findViewById(R.id.analog_clock);
                Utils.setClockIconTypeface(view);
            }

            private void bind(Context context, String str, String str2, boolean z) {
                Utils.refreshAlarm(context, this.itemView);
                Utils.updateDate(str, str2, this.itemView);
                Utils.setClockStyle(this.mDigitalClock, this.mAnalogClock);
                this.mHairline.setVisibility(z ? 0 : 8);
                Utils.setClockSecondsEnabled(this.mDigitalClock, this.mAnalogClock);
            }
        }
    }
}
