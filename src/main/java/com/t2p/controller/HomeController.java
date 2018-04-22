package com.t2p.controller;

import com.t2p.service.AmazonS3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Controller
public class HomeController {

    Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final AmazonS3Service amazonS3Service;
    private final ResourceLoader resourceLoader;

    @Value("${aws.bucket.name}")
    private String bucket;

    @Autowired
    public HomeController(AmazonS3Service amazonS3Service, ResourceLoader resourceLoader) {
        this.amazonS3Service = amazonS3Service;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {

        try {
            logger.info("Fetching all Files from S3");
            model.addAttribute("files", amazonS3Service.getAllFiles()
                    .stream()
                    .collect(Collectors.toMap(f -> f, f -> {
                        List<String> filesNFolders = new ArrayList<>();
                        String[] folders = f.split("/");
                        Arrays.asList(folders).forEach(filesNFolders::add);
                        return filesNFolders;
                    })));
        } catch (IOException e) {
            logger.error("Failed to Connect to AWS S3 - Please check your AWS Keys");
            e.printStackTrace();
        }

        return "index";
    }

    @PostMapping("/save")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {

        try {
            amazonS3Service.saveFile(file);
        } catch (IOException e) {
            logger.error("Failed to upload file on S3");
            e.printStackTrace();
        }

        return "redirect:/";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> serveFile(@RequestParam String filename) {
        return amazonS3Service.downloadFile(filename);
    }

    @GetMapping("/delete")
    public String deleteFile(@RequestParam String filename) {

        logger.info("Deleting File: {} from S3", filename);
        amazonS3Service.deleteFile(filename);

        return "redirect:/";
    }

}
