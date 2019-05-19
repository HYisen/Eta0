package net.alexhyisen.eta.sale;

import net.alexhyisen.Config;
import net.alexhyisen.Utility;
import net.alexhyisen.eta.mail.Mail;
import net.alexhyisen.eta.mail.MailService;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import javax.net.ssl.SSLProtocolException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Task {
    private String key;
    private long minPrize = 0, maxPrize = 0;

    private String argument;

    private Item stamp = null;
    private ScheduledExecutorService handle = null;

    private static Config config;
    private static MailService ms;

    private static final int MAX_PAGE = 20;

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
        Utility.log(Utility.LogCls.SALE, String.format("download %s with %d", url, document.wholeText().length()));
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
                    String time = v.child(1).child(2).child(1)
                            .getElementsByClass("feed-block-extras")
                            .first().ownText();
                    String href = v.child(1).child(0).child(0).attr("href");
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
                            break;
                        default:
                            Utility.log(Utility.LogCls.SALE, "unhandled type " + type);
                            Utility.log(Utility.LogCls.SALE, "\n" + v);
                    }
                    return new Item(type, name, cost, desc, from, time, href);
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
                        if (page >= MAX_PAGE) {
                            Utility.log(Utility.LogCls.SALE, "reach max page, abort current routine.\n" +
                                    "target item : " + stamp +
                                    "\nsubstitute item 0 :\n" + list.get(0) +
                                    "\nsubstitute item 1 :\n" + list.get(1) +
                                    "\nsubstitute item 2 :\n" + list.get(2));
                            stamp = list.get(0);//set stamp to first item
                            list.clear();
                            more.clear();
                            break;
                        }
                        list.addAll(more);
                    }
                    more.stream().takeWhile(v -> !v.equals(stamp)).forEach(list::add);
                }
            }
            if (!list.isEmpty()) {
                stamp = list.get(0);
            }
            Utility.log(Utility.LogCls.SALE, "succeed to collect data of " + toString());
            return list;
        } catch (SSLProtocolException e) {
            e.printStackTrace();
            if ("Read timed out".equals(e.getMessage())) {
                Utility.log(Utility.LogCls.SALE, "read timeout, skip");
            } else {
                Utility.log(Utility.LogCls.SALE, "SSLProtocolException other than timeout in " + toString());
            }
            return Collections.emptyList();
        } catch (HttpStatusException e) {
            e.printStackTrace();
            Utility.log(Utility.LogCls.SALE, "bad HTTP code, skip");
            return Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            Utility.log(Utility.LogCls.SALE, "because of IOException, failed to collect data of " + toString());
            return Collections.emptyList();
        }
    }

    private static AtomicLong globalCount = new AtomicLong(0);
    private long localCount = 0;

    @SuppressWarnings("WeakerAccess")
    public void run() {
        var list = collectAll();
        try {
            var size = list.size();
            Utility.log(String.format("%4d(%6d) | %s find %d",
                    ++localCount, globalCount.addAndGet(1), this.toString(), size));

            if (localCount == 1) {
                Utility.log(Utility.LogCls.SALE, String.format("skip the first scan of %s with %s",
                        this.toString(), Utility.genDesc(size, "result")));
            } else {
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
            }
        } catch (IOException e) {
            e.printStackTrace();
            Utility.log(Utility.LogCls.MAIL, "email fails in " + toString());
        } catch (Exception e) {
            e.printStackTrace();
            Utility.log("unpredicted exception, swallow & skip");
        }
    }

    public void start(long delaySecs) {
        Utility.log(Utility.LogCls.INFO, "start " + toString() +
                " with delay " + delaySecs + " sec" + (delaySecs > 1 ? "" : "s"));
        if (handle != null) {
            Utility.log(Utility.LogCls.SALE, "kill already started one");
            stop();
        }
        handle = Executors.newSingleThreadScheduledExecutor();
        handle.scheduleWithFixedDelay(this::run, 0, delaySecs, TimeUnit.SECONDS);
    }

    public void stop() {
        if (handle != null) {
            Utility.log(Utility.LogCls.SALE, "stop " + toString());
            handle.shutdown();
            handle = null;
        }
    }

    public Optional<String> getArgumentOptional() {
        return Optional.ofNullable(argument);
    }

    public void setArgument(String argument) {
        this.argument = argument;
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
