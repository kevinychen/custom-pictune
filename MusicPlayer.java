import javax.sound.midi.*;

public class MusicPlayer
{
    private Sequencer sequencer;
    private Track track;
    private int startPitch;
    private int beatsPerMinute;

    public MusicPlayer(int startPitch, int beatsPerMinute, int ticksPerQuarterNote) throws MidiUnavailableException, InvalidMidiDataException
    {
        this.sequencer = MidiSystem.getSequencer();
        Sequence sequence = new Sequence(Sequence.PPQ, ticksPerQuarterNote);
        this.startPitch = startPitch;
        this.beatsPerMinute = beatsPerMinute;
        this.track = sequence.createTrack();
        sequencer.setSequence(sequence);
    }

    private void addMidiEvent(int eventType, int note, int tick) throws InvalidMidiDataException
    {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(eventType, 0, note, 100);
        MidiEvent event = new MidiEvent(msg, tick);
        this.track.add(event);
    }

    public void addNote(int note, int startTick, int numTicks)
    {
        try
        {
            addMidiEvent(ShortMessage.NOTE_ON, note, startTick);
            addMidiEvent(ShortMessage.NOTE_OFF, note, startTick + numTicks);
        }
        catch (InvalidMidiDataException e)
        {
            e.printStackTrace();
        }
    }

    boolean hasNote(boolean[][] grid, int row, int col, int direction)
    {
        int N = grid.length;
        switch (direction)
        {
            case 0: return grid[row][N - 1 - col];
            case 1: return grid[N - 1 - row][N - 1 - col];
            case 2: return grid[col][row];
            case 3: return grid[col][N - 1 - row];
            default: return false;
        }
    }

    static final int[] dPitches = {0, 2, 4, 5, 7, 9, 11};
    public int getPitch(int delta)
    {
        return startPitch + 12 * (delta / 7) + dPitches[delta % 7];
    }

    public void play() throws MidiUnavailableException
    {
        sequencer.open();
        sequencer.setTempoInBPM(this.beatsPerMinute);
        sequencer.start();
        while (sequencer.isRunning())
            Thread.yield();
        sequencer.close();
    }

    public void startSong(final boolean[][] grid, final int direction)
    {
        new Thread()
        {
            public void run()
            {
                try
                {
                    int N = grid.length;
                    for (int row = 0; row < N; row++)
                        for (int col = 0; col < N; col++)
                            if (hasNote(grid, row, col, direction))
                                addNote(getPitch(col), row, 1);
                    addNote(0, N, 0);

                    play();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public int getTickPosition()
    {
        return (int)sequencer.getTickPosition();
    }

    public boolean isRunning()
    {
        return sequencer.isRunning();
    }
}
