package com.amalitech.module6.service;

import com.amalitech.module6.domain.Task;

import java.util.List;

/** A list read plus where it was served from, so the UI can show cache hits vs misses. */
public record ReadResult(List<Task> tasks, Source source, long elapsedMillis) {

    public enum Source { REDIS, RDS }
}
