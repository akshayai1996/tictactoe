using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Runtime.InteropServices;

namespace TicTacToeApp
{
    // --- Custom Theme ---
    public static class AppColors 
    {
        public static readonly Color Bg = ColorTranslator.FromHtml("#020617");
        public static readonly Color Card = ColorTranslator.FromHtml("#0f172a");
        public static readonly Color Cell = Color.FromArgb(153, 30, 41, 59);
        public static readonly Color Accent = ColorTranslator.FromHtml("#22d3ee");
        public static readonly Color AccentAlt = ColorTranslator.FromHtml("#818cf8");
        public static readonly Color Text = ColorTranslator.FromHtml("#f8fafc");
        public static readonly Color SubText = Color.FromArgb(203, 213, 225);
        public static readonly Color Win = ColorTranslator.FromHtml("#10b981");
        public static readonly Color Lose = ColorTranslator.FromHtml("#f43f5e");
        public static readonly Color Border = Color.FromArgb(25, 255, 255, 255);
        public static readonly Color SectionText = Color.FromArgb(100, 116, 139);
    }

    // --- Helpers ---
    public static class GfxHelper
    {
        public static GraphicsPath RoundedRect(Rectangle rect, int radius)
        {
            GraphicsPath path = new GraphicsPath();
            int d = radius * 2;
            path.AddArc(rect.X, rect.Y, d, d, 180, 90);
            path.AddArc(rect.Right - d, rect.Y, d, d, 270, 90);
            path.AddArc(rect.Right - d, rect.Bottom - d, d, d, 0, 90);
            path.AddArc(rect.X, rect.Bottom - d, d, d, 90, 90);
            path.CloseFigure();
            return path;
        }
    }

    // --- Custom Circular Radio Button ---
    public class CircleRadioButton : RadioButton
    {
        public CircleRadioButton()
        {
            this.SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
            this.ForeColor = AppColors.SubText;
            this.BackColor = Color.Transparent;
            this.Font = new Font("Segoe UI", 10, FontStyle.Bold);
            this.Cursor = Cursors.Hand;
            this.Height = 40;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            // Clear background completely
            g.Clear(this.BackColor == Color.Transparent ? AppColors.Card : this.BackColor);

            // Circle indicator position
            int circleSize = 20;
            int circleX = 6;
            int circleY = (Height - circleSize) / 2;
            Rectangle circleRect = new Rectangle(circleX, circleY, circleSize, circleSize);

            if (Checked)
            {
                // Outer ring - accent color
                using (Pen accPen = new Pen(AppColors.Accent, 2))
                {
                    g.DrawEllipse(accPen, circleRect);
                }
                // Inner dot
                Rectangle inner = new Rectangle(circleX + 5, circleY + 5, circleSize - 10, circleSize - 10);
                using (SolidBrush fill = new SolidBrush(AppColors.Accent))
                {
                    g.FillEllipse(fill, inner);
                }
            }
            else
            {
                // Empty ring - grey
                using (Pen borderPen = new Pen(Color.FromArgb(51, 65, 85), 2))
                {
                    g.DrawEllipse(borderPen, circleRect);
                }
            }

            // Draw text to the right of the circle
            int textX = circleX + circleSize + 8;
            int textY = (Height - Font.Height) / 2;
            TextRenderer.DrawText(g, this.Text, this.Font, new Point(textX, textY), this.ForeColor);
        }
    }

    // --- Custom Gradient Button ---
    public class GradientButton : Button
    {
        private bool _isPrimary;
        public bool IsPrimary 
        { 
            get { return _isPrimary; }
            set { _isPrimary = value; Invalidate(); }
        }

