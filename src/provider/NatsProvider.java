package provider;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.nats.client.*;
import io.nats.client.support.Encoding;

import javax.swing.text.html.Option;

public class NatsProvider {

    private String accessToken = null;
    private String natsUrl = null;
    private String _stream = null;
    private String jwt = null;

    private Options options = null;

    private Connection nc = null;

    private Dispatcher connectionDispatcher = null;


    public NatsProvider(String access_token, String nats_url, String stream) throws GeneralSecurityException, IOException {
        accessToken = access_token;
        natsUrl = nats_url;
        _stream = stream;
        jwt = createAppJwt(accessToken);

        File temp = File.createTempFile("temp", null);
        temp.deleteOnExit();
        FileWriter writer = new FileWriter(temp);
        writer.write(jwt);
        writer.close();
        String tempPath = temp.getAbsolutePath();

        AuthHandler authHandler = Nats.credentials(tempPath);
        options = new Options.Builder().server(natsUrl).authHandler(authHandler).build();

    }

    private String createAppJwt(String seed) throws GeneralSecurityException, IOException {
        char[] encodedAccSeed = seed.toCharArray();
        NKey account = NKey.fromSeed(encodedAccSeed);
        char[] accPubkey = account.getPublicKey();
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("jti", generate_jti());
        payload.put("iat", generate_iat());
        payload.put("iss", String.valueOf(accPubkey));
        payload.put("name", "developer");
        payload.put("sub", String.valueOf(accPubkey));
        payload.put("nats", get_nats_config());
        String sign = sign_jwt(payload, account);
        return "-----BEGIN NATS USER JWT-----\n" + sign + "\n------END NATS USER JWT------\n\n************************* IMPORTANT *************************\nNKEY Seed printed below can be used to sign and prove identity.\nNKEYs are sensitive and should be treated as secrets. \n\n-----BEGIN USER NKEY SEED-----\n" + seed + "\n------END USER NKEY SEED------\n\n*************************************************************";
    }


    private String generate_jti() {
        long timestamp = new Date().getTime() / 1000;
        String random_number = String.valueOf(new Random().nextDouble());//.substring(2);
        return timestamp + random_number;
    }

    private long generate_iat() {
        return new Date().getTime() / 1000;
    }

    private GSonNats get_nats_config() {
        return new Gson().fromJson("{\"pub\": {}, \"sub\": {}, \"subs\": -1, \"data\": -1, \"payload\": -1, \"type\": \"user\", \"version\": 2}", GSonNats.class);
    }

    private String sign_jwt(Object payload, NKey account) throws GeneralSecurityException, IOException {
        String header = "{\"typ\": \"JWT\", \"alg\": \"ed25519-nkey\"}";
        String header_encoded = new String(Encoding.base64UrlEncode(header.getBytes()), StandardCharsets.UTF_8).replaceAll("=", "");
        String payload_encoded = new String(Encoding.base64UrlEncode(new Gson().toJson(payload).getBytes()), StandardCharsets.UTF_8).replaceAll("=", "");
        String jwtbase = header_encoded + "." + payload_encoded;
        String signature = new String(Encoding.base64UrlEncode(account.sign(jwtbase.getBytes())), StandardCharsets.UTF_8).replaceAll("=", "");
        return jwtbase + "." + signature;
    }

    public void connect(MessageHandler connectionMessageHandler) throws IOException, InterruptedException {
        nc = Nats.connect(options);
        connectionDispatcher = nc.createDispatcher(connectionMessageHandler);
    }

    public void subscribe(MessageHandler subscribeMessageHandler) throws Exception {
        if (nc == null || connectionDispatcher == null)
            throw new Exception("Nats connection is not established");
        else
            connectionDispatcher.subscribe(_stream, subscribeMessageHandler);

    }

    public void publish(String message) throws Exception {
        if (nc == null)
            throw new Exception("Nats connection is not established");
        else
            nc.publish(_stream, (message + "x ").getBytes());

    }


    class GSonNats {
        @SerializedName("pub")
        @Expose
        private Object pub;
        @SerializedName("sub")
        @Expose
        private Object sub;
        @SerializedName("subs")
        @Expose
        private Integer subs;
        @SerializedName("data")
        @Expose
        private Integer data;
        @SerializedName("payload")
        @Expose
        private Integer payload;
        @SerializedName("type")
        @Expose
        private String type;
        @SerializedName("version")
        @Expose
        private Integer version;
    }
}
