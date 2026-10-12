package util;

import dto.*;
import dto.request.ChatRequestDTO;
import dto.request.CloseEventRequestDTO;
import dto.request.DepositRequestDTO;
import dto.request.ParticipationRequestDTO;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.io.File;

public final class HttpClientUtil {

    private HttpClientUtil() {
    }

    private static final Gson GSON = util.GsonProvider.GSON;

    private static final SimpleCookieManager COOKIE_MANAGER = new SimpleCookieManager();

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT =
            new OkHttpClient.Builder()
                    .cookieJar(COOKIE_MANAGER)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

    public static class ServerException extends IOException {

        private final int statusCode;

        public ServerException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    public static List<EventSummaryDTO> getEvents() throws IOException {
        String json = get(Constants.EVENTS);
        Type type = new TypeToken<List<EventSummaryDTO>>() {}.getType();
        return GSON.fromJson(json, type);
    }

    public static EventTradingStatusDTO getEventTradingStatus(int eventId) throws IOException {
        String url = urlOf(Constants.EVENT_STATUS)
                .addQueryParameter("id", String.valueOf(eventId))
                .build().toString();
        return GSON.fromJson(get(url), EventTradingStatusDTO.class);
    }

    public static List<OrderBookStatsDTO> getOrderBookStats(int eventId) throws IOException {
        String url = urlOf(Constants.EVENT_STATS)
                .addQueryParameter("id", String.valueOf(eventId))
                .build().toString();
        Type type = new TypeToken<List<OrderBookStatsDTO>>() {}.getType();
        return GSON.fromJson(get(url), type);
    }

    public static List<ParticipantHoldingDTO> getEventParticipants(int eventId) throws IOException {
        String url = urlOf(Constants.EVENT_PARTICIPANTS)
                .addQueryParameter("id", String.valueOf(eventId))
                .build().toString();
        Type type = new TypeToken<List<ParticipantHoldingDTO>>() {}.getType();
        return GSON.fromJson(get(url), type);
    }


    public static UserDTO getCurrentUser() throws IOException {
        return GSON.fromJson(get(Constants.CURRENT_USER), UserDTO.class);
    }


    public static int getVersion() throws IOException {
        return Integer.parseInt(get(Constants.VERSION).trim());
    }

    public static void login(String userName) throws IOException {
        HttpUrl url = urlOf(Constants.LOGIN)
                        .addQueryParameter(
                                Constants.USERNAME_PARAM,
                                userName
                        )
                        .build();

        Request request = new Request.Builder()
                        .url(url)
                        .post(
                                RequestBody.create(
                                        new byte[0]
                                )
                        )
                        .build();
        execute(request);
    }

    public static void logout() throws IOException {

        Request request = new Request.Builder()
                        .url(Constants.LOGOUT)
                        .post(
                                RequestBody.create(
                                        new byte[0]
                                )
                        )
                        .build();

        execute(request);

        // The server invalidated the session. SimpleCookieManager never overwrites
        // a cookie it already holds, so without this the next login would keep
        // sending the dead JSESSIONID.
        COOKIE_MANAGER.clear();
    }

    public static void uploadFile(File file) throws IOException {
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/xml"));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(Constants.UPLOAD)
                .post(requestBody)
                .build();

        execute(request);
    }

    public static void participateInEvent(
            //username is not needed because the server gets it from the session
            int eventId,
            int optionNumber,
            int shares,
            Side side,
            Double price) throws IOException {

        ParticipationRequestDTO participation =
                new ParticipationRequestDTO(
                        eventId,
                        optionNumber,
                        shares,
                        side,
                        price
                );

        String json = GSON.toJson(participation);

        RequestBody requestBody = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                        .url(Constants.EVENT_PARTICIPATE)
                        .post(requestBody)
                        .build();

        String response = execute(request);

        GSON.fromJson(response, PurchaseDTO.class);
    }

    public static void activateEvent(int eventId) throws IOException {

        HttpUrl url = urlOf(Constants.EVENT_ACTIVATE)
                .addQueryParameter("id", String.valueOf(eventId))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(new byte[0]))
                .build();

