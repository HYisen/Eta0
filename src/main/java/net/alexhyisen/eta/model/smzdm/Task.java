package net.alexhyisen.eta.model.smzdm;

import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Utility;
import net.alexhyisen.eta.model.mailer.Mail;
import net.alexhyisen.eta.model.mailer.MailService;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Task {
    private String key;
    private long minPrize = 0, maxPrize = 0;

    private Item stamp = null;
    private ScheduledExecutorService handle = null;

    private static Config config;
    private static MailService ms;

    static {
        config = new Config();
        config.load();
        ms = new MailService(config);
    }

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

//        var dice = new Random().nextInt(6) + 1;
//        System.out.println(dice + toString());
//        if (dice == 6) {
//            throw new IOException("Surprise!");
//        }

        return elements
                .stream()
                .map(v -> {
                    String type = v.child(0).child(0).text();
                    String name = v.child(1).child(0).child(0).text();
                    String cost = "";
                    String desc = v.child(1).child(1).text();
                    String from = "";
                    String time = v.child(1).child(2).child(1).getElementsByClass("feed-block-extras").text();
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
                        case "资讯":
                            cost = "N/A";
                            from = "N/A";
                            break;
                        case "百科商品":
                            try {
                                cost = v.child(1).child(0).child(1).text();
                            } catch (IndexOutOfBoundsException e) {
                                cost = "N/A";
                            }
                            try {
                                from = v.child(1).child(2).child(1).child(1).child(0).text();
                            } catch (IndexOutOfBoundsException e) {
                                from = "N/ A";
                            }
                        default:
                            System.out.println(type);
                            System.out.println(v);
                    }
                    return new Item(type, name, cost, desc, from, time);
                })
//                .peek(System.out::println)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Item> collectAll() {
        try {
            long page = 1;
            var list = collectOne(1);
            if (stamp != null) {
                //There should be a more elegant way to describe the following procedure.
                if (list.contains(stamp)) {
                    list = list.subList(0, list.indexOf(stamp));
                } else {
                    List<Item> more;
                    while (!(more = collectOne(++page)).contains(stamp)) {
                        list.addAll(more);
                    }
                    more.stream().takeWhile(v -> !v.equals(stamp)).forEach(list::add);
                }
            }else {
                Utility.log("Bootstrap goes on well, find "+list.size()+" but skipped.");
                list.clear();
            }
            if (!list.isEmpty()) {
                stamp = list.get(0);
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            Utility.log("fail to collect data of " + toString());
            return Collections.emptyList();
        }
    }

     private static long cnt = 0;

    public void run() {
        try {
            var list = collectAll();
            var size = list.size();
            Utility.log(String.format("%8d | ", ++cnt) + this.toString() + " " + "find " + size);

            if (size != 0) {
                if (size == 1) {
                    Item item = list.get(0);
                    //Should I use item.getName instead key?
                    ms.send(new Mail(config, key + " @ " + item.getCost(), item.toString()));
                } else {
                    var content = list
                            .stream()
                            .map(Item::toString)
                            .toArray(String[]::new);
                    ms.send(new Mail(config, key + " # " + size, content));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Utility.log("email fails in " + toString());
        }
    }

    public void start(long intervalSecs) {
        Utility.log("start " + toString() +
                " at interval " + intervalSecs + " sec" + (intervalSecs > 1 ? "" : "s"));
        if (handle != null) {
            Utility.log("kill already started one");
            stop();
        }
        handle = Executors.newSingleThreadScheduledExecutor();
        handle.scheduleAtFixedRate(this::run, intervalSecs, intervalSecs, TimeUnit.SECONDS);
    }

    public void stop() {
        if (handle != null) {
            Utility.log("stop " + toString());
            handle.shutdown();
            handle = null;
        }
    }

    @Override
    public String toString() {
        return "Task " + key + " (" + minPrize + "," + maxPrize + ")";
    }

    public static void main(String[] args) {
//        new Task("RT-AC86U").run();
//        var task = new Task("RT-AC86U");
        var more = new Task("surface");
//        task.start(5);
        more.start(10);
        var input = new Scanner(System.in).nextLine();

        System.out.println(input);
//        task.stop();
        more.stop();
    }
}
