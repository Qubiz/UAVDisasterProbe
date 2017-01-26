package com.uav.utwente.uavdisasterprobe;
import com.uav.utwente.uavdisasterprobe.UAVDisasterProbeApplication;

import dji.sdk.products.DJIAircraft;
import dji.sdk.products.DJIHandHeld;

/**
 * Created by dji on 16/1/6.
 */
public class DJIModuleVerificationUtil {
    public static boolean isProductModuleAvailable() {
        return (null != UAVDisasterProbeApplication.getProductInstance());
    }

    public static boolean isAircraft() {
        return UAVDisasterProbeApplication.getProductInstance() instanceof DJIAircraft;
    }

    public static boolean isHandHeld() {
        return UAVDisasterProbeApplication.getProductInstance() instanceof DJIHandHeld;
    }
    public static boolean isCameraModuleAvailable() {
        return isProductModuleAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getCamera());
    }

    public static boolean isPlaybackAvailable() {
        return isCameraModuleAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getCamera().getPlayback());
    }

    public static boolean isMediaManagerAvailable() {
        return isCameraModuleAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getCamera().getMediaManager());
    }

    public static boolean isRemoteControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() &&
                (null != UAVDisasterProbeApplication.getAircraftInstance().getRemoteController());
    }

    public static boolean isFlightControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() &&
                (null != UAVDisasterProbeApplication.getAircraftInstance().getFlightController());
    }

    public static boolean isCompassAvailable() {
        return isFlightControllerAvailable() && isAircraft() &&
                (null != UAVDisasterProbeApplication.getAircraftInstance().getFlightController().getCompass());
    }

    public static boolean isFlightLimitationAvailable() {
        return isFlightControllerAvailable() && isAircraft() &&
                (null != UAVDisasterProbeApplication.getAircraftInstance().
                        getFlightController().getFlightLimitation());
    }

    public static boolean isGimbalModuleAvailable() {
        return isProductModuleAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getGimbal());
    }

    public static boolean isAirlinkAvailable() {
        return isProductModuleAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getAirLink());
    }

    public static boolean isWiFiAirlinkAvailable() {
        return isAirlinkAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getAirLink().getWiFiLink());
    }

    public static boolean isLBAirlinkAvailable() {
        return isAirlinkAvailable() &&
                (null != UAVDisasterProbeApplication.getProductInstance().getAirLink().getLBAirLink());
    }

}
