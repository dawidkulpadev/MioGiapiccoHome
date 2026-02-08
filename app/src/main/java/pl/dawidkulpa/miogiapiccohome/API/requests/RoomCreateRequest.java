package pl.dawidkulpa.miogiapiccohome.API.requests;

public class RoomCreateRequest {
    String name;
    int humidity_target;
    public RoomCreateRequest(String n) { name = n; }
}
