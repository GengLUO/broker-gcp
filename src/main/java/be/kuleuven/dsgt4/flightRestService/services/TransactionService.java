package be.kuleuven.dsgt4.flightRestService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class TransactionService {
//    @Autowired
//    private WebClient.Builder webClientBuilder;
//
//    private WebClient webClient;
//
//    private static final String ENDPOINT = "https://airplane-europe.ew.r.appspot.com/flights/pubsub/push";

    private final WebClient webClient;

//    TODO: change the endpoint
    private static final String CONFIRM_ENDPOINT = "https://airplane-europe.ew.r.appspot.com/......";

    @Autowired
    public TransactionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> confirmAction(String messageId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(CONFIRM_ENDPOINT)
                        .queryParam("messageID", messageId)
                        .build())
                .retrieve() // Use retrieve() to fetch the data and map it to a Mono
                .bodyToMono(String.class) // Convert the body to a Mono of type String
                .onErrorResume(e -> Mono.just("Error occurred: " + e.getMessage())); // Simple error handling
    }

}
