package org.scoula.travel.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.pagination.Page;
import org.scoula.pagination.PageRequest;
import org.scoula.travel.dto.TravelDTO;
import org.scoula.travel.dto.TravelImageDTO;
import org.scoula.travel.service.TravelService;
import org.scoula.util.UploadFiles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/travel")
public class TravelController {
    @org.springframework.beans.factory.annotation.Value("${upload.local.path}")
    private String uploadPath;

    final TravelService service;

    @GetMapping("")
    public ResponseEntity<Page> getTravel(PageRequest pageRequest) {
        return ResponseEntity.ok(service.getPage(pageRequest));
    }

    @GetMapping("/{no}")
    public ResponseEntity<TravelDTO> getTravels(@PathVariable("no") Long no) {
        return ResponseEntity.ok(service.get(no));
    }

    @GetMapping("/image/{no}")
    public void viewImage(@PathVariable Long no, HttpServletResponse response) {
        TravelImageDTO image = service.getImage(no);
        File file = new File(image.getPath(uploadPath));
        UploadFiles.downloadImage(response, file);
    }
}
