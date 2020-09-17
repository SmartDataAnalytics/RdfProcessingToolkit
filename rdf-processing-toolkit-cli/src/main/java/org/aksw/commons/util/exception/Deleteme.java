package org.aksw.commons.util.exception;

class Foo {
    int i = 666;

    public class Bar {
        void print() { System.out.println(i); }
    }
}

public class Deleteme
    extends Foo
{
    public class Baz
        extends Bar {
        void print() { System.out.println("yay"); }

    }

    public static void main(String[] args) {
        Deleteme foo = new Deleteme();
        Bar bar = foo.new Baz();

        bar.print();
    }
}
