package com.uav.utwente.uavdisasterprobe;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;

public class MediaDownload {

    private DJIBaseProduct product;
    private DJICamera camera;
    private DJIMediaManager mediaManager;

    private ArrayList<DJIMedia> mediaList;

    private volatile boolean downloading = false;

    public MediaDownload() {

    }

    public File fetchLatestPhoto() {
        File photo = null;
        if(isMediaManagerAvailable()) {
            camera.setCameraMode(DJICameraSettingsDef.CameraMode.MediaDownload,
                    new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    fetchMediaList();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(downloading) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            DJIMedia media = null;

                            for(int i = 0; i < mediaList.size(); i++) {
                                if(mediaList.get(i).getFileName().endsWith(".jpg")) {
                                    if(media == null) {
                                        media = mediaList.get(i);
                                    } else {
                                        if(mediaList.get(i).mTimeCreated > media.mTimeCreated) {
                                            media = mediaList.get(i);
                                        }
                                    }
                                }
                            }
                            File destination = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/UAVDisasterProbe/");
                            fetchMedia(media, destination);
                        }
                    }).start();
                }
            });
        }
        return photo;
    }

    private void fetchMedia(final DJIMedia media, File destination) {
        downloading = true;
        if(media != null) {
            media.fetchMediaData(destination, null, new DJIMediaManager.CameraDownloadListener<String>() {
                @Override
                public void onStart() {
                    Log.d("MediaDownload", "fetchMedia() | Started fetching media file: " + media.getFileName());
                    downloading = true;
                }

                @Override
                public void onRateUpdate(long total, long current, long persize) {
                    Log.d("MediaDownload", "fetchMedia() | Download rate: " + persize);
                }

                @Override
                public void onProgress(long total, long current) {
                    Log.d("MediaDownload", "fetchMedia() | Progress: " + current + "/" + total);
                }

                @Override
                public void onSuccess(String string) {
                    Log.d("MediaDownload", "fetchMedia() | The media file (" + media.getFileName() + ") has been downloaded to: " + string);
                    downloading = false;
                }

                @Override
                public void onFailure(DJIError error) {
                    Log.d("MediaDownload", "fetchMedia() | Fetching the media file (" + media.getFileName() + ") has failed: " + error.getDescription());
                    downloading = false;
                }
            });
        } else {
            downloading = false;
            Log.d("MediaDownload", "fetchMedia() | Media file is null...");
        }
    }

    public void fetchMediaList() {
        downloading = true;
        if(isMediaManagerAvailable()) {
            mediaManager.fetchMediaList(new DJIMediaManager.CameraDownloadListener<ArrayList<DJIMedia>>() {
                @Override
                public void onStart() {
                    Log.d("MediaDownload", "fetchMediaList() | Started fetching media list...");
                }

                @Override
                public void onRateUpdate(long total, long current, long persize) {
                    Log.d("MediaDownload", "fetchMediaList() | Fetching media list in progress...");
                }

                @Override
                public void onProgress(long total, long current) {
                    Log.d("MediaDownload", "fetchMediaList() | Progress: " + current + "/" + total);
                }

                @Override
                public void onSuccess(ArrayList<DJIMedia> medias) {
                    if(medias != null && !medias.isEmpty()) {
                        mediaList = medias;
                        Log.d("MediaDownload", "fetchMediaList() | Total number of media files: " + mediaList.size());
                        printMediaList();
                    } else {
                        Log.d("MediaDownload", "fetchMediaList() | No media files in SD card...");
                    }
                    downloading = false;
                }

                @Override
                public void onFailure(DJIError error) {
                    downloading = false;
                    Log.d("MediaDownload", "Fetching the media list failed: " + error.getDescription());
                }
            });
        }
    }

    private boolean isMediaManagerAvailable() {
        product = UAVDisasterProbeApplication.getProductInstance();
        camera = product.getCamera();
        mediaManager = camera.getMediaManager();

        return (product != null && camera != null && mediaManager != null);
    }

    public void printMediaList() {
        if(mediaList != null) {
            if(!mediaList.isEmpty()) {
                for(int i = 0; i < mediaList.size(); i++) {
                    Log.d("MediaDownload", "Media " + (i + 1) + ": " + mediaList.get(i).getFileName() + " (" + mediaList.get(i).fileSize + ")");
                }
            } else {
                Log.d("MediaDownload", "Media list is empty...");
            }
        } else {
            Log.d("MediaDownload", "Media list is null...");
        }
    }

    public ArrayList<DJIMedia> getMediaList() {
        return mediaList;
    }

}
