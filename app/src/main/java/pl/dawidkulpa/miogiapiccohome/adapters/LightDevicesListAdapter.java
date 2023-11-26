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
import java.util.Calendar;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;

public class LightDevicesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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

            // Set state icon and next change time text
            Calendar cn= Calendar.getInstance();
            int nowMins= cn.get(Calendar.HOUR_OF_DAY) * 60 + cn.get(Calendar.MINUTE);

            if(nowMins>ld.getDe()){
                setStateIconAndChangeText(R.drawable.moon, ld.getStringDs());
            } else if(nowMins>ld.getSss()){
                setStateIconAndChangeText(R.drawable.sunset, ld.getStringDe());
            } else if(nowMins>ld.getSre()){
                setStateIconAndChangeText(R.drawable.sun, ld.getStringSss());
            } else if(nowMins>ld.getDs()) {
                setStateIconAndChangeText(R.drawable.sunrise, ld.getStringSre());
            } else {
                setStateIconAndChangeText(R.drawable.moon, ld.getStringDs());
            }

            root.setOnClickListener(v -> {

            });
        }

        private void setStateIconAndChangeText(int iconRes, String timeText){
            stateIcon.setImageResource(iconRes);
            changeTimeText.setText(root.getContext().getString(R.string.info_light_state_change, timeText));
        }
    }

    private final Context context;
    final private ArrayList<LightDevice> lightDevices;
    final private RoomsListAdapter.DataChangeListener dataChangeListener;

    public LightDevicesListAdapter(Context context, ArrayList<LightDevice> lightDevices,
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
