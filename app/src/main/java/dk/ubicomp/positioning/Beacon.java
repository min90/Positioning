package dk.ubicomp.positioning;

/**
 * Created by Jesper on 21/03/2017.
 */

public class Beacon {

    private String alias;
    private String uuid;
    private String major;
    private String minor;
    private String instanceID;
    private String roomId;
    private String level;
    private String roomName;

    public Beacon(String alias, String uuid, String major, String minor, String instanceID, String roomId, String level, String roomName) {
        this.alias = alias;
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.instanceID = instanceID;
        this.roomId = roomId;
        this.level = level;
        this.roomName = roomName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "Beacon{" +
                "alias='" + alias + '\'' +
                ", uuid='" + uuid + '\'' +
                ", major='" + major + '\'' +
                ", minor='" + minor + '\'' +
                ", instanceID='" + instanceID + '\'' +
                ", roomId='" + roomId + '\'' +
                ", level='" + level + '\'' +
                ", roomName='" + roomName + '\'' +
                '}';
    }
}
