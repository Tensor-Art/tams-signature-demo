# tams-signature-demo

This is a simple signature project responsible for handling authentication in tams-api calls. It provides only the basic
signature functionality and does not handle the SDK encapsulation for the API.
Note that all code suggestions are recommended for development purposes only. In a production environment, it is
advisable to use a more secure method to store your private key to prevent any loss or leakage.

### Prerequisites:

1. Firstly we need to create a private key and a public key in 2048bit PKCS#1 PEM format to verify the request
   signature. The private key is used to sign the request and the public key is used to verify the signature.
    ```bash
    openssl genrsa -out private_key.pem 2048
    openssl rsa -pubout -in private_key.pem -out public_key.pem
    ```
   sometimes you may need to convert the private_key to PKCS#8 format
    ```bash
    openssl pkcs8 -topk8 -inform PEM -in private_key.pem -outform PEM -nocrypt -out private_key_pkcs8.pem
    ```
   sometimes you may need to convert the private_key to der format, like in java example
    ```bash
    openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
    ```

2. replace the necessary parameters below in code and put ur private key and test pic under the root directory:
    ```bash
    url_pre = "https://" + "endpoint from your app"
    app_id = "your app id"
    ```

3. for detailed info, pls refer to the demo code, the python example is the latest if we added something new.

    - Example in python: [generate_signature.py](./python/generate_signature.py) and [demo.py](./python/demo.py)
    - Example in java: [SignatureGenerator.java](./java/src/main/java/SignatureGenerator.java)
      and [Demo.java](./java/src/main/java/Demo.java)
    - Example in php: [generate_signature.php](./php/generate_signature.php) and [demo.php](./php/demo.php)

P.S. in java demo, it uses HttpClient and string block that need the jdk17 at least. If u'r using jdk8, u can use the HttpURLConnection
or other package instead.