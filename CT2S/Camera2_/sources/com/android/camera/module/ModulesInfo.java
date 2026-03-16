package com.android.camera.module;

import android.content.Context;
import com.android.camera.CaptureModule;
import com.android.camera.PhotoModule;
import com.android.camera.VideoModule;
import com.android.camera.app.AppController;
import com.android.camera.app.ModuleManager;
import com.android.camera.debug.DebugPropertyHelper;
import com.android.camera.debug.Log;
import com.android.camera.util.GcamHelper;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.util.RefocusHelper;
import com.android.camera2.R;

public class ModulesInfo {
    private static final Log.Tag TAG = new Log.Tag("ModulesInfo");
    private static final boolean ENABLE_CAPTURE_MODULE = DebugPropertyHelper.isCaptureModuleEnabled();

    public static void setupModules(Context context, ModuleManager moduleManager) {
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId);
        moduleManager.setDefaultModuleIndex(photoModuleId);
        registerVideoModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_video));
        if (PhotoSphereHelper.hasLightCycleCapture(context)) {
            registerWideAngleModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_panorama));
            registerPhotoSphereModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_photosphere));
        }
        if (RefocusHelper.hasRefocusCapture(context)) {
            registerRefocusModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_refocus));
        }
        if (GcamHelper.hasGcamAsSeparateModule()) {
            registerGcamModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_gcam));
        }
    }

    private static void registerPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return !ModulesInfo.ENABLE_CAPTURE_MODULE;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return ModulesInfo.ENABLE_CAPTURE_MODULE ? new CaptureModule(app) : new PhotoModule(app);
            }
        });
    }

    private static void registerVideoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return new VideoModule(app);
            }
        });
    }

    private static void registerWideAngleModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return PhotoSphereHelper.createWideAnglePanoramaModule(app);
            }
        });
    }

    private static void registerPhotoSphereModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return PhotoSphereHelper.createPanoramaModule(app);
            }
        });
    }

    private static void registerRefocusModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return RefocusHelper.createRefocusModule(app);
            }
        });
    }

    private static void registerGcamModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return false;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return GcamHelper.createGcamModule(app);
            }
        });
    }
}
