package com.revapp.planengine.domain.utils;

public record PageRequest(int offset, int limit) {
    public static PageRequest of(Integer offset, Integer limit) {
        int o = (offset == null ? 0 : Math.max(0, offset));
        int l = (limit == null ? 20 : Math.max(1, Math.min(100, limit)));
        return new PageRequest(o, l);
    }
}
