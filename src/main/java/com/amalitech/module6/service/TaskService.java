package com.amalitech.module6.service;

import com.amalitech.module6.domain.Task;
import com.amalitech.module6.domain.TaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * Cache-aside: reads try Redis first and fall back to RDS (via RDS Proxy),
 * repopulating the cache; every write goes to RDS and then evicts the cache.
 * Redis failures degrade to direct database reads instead of failing requests.
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final String CACHE_KEY = "tasks:all";
    private static final TypeReference<List<Task>> TASK_LIST = new TypeReference<>() { };

    private final TaskRepository repository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public TaskService(TaskRepository repository,
                       StringRedisTemplate redis,
                       ObjectMapper objectMapper,
                       @Value("${app.cache.ttl-seconds:300}") long ttlSeconds) {
        this.repository = repository;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Transactional(readOnly = true)
    public ReadResult findAll() {
        long start = System.nanoTime();
        try {
            String cached = redis.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                List<Task> tasks = objectMapper.readValue(cached, TASK_LIST);
                log.info("cache HIT key={} items={}", CACHE_KEY, tasks.size());
                return new ReadResult(tasks, ReadResult.Source.REDIS, elapsedMillis(start));
            }
        } catch (Exception e) {
            log.warn("Redis read failed, falling back to database: {}", e.getMessage());
        }

        List<Task> tasks = repository.findAllByOrderByCompletedAscCreatedAtDesc();
        log.info("cache MISS key={} items={} loaded from RDS", CACHE_KEY, tasks.size());
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(tasks), ttl);
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }
        return new ReadResult(tasks, ReadResult.Source.RDS, elapsedMillis(start));
    }

    @Transactional(readOnly = true)
    public Task findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public Task create(String title) {
        Task saved = repository.save(new Task(title.trim()));
        evict();
        return saved;
    }

    @Transactional
    public Task update(Long id, String title, Boolean completed) {
        Task task = findById(id);
        if (title != null && !title.isBlank()) {
            task.setTitle(title.trim());
        }
        if (completed != null) {
            task.setCompleted(completed);
        }
        Task saved = repository.save(task);
        evict();
        return saved;
    }

    @Transactional
    public Task toggle(Long id) {
        return update(id, null, !findById(id).isCompleted());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        repository.deleteById(id);
        evict();
    }

    private void evict() {
        try {
            redis.delete(CACHE_KEY);
        } catch (Exception e) {
            log.warn("Redis evict failed, cache may be stale for up to {}s: {}", ttl.toSeconds(), e.getMessage());
        }
    }

    private static long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
