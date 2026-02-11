import sys
import random
import time
import threading
import winsound
from PySide6.QtWidgets import (QApplication, QMainWindow, QWidget, QVBoxLayout, 
                             QHBoxLayout, QLabel, QPushButton, QRadioButton, 
                             QButtonGroup, QStackedWidget, QGridLayout, QFrame,
                             QGraphicsDropShadowEffect)
from PySide6.QtCore import Qt, QTimer, QRect, QSize
from PySide6.QtGui import QFont, QColor, QPainter, QPen, QBrush

# --- PREMIUM THEME ---
COLORS = {
    "bg": "#020617",       
    "card": "#0f172a",     
    "cell": "rgba(30, 41, 59, 0.6)", 
    "accent": "#22d3ee",   
    "accent_alt": "#818cf8", 
    "text": "#f8fafc",     
    "win": "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #10b981, stop:1 #34d399)",
    "lose": "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #f43f5e, stop:1 #fb7185)",
}

# --- ADVANCED MODERN STYLES ---
STYLE_SHEET = f"""
QMainWindow {{
    background-color: {COLORS["bg"]};
}}

#container {{
    background-color: {COLORS["card"]};
    border-radius: 35px;
    border: 1px solid rgba(255, 255, 255, 0.1);
}}

QLabel#title {{
    font-size: 34px;
    font-weight: 900;
    color: {COLORS["accent"]};
    letter-spacing: -1.5px;
    margin-bottom: 20px;
}}

QLabel#status {{
    font-size: 18px;
    font-weight: 700;
    color: #e2e8f0;
    margin-bottom: 20px;
}}

#sectionLabel {{
    font-size: 11px;
    font-weight: 800;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 2px;
}}

QRadioButton {{
    color: #cbd5e1;
    font-size: 14px;
    font-weight: 600;
    spacing: 10px;
    padding: 12px;
    border-radius: 15px;
    background: rgba(255, 255, 255, 0.03);
}}

QRadioButton::indicator {{
    width: 0px;
    height: 0px;
}}

#primaryBtn {{
    background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 {COLORS["accent"]}, stop:1 {COLORS["accent_alt"]});
    color: #020617;
    border-radius: 20px;
    font-size: 18px;
    font-weight: 800;
    padding: 18px;
}}

#secondaryBtn {{
    background-color: rgba(255, 255, 255, 0.05);
    color: #cbd5e1;
    border-radius: 18px;
    font-size: 14px;
    font-weight: 700;
    padding: 15px;
    border: 1px solid rgba(255, 255, 255, 0.1);
}}

#cellBtn {{
    background-color: {COLORS["cell"]};
    color: {COLORS["accent"]};
    border-radius: 16px;
    font-size: 48px;
    font-weight: 900;
    border: 2px solid rgba(255, 255, 255, 0.05);
}}

#cellBtn:hover {{
    background-color: rgba(51, 65, 85, 0.8);
    border: 2px solid {COLORS["accent"]};
}}

#cellBtn:disabled {{
    background-color: rgba(15, 23, 42, 0.7);
    color: {COLORS["accent"]};
    border: none;
}}

#cellBtn[win="true"] {{
    background: {COLORS["win"]};
    color: #064e3b;
    border: none;
    border-radius: 16px;
}}
"""

# --- CUSTOM CIRCULAR RADIO BUTTON ---
class CircleRadioButton(QRadioButton):
    """A QRadioButton that paints its own perfectly circular indicator."""
    def __init__(self, text, parent=None):
        super().__init__(text, parent)
        self._circle_size = 22
        self._border_color = QColor("#334155")
        self._checked_color = QColor(COLORS["accent"])
        self._bg_color = QColor(COLORS["card"])

    def paintEvent(self, event):
        super().paintEvent(event)  # Draw the text & background via stylesheet
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing, True)

        # Position the circle on the left side
        y = (self.height() - self._circle_size) // 2
        x = 12  # Left padding
        rect = QRect(x, y, self._circle_size, self._circle_size)

        if self.isChecked():
            # Outer ring
            painter.setPen(QPen(self._checked_color, 2))
            painter.setBrush(QBrush(self._bg_color))
            painter.drawEllipse(rect)
            # Inner filled circle
            inner = rect.adjusted(5, 5, -5, -5)
            painter.setPen(Qt.NoPen)
            painter.setBrush(QBrush(self._checked_color))
            painter.drawEllipse(inner)
        else:
            # Empty ring
            painter.setPen(QPen(self._border_color, 2))
            painter.setBrush(Qt.NoBrush)
            painter.drawEllipse(rect)

        painter.end()

