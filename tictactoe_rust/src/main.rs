#![windows_subsystem = "windows"]

use eframe::egui;
use rand::Rng;
use std::thread;

// --- Windows Beep API ---
#[cfg(windows)]
mod sound {
    #[link(name = "kernel32")]
    extern "system" {
        fn Beep(dwFreq: u32, dwDuration: u32) -> i32;
        fn Sleep(dwMilliseconds: u32);
    }

    pub fn beep(freq: u32, dur: u32) {
        unsafe { Beep(freq, dur); }
    }

    pub fn sleep_ms(ms: u32) {
        unsafe { Sleep(ms); }
    }
}

#[cfg(not(windows))]
mod sound {
    pub fn beep(_freq: u32, _dur: u32) {}
    pub fn sleep_ms(_ms: u32) {}
}

// --- THEME COLORS ---
const BG: egui::Color32 = egui::Color32::from_rgb(2, 6, 23);
const CARD: egui::Color32 = egui::Color32::from_rgb(15, 23, 42);
const CELL: egui::Color32 = egui::Color32::from_rgb(30, 41, 59);
const CELL_HOVER: egui::Color32 = egui::Color32::from_rgb(51, 65, 85);
const ACCENT: egui::Color32 = egui::Color32::from_rgb(34, 211, 238);
const ACCENT_ALT: egui::Color32 = egui::Color32::from_rgb(129, 140, 248);
const TEXT: egui::Color32 = egui::Color32::from_rgb(248, 250, 252);
const TEXT_DIM: egui::Color32 = egui::Color32::from_rgb(203, 213, 225);
const TEXT_MUTED: egui::Color32 = egui::Color32::from_rgb(100, 116, 139);
const WIN_GREEN: egui::Color32 = egui::Color32::from_rgb(16, 185, 129);
const WIN_GREEN_LIGHT: egui::Color32 = egui::Color32::from_rgb(52, 211, 153);
const LOSE_RED: egui::Color32 = egui::Color32::from_rgb(244, 63, 94);
const LOSE_RED_LIGHT: egui::Color32 = egui::Color32::from_rgb(251, 113, 133);
const CELL_BORDER: egui::Color32 = egui::Color32::from_rgb(28, 36, 55);  // white 5% over card
const CARD_BORDER: egui::Color32 = egui::Color32::from_rgb(40, 48, 67);  // white 10% over card
const SUBTLE_BG: egui::Color32 = egui::Color32::from_rgb(18, 26, 45);    // white 3% over card (dark!)
const DISABLED_BG: egui::Color32 = egui::Color32::from_rgb(15, 23, 42);
const RADIO_BG: egui::Color32 = egui::Color32::from_rgb(20, 28, 48);     // slightly lighter than card
const RADIO_BG_CHECKED: egui::Color32 = egui::Color32::from_rgb(16, 32, 52); // subtle accent tint
const WIN_TEXT: egui::Color32 = egui::Color32::from_rgb(6, 78, 59);

// --- DIFFICULTY ---
#[derive(Clone, Copy, PartialEq)]
enum Difficulty {
    Easy,
    Medium,
    Hard,
}

// --- GAME SCREEN ---
#[derive(Clone, Copy, PartialEq)]
enum Screen {
    Settings,
    Game,
}

// --- CELL STATE ---
#[derive(Clone, Copy, PartialEq, Debug)]
enum Cell {
    Empty,
    X,
    O,
}

impl Cell {
    fn as_str(&self) -> &str {
        match self {
            Cell::X => "X",
            Cell::O => "O",
            Cell::Empty => "",
        }
    }
}

// --- APP STATE ---
struct TicTacToeApp {
    screen: Screen,
    board: [Cell; 9],
    player_marker: Cell,
    computer_marker: Cell,
    turn: Cell,
    difficulty: Difficulty,
    game_over: bool,
    sound_enabled: bool,
    winning_cells: [bool; 9],
    status_text: String,
    result: Option<GameResult>,

    // Settings selections
    play_as_x: bool,
    player_starts: bool,

    // Timer for CPU delay
    cpu_pending: bool,
    cpu_move_time: Option<std::time::Instant>,
}

#[derive(Clone, Copy, PartialEq)]
enum GameResult {
    Win,
    Lose,
    Draw,
}

