package net.defekt.minesweeper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.InputFilter;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.Window.Hint;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

public class MinesGame {

    private static final Theme THEME = SimpleTheme.makeTheme(true, TextColor.ANSI.WHITE, TextColor.ANSI.BLACK,
            TextColor.ANSI.WHITE, TextColor.ANSI.BLACK, TextColor.ANSI.WHITE, TextColor.ANSI.BLACK,
            TextColor.ANSI.BLACK);

    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_BOMBS = 10;

    private final Screen screen;
    private final WindowBasedTextGUI gui;
    private final TextBox canvas = new TextBox();
    private final Random rand = new Random();
    private TerminalPosition cursor = new TerminalPosition(0, 0);

    public MinesGame() throws IOException {
        screen = new DefaultTerminalFactory().createScreen();
        gui = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);

        canvas.setCaretWarp(false);

        canvas.setInputFilter(new InputFilter() {

            private final List<KeyType> arrows = Arrays.asList(KeyType.ArrowDown, KeyType.ArrowLeft, KeyType.ArrowRight,
                    KeyType.ArrowUp);

            @Override
            public boolean onInput(Interactable interactable, KeyStroke keyStroke) {
                KeyType key = keyStroke.getKeyType();
                TerminalPosition caret = canvas.getCaretPosition();

                int x = caret.getColumn() - offsetX;
                int z = caret.getRow() - offsetZ;

                if (!halt) {
                    if (key == KeyType.Enter) {
                        discoverField(x, z);
                    } else {
                        Character c = keyStroke.getCharacter();
                        if (c != null) {
                            if (c.toString().equals(" ")) {
                                flag(x, z);
                            }
                        }
                    }
                }
                Character c = keyStroke.getCharacter();
                if (c != null) {
                    switch (c.toString().toLowerCase()) {
                        case "n": {
                            newGame(size, bombs);
                            break;
                        }
                        case "l": {
                            displayDialog("Legend: \n" + "- = Discovered field \n" + "1-9 = Number of mines \n"
                                    + "P = Flag \n" + "x = Mine \n" + "B = Bad flag", false);
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                } else {
                    if (key == KeyType.Escape) {
                        displayDialog("Main menu");
                    }
                }

                boolean filter = arrows.contains(key);
                if (filter) {
                    switch (key) {
                        default:
                        case ArrowDown: {
                            z++;
                            break;
                        }
                        case ArrowLeft: {
                            x--;
                            break;
                        }
                        case ArrowUp: {
                            z--;
                            break;
                        }
                        case ArrowRight: {
                            x++;
                            break;
                        }
                    }
                    cursor = new TerminalPosition(x, z);
                    draw();
                }
                return filter;
            }
        });
    }

    private void flag(int x, int z) {
        if (x >= 0 && z >= 0 && x < matrix.length && z < matrix[x].length) {
            byte field = matrix[x][z];
            byte newField = -1;
            switch (field) {
                case 0: {
                    newField = 11;
                    break;
                }
                case 13: {
                    newField = 12;
                    break;
                }
                case 11: {
                    newField = 0;
                    break;
                }
                case 12: {
                    newField = 13;
                    break;
                }
                default: {
                    break;
                }
            }
            if (newField > -1) {
                matrix[x][z] = newField;
                checkWin();
            }
            draw();
        }
    }

    private boolean started = false;

    private boolean halt = false;

    private final Timer timer = new Timer(true);

    private void displayDialog(String message) {
        displayDialog(message, true);
    }

    private void displayDialog(String message, boolean controls) {
        Window win = new BasicWindow("Minesweeper");
        win.setHints(Arrays.asList(Hint.CENTERED));

        Panel buttonBox = new Panel(new LinearLayout(Direction.HORIZONTAL));

        Button nGame = new Button("New Game", new Runnable() {

            @Override
            public void run() {
                win.close();
                newGame(size, bombs);
            }
        });

        Button cGame = new Button("Cancel", new Runnable() {

            @Override
            public void run() {
                win.close();
            }
        });
        Button eGame = new Button("Exit", new Runnable() {

            @Override
            public void run() {
                System.exit(0);
            }
        });

        if (matrix.length != size) cGame.setEnabled(false);

        Button aGame = new Button("Settings", new Runnable() {

            @Override
            public void run() {
                Window settings = new BasicWindow("Settings");
                settings.setHints(Arrays.asList(Hint.CENTERED));

                Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

                Panel sizePanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
                Panel bombsPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));

                TextBox sizeBox = new TextBox(Integer.toString(size));
                TextBox bombsBox = new TextBox(Integer.toString(bombs));

                sizeBox.setPreferredSize(new TerminalSize(4, 1));
                bombsBox.setPreferredSize(new TerminalSize(4, 1));

                sizePanel.addComponent(new Label("Size: "));
                bombsPanel.addComponent(new Label("Mines: "));

                sizePanel.addComponent(sizeBox);
                bombsPanel.addComponent(bombsBox);

                Button done = new Button("Apply", new Runnable() {

                    @Override
                    public void run() {
                        try {
                            int size = Integer.parseInt(sizeBox.getText());
                            int bombs = Integer.parseInt(bombsBox.getText());

                            MinesGame.this.size = size;
                            MinesGame.this.bombs = bombs;

                            settings.close();
                        } catch (NumberFormatException ex) {
                        }
                    }
                });

                panel.addComponent(sizePanel);
                panel.addComponent(bombsPanel);
                panel.addComponent(new Label(" "));
                panel.addComponent(done);
                settings.setComponent(panel);
                gui.addWindow(settings);
            }
        });

