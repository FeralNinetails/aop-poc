package com.gm.aoppoc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final RestTemplate restTemplate;

    public String callApi(String url) {
        return restTemplate.getForObject(url, String.class);
    }
}
