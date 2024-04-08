import * as crypto from "crypto";
import * as fs from "fs";
import {generateSignature} from "./signature.js";

// tensor
const urlPre = "https://" + "endpoint from your app";
const appId = "your app id";

const privateKeyPath = '../private_key.pem';

const workflowUrl = '/v1/workflows';
const jobUrl = '/v1/jobs';
const imageUrl = '/v1/resource/image';

const txt2imgData = {
  "request_id": createMD5(),
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

async function main() {
  // Estimate computing credits
  // await getJobCredits();

  // Text to image
  // await text2img();

  // Image to image
  // await img2img('../test.webp');

  // Get workflow template
  // await getWorkflowTemplate('676018193025756628');

  // Check workflow template
  // await workflowTemplateCheck();

  // Create job from workflow template
  // await workflowTemplateJob();
}

async function getJobCredits() {
  const authHeader = generateSignature('POST', jobUrl + '/credits', appId, txt2imgData, privateKeyPath);
  const response = await sendRequest('POST', urlPre + jobUrl + '/credits', txt2imgData, authHeader);
  console.log(JSON.stringify(response));
}

async function text2img() {
  const resp = await createJob();
  if ('job' in resp) {
    const job_dict = resp.job;
    const job_id = job_dict.id;
    const job_status = job_dict.status;
    console.log(job_id, job_status);
    await getJobResult(job_id);
  }
}

async function img2img(imagePath) {
  const resourceId = await uploadImg(imagePath)
  const data = {
    "request_id": createMD5(),
    "stages": [
      {
        "type": "INPUT_INITIALIZE",
        "inputInitialize": {
          "image_resource_id": resourceId,
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
  const resp = await createJob(data);
  if ('job' in resp) {
    const job_dict = resp.job;
    const job_id = job_dict.id;
    const job_status = job_dict.status;
    console.log(job_id, job_status);
    await getJobResult(job_id);
  }
}

async function uploadImg(imagePath) {
  const uploadData = {
    "expireSec": 3600,
  }
  const authHeader = generateSignature('POST', imageUrl, appId, uploadData, privateKeyPath);
  const resp = await sendRequest('POST', urlPre + imageUrl, uploadData, authHeader);
  console.log(JSON.stringify(resp));
  const resourceId = resp['resourceId']
  const putUrl = resp['putUrl']
  const headers = resp['headers']
  fs.readFile(imagePath, async (err, data) => {
    if (err) {
      throw err;
    }
    const response = await fetch(putUrl, {
      method: 'PUT',
      headers: headers,
      body: data
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status} ${response.statusText}`);
    }
    console.log('Upload success');
  });
  return resourceId;
}

async function createJob() {
  const authHeader = generateSignature('POST', jobUrl, appId, txt2imgData, privateKeyPath);
  return await sendRequest('POST', urlPre + jobUrl, txt2imgData, authHeader);
}

async function getJobResult(jobId) {
  while (true) {
    await new Promise(resolve => setTimeout(resolve, 1000)); // Sleep for 1 second
    const authHeader = generateSignature('GET', jobUrl + '/' + jobId, appId, null, privateKeyPath);
    const resp = await sendRequest('GET', urlPre + jobUrl + '/' + jobId, null, authHeader);
    if ('job' in resp) {
      const jobDict = resp.job;
      const jobStatus = jobDict.status;
      console.log(jobStatus);
      if (jobStatus === 'SUCCESS') {
        console.log(JSON.stringify(jobDict));
        break;
      } else if (jobStatus === 'FAILED') {
        console.log(JSON.stringify(jobDict));
        break;
      } else {
        console.log(JSON.stringify(jobDict));
      }
    }
  }
}

async function getWorkflowTemplate(templateId) {
  const authHeader = generateSignature('GET', workflowUrl + '/' + templateId, appId, null, privateKeyPath);
  const response = await sendRequest('GET', urlPre + workflowUrl + '/' + templateId, null, authHeader);
  console.log(JSON.stringify(response));
}

async function workflowTemplateCheck() {
  const data = {
    "templateId": "676018193025756628",
    "fields": {
      "fieldAttrs": [
        {
          "nodeId": "25",
          "fieldName": "image",
          "fieldValue": 'f29036b4-ff7b-4394-8c26-aabc1bdae008'
        },
        {
          "nodeId": "27",
          "fieldName": "text",
          "fieldValue": "1 girl"
        }
      ]
    }
  }
  const authHeader = generateSignature('POST', workflowUrl + '/template/check', appId, data, privateKeyPath);
  const resp = await sendRequest('POST', urlPre + workflowUrl + '/template/check', data, authHeader);
  console.log(JSON.stringify(resp));
}

async function workflowTemplateJob() {
  const data = {
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

  const authHeader = generateSignature('POST', jobUrl + '/workflow/template', appId, data, privateKeyPath);
  const resp = await sendRequest('POST', urlPre + jobUrl + '/workflow/template', data, authHeader);
  console.log(JSON.stringify(resp));
}

async function sendRequest(method, url, body, signature) {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Authorization': signature
  };

  const response = body ?
    await fetch(url, {
      method: method,
      headers: headers,
      body: JSON.stringify(body)
    }) :
    await fetch(url, {
      method: method,
      headers: headers
    });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status} ${response.statusText}`);
  }

  return await response.json();
}

function createMD5() {
  return crypto.createHash('md5').update(`${Date.now()}`).digest('hex')
}

main()