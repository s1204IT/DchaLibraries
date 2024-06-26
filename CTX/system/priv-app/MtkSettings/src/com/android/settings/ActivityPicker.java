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
/* loaded from: classes.dex */
public class ActivityPicker extends AlertActivity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private PickAdapter mAdapter;
    private Intent mBaseIntent;

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        Intent intent = getIntent();
        Parcelable parcelableExtra = intent.getParcelableExtra("android.intent.extra.INTENT");
        if (parcelableExtra instanceof Intent) {
            this.mBaseIntent = (Intent) parcelableExtra;
            this.mBaseIntent.setFlags(this.mBaseIntent.getFlags() & (-196));
        } else {
            this.mBaseIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            this.mBaseIntent.addCategory("android.intent.category.DEFAULT");
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mOnClickListener = this;
        alertParams.mOnCancelListener = this;
        if (intent.hasExtra("android.intent.extra.TITLE")) {
            alertParams.mTitle = intent.getStringExtra("android.intent.extra.TITLE");
        } else {
            alertParams.mTitle = getTitle();
        }
        this.mAdapter = new PickAdapter(this, getItems());
        alertParams.mAdapter = this.mAdapter;
        setupAlert();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        setResult(-1, getIntentForPosition(i));
        finish();
    }

    @Override // android.content.DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialogInterface) {
        setResult(0);
        finish();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Intent getIntentForPosition(int i) {
        return ((PickAdapter.Item) this.mAdapter.getItem(i)).getIntent(this.mBaseIntent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected List<PickAdapter.Item> getItems() {
        PackageManager packageManager = getPackageManager();
        ArrayList arrayList = new ArrayList();
        Intent intent = getIntent();
        ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("android.intent.extra.shortcut.NAME");
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("android.intent.extra.shortcut.ICON_RESOURCE");
        if (stringArrayListExtra != null && parcelableArrayListExtra != null && stringArrayListExtra.size() == parcelableArrayListExtra.size()) {
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                String str = stringArrayListExtra.get(i);
                Drawable drawable = null;
                try {
                    Intent.ShortcutIconResource shortcutIconResource = (Intent.ShortcutIconResource) parcelableArrayListExtra.get(i);
                    Resources resourcesForApplication = packageManager.getResourcesForApplication(shortcutIconResource.packageName);
                    drawable = resourcesForApplication.getDrawable(resourcesForApplication.getIdentifier(shortcutIconResource.resourceName, null, null), null);
                } catch (PackageManager.NameNotFoundException e) {
                }
                arrayList.add(new PickAdapter.Item((Context) this, (CharSequence) str, drawable));
            }
        }
        if (this.mBaseIntent != null) {
            putIntentItems(this.mBaseIntent, arrayList);
        }
        return arrayList;
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void putIntentItems(Intent intent, List<PickAdapter.Item> list) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> queryIntentActivities = packageManager.queryIntentActivities(intent, 0);
        Collections.sort(queryIntentActivities, new ResolveInfo.DisplayNameComparator(packageManager));
        int size = queryIntentActivities.size();
        for (int i = 0; i < size; i++) {
            list.add(new PickAdapter.Item((Context) this, packageManager, queryIntentActivities.get(i)));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class PickAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final List<Item> mItems;

        /* loaded from: classes.dex */
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
                    int dimension = (int) resources.getDimension(17104896);
                    sResizer = new IconResizer(dimension, dimension, resources.getDisplayMetrics());
                }
                return sResizer;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Item(Context context, CharSequence charSequence, Drawable drawable) {
                this.label = charSequence;
                this.icon = getResizer(context).createIconThumbnail(drawable);
            }

            Item(Context context, PackageManager packageManager, ResolveInfo resolveInfo) {
                this.label = resolveInfo.loadLabel(packageManager);
                if (this.label == null && resolveInfo.activityInfo != null) {
                    this.label = resolveInfo.activityInfo.name;
                }
                this.icon = getResizer(context).createIconThumbnail(resolveInfo.loadIcon(packageManager));
                this.packageName = resolveInfo.activityInfo.applicationInfo.packageName;
                this.className = resolveInfo.activityInfo.name;
            }

            Intent getIntent(Intent intent) {
                Intent intent2 = new Intent(intent);
                if (this.packageName != null && this.className != null) {
                    intent2.setClassName(this.packageName, this.className);
                    if (this.extras != null) {
                        intent2.putExtras(this.extras);
                    }
                } else {
                    intent2.setAction("android.intent.action.CREATE_SHORTCUT");
                    intent2.putExtra("android.intent.extra.shortcut.NAME", this.label);
                }
                return intent2;
            }

            @Override // com.android.settings.AppWidgetLoader.LabelledItem
            public CharSequence getLabel() {
                return this.label;
            }
        }

        public PickAdapter(Context context, List<Item> list) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mItems = list;
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return this.mItems.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return this.mItems.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(R.layout.pick_item, viewGroup, false);
            }
            Item item = (Item) getItem(i);
            TextView textView = (TextView) view;
            textView.setText(item.label);
            textView.setCompoundDrawablesWithIntrinsicBounds(item.icon, (Drawable) null, (Drawable) null, (Drawable) null);
            return view;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class IconResizer {
        private final int mIconHeight;
        private final int mIconWidth;
        private final DisplayMetrics mMetrics;
        private final Rect mOldBounds = new Rect();
        private final Canvas mCanvas = new Canvas();

        public IconResizer(int i, int i2, DisplayMetrics displayMetrics) {
            this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
            this.mMetrics = displayMetrics;
            this.mIconWidth = i;
            this.mIconHeight = i2;
        }

        public Drawable createIconThumbnail(Drawable drawable) {
            int i = this.mIconWidth;
            int i2 = this.mIconHeight;
            if (drawable == null) {
                return new EmptyDrawable(i, i2);
            }
            try {
                if (drawable instanceof PaintDrawable) {
                    PaintDrawable paintDrawable = (PaintDrawable) drawable;
                    paintDrawable.setIntrinsicWidth(i);
                    paintDrawable.setIntrinsicHeight(i2);
                } else if (drawable instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    if (bitmapDrawable.getBitmap().getDensity() == 0) {
                        bitmapDrawable.setTargetDensity(this.mMetrics);
                    }
                }
                int intrinsicWidth = drawable.getIntrinsicWidth();
                int intrinsicHeight = drawable.getIntrinsicHeight();
                if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                    if (i >= intrinsicWidth && i2 >= intrinsicHeight) {
                        if (intrinsicWidth < i && intrinsicHeight < i2) {
                            Bitmap createBitmap = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, Bitmap.Config.ARGB_8888);
                            Canvas canvas = this.mCanvas;
                            canvas.setBitmap(createBitmap);
                            this.mOldBounds.set(drawable.getBounds());
                            int i3 = (i - intrinsicWidth) / 2;
                            int i4 = (i2 - intrinsicHeight) / 2;
                            drawable.setBounds(i3, i4, intrinsicWidth + i3, intrinsicHeight + i4);
                            drawable.draw(canvas);
                            drawable.setBounds(this.mOldBounds);
                            BitmapDrawable bitmapDrawable2 = new BitmapDrawable(createBitmap);
                            bitmapDrawable2.setTargetDensity(this.mMetrics);
                            canvas.setBitmap(null);
                            return bitmapDrawable2;
                        }
                        return drawable;
                    }
                    float f = intrinsicWidth / intrinsicHeight;
                    if (intrinsicWidth > intrinsicHeight) {
                        i2 = (int) (i / f);
                    } else if (intrinsicHeight > intrinsicWidth) {
                        i = (int) (i2 * f);
                    }
                    Bitmap createBitmap2 = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, drawable.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
                    Canvas canvas2 = this.mCanvas;
                    canvas2.setBitmap(createBitmap2);
                    this.mOldBounds.set(drawable.getBounds());
                    int i5 = (this.mIconWidth - i) / 2;
                    int i6 = (this.mIconHeight - i2) / 2;
                    drawable.setBounds(i5, i6, i5 + i, i6 + i2);
                    drawable.draw(canvas2);
                    drawable.setBounds(this.mOldBounds);
                    BitmapDrawable bitmapDrawable3 = new BitmapDrawable(createBitmap2);
                    bitmapDrawable3.setTargetDensity(this.mMetrics);
                    canvas2.setBitmap(null);
                    return bitmapDrawable3;
                }
                return drawable;
            } catch (Throwable th) {
                return new EmptyDrawable(i, i2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class EmptyDrawable extends Drawable {
        private final int mHeight;
        private final int mWidth;

        EmptyDrawable(int i, int i2) {
            this.mWidth = i;
            this.mHeight = i2;
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.mWidth;
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.mHeight;
        }

        @Override // android.graphics.drawable.Drawable
        public int getMinimumWidth() {
            return this.mWidth;
        }

        @Override // android.graphics.drawable.Drawable
        public int getMinimumHeight() {
            return this.mHeight;
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }
    }
}
