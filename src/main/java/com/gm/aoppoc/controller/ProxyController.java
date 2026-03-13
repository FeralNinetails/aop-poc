package com.gm.aoppoc.controller;

import com.gm.aoppoc.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProxyController {

    private final ApiService apiService;

    /*
    https://catfact.ninja/fact
    https://random.dog/woof.json
     */
    @GetMapping("/proxy")
    public String proxy(@RequestParam String url) {
        return apiService.callApi(url);
    }
}
