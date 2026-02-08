package pl.dawidkulpa.miogiapiccohome.API.requests;

public class SectorCreateRequest {
    String name;
    int room_id;
    public SectorCreateRequest(String n, int i) {name =n; room_id=i; }
}
