package dev.incident_response.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @GetMapping("/session")
    @ResponseBody
    public String sessionInfo(HttpSession session) {
        Object val = session.getAttribute("demo");
        return "sessionId=" + session.getId() + ", demo=" + (val == null ? "null" : val.toString());
    }

    @PostMapping("/session")
    @ResponseBody
    public String setSession(HttpSession session) {
        session.setAttribute("demo", "hello");
        return "OK:" + session.getId();
    }
}

