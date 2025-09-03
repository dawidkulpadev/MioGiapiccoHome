package pl.dawidkulpa.miogiapiccohome.adapters;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.EditTextWatcher;
import pl.dawidkulpa.miogiapiccohome.R;

import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.dialogs.AirDataPlotDialog;
import pl.dawidkulpa.miogiapiccohome.dialogs.NewSectorDialog;

public class RoomsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface DataChangeListener {
        void onDeviceUpdateEnableClick(Device d);
        void onDeviceDeleteClick(Device d);
        void onRoomDeleteClick(Room r);
        void onRoomNameChanged(Room r, String newName);

        void onLightDeviceDataChanged(LightDevice d);
        void onSoilDeviceDataChanged(SoilDevice d);
        void onAirDeviceDataChanged(AirDevice d);
        void onPlantDataChanged(Plant p);

        void onSectorNameChanged(Sector s, String name);
        void onSectorDeleteClick(Sector s);
    }


    static class RoomViewHolder extends RecyclerView.ViewHolder{
        View root;

        TextView nameText;

        RecyclerView sectorsRecyclerView;
        SectorsListAdapter sectorsListAdapter;

        NewSectorDialog newSectorDialog;
        Button newSectorButton;

        RecyclerView.LayoutManager layoutManager;
        DataChangeListener dataChangeListener;
        Button roomMoreButton;

        Room room;

        RoomViewHolder(View v){
            super(v);
            root= v;

            nameText= v.findViewById(R.id.room_name_text);
            sectorsRecyclerView= v.findViewById(R.id.room_sectors_list);
            layoutManager = new LinearLayoutManager(v.getContext());
            sectorsRecyclerView.setLayoutManager(layoutManager);
            newSectorButton= v.findViewById(R.id.new_sector_button);
            roomMoreButton= v.findViewById(R.id.room_menu_button);
        }

        void init(Room r, DataChangeListener dcl){
            dataChangeListener= dcl;
            room= r;

            roomMoreButton.setOnClickListener(v -> {
                        PopupMenu popup = new PopupMenu(root.getContext(), v);

                        popup.getMenuInflater().inflate(R.menu.room_menu, popup.getMenu());
                        popup.setGravity(Gravity.END);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            popup.setForceShowIcon(true);
                        }
                        popup.setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == R.id.action_remove) {
                                onRoomDeleteClick();
                                return true;
                            } else if(item.getItemId() == R.id.action_rename){
                                onRoomRenameClick();
                                return true;
                            }

                            return false;
                        });
                        popup.show();
                    });
        }

        void onRoomDeleteClick(){
            if(room.getSectors().isEmpty()) {
                MaterialAlertDialogBuilder madb = new MaterialAlertDialogBuilder(root.getContext());

                madb.setIcon(R.drawable.icon_remove);
                String title = root.getContext().getString(R.string.title_remove_room, room.getName());
                madb.setTitle(title);
                madb.setMessage(R.string.message_remove_room);
                madb.setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    dataChangeListener.onRoomDeleteClick(room);
                });

                madb.setNegativeButton(R.string.button_cancel, ((dialog1, which) -> {

                }));

                madb.show();
            } else {
                Snackbar.make(nameText, R.string.message_on_room_remove_not_empty, BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        }

        void onRoomRenameClick(){
            MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(root.getContext());

            madb.setIcon(R.drawable.icon_edit);
            String title= root.getContext().getString(R.string.title_rename_room, room.getName());
            madb.setTitle(title);
            madb.setView(R.layout.dialog_change_name);
            madb.setPositiveButton(R.string.button_set, (dialog, which) -> {
                TextInputEditText tiet= ((AlertDialog)dialog).findViewById(R.id.text_input);

                if(tiet!=null && tiet.getText()!=null) {
                    dataChangeListener.onRoomNameChanged(room, tiet.getText().toString());
                } else {
                    Snackbar.make(((AlertDialog) dialog).getButton(which),R.string.message_ui_error, Snackbar.LENGTH_SHORT).show();
                }
            });

            madb.setNegativeButton(R.string.button_cancel, ((dialog1, which) -> {

            }));

            AlertDialog dialog= madb.create();
            dialog.setOnShowListener(d -> {
                Button b= ((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(false);

                TextInputLayout til= ((AlertDialog)d).findViewById(R.id.text_input_layout);
                TextInputEditText tiet= ((AlertDialog)d).findViewById(R.id.text_input);
                if(til!=null && tiet!=null) {
                    til.setHint(R.string.hint_rooms_name);
                    tiet.addTextChangedListener(new EditTextWatcher(til, b, ((AlertDialog) d).getContext().getString(R.string.error_empty_name)));
                } else {
                    d.dismiss();
                    Snackbar.make(b, R.string.message_ui_error, Snackbar.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        }

        void createSectorsListAdapter(ArrayList<Sector> sectors, SectorsListAdapter.DataChangeListener dataChangeListener, AirDataPlotDialog.AirDataRequestListener ardl){
            sectorsListAdapter= new SectorsListAdapter(sectors, dataChangeListener, ardl);
            sectorsRecyclerView.setAdapter(sectorsListAdapter);
        }
    }

    private final Context context;
    private ArrayList<Room> rooms;
    private final DataChangeListener dcl;
    private final SectorsListAdapter.DataChangeListener sectorDcl;
    private final AirDataPlotDialog.AirDataRequestListener adrListener;
    private final NewSectorDialog.ClosedListener apiCreateSectorRequest;

    public RoomsListAdapter(Context context, ArrayList<Room> rooms, DataChangeListener dataChangeListener, AirDataPlotDialog.AirDataRequestListener adrl, NewSectorDialog.ClosedListener apiCreateSectorRequest){
        this.rooms= rooms;
        this.context= context;
        this.dcl = dataChangeListener;
        sectorDcl= new SectorsListAdapter.DataChangeListener() {
            @Override
            public void onDeviceUpdateEnableClick(Device d) {
                dcl.onDeviceUpdateEnableClick(d);
            }

            @Override
            public void onDeviceDeleteClick(Device d) {
                dcl.onDeviceDeleteClick(d);
            }

            @Override
            public void onLightDeviceDataChanged(LightDevice d) {
                dcl.onLightDeviceDataChanged(d);
            }

            @Override
            public void onSoilDeviceDataChanged(SoilDevice d) {
                dcl.onSoilDeviceDataChanged(d);
            }

            @Override
            public void onAirDeviceDataChanged(AirDevice d) {
                dcl.onAirDeviceDataChanged(d);
            }

            @Override
            public void onPlantDataChanged(Plant p) {
                dcl.onPlantDataChanged(p);
            }

            @Override
            public void onSectorNameChanged(Sector s, String newName) {
                dcl.onSectorNameChanged(s, newName);
            }

            @Override
            public void onSectorDeleteClick(Sector s) {
                dcl.onSectorDeleteClick(s);
            }

            @Override
            public ArrayList<Room> requestRoomsList() {
                return rooms;
            }
        };
        this.adrListener= adrl;
        this.apiCreateSectorRequest= apiCreateSectorRequest;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_room, parent, false);
        return new RoomViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RoomViewHolder h= ((RoomViewHolder) holder);
        h.nameText.setText(rooms.get(position).getName());
        h.init(rooms.get(position), dcl);
        h.createSectorsListAdapter(rooms.get(position).getSectors(), sectorDcl, adrListener);

        h.newSectorDialog= new NewSectorDialog(rooms.get(position).getId(), context, apiCreateSectorRequest);
        h.newSectorButton.setOnClickListener(v -> h.newSectorDialog.show());
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void updateList(ArrayList<Room> newRooms){
        rooms= newRooms;
    }
}
