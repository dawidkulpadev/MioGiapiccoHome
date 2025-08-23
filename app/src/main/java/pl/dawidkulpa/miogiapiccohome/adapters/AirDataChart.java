package pl.dawidkulpa.miogiapiccohome.adapters;

import static pl.dawidkulpa.miogiapiccohome.adapters.RoomsListAdapter.RoomViewHolder.ADH_PERIOD_LENGTH_3DAYS;
import static pl.dawidkulpa.miogiapiccohome.adapters.RoomsListAdapter.RoomViewHolder.ADH_PERIOD_LENGTH_DAY;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pl.dawidkulpa.miogiapiccohome.API.AirDataHistory;
import pl.dawidkulpa.miogiapiccohome.R;

public class AirDataChart {
    public class DataMarkerView extends MarkerView{
        private final TextView dateTextView;
        private final TextView humidityTextView;
        private final TextView temperatureTextView;
        private final ArrayList<Long> ts;
        private final SimpleDateFormat spf;

        public DataMarkerView(Context context, int layoutResource, ArrayList<Long> timestamps) {
            super(context, layoutResource);
            dateTextView= findViewById(R.id.date_text);
            humidityTextView= findViewById(R.id.humidity_text);
            temperatureTextView= findViewById(R.id.temperature_text);
            ts= timestamps;
            spf= new SimpleDateFormat("HH:mm   d MMM", Locale.getDefault());
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            Locale usersLocale;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                usersLocale= getResources().getConfiguration().getLocales().get(0);
            } else {
                usersLocale= getResources().getConfiguration().locale;
            }

            float xVal = e.getX();

            LineData lineData = ((LineChart)getChartView()).getLineData();
            dateTextView.setText(spf.format(ts.get((int)xVal)));

            if (lineData != null) {
                List<ILineDataSet> dataSets= lineData.getDataSets();
                Entry humEntry = dataSets.get(0).getEntryForXValue(xVal, Float.NaN);
                Entry tempEntry = dataSets.get(1).getEntryForXValue(xVal, Float.NaN);
                humidityTextView.setText(String.format(usersLocale, "%.0f%%", humEntry.getY()));
                temperatureTextView.setText(String.format(usersLocale, "%.1f℃", tempEntry.getY()));
            }

