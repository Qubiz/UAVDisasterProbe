package com.uav.utwente.uavdisasterprobe;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Looper;
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
import com.opencsv.CSVReader;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.products.DJIAircraft;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DJIMissionManager.MissionProgressStatusCallback, DJICommonCallbacks.DJICompletionCallback {

    private static final int FILE_REQUEST_CODE = 42;

    private static final CharSequence[] MAP_TYPE_ITEMS = {"Normal", "Satellite", "Terrain", "Hybrid"};

    private Button loadWaypointsButton;
    private Button mapTypesButton;
    private Button startFlightButton;

    private TextView productConnectedTextView;

    private DJIFlightController flightController;
    private DJIMissionManager missionManager;
    private DJIWaypointMission waypointMission;

    private GoogleMap googleMap;

    private Marker droneMarker;

    private double droneLocationLatitude = 181;
    private double droneLocationLongitude = 181;

    FlightPath flightPath;

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
                flightPath.startMission(missionManager);
            }
        });

        productConnectedTextView = (TextView) findViewById(R.id.product_connected_textview);
    }

    private void updateConnectedTextView() {
        if(productConnectedTextView == null) return;

        boolean ret = false;
        DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();

        if(product != null) {
            if(product.isConnected()) {
                productConnectedTextView.setText(UAVDisasterProbeApplication.getProductInstance().getModel() + " connected...");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft) product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        productConnectedTextView.setText("Only the RC is connected...");
                        ret = true;
                    }
                }
            }
        }

        if(!ret) {
            productConnectedTextView.setText("Disconnected...");
        }
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initiateFlightController();
            updateConnectedTextView();
        }
    };

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

    private void initiateFlightController() {
        DJIAircraft aircraft = UAVDisasterProbeApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            setResultToToast("Disconnected");
            flightController = null;
            return;
        } else {
            flightController = aircraft.getFlightController();
            flightController.getSimulator().setUpdatedSimulatorStateDataCallback(new DJISimulator.UpdatedSimulatorStateDataCallback() {
                @Override
                public void onSimulatorDataUpdated(final DJISimulatorStateData djiSimulatorStateData) {
                    droneLocationLatitude = djiSimulatorStateData.getLatitude();
                    droneLocationLongitude = djiSimulatorStateData.getLongitude();
                    updateDroneLocation();
                }
            });
        }
        /*DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
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
                    updateDroneLocation();
                }
            });
        }*/
    }

    private void initiateMissionManager() {
        DJIBaseProduct product = UAVDisasterProbeApplication.getProductInstance();
        if(product != null && product.isConnected()) {
            setResultToToast("Product connected...");
            missionManager = product.getMissionManager();
            missionManager.setMissionProgressStatusCallback(this);
            missionManager.setMissionExecutionFinishedCallback(this);
        } else {
            setResultToToast("Product disconnected...");
            missionManager = null;
            return;
        }

        waypointMission = new DJIWaypointMission();
    }

    private void setResultToToast(final String string){
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
    }

    private void selectWaypointFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/csv");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, FILE_REQUEST_CODE);
        }
    }

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
                    startFlightButton.setEnabled(true);
                }
                setResultToToast("Loaded file: " + waypointFile.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateDroneLocation() {
        LatLng position = new LatLng(droneLocationLatitude, droneLocationLongitude);

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(position);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(droneMarker != null) droneMarker.remove();
                if(checkGPSCoordinates(droneLocationLatitude, droneLocationLongitude)) {
                    droneMarker = googleMap.addMarker(markerOptions);
                }
            }
        });
    }

    private boolean checkGPSCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

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
                        break;
                    case 1:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case 2:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;
                    default:
                        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
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
}
