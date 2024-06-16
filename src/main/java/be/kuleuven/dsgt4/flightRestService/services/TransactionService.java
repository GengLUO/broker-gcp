package be.kuleuven.dsgt4.flightRestService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TransactionService {

    private final WebClient webClient;
//    TODO: change the endpoint
    private static final String CONFIRM_ENDPOINT = "https://broker-da44b.uc.r.appspot.com/feedback/confirmHotel";

    @Autowired
    public TransactionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> confirmAction(String packageId) {
        System.out.println("Send confirmation to the CONFIRM_ENDPOINT");

        ConfirmRequest request = new ConfirmRequest(packageId);

        return this.webClient.post()
                .uri(CONFIRM_ENDPOINT)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("Error occurred: " + e.getMessage()));
    }

    private static class ConfirmRequest {
        private String packageId;

        public ConfirmRequest(String packageId) {
            this.packageId = packageId;
        }

        public String getPackageId() {
            return packageId;
        }

        public void setPackageId(String packageId) {
            this.packageId = packageId;
        }
    }

}
