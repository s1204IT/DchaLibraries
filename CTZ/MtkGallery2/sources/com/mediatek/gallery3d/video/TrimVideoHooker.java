package com.mediatek.gallery3d.video;

import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.R;
import com.android.gallery3d.app.TrimVideo;
import com.mediatek.gallery3d.ext.DefaultActivityHooker;
import com.mediatek.gallery3d.util.Log;

public class TrimVideoHooker extends MovieHooker {
    private static final String CONTENT_MEDIA = "content://media";
    private static final int MENU_TRIM_VIDEO = 1;
    private static final String TAG = "VP_TrimVideoHooker";
    private MenuItem mMenutTrim;
    private IMovieItem mMovieItem;

    @Override
    public void setParameter(String str, Object obj) {
        super.setParameter(str, obj);
        Log.d(TAG, "setParameter(" + str + ", " + obj + ")");
        if (obj instanceof IMovieItem) {
            this.mMovieItem = (IMovieItem) obj;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.mMenutTrim = menu.add(DefaultActivityHooker.MENU_HOOKER_GROUP_ID, getMenuActivityId(1), 0, R.string.trim_action);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (MovieUtils.isLocalFile(this.mMovieItem.getUri(), this.mMovieItem.getMimeType()) && isUriSupportTrim(this.mMovieItem.getUri()) && this.mMovieItem.canBeRetrieved() && !this.mMovieItem.isDrm()) {
            this.mMenutTrim.setVisible(true);
        } else {
            this.mMenutTrim.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);
        if (getMenuOriginalId(menuItem.getItemId()) == 1) {
            Uri uri = this.mMovieItem.getUri();
            Log.d(TAG, "original=" + uri);
            String videoPath = this.mMovieItem.getVideoPath();
            Log.d(TAG, "path=" + videoPath);
            Intent intent = new Intent(getContext(), (Class<?>) TrimVideo.class);
            intent.setData(uri);
            intent.putExtra("media-item-path", videoPath);
            getContext().startActivity(intent);
            getContext().finish();
            return true;
        }
        return false;
    }

    private boolean isUriSupportTrim(Uri uri) {
        return String.valueOf(uri).toLowerCase().startsWith(CONTENT_MEDIA) || String.valueOf(uri).toLowerCase().startsWith("file://");
    }
}
