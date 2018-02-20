/*
    Copyright (C) 2017  Konstantinos Psychas <kpsychas@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kpsychas.lib;


import java.util.*;

public class Pattern {
    private final GroupNode root;
    private final String p;

    private Pattern(String p) throws PatternSyntaxException {
        this.p = p;
        root = compile();
    }

    /**
     * Following official library's way to return Pattern object
     * through a compile function.
     *
     * Grammar of regular expressions supported
     * REGEX := EXPR REGEX | EXPR | ""
     * EXPR := EXPR MOD | RANGE_EXPR | GROUP_EXPR | LITERAL
     * RANGE_EXPR := RANGE_START RANGE RANGE_END
     * RANGE := LITERAL "-" LITERAL RANGE | LITERAL RANGE | ""
     * GROUP_EXPR := "(" EXPR ")"
     * MOD := "*" | "?" | "+"
     * RANGE_START := "[^" | "["
     * RANGE_END := "]"
     * LITERAL := 0-9 | a-z | A-Z
     */
    public static Pattern compile(String p) throws PatternSyntaxException {
        return new Pattern(p);
    }

    enum QuantType {
        STAR, QUESTIONMARK, PLUS, NONE
    }

    enum State {
        EXPR, MOD, IN_RANGE, IN_RANGE_AFTER_DASH, IN_RANGE_BEFORE_DASH,
    }

    private GroupNode compile() throws PatternSyntaxException {
        GroupNode root = new GroupNode(null);
        GroupNode currentGroup = root;
        BaseNode currentNode = root;
        RangeNode range = null;
        State state = State.EXPR;

        Character prev, curr, next, range_start=Character.MIN_VALUE;

        for (int i = 0; i < p.length(); i += 1) {
            try {
                prev = p.charAt(i - 1);
            } catch (IndexOutOfBoundsException e) {
                prev = Character.MIN_VALUE;
            }
            curr = p.charAt(i);
            try {
                next = p.charAt(i + 1);
            } catch (IndexOutOfBoundsException e) {
                next = Character.MIN_VALUE;
            }

            switch (curr) {
                case '-':
                    if (state == State.IN_RANGE_BEFORE_DASH) {
                        state = State.IN_RANGE_AFTER_DASH;
                    } else {
                        throw new PatternSyntaxException("Unexpected character '-'", p, i);
                    }
                    break;
                case '*':
                    if (state == State.MOD) {
                        currentNode.mod = QuantType.STAR;
                        state = State.EXPR;
                    } else {
                        throw new PatternSyntaxException("Unexpected character '*'", p, i);
                    }
                    break;
                case '?':
                    if (state == State.MOD) {
                        currentNode.mod = QuantType.QUESTIONMARK;
                        state = State.EXPR;
                    } else {
                        throw new PatternSyntaxException("Unexpected character '?'", p, i);
                    }
                    break;
                case '+':
                    if (state == State.MOD) {
                        currentNode.mod = QuantType.PLUS;
                        state = State.EXPR;
                    } else {
                        throw new PatternSyntaxException("Unexpected character '+'", p, i);
                    }
                    break;
                case '[':
                    if (state == State.EXPR) {
                        range = new RangeNode(currentNode, next == '^');

                        state = State.IN_RANGE;
                    } else {
                        throw new PatternSyntaxException("Unexpected character '['", p, i);
                    }
                    break;
                case '^':
                    if (state == State.IN_RANGE) {
                        if (prev != '[') {
                            throw new PatternSyntaxException(
                                    "Character '^' only expected at beginning of range", p, i);
                        }
                    } else {
                        throw new PatternSyntaxException(
                                "Character '^' only expected at beginning of range", p, i);
                    }
                    break;
                case ']':
                    if (state == State.IN_RANGE) {
                        currentGroup.add(range);
                        if (isMod(next)) {
                            state = State.MOD;
                        } else {
                            state = State.EXPR;
                        }
                        currentNode = range;
                    } else {
                        throw new PatternSyntaxException("Unexpected character ']'", p, i);
                    }
                    break;
                case '(':
                    if (state == State.EXPR) {
                        GroupNode newGroup = new GroupNode(currentGroup);
                        currentGroup.add(newGroup);
                        currentGroup = newGroup;
                    } else {
                        throw new PatternSyntaxException("Unexpected character '('", p, i);
                    }
                    break;
                case ')':
                    if (state == State.EXPR) {
                        if (currentGroup == root) {
                            throw new PatternSyntaxException("Unexpected character ')'", p, i);
                        }
                        currentNode = currentGroup;
                        currentGroup = (GroupNode)currentGroup.parent;
                        if (isMod(next)) {
                            state = State.MOD;
                        } else {
                            state = State.EXPR;
                        }
                    } else {
                        throw new PatternSyntaxException("Unexpected character ')'", p, i);
                    }
                    break;
                default:
                    if (isLiteral(curr)) {
                        if (state == State.IN_RANGE_AFTER_DASH) {
                            if (!range.add_range(range_start, curr)) {
                                throw new PatternSyntaxException(
                                        "Range limits: " + range_start + ',' + curr + " are incompatible", p, i);
                            }
                            state = State.IN_RANGE;
                        } else if (state == State.IN_RANGE) {
                            if (next == '-') {
                                range_start = curr;
                                state = State.IN_RANGE_BEFORE_DASH;
                            } else {
                                range.add_range(curr, curr);
                            }
                        } else if (state == State.EXPR) {
                            currentNode = new LiteralNode(currentNode, curr);
                            currentGroup.add(currentNode);

                            if (isMod(next)) {
                                state = State.MOD;
                            }
                        }
                    } else {
                        throw new PatternSyntaxException(
                                "Illegal character '" + curr + "' in regular expression", p, i);
                    }
            }
        }
        if (currentGroup != root) {
            throw new PatternSyntaxException(
                    "There is an unmatched opening parenthesis in the expression", p, p.length());
        }
        if (state != State.EXPR) {
            throw new PatternSyntaxException(
                    "There is an unmatched opening bracket in the expression", p, p.length());
        }
        return root;

    }

    private static boolean isMod(Character c) {
        return ((c == '*') || (c == '?') || (c == '+'));
    }

    private static boolean isLiteral(Character c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static void printPatternRec(GroupNode g, int level) {
        for (BaseNode node : g.children) {
            if (node instanceof GroupNode) {
                System.out.println("Beginning of group at level " + level);
                printPatternRec((GroupNode)node, level+1);
                System.out.println("End of group at level " + level);
            } else if (node instanceof RangeNode) {
                System.out.println("Range with " + (((RangeNode) node).isNeg ?
                        "Non Matching characters:":"Matching characters") + ((RangeNode) node).charset);
            } else if (node instanceof LiteralNode) {
                System.out.println("Literal character: " + ((LiteralNode) node).c);
            }
            System.out.println("Modifier: " + node.mod);
        }
    }

    /**
     * Convenience function for debugging parsing of regular expression.
     */
    public void printPattern() {
        printPatternRec(root, 0);
    }

    boolean matches(Matcher matcher) {
        return matches(matcher, 0);
    }

    boolean matches(Matcher matcher, int index) {
        matcher.match = root.match(matcher.text, index);
        return (matcher.match != null);
    }

    public Matcher matcher(CharSequence seq) {
        return new Matcher(this, seq);
    }

    /**
     * Returns true if regex matches the sequence starting from the first character
     */
    public boolean matches(CharSequence seq) {
        return matches(seq, 0);
    }

    /**
     * Returns true if regex matches the sequence starting from the character
     * at position index (indexing starts at 0).
     */
    public boolean matches(CharSequence seq, int index) {
        QualMatchRecord match = root.match(seq, index);
        return (match != null);
    }

    /* Local classes */
    static abstract class BaseNode {
        final BaseNode parent;
        QuantType mod;

        public BaseNode(BaseNode parent) {
            this.parent = parent;
            this.mod = QuantType.NONE;
        }

        // Method is not used as a member of this abstract class.
        // Kept for documentation purposes.
        public abstract boolean matchRec(CharSequence seq, int index, QualMatchRecord qmr);
        public abstract QualMatchRecord match(CharSequence seq, int index);
    }

    static class RangeNode extends BaseNode {
        final boolean isNeg;
        final Set<Character> charset = new HashSet<>();

        RangeNode(BaseNode parent, boolean isNeg) {
            super(parent);
            this.isNeg = isNeg;
        }

        public boolean add_range(Character from, Character to) {
            if (!same_class(from, to)) {
                return false;
            }

            if (Character.compare(from, to) > 0) {
                return false;
            }

            for (char c=from; c<=to; c++) {
                charset.add(c);
            }
            return true;
        }

        private static boolean same_class(Character c1, Character c2) {
            return  ((c1 >= 'A') && (c1 <= 'Z') && (c2 >= 'A') && (c2 <= 'Z')) ||
                    ((c1 >= 'a') && (c1 <= 'z') && (c2 >= 'a') && (c2 <= 'z')) ||
                    ((c1 >= '0') && (c1 <= '9') && (c2 >= '0') && (c2 <= '9'));
        }

        public boolean matchRec(CharSequence seq, int index, QualMatchRecord qmr) {
            if (index >= seq.length()) return qmr.isMatched();

            if (charset.contains(seq.charAt(index)) != isNeg) {
                MatchRecord mr = new MatchRecord(index, 1);
                if (qmr.addMatch(mr))
                    return matchRec(seq, index+1, qmr);
                else
                    return qmr.isMatched();
            } else {
                return qmr.isMatched();
            }
        }

        public QualMatchRecord match(CharSequence seq, int index) {
            QualMatchRecord qmr = new QualMatchRecord(mod, this, index);

            if (matchRec(seq, index, qmr)) return qmr;

            return null;
        }
    }

    static class LiteralNode extends BaseNode {
        final Character c;

        LiteralNode(BaseNode parent, Character c) {
            super(parent);
            this.c = c;
        }

        public boolean matchRec(CharSequence seq, int index, QualMatchRecord qmr) {
            if (index >= seq.length()) return qmr.isMatched();

            if (seq.charAt(index) == c) {
                MatchRecord mr = new MatchRecord(index, 1);
                if (qmr.addMatch(mr))
                    return matchRec(seq, index+1, qmr);
                else
                    return qmr.isMatched();
            } else {
                return qmr.isMatched();
            }
        }

        public QualMatchRecord match(CharSequence seq, int index) {
            QualMatchRecord qmr = new QualMatchRecord(mod, this, index);

            if (matchRec(seq, index, qmr)) return qmr;

            return null;
        }
    }

    static class GroupNode extends BaseNode {
        final List<BaseNode> children;

        GroupNode(BaseNode parent) {
            super(parent);
            children = new ArrayList<>();
        }

        public void add(BaseNode child) {
            children.add(child);
        }

        private boolean matchRec2(CharSequence seq, int index, List<BaseNode> tokens, MatchRecord mr) {
            QualMatchRecord qmr;
            boolean hasBacktracked;

            // stopping condition, no more tokens = everything was matched
            if (tokens.isEmpty()) return true;

            // attempt to match next node
            BaseNode node = tokens.get(0);
            qmr = node.match(seq, index);

            if ((qmr == null) || !qmr.isMatched()) return false;

            // add token record and try to match remaining tokens
            mr.addQualMatch(qmr);
            while (!matchRec2(seq, qmr.matchEnd(), tokens.subList(1, tokens.size()), mr)) {
                // try to rematch by changing essentially only qmr
                // return false if not possible
                hasBacktracked = qmr.backtrack(seq);
                if (!hasBacktracked) {
                    mr.removeQualMatch(qmr);
                    return false;
                }
            }
            return true;

        }

        public boolean matchRec(CharSequence seq, int index, QualMatchRecord qmr) {
            if (index >= seq.length()) return qmr.isMatched();
            boolean isMatched;
            MatchRecord mr = new MatchRecord(index, 0);

            isMatched = matchRec2(seq, index, children, mr);

            if (isMatched) {
                if (qmr.addMatch(mr) && mr.matchLen > 0)
                    return matchRec(seq, index+mr.matchLen, qmr);
                else
                    return qmr.isMatched();
            } else {
                return qmr.isMatched();
            }
        }

        public QualMatchRecord match(CharSequence seq, int index) {
            QualMatchRecord qmr = new QualMatchRecord(mod, this, index);

            if (matchRec(seq, index, qmr)) return qmr;

            return null;
        }
    }

    /*
     * Match Record:
     * keeps track of matches of a non qualified expression.
     * A non group record will have no match in the list.
     */
    static class MatchRecord {
        final int matchLoc;
        int matchLen;
        final List<QualMatchRecord> recordList;

        public MatchRecord(int matchLoc, int matchLen) {
            this.matchLoc = matchLoc;
            this.matchLen = matchLen;
            recordList = new ArrayList<>();
        }

        public void addQualMatch(QualMatchRecord qmr) {
            recordList.add(qmr);
            matchLen += qmr.matchLen();
        }

        public void removeQualMatch(QualMatchRecord qmr) {
            recordList.remove(qmr);
            matchLen -= qmr.matchLen();
        }

        public int matchEnd() {
            if (recordList.isEmpty()) {
                return matchLoc + matchLen;
            } else {
                return recordList.get(recordList.size()-1).matchEnd();
            }
        }

        public boolean backtrack(CharSequence seq) {
            if (recordList.isEmpty()) return false;

            boolean hasBacktracked;
            Stack<BaseNode> nodeStack = new Stack<>();

            for (int i=recordList.size()-1; i >= 0; i--) {
                QualMatchRecord qmr = recordList.get(i);
                hasBacktracked = qmr.backtrack(seq);

                if (hasBacktracked) {
                    //match removed records
                    while(!nodeStack.isEmpty()) {
                        BaseNode node = nodeStack.pop();
                        qmr = node.match(seq, qmr.matchEnd());
                        if (qmr == null) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    //remove record
                    this.removeQualMatch(qmr);
                    nodeStack.add(qmr.node);
                }

            }

            return false;
        }
    }

    /*
     * Qualified Match Record:
     * keeps track of matches of a qualified expression i.e x? x+ x*
     */
    static class QualMatchRecord {
        final int matchStart;
        final QuantType quant;
        final BaseNode node;

        final Stack<MatchRecord> matches = new Stack<>();

        QualMatchRecord(QuantType quant, BaseNode node, int matchStart) {
            this.quant = quant;
            this.node = node;
            this.matchStart = matchStart;
        }

        public boolean addMatch(MatchRecord mr) {
            if (mr == null) return false;

            switch(quant) {
                case NONE:
                case QUESTIONMARK:
                    if (matches.size() > 0) {
                        return false;
                    }
                    // else
                case PLUS:
                case STAR:
                default:
                    matches.add(mr);
                    return true;
            }
        }

        public boolean backtrack(CharSequence seq) {
            MatchRecord mr;
            boolean hasBacktracked;

            if (quant == QuantType.NONE || quant == QuantType.PLUS) {
                if (matches.isEmpty()) return false;

                mr = matches.peek();
                hasBacktracked = mr.backtrack(seq);

                if (hasBacktracked) {
                    return true;
                } else if (matches.size() > 1) {
                    matches.pop();
                    return true;
                } else {
                    return false;
                }
            } else {
                if (matches.isEmpty()) return false;

                mr = matches.peek();
                hasBacktracked = mr.backtrack(seq);

                if (hasBacktracked) {
                    return true;
                } else {
                    matches.pop();
                    return true;
                }
            }
        }

        public boolean isMatched() {
            switch(quant) {
                case STAR:
                case QUESTIONMARK:
                    return true;
                case PLUS:
                case NONE:
                default:
                    return matches.size() > 0;
            }
        }

        public int matchEnd() {
            if (matches.isEmpty()) {
                return matchStart;
            } else {
                return matches.peek().matchEnd();
            }
        }

        public int matchLen() {
            return matchEnd() - matchStart;
        }

        public void printSelf() {
            System.out.println("---------------------");
            System.out.println("Start index:" + matchStart);
            System.out.println("End index:" + matchEnd());
            System.out.println("Total matches:" + matches.size());
            System.out.println("Quantifier:" + quant);
            System.out.println("---------------------");
        }
    }
}
