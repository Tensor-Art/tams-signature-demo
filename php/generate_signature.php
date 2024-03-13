<?php

function generate_signature($method, $url, $body, $app_id, $private_key_path) {
    $method_str = strtoupper($method);
    $url_str = $url;
    $timestamp = strval(time());
    $nonce_str = md5($timestamp);
    $body_str = $body;
    $to_sign = $method_str . "\n" . $url_str . "\n" . $timestamp . "\n" . $nonce_str . "\n" . $body_str;

    $private_key = openssl_pkey_get_private(file_get_contents($private_key_path));
    openssl_sign($to_sign, $signature, $private_key, OPENSSL_ALGO_SHA256);
    $signature_base64 = base64_encode($signature);

    return "TAMS-SHA256-RSA app_id=" . $app_id . ",nonce_str=" . $nonce_str . ",timestamp=" . $timestamp . ",signature=" . $signature_base64;
}

