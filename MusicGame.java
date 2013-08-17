import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.sound.midi.MidiUnavailableException;
import java.util.Timer;
import java.util.TimerTask;

public class MusicGame
{
    public static void main(String ... orange) throws Exception
    {
        JFrame frame = new JFrame("Music Game!");
        frame.setSize(560, 600);
        frame.setResizable(false);
        MyPanel panel = new MyPanel();
        panel.setLayout(null);
        frame.add(panel);
        panel.setLocation(0, 0);
        panel.setSize(560, 600);
        panel.setFocusable(true);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MyPanel extends JPanel
{
    private static final int N = 40; // dimension of grid
    private static final int L = 13; // tile size in pixels
    private static final int D = 20; // margin in pixels

    private static final int WIDTH = 570; // panel width
    private static final int BUTTON_WIDTH = WIDTH / 4;
    private static final int BUTTON_HEIGHT = 30;

    private static final int START_PITCH = 30;
    private static final int BPM = 240;

    private boolean[][] grid;
    private boolean color = true; // drawing or erasing
    private JButton finalButton; // finish or guess button
    private boolean guessMode = false; // whether creating or guessing
    private String answer = null;

    /* Draw blue line */
    private int songDirection; // what direction listening to sound
    private int index = -2; // where the bar is
    private boolean playing = false; // if playing sound

    MyPanel()
    {
        grid = new boolean[N][N];
        setBackground(Color.LIGHT_GRAY);
        addMouseListener(new MyListener());
        addMouseMotionListener(new MyMotionListener());
        addKeyListener(new MyKeyListener());

        JButton drawButton = new JButton("DRAW");
        drawButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        drawButton.setLocation(0, WIDTH);
        drawButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                color = true;
                requestFocus();
            }
        });
        add(drawButton);

        JButton eraseButton = new JButton("ERASE");
        eraseButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        eraseButton.setLocation(BUTTON_WIDTH, WIDTH);
        eraseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                color = false;
                requestFocus();
            }
        });
        add(eraseButton);

        JButton clearButton = new JButton("CLEAR");
        clearButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        clearButton.setLocation(2 * BUTTON_WIDTH, WIDTH);
        clearButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (guessMode)
                    return;
                clearScreen();
                repaint();
                requestFocus();
            }
        });
        add(clearButton);

        finalButton = new JButton("FINISH");
        finalButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        finalButton.setLocation(3 * BUTTON_WIDTH, WIDTH);
        finalButton.addActionListener(new FinishActionListener());
        add(finalButton);
    }

    public class FinishActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            answer = JOptionPane.showInputDialog(null, "Enter answer (empty to give up)");
            finalButton.removeActionListener(finalButton.getActionListeners()[0]);
            finalButton.setText("GUESS");
            guessMode = true;
            finalButton.addActionListener(new GuessActionListener());
            repaint();
            requestFocus();
        }
    }

    public class GuessActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String guess = JOptionPane.showInputDialog(null, "Enter guess:");
            if (guess.equals(answer))
            {
                JOptionPane.showMessageDialog(null, "Good job!");
            }
            else if (!guess.equals(""))
            {
                JOptionPane.showMessageDialog(null, "Incorrect.");
            }
            if (guess.equals(answer) || guess.equals(""))
            {
                finalButton.removeActionListener(finalButton.getActionListeners()[0]);
                finalButton.setText("FINISH");
                guessMode = false;
                finalButton.addActionListener(new FinishActionListener());
                repaint();
            }
            requestFocus();
        }
    }

    public void clearScreen()
    {
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                grid[i][j] = false;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        boolean scanning = index > 0 && index < N;
        if (scanning)
        {
            g.setColor(Color.BLUE);
            if (songDirection == 0) // right
                g.fillRect(D + index * L, 0, L, WIDTH);
            else if (songDirection == 1) // left
                g.fillRect(D + (N - index) * L - L, 0, L, WIDTH);
            else if (songDirection == 2) // down
                g.fillRect(0, D + index * L, WIDTH, L);
            else if (songDirection == 3) // up
                g.fillRect(0, D + (N - index) * L - L, WIDTH, L);
        }

        g.setColor(Color.BLACK);
        for (int i = 0; i <= N; i++)
        {
            g.drawLine(D + i * L, D, D + i * L, D + N * L);
            g.drawLine(D, D + i * L, D + N * L, D + i * L);
        }

        g.setColor(Color.GRAY);
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                if (grid[i][j] && !guessMode)
                    g.fillRect(D + i * L + 1, D + j * L + 1, L - 1, L - 1);

        if (scanning)
        {
            g.setColor(Color.DARK_GRAY);
            for (int move = 0; move < N; move++)
            {
                int i, j;
                if (songDirection == 0)
                {
                    i = index; j = move;
                }
                else if (songDirection == 1)
                {
                    i = N - 1 - index; j = move;
                }
                else if (songDirection == 2)
                {
                    i = move; j = index;
                }
                else if (songDirection == 3)
                {
                    i = move; j = N - 1 - index;
                }
                else
                    break;
                if (grid[i][j] && !guessMode)
                    g.fillRect(D + i * L + 1, D + j * L + 1, L - 1, L - 1);
            }
        }
    }

    void setTile(int x, int y)
    {
        if (x < 0 || x >= N || y < 0 || y >= N)
            return;
        grid[x][y] = color;
    }

    class MyListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (guessMode)
                return;
            int x = e.getX(), y = e.getY();
            setTile((x - D) / L, (y - D) / L);
            repaint();
        }
    }

    class MyMotionListener extends MouseMotionAdapter
    {
        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (guessMode)
                return;
            int x = e.getX(), y = e.getY();
            setTile((x - D) / L, (y - D) / L);
            repaint();
        }
    }

    class MyKeyListener extends KeyAdapter
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            if (playing)
                return;

            switch (e.getKeyCode())
            {
                case KeyEvent.VK_RIGHT:
                    songDirection = 0;
                    break;
                case KeyEvent.VK_LEFT:
                    songDirection = 1;
                    break;
                case KeyEvent.VK_DOWN:
                    songDirection = 2;
                    break;
                case KeyEvent.VK_UP:
                    songDirection = 3;
                    break;
                default:
                    return;
            }
            try
            {
                final MusicPlayer player = new MusicPlayer(START_PITCH, BPM, 2);
                player.startSong(grid, songDirection);
                final Timer timer = new Timer();
                timer.schedule(new TimerTask()
                {
                    public void run()
                    {
                        index = player.getTickPosition();
                        repaint();
                        if (player.isRunning())
                            playing = true;
                        else if (playing && !player.isRunning())
                        {
                            playing = false;
                            timer.cancel();
                        }
                    }
                }, 0, 2);
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
            }
        }
    }
}