impl Default for TicTacToeApp {
    fn default() -> Self {
        Self {
            screen: Screen::Settings,
            board: [Cell::Empty; 9],
            player_marker: Cell::X,
            computer_marker: Cell::O,
            turn: Cell::X,
            difficulty: Difficulty::Medium,
            game_over: false,
            sound_enabled: true,
            winning_cells: [false; 9],
            status_text: "Your Turn".to_string(),
            result: None,
            play_as_x: true,
            player_starts: true,
            cpu_pending: false,
            cpu_move_time: None,
        }
    }
}

impl TicTacToeApp {
    fn new(cc: &eframe::CreationContext<'_>) -> Self {
        // Configure dark visuals to match our theme
        let mut visuals = egui::Visuals::dark();

        // Widget colors for all states
        let dark_widget_bg = egui::Color32::from_rgb(20, 30, 50);
        let dark_widget_hover = egui::Color32::from_rgb(35, 50, 75);
        let dark_widget_active = egui::Color32::from_rgb(45, 60, 90);

        visuals.widgets.inactive = egui::style::WidgetVisuals {
            bg_fill: dark_widget_bg,
            weak_bg_fill: dark_widget_bg,
            bg_stroke: egui::Stroke::new(1.0, CELL_BORDER),
            fg_stroke: egui::Stroke::new(1.0, TEXT_DIM),
            corner_radius: egui::CornerRadius::same(15),
            expansion: 0.0,
        };

        visuals.widgets.hovered = egui::style::WidgetVisuals {
            bg_fill: dark_widget_hover,
            weak_bg_fill: dark_widget_hover,
            bg_stroke: egui::Stroke::new(1.0, ACCENT),
            fg_stroke: egui::Stroke::new(1.5, TEXT),
            corner_radius: egui::CornerRadius::same(15),
            expansion: 1.0,
        };

        visuals.widgets.active = egui::style::WidgetVisuals {
            bg_fill: dark_widget_active,
            weak_bg_fill: dark_widget_active,
            bg_stroke: egui::Stroke::new(1.0, ACCENT),
            fg_stroke: egui::Stroke::new(2.0, TEXT),
            corner_radius: egui::CornerRadius::same(15),
            expansion: 0.0,
        };

        visuals.widgets.open = egui::style::WidgetVisuals {
            bg_fill: dark_widget_bg,
            weak_bg_fill: dark_widget_bg,
            bg_stroke: egui::Stroke::new(1.0, ACCENT),
            fg_stroke: egui::Stroke::new(1.0, TEXT),
            corner_radius: egui::CornerRadius::same(15),
            expansion: 0.0,
        };

        visuals.widgets.noninteractive = egui::style::WidgetVisuals {
            bg_fill: CARD,
            weak_bg_fill: CARD,
            bg_stroke: egui::Stroke::NONE,
            fg_stroke: egui::Stroke::new(1.0, TEXT_DIM),
            corner_radius: egui::CornerRadius::same(0),
            expansion: 0.0,
        };

        // Overall panel/window backgrounds
        visuals.panel_fill = BG;
        visuals.window_fill = CARD;
        visuals.extreme_bg_color = BG;
        visuals.faint_bg_color = egui::Color32::from_rgb(10, 15, 30);

        cc.egui_ctx.set_visuals(visuals);

        // Load system emoji font for full emoji support
        let mut fonts = egui::FontDefinitions::default();
        if let Ok(emoji_data) = std::fs::read("C:\\Windows\\Fonts\\seguiemj.ttf") {
            fonts.font_data.insert(
                "segoe_emoji".to_owned(),
                egui::FontData::from_owned(emoji_data).into(),
            );
            // Add as fallback to proportional fonts
            if let Some(family) = fonts.families.get_mut(&egui::FontFamily::Proportional) {
                family.push("segoe_emoji".to_owned());
            }
        }
        cc.egui_ctx.set_fonts(fonts);

        Self::default()
    }

    // --- Sound Effects ---
    fn sfx_tap(&self) {
        if !self.sound_enabled { return; }
        thread::spawn(|| { sound::beep(700, 80); });
    }

    fn sfx_win(&self) {
        if !self.sound_enabled { return; }
        thread::spawn(|| {
            for freq in [523, 659, 784, 1046] {
                sound::beep(freq, 120);
                sound::sleep_ms(30);
            }
        });
    }

