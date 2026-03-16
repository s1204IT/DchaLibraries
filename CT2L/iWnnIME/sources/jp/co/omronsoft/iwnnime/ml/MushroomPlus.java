package jp.co.omronsoft.iwnnime.ml;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MushroomPlus extends ListActivity {
    private static final String ACTION_RECEIVE = "jp.co.omronsoft.iwnnime.WnnConnector.RECEIVE";
    public static final String GET_STRING_TYPE = "get_string_type";
    private static final String KEY_SEND = "modifiedtext";
    private static final String KEY_WORD = "text";
    private static final int MSG_START_MUSHROOM = 1;
    private static final int MSG_START_WNNCONNECTOR = 100;
    public static final String MUSHROOM_ACTION = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
    public static final String MUSHROOM_CATEGORY = "com.adamrocker.android.simeji.REPLACE";
    public static final String MUSHROOM_REPLACE_KEY = "replace_key";
    private CharSequence mCharSequence;
    private boolean mType;
    private int mWnnConnectorCnt = 0;
    final ArrayList<CharSequence> mClassNameArray = new ArrayList<>();
    final ArrayList<CharSequence> mPackageNameArray = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        CharSequence[] items;
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mCharSequence = intent.getCharSequenceExtra(MUSHROOM_REPLACE_KEY);
        this.mType = intent.getBooleanExtra(GET_STRING_TYPE, false);
        ArrayList<CharSequence> labelArray = new ArrayList<>();
        ArrayList<Drawable> entriesImage = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent connectorIntent = new Intent(ACTION_RECEIVE);
        List<ResolveInfo> resolveInfo = pm.queryIntentActivities(connectorIntent, 0);
        Collections.sort(resolveInfo, new ResolveInfo.DisplayNameComparator(pm));
        Intent mushintent = new Intent(MUSHROOM_ACTION);
        mushintent.addCategory(MUSHROOM_CATEGORY);
        List<ResolveInfo> mushresolveInfo = pm.queryIntentActivities(mushintent, 0);
        Collections.sort(mushresolveInfo, new ResolveInfo.DisplayNameComparator(pm));
        this.mWnnConnectorCnt = resolveInfo.size();
        resolveInfo.addAll(mushresolveInfo);
        for (int i = 0; i < resolveInfo.size(); i++) {
            ResolveInfo info = resolveInfo.get(i);
            ActivityInfo actInfo = info.activityInfo;
            CharSequence label = actInfo.loadLabel(pm);
            String classname = actInfo.name;
            String packagename = actInfo.packageName;
            if (i >= this.mWnnConnectorCnt) {
                int j = 0;
                while (j < this.mWnnConnectorCnt && (!this.mClassNameArray.get(j).equals(classname) || !this.mPackageNameArray.get(j).equals(packagename))) {
                    j++;
                }
                if (j >= this.mWnnConnectorCnt) {
                    labelArray.add(label);
                    entriesImage.add(createIconThumbnail(info.loadIcon(pm)));
                    this.mClassNameArray.add(classname);
                    this.mPackageNameArray.add(packagename);
                }
            }
        }
        int id = R.layout.mushroom_layout;
        if (labelArray.size() > 0) {
            items = new CharSequence[labelArray.size()];
            for (int i2 = 0; i2 < labelArray.size(); i2++) {
                items[i2] = labelArray.get(i2);
            }
        } else {
            items = new CharSequence[]{getResources().getString(R.string.ti_mushroom_launcher_activity_list_empty_txt)};
            id = R.layout.mushroom_non_layout;
        }
        WnnArrayAdapter<CharSequence> adapter = new WnnArrayAdapter<>(getApplicationContext(), 0, items);
        adapter.setEntriesImage(entriesImage);
        adapter.setLayoutId(id);
        setListAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == -1) {
            MushroomControl.getInstance().setResultString(data.getStringExtra(MUSHROOM_REPLACE_KEY));
            MushroomControl.getInstance().setResultType(false);
        } else if (requestCode == 100 && resultCode == -1) {
            MushroomControl.getInstance().setResultString(data.getCharSequenceExtra(KEY_SEND));
            MushroomControl.getInstance().setResultType(true);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        MushroomControl.getInstance().setResultString(LoggingEvents.EXTRA_CALLING_APP_NAME);
        super.onBackPressed();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (this.mPackageNameArray.size() < 1) {
            MushroomControl.getInstance().setResultString(LoggingEvents.EXTRA_CALLING_APP_NAME);
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(this.mPackageNameArray.get(position).toString(), this.mClassNameArray.get(position).toString());
        if (this.mWnnConnectorCnt < position + 1) {
            intent.setAction(MUSHROOM_ACTION);
            intent.addCategory(MUSHROOM_CATEGORY);
            if (this.mType) {
                intent.putExtra(MUSHROOM_REPLACE_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
            } else {
                intent.putExtra(MUSHROOM_REPLACE_KEY, this.mCharSequence.toString());
            }
            startActivityForResult(intent, 1);
            return;
        }
        intent.setAction(ACTION_RECEIVE);
        intent.putExtra(KEY_WORD, this.mCharSequence);
        startActivityForResult(intent, 100);
    }

    public Drawable createIconThumbnail(Drawable icon) {
        int mIconWidth = (int) getResources().getDimension(R.dimen.app_icon_size);
        int mIconHeight = (int) getResources().getDimension(R.dimen.app_icon_size);
        Canvas mCanvas = new Canvas();
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
        Rect mOldBounds = new Rect();
        int width = mIconWidth;
        int height = mIconHeight;
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        }
        if (width > 0 && height > 0) {
            if (width < iconWidth || height < iconHeight) {
                float ratio = iconWidth / iconHeight;
                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }
                Bitmap.Config c = icon.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                mCanvas.setBitmap(thumb);
                mOldBounds.set(icon.getBounds());
                int x = (mIconWidth - width) / 2;
                int y = (mIconHeight - height) / 2;
                icon.setBounds(x, y, x + width, y + height);
                icon.draw(mCanvas);
                icon.setBounds(mOldBounds);
                Drawable icon2 = new BitmapDrawable(getResources(), thumb);
                mCanvas.setBitmap(null);
                return icon2;
            }
            if (iconWidth < width && iconHeight < height) {
                Bitmap.Config c2 = Bitmap.Config.ARGB_8888;
                Bitmap thumb2 = Bitmap.createBitmap(mIconWidth, mIconHeight, c2);
                mCanvas.setBitmap(thumb2);
                mOldBounds.set(icon.getBounds());
                int x2 = (width - iconWidth) / 2;
                int y2 = (height - iconHeight) / 2;
                icon.setBounds(x2, y2, x2 + iconWidth, y2 + iconHeight);
                icon.draw(mCanvas);
                icon.setBounds(mOldBounds);
                Drawable icon3 = new BitmapDrawable(getResources(), thumb2);
                mCanvas.setBitmap(null);
                return icon3;
            }
            return icon;
        }
        return icon;
    }
}
