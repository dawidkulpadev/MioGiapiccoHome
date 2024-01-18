package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.EditTextWatcher;
import pl.dawidkulpa.miogiapiccohome.R;

public class DLIPickerDialog {
    public interface ClosedListener{
        void onPositiveClick(LightDevice d, int v);
    }

    private final LightDevice device;
    private final String plantName;
    private final ClosedListener closedListener;

    public DLIPickerDialog(LightDevice d, String plantName, ClosedListener closedListener){
        this.closedListener= closedListener;
        this.device= d;
        this.plantName= plantName;
    }

    public void show(Context c){
        View rootView = LayoutInflater.from(c).inflate(R.layout.dialog_set_dli, null, false);
        TextView sliderValueText= rootView.findViewById(R.id.intensity_value_text);
        Slider slider = rootView.findViewById(R.id.intensity_slider);
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            sliderValueText.setText(c.getString(R.string.value_dli, Math.round(value)));
        });
        slider.setValue(device.getDli());


        MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(c);
        madb.setTitle(device.getName())
                .setMessage(R.string.message_change_dli)
                .setView(rootView)
                .setIcon(R.drawable.icon_sun)
                .setNegativeButton(R.string.button_dismiss, (dialog, which) -> {

                })
                .setPositiveButton(R.string.button_set, ((dialog, which) -> {
                    int v= Math.round(((Slider)((AlertDialog)dialog).findViewById(R.id.intensity_slider)).getValue());
                    if(closedListener!=null){
                        closedListener.onPositiveClick(device, v);
                    }
                }));

        madb.create().show();
    }
}
