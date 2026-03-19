package com.android.documentsui.sidebar;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.MenuManager;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.RootInfo;

class RootItem extends Item {
    public DocumentInfo docInfo;
    private final ActionHandler mActionHandler;
    public final RootInfo root;

    public RootItem(RootInfo rootInfo, ActionHandler actionHandler) {
        super(R.layout.item_root, getStringId(rootInfo));
        this.root = rootInfo;
        this.mActionHandler = actionHandler;
    }

    private static String getStringId(RootInfo rootInfo) {
        return String.format("RootItem{%s/%s}", rootInfo.authority == null ? "" : rootInfo.authority, rootInfo.rootId);
    }

    @Override
    public void bindView(View view) {
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        TextView textView2 = (TextView) view.findViewById(android.R.id.summary);
        ImageView imageView2 = (ImageView) view.findViewById(R.id.eject_icon);
        Context context = view.getContext();
        imageView.setImageDrawable(this.root.loadDrawerIcon(context));
        textView.setText(this.root.title);
        if (this.root.supportsEject()) {
            imageView2.setVisibility(0);
            imageView2.setImageDrawable(this.root.loadEjectIcon(context));
            imageView2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    RootsFragment.ejectClicked(view2, RootItem.this.root, RootItem.this.mActionHandler);
                }
            });
        } else {
            imageView2.setVisibility(8);
            imageView2.setOnClickListener(null);
        }
        String string = this.root.summary;
        if (TextUtils.isEmpty(string) && this.root.availableBytes >= 0) {
            string = context.getString(R.string.root_available_bytes, Formatter.formatFileSize(context, this.root.availableBytes));
        }
        textView2.setText(string);
        textView2.setVisibility(TextUtils.isEmpty(string) ? 8 : 0);
    }

    @Override
    boolean isRoot() {
        return true;
    }

    @Override
    void open() {
        this.mActionHandler.openRoot(this.root);
    }

    @Override
    boolean isDropTarget() {
        return this.root.supportsCreate();
    }

    @Override
    boolean dropOn(DragEvent dragEvent) {
        return this.mActionHandler.dropOn(dragEvent, this.root);
    }

    @Override
    void createContextMenu(Menu menu, MenuInflater menuInflater, MenuManager menuManager) {
        menuInflater.inflate(R.menu.root_context_menu, menu);
        menuManager.updateRootContextMenu(menu, this.root, this.docInfo);
    }

    public String toString() {
        return "RootItem{id=" + this.stringId + ", root=" + this.root + ", docInfo=" + this.docInfo + "}";
    }
}
