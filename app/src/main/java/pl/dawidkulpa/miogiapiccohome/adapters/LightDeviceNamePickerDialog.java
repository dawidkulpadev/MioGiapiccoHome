package pl.dawidkulpa.miogiapiccohome.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import pl.dawidkulpa.miogiapiccohome.API.data.LightDevice;
import pl.dawidkulpa.miogiapiccohome.R;

public class LightDeviceNamePickerDialog {
    public interface ClosedListener{
        void onPositiveClick(LightDevice ld, String v);
    }

    private LightDevice ld;
    private final ClosedListener closedListener;
    private View rootView;

    public LightDeviceNamePickerDialog(LightDevice ld, ClosedListener closedListener){
        this.ld= ld;
        this.closedListener= closedListener;
    }

    public void show(Context c){
        AlertDialog.Builder adb= new AlertDialog.Builder(c);

        rootView= LayoutInflater.from(c).inflate(R.layout.dialog_new_room, null, false);
        adb.setView(rootView);

        ((TextView)rootView.findViewById(R.id.title_text)).setText(ld.getName());
        ((TextView)rootView.findViewById(R.id.message_text)).setText(R.string.message_change_devices_name);
        ((EditText)rootView.findViewById(R.id.text_input)).setHint(ld.getName());

        adb.setPositiveButton(R.string.button_set, (dialogInterface, i) -> closedListener.onPositiveClick(ld, ((EditText)rootView.findViewById(R.id.text_input)).getText().toString()));

        adb.setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {

        });

        adb.create().show();
    }
}
