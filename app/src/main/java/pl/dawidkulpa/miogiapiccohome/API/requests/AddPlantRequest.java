package pl.dawidkulpa.miogiapiccohome.API.requests;

public class AddPlantRequest {
    String plant_name;
    int sector_id;

    public AddPlantRequest(String plantName, int sectorId){
        plant_name= plantName;
        sectorId= sector_id;
    }
}
