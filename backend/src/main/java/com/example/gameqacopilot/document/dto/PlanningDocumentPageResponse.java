package com.example.gameqacopilot.document.dto;

import java.util.List;

public record PlanningDocumentPageResponse(
        int pageNumber, List<PageElement> elements, String imageUrl) {
    public record PageElement(String elementId, String elementType, String text, BoundingBox boundingBox) {}
    public record BoundingBox(double x, double y, double width, double height) {}
}
