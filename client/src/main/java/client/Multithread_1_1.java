package client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Multithread_1_1 {
    private static String s = "A";

    public static void main(String[] args) {
        final Object mon = new Object();
        new Thread(() -> {
            synchronized (mon) {
                for (int i = 0; i < 3; i++) {
                    while (!s.equals("A")) {
                        try {
                            mon.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("A");
                    s = "B";
                    mon.notifyAll();
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (mon) {
                for (int i = 0; i < 3; i++) {
                    while (!s.equals("B")) {
                        try {
                            mon.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("B");
                    s = "C";
                    mon.notifyAll();
                }
            }
        }).start();
        new Thread(() -> {
            synchronized (mon) {
                for (int i = 0; i < 3; i++) {
                    while (!s.equals("C")) {
                        try {
                            mon.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("C");
                    s = "A";
                    mon.notifyAll();
                }
            }
        }).start();
    }
}
