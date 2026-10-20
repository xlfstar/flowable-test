package com.example.flowable.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;

    private List<T> records;

    public PageResult() {
        this.total = 0;
        this.records = Collections.emptyList();
    }

    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records == null ? Collections.emptyList() : records;
    }

    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, records);
    }
}
