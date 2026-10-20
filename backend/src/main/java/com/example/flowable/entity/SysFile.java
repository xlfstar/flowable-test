package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalName;

    private String storedName;

    private String contentType;

    private Long fileSize;

    private String storagePath;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime createTime;
}
