/*
 *
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magnum.dataup;


import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import retrofit.client.Response;
import retrofit.http.Path;
import retrofit.mime.TypedFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

import static org.magnum.dataup.VideoSvcApi.VIDEO_DATA_PATH;
import static org.magnum.dataup.VideoSvcApi.VIDEO_SVC_PATH;

@Controller
@RestController
public class VideoController {

    /**
     * You will need to create one or more Spring controllers to fulfill the
     * requirements of the assignment. If you use this file, please rename it
     * to something other than "AnEmptyController"
     *
     *
     ________  ________  ________  ________          ___       ___  ___  ________  ___  __
     |\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \
     \ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_
     \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \
     \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \
     \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
     \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
     *
     */

    /**
     * This endpoint in the API returns a list of the videos that have
     * been added to the server. The Video objects should be returned as
     * JSON.
     * <p>
     * To manually test this endpoint, run your server and open this URL in a browser:
     * http://localhost:8080/video
     *
     * @return
     */

    ArrayList<Video> videos = new ArrayList<>();

    @ResponseBody
    @GetMapping(VIDEO_SVC_PATH)
    public Collection<Video> getVideoList() {
        return videos;
    }

    /**
     * This endpoint allows clients to add Video objects by sending POST requests
     * that have an application/json body containing the Video object information.
     *
     * @param v
     * @return
     */
    @ResponseBody
    @PostMapping(VIDEO_SVC_PATH)
    public Video addVideo(@RequestBody Video v) {
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        Long id = Math.abs(random.nextLong());
        String path = this.getUrlBaseForLocalServer() +"/" + id.toString() + "/data";
		v.setId(id);
		v.setDataUrl(path);
        videos.add(v);
        return v;
    }

    /**
     * This endpoint allows clients to set the mpeg video data for previously
     * added Video objects by sending multipart POST requests to the server.
     * The URL that the POST requests should be sent to includes the ID of the
     * Video that the data should be associated with (e.g., replace {id} in
     * the url /video/{id}/data with a valid ID of a video, such as /video/1/data
     * -- assuming that "1" is a valid ID of a video).
     *
     * @param id
     * @param videoData
     * @return
     */

    @PostMapping(value = "/video/{id}/data")
    public VideoStatus setVideoData(@PathVariable("id") long id, @RequestParam("data") MultipartFile videoData, HttpServletResponse response) {
        Video v = null;
        for (Video video : videos) {
            if (video.getId() == id) {
                v = video;
            }
        }
        if (v != null) {
            try {
                VideoFileManager videoFileManager = VideoFileManager.get();
                videoFileManager.saveVideoData(v, videoData.getInputStream());
            } catch (Exception e) {
                return null;
            }
        }else{
            response.setStatus(404);
        }
        return new VideoStatus(VideoStatus.VideoState.READY);
    }

    /**
     * This endpoint should return the video data that has been associated with
     * a Video object or a 404 if no video data has been set yet. The URL scheme
     * is the same as in the method above and assumes that the client knows the ID
     * of the Video object that it would like to retrieve video data for.
     * <p>
     * This method uses Retrofit's @Streaming annotation to indicate that the
     * method is going to access a large stream of data (e.g., the mpeg video
     * data on the server). The client can access this stream of data by obtaining
     * an InputStream from the Response as shown below:
     * <p>
     * VideoSvcApi client = ... // use retrofit to create the client
     * Response response = client.getData(someVideoId);
     * InputStream videoDataStream = response.getBody().in();
     *
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping(VIDEO_DATA_PATH)
    public void getData(@PathVariable("id") long id, HttpServletResponse response) {
        Video v = null;
        for (Video video : videos) {
            if (video.getId() == id) {
                v = video;
            }
        }
        if (v == null){
            response.setStatus(404);
        }else {
            try {
                VideoFileManager videoFileManager = VideoFileManager.get();
                videoFileManager.copyVideoData(v, response.getOutputStream());
                response.flushBuffer();
            } catch (Exception e) {
                return;
            }
        }
        return;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base = "http://" + request.getServerName() + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
        return base;
    }
}
