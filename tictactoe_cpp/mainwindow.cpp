#include "mainwindow.h"
#include <QApplication>
#include <QEvent>
#include <QGraphicsDropShadowEffect>
#include <QHBoxLayout>
#include <QPropertyAnimation>
#include <QRandomGenerator>
#include <QScreen>
#include <QStyle>
#include <QVBoxLayout>
#include <thread>
#include <windows.h> // For Beep()

// --- THEME ---
const QString COLORS_bg = "#020617";
const QString COLORS_card = "#0f172a";
const QString COLORS_cell = "rgba(30, 41, 59, 0.6)";
const QString COLORS_accent = "#22d3ee";
const QString COLORS_accent_alt = "#818cf8";
const QString COLORS_text = "#f8fafc";
const QString COLORS_win =
    "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #10b981, stop:1 #34d399)";
const QString COLORS_lose =
    "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #f43f5e, stop:1 #fb7185)";

// Helper: Custom Style
const QString STYLE_SHEET = R"(
    QMainWindow { background-color: #020617; }
    #container { background-color: #0f172a; border-radius: 35px; border: 1px solid rgba(255, 255, 255, 0.1); }
    QLabel#title { font-size: 34px; font-weight: 900; color: #22d3ee; margin-bottom: 20px; }
    QRadioButton { color: #cbd5e1; font-size: 14px; spacing: 10px; padding: 12px; border-radius: 15px; background: rgba(255, 255, 255, 0.03); }
    QRadioButton::indicator { width: 0px; height: 0px; }
    #primaryBtn { background: qlineargradient(spread:pad, x1:0, y1:0, x2:1, y2:0, stop:0 #22d3ee, stop:1 #818cf8); color: #020617; border-radius: 20px; font-size: 18px; font-weight: 800; padding: 18px; }
    #secondaryBtn { background-color: rgba(255, 255, 255, 0.05); color: #cbd5e1; border-radius: 18px; font-size: 14px; font-weight: 700; padding: 15px; border: 1px solid rgba(255, 255, 255, 0.1); }
    #cellBtn { background-color: rgba(30, 41, 59, 0.6); color: #22d3ee; border-radius: 16px; font-size: 48px; font-weight: 900; border: 2px solid rgba(255, 255, 255, 0.05); }
    #cellBtn:hover { background-color: rgba(51, 65, 85, 0.8); border: 2px solid #22d3ee; }
    #cellBtn:disabled { background-color: rgba(15, 23, 42, 0.7); color: #22d3ee; border: none; }
    QLabel#sectionLabel { font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 2px; }
    QLabel#status { font-size: 18px; font-weight: 700; color: #e2e8f0; margin-bottom: 20px; }
)";

// --- CUSTOM RADIO Button Impl ---
CircleRadioButton::CircleRadioButton(const QString &text, QWidget *parent)
    : QRadioButton(text, parent) {}

void CircleRadioButton::paintEvent(QPaintEvent *event) {
  QRadioButton::paintEvent(
      event); // Let button draw text/background via stylesheet
  QPainter painter(this);
  painter.setRenderHint(QPainter::Antialiasing);

  int size = 22;
  int x = 12;
  int y = (height() - size) / 2;
  QRect rect(x, y, size, size);

  QColor border("#334155");
  QColor accent("#22d3ee");
  QColor card("#0f172a");

  if (isChecked()) {
    painter.setPen(QPen(accent, 2));
    painter.setBrush(card);
    painter.drawEllipse(rect);
    QRect inner = rect.adjusted(5, 5, -5, -5);
    painter.setPen(Qt::NoPen);
    painter.setBrush(accent);
    painter.drawEllipse(inner);
  } else {
    painter.setPen(QPen(border, 2));
    painter.setBrush(Qt::NoBrush);
    painter.drawEllipse(rect);
  }
}

// --- MainWindow Impl ---
MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent), gameOver(false), soundEnabled(true), turn("X"),
      playerMarker("X"), computerMarker("O") {
  setWindowTitle("Tic Tac Toe");

  // Auto-fit Logic
  QRect screen = QApplication::primaryScreen()->availableGeometry();
  int h = qMin(780, screen.height() - 80);
  int w = qMin(500, (int)(h * 0.65));
  setFixedSize(w, h); // Initial Size
  cellSize = qMin(100, (w - 140) / 3);

  // Center logic
  move((screen.width() - w) / 2, (screen.height() - h) / 2);

  setStyleSheet(STYLE_SHEET);
  setAttribute(Qt::WA_TranslucentBackground);

  centralWidget = new QWidget(this);
  setCentralWidget(centralWidget);
  QVBoxLayout *mainLayout = new QVBoxLayout(centralWidget);
  mainLayout->setContentsMargins(20, 20, 20, 20);

  QFrame *container = new QFrame();
  container->setObjectName("container");
  QVBoxLayout *contLayout = new QVBoxLayout(container);
  int m = qMax(15, (int)(cellSize * 0.3));
  contLayout->setContentsMargins(m, m, m, m);
  mainLayout->addWidget(container);

  // Shadow
  QGraphicsDropShadowEffect *shadow = new QGraphicsDropShadowEffect();
  shadow->setBlurRadius(50);
  shadow->setOffset(0, 25);
  shadow->setColor(QColor(0, 0, 0, 200));
  container->setGraphicsEffect(shadow);

  stack = new QStackedWidget();
  contLayout->addWidget(stack);

  initSettingsScreen();
  initGameScreen();
  stack->setCurrentIndex(0);
}

