import tkinter as tk
from tkinter import messagebox
import random

class TicTacToe:
    WIN_COMBOS = [
        [0, 1, 2], [3, 4, 5], [6, 7, 8],  # Rows
        [0, 3, 6], [1, 4, 7], [2, 5, 8],  # Columns
        [0, 4, 8], [2, 4, 6]              # Diagonals
    ]
    
    def __init__(self, root):
        self.root = root
        self.root.title("Tic-Tac-Toe")
        self.reset_game_state()
        self.create_choice_window()

    def reset_game_state(self):
        self.current_player = None
        self.player_marker = None
        self.computer_marker = None
        self.board = [" " for _ in range(9)]
        self.game_over = False
        self.difficulty = "easy"

    def create_choice_window(self):
        self.choice_window = tk.Toplevel(self.root)
        self.choice_window.title("Game Settings")
        
        # Marker selection
        tk.Label(self.choice_window, text="Choose your marker:").pack(pady=5)
        self.marker_var = tk.StringVar(value="X")
        self.create_radiobuttons(self.choice_window, self.marker_var, [("X", "X"), ("O", "O")])
        
        # First player selection
        tk.Label(self.choice_window, text="Who goes first?").pack(pady=5)
        self.first_player_var = tk.StringVar(value="Player")
        self.create_radiobuttons(self.choice_window, self.first_player_var, [("Player", "Player"), ("Computer", "Computer")])
        
        # Difficulty levels
        tk.Label(self.choice_window, text="Select Difficulty:").pack(pady=5)
        self.difficulty_var = tk.StringVar(value="easy")
        difficulties = [
            ("Easy (You always win)", "easy"),
            ("Medium (50/50 chance)", "medium"),
            ("Hard (Never loses)", "hard")
        ]
        self.create_radiobuttons(self.choice_window, self.difficulty_var, difficulties)
        
        tk.Button(self.choice_window, text="Start Game", command=self.start_game).pack(pady=10)

    def create_radiobuttons(self, parent, variable, options):
        for text, value in options:
            tk.Radiobutton(parent, text=text, variable=variable, value=value).pack()

    def start_game(self):
        self.player_marker = self.marker_var.get()
        self.computer_marker = "O" if self.player_marker == "X" else "X"
        self.player_goes_first = (self.first_player_var.get() == "Player")
        self.difficulty = self.difficulty_var.get()
        self.current_player = self.player_marker if self.player_goes_first else self.computer_marker
        self.choice_window.destroy()
        self.create_board()
        if not self.player_goes_first:
            self.root.after(500, self.computer_move)

    def create_board(self):
        self.buttons = []
        for i in range(9):
            button = tk.Button(self.root, text=" ", font=("Helvetica", 24), width=5, height=2,
                               command=lambda idx=i: self.on_button_click(idx))
            button.grid(row=i // 3, column=i % 3)
            self.buttons.append(button)

    def on_button_click(self, idx):
        if self.game_over or self.board[idx] != " " or self.current_player != self.player_marker:
            return
        
        self.make_move(idx, self.player_marker)
        if self.check_winner(self.player_marker):
            self.end_game(f"Player {self.player_marker} wins!")
        elif self.is_draw():
            self.end_game("It's a draw!")
        else:
            self.current_player = self.computer_marker
            self.root.after(500, self.computer_move)

    def computer_move(self):
        if self.game_over:
            return

        if self.difficulty == "easy":
            move = self.find_losing_move()
        elif self.difficulty == "medium":
            move = self.find_medium_move()
        else:  # hard
            move = self.find_optimal_move()
            
        self.make_move(move, self.computer_marker)
        if self.check_winner(self.computer_marker):
            self.end_game(f"Computer ({self.computer_marker}) wins!")
        elif self.is_draw():
            self.end_game("It's a draw!")
        else:
            self.current_player = self.player_marker

    def make_move(self, idx, player):
        self.board[idx] = player
        self.buttons[idx].config(text=player, state=tk.DISABLED)

    def find_losing_move(self):
        # Intentionally pick a move that does not win immediately, so the player wins
        available = [i for i, spot in enumerate(self.board) if spot == " "]
        for move in available:
            self.board[move] = self.computer_marker
            if not self.check_winner(self.computer_marker):
                self.board[move] = " "
                return move
            self.board[move] = " "
        return random.choice(available)

    def find_medium_move(self):
        return self.find_optimal_move() if random.random() < 0.5 else self.find_random_move()

    def find_optimal_move(self):
        best_score = -float('inf')
        best_moves = []
        # Preference: center > corners > edges
        preference = {4: 3, 0: 2, 2: 2, 6: 2, 8: 2, 1: 1, 3: 1, 5: 1, 7: 1}
        
        for i in range(9):
            if self.board[i] == " ":
                self.board[i] = self.computer_marker
                score = self.minimax(0, False, -float('inf'), float('inf'))
                self.board[i] = " "
                if score > best_score:
                    best_score = score
                    best_moves = [i]
                elif score == best_score:
                    best_moves.append(i)
        
        # Use heuristic preference if multiple moves yield the same score.
        best_moves.sort(key=lambda x: preference.get(x, 0), reverse=True)
        return best_moves[0] if best_moves else -1

    def find_random_move(self):
        available = [i for i, spot in enumerate(self.board) if spot == " "]
        return random.choice(available)

    def minimax(self, depth, is_maximizing, alpha, beta):
        if self.check_winner(self.computer_marker):
            return 1
        if self.check_winner(self.player_marker):
            return -1
        if self.is_draw():
            return 0

        if is_maximizing:
            best_score = -float('inf')
            for i in range(9):
                if self.board[i] == " ":
                    self.board[i] = self.computer_marker
                    score = self.minimax(depth + 1, False, alpha, beta)
                    self.board[i] = " "
                    best_score = max(score, best_score)
                    alpha = max(alpha, score)
                    if beta <= alpha:
                        break
            return best_score
        else:
            best_score = float('inf')
            for i in range(9):
                if self.board[i] == " ":
                    self.board[i] = self.player_marker
                    score = self.minimax(depth + 1, True, alpha, beta)
                    self.board[i] = " "
                    best_score = min(score, best_score)
                    beta = min(beta, score)
                    if beta <= alpha:
                        break
            return best_score

    def check_winner(self, player):
        return any(all(self.board[i] == player for i in combo) for combo in self.WIN_COMBOS)

    def is_draw(self):
        return " " not in self.board

    def end_game(self, message):
        self.game_over = True
        messagebox.showinfo("Game Over", message)
        self.reset_board()

    def reset_board(self):
        self.board = [" " for _ in range(9)]
        self.game_over = False
        for button in self.buttons:
            button.config(text=" ", state=tk.NORMAL)
        self.reset_game_state()
        self.create_choice_window()

if __name__ == "__main__":
    root = tk.Tk()
    game = TicTacToe(root)
    root.mainloop()



