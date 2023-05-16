package com.atguigu.controller;

import com.atguigu.entity.BaseBrand;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/product/brand")
public class BrandController {
    @Autowired
    private BaseBrandService brandService;

    //1.分页查询
    @GetMapping("/queryBrandByPage/{pageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Integer pageNum,
                                   @PathVariable Integer pageSize) {
        Page<BaseBrand> page = new Page<>(pageNum,pageSize);
        brandService.page(page,new QueryWrapper<>());

        return RetVal.ok(page);
    }

    //2.添加品牌
    @PostMapping
    public RetVal saveBrand(@RequestBody BaseBrand brand) {
        brandService.save(brand);
        return RetVal.ok();
    }

    //http://127.0.0.1/product/brand/4
    //3.根据id查询品牌信息
    @GetMapping("/{brandId}")
    public RetVal getById(@PathVariable Long brandId) {
        BaseBrand brand = brandService.getById(brandId);
        return RetVal.ok(brand);
    }

    //4.更新品牌信息
    @PutMapping
    public RetVal updateBrand(@RequestBody BaseBrand brand) {
        brandService.updateById(brand);
        return RetVal.ok();
    }

    //5.删除品牌信息
    @DeleteMapping("{brandId}")
    public RetVal remove(@PathVariable Long brandId) {
        brandService.removeById(brandId);
        return RetVal.ok();
    }

    //6.查询所有的品牌
    @GetMapping("getAllBrand")
    public RetVal getAllBrand() {
        List<BaseBrand> brandList = brandService.list(null);
        return RetVal.ok(brandList);
    }

    //7.文件上传 fastdfs版本
    //7.http://127.0.0.1/product/brand/fileUpload
    @PostMapping("/fileUploadByFastdfs")
    public RetVal fileUploadByFastdfs(MultipartFile file) throws Exception {
        //需要一个配置文件告诉fastdfs在哪里
        String configFilePath = this.getClass().getResource("/tracker.conf").getFile();
        //初始化
        ClientGlobal.init(configFilePath);
        //创建trackerClient 客户端
        TrackerClient trackerClient = new TrackerClient();
        //用trackerClient获取连接
        TrackerServer trackerServer = trackerClient.getConnection();
        //创建StorageClient1
        StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
        //对文件实现上传
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String path = storageClient1.upload_appender_file1(file.getBytes(), extension, null);

//        System.out.println("trackerServer.getInetSocketAddress().getAddress() = " + trackerServer.getInetSocketAddress().getAddress().toString());
        String filePath ="http:/" + trackerServer.getInetSocketAddress().getAddress().toString() + ":8888/" + path;
        //拼接路径 http://192.168.121.128:8888/group1/M00/00/01/wKh5gGRiHAeEfdnWAAAAAIx0Xh4285.jpg
        return RetVal.ok(filePath);
    }

    @PostMapping("/fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {

        return RetVal.ok();
    }
}
