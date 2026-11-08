package com.grupocordillera.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=${random.uuid}${random.uuid}")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
