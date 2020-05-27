package org.aksw.data_profiler.cli;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.aksw.jena_sparql_api.rx.query_flow.RxUtils;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TestPublishConnect {


    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Stopwatch sw = Stopwatch.createStarted();

        Stopwatch rootSw = Stopwatch.createStarted();
        ConnectableFlowable<Integer> root = Flowable.
                fromStream(IntStream.range(0, 100).mapToObj(x -> x))
                .doOnComplete(() -> System.err.println("Root elapsed time: " + rootSw.elapsed(TimeUnit.MILLISECONDS) * 0.001))
                .publish();

        int capacity = 10;



        List<Flowable<String>> tasks = Arrays.asList(
            root
//				.flatMap(x -> Flowable.just(x).observeOn(Schedulers.computation()))
                .compose(RxUtils.queuedObserveOn(Schedulers.computation(), capacity))
//                .observeOn(Schedulers.computation())
                .map(x -> {
                    // Thread.sleep(80);
                    return "a" + x;
                })
//				.subscribe(x -> System.out.println(x));
                ,
            root
//				.flatMap(x -> Flowable.just(x).observeOn(Schedulers.computation()))
//                .observeOn(Schedulers.computation())
                .compose(RxUtils.queuedObserveOn(Schedulers.computation(), capacity))
                .map(x -> {
                    // Thread.sleep(90);
                    return "b" + x;
                })
                ,
            root
//				.flatMap(x -> Flowable.just(x).observeOn(Schedulers.computation()))
//                .observeOn(Schedulers.computation())
                .compose(RxUtils.queuedObserveOn(Schedulers.computation(), capacity))
                .map(x -> {
                    Thread.sleep(100);
                    return "c" + x;
                })
        );

        Flowable<String> task = Flowable.fromIterable(tasks)
    //		.parallel()
            .flatMap(x -> x
//					.subscribeOn(Schedulers.computation())
//					.observeOn(Schedulers.computation())
            )
    //		.sequential()
            ;

        CompletableFuture<?> future = new CompletableFuture<>();
        task.subscribe(x -> {
             System.out.println("Saw " + x + " on thread " + Thread.currentThread());
        }, e -> {}, () -> future.complete(null));


//		.subscribe(x -> System.out.println(x));


        root.connect();

        future.get();

        System.err.println("Elapsed time: " + sw.elapsed(TimeUnit.MILLISECONDS) * 0.001);
    }
}
