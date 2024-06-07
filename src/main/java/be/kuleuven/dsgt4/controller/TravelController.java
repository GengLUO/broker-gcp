package be.kuleuven.dsgt4.controller;


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

    @PostMapping("/login")
    public String handleLogin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              Model model) {
        if (validateEmail(email) && validatePassword(password)) {
            model.addAttribute("email", email);
            model.addAttribute("name", "John Doe"); // Example name, replace with actual logic
            return "dashboard";
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "index";
        }
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        // Replace with actual logic to get user details
        model.addAttribute("email", "user@example.com");
        model.addAttribute("name", "John Doe");
        return "forward:/profile.html";
    }

    @GetMapping("/settings")
    public String showSettings() {
        return "forward:/settings.html";
    }

    @PostMapping("/updateSettings")
    public String updateSettings(@RequestParam("newSetting") String newSetting, Model model) {
        // Add logic to update settings
        model.addAttribute("message", "Settings updated to: " + newSetting);
        return "settings";
    }

    @GetMapping("/dashboard")
    public String showDashboard() {
        return "dashboard";
    }

    @PostMapping("/reserve")
    public String handleReservation(@RequestParam("hotel") String hotel, Model model) {
        // Add logic to handle reservation
        model.addAttribute("message", "Reserved at " + hotel);
        return "dashboard";
    }

    private boolean validateEmail(String email) {
        // Simple email validation logic
        return email != null && email.contains("@");
    }

    private boolean validatePassword(String password) {
        // Simple password validation logic
        return password != null && !password.isEmpty();
    }
}
