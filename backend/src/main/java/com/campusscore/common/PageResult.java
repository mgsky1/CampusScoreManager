package com.campusscore.common;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分页结果信封。对应 {@code contracts/api.md §0.4}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;
    private boolean hasNext;

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        List<T> safeItems = items == null ? Collections.emptyList() : items;
        boolean hasNext = (long) page * size < total;
        return new PageResult<>(safeItems, total, page, size, hasNext);
    }

    /** 空分页快捷工厂。 */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size, false);
    }
}
