package io.playground.scraper.model.response.frame;

import java.util.List;

public record FrameProp(Frame frame, List<FrameProp> childFrames) {
}
