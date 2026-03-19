package com.android.alarmclock;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class DigitalAppWidgetCityService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new DigitalAppWidgetCityViewsFactory(getApplicationContext(), intent);
    }
}
