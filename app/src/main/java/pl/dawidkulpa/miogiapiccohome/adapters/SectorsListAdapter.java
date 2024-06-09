package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.R;

public class SectorsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static class SectorViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;
        TextView humText;
        TextView tempText;

        RecyclerView plantsRecyclerView;
        PlantsListAdapter plantsListAdapter;

        RecyclerView lightsRecyclerView;
        LightDevicesListAdapter lightsListAdapter;

        SectorViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.sector_name_text);
            humText= v.findViewById(R.id.sector_hum_text);
            tempText= v.findViewById(R.id.sector_temp_text);

            plantsRecyclerView= v.findViewById(R.id.sector_plants_list);
            RecyclerView.LayoutManager plantsLayoutManager = new LinearLayoutManager(v.getContext());
            plantsRecyclerView.setLayoutManager(plantsLayoutManager);

            lightsRecyclerView= v.findViewById(R.id.sector_lights_list);
            RecyclerView.LayoutManager lightsLayoutManager= new LinearLayoutManager(v.getContext());
            lightsRecyclerView.setLayoutManager(lightsLayoutManager);
        }

        void createPlantsListAdapter(ArrayList<Plant> plants, RoomsListAdapter.DataChangeListener dataChangeListener){
            plantsListAdapter= new PlantsListAdapter(root.getContext(), plants, dataChangeListener);
            plantsRecyclerView.setAdapter(plantsListAdapter);
        }

        void createLightsListAdapter(ArrayList<LightDevice> lights, RoomsListAdapter.DataChangeListener dataChangeListener){
            lightsListAdapter= new LightDevicesListAdapter(root.getContext(), lights, dataChangeListener);
            lightsRecyclerView.setAdapter(lightsListAdapter);
        }
    }

    private final Context context;
    final private ArrayList<Sector> sectors;
    private RoomsListAdapter.DataChangeListener dataChangeListener;

    public SectorsListAdapter(Context context, ArrayList<Sector> sectors, RoomsListAdapter.DataChangeListener dataChangeListener){
        this.sectors= sectors;
        this.context= context;
        this.dataChangeListener= dataChangeListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_sector, parent, false);
        return new SectorViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SectorViewHolder h= ((SectorViewHolder) holder);
        h.nameText.setText(sectors.get(position).getName());

        if(sectors.get(position).getAirDevices().isEmpty()){
            h.humText.setVisibility(View.GONE);
            h.tempText.setVisibility(View.GONE);
        } else {
            float hum = 0;
            float temp = 0;
            for (AirDevice a : sectors.get(position).getAirDevices()) {
                hum += a.getAirHumidity();
                temp += a.getAitTemperature();
            }

            hum = hum / sectors.get(position).getAirDevices().size();
            temp = temp / sectors.get(position).getAirDevices().size();

            h.humText.setVisibility(View.VISIBLE);
            h.tempText.setVisibility(View.VISIBLE);

            h.humText.setText(context.getString(R.string.value_humidity, hum));
            h.tempText.setText(context.getString(R.string.value_temperature, temp));
        }

        h.createPlantsListAdapter(sectors.get(position).getPlants(), dataChangeListener);
        h.createLightsListAdapter(sectors.get(position).getLightDevices(), dataChangeListener);
    }

    @Override
    public int getItemCount() {
        Log.e("Sectors cnt", String.valueOf(sectors.size()));
        return sectors.size();
    }
}
