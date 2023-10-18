import requests
import json
from generate_signature import generate_signature
import hashlib
import time

url_pre = "endpoint from your app"
url = "/v1/jobs"
app_id = "your app id"
private_key_path = "your private key path"
data = {
    "request_id":  hashlib.md5(str(int(time.time())).encode()).hexdigest(),
    "stages": [
        {
            "type": "INPUT_INITIALIZE",
            "inputInitialize": {
                "seed": -1,
                "count": 2
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
                "steps": 15,
                "sd_model": "600423083519508503",
                "clip_skip": 2,
                "cfg_scale": 7
            }
        }
    ]
}
body = json.dumps(data)
auth_header = generate_signature("POST", url, body, app_id, private_key_path)
print(auth_header)
headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Authorization': auth_header
}
response = requests.post(f"{url_pre}{url}", json=data, headers=headers)
print(response.text)
