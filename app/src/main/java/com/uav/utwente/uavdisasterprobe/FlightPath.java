package com.uav.utwente.uavdisasterprobe;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypoint;
import dji.sdk.missionmanager.DJIWaypointMission;

public class FlightPath {

    private ArrayList<Marker> markers;
    private Polyline path;

    private ArrayList<DJIWaypoint> waypointsList;

    private DJIWaypointMission waypointMission;

    private File waypointFile;

    public FlightPath(File waypointFile) throws IOException {
        this.waypointFile = waypointFile;
        waypointsList = new ArrayList<>();
        waypointMission = new DJIWaypointMission();
        waypointMission.finishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
        waypointMission.headingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
        readFromFile(waypointFile);
    }

    private void readFromFile(File waypointFile) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(waypointFile.getPath()), ';');

        double latitude;
        double longitude;
        float altitude;

        int count = 0;

        List<String[]> entries = reader.readAll();

        if(entries.get(0)[0].substring(1).equals("latitude") && entries.get(0)[1].equals("longitude") && entries.get(0)[2].equals("altitude")) {
            Log.d("Header", "The header of the file is correct!");
            for(String[] entry : entries) {
                if(count != 0) {
                    if(entry.length == 3) {
                        try {
                            latitude = Double.parseDouble(entry[0]);
                            longitude = Double.parseDouble(entry[1]);
                            altitude = Float.parseFloat(entry[2]);

                            DJIWaypoint waypoint = new DJIWaypoint(latitude, longitude, altitude);
                            waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, -90));
                            waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartTakePhoto, 0));

                            waypointsList.add(waypoint);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("Header", "Error at entry '" + count + "': all entries should have three parameters (latitude, longitude and altitude)!");
                    }
                }
                count++;
            }
        } else {
            Log.d("Header", "The header of the file is NOT correct!");
        }
    }

    public void showOnMap(GoogleMap googleMap) {
        createWaypoints(waypointsList, googleMap);
        createPolylinePath(waypointsList, googleMap);

        zoomTo(googleMap);
    }

    private LatLngBounds getBounds(Polyline path) {
        double minLatitude = path.getPoints().get(0).latitude;
        double maxLatitude = path.getPoints().get(0).latitude;
        double minLongitude = path.getPoints().get(0).longitude;
        double maxLongitude = path.getPoints().get(0).longitude;

        for(LatLng point : path.getPoints()) {
            if(point.latitude < minLatitude) minLatitude = point.latitude;
            if(point.latitude > maxLatitude) maxLatitude = point.latitude;
            if(point.longitude < minLongitude) minLongitude = point.longitude;
            if(point.longitude > maxLongitude) maxLongitude = point.longitude;
        }

        return new LatLngBounds(new LatLng(minLatitude, minLongitude), new LatLng(maxLatitude, maxLongitude));
    }

    private void createWaypoints(List<DJIWaypoint> waypoints, GoogleMap googleMap) {
        if(markers == null) {
            markers = new ArrayList<>();
        } else {
            for(Marker marker : markers) {
                marker.remove();
            }
        }

        for(int i = 0; i < waypoints.size(); i++) {
            LatLng point = new LatLng(waypoints.get(i).latitude, waypoints.get(i).longitude);
            MarkerOptions markerOptions = new MarkerOptions().position(point);

            if(i == 0) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                markerOptions.title("(START) Lat: " + waypoints.get(i).latitude
                        + " | Lng: " + waypoints.get(i).longitude
                        + " | Alt: " + waypoints.get(i).altitude );
            } else if(i == waypoints.size() - 1) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                markerOptions.title("(END) Lat: " + waypoints.get(i).latitude
                        + " | Lng: " + waypoints.get(i).longitude
                        + " | Alt: " + waypoints.get(i).altitude );
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                markerOptions.title("Lat: " + waypoints.get(i).latitude
                        + " | Lng: " + waypoints.get(i).longitude
                        + " | Alt: " + waypoints.get(i).altitude );
            }

            markers.add(googleMap.addMarker(markerOptions));
        }
    }

    private void createPolylinePath(List<DJIWaypoint> waypoints, GoogleMap googleMap) {
        if(path != null) {
            path.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions();

        for(int i = 0; i < waypoints.size() - 1; i++) {
            LatLng latLngBegin = new LatLng(waypoints.get(i).latitude, waypoints.get(i).longitude);
            LatLng latLngEnd = new LatLng(waypoints.get(i + 1).latitude, waypoints.get(i + 1).longitude);

            polylineOptions.add(latLngBegin, latLngEnd);
            polylineOptions.width(5);
            polylineOptions.color(Color.RED);
        }

        path = googleMap.addPolyline(polylineOptions);
    }

    public void removeFromMap() {
        if(markers == null) {
            markers = new ArrayList<>();
        } else {
            for(Marker marker : markers) {
                marker.remove();
            }
        }

        if(path != null) {
            path.remove();
        }
    }

    public void zoomTo(GoogleMap googleMap) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(path), 150));
    }

    public void startMission(DJIMissionManager missionManager) {
        if(missionManager != null) {
            missionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Log.d("startMission", "onResult: " + (error == null ? "Succes!" : error.getDescription()));
                }
            });
        }
    }

    public void stopMission(DJIMissionManager missionManager) {
        if(missionManager != null) {
            missionManager.stopMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Log.d("stopMission", "onResult: " + (error == null ? "Succes!" : error.getDescription()));
                }
            });
            waypointMission.removeAllWaypoints();
        }
    }

    public void prepareMission(DJIMissionManager missionManager) {
        if(missionManager != null && waypointMission != null) {
            waypointMission.removeAllWaypoints();
            waypointMission.addWaypoints(waypointsList);
            final DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                   Log.d("prepareMission", "onProgress (" + type.name() + "): " + progress);
                }
            };

            missionManager.prepareMission(waypointMission, progressHandler, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    MainActivity.setStartFlightButtonEnabled((error == null));
                    Log.d("prepareMission", "onResult: " + (error == null ? "Success!" : error.getDescription()));
                }
            });
        }
    }
}
