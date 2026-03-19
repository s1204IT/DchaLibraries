package com.android.gallery3d.filtershow;

import android.view.View;
import android.widget.FrameLayout;
import com.android.gallery3d.filtershow.editors.Editor;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class EditorPlaceHolder {
    private FilterShowActivity mActivity;
    private FrameLayout mContainer = null;
    private HashMap<Integer, Editor> mEditors = new HashMap<>();
    private Vector<ImageShow> mOldViews = new Vector<>();

    public EditorPlaceHolder(FilterShowActivity filterShowActivity) {
        this.mActivity = null;
        this.mActivity = filterShowActivity;
    }

    public void setContainer(FrameLayout frameLayout) {
        this.mContainer = frameLayout;
    }

    public void addEditor(Editor editor) {
        this.mEditors.put(Integer.valueOf(editor.getID()), editor);
    }

    public Editor showEditor(int i) {
        Editor editor = this.mEditors.get(Integer.valueOf(i));
        if (editor == null) {
            return null;
        }
        editor.createEditor(this.mActivity, this.mContainer);
        editor.getImageShow().attach();
        this.mContainer.setVisibility(0);
        this.mContainer.removeAllViews();
        View topLevelView = editor.getTopLevelView();
        ?? parent = topLevelView.getParent();
        if (parent != 0 && (parent instanceof FrameLayout)) {
            parent.removeAllViews();
        }
        this.mContainer.addView(topLevelView);
        hideOldViews();
        editor.setVisibility(0);
        return editor;
    }

    public void setOldViews(Vector<ImageShow> vector) {
        this.mOldViews = vector;
    }

    public void hide() {
        if (this.mContainer != null) {
            this.mContainer.setVisibility(8);
        }
    }

    public void hideOldViews() {
        Iterator<ImageShow> it = this.mOldViews.iterator();
        while (it.hasNext()) {
            it.next().setVisibility(8);
        }
    }

    public Editor getEditor(int i) {
        return this.mEditors.get(Integer.valueOf(i));
    }
}
