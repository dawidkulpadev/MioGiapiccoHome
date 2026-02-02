package pl.dawidkulpa.miogiapiccohome.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.data.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.data.Room;
import pl.dawidkulpa.miogiapiccohome.API.data.Sector;
import pl.dawidkulpa.miogiapiccohome.R;

public class LightDeviceSettingsDialog extends BottomSheetDialogFragment {
    public interface SettingsActions {
        void onClose(LightDevice device, boolean write);
        void onDeleteClick(LightDevice device);
        void onUpdateAllowedChange(LightDevice device);
    }

    BottomSheetDialog dialog= null;
    LightDevice device;
    ArrayList<Room> rooms;
    MaterialAutoCompleteTextView roomsAutoComplete;
    MaterialAutoCompleteTextView sectorsAutoComplete;

    Button nameResetButton;
    Button roomResetButton;
    Button sectorResetButton;

    TextInputEditText nameInputEditText;

    int selectedRoom= 0;
    int selectedSector= 0;

    int devRoomIdx=0;
    int devSectorIdx=0;

    SettingsActions actions;

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        boolean writeRequired= false;

        if(nameInputEditText.getText()!=null) {
            if(!nameInputEditText.getText().toString().equals(device.getName()))
                writeRequired= true;
            device.setName(nameInputEditText.getText().toString());
        }

        if(selectedRoom>=0 && selectedSector>=0) {
            if(device.getRoomId()!=rooms.get(selectedRoom).getId() &&
                device.getSectorId()!=rooms.get(selectedRoom).getSectors().get(selectedSector).getId()){
                writeRequired= true;
            }
            device.setRoomId(rooms.get(selectedRoom).getId());
            device.setSectorId(rooms.get(selectedRoom).getSectors().get(selectedSector).getId());
        }

        actions.onClose(device, writeRequired);
        super.onDismiss(dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_light_device_settings, container, false);


        // Setup name edit
        nameInputEditText= rootView.findViewById(R.id.device_name_edit);

