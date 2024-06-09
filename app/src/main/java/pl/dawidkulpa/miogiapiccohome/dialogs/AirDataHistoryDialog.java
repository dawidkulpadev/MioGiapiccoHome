package pl.dawidkulpa.miogiapiccohome.dialogs;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;

import pl.dawidkulpa.miogiapiccohome.API.AirDataHistory;
import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.R;

public class AirDataHistoryDialog {
    private LineChart chart;
    private final User user;
    private AlertDialog dialog;
    private final AirDevice airDevice;
    private final ArrayList<String> xLabels= new ArrayList<>();

    Calendar start;

    public AirDataHistoryDialog(User user, AirDevice airDevice){
        this.user= user;
        this.airDevice= airDevice;

        start= Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
    }


    public void show(Context c){
        View rootView = LayoutInflater.from(c).inflate(R.layout.dialog_air_data_plot, null, false);

        Button prevMonthButton = rootView.findViewById(R.id.prev_month_btn);
        Button nextMonthButton = rootView.findViewById(R.id.next_month_btn);
        Button prevDateButton = rootView.findViewById(R.id.prev_btn);
        Button nextDateButton = rootView.findViewById(R.id.next_btn);

        prevMonthButton.setOnClickListener((v) -> {
            start.add(Calendar.MONTH, -1);
            startDownloadPlotData();
        });

        nextMonthButton.setOnClickListener((v)->{
            start.add(Calendar.MONTH, 1);
            startDownloadPlotData();
        });

        prevDateButton.setOnClickListener((v)->{
            start.add(Calendar.DAY_OF_MONTH, -1);
            startDownloadPlotData();
        });

        nextDateButton.setOnClickListener((v)->{
            start.add(Calendar.DAY_OF_MONTH, 1);
            startDownloadPlotData();
        });


        MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(c);
        madb.setTitle(R.string.title_air_data_history)
                .setView(rootView)
                .setIcon(R.drawable.icon_data_chart)
                .setNeutralButton(R.string.button_close, (dialog, which) -> {
                    dialog.cancel();
                });



        dialog= madb.create();
        dialog.setOnShowListener(this::onShow);
        dialog.show();
    }

    public void startDownloadPlotData(){
        Calendar end= (Calendar)start.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);

        user.getAirDataHistory(airDevice, this::onPlotDataDownloaded, start ,end);
    }

    public void onPlotDataDownloaded(boolean success, AirDataHistory airDataHistory){
        if(success){
            putDataOnPlot(airDataHistory);
        }
    }

    public void putDataOnPlot(AirDataHistory adh){
        xLabels.clear();
        for(int i=0; i<adh.size(); i++){
            xLabels.add(adh.get(i).getStringTime());
        }

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

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
            set1 = new LineDataSet(values, "Humidity");

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
            set2 = new LineDataSet(values2, "Temperature");

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
            chart.setData(data);
        }

        chart.invalidate();
    }

    public void onShow(DialogInterface dialog){
        chart = ((AlertDialog)dialog).findViewById(R.id.chart);

        // background color
        chart.setBackgroundColor(Color.TRANSPARENT);

        // disable description text
        chart.getDescription().setEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);

        // set listeners
        chart.setDrawGridBackground(false);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        // chart.setScaleXEnabled(true);
        // chart.setScaleYEnabled(true);

        // force pinch zoom along both axis
        chart.setPinchZoom(true);

        XAxis xAxis;
        {   // // X-Axis Style // //
            xAxis = chart.getXAxis();
            xAxis.setTextColor(Color.WHITE);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            // vertical grid lines
            xAxis.enableGridDashedLine(10f, 10f, 0f);
            xAxis.setTextSize(12);
        }

        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = chart.getAxisLeft();
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
            yAxis2= chart.getAxisRight();
            yAxis2.setTextColor(Color.RED);
            yAxis2.enableGridDashedLine(10f, 10f, 0f);
            yAxis2.setTextSize(12);

            yAxis2.setAxisMaximum(32f);
            yAxis2.setAxisMinimum(15f);
        }

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setTextColor(Color.WHITE);
        l.setTextSize(11);

        // draw legend entries as lines
        l.setForm(Legend.LegendForm.LINE);
        chart.invalidate();

        startDownloadPlotData();
    }

}
