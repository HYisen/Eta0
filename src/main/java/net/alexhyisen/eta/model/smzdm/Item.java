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

    public Item(String type, String name, String cost, String desc, String from, String time) {
        this.type = type;
        this.name = name;
        this.cost = cost;
        this.desc = desc;
        this.from = from;
        this.time = time;
    }

    @Override
    public String toString() {
        return name + "\n" + time + " # " + type + " | " + from + " @ " + cost + "\n" + desc + "\n";
    }

    public static void main(String[] args) {

    }
}
