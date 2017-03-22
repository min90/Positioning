package dk.ubicomp.positioning;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Jesper on 21/03/2017.
 */

public class GeometryData {

    private static final String DEBUG_TAG = GeometryData.class.getSimpleName();

    private Context context;

    private static final String GEOMETRY = "ou44_geometry.geojson";
    private static final String BEACONS = "beacons.json";

    public GeometryData(Context context) {
        this.context = context;
    }

    private String readJson(String toParse) throws JSONException {
        String parsedJson = "";
        try {
            InputStream is = context.getAssets().open(toParse);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            parsedJson = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(DEBUG_TAG, "Unable to read boundaries", ex);
        }
        return parsedJson;
    }

    public ArrayList<Beacon> parseBeacons() {
        ArrayList<Beacon> beacons = new ArrayList<>();
        try {
            String bc = readJson(BEACONS);
            JSONObject bea = new JSONObject(bc);
            JSONArray bArray = bea.getJSONArray("beacons");
            for (int i = 0; i < bArray.length(); i++) {
                JSONObject object = bArray.getJSONObject(i);
                beacons.add(new Beacon(object.getString("alias"),
                        object.getString("UUID"),
                        object.getString("major"),
                        object.getString("minor"),
                        object.getString("instanceId"),
                        object.getString("room"),
                        object.getString("level"),
                        object.getString("roomName")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return beacons;
    }

    public ArrayList<Geometry> parseOULocations() {
        ArrayList<Geometry> boundsCoordinates = new ArrayList<>();
        try {
            String go = readJson(GEOMETRY);
            JSONObject s = new JSONObject(go);
            JSONArray features = s.getJSONArray("features");
            Log.d(DEBUG_TAG, "Features: " + features);

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                String roomId = "";
                if (properties.has("RoomId")) {
                    roomId = properties.getString("RoomId");
                }

                Geometry geometry = new Geometry(
                        roomId,
                        properties.getString("Class"),
                        properties.getString("ZLevel"),
                        properties.getString("AccessLeve"),
                        properties.getString("VenueId"),
                        properties.getString("VenueName"));

                JSONObject geom = feature.getJSONObject("geometry");
                JSONArray coord = geom.getJSONArray("coordinates");
                ArrayList<LatLng> coordinates = new ArrayList<>();
                for (int l = 0; l < coord.length(); l++) {
                    JSONArray innerArray = coord.getJSONArray(l);
                    for (int j = 0; j < innerArray.length(); j++) {
                        JSONArray innerInnerArray = innerArray.getJSONArray(j);
                        for (int k = 0; k < innerInnerArray.length(); k++) {
                                LatLng latLng = new LatLng(Double.parseDouble(innerInnerArray.getString(1)), Double.parseDouble(innerInnerArray.getString(0)));
                                coordinates.add(latLng);
                                //Log.d(DEBUG_TAG, "Longitude: " + innerInnerArray.getString(0));
                                //Log.d(DEBUG_TAG, "Latitude: " + innerInnerArray.getString(1));

                        }
                    }
                }
                geometry.setCoordinates(coordinates);
                boundsCoordinates.add(geometry);
            }

        } catch (JSONException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return boundsCoordinates;
    }
}
