package pl.dawidkulpa.miogiapiccohome.API.requests;

public class UnregisterDeviceRequest {
    String dev_id;
    int dev_type;

    public UnregisterDeviceRequest(String devId, int devType){
        dev_id= devId;
        dev_type= devType;
    }
}
