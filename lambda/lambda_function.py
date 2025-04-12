import boto3
import json
import logging
import base64
import time

# AWS Clients
s3_client = boto3.client("s3")
rekognition_client = boto3.client("rekognition")  # Rekognition client for face comparison
textract_client = boto3.client("textract")  # Textract client for text extraction
dynamodb = boto3.resource("dynamodb")

# Configure Logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Define Bucket Name & Image Keys
BUCKET_NAME = "participation-record-bucket-varun-imam-yousuf"
FACE_IMAGES_KEY = ["faces1_Feb17.jpg", "faces2_Feb17.jpg", "faces3_Feb17.jpg"]
TEXT_IMAGE_KEY = "names1_Feb17.jpg"  # Document containing names

# DynamoDB Table
table = dynamodb.Table("Participation-Record-Table")

def upload_file_s3(base64_file, name, date):
    try:
        file_data = base64.b64decode(base64_file)
        file_name = f"{date}_{name.replace(' ', '')}.jpg"
        s3_key = f"proj2/uploads/{file_name}"

        s3_client.put_object(
            Bucket=BUCKET_NAME,
            Key=s3_key,
            Body=file_data,
            ContentType="image/jpeg"
        )
        logger.info(f"File uploaded successfully: {s3_key}")
        return s3_key
    except Exception as e:
        logger.error(f"Error uploading file to S3: {str(e)}")
        raise Exception(f"Error uploading file: {str(e)}")

def extract_text_from_image():
    try:
        response_text = textract_client.detect_document_text(
            Document={"S3Object": {"Bucket": BUCKET_NAME, "Name": TEXT_IMAGE_KEY}}
        )
        extracted_text = [item["Text"] for item in response_text["Blocks"] if "Text" in item]
        logger.info(f"Text extracted: {extracted_text}")
        return extracted_text
    except Exception as e:
        logger.error(f"Error extracting text: {str(e)}")
        return []

def compare_faces(source_image_key, target_image_key):
    try:
        response = rekognition_client.compare_faces(
            SourceImage={"S3Object": {"Bucket": BUCKET_NAME, "Name": source_image_key}},
            TargetImage={"S3Object": {"Bucket": BUCKET_NAME, "Name": target_image_key}},
            SimilarityThreshold=80
        )

        if not response.get("FaceMatches"):
            logger.info(f"No face match found between {source_image_key} and {target_image_key}")
            return False

        match_score = max([match["Similarity"] for match in response["FaceMatches"]])
        logger.info(f"Face match found with similarity: {match_score:.2f}%")
        return True

    except Exception as e:
        logger.error(f"Error comparing faces: {str(e)}")
        return False

def lambda_handler(event, context):
    start_time = time.time()

    try:
        logger.info("Received event: %s", json.dumps(event))
        request_body = json.loads(event.get("body") or "{}")  # ✅ SAFER: Avoids NoneType error

        name = request_body.get("name")
        email = request_body.get("email")
        date = request_body.get("date")
        base64_file = request_body.get("base64_file")
        target_face_image_key = request_body.get("target_face_image_key")

        if not all([name, email, date]) or (not base64_file and not target_face_image_key):
            return {"statusCode": 400, "body": json.dumps({"message": "Missing required parameters"})}

        if base64_file:
            target_face_image_key = upload_file_s3(base64_file, name, date)
        elif not target_face_image_key:
            return {"statusCode": 400, "body": json.dumps({"message": "No valid target face image provided"})}

        extracted_texts = extract_text_from_image()
        text_detected = any(name.lower() in text.lower() for text in extracted_texts)

        face_match_found = any(compare_faces(target_face_image_key, face_key) for face_key in FACE_IMAGES_KEY)

        participation_status = text_detected or face_match_found

        table.put_item(Item={
            "email": email,
            "name": name,
            "class_meeting_date": date,
            "participation": participation_status
        })

        response = {
            "statusCode": 200,
            "body": json.dumps({
                "message": "Processing completed successfully",
                "text_match": text_detected,
                "face_match": face_match_found,
                "participation_status": participation_status,
                "target_key": target_face_image_key,
                "extracted_text": extracted_texts
            }),
            "headers": {
                "Access-Control-Allow-Origin": "*"
            }
        }

        elapsed_time = time.time() - start_time
        logger.info(f"Execution time: {elapsed_time:.2f} seconds")

        return response

    except Exception as e:
        logger.error(f"Lambda error: {str(e)}")
        return {"statusCode": 500, "body": json.dumps({"error": str(e)})}
