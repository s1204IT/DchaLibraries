package com.android.systemui.statusbar.car.hvac;

public interface TemperatureView {
    int getAreaId();

    int getPropertyId();

    void setTemp(float f);
}
