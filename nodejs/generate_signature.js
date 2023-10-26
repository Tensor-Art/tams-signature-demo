import { createHash, createSign } from "crypto";
import { readFileSync } from "fs";

function generate_signature(method, url, body, app_id, private_key_path) {
  const method_str = method.toUpperCase();
  const url_str = url;
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const nonce_str = createHash('md5').update(timestamp).digest("hex");
  const body_str = body || "";
  const to_sign = `${method_str}\n${url_str}\n${timestamp}\n${nonce_str}\n${body_str}`;
  
  const private_key = readFileSync(private_key_path);
  const sign = createSign('RSA-SHA256');
  sign.update(to_sign);
  const signature = sign.sign(private_key, "base64");
  const auth_header = `TAMS-SHA256-RSA app_id=${app_id},nonce_str=${nonce_str},timestamp=${timestamp},signature=${signature}`;
  return auth_header;
}

