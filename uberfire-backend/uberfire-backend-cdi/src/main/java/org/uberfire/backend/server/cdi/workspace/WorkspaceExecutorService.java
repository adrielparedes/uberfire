package org.uberfire.backend.server.cdi.workspace;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.rpc.SessionInfo;

public class WorkspaceExecutorService implements ExecutorService {

    private Logger logger = LoggerFactory.getLogger( WorkspaceExecutorService.class );

    @Inject
    private SessionInfo sessionInfo;

    @Resource
    private ManagedExecutorService wrapper;

    @Override
    public void shutdown() {
        wrapper.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return wrapper.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return wrapper.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return wrapper.isTerminated();
    }

    @Override
    public boolean awaitTermination( final long l,
                                     final TimeUnit timeUnit ) throws InterruptedException {
        return wrapper.awaitTermination( l, timeUnit );
    }

    @Override
    public <T> Future<T> submit( final Callable<T> callable ) {

        return wrapper.submit( this.generateCallable( callable ) );
    }

    @Override
    public <T> Future<T> submit( final Runnable runnable,
                                 final T t ) {
        return wrapper.submit( this.generateRunnable( runnable ), t );
    }

    @Override
    public Future<?> submit( final Runnable runnable ) {
        return wrapper.submit( this.generateRunnable( runnable ) );
    }

    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> collection ) throws InterruptedException {
        return wrapper.invokeAll( collection );
    }

    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> collection,
                                          final long l,
                                          final TimeUnit timeUnit ) throws InterruptedException {
        return wrapper.invokeAll( collection, l, timeUnit );
    }

    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> collection ) throws InterruptedException, ExecutionException {
        return wrapper.invokeAny( collection );
    }

    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> collection,
                            final long l,
                            final TimeUnit timeUnit ) throws InterruptedException, ExecutionException, TimeoutException {
        return wrapper.invokeAny( collection, l, timeUnit );
    }

    @Override
    public void execute( final Runnable runnable ) {
        wrapper.execute( this.generateRunnable( runnable ) );
    }

    private Runnable generateRunnable( final Runnable runnable ) {

        String workspace = sessionInfo.getIdentity().getIdentifier();
        return () -> {
            ThreadLocal<String> threadLocal = new ThreadLocal<>();
            threadLocal.set( workspace );
            runnable.run();
        };
    }

    private <T> Callable<T> generateCallable( final Callable<T> callable ) {

        String workspace = sessionInfo.getIdentity().getIdentifier();
        return () -> {
            ThreadLocal<String> threadLocal = new ThreadLocal<>();
            threadLocal.set( workspace );
            return callable.call();
        };
    }
}
