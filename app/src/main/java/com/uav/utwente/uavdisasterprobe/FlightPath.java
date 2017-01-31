package com.uav.utwente.uavdisasterprobe;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
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

/**
 * FlightPath:
 *
 * This class is used to
 * (1) create a flightpath from a CSV file,
 * (2) show the flightpath on a GoogleMap,
 * (3) prepare, start and stop the flight.
 */
public class FlightPath {
    // A list of markers defining the waypoints on the map.
    private ArrayList<Marker> markers;

    // A line to show the path the drone will fly.
    private Polyline path;

    // A list of waypoints that can be put into a mission.
    private ArrayList<DJIWaypoint> waypointsList;

    // The mission that will be uploaded to the aircraft.
    private DJIWaypointMission waypointMission;

    // The file containing the waypoints, needs to be a CSV file with the correct format.
    private File waypointFile;

    // A reference to the MainActivity, used to change the status of the buttons on the interface.
    private MainActivity mainActivity;

    /**
     * Creates a new flight path using the given waypoint file.
     *
     * @param mainActivity A reference to the MainActivity.
     * @param waypointFile The waypoint file to read.
     * @throws IOException
     */
    public FlightPath(MainActivity mainActivity, File waypointFile) throws IOException {
        this.mainActivity = mainActivity;
        this.waypointFile = waypointFile;
        waypointsList = new ArrayList<>();
        waypointMission = new DJIWaypointMission();
        waypointMission.finishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
        waypointMission.headingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
        waypointMission.autoFlightSpeed = 10.0f;
        readFromFile(waypointFile);
    }

