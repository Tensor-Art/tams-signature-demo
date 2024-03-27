import requests
import json
from generate_signature import generate_signature
import hashlib
import time

url_pre = "https://" + "endpoint from your app"
app_id = "your app id"

# u can put ur private key under the root directory
private_key_path = "../private_key.pem"

url_local = "http://localhost:8080"
url_workflow = '/v1/workflows'
url_job = "/v1/jobs"
url_resource = "/v1/resource"


# 文生图
def text2img():
    data = {
        "request_id": hashlib.md5(str(int(time.time())).encode()).hexdigest(),
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
                    # added strong inspection for parameters
                    # so sampler and sdVae are necessary now
                    # for more info, u can refer to the websites below
                    # https://tams-docs.tusiart.com/docs/api/guide/list-of-constants/#sampler
                    # https://tams-docs.tusiart.com/docs/api/guide/list-of-constants/#vae
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
    response_data = create_job(data)
    if 'job' in response_data:
        job_dict = response_data['job']
        job_id = job_dict.get('id')
        job_status = job_dict.get('status')
        print(job_id, job_status)
        get_job_result(job_id)


# 图生图
def img2img(img_path):
    resource_id = upload_img(img_path)
    data = {
        "request_id": hashlib.md5(str(int(time.time())).encode()).hexdigest(),
        "stages": [
            {
                "type": "INPUT_INITIALIZE",
                "inputInitialize": {
                    "image_resource_id": f"{resource_id}",
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
                    # added strong inspection for parameters
                    # so sampler and sdVae are necessary now
                    # for more info, u can refer to the websites below
                    # https://tams-docs.tusiart.com/docs/api/guide/list-of-constants/#sampler
                    # https://tams-docs.tusiart.com/docs/api/guide/list-of-constants/#vae
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
    response_data = create_job(data)
    if 'job' in response_data:
        job_dict = response_data['job']
        job_id = job_dict.get('id')
        job_status = job_dict.get('status')
        print(job_id, job_status)
        get_job_result(job_id)


def get_job_result(job_id):
    while True:
        time.sleep(1)
        response = requests.get(f"{url_pre}{url_job}/{job_id}", headers={
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Authorization': generate_signature("GET", f"{url_job}/{job_id}", "", app_id, private_key_path)
        })
        get_job_response_data = json.loads(response.text)
        if 'job' in get_job_response_data:
            job_dict = get_job_response_data['job']
            job_status = job_dict.get('status')
            if job_status == 'SUCCESS':
                print(job_dict)
                break
            elif job_status == 'FAILED':
                print(job_dict)
                break
            else:
                print(job_dict)


def create_job(data):
    body = json.dumps(data)
    auth_header = generate_signature("POST", url_job, body, app_id, private_key_path)
    response = requests.post(f"{url_pre}{url_job}", json=data, headers={
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': auth_header
    })
    print(response.text)
    return json.loads(response.text)


def upload_img(img_path):
    print(img_path)
    data = {
        "expireSec": 3600,
    }
    body = json.dumps(data)
    response = requests.post(f"{url_pre}{url_resource}/image", json=data, headers={
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': generate_signature("POST", f"{url_resource}/image", body, app_id, private_key_path)
    })
    print(response.text)
    response_data = json.loads(response.text)
    resource_id = response_data['resourceId']
    put_url = response_data['putUrl']
    headers = response_data['headers']
    with open(img_path, 'rb') as f:
        res = f.read()
        response = requests.put(put_url, data=res, headers=headers)
        print(response.text)
    return resource_id


def get_workflow_template(template_id):
    response = requests.get(f"{url_pre}{url_workflow}/{template_id}", headers={
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': generate_signature("GET", f"{url_workflow}/{template_id}", "", app_id, private_key_path)
    })
    print(response.text)


def workflow_template_check():
    data = {
        "templateId": "676018193025756628",
        "fields": {
            "fieldAttrs": [
                {
                    "nodeId": "25",
                    "fieldName": "image",
                    "fieldValue": None
                },
                {
                    "nodeId": "27",
                    "fieldName": "text",
                    "fieldValue": "1 girl"
                }
            ]
        }
    }
    body = json.dumps(data)
    response = requests.post(f"{url_pre}{url_workflow}/template/check", json=data, headers={
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': generate_signature("POST", f"{url_workflow}/template/check", body, app_id, private_key_path)
    })
    print(response.text)


def workflow_template_job():
    data = {
        "request_id": "9f2bf085998e76acd5c8bc306a4f034f",
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

    body = json.dumps(data)
    response = requests.post(f"{url_pre}{url_job}/workflow/template", json=data, headers={
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': generate_signature("POST", f"{url_job}/workflow/template", body, app_id, private_key_path)
    })
    print(response.text)


if __name__ == '__main__':
    # 文生图
    # text2img()
    # 图生图
    # img2img("../test.webp")

    # 模版相关
    # get_workflow_template(676018193025756628)
    # workflow_template_check()
    workflow_template_job()
