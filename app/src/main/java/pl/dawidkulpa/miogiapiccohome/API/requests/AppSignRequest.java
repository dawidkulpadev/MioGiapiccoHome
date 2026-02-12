package pl.dawidkulpa.miogiapiccohome.API.requests;

public class AppSignRequest {
    String mac;
    String pub;

    public AppSignRequest(String macBase64, String pubBase64){
        mac= macBase64;
        pub= pubBase64;
    }
}
