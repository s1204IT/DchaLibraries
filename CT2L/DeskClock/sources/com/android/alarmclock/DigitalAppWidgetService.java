package com.android.alarmclock;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class DigitalAppWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent i) {
        return new DigitalWidgetViewsFactory(getApplicationContext(), i);
    }
}
