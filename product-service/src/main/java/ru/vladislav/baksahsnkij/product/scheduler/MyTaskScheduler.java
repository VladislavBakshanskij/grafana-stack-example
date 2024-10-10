package ru.vladislav.baksahsnkij.product.scheduler;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.vladislav.baksahsnkij.product.repository.TaskRepository;
import ru.vladislav.baksahsnkij.product.model.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MyTaskScheduler {
    private final TaskRepository taskRepository;
    private final TaskDecorator taskDecorator;
    private final Tracer tracer;

    public MyTaskScheduler(TaskRepository taskRepository,
                           TaskDecorator taskDecorator,
                           Tracer tracer) {
        this.taskRepository = taskRepository;
        this.taskDecorator = taskDecorator;
        this.tracer = tracer;
    }

    @Scheduled(fixedDelay = 10_000)
    public void execute() {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            List<Task> all = taskRepository.findAll();
            all.forEach(task -> {
                        try {
                            MDC.setContextMap(task.getMdc());
                            CompletableFuture.runAsync(() -> {
                                CurrentTraceContext currentTraceContext = tracer.currentTraceContext();
                                taskRepository.delete(task);
                                System.out.println("TRACEID --> " + currentTraceContext.context().traceId());
                            }, command -> executor.execute(taskDecorator.decorate(command))).join();
                        } finally {
                            MDC.clear();
                        }
                    });
        }
    }

    public TaskRepository taskRepository() {
        return taskRepository;
    }

    public TaskDecorator taskDecorator() {
        return taskDecorator;
    }

    public Tracer tracer() {
        return tracer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MyTaskScheduler) obj;
        return Objects.equals(this.taskRepository, that.taskRepository) &&
                Objects.equals(this.taskDecorator, that.taskDecorator) &&
                Objects.equals(this.tracer, that.tracer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskRepository, taskDecorator, tracer);
    }

    @Override
    public String toString() {
        return "TaskScheduler[" +
                "taskRepository=" + taskRepository + ", " +
                "taskDecorator=" + taskDecorator + ", " +
                "tracer=" + tracer + ']';
    }

}