MainWindow::~MainWindow() {}

void MainWindow::resizeEvent(QResizeEvent *event) {
  // Basic responsive handling if needed
  QMainWindow::resizeEvent(event);
}

void MainWindow::initSettingsScreen() {
  QWidget *page = new QWidget();
  QVBoxLayout *layout = new QVBoxLayout(page);
  layout->setSpacing(15);

  QLabel *title = new QLabel("Tic Tac Toe");
  title->setObjectName("title");
  title->setAlignment(Qt::AlignCenter);
  layout->addWidget(title);

  // Side Selection
  QLabel *l1 = new QLabel("Choose Side");
  l1->setObjectName("sectionLabel");
  layout->addWidget(l1);
  QHBoxLayout *h1 = new QHBoxLayout();
  sideGrp = new QButtonGroup(this);
  rx = new CircleRadioButton("    Play as X");
  ro = new CircleRadioButton("    Play as O");
  rx->setChecked(true);
  sideGrp->addButton(rx);
  sideGrp->addButton(ro);
  h1->addWidget(rx);
  h1->addWidget(ro);
  layout->addLayout(h1);

  // Order Selection
  QLabel *l2 = new QLabel("Who Starts?");
  l2->setObjectName("sectionLabel");
  layout->addWidget(l2);
  QHBoxLayout *h2 = new QHBoxLayout();
  ordGrp = new QButtonGroup(this);
  ry = new CircleRadioButton("    You");
  rc = new CircleRadioButton("    CPU");
  ry->setChecked(true);
  ordGrp->addButton(ry);
  ordGrp->addButton(rc);
  h2->addWidget(ry);
  h2->addWidget(rc);
  layout->addLayout(h2);

  // Difficulty
  QLabel *l3 = new QLabel("Difficulty");
  l3->setObjectName("sectionLabel");
  layout->addWidget(l3);
  QHBoxLayout *h3 = new QHBoxLayout();
  diffGrp = new QButtonGroup(this);
  re = new CircleRadioButton("    Easy");
  rm = new CircleRadioButton("    Med");
  rh = new CircleRadioButton("    Hard");
  rm->setChecked(true);
  diffGrp->addButton(re);
  diffGrp->addButton(rm);
  diffGrp->addButton(rh);
  h3->addWidget(re);
  h3->addWidget(rm);
  h3->addWidget(rh);
  layout->addLayout(h3);

  layout->addSpacing(30);
  QPushButton *startBtn = new QPushButton("Start Game");
  startBtn->setObjectName("primaryBtn");
  startBtn->setCursor(Qt::PointingHandCursor);
  connect(startBtn, &QPushButton::clicked, this, &MainWindow::startGame);
  layout->addWidget(startBtn);

  soundBtn = new QPushButton("🔊 Sound: ON");
  soundBtn->setObjectName("secondaryBtn");
  soundBtn->setCursor(Qt::PointingHandCursor);
  connect(soundBtn, &QPushButton::clicked, this, &MainWindow::toggleSound);
  layout->addWidget(soundBtn);

  stack->addWidget(page);
}

