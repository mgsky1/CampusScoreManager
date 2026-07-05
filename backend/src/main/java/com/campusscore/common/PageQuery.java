package com.campusscore.common;

import java.util.Collections;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分页 / 排序 / 搜索的统一请求参数。对应 {@code contracts/api.md §0.4}。
 *
 * <p>规范：
 *
 * <ul>
 *   <li>{@code page} 从 1 起，&le; 0 规范化为 1；
 *   <li>{@code size} 服务端 clamp 到 {@code [1, 100]}；
 *   <li>{@code sort} 形如 {@code "field:asc"} / {@code "field:desc"}，仅当 field 在白名单
 *       中才生效，非法输入被忽略并回退到默认排序。
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageQuery {

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    private Integer page;
    private Integer size;
    private String keyword;
    private String sort;

    /** 规范化后的页码（&ge; 1）。 */
    public int safePage() {
        return page == null || page <= 0 ? 1 : page;
    }

    /** 规范化后的每页条数（clamp 到 {@code [MIN_SIZE, MAX_SIZE]}）。 */
    public int safeSize() {
        int s = size == null ? DEFAULT_SIZE : size;
        if (s < MIN_SIZE) {
            return MIN_SIZE;
        }
        if (s > MAX_SIZE) {
            return MAX_SIZE;
        }
        return s;
    }

    /** SQL {@code OFFSET} 值。 */
    public int offset() {
        return (safePage() - 1) * safeSize();
    }

    /**
     * 解析 {@code sort} 表达式为形如 {@code "field ASC"} 的 SQL 片段。
     * 若 {@code sort} 为空、格式非法或字段不在白名单，返回 {@code null}
     * （调用方回退到默认排序）。
     */
    public String safeSort(Set<String> whitelist) {
        if (sort == null || sort.isEmpty() || whitelist == null || whitelist.isEmpty()) {
            return null;
        }
        String[] parts = sort.split(":");
        if (parts.length != 2) {
            return null;
        }
        String field = parts[0].trim();
        String dir = parts[1].trim().toLowerCase();
        if (!whitelist.contains(field)) {
            return null;
        }
        if (!"asc".equals(dir) && !"desc".equals(dir)) {
            return null;
        }
        return field + " " + dir.toUpperCase();
    }

    /** 关键词，null 或空白视为不过滤。 */
    public String safeKeyword() {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 只读的默认白名单（空集，子类/调用方按需替换）。 */
    public static Set<String> emptyWhitelist() {
        return Collections.emptySet();
    }
}
