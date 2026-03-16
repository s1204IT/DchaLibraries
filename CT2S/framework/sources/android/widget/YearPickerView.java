package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import com.android.internal.R;
import java.util.Calendar;

class YearPickerView extends ListView implements AdapterView.OnItemClickListener, OnDateChangedListener {
    private final YearAdapter mAdapter;
    private final int mChildSize;
    private DatePickerController mController;
    private final Calendar mMaxDate;
    private final Calendar mMinDate;
    private int mSelectedPosition;
    private final int mViewSize;
    private int mYearSelectedCircleColor;

    public YearPickerView(Context context) {
        this(context, null);
    }

    public YearPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842868);
    }

    public YearPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public YearPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMinDate = Calendar.getInstance();
        this.mMaxDate = Calendar.getInstance();
        this.mSelectedPosition = -1;
        AbsListView.LayoutParams frame = new AbsListView.LayoutParams(-1, -2);
        setLayoutParams(frame);
        Resources res = context.getResources();
        this.mViewSize = res.getDimensionPixelOffset(R.dimen.datepicker_view_animator_height);
        this.mChildSize = res.getDimensionPixelOffset(R.dimen.datepicker_year_label_height);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(this.mChildSize / 3);
        int paddingTop = res.getDimensionPixelSize(R.dimen.datepicker_year_picker_padding_top);
        setPadding(0, paddingTop, 0, 0);
        setOnItemClickListener(this);
        setDividerHeight(0);
        this.mAdapter = new YearAdapter(getContext(), R.layout.year_label_text_view);
        setAdapter((ListAdapter) this.mAdapter);
    }

    public void setRange(Calendar min, Calendar max) {
        this.mMinDate.setTimeInMillis(min.getTimeInMillis());
        this.mMaxDate.setTimeInMillis(max.getTimeInMillis());
        updateAdapterData();
    }

    public void init(DatePickerController controller) {
        this.mController = controller;
        this.mController.registerOnDateChangedListener(this);
        updateAdapterData();
        onDateChanged();
    }

    public void setYearSelectedCircleColor(int color) {
        if (color != this.mYearSelectedCircleColor) {
            this.mYearSelectedCircleColor = color;
        }
        requestLayout();
    }

    public int getYearSelectedCircleColor() {
        return this.mYearSelectedCircleColor;
    }

    private void updateAdapterData() {
        this.mAdapter.clear();
        int maxYear = this.mMaxDate.get(1);
        for (int year = this.mMinDate.get(1); year <= maxYear; year++) {
            this.mAdapter.add(Integer.valueOf(year));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.mController.tryVibrate();
        if (position != this.mSelectedPosition) {
            this.mSelectedPosition = position;
            this.mAdapter.notifyDataSetChanged();
        }
        this.mController.onYearSelected(this.mAdapter.getItem(position).intValue());
    }

    void setItemTextAppearance(int resId) {
        this.mAdapter.setItemTextAppearance(resId);
    }

    private class YearAdapter extends ArrayAdapter<Integer> {
        int mItemTextAppearanceResId;

        public YearAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextViewWithCircularIndicator v = (TextViewWithCircularIndicator) super.getView(position, convertView, parent);
            v.setTextAppearance(getContext(), this.mItemTextAppearanceResId);
            v.requestLayout();
            int year = getItem(position).intValue();
            boolean selected = YearPickerView.this.mController.getSelectedDay().get(1) == year;
            v.setDrawIndicator(selected);
            if (selected) {
                v.setCircleColor(YearPickerView.this.mYearSelectedCircleColor);
            }
            return v;
        }

        public void setItemTextAppearance(int resId) {
            this.mItemTextAppearanceResId = resId;
        }
    }

    public void postSetSelectionCentered(int position) {
        postSetSelectionFromTop(position, (this.mViewSize / 2) - (this.mChildSize / 2));
    }

    public void postSetSelectionFromTop(final int position, final int offset) {
        post(new Runnable() {
            @Override
            public void run() {
                YearPickerView.this.setSelectionFromTop(position, offset);
                YearPickerView.this.requestLayout();
            }
        });
    }

    public int getFirstPositionOffset() {
        View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
    }

    @Override
    public void onDateChanged() {
        updateAdapterData();
        this.mAdapter.notifyDataSetChanged();
        postSetSelectionCentered(this.mController.getSelectedDay().get(1) - this.mMinDate.get(1));
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (event.getEventType() == 4096) {
            event.setFromIndex(0);
            event.setToIndex(0);
        }
    }
}
