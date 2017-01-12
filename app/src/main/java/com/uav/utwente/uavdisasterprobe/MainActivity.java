package com.uav.utwente.uavdisasterprobe;

import android.Manifest;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dji.sdk.missionmanager.DJIWaypoint;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerDragListener {

    private GoogleMap gMap;
    private List<LatLng> pointlist;
    private List<LatLng> linelist;
    private ArrayList<Marker> markers = new ArrayList();
    private Polygon square;
    private Polyline poliline;
    private double focalLength = 20.0;
    private double pixelSize = 0.00152;
    private double GSD = 0.01;
    private Boolean setSquare = false;
    private float distanceBorder = 5; // in meters



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object

    }

    public void onMapClick(final LatLng point) {
        if(setSquare) {
            square.remove();
            poliline.remove();
            for(Marker marker : markers){
                marker.remove();
            }
        }
            pointlist = createRectangle(point, 0.001, 0.001);
            PolygonOptions options = new PolygonOptions()
                    .addAll(pointlist)
                    .strokeColor(Color.BLACK)
                    .zIndex(-1);
            square = gMap.addPolygon(options);

            for (LatLng corner : pointlist) {
                markers.add(gMap.addMarker(new MarkerOptions()
                        .position(corner)
                        .draggable(true)));
            }
            linelist = createPoliline(pointlist, GSD);
            poliline = gMap.addPolyline(new PolylineOptions()
                    .addAll(linelist)
                    .color(Color.RED));
            setSquare = true;
    }

    private List<LatLng> createRectangle(LatLng center, double halfWidth, double halfHeight) {
        return Arrays.asList(new LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
                new LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
                new LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
                new LatLng(center.latitude + halfHeight, center.longitude - halfWidth));
    }
    private List<LatLng> createPoliline(List<LatLng> polygone, double GSD){
        polygone = adjustBorder(polygone);
        double totallatitude = polygone.get(3).latitude - polygone.get(0).latitude;
        double totallongitude = polygone.get(1).longitude - polygone.get(0).longitude;
        float[] result1 = new float[1];
        Location.distanceBetween(polygone.get(3).latitude, polygone.get(3).longitude, polygone.get(0).latitude, polygone.get(0).longitude, result1);
        float[] result2 = new float[1];
        Location.distanceBetween(polygone.get(1).latitude, polygone.get(1).longitude, polygone.get(0).latitude, polygone.get(0).longitude, result2);
        float width;
        float length;
        Boolean sideways;
        if(result1[0]<result2[0]){
            sideways = true;
            width = result2[0];
            length = result1[0];
        }else{
            sideways = false;
            width = result1[0];
            length = result2[0];
        }

        double flightHigth = GSD*focalLength/pixelSize;
        double imageWidth = GSD*4000;
        double imageLength = GSD *3000;
        double SP = imageWidth *(100-50)/100;
        int NFL = (int) ((width/SP)+0.9999999);
        double B = imageLength*(100-75)/100;
        int NIM = (int) ((length/B)+1.99999999);
        Log.d("createPoliline", "Image length:" + imageLength+"image width:"+ imageWidth);
        Log.d("createPoliline", "total length:" + length + "total width:" + width);
        Log.d("createPoliline", "distatnce lines"+ SP+"distance photo" + B);
        Log.d("createPoliline", "flight Higth:" + flightHigth);
        ArrayList<LatLng> poliline = new ArrayList<>();
        poliline.add(new LatLng(polygone.get(0).latitude, polygone.get(0).longitude));
       for (int j = 0; j<NFL;j++) {
            if (sideways) {
                double distance = totallongitude / NIM;
                double sideway = totallatitude / NFL;
                for (int i = 0; i < NIM; i++) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude + distance));
                }
                if(j<NFL) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude + sideway, poliline.get(poliline.size() - 1).longitude));
                }
                j++;
                for (int i = 0; i < NIM; i++) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude - distance));
                }
                if(j<NFL) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude + sideway, poliline.get(poliline.size() - 1).longitude));
                }

            } else {
                double distance = totallatitude/ NIM;
                double sideway = totallongitude / NFL;
                for (int i = 0; i < NIM; i++) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude + distance, poliline.get(poliline.size() - 1).longitude));
                }
                if(j<NFL) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude + sideway));
                }
                j++;
                for (int i = 0; i < NIM; i++) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude - distance, poliline.get(poliline.size() - 1).longitude));
                }
                if(j<NFL) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude + sideway));
                }
            }
       }
        return poliline;
    }
    private List<LatLng> adjustBorder(List<LatLng> polygone){

        double totallatitude = polygone.get(3).latitude - polygone.get(0).latitude;
        double totallongitude = polygone.get(1).longitude - polygone.get(0).longitude;
        float[] result1 = new float[1];
        Location.distanceBetween(polygone.get(3).latitude, polygone.get(3).longitude, polygone.get(0).latitude, polygone.get(0).longitude, result1);
        float[] result2 = new float[1];
        Location.distanceBetween(polygone.get(1).latitude, polygone.get(1).longitude, polygone.get(0).latitude, polygone.get(0).longitude, result2);
        double latBorderChange = (result1[0]- distanceBorder) / result1[0] *totallatitude;
        double LngBorderChange = (result2[0]- distanceBorder) / result2[0] *totallongitude;
        polygone.set(0,new LatLng(polygone.get(0).latitude+latBorderChange, polygone.get(0).longitude+LngBorderChange));
        polygone.set(1,new LatLng(polygone.get(1).latitude+latBorderChange, polygone.get(1).longitude-LngBorderChange));
        polygone.set(2,new LatLng(polygone.get(2).latitude-latBorderChange, polygone.get(2).longitude-LngBorderChange));
        polygone.set(3,new LatLng(polygone.get(3).latitude-latBorderChange, polygone.get(3).longitude+LngBorderChange));
        return polygone;
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng itcbuilding = new LatLng(52.223697,6.8836169);
        gMap.addMarker(new MarkerOptions().position(itcbuilding).title("Marker in ITC building"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(itcbuilding));
        gMap.setOnMarkerDragListener(this);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d("onMarkerDrag","size:" + markers.size()+"index:" + marker.getId());
        for(int i=0;i<markers.size();i++) {
            if(i==0 || i==2){
                if (markers.get(i).getId().equals(marker.getId())) {
                    pointlist.set(i , marker.getPosition());
                    markers.get(((i+1)%4)).setPosition(new LatLng(marker.getPosition().latitude, markers.get((i+1)%4).getPosition().longitude));
                    pointlist.set((i+1)%4, markers.get((i+1)%4).getPosition());
                    markers.get(((i+3)%4)).setPosition(new LatLng(markers.get(((i+3)%4)).getPosition().latitude, marker.getPosition().longitude));
                    pointlist.set(((i+3)%4), markers.get((i+3)%4).getPosition());
                    Log.d("onmarkerDrag", ""+i);
                }
            }else{
                if (markers.get(i).getId().equals(marker.getId())) {
                    pointlist.set(i , marker.getPosition());
                    markers.get((i+3)%4).setPosition(new LatLng(marker.getPosition().latitude, markers.get((i+3)%4).getPosition().longitude));
                    pointlist.set(((i+3)%4), markers.get((i+3)%4).getPosition());
                    markers.get(((i+1)%4)).setPosition(new LatLng(markers.get(((i+1)%4)).getPosition().latitude, marker.getPosition().longitude));
                    pointlist.set(((i+1)%4), markers.get(((i+1)%4)).getPosition());
                    Log.d("onmarkerDrag", ""+i);
                }
            }
        }
        square.setPoints(pointlist);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        poliline.setPoints(createPoliline(pointlist,GSD));

    }
}
