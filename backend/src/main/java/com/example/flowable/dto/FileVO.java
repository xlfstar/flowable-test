package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileVO {

    private Long id;

    private String name;

    private String url;

    private Long size;

    private String contentType;
}
