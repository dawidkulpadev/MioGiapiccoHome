package pl.dawidkulpa.miogiapiccohome.adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;

public class LightDevicesListAdapterDetails extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static class LightDeviceViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;

        TextView changeTimeText;
        ImageView stateIcon;

        TextView dliText;

        LightDeviceViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.ld_name_text);

            stateIcon= v.findViewById(R.id.ld_state_icon);
            changeTimeText= v.findViewById(R.id.ld_change_time_text);

            dliText= v.findViewById(R.id.dli_text);
        }

        void setLightDeviceDetails(LightDevice ld, RoomsListAdapter.DataChangeListener dataChangeListener){
            nameText.setText(String.valueOf(ld.getName()));

            dliText.setText(String.valueOf(ld.getDli()));

            root.findViewById(R.id.sun_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_SRE_ID, ld, dataChangeListener));
            root.findViewById(R.id.sunrise_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_DS_ID, ld, dataChangeListener));
            root.findViewById(R.id.sunset_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_SSS_ID, ld, dataChangeListener));
            root.findViewById(R.id.moon_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_DE_ID, ld, dataChangeListener));

            root.findViewById(R.id.dli_text).setOnClickListener(
                    view -> openChangeDLIDialog(ld, dataChangeListener));
        }

        void setLastSeenText(TextView v, int t){
            if (t == 0) {
                v.setText(R.string.info_seconds_ago);
            } else if (t < 60) {
                v.setText(root.getContext().getString(R.string.info_min_ago, t));
            } else if (t < 60*24){
                v.setText(root.getContext().getString(R.string.info_hours_ago, t/60));
            } else if (t < 60*24*30) {
                v.setText(root.getContext().getString(R.string.info_days_ago, t/(60*24)));
            } else {
                v.setText(R.string.info_seen_long_ago);
            }
        }

        void openTimePicker(final int field, final LightDevice d, RoomsListAdapter.DataChangeListener dataChangeListener){
            TimePickerDialog tpd= new TimePickerDialog(root.getContext(), (timePicker, hours, mins) -> {
                int t= hours*60+mins;

                d.setTimeOf(field, t);
                dataChangeListener.onLightDeviceDataChanged(d);
            }, d.getTimeOf(field)/60, d.getTimeOf(field)%60, true);
            tpd.show();
        }

        void openChangeDLIDialog(final LightDevice device, RoomsListAdapter.DataChangeListener dataChangeListener){
            DLIPickerDialog dialog= new DLIPickerDialog(device, "plantName", (d, v) -> {
                d.setDli(v);
                dataChangeListener.onLightDeviceDataChanged(d);
            });

            dialog.show(root.getContext());
        }
    }

    private final Context context;
    final private ArrayList<LightDevice> lightDevices;
    final private RoomsListAdapter.DataChangeListener dataChangeListener;

    public LightDevicesListAdapterDetails(Context context, ArrayList<LightDevice> lightDevices,
                                          RoomsListAdapter.DataChangeListener dataChangeListener){
        this.lightDevices= lightDevices;
        this.dataChangeListener = dataChangeListener;
        this.context= context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_light_device, parent, false);
        return new LightDeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LightDeviceViewHolder h= ((LightDeviceViewHolder) holder);
        h.setLightDeviceDetails(lightDevices.get(position), dataChangeListener);
    }

    @Override
    public int getItemCount() {
        return lightDevices.size();
    }
}
