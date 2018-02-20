package com.kpsychas.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by kon on 3/5/2017.
 */
class PatternTest {
    @Test
    void basicTest() {
        Pattern p;
        try {
            p = Pattern.compile("a");
            assertTrue(p.matches("a"));
            assertFalse(p.matches("b"));
            p = Pattern.compile("Zz+");
            assertTrue(p.matches("Zzzzzzzzz"));
            assertFalse(p.matches("ZZzzzzzzz"));
            p = Pattern.compile("[1-9][0-9]*");
            assertTrue(p.matches("1"));
            assertFalse(p.matches("099"));
            p = Pattern.compile("1(spam)?2");
            assertFalse(p.matches("111spam222"));
            assertTrue(p.matches("111spam222", 2));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void rangeTest() {
        Pattern p;
        try {
            p = Pattern.compile("[a-b]*c");
            assertTrue(p.matches("aabaababc"));
            p = Pattern.compile("[^a-b][a-b][a-b]");
            assertTrue(p.matches("cab"));
            assertTrue(p.matches("cabs"));
            assertFalse(p.matches("cat"));
            p = Pattern.compile("[A-Z][a-z]*[A-Z][a-z]*[0-9]");
            assertTrue(p.matches("RippedHippo6"));
            assertFalse(p.matches("Matt7"));
            assertFalse(p.matches("MrRobot"));
            p = Pattern.compile("[A-Fghi12-9]*End");
            assertTrue(p.matches("hi5End"));
            assertFalse(p.matches("AFKEnd"));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void groupTest() {
        Pattern p;
        try {
            p = Pattern.compile("(a?b)(a+b)(a*b)");
            assertTrue(p.matches("babaabab"));
            assertFalse(p.matches("abbab"));
            p = Pattern.compile("(a?b)*c");
            assertTrue(p.matches("abbabc"));
            assertFalse(p.matches("baabbc"));
            p = Pattern.compile("(a?(bc)+)*d");
            assertTrue(p.matches("bcbcabcd"));
            assertFalse(p.matches("abcaabcd"));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void advancedTest() {
        Pattern p;
        try {
            p = Pattern.compile("(Move([KQRBN]?[a-h][1-8])+)*End");
            assertTrue(p.matches("Movee4e5MoveNf3Nc6MoveBb5a6End"));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void backtrackTest() {
        Pattern p;
        try {
            p = Pattern.compile("(a*b)+ab");
            assertTrue(p.matches("aabaabab"));
            p = Pattern.compile("(a*b)+[^a]+");
            assertFalse(p.matches("aabaabab"));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void emptyTest() {
        Pattern p;
        try {
            p = Pattern.compile("a()b");
            assertTrue(p.matches("ab"));
            p = Pattern.compile("a()*b");
            assertTrue(p.matches("ab"));
            p = Pattern.compile("a[]b");
            assertFalse(p.matches("acb")); // [] Does not match with any character
            p = Pattern.compile("a[]*b");
            assertTrue(p.matches("ab"));
            p = Pattern.compile("a[^]b");
            assertTrue(p.matches("acb")); // [^] Matches with all characters
            p = Pattern.compile("a[^]*b");
            assertTrue(p.matches("ab"));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void multiTest() {
        Pattern p;
        try {
            p = Pattern.compile("[ace]");
            assertTrue(p.matches("a"));
            assertFalse(p.matches("b"));
            assertTrue(p.matches("c"));
            assertFalse(p.matches("d"));
            assertTrue(p.matches("e"));
            assertFalse(p.matches("f"));
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
    }
    @Test
    void syntaxTest() {
        Pattern p;
        String[] sArray = {"[", "]", "(", "?", "[a^b]", "^[a]", "[a^]", "a**b", "a?+b", "[a-]", "[-b]",
                "(()(())", "(())())", "[[]]", "*a"};
        for(String s: sArray) {
            try {
                p = Pattern.compile(s);
                fail("Pattern: " + s + " should not be valid");
            } catch (PatternSyntaxException e) {
                /* Test fails only is there is no exception */
            }
        }
    }

    @Test
    void semanticTest() {
        Pattern p;
        String[] sArray = {"[a-Z]", "[B-y]", "[9-0]", "[x-c]", "[W-D]"};
        for(String s: sArray) {
            try {
                p = Pattern.compile(s);
                fail("Pattern: " + s + " should not be valid");
            } catch (PatternSyntaxException e) {
                /* Test fails only is there is no exception */
                e.getMessage();
            }
        }
    }

    @Test
    /* Useful for coverage */
    void printTest() {
        Pattern p = null;
        Matcher m;
        try {
            p = Pattern.compile("((a*)b)+([^a]+)()");
            p.printPattern();
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
        m = p.matcher("aabaabab");
        m.printMatch();
        m = p.matcher("aabaabba");
        m.printMatch();
        m.match.printSelf();
    }

    @Test
    void matcherTest() {
        Pattern p = null;
        Matcher m;
        try {
            p = Pattern.compile("((a*)b)+([^a]+)()");
        } catch (PatternSyntaxException e) {
            fail(e.getMessage());
        }
        m = p.matcher("aabaaabab");
        assertFalse(m.matches());
        try {
            m.group();
            fail("There should be no group");
        } catch (IllegalStateException e) {
            /* Test fails only is there is no exception */
        }
        m = p.matcher("aabaaabba");
        assertTrue(m.matches());
        assertEquals(m.group(), "aabaaabb");
        assertEquals(m.group(1), "aaab");
        assertEquals(m.group(4), "");
        try {
            m.group(-1);
            fail("Given group should not be valid");
        } catch (IndexOutOfBoundsException e) {
            /* Test fails only is there is no exception */
        }
        try {
            m.group(5);
            fail("Given group should not be valid");
        } catch (IndexOutOfBoundsException e) {
            /* Test fails only is there is no exception */
        }
    }
}