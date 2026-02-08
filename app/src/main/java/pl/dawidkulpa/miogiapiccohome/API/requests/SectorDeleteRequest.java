package pl.dawidkulpa.miogiapiccohome.API.requests;

public class SectorDeleteRequest {
    int sector_id;
    int room_id;
    public SectorDeleteRequest(int r, int s) { sector_id = s; room_id=r; }
}
