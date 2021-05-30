package ch.fhnw.mada.huffman;

public class Leaf {

    char   data;
    int    freq;
    String code;
    Leaf   left, right;

    public Leaf(char data, int freq) {
        this.freq = freq;
        this.data = data;
    }

    public Leaf(int freq, Leaf left, Leaf right) {
        this.freq = freq;
        this.left = left;
        this.right = right;
    }

    public Leaf(char data, String code) {
        this.data = data;
        this.code = code;
    }

    public String toString() {
        return data + "->" + freq;
    }

    public boolean isLast() {
        return left == null && right == null;
    }

    public char getData() {
        return data;
    }

    public void setData(char data) {
        this.data = data;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Leaf getLeft() {
        return left;
    }

    public void setLeft(Leaf left) {
        this.left = left;
    }

    public Leaf getRight() {
        return right;
    }

    public void setRight(Leaf right) {
        this.right = right;
    }
}
