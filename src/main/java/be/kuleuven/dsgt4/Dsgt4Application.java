package be.kuleuven.dsgt4;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Dsgt4Application {



	@SuppressWarnings("unchecked")
	public static void main(String[] args)  {
		//System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));
		SpringApplication.run(Dsgt4Application.class, args);

}

	@Bean
	public boolean isProduction() {
		return Objects.equals(System.getenv("GAE_ENV"), "standard");
	}

    @Bean
    public String projectId() {
        if (this.isProduction()) {
            return "broker-da44b"; // production project ID
        } else {
            // return "demo-distributed-systems-kul"; // local project ID
			return "broker-da44b"; // local project ID
        }
    }

//	@Bean
//    public Firestore firestore() {
//        FirestoreOptions.Builder firestoreOptionsBuilder = FirestoreOptions.getDefaultInstance().toBuilder()
//                .setProjectId(projectId());
//        if (!isProduction()) {
//            firestoreOptionsBuilder.setCredentials(new FirestoreOptions.EmulatorCredentials())
//                    .setEmulatorHost("localhost:8084");
//        }
//        return firestoreOptionsBuilder.build().getService();
//    }

	@Bean
	public Firestore firestore() throws IOException {
		FileInputStream serviceAccount =
				new FileInputStream("src/broker-da44b-firebase-adminsdk-7s570-00d5f8b517.json");

		FirestoreOptions.Builder firestoreOptionsBuilder = FirestoreOptions.getDefaultInstance().toBuilder()
				.setProjectId(projectId());
		if (!isProduction()) {
			firestoreOptionsBuilder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
		}
		return firestoreOptionsBuilder.build().getService();
	}

	/*
	 * You can use this builder to create a Spring WebClient instance which can be used to make REST-calls.
	 */
	@Bean
	WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
		return configurer.registerHypermediaTypes(WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
				.codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
	}

	@Bean
	HttpFirewall httpFirewall() {
		DefaultHttpFirewall firewall = new DefaultHttpFirewall();
		firewall.setAllowUrlEncodedSlash(true);
		return firewall;
	}	

}
