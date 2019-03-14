package net.alexhyisen.eta.model.smzdm;

public class Item {
    private String type;
    private String name;
    private String cost;
    private String desc;
    private String from;
    private String time;

    @Override
    public boolean equals(Object obj) {
        //Not designed for classes that derived from Item.
        if (obj.getClass() != Item.class) {
            return false;
        } else {
            Item orig = (Item) obj;
            return orig.time.equals(this.time) &&
                    orig.name.equals(this.name) &&
//                    orig.type.equals(this.type) && //type may change as item becomes invalid
                    orig.cost.equals(this.cost) &&
                    orig.from.equals(this.from) &&
                    orig.desc.equals(this.desc);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Item(String type, String name, String cost, String desc, String from, String time) {
        this.type = type;
        this.name = name;
        this.cost = cost;
        this.desc = desc;
        this.from = from;
        this.time = time;
    }

    @SuppressWarnings("unused")
    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("WeakerAccess")
    public String getCost() {
        return cost;
    }

    @SuppressWarnings("unused")
    public String getDesc() {
        return desc;
    }

    public String getFrom() {
        return from;
    }

    @SuppressWarnings("unused")
    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return name + "\n" + time + " # " + type + " | " + from + " @ " + cost + "\n" + desc + "\n";
    }
}
