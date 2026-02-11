package main

import (
	"image/color"
	"math"
	"math/rand"
	"syscall"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/layout"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
)

var (
	kernel32 = syscall.NewLazyDLL("kernel32.dll")
	procBeep = kernel32.NewProc("Beep")
)

func beep(freq int, duration int) {
	procBeep.Call(uintptr(freq), uintptr(duration))
}

// --- COLORS (Matching Glassmorphism Theme) ---
var (
	ColorBg      = color.RGBA{R: 2, G: 6, B: 23, A: 255}      // #020617
	ColorCard    = color.RGBA{R: 15, G: 23, B: 42, A: 255}    // #0f172a
	ColorCell    = color.RGBA{R: 30, G: 41, B: 59, A: 255}    // #1e293b
	ColorAccent  = color.RGBA{R: 34, G: 211, B: 238, A: 255}  // #22d3ee
	ColorWin     = color.RGBA{R: 16, G: 185, B: 129, A: 255}  // #10b981
	ColorLose    = color.RGBA{R: 244, G: 63, B: 94, A: 255}   // #f43f5e
	ColorText    = color.RGBA{R: 248, G: 250, B: 252, A: 255} // #f8fafc
	ColorSubText = color.RGBA{R: 148, G: 163, B: 184, A: 255} // #94a3b8
)

// --- CUSTOM THEME ---
type MyTheme struct{}

func (m MyTheme) Color(name fyne.ThemeColorName, variant fyne.ThemeVariant) color.Color {
	switch name {
	case theme.ColorNameBackground:
		return ColorBg
	case theme.ColorNameButton:
		return ColorCard
	case theme.ColorNameDisabledButton:
		return ColorCell
	case theme.ColorNamePrimary:
		return ColorAccent
	case theme.ColorNameForeground:
		return ColorText
	case theme.ColorNamePlaceHolder:
		return ColorSubText
	}
	return theme.DefaultTheme().Color(name, variant)
}
func (m MyTheme) Icon(name fyne.ThemeIconName) fyne.Resource {
	return theme.DefaultTheme().Icon(name)
}
func (m MyTheme) Font(style fyne.TextStyle) fyne.Resource {
	return theme.DefaultTheme().Font(style)
}
func (m MyTheme) Size(name fyne.ThemeSizeName) float32 {
	return theme.DefaultTheme().Size(name)
}

// --- GAME STATE ---
var (
	board        [9]string
	player, cpu  string
	turn         string
	difficulty   int // 0=Easy, 1=Med, 2=Hard
	gameOver     bool
	soundEnabled = true
	buttons      [9]*widget.Button
	statusLabel  *canvas.Text
	window       fyne.Window
)

func main() {
	myApp := app.New()
	myApp.Settings().SetTheme(&MyTheme{}) // Apply Custom Theme
	window = myApp.NewWindow("Tic Tac Toe")
	window.Resize(fyne.NewSize(400, 600))
	window.SetFixedSize(true)

	// Build Initial UI (Settings Page)
	showSettings()

	window.ShowAndRun()
}

func showSettings() {
	title := canvas.NewText("Tic Tac Toe", ColorAccent)
	title.TextSize = 32
	title.Alignment = fyne.TextAlignCenter
	title.TextStyle = fyne.TextStyle{Bold: true}

	// Choose Side
	lblSide := canvas.NewText("CHOOSE SIDE", ColorSubText)
	lblSide.TextSize = 12
	sideGroup := widget.NewRadioGroup([]string{"Play as X", "Play as O"}, func(s string) {})
	sideGroup.Selected = "Play as X"

	// Who Starts
	lblStart := canvas.NewText("WHO STARTS?", ColorSubText)
	lblStart.TextSize = 12
	startGroup := widget.NewRadioGroup([]string{"You", "CPU"}, func(s string) {})
	startGroup.Selected = "You"

	// Difficulty
	lblDiff := canvas.NewText("DIFFICULTY", ColorSubText)
	lblDiff.TextSize = 12
	diffGroup := widget.NewRadioGroup([]string{"Easy", "Med", "Hard"}, func(s string) {})
	diffGroup.Selected = "Med"
	diffGroup.Horizontal = true

	// Sound Toggle
	chkSound := widget.NewCheck("Sound Effects", func(b bool) { soundEnabled = b })
	chkSound.Checked = true

	// Start Button
	btnStart := widget.NewButton("Start Game", func() {
		// Parse Settings
		if sideGroup.Selected == "Play as X" {
			player, cpu = "X", "O"
		} else {
			player, cpu = "O", "X"
		}

		if startGroup.Selected == "You" {
			turn = player
		} else {
			turn = cpu
		}

		switch diffGroup.Selected {
		case "Easy":
			difficulty = 0
		case "Hard":
			difficulty = 2
		default:
			difficulty = 1
		}

		startGame()
	})
	btnStart.Importance = widget.HighImportance

	// Layout
	content := container.NewVBox(
		layout.NewSpacer(),
		title,
		layout.NewSpacer(),
		container.NewPadded(
			container.NewVBox(
				lblSide, sideGroup,
				lblStart, startGroup,
				lblDiff, diffGroup,
				chkSound,
			),
		),
		layout.NewSpacer(),
		btnStart,
		layout.NewSpacer(),
	)

	// Wrap in a colored background (glassmorphism simulation)
	bg := canvas.NewRectangle(ColorBg)
	window.SetContent(container.NewStack(bg, container.NewPadded(content)))
}