    fn sfx_lose(&self) {
        if !self.sound_enabled { return; }
        thread::spawn(|| {
            for freq in [200, 150, 100] {
                sound::beep(freq, 250);
                sound::sleep_ms(60);
            }
        });
    }

    // --- Game Logic ---
    fn start_game(&mut self) {
        self.player_marker = if self.play_as_x { Cell::X } else { Cell::O };
        self.computer_marker = if self.play_as_x { Cell::O } else { Cell::X };
        self.turn = if self.player_starts { self.player_marker } else { self.computer_marker };
        self.board = [Cell::Empty; 9];
        self.winning_cells = [false; 9];
        self.game_over = false;
        self.result = None;
        self.status_text = "Your Turn".to_string();
        self.screen = Screen::Game;

        if self.turn == self.computer_marker {
            self.cpu_pending = true;
            self.cpu_move_time = Some(std::time::Instant::now() + std::time::Duration::from_millis(800));
            self.status_text = "Computer Thinking...".to_string();
        } else {
            self.cpu_pending = false;
            self.cpu_move_time = None;
        }
    }

    fn player_action(&mut self, idx: usize) {
        if self.game_over || self.board[idx] != Cell::Empty || self.turn != self.player_marker {
            return;
        }
        self.make_move(idx, self.player_marker);
        if !self.check_end(self.player_marker) {
            self.turn = self.computer_marker;
            self.status_text = "Computer Thinking...".to_string();
            self.cpu_pending = true;
            self.cpu_move_time = Some(std::time::Instant::now() + std::time::Duration::from_millis(700));
        }
    }

    fn cpu_move(&mut self) {
        if self.game_over { return; }
        let idx = match self.difficulty {
            Difficulty::Hard => self.get_best(),
            Difficulty::Easy => self.get_worst(),
            Difficulty::Medium => {
                let mut rng = rand::thread_rng();
                if rng.gen::<f64>() > 0.4 {
                    self.get_best()
                } else {
                    self.get_random()
                }
            }
        };
        if let Some(idx) = idx {
            let cm = self.computer_marker;
            self.make_move(idx, cm);
            if !self.check_end(cm) {
                self.turn = self.player_marker;
                self.status_text = "Your Turn".to_string();
            }
        }
        self.cpu_pending = false;
        self.cpu_move_time = None;
    }

    fn make_move(&mut self, idx: usize, who: Cell) {
        self.board[idx] = who;
        self.sfx_tap();
    }

