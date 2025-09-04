package dev.incident_response.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.time.ZonedDateTime;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }

        String instance = System.getenv().getOrDefault("APP_INSTANCE", "unknown");

        model.addAttribute("hostname", hostname);
        model.addAttribute("instance", instance);
        model.addAttribute("time", ZonedDateTime.now().toString());
        return "index";
    }
}

