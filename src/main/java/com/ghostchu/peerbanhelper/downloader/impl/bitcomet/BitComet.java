package com.ghostchu.peerbanhelper.downloader.impl.bitcomet;

import com.ghostchu.peerbanhelper.downloader.AbstractDownloader;
import com.ghostchu.peerbanhelper.downloader.DownloaderLoginResult;
import com.ghostchu.peerbanhelper.downloader.DownloaderStatistics;
import com.ghostchu.peerbanhelper.downloader.impl.bitcomet.crypto.BCAESTool;
import com.ghostchu.peerbanhelper.downloader.impl.bitcomet.resp.*;
import com.ghostchu.peerbanhelper.peer.Peer;
import com.ghostchu.peerbanhelper.peer.PeerImpl;
import com.ghostchu.peerbanhelper.text.Lang;
import com.ghostchu.peerbanhelper.text.TranslationComponent;
import com.ghostchu.peerbanhelper.torrent.Torrent;
import com.ghostchu.peerbanhelper.torrent.TorrentImpl;
import com.ghostchu.peerbanhelper.util.ByteUtil;
import com.ghostchu.peerbanhelper.util.HTTPUtil;
import com.ghostchu.peerbanhelper.util.json.JsonUtil;
import com.ghostchu.peerbanhelper.wrapper.BanMetadata;
import com.ghostchu.peerbanhelper.wrapper.PeerAddress;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.ghostchu.peerbanhelper.text.Lang.DOWNLOADER_BC_FAILED_SAVE_BANLIST;
import static com.ghostchu.peerbanhelper.text.TextManager.tlUI;

