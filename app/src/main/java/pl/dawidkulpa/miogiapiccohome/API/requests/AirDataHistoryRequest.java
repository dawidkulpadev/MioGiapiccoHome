package pl.dawidkulpa.miogiapiccohome.API.requests;

public class AirDataHistoryRequest {
    String dev_id;
    String hs;
    String he;

    public AirDataHistoryRequest(String devId, String historyStart, String historyEnd){
        dev_id= devId;
        hs= historyStart;
        he= historyEnd;
    }
}
