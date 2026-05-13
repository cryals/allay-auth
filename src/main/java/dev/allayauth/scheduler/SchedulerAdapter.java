package dev.allayauth.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SchedulerAdapter implements AutoCloseable {
    private final Plugin plugin;
    private final boolean folia;
    private final ExecutorService asyncExecutor;
    private final ScheduledExecutorService timerExecutor;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
        this.asyncExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                runnable -> {
                    Thread thread = new Thread(runnable, "AllayAuth-Async");
                    thread.setDaemon(true);
                    return thread;
                });
        this.timerExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "AllayAuth-Timer");
            thread.setDaemon(true);
            return thread;
        });
        plugin.getLogger().info("Scheduler mode: " + (folia ? "Folia" : "Bukkit/Paper"));
    }

    public boolean isFolia() {
        return folia;
    }

    public ExecutorService asyncExecutor() {
        return asyncExecutor;
    }

    public void runAsync(Runnable task) {
        asyncExecutor.execute(wrap(task));
    }

    public void runGlobal(Runnable task) {
        if (folia && tryGlobalScheduler(task)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, wrap(task));
    }

    public void runForPlayer(Player player, Runnable task) {
        if (folia && tryEntityScheduler(player, task)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                wrap(task).run();
            }
        });
    }

    public void runAtLocation(Location location, Runnable task) {
        if (folia && tryRegionScheduler(location, task)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, wrap(task));
    }

    public TaskHandle runLaterForPlayer(Player player, long delayTicks, Runnable task) {
        ScheduledFuture<?> future = timerExecutor.schedule(
                () -> runForPlayer(player, task),
                ticksToMillis(delayTicks),
                TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    public TaskHandle runRepeatingForPlayer(Player player, long initialDelayTicks, long periodTicks, Runnable task) {
        ScheduledFuture<?> future = timerExecutor.scheduleAtFixedRate(
                () -> runForPlayer(player, task),
                ticksToMillis(initialDelayTicks),
                Math.max(50L, ticksToMillis(periodTicks)),
                TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    public TaskHandle runLaterAsync(long delayTicks, Runnable task) {
        ScheduledFuture<?> future = timerExecutor.schedule(wrap(task), ticksToMillis(delayTicks), TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    public TaskHandle runRepeatingAsync(long initialDelayTicks, long periodTicks, Runnable task) {
        ScheduledFuture<?> future = timerExecutor.scheduleAtFixedRate(
                wrap(task),
                ticksToMillis(initialDelayTicks),
                Math.max(50L, ticksToMillis(periodTicks)),
                TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public void close() {
        timerExecutor.shutdownNow();
        asyncExecutor.shutdownNow();
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private boolean tryGlobalScheduler(Runnable task) {
        try {
            Method getter = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object scheduler = getter.invoke(null);
            Optional<Method> execute = findMethod(scheduler.getClass(), "execute", 2);
            if (execute.isPresent()) {
                execute.get().invoke(scheduler, plugin, wrap(task));
                return true;
            }
            Optional<Method> run = findMethod(scheduler.getClass(), "run", 2);
            if (run.isPresent()) {
                run.get().invoke(scheduler, plugin, java.util.function.Consumer.class.cast((java.util.function.Consumer<Object>) ignored -> wrap(task).run()));
                return true;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to dispatch global Folia task, falling back to Bukkit scheduler.", ex);
        }
        return false;
    }

    private boolean tryEntityScheduler(Player player, Runnable task) {
        try {
            Method getter = player.getClass().getMethod("getScheduler");
            Object scheduler = getter.invoke(player);
            Optional<Method> execute = findMethod(scheduler.getClass(), "execute", 4);
            if (execute.isPresent()) {
                execute.get().invoke(scheduler, plugin, wrap(task), null, 1L);
                return true;
            }
            Optional<Method> run = findMethod(scheduler.getClass(), "run", 3);
            if (run.isPresent()) {
                run.get().invoke(scheduler, plugin, (java.util.function.Consumer<Object>) ignored -> wrap(task).run(), null);
                return true;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to dispatch Folia entity task, falling back to Bukkit scheduler.", ex);
        }
        return false;
    }

    private boolean tryRegionScheduler(Location location, Runnable task) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        try {
            Method getter = Bukkit.class.getMethod("getRegionScheduler");
            Object scheduler = getter.invoke(null);
            Optional<Method> execute = findMethod(scheduler.getClass(), "execute", 5);
            if (execute.isPresent()) {
                execute.get().invoke(scheduler, plugin, world, location.getBlockX() >> 4, location.getBlockZ() >> 4, wrap(task));
                return true;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to dispatch Folia region task, falling back to Bukkit scheduler.", ex);
        }
        return false;
    }

    private Optional<Method> findMethod(Class<?> type, String name, int parameterCount) {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name))
                .filter(method -> method.getParameterCount() == parameterCount)
                .findFirst();
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Scheduled task failed.", throwable);
            }
        };
    }

    private long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * 50L;
    }

    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
    }
}