    fn check_end(&mut self, last: Cell) -> bool {
        const WINS: [[usize; 3]; 8] = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8],
            [0, 3, 6], [1, 4, 7], [2, 5, 8],
            [0, 4, 8], [2, 4, 6],
        ];
        for combo in &WINS {
            if self.board[combo[0]] == last && self.board[combo[1]] == last && self.board[combo[2]] == last {
                self.game_over = true;
                for &i in combo {
                    self.winning_cells[i] = true;
                }
                if last == self.player_marker {
                    self.status_text = "Victory! 🎉".to_string();
                    self.result = Some(GameResult::Win);
                    self.sfx_win();
                } else {
                    self.status_text = "Defeat 🤖".to_string();
                    self.result = Some(GameResult::Lose);
                    self.sfx_lose();
                }
                return true;
            }
        }
        if self.board.iter().all(|c| *c != Cell::Empty) {
            self.game_over = true;
            self.status_text = "It's a Draw 😐".to_string();
            self.result = Some(GameResult::Draw);
            return true;
        }
        false
    }

    // --- AI ---
    fn get_random(&self) -> Option<usize> {
        let empty: Vec<usize> = self.board.iter().enumerate()
            .filter(|(_, c)| **c == Cell::Empty).map(|(i, _)| i).collect();
        if empty.is_empty() { return None; }
        let mut rng = rand::thread_rng();
        Some(empty[rng.gen_range(0..empty.len())])
    }

    fn get_best(&self) -> Option<usize> {
        let empty: Vec<usize> = self.board.iter().enumerate()
            .filter(|(_, c)| **c == Cell::Empty).map(|(i, _)| i).collect();
        if empty.len() == 9 { return Some(4); }
        let mut best_score = i32::MIN;
        let mut best_move = None;
        let mut board = self.board;
        for i in &empty {
            board[*i] = self.computer_marker;
            let score = Self::minimax(&mut board, 0, false, self.computer_marker, self.player_marker);
            board[*i] = Cell::Empty;
            if score > best_score {
                best_score = score;
                best_move = Some(*i);
            }
        }
        best_move
    }

    fn get_worst(&self) -> Option<usize> {
        let empty: Vec<usize> = self.board.iter().enumerate()
            .filter(|(_, c)| **c == Cell::Empty).map(|(i, _)| i).collect();
        if empty.len() >= 8 { return self.get_random(); }
        let mut worst_score = i32::MAX;
        let mut worst_move = None;
        let mut board = self.board;
        for i in &empty {
            board[*i] = self.computer_marker;
            let score = Self::minimax(&mut board, 0, false, self.computer_marker, self.player_marker);
            board[*i] = Cell::Empty;
            if score < worst_score {
                worst_score = score;
                worst_move = Some(*i);
            }
        }
        worst_move.or_else(|| self.get_random())
    }

    fn minimax(board: &mut [Cell; 9], depth: i32, is_max: bool, comp: Cell, player: Cell) -> i32 {
        if Self::win_check(board, comp) { return 10 - depth; }
        if Self::win_check(board, player) { return depth - 10; }
        if board.iter().all(|c| *c != Cell::Empty) { return 0; }

        let empty: Vec<usize> = board.iter().enumerate()
            .filter(|(_, c)| **c == Cell::Empty).map(|(i, _)| i).collect();

        if is_max {
            let mut best = i32::MIN;
            for i in &empty {
                board[*i] = comp;
                best = best.max(Self::minimax(board, depth + 1, false, comp, player));
                board[*i] = Cell::Empty;
            }
            best
        } else {
            let mut best = i32::MAX;
            for i in &empty {
                board[*i] = player;
                best = best.min(Self::minimax(board, depth + 1, true, comp, player));
                board[*i] = Cell::Empty;
            }
            best
        }
    }

    fn win_check(board: &[Cell; 9], who: Cell) -> bool {
        const WINS: [[usize; 3]; 8] = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8],
            [0, 3, 6], [1, 4, 7], [2, 5, 8],
            [0, 4, 8], [2, 4, 6],
        ];
        WINS.iter().any(|c| board[c[0]] == who && board[c[1]] == who && board[c[2]] == who)
    }
}

// --- EGUI RENDERING ---
impl eframe::App for TicTacToeApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // Handle pending CPU move with delay
        if self.cpu_pending {
            if let Some(time) = self.cpu_move_time {
                if std::time::Instant::now() >= time {
                    self.cpu_move();
                }
            }
            ctx.request_repaint();
        }

        // Dark background
        let frame = egui::Frame::new()
            .fill(BG)
            .inner_margin(egui::Margin::same(20));

        egui::CentralPanel::default().frame(frame).show(ctx, |ui| {
            // Card container
            let card_frame = egui::Frame::new()
                .fill(CARD)
                .corner_radius(egui::CornerRadius::same(35))
                .stroke(egui::Stroke::new(1.0, CARD_BORDER))
                .inner_margin(egui::Margin::same(30))
                .shadow(egui::epaint::Shadow {
                    spread: 0,
                    blur: 50,
                    offset: [0, 25],
                    color: egui::Color32::from_rgba_premultiplied(0, 0, 0, 200),
                });

            card_frame.show(ui, |ui| {
                ui.set_min_width(ui.available_width());
                match self.screen {
                    Screen::Settings => self.render_settings(ui),
                    Screen::Game => self.render_game(ui),
                }
            });
        });
    }
}

