package com.fundmatrix.transaction.client;

import com.fundmatrix.transaction.dto.NavRecordDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "nav-service")
public interface NavServiceClient {
    @GetMapping("/nav/scheme/{schemeId}/latest")
    NavRecordDto getLatestNav(@PathVariable("schemeId") Long schemeId);
}
