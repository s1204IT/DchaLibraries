package com.android.documentsui.selection.demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.documentsui.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SelectionDemoAdapter extends RecyclerView.Adapter<DemoHolder> {
    private static final Map<String, DemoItem> sDemoData = new HashMap();
    private OnBindCallback mBindCallback;
    private final Context mContext;

    static {
        for (int i = 0; i < 1000; i++) {
            String strCreateId = createId(i);
            sDemoData.put(strCreateId, new DemoItem(strCreateId, "item" + i));
        }
    }

    SelectionDemoAdapter(Context context) {
        this.mContext = context;
    }

    void addOnBindCallback(OnBindCallback onBindCallback) {
        this.mBindCallback = onBindCallback;
    }

    void loadData() {
        onDataReady();
    }

    private void onDataReady() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return sDemoData.size();
    }

    @Override
    public void onBindViewHolder(DemoHolder demoHolder, int i) {
        demoHolder.update(sDemoData.get(createId(i)));
        if (this.mBindCallback != null) {
            this.mBindCallback.onBound(demoHolder, i);
        }
    }

    private static String createId(int i) {
        return "id" + i;
    }

    @Override
    public DemoHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new DemoHolder((LinearLayout) inflateLayout(this.mContext, viewGroup, R.layout.selection_demo_list_item));
    }

    String getStableId(int i) {
        return createId(i);
    }

    int getPosition(String str) {
        return Integer.parseInt(str.substring(2));
    }

    List<String> getStableIds() {
        return new ArrayList(sDemoData.keySet());
    }

    private static <V extends View> V inflateLayout(Context context, ViewGroup viewGroup, int i) {
        return (V) LayoutInflater.from(context).inflate(i, viewGroup, false);
    }

    static abstract class OnBindCallback {
        abstract void onBound(DemoHolder demoHolder, int i);

        OnBindCallback() {
        }
    }
}
