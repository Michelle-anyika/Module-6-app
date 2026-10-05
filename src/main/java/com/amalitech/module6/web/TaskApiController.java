package com.amalitech.module6.web;

import com.amalitech.module6.domain.Task;
import com.amalitech.module6.service.ReadResult;
import com.amalitech.module6.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private final TaskService service;

    public TaskApiController(TaskService service) {
        this.service = service;
    }

    /** X-Data-Source header reports REDIS (cache hit) or RDS (cache miss). */
    @GetMapping
    public ResponseEntity<List<Task>> list() {
        ReadResult result = service.findAll();
        return ResponseEntity.ok()
                .header("X-Data-Source", result.source().name())
                .header("X-Read-Millis", String.valueOf(result.elapsedMillis()))
                .body(result.tasks());
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody TaskForm form) {
        return service.create(form.title());
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody TaskForm form) {
        return service.update(id, form.title(), form.completed());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
