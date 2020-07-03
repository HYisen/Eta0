package net.alexhyisen.eta.core;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.alexhyisen.Keeper;
import net.alexhyisen.Utility;
import net.alexhyisen.eta.book.Book;
import net.alexhyisen.eta.book.Source;
import net.alexhyisen.eta.book.SourceElement;
import net.alexhyisen.log.LogCls;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RestfulRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String prefix;
    private final String adminUsername;
    private final String adminPassword;

    private final Source source;

    public static String HEADER_CREDENTIAL_NAME = "credential";
    private static Keeper keeper = new Keeper();


    public RestfulRequestHandler(String prefix, String adminUsername, String adminPassword, Source source) throws IOException {
        this.prefix = prefix;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.source = source;

        keeper.load();
    }

    /**
     * Would also respond with 403 FORBIDDEN if can not pass.
     *
     * @param ctx     the content to possible send failed response if failed the check
     * @param request the request need to be check
     * @return if the request is authorized
     */
    private boolean checkAuthorized(ChannelHandlerContext ctx, FullHttpRequest request) {
        String token = request.headers().get(HEADER_CREDENTIAL_NAME);
        if (token != null && !keeper.isAuthorized(token)) {
            Utils.respond(ctx, request, HttpResponseStatus.FORBIDDEN,
                    "text/plain", String.format("bad token %s", token).getBytes());
            return false;
        }
        return true;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.uri().startsWith("/" + prefix + "/")) {
            ctx.fireChannelRead(request.retain());
            return;
        }

        String uri = request.uri().substring(request.uri().indexOf('/', 1));
        Utility.log(LogCls.LOOP, "rrh accepted " + uri);

        if (request.method().equals(HttpMethod.OPTIONS)) {
            Utils.guaranteeCorsPreflight(ctx, request);
        } else if (uri.equals("/info") && request.method().equals(HttpMethod.GET)) {
            String infoMsg = "{\"desc\":\"info message\"}";
            Utils.respondOkJson(ctx, request, infoMsg.getBytes());
        } else if (uri.equals("/auth") && request.method().equals(HttpMethod.POST)) {
            String json = request.content().toString(StandardCharsets.UTF_8);
            var credential = new Credential(json);
            Optional<String> result = genCredential(credential.getUsername(), credential.getPassword());
            if (result.isPresent()) {
                Utils.respond(ctx, request, HttpResponseStatus.OK, "text/plain", result.get().getBytes());
            } else {
                Utils.respond(ctx, request, HttpResponseStatus.FORBIDDEN, "application/json", json.getBytes());
            }
        } else if (uri.equals("/auth") && request.method().equals(HttpMethod.PUT)) {
            String token = request.headers().get(HEADER_CREDENTIAL_NAME);
            String json = request.content().toString(StandardCharsets.UTF_8);
            var credential = new Credential(json);
            if (checkAuthorized(ctx, request)) {
                try {
                    keeper.put(credential.getUsername(), credential.getPassword());
                    Utils.respond(ctx, request, HttpResponseStatus.CREATED,
                            "application/json", json.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    Utils.respond(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            "text/plain", e.toString().getBytes());
                    return;
                }
                Utility.log(LogCls.AUTH, String.format("%s add %s", token, credential.getUsername()));
            }
        } else if (uri.equals("/resource")) {
            if (checkAuthorized(ctx, request)) {
                if (request.method().equals(HttpMethod.GET)) {
                    var data = source.getData().stream().map(SourceElement::new).collect(Collectors.toList());
                    Utils.respondOkJson(ctx, request, new Gson().toJson(data).getBytes());
                } else if (request.method().equals(HttpMethod.POST)) {
                    String json = request.content().toString(StandardCharsets.UTF_8);
                    SourceElement neo = new Gson().fromJson(json, SourceElement.class);
                    source.getData().add(new Book(neo.getLink(), neo.getPath(), neo.getName()));
                    Utility.log(LogCls.BOOK, "add book " + json);
                    Utils.respondOkJson(ctx, request, Integer.toString(source.getData().size()).getBytes());
                }
            }
        }
    }

    private static class Credential {
        private final String username;
        private final String password;

        public Credential(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Credential(String json) {
            String inner = extractInner(json);
            Map<String, String> dict = Arrays
                    .stream(inner.split(","))
                    .map(v -> v.split(":"))
                    .collect(Collectors.toMap(v -> extractInner(v[0]), v -> extractInner(v[1])));
            this.username = dict.get("username");
            this.password = dict.get("password");
        }

        private static String extractInner(String orig) {
            return orig.substring(1, orig.length() - 1);
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    private Optional<String> genCredential(String username, String password) {
        String ret = null;

        if (adminUsername.equals(username) && adminPassword.equals(password) || keeper.isAuthorized(username, password)) {
            ret = keeper.register();
            Utility.log(LogCls.AUTH, String.format("auth %s with %s", username, ret));
        } else {
            Utility.log(LogCls.AUTH, String.format("failed to auth %s|%s", username, password));
        }

        return Optional.ofNullable(ret);
    }
}
