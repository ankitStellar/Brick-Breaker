# Brick Breaker

Simple Brick Breaker game I made in Java. Everything is in one file, `BrickBreaker.java`, and it uses only Swing, so there's nothing to install.

You move the paddle, bounce the ball, and smash all the bricks. Easy to learn, hard to stop playing.

## How to run

You need Java 17 or newer. Then:

```bash
javac BrickBreaker.java
java BrickBreaker
```

That's it, the game window opens.

## Controls

- **Move paddle:** mouse, or arrow keys, or A / D
- **Launch ball:** Space or click
- **Pause:** P
- **Play again after game over:** Space

## Some things worth knowing

- Where the ball hits the paddle decides which way it flies. Hit it near the edge to send it sideways.
- Top rows give more points.
- You get 3 lives.
- After you clear a level you get 100 bonus points, and the next one has more rows and a faster ball.
- From level 3, the top two rows need two hits to break (they have a white border).
- The ball slowly speeds up as bricks disappear, so the end of a level is the hard part.

## Ideas for later

- Power-ups (bigger paddle, multi-ball)
- Sound
- Saving the high score
- Loading levels from files

Feel free to copy it, change it, or break it.
