package pl.dawidkulpa.miogiapiccohome.adapters;

import android.animation.ValueAnimator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDivider;

import java.util.ArrayList;
import java.util.Calendar;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.dialogs.DLIPickerDialog;
import pl.dawidkulpa.miogiapiccohome.dialogs.LightDeviceSettingsDialog;

public class LightDevicesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface DataChangeListener{
        void onDeviceDataChanged(LightDevice d);
        void onDeviceDeleteClick(Device d);
        void onDeviceUpdateEnableClick(Device d);
        ArrayList<Room> requestRoomsList();
    }

    static class LightDeviceViewHolder extends RecyclerView.ViewHolder{
        View root;

        // Main box data
        TextView nameText;
        TextView changeTimeText;
        ImageView stateIcon;

        // Config box data
        ConstraintLayout configsBox;
        TextView configsDliText;
        TextView configsDsText;
        TextView configsDeText;
        TextView configsSreText;
        TextView configsSssText;
        MaterialDivider divider;
        Button settingsButton;

        LightDeviceSettingsDialog lightDeviceSettingsDialog;

        LightDeviceViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.ld_name_text);
            stateIcon= v.findViewById(R.id.ld_state_icon);
            changeTimeText= v.findViewById(R.id.ld_change_time_text);

            configsBox= v.findViewById(R.id.ld_config_box);
            configsDsText= v.findViewById(R.id.ld_ds_text);
            configsSreText= v.findViewById(R.id.ld_sre_text);
            configsSssText= v.findViewById(R.id.ld_sss_text);
            configsDeText= v.findViewById(R.id.ld_de_text);
            configsDliText= v.findViewById(R.id.ld_dli_text);

            settingsButton= v.findViewById(R.id.ld_settings_button);

            divider= v.findViewById(R.id.divider);
            lightDeviceSettingsDialog= new LightDeviceSettingsDialog();
        }

        void setLightDeviceDetails(LightDevice ld, DataChangeListener dataChangeListener){
            nameText.setText(String.valueOf(ld.getName()));
            configsDliText.setText(root.getContext().getString(R.string.value_dli, ld.getDli()));
            configsDsText.setText(ld.getStringDs());
            configsSreText.setText(ld.getStringSre());
            configsSssText.setText(ld.getStringSss());
            configsDeText.setText(ld.getStringDe());

            if(ld.isUpdateAvailable()){
                root.findViewById(R.id.ld_update_available_icon).setVisibility(View.VISIBLE);
            }

            setStateData(ld);
            setConfigsUI(ld, dataChangeListener);

            root.setOnClickListener(v -> {
                if(configsBox.getVisibility()==View.GONE) {
                    configsBox.setVisibility(View.VISIBLE);
                } else {
                    configsBox.setVisibility(View.GONE);
                }

                int prevHeight = v.getHeight();
                v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int targetHeight = v.getMeasuredHeight();

                ValueAnimator animator = ValueAnimator.ofInt(prevHeight, targetHeight);
                animator.addUpdateListener(animation -> {
                    ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                    layoutParams.height = (int) animation.getAnimatedValue();
                    v.setLayoutParams(layoutParams);
                });
                animator.setDuration(400); // Czas trwania animacji
                animator.start();
            });
        }

        private void setStateData(LightDevice ld){
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
        }

        private void setConfigsUI(LightDevice ld, DataChangeListener dataChangeListener){
            root.findViewById(R.id.ld_sre_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_SRE_ID, ld, dataChangeListener));
            root.findViewById(R.id.ld_ds_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_DS_ID, ld, dataChangeListener));
            root.findViewById(R.id.ld_sss_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_SSS_ID, ld, dataChangeListener));
            root.findViewById(R.id.ld_de_box).setOnClickListener(
                    view -> openTimePicker(LightDevice.FIELD_DE_ID, ld, dataChangeListener));

            root.findViewById(R.id.ld_dli_text).setOnClickListener(
                    view -> openChangeDLIDialog(ld, dataChangeListener));

            settingsButton.setOnClickListener(v -> {
                lightDeviceSettingsDialog.show(ld, dataChangeListener.requestRoomsList(),
                        ((AppCompatActivity) root.getContext()).getSupportFragmentManager(),
                        "light_settings",
                        new LightDeviceSettingsDialog.SettingsActions() {
                            @Override
                            public void onClose(LightDevice device, boolean write) {
                                if(write)
                                    dataChangeListener.onDeviceDataChanged(device);
                            }

                            @Override
                            public void onDeleteClick(LightDevice device) {
                                dataChangeListener.onDeviceDeleteClick(device);
                            }

                            @Override
                            public void onUpdateAllowedChange(LightDevice device) {
                                dataChangeListener.onDeviceUpdateEnableClick(device);
                            }
                        });
            });
        }

        private void setStateIconAndChangeText(int iconRes, String timeText){
            stateIcon.setImageResource(iconRes);
            changeTimeText.setText(root.getContext().getString(R.string.info_light_state_change, timeText));
        }

        void openTimePicker(final int field, final LightDevice d, DataChangeListener dataChangeListener){
            TimePickerDialog tpd= new TimePickerDialog(root.getContext(), (timePicker, hours, mins) -> {
                int t= hours*60+mins;

                d.setTimeOf(field, t);
                dataChangeListener.onDeviceDataChanged(d);
            }, d.getTimeOf(field)/60, d.getTimeOf(field)%60, true);
            tpd.show();
        }

        void openChangeDLIDialog(final LightDevice device, DataChangeListener dataChangeListener){
            DLIPickerDialog dialog= new DLIPickerDialog(device, (d, v) -> {
                d.setDli(v);
                dataChangeListener.onDeviceDataChanged(d);
            });

            dialog.show(root.getContext());
        }
    }

    final private ArrayList<LightDevice> lightDevices;
    final private DataChangeListener dcl;


    public LightDevicesListAdapter(ArrayList<LightDevice> lightDevices,
                                   DataChangeListener dataChangeListener){
        this.lightDevices= lightDevices;
        this.dcl = dataChangeListener;
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
        h.setLightDeviceDetails(lightDevices.get(position), dcl);

        if(position==lightDevices.size()-1){
            h.divider.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lightDevices.size();
    }
}
