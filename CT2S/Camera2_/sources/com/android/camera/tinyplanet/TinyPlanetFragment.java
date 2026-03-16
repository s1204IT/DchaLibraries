package com.android.camera.tinyplanet;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.android.camera.CameraActivity;
import com.android.camera.app.CameraApp;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.tinyplanet.TinyPlanetPreview;
import com.android.camera.util.XmpUtil;
import com.android.camera2.R;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TinyPlanetFragment extends DialogFragment implements TinyPlanetPreview.PreviewSizeListener {
    public static final String ARGUMENT_TITLE = "title";
    public static final String ARGUMENT_URI = "uri";
    public static final String CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS = "FullPanoHeightPixels";
    public static final String CROPPED_AREA_FULL_PANO_WIDTH_PIXELS = "FullPanoWidthPixels";
    public static final String CROPPED_AREA_IMAGE_HEIGHT_PIXELS = "CroppedAreaImageHeightPixels";
    public static final String CROPPED_AREA_IMAGE_WIDTH_PIXELS = "CroppedAreaImageWidthPixels";
    public static final String CROPPED_AREA_LEFT = "CroppedAreaLeftPixels";
    public static final String CROPPED_AREA_TOP = "CroppedAreaTopPixels";
    private static final String FILENAME_PREFIX = "TINYPLANET_";
    public static final String GOOGLE_PANO_NAMESPACE = "http://ns.google.com/photos/1.0/panorama/";
    private static final int RENDER_DELAY_MILLIS = 50;
    private static final Log.Tag TAG = new Log.Tag("TinyPlanetActivity");
    private ProgressDialog mDialog;
    private TinyPlanetPreview mPreview;
    private Bitmap mResultBitmap;
    private Bitmap mSourceBitmap;
    private Uri mSourceImageUri;
    private int mPreviewSizePx = 0;
    private float mCurrentZoom = 0.5f;
    private float mCurrentAngle = 0.0f;
    private final Lock mResultLock = new ReentrantLock();
    private String mOriginalTitle = "";
    private final Handler mHandler = new Handler();
    private Boolean mRendering = false;
    private Boolean mRenderOneMore = false;
    private final Runnable mCreateTinyPlanetRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (TinyPlanetFragment.this.mRendering) {
                if (TinyPlanetFragment.this.mRendering.booleanValue()) {
                    TinyPlanetFragment.this.mRenderOneMore = true;
                } else {
                    TinyPlanetFragment.this.mRendering = true;
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            TinyPlanetFragment.this.mResultLock.lock();
                            try {
                                if (TinyPlanetFragment.this.mSourceBitmap != null && TinyPlanetFragment.this.mResultBitmap != null) {
                                    int width = TinyPlanetFragment.this.mSourceBitmap.getWidth();
                                    int height = TinyPlanetFragment.this.mSourceBitmap.getHeight();
                                    TinyPlanetNative.process(TinyPlanetFragment.this.mSourceBitmap, width, height, TinyPlanetFragment.this.mResultBitmap, TinyPlanetFragment.this.mPreviewSizePx, TinyPlanetFragment.this.mCurrentZoom, TinyPlanetFragment.this.mCurrentAngle);
                                }
                                return null;
                            } finally {
                                TinyPlanetFragment.this.mResultLock.unlock();
                            }
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            TinyPlanetFragment.this.mPreview.setBitmap(TinyPlanetFragment.this.mResultBitmap, TinyPlanetFragment.this.mResultLock);
                            synchronized (TinyPlanetFragment.this.mRendering) {
                                TinyPlanetFragment.this.mRendering = false;
                                if (TinyPlanetFragment.this.mRenderOneMore.booleanValue()) {
                                    TinyPlanetFragment.this.mRenderOneMore = false;
                                    TinyPlanetFragment.this.scheduleUpdate();
                                }
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                }
            }
        }
    };

    private static final class TinyPlanetImage {
        public final byte[] mJpegData;
        public final int mSize;

        public TinyPlanetImage(byte[] jpegData, int size) {
            this.mJpegData = jpegData;
            this.mSize = size;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(0, R.style.Theme_Camera);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(1);
        getDialog().setCanceledOnTouchOutside(true);
        View view = inflater.inflate(R.layout.tinyplanet_editor, container, false);
        this.mPreview = (TinyPlanetPreview) view.findViewById(R.id.preview);
        this.mPreview.setPreviewSizeChangeListener(this);
        SeekBar zoomSlider = (SeekBar) view.findViewById(R.id.zoomSlider);
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TinyPlanetFragment.this.onZoomChange(progress);
            }
        });
        SeekBar angleSlider = (SeekBar) view.findViewById(R.id.angleSlider);
        angleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TinyPlanetFragment.this.onAngleChange(progress);
            }
        });
        Button createButton = (Button) view.findViewById(R.id.creatTinyPlanetButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TinyPlanetFragment.this.onCreateTinyPlanet();
            }
        });
        this.mOriginalTitle = getArguments().getString(ARGUMENT_TITLE);
        this.mSourceImageUri = Uri.parse(getArguments().getString(ARGUMENT_URI));
        this.mSourceBitmap = createPaddedSourceImage(this.mSourceImageUri, true);
        if (this.mSourceBitmap == null) {
            Log.e(TAG, "Could not decode source image.");
            dismiss();
        }
        return view;
    }

    private Bitmap createPaddedSourceImage(Uri sourceImageUri, boolean previewSize) {
        InputStream is = getInputStream(sourceImageUri);
        if (is == null) {
            Log.e(TAG, "Could not create input stream for image.");
            dismiss();
        }
        Bitmap sourceBitmap = BitmapFactory.decodeStream(is);
        XMPMeta xmp = XmpUtil.extractXMPMeta(getInputStream(sourceImageUri));
        if (xmp != null) {
            int size = previewSize ? getDisplaySize() : sourceBitmap.getWidth();
            return createPaddedBitmap(sourceBitmap, xmp, size);
        }
        return sourceBitmap;
    }

    private void onCreateTinyPlanet() {
        synchronized (this.mRendering) {
            this.mRenderOneMore = false;
        }
        final String savingTinyPlanet = getActivity().getResources().getString(R.string.saving_tiny_planet);
        new AsyncTask<Void, Void, TinyPlanetImage>() {
            @Override
            protected void onPreExecute() {
                TinyPlanetFragment.this.mDialog = ProgressDialog.show(TinyPlanetFragment.this.getActivity(), null, savingTinyPlanet, true, false);
            }

            @Override
            protected TinyPlanetImage doInBackground(Void... params) {
                return TinyPlanetFragment.this.createFinalTinyPlanet();
            }

            @Override
            protected void onPostExecute(TinyPlanetImage image) {
                final CameraActivity activity = (CameraActivity) TinyPlanetFragment.this.getActivity();
                MediaSaver mediaSaver = ((CameraApp) activity.getApplication()).getMediaSaver();
                MediaSaver.OnMediaSavedListener doneListener = new MediaSaver.OnMediaSavedListener() {
                    @Override
                    public void onMediaSaved(Uri uri) {
                        activity.notifyNewMedia(uri);
                        TinyPlanetFragment.this.mDialog.dismiss();
                        TinyPlanetFragment.this.dismiss();
                    }
                };
                String tinyPlanetTitle = TinyPlanetFragment.FILENAME_PREFIX + TinyPlanetFragment.this.mOriginalTitle;
                mediaSaver.addImage(image.mJpegData, tinyPlanetTitle, new Date().getTime(), null, image.mSize, image.mSize, 0, null, doneListener, TinyPlanetFragment.this.getActivity().getContentResolver());
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private TinyPlanetImage createFinalTinyPlanet() {
        this.mResultLock.lock();
        try {
            this.mResultBitmap.recycle();
            this.mResultBitmap = null;
            this.mSourceBitmap.recycle();
            this.mSourceBitmap = null;
            this.mResultLock.unlock();
            Bitmap sourceBitmap = createPaddedSourceImage(this.mSourceImageUri, false);
            int width = sourceBitmap.getWidth();
            int height = sourceBitmap.getHeight();
            int outputSize = width / 2;
            Bitmap resultBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
            TinyPlanetNative.process(sourceBitmap, width, height, resultBitmap, outputSize, this.mCurrentZoom, this.mCurrentAngle);
            sourceBitmap.recycle();
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, jpeg);
            return new TinyPlanetImage(addExif(jpeg.toByteArray()), outputSize);
        } catch (Throwable th) {
            this.mResultLock.unlock();
            throw th;
        }
    }

    private byte[] addExif(byte[] jpeg) {
        ExifInterface exif = new ExifInterface();
        exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, System.currentTimeMillis(), TimeZone.getDefault());
        ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
        try {
            exif.writeExif(jpeg, jpegOut);
        } catch (IOException e) {
            Log.e(TAG, "Could not write EXIF", e);
        }
        return jpegOut.toByteArray();
    }

    private int getDisplaySize() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return Math.min(size.x, size.y);
    }

    @Override
    public void onSizeChanged(int sizePx) {
        this.mPreviewSizePx = sizePx;
        this.mResultLock.lock();
        try {
            if (this.mResultBitmap == null || this.mResultBitmap.getWidth() != sizePx || this.mResultBitmap.getHeight() != sizePx) {
                if (this.mResultBitmap != null) {
                    this.mResultBitmap.recycle();
                }
                this.mResultBitmap = Bitmap.createBitmap(this.mPreviewSizePx, this.mPreviewSizePx, Bitmap.Config.ARGB_8888);
            }
            this.mResultLock.unlock();
            scheduleUpdate();
        } catch (Throwable th) {
            this.mResultLock.unlock();
            throw th;
        }
    }

    private void onZoomChange(int zoom) {
        this.mCurrentZoom = zoom / 1000.0f;
        scheduleUpdate();
    }

    private void onAngleChange(int angle) {
        this.mCurrentAngle = (float) Math.toRadians(angle);
        scheduleUpdate();
    }

    private void scheduleUpdate() {
        this.mHandler.removeCallbacks(this.mCreateTinyPlanetRunnable);
        this.mHandler.postDelayed(this.mCreateTinyPlanetRunnable, 50L);
    }

    private InputStream getInputStream(Uri uri) {
        try {
            return getActivity().getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not load source image.", e);
            return null;
        }
    }

    private static Bitmap createPaddedBitmap(Bitmap bitmapIn, XMPMeta xmp, int intermediateWidth) {
        try {
            int croppedAreaWidth = getInt(xmp, CROPPED_AREA_IMAGE_WIDTH_PIXELS);
            int croppedAreaHeight = getInt(xmp, CROPPED_AREA_IMAGE_HEIGHT_PIXELS);
            int fullPanoWidth = getInt(xmp, CROPPED_AREA_FULL_PANO_WIDTH_PIXELS);
            int fullPanoHeight = getInt(xmp, CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS);
            int left = getInt(xmp, CROPPED_AREA_LEFT);
            int top = getInt(xmp, CROPPED_AREA_TOP);
            if (fullPanoWidth != 0 && fullPanoHeight != 0) {
                Bitmap paddedBitmap = null;
                float scale = intermediateWidth / fullPanoWidth;
                while (paddedBitmap == null) {
                    try {
                        paddedBitmap = Bitmap.createBitmap((int) (fullPanoWidth * scale), (int) (fullPanoHeight * scale), Bitmap.Config.ARGB_8888);
                    } catch (OutOfMemoryError e) {
                        System.gc();
                        scale /= 2.0f;
                    }
                }
                Canvas paddedCanvas = new Canvas(paddedBitmap);
                int right = left + croppedAreaWidth;
                int bottom = top + croppedAreaHeight;
                RectF destRect = new RectF(left * scale, top * scale, right * scale, bottom * scale);
                paddedCanvas.drawBitmap(bitmapIn, (Rect) null, destRect, (Paint) null);
                return paddedBitmap;
            }
            return bitmapIn;
        } catch (XMPException e2) {
            return bitmapIn;
        }
    }

    private static int getInt(XMPMeta xmp, String key) throws XMPException {
        if (xmp.doesPropertyExist(GOOGLE_PANO_NAMESPACE, key)) {
            return xmp.getPropertyInteger(GOOGLE_PANO_NAMESPACE, key).intValue();
        }
        return 0;
    }
}
