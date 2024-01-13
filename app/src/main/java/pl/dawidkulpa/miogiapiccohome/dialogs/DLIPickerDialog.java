package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;

public class DLIPickerDialog {
    public interface ClosedListener{
        void onPositiveClick(LightDevice d, int v);
    }

    private final LightDevice device;
    private final String plantName;
    private final ClosedListener closedListener;
    private View rootView;

    public DLIPickerDialog(LightDevice d, String plantName, ClosedListener closedListener){
        this.closedListener= closedListener;
        this.device= d;
        this.plantName= plantName;
    }

    public void show(Context c){
        AlertDialog.Builder adb= new AlertDialog.Builder(c);

        rootView= LayoutInflater.from(c).inflate(R.layout.dialog_edittext, null, false);
        adb.setView(rootView);

        ((TextView)rootView.findViewById(R.id.title_text)).setText(plantName);
        ((TextView)rootView.findViewById(R.id.message_text)).setText(R.string.message_change_dli);
        ((EditText)rootView.findViewById(R.id.text_input)).setHint(String.valueOf(device.getDli()));

        adb.setPositiveButton(R.string.button_set, (dialogInterface, i) -> closedListener.onPositiveClick(device, Integer.parseInt(((EditText)rootView.findViewById(R.id.text_input)).getText().toString())));

        adb.setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {

        });

        adb.create().show();
    }
}
