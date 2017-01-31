package com.uav.utwente.uavdisasterprobe;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * GridFlightPath
 *
 * This class is used to create a grid flight used to take pictures of an square area
 */

public class GridFlightPath {
    //The square that is drawn by the user to define the area the drone makes pictures of
    private ResizableRectangle rectangle;
    // the line of the flightpath that is added to the map
    private Polyline flightPath;
    // focal length is used to determine the flight height
    private double focalLength = 4.0;
    // pixelsize is used to determine the flight height
    private double pixelSize = 0.00152;
    // the GSD is the size of a pixel on the real ground in meters
    private double GSD = 0.03;
    // used to determine the position of the first line of pictures from the edge in meters
    private float distanceBorder = 5;
    // used to determine if the rectangle is longer to the side or going up
    private boolean sideways;
    //is the amount of line the path makes on the small side of the square
    private int NFL;
    //is the amount of line the path makes on the long side of the square
    private int NIM;
    // the total distance of the side square in longitude degree
    private double totallongitude;
    //the total distance of the side square in latitude degree
    private double totallatitude;
    //is the object that is used as the background of the app
    private GoogleMap gMap;
    //is the list of all the points the drone will go through
    private ArrayList<LatLng> polyline ;

    /**
     * creates a new GridflightPath object
     *
     *@param rectangle the square used to create the grid path
     *@param googleMap the map to draw on
     */
    public GridFlightPath(ResizableRectangle rectangle, GoogleMap googleMap){
        this.gMap = googleMap;
        this.create(rectangle);
    }
    /**
     * creates a list of point that make up the grid fligth path
     *
     * @param rectanglePoints the points used to determine the square the user has drawn.
     * @return the polyline that can be drawn on the google maps element
     */
    public ArrayList<LatLng> createFlightpoints(ArrayList<LatLng> rectanglePoints){
        polyline = new ArrayList<>();
        polyline.add(new LatLng(rectanglePoints.get(0).latitude, rectanglePoints.get(0).longitude));
        for (int j = 0; j<NFL;j++) {
            if (sideways) {
                double distance = totallongitude / NIM;
                double sideway = totallatitude / NFL;
                for (int i = 0; i < NIM; i++) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude, polyline.get(polyline.size() - 1).longitude + distance));
                }
                if(j<NFL) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude + sideway, polyline.get(polyline.size() - 1).longitude));
                }
                j++;
                for (int i = 0; i < NIM; i++) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude, polyline.get(polyline.size() - 1).longitude - distance));
                }
                if(j<NFL) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude + sideway, polyline.get(polyline.size() - 1).longitude));
                }
                if(j==(NFL-1)){
                    for (int i = 0; i < NIM; i++) {
                        polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude, polyline.get(polyline.size() - 1).longitude + distance));
                    }
                }

            } else {
                double distance = totallatitude/ NIM;
                double sideway = totallongitude / NFL;
                for (int i = 0; i < NIM; i++) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude + distance, polyline.get(polyline.size() - 1).longitude));
                    //Log.d("createFlightPath", "eerst breedte");
                }
                if(j<NFL) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude, polyline.get(polyline.size() - 1).longitude + sideway));

                    float[] result3 = new float[1];
                    Location.distanceBetween(polyline.get(polyline.size()-1).latitude, polyline.get(polyline.size()-1).longitude, polyline.get(polyline.size()-2).latitude, polyline.get(polyline.size()-2).longitude, result3);
                }
                j++;
                for (int i = 0; i < NIM; i++) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude - distance, polyline.get(polyline.size() - 1).longitude));
                }
                if(j<NFL) {
                    polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude, polyline.get(polyline.size() - 1).longitude + sideway));

                    float[] result3 = new float[1];
                    Location.distanceBetween(polyline.get(polyline.size()-1).latitude, polyline.get(polyline.size()-1).longitude, polyline.get(polyline.size()-2).latitude, polyline.get(polyline.size()-2).longitude, result3);
                }
                if(j==(NFL-1)){
                    for (int i = 0; i < NIM; i++) {
                        polyline.add(new LatLng(polyline.get(polyline.size() - 1).latitude + distance, polyline.get(polyline.size() - 1).longitude));
                    }
                }
            }
        }
        return polyline;




    }

    /**
     * sets the flight details like the NFL and the NIM
     * @param rectanglePoints the corner points of the square
     */

    private void setFlightdetails(ArrayList<LatLng> rectanglePoints){
        double rectangleWidth;
        double rectangleLength;
        totallatitude = rectanglePoints.get(3).latitude - rectanglePoints.get(0).latitude;
        totallongitude = rectanglePoints.get(1).longitude - rectanglePoints.get(0).longitude;
        float[] result1 = new float[1];
        Location.distanceBetween(rectanglePoints.get(3).latitude, rectanglePoints.get(3).longitude, rectanglePoints.get(0).latitude, rectanglePoints.get(0).longitude, result1);
        float[] result2 = new float[1];
        Location.distanceBetween(rectanglePoints.get(1).latitude, rectanglePoints.get(1).longitude, rectanglePoints.get(0).latitude, rectanglePoints.get(0).longitude, result2);
        if(result1[0]<result2[0]){
            sideways = true;
            rectangleWidth = result1[0];
            rectangleLength = result2[0];
        }else{
            sideways = false;
            rectangleWidth = result2[0];
            rectangleLength = result1[0];
        }
        double imageWidth = GSD*4000;
        double imageLength = GSD *3000;
        double SP = imageWidth *(100-50)/100;
        NFL = (int) ((rectangleWidth/SP)+1);
        double B = imageLength*(100-75)/100;
        NIM = (int) ((rectangleLength/B)+2);
    }
    /**
     * returns the flight higth
     * @return flight higth in meters.
     */

    public double getFlightHigth(){
        return GSD*focalLength/pixelSize;
    }

    /**
     * the makes the flight path a bit smaller because the aircraft doesn't need to fly over the
     * borders. because the image is wider than the flight path.
     * @param rectanglePoints the corners of the original square
     * @param distanceBorder the distance between the the original square and the area the drone flies over
     * @return a list of the new corners for the area the drone flies over
     */

    private ArrayList<LatLng> adjustBorder(ArrayList<LatLng> rectanglePoints, double distanceBorder){

        double totallatitude = rectanglePoints.get(3).latitude - rectanglePoints.get(0).latitude;
        double totallongitude = rectanglePoints.get(1).longitude - rectanglePoints.get(0).longitude;
        float[] result1 = new float[1];
        Location.distanceBetween(rectanglePoints.get(3).latitude, rectanglePoints.get(3).longitude, rectanglePoints.get(0).latitude, rectanglePoints.get(0).longitude, result1);
        float[] result2 = new float[1];
        Location.distanceBetween(rectanglePoints.get(1).latitude, rectanglePoints.get(1).longitude, rectanglePoints.get(0).latitude, rectanglePoints.get(0).longitude, result2);
        double latBorderChange = (result1[0]- distanceBorder) / result1[0] *totallatitude;
        double LngBorderChange = (result2[0]- distanceBorder) / result2[0] *totallongitude;
        rectanglePoints.set(0,new LatLng(rectanglePoints.get(0).latitude+latBorderChange, rectanglePoints.get(0).longitude+LngBorderChange));
        rectanglePoints.set(1,new LatLng(rectanglePoints.get(1).latitude+latBorderChange, rectanglePoints.get(1).longitude-LngBorderChange));
        rectanglePoints.set(2,new LatLng(rectanglePoints.get(2).latitude-latBorderChange, rectanglePoints.get(2).longitude-LngBorderChange));
        rectanglePoints.set(3,new LatLng(rectanglePoints.get(3).latitude-latBorderChange, rectanglePoints.get(3).longitude+LngBorderChange));
        return rectanglePoints;
    }

    /**
     * used to get the drawable object of the grid flight
     * @return a polyline that can be drawn on a google maps instance
     */
    public ArrayList<LatLng> getFlightPath(){
        return polyline;
    }

    /**
     * Creates the grid flight path and draws on the google maps instance
     * @param rectangle the original rectangle drawn on the map.
     */
    public void create(ResizableRectangle rectangle){
        ArrayList<LatLng> rectanglePoints = rectangle.getCornerCoordinates();
        rectanglePoints = adjustBorder(rectanglePoints, distanceBorder);
        setFlightdetails(rectanglePoints);
        Log.d("flightHigth", " "+this.getFlightHigth() );
        ArrayList<LatLng> flightpoints = new ArrayList<>(createFlightpoints(rectanglePoints));
        flightPath = gMap.addPolyline(new PolylineOptions()
                .addAll(flightpoints)
                .color(Color.RED));
    }

    /**
     * removes the flight path
     */
    public void remove(){
        flightPath.remove();
    }

    /**
     * Updates the flightpath given a new rectangle
     * @param rectangle the new rectangle drawn by the user
     */
    public void update(ResizableRectangle rectangle){
        flightPath.remove();
        create(rectangle);

    }
}

