package net.alexhyisen.eta.website;

import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Overseer {
    private final Path path;
    private boolean updated = false;
    private String indexPage = "";
    private List<Paper> papers = new ArrayList<>();

    public String genIndexPage() {
        if (updated) {
            String content = papers.stream().map(v -> String.format(
                    "        <tr>\n" +
                            "            <td><a href=\"%s\">%s</a></td>\n" +
                            "            <td>%s</td>\n" +
                            "        </tr>",
                    v.getHref(), v.getName(), v.getDate()
            )).collect(Collectors.joining("\n"));
            indexPage = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>index</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <table>\n" +
                    "        <tr>\n" +
                    "            <th>Title</th>\n" +
                    "            <th>Date</th>\n" +
                    "        </tr>\n" +
                    content + "\n" +
                    "    </table>\n" +
                    "</body>\n" +
                    "</html>";
        }
        return indexPage;
    }

    public Overseer(Path path) throws IOException {
        if (!path.toFile().isDirectory()) {
            throw new IOException("expect path " + path.toAbsolutePath() + " to be a directory");
        }
        this.path = path;
    }

    public static void main(String[] args) throws IOException {
        Overseer overseer = new Overseer(Path.of("."));
        overseer.maintain();
    }

    public void maintain() throws IOException {
        papers.clear();
        maintain(path);
        updated = true;

        Files.writeString(path.resolve("index.html"), genIndexPage(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void maintain(Path rootPath) throws IOException {
        File[] files = rootPath.toFile().listFiles();
        if (files == null) {
            throw new IOException(new NullPointerException("result of listFiles() shall not be null"));
        }

        Set<String> total = new HashSet<>();
        Set<String> exist = new HashSet<>();
        List<File> subDirs = new ArrayList<>();
        for (File childFile : files) {
            if (childFile.isDirectory() && !childFile.getName().endsWith(".assets") && childFile.getName().startsWith(".")) {
                subDirs.add(childFile);
            } else {
                String filename = childFile.getName();
                String name = Utility.extractName(childFile.toPath());
                if (filename.endsWith(".md")) {
                    String title = filename.substring(0, filename.length() - 3);
                    String href = "/" + path.getParent().relativize(rootPath).toString() + "/" + title;
                    var instant = Instant.ofEpochMilli(childFile.lastModified());
                    String date = LocalDateTime.ofInstant(instant, ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE);
                    papers.add(new Paper(title, href, date));

                    total.add(name);
                } else if (filename.endsWith(".html")) {
                    exist.add(name);
                }
            }
        }

        total.removeAll(exist);

        for (String name : total) {
            Path target = Path.of(rootPath.toString(), name + ".md");
            Utility.log(LogCls.LOOP, "generate " + target.toAbsolutePath());
            Transformer.generate(target);
        }
        for (File subDir : subDirs) {
            maintain(subDir.toPath());
        }
    }
}
