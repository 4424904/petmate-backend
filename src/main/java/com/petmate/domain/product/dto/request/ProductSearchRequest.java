package com.petmate.domain.product.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {

    private Integer companyId;
    private String serviceType;
    private Integer minPrice;
    private Integer maxPrice;
    private String keyword;
    private Integer isActive = 1;
    private Integer limit;

}
