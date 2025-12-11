package com.nhnacademy.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "toss.secret-key=dummy-secret-key-for-test"
})
@Sql("/schema.sql")
class OrderApplicationTests {

    @Test
    void contextLoads() {
    }

}