public class BitComet extends AbstractDownloader {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitComet.class);
    private static final UUID clientId = UUID.randomUUID();
    protected final String apiEndpoint;
    protected final HttpClient httpClient;
    private final Config config;
    private String deviceToken;
    private String serverId;
    private String serverVersion;
    private String serverName;

    /*
            API 受限，实际实现起来意义不大

            */
    public BitComet(String name, Config config) {
        super(name);
        this.config = config;
        this.apiEndpoint = config.getEndpoint();
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        Methanol.Builder builder = Methanol.newBuilder()
                .version(HttpClient.Version.valueOf(config.getHttpVersion()))
                .defaultHeader("Accept-Encoding", "gzip,deflate")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Client-Type", "BitComet WebUI")
                .defaultHeader("User-Agent", "PeerBanHelper BitComet Adapter")
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.of(15, ChronoUnit.SECONDS))
                .headersTimeout(Duration.of(15, ChronoUnit.SECONDS))
                .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
                .requestTimeout(Duration.of(30, ChronoUnit.SECONDS));
        if (!config.isVerifySsl() && HTTPUtil.getIgnoreSslContext() != null) {
            builder.sslContext(HTTPUtil.getIgnoreSslContext());
        }
        this.httpClient = builder.build();
    }

    public static BitComet loadFromConfig(String name, ConfigurationSection section) {
        Config config = Config.readFromYaml(section);
        return new BitComet(name, config);
    }

    public static BitComet loadFromConfig(String name, JsonObject section) {
        Config config = JsonUtil.getGson().fromJson(section.toString(), Config.class);
        return new BitComet(name, config);
    }

    private static PeerAddress parseAddress(String address) {
        String ip;
        String port;
        if (address.startsWith("[")) {
            // 处理 IPv6 地址
            int endIndex = address.indexOf(']');
            ip = address.substring(1, endIndex);
            port = address.substring(endIndex + 2);
        } else {
            // 处理 IPv4 地址
            int colonIndex = address.indexOf(':');
            ip = address.substring(0, colonIndex);
            port = address.substring(colonIndex + 1);
        }
        return new PeerAddress(ip, Integer.parseInt(port));
    }

    @Override
    public JsonObject saveDownloaderJson() {
        return JsonUtil.getGson().toJsonTree(config).getAsJsonObject();
    }

    @Override
    public YamlConfiguration saveDownloader() {
        return config.saveToYaml();
    }

    public DownloaderLoginResult login0() throws Exception {
        if (isLoggedIn())
            return new DownloaderLoginResult(DownloaderLoginResult.Status.SUCCESS, new TranslationComponent(Lang.STATUS_TEXT_OK)); // 重用 Session 会话
        Map<String, String> loginAttemptCred = new HashMap<>();
        loginAttemptCred.put("username", config.getUsername());
        loginAttemptCred.put("password", config.getPassword());
        String aesEncrypted = BCAESTool.credential(JsonUtil.standard().toJson(loginAttemptCred), clientId.toString());
        Map<String, String> loginJsonObject = new HashMap<>();
        loginJsonObject.put("authentication", aesEncrypted);
        loginJsonObject.put("client_id", clientId.toString());
        try {
            HttpResponse<String> request = httpClient.send(
                    MutableRequest.POST(apiEndpoint + BCEndpoint.USER_LOGIN.getEndpoint(),
                            HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(loginJsonObject)))
                    , HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var loginResponse = JsonUtil.standard().fromJson(request.body(), BCLoginResponse.class);
            if (loginResponse.getErrorCode().equals("PASSWORD_ERROR")) {
                return new DownloaderLoginResult(DownloaderLoginResult.Status.INCORRECT_CREDENTIAL, new TranslationComponent(Lang.DOWNLOADER_LOGIN_EXCEPTION, request.body()));
            }
            Map<String, String> inviteTokenRetrievePayload = new HashMap<>();
            inviteTokenRetrievePayload.put("device_id", clientId.toString());
            inviteTokenRetrievePayload.put("device_name", "PeerBanHelper - BitComet Adapter");
            inviteTokenRetrievePayload.put("invite_token", loginResponse.getInviteToken());
            inviteTokenRetrievePayload.put("platform", "webui");
            HttpResponse<String> retrieveDeviceToken = httpClient.send(
                    MutableRequest.POST(apiEndpoint + BCEndpoint.GET_DEVICE_TOKEN.getEndpoint(),
                                    HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(inviteTokenRetrievePayload)))
                            .header("Authorization", "Bearer " + loginResponse.getInviteToken()),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("Retrieve device token: " + retrieveDeviceToken.body());
            var deviceTokenResponse = JsonUtil.standard().fromJson(retrieveDeviceToken.body(), BCDeviceTokenResult.class);
            this.deviceToken = deviceTokenResponse.getDeviceToken();
            this.serverId = deviceTokenResponse.getServerId();
            this.serverVersion = deviceTokenResponse.getVersion();
            this.serverName = deviceTokenResponse.getServerName();
            return new DownloaderLoginResult(DownloaderLoginResult.Status.SUCCESS, new TranslationComponent(Lang.STATUS_TEXT_OK));
            // return request.statusCode() == 200;
        } catch (Exception e) {
            return new DownloaderLoginResult(DownloaderLoginResult.Status.EXCEPTION, new TranslationComponent(Lang.DOWNLOADER_LOGIN_IO_EXCEPTION, e.getClass().getName() + ": " + e.getMessage()));
        }
    }

    @Override
    public String getEndpoint() {
        return apiEndpoint;
    }

    @Override
    public String getType() {
        return "BitComet";
    }

    public boolean isLoggedIn() {
        try {
            getTorrents();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setBanList(@NotNull Collection<PeerAddress> fullList, @Nullable Collection<BanMetadata> added, @Nullable Collection<BanMetadata> removed, boolean applyFullList) {
//        if (removed != null && removed.isEmpty() && added != null && config.isIncrementBan() && !applyFullList) {
//            setBanListIncrement(added);
//        } else {
        setBanListFull(fullList);
        //}
    }

    private void enableIpFilter() throws IOException, InterruptedException {
        Map<String, Object> settings = new HashMap<>() {{
            put("ip_filter_config", new HashMap<>() {{
                put("enable_ip_filter", true);
                put("enable_whitelist_mode", false);
            }});
        }};
        HttpResponse<String> updatePreferencesToEnableIpFilter =
                httpClient.send(
                        MutableRequest.POST(apiEndpoint + BCEndpoint.GET_TASK_LIST.getEndpoint(),
                                        HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(settings)))
                                .header("Authorization", "Bearer " + this.deviceToken),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
    }

    @Override
    public List<Torrent> getTorrents() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("group_state", "ACTIVE");
        requirements.put("sort_key", "");
        requirements.put("sort_order", "unsorted");

        HttpResponse<String> request;
        try {
            request = httpClient.send(
                    MutableRequest.POST(apiEndpoint + BCEndpoint.GET_TASK_LIST.getEndpoint(),
                                    HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(requirements)))
                            .header("Authorization", "Bearer " + this.deviceToken)
                    , HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (request.statusCode() != 200) {
            throw new IllegalStateException(tlUI(Lang.DOWNLOADER_BC_FAILED_REQUEST_TORRENT_LIST, request.statusCode(), request.body()));
        }
        var response = JsonUtil.standard().fromJson(request.body(), BCTaskListResponse.class);
        return response.getTasks().stream().map(torrent -> {
            try {
                Map<String, String> taskIds = new HashMap<>();
                taskIds.put("task_id", torrent.getTaskId().toString());
                HttpResponse<String> fetch = httpClient.send(MutableRequest.POST(apiEndpoint + BCEndpoint.GET_TASK_SUMMARY.getEndpoint(),
                                        HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(taskIds)))
                                .header("Authorization", "Bearer " + this.deviceToken),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                return JsonUtil.standard().fromJson(fetch.body(), BCTaskTorrentResponse.class);
            } catch (IOException | InterruptedException e) {
                log.warn("Unable to fetch task summary", e);
                return null;
            }
        }).filter(Objects::nonNull).map(torrent -> new TorrentImpl(torrent.getTask().getTaskId().toString(),
                torrent.getTask().getTaskName(),
                torrent.getTaskDetail().getInfohash() != null ? torrent.getTaskDetail().getInfohash() : torrent.getTaskDetail().getInfohashV2(),
                torrent.getTaskDetail().getTotalSize(),
                Double.parseDouble(torrent.getTaskStatus().getProgress().replaceFirst("%", "")) / 100,
                -1,
                -1,
                torrent.getTaskDetail().getTorrentPrivate()
        )).collect(Collectors.toList());
    }

    @Override
    public DownloaderStatistics getStatistics() {
        return new DownloaderStatistics(0L, 0L);
    }

    @Override
    public List<Peer> getPeers(Torrent torrent) {
        HttpResponse<String> resp;
        try {
            Map<String, String> requirements = new HashMap<>();
            requirements.put("task_id", torrent.getId());
            resp = httpClient.send(MutableRequest.POST(apiEndpoint + BCEndpoint.GET_TASK_PEERS.getEndpoint(),
                                    HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(requirements)))
                            .header("Authorization", "Bearer " + this.deviceToken),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (resp.statusCode() != 200) {
            throw new IllegalStateException(tlUI(Lang.DOWNLOADER_BC_FAILED_REQUEST_PEERS_LIST_IN_TORRENT, resp.statusCode(), resp.body()));
        }
        var peers = JsonUtil.standard().fromJson(resp.body(), BCTaskPeersResponse.class);
        return peers.getPeers().stream().map(peer -> new PeerImpl(parseAddress(peer.getIp()),
                peer.getIp(),
                new String(ByteUtil.hexToByteArray(peer.getPeerId()), StandardCharsets.ISO_8859_1),
                peer.getClientType(),
                peer.getDlRate(),
                -1,
                peer.getUpRate(),
                -1,
                Double.parseDouble(peer.getProgress().replaceFirst("%", "")) / 100, null)
        ).collect(Collectors.toList());
    }

    protected void setBanListFull(Collection<PeerAddress> peerAddresses) {
        Map<String, String> banListSettings = new HashMap<>();
        banListSettings.put("import_type", "replace");
        StringJoiner joiner = new StringJoiner("\n");
        peerAddresses.forEach(p -> joiner.add(p.getIp()));
        banListSettings.put("content_base64", Base64.getEncoder().encodeToString(joiner.toString().getBytes(StandardCharsets.UTF_8)));
        try {
            enableIpFilter();
            HttpResponse<String> request = httpClient.send(MutableRequest.POST(apiEndpoint + BCEndpoint.IP_FILTER_UPLOAD.getEndpoint(),
                                    HttpRequest.BodyPublishers.ofString(JsonUtil.standard().toJson(banListSettings)))
                            .header("Authorization", "Bearer " + this.deviceToken),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (request.statusCode() != 200) {
                log.error(tlUI(DOWNLOADER_BC_FAILED_SAVE_BANLIST, name, apiEndpoint, request.statusCode(), "HTTP ERROR", request.body()));
                throw new IllegalStateException("Save BitComet banlist error: statusCode=" + request.statusCode());
            }
        } catch (Exception e) {
            log.error(tlUI(DOWNLOADER_BC_FAILED_SAVE_BANLIST, name, apiEndpoint, "N/A", e.getClass().getName(), e.getMessage()), e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws Exception {

    }


    @NoArgsConstructor
    @Data
    public static class Config {

        private String type;
        private String endpoint;
        private String username;
        private String password;
        private String httpVersion;
        private boolean verifySsl;
        private String rpcUrl;
        private boolean ignorePrivate;

        public static Config readFromYaml(ConfigurationSection section) {
            Config config = new Config();
            config.setType("bitcomet");
            config.setEndpoint(section.getString("endpoint"));
            if (config.getEndpoint().endsWith("/")) { // 浏览器复制党 workaround 一下， 避免连不上的情况
                config.setEndpoint(config.getEndpoint().substring(0, config.getEndpoint().length() - 1));
            }
            config.setUsername(section.getString("username", ""));
            config.setPassword(section.getString("password", ""));
            config.setHttpVersion(section.getString("http-version", "HTTP_1_1"));
            config.setVerifySsl(section.getBoolean("verify-ssl", true));
            config.setIgnorePrivate(section.getBoolean("ignore-private", false));
            return config;
        }

        public YamlConfiguration saveToYaml() {
            YamlConfiguration section = new YamlConfiguration();
            section.set("type", "bitcomet");
            section.set("endpoint", endpoint);
            section.set("username", username);
            section.set("password", password);
            section.set("rpc-url", rpcUrl);
            section.set("http-version", httpVersion);
            section.set("verify-ssl", verifySsl);
            section.set("ignore-private", ignorePrivate);
            return section;
        }
    }
}