package com.android.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.AppWidgetLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityPicker extends AlertActivity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private PickAdapter mAdapter;
    private Intent mBaseIntent;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Parcelable parcel = intent.getParcelableExtra("android.intent.extra.INTENT");
        if (parcel instanceof Intent) {
            this.mBaseIntent = (Intent) parcel;
            this.mBaseIntent.setFlags(this.mBaseIntent.getFlags() & (-196));
        } else {
            this.mBaseIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            this.mBaseIntent.addCategory("android.intent.category.DEFAULT");
        }
        AlertController.AlertParams params = this.mAlertParams;
        params.mOnClickListener = this;
        params.mOnCancelListener = this;
        if (intent.hasExtra("android.intent.extra.TITLE")) {
            params.mTitle = intent.getStringExtra("android.intent.extra.TITLE");
        } else {
            params.mTitle = getTitle();
        }
        List<PickAdapter.Item> items = getItems();
        this.mAdapter = new PickAdapter(this, items);
        params.mAdapter = this.mAdapter;
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = getIntentForPosition(which);
        setResult(-1, intent);
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(0);
        finish();
    }

    protected Intent getIntentForPosition(int position) {
        PickAdapter.Item item = (PickAdapter.Item) this.mAdapter.getItem(position);
        return item.getIntent(this.mBaseIntent);
    }

    protected List<PickAdapter.Item> getItems() {
        PackageManager packageManager = getPackageManager();
        List<PickAdapter.Item> items = new ArrayList<>();
        Intent intent = getIntent();
        ArrayList<String> labels = intent.getStringArrayListExtra("android.intent.extra.shortcut.NAME");
        ArrayList<Intent.ShortcutIconResource> icons = intent.getParcelableArrayListExtra("android.intent.extra.shortcut.ICON_RESOURCE");
        if (labels != null && icons != null && labels.size() == icons.size()) {
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                Drawable icon = null;
                try {
                    Intent.ShortcutIconResource iconResource = icons.get(i);
                    Resources res = packageManager.getResourcesForApplication(iconResource.packageName);
                    icon = res.getDrawable(res.getIdentifier(iconResource.resourceName, null, null), null);
                } catch (PackageManager.NameNotFoundException e) {
                }
                items.add(new PickAdapter.Item((Context) this, (CharSequence) label, icon));
            }
        }
        if (this.mBaseIntent != null) {
            putIntentItems(this.mBaseIntent, items);
        }
        return items;
    }

    protected void putIntentItems(Intent baseIntent, List<PickAdapter.Item> items) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(baseIntent, 0);
        Collections.sort(list, new ResolveInfo.DisplayNameComparator(packageManager));
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            ResolveInfo resolveInfo = list.get(i);
            items.add(new PickAdapter.Item((Context) this, packageManager, resolveInfo));
        }
    }

    protected static class PickAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final List<Item> mItems;

        public static class Item implements AppWidgetLoader.LabelledItem {
            protected static IconResizer sResizer;
            String className;
            Bundle extras;
            Drawable icon;
            CharSequence label;
            String packageName;

            protected IconResizer getResizer(Context context) {
                if (sResizer == null) {
                    Resources resources = context.getResources();
                    int size = (int) resources.getDimension(android.R.dimen.app_icon_size);
                    sResizer = new IconResizer(size, size, resources.getDisplayMetrics());
                }
                return sResizer;
            }

            Item(Context context, CharSequence label, Drawable icon) {
                this.label = label;
                this.icon = getResizer(context).createIconThumbnail(icon);
            }

            Item(Context context, PackageManager pm, ResolveInfo resolveInfo) {
                this.label = resolveInfo.loadLabel(pm);
                if (this.label == null && resolveInfo.activityInfo != null) {
                    this.label = resolveInfo.activityInfo.name;
                }
                this.icon = getResizer(context).createIconThumbnail(resolveInfo.loadIcon(pm));
                this.packageName = resolveInfo.activityInfo.applicationInfo.packageName;
                this.className = resolveInfo.activityInfo.name;
            }

            Intent getIntent(Intent baseIntent) {
                Intent intent = new Intent(baseIntent);
                if (this.packageName != null && this.className != null) {
                    intent.setClassName(this.packageName, this.className);
                    if (this.extras != null) {
                        intent.putExtras(this.extras);
                    }
                } else {
                    intent.setAction("android.intent.action.CREATE_SHORTCUT");
                    intent.putExtra("android.intent.extra.shortcut.NAME", this.label);
                }
                return intent;
            }

            @Override
            public CharSequence getLabel() {
                return this.label;
            }
        }

        public PickAdapter(Context context, List<Item> items) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mItems = items;
        }

        @Override
        public int getCount() {
            return this.mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return this.mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.pick_item, parent, false);
            }
            Item item = (Item) getItem(position);
            TextView textView = (TextView) convertView;
            textView.setText(item.label);
            textView.setCompoundDrawablesWithIntrinsicBounds(item.icon, (Drawable) null, (Drawable) null, (Drawable) null);
            return convertView;
        }
    }

    private static class IconResizer {
        private final int mIconHeight;
        private final int mIconWidth;
        private final DisplayMetrics mMetrics;
        private final Rect mOldBounds = new Rect();
        private final Canvas mCanvas = new Canvas();

        public IconResizer(int width, int height, DisplayMetrics metrics) {
            this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
            this.mMetrics = metrics;
            this.mIconWidth = width;
            this.mIconHeight = height;
        }

        public Drawable createIconThumbnail(Drawable icon) {
            Drawable icon2;
            int width = this.mIconWidth;
            int height = this.mIconHeight;
            if (icon == null) {
                return new EmptyDrawable(width, height);
            }
            try {
                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                } else if (icon instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    if (bitmap.getDensity() == 0) {
                        bitmapDrawable.setTargetDensity(this.mMetrics);
                    }
                }
                int iconWidth = icon.getIntrinsicWidth();
                int iconHeight = icon.getIntrinsicHeight();
                if (iconWidth > 0 && iconHeight > 0) {
                    try {
                        if (width < iconWidth || height < iconHeight) {
                            float ratio = iconWidth / iconHeight;
                            if (iconWidth > iconHeight) {
                                height = (int) (width / ratio);
                            } else if (iconHeight > iconWidth) {
                                width = (int) (height * ratio);
                            }
                            Bitmap.Config c = icon.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                            Bitmap thumb = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, c);
                            Canvas canvas = this.mCanvas;
                            canvas.setBitmap(thumb);
                            this.mOldBounds.set(icon.getBounds());
                            int x = (this.mIconWidth - width) / 2;
                            int y = (this.mIconHeight - height) / 2;
                            icon.setBounds(x, y, x + width, y + height);
                            icon.draw(canvas);
                            icon.setBounds(this.mOldBounds);
                            icon2 = new BitmapDrawable(thumb);
                            ((BitmapDrawable) icon2).setTargetDensity(this.mMetrics);
                            canvas.setBitmap(null);
                            icon = icon2;
                        } else if (iconWidth < width && iconHeight < height) {
                            Bitmap.Config c2 = Bitmap.Config.ARGB_8888;
                            Bitmap thumb2 = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, c2);
                            Canvas canvas2 = this.mCanvas;
                            canvas2.setBitmap(thumb2);
                            this.mOldBounds.set(icon.getBounds());
                            int x2 = (width - iconWidth) / 2;
                            int y2 = (height - iconHeight) / 2;
                            icon.setBounds(x2, y2, x2 + iconWidth, y2 + iconHeight);
                            icon.draw(canvas2);
                            icon.setBounds(this.mOldBounds);
                            icon2 = new BitmapDrawable(thumb2);
                            ((BitmapDrawable) icon2).setTargetDensity(this.mMetrics);
                            canvas2.setBitmap(null);
                            icon = icon2;
                        }
                    } catch (Throwable th) {
                        icon = new EmptyDrawable(width, height);
                    }
                }
            } catch (Throwable th2) {
            }
            return icon;
        }
    }

    private static class EmptyDrawable extends Drawable {
        private final int mHeight;
        private final int mWidth;

        EmptyDrawable(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return this.mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return this.mHeight;
        }

        @Override
        public int getMinimumWidth() {
            return this.mWidth;
        }

        @Override
        public int getMinimumHeight() {
            return this.mHeight;
        }

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return -3;
        }
    }
}
