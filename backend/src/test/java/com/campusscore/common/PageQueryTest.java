package com.campusscore.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void nullOrNonPositivePageBecomesOne() {
        assertThat(new PageQuery(null, null, null, null).safePage()).isEqualTo(1);
        assertThat(new PageQuery(0, null, null, null).safePage()).isEqualTo(1);
        assertThat(new PageQuery(-5, null, null, null).safePage()).isEqualTo(1);
        assertThat(new PageQuery(3, null, null, null).safePage()).isEqualTo(3);
    }

    @Test
    void sizeIsClampedToUpperBoundOneHundred() {
        assertThat(new PageQuery(1, 500, null, null).safeSize()).isEqualTo(100);
        assertThat(new PageQuery(1, 100, null, null).safeSize()).isEqualTo(100);
        assertThat(new PageQuery(1, 20, null, null).safeSize()).isEqualTo(20);
    }

    @Test
    void sizeIsClampedToLowerBoundOne() {
        assertThat(new PageQuery(1, 0, null, null).safeSize()).isEqualTo(1);
        assertThat(new PageQuery(1, -3, null, null).safeSize()).isEqualTo(1);
    }

    @Test
    void nullSizeDefaultsToTwenty() {
        assertThat(new PageQuery(1, null, null, null).safeSize()).isEqualTo(20);
    }

    @Test
    void offsetIsComputedFromSafePageAndSize() {
        assertThat(new PageQuery(1, 20, null, null).offset()).isEqualTo(0);
        assertThat(new PageQuery(2, 20, null, null).offset()).isEqualTo(20);
        assertThat(new PageQuery(5, 15, null, null).offset()).isEqualTo(60);
        assertThat(new PageQuery(0, 20, null, null).offset()).isEqualTo(0);
    }

    @Test
    void safeSortAcceptsWhitelistedFieldOnly() {
        Set<String> wl = new HashSet<>();
        wl.add("createdAt");
        wl.add("score");
        assertThat(new PageQuery(1, 20, null, "createdAt:desc").safeSort(wl))
                .isEqualTo("createdAt DESC");
        assertThat(new PageQuery(1, 20, null, "score:asc").safeSort(wl)).isEqualTo("score ASC");
    }

    @Test
    void safeSortRejectsUnknownFieldOrDirection() {
        Set<String> wl = new HashSet<>();
        wl.add("createdAt");
        assertThat(new PageQuery(1, 20, null, "password:asc").safeSort(wl)).isNull();
        assertThat(new PageQuery(1, 20, null, "createdAt:sideways").safeSort(wl)).isNull();
        assertThat(new PageQuery(1, 20, null, "malformed").safeSort(wl)).isNull();
        assertThat(new PageQuery(1, 20, null, null).safeSort(wl)).isNull();
        assertThat(new PageQuery(1, 20, null, "").safeSort(wl)).isNull();
    }

    @Test
    void safeKeywordTrimsAndNullsBlank() {
        assertThat(new PageQuery(1, 20, "  张三  ", null).safeKeyword()).isEqualTo("张三");
        assertThat(new PageQuery(1, 20, "   ", null).safeKeyword()).isNull();
        assertThat(new PageQuery(1, 20, null, null).safeKeyword()).isNull();
    }
}