        if (controls) buttonBox.addComponent(nGame);
        buttonBox.addComponent(cGame);
        if (controls) {
            buttonBox.addComponent(aGame);
            buttonBox.addComponent(eGame);
        }
        Panel pan = new Panel(new LinearLayout(Direction.VERTICAL));
        pan.addComponent(new Label(message));
        pan.addComponent(new Label(" "));
        pan.addComponent(buttonBox);

        win.setComponent(pan);
        gui.addWindow(win);
    }

    private void discoverField(int x, int z) {
        discoverField(x, z, true, false);
    }

    private void discoverField(int x, int z, boolean original, boolean chording) {
        if (x >= 0 && z >= 0 && x < matrix.length && z < matrix[x].length) {
            byte field = matrix[x][z];
            switch (field) {
                case 13: {
                    if (!started) {
                        matrix[x][z] = 0;
                        placeBomb();
                        discoverField(x, z, true, false);
                    } else {
                        halt = true;
                        int index = 0;
                        matrix[x][z] = 14;
                        for (int i = 0; i < matrix.length; i++)
                            for (int j = 0; j < matrix[i].length; j++) {
                                byte f = matrix[i][j];
                                byte nf = -1;
                                if (f == 13) nf = 14;
                                if (f == 11) nf = 15;

                                int iF = i;
                                int jF = j;
                                byte nfF = nf;

                                if (nf != -1) {
                                    index++;
                                    timer.schedule(new TimerTask() {

                                        @Override
                                        public void run() {
                                            matrix[iF][jF] = nfF;
                                            draw();
                                        }
                                    }, 25 * index);
                                }

                                if (iF == matrix.length - 1 && jF == matrix[iF].length - 1) {
                                    timer.schedule(new TimerTask() {

                                        @Override
                                        public void run() {
                                            displayDialog("YOU LOST!");
                                        }
                                    }, 25 * index);
                                }
                            }

                    }
                    break;
                }
                case 0: {
                    if (!started) startTime = System.currentTimeMillis();
                    started = true;
                    int count = countBombs(x, z);
                    if (count == 0) {
                        matrix[x][z] = 10;
                        for (int i = -1; i <= 1; i++)
                            for (int j = -1; j <= 1; j++)
                                discoverField(x + i, z + j, false, false);
                    } else {
                        matrix[x][z] = (byte) count;
                    }

                    if (original || chording) checkWin();
                    break;
                }
                default: {
                    if (original && field > 0 && field < 10) {
                        if (countFlags(x, z) == field) {
                            for (int i = -1; i <= 1; i++)
                                for (int j = -1; j <= 1; j++) {
                                    discoverField(x + i, z + j, false, true);
                                }
                        }
                    }
                    break;
                }
            }

            draw();
        }
    }

    private long startTime = 0;

    private void checkWin() {
        if (countEmpty() <= 0 && countBombs() == 0 && countFalseFlags() == 0) {
            halt = true;
            displayDialog("You won! \n" + "Time: " + (System.currentTimeMillis() - startTime) / 1000 + "s");
        }
    }

    private int countBombs(int cx, int cz) {
        int count = 0;
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int z = cz - 1; z <= cz + 1; z++) {
                if (x >= 0 && z >= 0 && x < matrix.length && z < matrix[x].length) {
                    byte field = matrix[x][z];
                    if (field == 12 || field == 13) count++;
                }
            }
        return count;
    }

    private int countFlags(int cx, int cz) {
        int count = 0;
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int z = cz - 1; z <= cz + 1; z++) {
                if (x >= 0 && z >= 0 && x < matrix.length && z < matrix[x].length) {
                    byte field = matrix[x][z];
                    if (field == 11 || field == 12) count++;
                }
            }
        return count;
    }

    public void start() throws IOException {
        screen.startScreen();
        Window win = new BasicWindow();
        win.setTheme(THEME);
        win.setHints(Arrays.asList(Hint.FULL_SCREEN, Hint.NO_DECORATIONS));
        win.setComponent(canvas);
        gui.addWindow(win);
        ((SeparateTextGUIThread) gui.getGUIThread()).start();

        displayDialog("Controls: \n" + "Enter - Discover field/Chord \n" + "Space - Flag field");
    }

    private byte[][] matrix = new byte[1][1];

    public void newGame(int size, int bombs) {
        initMatrix(size, bombs);
        halt = false;
        started = false;
        draw();
    }

    private int offsetX = 0;
    private int offsetZ = 3;

    private int size = DEFAULT_SIZE;
    private int bombs = DEFAULT_BOMBS;

    private int countBombs() {
        int realBombs = 0;
        int flags = 0;
        for (byte[] i : matrix)
            for (byte j : i) {
                if (j == 14 || j == 13 || j == 12) {
                    realBombs++;
                }
                if (j == 12 || j == 11) flags++;
            }
        return realBombs - flags;
    }

    private int countFalseFlags() {
        int flags = 0;
        for (byte[] i : matrix)
            for (byte j : i) {
                if (j == 11) flags++;
            }
        return flags;
    }

    private int countEmpty() {
        int empty = 0;
        for (byte[] i : matrix)
            for (byte j : i) {
                if (j == 0) {
                    empty++;
                }
            }
        return empty;
    }

    private void draw() {
        if (matrix.length > 0 && matrix[0].length > 0) {
            StringBuilder bd = new StringBuilder();
            int len = Integer.toString(matrix.length - 1).length();
            offsetX = len + 2;

            for (int z = 0; z < 3; z++) {
                bd.append(Chars.padSpace(offsetX));
                for (int x = 0; x < matrix.length; x++) {
                    String ch = z == 0 ? (x == cursor.getColumn() ? "v" : " ") : z == 1 ? Chars.getLetter(x) : "=";
                    bd.append(ch);
                }
                bd.append("\n");
            }

            for (int z = 0; z < matrix[0].length; z++) {
                String line = (z == cursor.getRow() ? ">" : " ") + Chars.pad(z, len) + "|";
                bd.append(line);
                for (int x = 0; x < matrix.length; x++) {
                    byte field = matrix[x][z];
                    String ch = Chars.getField(field);
                    bd.append(ch);
                }
                bd.append("\n");
            }
            bd.append("\n");
            bd.append("Bombs: " + countBombs() + "\n");
            bd.append("N - New Game\n");
            bd.append("L - Legend\n");
            bd.append("Esc - Menu\n");

            canvas.setText(bd.toString());
        }
    }

    private void initMatrix(int size, int bombs) {
        matrix = new byte[size][size];
        for (int z = 0; z < matrix.length; z++)
            for (int x = 0; x < matrix[z].length; x++)
                matrix[z][x] = 0;
        initBombs(bombs);
    }

    private void initBombs(int count) {
        for (int x = 0; x < count; x++) {
            placeBomb();
        }
    }

    private void placeBomb() {
        int cx = rand.nextInt(matrix.length);
        int cz = rand.nextInt(matrix[0].length);
        if (matrix[cx][cz] == 0)
            matrix[cx][cz] = 13;
        else
            placeBomb();
    }
}
