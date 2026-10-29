package nl.clubbaas.engine;

import java.util.Random;

/**
 * Randomgenerator (SplitMix64) waarvan de toestand één getal is. Die kunnen we opslaan, zodat een
 * geladen spel precies hetzelfde verder speelt. nextGaussian() houdt geen cache bij, om dezelfde reden.
 */
public class Rng extends Random {
    private static final long serialVersionUID = 1L;

    private long state;

    public Rng(long seed) {
        super(0);
        this.state = seed;
    }

    @Override
    public synchronized void setSeed(long seed) {
        this.state = seed;
    }

    long state() {
        return state;
    }

    void setState(long state) {
        this.state = state;
    }

    @Override
    protected int next(int bits) {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (int) (z >>> (64 - bits));
    }

    /** Box-Muller zonder tweede waarde te bewaren. */
    @Override
    public double nextGaussian() {
        double u1 = Math.max(1e-300, nextDouble());
        double u2 = nextDouble();
        return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }
}
