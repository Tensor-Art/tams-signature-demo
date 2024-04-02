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

    private static final String img2imgData =
            """
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
                    """;

    public static void main(String[] args) throws Exception {
//        算力预估
//        getJobCredits();

//        文生图
//        text2img();
//        图生图
//        img2img("./test.webp");
//        图片上传
//        uploadImg("./test.webp");

//        模板相关
//        getWorkflowTemplate("676018193025756628");
//        workflowTemplateCheck();
        workflowTemplateJob();
    }

    public static void getJobCredits() throws Exception {
        var data = String.format(txt2imgData, md5(Long.toString(System.currentTimeMillis())));
        var authHeader = SignatureGenerator.generateSignature("POST", jobUrl + "/credits", data, appId, privateKeyPath);
        var response = sendPostRequest(urlPre + jobUrl + "/credits", data, authHeader);
        System.out.println(response.body());
    }

    // 文生图
    public static void text2img() throws Exception {
        var data = String.format(txt2imgData, md5(Long.toString(System.currentTimeMillis())));
        createJob(data);
    }

    // 图生图
    public static void img2img(String imgPath) throws Exception {
        var resourceId = uploadImg(imgPath);
        var data = String.format(img2imgData, md5(Long.toString(System.currentTimeMillis())), resourceId);

        createJob(data);
    }

    public static void getWorkflowTemplate(String templateId) throws Exception {
        var authHeader = SignatureGenerator.generateSignature("GET", workflowUrl + "/" + templateId, "", appId, privateKeyPath);

        var response = sendGetRequest(urlPre + workflowUrl + "/" + templateId, authHeader);
        System.out.println(response.toString());
    }

    public static void workflowTemplateCheck() throws Exception {
        var data = """
                {
                    "templateId": "676018193025756628",
                    "fields": {
                        "fieldAttrs": [
                            {
                                "nodeId": "25",
                                "fieldName": "image",
                                "fieldValue": null
                            },
                            {
                                "nodeId": "27",
                                "fieldName": "text",
                                "fieldValue": "1 girl"
                            }
                        ]
                    }
                }
                """;
        var authHeader = SignatureGenerator.generateSignature("POST", workflowUrl + "/template/check", data, appId, privateKeyPath);

        var response = sendPostRequest(urlPre + workflowUrl + "/template/check", data, authHeader);
        System.out.println(response.body());
    }

    public static void workflowTemplateJob() throws Exception {
        var workflowTemplateJobData = """
                {
                    "request_id": "%s",
                    "templateId": "676018193025756628",
                    "fields": {
                        "fieldAttrs": [
                            {
                                "nodeId": "25",
                                "fieldName": "image",
                                "fieldValue": "f29036b4-ff7b-4394-8c26-aabc1bdae008"
                            },
                            {
                                "nodeId": "27",
                                "fieldName": "text",
                                "fieldValue": "1 girl, amber_eyes"
                            }
                        ]
                    }
                }
                """;
        var data = String.format(workflowTemplateJobData, md5(Long.toString(System.currentTimeMillis())));
        var authHeader = SignatureGenerator.generateSignature("POST", jobUrl + "/workflow/template", data, appId, privateKeyPath);

        var response = sendPostRequest(urlPre + jobUrl + "/workflow/template", data, authHeader);
        System.out.println(response.body());
    }

    private static void createJob(String data) throws Exception {
        var authHeader = SignatureGenerator.generateSignature("POST", jobUrl, data, appId, privateKeyPath);
        var response = sendPostRequest(urlPre + jobUrl, data, authHeader);

        if (response.statusCode() == 200) {
            var responseBody = response.body();
            try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
                var jsonResponse = jsonReader.readObject();
                if (jsonResponse.containsKey("job")) {
                    var job = jsonResponse.getJsonObject("job");
                    var jobId = job.getString("id");
                    var jobStatus = job.getString("status");
                    System.out.println(jobId + " " + jobStatus);
                    getJobResult(jobId);
                }
            }
        }
    }

    public static void getJobResult(String jobId) throws Exception {
        while (true) {
            Thread.sleep(1000);
            var authHeader = SignatureGenerator.generateSignature("GET", jobUrl + "/" + jobId, "", appId, privateKeyPath);
            var response = sendGetRequest(urlPre + jobUrl + "/" + jobId, authHeader);
            if (response.containsKey("job")) {
                var job = response.getJsonObject("job");
                var jobStatus = job.getString("status");
                if (jobStatus.equals("SUCCESS") || jobStatus.equals("FAILED")) {
                    System.out.println(job);
                    break;
                } else {
                    System.out.println(job);
                }
            }
        }
    }

    private static JsonObject sendGetRequest(String url, String authHeader) throws Exception {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var responseBody = response.body();
            try (var jsonReader = Json.createReader(new StringReader(responseBody))) {
                return jsonReader.readObject();
            }
        } else {
            throw new Exception("HTTP request failed with status code: " + response.statusCode());
        }
    }

    private static HttpResponse<String> sendPostRequest(String url, String body, String authHeader) throws Exception {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static String uploadImg(String imgPath) throws Exception {
        System.out.println(imgPath);

        var data = Json.createObjectBuilder()
                .add("expireSec", 3600)
                .build();

        var body = data.toString();
        var authHeader = SignatureGenerator.generateSignature("POST", imageUrl, body, appId, privateKeyPath);

        var response = sendPostRequest(urlPre + imageUrl, body, authHeader);

        var responseBody = response.body();
        System.out.println(responseBody);

        var responseData = Json.createReader(new StringReader(responseBody)).readObject();
        var resourceId = responseData.getString("resourceId");
        var putUrl = responseData.getString("putUrl");
        var headers = responseData.getJsonObject("headers");

        var fileBytes = Files.readAllBytes(Paths.get(imgPath));
        var putRequestBuilder = HttpRequest.newBuilder()
                .uri(new URI(putUrl))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes));

        for (var key : headers.keySet()) {
            putRequestBuilder.header(key, headers.getString(key));
        }
        var client = HttpClient.newHttpClient();
        var putResponse = client.send(putRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println(putResponse.body());
        return resourceId;
    }

    private static String md5(String input) throws Exception {
        var md = MessageDigest.getInstance("MD5");
        var digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        var bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }
}