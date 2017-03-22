package dk.ubicomp.positioning;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Jesper on 21/03/2017.
 */

public class Geometry {

    private String roomId;
    private String klass;
    private String Zlevel;
    private String accessLevel;
    private String venueId;
    private String venueName;
    private ArrayList<LatLng> coordinates;

    public Geometry(String roomId, String klass, String zlevel, String accessLevel, String venueId, String venueName) {
        this.roomId = roomId;
        this.klass = klass;
        Zlevel = zlevel;
        this.accessLevel = accessLevel;
        this.venueId = venueId;
        this.venueName = venueName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }

    public String getZlevel() {
        return Zlevel;
    }

    public void setZlevel(String zlevel) {
        Zlevel = zlevel;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public ArrayList<LatLng> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ArrayList<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        return "Geometry{" +
                "roomId='" + roomId + '\'' +
                ", klass='" + klass + '\'' +
                ", Zlevel='" + Zlevel + '\'' +
                ", accessLevel='" + accessLevel + '\'' +
                ", venueId='" + venueId + '\'' +
                ", venueName='" + venueName + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
