package net.defekt.minesweeper;

public class Main {

    public static void main(String[] args) {
        try {
            MinesGame game = new MinesGame();
            game.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
