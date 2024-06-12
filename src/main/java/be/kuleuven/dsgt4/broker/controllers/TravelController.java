package be.kuleuven.dsgt4.broker.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TravelController {

    @GetMapping("/")
    public String showLoginForm() {
        return "forward:/index.html";
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {

        return "forward:/profile.html";
    }

    @GetMapping("/settings")
    public String showSettings() {
        return "forward:/settings.html";
    }



    @GetMapping("/mybookings")
    public String showBookings() {
        return "forward:/mybookings.html";
    }
    @GetMapping("/dashboard")
    public String showDashboard() {
        return "forward:/dashboard.html";
    }

}
