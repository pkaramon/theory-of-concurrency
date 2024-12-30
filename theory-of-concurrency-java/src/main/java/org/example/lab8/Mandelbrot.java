package org.example.lab8;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

class Worker implements Runnable {
    private final double ZOOM = 150;

    private final int[] row;
    private final int row_num;
    private final int max_iter;

    public Worker(int[] row, int row_num, int max_iter) {
        this.row = row;
        this.row_num = row_num;
        this.max_iter = max_iter;
    }

    @Override
    public void run() {
        double zx, zy, cX, cY, tmp;

        for (int col = 0; col < row.length; col++) {
            zx = zy = 0;
            cX = (col - 400) / ZOOM;
            cY = (row_num - 300) / ZOOM;
            int iter = max_iter;
            while (zx * zx + zy * zy < 4 && iter > 0) {
                tmp = zx * zx - zy * zy + cX;
                zy = 2.0 * zx * zy + cY;
                zx = tmp;
                iter--;
            }
            row[col] = iter | (iter << 8);
        }
    }
}


public class Mandelbrot {
private record ExecutorWithDesc(ExecutorService executor, String desc) {
}
public static void main(String[] args) {

    int max_iter = 200;
    int height = 600;
    int width = 800;
    int[][] image = run(Executors.newSingleThreadExecutor(), max_iter, height, width);
    MandelbrotImage mandelbrotImage = new MandelbrotImage();
    mandelbrotImage.display(image);

//        var executors = List.of(
//                new ExecutorWithDesc(Executors.newSingleThreadExecutor(),
//                             "Single thread"),
//                new ExecutorWithDesc(Executors.newFixedThreadPool(1),
//                             "Fixed thread pool (1 threads)"),
//                new ExecutorWithDesc(Executors.newFixedThreadPool(2),
//                             "Fixed thread pool (2 threads)"),
//                new ExecutorWithDesc(Executors.newFixedThreadPool(4),
//                             "Fixed thread pool (4 threads)"),
//                new ExecutorWithDesc(Executors.newFixedThreadPool(8),
//                             "Fixed thread pool (8 threads)"),
//                new ExecutorWithDesc(Executors.newFixedThreadPool(16),
//                             "Fixed thread pool (16 threads)"),
//                new ExecutorWithDesc(Executors.newFixedThreadPool(32),
//                             "Fixed thread pool (32 threads)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(),
//                             "Work stealing pool (parallelism = #CPU)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(1),
//                             "Work stealing pool (parallelism = 1)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(2),
//                        "Work stealing pool (parallelism = 2)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(4),
//                             "Work stealing pool (parallelism = 4)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(8),
//                             "Work stealing pool (parallelism = 8)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(16),
//                             "Work stealing pool (parallelism = 16)"),
//                new ExecutorWithDesc(Executors.newWorkStealingPool(32),
//                             "Work stealing pool (parallelism = 32)"),
//                new ExecutorWithDesc(Executors.newCachedThreadPool(),
//                             "Cached thread pool")
//        );
//
//        System.out.println("ExecutorName,Time");
//        for (var executor : executors) {
//            var start = System.nanoTime();
//            int[][] image = run(executor.executor, 200_000, 600, 800);
//            var end = System.nanoTime();
//
//            var timeMs = (end - start) / 1_000_000;
//            System.out.println(executor.desc + "," + timeMs);
//        }

}

private static int[][] run(ExecutorService executor, int max_iter, int height, int width) {
    int[][] image = new int[height][width];
    var futures = IntStream.range(0, height)
            .mapToObj(row -> new Worker(image[row], row,  max_iter))
            .map(executor::submit)
            .toList();

    executor.shutdown();
    for (var future : futures) {
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get future", e);
        }
    }

    return image;
}

}

class MandelbrotImage extends JFrame {
    private BufferedImage I;

    public MandelbrotImage() {
        super("MandelbrotImage Set");
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }


    public void display(int[][] image) {
        setBounds(100, 100, image[0].length, image.length);
        I = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < getHeight(); row++) {
            for (int col = 0; col < getWidth(); col++) {
                I.setRGB(col, row, image[row][col]);
            }
        }
        setVisible(true);
    }


    @Override
    public void paint(Graphics g) {
        g.drawImage(I, 0, 0, this);
    }
}

