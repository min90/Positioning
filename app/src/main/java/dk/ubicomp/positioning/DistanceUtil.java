package dk.ubicomp.positioning;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jesper on 17/03/2017.
 */

public class DistanceUtil {

    // Weight is strength of signal

    private LatLng weightedCentroidLocalication(int numberOfBeacons, double RSSI) {
        int g = 0;
        double calculatedRSSI = 10 * (RSSI / 10);
        double calculatedRSSIpower = Math.sqrt(Math.pow(calculatedRSSI, g));


        return new LatLng(0, 0);
    }
}
