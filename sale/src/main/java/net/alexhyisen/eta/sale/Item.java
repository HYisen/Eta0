package net.alexhyisen.eta.sale;

public class Item {
    private String type;
    private String name;
    private String cost;
    private String desc;
    private String from;
    private String time;
    private String href;

    @SuppressWarnings("WeakerAccess")
    public Item(String type, String name, String cost, String desc, String from, String time, String href) {
        this.type = type;
        this.name = name;
        this.cost = cost;
        this.desc = desc;
        this.from = from;
        this.time = time;
        this.href = href;
    }

    @Override
    public boolean equals(Object obj) {
        //Not designed for classes that derived from Item.
        if (obj.getClass() != Item.class) {
            return false;
        } else {
            Item orig = (Item) obj;
            return orig.href.equals(this.href) &&
//                    orig.name.equals(this.name) && //name changes when a prediction matured
//                    orig.time.equals(this.time) && //time changes as today's become yesterday's
//                    orig.type.equals(this.type) && //type may change as item becomes invalid
                    orig.cost.equals(this.cost) &&
                    orig.from.equals(this.from) &&
                    orig.desc.equals(this.desc);
        }
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

    @SuppressWarnings("unused")
    public String getHref() {
        return href;
    }

    @Override
    public String toString() {
        return name + "\n" + time + " # " + type + " | " + from + " @ " + cost + "\n" + desc + "\n" + href + "\n";
    }
}
