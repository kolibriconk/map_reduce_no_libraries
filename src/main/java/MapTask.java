import java.util.ArrayList;
import java.util.List;

public class MapTask implements Runnable {
    private String line;
    private final List<KeyValuePair<Character, Integer>> result;
    private int count;

    public MapTask(String line) {
        this.line = line;
        this.result = new ArrayList<>();
        this.count = line.length();
    }

    @Override
    public void run() {
        this.count = this.line.split(" ").length;
        this.line = this.line.toLowerCase();
        this.line = line.replaceAll("[^\\w\\s\\pL]", "");

        List<KeyValuePair<Character, Integer>> auxList = new ArrayList<>();
        for (String word : this.line.split(" ")) {
            auxList.clear();
            for (char letter : word.toCharArray()) {
                if(!checkIfKeyValueHasKey(letter, auxList)) {
                    auxList.add(new KeyValuePair<>(letter, 1));
                }
            }
            result.addAll(auxList);
        }
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
