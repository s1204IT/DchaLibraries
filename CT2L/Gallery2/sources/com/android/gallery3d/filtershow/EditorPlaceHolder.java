package com.android.gallery3d.filtershow;

import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.android.gallery3d.filtershow.editors.Editor;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import java.util.HashMap;
import java.util.Vector;

public class EditorPlaceHolder {
    private FilterShowActivity mActivity;
    private FrameLayout mContainer = null;
    private HashMap<Integer, Editor> mEditors = new HashMap<>();
    private Vector<ImageShow> mOldViews = new Vector<>();

    public EditorPlaceHolder(FilterShowActivity activity) {
        this.mActivity = null;
        this.mActivity = activity;
    }

    public void setContainer(FrameLayout container) {
        this.mContainer = container;
    }

    public void addEditor(Editor c) {
        this.mEditors.put(Integer.valueOf(c.getID()), c);
    }

    public Editor showEditor(int type) {
        Editor editor = this.mEditors.get(Integer.valueOf(type));
        if (editor == null) {
            return null;
        }
        editor.createEditor(this.mActivity, this.mContainer);
        editor.getImageShow().attach();
        this.mContainer.setVisibility(0);
        this.mContainer.removeAllViews();
        View eview = editor.getTopLevelView();
        ViewParent parent = eview.getParent();
        if (parent != null && (parent instanceof FrameLayout)) {
            ((FrameLayout) parent).removeAllViews();
        }
        this.mContainer.addView(eview);
        hideOldViews();
        editor.setVisibility(0);
        return editor;
    }

    public void setOldViews(Vector<ImageShow> views) {
        this.mOldViews = views;
    }

    public void hide() {
        this.mContainer.setVisibility(8);
    }

    public void hideOldViews() {
        for (View view : this.mOldViews) {
            view.setVisibility(8);
        }
    }

    public Editor getEditor(int editorId) {
        return this.mEditors.get(Integer.valueOf(editorId));
    }
}
