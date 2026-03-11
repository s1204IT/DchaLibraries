package com.android.systemui.tuner;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.NavigationBarInflaterView;
import com.android.systemui.tuner.KeycodeSelectionHelper;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.List;

public class NavBarTuner extends Fragment implements TunerService.Tunable {
    private NavBarAdapter mNavBarAdapter;
    private PreviewNavInflater mPreview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nav_bar_tuner, container, false);
        inflatePreview((ViewGroup) view.findViewById(R.id.nav_preview_frame));
        return view;
    }

    private void inflatePreview(ViewGroup view) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        boolean isRotated = display.getRotation() == 1 || display.getRotation() == 3;
        Configuration config = new Configuration(getContext().getResources().getConfiguration());
        boolean isPhoneLandscape = isRotated && config.smallestScreenWidthDp < 600;
        float scale = isPhoneLandscape ? 0.75f : 0.95f;
        config.densityDpi = (int) (config.densityDpi * scale);
        this.mPreview = (PreviewNavInflater) LayoutInflater.from(getContext().createConfigurationContext(config)).inflate(R.layout.nav_bar_tuner_inflater, view, false);
        ViewGroup.LayoutParams layoutParams = this.mPreview.getLayoutParams();
        layoutParams.width = (int) ((isPhoneLandscape ? display.getHeight() : display.getWidth()) * scale);
        layoutParams.height = (int) (layoutParams.height * scale);
        if (isPhoneLandscape) {
            int width = layoutParams.width;
            layoutParams.width = layoutParams.height;
            layoutParams.height = width;
        }
        view.addView(this.mPreview);
        if (isRotated) {
            this.mPreview.findViewById(R.id.rot0).setVisibility(8);
            this.mPreview.findViewById(R.id.rot90);
        } else {
            this.mPreview.findViewById(R.id.rot90).setVisibility(8);
            this.mPreview.findViewById(R.id.rot0);
        }
    }

    public void notifyChanged() {
        this.mPreview.onTuningChanged("sysui_nav_bar", this.mNavBarAdapter.getNavString());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        Context context = getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.mNavBarAdapter = new NavBarAdapter(context);
        recyclerView.setAdapter(this.mNavBarAdapter);
        recyclerView.addItemDecoration(new Dividers(context));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this.mNavBarAdapter.mCallbacks);
        this.mNavBarAdapter.setTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        TunerService.get(getContext()).addTunable(this, "sysui_nav_bar");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        TunerService.get(getContext()).removeTunable(this);
    }

    @Override
    public void onTuningChanged(String key, String navLayout) {
        if ("sysui_nav_bar".equals(key)) {
            Context context = getContext();
            if (navLayout == null) {
                navLayout = context.getString(R.string.config_navBarLayout);
            }
            String[] views = navLayout.split(";");
            String[] groups = {"start", "center", "end"};
            CharSequence[] groupLabels = {getString(R.string.start), getString(R.string.center), getString(R.string.end)};
            this.mNavBarAdapter.clear();
            for (int i = 0; i < 3; i++) {
                this.mNavBarAdapter.addButton(groups[i], groupLabels[i]);
                for (String button : views[i].split(",")) {
                    this.mNavBarAdapter.addButton(button, getLabel(button, context));
                }
            }
            this.mNavBarAdapter.addButton("add", getString(R.string.add_button));
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, 2, 0, getString(R.string.save)).setShowAsAction(1);
        menu.add(0, 3, 0, getString(R.string.reset));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 2) {
            if (this.mNavBarAdapter.hasHomeButton()) {
                Settings.Secure.putString(getContext().getContentResolver(), "sysui_nav_bar", this.mNavBarAdapter.getNavString());
            } else {
                new AlertDialog.Builder(getContext()).setTitle(R.string.no_home_title).setMessage(R.string.no_home_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
            return true;
        }
        if (item.getItemId() != 3) {
            return super.onOptionsItemSelected(item);
        }
        Settings.Secure.putString(getContext().getContentResolver(), "sysui_nav_bar", null);
        return true;
    }

    public static CharSequence getLabel(String button, Context context) {
        if (button.startsWith("home")) {
            return context.getString(R.string.accessibility_home);
        }
        if (button.startsWith("back")) {
            return context.getString(R.string.accessibility_back);
        }
        if (button.startsWith("recent")) {
            return context.getString(R.string.accessibility_recent);
        }
        if (button.startsWith("space")) {
            return context.getString(R.string.space);
        }
        if (button.startsWith("menu_ime")) {
            return context.getString(R.string.menu_ime);
        }
        if (button.startsWith("clipboard")) {
            return context.getString(R.string.clipboard);
        }
        if (button.startsWith("key")) {
            return context.getString(R.string.keycode);
        }
        return button;
    }

    private static class Holder extends RecyclerView.ViewHolder {
        private TextView title;

        public Holder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(android.R.id.title);
        }
    }

    private static class Dividers extends RecyclerView.ItemDecoration {
        private final Drawable mDivider;

        public Dividers(Context context) {
            TypedValue value = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.listDivider, value, true);
            this.mDivider = context.getDrawable(value.resourceId);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + this.mDivider.getIntrinsicHeight();
                this.mDivider.setBounds(left, top, right, bottom);
                this.mDivider.draw(c);
            }
        }
    }

    public void selectImage() {
        startActivityForResult(KeycodeSelectionHelper.getSelectImageIntent(), 42);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 42 && resultCode == -1 && data != null) {
            Uri uri = data.getData();
            int takeFlags = data.getFlags() & 1;
            getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
            this.mNavBarAdapter.onImageSelected(uri);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class NavBarAdapter extends RecyclerView.Adapter<Holder> implements View.OnClickListener {
        private int mButtonLayout;
        private int mCategoryLayout;
        private int mKeycode;
        private ItemTouchHelper mTouchHelper;
        private List<String> mButtons = new ArrayList();
        private List<CharSequence> mLabels = new ArrayList();
        private final ItemTouchHelper.Callback mCallbacks = new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() != 1) {
                    return makeMovementFlags(0, 0);
                }
                return makeMovementFlags(3, 0);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (to == 0) {
                    return false;
                }
                move(from, to, NavBarAdapter.this.mButtons);
                move(from, to, NavBarAdapter.this.mLabels);
                NavBarTuner.this.notifyChanged();
                NavBarAdapter.this.notifyItemMoved(from, to);
                return true;
            }

            private <T> void move(int from, int to, List<T> list) {
                list.add(from > to ? to : to + 1, list.get(from));
                if (from > to) {
                    from++;
                }
                list.remove(from);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };

        public NavBarAdapter(Context context) {
            TypedArray attrs = context.getTheme().obtainStyledAttributes(null, android.R.styleable.Preference, android.R.attr.preferenceStyle, 0);
            this.mButtonLayout = attrs.getResourceId(3, 0);
            TypedArray attrs2 = context.getTheme().obtainStyledAttributes(null, android.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
            this.mCategoryLayout = attrs2.getResourceId(3, 0);
        }

        public void setTouchHelper(ItemTouchHelper itemTouchHelper) {
            this.mTouchHelper = itemTouchHelper;
        }

        public void clear() {
            this.mButtons.clear();
            this.mLabels.clear();
            notifyDataSetChanged();
        }

        public void addButton(String button, CharSequence label) {
            this.mButtons.add(button);
            this.mLabels.add(label);
            notifyItemInserted(this.mLabels.size() - 1);
            NavBarTuner.this.notifyChanged();
        }

        public boolean hasHomeButton() {
            int N = this.mButtons.size();
            for (int i = 0; i < N; i++) {
                if (this.mButtons.get(i).startsWith("home")) {
                    return true;
                }
            }
            return false;
        }

        public String getNavString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < this.mButtons.size() - 1; i++) {
                String button = this.mButtons.get(i);
                if (button.equals("center") || button.equals("end")) {
                    if (builder.length() == 0 || builder.toString().endsWith(";")) {
                        builder.append("space");
                    }
                    builder.append(";");
                } else {
                    if (builder.length() != 0 && !builder.toString().endsWith(";")) {
                        builder.append(",");
                    }
                    builder.append(button);
                }
            }
            if (builder.toString().endsWith(";")) {
                builder.append("space");
            }
            return builder.toString();
        }

        @Override
        public int getItemViewType(int position) {
            String button = this.mButtons.get(position);
            if (button.equals("start") || button.equals("center") || button.equals("end")) {
                return 2;
            }
            if (button.equals("add")) {
                return 0;
            }
            return 1;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(getLayoutId(viewType), parent, false);
            if (viewType == 1) {
                inflater.inflate(R.layout.nav_control_widget, (ViewGroup) view.findViewById(android.R.id.widget_frame));
            }
            return new Holder(view);
        }

        private int getLayoutId(int viewType) {
            if (viewType == 2) {
                return this.mCategoryLayout;
            }
            return this.mButtonLayout;
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.title.setText(this.mLabels.get(position));
            if (holder.getItemViewType() == 1) {
                bindButton(holder, position);
            } else {
                if (holder.getItemViewType() != 0) {
                    return;
                }
                bindAdd(holder);
            }
        }

        private void bindAdd(Holder holder) {
            TypedValue value = new TypedValue();
            Context context = holder.itemView.getContext();
            context.getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
            ImageView icon = (ImageView) holder.itemView.findViewById(android.R.id.icon);
            icon.setImageResource(R.drawable.ic_add);
            icon.setImageTintList(ColorStateList.valueOf(context.getColor(value.resourceId)));
            holder.itemView.findViewById(android.R.id.summary).setVisibility(8);
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavBarAdapter.this.showAddDialog(v.getContext());
                }
            });
        }

        private void bindButton(final Holder holder, int position) {
            holder.itemView.findViewById(android.R.id.icon_frame).setVisibility(8);
            holder.itemView.findViewById(android.R.id.summary).setVisibility(8);
            bindClick(holder.itemView.findViewById(R.id.close), holder);
            bindClick(holder.itemView.findViewById(R.id.width), holder);
            holder.itemView.findViewById(R.id.drag).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    NavBarAdapter.this.mTouchHelper.startDrag(holder);
                    return true;
                }
            });
        }

        public void showAddDialog(final Context context) {
            final String[] options = {"back", "home", "recent", "menu_ime", "space", "clipboard", "key"};
            final CharSequence[] labels = new CharSequence[options.length];
            for (int i = 0; i < options.length; i++) {
                labels[i] = NavBarTuner.getLabel(options[i], context);
            }
            new AlertDialog.Builder(context).setTitle(R.string.select_button).setItems(labels, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if ("key".equals(options[which])) {
                        NavBarAdapter.this.showKeyDialogs(context);
                        return;
                    }
                    int index = NavBarAdapter.this.mButtons.size() - 1;
                    NavBarAdapter.this.showAddedMessage(context, options[which]);
                    NavBarAdapter.this.mButtons.add(index, options[which]);
                    NavBarAdapter.this.mLabels.add(index, labels[which]);
                    NavBarAdapter.this.notifyItemInserted(index);
                    NavBarTuner.this.notifyChanged();
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        }

        public void onImageSelected(Uri uri) {
            int index = this.mButtons.size() - 1;
            this.mButtons.add(index, "key(" + this.mKeycode + ":" + uri.toString() + ")");
            this.mLabels.add(index, NavBarTuner.getLabel("key", NavBarTuner.this.getContext()));
            notifyItemInserted(index);
            NavBarTuner.this.notifyChanged();
        }

        public void showKeyDialogs(final Context context) {
            final KeycodeSelectionHelper.OnSelectionComplete listener = new KeycodeSelectionHelper.OnSelectionComplete() {
                @Override
                public void onSelectionComplete(int code) {
                    NavBarAdapter.this.mKeycode = code;
                    NavBarTuner.this.selectImage();
                }
            };
            new AlertDialog.Builder(context).setTitle(R.string.keycode).setMessage(R.string.keycode_description).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    KeycodeSelectionHelper.showKeycodeSelect(context, listener);
                }
            }).show();
        }

        public void showAddedMessage(Context context, String button) {
            if ("clipboard".equals(button)) {
                new AlertDialog.Builder(context).setTitle(R.string.clipboard).setMessage(R.string.clipboard_description).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
        }

        private void bindClick(View view, Holder holder) {
            view.setOnClickListener(this);
            view.setTag(holder);
        }

        @Override
        public void onClick(View v) {
            Holder holder = (Holder) v.getTag();
            if (v.getId() == R.id.width) {
                showWidthDialog(holder, v.getContext());
                return;
            }
            if (v.getId() != R.id.close) {
                return;
            }
            int position = holder.getAdapterPosition();
            this.mButtons.remove(position);
            this.mLabels.remove(position);
            notifyItemRemoved(position);
            NavBarTuner.this.notifyChanged();
        }

        private void showWidthDialog(final Holder holder, Context context) {
            final String buttonSpec = this.mButtons.get(holder.getAdapterPosition());
            float amount = NavigationBarInflaterView.extractSize(buttonSpec);
            final AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.adjust_button_width).setView(R.layout.nav_width_view).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            dialog.setButton(-1, context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    String button = NavigationBarInflaterView.extractButton(buttonSpec);
                    SeekBar seekBar = (SeekBar) dialog.findViewById(R.id.seekbar);
                    if (seekBar.getProgress() == 75) {
                        NavBarAdapter.this.mButtons.set(holder.getAdapterPosition(), button);
                    } else {
                        float amount2 = (seekBar.getProgress() + 25) / 100.0f;
                        NavBarAdapter.this.mButtons.set(holder.getAdapterPosition(), button + "[" + amount2 + "]");
                    }
                    NavBarTuner.this.notifyChanged();
                }
            });
            dialog.show();
            SeekBar seekBar = (SeekBar) dialog.findViewById(R.id.seekbar);
            seekBar.setMax(150);
            seekBar.setProgress((int) ((amount - 0.25f) * 100.0f));
        }

        @Override
        public int getItemCount() {
            return this.mButtons.size();
        }
    }
}
