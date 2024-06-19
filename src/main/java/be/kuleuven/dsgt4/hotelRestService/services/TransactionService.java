package be.kuleuven.dsgt4.hotelRestService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service("hotelTransactionService")
public class TransactionService {
    private final WebClient.Builder webClientBuilder;

    //    TODO: change the endpoint
//    https://broker-da44b.uc.r.appspot.com/feedback/confirmHotel
    private static final String CONFIRM_ENDPOINT = "https://broker-da44b.uc.r.appspot.com/feedback/confirmHotel";

    @Autowired
    public TransactionService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<String> confirmAction(String packageId) {
        System.out.println("Sending confirmation to: " + CONFIRM_ENDPOINT + " with packageId: "+ packageId);

        Mono<String> requestBody = Mono.just(packageId);

        requestBody.subscribe(data -> System.out.println("Request body sent: " + data));

        return webClientBuilder.build()
                .post()
                .uri(CONFIRM_ENDPOINT)
                .body(BodyInserters.fromValue(packageId))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("Response received: " + response);
                })
                .doOnError(e -> {
                    System.out.println("Error occurred: " + e.getMessage());
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doAfterRetry(retrySignal -> {
                            System.out.println("Retrying... Attempt: " + (retrySignal.totalRetries() + 1));
                        })
                )
                .onErrorResume(e -> {
                    System.out.println("Error occurred after retries: " + e.getMessage());
                    return Mono.just("Error occurred: " + e.getMessage());
                });
    }

}