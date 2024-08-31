package io.playground.scraper.model.response.boxmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record Point(double x, double y) {

    private static final Random random = new Random();

    public Point getRandom(int bound) {
        double rand = random.nextDouble(bound + bound) - bound;
        return new Point(Math.max(0, x + rand), Math.max(0, y + rand));
    }

    public List<Point> getSpreadPoints(int numberOfSpread) {
        List<Point> points = new ArrayList<>();
        double currentY = 0;
        points.add(new Point(x, currentY));
        for (int i = 1; i < numberOfSpread - 1; i++) {
            double rand = random.nextDouble(50 + 4) - 4;
            if (currentY + rand > y) {
                break;
            } else {
                currentY = currentY + rand;
                points.add(new Point(x, currentY));
            }
        }

        points.add(this);
        return points;
    }

    public int getIntegerX() {
        return (int) x;
    }

    public int getIntegerY() {
        return (int) y;
    }
}
