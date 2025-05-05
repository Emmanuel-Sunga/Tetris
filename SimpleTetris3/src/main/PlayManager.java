package main;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import mino.*;

public class PlayManager {
    // Game State
    public static GameState gameState = GameState.MENU;
    
    // Play area
    final int WIDTH = 360;
    final int HEIGHT = 600;
    public static int left_x;
    public static int right_x;
    public static int top_y;
    public static int bottom_y;
    
    // Mino
    public Mino currentMino;
    final int MINO_START_X;
    final int MINO_START_Y;
    public Mino nextMino;
    final int NEXTMINO_X;
    final int NEXTMINO_Y;
    public static ArrayList<Block> staticBlocks = new ArrayList<>();
    
    // Mino randomization
    private ArrayList<Class<? extends Mino>> minoTypes;
    private ArrayList<Mino> minoBag;
    private Random random;
    
    // Drop speed
    public static int dropInterval = 60;
    
    // Game Over
    boolean gameOver;
    
    // Effects
    boolean effectCounterOn;
    int effectCounter;
    ArrayList<Integer> effectY = new ArrayList<>();
    
    // Score
    int level = 1;
    int lines;
    int score;
    
    // Ghost Block
    private GhostMino ghostMino;

    public PlayManager() {
        left_x = (GamePanel.WIDTH / 2) - (WIDTH / 2);
        right_x = left_x + WIDTH;
        top_y = 50;
        bottom_y = top_y + HEIGHT;

        MINO_START_X = left_x + (WIDTH / 2) - Block.SIZE;
        MINO_START_Y = top_y + Block.SIZE;

        NEXTMINO_X = right_x + 175;
        NEXTMINO_Y = top_y + 500;

        random = new Random();
        minoTypes = new ArrayList<>();
        minoTypes.add(Mino_L1.class);
        minoTypes.add(Mino_L2.class);
        minoTypes.add(Mino_Square.class);
        minoTypes.add(Mino_Bar.class);
        minoTypes.add(Mino_T.class);
        minoTypes.add(Mino_Z1.class);
        minoTypes.add(Mino_Z2.class);

        minoBag = new ArrayList<>();
        refillBag();

        currentMino = pickMino();
        currentMino.setXY(MINO_START_X, MINO_START_Y);
        ghostMino = new GhostMino(currentMino);
        ghostMino.dropToBottom();
        nextMino = pickMino();
        nextMino.setXY(NEXTMINO_X, NEXTMINO_Y);
    }

