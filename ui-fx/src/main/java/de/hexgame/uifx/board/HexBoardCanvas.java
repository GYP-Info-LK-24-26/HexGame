package de.hexgame.uifx.board;

import de.hexgame.algorithm.mcts.ICEResult;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static de.hexgame.logic.GameState.NO_PIECE;
import static de.hexgame.logic.GameState.RED;

public class HexBoardCanvas extends Canvas {
    private static final int SIZE = GameState.BOARD_SIZE;
    @Getter
    private HexGeometry geometry;
    @Setter
    private GameState gameState;
    @Setter
    private String currentPlayerLabel;
    @Setter
    private Position hoverPosition;
    @Setter
    private boolean inputEnabled;
    @Setter
    private String winnerLabel;
    @Setter
    private ICEResult iceResult;

    private static final double TOP_MARGIN = 30;

    public HexBoardCanvas(double width, double height) {
        super(width, height);
        geometry = new HexGeometry(width, height, TOP_MARGIN);
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.gray(0.15));
        gc.fillRect(0, 0, getWidth(), getHeight());

        drawGoals(gc);
        drawHexagons(gc);
        drawCurrentPlayer(gc);
    }

    // --- Goal band rendering ---

    /**
     * Pointy-top corner indices (angle = 60*i + 30, y-down screen coords):
     *   0 = lower-right (30°)
     *   1 = bottom      (90°)
     *   2 = lower-left  (150°)
     *   3 = upper-left  (210°)
     *   4 = top          (270°)
     *   5 = upper-right (330°)
     * <p>
     * Board boundaries and shared corners between adjacent hexes:
     *   Left  (Red,  col=0):       edges 2→3 and 1→2;  c1(r,0) = c3(r+1,0)
     *   Right (Red,  col=SIZE-1):  edges 4→5 and 5→0;  c0(r,S-1) = c4(r+1,S-1)
     *   Top   (Blue, row=0):       edges 3→4 and 4→5;  c5(0,c) = c3(0,c+1)
     *   Bottom(Blue, row=SIZE-1):  edges 1→2 and 0→1;  c0(S-1,c) = c2(S-1,c+1)
     */
    private void drawGoals(GraphicsContext gc) {
        double bandWidth = geometry.getHexSize() * 0.35;

        List<double[]> leftPath = buildLeftPath();
        List<double[]> rightPath = buildRightPath();
        List<double[]> topPath = buildTopPath();
        List<double[]> bottomPath = buildBottomPath();

        drawFilledBand(gc, leftPath, Color.CRIMSON, bandWidth);
        drawFilledBand(gc, rightPath, Color.CRIMSON, bandWidth);
        drawFilledBand(gc, topPath, Color.DODGERBLUE, bandWidth);
        drawFilledBand(gc, bottomPath, Color.DODGERBLUE, bandWidth);

        drawCornerWedges(gc, leftPath, rightPath, topPath, bottomPath, bandWidth);
    }

    /** Left boundary: c3(0,0), c2(0,0), c1(0,0)=c3(1,0), ..., c2(S-1,0). Stops before c1(S-1,0) to avoid overlap with bottom. */
    private List<double[]> buildLeftPath() {
        List<double[]> path = new ArrayList<>();
        path.add(corner(0, 0, 3));
        for (int r = 0; r < SIZE - 1; r++) {
            path.add(corner(r, 0, 2));
            path.add(corner(r, 0, 1));
        }
        path.add(corner(SIZE - 1, 0, 2));
        return path;
    }

    /** Right boundary: c5(0,S-1), c0(0,S-1)=c4(1,S-1), ..., c0(S-1,S-1). Starts at c5 to avoid overlap with top. */
    private List<double[]> buildRightPath() {
        List<double[]> path = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            path.add(corner(r, SIZE - 1, 5));
            path.add(corner(r, SIZE - 1, 0));
        }
        return path;
    }

    /** Top boundary: c3(0,0), c4(0,0), c5(0,0)=c3(0,1), ..., c4(0,S-1), c5(0,S-1) */
    private List<double[]> buildTopPath() {
        List<double[]> path = new ArrayList<>();
        path.add(corner(0, 0, 3));
        for (int c = 0; c < SIZE; c++) {
            path.add(corner(0, c, 4));
            path.add(corner(0, c, 5));
        }
        return path;
    }

    /** Bottom boundary: c1(S-1,0), c0(S-1,0)=c2(S-1,1), ..., c1(S-1,S-1), c0(S-1,S-1). Starts at c1 to avoid overlap with left. */
    private List<double[]> buildBottomPath() {
        List<double[]> path = new ArrayList<>();
        for (int c = 0; c < SIZE; c++) {
            path.add(corner(SIZE - 1, c, 1));
            path.add(corner(SIZE - 1, c, 0));
        }
        return path;
    }

    private double[] corner(int row, int col, int cornerIdx) {
        double cx = geometry.centerX(row, col);
        double cy = geometry.centerY(row, col);
        double angle = Math.toRadians(60 * cornerIdx + 30);
        return new double[]{
                cx + geometry.getHexSize() * Math.cos(angle),
                cy + geometry.getHexSize() * Math.sin(angle)
        };
    }

    private void drawFilledBand(GraphicsContext gc, List<double[]> path, Color color, double bandWidth) {
        int n = path.size();
        double bcx = geometry.centerX(SIZE / 2, SIZE / 2);
        double bcy = geometry.centerY(SIZE / 2, SIZE / 2);

        double[] outerXs = new double[n];
        double[] outerYs = new double[n];

        for (int i = 0; i < n; i++) {
            double nx = 0, ny = 0;
            if (i > 0) {
                double dx = path.get(i)[0] - path.get(i - 1)[0];
                double dy = path.get(i)[1] - path.get(i - 1)[1];
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len > 0) { nx += -dy / len; ny += dx / len; }
            }
            if (i < n - 1) {
                double dx = path.get(i + 1)[0] - path.get(i)[0];
                double dy = path.get(i + 1)[1] - path.get(i)[1];
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len > 0) { nx += -dy / len; ny += dx / len; }
            }
            double len = Math.sqrt(nx * nx + ny * ny);
            if (len > 0) { nx /= len; ny /= len; }

            // Ensure normal points away from the board center
            double toCenter = (bcx - path.get(i)[0]) * nx + (bcy - path.get(i)[1]) * ny;
            if (toCenter > 0) { nx = -nx; ny = -ny; }

            outerXs[i] = path.get(i)[0] + nx * bandWidth;
            outerYs[i] = path.get(i)[1] + ny * bandWidth;
        }

        // Polygon: inner path forward, then outer path reversed
        double[] xs = new double[2 * n];
        double[] ys = new double[2 * n];
        for (int i = 0; i < n; i++) {
            xs[i] = path.get(i)[0];
            ys[i] = path.get(i)[1];
            xs[2 * n - 1 - i] = outerXs[i];
            ys[2 * n - 1 - i] = outerYs[i];
        }

        gc.setFill(color.deriveColor(0, 1, 1, 0.55));
        gc.fillPolygon(xs, ys, 2 * n);

        gc.setStroke(color);
        gc.setLineWidth(2.5);
        gc.beginPath();
        gc.moveTo(path.getFirst()[0], path.getFirst()[1]);
        for (int i = 1; i < n; i++) {
            gc.lineTo(path.get(i)[0], path.get(i)[1]);
        }
        gc.stroke();
    }

    /**
     * Fill the 4 corner wedges where Red and Blue bands meet.
     * At top-left and bottom-right the paths share the same endpoint.
     * At top-right and bottom-left the paths have adjacent endpoints
     * (the shared edge was removed from one path to prevent overlap).
     */
    private void drawCornerWedges(GraphicsContext gc,
                                  List<double[]> leftPath, List<double[]> rightPath,
                                  List<double[]> topPath, List<double[]> bottomPath,
                                  double bandWidth) {
        double bcx = geometry.centerX(SIZE / 2, SIZE / 2);
        double bcy = geometry.centerY(SIZE / 2, SIZE / 2);

        // Top-left: left start c3(0,0) == top start c3(0,0) — shared point
        fillWedge(gc, leftPath, 0, topPath, 0, bandWidth, bcx, bcy);
        // Top-right: top ends at c5(0,S-1), right starts at c5(0,S-1) — shared point
        fillWedge(gc, topPath, topPath.size() - 1, rightPath, 0, bandWidth, bcx, bcy);
        // Bottom-left: left ends at c2(S-1,0), bottom starts at c1(S-1,0) — adjacent points
        fillWedge(gc, leftPath, leftPath.size() - 1, bottomPath, 0, bandWidth, bcx, bcy);
        // Bottom-right: right ends at c0(S-1,S-1) == bottom ends at c0(S-1,S-1) — shared point
        fillWedge(gc, rightPath, rightPath.size() - 1, bottomPath, bottomPath.size() - 1, bandWidth, bcx, bcy);
    }

    private void fillWedge(GraphicsContext gc,
                           List<double[]> pathA, int idxA,
                           List<double[]> pathB, int idxB,
                           double bandWidth, double bcx, double bcy) {
        double[] pA = pathA.get(idxA);
        double[] pB = pathB.get(idxB);

        double[] outerA = offsetOutward(pathA, idxA, bandWidth, bcx, bcy);
        double[] outerB = offsetOutward(pathB, idxB, bandWidth, bcx, bcy);

        // Find the far outer corner where both offset lines would meet
        double[] outerCorner = new double[]{
                outerA[0] + (outerB[0] - pB[0]),
                outerA[1] + (outerB[1] - pB[1])
        };

        gc.setFill(Color.gray(0.15));
        boolean samePoint = Math.abs(pA[0] - pB[0]) < 0.5 && Math.abs(pA[1] - pB[1]) < 0.5;
        if (samePoint) {
            gc.fillPolygon(
                    new double[]{pA[0], outerA[0], outerCorner[0], outerB[0]},
                    new double[]{pA[1], outerA[1], outerCorner[1], outerB[1]}, 4);
        } else {
            gc.fillPolygon(
                    new double[]{pA[0], outerA[0], outerCorner[0], outerB[0], pB[0]},
                    new double[]{pA[1], outerA[1], outerCorner[1], outerB[1], pB[1]}, 5);
        }
    }

    private double[] offsetOutward(List<double[]> path, int i, double bandWidth, double bcx, double bcy) {
        int n = path.size();
        double nx = 0, ny = 0;
        if (i > 0) {
            double dx = path.get(i)[0] - path.get(i - 1)[0];
            double dy = path.get(i)[1] - path.get(i - 1)[1];
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 0) { nx += -dy / len; ny += dx / len; }
        }
        if (i < n - 1) {
            double dx = path.get(i + 1)[0] - path.get(i)[0];
            double dy = path.get(i + 1)[1] - path.get(i)[1];
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 0) { nx += -dy / len; ny += dx / len; }
        }
        double len = Math.sqrt(nx * nx + ny * ny);
        if (len > 0) { nx /= len; ny /= len; }
        double toCenter = (bcx - path.get(i)[0]) * nx + (bcy - path.get(i)[1]) * ny;
        if (toCenter > 0) { nx = -nx; ny = -ny; }
        return new double[]{path.get(i)[0] + nx * bandWidth, path.get(i)[1] + ny * bandWidth};
    }

    // --- Hexagon rendering ---

    private void drawHexagons(GraphicsContext gc) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                double cx = geometry.centerX(row, col);
                double cy = geometry.centerY(row, col);
                double[] xs = geometry.hexCornerXs(cx, cy);
                double[] ys = geometry.hexCornerYs(cx, cy);

                Position pos = new Position(row, col);
                int idx = row * SIZE + col;
                Color fill = Color.gray(0.9);
                if (gameState != null) {
                    int piece = gameState.getPiece(pos);
                    if (piece != NO_PIECE) {
                        fill = piece == RED ? Color.CRIMSON : Color.DODGERBLUE;
                    } else if (inputEnabled && hoverPosition != null
                            && hoverPosition.row() == row && hoverPosition.column() == col) {
                        int side = gameState.getSideToMove();
                        fill = (side == RED ? Color.CRIMSON : Color.DODGERBLUE).deriveColor(0, 0.5, 1, 0.4);
                    }
                }
                gc.setFill(fill);
                gc.fillPolygon(xs, ys, 6);

                // ICE overlay
                if (iceResult != null && gameState != null && gameState.getPiece(idx) == NO_PIECE) {
                    Color overlay = null;
                    if (iceResult.dead[idx]) {
                        overlay = Color.color(0, 0, 0, 0.55);
                    } else if (iceResult.capturedByRed[idx]) {
                        overlay = Color.CRIMSON.deriveColor(0, 1, 1, 0.45);
                    } else if (iceResult.capturedByBlue[idx]) {
                        overlay = Color.DODGERBLUE.deriveColor(0, 1, 1, 0.45);
                    }
                    if (overlay != null) {
                        gc.setFill(overlay);
                        gc.fillPolygon(xs, ys, 6);
                    }
                }

                gc.setStroke(Color.gray(0.3));
                gc.setLineWidth(1.5);
                gc.strokePolygon(xs, ys, 6);

                // ICE label
                if (iceResult != null && gameState != null && gameState.getPiece(idx) == NO_PIECE) {
                    String label = null;
                    Color labelColor = Color.WHITE;
                    if (iceResult.dead[idx]) {
                        label = "D";
                    } else if (iceResult.capturedByRed[idx]) {
                        label = "R";
                        labelColor = Color.WHITE;
                    } else if (iceResult.capturedByBlue[idx]) {
                        label = "B";
                        labelColor = Color.WHITE;
                    }
                    if (label != null) {
                        gc.setFont(Font.font(geometry.getHexSize() * 0.6));
                        gc.setFill(labelColor);
                        gc.fillText(label, cx - geometry.getHexSize() * 0.18, cy + geometry.getHexSize() * 0.2);
                    }
                }
            }
        }
    }

    private void drawCurrentPlayer(GraphicsContext gc) {
        gc.setFont(Font.font(14));
        if (winnerLabel != null) {
            gc.setFill(Color.WHITE);
            gc.fillText(winnerLabel, 10, 20);
        } else if (currentPlayerLabel != null) {
            gc.setFill(Color.WHITE);
            gc.fillText(currentPlayerLabel, 10, 20);
        }
    }
}
