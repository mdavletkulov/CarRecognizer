package com.gorcer.iseeyou.controllers;

import com.gorcer.iseeyou.model.CarNumberResponse;
import com.gorcer.iseeyou.service.Anrp;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Controller
public class MainController {

    @Autowired
    private Anrp anrp;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/recognize/{fileName}")
    public ResponseEntity<CarNumberResponse> recognize(@PathVariable String fileName) throws IOException {
        byte[] decodedBytes = Base64.decodeBase64(fileName.getBytes());
        ResponseEntity<CarNumberResponse> result = anrp.recognize(new String(decodedBytes));
        return result;
    }

    @GetMapping("/recognize")
    public ResponseEntity<CarNumberResponse> recognize1() {
        byte[] encodedBytes = Base64.encodeBase64("http://s.auto.drom.ru/i24195/s/photos/21465/21464270/167091099.jpg".getBytes());
        return restTemplate.getForEntity("http://localhost:8081/recognize/"+new String(encodedBytes), CarNumberResponse.class);
    }

}
