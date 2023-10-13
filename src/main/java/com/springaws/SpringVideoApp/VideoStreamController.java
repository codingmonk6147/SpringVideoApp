package com.springaws.SpringVideoApp;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Controller
public class VideoStreamController {

    private final VideoStreamService vid;

    VideoStreamController(VideoStreamService vid){
        this.vid = vid;
    }


    private final String bucket = "spring-aws-streaming-app";

    @RequestMapping(value = "/")
    public String root() {
        return "index";
    }

    @GetMapping("/watch")
    public String designer() {
        return "video";
    }

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }

    // Upload a MP4 to an Amazon S3 bucket
    @RequestMapping(value = "/fileupload", method = RequestMethod.POST)
    @ResponseBody
    public ModelAndView singleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam String description) {
        try {
            byte[] bytes = file.getBytes();
            String name = file.getOriginalFilename() ;

            // Put the MP4 file into an Amazon S3 bucket.
            vid.putVideo(bytes, bucket, name, description);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView(new RedirectView("upload"));
    }

    // Returns items to populate the Video menu.
    @RequestMapping(value = "/items", method = RequestMethod.GET)
    @ResponseBody
    public String getItems(HttpServletRequest request, HttpServletResponse response) {
        String xml = vid.getTags(bucket);
        return xml;
    }

    // Returns the video in the bucket specified by the ID value.
    @RequestMapping(value = "/{id}/stream", method = RequestMethod.GET)
    public Mono<ResponseEntity<byte[]>> streamVideo(@PathVariable String id) {
        String fileName = id;
        return Mono.just(vid.getObjectBytes(bucket, fileName));
    }
}
