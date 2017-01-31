package com.uav.utwente.uavdisasterprobe;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;

import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.products.DJIAircraft;

/**
 * The MainActivity of the application.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DJIMissionManager.MissionProgressStatusCallback, DJICommonCallbacks.DJICompletionCallback {

    private static final int FILE_REQUEST_CODE = 42;

    private static final CharSequence[] MAP_TYPE_ITEMS = {"Normal", "Satellite", "Terrain", "Hybrid"};

    private Button loadWaypointsButton;
    private Button mapTypesButton;
    private Button startFlightButton;
    private Button stopFlightButton;
    private Button prepareFlightButton;
    //private Button downloadButton;

    private TextView productConnectedTextView;
    private TextView loadedFileTextView;

    private DJIFlightController flightController;
    private DJIMissionManager missionManager;

    private GoogleMap googleMap;
    private Marker droneMarker;

    private double droneLocationLatitude = 181;
    private double droneLocationLongitude = 181;

    private FlightPath flightPath;

    // private MediaDownload mediaDownload;


    /**
     * onCreate(Bundle) is where you initialize your activity. Most importantly, here you will usually
     * call setContentView(int) with a layout resource defining your UI, and using findViewById(int)
     * to retrieve the widgets in that UI that you need to interact with programmatically.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UAVDisasterProbeApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(receiver, filter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        loadWaypointsButton = (Button) findViewById(R.id.load_waypoints_button);
        loadWaypointsButton.setText("LOAD WAYPOINTS");
        loadWaypointsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWaypointFile();
            }
        });

        mapTypesButton = (Button) findViewById(R.id.map_types_button);
        mapTypesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMapTypeSelectorDialog();
            }
        });
        mapTypesButton.setText(MAP_TYPE_ITEMS[1]);

        startFlightButton = (Button) findViewById(R.id.start_flight_button);
        startFlightButton.setText("START");
        startFlightButton.setEnabled(false);
        startFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResultToToast("Starting flight..");
                flightPath.startMission(missionManager);
                /*stopFlightButton.setEnabled(true);
                startFlightButton.setEnabled(false);
                prepareFlightButton.setEnabled(false);*/
            }
        });

        stopFlightButton = (Button) findViewById(R.id.stop_flight_button);
        stopFlightButton.setText("STOP");
        stopFlightButton.setEnabled(false);
        stopFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResultToToast("Stopping flight..");
                flightPath.stopMission(missionManager);
                /*stopFlightButton.setEnabled(false);
                if(flightPath != null) {
                    prepareFlightButton.setEnabled(true);
                }*/
                //updateButtons();
            }
        });

        prepareFlightButton = (Button) findViewById(R.id.prepare_flight_button);
        prepareFlightButton.setText("PREPARE");
        prepareFlightButton.setEnabled(false);
        prepareFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResultToToast("Preparing flight..");
                flightPath.prepareMission(missionManager);
                //startFlightButton.setEnabled(true);

            }
        });

        /*
        downloadButton = (Button) findViewById(R.id.download_button);
        downloadButton.setText("DOWNLOAD");
        downloadButton.setEnabled(true);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
                if(product != null && product.isConnected()) {
                    setResultToToast("Trying to download media list...");
                    if(mediaDownload == null) {
                        mediaDownload = new MediaDownload();
                    }
                    mediaDownload.fetchLatestPhoto();
                } else {
                    setResultToToast("No product connected...");
                }
            }
        });
        */

        productConnectedTextView = (TextView) findViewById(R.id.product_connected_textview);
        productConnectedTextView.setTextColor(Color.WHITE);

        loadedFileTextView = (TextView) findViewById(R.id.loaded_file_textview);
        loadedFileTextView.setTextColor(Color.WHITE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initiateFlightController();
        initiateMissionManager();
        updateConnectedTextView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    /**
     * Called when the map is ready to be used.
     *
     * @param googleMap A non-null instance of a GoogleMap associated with the MapFragment or MapView that defines the callback.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(this.googleMap == null) {
            this.googleMap = googleMap;
        }

        UiSettings uiSettings = this.googleMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setTiltGesturesEnabled(false);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(false);

        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    /**
     * Updates the the status text with the current connection state.
     */
    private void updateConnectedTextView() {
        if(productConnectedTextView == null) return;

        boolean ret = false;
        DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();

        if(product != null) {
            if(product.isConnected()) {
                productConnectedTextView.setText(UAVDisasterProbeApplication.getProductInstance().getModel() + " connected");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft) product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        productConnectedTextView.setText("Only the remote controller is connected");
                        ret = true;
                    }
                }
            }
        }

        if(!ret) {
            productConnectedTextView.setText("Disconnected");
        }
    }

    /**
     * Called when a broadcast is received about a connection change between the application
     * and the product.
     */
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initiateFlightController();
            updateConnectedTextView();
        }
    };

    /**
     * Called to initiate the flight controller of the aircraft if it is connected. Needs to
     * be called whenever there is a connection change.
     */
    private void initiateFlightController() {
        DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
        if(product != null && product.isConnected()) {
            if(product instanceof DJIAircraft) {
                flightController = ((DJIAircraft) product).getFlightController();
            }
        }

        if(flightController != null) {
            flightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerCurrentState state) {
                    droneLocationLatitude = state.getAircraftLocation().getLatitude();
                    droneLocationLongitude = state.getAircraftLocation().getLongitude();
                    //updateDroneLocation();
                }
            });
        }
    }

    /**
     * Called to initiate the mission manager. Needs to be called whenever there is a connection change.
     */
    private void initiateMissionManager() {
        DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
        if(product != null && product.isConnected()) {
            setResultToToast("Product connected...");
            missionManager = product.getMissionManager();
            missionManager.setMissionProgressStatusCallback(this);
            missionManager.setMissionExecutionFinishedCallback(this);
        } else {
            setResultToToast("Product not connected...");
            missionManager = null;
        }
    }

    /**
     * Enables or disables the control (prepare, start and stop) buttons depending on whether there
     * is a connection to the drone and whether there is a flightpath loaded.
     */
    private void updateButtons() {
        DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
        if(product != null && product.isConnected()) {
            if(flightPath != null) {
                prepareFlightButton.setEnabled(true);
            }
        } else {
            prepareFlightButton.setEnabled(false);
            startFlightButton.setEnabled(false);
            stopFlightButton.setEnabled(false);
        }
    }

    /**
     * Sends a short message as a 'toast' to the screen.
     *
     * @param string The message to display.
     */
    public void setResultToToast(final String string){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {

    }

    @Override
    public void onResult(DJIError error) {
        setResultToToast("Execution finished: " + (error == null ? "Succes!" : error.getDescription()));
        flightPath.stopMission(missionManager);
    }

    /**
     * Called to start a file loader to allow the user to search for a certain file in their system.
     */
    private void selectWaypointFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/csv");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, FILE_REQUEST_CODE);
        }
    }

    /**
     * Called when the user succesfully selected a file using the file loader. In here the creation of the
     * flightpath from the CSV file is being done.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            File waypointFile = new File(data.getData().getPath());
            try {
                if(flightPath != null) {
                    flightPath.removeFromMap();
                }
                flightPath = new FlightPath(this, waypointFile);
                flightPath.showOnMap(googleMap);

                DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
                if(product != null && product.isConnected()) {
                    prepareFlightButton.setEnabled(true);
                }
                setResultToToast("Loaded file: " + waypointFile.getName());
                loadedFileTextView.setText(waypointFile.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * /!\ This method sometimes makes the application unresponsive to user input /!\
     * Updates the location of the drone on the map.
     */
    private void updateDroneLocation() {
        LatLng position = new LatLng(droneLocationLatitude, droneLocationLongitude);

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(position);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(droneMarker != null) droneMarker.remove();
                if(checkGPSCoordinates(droneLocationLatitude, droneLocationLongitude)) {
                    Log.d("updateDroneLocation", "" + droneLocationLatitude + ", " + droneLocationLongitude);
                    droneMarker = googleMap.addMarker(markerOptions);
                }
            }
        });
    }

    /**
     * Checks whether the given GPS coordinates are correct.
     *
     * @param latitude
     * @param longitude
     * @return TRUE if the GPS coordinates are correct, FALSE otherwise.
     */
    private boolean checkGPSCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    /**
     * Shows a dialog to the user that allows to switch between map types.
     */
    private void showMapTypeSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SELECT MAP TYPE");

        int checkItem = googleMap.getMapType() - 1;

        builder.setSingleChoiceItems(MAP_TYPE_ITEMS, checkItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int item) {
                switch (item) {
                    case 0:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        productConnectedTextView.setTextColor(Color.BLACK);
                        loadedFileTextView.setTextColor(Color.BLACK);
                        break;
                    case 1:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        productConnectedTextView.setTextColor(Color.WHITE);
                        loadedFileTextView.setTextColor(Color.WHITE);
                        break;
                    case 2:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        productConnectedTextView.setTextColor(Color.BLACK);
                        loadedFileTextView.setTextColor(Color.BLACK);
                        break;
                    default:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        productConnectedTextView.setTextColor(Color.WHITE);
                        loadedFileTextView.setTextColor(Color.WHITE);
                        break;
                }

                mapTypesButton.setText(MAP_TYPE_ITEMS[item]);
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void setStartFlightButtonEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startFlightButton.setEnabled(enabled);
            }
        });

    }

    public void setPrepareFlightButtonEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepareFlightButton.setEnabled(enabled);
            }
        });

    }

    public void setStopFlightButtonEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopFlightButton.setEnabled(enabled);
            }
        });
    }
}
