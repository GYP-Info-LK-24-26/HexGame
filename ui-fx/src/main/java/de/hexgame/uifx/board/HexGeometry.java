package de.hexgame.uifx.board;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;
import lombok.Getter;

/**
 * Hex coordinate math: converts between screen pixels and board positions.
 * Uses pointy-top hexagons (vertical left/right edges) laid out in an offset grid.
 */
public class HexGeometry {
    @Getter
    private final double hexSize;
    private final double boardLeftX;
    private final double boardTopY;
    private static final int SIZE = GameState.BOARD_SIZE;
    private static final double SQRT3 = Math.sqrt(3);

    public HexGeometry(double canvasWidth, double canvasHeight, double topMargin) {
        double margin = 40;
        double availableWidth = canvasWidth - 2 * margin;
        double availableHeight = canvasHeight - topMargin - margin;

        // Pointy-top hex: width = sqrt(3) * size, height = 2 * size
        // Grid with row offset:
        //   Total width = sqrt(3) * size * SIZE + sqrt(3)/2 * size * (SIZE - 1)
        //   Total height = 1.5 * size * (SIZE - 1) + 2 * size
        double sizeByWidth = availableWidth / (SQRT3 * SIZE + SQRT3 / 2 * (SIZE - 1));
        double sizeByHeight = availableHeight / (1.5 * (SIZE - 1) + 2);
        hexSize = Math.min(sizeByWidth, sizeByHeight);

        double totalWidth = SQRT3 * hexSize * SIZE + SQRT3 / 2 * hexSize * (SIZE - 1);
        double totalHeight = 1.5 * hexSize * (SIZE - 1) + 2 * hexSize;
        boardLeftX = (canvasWidth - totalWidth) / 2;
        boardTopY = topMargin + (availableHeight - totalHeight) / 2;
    }

    /** Center X of a hexagon at (row, col). Rows shift right by half a hex width. */
    public double centerX(int row, int col) {
        return boardLeftX + col * SQRT3 * hexSize + row * SQRT3 / 2 * hexSize + SQRT3 / 2 * hexSize;
    }

    /** Center Y of a hexagon at (row, col). */
    public double centerY(int row, int col) {
        return boardTopY + row * 1.5 * hexSize + hexSize;
    }

    /** Returns the 6 corner X-coordinates of a pointy-top hexagon centered at (cx, cy). */
    public double[] hexCornerXs(double cx, double cy) {
        double[] xs = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i + 30);
            xs[i] = cx + hexSize * Math.cos(angle);
        }
        return xs;
    }

    /** Returns the 6 corner Y-coordinates of a pointy-top hexagon centered at (cx, cy). */
    public double[] hexCornerYs(double cx, double cy) {
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i + 30);
            ys[i] = cy + hexSize * Math.sin(angle);
        }
        return ys;
    }

    /** Convert screen coordinates to board Position. Returns Position(-1,-1) if outside the board. */
    public Position screenToBoard(double sx, double sy) {
        // Approximate: find closest hex center
        double minDist = Double.MAX_VALUE;
        int bestRow = -1, bestCol = -1;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                double cx = centerX(r, c);
                double cy = centerY(r, c);
                double dist = (sx - cx) * (sx - cx) + (sy - cy) * (sy - cy);
                if (dist < minDist) {
                    minDist = dist;
                    bestRow = r;
                    bestCol = c;
                }
            }
        }
        // Check if click is within hex bounds (inner radius)
        double innerRadius = Math.sqrt(3) / 2 * hexSize;
        if (Math.sqrt(minDist) > innerRadius) {
            return new Position(-1, -1);
        }
        return new Position(bestRow, bestCol);
    }
}