    /**
     * Reads the given file and prepares a list of waypoints.
     *
     * While reading the file it checks whether the header of the file is correctly
     * formatted as: 'latitide;longitude;altitude;pitch'. It then proceeds with reading the
     * values of the file.
     *
     * @param waypointFile The waypoint file to read.
     * @throws IOException
     */
    private void readFromFile(File waypointFile) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(waypointFile.getPath()), ';');

        double latitude;
        double longitude;
        float altitude;
        int pitch;
        int yaw;

        int count = 0;

        // Read all the entries of the CSV file.
        List<String[]> entries = reader.readAll();

        // Checks whether the header of the file is correct.
        if(entries.get(0)[0].substring(1).equals("latitude")
                && entries.get(0)[1].equals("longitude")
                && entries.get(0)[2].equals("altitude")
                && entries.get(0)[3].equals("pitch")
                && entries.get(0)[4].equals("yaw")) {
            Log.d("FlightPath", "readFromFile | The header of the file is correct!");
            // Iterate through all the entries one by one.
            for(String[] entry : entries) {
                if(count != 0) {
                    // Check whether there are 5 values in the row.
                    if(entry.length == 5) {
                        try {
                            // Parse all the values.
                            latitude = Double.parseDouble(entry[0]);
                            longitude = Double.parseDouble(entry[1]);
                            altitude = Float.parseFloat(entry[2]);
                            pitch = Integer.parseInt(entry[3]);
                            yaw = Integer.parseInt(entry[4]);

                            // Check whether the coordinate values are correct.
                            if(checkCoordinates(latitude, longitude)) {
                                // Check whether the pitch and yaw values are correct
                                if(checkPitchYaw(pitch, yaw)) {
                                    DJIWaypoint waypoint = new DJIWaypoint(latitude, longitude, altitude);
                                    waypoint.turnMode = DJIWaypoint.DJIWaypointTurnMode.Clockwise;
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, pitch));
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.RotateAircraft, yaw));
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartTakePhoto, 0));
                                    waypoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, 0));
                                    waypointsList.add(waypoint);
                                    Log.d("FlightPath", "readFromFile | Created waypoint: " + waypoint.latitude + " | " + waypoint.longitude);
                                } else {
                                    //!\\ ERROR SHOULD BE HANDLED BETTER //!\\
                                    // TODO: Show the user which entry in the CSV file is incorrect and stop reading the file.
                                    Log.d("FlightPath", "readFromFile | Pitch and yaw values incorrect.");
                                }
                            } else {
                                //!\\ ERROR SHOULD BE HANDLED BETTER //!\\
                                // TODO: Show the user which entry in the CSV file is incorrect and stop reading the file.
                                Log.d("FlightPath", "readFromFile | Coordinates incorrect.");
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //!\\ ERROR SHOULD BE HANDLED BETTER //!\\
                        // TODO: Show the user which entry in the CSV file is incorrect and stop reading the file.
                        Log.d("FlightPath", "readFromFile | Error at entry '" + count + "': all entries should have five parameters (latitude, longitude, altitude, pitch and yaw)!");
                    }
                }
                count++;
            }
        } else {
            Log.d("FlightPath", "readFromFile | The header of the file is NOT correct!");
        }
    }

    /**
     * Called to show the flightpath on the map by creating waypoints and a line to show the path.
     * This also makes the GoogleMap view zoom to the location of the flightpath.
     * @param googleMap
     */
    public void showOnMap(GoogleMap googleMap) {
        createMarkers(waypointsList, googleMap);
        createPolylinePath(waypointsList, googleMap);

        zoomTo(googleMap);
    }

    /**
     * Determines a bounding box around the given path. Used to determine the size of the GoogleMap
     * view when zooming in.
     * @param path The path to create a bounding box around.
     * @return
     */
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

    /**
     * Creates markers to show the waypoints on the map. The markers are clickable and show the
     * details (lat, long, alt, pitch and yaw) of the waypoints.
     *
     * @param waypoints The waypoints list containing the locations for the markers.
     * @param googleMap The GoogleMap reference to show the markers on.
     */
    private void createMarkers(List<DJIWaypoint> waypoints, GoogleMap googleMap) {
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

            if(i == 0) { // START POINT OF THE FLIGHT PATH
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            } else if(i == waypoints.size() - 1) { // END POINT OF THE FLIGHT PATH
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

        // Set a custom window adapter to show a multiline snippet to the user when they click on a marker.
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

    /**
     * Creates a line on the map that represents the path the drone will fly.
     *
     * @param waypoints The list of waypoints.
     * @param googleMap The GoogleMap reference to show the path on.
     */
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

    /**
     * Removes the flight path from the map.
     */
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

    /**
     * Zooms the camera the the flight path.
     * @param googleMap The GoogleMap reference.
     */
    public void zoomTo(GoogleMap googleMap) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(path), 200));
    }

    /**
     * Called to start the mission. Should be called after prepareMission is succesfully called.
     * @param missionManager
     */
    public void startMission(DJIMissionManager missionManager) {
        if(missionManager != null) {
            missionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Log.d("startMission", "onResult: " + (error == null ? "Succes!" : error.getDescription()));
                    if(error == null) {
                        Log.d("startMission", "onResult: " + "wtf??");
                        mainActivity.setStopFlightButtonEnabled(true);
                        mainActivity.setStartFlightButtonEnabled(false);
                        mainActivity.setPrepareFlightButtonEnabled(false);
                    } else {
                        Log.d("startMission", "onResult: " + "OK...??");
                        mainActivity.setStartFlightButtonEnabled(false);
                        mainActivity.setStartFlightButtonEnabled(false);
                        mainActivity.setPrepareFlightButtonEnabled(true);
                    }
                }
            });
        }
    }

    /**
     * Called to stop the mission.
     * @param missionManager
     */
    public void stopMission(DJIMissionManager missionManager) {
        if(missionManager != null) {
            missionManager.stopMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    mainActivity.setPrepareFlightButtonEnabled(true);
                    mainActivity.setStartFlightButtonEnabled(false);
                    mainActivity.setStopFlightButtonEnabled(false);
                    Log.d("stopMission", "onResult: " + (error == null ? "Succes!" : error.getDescription()));
                }
            });
            waypointMission.removeAllWaypoints();
        }
    }

    /**
     * Called to prepare the mission. It uploads the flightpath to the drone.
     * @param missionManager An instance of the mission manager of the drone.
     */
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
                    if(error == null) {
                        mainActivity.setStartFlightButtonEnabled(true);
                        mainActivity.setPrepareFlightButtonEnabled(false);
                        mainActivity.setStopFlightButtonEnabled(false);
                    } else{
                        mainActivity.setStartFlightButtonEnabled(false);
                        mainActivity.setPrepareFlightButtonEnabled(true);
                        mainActivity.setStopFlightButtonEnabled(false);
                    }
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
