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

/**
 * ResizableRectangle:
 *
 * This class creates a resizable rectangle on a GoogleMap. The rectangle can be dragged at its corner
 * to change its dimensions. Furthermore, using the area of the rectangle a GridFlightPath is created.
 */
public class ResizableRectangle implements GoogleMap.OnMarkerDragListener {

    private GoogleMap googleMap;

    private Polygon rectangle;

    private ArrayList<Marker> cornerMarkers;

    private LatLng centerPoint;

    private GridFlightPath gridFlightPath;

    /**
     * Creates a new ResizableRectangle object.
     * @param googleMap The map to draw on.
     * @param centerPoint The initial center point of the rectangle.
     * @param width The initial width of the rectangle.
     * @param height The initial height of the rectangle
     */
    public ResizableRectangle(GoogleMap googleMap, LatLng centerPoint, double width, double height) {
        this.googleMap = googleMap;
        this.centerPoint = centerPoint;

        rectangle = createRectangle(centerPoint, width, height);
        cornerMarkers = createCornerMarkers(rectangle);

        googleMap.setOnMarkerDragListener(this);
        gridFlightPath = new GridFlightPath(this, googleMap);
    }

    /**
     * @return A list of coordinates of the corners of the rectangle.
     */
    public ArrayList<LatLng> getCornerCoordinates() {
        ArrayList<LatLng> coordinates = new ArrayList<>();

        for(Marker marker : cornerMarkers) {
            coordinates.add(marker.getPosition());
        }

        return coordinates;
    }

    /**
     * Creates a rectangle on the map.
     * @param centerPoint Center point of the rectangle.
     * @param width Width of the rectangle.
     * @param height Height of the rectangle.
     * @return
     */
    private Polygon createRectangle(LatLng centerPoint, double width, double height) {
        PolygonOptions rectangleOptions = new PolygonOptions()
                .add(new LatLng(centerPoint.latitude + (height/2), centerPoint.longitude - (width/2)), // TOP LEFT
                        new LatLng(centerPoint.latitude + (height/2), centerPoint.longitude + (width/2)), // TOP RIGHT
                        new LatLng(centerPoint.latitude - (height/2), centerPoint.longitude + (width/2)), // BOTTOM RIGHT
                        new LatLng(centerPoint.latitude - (height/2), centerPoint.longitude - (width/2))); // BOTTOM LEFT

        return googleMap.addPolygon(rectangleOptions);
    }

    /**
     * Creates a polygon using 4 points.
     * @param point1
     * @param point2
     * @param point3
     * @param point4
     * @return
     */
    private Polygon createRectangle(LatLng point1, LatLng point2, LatLng point3, LatLng point4) {
        PolygonOptions rectangleOptions = new PolygonOptions()
                .add(point1, point2, point3, point4, point1);

        return googleMap.addPolygon(rectangleOptions);
    }

    /**
     * Creates draggable markers for the corners of the rectangle.
     * @param rectangle the rectangle.
     * @return A list of draggable markers.
     */
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

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    /**
     * Called to update the rectangles dimensions when dragging the markers.
     * @param marker
     */
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
        // Recalculate the flightpath when the user stops dragging the markers.
        gridFlightPath.update(this);
    }

    public GridFlightPath getGridFlightPath() {
        return gridFlightPath;
    }

    /**
     * Zoom to get the full rectangle in the GoogleMap view.
     * @param animate
     */
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

    /**
     * Remove the rectangle and flightpath from the map.
     */
    public void remove() {
        rectangle.remove();
        for(Marker marker : cornerMarkers) {
            marker.remove();
        }
        gridFlightPath.remove();
    }
}