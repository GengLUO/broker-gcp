package be.kuleuven.dsgt4.broker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class FlightSupplierService {
    private final WebClient webClient;

    @Autowired
    public FlightSupplierService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8086/flights").build(); // 修改为实际的PlaneTicketRestService的URL
    }

    public String getFlights(String apiKey) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/all").queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(String.class) // 将响应体解析为字符串
                .block();
    }

    public String getFlight(Long id, String apiKey) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/{id}").queryParam("key", apiKey).build(id))
                .retrieve()
                .bodyToMono(String.class) // 将响应体解析为字符串
                .block();
    }

    public boolean bookFlight(Long flightId, int seats, String apiKey) {
        return Boolean.TRUE.equals(this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/book")
                        .queryParam("flightId", flightId)
                        .queryParam("seats", seats)
                        .queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());
    }

    public boolean isFlightAvailable(Long flightId, int seats, String apiKey) {
        return Boolean.TRUE.equals(this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/available")
                        .queryParam("flightId", flightId)
                        .queryParam("seats", seats)
                        .queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());
    }

    public boolean cancelFlight(Long flightId, int seats, String apiKey) {
        return Boolean.TRUE.equals(this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/cancel")
                        .queryParam("flightId", flightId)
                        .queryParam("seats", seats)
                        .queryParam("key", apiKey).build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());
    }
}