impl TicTacToeApp {
    fn render_settings(&mut self, ui: &mut egui::Ui) {
        ui.vertical_centered(|ui| {
            ui.add_space(10.0);

            // Title
            ui.label(
                egui::RichText::new("Tic Tac Toe")
                    .size(34.0)
                    .color(ACCENT)
                    .strong()
            );

            ui.add_space(25.0);

            // --- Choose Side ---
            ui.label(
                egui::RichText::new("CHOOSE SIDE")
                    .size(11.0)
                    .color(TEXT_MUTED)
                    .strong()
            );
            ui.add_space(8.0);
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 10.0;
                let w = (ui.available_width() - 10.0) / 2.0;
                if self.styled_radio(ui, "  Play as X", self.play_as_x, w) {
                    self.play_as_x = true;
                }
                if self.styled_radio(ui, "  Play as O", !self.play_as_x, w) {
                    self.play_as_x = false;
                }
            });

            ui.add_space(15.0);

            // --- Who Starts ---
            ui.label(
                egui::RichText::new("WHO STARTS?")
                    .size(11.0)
                    .color(TEXT_MUTED)
                    .strong()
            );
            ui.add_space(8.0);
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 10.0;
                let w = (ui.available_width() - 10.0) / 2.0;
                if self.styled_radio(ui, "  You", self.player_starts, w) {
                    self.player_starts = true;
                }
                if self.styled_radio(ui, "  CPU", !self.player_starts, w) {
                    self.player_starts = false;
                }
            });

            ui.add_space(15.0);

            // --- Difficulty ---
            ui.label(
                egui::RichText::new("DIFFICULTY")
                    .size(11.0)
                    .color(TEXT_MUTED)
                    .strong()
            );
            ui.add_space(8.0);
            ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing.x = 8.0;
                let w = (ui.available_width() - 16.0) / 3.0;
                if self.styled_radio(ui, "  Easy", self.difficulty == Difficulty::Easy, w) {
                    self.difficulty = Difficulty::Easy;
                }
                if self.styled_radio(ui, "  Med", self.difficulty == Difficulty::Medium, w) {
                    self.difficulty = Difficulty::Medium;
                }
                if self.styled_radio(ui, "  Hard", self.difficulty == Difficulty::Hard, w) {
                    self.difficulty = Difficulty::Hard;
                }
            });

            ui.add_space(30.0);

            // --- Start Button ---
            let start_btn = egui::Button::new(
                egui::RichText::new("Start Game").size(18.0).color(BG).strong()
            )
            .fill(ACCENT)
            .corner_radius(egui::CornerRadius::same(20))
            .min_size(egui::Vec2::new(ui.available_width(), 55.0));

            if ui.add(start_btn).clicked() {
                self.start_game();
            }

            ui.add_space(10.0);

            // --- Sound Toggle ---
            let snd_text = if self.sound_enabled { "🔊 Sound: ON" } else { "🔇 Sound: OFF" };
            let snd_btn = egui::Button::new(
                egui::RichText::new(snd_text).size(14.0).color(TEXT_DIM).strong()
            )
            .fill(SUBTLE_BG)
            .stroke(egui::Stroke::new(1.0, CARD_BORDER))
            .corner_radius(egui::CornerRadius::same(18))
            .min_size(egui::Vec2::new(ui.available_width(), 48.0));

            if ui.add(snd_btn).clicked() {
                self.sound_enabled = !self.sound_enabled;
            }

            ui.add_space(10.0);
        });
    }

    fn render_game(&mut self, ui: &mut egui::Ui) {
        ui.vertical_centered(|ui| {
            ui.add_space(10.0);

            // Status label
            let status_color = match self.result {
                Some(GameResult::Win) => WIN_GREEN,
                Some(GameResult::Lose) => LOSE_RED,
                Some(GameResult::Draw) => egui::Color32::from_rgb(250, 204, 21), // yellow
                _ => TEXT,
            };
            let status_size = if self.game_over { 22.0 } else { 18.0 };
            ui.label(
                egui::RichText::new(&self.status_text)
                    .size(status_size)
                    .color(status_color)
                    .strong()
            );

            ui.add_space(15.0);

            // --- Game Grid ---
            let cell_size = 100.0_f32;
            let spacing = 10.0_f32;
            let grid_size = cell_size * 3.0 + spacing * 2.0;

            // Center the grid
            let avail = ui.available_width();
            let left_pad = ((avail - grid_size) / 2.0).max(0.0);

            ui.horizontal(|ui| {
                ui.add_space(left_pad);
                ui.vertical(|ui| {
                    ui.spacing_mut().item_spacing = egui::Vec2::new(spacing, spacing);
                    for row in 0..3 {
                        ui.horizontal(|ui| {
                            ui.spacing_mut().item_spacing.x = spacing;
                            for col in 0..3 {
                                let idx = row * 3 + col;
                                self.render_cell(ui, idx, cell_size);
                            }
                        });
                    }
                });
            });

            ui.add_space(20.0);

            // --- Game Over Buttons ---
            if self.game_over {
                let again_btn = egui::Button::new(
                    egui::RichText::new("Play Again").size(18.0).color(BG).strong()
                )
                .fill(ACCENT)
                .corner_radius(egui::CornerRadius::same(20))
                .min_size(egui::Vec2::new(ui.available_width(), 55.0));
                if ui.add(again_btn).clicked() {
                    self.start_game();
                }

                ui.add_space(8.0);

                let back_btn = egui::Button::new(
                    egui::RichText::new("Back to Settings").size(14.0).color(TEXT_DIM).strong()
                )
                .fill(SUBTLE_BG)
                .stroke(egui::Stroke::new(1.0, CARD_BORDER))
                .corner_radius(egui::CornerRadius::same(18))
                .min_size(egui::Vec2::new(ui.available_width(), 48.0));
                if ui.add(back_btn).clicked() {
                    self.screen = Screen::Settings;
                }
            }

            ui.add_space(10.0);
        });
    }

    fn render_cell(&mut self, ui: &mut egui::Ui, idx: usize, size: f32) {
        let cell = self.board[idx];
        let is_winning = self.winning_cells[idx];
        let is_empty = cell == Cell::Empty;
        let can_click = is_empty && !self.game_over && self.turn == self.player_marker && !self.cpu_pending;

        // Determine colors
        let (bg_color, text_color, border_color) = if is_winning {
            if self.result == Some(GameResult::Win) {
                (WIN_GREEN, WIN_TEXT, WIN_GREEN_LIGHT)
            } else {
                (LOSE_RED, WIN_TEXT, LOSE_RED_LIGHT)
            }
        } else if !is_empty {
            (DISABLED_BG, ACCENT, DISABLED_BG)
        } else {
            (CELL, ACCENT, CELL_BORDER)
        };

        let btn = egui::Button::new(
            egui::RichText::new(cell.as_str())
                .size(48.0)
                .color(text_color)
                .strong()
        )
        .fill(bg_color)
        .stroke(egui::Stroke::new(2.0, border_color))
        .corner_radius(egui::CornerRadius::same(16))
        .min_size(egui::Vec2::new(size, size));

        let response = ui.add(btn);

        // Hover effect for empty clickable cells
        if can_click && response.hovered() {
            let painter = ui.painter();
            let rect = response.rect;
            painter.rect_filled(rect, egui::CornerRadius::same(16), CELL_HOVER);
            painter.rect_stroke(rect, egui::CornerRadius::same(16), egui::Stroke::new(2.0, ACCENT), egui::StrokeKind::Outside);
        }

        if can_click && response.clicked() {
            self.player_action(idx);
        }
    }

    fn styled_radio(&self, ui: &mut egui::Ui, label: &str, checked: bool, width: f32) -> bool {
        let (bg, border, circle_color) = if checked {
            (RADIO_BG_CHECKED, ACCENT, ACCENT)
        } else {
            (RADIO_BG, CELL_BORDER, TEXT_MUTED)
        };

        let btn = egui::Button::new(
            egui::RichText::new(label).size(14.0).color(TEXT_DIM).strong()
        )
        .fill(bg)
        .stroke(egui::Stroke::new(if checked { 2.0 } else { 1.0 }, border))
        .corner_radius(egui::CornerRadius::same(15))
        .min_size(egui::Vec2::new(width, 44.0));

        let response = ui.add(btn);

        // Draw custom radio circle
        let painter = ui.painter();
        let rect = response.rect;
        let circle_center = egui::Pos2::new(rect.left() + 22.0, rect.center().y);
        let radius = 8.0;

        if checked {
            painter.circle_stroke(circle_center, radius, egui::Stroke::new(2.0, circle_color));
            painter.circle_filled(circle_center, 5.0, circle_color);
        } else {
            painter.circle_stroke(circle_center, radius, egui::Stroke::new(2.0, circle_color));
        }

        response.clicked()
    }
}

fn main() -> eframe::Result<()> {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_title("Tic Tac Toe")
            .with_inner_size([460.0, 720.0])
            .with_min_inner_size([400.0, 640.0])
            .with_resizable(false),
        ..Default::default()
    };

    eframe::run_native(
        "Tic Tac Toe",
        options,
        Box::new(|cc| Ok(Box::new(TicTacToeApp::new(cc)))),
    )
}
