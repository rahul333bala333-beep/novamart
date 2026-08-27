package com.novamart.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nova Mart API gateway.
 *
 * <p>The one address every client knows. Browsers never reach a service
 * directly, which is what lets services be split, moved or renamed without any
 * client changing, and what gives the platform a single place to terminate CORS,
 * check tokens and scrub inbound headers.
 *
 * <p>Reactive rather than servlet-based, unlike the services behind it. A
 * gateway spends nearly all of its time waiting on the network, so a
 * non-blocking stack holds far more concurrent connections per instance than a
 * thread-per-request one. The services do blocking JDBC work, where the servlet
 * model is the simpler and faster fit. Matching each layer to its workload is
 * worth the inconsistency.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
