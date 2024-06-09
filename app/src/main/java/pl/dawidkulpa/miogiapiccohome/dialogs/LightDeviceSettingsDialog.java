package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;

public class LightDeviceSettingsDialog {

    public interface InteractionListener{
        void onPositiveButtonClick(LightDevice d, String newName);
        void onUpdateButtonClick(LightDevice d);
        void onDeleteButtonClick(LightDevice d);
    }

    private final LightDevice device;
    private final InteractionListener interactionListener;

    public LightDeviceSettingsDialog(LightDevice d, @NonNull InteractionListener interactionListener){
        this.interactionListener = interactionListener;
        this.device= d;
    }

    public void show(Context c){
        View rootView = LayoutInflater.from(c).inflate(R.layout.dialog_light_device_settings, null, false);
        TextInputEditText nameInputText= rootView.findViewById(R.id.name_input);

        String sv= device.getSoftwareVersion();
        String hv= device.getHardwareVersion();

        if(sv.isEmpty())
            sv= c.getString(R.string.value_unknown);

        if(hv.isEmpty())
            hv= c.getString(R.string.value_unknown);

        ((TextView)rootView.findViewById(R.id.software_version_text)).setText(
                c.getString(R.string.info_software_version, sv));
        ((TextView)rootView.findViewById(R.id.hardware_version_text)).setText(
                c.getString(R.string.info_hardware_version, hv));

        if(device.isUpdateAvailable()){
            rootView.findViewById(R.id.software_update_button).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.software_update_button).setOnClickListener(v -> interactionListener.onUpdateButtonClick(device));
        }

        nameInputText.setHint(device.getName());
        nameInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length()==0){
                    nameInputText.setHint(device.getName());
                } else {
                    nameInputText.setHint(R.string.hint_light_device_name);
                }
            }
        });


        MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(c);
        madb.setTitle(R.string.title_light_device_settings)
                .setView(rootView)
                .setIcon(R.drawable.icon_sun)
                .setNegativeButton(R.string.button_dismiss, (dialog, which) -> {

                })
                .setPositiveButton(R.string.button_set, (dialog, which) -> onPositiveButtonClick((AlertDialog)dialog) );

        madb.create().show();
    }

    public void onPositiveButtonClick(AlertDialog dialog){
        String newName;

        try{
            newName= Objects.requireNonNull(
                    ((TextInputEditText)dialog.findViewById(R.id.name_input)).getText()).toString();
        } catch (NullPointerException e){
            newName="";
        }

        interactionListener.onPositiveButtonClick(device, newName);
    }


}
