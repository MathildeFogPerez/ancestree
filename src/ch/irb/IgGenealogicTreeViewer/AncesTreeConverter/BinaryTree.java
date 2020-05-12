package ch.irb.IgGenealogicTreeViewer.AncesTreeConverter;

import java.util.*;
import java.io.*;

/*
 * Copyright 2020 - Mathilde Foglierini Perez

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This class will create BinaryTree using newick format and will generate a string with all parent/kid relationships
 */

public class BinaryTree<E> {
    private BinaryTree<E> left, right;    // children; can be null
    E data;

    /**
     * Constructs leaf node -- left and right are null
     */
    public BinaryTree(E data) {
        this.data = data;
        this.left = null;
        this.right = null;
    }

    /**
     * Constructs inner node
     */
    public BinaryTree(E data, BinaryTree<E> left, BinaryTree<E> right) {
        this.data = data;
        this.left = left;
        this.right = right;
        // String kids= String.format("%s%s", left, right);
        // System.out.println("Parent: "+data+", children "+kids);
    }

    /**
     * Is it an inner node?
     */
    public boolean isInner() {
        return left != null || right != null;
    }

    /**
     * Is it a leaf node?
     */
    public boolean isLeaf() {
        return left == null && right == null;
    }

    /**
     * Does it have a left child?
     */
    public boolean hasLeft() {
        return left != null;
    }

    /**
     * Does it have a right child?
     */
    public boolean hasRight() {
        return right != null;
    }

    /**
     * Number of nodes (inner and leaf) in tree
     */
    public int size() {
        int num = 1;
        if (hasLeft()) num += left.size();
        if (hasRight()) num += right.size();
        return num;
    }


    /**
     * Leaves, in order from left to right
     */
    public ArrayList<E> fringe() {
        ArrayList<E> f = new ArrayList<E>();
        addToFringe(f);
        return f;
    }

    /**
     * Helper for fringe, adding fringe data to the list
     */
    public void addToFringe(ArrayList<E> fringe) {
        if (isLeaf()) {
            fringe.add(data);
        } else {
            if (hasLeft()) left.addToFringe(fringe);
            if (hasRight()) right.addToFringe(fringe);
        }
    }

    /**
     * Returns a string representation of the tree
     */
    public String toString() {
        return toStringHelper("");
    }

    /**
     * Recursively constructs a String representation of the tree from this node,
     * starting with the given indentation and indenting further going down the tree
     */
    public String toStringHelper(String indent) {
        String res = indent + data + "\n";
        if (hasLeft()) res += left.toStringHelper(indent + "  "); //was left.toStringHelper(indent + "  ");
        if (hasRight()) res += right.toStringHelper(indent + "  ");
        return res;
    }


    /**
     * Returns a string representation of the tree
     */
    public String toTreeRelationship() {
        return toParentKid("");
    }
    /**
     * Recursively constructs a String representation of the tree from this node,
     * starting with the given indentation and indenting further going down the tree
     */
    public String toParentKid(String indent) {
        String res = indent + data + "\n";
        if (hasLeft()) res += data+" "+left.toParentKid(""); //was left.toStringHelper(indent + "  ");
        if (hasRight()) res += data+" "+right.toParentKid("");
        return res;
    }

    /**
     * Very simplistic binary tree parser based on Newick representation
     * Assumes that each node is given a label; that becomes the data
     * Any distance information (following the colon) is stripped
     * <tree> = "(" <tree> "," <tree> ")" <label> [":"<dist>]
     * | <label> [":"<dist>]
     * No effort at all to handle malformed trees or those not following these strict requirements
     */
    public static BinaryTree<String> parseNewick(String s) {
        BinaryTree<String> t = parseNewick(new StringTokenizer(s, "(,)", true));
        // Get rid of the semicolon
        t.data = t.data.substring(0, t.data.length() - 1);
        return t;
    }

    /**
     * Does the real work of parsing, now given a tokenizer for the string
     */
    public static BinaryTree<String> parseNewick(StringTokenizer st) {
        String token = st.nextToken();
        if (token.equals("(")) {
            // Inner node
            BinaryTree<String> left = parseNewick(st);
            String comma = st.nextToken();
            BinaryTree<String> right = parseNewick(st);
            String close = st.nextToken();
            String label = st.nextToken();
            String[] pieces = label.split(":");
            //System.out.println("Inner node: " + pieces[0]);
            return new BinaryTree<String>(pieces[0], left, right);
        } else {
            // Leaf
            String[] pieces = token.split(":");
            //System.out.println("Leaf node: " + pieces[0]);
            return new BinaryTree<String>(pieces[0]);
        }
    }
    /**
     * Some tree testing
     */
    public static void main(String[] args) throws IOException {
        // Smaller trees
        String t1string = "(((2:0.0000004385,3:0.0000000100)0_0:0.0000008347,(8:0.0000006790,7:0.0000006790)0_1:0.0099833738)0_4:0.2796689018,1_GERM:0.0000100000);";
        //String t1string = "(26546513216:0.0000004385,3885644:0.0000000100)15652632_GERM:0.0000100000;";
        System.out.println(t1string);
        BinaryTree<String> t1 = parseNewick(t1string);
        System.out.println("t1:" + t1.toTreeRelationship() + "\n///////////////////////////");
    }
}