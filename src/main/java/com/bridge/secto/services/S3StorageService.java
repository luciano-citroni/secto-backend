package com.bridge.secto.services;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bridge.secto.dtos.FileUploadResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;

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
}