        ((TextView)rootView.findViewById(R.id.title)).setText(device.getName());
        nameInputEditText.setText(device.getName());
        nameInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().equals(device.getName())){
                    nameResetButton.setVisibility(View.VISIBLE);
                } else {
                    nameResetButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup placement lists
        roomsAutoComplete= rootView.findViewById(R.id.room_select_autocomplete);
        sectorsAutoComplete= rootView.findViewById(R.id.sector_select_autocomplete);
        prepareRoomsListAdapter();

        devRoomIdx= findRoomIdx(device.getRoomId());
        if(devRoomIdx>=0){
            devSectorIdx= findSectorName(devRoomIdx, device.getSectorId());
            if(devSectorIdx<0){
                Snackbar.make(rootView,R.string.error_reading_light_device_settings, Snackbar.LENGTH_SHORT).show();
                dismiss();
            }
        } else {
            Snackbar.make(rootView,R.string.error_reading_light_device_settings, Snackbar.LENGTH_SHORT).show();
            dismiss();
        }

        selectedRoom= devRoomIdx;
        selectedSector= devSectorIdx;

        roomsAutoComplete.setText(rooms.get(devRoomIdx).getName(), false);
        prepareSectorsListAdapter(devRoomIdx);
        sectorsAutoComplete.setText(rooms.get(devRoomIdx).getSectors().get(devSectorIdx).getName(), false);

        // Setup reset buttons
        nameResetButton= rootView.findViewById(R.id.name_reset_button);
        roomResetButton= rootView.findViewById(R.id.room_reset_button);
        sectorResetButton= rootView.findViewById(R.id.sector_reset_button);

        nameResetButton.setVisibility(View.GONE);
        roomResetButton.setVisibility(View.GONE);
        sectorResetButton.setVisibility(View.GONE);

        nameResetButton.setOnClickListener(v -> onNameResetClick());
        roomResetButton.setOnClickListener(v->onRoomResetClick());
        sectorResetButton.setOnClickListener(v->onSectorResetClick());

        // Setup delete button
        rootView.findViewById(R.id.device_unregister_button).setOnClickListener(v->{
            openDeleteDialog();
        });

        // Set version texts and update button
        if(device.getSoftwareVersion().isEmpty() || device.getHardwareVersion().isEmpty()){
            rootView.findViewById(R.id.software_version_info_layout).setVisibility(View.GONE);
            rootView.findViewById(R.id.hardware_version_text).setVisibility(View.GONE);
        } else {
            String svText = rootView.getContext().getString(R.string.info_software_version, device.getSoftwareVersion());
            String hvText = rootView.getContext().getString(R.string.info_hardware_version, device.getHardwareVersion());

            ((TextView) rootView.findViewById(R.id.software_version_text)).setText(svText);
            ((TextView) rootView.findViewById(R.id.hardware_version_text)).setText(hvText);

            if(device.isUpdateAvailable()){
                rootView.findViewById(R.id.software_update_button).setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.software_update_button).setOnClickListener(v->actions.onUpdateAllowedChange(device));
            } else {
                rootView.findViewById(R.id.software_update_button).setVisibility(View.GONE);
            }
        }

        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog= (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        return dialog;
    }

    private void openDeleteDialog(){
        MaterialAlertDialogBuilder adb= new MaterialAlertDialogBuilder(dialog.getContext());

        adb.setTitle(dialog.getContext().getString(R.string.title_remove_device, device.getName()));
        adb.setMessage(R.string.message_remove_device);
        adb.setPositiveButton(R.string.button_remove, (dialog, which) -> {
            dismiss();
            actions.onDeleteClick(device);
        });
        adb.setNegativeButton(R.string.button_cancel, null);

        adb.create().show();
    }

    public void show(LightDevice lightDevice, ArrayList<Room> rooms, @NonNull FragmentManager manager, @Nullable String tag, SettingsActions settingsClosedListener){
        device= lightDevice;
        actions = settingsClosedListener;
        this.rooms = rooms;
        super.show(manager, tag);
    }

    private int findRoomIdx(int id){
        for(int i=0; i<rooms.size(); i++){
            if(rooms.get(i).getId()==id){
                return i;
            }
        }

        return -1;
    }

    private int findSectorName(int roomIdx, int sectorId){
        for(int i=0; i<rooms.get(roomIdx).getSectors().size(); i++){
            if(rooms.get(roomIdx).getSectors().get(i).getId()==sectorId){
                return i;
            }
        }

        return -1;
    }

    private void onNameResetClick(){
        nameInputEditText.setText(device.getName());

    }

    private void onRoomResetClick(){
        selectedRoom= devRoomIdx;
        selectedSector= devSectorIdx;

        roomsAutoComplete.setText(rooms.get(selectedRoom).getName(), false);
        sectorsAutoComplete.setText(rooms.get(selectedRoom).getSectors().get(selectedSector).getName(), false);

        roomResetButton.setVisibility(View.GONE);
        sectorResetButton.setVisibility(View.GONE);
        prepareSectorsListAdapter(selectedRoom);
    }

    private void onSectorResetClick(){
        selectedRoom= devRoomIdx;
        selectedSector= devSectorIdx;

        roomsAutoComplete.setText(rooms.get(selectedRoom).getName(), false);
        sectorsAutoComplete.setText(rooms.get(selectedRoom).getSectors().get(selectedSector).getName(), false);

        roomResetButton.setVisibility(View.GONE);
        sectorResetButton.setVisibility(View.GONE);
        prepareSectorsListAdapter(selectedRoom);
    }

    private void prepareRoomsListAdapter(){
        ArrayList<String> roomNames= new ArrayList<>();

        for(Room r: rooms){
            roomNames.add(r.getName());
        }

        roomsAutoComplete.setSimpleItems(roomNames.toArray(new String[0]));
        roomsAutoComplete.setOnItemClickListener((parent, view, position, id) -> onRoomItemSelected(position));
    }

    private void onRoomItemSelected(int pos){
        selectedRoom= pos;

        if(device.getRoomId()!= rooms.get(pos).getId()){
            roomResetButton.setVisibility(View.VISIBLE);
        } else {
            roomResetButton.setVisibility(View.GONE);
        }
        prepareSectorsListAdapter(pos);
        sectorsAutoComplete.setText(rooms.get(pos).getSectors().get(0).getName(), false);
        if(device.getSectorId()!=rooms.get(pos).getSectors().get(0).getId()){
            sectorResetButton.setVisibility(View.VISIBLE);
        } else {
            sectorResetButton.setVisibility(View.GONE);
        }
    }

    private void prepareSectorsListAdapter(int roomIdx){
        ArrayList<String> sectorNames= new ArrayList<>();
        ArrayList<Sector> userSectors= rooms.get(roomIdx).getSectors();
        for(Sector s: userSectors){
            sectorNames.add(s.getName());
        }

        sectorsAutoComplete.setSimpleItems(sectorNames.toArray(new String[0]));
        sectorsAutoComplete.setOnItemClickListener((parent, view, position, id) -> onSectorItemSelected(position));
    }

    private void onSectorItemSelected(int pos){
        selectedSector= pos;
        if(device.getSectorId()!=rooms.get(selectedRoom).getSectors().get(selectedSector).getId()){
            sectorResetButton.setVisibility(View.VISIBLE);
        } else {
            sectorResetButton.setVisibility(View.GONE);
        }
    }
}
