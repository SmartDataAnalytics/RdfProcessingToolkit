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

        Flowable<Integer> root = Flowable.
            fromIterable(() -> IntStream.range(0, 100).mapToObj(x -> x).iterator())
            .doOnComplete(() -> System.err.println("Root elapsed time: " + rootSw.elapsed(TimeUnit.MILLISECONDS) * 0.001))
            .compose(RxUtils.counter("root", 1));


        ConnectableFlowable<Integer> rootPub = root
//                .share()
                .publish();

        int capacity = 3;




        Flowable<String> aPublisher = rootPub
//			.flatMap(x -> Flowable.just(x).observeOn(Schedulers.computation()))
            .compose(RxUtils.queuedObserveOn(Schedulers.newThread(), capacity))
//            .observeOn(Schedulers.computation())
//            .observeOn(Schedulers.computation(), false, 1)
            .map(x -> {
                // Thread.sleep(80);
                return "a" + x;
            })
            .compose(RxUtils.counter("aPublisher", 1))
            .share()
            ;

        List<Flowable<String>> tasks = Arrays.asList(
//				.subscribe(x -> System.out.println(x));
                aPublisher
                    .map(x -> {
//                        Thread.sleep(100);
                        return "pub1:" + x;
                    })
                    .compose(RxUtils.queuedObserveOn(Schedulers.newThread(), capacity))
//                    .observeOn(Schedulers.computation())
                    ,
                aPublisher
                    .map(x -> "pub2:" + x)
                    .compose(RxUtils.queuedObserveOn(Schedulers.computation(), capacity))
                    .observeOn(Schedulers.computation(), false, 1)
                ,
            rootPub
//				.flatMap(x -> Flowable.just(x).observeOn(Schedulers.computation()))
//                .observeOn(Schedulers.computation())
                .compose(RxUtils.queuedObserveOn(Schedulers.computation(), 3))
//                .observeOn(Schedulers.computation(), false, 1)
                .map(x -> {
                    Thread.sleep(100);
                    return "b" + x;
                })
                ,
            rootPub
//				.flatMap(x -> Flowable.just(x).observeOn(Schedulers.computation()))
                .observeOn(Schedulers.computation())
//                .compose(RxUtils.queuedObserveOn(Schedulers.computation(), capacity))
//                .observeOn(Schedulers.computation(), false, 1)
                .map(x -> {
                    //Thread.sleep(100);
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


        rootPub.connect();

        future.get();

        System.err.println("Elapsed time: " + sw.elapsed(TimeUnit.MILLISECONDS) * 0.001);
    }
}
