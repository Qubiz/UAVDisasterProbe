package com.uav.utwente.uavdisasterprobe;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.products.DJIAircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class UAVDisasterProbeApplication extends Application {

    private static final String TAG = UAVDisasterProbeApplication.class.getName();

    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private static DJIBaseProduct product;

    private Handler handler;

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof DJIAircraft;
    }

    public static synchronized DJIAircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (DJIAircraft) getProductInstance();
    }

    public static synchronized DJIBaseProduct getProductInstance() {
        if (null == product) {
            product = DJISDKManager.getInstance().getDJIProduct();
        }
        return product;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);

    }

    protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {

        @Override
        public void onGetRegisteredResult(DJIError error) {

            Handler handler = new Handler(Looper.getMainLooper());

            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG, "Register success");

            } else {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                    }
                });

                Log.d(TAG, "Register failed");

            }
            Log.e(TAG, error == null ? "success" : error.getDescription());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            Log.v(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
            product = newProduct;
            if(product != null) {
                product.setDJIBaseProductListener(mDJIBaseProductListener);
            }

            notifyStatusChange();
        }
    };

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {

        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            Log.v(TAG, String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s", key, oldComponent, newComponent));

            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            Log.v(TAG, "onProductConnectivityChanged: " + isConnected);
            notifyStatusChange();
        }
    };

    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {

        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }

    };

    private void notifyStatusChange() {
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
}
