package com.example.snake;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

final class SnakePanel extends JPanel {
    private static final int CELL_SIZE = 24;
    private static final int COLS = 28;
    private static final int ROWS = 22;
    private static final int HEADER_HEIGHT = 48;
    private static final int TIMER_DELAY_MS = 105;

    private final Random random = new Random();
    private final Deque<Point> snake = new ArrayDeque<>();
    private final Timer timer = new Timer(TIMER_DELAY_MS, this::tick);

    private Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private Point food = new Point();
    private int score;
    private boolean running;
    private boolean gameOver;

    SnakePanel() {
        setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE + HEADER_HEIGHT));
        setBackground(new Color(19, 24, 33));
        setFocusable(true);
        setupKeyBindings();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                requestFocusInWindow();
            }
        });
        resetGame();
    }

    private void setupKeyBindings() {
        bind("UP", KeyEvent.VK_UP, () -> turn(Direction.UP));
        bind("W", KeyEvent.VK_W, () -> turn(Direction.UP));
        bind("DOWN", KeyEvent.VK_DOWN, () -> turn(Direction.DOWN));
        bind("S", KeyEvent.VK_S, () -> turn(Direction.DOWN));
        bind("LEFT", KeyEvent.VK_LEFT, () -> turn(Direction.LEFT));
        bind("A", KeyEvent.VK_A, () -> turn(Direction.LEFT));
        bind("RIGHT", KeyEvent.VK_RIGHT, () -> turn(Direction.RIGHT));
        bind("D", KeyEvent.VK_D, () -> turn(Direction.RIGHT));
        bind("SPACE", KeyEvent.VK_SPACE, this::toggleRunning);
        bind("R", KeyEvent.VK_R, this::resetGame);
    }

    private void bind(String name, int keyCode, Runnable runnable) {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        actionMap.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        });
    }

    private void resetGame() {
        snake.clear();
        int startX = COLS / 2;
        int startY = ROWS / 2;
        snake.addFirst(new Point(startX, startY));
        snake.addLast(new Point(startX - 1, startY));
        snake.addLast(new Point(startX - 2, startY));
        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        score = 0;
        running = false;
        gameOver = false;
        placeFood();
        timer.start();
        repaint();
    }

    private void toggleRunning() {
        if (gameOver) {
            resetGame();
            running = true;
            return;
        }
        running = !running;
        repaint();
    }

    private void turn(Direction requested) {
        if (!requested.isOpposite(direction)) {
            nextDirection = requested;
        }
    }

    private void tick(ActionEvent event) {
        if (!running || gameOver) {
            repaint();
            return;
        }

        direction = nextDirection;
        Point head = snake.peekFirst();
        Point next = new Point(head.x + direction.dx, head.y + direction.dy);

        if (hitsWall(next) || hitsSnake(next)) {
            running = false;
            gameOver = true;
            repaint();
            return;
        }

        snake.addFirst(next);
        if (next.equals(food)) {
            score += 10;
            placeFood();
        } else {
            snake.removeLast();
        }

        repaint();
    }

    private boolean hitsWall(Point point) {
        return point.x < 0 || point.x >= COLS || point.y < 0 || point.y >= ROWS;
    }

    private boolean hitsSnake(Point point) {
        return snake.stream().anyMatch(segment -> segment.equals(point));
    }

    private void placeFood() {
        Point candidate;
        do {
            candidate = new Point(random.nextInt(COLS), random.nextInt(ROWS));
        } while (snake.contains(candidate));
        food = candidate;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintHeader(g);
        paintBoard(g);
        paintSnake(g);
        paintFood(g);
        paintOverlay(g);

        g.dispose();
    }

    private void paintHeader(Graphics2D g) {
        g.setColor(new Color(13, 17, 23));
        g.fillRect(0, 0, getWidth(), HEADER_HEIGHT);
        g.setColor(new Color(229, 231, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("Snake", 18, 30);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        String text = "Score: " + score + "    Space: start/pause    R: restart";
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, getWidth() - metrics.stringWidth(text) - 18, 30);
    }

    private void paintBoard(Graphics2D g) {
        int yOffset = HEADER_HEIGHT;
        g.setColor(new Color(28, 35, 48));
        g.fillRect(0, yOffset, COLS * CELL_SIZE, ROWS * CELL_SIZE);

        g.setColor(new Color(39, 48, 64));
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x <= COLS; x++) {
            int px = x * CELL_SIZE;
            g.drawLine(px, yOffset, px, yOffset + ROWS * CELL_SIZE);
        }
        for (int y = 0; y <= ROWS; y++) {
            int py = yOffset + y * CELL_SIZE;
            g.drawLine(0, py, COLS * CELL_SIZE, py);
        }
    }

    private void paintSnake(Graphics2D g) {
        int index = 0;
        for (Point segment : snake) {
            boolean head = index == 0;
            g.setColor(head ? new Color(110, 231, 183) : new Color(52, 211, 153));
            int x = segment.x * CELL_SIZE + 3;
            int y = HEADER_HEIGHT + segment.y * CELL_SIZE + 3;
            g.fillRoundRect(x, y, CELL_SIZE - 6, CELL_SIZE - 6, 8, 8);
            index++;
        }
    }

    private void paintFood(Graphics2D g) {
        int padding = 5;
        int x = food.x * CELL_SIZE + padding;
        int y = HEADER_HEIGHT + food.y * CELL_SIZE + padding;
        int size = CELL_SIZE - padding * 2;
        g.setColor(new Color(248, 113, 113));
        g.fillOval(x, y, size, size);
        g.setColor(new Color(254, 202, 202));
        g.fillOval(x + 5, y + 4, 5, 5);
    }

    private void paintOverlay(Graphics2D g) {
        if (running && !gameOver) {
            return;
        }

        String title = gameOver ? "Game Over" : "Press Space to Start";
        String subtitle = gameOver ? "Press R or Space to play again" : "Use Arrow Keys or WASD";
        g.setColor(new Color(0, 0, 0, 145));
        g.fillRect(0, HEADER_HEIGHT, getWidth(), getHeight() - HEADER_HEIGHT);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        FontMetrics titleMetrics = g.getFontMetrics();
        int titleX = (getWidth() - titleMetrics.stringWidth(title)) / 2;
        int titleY = HEADER_HEIGHT + (ROWS * CELL_SIZE) / 2 - 12;
        g.setColor(new Color(248, 250, 252));
        g.drawString(title, titleX, titleY);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        FontMetrics subtitleMetrics = g.getFontMetrics();
        int subtitleX = (getWidth() - subtitleMetrics.stringWidth(subtitle)) / 2;
        g.setColor(new Color(203, 213, 225));
        g.drawString(subtitle, subtitleX, titleY + 34);
    }

    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private boolean isOpposite(Direction other) {
            return dx + other.dx == 0 && dy + other.dy == 0;
        }
    }
}
