package com.t2p.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class AmazonS3Service {


    Logger logger = LoggerFactory.getLogger(AmazonS3Service.class);

    @Value("${aws.bucket.name}")
    private String bucket;

    private final ResourcePatternResolver resourcePatternResolver;
    private final ResourceLoader resourceLoader;
    private final AmazonS3 amazonS3;

    @Autowired
    public AmazonS3Service(ResourcePatternResolver resourcePatternResolver, ResourceLoader resourceLoader, AmazonS3 amazonS3) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;
        this.amazonS3 = amazonS3;
    }

    public List<String> getAllFiles() throws IOException {
        String bucketPath = "s3://" + bucket + "/";
        Resource[] allFilesInFolder = resourcePatternResolver.getResources(bucketPath + "**");
        List<Resource> resources = Arrays.asList(allFilesInFolder);
        List<String> filesInS3Bucket = new ArrayList<>();

        resources.forEach(f -> {
            filesInS3Bucket.add(f.getFilename());
        });

        Collections.sort(filesInS3Bucket);

        return filesInS3Bucket;
    }

    public void saveFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        try {
            byte[] bytes = IOUtils.toByteArray(file.getInputStream());
            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setContentLength(bytes.length);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            PutObjectRequest putObjectRequest =
                    new PutObjectRequest(bucket, fileName, byteArrayInputStream, metaData)
                            .withCannedAcl(CannedAccessControlList.PublicRead);

            amazonS3.putObject(putObjectRequest);

            logger.info("Upload file on S3 successfully");
            S3Object s3Object = amazonS3.getObject(new GetObjectRequest(bucket, fileName));
            logger.info("URL: " + s3Object.getObjectContent().getHttpRequest().getURI());
        } catch (Exception e) {
            logger.error("Failed to upload file on S3");
            e.printStackTrace();
        }
    }

    public void deleteFile(String file) {
        amazonS3.deleteObject(bucket, file);
    }


    public ResponseEntity<Resource> downloadFile(String filename) {

        String bucketPath = "s3://" + bucket + "/";
        Resource s3Resource = resourceLoader.getResource(bucketPath + filename);

        String s3FileName = filename.substring(filename.lastIndexOf("/"));
        s3FileName = s3FileName.replace("/", "");

        logger.info("Downloading File: {} from S3", s3FileName);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + s3FileName + "\"")
                .body(s3Resource);

    }

}
