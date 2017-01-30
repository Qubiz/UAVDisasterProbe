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
 * Created by User on 12-1-2017.
 */

public class GridFlightPath {

    private ResizableRectangle rectangle;
    private Polyline flightPath;
    //private ArrayList<LatLng> rectanglePoints = new ArrayList();
    private double focalLength = 4.0;
    private double pixelSize = 0.00152;
    private double GSD = 0.03;
    private float distanceBorder = 5; // in meters
    private boolean sideways;
    private int NFL;
    private int NIM;
    private double totallongitude;
    private double totallatitude;
    private GoogleMap gMap;
    private ArrayList<LatLng> poliline ;

    public GridFlightPath(ResizableRectangle rectangle, GoogleMap googleMap){
        this.gMap = googleMap;
        this.create(rectangle);
    }
    public ArrayList<LatLng> createFlightpoints(ArrayList<LatLng> rectanglePoints){
        poliline = new ArrayList<>();
        poliline.add(new LatLng(rectanglePoints.get(0).latitude, rectanglePoints.get(0).longitude));
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
                if(j==(NFL-1)){
                    for (int i = 0; i < NIM; i++) {
                        poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude + distance));
                    }
                }

            } else {
                double distance = totallatitude/ NIM;
                double sideway = totallongitude / NFL;
                for (int i = 0; i < NIM; i++) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude + distance, poliline.get(poliline.size() - 1).longitude));
                    //Log.d("createFlightPath", "eerst breedte");
                }
                if(j<NFL) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude + sideway));

                    float[] result3 = new float[1];
                    Location.distanceBetween(poliline.get(poliline.size()-1).latitude, poliline.get(poliline.size()-1).longitude, poliline.get(poliline.size()-2).latitude, poliline.get(poliline.size()-2).longitude, result3);
                }
                j++;
                for (int i = 0; i < NIM; i++) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude - distance, poliline.get(poliline.size() - 1).longitude));
                }
                if(j<NFL) {
                    poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude, poliline.get(poliline.size() - 1).longitude + sideway));

                    float[] result3 = new float[1];
                    Location.distanceBetween(poliline.get(poliline.size()-1).latitude, poliline.get(poliline.size()-1).longitude, poliline.get(poliline.size()-2).latitude, poliline.get(poliline.size()-2).longitude, result3);
                }
                if(j==(NFL-1)){
                    for (int i = 0; i < NIM; i++) {
                        poliline.add(new LatLng(poliline.get(poliline.size() - 1).latitude + distance, poliline.get(poliline.size() - 1).longitude));
                    }
                }
            }
        }
        return poliline;




    }
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
    public double getFlightHeigth(){
        return GSD*focalLength/pixelSize;
    }

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
    public ArrayList<LatLng> getFlightPath(){
        return poliline;
    }

    public void create(ResizableRectangle rectangle){
        ArrayList<LatLng> rectanglePoints = rectangle.getCornerCoordinates();
        rectanglePoints = adjustBorder(rectanglePoints, distanceBorder);
        setFlightdetails(rectanglePoints);
        Log.d("flightHigth", " "+this.getFlightHeigth() );
        ArrayList<LatLng> flightpoints = new ArrayList<>(createFlightpoints(rectanglePoints));
        flightPath = gMap.addPolyline(new PolylineOptions()
                .addAll(flightpoints)
                .color(Color.RED));
    }
    public void remove(){
        flightPath.remove();
    }
    public void update(ResizableRectangle rectangle){
        flightPath.remove();
        create(rectangle);

    }
}