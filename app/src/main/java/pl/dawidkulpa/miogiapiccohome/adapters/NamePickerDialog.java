package pl.dawidkulpa.miogiapiccohome.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.R;

public class NamePickerDialog {
    public interface ClosedListener{
        void onPositiveClick(Plant p, String v);
    }

    private final Plant p;
    private final ClosedListener closedListener;
    private View rootView;

    public NamePickerDialog(Plant p, ClosedListener closedListener){
        this.p= p;
        this.closedListener= closedListener;
    }

    public void show(Context c){
        AlertDialog.Builder adb= new AlertDialog.Builder(c);

        rootView= LayoutInflater.from(c).inflate(R.layout.dialog_edittext, null, false);
        adb.setView(rootView);

        ((TextView)rootView.findViewById(R.id.title_text)).setText(p.getName());
        ((TextView)rootView.findViewById(R.id.message_text)).setText(R.string.message_change_plant_name);
        ((EditText)rootView.findViewById(R.id.text_input)).setHint(p.getName());

        adb.setPositiveButton(R.string.button_set, (dialogInterface, i) -> closedListener.onPositiveClick(p, ((EditText)rootView.findViewById(R.id.text_input)).getText().toString()));

        adb.setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {

        });

        adb.create().show();
    }
}
