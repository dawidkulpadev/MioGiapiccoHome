package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.R;

public class LightDeviceUnbindDialog {
    public interface ClosedListener{
        void onPositiveClick(LightDevice ld);
    }

    private final LightDevice ld;
    private final ClosedListener closedListener;
    private View rootView;

    public LightDeviceUnbindDialog(LightDevice ld, ClosedListener closedListener){
        this.ld= ld;
        this.closedListener= closedListener;
    }

    public void show(Context c){
        AlertDialog.Builder adb= new AlertDialog.Builder(c);

        rootView= LayoutInflater.from(c).inflate(R.layout.dialog_unbind_device, null, false);
        adb.setView(rootView);

        ((TextView)rootView.findViewById(R.id.title_text)).setText(ld.getName());
        ((TextView)rootView.findViewById(R.id.message_text)).setText(R.string.message_unbind_light_device);

        adb.setPositiveButton(R.string.button_unbind, (dialogInterface, i) -> closedListener.onPositiveClick(ld));

        adb.setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {

        });

        adb.create().show();
    }
}
