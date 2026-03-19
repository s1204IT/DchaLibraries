package android.app;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

public class ExpandableListActivity extends Activity implements View.OnCreateContextMenuListener, ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupCollapseListener, ExpandableListView.OnGroupExpandListener {
    ExpandableListAdapter mAdapter;
    boolean mFinishedStart = false;
    ExpandableListView mList;

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long j) {
        return false;
    }

    @Override
    public void onGroupCollapse(int i) {
    }

    @Override
    public void onGroupExpand(int i) {
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        ensureList();
        super.onRestoreInstanceState(bundle);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View viewFindViewById = findViewById(16908292);
        this.mList = (ExpandableListView) findViewById(16908298);
        if (this.mList == null) {
            throw new RuntimeException("Your content must have a ExpandableListView whose id attribute is 'android.R.id.list'");
        }
        if (viewFindViewById != null) {
            this.mList.setEmptyView(viewFindViewById);
        }
        this.mList.setOnChildClickListener(this);
        this.mList.setOnGroupExpandListener(this);
        this.mList.setOnGroupCollapseListener(this);
        if (this.mFinishedStart) {
            setListAdapter(this.mAdapter);
        }
        this.mFinishedStart = true;
    }

    public void setListAdapter(ExpandableListAdapter expandableListAdapter) {
        synchronized (this) {
            ensureList();
            this.mAdapter = expandableListAdapter;
            this.mList.setAdapter(expandableListAdapter);
        }
    }

    public ExpandableListView getExpandableListView() {
        ensureList();
        return this.mList;
    }

    public ExpandableListAdapter getExpandableListAdapter() {
        return this.mAdapter;
    }

    private void ensureList() {
        if (this.mList != null) {
            return;
        }
        setContentView(17367041);
    }

    public long getSelectedId() {
        return this.mList.getSelectedId();
    }

    public long getSelectedPosition() {
        return this.mList.getSelectedPosition();
    }

    public boolean setSelectedChild(int i, int i2, boolean z) {
        return this.mList.setSelectedChild(i, i2, z);
    }

    public void setSelectedGroup(int i) {
        this.mList.setSelectedGroup(i);
    }
}