void MainWindow::initGameScreen() {
  QWidget *page = new QWidget();
  QVBoxLayout *layout = new QVBoxLayout(page);

  statusLabel = new QLabel("Your Turn");
  statusLabel->setObjectName("status");
  statusLabel->setAlignment(Qt::AlignCenter);
  layout->addWidget(statusLabel);

  QGridLayout *grid = new QGridLayout();
  grid->setSpacing(qMax(8, cellSize / 8));

  for (int i = 0; i < 9; i++) {
    QPushButton *btn = new QPushButton("");
    btn->setObjectName("cellBtn");
    btn->setFixedSize(cellSize, cellSize);
    btn->setCursor(Qt::PointingHandCursor);
    // initial style
    QString baseStyle = QString("font-size: %1px; border-radius: 16px;")
                            .arg(qMax(24, cellSize / 2));
    btn->setStyleSheet(baseStyle);

    connect(btn, &QPushButton::clicked, [this, i]() { handleCellClick(i); });
    grid->addWidget(btn, i / 3, i % 3);
    cellBtns.append(btn);
  }
  layout->addLayout(grid);

  // End Game Buttons
  playAgainBtn = new QPushButton("Play Again");
  playAgainBtn->setObjectName("primaryBtn");
  playAgainBtn->setCursor(Qt::PointingHandCursor);
  connect(playAgainBtn, &QPushButton::clicked, this, &MainWindow::startGame);
  playAgainBtn->hide();
  layout->addWidget(playAgainBtn);

  backBtn = new QPushButton("Back to Settings");
  backBtn->setObjectName("secondaryBtn");
  backBtn->setCursor(Qt::PointingHandCursor);
  connect(backBtn, &QPushButton::clicked, this, &MainWindow::showSettings);
  backBtn->hide();
  layout->addWidget(backBtn);

  stack->addWidget(page);
}

void MainWindow::toggleSound() {
  soundEnabled = !soundEnabled;
  soundBtn->setText(soundEnabled ? "🔊 Sound: ON" : "🔇 Sound: OFF");
}

void MainWindow::startGame() {
  playerMarker = rx->isChecked() ? "X" : "O";
  computerMarker = (playerMarker == "X") ? "O" : "X";

  if (re->isChecked())
    difficulty = EASY;
  else if (rh->isChecked())
    difficulty = HARD;
  else
    difficulty = MEDIUM;

  turn = ry->isChecked() ? playerMarker : computerMarker;

  for (int i = 0; i < 9; i++) {
    board[i] = "";
    cellBtns[i]->setText("");
    cellBtns[i]->setEnabled(true);
    cellBtns[i]->setProperty("win", false);
    cellBtns[i]->style()->unpolish(cellBtns[i]);
    cellBtns[i]->style()->polish(cellBtns[i]);

    // Reset base style
    QString baseStyle = QString("font-size: %1px; border-radius: 16px;")
                            .arg(qMax(24, cellSize / 2));
    cellBtns[i]->setStyleSheet(baseStyle); // Apply base style
  }
  gameOver = false;
  stack->setCurrentIndex(1);

  playAgainBtn->hide();
  backBtn->hide();
  statusLabel->setStyleSheet("");
  updateStatus();

  if (turn == computerMarker) {
    QTimer::singleShot(800, this, &MainWindow::computerMove);
  }
}

void MainWindow::showSettings() { stack->setCurrentIndex(0); }

void MainWindow::playSound(const QString &type) {
  if (!soundEnabled)
    return;

  // Non-blocking simple Windows beep thread
  // Format: "freq:dur,freq:dur..."
  std::thread([type]() {
    if (type == "tap") {
      Beep(700, 80);
    } else if (type == "win") {
      Beep(523, 120);
      Sleep(30);
      Beep(659, 120);
      Sleep(30);
      Beep(784, 120);
      Sleep(30);
      Beep(1046, 120);
    } else if (type == "lose") {
      Beep(200, 250);
      Sleep(60);
      Beep(150, 250);
      Sleep(60);
      Beep(100, 250);
    }
  }).detach();
}

void MainWindow::handleCellClick(int idx) {
  if (gameOver || !board[idx].isEmpty() || turn != playerMarker)
    return;

  board[idx] = playerMarker;
  cellBtns[idx]->setText(playerMarker);
  cellBtns[idx]->setEnabled(false);
  playSound("tap");

  if (!checkWin(playerMarker)) {
    turn = computerMarker;
    updateStatus();
    QTimer::singleShot(700, this, &MainWindow::computerMove);
  }
}

void MainWindow::computerMove() {
  if (gameOver)
    return;
  int idx = -1;

  if (difficulty == HARD)
    idx = getBestMove();
  else if (difficulty == EASY)
    idx = getWorstMove();
  else { // MEDIUM (Simple random mix)
    if (QRandomGenerator::global()->bounded(10) > 4)
      idx = getBestMove();
    else
      idx = getRandomMove();
  }

  if (idx == -1)
    idx = getRandomMove(); // Fallback

  board[idx] = computerMarker;
  cellBtns[idx]->setText(computerMarker);
  cellBtns[idx]->setEnabled(false);
  playSound("tap");

  if (!checkWin(computerMarker)) {
    turn = playerMarker;
    updateStatus();
  }
}

