package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.R;

public class PlantsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static class PlantViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;
        TextView soilDevHumText;
        ImageView stateIcon;

        PlantViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.plant_name_text);
            soilDevHumText= v.findViewById(R.id.plant_soil_dev_hum_text);
            stateIcon= v.findViewById(R.id.plant_state_icon);
        }

        void createUI(Plant p, RoomsListAdapter.DataChangeListener dataChangeListener){


        }
    }

    private final Context context;
    final private ArrayList<Plant> plants;
    final private RoomsListAdapter.DataChangeListener dataChangeListener;

    public PlantsListAdapter(Context context, ArrayList<Plant> plants,
                                   RoomsListAdapter.DataChangeListener dataChangeListener){
        this.plants= plants;
        this.dataChangeListener = dataChangeListener;
        this.context= context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_plant, parent, false);
        return new PlantViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PlantViewHolder h= ((PlantViewHolder) holder);
        h.nameText.setText(plants.get(position).getName());

        if(plants.get(position).getSoilDevice()!=null) {
            h.soilDevHumText.setVisibility(View.VISIBLE);
            h.soilDevHumText.setText(String.valueOf(plants.get(position).getSoilDevice().getSoilHumidity()));
        } else {
            h.soilDevHumText.setVisibility(View.GONE);
        }

        h.createUI(plants.get(position), dataChangeListener);
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }
}
