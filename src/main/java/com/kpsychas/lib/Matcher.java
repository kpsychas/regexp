package com.kpsychas.lib;

/**
 * Created by kon on 3/10/2017.
 */
public class Matcher {
    CharSequence text;
    Pattern.QualMatchRecord match;

    Matcher(Pattern p, CharSequence text) {
        this.text = text;
        p.matches(this);
    }

    public String group() {
        return group(0);
    }

    public boolean matches() {
        return (match != null);
    }

    public String group(int group) {
        if (match == null) {
            throw new IllegalStateException("No match found");
        }
        if (group < 0) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        StringBuilder s = new StringBuilder();
        int groups_parsed = group_rec(match, text, 0, group, s);

        if (groups_parsed > group) {
            return s.toString();
        } else {
            throw new IndexOutOfBoundsException("No group " + group);
        }
    }

    private int group_rec(Pattern.QualMatchRecord match, CharSequence text,
                          int group, int group_target, StringBuilder result) {
        if (!match.matches.isEmpty()) {
            if (group == group_target) {
                int s, e;
                if (group_target == 0) {
                    s = match.matchStart;
                    e = match.matchEnd();
                } else {
                    s = match.matches.peek().matchLoc;
                    e = match.matches.peek().matchEnd();
                }
                result.append(text.toString().substring(s, e));
                return group + 1;
            } else {
                group++;
            }
        }

        for (Pattern.QualMatchRecord qmr : match.matches.peek().recordList) {
            if (qmr.node instanceof Pattern.GroupNode) {
                if (group > group_target) return group;
                group = group_rec(qmr, text, group, group_target, result);
            }
        }
        return group;
    }
    /**
     * Function that prints what was matched as well as the last match of a group.
     */
    public void printMatch() {
        if (this.match == null)
            System.out.println("No matching was found");
        else
            printMatchRec(this.text, this.match, 0);
    }

    private static int printMatchRec(CharSequence seq, Pattern.QualMatchRecord match, int group) {
        if (match.matches != null && !match.matches.isEmpty()) {
            if (group == 0) {
                int s = match.matchStart;
                int e = match.matchEnd();

                System.out.printf("Full expression match from %d to %d: %s\n", s, e, seq.toString().substring(s, e));
                group++;
            } else {
                int s = match.matches.peek().matchLoc;
                int e = match.matches.peek().matchEnd();
                System.out.printf("Group %d match from %d to %d: %s\n", group, s, e, seq.toString().substring(s, e));
                group++;
            }
            for (Pattern.QualMatchRecord qmr : match.matches.peek().recordList) {
                if (qmr.node instanceof Pattern.GroupNode) {
                    group = printMatchRec(seq, qmr, group);
                }
            }
            return group;
        } else {
            if (group == 0) {
                int s = match.matchStart;
                int e = match.matchEnd();

                System.out.printf("Full expression match from %d to %d: %s\n", s, e, seq.toString().substring(s, e));
                group++;
            } else {
                /* Empty match is possible */
                int s = match.matchStart;
                int e = match.matchEnd();
                System.out.printf("Group %d match from %d to %d: %s\n", group, s, e, seq.toString().substring(s, e));
                group++;
            }
            return group;
        }
    }
}
