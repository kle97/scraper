package io.playground.scraper.constant;

public enum Direction {
    UP, 
    DOWN, 
    LEFT, 
    RIGHT;
    
    public static Direction getOpposite(Direction direction) {
        return switch (direction) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }
}
