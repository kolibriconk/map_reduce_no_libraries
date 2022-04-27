import java.util.ArrayList;
import java.util.List;

public class MapTask implements Runnable {
    private final List<KeyValuePair<Character, Integer>> result;
    private final int count;
    private String[] words;

    private static final Character[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'à', 'á', 'â', 'ã', 'ä', 'å', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', 'ø', 'ù', 'ú', 'û', 'ü', 'ý', 'þ', 'ÿ'};

    private static final List<KeyValuePair<Character, Integer>> alphabetList = new ArrayList<>();

    static {
        for (Character c : alphabet) {
            alphabetList.add(new KeyValuePair<>(c, 1));
        }
    }

    public MapTask(String line) {
        this.result = new ArrayList<>();
        this.count = line.split(" ").length;
        this.words = line.toLowerCase().replaceAll("[^\\w\\s\\pL]", "").split(" ");
    }

    @Override
    public void run() {
        List<Character> auxList = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words[i].length(); j++) {
                if (!auxList.contains(words[i].charAt(j))) {
                    result.add(getKeyValuePair(words[i].charAt(j)));
                    auxList.add(words[i].charAt(j));
                }
            }
            auxList.clear();
        }
        this.words = null;
        auxList = null;
    }

    public List<KeyValuePair<Character, Integer>> getResult() {
        return result;
    }

    public int getCount() {
        return count;
    }

    private KeyValuePair<Character, Integer> getKeyValuePair(Character letter) {
        for (KeyValuePair<Character, Integer> pair : alphabetList) {
            if (pair.getKey() == letter) return pair;
        }
        return new KeyValuePair<>(letter, 1);
    }
}