class TicTacToe(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Tic Tac Toe")
        # Increased height and width slightly to prevent truncation
        # Auto-fit to screen
        screen = QApplication.primaryScreen().availableGeometry()
        max_h = screen.height() - 80  # Taskbar margin
        win_h = min(780, max_h)
        win_w = min(500, int(win_h * 0.65))
        self.cell_size = min(100, (win_w - 140) // 3)  # Scale cells to fit
        self.setFixedSize(win_w, win_h)
        self.setStyleSheet(STYLE_SHEET)
        self.setAttribute(Qt.WA_TranslucentBackground)

        # Center on screen
        x = (screen.width() - win_w) // 2
        y = (screen.height() - win_h) // 2
        self.move(x, y)

        # State initialization (Logic is identical)
        self.board = [None] * 9
        self.player_marker = "X"
        self.computer_marker = "O"
        self.turn = "X"
        self.difficulty = "medium"
        self.game_over = False
        self.sound_enabled = True

        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.main_layout = QVBoxLayout(self.central_widget)
        self.main_layout.setContentsMargins(20, 20, 20, 20)

        self.container = QFrame()
        self.container.setObjectName("container")
        self.container_layout = QVBoxLayout(self.container)
        margin = max(15, int(self.cell_size * 0.3))
        self.container_layout.setContentsMargins(margin, margin, margin, margin)
        self.main_layout.addWidget(self.container)

        shadow = QGraphicsDropShadowEffect()
        shadow.setBlurRadius(50)
        shadow.setXOffset(0)
        shadow.setYOffset(25)
        shadow.setColor(QColor(0, 0, 0, 200))
        self.container.setGraphicsEffect(shadow)

        self.stack = QStackedWidget()
        self.container_layout.addWidget(self.stack)

        self.init_settings_screen()
        self.init_game_screen()
        self.stack.setCurrentIndex(0)

    # --- AUDIO ENGINE (Unchanged winsound logic) ---
    def play_tone(self, freq, duration):
        if not self.sound_enabled: return
        threading.Thread(target=lambda: winsound.Beep(int(freq), int(duration)), daemon=True).start()

    def sfx_tap(self): self.play_tone(700, 80)
    def sfx_win(self):
        def seq():
            for n in [523, 659, 784, 1046]:
                if not self.sound_enabled: break
                winsound.Beep(n, 120); time.sleep(0.03)
        threading.Thread(target=seq, daemon=True).start()
    def sfx_lose(self):
        def seq():
            for n in [200, 150, 100]:
                if not self.sound_enabled: break
                winsound.Beep(n, 250); time.sleep(0.06)
        threading.Thread(target=seq, daemon=True).start()

    # --- UI LAYOUTS ---
    def init_settings_screen(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setSpacing(15)

        title = QLabel("Tic Tac Toe")
        title.setObjectName("title")
        title.setAlignment(Qt.AlignCenter)
        layout.addWidget(title)

        layout.addWidget(self.create_lbl("Choose Side"))
        side_l = QHBoxLayout()
        self.side_grp = QButtonGroup(self)
        self.rx, self.ro = CircleRadioButton("    Play as X"), CircleRadioButton("    Play as O")
        self.rx.setChecked(True)
        for r in [self.rx, self.ro]: self.side_grp.addButton(r); side_l.addWidget(r)
        layout.addLayout(side_l)

        layout.addWidget(self.create_lbl("Who Starts?"))
        ord_l = QHBoxLayout()
        self.ord_grp = QButtonGroup(self)
        self.ry, self.rc = CircleRadioButton("    You"), CircleRadioButton("    CPU")
        self.ry.setChecked(True)
        for r in [self.ry, self.rc]: self.ord_grp.addButton(r); ord_l.addWidget(r)
        layout.addLayout(ord_l)

        layout.addWidget(self.create_lbl("Difficulty"))
        diff_l = QHBoxLayout()
        self.diff_grp = QButtonGroup(self)
        self.re, self.rm, self.rh = CircleRadioButton("    Easy"), CircleRadioButton("    Med"), CircleRadioButton("    Hard")
        self.rm.setChecked(True)
        for r in [self.re, self.rm, self.rh]: self.diff_grp.addButton(r); diff_l.addWidget(r)
        layout.addLayout(diff_l)

        layout.addSpacing(30)
        start = QPushButton("Start Game")
        start.setObjectName("primaryBtn")
        start.setCursor(Qt.PointingHandCursor)
        start.clicked.connect(self.start_game)
        layout.addWidget(start)

        self.snd_btn = QPushButton("🔊 Sound: ON")
        self.snd_btn.setObjectName("secondaryBtn")
        self.snd_btn.clicked.connect(self.toggle_sound)
        layout.addWidget(self.snd_btn)
        self.stack.addWidget(widget)

    def init_game_screen(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)

        self.stat_lbl = QLabel("Your Turn")
        self.stat_lbl.setObjectName("status")
        self.stat_lbl.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.stat_lbl)

        grid = QGridLayout()
        grid.setSpacing(max(8, self.cell_size // 8))
        self.btns = []
        for i in range(9):
            b = QPushButton("")
            b.setObjectName("cellBtn")
            b.setFixedSize(self.cell_size, self.cell_size)
            b.setCursor(Qt.PointingHandCursor)
            b.setStyleSheet(f"font-size: {max(24, self.cell_size // 2)}px; border-radius: 16px;")
            b.clicked.connect(lambda chk=False, idx=i: self.player_action(idx))
            grid.addWidget(b, i // 3, i % 3)
            self.btns.append(b)
        layout.addLayout(grid)

        # Buttons that appear after game over
        self.play_again_btn = QPushButton("Play Again")
        self.play_again_btn.setObjectName("primaryBtn")
        self.play_again_btn.setCursor(Qt.PointingHandCursor)
        self.play_again_btn.clicked.connect(self.start_game)
        self.play_again_btn.hide()
        layout.addWidget(self.play_again_btn)

        self.back_btn = QPushButton("Back to Settings")
        self.back_btn.setObjectName("secondaryBtn")
        self.back_btn.setCursor(Qt.PointingHandCursor)
        self.back_btn.clicked.connect(self.show_settings)
        self.back_btn.hide()
        layout.addWidget(self.back_btn)

        self.stack.addWidget(widget)

    def create_lbl(self, t):
        l = QLabel(t); l.setObjectName("sectionLabel"); return l

    def toggle_sound(self):
        self.sound_enabled = not self.sound_enabled
        self.snd_btn.setText("🔊 Sound: ON" if self.sound_enabled else "🔇 Sound: OFF")

    def show_settings(self): self.stack.setCurrentIndex(0)

    def start_game(self):
        self.player_marker = "X" if self.rx.isChecked() else "O"
        self.computer_marker = "O" if self.player_marker == "X" else "X"
        self.difficulty = "easy" if self.re.isChecked() else "hard" if self.rh.isChecked() else "medium"
        self.turn = self.player_marker if self.ry.isChecked() else self.computer_marker
        self.board = [None] * 9
        self.game_over = False
        for b in self.btns:
            b.setText(""); b.setEnabled(True); b.setProperty("win", "false")
            b.style().unpolish(b); b.style().polish(b)
        self.stack.setCurrentIndex(1)
        self.stat_lbl.setStyleSheet("")
        self.play_again_btn.hide()
        self.back_btn.hide()
        self.update_status()
        if self.turn == self.computer_marker: QTimer.singleShot(800, self.cpu_move)

    def player_action(self, idx):
        if self.game_over or self.board[idx] or self.turn != self.player_marker: return
        self.make_move(idx, self.player_marker)
        if not self.check_end(self.player_marker):
            self.turn = self.computer_marker; self.update_status()
            QTimer.singleShot(700, self.cpu_move)

    def cpu_move(self):
        if self.game_over: return
        idx = self.get_best() if self.difficulty == "hard" else (self.get_best() if random.random() > 0.4 else self.get_random()) if self.difficulty == "medium" else self.get_worst()
        self.make_move(idx, self.computer_marker)
        if not self.check_end(self.computer_marker):
            self.turn = self.player_marker; self.update_status()

    def make_move(self, idx, who):
        self.board[idx] = who; self.btns[idx].setText(who); self.btns[idx].setEnabled(False); self.sfx_tap()

    def update_status(self):
        if self.game_over: return
        self.stat_lbl.setText("Your Turn" if self.turn == self.player_marker else "Computer Thinking...")

    def check_end(self, last):
        wins = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]]
        for c in wins:
            if all(self.board[i] == last for i in c):
                self.game_over = True
                for i in c:
                    self.btns[i].setProperty("win", "true"); self.btns[i].style().unpolish(self.btns[i]); self.btns[i].style().polish(self.btns[i])
                msg = "Victory! 🎉" if last == self.player_marker else "Defeat 🤖"
                self.stat_lbl.setText(msg)
                self.stat_lbl.setStyleSheet(f"color: {COLORS['win'] if last == self.player_marker else COLORS['lose']}; font-weight: bold; font-size: 22px;")
                if last == self.player_marker: self.sfx_win()
                else: self.sfx_lose()
                self.show_game_over_buttons()
                return True
        if None not in self.board:
            self.game_over = True; self.stat_lbl.setText("It's a Draw 😐")
            self.show_game_over_buttons()
            return True
        return False

    def show_game_over_buttons(self):
        self.play_again_btn.show()
        self.back_btn.show()

    def get_random(self):
        e = [i for i, v in enumerate(self.board) if v is None]
        return random.choice(e) if e else -1
    def get_worst(self):
        e = [i for i, v in enumerate(self.board) if v is None]
        if len(e) >= 8: return self.get_random()
        ws = float('inf'); bi = -1
        for i in e:
            self.board[i] = self.computer_marker
            s = self.minimax(self.board, 0, False); self.board[i] = None
            if s < ws: ws, bi = s, i
        return bi if bi != -1 else self.get_random()
    def get_best(self):
        e = [i for i, v in enumerate(self.board) if v is None]
        if len(e) == 9: return 4
        bs = -float('inf'); bi = -1
        for i in e:
            self.board[i] = self.computer_marker; s = self.minimax(self.board, 0, False); self.board[i] = None
            if s > bs: bs, bi = s, i
        return bi
    def minimax(self, bd, d, is_m):
        if self.win_chk(bd, self.computer_marker): return 10 - d
        if self.win_chk(bd, self.player_marker): return d - 10
        if None not in bd: return 0
        e = [i for i, v in enumerate(bd) if v is None]
        if is_m:
            me = -float('inf')
            for i in e: bd[i] = self.computer_marker; me = max(me, self.minimax(bd, d + 1, False)); bd[i] = None
            return me
        else:
            mi = float('inf')
            for i in e: bd[i] = self.player_marker; mi = min(mi, self.minimax(bd, d + 1, True)); bd[i] = None
            return mi
    def win_chk(self, bd, w):
        wins = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]]
        return any(all(bd[i] == w for i in c) for c in wins)

if __name__ == "__main__":
    app = QApplication(sys.argv)
    app.setStyle("Fusion") # Force modern, customizable style
    game = TicTacToe()
    game.show()
    sys.exit(app.exec())
