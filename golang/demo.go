package main

import (
	"bytes"
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strconv"
	"tams-signature-demo/signature"
	"time"
)

const (
	urlPre = "https://" + "endpoint from ur app"
	appID  = "ur app id"
)

const (
	privateKeyPath = "../private_key.pem"
	localUrl       = "http://localhost:8080"
	workflowUrl    = "/v1/workflows"
	jobUrl         = "/v1/jobs"
	imageUrl       = "/v1/resource/image"
)

const (
	txt2img_data = `
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
	`
	img2imgData = `
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
	`
)

type JobData struct {
	RequestID string  `json:"request_id"`
	Stages    []Stage `json:"stages"`
}

type Stage struct {
	Type string `json:"type"`
	// Define other fields as per your requirements
}

func main() {
	// 预估算力
	//getJobCredits()

	// 文生图
	//text2img()
	// 图生图
	//img2img("../test.webp")

}

func getJobCredits() {
	data := fmt.Sprintf(txt2img_data, createMD5(time.Now().Unix()))
	authHeader, err := signature.GenerateSignatureHeader("POST", jobUrl+"/credits", appID, data, privateKeyPath)
	if err != nil {
		fmt.Println(err)
		return
	}
	response, err := sendRequest("POST", urlPre+jobUrl+"/credits", []byte(data), authHeader)
	if err != nil {
		return
	}

	fmt.Println(response)
}

func text2img() {
	data := fmt.Sprintf(txt2img_data, createMD5(time.Now().Unix()))
	err := createJob(data)
	if err != nil {
		fmt.Println(err)
		return
	}
}

func img2img(imgPath string) {
	imgResourceId, err := uploadImg(imgPath)
	if err != nil {
		fmt.Println(err)
		return
	}
	data := fmt.Sprintf(img2imgData, createMD5(time.Now().Unix()), imgResourceId)
	err = createJob(data)
	if err != nil {
		fmt.Println(err)
		return
	}
}

func getWorkflowTemplate(templateId string) {
	// Implement the logic to get workflow template here
}

func workflowTemplateCheck() {
	// Implement the logic to check workflow template here
}

func workflowTemplateJob() {
	// Implement the logic to create a workflow template job here
}

func createJob(data string) (err error) {
	authHeader, err := signature.GenerateSignatureHeader("POST", jobUrl, appID, data, privateKeyPath)
	if err != nil {
		fmt.Println(err)
		return
	}
	response, err := sendRequest("POST", urlPre+jobUrl, []byte(data), authHeader)
	if err != nil {
		fmt.Println(err)
		return
	}
	var result map[string]map[string]interface{}
	json.Unmarshal([]byte(response), &result)
	jobId := result["job"]["id"].(string)
	err = getJobResult(jobId)
	if err != nil {
		return
	}
	return
}

func getJobResult(jobId string) (err error) {
	for {
		time.Sleep(1 * time.Second)
		authHeader, err := signature.GenerateSignatureHeader("GET", jobUrl+"/"+jobId, appID, "", privateKeyPath)
		if err != nil {
			return err
		}
		response, err := sendRequest("GET", urlPre+jobUrl+"/"+jobId, []byte{}, authHeader)
		if err != nil {
			return err
		}
		var job map[string]map[string]interface{}
		err = json.Unmarshal([]byte(response), &job)
		if err != nil {
			return err
		}
		if job["job"]["status"] == "SUCCESS" || job["job"]["status"] == "FAILED" {
			fmt.Println(job)
			break
		} else {
			fmt.Println(job)
		}
	}
	return nil
}

type putImageRespData struct {
	ResourceID string            `json:"resourceId"`
	PutURL     string            `json:"putUrl"`
	Headers    map[string]string `json:"headers"`
}

func uploadImg(imgPath string) (string, error) {
	data := `
		{
			"expireSec": 3600
		}
	`
	authHeader, err := signature.GenerateSignatureHeader("POST", imageUrl, appID, data, privateKeyPath)
	if err != nil {
		fmt.Println(err)
		return "", err
	}
	response, err := sendRequest("POST", urlPre+imageUrl, []byte(data), authHeader)
	if err != nil {
		fmt.Println(err)
		return "", err
	}
	fmt.Println(response)

	var responseData putImageRespData
	err = json.Unmarshal([]byte(response), &responseData)
	if err != nil {
		return "", err
	}

	fileBytes, err := ioutil.ReadFile(imgPath)
	if err != nil {
		return "", err
	}

	req, err := http.NewRequest("PUT", responseData.PutURL, bytes.NewBuffer(fileBytes))
	if err != nil {
		return "", err
	}

	for key, value := range responseData.Headers {
		req.Header.Set(key, value)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	respBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	fmt.Println(string(respBody))
	return responseData.ResourceID, nil

}

func createMD5(timestamp int64) string {
	hasher := md5.New()
	hasher.Write([]byte(strconv.FormatInt(timestamp, 10)))
	return hex.EncodeToString(hasher.Sum(nil))
}

func sendRequest(method, url string, body []byte, signature string) (string, error) {
	client := &http.Client{}
	req, err := http.NewRequest(method, url, bytes.NewReader(body))
	if err != nil {
		return "", err
	}

	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("Accept", "application/json")
	req.Header.Add("Authorization", signature)

	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	respBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return string(respBody), nil
}
