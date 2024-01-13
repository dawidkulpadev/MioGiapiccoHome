package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import pl.dawidkulpa.miogiapiccohome.R;

public class CreateRoomDialog {
    public interface ClosedListener{
        void onPositiveClick(String v);
    }

    private final ClosedListener closedListener;
    private View rootView;

    public CreateRoomDialog(ClosedListener closedListener){
        this.closedListener= closedListener;
    }

    public void show(Context c) {
        AlertDialog.Builder adb= new AlertDialog.Builder(c);

        rootView= LayoutInflater.from(c).inflate(R.layout.dialog_edittext, null, false);
        adb.setView(rootView);

        ((TextView)rootView.findViewById(R.id.title_text)).setText(R.string.title_create_room_dialog);
        ((TextView)rootView.findViewById(R.id.message_text)).setText(R.string.message_set_rooms_name);
        ((EditText)rootView.findViewById(R.id.text_input)).setHint(R.string.hint_rooms_name);

        adb.setPositiveButton(R.string.button_set, (dialogInterface, i) -> closedListener.onPositiveClick(((EditText)rootView.findViewById(R.id.text_input)).getText().toString()));

        adb.setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {

        });

        adb.create().show();
    }
}
