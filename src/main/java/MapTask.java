import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Jose Antonio Ramos Andrades - 1565479
 * @author Victor Sancho Aguilera - 1529721
 * MapTask class is responsible for reading the input lines and convert it to key-value pairs.
 * It implements the Callable interface and returns a key-value pair with the key being the
 * word count and the value being a list of key-value pairs that are the characters and their counts.
 */
public class MapTask implements Callable<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> {
    private String input;

    //This object is used to avoid the creation of multiple objects of the same type;
    private static final Character[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm'
            , 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'à', 'á', 'â', 'ã', 'ä', 'å', 'æ'
            , 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', 'ø', 'ù', 'ú', 'û'
            , 'ü', 'ý', 'þ', 'ÿ'};

    //This object is used to avoid the creation of multiple objects of the same type;
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

    /**
     * This method is used to get the key-value pair for a given character from the static object.
     *
     * @param letter the character to be searched for.
     * @return the key-value pair for the given character.
     */
    private KeyValuePair<Character, List<Integer>> getKeyValuePair(Character letter) {
        for (KeyValuePair<Character, List<Integer>> pair : alphabetList) {
            if (pair.getKey() == letter) return pair;
        }
        KeyValuePair<Character, List<Integer>> pair = new KeyValuePair<>(letter, new ArrayList<>());
        pair.getValue().add(1);
        return pair;
    }

    //REGEX TO DELETE ALL EXCEPT ALPHA
    private static final String REGEX_PATTERN = "[^abcdefghijklmnopqrstuvwxyzàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ ]";

    @Override
    public KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> call() {
        int count = input.split(" ").length; //The count of the words in the input.
        //Replace all characters not matching the regex pattern and split the input into words.
        String[] words = input.toLowerCase().replaceAll(REGEX_PATTERN, "").split(" ");
        input = null; //Deallocate the input.
        List<Character> auxList = new ArrayList<>();
        List<KeyValuePair<Character, List<Integer>>> result = new ArrayList<>();

        for (String word : words) {
            for (int j = 0; j < word.length(); j++) {
                //For each character in the word search for the key-value pair
                // in the static object if this character is not already in the list.
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