void MainWindow::updateStatus() {
  if (gameOver)
    return;
  statusLabel->setText(turn == playerMarker ? "Your Turn"
                                            : "Computer Thinking...");
}

bool MainWindow::checkWin(QString last) {
  int wins[8][3] = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6},
                    {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

  // Check Win
  for (auto &combo : wins) {
    if (board[combo[0]] == last && board[combo[1]] == last &&
        board[combo[2]] == last) {
      gameOver = true;
      for (int i : combo) {
        cellBtns[i]->setProperty("win", true);
        // Force WIN style
        QString winStyle =
            QString("background: %1; color: #064e3b; border: none; "
                    "border-radius: 16px; font-size: 48px; font-weight: 900;")
                .arg(COLORS_win);
        if (last != playerMarker) {
          winStyle =
              QString("background: %1; color: #064e3b; border: none; "
                      "border-radius: 16px; font-size: 48px; font-weight: 900;")
                  .arg(COLORS_lose);
        }
        cellBtns[i]->setStyleSheet(winStyle);
      }
      statusLabel->setText(last == playerMarker ? "Victory! 🎉" : "Defeat 🤖");
      QString col = (last == playerMarker) ? COLORS_win : COLORS_lose;
      // Hacky extraction of color from gradient string for label
      statusLabel->setStyleSheet(
          QString("font-size: 22px; font-weight: bold; color: #22d3ee;"));

      playSound(last == playerMarker ? "win" : "lose");
      playAgainBtn->show();
      backBtn->show();
      return true;
    }
  }

  // Check Draw
  bool full = true;
  for (int i = 0; i < 9; i++)
    if (board[i].isEmpty())
      full = false;

  if (full) {
    gameOver = true;
    statusLabel->setText("It's a Draw 😐");
    playAgainBtn->show();
    backBtn->show();
    return true;
  }
  return false;
}

// --- AI LOGIC ---
bool MainWindow::checkWinLogic(QString bd[9], const QString &w) {
  int wins[8][3] = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6},
                    {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};
  for (auto &c : wins)
    if (bd[c[0]] == w && bd[c[1]] == w && bd[c[2]] == w)
      return true;
  return false;
}
bool MainWindow::isBoardFull(QString bd[9]) {
  for (int i = 0; i < 9; i++)
    if (bd[i].isEmpty())
      return false;
  return true;
}

int MainWindow::minimax(QString bd[9], int depth, bool isMax) {
  if (checkWinLogic(bd, computerMarker))
    return 10 - depth;
  if (checkWinLogic(bd, playerMarker))
    return depth - 10;
  if (isBoardFull(bd))
    return 0;

  if (isMax) {
    int best = -1000;
    for (int i = 0; i < 9; i++) {
      if (bd[i].isEmpty()) {
        bd[i] = computerMarker;
        best = qMax(best, minimax(bd, depth + 1, !isMax));
        bd[i] = "";
      }
    }
    return best;
  } else {
    int best = 1000;
    for (int i = 0; i < 9; i++) {
      if (bd[i].isEmpty()) {
        bd[i] = playerMarker;
        best = qMin(best, minimax(bd, depth + 1, !isMax));
        bd[i] = "";
      }
    }
    return best;
  }
}

int MainWindow::getBestMove() {
  int bestVal = -1000;
  int bestMove = -1;
  // If empty board, take center or corner
  if (!board[4].isEmpty()) { /* optimization could go here */
  }

  for (int i = 0; i < 9; i++) {
    if (board[i].isEmpty()) {
      board[i] = computerMarker;
      int moveVal = minimax(board, 0, false);
      board[i] = "";
      if (moveVal > bestVal) {
        bestMove = i;
        bestVal = moveVal;
      }
    }
  }
  return bestMove;
}

int MainWindow::getWorstMove() {
  // "Suicide Mode" - try to lose
  int worstVal = 1000;
  int worstMove = -1;
  for (int i = 0; i < 9; i++) {
    if (board[i].isEmpty()) {
      board[i] = computerMarker;
      int moveVal =
          minimax(board, 0, false); // use normal minimax but pick lowest score
      board[i] = "";
      if (moveVal < worstVal) {
        worstMove = i;
        worstVal = moveVal;
      }
    }
  }
  if (worstMove == -1)
    return getRandomMove();
  return worstMove;
}

int MainWindow::getRandomMove() {
  QList<int> empty;
  for (int i = 0; i < 9; i++)
    if (board[i].isEmpty())
      empty.append(i);
  if (empty.isEmpty())
    return -1;
  return empty[QRandomGenerator::global()->bounded(empty.size())];
}
