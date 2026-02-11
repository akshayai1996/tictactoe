
import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicTacToe extends JFrame {

    // --- THEME ---
    static final Color BG = Color.decode("#020617");
    static final Color CARD = Color.decode("#0f172a");
    static final Color CELL_BG = new Color(30, 41, 59, 200);
    static final Color ACCENT = Color.decode("#22d3ee");
    static final Color ACCENT2 = Color.decode("#818cf8");
    static final Color TEXT = Color.decode("#f8fafc");
    static final Color SUBTEXT = new Color(203, 213, 225);
    static final Color WIN_C = Color.decode("#10b981");
    static final Color LOSE_C = Color.decode("#f43f5e");
    static final Color BORDER_C = new Color(255, 255, 255, 25);
    static final Color SEC_TEXT = new Color(100, 116, 139);

    // --- CUSTOM COMPONENTS ---

    // Rounded Panel (Container)
    static class RoundedPanel extends JPanel {
        int radius;

        RoundedPanel(int r) {
            radius = r;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(BORDER_C);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Circular Radio Button
    static class CRadio extends JRadioButton {
        CRadio(String text) {
            super(text);
            setOpaque(false);
            setForeground(SUBTEXT);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusPainted(false);
            setIcon(new CIcon(false));
            setSelectedIcon(new CIcon(true));
            setIconTextGap(8);
        }

        static class CIcon implements Icon {
            boolean sel;

            CIcon(boolean s) {
                sel = s;
            }

            @Override
            public int getIconWidth() {
                return 22;
            }

            @Override
            public int getIconHeight() {
                return 22;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (sel) {
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(x, y, 20, 20);
                    g2.fillOval(x + 5, y + 5, 10, 10);
                } else {
                    g2.setColor(new Color(51, 65, 85));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(x, y, 20, 20);
                }
                g2.dispose();
            }
        }
    }

    // Gradient Button
    static class GBtn extends JButton {
        boolean primary;

        GBtn(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setForeground(primary ? BG : SUBTEXT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (primary) {
                g2.setPaint(new GradientPaint(0, 0, ACCENT, w, 0, ACCENT2));
                g2.fillRoundRect(0, 0, w, h, 24, 24);
            } else {
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRoundRect(0, 0, w, h, 24, 24);
                g2.setColor(BORDER_C);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Game Cell Button
    static class CellBtn extends JButton {
        boolean win, lose;

        CellBtn() {
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 42));
            setForeground(ACCENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void reset() {
            win = false;
            lose = false;
            setText("");
            setEnabled(true);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw rounded cell background
            Color bg = win ? WIN_C : (lose ? LOSE_C : CELL_BG);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            if (!win && !lose) {
                g2.setColor(new Color(255, 255, 255, 20));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            }

            // Draw X/O text manually in the cyan accent color
            String txt = getText();
            if (txt != null && !txt.isEmpty()) {
                Color textCol = (win || lose) ? new Color(6, 78, 59) : ACCENT;
                g2.setColor(textCol);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(txt)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, tx, ty);
            }

            g2.dispose();
            // Do NOT call super.paintComponent() — we draw text ourselves
        }
    }

    // --- APP STATE ---
    private CardLayout cards;
    private JPanel cardPanel;

    private CRadio rx, ro, ry, rc, re, rm, rh;
    private JLabel statusLbl;
    private List<CellBtn> grid = new ArrayList<>();
    private GBtn playAgainBtn, backBtn, soundBtn;

    private String[] board = new String[9];
    private String pMark = "X", cMark = "O", turn = "X";
    private int diff = 1;
    private boolean over = false, sound = true;
    private Random rng = new Random();

    // --- CONSTRUCTOR ---
    public TicTacToe() {
        setTitle("Tic Tac Toe");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Background content pane
        JPanel bg = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        setContentPane(bg);

        // Rounded container
        RoundedPanel container = new RoundedPanel(30);
        container.setPreferredSize(new Dimension(420, 700));
        container.setLayout(new BorderLayout());
        bg.add(container);

        cards = new CardLayout();
        cardPanel = new JPanel(cards);
        cardPanel.setOpaque(false);
        container.add(cardPanel, BorderLayout.CENTER);

        buildSettings();
        buildGame();
        cards.show(cardPanel, "SETTINGS");

        setSize(520, 830);
        setLocationRelativeTo(null);
    }

    // --- SETTINGS PAGE ---
    private void buildSettings() {
        JPanel p = new JPanel(null); // Absolute layout for pixel-perfect control
        p.setOpaque(false);

        // Title
        JLabel title = new JLabel("Tic Tac Toe");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(ACCENT);
        title.setBounds(0, 20, 420, 50);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(title);

        int y = 90;

        // --- CHOOSE SIDE ---
        p.add(secLabel("CHOOSE SIDE", 40, y));
        y += 25;
        ButtonGroup sg = new ButtonGroup();
        rx = new CRadio("Play as X");
        ro = new CRadio("Play as O");
        rx.setSelected(true);
        sg.add(rx);
        sg.add(ro);
        rx.setBounds(40, y, 150, 35);
        ro.setBounds(210, y, 150, 35);
        p.add(rx);
        p.add(ro);
        y += 55;

        // --- WHO STARTS ---
        p.add(secLabel("WHO STARTS?", 40, y));
        y += 25;
        ButtonGroup og = new ButtonGroup();
        ry = new CRadio("You");
        rc = new CRadio("CPU");
        ry.setSelected(true);
        og.add(ry);
        og.add(rc);
        ry.setBounds(40, y, 150, 35);
        rc.setBounds(210, y, 150, 35);
        p.add(ry);
        p.add(rc);
        y += 55;

        // --- DIFFICULTY ---
        p.add(secLabel("DIFFICULTY", 40, y));
        y += 25;
        ButtonGroup dg = new ButtonGroup();
        re = new CRadio("Easy");
        rm = new CRadio("Med");
        rh = new CRadio("Hard");
        rm.setSelected(true);
        dg.add(re);
        dg.add(rm);
        dg.add(rh);
        re.setBounds(40, y, 110, 35);
        rm.setBounds(155, y, 110, 35);
        rh.setBounds(270, y, 110, 35);
        p.add(re);
        p.add(rm);
        p.add(rh);
        y += 70;

        // Start button
        GBtn startBtn = new GBtn("Start Game", true);
        startBtn.setBounds(40, y, 340, 55);
        startBtn.addActionListener(e -> startGame());
        p.add(startBtn);
        y += 70;

        // Sound button
        soundBtn = new GBtn("Sound: ON", false);
        soundBtn.setBounds(40, y, 340, 50);
        soundBtn.addActionListener(e -> {
            sound = !sound;
            soundBtn.setText(sound ? "Sound: ON" : "Sound: OFF");
        });
        p.add(soundBtn);

        cardPanel.add(p, "SETTINGS");
    }

    private JLabel secLabel(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(SEC_TEXT);
        l.setBounds(x, y, 200, 20);
        return l;
    }

    // --- GAME PAGE ---
    private void buildGame() {
        JPanel p = new JPanel(null);
        p.setOpaque(false);

        statusLbl = new JLabel("Your Turn", SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        statusLbl.setForeground(TEXT);
        statusLbl.setBounds(10, 15, 400, 40);
        p.add(statusLbl);

        int sx = 35, sy = 70, gap = 12, sz = 105;
        for (int i = 0; i < 9; i++) {
            CellBtn btn = new CellBtn();
            btn.setBounds(sx + (i % 3) * (sz + gap), sy + (i / 3) * (sz + gap), sz, sz);
            final int idx = i;
            btn.addActionListener(e -> cellClick(idx));
            grid.add(btn);
            p.add(btn);
        }

        int btnY = sy + 3 * (sz + gap) + 20;
        playAgainBtn = new GBtn("Play Again", true);
        playAgainBtn.setBounds(40, btnY, 340, 55);
        playAgainBtn.setVisible(false);
        playAgainBtn.addActionListener(e -> startGame());
        p.add(playAgainBtn);

        backBtn = new GBtn("Back to Settings", false);
        backBtn.setBounds(40, btnY + 65, 340, 50);
        backBtn.setVisible(false);
        backBtn.addActionListener(e -> cards.show(cardPanel, "SETTINGS"));
        p.add(backBtn);

        cardPanel.add(p, "GAME");
    }

    // --- GAME LOGIC ---
    private void startGame() {
        pMark = rx.isSelected() ? "X" : "O";
        cMark = pMark.equals("X") ? "O" : "X";
        diff = re.isSelected() ? 0 : (rh.isSelected() ? 2 : 1);
        turn = ry.isSelected() ? pMark : cMark;

        for (int i = 0; i < 9; i++) {
            board[i] = null;
            grid.get(i).reset();
        }
        over = false;
        statusLbl.setForeground(TEXT);
        playAgainBtn.setVisible(false);
        backBtn.setVisible(false);
        updateStatus();
        cards.show(cardPanel, "GAME");

        if (turn.equals(cMark)) {
            Timer t = new Timer(800, e -> cpuMove());
            t.setRepeats(false);
            t.start();
        }
    }

    private void cellClick(int idx) {
        if (over || board[idx] != null || !turn.equals(pMark))
            return;
        makeMove(idx, pMark);
        if (!checkEnd()) {
            turn = cMark;
            updateStatus();
            Timer t = new Timer(600, e -> cpuMove());
            t.setRepeats(false);
            t.start();
        }
    }

    private void cpuMove() {
        if (over)
            return;
        int idx;
        if (diff == 2)
            idx = bestMove();
        else if (diff == 0)
            idx = worstMove();
        else
            idx = (rng.nextInt(10) > 4) ? bestMove() : randMove();
        if (idx == -1)
            idx = randMove();
        if (idx == -1)
            return;
        makeMove(idx, cMark);
        if (!checkEnd()) {
            turn = pMark;
            updateStatus();
        }
    }

    private void makeMove(int i, String w) {
        board[i] = w;
        grid.get(i).setText(w);
        grid.get(i).setEnabled(false);
        playSound("tap");
    }

    // --- SOUND ---
    private void tone(int freq, int ms) {
        try {
            float rate = 44100;
            int samples = (int) (rate * ms / 1000);
            byte[] buf = new byte[samples];
            for (int i = 0; i < samples; i++) {
                buf[i] = (byte) (Math.sin(2.0 * Math.PI * i * freq / rate) * 80);
            }
            AudioFormat af = new AudioFormat(rate, 8, 1, true, false);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, af));
            line.open(af);
            line.start();
            line.write(buf, 0, buf.length);
            line.drain();
            line.close();
        } catch (Exception e) {
        }
    }

    private void playSound(String type) {
        if (!sound)
            return;
        new Thread(() -> {
            try {
                if (type.equals("tap")) {
                    tone(700, 80);
                } else if (type.equals("win")) {
                    tone(523, 120);
                    Thread.sleep(30);
                    tone(659, 120);
                    Thread.sleep(30);
                    tone(784, 120);
                    Thread.sleep(30);
                    tone(1046, 120);
                } else if (type.equals("lose")) {
                    tone(200, 250);
                    Thread.sleep(60);
                    tone(150, 250);
                    Thread.sleep(60);
                    tone(100, 250);
                }
            } catch (Exception e) {
            }
        }).start();
    }

    private void updateStatus() {
        if (!over)
            statusLbl.setText(turn.equals(pMark) ? "Your Turn" : "Computer Thinking...");
    }

    private boolean checkEnd() {
        int[][] wins = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 },
                { 2, 4, 6 } };
        for (int[] c : wins) {
            String a = board[c[0]];
            if (a != null && a.equals(board[c[1]]) && a.equals(board[c[2]])) {
                over = true;
                boolean pw = a.equals(pMark);
                for (int j : c) {
                    if (pw)
                        grid.get(j).win = true;
                    else
                        grid.get(j).lose = true;
                    grid.get(j).repaint();
                }
                statusLbl.setText(pw ? "Victory!" : "Defeat!");
                statusLbl.setForeground(pw ? WIN_C : LOSE_C);
                playSound(pw ? "win" : "lose");
                playAgainBtn.setVisible(true);
                backBtn.setVisible(true);
                return true;
            }
        }
        boolean full = true;
        for (String s : board)
            if (s == null) {
                full = false;
                break;
            }
        if (full) {
            over = true;
            statusLbl.setText("It's a Draw!");
            playAgainBtn.setVisible(true);
            backBtn.setVisible(true);
            return true;
        }
        return false;
    }

    // --- AI ---
    private int mm(String[] b, int d, boolean mx) {
        if (wl(b, cMark))
            return 10 - d;
        if (wl(b, pMark))
            return d - 10;
        if (fl(b))
            return 0;
        int best = mx ? -1000 : 1000;
        for (int i = 0; i < 9; i++) {
            if (b[i] == null) {
                b[i] = mx ? cMark : pMark;
                int v = mm(b, d + 1, !mx);
                best = mx ? Math.max(best, v) : Math.min(best, v);
                b[i] = null;
            }
        }
        return best;
    }

    private int bestMove() {
        int bv = -1000, bm = -1;
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = cMark;
                int v = mm(board, 0, false);
                board[i] = null;
                if (v > bv) {
                    bv = v;
                    bm = i;
                }
            }
        }
        return bm;
    }

    private int worstMove() {
        int wv = 1000, wm = -1;
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = cMark;
                int v = mm(board, 0, false);
                board[i] = null;
                if (v < wv) {
                    wv = v;
                    wm = i;
                }
            }
        }
        return wm != -1 ? wm : randMove();
    }

    private int randMove() {
        List<Integer> e = new ArrayList<>();
        for (int i = 0; i < 9; i++)
            if (board[i] == null)
                e.add(i);
        return e.isEmpty() ? -1 : e.get(rng.nextInt(e.size()));
    }

    private boolean wl(String[] b, String w) {
        int[][] wins = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 },
                { 2, 4, 6 } };
        for (int[] c : wins)
            if (w.equals(b[c[0]]) && w.equals(b[c[1]]) && w.equals(b[c[2]]))
                return true;
        return false;
    }

    private boolean fl(String[] b) {
        for (String s : b)
            if (s == null)
                return false;
        return true;
    }

    // --- MAIN ---
    public static void main(String[] args) {
        // Set system look and feel for better font rendering
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        // Enable anti-aliased text globally
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> new TicTacToe().setVisible(true));
    }
}
