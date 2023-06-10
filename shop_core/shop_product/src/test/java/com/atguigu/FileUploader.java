package com.atguigu;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.MinioException;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileUploader {
    /*
     * http://192.168.21.128:9000
     * enjoy6288
     * godhand
     * C:\Users\SkiroNako\Desktop\naxida.jpg
     * */
    public static void main(String[] args)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
            MinioClient minioClient = new MinioClient("http://192.168.21.128:9000", "enjoy6288", "enjoy6288");
            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists("godhand");
            if (isExist) {
                System.out.println("Bucket already exists.");
            } else {
                // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
                minioClient.makeBucket("godhand");
            }
            // 使用putObject上传一个文件到存储桶中。
            FileInputStream fileInputStream = new FileInputStream("C:\\Users\\SkiroNako\\Desktop\\naxida.jpg");
            //这个是文件上传的时候需要的参数 文件可用大小与文件上传多少
            PutObjectOptions options = new PutObjectOptions(fileInputStream.available(), -1);
            minioClient.putObject("godhand", "new.jpg", fileInputStream, options);
            System.out.println("上传成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
        }
    }
}