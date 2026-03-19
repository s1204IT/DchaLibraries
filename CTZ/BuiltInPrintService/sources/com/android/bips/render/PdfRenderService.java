package com.android.bips.render;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import com.android.bips.jni.SizeD;
import com.android.bips.render.IPdfRender;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class PdfRenderService extends Service {
    private static final String TAG = PdfRenderService.class.getSimpleName();
    private PdfRenderer.Page mPage;
    private PdfRenderer mRenderer;
    private final Object mPageOpenLock = new Object();
    private final IPdfRender.Stub mBinder = new IPdfRender.Stub() {
        @Override
        public int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
            if (open(parcelFileDescriptor)) {
                return PdfRenderService.this.mRenderer.getPageCount();
            }
            return 0;
        }

        @Override
        public SizeD getPageSize(int i) throws RemoteException {
            if (!openPage(i)) {
                return null;
            }
            return new SizeD(PdfRenderService.this.mPage.getWidth(), PdfRenderService.this.mPage.getHeight());
        }

        @Override
        public ParcelFileDescriptor renderPageStripe(int i, int i2, int i3, int i4, double d) throws RemoteException {
            if (!openPage(i)) {
                return null;
            }
            try {
                ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                PdfRenderService.this.new RenderThread(PdfRenderService.this.mPage, i2, i3, i4, d, parcelFileDescriptorArrCreatePipe[1]).start();
                return parcelFileDescriptorArrCreatePipe[0];
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public void closeDocument() throws RemoteException {
            PdfRenderService.this.closeAll();
        }

        private boolean open(ParcelFileDescriptor parcelFileDescriptor) {
            PdfRenderService.this.closeAll();
            try {
                PdfRenderService.this.mRenderer = new PdfRenderer(parcelFileDescriptor);
                return true;
            } catch (IOException e) {
                Log.w(PdfRenderService.TAG, "Could not open file descriptor for rendering", e);
                return false;
            }
        }

        private boolean openPage(int i) {
            if (PdfRenderService.this.mRenderer != null) {
                if (PdfRenderService.this.mPage != null && PdfRenderService.this.mPage.getIndex() != i) {
                    PdfRenderService.this.closePage();
                }
                if (PdfRenderService.this.mPage == null) {
                    PdfRenderService.this.mPage = PdfRenderService.this.mRenderer.openPage(i);
                    return true;
                }
                return true;
            }
            return false;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        closeAll();
        return super.onUnbind(intent);
    }

    private void closePage() {
        if (this.mPage != null) {
            synchronized (this.mPageOpenLock) {
                this.mPage.close();
            }
            this.mPage = null;
        }
    }

    private void closeAll() {
        closePage();
        if (this.mRenderer != null) {
            this.mRenderer.close();
            this.mRenderer = null;
        }
    }

    private class RenderThread extends Thread {
        private final ByteBuffer mBuffer;
        private final int mHeight;
        private final ParcelFileDescriptor mOutput;
        private final PdfRenderer.Page mPage;
        private final int mRowsPerStripe;
        private final int mWidth;
        private final int mYOffset;
        private final double mZoomFactor;

        RenderThread(PdfRenderer.Page page, int i, int i2, int i3, double d, ParcelFileDescriptor parcelFileDescriptor) {
            this.mPage = page;
            this.mWidth = i2;
            this.mYOffset = i;
            this.mHeight = i3;
            this.mZoomFactor = d;
            this.mOutput = parcelFileDescriptor;
            this.mRowsPerStripe = (5242880 / this.mWidth) / 4;
            this.mBuffer = ByteBuffer.allocate(this.mWidth * this.mRowsPerStripe * 4);
        }

        @Override
        public void run() {
            ?? r3;
            ?? r32;
            ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
            synchronized (PdfRenderService.this.mPageOpenLock) {
                ?? r1 = 0;
                r1 = 0;
                try {
                    try {
                        try {
                            try {
                                try {
                                    autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(this.mOutput);
                                } catch (Throwable th) {
                                    th = th;
                                }
                                try {
                                } catch (Throwable th2) {
                                    th = th2;
                                    if (0 != 0) {
                                        try {
                                            autoCloseOutputStream.close();
                                        } catch (Throwable th3) {
                                            r1.addSuppressed(th3);
                                        }
                                    } else {
                                        autoCloseOutputStream.close();
                                    }
                                    throw th;
                                }
                            } catch (IOException e) {
                                e = e;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            if (r1 != 0) {
                                r1.recycle();
                            }
                            throw th;
                        }
                    } catch (IOException e2) {
                        e = e2;
                        r1 = r32;
                    } catch (Throwable th5) {
                        th = th5;
                        r1 = r3;
                        if (r1 != 0) {
                        }
                        throw th;
                    }
                    if (this.mPage == null) {
                        Log.e(PdfRenderService.TAG, "Page lost");
                        autoCloseOutputStream.close();
                        return;
                    }
                    Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mWidth, this.mRowsPerStripe, Bitmap.Config.ARGB_8888);
                    try {
                        int i = this.mYOffset;
                        while (i < this.mYOffset + this.mHeight) {
                            int iMin = Math.min(this.mRowsPerStripe, (this.mYOffset + this.mHeight) - i);
                            renderToBitmap(i, bitmapCreateBitmap);
                            writeRgb(bitmapCreateBitmap, iMin, autoCloseOutputStream);
                            i += this.mRowsPerStripe;
                        }
                        autoCloseOutputStream.close();
                        if (bitmapCreateBitmap != null) {
                            bitmapCreateBitmap.recycle();
                        }
                    } catch (Throwable th6) {
                        throw th6;
                    }
                    Log.e(PdfRenderService.TAG, "Failed to write", e);
                    if (r1 != 0) {
                        r1.recycle();
                    }
                } finally {
                }
            }
        }

        private void renderToBitmap(int i, Bitmap bitmap) {
            Matrix matrix = new Matrix();
            matrix.setScale((float) this.mZoomFactor, (float) this.mZoomFactor);
            matrix.postTranslate(0.0f, 0 - i);
            bitmap.eraseColor(-1);
            this.mPage.render(bitmap, null, matrix, 2);
        }

        private void writeRgb(Bitmap bitmap, int i, OutputStream outputStream) throws IOException {
            this.mBuffer.clear();
            bitmap.copyPixelsToBuffer(this.mBuffer);
            int i2 = this.mWidth * i * 4;
            byte[] bArrArray = this.mBuffer.array();
            int i3 = 0;
            int i4 = 0;
            while (i3 < i2) {
                bArrArray[i4] = bArrArray[i3];
                bArrArray[i4 + 1] = bArrArray[i3 + 1];
                bArrArray[i4 + 2] = bArrArray[i3 + 2];
                i3 += 4;
                i4 += 3;
            }
            outputStream.write(this.mBuffer.array(), 0, i4);
        }
    }
}
