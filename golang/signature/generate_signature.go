package signature

import (
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/pem"
	"fmt"
	"io/ioutil"
	"time"
)

func GenerateSignatureHeader(method, url, appId, body, privateKeyPath string) (string, error) {
	methodStr := method
	urlStr := url
	timestamp := fmt.Sprintf("%d", time.Now().Unix())
	nonceStr := fmt.Sprintf("%x", sha256.Sum256([]byte(timestamp)))
	bodyStr := body

	toSign := fmt.Sprintf("%s\n%s\n%s\n%s\n%s", methodStr, urlStr, timestamp, nonceStr, bodyStr)

	// Read the private key from file
	privateKeyBytes, err := ioutil.ReadFile(privateKeyPath)
	if err != nil {
		return "", err
	}
	block, _ := pem.Decode(privateKeyBytes)
	if block == nil {
		return "", fmt.Errorf("failed to parse PEM block containing the key")
	}

	parsed, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return "", err
	}
	privateKey := parsed.(*rsa.PrivateKey)

	// Sign the string toSign using the private key
	hashed := sha256.Sum256([]byte(toSign))
	signature, err := rsa.SignPKCS1v15(rand.Reader, privateKey, crypto.SHA256, hashed[:])
	if err != nil {
		return "", err
	}

	// Encode the signature to base64
	signatureBase64 := base64.StdEncoding.EncodeToString(signature)

	authHeader := fmt.Sprintf("TAMS-SHA256-RSA app_id=%s,nonce_str=%s,timestamp=%s,signature=%s", appId, nonceStr, timestamp, signatureBase64)
	return authHeader, nil
}
