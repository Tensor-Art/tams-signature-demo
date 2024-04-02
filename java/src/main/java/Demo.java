import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;

// for jdk17
public class Demo {
    private static final String urlPre = "endpoint from your app";
    private static final String appId = "your app id";

    // 需要用以下命令将私钥转为der格式
    // openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
    private static final String privateKeyPath = "./private_key.der";

    private static final String localUrl = "http://localhost:8080";
    private static final String workflowUrl = "/v1/workflows";
    private static final String jobUrl = "/v1/jobs";
    private static final String imageUrl = "/v1/resource/image";

    private static final String txt2imgData =
            """
                    {
                        "request_id": "%s",
                        "stages": [
                            {
                                "type": "INPUT_INITIALIZE",
                                "inputInitialize": {
                                    "seed": -1,
                                    "count": 1
                                }
                            },
                            {
                                "type": "DIFFUSION",
                                "diffusion": {
                                    "width": 512,
                                    "height": 512,
                                    "prompts": [
                                        {
                                            "text": "1girl"
                                        }
                                    ],
                                    "sampler": "DPM++ 2M Karras",
                                    "sdVae": "Automatic",
                                    "steps": 15,
                                    "sd_model": "600423083519508503",
                                    "clip_skip": 2,
                                    "cfg_scale": 7
                                }
                            }
                        ]
                    }
                    """;

    public static void main(String[] args) throws Exception {
//        算力预估
//        getJobCredits();

//        文生图
//        text2img();
//        图生图
        img2img("./test.webp");
//        图片上传
//        uploadImg("./test.webp");
    }

    public static void getJobCredits() throws Exception {
        String data = String.format(txt2imgData, md5(Long.toString(System.currentTimeMillis())));
        String authHeader = SignatureGenerator.generateSignature("POST", jobUrl + "/credits", data, appId, privateKeyPath);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlPre + jobUrl + "/credits"))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

    // 文生图
    public static void text2img() throws Exception {
        String data = String.format(txt2imgData, md5(Long.toString(System.currentTimeMillis())));
        createJob(data);
    }

    // 图生图
    public static void img2img(String imgPath) throws Exception {
        String resourceId = uploadImg(imgPath);
        String data = String.format("""
                {
                        "request_id": "%s",
                        "stages": [
                            {
                                "type": "INPUT_INITIALIZE",
                                "inputInitialize": {
                                    "image_resource_id": "%s",
                                    "count": 1
                                }
                            },
                            {
                                "type": "DIFFUSION",
                                "diffusion": {
                                    "width": 512,
                                    "height": 512,
                                    "prompts": [
                                        {
                                            "text": "1girl"
                                        }
                                    ],
                                    "sampler": "DPM++ 2M Karras",
                                    "sdVae": "Automatic",
                                    "steps": 15,
                                    "sd_model": "600423083519508503",
                                    "clip_skip": 2,
                                    "cfg_scale": 7
                                }
                            }
                        ]
                    }
                """, md5(Long.toString(System.currentTimeMillis())), resourceId);

        createJob(data);
    }

    private static void createJob(String data) throws Exception {
        String authHeader = SignatureGenerator.generateSignature("POST", jobUrl, data, appId, privateKeyPath);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlPre + jobUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
                JsonObject jsonResponse = jsonReader.readObject();
                if (jsonResponse.containsKey("job")) {
                    JsonObject job = jsonResponse.getJsonObject("job");
                    String jobId = job.getString("id");
                    String jobStatus = job.getString("status");
                    System.out.println(jobId + " " + jobStatus);
                    getJobResult(jobId);
                }
            }
        }
    }

    public static void getJobResult(String jobId) throws Exception {
        while (true) {
            Thread.sleep(1000);
            String authHeader = SignatureGenerator.generateSignature("GET", jobUrl + "/" + jobId, "", appId, privateKeyPath);
            JsonObject response = sendGetRequest(urlPre + jobUrl + "/" + jobId, authHeader);
            if (response.containsKey("job")) {
                JsonObject job = response.getJsonObject("job");
                String jobStatus = job.getString("status");
                if (jobStatus.equals("SUCCESS") || jobStatus.equals("FAILED")) {
                    System.out.println(job);
                    break;
                } else {
                    System.out.println(job);
                }
            }
        }
    }

    public static JsonObject sendGetRequest(String urlString, String authHeader) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlString))
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
                return jsonReader.readObject();
            }
        } else {
            throw new Exception("HTTP request failed with status code: " + response.statusCode());
        }
    }

    public static String uploadImg(String imgPath) throws Exception {
        System.out.println(imgPath);

        JsonObject data = Json.createObjectBuilder()
                .add("expireSec", 3600)
                .build();

        String body = data.toString();
        String authHeader = SignatureGenerator.generateSignature("POST", imageUrl, body, appId, privateKeyPath);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlPre + imageUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        System.out.println(responseBody);

        JsonObject responseData = Json.createReader(new StringReader(responseBody)).readObject();
        String resourceId = responseData.getString("resourceId");
        String putUrl = responseData.getString("putUrl");
        JsonObject headers = responseData.getJsonObject("headers");

        byte[] fileBytes = Files.readAllBytes(Paths.get(imgPath));
        HttpRequest.Builder putRequestBuilder = HttpRequest.newBuilder()
                .uri(new URI(putUrl))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes));

        for (String key : headers.keySet()) {
            putRequestBuilder.header(key, headers.getString(key));
        }
        HttpResponse<String> putResponse = client.send(putRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println(putResponse.body());
        return resourceId;
    }


    public static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }
}