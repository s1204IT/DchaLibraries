package com.android.documentsui.sorting;

import android.app.Activity;
import com.android.documentsui.Metrics;
import com.android.documentsui.R;
import java.util.function.Consumer;

public final class SortController {
    static final boolean $assertionsDisabled = false;
    private final WidgetController mDropdownController;
    private final WidgetController mTableHeaderController;

    public interface WidgetController {
        void destroy();

        void setVisibility(int i);
    }

    public SortController(WidgetController widgetController, WidgetController widgetController2) {
        this.mDropdownController = widgetController;
        this.mTableHeaderController = widgetController2;
    }

    public void onViewModeChanged(int i) {
        if (this.mTableHeaderController == null) {
            this.mDropdownController.setVisibility(0);
        }
        switch (i) {
            case 0:
            case 2:
                this.mTableHeaderController.setVisibility(8);
                this.mDropdownController.setVisibility(0);
                break;
            case 1:
                this.mTableHeaderController.setVisibility(0);
                this.mDropdownController.setVisibility(8);
                break;
        }
    }

    public void destroy() {
        this.mDropdownController.destroy();
        if (this.mTableHeaderController != null) {
            this.mTableHeaderController.destroy();
        }
    }

    public static SortController create(final Activity activity, int i, SortModel sortModel) {
        sortModel.setMetricRecorder(new Consumer() {
            @Override
            public final void accept(Object obj) {
                SortController.lambda$create$0(activity, (SortDimension) obj);
            }
        });
        SortController sortController = new SortController(new DropdownSortWidgetController(sortModel, activity.findViewById(R.id.dropdown_sort_widget)), TableHeaderController.create(sortModel, activity.findViewById(R.id.table_header)));
        sortController.onViewModeChanged(i);
        return sortController;
    }

    static void lambda$create$0(Activity activity, SortDimension sortDimension) {
        int id = sortDimension.getId();
        if (id == 16908310) {
            Metrics.logUserAction(activity, 4);
        } else if (id == R.id.date) {
            Metrics.logUserAction(activity, 5);
        } else if (id == R.id.size) {
            Metrics.logUserAction(activity, 6);
        }
    }
}
