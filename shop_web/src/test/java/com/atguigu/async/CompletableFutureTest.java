package com.atguigu.async;


import java.util.concurrent.CompletableFuture;

public class CompletableFutureTest {

    public static void main(String[] args) {
        supplyAsyncThenAsync();
        System.out.println(Thread.currentThread().getName() + " helloFuture");

    }

    public static void runAsync() {
        CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {

            System.out.println(Thread.currentThread().getName() + " 执行runAsync");
        });
    }

    public static void supplyAsync() {
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() -> {
//            System.out.println(1 / 0);
            System.out.println(Thread.currentThread().getName() + " 执行supplyAsync");

            return " hello";
        });

        supplyAsync.whenComplete((acceptVal, throwable) -> {

            if (throwable == null) {
                System.out.println(Thread.currentThread().getName() + acceptVal);
            } else {
                System.out.println(Thread.currentThread().getName() + throwable);
            }
        }).exceptionally(throwable -> {
            System.out.println(throwable);
            return "error";
        });
    }

    public static void supplyAsyncThen() {
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName() + " 执行supplyAsync");

            return " hello";
        });
        supplyAsync.thenAccept(s -> {
            System.out.println(Thread.currentThread().getName() + " Accept " + s);
        });

        CompletableFuture<String> future = supplyAsync.thenApply(s -> {
            System.out.println(Thread.currentThread().getName() + " Apply " + s);
            return "apply";
        });
    }


    public static void supplyAsyncThenAsync() {
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName() + " 执行supplyAsync");

            return " hello";
        });
        supplyAsync.thenAccept(s -> {
            System.out.println(Thread.currentThread().getName() + " Accept " + s);
        });

        supplyAsync.thenAcceptAsync(s -> {
            System.out.println(Thread.currentThread().getName() + " AcceptAsync " + s);
        });
    }
}