        execute(request);
    }

    public static List<UserDTO> getUsers() throws IOException {

        String json = get(Constants.USERS);

        Type type = new TypeToken<List<UserDTO>>() {}.getType();

        return GSON.fromJson(json, type);
    }

    public static List<UserEventDTO> getUserEvents()
            throws IOException {

        String json = get(Constants.USER_EVENTS);

        Type type =
                new TypeToken<List<UserEventDTO>>() {}.getType();

        return GSON.fromJson(json, type);
    }

    public static UserOrderBookPositionDTO getUserOrderBookPosition(
            int eventId
    ) throws IOException {

        String url =
                urlOf(Constants.USER_ORDER_BOOK_POSITION)
                        .addQueryParameter(
                                "id",
                                String.valueOf(eventId)
                        )
                        .build()
                        .toString();

        return GSON.fromJson(
                get(url),
                UserOrderBookPositionDTO.class
        );
    }

    public static UserDTO deposit(double amount)
            throws IOException {

        DepositRequestDTO deposit =
                new DepositRequestDTO(amount);

        String json = GSON.toJson(deposit);

        RequestBody requestBody =
                RequestBody.create(json, JSON);

        Request request =
                new Request.Builder()
                        .url(Constants.DEPOSIT)
                        .post(requestBody)
                        .build();

        String response = execute(request);

        return GSON.fromJson(
                response,
                UserDTO.class
        );
    }

    public static List<AccountTransactionDTO> getAccountTransactions() throws IOException {

        String json = get(Constants.ACCOUNT_TRANSACTIONS);

        Type type =
                new TypeToken<
                        List<AccountTransactionDTO>
                        >() {}.getType();

        return GSON.fromJson(json, type);
    }

    public static void closeEvent(int eventId, int winningOption) throws IOException {

        CloseEventRequestDTO closeRequest = new CloseEventRequestDTO(eventId, winningOption);

        String json = GSON.toJson(closeRequest);

        RequestBody requestBody =
                RequestBody.create(
                        json,
                        JSON
                );

        Request request =
                new Request.Builder()
                        .url(Constants.EVENT_CLOSE)
                        .post(requestBody)
                        .build();

        execute(request);
    }

    /**
     * Sends one throwaway GET to the login URL on a background thread, so the
     * first real login does not pay for OkHttp's one-time class loading, the
     * TCP connect, and the server's lazy servlet start-up. The GET is answered
     * with 405 (login is POST-only) and creates no session or user. Best
     * effort: a failure - including a server that is not up yet - is ignored.
     */

    public static List<ChatMessageDTO> getChatMessages(int since) throws IOException {
        String url = Objects.requireNonNull(HttpUrl.parse(Constants.CHAT)).newBuilder()
                .addQueryParameter("since", String.valueOf(since))
                .build().toString();

        Type type = new TypeToken<List<ChatMessageDTO>>() {}.getType();
        return GSON.fromJson(get(url), type);
    }

    public static void sendChatMessage(String text) throws IOException {
        RequestBody body = RequestBody.create(GSON.toJson(new ChatRequestDTO(text)), JSON);
        execute(new Request.Builder().url(Constants.CHAT).post(body).build());
    }


    public static void warmUp() {
        Thread thread = new Thread(() -> {
            try {
                execute(new Request.Builder().url(Constants.LOGIN).get().build());
            } catch (Exception ignored) {
                // 405 is the expected answer; nothing here is worth reporting
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static String get(String url) throws IOException {

        Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        return execute(request);
    }

    private static String execute(Request request) throws IOException {
        try (Response response = CLIENT.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string().trim();

            if (!response.isSuccessful()) {
                throw new ServerException(
                        response.code(),
                        body.isEmpty()
                                ? "The server returned "
                                + response.code()
                                + "."
                                : body
                );
            }
            return body;
        }
    }

    private static HttpUrl.Builder urlOf(String url) {
        return Objects.requireNonNull(
                HttpUrl.parse(url),
                "Malformed URL: " + url
        ).newBuilder();
    }
}