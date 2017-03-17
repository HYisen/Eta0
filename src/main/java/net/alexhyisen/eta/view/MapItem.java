package net.alexhyisen.eta.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.alexhyisen.eta.model.Config;

import java.util.Map;

/**
 * Created by Alex on 2017/3/17.
 */
public class MapItem {
    private Map<String,String> orig;
    private final SimpleStringProperty key;
    private final SimpleStringProperty value;

    public MapItem(Map<String,String> orig,String key, String value){
        this.orig=orig;
        this.key=new SimpleStringProperty(key);
        this.value=new SimpleStringProperty(value);
    }

    public String getKey() {
        return key.get();
    }

    public String getValue() {
        return value.get();
    }

    public void setKey(String key) {
        System.out.println("setKey");
        orig.put(key,getValue());
        if(!orig.remove(getKey(),getValue())){
            throw new RuntimeException("failed to remove entry in Config");
        }
        this.key.set(key);
    }

    public void setValue(String value) {
        orig.put(getKey(),value);
        System.out.println("setValue to "+orig.get(getKey()));
        this.value.set(value);
    }

    public StringProperty keyProperty(){
        return key;
    }
    public StringProperty valueProperty(){
        return value;
    }
}
