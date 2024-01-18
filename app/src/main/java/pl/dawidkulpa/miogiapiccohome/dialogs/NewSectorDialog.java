package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.content.Context;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.EditTextWatcher;
import pl.dawidkulpa.miogiapiccohome.R;

public class NewSectorDialog {
    public interface ClosedListener {
        void onPositiveClick(int roomId, String name);
    }

    private final int roomId;
    private final Context context;
    private final ClosedListener apiCreateSectorRequest;

    public NewSectorDialog(int roomId, Context context, ClosedListener apiCreateSectorRequest){
        this.roomId= roomId;
        this.context= context;
        this.apiCreateSectorRequest= apiCreateSectorRequest;
    }

    public void show(){
        MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(context);
        madb.setTitle(R.string.title_create_sector_dialog).setIcon(R.drawable.icon_add)
                .setMessage(R.string.message_set_sector_name)
                .setView(R.layout.dialog_new_sector)
                .setNegativeButton(R.string.button_dismiss, (dialog, which) -> {

                })
                .setPositiveButton(R.string.button_create, null);

        AlertDialog dialog= madb.create();
        dialog.setOnShowListener(d -> {
            Button b= ((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(v -> onPositiveClick(((AlertDialog) d)));
            b.setEnabled(false);

            TextInputLayout til= ((AlertDialog)d).findViewById(R.id.text_input_layout);
            TextInputEditText tiet= ((AlertDialog)d).findViewById(R.id.text_input);
            if(til!=null && tiet!=null)
                tiet.addTextChangedListener(new EditTextWatcher(til, b, ((AlertDialog) d).getContext().getString(R.string.error_empty_name)));
        });

        dialog.show();
    }

    public void onPositiveClick(AlertDialog d){
        TextInputEditText input= d.findViewById(R.id.text_input);

        if(input!=null){
            apiCreateSectorRequest.onPositiveClick(roomId, Objects.requireNonNull(input.getText()).toString());
            d.dismiss();
        }
    }
}
