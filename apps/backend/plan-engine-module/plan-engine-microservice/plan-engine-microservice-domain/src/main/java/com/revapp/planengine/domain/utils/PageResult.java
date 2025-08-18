package com.revapp.planengine.domain.utils;

import java.util.List;

public record PageResult<T>(List<T> items, int offset, int limit, long total) { }
