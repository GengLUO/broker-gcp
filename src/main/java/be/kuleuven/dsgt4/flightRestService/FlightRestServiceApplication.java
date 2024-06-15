//package be.kuleuven.dsgt4.flightRestService;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.hateoas.config.EnableHypermediaSupport;
//import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
//import org.springframework.http.client.reactive.ReactorClientHttpConnector;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.netty.http.client.HttpClient;
//
//@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
//@SpringBootApplication
//@PropertySource("classpath:plane-application.properties")
//public class FlightRestServiceApplication {
//    public static void main(String[] args) {
//        SpringApplication.run(FlightRestServiceApplication.class, args);
//    }
//
//    /*
//     * You can use this builder to create a Spring WebClient instance which can be used to make REST-calls.
//     */
//    @Bean
//    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
//        return configurer.registerHypermediaTypes(WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
//                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
//    }
//}
//
//// package be.kuleuven.dsgt4.flightRestService;
//
//// import org.springframework.boot.SpringApplication;
//// import org.springframework.boot.autoconfigure.SpringBootApplication;
//// import org.springframework.context.annotation.PropertySource;
//
//// @SpringBootApplication
//// @PropertySource("classpath:plane-application.properties")
//// public class FlightRestServiceApplication {
////     public static void main(String[] args) {
////         SpringApplication.run(FlightRestServiceApplication.class, args);
////     }
//// }