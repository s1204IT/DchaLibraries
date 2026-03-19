package com.android.server.am;

import android.R;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

final class CarUserSwitchingDialog extends UserSwitchingDialog {
    private static final String TAG = "ActivityManagerCarUserSwitchingDialog";

    public CarUserSwitchingDialog(ActivityManagerService activityManagerService, Context context, UserInfo userInfo, UserInfo userInfo2, boolean z, String str, String str2) {
        super(activityManagerService, context, userInfo, userInfo2, z, str, str2);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
    }

    @Override
    void inflateContent() {
        setCancelable(false);
        Resources resources = getContext().getResources();
        View viewInflate = LayoutInflater.from(getContext()).inflate(R.layout.app_permission_item, (ViewGroup) null);
        Bitmap userIcon = ((UserManager) getContext().getSystemService("user")).getUserIcon(this.mNewUser.id);
        if (userIcon != null) {
            ((ImageView) viewInflate.findViewById(R.id.remoteMessaging)).setImageDrawable(CircleFramedDrawable.getInstance(userIcon, resources.getDimension(R.dimen.action_button_min_width_overflow_material)));
        }
        ((TextView) viewInflate.findViewById(R.id.rectangle)).setText(resources.getString(R.string.accessibility_label_communal_profile));
        setView(viewInflate);
    }

    static class CircleFramedDrawable extends Drawable {
        private final Bitmap mBitmap;
        private RectF mDstRect;
        private final Paint mPaint;
        private float mScale;
        private final int mSize;
        private Rect mSrcRect;

        public static CircleFramedDrawable getInstance(Bitmap bitmap, float f) {
            return new CircleFramedDrawable(bitmap, (int) f);
        }

        public CircleFramedDrawable(Bitmap bitmap, int i) {
            this.mSize = i;
            this.mBitmap = Bitmap.createBitmap(this.mSize, this.mSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(this.mBitmap);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int iMin = Math.min(width, height);
            Rect rect = new Rect((width - iMin) / 2, (height - iMin) / 2, iMin, iMin);
            RectF rectF = new RectF(0.0f, 0.0f, this.mSize, this.mSize);
            Path path = new Path();
            path.addArc(rectF, 0.0f, 360.0f);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            this.mPaint = new Paint();
            this.mPaint.setAntiAlias(true);
            this.mPaint.setColor(-16777216);
            this.mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, this.mPaint);
            this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rectF, this.mPaint);
            this.mPaint.setXfermode(null);
            this.mScale = 1.0f;
            this.mSrcRect = new Rect(0, 0, this.mSize, this.mSize);
            this.mDstRect = new RectF(0.0f, 0.0f, this.mSize, this.mSize);
        }

        @Override
        public void draw(Canvas canvas) {
            float f = (this.mSize - (this.mScale * this.mSize)) / 2.0f;
            this.mDstRect.set(f, f, this.mSize - f, this.mSize - f);
            canvas.drawBitmap(this.mBitmap, this.mSrcRect, this.mDstRect, (Paint) null);
        }

        @Override
        public int getOpacity() {
            return -3;
        }

        @Override
        public void setAlpha(int i) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }
    }
}