func startGame() {
	// Reset Board
	for i := 0; i < 9; i++ {
		board[i] = ""
	}
	gameOver = false

	// Status Label
	statusLabel = canvas.NewText(getTurnText(), ColorText)
	statusLabel.TextSize = 24
	statusLabel.Alignment = fyne.TextAlignCenter
	statusLabel.TextStyle = fyne.TextStyle{Bold: true}

	// Grid
	grid := container.NewGridWithColumns(3)
	for i := 0; i < 9; i++ {
		idx := i
		// Create button with specific emphasis
		btn := widget.NewButton("", func() {
			if !gameOver && board[idx] == "" && turn == player {
				makeMove(idx, player)
				if !checkEnd() {
					turn = cpu
					updateStatus()
					go func() {
						time.Sleep(600 * time.Millisecond)
						cpuMove()
					}()
				}
			}
		})
		buttons[i] = btn
		// Wrap in fixed size container to force square shape
		grid.Add(container.NewGridWrap(fyne.NewSize(90, 90), btn))
	}

	// Controls
	playAgainBtn = widget.NewButton("Play Again", func() { startGame() })
	playAgainBtn.Importance = widget.HighImportance
	playAgainBtn.Hide() // Hidden initially

	backCtxBtn = widget.NewButton("Back to Settings", func() { showSettings() })
	backCtxBtn.Hide() // Hidden initially

	// Layout
	content := container.NewBorder(
		container.NewVBox(layout.NewSpacer(), statusLabel, layout.NewSpacer()),
		container.NewVBox(container.NewPadded(playAgainBtn), container.NewPadded(backCtxBtn)), // Bottom
		nil, nil,
		container.NewCenter(grid), // Centered Grid
	)

	bg := canvas.NewRectangle(ColorBg)
	window.SetContent(container.NewStack(bg, content))

	if turn == cpu {
		go func() {
			time.Sleep(800 * time.Millisecond)
			cpuMove()
		}()
	}
}

func getTurnText() string {
	if turn == player {
		return "Your Turn"
	}
	return "Computer Thinking..."
}

func updateStatus() {
	statusLabel.Text = getTurnText()
	statusLabel.Refresh()
}

func makeMove(idx int, mark string) {
	board[idx] = mark
	buttons[idx].SetText(mark)
	// Use HighImportance to trigger the Primary Color (Cyan) for X/O
	buttons[idx].Importance = widget.HighImportance 
	buttons[idx].Refresh()
	
	playSound("tap")
}

func cpuMove() {
	if gameOver {
		return
	}
	
	move := -1
	
	// Minimax for Hard/Med
	if difficulty == 2 || (difficulty == 1 && rand.Intn(10) > 3) {
		move = getBestMove()
	}
	
	// Random fallback
	if move == -1 || difficulty == 0 {
		var available []int
		for i, v := range board {
			if v == "" {
				available = append(available, i)
			}
		}
		if len(available) > 0 {
			move = available[rand.Intn(len(available))]
		}
	}
	
	if move != -1 {
		makeMove(move, cpu)
		if !checkEnd() {
			turn = player
			updateStatus()
		}
	}
}

// --- AI (Minimax) ---
func getBestMove() int {
	bestVal := -1000
	bestMove := -1
	for i := 0; i < 9; i++ {
		if board[i] == "" {
			board[i] = cpu
			score := minimax(0, false)
			board[i] = ""
			if score > bestVal {
				bestVal = score
				bestMove = i
			}
		}
	}
	return bestMove
}

