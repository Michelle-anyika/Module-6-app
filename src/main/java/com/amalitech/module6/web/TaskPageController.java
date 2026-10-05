package com.amalitech.module6.web;

import com.amalitech.module6.service.ReadResult;
import com.amalitech.module6.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskPageController {

    private static final String INVALID_TITLE = "Title is required (max 200 characters).";

    private final TaskService service;
    private final AppInfo appInfo;

    public TaskPageController(TaskService service, AppInfo appInfo) {
        this.service = service;
        this.appInfo = appInfo;
    }

    @GetMapping("/")
    public String index(Model model) {
        ReadResult result = service.findAll();
        model.addAttribute("tasks", result.tasks());
        model.addAttribute("source", result.source().name());
        model.addAttribute("elapsed", result.elapsedMillis());
        model.addAttribute("app", appInfo);
        return "index";
    }

    @PostMapping("/tasks")
    public String create(@Valid @ModelAttribute TaskForm form, BindingResult binding, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", INVALID_TITLE);
        } else {
            service.create(form.title());
        }
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/update")
    public String update(@PathVariable Long id, @Valid @ModelAttribute TaskForm form, BindingResult binding,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", INVALID_TITLE);
        } else {
            service.update(id, form.title(), null);
        }
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        service.toggle(id);
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/";
    }
}
