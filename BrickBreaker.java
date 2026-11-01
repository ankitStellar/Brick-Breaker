import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Brick Breaker - a classic arcade game in plain Java Swing.
 *
 * Controls:
 *   Left/Right arrows or A/D, or the mouse  - move paddle
 *   SPACE or mouse click                    - launch ball / restart after game over
 *   P                                       - pause / resume
 *
 * Compile & run:  javac BrickBreaker.java && java BrickBreaker
 */
public class BrickBreaker extends JPanel implements ActionListener, KeyListener {

    // ---- constants -------------------------------------------------------
    static final int W = 800, H = 600;
    static final int PW = 110, PH = 14, PY = H - 50;      // paddle
    static final int BR = 8;                               // ball radius
    static final int COLS = 10, GAP = 4, BRICK_H = 22, TOP = 70, SIDE = 30;
    static final int START_LIVES = 3;
    static final Color[] ROW_COLORS = {
        new Color(0xEF5350), new Color(0xFF7043), new Color(0xFFCA28),
        new Color(0x9CCC65), new Color(0x26C6DA), new Color(0x5C6BC0),
        new Color(0xAB47BC), new Color(0xEC407A), new Color(0x8D6E63)
    };

    enum State { READY, PLAYING, PAUSED, GAME_OVER }

    static class Brick {
        final Rectangle rect;
        final Color color;
        final int points;
        int hits;
        Brick(Rectangle rect, Color color, int points, int hits) {
            this.rect = rect; this.color = color; this.points = points; this.hits = hits;
        }
    }

    // ---- game state ------------------------------------------------------
    private final List<Brick> bricks = new ArrayList<>();
    private final Random rng = new Random();
    private State state = State.READY;
    private boolean left, right;
    private int paddleX = (W - PW) / 2;
    private double bx, by, vx, vy, speed;
    private int score, highScore, lives, level;

