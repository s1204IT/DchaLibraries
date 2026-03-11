package com.android.systemui.tuner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.IntentButtonProvider;
import com.android.systemui.statusbar.ScalingDrawableWrapper;
import com.android.systemui.statusbar.phone.ExpandableIndicator;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.tuner.LockscreenFragment;
import com.android.systemui.tuner.ShortcutParser;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class LockscreenFragment extends PreferenceFragment {
    private Handler mHandler;
    private final ArrayList<TunerService.Tunable> mTunables = new ArrayList<>();
    private TunerService mTunerService;

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        this.mTunerService = (TunerService) Dependency.get(TunerService.class);
        this.mHandler = new Handler();
        addPreferencesFromResource(R.xml.lockscreen_settings);
        setupGroup("sysui_keyguard_left", "sysui_keyguard_left_unlock");
        setupGroup("sysui_keyguard_right", "sysui_keyguard_right_unlock");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mTunables.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.mTunerService.removeTunable((TunerService.Tunable) obj);
            }
        });
    }

    private void setupGroup(String str, String str2) {
        final Preference preferenceFindPreference = findPreference(str);
        final SwitchPreference switchPreference = (SwitchPreference) findPreference(str2);
        addTunable(new TunerService.Tunable() {
            @Override
            public final void onTuningChanged(String str3, String str4) {
                LockscreenFragment.lambda$setupGroup$1(this.f$0, switchPreference, preferenceFindPreference, str3, str4);
            }
        }, str);
    }

    public static void lambda$setupGroup$1(LockscreenFragment lockscreenFragment, SwitchPreference switchPreference, Preference preference, String str, String str2) {
        switchPreference.setVisible(!TextUtils.isEmpty(str2));
        lockscreenFragment.setSummary(preference, str2);
    }

    private void setSummary(Preference preference, String str) {
        if (str == null) {
            preference.setSummary(R.string.lockscreen_none);
            return;
        }
        if (str.contains("::")) {
            ShortcutParser.Shortcut shortcutInfo = getShortcutInfo(getContext(), str);
            preference.setSummary(shortcutInfo != null ? shortcutInfo.label : null);
        } else if (str.contains("/")) {
            ActivityInfo activityinfo = getActivityinfo(getContext(), str);
            preference.setSummary(activityinfo != null ? activityinfo.loadLabel(getContext().getPackageManager()) : null);
        } else {
            preference.setSummary(R.string.lockscreen_none);
        }
    }

    private void addTunable(TunerService.Tunable tunable, String... strArr) {
        this.mTunables.add(tunable);
        this.mTunerService.addTunable(tunable, strArr);
    }

    public static ActivityInfo getActivityinfo(Context context, String str) {
        try {
            return context.getPackageManager().getActivityInfo(ComponentName.unflattenFromString(str), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static ShortcutParser.Shortcut getShortcutInfo(Context context, String str) {
        return ShortcutParser.Shortcut.create(context, str);
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public final ExpandableIndicator expand;
        public final ImageView icon;
        public final TextView title;

        public Holder(View view) {
            super(view);
            this.icon = (ImageView) view.findViewById(android.R.id.icon);
            this.title = (TextView) view.findViewById(android.R.id.title);
            this.expand = (ExpandableIndicator) view.findViewById(R.id.expand);
        }
    }

    private static class StaticShortcut extends Item {
        private final Context mContext;
        private final ShortcutParser.Shortcut mShortcut;

        @Override
        public Drawable getDrawable() {
            return this.mShortcut.icon.loadDrawable(this.mContext);
        }

        @Override
        public String getLabel() {
            return this.mShortcut.label;
        }

        @Override
        public Boolean getExpando() {
            return null;
        }
    }

    static class App extends Item {
        private final ArrayList<Item> mChildren;
        private final Context mContext;
        private boolean mExpanded;
        private final LauncherActivityInfo mInfo;

        @Override
        public Drawable getDrawable() {
            return this.mInfo.getBadgedIcon(this.mContext.getResources().getConfiguration().densityDpi);
        }

        @Override
        public String getLabel() {
            return this.mInfo.getLabel().toString();
        }

        @Override
        public Boolean getExpando() {
            if (this.mChildren.size() != 0) {
                return Boolean.valueOf(this.mExpanded);
            }
            return null;
        }

        @Override
        public void toggleExpando(final Adapter adapter) {
            this.mExpanded = !this.mExpanded;
            if (this.mExpanded) {
                this.mChildren.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        adapter.addItem(this.f$0, (LockscreenFragment.Item) obj);
                    }
                });
            } else {
                this.mChildren.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        adapter.remItem((LockscreenFragment.Item) obj);
                    }
                });
            }
        }
    }

    static abstract class Item {
        public abstract Drawable getDrawable();

        public abstract Boolean getExpando();

        public abstract String getLabel();

        private Item() {
        }

        public void toggleExpando(Adapter adapter) {
        }
    }

    public static class Adapter extends RecyclerView.Adapter<Holder> {
        private final Consumer<Item> mCallback;
        private ArrayList<Item> mItems;

        @Override
        public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new Holder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.tuner_shortcut_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(final Holder holder, int i) {
            Item item = this.mItems.get(i);
            holder.icon.setImageDrawable(item.getDrawable());
            holder.title.setText(item.getLabel());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    LockscreenFragment.Adapter adapter = this.f$0;
                    adapter.mCallback.accept(adapter.mItems.get(holder.getAdapterPosition()));
                }
            });
            Boolean expando = item.getExpando();
            if (expando != null) {
                holder.expand.setVisibility(0);
                holder.expand.setExpanded(expando.booleanValue());
                holder.expand.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        LockscreenFragment.Adapter adapter = this.f$0;
                        adapter.mItems.get(holder.getAdapterPosition()).toggleExpando(adapter);
                    }
                });
                return;
            }
            holder.expand.setVisibility(8);
        }

        @Override
        public int getItemCount() {
            return this.mItems.size();
        }

        public void remItem(Item item) {
            int iIndexOf = this.mItems.indexOf(item);
            this.mItems.remove(item);
            notifyItemRemoved(iIndexOf);
        }

        public void addItem(Item item, Item item2) {
            int iIndexOf = this.mItems.indexOf(item) + 1;
            this.mItems.add(iIndexOf, item2);
            notifyItemInserted(iIndexOf);
        }
    }

    public static class LockButtonFactory implements ExtensionController.TunerFactory<IntentButtonProvider.IntentButton> {
        private final Context mContext;
        private final String mKey;

        @Override
        public IntentButtonProvider.IntentButton create(Map map) {
            return create((Map<String, String>) map);
        }

        public LockButtonFactory(Context context, String str) {
            this.mContext = context;
            this.mKey = str;
        }

        @Override
        public String[] keys() {
            return new String[]{this.mKey};
        }

        @Override
        public IntentButtonProvider.IntentButton create(Map<String, String> map) {
            ActivityInfo activityinfo;
            String str = map.get(this.mKey);
            if (!TextUtils.isEmpty(str)) {
                if (str.contains("::")) {
                    ShortcutParser.Shortcut shortcutInfo = LockscreenFragment.getShortcutInfo(this.mContext, str);
                    if (shortcutInfo != null) {
                        return new ShortcutButton(this.mContext, shortcutInfo);
                    }
                    return null;
                }
                if (str.contains("/") && (activityinfo = LockscreenFragment.getActivityinfo(this.mContext, str)) != null) {
                    return new ActivityButton(this.mContext, activityinfo);
                }
                return null;
            }
            return null;
        }
    }

    private static class ShortcutButton implements IntentButtonProvider.IntentButton {
        private final IntentButtonProvider.IntentButton.IconState mIconState = new IntentButtonProvider.IntentButton.IconState();
        private final ShortcutParser.Shortcut mShortcut;

        public ShortcutButton(Context context, ShortcutParser.Shortcut shortcut) {
            this.mShortcut = shortcut;
            this.mIconState.isVisible = true;
            this.mIconState.drawable = shortcut.icon.loadDrawable(context).mutate();
            this.mIconState.contentDescription = this.mShortcut.label;
            this.mIconState.drawable = new ScalingDrawableWrapper(this.mIconState.drawable, ((int) TypedValue.applyDimension(1, 32.0f, context.getResources().getDisplayMetrics())) / this.mIconState.drawable.getIntrinsicWidth());
            this.mIconState.tint = false;
        }

        @Override
        public IntentButtonProvider.IntentButton.IconState getIcon() {
            return this.mIconState;
        }

        @Override
        public Intent getIntent() {
            return this.mShortcut.intent;
        }
    }

    private static class ActivityButton implements IntentButtonProvider.IntentButton {
        private final IntentButtonProvider.IntentButton.IconState mIconState = new IntentButtonProvider.IntentButton.IconState();
        private final Intent mIntent;

        public ActivityButton(Context context, ActivityInfo activityInfo) {
            this.mIntent = new Intent().setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
            this.mIconState.isVisible = true;
            this.mIconState.drawable = activityInfo.loadIcon(context.getPackageManager()).mutate();
            this.mIconState.contentDescription = activityInfo.loadLabel(context.getPackageManager());
            this.mIconState.drawable = new ScalingDrawableWrapper(this.mIconState.drawable, ((int) TypedValue.applyDimension(1, 32.0f, context.getResources().getDisplayMetrics())) / this.mIconState.drawable.getIntrinsicWidth());
            this.mIconState.tint = false;
        }

        @Override
        public IntentButtonProvider.IntentButton.IconState getIcon() {
            return this.mIconState;
        }

        @Override
        public Intent getIntent() {
            return this.mIntent;
        }
    }
}
