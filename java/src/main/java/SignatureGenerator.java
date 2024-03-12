import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SignatureGenerator {

    public static String generateSignature(String method, String url, String body, String appId, String privateKeyPath) throws Exception {
        String methodStr = method.toUpperCase();
        String timestamp = Long.toString(System.currentTimeMillis() / 1000L);
        String nonceStr = DigestUtils.md5Hex(timestamp.getBytes());
        String toSign = methodStr + "\n" + url + "\n" + timestamp + "\n" + nonceStr + "\n" + body;

        byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(toSign.getBytes(StandardCharsets.UTF_8));
        byte[] signature = privateSignature.sign();

        String signatureBase64 = Base64.getEncoder().encodeToString(signature);
        return "TAMS-SHA256-RSA app_id=" + appId + ",nonce_str=" + nonceStr + ",timestamp=" + timestamp + ",signature=" + signatureBase64;
    }
}