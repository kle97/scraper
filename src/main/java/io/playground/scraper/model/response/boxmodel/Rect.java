package io.playground.scraper.model.response.boxmodel;

public record Rect(
        int x,
        int y,
        int width,
        int height,
        int scrollLeft,
        int scrollTop,
        int clientLeft,
        int clientTop,
        int clientWidth,
        int clientHeight
) {
    
    public static Rect defaultRect() {
        return new Rect(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
                        Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
    
    public boolean isNotNull() {
        return !this.equals(defaultRect());
    }

    public Point getCenter() {
        double centerX = x + (double) width / 2;
        double centerY = y + (double) height / 2;
        return new Point(centerX, centerY);
    }
}
