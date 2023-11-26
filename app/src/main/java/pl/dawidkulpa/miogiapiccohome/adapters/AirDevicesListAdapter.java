package pl.dawidkulpa.miogiapiccohome.adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;

public class AirDevicesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static class AirDeviceViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;
        TextView lastSeenText;

        TextView airHumText;
        TextView airTempText;
        TextView hwlText;

        AirDeviceViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.ad_name_text);
            lastSeenText= v.findViewById(R.id.ad_last_seen_text);

            airHumText= v.findViewById(R.id.ad_hum_text);
            airTempText= v.findViewById(R.id.ad_temp_text);
            hwlText= v.findViewById(R.id.ad_hwl_text);
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
    final private ArrayList<AirDevice> airDevices;
    final private RoomsListAdapter.DataChangeListener dataChangeListener;

    public AirDevicesListAdapter(Context context, ArrayList<AirDevice> airDevices,
                                   RoomsListAdapter.DataChangeListener dataChangeListener){
        this.airDevices= airDevices;
        this.dataChangeListener = dataChangeListener;
        this.context= context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_air_device, parent, false);
        return new AirDeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AirDeviceViewHolder h= ((AirDeviceViewHolder) holder);
        h.nameText.setText(airDevices.get(position).getName());
        h.setLastSeenText(airDevices.get(position).getLastSeen());
        h.airHumText.setText(String.valueOf(airDevices.get(position).getAirHumidity()));
        h.airTempText.setText(String.valueOf(airDevices.get(position).getAitTemperature()));
        h.hwlText.setText(String.valueOf(airDevices.get(position).getHumidWaterLvl()));
    }

    @Override
    public int getItemCount() {
        return airDevices.size();
    }
}
