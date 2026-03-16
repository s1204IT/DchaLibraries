package com.android.gallery3d.app;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.GLRootView;

public class PickerActivity extends AbstractGalleryActivity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isDialog = getResources().getBoolean(R.bool.picker_is_dialog);
        if (!isDialog) {
            requestWindowFeature(8);
            requestWindowFeature(9);
        }
        setContentView(R.layout.dialog_picker);
        if (isDialog) {
            View view = findViewById(R.id.cancel);
            view.setOnClickListener(this);
            view.setVisibility(0);
            ((GLRootView) findViewById(R.id.gl_root_view)).setZOrderOnTop(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pickup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.action_cancel) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cancel) {
            finish();
        }
    }
}