    public BrickBreaker() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { movePaddleTo(e.getX()); }
            @Override public void mouseDragged(MouseEvent e) { movePaddleTo(e.getX()); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { primaryAction(); }
        });
        newGame();
        new Timer(16, this).start();   // ~60 FPS
    }

    // ---- setup -----------------------------------------------------------
    private void newGame() {
        score = 0; lives = START_LIVES; level = 1;
        buildBricks();
        resetBall();
    }

    private void buildBricks() {
        bricks.clear();
        int rows = Math.min(5 + level, ROW_COLORS.length);
        int bw = (W - 2 * SIDE - GAP * (COLS - 1)) / COLS;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = SIDE + c * (bw + GAP);
                int y = TOP + r * (BRICK_H + GAP);
                int hits = (level >= 3 && r < 2) ? 2 : 1;       // tougher top rows from level 3
                int points = (rows - r) * 10;
                bricks.add(new Brick(new Rectangle(x, y, bw, BRICK_H), ROW_COLORS[r], points, hits));
            }
        }
    }

    private void resetBall() {
        state = State.READY;
        speed = Math.min(5.0 + (level - 1) * 0.6, 9.0);
        paddleX = (W - PW) / 2;
        stickBallToPaddle();
    }

    private void stickBallToPaddle() {
        bx = paddleX + PW / 2.0;
        by = PY - BR;
    }

    private void launch() {
        double angle = Math.toRadians(rng.nextInt(61) - 30);   // -30..+30 degrees from vertical
        vx = speed * Math.sin(angle);
        vy = -speed * Math.cos(angle);
        state = State.PLAYING;
    }

    // ---- input -----------------------------------------------------------
    private void movePaddleTo(int mouseX) {
        if (state == State.PAUSED || state == State.GAME_OVER) return;
        paddleX = clamp(mouseX - PW / 2, 0, W - PW);
    }

    private void primaryAction() {
        if (state == State.READY) launch();
        else if (state == State.GAME_OVER) newGame();
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> primaryAction();
            case KeyEvent.VK_P -> {
                if (state == State.PLAYING) state = State.PAUSED;
                else if (state == State.PAUSED) state = State.PLAYING;
            }
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = false;
        }
    }
    @Override public void keyTyped(KeyEvent e) {}

    // ---- game loop -------------------------------------------------------
    @Override public void actionPerformed(ActionEvent e) {
        update();
        repaint();
        Toolkit.getDefaultToolkit().sync();
    }

    private void update() {
        if (state == State.PAUSED || state == State.GAME_OVER) return;

        if (left)  paddleX = clamp(paddleX - 9, 0, W - PW);
        if (right) paddleX = clamp(paddleX + 9, 0, W - PW);

        if (state == State.READY) { stickBallToPaddle(); return; }

        bx += vx;
        by += vy;

        // walls
        if (bx - BR < 0)     { bx = BR;     vx =  Math.abs(vx); }
        if (bx + BR > W)     { bx = W - BR; vx = -Math.abs(vx); }
        if (by - BR < 0)     { by = BR;     vy =  Math.abs(vy); }

        // paddle: bounce angle depends on where the ball lands
        if (vy > 0 && by + BR >= PY && by + BR <= PY + PH + vy
                && bx >= paddleX - BR && bx <= paddleX + PW + BR) {
            double rel = clamp((bx - (paddleX + PW / 2.0)) / (PW / 2.0), -1.0, 1.0);
            double angle = rel * Math.toRadians(65) + Math.toRadians(rng.nextDouble() * 4 - 2); // tiny jitter avoids endless vertical loops
            vx = speed * Math.sin(angle);
            vy = -speed * Math.cos(angle);
            by = PY - BR;
        }

        // bricks (at most one per frame)
        for (Iterator<Brick> it = bricks.iterator(); it.hasNext(); ) {
            Brick b = it.next();
            Rectangle r = b.rect;
            double cx = clamp(bx, r.x, r.x + r.width);
            double cy = clamp(by, r.y, r.y + r.height);
            double dx = bx - cx, dy = by - cy;
            if (dx * dx + dy * dy > BR * BR) continue;

            if (dx == 0 && dy == 0)               vy = -vy;
            else if (Math.abs(dx) > Math.abs(dy)) vx = dx > 0 ? Math.abs(vx) : -Math.abs(vx);
            else                                  vy = dy > 0 ? Math.abs(vy) : -Math.abs(vy);

            if (--b.hits <= 0) { score += b.points; it.remove(); }
            else               { score += 2; }
            highScore = Math.max(highScore, score);

            // ball gets slightly faster as the board clears
            speed = Math.min(speed + 0.04, 11.0);
            double len = Math.hypot(vx, vy);
            vx = vx / len * speed;
            vy = vy / len * speed;
            break;
        }

        // level cleared
        if (bricks.isEmpty()) {
            level++;
            score += 100;
            highScore = Math.max(highScore, score);
            buildBricks();
            resetBall();
            return;
        }

        // ball lost
        if (by - BR > H) {
            if (--lives <= 0) state = State.GAME_OVER;
            else resetBall();
        }
    }

    // ---- rendering -------------------------------------------------------
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x0D1B2A), 0, H, new Color(0x1B263B)));
        g.fillRect(0, 0, W, H);

        for (Brick b : bricks) drawBrick(g, b);

        // paddle
        g.setPaint(new GradientPaint(0, PY, new Color(0xE0E0E0), 0, PY + PH, new Color(0x757575)));
        g.fill(new RoundRectangle2D.Double(paddleX, PY, PW, PH, 12, 12));

        // ball
        g.setColor(Color.WHITE);
        g.fillOval((int) (bx - BR), (int) (by - BR), BR * 2, BR * 2);

        drawHud(g);

        switch (state) {
            case READY     -> overlay(g, "LEVEL " + level, "Press SPACE or click to launch");
            case PAUSED    -> overlay(g, "PAUSED", "Press P to resume");
            case GAME_OVER -> overlay(g, "GAME OVER", "Score " + score + "  -  press SPACE to play again");
            default -> {}
        }
    }

    private void drawBrick(Graphics2D g, Brick b) {
        Rectangle r = b.rect;
        Color c = b.hits > 1 ? b.color.darker() : b.color;
        g.setColor(c);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height / 3, 4, 4);
        if (b.hits > 1) {                                  // armored brick outline
            g.setColor(new Color(255, 255, 255, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 6, 6);
        }
    }

    private void drawHud(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 20, 30);
        g.drawString("Level " + level, W / 2 - 30, 30);
        g.drawString("Best: " + highScore, 20, 52);
        for (int i = 0; i < lives; i++) {
            g.setColor(new Color(0xEF5350));
            g.fillOval(W - 40 - i * 26, 16, 16, 16);
        }
    }

    private void overlay(Graphics2D g, String title, String subtitle) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, H / 2 - 70, W, 140);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        centered(g, title, H / 2 - 5);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        centered(g, subtitle, H / 2 + 35);
    }

    private void centered(Graphics2D g, String s, int y) {
        g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    // ---- utils -----------------------------------------------------------
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Brick Breaker");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(new BrickBreaker());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
