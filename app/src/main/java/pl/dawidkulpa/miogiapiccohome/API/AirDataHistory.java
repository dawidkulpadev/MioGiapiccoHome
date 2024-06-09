package pl.dawidkulpa.miogiapiccohome.API;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class AirDataHistory implements Iterable<AirData> {

    private final ArrayList<AirData> data;

    public AirDataHistory(JSONObject jobj) throws JSONException, ParseException{
        data= new ArrayList<>();
        JSONArray jarr = jobj.getJSONArray("data");

        for(int i=0; i<jarr.length(); i++){
            data.add(new AirData(jarr.getJSONObject(i)));
        }
    }

    public AirData get(int i){
        return data.get(i);
    }

    public int size(){
        return data.size();
    }

    @NonNull
    @Override
    public Iterator<AirData> iterator() {
        return data.iterator();
    }

    @Override
    @RequiresApi(24)
    public void forEach(@NonNull Consumer<? super AirData> action) {
        for(AirData d: data){
            action.accept(d);
        }
    }

    @NonNull
    @Override
    @RequiresApi(24)
    public Spliterator<AirData> spliterator() {
        return data.spliterator();
    }
}
