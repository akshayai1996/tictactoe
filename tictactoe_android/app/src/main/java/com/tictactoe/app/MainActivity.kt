package com.tictactoe.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var statusLabel: TextView
    private lateinit var gameGrid: GridLayout
    private lateinit var btnPlayAgain: TextView
    private lateinit var btnBack: TextView
    private lateinit var btnSound: TextView

    // Settings radios
    private lateinit var rbX: RadioButton
    private lateinit var rbO: RadioButton
    private lateinit var rbYou: RadioButton
    private lateinit var rbCPU: RadioButton
    private lateinit var rbEasy: RadioButton
    private lateinit var rbMed: RadioButton
    private lateinit var rbHard: RadioButton

    // Game cells
    private val cells = mutableListOf<TextView>()

    // Game state
    private val board = arrayOfNulls<String>(9)
    private var playerMark = "X"
    private var cpuMark = "O"
    private var turn = "X"
    private var difficulty = 1 // 0=Easy, 1=Med, 2=Hard
    private var gameOver = false
    private var soundEnabled = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        buildGrid()
        setupListeners()
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.viewFlipper)
        statusLabel = findViewById(R.id.statusLabel)
        gameGrid = findViewById(R.id.gameGrid)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        btnBack = findViewById(R.id.btnBack)
        btnSound = findViewById(R.id.btnSound)

        rbX = findViewById(R.id.rbX)
        rbO = findViewById(R.id.rbO)
        rbYou = findViewById(R.id.rbYou)
        rbCPU = findViewById(R.id.rbCPU)
        rbEasy = findViewById(R.id.rbEasy)
        rbMed = findViewById(R.id.rbMed)
        rbHard = findViewById(R.id.rbHard)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun buildGrid() {
        val cellSize = dpToPx(100)
        val margin = dpToPx(6)

        for (i in 0 until 9) {
            val cell = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    setMargins(margin, margin, margin, margin)
                    rowSpec = GridLayout.spec(i / 3)
                    columnSpec = GridLayout.spec(i % 3)
                }
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.cell_bg)
                gravity = Gravity.CENTER
                textSize = 36f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isClickable = true
                isFocusable = true
                tag = i

                setOnClickListener { onCellClick(i) }
            }
            cells.add(cell)
            gameGrid.addView(cell)
        }
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.btnStart).setOnClickListener { startGame() }
        btnPlayAgain.setOnClickListener { startGame() }
        btnBack.setOnClickListener { viewFlipper.displayedChild = 0 }
        btnSound.setOnClickListener {
            soundEnabled = !soundEnabled
            btnSound.text = if (soundEnabled) "Sound: ON" else "Sound: OFF"
        }
    }

    // --- GAME LOGIC ---

    private fun startGame() {
        playerMark = if (rbX.isChecked) "X" else "O"
        cpuMark = if (playerMark == "X") "O" else "X"
        difficulty = when {
            rbEasy.isChecked -> 0
            rbHard.isChecked -> 2
            else -> 1
        }
        turn = if (rbYou.isChecked) playerMark else cpuMark

        // Reset board
        for (i in 0 until 9) {
            board[i] = null
            cells[i].text = ""
            cells[i].isEnabled = true
            cells[i].background = ContextCompat.getDrawable(this, R.drawable.cell_bg)
            cells[i].setTextColor(ContextCompat.getColor(this, R.color.accent))
        }

        gameOver = false
        statusLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        btnPlayAgain.visibility = View.GONE
        btnBack.visibility = View.GONE
        updateStatus()

        viewFlipper.displayedChild = 1

        if (turn == cpuMark) {
            handler.postDelayed({ cpuMove() }, 800)
        }
    }

    private fun onCellClick(idx: Int) {
        if (gameOver || board[idx] != null || turn != playerMark) return

        makeMove(idx, playerMark)
        if (!checkEnd()) {
            turn = cpuMark
            updateStatus()
            handler.postDelayed({ cpuMove() }, 600)
        }
    }

    private fun cpuMove() {
        if (gameOver) return

        var idx = when (difficulty) {
            2 -> getBestMove()
            0 -> getWorstMove()
            else -> if (Random.nextInt(10) > 4) getBestMove() else getRandomMove()
        }
        if (idx == -1) idx = getRandomMove()
        if (idx == -1) return

        makeMove(idx, cpuMark)
        if (!checkEnd()) {
            turn = playerMark
            updateStatus()
        }
    }

    private fun makeMove(idx: Int, mark: String) {
        board[idx] = mark
        cells[idx].text = mark
        cells[idx].isEnabled = false
        playSound("tap")
    }

    private fun updateStatus() {
        if (!gameOver) {
            statusLabel.text = if (turn == playerMark) "Your Turn" else "Computer Thinking..."
        }
    }

    private fun checkEnd(): Boolean {
        val wins = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )

        for (combo in wins) {
            val a = board[combo[0]]
            if (a != null && a == board[combo[1]] && a == board[combo[2]]) {
                gameOver = true
                val isWin = a == playerMark

                for (idx in combo) {
                    if (isWin) {
                        cells[idx].background = ContextCompat.getDrawable(this, R.drawable.cell_win)
                        cells[idx].setTextColor(ContextCompat.getColor(this, R.color.win_color))
                    } else {
                        cells[idx].background = ContextCompat.getDrawable(this, R.drawable.cell_lose)
                        cells[idx].setTextColor(ContextCompat.getColor(this, R.color.lose_color))
                    }
                }

                statusLabel.text = if (isWin) "Victory!" else "Defeat!"
                statusLabel.setTextColor(
                    ContextCompat.getColor(this, if (isWin) R.color.win_color else R.color.lose_color)
                )
                playSound(if (isWin) "win" else "lose")
                showEndButtons()
                return true
            }
        }

        if (board.all { it != null }) {
            gameOver = true
            statusLabel.text = "It's a Draw!"
            showEndButtons()
            return true
        }

        return false
    }

    private fun showEndButtons() {
        btnPlayAgain.visibility = View.VISIBLE
        btnBack.visibility = View.VISIBLE
    }

    // --- AI (Minimax) ---

    private fun minimax(bd: Array<String?>, depth: Int, isMax: Boolean): Int {
        if (checkWinLogic(bd, cpuMark)) return 10 - depth
        if (checkWinLogic(bd, playerMark)) return depth - 10
        if (bd.all { it != null }) return 0

        if (isMax) {
            var best = -1000
            for (i in 0 until 9) {
                if (bd[i] == null) {
                    bd[i] = cpuMark
                    best = maxOf(best, minimax(bd, depth + 1, false))
                    bd[i] = null
                }
            }
            return best
        } else {
            var best = 1000
            for (i in 0 until 9) {
                if (bd[i] == null) {
                    bd[i] = playerMark
                    best = minOf(best, minimax(bd, depth + 1, true))
                    bd[i] = null
                }
            }
            return best
        }
    }

    private fun getBestMove(): Int {
        var bestVal = -1000
        var bestMove = -1
        for (i in 0 until 9) {
            if (board[i] == null) {
                board[i] = cpuMark
                val moveVal = minimax(board, 0, false)
                board[i] = null
                if (moveVal > bestVal) { bestVal = moveVal; bestMove = i }
            }
        }
        return bestMove
    }

    private fun getWorstMove(): Int {
        var worstVal = 1000
        var worstMove = -1
        for (i in 0 until 9) {
            if (board[i] == null) {
                board[i] = cpuMark
                val moveVal = minimax(board, 0, false)
                board[i] = null
                if (moveVal < worstVal) { worstVal = moveVal; worstMove = i }
            }
        }
        return if (worstMove != -1) worstMove else getRandomMove()
    }

    private fun getRandomMove(): Int {
        val empty = (0 until 9).filter { board[it] == null }
        return if (empty.isEmpty()) -1 else empty.random()
    }

    private fun checkWinLogic(bd: Array<String?>, who: String): Boolean {
        val wins = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        return wins.any { bd[it[0]] == who && bd[it[1]] == who && bd[it[2]] == who }
    }

    // --- SOUND (matching Python/C#/Java frequencies) ---

    private fun playSound(type: String) {
        if (!soundEnabled) return
        Thread {
            try {
                when (type) {
                    "tap" -> playTone(700, 80)
                    "win" -> {
                        playTone(523, 120); Thread.sleep(30)
                        playTone(659, 120); Thread.sleep(30)
                        playTone(784, 120); Thread.sleep(30)
                        playTone(1046, 120)
                    }
                    "lose" -> {
                        playTone(200, 250); Thread.sleep(60)
                        playTone(150, 250); Thread.sleep(60)
                        playTone(100, 250)
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun playTone(freq: Int, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val samples = ShortArray(numSamples) { i ->
            (sin(2.0 * PI * i * freq / sampleRate) * Short.MAX_VALUE * 0.3).toInt().toShort()
        }

        val bufSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufSize, numSamples * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, numSamples)
        track.play()
        Thread.sleep(durationMs.toLong())
        track.stop()
        track.release()
    }
}