            // Wymuszenie przeliczenia rozmiaru
            /*textView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            layout(0, 0, getMeasuredWidth(), getMeasuredHeight());*/

            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2f), -getHeight()-20);
        }
    }


    public static class HourValueFormatter extends ValueFormatter {
        private ArrayList<Long> times;
        private final Locale ul;

        HourValueFormatter(ArrayList<Long> ts, Locale usersLocale){
            super();
            times= ts;
            ul = usersLocale;
        }

        @Override
        public String getFormattedValue(float value) {
            double duration= times.get(times.size()-1)-times.get(0);
            double i= value/times.size();

            long xvalue= (long)(times.get(0)+duration*i);

            Date date = new Date(xvalue);

            // Formatowanie daty tylko do pełnej godziny
            return new SimpleDateFormat("HH:mm", ul).format(date);
        }
    }
    public static class DayMonValueFormatter extends ValueFormatter {
        private ArrayList<Long> times;
        private final Locale ul;

        DayMonValueFormatter(ArrayList<Long> ts, Locale usersLocale){
            super();
            times= ts;
            ul = usersLocale;
        }

        @Override
        public String getFormattedValue(float value) {
            double duration= times.get(times.size()-1)-times.get(0);
            double i= value/times.size();

            long xvalue= (long)(times.get(0)+duration*i);

            // Konwertowanie wartości float na czas
            Date date = new Date(xvalue);

            // Formatowanie daty tylko do pełnej godziny
            return new SimpleDateFormat("d MMM", ul).format(date);
        }
    }

    public static class YAxisValueFormatter extends ValueFormatter {

        private final String unit;

        public YAxisValueFormatter(String unit) {
            this.unit = unit;
        }

        @Override
        public String getFormattedValue(float value) {
            return Math.round(value) + unit;
        }
    }

    private final LineChart chartView;
    private final String humTitle;
    private final String tempTitle;
    private final Locale usersLocale;

    public AirDataChart(LineChart v, String humTitleText, String tempTitleText, Context context){
        chartView= v;
        humTitle= humTitleText;
        tempTitle= tempTitleText;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            usersLocale= context.getResources().getConfiguration().getLocales().get(0);
        } else {
            usersLocale= context.getResources().getConfiguration().locale;
        }
    }

    public void init(){
// background color
        chartView.setNoDataText("");
        chartView.setBackgroundColor(Color.TRANSPARENT);

        // disable description text
        chartView.getDescription().setEnabled(false);

        // enable touch gestures
        chartView.setTouchEnabled(true);

        // set listeners
        chartView.setDrawGridBackground(false);

        // enable scaling and dragging
        chartView.setDragEnabled(true);
        //chartView.setScaleEnabled(true);
        chartView.setScaleXEnabled(true);
        chartView.setScaleYEnabled(false);

        // force pinch zoom along both axis
        chartView.setPinchZoom(true);

        XAxis xAxis;
        {   // // X-Axis Style // //
            xAxis = chartView.getXAxis();
            xAxis.setTextColor(Color.WHITE);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            xAxis.setYOffset(10f);
            // vertical grid lines
            xAxis.enableGridDashedLine(10f, 10f, 0f);
            xAxis.setLabelCount(5, true);
            xAxis.setTextSize(12);
        }

        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = chartView.getAxisLeft();
            yAxis.setTextColor(Color.CYAN);
            yAxis.setTextSize(12);

            // horizontal grid lines
            yAxis.enableGridDashedLine(10f, 10f, 0f);

            // axis range
            yAxis.setAxisMaximum(90f);
            yAxis.setAxisMinimum(20f);
            yAxis.setValueFormatter(new AirDataChart.YAxisValueFormatter("%"));
        }

        YAxis yAxis2;
        {
            yAxis2= chartView.getAxisRight();
            yAxis2.setTextColor(Color.RED);
            yAxis2.enableGridDashedLine(10f, 10f, 0f);
            yAxis2.setTextSize(12);

            yAxis2.setAxisMaximum(32f);
            yAxis2.setAxisMinimum(15f);
            yAxis2.setLabelCount(8, true);
            yAxis2.setValueFormatter(new AirDataChart.YAxisValueFormatter("℃"));
        }

        // get the legend (only possible after setting data)
        Legend l = chartView.getLegend();
        l.setTextColor(Color.WHITE);
        l.setTextSize(12);
        l.setXEntrySpace(30);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // draw legend entries as lines
        l.setForm(Legend.LegendForm.LINE);
        l.setDrawInside(false);

        chartView.setExtraOffsets(0, 0, 0, 10f);
        chartView.invalidate();
    }

    public void setData(AirDataHistory d, int adhPeriodLen){
        ArrayList<Long> xTimes = new ArrayList<>();
        for(int i=0; i<d.size(); i++){
            xTimes.add(d.get(i).getTimestamp().getTimeInMillis());
        }

        if(adhPeriodLen==ADH_PERIOD_LENGTH_DAY)
            chartView.getXAxis().setValueFormatter(new HourValueFormatter(xTimes, usersLocale));
        else if(adhPeriodLen==ADH_PERIOD_LENGTH_3DAYS)
            chartView.getXAxis().setValueFormatter(new DayMonValueFormatter(xTimes, usersLocale));
        else
            chartView.getXAxis().setValueFormatter(new DayMonValueFormatter(xTimes, usersLocale));

        ArrayList<Entry> values = new ArrayList<>();

        for(int i=0; i<d.size(); i++){
            values.add(new Entry(i, d.get(i).getHum()));
        }

        ArrayList<Entry> values2 = new ArrayList<>();

        for(int i=0; i<d.size(); i++){
            values2.add(new Entry(i, d.get(i).getTemp()));
        }

        LineDataSet set1, set2;
        {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, humTitle);

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
            set2 = new LineDataSet(values2, tempTitle);

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

            // create a data object with the data sets
            LineData data = new LineData(set1, set2);

            // Initialize marker (popup) view with date at point
            DataMarkerView marketView = new DataMarkerView(chartView.getContext(), R.layout.view_marker, xTimes);
            marketView.setChartView(chartView);
            chartView.setMarker(marketView);

            // set data
            chartView.setData(data);
        }

        chartView.invalidate();
    }

    public void clear(){
        chartView.clear();
    }

    public void hide(){
        chartView.setVisibility(View.GONE);
    }

    public void makeVisible(){
        chartView.setVisibility(View.VISIBLE);
    }
}
