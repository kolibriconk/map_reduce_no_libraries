import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MapTask implements Callable<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> {
    private String input;

    private static final Character[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm'
            , 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'à', 'á', 'â', 'ã', 'ä', 'å', 'æ'
            , 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', 'ø', 'ù', 'ú', 'û'
            , 'ü', 'ý', 'þ', 'ÿ'};

    private static final List<KeyValuePair<Character, List<Integer>>> alphabetList = new ArrayList<>();

    static {
        for (Character c : alphabet) {
            KeyValuePair<Character, List<Integer>> pair = new KeyValuePair<>(c, new ArrayList<>());
            pair.getValue().add(1);
            alphabetList.add(pair);
        }
    }

    public MapTask(String input) {
        this.input = input;
    }

    private KeyValuePair<Character, List<Integer>> getKeyValuePair(Character letter) {
        for (KeyValuePair<Character, List<Integer>> pair : alphabetList) {
            if (pair.getKey() == letter) return pair;
        }
        KeyValuePair<Character, List<Integer>> pair = new KeyValuePair<>(letter, new ArrayList<>());
        pair.getValue().add(1);
        return pair;
    }

    @Override
    public KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> call() {
        int count = input.split(" ").length;
        String[] words = input.toLowerCase().replaceAll("[^\\w\\s\\pL]", "").split(" ");
        input = null;
        List<Character> auxList = new ArrayList<>();
        List<KeyValuePair<Character, List<Integer>>> result = new ArrayList<>();
        for (String word : words) {
            for (int j = 0; j < word.length(); j++) {
                if (!auxList.contains(word.charAt(j))) {
                    result.add(getKeyValuePair(word.charAt(j)));
                    auxList.add(word.charAt(j));
                }
            }
            auxList.clear();
        }
        words = null;
        auxList = null;
        return new KeyValuePair<>(count, result);
    }
}
