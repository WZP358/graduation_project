package com.wzp.music.controller;

import com.wzp.music.domain.Music;
import com.wzp.music.mapper.MusicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private MusicMapper musicMapper;

    @GetMapping("/files/{musicId}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long musicId) {
        try {
            Music music = musicMapper.selectMusicById(musicId);
            if (music == null || music.getFilePath() == null) {
                System.out.println("音乐不存在或文件路径为空: ID=" + musicId);
                return ResponseEntity.notFound().build();
            }

            String filePath = music.getFilePath();
            Path path = Paths.get(filePath);
            
            System.out.println("请求音乐文件: ID=" + musicId);
            System.out.println("文件路径: " + filePath);
            
            if (!Files.exists(path)) {
                System.out.println("文件不存在: " + filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path);
            
            // 检测文件类型
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            System.out.println("文件类型: " + contentType);
            
            String filename = music.getName() + path.getFileName().toString().substring(path.getFileName().toString().lastIndexOf('.'));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (IOException e) {
            System.out.println("读取文件失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}