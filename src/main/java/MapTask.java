import java.util.ArrayList;
import java.util.List;

public class MapTask implements Runnable {
    private final List<KeyValuePair<Character, Integer>> result;
    private final int count;
    private String[] words;

    public MapTask(String line) {
        this.result = new ArrayList<>();
        this.count = line.split(" ").length;
        this.words = line.toLowerCase().replaceAll("[^\\w\\s\\pL]", "").split(" ");
    }

    @Override
    public void run() {
        List<KeyValuePair<Character, Integer>> auxList = new ArrayList<>();
        for (String word : this.words) {
            auxList.clear();
            for (char letter : word.toCharArray()) {
                if (!checkIfKeyValueHasKey(letter, auxList)) {
                    auxList.add(new KeyValuePair<>(letter, 1));
                }
            }
            result.addAll(auxList);
        }
        auxList = null;
    }

    private boolean checkIfKeyValueHasKey(char letter, List<KeyValuePair<Character, Integer>> auxList) {
        for (KeyValuePair<Character, Integer> pair : auxList) {
            if (pair.getKey() == letter) return true;
        }
        return false;
    }

    public List<KeyValuePair<Character, Integer>> getResult() {
        return result;
    }

    public int getCount() {
        return count;
    }
}
