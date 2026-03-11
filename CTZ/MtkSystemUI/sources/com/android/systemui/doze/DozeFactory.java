package com.android.systemui.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;

public class DozeFactory {
    public DozeMachine assembleMachine(DozeService dozeService) {
        SensorManager sensorManager = (SensorManager) Dependency.get(AsyncSensorManager.class);
        AlarmManager alarmManager = (AlarmManager) dozeService.getSystemService(AlarmManager.class);
        DozeHost host = getHost(dozeService);
        AmbientDisplayConfiguration ambientDisplayConfiguration = new AmbientDisplayConfiguration(dozeService);
        DozeParameters dozeParameters = DozeParameters.getInstance(dozeService);
        Handler handler = new Handler();
        DelayedWakeLock delayedWakeLock = new DelayedWakeLock(handler, WakeLock.createPartial(dozeService, "Doze"));
        DozeMachine.Service serviceWrapIfNeeded = DozeSuspendScreenStatePreventingAdapter.wrapIfNeeded(DozeScreenStatePreventingAdapter.wrapIfNeeded(new DozeBrightnessHostForwarder(dozeService, host), dozeParameters), dozeParameters);
        DozeMachine dozeMachine = new DozeMachine(serviceWrapIfNeeded, ambientDisplayConfiguration, delayedWakeLock);
        dozeMachine.setParts(new DozeMachine.Part[]{new DozePauser(handler, dozeMachine, alarmManager, dozeParameters.getPolicy()), new DozeFalsingManagerAdapter(FalsingManager.getInstance(dozeService)), createDozeTriggers(dozeService, sensorManager, host, alarmManager, ambientDisplayConfiguration, dozeParameters, handler, delayedWakeLock, dozeMachine), createDozeUi(dozeService, host, delayedWakeLock, dozeMachine, handler, alarmManager, dozeParameters), new DozeScreenState(serviceWrapIfNeeded, handler, dozeParameters, delayedWakeLock), createDozeScreenBrightness(dozeService, serviceWrapIfNeeded, sensorManager, host, dozeParameters, handler), new DozeWallpaperState(dozeService)});
        return dozeMachine;
    }

    private DozeMachine.Part createDozeScreenBrightness(Context context, DozeMachine.Service service, SensorManager sensorManager, DozeHost dozeHost, DozeParameters dozeParameters, Handler handler) {
        return new DozeScreenBrightness(context, service, sensorManager, DozeSensors.findSensorWithType(sensorManager, context.getString(R.string.doze_brightness_sensor_type)), dozeHost, handler, dozeParameters.getPolicy());
    }

    private DozeTriggers createDozeTriggers(Context context, SensorManager sensorManager, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration ambientDisplayConfiguration, DozeParameters dozeParameters, Handler handler, WakeLock wakeLock, DozeMachine dozeMachine) {
        return new DozeTriggers(context, dozeMachine, dozeHost, alarmManager, ambientDisplayConfiguration, dozeParameters, sensorManager, handler, wakeLock, true);
    }

    private DozeMachine.Part createDozeUi(Context context, DozeHost dozeHost, WakeLock wakeLock, DozeMachine dozeMachine, Handler handler, AlarmManager alarmManager, DozeParameters dozeParameters) {
        return new DozeUi(context, alarmManager, dozeMachine, wakeLock, dozeHost, handler, dozeParameters, KeyguardUpdateMonitor.getInstance(context));
    }

    public static DozeHost getHost(DozeService dozeService) {
        return (DozeHost) ((SystemUIApplication) dozeService.getApplication()).getComponent(DozeHost.class);
    }
}
