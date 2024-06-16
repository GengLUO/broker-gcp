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
        return "forward:/html/frontpage.html";
    }

    @GetMapping("/manager")
    public String showLManager() {
        return "forward:/html/manager.html";
    }

    @GetMapping("/profile")
    public String showProfile() { return "forward:/html/profile.html";}

    @GetMapping("/settings")
    public String showSettings() {
        return "forward:/html/settings.html";
    }

    @GetMapping("/mybookings")
    public String showBookings() {
        return "forward:/html/mybookings.html";
    }

    @GetMapping("/dashboard")
    public void showDashboard() {
    }

}
