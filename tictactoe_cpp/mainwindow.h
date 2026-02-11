#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QRadioButton>
#include <QButtonGroup>
#include <QLabel>
#include <QStackedWidget>
#include <QPushButton>
#include <QGridLayout>
#include <QPainter>
#include <QPainterPath>
#include <QTimer>
#include <QThread>

// --- Custom Radial RadioButton ---
class CircleRadioButton : public QRadioButton {
    Q_OBJECT
public:
    explicit CircleRadioButton(const QString &text, QWidget *parent = nullptr);
protected:
    void paintEvent(QPaintEvent *event) override;
};

// --- Main Window ---
class MainWindow : public QMainWindow {
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    void startGame();
    void showSettings();
    void toggleSound();
    void handleCellClick(int idx);
    void computerMove();

private:
    // UI Elements
    QWidget *centralWidget;
    QStackedWidget *stack;
    QButtonGroup *sideGrp, *ordGrp, *diffGrp;
    CircleRadioButton *rx, *ro, *ry, *rc, *re, *rm, *rh;
    QLabel *statusLabel;
    QList<QPushButton*> cellBtns;
    QPushButton *soundBtn;
    QPushButton *playAgainBtn;
    QPushButton *backBtn;
    int cellSize;

    // Game Logic
    QString board[9];
    QString playerMarker, computerMarker, turn;
    enum Difficulty { EASY, MEDIUM, HARD } difficulty;
    bool gameOver;
    bool soundEnabled;

    // Audio & Logic
    void initSettingsScreen();
    void initGameScreen();
    void updateStatus();
    bool checkWin(QString winner);
    void playSound(const QString& freq_dur); // Placeholder for Beep strings
    
    // AI
    int getBestMove();
    int getWorstMove();
    int getRandomMove();
    int minimax(QString bd[9], int depth, bool isMax);
    bool checkWinLogic(QString bd[9], const QString& who);
    bool isBoardFull(QString bd[9]);

    // Resizing
    void resizeEvent(QResizeEvent *event) override;
};

#endif // MAINWINDOW_H
