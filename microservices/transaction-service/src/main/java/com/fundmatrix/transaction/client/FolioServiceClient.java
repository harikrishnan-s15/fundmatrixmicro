package com.fundmatrix.transaction.client;

import com.fundmatrix.transaction.dto.FolioDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "folio-service")
public interface FolioServiceClient {
    @GetMapping("/folios/{id}")
    FolioDto getFolioById(@PathVariable("id") Long id);
}
