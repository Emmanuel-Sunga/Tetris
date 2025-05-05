package mino;

import java.awt.Graphics2D;
import main.PlayManager;

public class GhostMino {
    public Block b[] = new Block[4];
    //public int x, y;
    
    public GhostMino(Mino original) {
    	//Initialize position to match original mino
    	
        // Copy the original mino's blocks
        for(int i = 0; i < 4; i++) {
            b[i] = new Block(original.b[i].c);
            b[i].x = original.b[i].x;
            b[i].y = original.b[i].y;
            b[i].setGhost(true);
        }
    }
    
    public void updatePosition(Mino original) {
        // Update position to match original mino
        for(int i = 0; i < 4; i++) {
            b[i].x = original.b[i].x;
            b[i].y = original.b[i].y;
        }
    }
    
    public void dropToBottom() {
        boolean canDrop = true;
        while(canDrop) {
            // Check if any block would hit the bottom or another block
            for(Block block : b) {
                // Check bottom boundary
                if(block.y + Block.SIZE >= PlayManager.bottom_y) {
                    canDrop = false;
                    break;
                }
                
                // Check collision with static blocks
                for(Block staticBlock : PlayManager.staticBlocks) {
                    if(block.y + Block.SIZE == staticBlock.y && block.x == staticBlock.x) {
                        canDrop = false;
                        break;
                    }
                }
                if(!canDrop) break;
            }
            
            if(canDrop) {
                for(Block block : b) {
                    block.y += Block.SIZE;
                }
            }
        }
    }
    
    public void draw(Graphics2D g2) {
        for(Block block : b) {
            block.draw(g2);
        }
    }
    
    // Helper method to check bottom collision (similar to Mino's method)
    private boolean checkBottomCollision() {
        for(Block block : b) {
            // Check if reached bottom
            if(block.y + Block.SIZE >= PlayManager.bottom_y) {
                return true;
            }
            
            // Check collision with static blocks
            for(Block staticBlock : PlayManager.staticBlocks) {
                if(staticBlock.y == block.y + Block.SIZE && 
                   staticBlock.x == block.x) {
                    return true;
                }
            }
        }
        return false;
    }
}