package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.R;

import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.dialogs.NewSectorDialog;

public class RoomsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface DataChangeListener {
        void onLightDeviceDataChanged(LightDevice d);
        void onDeviceUpdateClick(LightDevice d);
        void onDeviceDeleteClick(LightDevice d);
        void onSoilDeviceDataChanged(SoilDevice d);
        void onAirDeviceDataChanged(AirDevice d);

        void onPlantDataChanged(Plant p);
        void onRoomDataChanged(Room r);
        void onSectorDataChanged(Sector s);
    }

    public interface DataRequestListener {
        void onShowAirDataHistoryClick(Room r);
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;
        TextView humText;
        TextView tempText;

        RecyclerView sectorsRecyclerView;
        SectorsListAdapter sectorsListAdapter;

        NewSectorDialog newSectorDialog;
        Button newSectorButton;

        ConstraintLayout airParamsBox;

        RecyclerView.LayoutManager layoutManager;

        RoomViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.room_name_text);
            humText= v.findViewById(R.id.air_hum_text);
            tempText= v.findViewById(R.id.air_temp_text);

            airParamsBox= v.findViewById(R.id.air_params_box);

            sectorsRecyclerView= v.findViewById(R.id.room_sectors_list);

            layoutManager = new LinearLayoutManager(v.getContext());
            sectorsRecyclerView.setLayoutManager(layoutManager);

            newSectorButton= v.findViewById(R.id.new_sector_button);
        }

        void createSectorsListAdapter(ArrayList<Sector> sectors, DataChangeListener dataChangeListener){
            sectorsListAdapter= new SectorsListAdapter(root.getContext(), sectors, dataChangeListener);
            sectorsRecyclerView.setAdapter(sectorsListAdapter);
        }
    }

    private final Context context;
    private ArrayList<Room> rooms;
    private DataChangeListener dataChangeListener;
    private DataRequestListener dataRequestListener;
    private NewSectorDialog.ClosedListener apiCreateSectorRequest;

    public RoomsListAdapter(Context context, ArrayList<Room> rooms, DataChangeListener dataChangeListener, DataRequestListener dataRequestListener, NewSectorDialog.ClosedListener apiCreateSectorRequest){
        this.rooms= rooms;
        this.context= context;
        this.dataChangeListener= dataChangeListener;
        this.dataRequestListener= dataRequestListener;
        this.apiCreateSectorRequest= apiCreateSectorRequest;
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
            for (AirDevice a : rooms.get(position).getAirDevices()) {
                hum += (float) a.getAirHumidity();
                temp += (float) a.getAitTemperature();
            }

            hum = hum / rooms.get(position).getAirDevices().size();
            temp = temp / rooms.get(position).getAirDevices().size();

            h.humText.setVisibility(View.VISIBLE);
            h.tempText.setVisibility(View.VISIBLE);

            h.humText.setText(context.getString(R.string.value_humidity, hum));
            h.tempText.setText(context.getString(R.string.value_temperature, temp));

            h.airParamsBox.setOnClickListener(v -> {
                if(!rooms.get(position).getAirDevices().isEmpty())
                    dataRequestListener.onShowAirDataHistoryClick(rooms.get(position));
            });

        } else {
            h.humText.setVisibility(View.GONE);
            h.tempText.setVisibility(View.GONE);
        }

        h.newSectorDialog= new NewSectorDialog(rooms.get(position).getId(), context, apiCreateSectorRequest);
        h.newSectorButton.setOnClickListener(v -> {
            h.newSectorDialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void updateList(ArrayList<Room> newRooms){
        rooms= newRooms;
    }

}
