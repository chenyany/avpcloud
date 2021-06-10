package com.navinfo.server.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 服务状态监控
 */
@Controller
public class MonitorController {

    @Value("${host}")
    private String host;

    @RequestMapping("monitor")
    public String index(Model model) {
        model.addAttribute("host", host);
        return "index";
    }
}
