package com.uav.utwente.uavdisasterprobe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    private MainActivity mainActivity;

    public FlightPath(MainActivity mainActivity, File waypointFile) throws IOException {
        this.mainActivity = mainActivity;
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
        int pitch;
        int yaw;

        int count = 0;

        List<String[]> entries = reader.readAll();

        if(entries.get(0)[0].substring(1).equals("latitude")
                && entries.get(0)[1].equals("longitude")
                && entries.get(0)[2].equals("altitude")
                && entries.get(0)[3].equals("pitch")
                && entries.get(0)[4].equals("yaw")) {
            Log.d("Header", "The header of the file is correct!");
            for(String[] entry : entries) {
                if(count != 0) {
                    if(entry.length == 5) {
                        try {
                            latitude = Double.parseDouble(entry[0]);
                            longitude = Double.parseDouble(entry[1]);
                            altitude = Float.parseFloat(entry[2]);
                            pitch = Integer.parseInt(entry[3]);
                            yaw = Integer.parseInt(entry[4]);

                            if(checkCoordinates(latitude, longitude)) {
                                if(checkPitchYaw(pitch, yaw)) {
                                    DJIWaypoint waypoint = new DJIWaypoint(latitude, longitude, altitude);
                                    waypoint.turnMode = DJIWaypoint.DJIWaypointTurnMode.Clockwise;
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, pitch));
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.RotateAircraft, yaw));
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartTakePhoto, 0));
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, 0));
                                    waypointsList.add(waypoint);
                                    Log.d("FlightPath", "Created waypoint: " + waypoint.latitude + " | " + waypoint.longitude);

                                } else {
                                    Log.d("checkPitchYaw", "false");
                                }
                            } else {
                                Log.d("checkCoordinates", "false");
                            }
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
            } else if(i == waypoints.size() - 1) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }

            markerOptions.title("Waypoint " + (i + 1) + "/" + waypoints.size());
            markerOptions.snippet("Latitude: " + waypoints.get(i).latitude
                    + "\n" + "Longitude: " + waypoints.get(i).longitude
                    + "\n" + "Altitude: " + waypoints.get(i).altitude
                    + "\n" + "Pitch: " + waypoints.get(i).getActionAtIndex(0).mActionParam
                    + "\n" + "Yaw: " + waypoints.get(i).getActionAtIndex(1).mActionParam);

            Marker marker = googleMap.addMarker(markerOptions);
            marker.setTag(waypoints.get(i));
            markers.add(marker);
        }

        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Context context = mainActivity;

                LinearLayout info = new LinearLayout(mainActivity);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
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
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(path), 200));
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
                    mainActivity.setStartFlightButtonEnabled((error == null));
                    mainActivity.setResultToToast("Prepare mission: " + (error == null ? "Success!" : error.getDescription()));
                    Log.d("prepareMission", "onResult: " + (error == null ? "Success!" : error.getDescription()));
                }
            });
        }
    }

    private boolean checkCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private boolean checkPitchYaw(int pitch, int yaw) {
        return ((pitch >= -90 && pitch <= 0) && (yaw >= -180 && yaw <= 180));
    }
}
