import base64
import hashlib
import time
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.asymmetric import padding


def generate_signature(method, url, body, app_id, private_key_path):
    method_str = method.upper()
    url_str = url
    timestamp = str(int(time.time()))
    nonce_str = hashlib.md5(timestamp.encode()).hexdigest()
    body_str = body
    to_sign = f"{method_str}\n{url_str}\n{timestamp}\n{nonce_str}\n{body_str}"
    with open(private_key_path, "rb") as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )
    signature = private_key.sign(
        to_sign.encode(),
        padding.PKCS1v15(),
        hashes.SHA256()
    )
    signature_base64 = base64.b64encode(signature).decode()
    auth_header = f"TAMS-SHA256-RSA app_id={app_id},nonce_str={nonce_str},timestamp={timestamp},signature={signature_base64}"
    return auth_header


