package com.android.car.list;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class IconToggleLineItem extends TypedPagedListAdapter.LineItem<ViewHolder> {
    protected final Context mContext;
    protected IconUpdateListener mIconUpdateListener;
    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public final boolean onTouch(View view, MotionEvent motionEvent) {
            return this.f$0.onToggleTouched((Switch) view, motionEvent);
        }
    };
    protected SwitchStateUpdateListener mSwitchStateUpdateListener;
    private final CharSequence mTitle;

    public interface IconUpdateListener {
    }

    public interface SwitchStateUpdateListener {
    }

    public abstract int getIcon();

    public abstract boolean isChecked();

    public abstract boolean onToggleTouched(Switch r1, MotionEvent motionEvent);

    public IconToggleLineItem(CharSequence charSequence, Context context) {
        this.mTitle = charSequence;
        this.mContext = context;
    }

    @Override
    public int getType() {
        return 5;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.title.setText(this.mTitle);
        CharSequence desc = getDesc();
        if (TextUtils.isEmpty(desc)) {
            viewHolder.summary.setVisibility(8);
        } else {
            viewHolder.summary.setVisibility(0);
            viewHolder.summary.setText(desc);
        }
        viewHolder.toggle.setEnabled(true);
        viewHolder.toggle.setChecked(isChecked());
        viewHolder.onUpdateIcon(getIcon());
        viewHolder.toggle.setOnTouchListener(this.mOnTouchListener);
        this.mIconUpdateListener = viewHolder;
        this.mSwitchStateUpdateListener = viewHolder;
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements IconUpdateListener, SwitchStateUpdateListener {
        public final ImageView icon;
        public final TextView summary;
        public final TextView title;
        public final Switch toggle;

        public ViewHolder(View view) {
            super(view);
            this.icon = (ImageView) view.findViewById(R.id.icon);
            this.title = (TextView) view.findViewById(R.id.title);
            this.summary = (TextView) view.findViewById(R.id.desc);
            this.toggle = (Switch) view.findViewById(R.id.toggle_switch);
        }

        public void onUpdateIcon(int i) {
            this.icon.setImageResource(i);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.icon_toggle_line_item, viewGroup, false));
    }

    @Override
    public boolean isClickable() {
        return true;
    }
}
