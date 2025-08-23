package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import pl.dawidkulpa.miogiapiccohome.API.AirDataHistory;
import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.R;

import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.dialogs.HumidityTargetPickerDialog;
import pl.dawidkulpa.miogiapiccohome.dialogs.NewSectorDialog;

public class RoomsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface DataChangeListener {
        void onDeviceUpdateClick(LightDevice d);
        void onDeviceDeleteClick(LightDevice d);
        void onRoomDeleteClick(Room r);

        void onLightDeviceDataChanged(LightDevice d);
        void onSoilDeviceDataChanged(SoilDevice d);
        void onAirDeviceDataChanged(AirDevice d);
        void onPlantDataChanged(Plant p);
        void onRoomDataChanged(Room r);
        void onSectorDataChanged(Sector s);
    }

    public interface DataRequestListener {
        void requestAirData(AirDevice ad, User.DownloadAirDataHistoryListener dadh, Calendar start, Calendar end);
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder{
        static final int ADH_PERIOD_LENGTH_DAY= 1;
        static final int ADH_PERIOD_LENGTH_3DAYS=2;
        static final int ADH_PERIOD_LENGTH_MONTH= 3;
        static final int ADH_PERIOD_LENGTH_3MONTHS= 4;
        static final int ADH_PERIOD_LENGTH_YEAR= 5;
        static final int ADH_PERIOD_LENGTH_ALL_DATA= 10;

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
        ConstraintLayout detailsBoxLayout;

        Calendar airDataHistoryStart;

        AirDataChart airDataChart;
        ChipGroup adhPeriodSelectorGroup;

        TextView chartTitleTextView;

        ProgressBar chartDataProgressBar;

        DataRequestListener dataRequestListener;
        DataChangeListener dataChangeListener;

        int adhPeriodLen= 1;

        Room room;


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
            detailsBoxLayout= v.findViewById(R.id.room_details_box_layout);

            airDataChart= new AirDataChart(v.findViewById(R.id.chart),
                    v.getContext().getString(R.string.label_humidity),
                    v.getContext().getString(R.string.label_temperature),
                    v.getContext());

            chartTitleTextView= root.findViewById(R.id.date_text);
            chartDataProgressBar= v.findViewById(R.id.chart_data_progressbar);

            adhPeriodSelectorGroup= v.findViewById(R.id.adh_periods_group);
            adhPeriodSelectorGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                Calendar now= Calendar.getInstance();

                if(group.getCheckedChipId()==R.id.adh_period_day_chip){
                    airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    airDataHistoryStart.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                    adhPeriodLen= ADH_PERIOD_LENGTH_DAY;
                    root.findViewById(R.id.prev_btn).setEnabled(true);
                    root.findViewById(R.id.next_btn).setEnabled(true);
                } else if(group.getCheckedChipId()==R.id.adh_period_3days_chip){
                    airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    airDataHistoryStart.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                    airDataHistoryStart.add(Calendar.DAY_OF_MONTH, -2);
                    adhPeriodLen= ADH_PERIOD_LENGTH_3DAYS;
                    root.findViewById(R.id.prev_btn).setEnabled(true);
                    root.findViewById(R.id.next_btn).setEnabled(true);
                } else if(group.getCheckedChipId()==R.id.adh_period_month_chip){
                    airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    adhPeriodLen= ADH_PERIOD_LENGTH_MONTH;
                    airDataHistoryStart.set(Calendar.DAY_OF_MONTH, 1);
                    root.findViewById(R.id.prev_btn).setEnabled(true);
                    root.findViewById(R.id.next_btn).setEnabled(true);
                } else if (group.getCheckedChipId()==R.id.adh_period_3months_chip) {
                    adhPeriodLen= ADH_PERIOD_LENGTH_3MONTHS;
                    airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    airDataHistoryStart.set(Calendar.DAY_OF_MONTH, 1);
                    airDataHistoryStart.add(Calendar.MONTH, -2);
                    root.findViewById(R.id.prev_btn).setEnabled(true);
                    root.findViewById(R.id.next_btn).setEnabled(true);
                } else if (group.getCheckedChipId()==R.id.adh_period_year_chip) {
                    adhPeriodLen= ADH_PERIOD_LENGTH_YEAR;
                    airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    airDataHistoryStart.set(Calendar.DAY_OF_MONTH, 1);
                    airDataHistoryStart.set(Calendar.MONTH, 1);
                    root.findViewById(R.id.prev_btn).setEnabled(true);
                    root.findViewById(R.id.next_btn).setEnabled(true);
                }

                downloadAirData();
            });

            root.findViewById(R.id.prev_btn).setOnClickListener(view->{
                airDataHistoryStart.add(getCalendarFieldForADHPeriod(adhPeriodLen), -getCalendarShiftsForADHPeriod(adhPeriodLen));
                downloadAirData();
            });
            root.findViewById(R.id.next_btn).setOnClickListener((view)->{
                airDataHistoryStart.add(getCalendarFieldForADHPeriod(adhPeriodLen), getCalendarShiftsForADHPeriod(adhPeriodLen));
                downloadAirData();
            });

            root.findViewById(R.id.room_settings_button).setOnClickListener((view)-> toggleRoomsSettings());
        }

        void toggleRoomsSettings(){
            ConstraintLayout settingsPane= root.findViewById(R.id.room_settingsbox_layout_box);
            if(settingsPane.getVisibility()==View.VISIBLE){
                settingsPane.setVisibility(View.GONE);
            } else {
                settingsPane.setVisibility(View.VISIBLE);
            }
        }

        int getCalendarFieldForADHPeriod(int adhPeriod){
            switch (adhPeriod){
                case ADH_PERIOD_LENGTH_MONTH:
                case ADH_PERIOD_LENGTH_3MONTHS:
                    return Calendar.MONTH;
                case ADH_PERIOD_LENGTH_YEAR:
                case ADH_PERIOD_LENGTH_ALL_DATA:
                    return Calendar.YEAR;
                default:
                    return Calendar.DAY_OF_MONTH;
            }
        }

        int getCalendarShiftsForADHPeriod(int adhPeriod){
            switch (adhPeriod){
                case ADH_PERIOD_LENGTH_3MONTHS:
                case ADH_PERIOD_LENGTH_3DAYS:
                    return 3;
                case ADH_PERIOD_LENGTH_ALL_DATA:
                    return  0;
                default:
                    return  1;
            }
        }

        void setDateOnChartTitle(Calendar start, Calendar end){
            switch (adhPeriodLen){
                case ADH_PERIOD_LENGTH_DAY:
                    setDateOnChartTitleForDay(start, chartTitleTextView);
                    break;
                case ADH_PERIOD_LENGTH_3DAYS:
                    setDateOnChartTitleFor3Days(start, end, chartTitleTextView);
                    break;
                case ADH_PERIOD_LENGTH_MONTH:
                    setDateOnChartTitleForMonth(start, chartTitleTextView);
                    break;
                case ADH_PERIOD_LENGTH_3MONTHS:
                    setDateOnChartTitleFor3Months(start, end, chartTitleTextView);
                    break;
                case ADH_PERIOD_LENGTH_YEAR:
                    setDateOnChartTitleForYear(start, chartTitleTextView);
                    break;
                case ADH_PERIOD_LENGTH_ALL_DATA:
                    setDateOnChartTitleForAllData(chartTitleTextView);
                    break;
            }
        }

        static void setDateOnChartTitleForDay(Calendar start, TextView titleView){
            Calendar today= Calendar.getInstance();

            if(today.get(Calendar.DAY_OF_MONTH)==start.get(Calendar.DAY_OF_MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH)==start.get(Calendar.DAY_OF_MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH)==start.get(Calendar.DAY_OF_MONTH)){
                titleView.setText(R.string.title_today);
            } else {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd LLLL yyyy", Locale.getDefault());
                String dateString = simpleDateFormat.format(start.getTime());
                titleView.setText(dateString);
            }
        }

        static void setDateOnChartTitleFor3Days(Calendar start, Calendar end, TextView titleView){
            SimpleDateFormat simpleDateEndFormat = new SimpleDateFormat("d LLLL yyyy", Locale.getDefault());
            SimpleDateFormat simpleDateStartFormat = new SimpleDateFormat("d", Locale.getDefault());
            String dateString = simpleDateStartFormat.format(start.getTime()) + " - " + simpleDateEndFormat.format(end.getTime());
            titleView.setText(dateString);
        }

        static void setDateOnChartTitleForMonth(Calendar start, TextView titleView){
            SimpleDateFormat simpleDateFormat= new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
            titleView.setText(simpleDateFormat.format(start.getTime()));
        }

        static void setDateOnChartTitleFor3Months(Calendar start, Calendar end, TextView titleView){
            SimpleDateFormat simpleMonthFormat = new SimpleDateFormat("LLLL", Locale.getDefault());
            SimpleDateFormat simpleYearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
            String dateString = simpleMonthFormat.format(start.getTime()) + " - " + simpleMonthFormat.format(end.getTime()) + " " + simpleYearFormat.format(end.getTime());
            titleView.setText(dateString);
        }

        static void setDateOnChartTitleForYear(Calendar start, TextView titleView){
            SimpleDateFormat simpleDateFormat= new SimpleDateFormat("yyyy", Locale.getDefault());
            titleView.setText(simpleDateFormat.format(start.getTime()));
        }

        static void setDateOnChartTitleForAllData(TextView titleView){
            titleView.setText(R.string.title_period_all_data);
        }

        void init(DataRequestListener drl, DataChangeListener dcl, Room r){
            dataRequestListener= drl;
            dataChangeListener= dcl;
            room= r;

            airDataHistoryStart= Calendar.getInstance();
            airDataHistoryStart.set(Calendar.HOUR_OF_DAY, 0);
            airDataHistoryStart.set(Calendar.MINUTE, 0);
            airDataHistoryStart.set(Calendar.SECOND, 0);

            root.setOnClickListener(v-> toggleDetails());

            if(!room.getAirDevices().isEmpty()){
                airDataChart.init();
                downloadAirData();

                ((TextView)root.findViewById(R.id.humidity_target_text)).setText(
                        root.getContext().getString(R.string.value_humidity_integer, room.getHumidityTarget()));

                if(room.getAirDevices().get(0).getBatteryVoltage()==0){
                    ((TextView) root.findViewById(R.id.battery_level_text)).setVisibility(View.GONE);
                    root.findViewById(R.id.battery_level_label).setVisibility(View.GONE);
                } else {
                    ((TextView) root.findViewById(R.id.battery_level_text)).setText(
                            root.getContext().getString(R.string.value_battery_voltage, room.getAirDevices().get(0).getBatteryVoltage())
                    );
                }
                root.findViewById(R.id.humidity_target_edit_button).setOnClickListener(
                        (v)-> openHumidityTargetPickerDialog(dcl));
            } else {
                root.findViewById(R.id.ad_bottom_divider).setVisibility(View.GONE);
                root.findViewById(R.id.air_params_box_title).setVisibility(View.GONE);
                root.findViewById(R.id.control_box).setVisibility(View.GONE);
                root.findViewById(R.id.adh_periods_group).setVisibility(View.GONE);
                root.findViewById(R.id.chart).setVisibility(View.GONE);
                root.findViewById(R.id.air_device_params_box).setVisibility(View.GONE);
            }

            root.findViewById(R.id.room_rename_button).setOnClickListener(view-> onRoomRenameClick());
            root.findViewById(R.id.room_delete_button).setOnClickListener(view->onRoomDeleteClick());
        }

        void onRoomDeleteClick(){
            MaterialAlertDialogBuilder adb= new MaterialAlertDialogBuilder(root.getContext());

            adb.setTitle(root.getContext().getString(R.string.title_remove_room, room.getName()));
            adb.setMessage(R.string.message_remove_device);
            adb.setPositiveButton(R.string.button_remove, (dialog, which) -> dataChangeListener.onRoomDeleteClick(room));
            adb.setNegativeButton(R.string.button_cancel, null);

            adb.create().show();
        }

        void onRoomRenameClick(){

        }

        void openHumidityTargetPickerDialog(DataChangeListener dcl){
            HumidityTargetPickerDialog htpd= new HumidityTargetPickerDialog(room, (r, v) -> {
                r.setHumidityTarget(v);
                dcl.onRoomDataChanged(r);
            });

            htpd.show(root.getContext());
        }

        void createSectorsListAdapter(ArrayList<Sector> sectors, DataChangeListener dataChangeListener){
            sectorsListAdapter= new SectorsListAdapter(root.getContext(), sectors, dataChangeListener);
            sectorsRecyclerView.setAdapter(sectorsListAdapter);
        }

        void toggleDetails(){
            if(detailsBoxLayout.getVisibility()==View.GONE){
                detailsBoxLayout.setVisibility(View.VISIBLE);
            } else {
                detailsBoxLayout.setVisibility(View.GONE);
            }
        }

        private void downloadAirData(){
            airDataChart.clear();
            chartDataProgressBar.setVisibility(View.VISIBLE);

            Calendar airDataHistoryEnd= (Calendar)airDataHistoryStart.clone();

            switch (adhPeriodLen){
                case ADH_PERIOD_LENGTH_DAY:
                    airDataHistoryEnd.add(Calendar.SECOND, 3600*24);
                    break;
                case ADH_PERIOD_LENGTH_3DAYS:
                    airDataHistoryEnd.add(Calendar.DAY_OF_MONTH, 3);
                    break;
                case ADH_PERIOD_LENGTH_MONTH:
                    airDataHistoryEnd.add(Calendar.MONTH, 1);
                    break;
                case ADH_PERIOD_LENGTH_3MONTHS:
                    airDataHistoryEnd.add(Calendar.MONTH, 3);
                    break;
                case ADH_PERIOD_LENGTH_YEAR:
                    airDataHistoryEnd.add(Calendar.YEAR, 1);
                    break;
                case ADH_PERIOD_LENGTH_ALL_DATA:
                    airDataHistoryEnd.add(Calendar.YEAR, 1000);

            }
            airDataHistoryEnd.add(Calendar.SECOND, -1);

            setDateOnChartTitle(airDataHistoryStart, airDataHistoryEnd);
            int lastIdx = room.getAirDevices().size()-1;
            dataRequestListener.requestAirData(room.getAirDevices().get(lastIdx), this::onAirDataReceived, airDataHistoryStart, airDataHistoryEnd);
        }

        void onAirDataReceived(boolean success, AirDataHistory airDataHistory){
            chartDataProgressBar.setVisibility(View.GONE);
            if(success){
                airDataChart.setData(airDataHistory, adhPeriodLen);
            } else {
                Toast.makeText(root.getContext(), "Failed downloading air data history", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private final Context context;
    private ArrayList<Room> rooms;
    private final DataChangeListener dataChangeListener;
    private final DataRequestListener dataRequestListener;
    private final NewSectorDialog.ClosedListener apiCreateSectorRequest;

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

        h.init(dataRequestListener, dataChangeListener, rooms.get(position));

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

        } else {
            h.humText.setVisibility(View.GONE);
            h.tempText.setVisibility(View.GONE);
        }

        h.newSectorDialog= new NewSectorDialog(rooms.get(position).getId(), context, apiCreateSectorRequest);
        h.newSectorButton.setOnClickListener(v -> h.newSectorDialog.show());
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void updateList(ArrayList<Room> newRooms){
        rooms= newRooms;
    }

}
