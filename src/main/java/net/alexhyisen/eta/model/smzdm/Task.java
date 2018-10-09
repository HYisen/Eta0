package net.alexhyisen.eta.model.smzdm;

import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Utility;
import net.alexhyisen.eta.model.mailer.MailService;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Task {
    private String key;
    private long minPrize = 0, maxPrize = 0;

    private Item stamp = null;
    private static MailService ms = new MailService(new Config());

    public Task(String key, long minPrize, long maxPrize) {
        this.key = key;
        this.minPrize = minPrize;
        this.maxPrize = maxPrize;
    }

    public Task(String key) {
        this.key = key;
    }

    private String genUrl(long page) {
        var rtn = String.format("https://search.smzdm.com/?c=home&s=%s&v=b", key);
        if (minPrize != 0 || maxPrize != 0) {
            rtn += "&min_price=" + (minPrize == 0 ? "" : String.valueOf(minPrize));
            rtn += "&max_price=" + (maxPrize == 0 ? "" : String.valueOf(maxPrize));
        }
        if (page != 1) {
            rtn += "&p=" + page;
        }
        return rtn;
    }

    private List<Item> collectOne(long page) throws IOException {
        var url = genUrl(page);
//        System.out.println("url = " + url);
        var document = Jsoup.connect(url).get();
//        System.out.println("title = " + document.title());
        var elements = document.getElementsByClass("feed-block z-hor-feed");

        return elements
                .stream()
                .map(v -> {
                    String type = v.child(0).child(0).text();
                    String name = v.child(1).child(0).child(0).text();
                    String cost = "";
                    String desc = v.child(1).child(1).text();
                    String from = "";
                    String time = v.child(1).child(2).child(1).child(1).text();
                    switch (type) {
                        case "极速发":
                        case "好价频道":
                        case "国内优惠":
                        case "过期":
                        case "海淘优惠":
                        case "售罄":
                            cost = v.child(1).child(0).child(1).text();
                            from = v.child(1).child(2).child(1).child(1).child(0).text();
                            break;
                        case "原创":
                        case "百科点评":
                            cost = "N/A";
                            from = v.child(1).child(2).child(1).child(0).child(1).text();
                            break;
                        default:
                            System.out.println(type);
                            System.out.println(v);
                    }
                    return new Item(type, name, cost, desc, from, time);
                })
                .peek(System.out::println)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Item> collectAll() throws IOException {
        long page = 1;
        var list = collectOne(1);
        if (stamp != null) {
            if (!list.contains(stamp)) {
                List<Item> more;
                while (!(more = collectOne(++page)).contains(stamp)) {
                    list.addAll(more);
                }
                more.stream().takeWhile(v -> !v.equals(stamp)).forEach(list::add);
            }
        }
        if (!list.isEmpty()) {
            stamp = list.get(0);
        }
        return list;
    }

    public void run() {
        try {
            var list = collectAll();
            var size = list.size();
            Utility.log(this.toString() + " " + "find " + size);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Task " + key + " (" + minPrize + "," + maxPrize + ")";
    }

    public static void main(String[] args) {
//        byte[] clean = Utility.clean("https://search.smzdm.com/?c=home&s=%E6%98%BE%E5%8D%A1&v=b");
//        System.out.println(new String(clean));
        new Task("RT-AC86U").run();
    }
}
