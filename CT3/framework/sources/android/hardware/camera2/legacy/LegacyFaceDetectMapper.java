package android.hardware.camera2.legacy;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.ParameterUtils;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.utils.ParamsUtils;
import android.util.Log;
import android.util.Size;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class LegacyFaceDetectMapper {
    private static final boolean DEBUG = false;
    private static String TAG = "LegacyFaceDetectMapper";
    private final Camera mCamera;
    private final boolean mFaceDetectSupported;
    private Camera.Face[] mFaces;
    private Camera.Face[] mFacesPrev;
    private boolean mFaceDetectEnabled = false;
    private boolean mFaceDetectScenePriority = false;
    private boolean mFaceDetectReporting = false;
    private final Object mLock = new Object();

    public LegacyFaceDetectMapper(Camera camera, CameraCharacteristics characteristics) {
        this.mCamera = (Camera) Preconditions.checkNotNull(camera, "camera must not be null");
        Preconditions.checkNotNull(characteristics, "characteristics must not be null");
        this.mFaceDetectSupported = ArrayUtils.contains((int[]) characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES), 1);
        if (!this.mFaceDetectSupported) {
            return;
        }
        this.mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera2) {
                int lengthFaces = faces == null ? 0 : faces.length;
                synchronized (LegacyFaceDetectMapper.this.mLock) {
                    if (LegacyFaceDetectMapper.this.mFaceDetectEnabled) {
                        LegacyFaceDetectMapper.this.mFaces = faces;
                    } else if (lengthFaces > 0) {
                        Log.d(LegacyFaceDetectMapper.TAG, "onFaceDetection - Ignored some incoming faces sinceface detection was disabled");
                    }
                }
            }
        });
    }

    public void processFaceDetectMode(CaptureRequest captureRequest, Camera.Parameters parameters) {
        Preconditions.checkNotNull(captureRequest, "captureRequest must not be null");
        int fdMode = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.STATISTICS_FACE_DETECT_MODE, 0)).intValue();
        if (fdMode != 0 && !this.mFaceDetectSupported) {
            Log.w(TAG, "processFaceDetectMode - Ignoring statistics.faceDetectMode; face detection is not available");
            return;
        }
        int sceneMode = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_SCENE_MODE, 0)).intValue();
        if (sceneMode == 1 && !this.mFaceDetectSupported) {
            Log.w(TAG, "processFaceDetectMode - ignoring control.sceneMode == FACE_PRIORITY; face detection is not available");
            return;
        }
        switch (fdMode) {
            case 0:
            case 1:
                break;
            case 2:
                Log.w(TAG, "processFaceDetectMode - statistics.faceDetectMode == FULL unsupported, downgrading to SIMPLE");
                break;
            default:
                Log.w(TAG, "processFaceDetectMode - ignoring unknown statistics.faceDetectMode = " + fdMode);
                return;
        }
        boolean enableFaceDetect = fdMode != 0 || sceneMode == 1;
        synchronized (this.mLock) {
            if (enableFaceDetect != this.mFaceDetectEnabled) {
                if (enableFaceDetect) {
                    this.mCamera.startFaceDetection();
                } else {
                    this.mCamera.stopFaceDetection();
                    this.mFaces = null;
                }
                this.mFaceDetectEnabled = enableFaceDetect;
                this.mFaceDetectScenePriority = sceneMode == 1;
                this.mFaceDetectReporting = fdMode != 0;
            }
        }
    }

    public void mapResultFaces(CameraMetadataNative result, LegacyRequest legacyRequest) {
        int fdMode;
        Camera.Face[] faceArr;
        boolean fdScenePriority;
        Preconditions.checkNotNull(result, "result must not be null");
        Preconditions.checkNotNull(legacyRequest, "legacyRequest must not be null");
        synchronized (this.mLock) {
            fdMode = this.mFaceDetectReporting ? 1 : 0;
            if (this.mFaceDetectReporting) {
                faceArr = this.mFaces;
            } else {
                faceArr = null;
            }
            fdScenePriority = this.mFaceDetectScenePriority;
            Camera.Face[] faceArr2 = this.mFacesPrev;
            this.mFacesPrev = faceArr;
        }
        CameraCharacteristics characteristics = legacyRequest.characteristics;
        CaptureRequest request = legacyRequest.captureRequest;
        Size previewSize = legacyRequest.previewSize;
        Camera.Parameters params = legacyRequest.parameters;
        Rect activeArray = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        ParameterUtils.ZoomData zoomData = ParameterUtils.convertScalerCropRegion(activeArray, (Rect) request.get(CaptureRequest.SCALER_CROP_REGION), previewSize, params);
        List<Face> convertedFaces = new ArrayList<>();
        if (faceArr != null) {
            for (Camera.Face face : faceArr) {
                if (face != null) {
                    convertedFaces.add(ParameterUtils.convertFaceFromLegacy(face, activeArray, zoomData));
                } else {
                    Log.w(TAG, "mapResultFaces - read NULL face from camera1 device");
                }
            }
        }
        result.set(CaptureResult.STATISTICS_FACES, (Face[]) convertedFaces.toArray(new Face[0]));
        result.set(CaptureResult.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(fdMode));
        if (!fdScenePriority) {
            return;
        }
        result.set((CaptureResult.Key<int>) CaptureResult.CONTROL_SCENE_MODE, 1);
    }
}
