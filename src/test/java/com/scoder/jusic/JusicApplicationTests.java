package com.scoder.jusic;

import com.scoder.jusic.configuration.JusicInitializing;
import com.scoder.jusic.job.MusicTopJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class JusicApplicationTests {

    @MockBean
    private JusicInitializing jusicInitializing;
    @MockBean
    private MusicTopJob musicTopJob;

    @Test
    void contextLoads() {

    }

}