        public GradientButton()
        {
            _isPrimary = true;
            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderSize = 0;
            FlatAppearance.MouseOverBackColor = Color.Transparent;
            FlatAppearance.MouseDownBackColor = Color.Transparent;
            Font = new Font("Segoe UI", 13, FontStyle.Bold);
            Cursor = Cursors.Hand;
            this.SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(AppColors.Card);

            Rectangle r = ClientRectangle;
            r.Width -= 1; r.Height -= 1;

            using (GraphicsPath path = GfxHelper.RoundedRect(r, 20))
            {
                if (_isPrimary)
                {
                    using (LinearGradientBrush brush = new LinearGradientBrush(r, AppColors.Accent, AppColors.AccentAlt, 0f))
                    {
                        g.FillPath(brush, path);
                    }
                    TextRenderer.DrawText(g, Text, Font, r, AppColors.Bg, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
                }
                else
                {
                    using (SolidBrush brush = new SolidBrush(Color.FromArgb(15, 255, 255, 255)))
                    using (Pen pen = new Pen(AppColors.Border))
                    {
                        g.FillPath(brush, path);
                        g.DrawPath(pen, path);
                    }
                    TextRenderer.DrawText(g, Text, Font, r, AppColors.SubText, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
                }
            }
        }
    }

    // --- Custom Game Cell Button ---
    public class GameButton : Button
    {
        private bool _isWin;
        private bool _isLose;

        public bool IsWin
        {
            get { return _isWin; }
            set { _isWin = value; Invalidate(); }
        }
        public bool IsLose
        {
            get { return _isLose; }
            set { _isLose = value; Invalidate(); }
        }

        public GameButton()
        {
            _isWin = false;
            _isLose = false;
            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderSize = 0;
            FlatAppearance.MouseOverBackColor = Color.Transparent;
            FlatAppearance.MouseDownBackColor = Color.Transparent;
            Font = new Font("Segoe UI", 36, FontStyle.Bold);
            Cursor = Cursors.Hand;
            Size = new Size(100, 100);
            this.SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(AppColors.Card);

            Color bg = _isWin ? AppColors.Win : (_isLose ? AppColors.Lose : AppColors.Cell);
            Color textCol = (_isWin || _isLose) ? Color.FromArgb(6, 78, 59) : AppColors.Accent;

            Rectangle r = ClientRectangle;
            r.Width -= 1; r.Height -= 1;

            using (GraphicsPath path = GfxHelper.RoundedRect(r, 16))
            using (SolidBrush brush = new SolidBrush(bg))
            {
                g.FillPath(brush, path);
                if (!_isWin && !_isLose)
                {
                    using (Pen pen = new Pen(Color.FromArgb(20, 255, 255, 255), 2))
                    {
                        g.DrawPath(pen, path);
                    }
                }
            }

            TextRenderer.DrawText(g, Text, Font, r, textCol, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
        }
    }

    // --- Rounded Container Panel ---
    public class RoundedPanel : Panel
    {
        public RoundedPanel()
        {
            this.DoubleBuffered = true;
            this.BackColor = AppColors.Card;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;

            Rectangle r = ClientRectangle;
            r.Width -= 1; r.Height -= 1;

            using (GraphicsPath path = GfxHelper.RoundedRect(r, 30))
            using (SolidBrush brush = new SolidBrush(AppColors.Card))
            using (Pen pen = new Pen(AppColors.Border, 1))
            {
                g.FillPath(brush, path);
                g.DrawPath(pen, path);
            }
        }
    }

    // --- MAIN FORM ---
    public class MainForm : Form
    {
        // Pages
        private Panel settingsPage;
        private Panel gamePage;

        // Settings radio buttons (each group in its own Panel for proper grouping)
        private CircleRadioButton rx, ro, ry, rc, re, rm, rh;

        // Game elements
        private Label statusLbl;
        private List<GameButton> gridBtns;
        private GradientButton playAgainBtn, backBtn;
        private GradientButton soundBtn;

        // Game state
        private string[] board;
        private string playerMarker;
        private string cpuMarker;
        private string turn;
        private int difficulty;  // 0=Easy, 1=Med, 2=Hard
        private bool gameOver;
        private bool soundEnabled;
        private Random rng;

        public MainForm()
        {
            gridBtns = new List<GameButton>();
            board = new string[9];
            playerMarker = "X";
            cpuMarker = "O";
            turn = "X";
            difficulty = 1;
            gameOver = false;
            soundEnabled = true;
            rng = new Random();

            this.Text = "Tic Tac Toe";
            this.BackColor = AppColors.Bg;
            this.AutoScaleMode = AutoScaleMode.Dpi;
            this.ClientSize = new Size(460, 720);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.DoubleBuffered = true;

            BuildUI();
        }

        private void BuildUI()
        {
            // Container
            RoundedPanel container = new RoundedPanel();
            container.Size = new Size(420, 680);
            container.Location = new Point((ClientSize.Width - 420) / 2, (ClientSize.Height - 680) / 2);
            Controls.Add(container);

            // Settings Page
            settingsPage = new Panel();
            settingsPage.Size = container.Size;
            settingsPage.Location = new Point(0, 0);
            settingsPage.BackColor = Color.Transparent;
            container.Controls.Add(settingsPage);

            BuildSettingsPage();

            // Game Page
            gamePage = new Panel();
            gamePage.Size = container.Size;
            gamePage.Location = new Point(0, 0);
            gamePage.BackColor = Color.Transparent;
            gamePage.Visible = false;
            container.Controls.Add(gamePage);

            BuildGamePage();
        }

        private void BuildSettingsPage()
        {
            int y = 25;

            // Title
            Label title = new Label();
            title.Text = "Tic Tac Toe";
            title.Font = new Font("Segoe UI", 26, FontStyle.Bold);
            title.ForeColor = AppColors.Accent;
            title.AutoSize = true;
            title.BackColor = Color.Transparent;
            settingsPage.Controls.Add(title);
            title.Location = new Point((420 - title.PreferredWidth) / 2, y);
            y += 70;

            // --- Choose Side ---
            AddSectionLabel(settingsPage, "CHOOSE SIDE", 40, y);
            y += 25;

            // GroupBox-like panel for side (so radio buttons are grouped together)
            Panel sidePanel = new Panel();
            sidePanel.BackColor = AppColors.Card;
            sidePanel.Location = new Point(30, y);
            sidePanel.Size = new Size(360, 45);
            settingsPage.Controls.Add(sidePanel);

            rx = new CircleRadioButton();
            rx.Text = "Play as X";
            rx.Checked = true;
            rx.Location = new Point(5, 2);
            rx.Size = new Size(160, 40);
            sidePanel.Controls.Add(rx);

            ro = new CircleRadioButton();
            ro.Text = "Play as O";
            ro.Location = new Point(180, 2);
            ro.Size = new Size(160, 40);
            sidePanel.Controls.Add(ro);
            y += 60;

            // --- Who Starts ---
            AddSectionLabel(settingsPage, "WHO STARTS?", 40, y);
            y += 25;

            Panel orderPanel = new Panel();
            orderPanel.BackColor = AppColors.Card;
            orderPanel.Location = new Point(30, y);
            orderPanel.Size = new Size(360, 45);
            settingsPage.Controls.Add(orderPanel);

            ry = new CircleRadioButton();
            ry.Text = "You";
            ry.Checked = true;
            ry.Location = new Point(5, 2);
            ry.Size = new Size(160, 40);
            orderPanel.Controls.Add(ry);

            rc = new CircleRadioButton();
            rc.Text = "CPU";
            rc.Location = new Point(180, 2);
            rc.Size = new Size(160, 40);
            orderPanel.Controls.Add(rc);
            y += 60;

            // --- Difficulty ---
            AddSectionLabel(settingsPage, "DIFFICULTY", 40, y);
            y += 25;

            Panel diffPanel = new Panel();
            diffPanel.BackColor = AppColors.Card;
            diffPanel.Location = new Point(30, y);
            diffPanel.Size = new Size(360, 45);
            settingsPage.Controls.Add(diffPanel);

            re = new CircleRadioButton();
            re.Text = "Easy";
            re.Location = new Point(5, 2);
            re.Size = new Size(110, 40);
            diffPanel.Controls.Add(re);

            rm = new CircleRadioButton();
            rm.Text = "Med";
            rm.Checked = true;
            rm.Location = new Point(125, 2);
            rm.Size = new Size(110, 40);
            diffPanel.Controls.Add(rm);

            rh = new CircleRadioButton();
            rh.Text = "Hard";
            rh.Location = new Point(245, 2);
            rh.Size = new Size(110, 40);
            diffPanel.Controls.Add(rh);
            y += 80;

            // Start Button
            GradientButton startBtn = new GradientButton();
            startBtn.Text = "Start Game";
            startBtn.IsPrimary = true;
            startBtn.Size = new Size(340, 55);
            startBtn.Location = new Point(40, y);
            startBtn.Click += new EventHandler(delegate(object s, EventArgs ev) { StartGame(); });
            settingsPage.Controls.Add(startBtn);
            y += 70;

            // Sound Button
            soundBtn = new GradientButton();
            soundBtn.Text = "Sound: ON";
            soundBtn.IsPrimary = false;
            soundBtn.Size = new Size(340, 50);
            soundBtn.Location = new Point(40, y);
            soundBtn.Click += new EventHandler(delegate(object s, EventArgs ev) {
                soundEnabled = !soundEnabled;
                soundBtn.Text = soundEnabled ? "Sound: ON" : "Sound: OFF";
                soundBtn.Invalidate();
            });
            settingsPage.Controls.Add(soundBtn);
        }

        private void BuildGamePage()
        {
            // Status Label
            statusLbl = new Label();
            statusLbl.Text = "Your Turn";
            statusLbl.Font = new Font("Segoe UI", 16, FontStyle.Bold);
            statusLbl.ForeColor = Color.FromArgb(226, 232, 240);
            statusLbl.BackColor = Color.Transparent;
            statusLbl.AutoSize = false;
            statusLbl.TextAlign = ContentAlignment.MiddleCenter;
            statusLbl.Size = new Size(400, 40);
            statusLbl.Location = new Point(10, 15);
            gamePage.Controls.Add(statusLbl);

            // Grid
            int startX = 45;
            int startY = 70;
            int gap = 12;
            int cellSize = 100;

            for (int i = 0; i < 9; i++)
            {
                GameButton btn = new GameButton();
                btn.Tag = i;
                btn.Size = new Size(cellSize, cellSize);
                btn.Location = new Point(startX + (i % 3) * (cellSize + gap), startY + (i / 3) * (cellSize + gap));
                btn.Click += new EventHandler(Cell_Click);
                gridBtns.Add(btn);
                gamePage.Controls.Add(btn);
            }

            // End-game buttons
            int btnY = startY + 3 * (cellSize + gap) + 20;

            playAgainBtn = new GradientButton();
            playAgainBtn.Text = "Play Again";
            playAgainBtn.IsPrimary = true;
            playAgainBtn.Size = new Size(340, 55);
            playAgainBtn.Location = new Point(40, btnY);
            playAgainBtn.Visible = false;
            playAgainBtn.Click += new EventHandler(delegate(object s, EventArgs ev) { StartGame(); });
            gamePage.Controls.Add(playAgainBtn);

            backBtn = new GradientButton();
            backBtn.Text = "Back to Settings";
            backBtn.IsPrimary = false;
            backBtn.Size = new Size(340, 50);
            backBtn.Location = new Point(40, btnY + 65);
            backBtn.Visible = false;
            backBtn.Click += new EventHandler(delegate(object s, EventArgs ev) {
                gamePage.Visible = false;
                settingsPage.Visible = true;
                settingsPage.BringToFront();
            });
            gamePage.Controls.Add(backBtn);
        }

        private void AddSectionLabel(Panel parent, string text, int x, int y)
        {
            Label l = new Label();
            l.Text = text;
            l.Font = new Font("Segoe UI", 8, FontStyle.Bold);
            l.ForeColor = AppColors.SectionText;
            l.BackColor = Color.Transparent;
            l.AutoSize = true;
            l.Location = new Point(x, y);
            parent.Controls.Add(l);
        }

        // --- GAME LOGIC ---

        private void StartGame()
        {
            playerMarker = rx.Checked ? "X" : "O";
            cpuMarker = (playerMarker == "X") ? "O" : "X";

            if (re.Checked) difficulty = 0;
            else if (rh.Checked) difficulty = 2;
            else difficulty = 1;

            turn = ry.Checked ? playerMarker : cpuMarker;

            for (int i = 0; i < 9; i++)
            {
                board[i] = null;
                gridBtns[i].Text = "";
                gridBtns[i].Enabled = true;
                gridBtns[i].IsWin = false;
                gridBtns[i].IsLose = false;
            }

            gameOver = false;
            statusLbl.ForeColor = Color.FromArgb(226, 232, 240);
            statusLbl.Text = "";
            playAgainBtn.Visible = false;
            backBtn.Visible = false;

            settingsPage.Visible = false;
            gamePage.Visible = true;
            gamePage.BringToFront();
            UpdateStatus();

            if (turn == cpuMarker)
            {
                Task.Factory.StartNew(delegate() {
                    Thread.Sleep(800);
                    this.Invoke(new Action(CpuMove));
                });
            }
        }

        private void Cell_Click(object sender, EventArgs e)
        {
            if (gameOver || turn != playerMarker) return;
            GameButton btn = (GameButton)sender;
            int idx = (int)btn.Tag;
            if (board[idx] != null) return;

            MakeMove(idx, playerMarker);
            if (!CheckEnd())
            {
                turn = cpuMarker;
                UpdateStatus();
                Task.Factory.StartNew(delegate() {
                    Thread.Sleep(600);
                    this.Invoke(new Action(CpuMove));
                });
            }
        }

        private void CpuMove()
        {
            if (gameOver) return;
            int idx = -1;

            if (difficulty == 2) idx = GetBestMove();
            else if (difficulty == 0) idx = GetWorstMove();
            else idx = (rng.Next(10) > 4) ? GetBestMove() : GetRandomMove();

            if (idx == -1) idx = GetRandomMove();
            if (idx == -1) return;

            MakeMove(idx, cpuMarker);
            if (!CheckEnd())
            {
                turn = playerMarker;
                UpdateStatus();
            }
        }

        private void MakeMove(int idx, string who)
        {
            board[idx] = who;
            gridBtns[idx].Text = who;
            gridBtns[idx].Enabled = false;
            gridBtns[idx].Invalidate();
            PlaySound("tap");
        }

        private void UpdateStatus()
        {
            if (gameOver) return;
            statusLbl.Text = (turn == playerMarker) ? "Your Turn" : "Computer Thinking...";
        }

        private bool CheckEnd()
        {
            int[,] wins = new int[,] { {0,1,2}, {3,4,5}, {6,7,8}, {0,3,6}, {1,4,7}, {2,5,8}, {0,4,8}, {2,4,6} };

            for (int i = 0; i < 8; i++)
            {
                int a = wins[i, 0], b = wins[i, 1], c = wins[i, 2];
                if (board[a] != null && board[a] == board[b] && board[b] == board[c])
                {
                    gameOver = true;
                    bool isPlayerWin = (board[a] == playerMarker);
                    int[] combo = new int[] { a, b, c };
                    foreach (int idx in combo)
                    {
                        if (isPlayerWin) gridBtns[idx].IsWin = true;
                        else gridBtns[idx].IsLose = true;
                    }

                    statusLbl.Text = isPlayerWin ? "Victory!" : "Defeat!";
                    statusLbl.ForeColor = isPlayerWin ? AppColors.Win : AppColors.Lose;
                    PlaySound(isPlayerWin ? "win" : "lose");
                    playAgainBtn.Visible = true;
                    backBtn.Visible = true;
                    return true;
                }
            }

            bool full = true;
            foreach (string s in board) if (s == null) full = false;
            if (full)
            {
                gameOver = true;
                statusLbl.Text = "It's a Draw!";
                playAgainBtn.Visible = true;
                backBtn.Visible = true;
                return true;
            }

            return false;
        }

        private void PlaySound(string type)
        {
            if (!soundEnabled) return;
            Task.Factory.StartNew(delegate() {
                if (type == "tap") { Console.Beep(700, 80); }
                else if (type == "win") { Console.Beep(523, 120); Thread.Sleep(30); Console.Beep(659, 120); Thread.Sleep(30); Console.Beep(784, 120); Thread.Sleep(30); Console.Beep(1046, 120); }
                else if (type == "lose") { Console.Beep(200, 250); Thread.Sleep(60); Console.Beep(150, 250); Thread.Sleep(60); Console.Beep(100, 250); }
            });
        }

        // --- AI ---
        private int Minimax(string[] bd, int depth, bool isMax)
        {
            if (CheckWinLogic(bd, cpuMarker)) return 10 - depth;
            if (CheckWinLogic(bd, playerMarker)) return depth - 10;
            if (IsFull(bd)) return 0;

            if (isMax)
            {
                int best = -1000;
                for (int i = 0; i < 9; i++)
                {
                    if (bd[i] == null)
                    {
                        bd[i] = cpuMarker;
                        int val = Minimax(bd, depth + 1, false);
                        if (val > best) best = val;
                        bd[i] = null;
                    }
                }
                return best;
            }
            else
            {
                int best = 1000;
                for (int i = 0; i < 9; i++)
                {
                    if (bd[i] == null)
                    {
                        bd[i] = playerMarker;
                        int val = Minimax(bd, depth + 1, true);
                        if (val < best) best = val;
                        bd[i] = null;
                    }
                }
                return best;
            }
        }

        private int GetBestMove()
        {
            int bestVal = -1000;
            int bestMove = -1;
            for (int i = 0; i < 9; i++)
            {
                if (board[i] == null)
                {
                    board[i] = cpuMarker;
                    int moveVal = Minimax(board, 0, false);
                    board[i] = null;
                    if (moveVal > bestVal) { bestMove = i; bestVal = moveVal; }
                }
            }
            return bestMove;
        }

        private int GetWorstMove()
        {
            int worstVal = 1000;
            int worstMove = -1;
            for (int i = 0; i < 9; i++)
            {
                if (board[i] == null)
                {
                    board[i] = cpuMarker;
                    int moveVal = Minimax(board, 0, false);
                    board[i] = null;
                    if (moveVal < worstVal) { worstMove = i; worstVal = moveVal; }
                }
            }
            return (worstMove != -1) ? worstMove : GetRandomMove();
        }

        private int GetRandomMove()
        {
            List<int> empty = new List<int>();
            for (int i = 0; i < 9; i++) if (board[i] == null) empty.Add(i);
            return (empty.Count > 0) ? empty[rng.Next(empty.Count)] : -1;
        }

        private bool CheckWinLogic(string[] bd, string who)
        {
            int[,] wins = new int[,] { {0,1,2}, {3,4,5}, {6,7,8}, {0,3,6}, {1,4,7}, {2,5,8}, {0,4,8}, {2,4,6} };
            for (int i = 0; i < 8; i++)
            {
                if (bd[wins[i, 0]] == who && bd[wins[i, 1]] == who && bd[wins[i, 2]] == who) return true;
            }
            return false;
        }

        private bool IsFull(string[] bd)
        {
            foreach (string s in bd) if (s == null) return false;
            return true;
        }
    }

    static class Program
    {
        [DllImport("user32.dll")]
        private static extern bool SetProcessDPIAware();

        [STAThread]
        static void Main()
        {
            SetProcessDPIAware();
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
