package com.andrew.hnt.api.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 파비콘 공통 매핑 컨트롤러
 * - 모든 페이지의 /favicon.ico 요청을 /static/images/hntbi.png 로 매핑하여 PNG 아이콘을 반환합니다.
 */
@Controller
public class FaviconController {

  @GetMapping(value = "/favicon.ico", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<Resource> favicon() {
    Resource png = new ClassPathResource("static/images/hntbi.png");
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(png);
  }
}


















