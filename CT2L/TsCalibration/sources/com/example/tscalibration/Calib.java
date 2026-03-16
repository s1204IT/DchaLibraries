package com.example.tscalibration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import com.panasonic.sanyo.ce.bej.hard.Touchpanel;

class Calib extends View {
    public int Index;
    private int[] Pos;
    final String TAG;
    public int act;
    private int[] cx;
    private int[] cy;
    public int dispX;
    public int dispY;
    int dofs;
    private int[] dx;
    private int[] dy;
    final int ofs;
    private int[] tx;
    private int[] ty;
    private int upchk;
    public int x;
    public int y;

    public Calib(Context c) {
        super(c);
        this.Index = 0;
        this.dispX = 1280;
        this.dispY = 800;
        this.dx = new int[5];
        this.dy = new int[5];
        this.tx = new int[5];
        this.ty = new int[5];
        this.cx = new int[5];
        this.cy = new int[5];
        this.Pos = new int[]{1, 2, 3, 4, 5};
        this.upchk = 0;
        this.TAG = "TsCalibration";
        this.ofs = 5;
        this.dofs = 0;
        setFocusable(true);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.v("TsCalibration", "w:" + w + " h:" + h + " oldw:" + oldw + " oldh:" + oldh);
    }

    public void Init() {
        this.dx[0] = this.dispX / 20;
        this.dy[0] = this.dispY / 20;
        this.dx[1] = this.dispX / 20;
        this.dy[1] = this.dispY - (this.dispY / 20);
        this.dx[2] = this.dispX - (this.dispX / 20);
        this.dy[2] = this.dispY / 20;
        this.dx[3] = this.dispX - (this.dispX / 20);
        this.dy[3] = this.dispY - (this.dispY / 20);
        this.dx[4] = this.dispX / 2;
        this.dy[4] = this.dispY / 2;
        this.x = 0;
        this.y = 0;
        for (int i = 0; i < 5; i++) {
            this.cx[i] = 65535;
            this.cy[i] = 65535;
            this.tx[i] = 0;
            this.ty[i] = 0;
        }
        this.Index = 0;
        this.upchk = 0;
    }

    private void Check_dx() {
        this.x = 65535;
        this.y = 65535;
    }

    @Override
    @SuppressLint({"DrawAllocation"})
    public void onDraw(Canvas canvas) {
        canvas.drawColor(-1);
        switch (this.act) {
            case 0:
            case 2:
                this.upchk = 1;
                if (this.Index < 5) {
                    this.tx[this.Index] = this.x;
                    this.ty[this.Index] = this.y;
                } else if (this.Index > 5 && this.Index < 90) {
                    this.cx[this.Index - 6] = this.x;
                    this.cy[this.Index - 6] = this.y;
                }
                break;
            case 1:
                if (this.Index < 5) {
                    this.Index++;
                    this.upchk = 0;
                } else if (this.Index > 5 && this.Index < 90 && this.Index > 5 && this.Index < 90) {
                    this.upchk = 2;
                }
                break;
        }
        if (this.Index < 5) {
            Paint ppaint = new Paint();
            ppaint.setColor(-16777216);
            ppaint.setStrokeWidth(3.0f);
            ppaint.setStyle(Paint.Style.FILL);
            canvas.drawLine(this.dx[this.Index] - 8, this.dy[this.Index] - this.dofs, this.dx[this.Index] + 8, this.dy[this.Index] - this.dofs, ppaint);
            canvas.drawLine(this.dx[this.Index], (this.dy[this.Index] - 8) - this.dofs, this.dx[this.Index], (this.dy[this.Index] + 8) - this.dofs, ppaint);
        } else if (this.Index == 5) {
            int ret = Touchpanel.calibration(this.dx, this.dy, this.tx, this.ty, this.Pos);
            Log.v("TsCalibration", "calibration ret:" + ret);
            if (ret >= 0) {
                Touchpanel.coefficient_set(1);
                Check_dx();
                this.Index++;
                Log.v("TsCalibration", " Calibration Check:Index:" + this.Index);
            } else {
                this.Index = 98;
                Log.v("TsCalibration", " Calibration Check NG:" + this.Index);
            }
        }
        if (this.Index > 5 && this.Index < 90) {
            Paint ppaint2 = new Paint();
            ppaint2.setColor(-16777216);
            ppaint2.setStrokeWidth(3.0f);
            ppaint2.setStyle(Paint.Style.FILL);
            if (this.x != 65535) {
                canvas.drawLine(this.x - 8, this.y - this.dofs, this.x + 8, this.y - this.dofs, ppaint2);
                canvas.drawLine(this.x, (this.y - 8) - this.dofs, this.x, (this.y + 8) - this.dofs, ppaint2);
            }
            if (this.upchk == 2) {
                if (this.x >= this.dx[this.Index - 6] - 8 && this.x <= this.dx[this.Index - 6] + 8 && this.y >= this.dy[this.Index - 6] - 8 && this.y <= this.dy[this.Index - 6] + 8) {
                    if (this.Index < 10) {
                        this.Index++;
                    } else {
                        Log.v("TsCalibration", "Calibration Success End!!");
                        this.Index = 99;
                    }
                }
                this.upchk = 0;
            }
            ppaint2.setStyle(Paint.Style.STROKE);
            int max_index = this.Index != 99 ? this.Index - 5 : 5;
            for (int i = 0; i < max_index; i++) {
                ppaint2.setColor(-16777216);
                canvas.drawRect(this.dx[i] - 8, (this.dy[i] - 8) - this.dofs, this.dx[i] + 8, (this.dy[i] + 8) - this.dofs, ppaint2);
                if (this.cx[i] != 65535) {
                    ppaint2.setColor(-65536);
                    canvas.drawLine(this.cx[i] - 8, this.cy[i] - this.dofs, this.cx[i] + 8, this.cy[i] - this.dofs, ppaint2);
                    canvas.drawLine(this.cx[i], (this.cy[i] - 8) - this.dofs, this.cx[i], (this.cy[i] + 8) - this.dofs, ppaint2);
                }
            }
        }
        if (this.Index == 98) {
            Paint paint = new Paint();
            paint.setTextSize(36.0f);
            paint.setColor(-16777216);
            canvas.drawText("タッチパネルの補正に失敗しました!!", 100.0f, 200.0f, paint);
            paint.setTextSize(36.0f);
            paint.setColor(-16777216);
            canvas.drawText("パネルのタッチで再補正を行います", 100.0f, 300.0f, paint);
            return;
        }
        if (this.Index == 97) {
            Paint paint2 = new Paint();
            paint2.setTextSize(36.0f);
            paint2.setColor(-16777216);
            canvas.drawText("タッチパネルAPIでエラーが発生したため", 100.0f, 200.0f, paint2);
            paint2.setTextSize(36.0f);
            paint2.setColor(-16777216);
            canvas.drawText("キャリブレーション出来ません", 100.0f, 300.0f, paint2);
        }
    }
}
