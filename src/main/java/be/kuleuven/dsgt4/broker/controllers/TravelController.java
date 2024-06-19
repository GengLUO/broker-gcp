package be.kuleuven.dsgt4.broker.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class TravelController {

    @GetMapping("/")
    public String showLoginForm() {
      return "forward:/html/frontpage.html";
    }

}
