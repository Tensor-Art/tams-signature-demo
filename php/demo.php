<?php

require 'vendor/autoload.php';
require 'generate_signature.php';

use GuzzleHttp\Client;

$client = new Client();
$url_pre = "endpoint from your app";
$url = "/v1/jobs";
$app_id = "your app id";
$private_key_path = "../private_key.pem";

function get_job_result($job_id)
{
    global $client, $url_pre, $url, $app_id, $private_key_path;
    while (true) {
        sleep(1);
        $response = $client->request('GET', "$url_pre$url/$job_id", [
            'headers' => [
                'Content-Type' => 'application/json',
                'Accept' => 'application/json',
                'Authorization' => generate_signature("GET", "$url/$job_id", "", $app_id, $private_key_path)
            ]
        ]);
        $get_job_response_data = json_decode($response->getBody(), true);
        if (isset($get_job_response_data['job'])) {
            $job_dict = $get_job_response_data['job'];
            $job_status = $job_dict['status'];
            print_r($job_dict);
            print("\n");
            if ($job_status == 'SUCCESS' || $job_status == 'FAILED') {
                break;
            }
        }
    }
}

function create_job($data)
{
    global $client, $url_pre, $url, $app_id, $private_key_path;
    $body = json_encode($data);
    $auth_header = generate_signature("POST", $url, $body, $app_id, $private_key_path);
    $response = $client->request('POST', "$url_pre$url", [
        'json' => $data,
        'headers' => [
            'Content-Type' => 'application/json',
            'Accept' => 'application/json',
            'Authorization' => $auth_header
        ]
    ]);
    print($response->getBody());
    print("\n");
    return json_decode($response->getBody(), true);
}

function upload_img($img_path)
{
    global $client, $url_pre, $app_id, $private_key_path;
    print($img_path);
    $data = [
        "expireSec" => 3600,
    ];
    $body = json_encode($data);
    $response = $client->request('POST', "$url_pre/v1/resource/image", [
        'json' => $data,
        'headers' => [
            'Content-Type' => 'application/json',
            'Accept' => 'application/json',
            'Authorization' => generate_signature("POST", "/v1/resource/image", $body, $app_id, $private_key_path)
        ]
    ]);
    print($response->getBody());
    $response_data = json_decode($response->getBody(), true);
    $resource_id = $response_data['resourceId'];
    $put_url = $response_data['putUrl'];
    $headers = $response_data['headers'];
    $response = $client->request('PUT', $put_url, [
        'body' => fopen($img_path, 'r'),
        'headers' => $headers
    ]);
    print($response->getBody());
    return $resource_id;
}

function text2img()
{
    $data = [
        "request_id" => md5(strval(time())),
        "stages" => [
            [
                "type" => "INPUT_INITIALIZE",
                "inputInitialize" => [
                    "seed" => -1,
                    "count" => 1
                ]
            ],
            [
                "type" => "DIFFUSION",
                "diffusion" => [
                    "width" => 512,
                    "height" => 512,
                    "prompts" => [
                        [
                            "text" => "1girl"
                        ]
                    ],
                    "sampler" => "DPM++ 2M Karras",
                    "sdVae" => "Automatic",
                    "steps" => 15,
                    "sd_model" => "600423083519508503",
                    "clip_skip" => 2,
                    "cfg_scale" => 7
                ]
            ]
        ]
    ];
    startTask($data);
}

function img2img($img_path)
{
    $resource_id = upload_img($img_path);
    $data = [
        "request_id" => md5(strval(time())),
        "stages" => [
            [
                "type" => "INPUT_INITIALIZE",
                "inputInitialize" => [
                    "image_resource_id" => "$resource_id",
                    "count" => 1
                ]
            ],
            [
                "type" => "DIFFUSION",
                "diffusion" => [
                    "width" => 512,
                    "height" => 512,
                    "prompts" => [
                        [
                            "text" => "1girl"
                        ]
                    ],
                    "sampler" => "DPM++ 2M Karras",
                    "sdVae" => "Automatic",
                    "steps" => 15,
                    "sd_model" => "600423083519508503",
                    "clip_skip" => 2,
                    "cfg_scale" => 7
                ]
            ]
        ]
    ];
    startTask($data);
}

function startTask(array $data)
{
    $response_data = create_job($data);
    if (isset($response_data['job'])) {
        $job_dict = $response_data['job'];
        $job_id = $job_dict['id'];
        $job_status = $job_dict['status'];
        print("$job_id, $job_status");
        print("\n");
        get_job_result($job_id);
    }
}

//text2img();
img2img("../test.webp");

