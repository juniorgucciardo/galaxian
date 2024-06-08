package com.utils.galaxian;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class GalaxianGame extends JPanel implements ActionListener {

    private Timer timer;
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Missile> missiles;
    private ArrayList<EnemyMissile> enemyMissiles; // Adiciona lista de mísseis inimigos

    public final int PLAYER_COOLDOWN = 500; // Cooldown de 500ms para o disparo do player
    public long lastPlayerShot;
    public int enemySpeed = 3; // Velocidade inicial dos inimigos
    public int enemyDirection = 1; // Direção inicial dos inimigos: 1 para direita, -1 para esquerda
    private boolean inGame = true;
    private long lastEnemyMoveTime;
    private final int ENEMY_MOVE_DELAY = 100; // Delay de 100ms para a movimentação dos inimigos
    private final int ENEMY_SHOOT_INTERVAL = 1000; // Intervalo de 3 segundos para os disparos dos inimigos
    private long lastEnemyShootTime;

    public GalaxianGame() {
        setFocusable(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(800, 600));
        player = new Player(this);
        enemies = new ArrayList<>();
        enemyMissiles = new ArrayList<>(); // Inicializa lista de mísseis inimigos
        missiles = new ArrayList<>();
        addKeyListener(new TAdapter());
        timer = new Timer(10, this);
        timer.start();
        initEnemies();
    }

    private void initEnemies() {
        int startX = 100;
        int startY = 120;
        int spacingX = 60;
        int spacingY = 40;

        // Linhas de 10 inimigos
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 10; i++) {
                enemies.add(new Enemy(startX + i * spacingX, startY + j * spacingY, j % 2 == 0));
            }
        }

        // Linha de 8 inimigos
        startX += spacingX / 2;
        startY -= spacingY;
        for (int i = 0; i < 9; i++) {
            enemies.add(new Enemy(startX + i * spacingX, startY, true));
        }

        // Linha de 6 inimigos
        startX += spacingX / 2;
        startY -= spacingY;
        for (int i = 0; i < 8; i++) {
            enemies.add(new Enemy(startX + i * spacingX, startY, false));
        }

        // Adicionar "boos" (inimigos especiais)
        enemies.add(new Enemy(50, 0, true));
        enemies.add(new Enemy(750, 0, true));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawObjects(g);
    }

    private void drawObjects(Graphics g) {
        if (inGame) {
            if (player.isVisible()) {
                g.setColor(Color.WHITE);
                g.fillRect(player.getX(), player.getY(), 20, 20);
            }

            for (Enemy enemy : enemies) {
                if (enemy.isVisible()) {
                    g.setColor(enemy.isTypeOne() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.fillRect(enemy.getX(), enemy.getY(), 20, 20);
                }
            }

            for (Missile missile : missiles) {
                if (missile.isVisible()) {
                    g.setColor(Color.GRAY);
                    g.fillRect(missile.getX(), missile.getY(), 5, 10);
                }
            }

            // Desenha mísseis inimigos
            for (EnemyMissile enemyMissile : enemyMissiles) {
                if (enemyMissile.isVisible()) {
                    g.setColor(Color.RED);
                    g.fillRect(enemyMissile.getX(), enemyMissile.getY(), 5, 10);
                }
            }

            Toolkit.getDefaultToolkit().sync();
        } else {
            showGameOver(g);
        }
    }

    private void showGameOver(Graphics g) {
        String message = "Game Over";
        String restartPrompt = "Press R to Restart";
        Font small = new Font("Helvetica", Font.BOLD, 20);
        FontMetrics fm = getFontMetrics(small);

        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(message, (getWidth() - fm.stringWidth(message)) / 2, getHeight() / 2 - 50);
        g.drawString(restartPrompt, (getWidth() - fm.stringWidth(restartPrompt)) / 2, getHeight() / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateMissiles();
        updatePlayer();
        updateEnemies();
        updateEnemyMissiles(); // Atualiza mísseis inimigos
        checkCollisions();
        repaint();
    }

    private void updateMissiles() {
        ArrayList<Missile> toRemove = new ArrayList<>();
        for (Missile missile : missiles) {
            if (missile.isVisible()) {
                missile.move();
            } else {
                toRemove.add(missile);
            }
        }
        missiles.removeAll(toRemove);
    }

    private void updatePlayer() {
        player.move();
    }

    private void updateEnemies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemyMoveTime < ENEMY_MOVE_DELAY) {
            return; // Retorna se o delay não passou
        }
        lastEnemyMoveTime = currentTime;

        boolean changeDirection = false;

        for (Enemy enemy : enemies) {
            if (enemy.isVisible()) {
                enemy.move(enemyDirection * enemySpeed, 0);

                if (enemy.getX() <= 0 || enemy.getX() >= getWidth() - 20) {
                    changeDirection = true;
                }
            }
        }

        if (changeDirection) {
            enemyDirection *= -1;
            for (Enemy enemy : enemies) {
                if (enemy.isVisible()) {
                    enemy.move(0, 20); // Move para baixo
                }
            }
        }

        // Disparo aleatório dos inimigos
        if (currentTime - lastEnemyShootTime > ENEMY_SHOOT_INTERVAL) {
            shootFromNearestEnemy();
            lastEnemyShootTime = currentTime;
        }
    }

    private void shootFromRandomEnemy() {
        ArrayList<Enemy> visibleEnemies = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.isVisible()) {
                visibleEnemies.add(enemy);
            }
        }

        if (!visibleEnemies.isEmpty()) {
            Enemy shooter = visibleEnemies.get((int) (Math.random() * visibleEnemies.size()));
            enemyMissiles.add(new EnemyMissile(shooter.getX() + 10, shooter.getY() + 20));
        }
    }

    private void shootFromNearestEnemy() {
        Enemy nearestEnemy = findNearestEnemy();
        if (nearestEnemy != null) {
            enemyMissiles.add(new EnemyMissile(nearestEnemy.getX() + 10, nearestEnemy.getY() + 20));
        }
    }

    private void updateEnemyMissiles() {
        ArrayList<EnemyMissile> toRemove = new ArrayList<>();
        for (EnemyMissile missile : enemyMissiles) {
            if (missile.isVisible()) {
                missile.move();
            } else {
                toRemove.add(missile);
            }
        }
        enemyMissiles.removeAll(toRemove);
    }

    private Enemy findNearestEnemy() {
        Enemy nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        for (Enemy enemy : enemies) {
            if (enemy.isVisible()) {
                double distance = Math.sqrt(Math.pow(player.getX() - enemy.getX(), 2) + Math.pow(player.getY() - enemy.getY(), 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestEnemy = enemy;
                }
            }
        }

        return nearestEnemy;
    }

    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();

        for (Missile missile : missiles) {
            Rectangle missileBounds = missile.getBounds();
            for (Enemy enemy : enemies) {
                if (enemy.isVisible() && missileBounds.intersects(enemy.getBounds())) {
                    missile.setVisible(false);
                    enemy.setVisible(false);
                }
            }
        }

        for (Enemy enemy : enemies) {
            if (enemy.isVisible() && playerBounds.intersects(enemy.getBounds())) {
                inGame = false;
            }
        }

        for (EnemyMissile missile : enemyMissiles) {
            if (missile.isVisible() && playerBounds.intersects(missile.getBounds())) {
                missile.setVisible(false);
                player.loseLife();
                if (player.getLives() <= 0) {
                    inGame = false;
                }
            }
        }

        if (!player.isVisible()) {
            inGame = false;
        }
    }

    public void addMissile(Missile missile) {
        missiles.add(missile);
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyReleased(KeyEvent e) {
            player.keyReleased(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            player.keyPressed(e);

            int key = e.getKeyCode();
            if (key == KeyEvent.VK_R && !inGame) {
                restartGame();
            }
        }
    }

    private void restartGame() {
        inGame = true;
        player = new Player(this);
        enemies.clear();
        missiles.clear();
        initEnemies();
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Galaxian Game");
        GalaxianGame game = new GalaxianGame();
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class Player {

    private int x;
    private int y;
    private int dx;
    private int dy;
    private boolean visible;
    private GalaxianGame game;
    private int lives = 3; // Adiciona contagem de vidas

    public Player(GalaxianGame game) {
        this.game = game;
        x = 400;
        y = 550;
        visible = true;
    }

    public void move() {
        x += dx;
        y += dy;
        if (x < 0) {
            x = 0;
        }
        if (x > game.getWidth() - 20) {
            x = game.getWidth() - 20;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
            dx = -2;
        }

        if (key == KeyEvent.VK_RIGHT) {
            dx = 2;
        }

        if (key == KeyEvent.VK_SPACE && System.currentTimeMillis() - game.lastPlayerShot > game.PLAYER_COOLDOWN) {
            fire();
            game.lastPlayerShot = System.currentTimeMillis();
        }
    }

    public void fire() {
        game.addMissile(new Missile(x + 10, y));
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            dx = 0;
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 20, 20);
    }

    public void loseLife() {
        lives--;
        if (lives <= 0) {
            visible = false;
        }
    }

    public int getLives() {
        return lives;
    }
}

class Enemy {

    private int x;
    private int y;
    private boolean visible;
    private boolean typeOne;

    public Enemy(int x, int y, boolean typeOne) {
        this.x = x;
        this.y = y;
        this.typeOne = typeOne;
        visible = true;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isTypeOne() {
        return typeOne;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 20, 20);
    }
}

class Missile {

    private int x;
    private int y;
    private boolean visible;

    public Missile(int x, int y) {
        this.x = x;
        this.y = y;
        visible = true;
    }

    public void move() {
        y -= 4;
        if (y < 0) {
            visible = false;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 5, 10);
    }
}

class EnemyMissile {

    private int x;
    private int y;
    private boolean visible;

    public EnemyMissile(int x, int y) {
        this.x = x;
        this.y = y;
        visible = true;
    }

    public void move() {
        y += 4; // Move para baixo
        if (y > 600) { // Se sair da tela, torna invisível
            visible = false;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 5, 10);
    }
}
