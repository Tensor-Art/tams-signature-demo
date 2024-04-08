import {createHash, createSign} from "crypto";
import {readFileSync} from "fs";

export function generateSignature(method, url, appId, body, privateKeyPath) {
  const methodStr = method.toUpperCase();
  const urlStr = url;
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const nonceStr = createHash('md5').update(timestamp).digest("hex");
  const bodyStr = body ? JSON.stringify(body) : '';
  const toSign = `${methodStr}\n${urlStr}\n${timestamp}\n${nonceStr}\n${bodyStr}`;

  const privateKey = readFileSync(privateKeyPath);
  const sign = createSign('RSA-SHA256');
  sign.update(toSign);
  const signature = sign.sign(privateKey, "base64");
  return `TAMS-SHA256-RSA app_id=${appId},nonce_str=${nonceStr},timestamp=${timestamp},signature=${signature}`;
}
