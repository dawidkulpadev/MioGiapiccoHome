package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import pl.dawidkulpa.miogiapiccohome.API.data.AirDataHistory;
import pl.dawidkulpa.miogiapiccohome.API.data.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.data.User;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.adapters.AirDataChart;

// Create pipeline: show() -> onCreateDialog() -> onCreateView()

public class AirDataPlotDialog extends BottomSheetDialogFragment {
    public static final int ADH_PERIOD_LENGTH_DAY= 1;
    public static final int ADH_PERIOD_LENGTH_3DAYS=2;
    public static final int ADH_PERIOD_LENGTH_MONTH= 3;
    public static final int ADH_PERIOD_LENGTH_3MONTHS= 4;
    public static final int ADH_PERIOD_LENGTH_YEAR= 5;
    public static final int ADH_PERIOD_LENGTH_ALL_DATA= 10;

    public interface AirDataRequestListener {
        void request(AirDevice ad, User.DownloadAirDataHistoryListener dadh, Calendar start, Calendar end);
    }

    BottomSheetDialog dialog= null;

    Calendar airDataHistoryStart;

    AirDataChart airDataChart;
    ChipGroup adhPeriodSelectorGroup;

    TextView chartTitleTextView;

    ProgressBar chartDataProgressBar;

    Button nextButton;
    Button prevButton;
    TextView batteryLevelTextView;
    ImageView batteryLevelIcon;

    int adhPeriodLen= 1;

    AirDevice device;

    AirDataRequestListener adrListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_air_data_plot, container, false);

        chartTitleTextView= rootView.findViewById(R.id.date_text);
        chartDataProgressBar= rootView.findViewById(R.id.chart_data_progressbar);

        nextButton= rootView.findViewById(R.id.next_btn);
        prevButton= rootView.findViewById(R.id.prev_btn);

        batteryLevelTextView= rootView.findViewById(R.id.battery_level_text);
        batteryLevelIcon= rootView.findViewById(R.id.battery_icon);

        adhPeriodSelectorGroup= rootView.findViewById(R.id.adh_periods_group);
        adhPeriodSelectorGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Calendar now= Calendar.getInstance();

            if(group.getCheckedChipId()==R.id.adh_period_day_chip){
                airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                airDataHistoryStart.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                adhPeriodLen= ADH_PERIOD_LENGTH_DAY;
                prevButton.setEnabled(true);
                nextButton.setEnabled(true);
            } else if(group.getCheckedChipId()==R.id.adh_period_3days_chip){
                airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                airDataHistoryStart.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                airDataHistoryStart.add(Calendar.DAY_OF_MONTH, -2);
                adhPeriodLen= ADH_PERIOD_LENGTH_3DAYS;
                prevButton.setEnabled(true);
                nextButton.setEnabled(true);
            } else if(group.getCheckedChipId()==R.id.adh_period_month_chip){
                airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                adhPeriodLen= ADH_PERIOD_LENGTH_MONTH;
                airDataHistoryStart.set(Calendar.DAY_OF_MONTH, 1);
                prevButton.setEnabled(true);
                nextButton.setEnabled(true);
            } else if (group.getCheckedChipId()==R.id.adh_period_3months_chip) {
                adhPeriodLen= ADH_PERIOD_LENGTH_3MONTHS;
                airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                airDataHistoryStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
                airDataHistoryStart.set(Calendar.DAY_OF_MONTH, 1);
                airDataHistoryStart.add(Calendar.MONTH, -2);
                prevButton.setEnabled(true);
                nextButton.setEnabled(true);
            } else if (group.getCheckedChipId()==R.id.adh_period_year_chip) {
                adhPeriodLen= ADH_PERIOD_LENGTH_YEAR;
                airDataHistoryStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
                airDataHistoryStart.set(Calendar.DAY_OF_MONTH, 1);
                airDataHistoryStart.set(Calendar.MONTH, 1);
                prevButton.setEnabled(true);
                nextButton.setEnabled(true);
            }

            downloadAirData();
        });

        prevButton.setOnClickListener(view->{
            airDataHistoryStart.add(getCalendarFieldForADHPeriod(adhPeriodLen), -getCalendarShiftsForADHPeriod(adhPeriodLen));
            downloadAirData();
        });
        nextButton.setOnClickListener((view)->{
            airDataHistoryStart.add(getCalendarFieldForADHPeriod(adhPeriodLen), getCalendarShiftsForADHPeriod(adhPeriodLen));
            downloadAirData();
        });

        airDataChart= new AirDataChart(rootView.findViewById(R.id.chart),
                dialog.getContext().getString(R.string.label_humidity),
                dialog.getContext().getString(R.string.label_temperature),
                dialog.getContext());

        airDataHistoryStart= Calendar.getInstance();
        airDataHistoryStart.set(Calendar.HOUR_OF_DAY, 0);
        airDataHistoryStart.set(Calendar.MINUTE, 0);
        airDataHistoryStart.set(Calendar.SECOND, 0);

        airDataChart.init();
        downloadAirData();

        if(device.getBatteryVoltage()==0){
            batteryLevelTextView.setVisibility(View.GONE);
            batteryLevelIcon.setVisibility(View.GONE);
        } else {
            batteryLevelTextView.setText(
                    dialog.getContext().getString(R.string.value_battery_voltage, device.getBatteryVoltage())
            );
        }

        airDataChart.init();

        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog= (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        return dialog;
    }

    public void show(AirDevice airDevice, @NonNull FragmentManager manager, @Nullable String tag, AirDataRequestListener listener){
        Log.e("AirDataPlotDialog", "show");
        device= airDevice;
        adrListener= listener;
        super.show(manager, tag);
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
        adrListener.request(device, this::onAirDataReceived, airDataHistoryStart, airDataHistoryEnd);
    }

    void onAirDataReceived(boolean success, AirDataHistory airDataHistory){
        chartDataProgressBar.setVisibility(View.GONE);
        if(success){
            airDataChart.setData(airDataHistory, adhPeriodLen);
        } else {
            Toast.makeText(dialog.getContext(), "Failed downloading air data history", Toast.LENGTH_SHORT).show();
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
}
