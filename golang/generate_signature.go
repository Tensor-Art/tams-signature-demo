package generate_signature

import (
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/pem"
	"errors"
	"fmt"
	"github.com/google/uuid"
	"time"
)

const privateKey = `your private key`

func GenerateSignatureHeader(appId, method, path, payload string) (string, error) {
	timestamp := time.Now().UTC().Unix()
	nonceStr := uuid.New().String()

	message := fmt.Sprintf("%s\n%s\n%d\n%s\n%s", method, path, timestamp, nonceStr, payload)
	signature, err := Sign([]byte(message), []byte(privateKey))
	if err != nil {
		return "", err
	}

	return fmt.Sprintf("TAMS-SHA256-RSA app_id=%s,timestamp=%d,nonce_str=%s,signature=%s", appId, timestamp, nonceStr, base64.StdEncoding.EncodeToString(signature)), nil
}

func Sign(message []byte, privateKeyBytes []byte) ([]byte, error) {
	block, _ := pem.Decode(privateKeyBytes)
	if block == nil {
		return nil, errors.New("invalid private key")
	}

	privateKey, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		return nil, err
	}

	hashed := sha256.Sum256(message)

	signature, err := rsa.SignPKCS1v15(rand.Reader, privateKey, crypto.SHA256, hashed[:])
	if err != nil {
		return nil, err
	}

	return signature, nil
}
