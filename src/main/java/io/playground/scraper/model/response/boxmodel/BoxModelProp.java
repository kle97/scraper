package io.playground.scraper.model.response.boxmodel;

import java.util.List;

public record BoxModelProp(
        List<Double> content,
        Object padding,
        Object border,
        Object margin,
        double width,
        double height
) {
    public Point getTopLeft() {
        if (content != null && !content.isEmpty()) {
            return new Point(content.get(0), content.get(1));
        } else {
            return new Point(0, 0);
        }
    }

    public Point getBottomLeft() {
        if (content != null && !content.isEmpty()) {
            return new Point(content.get(6), content.get(7));
        } else {
            return new Point(0, 0);
        }
    }

    public Point getTopRight() {
        if (content != null && !content.isEmpty()) {
            return new Point(content.get(2), content.get(3));
        } else {
            return new Point(0, 0);
        }
    }

    public Point getBottomRight() {
        if (content != null && !content.isEmpty()) {
            return new Point(content.get(4), content.get(5));
        } else {
            return new Point(0, 0);
        }
    }

    public Point getCenter() {
        Point topLeft = getTopLeft();
        double centerX = topLeft.x() + width / 2;
        double centerY = topLeft.y() + height / 2;
        return new Point(centerX, centerY);
    }
}