    private void refillBag() {
        ArrayList<Class<? extends Mino>> shuffled = new ArrayList<>(minoTypes);
        Collections.shuffle(shuffled, random);

        try {
            for (Class<? extends Mino> minoClass : shuffled) {
                minoBag.add(minoClass.getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Mino pickMino() {
        if (minoBag.isEmpty()) {
            refillBag();
        }
        return minoBag.remove(0);
    }

    public void update() {
        if (gameState != GameState.PLAYING) return;
        
        //Handle hard drop input
        if(KeyHandler.hardDropPressed) {
        	hardDrop();
        	KeyHandler.hardDropPressed = false;
        }

        // Update ghost mino
        if (currentMino != null) {
            if (ghostMino == null) {
                ghostMino = new GhostMino(currentMino);
            }
            ghostMino.updatePosition(currentMino);
            ghostMino.dropToBottom();
        }

        if (!currentMino.active) {
            // Add current mino blocks to static blocks
            for (int i = 0; i < 4; i++) {
                staticBlocks.add(currentMino.b[i]);
            }

            // Check if game over
            if (currentMino.b[0].x == MINO_START_X && currentMino.b[0].y == MINO_START_Y) {
                gameOver = true;
                GamePanel.music.stop();
                GamePanel.se.play(2, false);
            }

            currentMino.deactivating = false;
            currentMino = nextMino;
            currentMino.setXY(MINO_START_X, MINO_START_Y);
            nextMino = pickMino();
            nextMino.setXY(NEXTMINO_X, NEXTMINO_Y);
            
            // Create new ghost for the new piece
            ghostMino = new GhostMino(currentMino);
            ghostMino.dropToBottom();

            checkDelete();
        } else {
            currentMino.update();
        }
    }
    
    public void hardDrop() {
        if (ghostMino != null && ghostMino.b.length > 0) {
            // Play drop sound FIRST
            GamePanel.se.play(4, false);
            
            // Calculate how far to move down
            int dropDistance = ghostMino.b[0].y - currentMino.b[0].y;
            
            // Move current mino down by this distance
            currentMino.y += dropDistance;
            for(Block block : currentMino.b) {
                block.y += dropDistance;
            }
            
            // Deactivate the current mino
            currentMino.active = false;
            
            // Update immediately
            update();
        }
    }

    private void checkDelete() {
        int x = left_x;
        int y = top_y;
        int blockCount = 0;
        int lineCount = 0;
        
        while(x < right_x && y < bottom_y) {
            for(int i = 0; i < staticBlocks.size(); i++) {
                if(staticBlocks.get(i).x == x && staticBlocks.get(i).y == y) {
                    blockCount++;
                }
            }
            
            x += Block.SIZE;
            
            if(x == right_x) {
                if(blockCount == 12) {
                    effectCounterOn = true;
                    effectY.add(y);
                    
                    for(int i = staticBlocks.size()-1; i > -1; i--) {
                        if(staticBlocks.get(i).y == y) {
                            staticBlocks.remove(i);
                        }
                    }
                    
                    lineCount++;
                    lines++;
                    
                    if(lines % 10 == 0 && dropInterval > 1) {
                        level++;
                        if(dropInterval > 10) {
                            dropInterval -= 10;
                        }
                        else {
                            dropInterval -= 1;
                        }
                    }
                  
                    for(int i = 0; i < staticBlocks.size(); i++) {
                        if(staticBlocks.get(i).y < y) {
                            staticBlocks.get(i).y += Block.SIZE;
                        }
                    }
                }
                
                blockCount = 0;
                x = left_x;
                y += Block.SIZE;
            }
        }
        
        if(lineCount > 0) {
            GamePanel.se.play(1, false);
            int singleLineScore = 50 * level;
            score += singleLineScore * lineCount;
        }
    }

    public void draw(Graphics2D g2) {
        // Draw play area frame
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(left_x - 4, top_y - 4, WIDTH + 8, HEIGHT + 8);

        // Draw next mino frame
        int x = right_x + 100;
        int y = bottom_y - 200;
        g2.setColor(Color.white);
        g2.drawRect(x, y, 200, 200);
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString("NEXT", x + 60, y + 60);

        // Draw score frame
        g2.drawRect(x, top_y, 250, 300);
        x += 40;
        y = top_y + 50;
        g2.drawString("LEVEL: " + level, x, y); y += 70;
        g2.drawString("LINES: " + lines, x, y); y += 70;
        g2.drawString("SCORE: " + score, x, y); y += 70;

        if (gameState != GameState.MENU) {
            // Draw ghost mino first (behind everything)
            if (ghostMino != null) {
                ghostMino.draw(g2);
            }

            // Draw current mino
            if (currentMino != null) {
                currentMino.draw(g2);
            }

            // Draw next mino
            nextMino.draw(g2);

            // Draw static blocks
            for (Block block : staticBlocks) {
                block.draw(g2);
            }

            // Draw line clear effects
            if (effectCounterOn) {
                effectCounter++;
                Color flashColor = (effectCounter % 4 < 2) ? Color.red : Color.white;
                g2.setColor(flashColor);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                for (int lineY : effectY) {
                    g2.fillRect(left_x, lineY, WIDTH, Block.SIZE);
                }
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                if (effectCounter >= 8) {
                    effectCounterOn = false;
                    effectCounter = 0;
                    effectY.clear();
                }
            }

            // Draw game over screen
            if (gameOver) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(left_x, top_y, WIDTH, HEIGHT);
                g2.setColor(Color.RED);
                g2.setFont(new Font("Arial", Font.BOLD, 50));
                g2.drawString("GAME OVER", left_x + 20, top_y + 320);
            } 
            // Draw pause screen
            else if (gameState == GameState.PAUSED) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(left_x, top_y, WIDTH, HEIGHT);
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Arial", Font.BOLD, 50));
                g2.drawString("PAUSED", left_x + 70, top_y + 320);
            }
        }

        // Draw title
        int titleX = left_x - 300;
        int titleY = top_y + 350;
        
        // Shadow
        g2.setColor(new Color(50, 50, 50, 150));
        g2.setFont(new Font("Impact", Font.BOLD, 80));
        g2.drawString("TETRIS", titleX + 5, titleY + 5);
        
        // Main text
        g2.setColor(new Color(255, 50, 50));
        g2.drawString("TETRIS", titleX, titleY);
        
        // Subtitle
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("The Classic Block Game", titleX + 8, titleY + 30);
    }
}