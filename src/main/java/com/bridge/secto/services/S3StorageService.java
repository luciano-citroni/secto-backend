package com.bridge.secto.services;

import java.io.InputStream;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bridge.secto.dtos.FileUploadResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;
    
    @Value("${aws.s3.region}")
    private String region;

    public FileUploadResponseDto uploadFile(MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();

            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromInputStream(inputStream, file.getSize())
            );

            String fileUrl;
            if (s3Endpoint.contains("amazonaws.com")) {
                 fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
            } else {
                 // Path style for others
                 fileUrl = String.format("%s/%s/%s", s3Endpoint, bucketName, fileName);
            }

            return new FileUploadResponseDto(fileName, fileUrl);
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("Error uploading file to S3", e);
        }
    }

    public String buildObjectUrl(String objectKey) {
        if (s3Endpoint.contains("amazonaws.com")) {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
        } else {
            return String.format("%s/%s/%s", s3Endpoint, bucketName, objectKey);
        }
    }

    public void copyObject(String sourceKey, String destinationKey) {
        try {
            s3Client.copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error copying S3 object from {} to {}", sourceKey, destinationKey, e);
            throw new RuntimeException("Error copying S3 object", e);
        }
    }

    public void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error deleting S3 object: {}", objectKey, e);
            throw new RuntimeException("Error deleting S3 object", e);
        }
    }

    public java.nio.file.Path downloadToTempFile(String objectKey) {
        try {
            String extension = ".tmp";
            if (objectKey.contains(".")) {
                extension = objectKey.substring(objectKey.lastIndexOf("."));
            }
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("s3-download", extension);
            java.nio.file.Files.delete(tempFile);
            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toFile(tempFile)
            );
            return tempFile;
        } catch (Exception e) {
            log.error("Error downloading file from S3: {}", objectKey, e);
            throw new RuntimeException("Error downloading file from S3", e);
        }
    }

    /**
     * Generates a presigned URL for downloading an S3 object.
     * The URL is valid for the specified duration.
     *
     * @param objectKey the S3 object key (e.g. "1740000000000_recording.mp3")
     * @param expiration duration for which the URL is valid
     * @return the presigned URL string
     */
    public String generatePresignedUrl(String objectKey, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .responseContentDisposition("attachment; filename=\"" + objectKey + "\"")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Error generating presigned URL for key: {}", objectKey, e);
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }
}
