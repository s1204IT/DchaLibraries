package com.android.internal.app;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaRouter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.R;
import java.util.Comparator;

public class MediaRouteChooserDialog extends Dialog {
    private RouteAdapter mAdapter;
    private boolean mAttachedToWindow;
    private final MediaRouterCallback mCallback;
    private Button mExtendedSettingsButton;
    private View.OnClickListener mExtendedSettingsClickListener;
    private ListView mListView;
    private int mRouteTypes;
    private final MediaRouter mRouter;

    public MediaRouteChooserDialog(Context context, int i) {
        super(context, i);
        this.mRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.mCallback = new MediaRouterCallback();
    }

    public int getRouteTypes() {
        return this.mRouteTypes;
    }

    public void setRouteTypes(int i) {
        if (this.mRouteTypes != i) {
            this.mRouteTypes = i;
            if (this.mAttachedToWindow) {
                this.mRouter.removeCallback(this.mCallback);
                this.mRouter.addCallback(i, this.mCallback, 1);
            }
            refreshRoutes();
        }
    }

    public void setExtendedSettingsClickListener(View.OnClickListener onClickListener) {
        if (onClickListener != this.mExtendedSettingsClickListener) {
            this.mExtendedSettingsClickListener = onClickListener;
            updateExtendedSettingsButton();
        }
    }

    public boolean onFilterRoute(MediaRouter.RouteInfo routeInfo) {
        return !routeInfo.isDefault() && routeInfo.isEnabled() && routeInfo.matchesTypes(this.mRouteTypes);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        int i;
        super.onCreate(bundle);
        getWindow().requestFeature(3);
        setContentView(R.layout.media_route_chooser_dialog);
        if (this.mRouteTypes == 4) {
            i = R.string.media_route_chooser_title_for_remote_display;
        } else {
            i = R.string.media_route_chooser_title;
        }
        setTitle(i);
        getWindow().setFeatureDrawableResource(3, isLightTheme(getContext()) ? R.drawable.ic_media_route_off_holo_light : R.drawable.ic_media_route_off_holo_dark);
        this.mAdapter = new RouteAdapter(getContext());
        this.mListView = (ListView) findViewById(R.id.media_route_list);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this.mAdapter);
        this.mListView.setEmptyView(findViewById(16908292));
        this.mExtendedSettingsButton = (Button) findViewById(R.id.media_route_extended_settings_button);
        updateExtendedSettingsButton();
    }

    private void updateExtendedSettingsButton() {
        if (this.mExtendedSettingsButton != null) {
            this.mExtendedSettingsButton.setOnClickListener(this.mExtendedSettingsClickListener);
            this.mExtendedSettingsButton.setVisibility(this.mExtendedSettingsClickListener != null ? 0 : 8);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        this.mRouter.addCallback(this.mRouteTypes, this.mCallback, 1);
        refreshRoutes();
    }

    @Override
    public void onDetachedFromWindow() {
        this.mAttachedToWindow = false;
        this.mRouter.removeCallback(this.mCallback);
        super.onDetachedFromWindow();
    }

    public void refreshRoutes() {
        if (this.mAttachedToWindow) {
            this.mAdapter.update();
        }
    }

    static boolean isLightTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.isLightTheme, typedValue, true) && typedValue.data != 0;
    }

    private final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo> implements AdapterView.OnItemClickListener {
        private final LayoutInflater mInflater;

        public RouteAdapter(Context context) {
            super(context, 0);
            this.mInflater = LayoutInflater.from(context);
        }

        public void update() {
            clear();
            int routeCount = MediaRouteChooserDialog.this.mRouter.getRouteCount();
            for (int i = 0; i < routeCount; i++) {
                MediaRouter.RouteInfo routeAt = MediaRouteChooserDialog.this.mRouter.getRouteAt(i);
                if (MediaRouteChooserDialog.this.onFilterRoute(routeAt)) {
                    add(routeAt);
                }
            }
            sort(RouteComparator.sInstance);
            notifyDataSetChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return getItem(i).isEnabled();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(R.layout.media_route_list_item, viewGroup, false);
            }
            MediaRouter.RouteInfo item = getItem(i);
            TextView textView = (TextView) view.findViewById(16908308);
            TextView textView2 = (TextView) view.findViewById(16908309);
            textView.setText(item.getName());
            CharSequence description = item.getDescription();
            if (TextUtils.isEmpty(description)) {
                textView2.setVisibility(8);
                textView2.setText("");
            } else {
                textView2.setVisibility(0);
                textView2.setText(description);
            }
            view.setEnabled(item.isEnabled());
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            MediaRouter.RouteInfo item = getItem(i);
            if (item.isEnabled()) {
                item.select();
                MediaRouteChooserDialog.this.dismiss();
            }
        }
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
        private MediaRouterCallback() {
        }

        @Override
        public void onRouteAdded(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            MediaRouteChooserDialog.this.refreshRoutes();
        }

        @Override
        public void onRouteRemoved(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            MediaRouteChooserDialog.this.refreshRoutes();
        }

        @Override
        public void onRouteChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            MediaRouteChooserDialog.this.refreshRoutes();
        }

        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            MediaRouteChooserDialog.this.dismiss();
        }
    }

    private static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        public static final RouteComparator sInstance = new RouteComparator();

        private RouteComparator() {
        }

        @Override
        public int compare(MediaRouter.RouteInfo routeInfo, MediaRouter.RouteInfo routeInfo2) {
            return routeInfo.getName().toString().compareTo(routeInfo2.getName().toString());
        }
    }
}
