import java.util.List;
import java.util.Random;

public class Position {

    private int posX;
    private int posY;

    public Position(int range)
    {
        posX = (new Random().nextInt(range) + 1)*(new Utils<Integer>().getRandomItem(List.of(-1,1)));
        posY = (new Random().nextInt(range) + 1)*(new Utils<Integer>().getRandomItem(List.of(-1,1)));
    }

    public Position(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public double computeEuclideanDistance(Position pos){
        return Math.sqrt(Math.pow(posX-pos.getPosX(),2) + Math.pow(posY-pos.getPosY(),2));
    }

    public double computeDistanceBLEWithRandomError(Position pos){
        double d = computeEuclideanDistance(pos);
        double err = Math.random() * 0.5;
        return d / (1 + err*(new Utils<Integer>().getRandomItem(List.of(-1,1))));
    }

    public void randomMove()
    {
        if(Math.random()<=0.5){
            posX = posX + (new Random().nextInt(5) + 1)*(new Utils<Integer>().getRandomItem(List.of(-1,1)));
            posY = posY + (new Random().nextInt(5) + 1)*(new Utils<Integer>().getRandomItem(List.of(-1,1)));
        }
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

}
