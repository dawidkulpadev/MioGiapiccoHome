package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        TextView lastSeenText;
        TextView soilDevIdText;
        TextView soilDevHumText;

        PlantViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.plant_name_text);
            lastSeenText= v.findViewById(R.id.plant_lastseen_text);

            soilDevIdText= v.findViewById(R.id.plant_soil_dev_id_text);
            soilDevHumText= v.findViewById(R.id.plant_soil_dev_hum_text);
        }


        void setLastSeenText(int t){
            if (t == 0) {
                lastSeenText.setText(R.string.info_seconds_ago);
            } else if (t < 60) {
                lastSeenText.setText(root.getContext().getString(R.string.info_min_ago, t));
            } else if (t < 60*24){
                lastSeenText.setText(root.getContext().getString(R.string.info_hours_ago, t/60));
            } else if (t < 60*24*30) {
                lastSeenText.setText(root.getContext().getString(R.string.info_days_ago, t/(60*24)));
            } else {
                lastSeenText.setText(R.string.info_seen_long_ago);
            }
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
            h.setLastSeenText(plants.get(position).getSoilDevice().getLastSeen());
            h.soilDevIdText.setText(plants.get(position).getSoilDevice().getId());
            h.soilDevHumText.setText(String.valueOf(plants.get(position).getSoilDevice().getSoilHumidity()));
        } else {
            h.lastSeenText.setText("");
            h.soilDevIdText.setText(R.string.info_no_soil_device_assigned);
            h.soilDevHumText.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }
}
