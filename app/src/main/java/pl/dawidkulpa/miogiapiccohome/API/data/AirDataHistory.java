package pl.dawidkulpa.miogiapiccohome.API.data;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class AirDataHistory implements Iterable<AirData> {

    private final ArrayList<AirData> data;

    public AirDataHistory(JsonObject jobj) throws ParseException{
        data= new ArrayList<>();
        JsonArray jarr = jobj.getAsJsonArray("data");

        for(int i=0; i<jarr.size(); i++){
            data.add(new AirData(jarr.get(i).getAsJsonObject()));
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
    public void forEach(@NonNull Consumer<? super AirData> action) {
        for(AirData d: data){
            action.accept(d);
        }
    }

    @NonNull
    @Override
    public Spliterator<AirData> spliterator() {
        return data.spliterator();
    }
}
