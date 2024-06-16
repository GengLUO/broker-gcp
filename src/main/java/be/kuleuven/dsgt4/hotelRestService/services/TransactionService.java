package be.kuleuven.dsgt4.hotelRestService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TransactionService {
    private final WebClient webClient;
    //    TODO: change the endpoint
    private static final String CONFIRM_ENDPOINT = "https://airplane-europe.ew.r.appspot.com/......";

    @Autowired
    public TransactionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    //SO, THE OTHER SIDE SHOULD HAVE A POSTMAPPING TAHT WILL GET A JSON LIKE THIS:
    //{
    //  "packageId": "123",
    //  "commitEndpoint": "https://airplane-europe.ew.r.appspot.com/flights/commit/1"
    //}
    //I THINK WE HAVE TO SEND THE COMMIT ENDPOINT SO THAT THE BROKER KNOW WHERE TO SEND THE COMMIT MESSAGE
    //BUT THERE MAY BE OTHER WAYS I DO NOT KNOW
    public Mono<String> confirmAction(String packageId) {
        System.out.println("Sending confirmation to the CONFIRM_ENDPOINT");

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