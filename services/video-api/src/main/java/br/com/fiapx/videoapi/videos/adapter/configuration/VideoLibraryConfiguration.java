package br.com.fiapx.videoapi.videos.adapter.configuration;

import br.com.fiapx.videoapi.videos.application.FindVideoDetail;
import br.com.fiapx.videoapi.videos.application.FindVideoLibrary;
import br.com.fiapx.videoapi.videos.application.port.out.VideoLibraryReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class VideoLibraryConfiguration {
    @Bean
    FindVideoLibrary findVideoLibrary(VideoLibraryReader reader) {
        return new FindVideoLibrary(reader);
    }

    @Bean
    FindVideoDetail findVideoDetail(VideoLibraryReader reader) {
        return new FindVideoDetail(reader);
    }
}
