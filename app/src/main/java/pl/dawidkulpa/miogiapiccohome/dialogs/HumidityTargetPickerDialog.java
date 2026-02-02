package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.data.Room;
import pl.dawidkulpa.miogiapiccohome.R;

public class HumidityTargetPickerDialog {
    public interface ClosedListener{
        void onPositiveClick(Room r, int v);
    }

    private final Room room;
    private final ClosedListener closedListener;

    public HumidityTargetPickerDialog(Room r, ClosedListener closedListener){
        this.closedListener= closedListener;
        this.room= r;
    }

    public void show(Context c){
        View rootView = LayoutInflater.from(c).inflate(R.layout.dialog_set_ht, null, false);
        TextView sliderValueText= rootView.findViewById(R.id.ht_value_text);
        Slider slider = rootView.findViewById(R.id.ht_slider);
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            sliderValueText.setText(c.getString(R.string.value_humidity_integer, Math.round(value)));
        });

        if(room.getHumidityTarget()<20){
            slider.setValue(20);
        } else {
            slider.setValue(Math.min(room.getHumidityTarget(), 99));
        }
        sliderValueText.setText(c.getString(R.string.value_humidity_integer, Math.round(slider.getValue())));



        MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(c);
        madb.setTitle(room.getName())
                .setMessage(R.string.message_change_humidity_target)
                .setView(rootView)
                .setIcon(R.drawable.icon_water_drop)
                .setNegativeButton(R.string.button_dismiss, (dialog, which) -> {

                })
                .setPositiveButton(R.string.button_set, ((dialog, which) -> {
                    int v= Math.round(((Slider) Objects.requireNonNull(((AlertDialog) dialog).findViewById(R.id.ht_slider))).getValue());
                    if(closedListener!=null){
                        closedListener.onPositiveClick(room, v);
                    }
                }));

        madb.create().show();
    }
}