func minimax(depth int, isMax bool) int {
	score := evaluate()
	if score == 10 { return score - depth }
	if score == -10 { return score + depth }
	if !isMovesLeft() { return 0 }

	if isMax {
		best := -1000
		for i := 0; i < 9; i++ {
			if board[i] == "" {
				board[i] = cpu
				best = int(math.Max(float64(best), float64(minimax(depth+1, !isMax))))
				board[i] = ""
			}
		}
		return best
	} else {
		best := 1000
		for i := 0; i < 9; i++ {
			if board[i] == "" {
				board[i] = player
				best = int(math.Min(float64(best), float64(minimax(depth+1, !isMax))))
				board[i] = ""
			}
		}
		return best
	}
}

func evaluate() int {
	lines := [][3]int{
		{0, 1, 2}, {3, 4, 5}, {6, 7, 8},
		{0, 3, 6}, {1, 4, 7}, {2, 5, 8},
		{0, 4, 8}, {2, 4, 6},
	}
	for _, l := range lines {
		if board[l[0]] == board[l[1]] && board[l[1]] == board[l[2]] {
			if board[l[0]] == cpu {
				return 10
			} else if board[l[0]] == player {
				return -10
			}
		}
	}
	return 0
}

func isMovesLeft() bool {
	for _, v := range board {
		if v == "" {
			return true
		}
	}
	return false
}

func checkEnd() bool {
	// Re-evaluate to find the winning line for highlighting
	lines := [][3]int{
		{0, 1, 2}, {3, 4, 5}, {6, 7, 8},
		{0, 3, 6}, {1, 4, 7}, {2, 5, 8},
		{0, 4, 8}, {2, 4, 6},
	}
	
	var winningLine []int
	var winner string
	
	for _, l := range lines {
		if board[l[0]] != "" && board[l[0]] == board[l[1]] && board[l[1]] == board[l[2]] {
			winningLine = l[:]
			winner = board[l[0]]
			break
		}
	}

	if winner != "" {
		gameOver = true
		win := (winner == player)
		
		// Highlight buttons
		for _, idx := range winningLine {
			if win {
				buttons[idx].Importance = widget.SuccessImportance // Green
			} else {
				buttons[idx].Importance = widget.DangerImportance // Red
			}
			buttons[idx].Refresh()
		}

		if win {
			statusLabel.Text = "Victory!"
			statusLabel.Color = ColorWin
			playSound("win")
		} else {
			statusLabel.Text = "Defeat!"
			statusLabel.Color = ColorLose
			playSound("lose")
		}
		statusLabel.Refresh()
		
		// Show Play Again (Find it in layout logic or just rebuild UI)
		// Since we can't easily access the local btnPlayAgain variable from here without global or passing,
		// we'll trigger a UI refresh or assume it shows if we structure it right.
		// Actually, let's just create the buttons globally or refactor slightly.
		// For simplicity in this edit: We will just rebuild the bottom container? No that's hard.
		// We'll use the 'showEndButtons' pattern if we had one.
		// Wait, I declared btnPlayAgain inside startGame. I need to access it.
		// I will assume for now that I can't access it here easily without moving it to global.
		// Fix: Move btnPlayAgain to global 'playAgainBtn'
		
		if playAgainBtn != nil {
			playAgainBtn.Show()
		}
		if backCtxBtn != nil {
			backCtxBtn.Show()
		}
		
		return true
	}
	
	if !isMovesLeft() {
		gameOver = true
		statusLabel.Text = "It's a Draw!"
		statusLabel.Color = ColorText
		statusLabel.Refresh()
		if playAgainBtn != nil {
			playAgainBtn.Show()
		}
		if backCtxBtn != nil {
			backCtxBtn.Show()
		}
		return true
	}
	return false
}

// Global button for access in checkEnd
var playAgainBtn *widget.Button
var backCtxBtn *widget.Button

// --- SOUND (Windows Beep Implementation) ---
func playSound(typ string) {
	if !soundEnabled {
		return
	}
	go func() {
		if typ == "tap" {
			beep(700, 80)
		} else if typ == "win" {
			beep(523, 120)
			time.Sleep(30 * time.Millisecond)
			beep(659, 120)
			time.Sleep(30 * time.Millisecond)
			beep(784, 120)
			time.Sleep(30 * time.Millisecond)
			beep(1046, 120)
		} else if typ == "lose" {
			beep(200, 250)
			time.Sleep(60 * time.Millisecond)
			beep(150, 250)
			time.Sleep(60 * time.Millisecond)
			beep(100, 250)
		}
	}()
}
