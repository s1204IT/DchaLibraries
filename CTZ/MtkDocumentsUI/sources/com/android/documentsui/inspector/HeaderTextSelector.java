package com.android.documentsui.inspector;

import android.text.Spannable;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.android.internal.util.Preconditions;

public final class HeaderTextSelector implements ActionMode.Callback {
    private final Selector mSelector;
    private final TextView mText;

    public interface Selector {
        void select(Spannable spannable, int i, int i2);
    }

    public HeaderTextSelector(TextView textView, Selector selector) {
        Preconditions.checkArgument(textView != null);
        Preconditions.checkArgument(selector != null);
        this.mText = textView;
        this.mSelector = selector;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        CharSequence text = this.mText.getText();
        if (text instanceof Spannable) {
            this.mSelector.select((Spannable) text, 0, getLengthOfFilename(text));
            return true;
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
    }

    private static int getLengthOfFilename(CharSequence charSequence) {
        int iIndexOf;
        String string = charSequence.toString();
        if (string != null && (iIndexOf = string.indexOf(46)) > 0) {
            return iIndexOf;
        }
        return charSequence.length();
    }
}
