package com.android.documentsui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.documentsui.DocumentsActivity;
import com.android.documentsui.model.DocumentInfo;
import com.android.documentsui.model.RootInfo;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RootsFragment extends Fragment {
    private RootsAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<Collection<RootInfo>> mCallbacks;
    private AdapterView.OnItemClickListener mItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DocumentsActivity activity = DocumentsActivity.get(RootsFragment.this);
            Item item = RootsFragment.this.mAdapter.getItem(position);
            if (item instanceof RootItem) {
                activity.onRootPicked(((RootItem) item).root, true);
            } else {
                if (item instanceof AppItem) {
                    activity.onAppPicked(((AppItem) item).info);
                    return;
                }
                throw new IllegalStateException("Unknown root: " + item);
            }
        }
    };
    private AdapterView.OnItemLongClickListener mItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Item item = RootsFragment.this.mAdapter.getItem(position);
            if (!(item instanceof AppItem)) {
                return false;
            }
            RootsFragment.this.showAppDetails(((AppItem) item).info);
            return true;
        }
    };
    private ListView mList;

    public static void show(FragmentManager fm, Intent includeApps) {
        Bundle args = new Bundle();
        args.putParcelable("includeApps", includeApps);
        RootsFragment fragment = new RootsFragment();
        fragment.setArguments(args);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_roots, fragment);
        ft.commitAllowingStateLoss();
    }

    public static RootsFragment get(FragmentManager fm) {
        return (RootsFragment) fm.findFragmentById(R.id.container_roots);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        inflater.getContext();
        View view = inflater.inflate(R.layout.fragment_roots, container, false);
        this.mList = (ListView) view.findViewById(android.R.id.list);
        this.mList.setOnItemClickListener(this.mItemListener);
        this.mList.setChoiceMode(1);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();
        final RootsCache roots = DocumentsApplication.getRootsCache(context);
        final DocumentsActivity.State state = ((DocumentsActivity) context).getDisplayState();
        this.mCallbacks = new LoaderManager.LoaderCallbacks<Collection<RootInfo>>() {
            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int id, Bundle args) {
                return new RootsLoader(context, roots, state);
            }

            @Override
            public void onLoadFinished(Loader<Collection<RootInfo>> loader, Collection<RootInfo> result) {
                if (RootsFragment.this.isAdded()) {
                    Intent includeApps = (Intent) RootsFragment.this.getArguments().getParcelable("includeApps");
                    RootsFragment.this.mAdapter = new RootsAdapter(context, result, includeApps);
                    RootsFragment.this.mList.setAdapter((ListAdapter) RootsFragment.this.mAdapter);
                    RootsFragment.this.onCurrentRootChanged();
                }
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                RootsFragment.this.mAdapter = null;
                RootsFragment.this.mList.setAdapter((ListAdapter) null);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        onDisplayStateChanged();
    }

    public void onDisplayStateChanged() {
        Context context = getActivity();
        DocumentsActivity.State state = ((DocumentsActivity) context).getDisplayState();
        if (state.action == 3) {
            this.mList.setOnItemLongClickListener(this.mItemLongClickListener);
        } else {
            this.mList.setOnItemLongClickListener(null);
            this.mList.setLongClickable(false);
        }
        getLoaderManager().restartLoader(2, null, this.mCallbacks);
    }

    public void onCurrentRootChanged() {
        if (this.mAdapter != null) {
            RootInfo root = ((DocumentsActivity) getActivity()).getCurrentRoot();
            for (int i = 0; i < this.mAdapter.getCount(); i++) {
                Object item = this.mAdapter.getItem(i);
                if (item instanceof RootItem) {
                    RootInfo testRoot = ((RootItem) item).root;
                    if (Objects.equals(testRoot, root)) {
                        this.mList.setItemChecked(i, true);
                        return;
                    }
                }
            }
        }
    }

    private void showAppDetails(ResolveInfo ri) {
        if (BenesseExtension.getDchaState() == 0) {
            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", ri.activityInfo.packageName, null));
            intent.addFlags(524288);
            startActivity(intent);
        }
    }

    private static abstract class Item {
        private final int mLayoutId;

        public abstract void bindView(View view);

        public Item(int layoutId) {
            this.mLayoutId = layoutId;
        }

        public View getView(View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(this.mLayoutId, parent, false);
            }
            bindView(convertView);
            return convertView;
        }
    }

    private static class RootItem extends Item {
        public final RootInfo root;

        public RootItem(RootInfo root) {
            super(R.layout.item_root);
            this.root = root;
        }

        @Override
        public void bindView(View convertView) {
            ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
            Context context = convertView.getContext();
            icon.setImageDrawable(this.root.loadDrawerIcon(context));
            title.setText(this.root.title);
            String summaryText = this.root.summary;
            if (TextUtils.isEmpty(summaryText) && this.root.availableBytes >= 0) {
                summaryText = context.getString(R.string.root_available_bytes, Formatter.formatFileSize(context, this.root.availableBytes));
            }
            summary.setText(summaryText);
            summary.setVisibility(TextUtils.isEmpty(summaryText) ? 8 : 0);
        }
    }

    private static class SpacerItem extends Item {
        public SpacerItem() {
            super(R.layout.item_root_spacer);
        }

        @Override
        public void bindView(View convertView) {
        }
    }

    private static class AppItem extends Item {
        public final ResolveInfo info;

        public AppItem(ResolveInfo info) {
            super(R.layout.item_root);
            this.info = info;
        }

        @Override
        public void bindView(View convertView) {
            ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
            PackageManager pm = convertView.getContext().getPackageManager();
            icon.setImageDrawable(this.info.loadIcon(pm));
            title.setText(this.info.loadLabel(pm));
            summary.setVisibility(8);
        }
    }

    private static class RootsAdapter extends ArrayAdapter<Item> {
        public RootsAdapter(Context context, Collection<RootInfo> roots, Intent includeApps) {
            super(context, 0);
            RootItem recents = null;
            RootItem images = null;
            RootItem videos = null;
            RootItem audio = null;
            RootItem downloads = null;
            List<RootInfo> clouds = Lists.newArrayList();
            List<RootInfo> locals = Lists.newArrayList();
            for (RootInfo root : roots) {
                if (root.isRecents()) {
                    recents = new RootItem(root);
                } else if (root.isExternalStorage()) {
                    locals.add(root);
                } else if (root.isDownloads()) {
                    downloads = new RootItem(root);
                } else if (root.isImages()) {
                    images = new RootItem(root);
                } else if (root.isVideos()) {
                    videos = new RootItem(root);
                } else if (root.isAudio()) {
                    audio = new RootItem(root);
                } else {
                    clouds.add(root);
                }
            }
            RootComparator comp = new RootComparator();
            Collections.sort(clouds, comp);
            Collections.sort(locals, comp);
            if (recents != null) {
                add(recents);
            }
            for (RootInfo cloud : clouds) {
                add(new RootItem(cloud));
            }
            if (images != null) {
                add(images);
            }
            if (videos != null) {
                add(videos);
            }
            if (audio != null) {
                add(audio);
            }
            if (downloads != null) {
                add(downloads);
            }
            for (RootInfo local : locals) {
                add(new RootItem(local));
            }
            if (includeApps != null) {
                PackageManager pm = context.getPackageManager();
                List<ResolveInfo> infos = pm.queryIntentActivities(includeApps, 65536);
                List<AppItem> apps = Lists.newArrayList();
                for (ResolveInfo info : infos) {
                    if (!context.getPackageName().equals(info.activityInfo.packageName)) {
                        apps.add(new AppItem(info));
                    }
                }
                if (apps.size() > 0) {
                    add(new SpacerItem());
                    for (AppItem item : apps) {
                        add(item);
                    }
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item item = getItem(position);
            return item.getView(convertView, parent);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != 1;
        }

        @Override
        public int getItemViewType(int position) {
            Item item = getItem(position);
            return ((item instanceof RootItem) || (item instanceof AppItem)) ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }

    public static class RootComparator implements Comparator<RootInfo> {
        @Override
        public int compare(RootInfo lhs, RootInfo rhs) {
            int score = DocumentInfo.compareToIgnoreCaseNullable(lhs.title, rhs.title);
            return score != 0 ? score : DocumentInfo.compareToIgnoreCaseNullable(lhs.summary, rhs.summary);
        }
    }
}
