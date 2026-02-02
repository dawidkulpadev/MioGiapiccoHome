package pl.dawidkulpa.miogiapiccohome.API.requests;

public class RegisterDeviceRequest {
    String dev_id;
    int dev_type;
    int room_id;
    int sector_id;
    int plant_id;
    String dev_name;

    public RegisterDeviceRequest(String devId, int devType, int roomId, int sectorId, int plantId, String devName){
        dev_id= devId;
        dev_type= devType;
        room_id= roomId;
        sector_id= sectorId;
        plant_id= plantId;
        dev_name= devName;
    }
}
