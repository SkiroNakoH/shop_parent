package com.atguigu.utils;

import com.atguigu.perproties.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@EnableConfigurationProperties(MinioProperties.class)
@Component
public class MinioUtil {
    @Autowired
    private MinioProperties minioProperties;
    @Autowired
    private MinioClient minioClient;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = null;
        try {
            minioClient = new MinioClient(minioProperties.getEndpoint(), minioProperties.getAccessKey(), minioProperties.getSecretKey());
            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists(minioProperties.getBucketName());
            if (!isExist) {
                // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
                minioClient.makeBucket(minioProperties.getBucketName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return minioClient;
    }

    public String upload(MultipartFile multipartFile)
            throws Exception {

        // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象

        // 使用putObject上传一个文件到存储桶中。
        InputStream inputStream = multipartFile.getInputStream();

        String prefix = UUID.randomUUID().toString().replace("-", "");

        String oldFileName = multipartFile.getOriginalFilename();
        String suffix = oldFileName.substring(oldFileName.lastIndexOf("."));

        String fileName = prefix + "." + suffix;

        //这个是文件上传的时候需要的参数 文件可用大小与文件上传多少
        PutObjectOptions options = new PutObjectOptions(inputStream.available(), -1);
        minioClient.putObject(minioProperties.getBucketName(), fileName, inputStream, options);

        return minioProperties.getEndpoint() + "/"
                + minioProperties.getBucketName() + "/"
                + fileName;
    }
}