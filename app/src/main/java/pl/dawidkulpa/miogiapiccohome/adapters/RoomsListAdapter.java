package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.R;

import pl.dawidkulpa.miogiapiccohome.API.Room;

public class RoomsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface DataChangeListener {
        void onLightDeviceDataChanged(LightDevice d);
        void onSoilDeviceDataChanged(SoilDevice d);
        void onAirDeviceDataChanged(AirDevice d);
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;
        TextView humText;
        TextView tempText;
        TextView co2Text;

        RecyclerView sectorsRecyclerView;
        SectorsListAdapter sectorsListAdapter;

        RoomViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.room_name_text);
            humText= v.findViewById(R.id.air_hum_text);
            tempText= v.findViewById(R.id.air_temp_text);
            co2Text= v.findViewById(R.id.air_co2ppm_text);

            sectorsRecyclerView= v.findViewById(R.id.room_sectors_list);
            RecyclerView.LayoutManager layoutManager;
            layoutManager = new LinearLayoutManager(v.getContext());
            sectorsRecyclerView.setLayoutManager(layoutManager);
        }

        void createSectorsListAdapter(ArrayList<Sector> sectors, DataChangeListener dataChangeListener){
            sectorsListAdapter= new SectorsListAdapter(root.getContext(), sectors, dataChangeListener);
            sectorsRecyclerView.setAdapter(sectorsListAdapter);
        }
    }

    private final Context context;
    private ArrayList<Room> rooms;
    private DataChangeListener dataChangeListener;

    public RoomsListAdapter(Context context, ArrayList<Room> rooms, DataChangeListener dataChangeListener){
        this.rooms= rooms;
        this.context= context;
        this.dataChangeListener= dataChangeListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_room, parent, false);
        return new RoomViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RoomViewHolder h= ((RoomViewHolder) holder);
        h.nameText.setText(rooms.get(position).getName());
        h.createSectorsListAdapter(rooms.get(position).getSectors(), dataChangeListener);

        if(!rooms.get(position).getAirDevices().isEmpty()) {
            float hum = 0;
            float temp = 0;
            int co2= 0;
            for (AirDevice a : rooms.get(position).getAirDevices()) {
                hum += a.getAirHumidity();
                temp += a.getAitTemperature();
                co2 += a.getCo2ppm();
            }

            co2 = co2 / rooms.get(position).getAirDevices().size();
            hum = hum / rooms.get(position).getAirDevices().size();
            temp = temp / rooms.get(position).getAirDevices().size();

            h.humText.setVisibility(View.VISIBLE);
            h.tempText.setVisibility(View.VISIBLE);
            // Check if co2 level is valid
            if(co2!=0)
                h.co2Text.setVisibility(View.VISIBLE);
            else
                h.co2Text.setVisibility(View.GONE);
            h.co2Text.setText(context.getString(R.string.value_co2_ppm, co2));
            h.humText.setText(context.getString(R.string.value_humidity, hum));
            h.tempText.setText(context.getString(R.string.value_temperature, temp));
        } else {
            h.humText.setVisibility(View.GONE);
            h.tempText.setVisibility(View.GONE);
            h.co2Text.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void updateList(ArrayList<Room> newRooms){
        rooms= newRooms;
    }

}
