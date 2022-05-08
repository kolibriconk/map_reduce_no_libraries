import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MapTask implements Callable<KeyValuePair<Integer, List<KeyValuePair<Character, Float>>>> {
    private String input;

    public static final Character[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm'
            , 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'à', 'á', 'â', 'ã', 'ä', 'å', 'æ'
            , 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', 'ø', 'ù', 'ú', 'û'
            , 'ü', 'ý', 'þ', 'ÿ'};

    private static final List<KeyValuePair<Character, Float>> alphabetList = new ArrayList<>();

    static {
        for (Character c : alphabet) {
            alphabetList.add(new KeyValuePair<>(c, 1f));
        }
    }

    private static final String REGEX_PATTERN = "[^abcdefghijklmnopqrstuvwxyzàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ ]";

    public MapTask(String input) {
        this.input = input;
    }

    private KeyValuePair<Character, Float> getKeyValuePair(Character letter) {
        for (KeyValuePair<Character, Float> pair : alphabetList) {
            if (pair.getKey() == letter) return pair;
        }
        return new KeyValuePair<>(letter, 1f);
    }

    @Override
    public KeyValuePair<Integer, List<KeyValuePair<Character, Float>>> call() {
        int count = input.split(" ").length;
        String[] words = input.toLowerCase().replaceAll(REGEX_PATTERN, "").split(" ");
        input = null;
        List<Character> auxList = new ArrayList<>();
        List<KeyValuePair<Character, Float>> result = new ArrayList<>();
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
