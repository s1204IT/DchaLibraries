package com.mediatek.galleryfeature.pq;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryfeature.pq.PresentImage;
import com.mediatek.galleryfeature.pq.adapter.PQDataAdapter;
import com.mediatek.plugin.component.PluginBaseActivity;

public class PictureQualityActivity extends PluginBaseActivity implements PresentImage.RenderingRequestListener {
    public static final String ACTION_PQ = "android.media.action.PQ";
    public static final int ITEM_HEIGHT = 85;
    private static final String TAG = "MtkGallery2/PictureQualityActivity";
    private static final int TOAST_DISPLAY_DELAY = 2000;
    public static float sDensity = 1.0f;
    public static int sTargetWidth;
    private PQDataAdapter mAdapter;
    private int mHeight;
    private ImageView mImageView;
    private ListView mListView;
    private String mUri = null;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(8);
        requestWindowFeature(9);
        Bundle extras = this.mThis.getIntent().getExtras();
        setContentView(R.layout.m_pq_main);
        if (extras != null) {
            this.mUri = extras.getString("PQUri");
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mThis.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.mHeight = displayMetrics.heightPixels;
        TiledBitmapDecoder.sViewWidth = displayMetrics.widthPixels;
        TiledBitmapDecoder.sViewHeight = displayMetrics.heightPixels;
        Log.d(TAG, "<onCreate> PictureQualityActivity display Width = " + displayMetrics.widthPixels + " Height = " + displayMetrics.heightPixels);
        this.mImageView = (ImageView) findViewById(R.id.m_imageview);
        this.mListView = (ListView) findViewById(R.id.m_getInfo);
        try {
            this.mAdapter = new PQDataAdapter(this, this.mThis, this.mUri);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "<onCreate>PictureQualityActivity onCreate issue!");
            Toast.makeText(this.mThis, "NoClassDefFoundError Please Check!!", TOAST_DISPLAY_DELAY).show();
            e.printStackTrace();
            finish();
        } catch (UnsatisfiedLinkError e2) {
            Log.d(TAG, "<onCreate> PictureQualityActivity onCreate issue!");
            Toast.makeText(this.mThis, "UnsatisfiedLinkError Please Check!!", TOAST_DISPLAY_DELAY).show();
            e2.printStackTrace();
            finish();
        }
        sDensity = displayMetrics.density;
        sTargetWidth = Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels) / 2;
        Log.d(TAG, "<onCreate>sDensity=" + sDensity + " sTargetWidth = " + sTargetWidth);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.m_pq_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.cancel) {
            recoverIndex();
        }
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        recoverIndex();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        if (this.mAdapter != null) {
            this.mAdapter.setListView(this.mListView);
            this.mAdapter.onResume();
        }
        setListViewHeightBasedOnChildren(this.mListView);
        PresentImage.getPresentImage().setListener(this.mThis, this);
        PresentImage.getPresentImage().loadBitmap(this.mUri);
    }

    @Override
    public void onPause() {
        super.onPause();
        PresentImage.getPresentImage().stopLoadBitmap();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mAdapter != null) {
            this.mAdapter.onDestroy();
        }
        releseResource();
    }

    public int getDefaultItemHeight() {
        return (int) (85.0f * sDensity);
    }

    public int getActionBarHeight() {
        int height = this.mThis.getActionBar().getHeight();
        if (height != 0) {
            return height;
        }
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
        }
        return height;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mThis.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.mHeight = displayMetrics.heightPixels;
        Log.d(TAG, "<onConfigurationChanged>onConfigurationChanged  height=" + this.mHeight);
        if (this.mListView != null) {
            setListViewHeightBasedOnChildren(this.mListView);
        }
        toggleStatusBarByOrientation();
    }

    @Override
    public boolean available(Bitmap bitmap, String str) {
        if (this.mUri != null && this.mUri == str) {
            this.mImageView.setImageBitmap(bitmap);
            return true;
        }
        bitmap.recycle();
        return false;
    }

    private void recoverIndex() {
        if (this.mAdapter != null) {
            this.mAdapter.restoreIndex();
        }
    }

    private void releseResource() {
        this.mImageView.setImageBitmap(null);
        PresentImage.getPresentImage().free();
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }
        for (int i = 0; i < adapter.getCount(); i++) {
            adapter.getView(i, null, null).measure(0, 0);
        }
        int i2 = this.mHeight;
        int actionBarHeight = getActionBarHeight();
        int actionBarHeight2 = this.mHeight - (2 * getActionBarHeight());
        ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        marginLayoutParams.setMargins(0, actionBarHeight, 0, 0);
        ((ViewGroup.LayoutParams) marginLayoutParams).height = actionBarHeight2;
        listView.setLayoutParams(layoutParams);
    }

    private void toggleStatusBarByOrientation() {
        Window window = this.mThis.getWindow();
        if (window == null) {
            return;
        }
        window.clearFlags(1024);
    }
}
