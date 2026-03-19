package com.android.internal.globalactions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ActionsAdapter extends BaseAdapter {
    private final Context mContext;
    private final BooleanSupplier mDeviceProvisioned;
    private final List<Action> mItems;
    private final BooleanSupplier mKeyguardShowing;

    public ActionsAdapter(Context context, List<Action> list, BooleanSupplier booleanSupplier, BooleanSupplier booleanSupplier2) {
        this.mContext = context;
        this.mItems = list;
        this.mDeviceProvisioned = booleanSupplier;
        this.mKeyguardShowing = booleanSupplier2;
    }

    @Override
    public int getCount() {
        boolean asBoolean = this.mKeyguardShowing.getAsBoolean();
        boolean asBoolean2 = this.mDeviceProvisioned.getAsBoolean();
        int i = 0;
        for (int i2 = 0; i2 < this.mItems.size(); i2++) {
            Action action = this.mItems.get(i2);
            if ((!asBoolean || action.showDuringKeyguard()) && (asBoolean2 || action.showBeforeProvisioning())) {
                i++;
            }
        }
        return i;
    }

    @Override
    public boolean isEnabled(int i) {
        return getItem(i).isEnabled();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public Action getItem(int i) {
        boolean asBoolean = this.mKeyguardShowing.getAsBoolean();
        boolean asBoolean2 = this.mDeviceProvisioned.getAsBoolean();
        int i2 = 0;
        for (int i3 = 0; i3 < this.mItems.size(); i3++) {
            Action action = this.mItems.get(i3);
            if ((!asBoolean || action.showDuringKeyguard()) && (asBoolean2 || action.showBeforeProvisioning())) {
                if (i2 == i) {
                    return action;
                }
                i2++;
            }
        }
        throw new IllegalArgumentException("position " + i + " out of range of showable actions, filtered count=" + getCount() + ", keyguardshowing=" + asBoolean + ", provisioned=" + asBoolean2);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return getItem(i).create(this.mContext, view, viewGroup, LayoutInflater.from(this.mContext));
    }
}
