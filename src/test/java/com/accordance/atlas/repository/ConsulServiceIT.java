package com.accordance.atlas.repository;

import com.accordance.atlas.AtlasBootstrap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AtlasBootstrap.class)
@IntegrationTest
public class ConsulServiceIT {

    @Autowired
    ConsulService consulService;

    @Test
    public void getService() {
        System.out.println(consulService.getSingleServiceAddress("orientdb"));
    }
}
