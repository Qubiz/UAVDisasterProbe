package com.uav.utwente.uavdisasterprobe;

import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

public class ResizableRectangle implements GoogleMap.OnMarkerDragListener {

    private GoogleMap googleMap;
    private Polygon rectangle;
    private ArrayList<Marker> cornerMarkers;
    private LatLng centerPoint;

    public ResizableRectangle(GoogleMap googleMap, LatLng centerPoint, double width, double height) {
        this.googleMap = googleMap;
        this.centerPoint = centerPoint;

        rectangle = createRectangle(centerPoint, width, height);
        cornerMarkers = createCornerMarkers(rectangle);

        googleMap.setOnMarkerDragListener(this);
    }

    private Polygon createRectangle(LatLng centerPoint, double width, double height) {
        PolygonOptions rectangleOptions = new PolygonOptions()
                .add(new LatLng(centerPoint.latitude + (height/2), centerPoint.longitude - (width/2)), // TOP LEFT
                        new LatLng(centerPoint.latitude + (height/2), centerPoint.longitude + (width/2)), // TOP RIGHT
                        new LatLng(centerPoint.latitude - (height/2), centerPoint.longitude + (width/2)), // BOTTOM RIGHT
                        new LatLng(centerPoint.latitude - (height/2), centerPoint.longitude - (width/2))); // BOTTOM LEFT

        return googleMap.addPolygon(rectangleOptions);
    }

    private Polygon createRectangle(LatLng point1, LatLng point2, LatLng point3, LatLng point4) {
        PolygonOptions rectangleOptions = new PolygonOptions()
                .add(point1, point2, point3, point4, point1);

        return googleMap.addPolygon(rectangleOptions);
    }

    private ArrayList<Marker> createCornerMarkers(Polygon rectangle) {
        ArrayList<Marker> markers = new ArrayList<>();

        for (int i = 0; i < rectangle.getPoints().size() - 1; i++) {
            LatLng point = new LatLng(rectangle.getPoints().get(i).latitude, rectangle.getPoints().get(i).longitude);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(point)
                    .draggable(true);
            markers.add(googleMap.addMarker(markerOptions));
        }

        return markers;
    }

    public List<LatLng> getCornerCoordinates() {
        return rectangle.getPoints();
    }

    public LatLng getCenterPoint() {
        return centerPoint;
    }

    public void zoomTo(boolean animate) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(Marker marker : cornerMarkers) {
            builder.include(marker.getPosition());
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 150);

        if (animate) {
            googleMap.animateCamera(cameraUpdate);
        } else {
            googleMap.moveCamera(cameraUpdate);
        }
    }

    public void remove() {
        rectangle.remove();
        for(Marker marker : cornerMarkers) {
            marker.remove();
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d("onMarkerDrag", marker.getId());
        if (cornerMarkers.get(0).getId().equals(marker.getId())) {
            cornerMarkers.get(1).setPosition(new LatLng(marker.getPosition().latitude, cornerMarkers.get(1).getPosition().longitude));
            cornerMarkers.get(3).setPosition(new LatLng(cornerMarkers.get(3).getPosition().latitude, marker.getPosition().longitude));
        } else if(cornerMarkers.get(1).getId().equals(marker.getId())) {
            cornerMarkers.get(0).setPosition(new LatLng(marker.getPosition().latitude, cornerMarkers.get(0).getPosition().longitude));
            cornerMarkers.get(2).setPosition(new LatLng(cornerMarkers.get(2).getPosition().latitude, marker.getPosition().longitude));
        } else if(cornerMarkers.get(2).getId().equals(marker.getId())) {
            cornerMarkers.get(3).setPosition(new LatLng(marker.getPosition().latitude, cornerMarkers.get(3).getPosition().longitude));
            cornerMarkers.get(1).setPosition(new LatLng(cornerMarkers.get(1).getPosition().latitude, marker.getPosition().longitude));
        } else if(cornerMarkers.get(3).getId().equals(marker.getId())) {
            cornerMarkers.get(2).setPosition(new LatLng(marker.getPosition().latitude, cornerMarkers.get(2).getPosition().longitude));
            cornerMarkers.get(0).setPosition(new LatLng(cornerMarkers.get(0).getPosition().latitude, marker.getPosition().longitude));
        }

        rectangle.remove();
        rectangle = createRectangle(cornerMarkers.get(0).getPosition(),
                cornerMarkers.get(1).getPosition(),
                cornerMarkers.get(2).getPosition(),
                cornerMarkers.get(3).getPosition());
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }
}