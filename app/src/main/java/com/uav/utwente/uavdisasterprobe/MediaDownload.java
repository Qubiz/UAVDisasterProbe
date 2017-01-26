package com.uav.utwente.uavdisasterprobe;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import dji.common.airlink.LBAirLinkDataRate;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.common.camera.DJICameraSettingsDef;

import dji.sdk.camera.DJIMediaManager;
import dji.sdk.camera.DJIMedia;

import java.io.File;
import java.util.ArrayList;


public class MediaDownload {
    private ArrayList<DJIMedia> mList;
    private DJICameraSettingsDef.CameraMode getCameraMode = null;
    private boolean setCameraMode = false;
    private int attemptToDownload = 9;

    public MediaDownload(Context context) {
        this.context = context;
    }

    protected void startMediaDownload() {
        Log.d("startMediaDownload", "start");
        if (DJIModuleVerificationUtil.isCameraModuleAvailable()) {
            if (DJIModuleVerificationUtil.isMediaManagerAvailable()) {
                UAVDisasterProbeApplication.getProductInstance().getAirLink().getLBAirLink().setDataRate(LBAirLinkDataRate.Bandwidth4Mbps, new DJICommonCallbacks.DJICompletionCallback() {
                    public void onResult(DJIError djiError) {
                        Log.d("setDataRate", djiError == null ? "success" : djiError.getDescription());
                    }
                });
                fetchMediaList();
            } else {
                Log.d("startMediaDownload", "not support mediadownload");
            }
        }
    }


    private void fetchMediaList() {
        Log.d("fetchMediaList", "start");
        if (DJIModuleVerificationUtil.isMediaManagerAvailable()) {
            if((getCameraMode()==DJICameraSettingsDef.CameraMode.MediaDownload)||setCameraMode()) {
                UAVDisasterProbeApplication.getProductInstance().getCamera().getMediaManager().fetchMediaList(
                        new DJIMediaManager.CameraDownloadListener<ArrayList<DJIMedia>>() {
                            String str;

                            @Override
                            public void onStart() {
                                Log.d("fetchListonStart", "start fetch media list");
                            }

                            @Override
                            public void onRateUpdate(long total, long current, long persize) {
                                Log.d("fetchListonRateUpdate", "in porgress");
                            }

                            @Override
                            public void onProgress(long l, long l1) {
                                Log.d("fetchListonProgress", "working on it");

                            }

                            @Override
                            public void onSuccess(ArrayList<DJIMedia> djiMedias) {
                                mList = djiMedias;
                                Log.d("fetchListonSuccess", "Success" + djiMedias);
                                if (null != djiMedias) {
                                    if (!djiMedias.isEmpty()) {
                                        Log.d("FetchMediaList", "onSuccess: " + "Total Media files:" + djiMedias.size());
                                        for (int i = 0; i < djiMedias.size(); i++) {
                                            Log.d("FetchMediaList", "onSuccess: " + "Media " + i + ": " + djiMedias.get(i).getFileName());
                                        }
                                        try{
                                            Thread.sleep(10000);
                                        }catch (InterruptedException e) {
                                            Log.d("sleep",e.getLocalizedMessage());
                                        }
                                        fetchMedia(mList);
                                    }
                                } else {
                                    Log.d("fetchMedialist", "No Media in SD Card");
                                }
                            }

                            @Override
                            public void onFailure(DJIError djiError) {
                                Log.d("fetchListonFailure", "fetch media list failure: with error" + djiError.getDescription());
                            }
                        }
                );
            }
        }
    }

    protected void fetchMedia(ArrayList<DJIMedia> djiMedias) {
        mList = djiMedias;
        if((getCameraMode()==DJICameraSettingsDef.CameraMode.MediaDownload)||setCameraMode()){
            File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/UAVDisasterProbe/");
            Log.d("fetchMediaData", Environment.getExternalStorageDirectory().getPath() + "/UAVDisasterProbe/");
            mList.get(mList.size() - 1).fetchMediaData(destDir, "test2", new DJIMediaManager.CameraDownloadListener<String>() {
                @Override
                public void onStart() {
                    Log.d("fetchMediaData", "onStart");
                }

                @Override
                public void onRateUpdate(long l, long l1, long l2) {
                    Log.d("fetchMediaData", "onRateUpdate");
                }

                @Override
                public void onProgress(long total, long current) {
                    Log.d("fetchMediaData", "onProgress: " + current + ", " + total+", " +(current/total));
                }

                @Override
                public void onSuccess(String s) {

                    Log.d("fetchMediaData", "Success its path is: " + s);
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Log.d("fetchMediaDataFailure", djiError.getDescription());
//                    if(attemptToDownload>0) {
//                        fetchMedia(mList);
//                        attemptToDownload--;
//                    }
                }
            });
        }
    }
    private DJICameraSettingsDef.CameraMode getCameraMode(){
        UAVDisasterProbeApplication.getProductInstance().getCamera().getCameraMode(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraMode>() {
            @Override
            public void onSuccess(DJICameraSettingsDef.CameraMode cameraMode) {
                getCameraMode = cameraMode;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.d("getCameraMode", djiError.getDescription());
            }
        });
        return getCameraMode;
    }
    private boolean setCameraMode(){
        setCameraMode = false;
        UAVDisasterProbeApplication.getProductInstance().getCamera().setCameraMode(DJICameraSettingsDef.CameraMode.MediaDownload,
                new DJICommonCallbacks.DJICompletionCallback(){


                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError==null){
                            setCameraMode = true;
                        }else{
                            Log.d("setCameraMode", djiError.getDescription());
                        }
                    }
                });
        Log.d("startMediaDownload", "setCameraMode: "+setCameraMode);
        return setCameraMode;
    }
}