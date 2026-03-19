package com.android.gallery3d.app;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.GLRootView;

public class PickerActivity extends AbstractGalleryActivity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        boolean z = getResources().getBoolean(R.bool.picker_is_dialog);
        if (!z) {
            requestWindowFeature(8);
            requestWindowFeature(9);
        }
        setContentView(R.layout.dialog_picker);
        if (z) {
            View viewFindViewById = findViewById(R.id.cancel);
            viewFindViewById.setOnClickListener(this);
            viewFindViewById.setVisibility(0);
            ((GLRootView) findViewById(R.id.gl_root_view)).setZOrderOnTop(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pickup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_cancel) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cancel) {
            finish();
        }
    }
}
