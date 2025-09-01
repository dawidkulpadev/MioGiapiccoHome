package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.dialogs.AirDataPlotDialog;

public class SectorsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static class SectorViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;
        //TextView humText;
        //TextView tempText;

        RecyclerView plantsRecyclerView;
        PlantsListAdapter plantsListAdapter;

        RecyclerView lightsRecyclerView;
        LightDevicesListAdapter lightsListAdapter;

        Button airParamsButton;
        AirDataPlotDialog.AirDataRequestListener adrListener;
        AirDataPlotDialog airDataPlotDialog;

        Sector sector;

        SectorViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.sector_name_text);
            airParamsButton= v.findViewById(R.id.air_params_button);
            //humText= v.findViewById(R.id.air_hum_text);
            //tempText= v.findViewById(R.id.air_temp_text);

            plantsRecyclerView= v.findViewById(R.id.sector_plants_list);
            RecyclerView.LayoutManager plantsLayoutManager = new LinearLayoutManager(v.getContext());
            plantsRecyclerView.setLayoutManager(plantsLayoutManager);

            lightsRecyclerView= v.findViewById(R.id.sector_lights_list);
            RecyclerView.LayoutManager lightsLayoutManager= new LinearLayoutManager(v.getContext());
            lightsRecyclerView.setLayoutManager(lightsLayoutManager);

            airDataPlotDialog= new AirDataPlotDialog();
        }

        void init(AirDataPlotDialog.AirDataRequestListener adrl, Sector s){
            adrListener= adrl;
            sector= s;
            airParamsButton.setOnClickListener(v->toggleDetails());

            if(s.getAirDevice()==null){
                airParamsButton.setVisibility(View.GONE);
                //humText.setVisibility(View.GONE);
                //tempText.setVisibility(View.GONE);
            } else {
                float hum = (float)s.getAirDevice().getAirHumidity();
                float temp = (float)s.getAirDevice().getAitTemperature();

                //humText.setVisibility(View.VISIBLE);
                //tempText.setVisibility(View.VISIBLE);
                airParamsButton.setVisibility(View.VISIBLE);

                String airParamsText = root.getContext().getString(R.string.value_temp_hum, temp, Math.round(hum));
                airParamsButton.setText(airParamsText);

                //humText.setText(root.getContext().getString(R.string.value_humidity, hum));
                //tempText.setText(root.getContext().getString(R.string.value_temperature, temp));
            }
        }

        void createPlantsListAdapter(ArrayList<Plant> plants, RoomsListAdapter.DataChangeListener dataChangeListener){
            plantsListAdapter= new PlantsListAdapter(root.getContext(), plants, dataChangeListener);
            plantsRecyclerView.setAdapter(plantsListAdapter);
        }

        void createLightsListAdapter(ArrayList<LightDevice> lights, RoomsListAdapter.DataChangeListener dataChangeListener){
            lightsListAdapter= new LightDevicesListAdapter(lights, dataChangeListener);
            lightsRecyclerView.setAdapter(lightsListAdapter);
        }

        void toggleDetails(){
            airDataPlotDialog.show(sector.getAirDevice(), ((AppCompatActivity)root.getContext()).getSupportFragmentManager(),"airdata", adrListener);
        }
    }

    private final Context context;
    final private ArrayList<Sector> sectors;
    private final RoomsListAdapter.DataChangeListener dataChangeListener;
    private final AirDataPlotDialog.AirDataRequestListener adrListener;

    public SectorsListAdapter(Context context, ArrayList<Sector> sectors, RoomsListAdapter.DataChangeListener dataChangeListener, AirDataPlotDialog.AirDataRequestListener adrl){
        this.sectors= sectors;
        this.context= context;
        this.dataChangeListener= dataChangeListener;
        this.adrListener= adrl;
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
        h.init(adrListener, sectors.get(position));

        h.createPlantsListAdapter(sectors.get(position).getPlants(), dataChangeListener);
        h.createLightsListAdapter(sectors.get(position).getLightDevices(), dataChangeListener);
    }

    @Override
    public int getItemCount() {
        return sectors.size();
    }
}
