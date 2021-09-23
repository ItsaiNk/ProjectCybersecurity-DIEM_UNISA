import java.util.List;
import java.util.Random;

public class Utils <T>{

    public T getRandomItem(List<T> list)
    {
        return list.get(new Random().nextInt(list.size()));
    }
}
