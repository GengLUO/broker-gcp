package be.kuleuven.dsgt4.broker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class HotelSupplierService {
    private final WebClient webClient;

    @Autowired
    public HotelSupplierService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8086/hotels").build();  // 修改为实际的HotelRestService的URL
    }

    // An API key is a string used to verify the identity of a client. It is typically used as an authentication method when invoking REST API to ensure that only authorized clients can access the API
    public String getHotels(String apiKey) {
        return this.webClient.get() // create a GET request
                .uri(uriBuilder -> uriBuilder.path("/all").queryParam("key", apiKey).build()) // Set WebClient base URL to http://localhost:8086/hotels. In this way, when using WebClient to make HTTP requests, you do not need to specify the full URL each time, you can use a relative path
                .retrieve() // execute the request and get a response
                .bodyToMono(String.class) // 将响应体解析为字符串
                .block(); // 阻塞直到响应返回，并返回结果
    }

    public String getHotel(Long id, String apiKey) {
        return this.webClient.get() // create a GET request
                .uri(uriBuilder -> uriBuilder.path("/{id}").queryParam("key", apiKey).build(id))
                .retrieve() // execute the request and get a response
                .bodyToMono(String.class) // 将响应体解析为字符串
                .block(); // 阻塞直到响应返回，并返回结果
    }

    public boolean bookHotel(Long hotelId, int rooms, String apiKey) {
        // 构建一个POST请求
        // 将响应体解析为单个字符串的Mono
        return Boolean.TRUE.equals(this.webClient.post() // 构建一个POST请求
                .uri(uriBuilder -> uriBuilder.path("/book")
                        .queryParam("hotelId", hotelId)
                        .queryParam("rooms", rooms)
                        .queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(Boolean.class) // 将响应体解析为单个字符串的Mono
                .block());
    }

    public boolean isHotelAvailable(Long hotelId, int rooms, String apiKey) {
        return Boolean.TRUE.equals(this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/available")
                        .queryParam("hotelId", hotelId)
                        .queryParam("rooms", rooms)
                        .queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());
    }

    public boolean cancelHotel(Long hotelId, int rooms, String apiKey) {
        return Boolean.TRUE.equals(this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/cancel")
                        .queryParam("hotelId", hotelId)
                        .queryParam("rooms", rooms)
                        .queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());
    }
}
