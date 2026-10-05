package util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.*;

public final class GsonProvider {
    private GsonProvider() {}
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(OptionDTO.class, "type")
            .registerSubtype(LMSROptionDTO.class, "lmsr")
            .registerSubtype(OBOptionDTO.class, "orderbook"))
        .registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(MethodDTO.class, "type")
            .registerSubtype(LMSRDTO.class, "lmsr")
            .registerSubtype(OrderBookDTO.class, "orderbook"))
        .create();
}
