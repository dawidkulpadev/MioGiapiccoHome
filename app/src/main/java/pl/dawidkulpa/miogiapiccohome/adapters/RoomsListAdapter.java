package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.graphics.Color;
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

        void requestAirData(AirDevice ad, User.DownloadAirDataHistoryListener dadh, Calendar start, Calendar end);
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder{
        static final int ADH_PERIOD_LENGTH_DAY= 1;
        static final int ADH_PERIOD_LENGTH_MONTH= 2;
        static final int ADH_PERIOD_LENGTH_3MONTHS= 3;
        static final int ADH_PERIOD_LENGTH_YEAR= 4;
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

        LineChart airDataChart;
        ChipGroup adhPeriodSelectorGroup;

        TextView chartTitleTextView;

        ProgressBar chartDataProgressBar;


        DataRequestListener dataRequestListener;

        int adhPeriodLen= 1; // 1- day, 2- month, 3- 3 months, 4-year, 5- whole available data

        Room room;

        public static class HourValueFormatter extends ValueFormatter {
            ArrayList<Long> times;

            HourValueFormatter(ArrayList<Long> ts){
                super();
                times= ts;
            }

            @Override
            public String getFormattedValue(float value) {
                double duration= times.get(times.size()-1)-times.get(0);
                double i= value/times.size();

                long xvalue= (long)(times.get(0)+duration*i);

                Date date = new Date(xvalue);

                // Formatowanie daty tylko do pełnej godziny
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            }
        }

        public static class DayMonValueFormatter extends ValueFormatter {
            ArrayList<Long> times;

            DayMonValueFormatter(ArrayList<Long> ts){
                super();
                times= ts;
            }

            @Override
            public String getFormattedValue(float value) {
                double duration= times.get(times.size()-1)-times.get(0);
                double i= value/times.size();

                long xvalue= (long)(times.get(0)+duration*i);

                // Konwertowanie wartości float na czas
                Date date = new Date(xvalue);

                // Formatowanie daty tylko do pełnej godziny
                return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(date);
            }
        }


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

            airDataChart= v.findViewById(R.id.chart);

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


        void init(DataRequestListener drl, Room r){
            dataRequestListener= drl;
            room= r;

            airDataHistoryStart= Calendar.getInstance();
            airDataHistoryStart.set(Calendar.HOUR_OF_DAY, 0);
            airDataHistoryStart.set(Calendar.MINUTE, 0);
            airDataHistoryStart.set(Calendar.SECOND, 0);

            root.setOnClickListener(v-> toggleDetails());

            initChartView();
            if(!room.getAirDevices().isEmpty()){
                downloadAirData();
            } else {
                airDataChart.setVisibility(View.GONE);
            }
        }

        void initChartView(){
            // background color
            airDataChart.setNoDataText("");
            airDataChart.setBackgroundColor(Color.TRANSPARENT);

            // disable description text
            airDataChart.getDescription().setEnabled(false);

            // enable touch gestures
            airDataChart.setTouchEnabled(true);

            // set listeners
            airDataChart.setDrawGridBackground(false);

            // enable scaling and dragging
            airDataChart.setDragEnabled(true);
            //airDataChart.setScaleEnabled(true);
            airDataChart.setScaleXEnabled(true);
            airDataChart.setScaleYEnabled(false);

            // force pinch zoom along both axis
            airDataChart.setPinchZoom(true);

            XAxis xAxis;
            {   // // X-Axis Style // //
                xAxis = airDataChart.getXAxis();
                xAxis.setTextColor(Color.WHITE);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                xAxis.setYOffset(10f);
                // vertical grid lines
                xAxis.enableGridDashedLine(10f, 10f, 0f);
                xAxis.setGranularity(3600f);
                xAxis.setGranularityEnabled(true);
                xAxis.setLabelCount(6, true);
                xAxis.setTextSize(12);
            }

            YAxis yAxis;
            {   // // Y-Axis Style // //
                yAxis = airDataChart.getAxisLeft();
                yAxis.setTextColor(Color.CYAN);
                yAxis.setTextSize(12);

                // horizontal grid lines
                yAxis.enableGridDashedLine(10f, 10f, 0f);

                // axis range
                yAxis.setAxisMaximum(90f);
                yAxis.setAxisMinimum(40f);
            }

            YAxis yAxis2;
            {
                yAxis2= airDataChart.getAxisRight();
                yAxis2.setTextColor(Color.RED);
                yAxis2.enableGridDashedLine(10f, 10f, 0f);
                yAxis2.setTextSize(12);

                yAxis2.setAxisMaximum(32f);
                yAxis2.setAxisMinimum(15f);
            }

            // get the legend (only possible after setting data)
            Legend l = airDataChart.getLegend();
            l.setTextColor(Color.WHITE);
            l.setTextSize(12);
            l.setXEntrySpace(30);
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

            // draw legend entries as lines
            l.setForm(Legend.LegendForm.LINE);
            l.setDrawInside(false);

            airDataChart.setExtraOffsets(0, 0, 0, 10f);
            airDataChart.invalidate();
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
            dataRequestListener.requestAirData(room.getAirDevices().get(0), this::onAirDataReceived, airDataHistoryStart, airDataHistoryEnd);
        }

        void onAirDataReceived(boolean success, AirDataHistory airDataHistory){
            chartDataProgressBar.setVisibility(View.GONE);
            if(success){
                putDataOnPlot(airDataHistory);
            } else {
                Toast.makeText(root.getContext(), "Failed downloading air data history", Toast.LENGTH_SHORT).show();
            }
        }

        public void putDataOnPlot(@NonNull AirDataHistory adh){

            ArrayList<Long> xTimes = new ArrayList<>();
            for(int i=0; i<adh.size(); i++){
                xTimes.add(adh.get(i).getTimestamp().getTimeInMillis());
            }

            if(adhPeriodLen==ADH_PERIOD_LENGTH_DAY)
                airDataChart.getXAxis().setValueFormatter(new HourValueFormatter(xTimes));
            else
                airDataChart.getXAxis().setValueFormatter(new DayMonValueFormatter(xTimes));

            ArrayList<Entry> values = new ArrayList<>();

            for(int i=0; i<adh.size(); i++){
                values.add(new Entry(i, adh.get(i).getHum()));
            }

            ArrayList<Entry> values2 = new ArrayList<>();

            for(int i=0; i<adh.size(); i++){
                values2.add(new Entry(i, adh.get(i).getTemp()));
            }

            LineDataSet set1, set2;
            {
                // create a dataset and give it a type
                set1 = new LineDataSet(values, root.getContext().getString(R.string.label_humidity));

                set1.setDrawIcons(false);

                // black lines and points
                set1.setColor(Color.CYAN);

                // line thickness and point size
                set1.setLineWidth(1.5f);

                set1.setDrawCircles(false);

                // customize legend entry
                set1.setFormLineWidth(1f);
                set1.setFormSize(15.f);
                set1.setAxisDependency(YAxis.AxisDependency.LEFT);

                set1.setValueTextColor(Color.TRANSPARENT);

                // draw selection line as dashed
                set1.enableDashedHighlightLine(10f, 5f, 0f);

                // create a dataset and give it a type
                set2 = new LineDataSet(values2, root.getContext().getString(R.string.label_temperature));

                set2.setDrawIcons(false);

                // black lines and points
                set2.setColor(Color.RED);

                // line thickness and point size
                set2.setLineWidth(1.5f);

                set2.setDrawCircles(false);

                // customize legend entry
                set2.setFormLineWidth(1f);
                set2.setFormSize(15.f);

                set2.setValueTextColor(Color.TRANSPARENT);

                set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
                // draw selection line as dashed
                set2.enableDashedHighlightLine(10f, 5f, 0f);

                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(set1); // add the data sets
                dataSets.add(set2);

                // create a data object with the data sets
                LineData data = new LineData(dataSets);

                // set data
                airDataChart.setData(data);
            }

            airDataChart.invalidate();
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

        h.init(dataRequestListener, rooms.get(position));

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
