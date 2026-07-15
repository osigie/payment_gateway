package com.osigie.payment_gateway.dto;

import java.util.List;

public record PageResponse<T>(
        long totalRecords,
        int pageNo,
        int pageSize,
        List<T> content
) {
}
