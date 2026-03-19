package android.app;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class LauncherActivity extends ListActivity {
    IconResizer mIconResizer;
    Intent mIntent;
    PackageManager mPackageManager;

    public static class ListItem {
        public String className;
        public Bundle extras;
        public Drawable icon;
        public CharSequence label;
        public String packageName;
        public ResolveInfo resolveInfo;

        ListItem(PackageManager pm, ResolveInfo resolveInfo, IconResizer resizer) {
            this.resolveInfo = resolveInfo;
            this.label = resolveInfo.loadLabel(pm);
            ComponentInfo ci = resolveInfo.activityInfo;
            ci = ci == null ? resolveInfo.serviceInfo : ci;
            if (this.label == null && ci != null) {
                this.label = resolveInfo.activityInfo.name;
            }
            if (resizer != null) {
                this.icon = resizer.createIconThumbnail(resolveInfo.loadIcon(pm));
            }
            this.packageName = ci.applicationInfo.packageName;
            this.className = ci.name;
        }

        public ListItem() {
        }
    }

    private class ActivityAdapter extends BaseAdapter implements Filterable {
        private final Object lock = new Object();
        protected List<ListItem> mActivitiesList;
        private Filter mFilter;
        protected final IconResizer mIconResizer;
        protected final LayoutInflater mInflater;
        private ArrayList<ListItem> mOriginalValues;
        private final boolean mShowIcons;

        public ActivityAdapter(IconResizer resizer) {
            this.mIconResizer = resizer;
            this.mInflater = (LayoutInflater) LauncherActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mShowIcons = LauncherActivity.this.onEvaluateShowIcons();
            this.mActivitiesList = LauncherActivity.this.makeListItems();
        }

        public Intent intentForPosition(int position) {
            if (this.mActivitiesList == null) {
                return null;
            }
            Intent intent = new Intent(LauncherActivity.this.mIntent);
            ListItem item = this.mActivitiesList.get(position);
            intent.setClassName(item.packageName, item.className);
            if (item.extras != null) {
                intent.putExtras(item.extras);
            }
            return intent;
        }

        public ListItem itemForPosition(int position) {
            if (this.mActivitiesList == null) {
                return null;
            }
            return this.mActivitiesList.get(position);
        }

        @Override
        public int getCount() {
            if (this.mActivitiesList != null) {
                return this.mActivitiesList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = this.mInflater.inflate(17367076, parent, false);
            } else {
                view = convertView;
            }
            bindView(view, this.mActivitiesList.get(position));
            return view;
        }

        private void bindView(View view, ListItem item) {
            TextView text = (TextView) view;
            text.setText(item.label);
            if (!this.mShowIcons) {
                return;
            }
            if (item.icon == null) {
                item.icon = this.mIconResizer.createIconThumbnail(item.resolveInfo.loadIcon(LauncherActivity.this.getPackageManager()));
            }
            text.setCompoundDrawablesWithIntrinsicBounds(item.icon, (Drawable) null, (Drawable) null, (Drawable) null);
        }

        @Override
        public Filter getFilter() {
            ArrayFilter arrayFilter = null;
            if (this.mFilter == null) {
                this.mFilter = new ArrayFilter(this, arrayFilter);
            }
            return this.mFilter;
        }

        private class ArrayFilter extends Filter {
            ArrayFilter(ActivityAdapter this$1, ArrayFilter arrayFilter) {
                this();
            }

            private ArrayFilter() {
            }

            @Override
            protected Filter.FilterResults performFiltering(CharSequence prefix) {
                Filter.FilterResults results = new Filter.FilterResults();
                if (ActivityAdapter.this.mOriginalValues == null) {
                    synchronized (ActivityAdapter.this.lock) {
                        ActivityAdapter.this.mOriginalValues = new ArrayList(ActivityAdapter.this.mActivitiesList);
                    }
                }
                if (prefix == null || prefix.length() == 0) {
                    synchronized (ActivityAdapter.this.lock) {
                        ArrayList<ListItem> list = new ArrayList<>(ActivityAdapter.this.mOriginalValues);
                        results.values = list;
                        results.count = list.size();
                    }
                } else {
                    String prefixString = prefix.toString().toLowerCase();
                    ArrayList<ListItem> values = ActivityAdapter.this.mOriginalValues;
                    int count = values.size();
                    ArrayList<ListItem> newValues = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        ListItem item = values.get(i);
                        String[] words = item.label.toString().toLowerCase().split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        int wordCount = words.length;
                        int k = 0;
                        while (true) {
                            if (k < wordCount) {
                                String word = words[k];
                                if (!word.startsWith(prefixString)) {
                                    k++;
                                } else {
                                    newValues.add(item);
                                    break;
                                }
                            }
                        }
                    }
                    results.values = newValues;
                    results.count = newValues.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                ActivityAdapter.this.mActivitiesList = (List) results.values;
                if (results.count > 0) {
                    ActivityAdapter.this.notifyDataSetChanged();
                } else {
                    ActivityAdapter.this.notifyDataSetInvalidated();
                }
            }
        }
    }

    public class IconResizer {
        private int mIconHeight;
        private int mIconWidth;
        private final Rect mOldBounds = new Rect();
        private Canvas mCanvas = new Canvas();

        public IconResizer() {
            this.mIconWidth = -1;
            this.mIconHeight = -1;
            this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
            Resources resources = LauncherActivity.this.getResources();
            int dimension = (int) resources.getDimension(R.dimen.app_icon_size);
            this.mIconHeight = dimension;
            this.mIconWidth = dimension;
        }

        public Drawable createIconThumbnail(Drawable drawable) {
            int width = this.mIconWidth;
            int height = this.mIconHeight;
            int iconWidth = drawable.getIntrinsicWidth();
            int iconHeight = drawable.getIntrinsicHeight();
            if (drawable instanceof PaintDrawable) {
                drawable.setIntrinsicWidth(width);
                drawable.setIntrinsicHeight(height);
            }
            if (width > 0 && height > 0) {
                if (width < iconWidth || height < iconHeight) {
                    float ratio = iconWidth / iconHeight;
                    if (iconWidth > iconHeight) {
                        height = (int) (width / ratio);
                    } else if (iconHeight > iconWidth) {
                        width = (int) (height * ratio);
                    }
                    Bitmap.Config c = drawable.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                    Bitmap thumb = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, c);
                    Canvas canvas = this.mCanvas;
                    canvas.setBitmap(thumb);
                    this.mOldBounds.set(drawable.getBounds());
                    int x = (this.mIconWidth - width) / 2;
                    int y = (this.mIconHeight - height) / 2;
                    drawable.setBounds(x, y, x + width, y + height);
                    drawable.draw(canvas);
                    drawable.setBounds(this.mOldBounds);
                    Drawable icon = new BitmapDrawable(LauncherActivity.this.getResources(), thumb);
                    canvas.setBitmap(null);
                    return icon;
                }
                if (iconWidth < width && iconHeight < height) {
                    Bitmap.Config c2 = Bitmap.Config.ARGB_8888;
                    Bitmap thumb2 = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, c2);
                    Canvas canvas2 = this.mCanvas;
                    canvas2.setBitmap(thumb2);
                    this.mOldBounds.set(drawable.getBounds());
                    int x2 = (width - iconWidth) / 2;
                    int y2 = (height - iconHeight) / 2;
                    drawable.setBounds(x2, y2, x2 + iconWidth, y2 + iconHeight);
                    drawable.draw(canvas2);
                    drawable.setBounds(this.mOldBounds);
                    Drawable icon2 = new BitmapDrawable(LauncherActivity.this.getResources(), thumb2);
                    canvas2.setBitmap(null);
                    return icon2;
                }
                return drawable;
            }
            return drawable;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPackageManager = getPackageManager();
        if (!this.mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            requestWindowFeature(5);
            setProgressBarIndeterminateVisibility(true);
        }
        onSetContentView();
        this.mIconResizer = new IconResizer();
        this.mIntent = new Intent(getTargetIntent());
        this.mIntent.setComponent(null);
        this.mAdapter = new ActivityAdapter(this.mIconResizer);
        setListAdapter(this.mAdapter);
        getListView().setTextFilterEnabled(true);
        updateAlertTitle();
        updateButtonText();
        if (this.mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            return;
        }
        setProgressBarIndeterminateVisibility(false);
    }

    private void updateAlertTitle() {
        TextView alertTitle = (TextView) findViewById(16909087);
        if (alertTitle == null) {
            return;
        }
        alertTitle.setText(getTitle());
    }

    private void updateButtonText() {
        Button cancelButton = (Button) findViewById(R.id.button1);
        if (cancelButton == null) {
            return;
        }
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LauncherActivity.this.finish();
            }
        });
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        updateAlertTitle();
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        updateAlertTitle();
    }

    protected void onSetContentView() {
        setContentView(17367075);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = intentForPosition(position);
        startActivity(intent);
    }

    protected Intent intentForPosition(int position) {
        ActivityAdapter adapter = (ActivityAdapter) this.mAdapter;
        return adapter.intentForPosition(position);
    }

    protected ListItem itemForPosition(int position) {
        ActivityAdapter adapter = (ActivityAdapter) this.mAdapter;
        return adapter.itemForPosition(position);
    }

    protected Intent getTargetIntent() {
        return new Intent();
    }

    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
        return this.mPackageManager.queryIntentActivities(queryIntent, 0);
    }

    protected void onSortResultList(List<ResolveInfo> results) {
        Collections.sort(results, new ResolveInfo.DisplayNameComparator(this.mPackageManager));
    }

    public List<ListItem> makeListItems() {
        List<ResolveInfo> list = onQueryPackageManager(this.mIntent);
        onSortResultList(list);
        ArrayList<ListItem> result = new ArrayList<>(list.size());
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            ResolveInfo resolveInfo = list.get(i);
            result.add(new ListItem(this.mPackageManager, resolveInfo, null));
        }
        return result;
    }

    protected boolean onEvaluateShowIcons() {
        return true;
    }
}
