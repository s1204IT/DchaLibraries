package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.datetimepicker.R;
import com.android.datetimepicker.date.DatePickerDialog;
import java.util.ArrayList;
import java.util.List;

public class YearPickerView extends ListView implements AdapterView.OnItemClickListener, DatePickerDialog.OnDateChangedListener {
    private YearAdapter mAdapter;
    private int mChildSize;
    private final DatePickerController mController;
    private TextViewWithCircularIndicator mSelectedView;
    private int mViewSize;

    public YearPickerView(Context context, DatePickerController datePickerController) {
        super(context);
        setFocusable(false);
        this.mController = datePickerController;
        this.mController.registerOnDateChangedListener(this);
        setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        Resources resources = context.getResources();
        this.mViewSize = resources.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height);
        this.mChildSize = resources.getDimensionPixelOffset(R.dimen.year_label_height);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(this.mChildSize / 3);
        init(context);
        setOnItemClickListener(this);
        setSelector(new StateListDrawable());
        setDividerHeight(0);
        onDateChanged();
    }

    private void init(Context context) {
        ArrayList arrayList = new ArrayList();
        for (int minYear = this.mController.getMinYear(); minYear <= this.mController.getMaxYear(); minYear++) {
            arrayList.add(String.format("%d", Integer.valueOf(minYear)));
        }
        this.mAdapter = new YearAdapter(context, R.layout.year_label_text_view, arrayList);
        setAdapter((ListAdapter) this.mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        this.mController.tryVibrate();
        TextViewWithCircularIndicator textViewWithCircularIndicator = (TextViewWithCircularIndicator) view;
        if (textViewWithCircularIndicator != null) {
            if (textViewWithCircularIndicator != this.mSelectedView) {
                if (this.mSelectedView != null) {
                    this.mSelectedView.drawIndicator(false);
                    this.mSelectedView.requestLayout();
                }
                textViewWithCircularIndicator.drawIndicator(true);
                textViewWithCircularIndicator.requestLayout();
                this.mSelectedView = textViewWithCircularIndicator;
            }
            this.mController.onYearSelected(getYearFromTextView(textViewWithCircularIndicator));
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private static int getYearFromTextView(TextView textView) {
        return Integer.parseInt(textView.getText().toString());
    }

    private class YearAdapter extends ArrayAdapter<String> {
        public YearAdapter(Context context, int i, List<String> list) {
            super(context, i, list);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextViewWithCircularIndicator textViewWithCircularIndicator = (TextViewWithCircularIndicator) super.getView(i, view, viewGroup);
            textViewWithCircularIndicator.requestLayout();
            boolean z = YearPickerView.this.mController.getSelectedDay().getYear() == YearPickerView.getYearFromTextView(textViewWithCircularIndicator);
            textViewWithCircularIndicator.drawIndicator(z);
            if (z) {
                YearPickerView.this.mSelectedView = textViewWithCircularIndicator;
            }
            return textViewWithCircularIndicator;
        }
    }

    public void postSetSelectionCentered(int i) {
        postSetSelectionFromTop(i, (this.mViewSize / 2) - (this.mChildSize / 2));
    }

    public void postSetSelectionFromTop(final int i, final int i2) {
        post(new Runnable() {
            @Override
            public void run() {
                YearPickerView.this.setSelectionFromTop(i, i2);
                YearPickerView.this.requestLayout();
            }
        });
    }

    public int getFirstPositionOffset() {
        View childAt = getChildAt(0);
        if (childAt == null) {
            return 0;
        }
        return childAt.getTop();
    }

    @Override
    public void onDateChanged() {
        this.mAdapter.notifyDataSetChanged();
        postSetSelectionCentered(this.mController.getSelectedDay().getYear() - this.mController.getMinYear());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (accessibilityEvent.getEventType() == 4096) {
            accessibilityEvent.setFromIndex(0);
            accessibilityEvent.setToIndex(0);
        }
    }
}
