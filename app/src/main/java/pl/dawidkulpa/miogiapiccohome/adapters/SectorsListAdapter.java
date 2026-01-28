package pl.dawidkulpa.miogiapiccohome.adapters;

import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.EditTextWatcher;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.dialogs.AirDataPlotDialog;

public class SectorsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface DataChangeListener {
        void onDeviceUpdateEnableClick(Device d);
        void onDeviceDeleteClick(Device d);

        void onLightDeviceDataChanged(LightDevice d);
        void onSoilDeviceDataChanged(SoilDevice d);
        void onAirDeviceDataChanged(AirDevice d);
        void onPlantDataChanged(Plant p);

        void onSectorNameChanged(Sector s, String newName);
        void onSectorDeleteClick(Sector s);

        ArrayList<Room> requestRoomsList();
    }

    static class SectorViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;

        RecyclerView plantsRecyclerView;
        PlantsListAdapter plantsListAdapter;

        RecyclerView lightsRecyclerView;
        LightDevicesListAdapter lightsListAdapter;

        Button airParamsChartButton;
        TextView airHumLabelText;
        TextView airHumValueText;
        TextView airTempLabelText;
        TextView airTempValueText;
        Button sectorMoreButton;
        AirDataPlotDialog.AirDataRequestListener adrListener;
        AirDataPlotDialog airDataPlotDialog;
        DataChangeListener dcl;

        Sector sector;

        SectorViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.sector_name_text);
            airParamsChartButton = v.findViewById(R.id.air_chart_button);
            airHumLabelText= v.findViewById(R.id.air_humidity_label);
            airHumValueText= v.findViewById(R.id.air_humidity_value_text);
            airTempLabelText= v.findViewById(R.id.air_temperature_label);
            airTempValueText= v.findViewById(R.id.air_temperature_value_text);

            plantsRecyclerView= v.findViewById(R.id.sector_plants_list);
            RecyclerView.LayoutManager plantsLayoutManager = new LinearLayoutManager(v.getContext());
            plantsRecyclerView.setLayoutManager(plantsLayoutManager);

            lightsRecyclerView= v.findViewById(R.id.sector_lights_list);
            RecyclerView.LayoutManager lightsLayoutManager= new LinearLayoutManager(v.getContext());
            lightsRecyclerView.setLayoutManager(lightsLayoutManager);

            sectorMoreButton= root.findViewById(R.id.sector_more_button);

            airDataPlotDialog= new AirDataPlotDialog();
        }

        void init(AirDataPlotDialog.AirDataRequestListener adrl, Sector s, DataChangeListener dataChangeListener){
            adrListener= adrl;
            sector= s;
            airParamsChartButton.setOnClickListener(v->toggleDetails());
            dcl= dataChangeListener;

            if(s.getAirDevice()==null){
                root.findViewById(R.id.air_device_box).setVisibility(View.GONE);
            } else {
                float hum = (float)s.getAirDevice().getAirHumidity();
                float temp = (float)s.getAirDevice().getAitTemperature();

                root.findViewById(R.id.air_device_box).setVisibility(View.VISIBLE);

                airTempValueText.setText(root.getContext().getString(R.string.value_temperature, temp));
                airHumValueText.setText(root.getContext().getString(R.string.value_humidity, hum));
            }

            sectorMoreButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(root.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.room_menu, popup.getMenu());
                popup.setGravity(Gravity.END);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popup.setForceShowIcon(true);
                }
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_remove) {
                        onSectorDeleteClick();
                        return true;
                    } else if(item.getItemId() == R.id.action_rename){
                        onSectorRenameClick();
                        return true;
                    }

                    return false;
                });
                popup.show();
            });
        }

        void onSectorDeleteClick(){
            if(sector.getLightDevices().isEmpty() && sector.getAirDevice()==null && sector.getPlants().isEmpty()) {
                MaterialAlertDialogBuilder madb = new MaterialAlertDialogBuilder(root.getContext());

                madb.setIcon(R.drawable.icon_remove);
                String title = root.getContext().getString(R.string.title_remove_room, sector.getName());
                madb.setTitle(title);
                madb.setMessage(R.string.message_remove_room);
                madb.setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    dcl.onSectorDeleteClick(sector);
                });

                madb.setNegativeButton(R.string.button_cancel, ((dialog1, which) -> {

                }));

                madb.show();
            } else {
                Snackbar.make(nameText, R.string.message_on_room_remove_not_empty, BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        }

        void onSectorRenameClick(){
            MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(root.getContext());

            madb.setIcon(R.drawable.icon_edit);
            String title= root.getContext().getString(R.string.title_rename_sector, sector.getName());
            madb.setTitle(title);
            madb.setView(R.layout.dialog_change_name);
            madb.setPositiveButton(R.string.button_set, (dialog, which) -> {
                TextInputEditText tiet= ((AlertDialog)dialog).findViewById(R.id.text_input);

                if(tiet!=null && tiet.getText()!=null) {
                    dcl.onSectorNameChanged(sector, tiet.getText().toString());
                } else {
                    Snackbar.make(((AlertDialog) dialog).getButton(which),R.string.message_ui_error, Snackbar.LENGTH_SHORT).show();
                }
            });

            madb.setNegativeButton(R.string.button_cancel, ((dialog1, which) -> {

            }));

            AlertDialog dialog= madb.create();
            dialog.setOnShowListener(d -> {
                Button b= ((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(false);

                TextInputLayout til= ((AlertDialog)d).findViewById(R.id.text_input_layout);
                TextInputEditText tiet= ((AlertDialog)d).findViewById(R.id.text_input);
                if(til!=null && tiet!=null) {
                    til.setHint(R.string.hint_rooms_name);
                    tiet.addTextChangedListener(new EditTextWatcher(til, b, ((AlertDialog) d).getContext().getString(R.string.error_empty_name)));
                } else {
                    d.dismiss();
                    Snackbar.make(b, R.string.message_ui_error, Snackbar.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        }

        void createPlantsListAdapter(ArrayList<Plant> plants, PlantsListAdapter.DataChangeListener plantsDcl){
            if(plants.isEmpty()){
                root.findViewById(R.id.plants_label).setVisibility(View.GONE);
            } else {
                root.findViewById(R.id.plants_label).setVisibility(View.VISIBLE);
                plantsListAdapter= new PlantsListAdapter(root.getContext(), plants, plantsDcl);
                plantsRecyclerView.setAdapter(plantsListAdapter);
            }
        }

        void createLightsListAdapter(ArrayList<LightDevice> lights, LightDevicesListAdapter.DataChangeListener dataChangeListener){
            if(!lights.isEmpty()) {
                root.findViewById(R.id.lights_label).setVisibility(View.VISIBLE);
                lightsListAdapter = new LightDevicesListAdapter(lights, dataChangeListener);
                lightsRecyclerView.setAdapter(lightsListAdapter);
            } else {
                root.findViewById(R.id.lights_label).setVisibility(View.GONE);
            }
        }

        void toggleDetails(){
            airDataPlotDialog.show(sector.getAirDevice(), ((AppCompatActivity)root.getContext()).getSupportFragmentManager(),"airdata", adrListener);
        }
    }

    final private ArrayList<Sector> sectors;
    private final DataChangeListener dcl;
    private final AirDataPlotDialog.AirDataRequestListener adrListener;

    public SectorsListAdapter(ArrayList<Sector> sectors, DataChangeListener dataChangeListener, AirDataPlotDialog.AirDataRequestListener adrl){
        this.sectors= sectors;
        this.dcl= dataChangeListener;
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
        h.init(adrListener, sectors.get(position), dcl);

        h.createPlantsListAdapter(sectors.get(position).getPlants(), dcl::onPlantDataChanged);

        h.createLightsListAdapter(sectors.get(position).getLightDevices(), new LightDevicesListAdapter.DataChangeListener() {
            @Override
            public void onDeviceDataChanged(LightDevice d) {
                dcl.onLightDeviceDataChanged(d);
            }

            @Override
            public void onDeviceDeleteClick(Device d) {
                dcl.onDeviceDeleteClick(d);
            }

            @Override
            public void onDeviceUpdateEnableClick(Device d) {
                dcl.onDeviceUpdateEnableClick(d);
            }

            @Override
            public ArrayList<Room> requestRoomsList() {
                return dcl.requestRoomsList();
            }
        });
    }

    @Override
    public int getItemCount() {
        return sectors.size();
    }
}
