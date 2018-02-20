import com.kpsychas.lib.Matcher;
import com.kpsychas.lib.Pattern;
import com.kpsychas.lib.PatternSyntaxException;

public class App {
    public static void main(String[] args) {
        try {
            Pattern p = Pattern.compile("((a*)b)+[^c]([a-b])?");
            p.printPattern();

            Matcher m = p.matcher("aabaabab");
            m.printMatch();

        } catch (PatternSyntaxException e) {
            e.printStackTrace();
        }
    }
}
