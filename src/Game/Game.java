package Game;

import Bug.Bug;
import Bug.BugOne;
import Grid.BugGrid;
import Grid.CollisionDetector;
import Grid.PlayerGrid;
import Player.PlayerController;
import Props.CoffeeCup;
import Props.Cpu;
import org.academiadecodigo.simplegraphics.graphics.Color;
import org.academiadecodigo.simplegraphics.graphics.Text;
import org.academiadecodigo.simplegraphics.pictures.Picture;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class Game {

    private PlayerController playerController;
    private BugGrid bugGrid;
    private Cpu cpu;
    private PlayerGrid playerGrid;
    private CollisionDetector collisionDetector;
    private Bug[] bugs;
    private CoffeeCup[] coffeeCups;
    private Text bugsKillScore;
    private Text coffeeScore;
    private Text cpuScore;
    private int coffeesToSpend;
    private Text coffeeToSpend;
    private Text stageNumber;
    private static Color color = new Color(199, 193, 169);
    private static Color colorRed = new Color(140, 48, 48);
    private final int DELAY = 800;
    public int stage = 1;
    private final static int COL_NUM = 26;
    private final static int ROW_NUM = 26;
    private final static double CPU_OFFSET = 7.4;
    public boolean isStarted;

    public void playerInit() {

        try {
            URL audioUrl = getClass().getResource("/8bit-st.wav");
            AudioInputStream audio = AudioSystem.getAudioInputStream(audioUrl);

            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();

            FloatControl gainControl =
                    (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-15.0f);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }

        bugGrid = new BugGrid(COL_NUM, ROW_NUM);
        playerGrid = new PlayerGrid(COL_NUM, ROW_NUM);
        cpu = new Cpu(playerGrid);
        collisionDetector = new CollisionDetector();

        setTexts();

        playerController = new PlayerController(playerGrid, cpu, collisionDetector, this);
        playerController.init();

        Picture startScreen = new Picture(10,10, "startscreen.jpg");
        startScreen.draw();

        while(!isStarted){
            Thread.yield();
        }

        startScreen.delete();
        try {
            Thread.sleep(1300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setStage(stage);
    }


    public void setTexts() {

        bugsKillScore = new Text( 885, 216, "0");
        bugsKillScore.grow(8,8);
        bugsKillScore.setColor(color);
        bugsKillScore.draw();

        coffeeScore = new Text(885, 245, "0");
        coffeeScore.grow(8,8);
        coffeeScore.setColor(color);
        coffeeScore.draw();

        coffeeToSpend = new Text(857, 336, "X");
        coffeeToSpend.grow(8, 8);
        coffeeToSpend.setColor(colorRed);
        coffeeToSpend.draw();

        stageNumber = new Text(875, 426, String.valueOf(stage));
        stageNumber.grow(8, 8);
        stageNumber.setColor(color);
        stageNumber.draw();

        cpuScore = new Text(cpu.getCpuHealthXPos()+CPU_OFFSET+10, cpu.getCpuHealthYPos()+10, "100");
        cpuScore.grow(6*3, 8);
        cpuScore.draw();

    }

    public void setStage(int stage) {

        coffeesToSpend = stage - 2;

        if(coffeeCups != null){
            for (CoffeeCup coffeeCup : coffeeCups) {
                coffeeCup.setWasted();
            }
        }

        if (stage % 2 == 0 && stage != 2) {
            coffeeCups = new CoffeeCup[stage / 2];
            for (int i = 0; i < coffeeCups.length; i++) {
                coffeeCups[i] = new CoffeeCup(playerGrid);
            }
        }
        collisionDetector.setCoffeeCups(coffeeCups);


        bugs = new Bug[stage + stage * 2];
        for (int i = 0; i < bugs.length; i++) {
            bugs[i] = new BugOne(bugGrid, cpu, collisionDetector);
        }
        collisionDetector.setBugsArray(bugs, stage);

        startStage();

    }

    public void startStage() {

        while (bugs[0].getStageDeadBugs() < bugs.length && cpu.getHealth() > 0) {

            try{
                Thread.sleep(DELAY);
            }catch(InterruptedException | IllegalArgumentException e) {
                e.printStackTrace();
            }

            stageUpdate();

            for (int i = 0; i < bugs.length; i++) {
                if(cpu.getHealth() > 0) {
                    bugs[i].bugMove();
                    bugKillScoreUpdate();
                    cpuScoreUpdate();
                    if(stage > 3) {
                        coffeeScoreUpdate();
                        coffeeToSpendUpdate();
                    }
                    if (collisionDetector.collisionDetector(bugs[i].getBugPosition(), playerController.getPlayer().getPlayerPosition())) {
                        bugs[i].setIsDead();
                    }
                }
            }
        }

        if(cpu.getHealth() < 1) {

            Picture picture = new Picture(10, 10, "gameover.jpg");
            picture.draw();

            Text finalScore = new Text(478,370, String.valueOf(bugs[0].getDeadBugs()));
            finalScore.setColor(colorRed);
            finalScore.grow(15*String.valueOf(bugs[0].getDeadBugs()).length(),20);
            finalScore.draw();

            isStarted = false;

            ScoreSaver scoreSaver = new ScoreSaver(bugs[0].getDeadBugs());

            Text bestScore = new Text(587, 419, String.valueOf(scoreSaver.getHighScore()));
            bestScore.setColor(color);
            bestScore.grow(7*String.valueOf(scoreSaver.getHighScore()).length(), 8);
            bestScore.draw();

            while (!isStarted){
                Thread.yield();
            }

            finalScore.delete();
            bestScore.delete();
            picture.delete();
            restartGame();
            return;

        }
        setStage(++stage);

    }

    public void restartGame(){
        cpu.setHealth();
        CoffeeCup prop = new CoffeeCup(playerGrid);
        prop.setPickedCoffees();
        prop.setWasted();
        for(Bug bug: bugs){
            bug.setIsDead();
        }
        bugs[0].resetBugs();
        bugsKillScore.delete();
        cpuScore.delete();
        coffeeToSpend.delete();
        stageNumber.delete();
        coffeeScore.delete();
        stage = 1;
        setTexts();
        setStage(1);
    }

    public void bugKillScoreUpdate(){
        bugsKillScore.delete();
        bugsKillScore = new Text(885, 216, String.valueOf(bugs[0].getDeadBugs()));
        bugsKillScore.grow(8*String.valueOf(bugs[0].getDeadBugs()).length(),8);
        bugsKillScore.setColor(color);
        bugsKillScore.draw();
    }

    public void cpuScoreUpdate(){
        cpuScore.delete();
        cpuScore = new Text(cpu.getCpuHealthXPos()+CPU_OFFSET+10, cpu.getCpuHealthYPos()+10, String.valueOf(cpu.getHealth()));
        cpuScore.grow(6*String.valueOf(cpu.getHealth()).length(),8);
        cpuScore.draw();
    }

    public void coffeeScoreUpdate(){
        coffeeScore.setText(String.valueOf(coffeeCups[0].getPickedCoffees()));
    }

    public void coffeeToSpendUpdate(){
        coffeeToSpend.delete();
        coffeeToSpend = new Text(859, 336, String.valueOf(coffeesToSpend));
        coffeeToSpend.setColor(color);
        coffeeToSpend.grow(8*String.valueOf(coffeesToSpend).length(), 8);
        coffeeToSpend.draw();
    }

    public void stageUpdate(){
        stageNumber.setText(String.valueOf(stage));
    }

}
