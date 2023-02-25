package pl.dawidkulpa.miogiapiccohome.adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.API.SensorDevice;

public class PlantsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_PLANT=0;

    public interface DataChangeListener {
        void onPlantNameChanged(Plant p);
        void onLightDataChanged(LightDevice d);
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder{
        View root;

        RelativeLayout ldDetailsBox;
        RelativeLayout sdDetailsBox;

        TextView nameText;
        TextView lastLastSeenText;

        PlantViewHolder(View v){
            super(v);
            root= v;

            ldDetailsBox= v.findViewById(R.id.detailsbox_lightdevice);
            sdDetailsBox= v.findViewById(R.id.detailsbox_sensordevice);

            nameText= v.findViewById(R.id.name_text);
            lastLastSeenText= v.findViewById(R.id.last_last_seen_text);
        }

        private void showLDDetails(){
            ldDetailsBox.setVisibility(View.VISIBLE);
        }

        private void showSDDetails(){
            sdDetailsBox.setVisibility(View.VISIBLE);
        }

        private void hideLDDetails(){
            ldDetailsBox.setVisibility(View.GONE);
        }

        private void hideSDDetails(){
            sdDetailsBox.setVisibility(View.GONE);
        }

        void setLightDeviceDetails(String plantName, LightDevice ld, DataChangeListener dataChangeListener){
            if(ld!=null){
                setLastSeenText(ldDetailsBox.findViewById(R.id.ld_last_seen_text), ld.getLastSeen());

                ((TextView)root.findViewById(R.id.dli_text)).setText(String.format(Locale.getDefault(), "%d%%", ld.getDli()));
                ((TextView)root.findViewById(R.id.sunrise_text)).setText(ld.getStringDs());
                ((TextView)root.findViewById(R.id.sun_text)).setText(ld.getStringSre());
                ((TextView)root.findViewById(R.id.moon_text)).setText(ld.getStringDe());
                ((TextView)root.findViewById(R.id.sunset_text)).setText(ld.getStringSss());

                Calendar now = Calendar.getInstance();
                int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                ImageView stateIcon= root.findViewById(R.id.state_icon);

                if (nowMin < ld.getDs() || nowMin > ld.getDe()) {
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(root.getContext(), R.drawable.moon));
                } else if (nowMin < ld.getSre()) {
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(root.getContext(), R.drawable.sunrise));
                } else if (nowMin < ld.getSss()) {
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(root.getContext(), R.drawable.sun));
                } else {
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(root.getContext(), R.drawable.sunset));
                }

                root.findViewById(R.id.sun_box).setOnClickListener(
                        view -> openTimePicker(LightDevice.FIELD_SRE_ID, ld, dataChangeListener));
                root.findViewById(R.id.sunrise_box).setOnClickListener(
                        view -> openTimePicker(LightDevice.FIELD_DS_ID, ld, dataChangeListener));
                root.findViewById(R.id.sunset_box).setOnClickListener(
                        view -> openTimePicker(LightDevice.FIELD_SSS_ID, ld, dataChangeListener));
                root.findViewById(R.id.moon_box).setOnClickListener(
                        view -> openTimePicker(LightDevice.FIELD_DE_ID, ld, dataChangeListener));

                root.findViewById(R.id.dli_text).setOnClickListener(
                        view -> openChangeDLIDialog(plantName, ld, dataChangeListener));
            }
        }

        void setSensorDeviceDetails(SensorDevice sd){
            if(sd!=null){
                Context context= root.getContext();

                setLastSeenText(sdDetailsBox.findViewById(R.id.sd_last_seen_text), sd.getLastSeen());

                ((TextView)root.findViewById(R.id.at_text)).setText(
                        String.format(Locale.getDefault(), "%d%%", sd.getAirTemperature()));
                ((TextView)root.findViewById(R.id.ah_text)).setText(
                        String.format(Locale.getDefault(), "%d%%", sd.getAirHumidity()));
                ((TextView)root.findViewById(R.id.sh_text)).setText(
                        String.format(Locale.getDefault(), "%d%%", sd.getSoilHumidity()));

                ImageView btryIcon= root.findViewById(R.id.btry_icon);

                if(sd.getBatteryLevel()>80)
                    btryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.battery_5_5));
                else if(sd.getBatteryLevel()>60)
                    btryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.battery_4_5));
                else if(sd.getBatteryLevel()>40)
                    btryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.battery_3_5));
                else if(sd.getBatteryLevel()>20)
                    btryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.battery_2_5));
                else if(sd.getBatteryLevel()>5)
                    btryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.battery_1_5));
                else
                    btryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.battery_0_5));
            }
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

        void setLastLastSeenText(Plant plant){
            if(!plant.hasAnyDevice()){
                lastLastSeenText.setText(R.string.info_no_devices_bind);
            } else {
                setLastSeenText(lastLastSeenText, plant.getLastLastSeen());
            }
        }

        void openTimePicker(final int field, final LightDevice d, DataChangeListener dataChangeListener){
            TimePickerDialog tpd= new TimePickerDialog(root.getContext(), (timePicker, hours, mins) -> {
                int t= hours*60+mins;

                d.setTimeOf(field, t);
                dataChangeListener.onLightDataChanged(d);
            }, d.getTimeOf(field)/60, d.getTimeOf(field)%60, true);
            tpd.show();
        }

        void openChangeDLIDialog(String plantName, final LightDevice device, DataChangeListener dataChangeListener){
            DLIPickerDialog dialog= new DLIPickerDialog(device, plantName, (d, v) -> {
                d.setDli(v);
                dataChangeListener.onLightDataChanged(d);
            });

            dialog.show(root.getContext());
        }
    }

    private final Context context;
    final private ArrayList<Plant> plants;
    final private DataChangeListener dataChangeListener;

    public PlantsListAdapter(Context context, ArrayList<Plant> plants, DataChangeListener dataChangeListener){
        this.plants= plants;
        this.dataChangeListener = dataChangeListener;
        this.context= context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_plant, parent, false);
        return new PlantViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PlantViewHolder h= ((PlantViewHolder) holder);
        h.nameText.setText(plants.get(position).getName());
        h.nameText.setOnClickListener(view -> openChangeNameDialog(plants.get(holder.getAdapterPosition())));

        h.setLastLastSeenText(plants.get(position));
        h.setLightDeviceDetails(plants.get(position).getName(),
                plants.get(position).getLightDevice(), dataChangeListener);
        h.setSensorDeviceDetails(plants.get(position).getSensorDevice());

        if(plants.get(position).isShowingDetails()){
            if(plants.get(position).getLightDevice()!=null){
                h.showLDDetails();
            } else {
                h.hideLDDetails();
            }
            if(plants.get(position).getSensorDevice()!=null){
                h.showSDDetails();
            } else {
                h.hideSDDetails();
            }
        } else {
            h.hideLDDetails();
            h.hideSDDetails();
        }

        h.root.setOnClickListener(view -> {
            plants.get(h.getAdapterPosition()).toggleShowingDetails();
            notifyItemChanged(h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_PLANT;
    }

    public void openChangeNameDialog(Plant plant){
        NamePickerDialog dialog= new NamePickerDialog(plant, (p, v) -> {
            p.setName(v);
            dataChangeListener.onPlantNameChanged(p);
        });

        dialog.show(context);
    }
}
